/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author :TODO: Шемякин Никита
 */
class Solution : MonotonicClock {
    private var c1 by RegularInt(0)
    private var c2 by RegularInt(0)
    private var c3 by RegularInt(0)
    private var c4 by RegularInt(0)
    private var c5 by RegularInt(0)
    private var c6 by RegularInt(0)

    override fun write(time: Time) {
        // write right-to-left
        c4 = time.d1
        c5 = time.d2
        c6 = time.d3
        c3 = time.d3
        c2 = time.d2
        c1 = time.d1
    }

    override fun read(): Time {
        // read left-to-right
        //return Time(c1, c2, c3)
        val r1 = c1
        val r2 = c2
        val r3 = c3
        val r6 = c6
        val r5 = c5
        val r4 = c4
        if (r1 == r4 && r2 == r5 && r3 == r6) {
            return Time(r1, r2, r3);
        }
        var p:Int = 0
        if (r1 == r4) {
            p++;
            if (r2 == r5) {
                p++;
            }
        }
        if (p == 0) {
            return Time(r4, 0, 0);
        }
        if (p == 1) {
            return Time(r4, r5, 0);
        }
        return Time(r4, r5, r6);
    }
}