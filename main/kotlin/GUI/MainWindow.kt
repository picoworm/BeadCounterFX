/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package GUI

import BeadCounter.Feeder
import BeadCounter.NumberFormat
import Comm.CommThread
import Comm.Device
import de.jensd.fx.glyphs.GlyphsDude
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ObservableObjectValue
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import tornadofx.*
import java.io.File
import java.io.StringReader
import java.util.*
import java.util.concurrent.Callable
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject
import javax.xml.bind.DatatypeConverter

class MainWindow(val CT: CommThread) : Fragment() {
    val DM = CT.DM!!
    override val root = VBox()

    val WorkingCount = SimpleIntegerProperty(0)
    val Working = Bindings.createBooleanBinding(Callable { WorkingCount.get() > 0 }, WorkingCount)
    val UnusedDevs = FXCollections.observableArrayList<Device>()
    val Feeders = FXCollections.observableArrayList<Feeder>()
    val Log = TextArea()

    val WorkTable = TableView(Feeders)

    val UnusedDevList = ListView(UnusedDevs).apply {
        setCellFactory { lv ->
            ListCell<Device>().apply {
                setOnDragDetected {
                    if (!isEmpty) {
                        val db = startDragAndDrop(TransferMode.MOVE)
                        val cc = ClipboardContent()
                        cc.putString(item.toString())
                        db.setContent(cc)
                        Dragging = item
                    }
                }

                val cellContextMenu = ContextMenu()
                cellContextMenu.items.add(MenuItem("Тест").apply {
                    setOnAction { DevTestDialog(item, null) }
                })

                emptyProperty().addListener { obs, wasEmpty, isNowEmpty ->
                    if (isNowEmpty) {
                        contextMenu = null
                        text = ""
                    } else {
                        contextMenu = cellContextMenu
                        text = item.toString()
                    }
                }
            }
        }
    }

    val FeedersTable = TableView(Feeders).apply {
        column("Название", Feeder::Name).makeEditable()
        column<Feeder, ObservableObjectValue<ByteArray?>?>("Номер счетчика", Feeder::devUID).setCellFactory {
            object : TableCell<Feeder, ObservableObjectValue<ByteArray?>?>() {
                override fun updateItem(item: ObservableObjectValue<ByteArray?>?, empty: Boolean) {
                    if (!empty && item != null) {
                        graphicProperty().bind(Bindings.createObjectBinding(Callable {
                            when (rowItem.DevState.value) {
                                1 -> GlyphsDude.createIcon(FontAwesomeIcon.EXPAND)// Disconnected
                                2 -> GlyphsDude.createIcon(FontAwesomeIcon.CHECK) // Ok
                                else -> GlyphsDude.createIcon(FontAwesomeIcon.CLOSE) // Red
                            }
                        }, rowItem.DevState))

                        textProperty().bind(Bindings.createStringBinding(Callable {
                            if (item.value != null) {
                                Arrays.toString(item.value)
                            } else "Нет счетчика"
                        }, item))
                    } else {
                        graphicProperty().unbind()
                        textProperty().unbind()
                        graphic = null; text = null
                    }
                }
            }
        }
        isEditable = true
        setRowFactory { cb ->
            val row = TableRow<Feeder>()
            val contextMenu = ContextMenu().apply {
                menuitem("Добавить дозатор") { Platform.runLater { Feeders.addAll(Feeder()) } }
                val disconnectItem = menuitem("Разъединить устройство") {
                    val item = row.item
                    RemoveDev(item)
                }
                val setting = menuitem("Настройка") { DevTestDialog(row.item.dev.value, row.item) }
                val removeFeeder = menuitem("Удалить") {
                    val item = row.item
                    RemoveDev(item)
                    Feeders.remove(item)
                }
                setOnShowing {
                    var Vis = false
                    if (!row.isEmpty) {
                        if (row.item.isDevAssigned) Vis = true
                    }
                    removeFeeder.isVisible = !row.isEmpty
                    disconnectItem.isVisible = Vis
                    setting.isVisible = Vis
                }
            }

            row.contextMenu = contextMenu
            row.setOnDragOver {
                // data is dragged over the target
                if (!row.isEmpty) {
                    //Dragboard db = event.getDragboard();
                    if (it.dragboard.hasString()) {
                        it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                    }
                    it.consume()
                }
            }

            row.setOnDragDropped {
                if (row.item.SetDevice(Dragging!!)) {
                    UnusedDevs.remove(Dragging)
                }
                it.isDropCompleted = true
                it.consume()
            }
            row
        }

        contextMenu = ContextMenu().apply { menuitem("Добавить дозатор") { Platform.runLater { Feeders.addAll(Feeder()) } } }
        columns.forEach { it.isSortable = false }
    }

