package kirillale.lakinais.booking

import java.time.LocalTime

data class WorkDayDefaults(
    val workStart: LocalTime,
    val workEnd: LocalTime,
    val breakStart: LocalTime,
    val breakEnd: LocalTime,
    val defaultOpenHorizonDays: Int = 30,
) {
    companion object {
        fun parse(
            workStart: String = "10:00",
            workEnd: String = "19:00",
            breakStart: String = "14:00",
            breakEnd: String = "15:00",
            horizonDays: Int = 30,
        ): WorkDayDefaults = WorkDayDefaults(
            workStart = LocalTime.parse(workStart),
            workEnd = LocalTime.parse(workEnd),
            breakStart = LocalTime.parse(breakStart),
            breakEnd = LocalTime.parse(breakEnd),
            defaultOpenHorizonDays = horizonDays,
        )
    }
}
