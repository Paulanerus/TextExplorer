package dev.paulee.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.paulee.api.data.IDataService
import dev.paulee.api.plugin.IPluginService
import dev.paulee.ui.components.DiffViewerWindow
import dev.paulee.ui.components.DropDownMenu
import dev.paulee.ui.components.FileDialog
import dev.paulee.ui.components.TableView
import java.nio.file.Path
import kotlin.io.path.*

class TextExplorerUI(private val pluginService: IPluginService, private val dataService: IDataService) {

    private val appDir = Path(System.getProperty("user.home")).resolve(".textexplorer")

    private val pluginsDir = appDir.resolve("plugins")

    private val dataDir = appDir.resolve("data")

    private val versionString = "v${System.getProperty("app.version")} ${
        listOf("api", "core", "ui").joinToString(
            prefix = "(", postfix = ")", separator = ", "
        ) {
            "${it.uppercase()} - ${System.getProperty("${it}.version")}"
        }
    }"

    init {
        if (!pluginsDir.exists()) pluginsDir.createDirectories()

        this.pluginService.loadFromDirectory(pluginsDir)

        this.pluginService.initAll()

        val size = this.dataService.loadDataPools(dataDir, this.pluginService.getAllDataInfos())

        println("Loaded $size data pools")
    }

    @Composable
    private fun content() {
        var text by remember { mutableStateOf("") }
        var selectedRows by remember { mutableStateOf(setOf<List<String>>()) }
        var displayDiffWindow by remember { mutableStateOf(false) }
        var showTable by remember { mutableStateOf(false) }
        var isOpened by remember { mutableStateOf(false) }
        var totalPages by remember { mutableStateOf(0L) }
        var currentPage by remember { mutableStateOf(0) }
        var currentData by remember { mutableStateOf(listOf<List<String>>()) }

        val header = listOf(
            "Column 1",
            "Column 2",
            "Column 3",
            "Column 4",
            "Column 5",
            "Column 6",
        )

        val data = List(150) { row ->
            List(header.size) { col -> "${header[col]} - $row" }
        }

        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize()) {

                DropDownMenu(
                    modifier = Modifier.align(Alignment.TopEnd), items = listOf("Load Plugin"), clicked = {
                        when (it) {
                            "Load Plugin" -> isOpened = true
                        }
                    })

                if (isOpened) {
                    FileDialog { paths ->
                        isOpened = false

                        paths.filter { it.extension == "jar" }.forEach {
                            if (loadPlugin(it)) println("Loaded plugin ${it.name}")
                            else println("Failed to load plugin ${it.name}")
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Text Explorer", fontSize = 32.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text("Search...") },
                            modifier = Modifier.width(600.dp).background(
                                color = Color.LightGray,
                                shape = RoundedCornerShape(24.dp),
                            ),
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    text = ""
                                    showTable = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            })

                        IconButton(
                            onClick = {
                                currentPage = 0
                                totalPages = 10L
                                showTable = true
                            },
                            modifier = Modifier.height(70.dp).padding(horizontal = 10.dp),
                            enabled = text.isNotEmpty() && text.isNotBlank()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = showTable) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                        ) {
                            TableView(
                                modifier = Modifier.weight(1f),
                                columns = header,
                                data = data,
                                onRowSelect = { selectedRows = it },
                                clicked = { displayDiffWindow = true })

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentPage > 0) {
                                            currentPage--
                                        }
                                    }, enabled = currentPage > 0
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left")
                                }

                                Text("Page ${currentPage + 1} of $totalPages")

                                IconButton(
                                    onClick = {
                                        if (currentPage < totalPages - 1) {
                                            currentPage++
                                        }
                                    }, enabled = currentPage < totalPages - 1
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right")
                                }
                            }
                        }
                    }
                }

                Text(
                    versionString,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    fontSize = 10.sp,
                    color = Color.LightGray
                )

                if (displayDiffWindow) DiffViewerWindow(selectedRows) { displayDiffWindow = false }
            }
        }
    }

    fun start() = application(exitProcessOnExit = true) {
        val windowState =
            rememberWindowState(position = WindowPosition.Aligned(Alignment.Center), size = DpSize(1600.dp, 900.dp))

        Window(title = "TextExplorer", state = windowState, onCloseRequest = {
            dataService.close()
            exitApplication()
        }) {
            content()
        }
    }

    private fun loadPlugin(path: Path): Boolean {
        val pluginPath = pluginsDir.resolve(path.name)

        if (pluginPath.exists()) return true

        path.copyTo(pluginPath)

        val plugin = pluginService.loadPlugin(pluginPath, true)

        if (plugin == null) return false

        this.pluginService.getDataInfo(plugin)?.let {
            if (it.sources.isEmpty()) return@let

            if (this.dataService.createDataPool(it, dataDir)) println("Created data pool for ${it.name}")
            else println("Failed to create data pool for ${it.name}")
        }
        return true
    }
}