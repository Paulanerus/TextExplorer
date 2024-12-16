package dev.paulee.api.data

import dev.paulee.api.data.provider.IStorageProvider
import java.io.Closeable
import java.nio.file.Path

interface IDataService : Closeable {

    fun createDataPool(dataInfo: RequiresData, path: Path): Boolean

    fun loadDataPools(path: Path, dataInfo: Set<RequiresData>): Int

    fun selectDataPool()

    fun getPage(query: String, pageCount: Int = -1): List<Map<String, String>>

    fun getPageCount(query: String): Long
}