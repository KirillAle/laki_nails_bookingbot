package kirillale.lakinais.booking.schedule

import java.time.LocalTime

object TimePickerOptions {
    fun quarterHours(from: LocalTime = LocalTime.of(8, 0), to: LocalTime = LocalTime.of(21, 0)): List<LocalTime> {
        val times = mutableListOf<LocalTime>()
        var t = from
        while (!t.isAfter(to)) {
            times += t
            t = t.plusMinutes(15)
        }
        return times
    }
}
