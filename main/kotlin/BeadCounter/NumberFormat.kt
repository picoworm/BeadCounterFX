/*
* Roman Stratiienko 2015
* picoworm@gmail.com
*/

package BeadCounter

class NumberFormat {
    constructor() {
    }

    constructor(Min: Double, Max: Double, BP: Int, AP: Int) {
        MinValue = Min
        MaxValue = Max
        DigitsAP = AP
        DigitsBP = BP
    }

    fun canParse(Number: String): Boolean {
        var BeforePoint = 0
        var AfterPoint = 0
        var Point = false
        var Minus = false
        for (i in 0..Number.length - 1) {
            val c = Number[i]
			when {
                Character.isDigit(c) -> when {
                    Point -> AfterPoint++
                    else -> BeforePoint++
                }
                c == '.' -> when {
                    Point -> return false
                    else -> Point = true
                }
                i == 0 && c == '-' -> Minus = true
                else -> return false
            }
        }
        if (BeforePoint > DigitsBP || AfterPoint > DigitsAP) return false
        if (BeforePoint == 0 && AfterPoint == 0) return false
        val Value = java.lang.Double.parseDouble(Number)
        if (Value < MinValue) return false
        if (Value > MaxValue) return false
        return true
    }

    var MinValue = java.lang.Double.NEGATIVE_INFINITY
    var MaxValue = java.lang.Double.POSITIVE_INFINITY
    var DigitsBP = 10
    var DigitsAP = 10
    var Unit = ""
    var Percentage = false // value is multiplied by 100 before indication, and divided by 100 before storage

    companion object {
        var DefaultNumberFormat = NumberFormat()
        var BeadCountNumberFormat = NumberFormat(0.0, 100000000.0, 8, 0)
    }
}