    init {
        root.apply {
            menubar {
                menu("Файл") {
                    menuitem("Открыть...") {
                        FileChooser().apply {
                            title = "Выбрать программу дозирования"
                            initialDirectory = MainDir.resolve("portions").apply { mkdirs() }
                            showOpenDialog(modalStage)?.apply {
                                loadProgram(this)
                            }
                        }
                    }
                    menuitem("Сохранить как...") {
                        FileChooser().apply {
                            title = "Указать имя программы дозирования"
                            initialDirectory = MainDir.resolve("portions").apply { mkdirs() }
                            showSaveDialog(modalStage)?.apply {
                                storeProgram(this)
                            }
                        }
                    }
                    menuitem("Закрыть") { closeModal() }
                }
            }
            tabpane {
                vboxConstraints { vGrow = Priority.ALWAYS }
                tab("Работа", VBox()) {
                    this += WorkTable.apply {
                        vboxConstraints { vGrow = Priority.ALWAYS }
                        makeIndexColumn()
                        column("Дозатор", Feeder::Name)
                        column("Маркировка", Feeder::BeadType).makeEditable()
                        column("Показание счетчика", Feeder::CounterValue)
                        column("Остаток", Feeder::RemainValue)
                        column("Состояние", Feeder::DevState)
                        column("Задание (значение)", Feeder::ToCount).run { makeEditable() }
                        isEditable = true
                        columns.forEach { it.isSortable = false }
                    }
                    hbox(10.0) {
                        padding = Insets(20.0)
                        alignment = Pos.CENTER_RIGHT
                        button("Старт").apply {
                            disableProperty().bind(Working)
                            setOnAction { startButtonPressed() }
                        }
                        button("Стоп").apply {
                            disableProperty().bind(Working.not())
                            setOnAction { stopButtonPressed() }
                        }
                    }
                }.isClosable = false
                tab("Настройка", SplitPane()) {
                    items {
                        titledpane("Незадействованные счетчики", UnusedDevList).apply { isCollapsible = false }
                        titledpane("Дозаторы", FeedersTable).apply { isCollapsible = false }
                    }
                    orientation = Orientation.HORIZONTAL
                }.isClosable = false
                tab("Журнал", Log).isClosable = false
            }
        }
    }

    fun newDeviceDetected(Dev: Device) {
        for (feeder in Feeders) {
            if (feeder.devUID.value != null) {
                if (Arrays.equals(Dev.UID, feeder.devUID.value)) {
                    feeder.ConnectDevice(Dev)
                    return
                }
            }
        }
        UnusedDevs.add(Dev)
    }

    fun deviceDisconnectedEvent(Dev: Device) {
        val f = Feeders.find { it.dev.value == Dev }
        if (f != null) f.DisconnectDevice() else UnusedDevs.remove(Dev)
    }

    fun LOGme(S: String) {
        Log.appendText("$S\n")
    }

    internal var Dragging: Device? = null

    fun show() {
        MW = this
        tryPST {
            tryPST {
                MainDir.mkdirs()
                Json.createReader(StringReader(MainDir.resolve("Feeders.conf").readText())).readArray()?.forEach {
                    it as JsonObject
                    println(it)
                    Feeders.add(Feeder().apply {
                        Name.value = it.getString("Name")
                        MaxPower.value = it.getInt("MaxPower", MaxPower.value)
                        MinPower.value = it.getInt("MinPower", MinPower.value)
                        MinForcePower.value = it.getInt("MinForcePower", MinForcePower.value)
                        CompVoltage.value = it.getInt("CompVoltage", CompVoltage.value)
                        it.getString("devUID", null)?.run { devUID.value = DatatypeConverter.parseBase64Binary(this) }
                    })
                }
            }
            loadProgram(MainDir.resolve("autosave.beads"))

            title = "BeadCounter"

            openModal()

            tryPST {
                Json.createReader(StringReader(MainDir.resolve("pref.conf").readText())).readObject()?.apply {
                    getJsonObject("MainStage")?.apply { modalStage?.JsonToLayout(this) }
                    getJsonArray("WorkColumns")?.apply { WorkTable.JsonToColumnsWidth(this) }
                    getJsonArray("SettColumns")?.apply { FeedersTable.JsonToColumnsWidth(this) }
                }
            }

            modalStage?.setOnHiding {
                tryPST {
                    MainDir.resolve("Feeders.conf").writeText(Json.createArrayBuilder().apply {
                        Feeders.forEach {
                            add(Json.createObjectBuilder().apply {
                                add("Name", it.Name.value)
                                add("MaxPower", it.MaxPower.value)
                                add("MinPower", it.MinPower.value)
                                add("MinForcePower", it.MinForcePower.value)
                                add("CompVoltage", it.CompVoltage.value)
                                it.devUID.value?.run { add("devUID", DatatypeConverter.printBase64Binary(this)) }
                            })
                        }
                    }.build().toPrettyString())

                    MainDir.resolve("pref.conf").writeText(Json.createObjectBuilder().apply {
                        add("MainStage", modalStage?.layoutToJson())
                        add("WorkColumns", WorkTable.ColumnsWidthToJson())
                        add("SettColumns", FeedersTable.ColumnsWidthToJson())
                    }.build().toPrettyString())
                }
                storeProgram(MainDir.resolve("autosave.beads"))
            }
        }
    }

