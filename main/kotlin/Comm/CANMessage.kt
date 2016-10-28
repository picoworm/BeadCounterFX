/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package Comm

class CANMessage {
    var ID: Int = 0
    var DLC: Int = 0
    var Data = ByteArray(8)

    constructor(_ID: Int) {
        ID = _ID
        DLC = 0
    }

    constructor(_ID: Int, _Data: ByteArray) {
        ID = _ID
        DLC = _Data.size
        for (i in 0..DLC - 1) Data[i] = _Data[i]
    }

    constructor(_ID: Int, _Data: Long) : this(_ID, longToBytes(_Data))

    override fun toString(): String {
        var S = "($ID) - ["
        for (i in 0..DLC - 1) {
            if (i > 0) S += ", "
            S += Data[i]
        }
        S += "]"
        return S
    }

    companion object {
        fun longToBytes(l: Long): ByteArray {
            var l = l
            val result = ByteArray(8)
            for (i in 0..7) {
                result[i] = (l and 0xFF).toByte()
                l = l shr 8
            }
            return result
        }

        fun bytesToLong(b: ByteArray): Long {
            var result: Long = 0
            for (i in 0..7) {
                result = result shl 8
                result = result or (b[i].toInt() and 0xFF).toLong()
            }
            return result
        }
    }
}
