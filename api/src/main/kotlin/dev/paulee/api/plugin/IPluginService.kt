package dev.paulee.api.plugin

import dev.paulee.api.data.IDataService
import dev.paulee.api.data.RequiresData
import dev.paulee.api.data.ViewFilter
import java.nio.file.Path

interface IPluginService {

    fun loadFromDirectory(path: Path): Int

    fun loadPlugin(path: Path): IPlugin?

    fun getPluginMetadata(plugin: IPlugin): PluginMetadata?

    fun getDataInfo(plugin: IPlugin): RequiresData?

    fun initAll(dataService: IDataService, path: Path)

    fun getPlugins(): List<IPlugin>

    fun getAllDataInfos(): Set<RequiresData>

    fun getDataSources(dataInfo: String): Set<String>

    fun getViewFilter(plugin: IPlugin): ViewFilter?

    fun getVariants(dataInfo: RequiresData?): Set<String>

    fun getPreFilters(dataInfo: RequiresData?): Set<String>
}