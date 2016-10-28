/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package GUI

import BeadCounter.Feeder
import BeadCounter.NumberFormat
import Comm.Device
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Separator
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.converter.NumberStringConverter
import tornadofx.*

class DevTestDialog(device: Device?, feeder: Feeder?) : Fragment() {
    override val root = VBox(10.0)

    init {
        with(root) {
            padding = Insets(20.0)
            feeder?.apply {
                hbox(10.0) {
                    alignment = Pos.CENTER
                    label("Макс. мощность на мотор")
                    textfield { textProperty().bindBidirectional(MaxPower, NumberStringConverter()); hboxConstraints { hGrow = Priority.ALWAYS } }
                }
                hbox(10.0) {
                    alignment = Pos.CENTER
                    label("Мин. мощность на мотор")
                    textfield { textProperty().bindBidirectional(MinPower, NumberStringConverter()); hboxConstraints { hGrow = Priority.ALWAYS } }
                }
                hbox(10.0) {
                    alignment = Pos.CENTER
                    label("Мин. мощность на мотор доворот *1")
                    textfield { textProperty().bindBidirectional(MinForcePower, NumberStringConverter()); hboxConstraints { hGrow = Priority.ALWAYS } }
                }
                hbox(10.0) {
                    alignment = Pos.CENTER
                    label("Среднее значение компаратора сигналов")
                    textfield { textProperty().bindBidirectional(CompVoltage, NumberStringConverter()) }
                }
                label("*1 (Мощность применяется в том случае если длит. время нет счета)")
                button("Применить") {
                    setOnAction { updateSettings() }
                    disableProperty().bind(feeder.dev.isNull)
                }
                children += Separator()
            }

            device?.apply {
                hbox(10.0) {
                    val toCount = textfield("0").apply { acceptNumbersOnly(NumberFormat.BeadCountNumberFormat) }
                    button("Отсчитать") {
                        setOnAction {
                            SetToCount(Integer.parseInt(toCount.text))
                            EnableMotor(true)
                        }
                    }
                    button("Стоп") { setOnAction { EnableMotor(false) } }
                }
                checkbox("Мигание светодиодами") { setOnAction { SetBlinkLedsMode(isSelected) } }
                hbox(10.0) {
                    alignment = Pos.CENTER
                    label("Счетчик: "); label().textProperty().bind(device.PartCount.asString())
                    button("Сброс счетчика") { SetCounterValue(0) }
                }
            }

            hbox {
                alignment = Pos.CENTER_RIGHT
                button("Закрыть").apply {
                    isDefaultButton = true
                    setOnAction { closeModal() }
                }
            }
        }

        title = "Настройка"
        openModal()
        modalStage?.setOnHiding { device?.SetBlinkLedsMode(false) }
    }
}
