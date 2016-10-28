/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package Comm

import com.fazecast.jSerialComm.SerialPort
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

class CommThread(val serialPort: SerialPort) {
    var DM: DevManager? = null
    private var running = true

    var LOGme = { S: String -> println(S) }

    internal var IDManagerReceived: (CANMessage) -> Unit = {}
    internal val CommCycleFinished = ArrayList<() -> Unit>()
    internal val toSend = LinkedList<CANMessage>()

    var CommFail = { s: String -> println(s)}
    var CommConnected = {}

    var _In: InputStream? = null
    var _Out: OutputStream? = null

    fun start() {
        DM = DevManager(this)
        thread {
            try {
                println("Thread started")
                serialPort.baudRate = 115200
                serialPort.parity = SerialPort.NO_PARITY
                serialPort.numDataBits = 8
                serialPort.numStopBits = 1
                serialPort.openPort()
                serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0)

                val In = serialPort.getInputStream()
                val Out = serialPort.getOutputStream()
                _In = In
                _Out = Out

                println("Sending request")
                Out.write(10)
                println("Waiting response")
                val BA = ByteArray(1, {0})
                serialPort.readBytes(BA, 1)
                if (BA[0] != 20.toByte()) throw Exception("Коммуникационный драйвер не обнаружен")
                println("Done")

                CommConnected()

                sendCANMessage(0x30, 0, null)

                val CM = CANMessage(0)

                var Action = 2
                while (running) {
                    while (!toSend.isEmpty()) {
                        sendCANMessage(toSend.removeFirst())
                        Action = 2
                    }

                    while (receiveCANMessage(CM)) {
                        Action = 2
                        println(CM.toString())
                        if (CM.ID == 0x10 || CM.ID == 0x11) IDManagerReceived(CM)
                        if (CM.ID >= 0x600 && CM.ID <= 0x1000) if (DM!!.Devices.size > CM.ID - 0x600) if (DM!!.Devices[CM.ID - 0x600] != null) DM!!.Devices[CM.ID - 0x600].received(CM)
                    }

                    Thread.sleep(50)

                    Action--
                    if (Action == 0) {
                        Action = 2
                        CommCycleFinished.forEach { it() }
                    }
                }
            } catch (E: Exception) {
                CommFail("Ошибка: " + E.message)
                E.printStackTrace()
            } finally {
                serialPort.closePort()
            }
        }
    }

    fun stop() {
        running = false
    }

    internal fun sendCANMessage(CM: CANMessage) {
        sendCANMessage(CM.ID, CM.DLC, CM.Data)
    }

    internal fun sendCANMessage(ID: Int, DLC: Int, Data: ByteArray?) {
        do {
            val b = _sendCANMessage(ID, DLC, Data)
            if (b == 20) return
            if (b != 40) throw Exception("Response on send message wrong!")
            Thread.sleep(100) // CAN OUT buffer full. Wait
        } while (true)
    }

    private fun _sendCANMessage(ID: Int, DLC: Int, Data: ByteArray?): Int {
        if (DLC != 0) if (Data?.size != 8) throw Exception("CAN sendmessage data parameter must have length 8")
        _Out?.run {
            write(11);write(ID); write(ID shr 8); write(DLC);
            if (DLC != 0) write(Data)
        }
        return _In!!.read()
    }

    internal fun receiveCANMessage(CM: CANMessage): Boolean {
        _Out?.write(12)
        val b = _In!!.read()
        if (b == 40) return false
        if (b == 20) {
            _In!!.run {
                CM.ID = read() or (read() shl 8)
                CM.DLC = read()
                if (CM.DLC > 0)
                    for (i in 0..7) CM.Data[i] = read().toByte()
            }
            return true
        }
        throw Exception("Unknown answer code")
    }

    internal fun isAllSent(CM: CANMessage): Boolean {
        _Out?.write(13)
        val b = _In!!.read()
        if (b == 40) return false
        if (b == 20) return true
        throw Exception("Unknown answer code")
    }

    fun addMessage(CM: CANMessage) {
        toSend.addLast(CM)
    }
}
