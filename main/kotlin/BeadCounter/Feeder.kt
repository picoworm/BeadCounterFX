/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package BeadCounter

import Comm.Device
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.JsonModelAuto
import java.util.*

class Feeder : JsonModelAuto {
    val ToCount = SimpleIntegerProperty(0)
    val Name = SimpleStringProperty("Без имени")
    val BeadType = SimpleStringProperty("<нет>")
    val ID = SimpleStringProperty("Нет счетчика")

    val CounterValue = SimpleIntegerProperty(0)
    val RemainValue = SimpleIntegerProperty(0)
    val DevState = SimpleIntegerProperty(0)

    val MinPower = SimpleIntegerProperty(200)
    val MinForcePower = SimpleIntegerProperty(400)
    val MaxPower = SimpleIntegerProperty(4095)
    val CompVoltage = SimpleIntegerProperty(2048)

    var Working: Boolean = false
    var FinishedCallback: ()->Unit = {}

    fun RemoveDevice() {
        DisconnectDevice()
        devUID.value = null
        DevState.set(0)
        ID.value = "Нет счетчика"
    }

    fun SetDevice(Dev: Device): Boolean {
        if (devUID.value != null) return false
        devUID.value = Dev.UID
        ID.value = Arrays.toString(devUID.value)
        ConnectDevice(Dev)
        return true
    }

    fun DisconnectDevice() {
        DevState.set(1)
        dev.value = null
        CounterValue.unbind()
        RemainValue.unbind()
    }

    fun ConnectDevice(Dev: Device) {
        if (devUID.value == null) return
        if (!Arrays.equals(Dev.UID, devUID.value)) return
        dev.value = Dev
        CounterValue.bind(Dev.PartCount)
        RemainValue.bind(ToCount.subtract(CounterValue))

        DevState.set(2)
        RemainValue.addListener { observable, oldValue, newValue ->
            if (oldValue.toInt() > 0 && newValue.toInt() <= 0) {
                if (Working) {
                    Working = false
                    FinishedCallback()
                }
            }
        }
        updateSettings()
    }

    fun updateSettings() {
        dev.value?.apply {
            setParam(Device.SET_MOTORPOWER_MIN, MinPower.get())
            if (MaxPower.get() <= 4095) setParam(Device.SET_MOTORPOWER_MAX, MaxPower.get())
            setParam(Device.SET_MOTORPOWER_MIN_DELAYED, MinForcePower.get())
            setParam(Device.SET_COMP_VOLTAGE, CompVoltage.get())
        }
    }

    val isDevAssigned: Boolean
        get() = devUID.value != null

    val devUID = SimpleObjectProperty<ByteArray>()
    val dev = SimpleObjectProperty<Device>()
}