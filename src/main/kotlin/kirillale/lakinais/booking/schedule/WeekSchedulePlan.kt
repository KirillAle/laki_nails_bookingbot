package kirillale.lakinais.booking.schedule

import java.time.DayOfWeek
import java.time.LocalDate

/** План графика: отдельные профили для будней и выходных. */
data class WeekSchedulePlan(
    val weekday: WorkDayProfile,
    val weekend: WorkDayProfile,
    val defaultHorizonDays: Int = 30,
) {
    fun profileFor(date: LocalDate): WorkDayProfile =
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) weekend else weekday

    fun isWeekend(date: LocalDate): Boolean =
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

    fun formatSummary(): String = buildString {
        appendLine("Будни: ${weekday.formatShort()}")
        append("Выходные: ${weekend.formatShort()}")
    }

    fun withWeekday(profile: WorkDayProfile): WeekSchedulePlan = copy(weekday = profile)

    fun withWeekend(profile: WorkDayProfile): WeekSchedulePlan = copy(weekend = profile)

    companion object {
        fun fromConfig(
            weekdayWorkStart: String = "10:00",
            weekdayWorkEnd: String = "19:00",
            weekdayBreakStart: String = "14:00",
            weekdayBreakEnd: String = "15:00",
            weekendWorkStart: String? = null,
            weekendWorkEnd: String? = null,
            weekendBreakStart: String? = null,
            weekendBreakEnd: String? = null,
            horizonDays: Int = 30,
        ): WeekSchedulePlan = WeekSchedulePlan(
            weekday = WorkDayProfile.parse(
                weekdayWorkStart, weekdayWorkEnd, weekdayBreakStart, weekdayBreakEnd,
            ),
            weekend = WorkDayProfile.parse(
                workStart = weekendWorkStart ?: weekdayWorkStart,
                workEnd = weekendWorkEnd ?: weekdayWorkEnd,
                breakStart = weekendBreakStart,
                breakEnd = weekendBreakEnd,
            ),
            defaultHorizonDays = horizonDays,
        )
    }
}

enum class DayKindFilter {
    ALL,
    WEEKDAYS,
    WEEKENDS,
    ;

    fun matches(date: LocalDate): Boolean = when (this) {
        ALL -> true
        WEEKDAYS -> date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
        WEEKENDS -> date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }
}
