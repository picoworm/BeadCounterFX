/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package GUI

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import tornadofx.button
import tornadofx.plusAssign

class CommunicationStatusDialog(onCancel: ()->Unit) {
    val stage: Stage
    val Status = Label("Установка связи с коммуникационным блоком. Подождите.")

    init {
        stage = Stage().apply {
            initModality(Modality.APPLICATION_MODAL)
            initStyle(StageStyle.DECORATED)//UTILITY);
            title = "Связь"
            scene = Scene(HBox(5.0).apply {
                alignment = Pos.CENTER
                padding = Insets(10.0);
                this += Status
                button("Отмена") {
                    isDefaultButton = true
                    setOnAction { close(); onCancel() }
                }
            })
        }
        stage.show()
    }

    fun setLabel(text: String) {
        Status.text = text
    }

    fun close() {
        stage.close()
    }
}
