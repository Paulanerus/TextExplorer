package dev.paulee.api.data.provider

import dev.paulee.api.data.RequiresData
import java.io.Closeable
import java.nio.file.Path

enum class StorageType {
    SQLITE,
    BINARY,
}

interface IStorageProvider : Closeable {

    fun init(dataInfo: RequiresData, path: Path): Int

    fun insert(name: String, entry: List<Map<String, String>>)

}