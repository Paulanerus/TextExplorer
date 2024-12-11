package dev.paulee.api.plugin

import java.nio.file.Path

interface IPluginService {

    fun loadFromDirectory(path: Path): Int

    fun loadPlugin(path: Path, init: Boolean = false): Boolean

    fun getPluginMetadata(plugin: IPlugin): PluginMetadata?

    fun initAll()

    fun getPlugins(): List<IPlugin>
}