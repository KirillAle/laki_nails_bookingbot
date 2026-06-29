package kirillale.lakinais.booking

import kirillale.lakinais.booking.schedule.WeekSchedulePlan
import kirillale.lakinais.booking.schedule.WorkDayProfile
import java.time.LocalTime

/** @deprecated Используйте [WeekSchedulePlan]. Оставлено для обратной совместимости конфига. */
data class WorkDayDefaults(
    val workStart: LocalTime,
    val workEnd: LocalTime,
    val breakStart: LocalTime,
    val breakEnd: LocalTime,
    val defaultOpenHorizonDays: Int = 30,
) {
    fun toWeekPlan(): WeekSchedulePlan = WeekSchedulePlan(
        weekday = WorkDayProfile(
            workStart = workStart,
            workEnd = workEnd,
            breakInterval = kirillale.lakinais.booking.schedule.TimeRange(breakStart, breakEnd),
        ),
        weekend = WorkDayProfile(
            workStart = workStart,
            workEnd = workEnd,
            breakInterval = null,
        ),
        defaultHorizonDays = defaultOpenHorizonDays,
    )

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
