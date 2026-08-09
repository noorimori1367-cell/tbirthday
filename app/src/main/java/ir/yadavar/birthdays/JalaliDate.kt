package ir.yadavar.birthdays

import java.util.Calendar

data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    companion object {
        /** تبدیل تاریخ میلادی به جلالی، بدون نیاز به کتابخانهٔ خارجی. */
        fun fromGregorian(calendar: Calendar): JalaliDate {
            val gy = calendar.get(Calendar.YEAR) - 1600
            val gm = calendar.get(Calendar.MONTH) + 1
            val gd = calendar.get(Calendar.DAY_OF_MONTH) - 1
            val gDaysInMonth = intArrayOf(31, if (isGregorianLeap(gy + 1600)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            var days = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
            for (i in 0 until gm - 1) days += gDaysInMonth[i]
            days += gd
            var jDays = days - 79
            val jNp = jDays / 12053
            jDays %= 12053
            var jy = 979 + 33 * jNp + 4 * (jDays / 1461)
            jDays %= 1461
            if (jDays >= 366) {
                jy += (jDays - 1) / 365
                jDays = (jDays - 1) % 365
            }
            val jm: Int
            val jd: Int
            if (jDays < 186) { jm = 1 + jDays / 31; jd = 1 + jDays % 31 }
            else { jm = 7 + (jDays - 186) / 30; jd = 1 + (jDays - 186) % 30 }
            return JalaliDate(jy, jm, jd)
        }

        private fun isGregorianLeap(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}

val persianMonths = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
fun JalaliDate.display() = "$day ${persianMonths[month - 1]} $year"
