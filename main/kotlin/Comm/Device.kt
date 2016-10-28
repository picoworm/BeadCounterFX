/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package Comm

import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays

class Device (internal val CT: CommThread, val UID: ByteArray // 96 bit u-id of device
                                  , val NodeID: Int // Node ID after enumeration
) {

    var lastReceivedTS: Long = 0
        internal set

    fun received(CM: CANMessage) {
        lastReceivedTS = System.currentTimeMillis()
        val b = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        b.put(CM.Data)
        b.rewind()
        val Code = b.int
        val Data = b.int

        when (Code) {
            READ_COUNTER, READ_COUNTER_ONLYIFCHANGED -> Platform.runLater { PartCount.set(Data) }
            else -> println("Unhandled code " + String.format("0x%08X", Code) + "received: $Data")
        }
    }

    var PartCount = SimpleIntegerProperty(0)

    fun isUID(_UID: ByteArray) = Arrays.equals(UID, _UID)

    override fun toString() = "(${Arrays.toString(UID)}): NodeID=$NodeID"

    //internal var CounterValue: Int = 0

    fun SetCounterValue(i: Int) {
        setParam(SET_COUNTERVALUE, i)
    }

    fun setParam(Code: Int, Data: Int) {
        CT.addMessage(CANMessage(0x200 + NodeID, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(Code).putInt(Data).array()))
    }

    fun SetToCount(Count: Int) {
        setParam(SET_TOCOUNT, Count)
    }

    fun EnableMotor(En: Boolean) {
        setParam(SET_MOTOR_EN, if (En) 1 else 0)
    }

    fun SetBlinkLedsMode(BlinkEn: Boolean) {
        setParam(SET_BLINKLEDMODE_EN, if (BlinkEn) 1 else 0)
        println(BlinkEn)
    }

    companion object {
        val READ_COUNTER = 0x100
        val READ_TOCOUNT = 0x101
        val READ_COUNTER_ONLYIFCHANGED = 0x200
        val SET_COUNTERVALUE = 0x35
        val SET_TOCOUNT = 0x36
        val SET_MOTOR_EN = 0x37
        val SET_BLINKLEDMODE_EN = 0x40
        val SET_COMP_VOLTAGE = 0x31 // 0-4095
        val SET_MOTORPOWER_MAX = 0x32 // 0-4095
        val SET_MOTORPOWER_MIN = 0x33 // 0-4095
        val SET_MOTORPOWER_MIN_DELAYED = 0x34 // 0-4096
    }
}
