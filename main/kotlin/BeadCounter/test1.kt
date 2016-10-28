/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package BeadCounter

import Comm.CommThread
import GUI.CommunicationStatusDialog
import GUI.MainWindow
import GUI.SerialPortSelectionDialog
import GUI.tryPST
import com.fazecast.jSerialComm.SerialPort
import javafx.application.Application
import javafx.application.Platform
import javafx.application.Platform.runLater
import javafx.stage.Stage
import java.util.prefs.Preferences

class test1 : Application() {
    var _CT: CommThread? = null
    override fun start(stage: Stage) {
        tryPST {
            val sp = SerialPort.getCommPorts()

            val prefs = Preferences.userNodeForPackage(this.javaClass)
            var pname = prefs.get("SerialPort", "")

            val sport = sp.filter { it.systemPortName == pname }
            var Port: SerialPort? = null

            if (sport.size > 0) Port = sport[0]

            if (Port == null) {
                Port = SerialPortSelectionDialog(sp).SelectedPort
                if (Port == null) return@tryPST
                prefs.put("SerialPort", Port.systemPortName)
            }

            println(Port.systemPortName)

            val CSD = CommunicationStatusDialog({ Platform.exit(); System.exit(-1);})

            val CT = CommThread(Port)
            CT.CommFail = {
                runLater {
                    CSD.setLabel(it)
                    prefs.put("SerialPort", "")
                }
            }

            CT.CommConnected = {
                runLater {
                    println("connected OK")
                    val MW = MainWindow(CT)
                    CSD.close()
                    MW.show()
                    CT.LOGme = { MW.LOGme(it) }
                    CT.DM!!.NewDeviceDetectedEvent.add({ MW.newDeviceDetected(it) })
                    CT.DM!!.DeviceDisconnectedEvent.add({ MW.deviceDisconnectedEvent(it) })
                }
            }

            //CT.CommConnected()
            CT.start()

            _CT = CT
        }
    }

    override fun stop() {
        _CT?.stop()
    }

}
