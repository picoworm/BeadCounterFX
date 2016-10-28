/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package Comm

import javafx.application.Platform.runLater
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class DevManager(val CT: CommThread) {
    init {
        CT.addMessage(CANMessage(0x24)) // broadcast clear all identification
        CT.addMessage(CANMessage(0x20)) // Broadcast send identification part 1
        CT.IDManagerReceived = { CM ->
            if (CM.ID === 0x10) {
                val buffer = ByteBuffer.allocate(java.lang.Long.BYTES).order(ByteOrder.LITTLE_ENDIAN)
                buffer.put(CM.Data)
                //buffer.flip();//need flip
                buffer.rewind()
                val ID1st = buffer.long
                if (!FirstPartIDs.contains(ID1st)) {
                    FirstPartIDs.add(ID1st)
                }
                println("Received 0x10: " + buffer)
            }
            if (CM.ID === 0x11) {
                val bytes = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong(Current1stID).put(CM.Data, 0, 4).array()

                var presence = false
                var NodeID2Set = -1
                for (Dev in Devices) {
                    if (Dev != null) {
                        if (Dev.isUID(bytes)) {
                            presence = true
                            NodeID2Set = Dev.NodeID
                            CT.LOGme("Device with UID: " + Arrays.toString(bytes) + " - Reconnected!")
                        }
                    }
                }


                if (!presence) {
                    LastNodeID++
                    NodeID2Set = LastNodeID
                    val Dev = Device(CT, bytes, LastNodeID)
                    if (Devices.size <= LastNodeID)
                        Devices.setSize(LastNodeID + 5)
                    Devices[LastNodeID] = Dev
                    runLater { NewDeviceDetectedEvent.forEach { it(Dev) } }
                }
                // Activate device and set NodeID
                CT.addMessage(CANMessage(0x22, Current1stID))
                CT.addMessage(CANMessage(0x23, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put(bytes, 8, 4).putInt(NodeID2Set).array()))
                println("Received 0x11 DevNum: " + NodeID2Set)
            }
        }

        CT.CommCycleFinished.add({ // send second ID request
            if (FirstPartIDs.size > 0) {
                Current1stID = FirstPartIDs[0]
                CT.addMessage(CANMessage(0x21, Current1stID))
                FirstPartIDs.removeAt(0)
                println("Send msg: " + Current1stID)
            }
            // Send broadcast request
            CT.addMessage(CANMessage(0x200, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(Device.READ_COUNTER_ONLYIFCHANGED).putInt(0).array()))
        })

        // TODO Check Devices Alive
        CT.CommCycleFinished.add({
            do {
                if (!AliveChecking) {
                    AliveIterator++
                    if (AliveIterator >= Devices.size) {
                        AliveIterator = -1
                        break
                    }
                    val D = Devices[AliveIterator] ?: break
                    if (System.currentTimeMillis() - Devices[AliveIterator].lastReceivedTS > 500) {
                        AliveChecking = true
                        LastReceivedTS = D.lastReceivedTS
                        AliveStartTS = System.currentTimeMillis()
                        D.setParam(Device.READ_COUNTER, 0)
                    }
                } else {
                    if (AliveIterator < 0 || AliveIterator >= Devices.size) {
                        AliveChecking = false
                        break
                    }

                    val D = Devices[AliveIterator]
                    if (D.lastReceivedTS > LastReceivedTS) {
                        // Device is online
                        AliveChecking = false
                        break
                    }

                    if (System.currentTimeMillis() > AliveStartTS + 500) {
                        // device is offline
                        Devices[AliveIterator] = null
                        runLater { CT.LOGme("Device disconnected: $D") }
                        DeviceDisconnectedEvent.forEach { runLater { it(D) } }
                        AliveChecking = false
                        break
                    }
                }
            } while (false)
        })
    }

    internal var AliveIterator = -1
    internal var AliveChecking = false
    internal var AliveStartTS: Long = 0
    internal var LastReceivedTS: Long = 0

    fun sendBroadcast(Code: Int, Data: Int) {
        CT.addMessage(CANMessage(0x200, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(Code).putInt(Data).array()))
    }

    val NewDeviceDetectedEvent = ArrayList<(Device)->Unit>()
    val DeviceDisconnectedEvent = ArrayList<(Device)->Unit>()

    internal var LastNodeID = 0

    internal val FirstPartIDs = Vector<Long>()

    internal val Devices = Vector<Device>()

    internal var Current1stID: Long = 0
}
