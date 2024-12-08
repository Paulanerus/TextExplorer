package dev.paulee.core.data.sql

import dev.paulee.api.data.DataSource
import dev.paulee.api.data.Unique
import org.jetbrains.annotations.Nullable
import java.io.Closeable
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.createDirectories
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor

private enum class ColumnType {
    TEXT, INTEGER, REAL, NUMERIC,
}

private fun typeToColumnType(type: KClassifier): ColumnType = when (type) {
    String::class -> ColumnType.TEXT
    Char::class -> ColumnType.TEXT
    Short::class -> ColumnType.INTEGER
    Int::class -> ColumnType.INTEGER
    Long::class -> ColumnType.INTEGER
    Float::class -> ColumnType.REAL
    Double::class -> ColumnType.REAL
    Boolean::class -> ColumnType.NUMERIC
    else -> throw IllegalArgumentException("Unsupported type: $type")
}

private data class Column(val name: String, val type: ColumnType, val primary: Boolean, val nullable: Boolean) {
    override fun toString(): String = "$name $type ${if (primary) "PRIMARY KEY" else if (nullable) "" else "NOT NULL"}"
}

private class Table(val name: String, columns: List<Column>) {

    val primaryKey: Column = columns.find { it.primary } ?: Column("${name}_ag_id", ColumnType.INTEGER, true, false)

    val columns = listOf(primaryKey) + columns.filter { !it.primary }

    fun createIfNotExists(connection: Connection) {
        connection.createStatement().use {
            it.execute("CREATE TABLE IF NOT EXISTS $name (${columns.joinToString(", ")})")
        }
    }

    fun insert(connection: Connection, entries: List<Map<String, String>>) {

        if (entries.isEmpty()) return

        val placeholders =
            List(entries.size) { List(columns.size) { "?" }.joinToString(", ", prefix = "(", postfix = ")") }

        val query = buildString {
            append("INSERT INTO ")
            append(name)
            append(" (")
            append(columns.joinToString(", ") { it.name })
            append(") VALUES ")
            append(placeholders.joinToString(", "))
        }

        connection.prepareStatement(query).use {
            var size = columns.size
            entries.forEachIndexed { index, map ->
                columns.forEachIndexed { idx, column ->
                    val value = map[column.name] ?: return@forEachIndexed

                    it.setString((size * index) + idx + 1, value)
                }
            }
            it.executeUpdate()
        }
    }

    fun selectAll(
        connection: Connection, whereClause: String? = null, offset: Int = 0, limit: Int = Int.MAX_VALUE
    ): List<Map<String, String>> {
        val query = buildString {
            append("SELECT * FROM ")
            append(name)

            if (!whereClause.isNullOrEmpty()) {
                append(" WHERE ")
                append(whereClause)
            }

            append("OFFSET ")
            append(offset)

            append(" LIMIT ")
            append(limit)
        }

        return connection.createStatement().use { statement ->
            statement.executeQuery(query).use {
                val results = mutableListOf<Map<String, String>>()

                while (it.next()) {
                    val row = columns.associate { column -> column.name to (it.getString(column.name) ?: "") }

                    results.add(row)

                    println(row)
                }

                results
            }
        }
    }

    override fun toString(): String = "$name primary=${primaryKey}, columns={${columns.joinToString(", ")}}"
}

internal class Database(path: Path) : Closeable {

    private val dbPath = "jdbc:sqlite:$path"

    private var connection: Connection? = null

    private val tables = mutableSetOf<Table>()

    init {
        path.parent.createDirectories()
    }

    fun connect() {
        if (this.connection != null) return

        this.connection = DriverManager.getConnection(dbPath)
    }

    fun createTables(klasses: Array<KClass<*>>) {
        klasses.forEach { createTable(it) }
    }

    fun insert(name: String, entries: List<Map<String, String>>) {
        val table = tables.find { it.name == name } ?: return

        table.insert(this.connection ?: return, entries)
    }

    fun createTable(klass: KClass<*>) {
        val tableName = klass.findAnnotation<DataSource>()?.file ?: return

        val columns = klass.primaryConstructor?.parameters.orEmpty().mapNotNull { param ->
            val name = param.name ?: return@mapNotNull null
            val type = param.type.classifier ?: return@mapNotNull null

            val isNullable = param.hasAnnotation<Nullable>()

            val isPrimary = param.hasAnnotation<Unique>() && !isNullable

            Column(name, typeToColumnType(type), isPrimary, isNullable)
        }

        val table = Table(tableName.substringBeforeLast("."), columns)

        transaction {
            table.createIfNotExists(this)

            tables.add(table)
        }
    }

    fun selectAll(
        name: String,
        whereClause: String? = null,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Map<String, String>> {
        val table = tables.find { it.name == name } ?: return emptyList()

        return transaction {
            table.selectAll(this, whereClause, offset, limit)
        }
    }

    fun <T> transaction(block: Connection.() -> T): T {
        val conn = this.connection ?: throw IllegalStateException("Failed to start transaction: Connection is null.")

        conn.autoCommit = false

        return try {
            runCatching { conn.block() }
                .onSuccess {
                    conn.commit()
                }.onFailure {
                    conn.rollback()
                }.getOrThrow()
        } finally {
            conn.autoCommit = true
        }
    }

    override fun close() {
        this.connection?.close()
    }
}