    private fun startButtonPressed() {
        DM.sendBroadcast(0x37, 0) // Disable all motors
        DM.sendBroadcast(0x36, 0) // All toCount = 0
        DM.sendBroadcast(0x35, 0) // Reset all counter value (for correct on-screen indication
        WorkingCount.set(0)
        Feeders.forEach { it.Working = false }
        for (feeder in Feeders) {
            // Program tocount registers for each counter
            feeder.dev.value?.run {
                if (feeder.ToCount.get() > 0) {
                    SetToCount(feeder.ToCount.get())
                    EnableMotor(true)
                    WorkingCount.set(WorkingCount.get() + 1)
                    feeder.FinishedCallback = { WorkingCount.set(WorkingCount.get() - 1) }
                    feeder.Working = true
                }
            }
        }
        DM.sendBroadcast(0x37, 1) // Enable all motors
    }

    private fun stopButtonPressed() {
        DM.sendBroadcast(0x37, 0) // Disable all motors
        WorkingCount.set(0)
    }

    internal fun RemoveDev(F: Feeder) {
        val Dev = F.dev.value
        if (Dev != null) UnusedDevs.add(Dev)
        F.RemoveDevice()
    }

    fun loadProgram(file: File) {
        tryPST {
            Json.createReader(StringReader(file.readText())).readArray()?.forEach {
                it as JsonObject
                it.getString("Дозатор", null)?.apply {
                    Feeders.find { it.Name.value == this }?.apply {
                        it.getString("Тип", null)?.apply { BeadType.value = this }
                        it.getInt("Кол-во", 0).apply { ToCount.value = this }
                    }
                }
            }
        }
    }

    fun storeProgram(file: File) {
        tryPST {
            file.writeText(Json.createArrayBuilder().apply {
                Feeders.forEach {
                    this.add(Json.createObjectBuilder().apply {
                        add("Дозатор", it.Name.value)
                        add("Тип", it.BeadType.value)
                        add("Кол-во", it.ToCount.value)
                    })
                }
            }.build().toPrettyString())
        }
    }

    companion object {
        var MW: MainWindow? = null
        val MainDir = File(System.getProperty("user.home") + "/BeadFlow")
    }
}

fun TextField.acceptNumbersOnly(Format: NumberFormat) {
    textProperty().addListener { observable, oldValue, newValue ->
        if (newValue.isEmpty()) text = "0"
        else if (!Format.canParse(newValue)) text = oldValue
    }
    properties.put("vkType", "numeric")
}

fun tryPST(op: () -> Unit) {
    try {
        op()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Stage.layoutToJson(): JsonObject {
    return Json.createObjectBuilder().apply {
        add("X", x)
        add("Y", y)
        add("W", width)
        add("H", height)
    }.build()
}

fun Stage.JsonToLayout(json: JsonObject) {
    with(json) {
        getJsonNumber("X")?.doubleValue()?.apply { x = this }
        getJsonNumber("Y")?.doubleValue()?.apply { y = this }
        getJsonNumber("W")?.doubleValue()?.apply { width = this }
        getJsonNumber("H")?.doubleValue()?.apply { height = this }
    }
}

fun<T> TableView<T>.ColumnsWidthToJson(): JsonArray {
    return Json.createArrayBuilder().apply { columns.forEach { add(it.width) } }.build()
}

fun<T> TableView<T>.JsonToColumnsWidth(json: JsonArray) {
    columns.forEachIndexed { i, it -> json.getJsonNumber(i)?.doubleValue()?.apply { it.prefWidth = this } }
}