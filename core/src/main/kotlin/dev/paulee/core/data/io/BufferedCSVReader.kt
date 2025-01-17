package dev.paulee.core.data.io

import dev.paulee.core.splitStr
import java.nio.file.Path
import kotlin.io.path.bufferedReader

internal class BufferedCSVReader(path: Path, private val delimiter: Char = ',') {

    var errorCount: Long = 0

    private var reader = path.bufferedReader()

    private var headSize: Int = -1

    fun readLines(callback: (List<Map<String, String>>) -> Unit) {
        val batch = mutableListOf<Map<String, String>>()

        reader.use {
            val head = this.readLine() ?: return

            val header = splitStr(head, delimiter)

            this.headSize = header.size

            var line: String? = this.readLine()
            while (line != null) {
                val split = splitStr(line, delimiter)

                if (split.size == this.headSize) {
                    val headToValue = mutableMapOf<String, String>()

                    split.forEachIndexed { index, entry -> headToValue[header[index]] = entry.trim('"') }

                    batch.add(headToValue)
                } else errorCount++

                if (batch.size == 100) callback(batch).also { batch.clear() }

                line = this.readLine()
            }

            if (batch.isNotEmpty()) callback(batch)
        }
    }

    private fun readLine(): String? {
        val line = runCatching { this.reader.readLine() }.getOrNull() ?: return null

        if (this.headSize == -1 || (this.getDelimiterCount(line) + 1) == this.headSize) return line

        var fullLine = line

        while ((this.getDelimiterCount(fullLine) + 1) < this.headSize) fullLine += runCatching { this.reader.readLine() }.getOrNull()
            ?.trim() ?: ""

        return fullLine
    }

    private fun getDelimiterCount(str: String): Int {
        var amount = 0

        var insideQuotes = false
        str.forEach {
            if (it == '"') insideQuotes = !insideQuotes

            if (it == delimiter && !insideQuotes) amount++
        }

        return amount
    }
}