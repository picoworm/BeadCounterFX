/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package GUI

import com.fazecast.jSerialComm.SerialPort
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.StringConverter
import tornadofx.button
import tornadofx.combobox
import tornadofx.hbox
import tornadofx.vboxConstraints

class SerialPortSelectionDialog(PortList: Array<SerialPort>) {
    var SelectedPort: SerialPort? = null
        private set

    init {
        Stage().apply {
            initModality(Modality.APPLICATION_MODAL)
            initStyle(StageStyle.DECORATED)//UTILITY);
            title = "Выбери порт дозатора"
            scene = Scene(VBox(5.0).apply {
                padding = Insets(10.0);
                val CB = combobox<SerialPort> {
                    converter = object : StringConverter<SerialPort>() {
                        override fun toString(obj: SerialPort?): String? = obj?.systemPortName
                        override fun fromString(string: String?): SerialPort? = null
                    }
                    items.addAll(PortList)
                    prefWidth = 250.0
                }
                hbox(20.0) {
                    vboxConstraints { marginTop = 20.0; alignment = Pos.CENTER_RIGHT }
                    button("Cancel") {
                        isCancelButton = true
                        setOnAction { close() }
                    }
                    button("Select") {
                        isDefaultButton = true
                        disableProperty().bind(CB.valueProperty().isNull)
                        setOnAction {
                            SelectedPort = CB.value
                            close()
                        }
                    }
                }
            })
        }.showAndWait()
    }
}
