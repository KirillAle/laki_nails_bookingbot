package kirillale.lakinais.booking

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class DateInterval(
    val from: LocalDate,
    val to: LocalDate,
    val label: String,
)

data class AvailableDate(
    val scheduleId: java.util.UUID,
    val date: LocalDate,
    val label: String,
)

object DateIntervalBuilder {
    const val BOOKING_HORIZON_DAYS = 30L

    private val locale = Locale("ru")

    fun visibleIntervals(today: LocalDate): List<DateInterval> {
        val horizonEnd = today.plusDays(BOOKING_HORIZON_DAYS)
        val result = mutableListOf<DateInterval>()
        var month = YearMonth.from(today)
        val endMonth = YearMonth.from(horizonEnd)

        while (month <= endMonth) {
            result += chunksForMonth(month, today, horizonEnd)
            month = month.plusMonths(1)
        }
        return result.distinctBy { it.from to it.to }
    }

    private fun chunksForMonth(
        yearMonth: YearMonth,
        today: LocalDate,
        horizonEnd: LocalDate,
    ): List<DateInterval> {
        val lastDay = yearMonth.lengthOfMonth()
        val rawChunks = listOf(
            1 to 10,
            11 to 20,
            21 to lastDay,
        )
        return rawChunks.mapNotNull { (startDay, endDay) ->
            val from = yearMonth.atDay(startDay.coerceAtMost(lastDay))
            val to = yearMonth.atDay(endDay.coerceAtMost(lastDay))
            val effectiveFrom = maxOf(from, today)
            val effectiveTo = minOf(to, horizonEnd)
            if (effectiveFrom > effectiveTo) return@mapNotNull null
            DateInterval(
                from = effectiveFrom,
                to = effectiveTo,
                label = formatLabel(effectiveFrom, effectiveTo),
            )
        }
    }

    fun formatDayLabel(date: LocalDate): String {
        val month = date.month.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        return "${date.dayOfMonth} $month, $weekday"
    }

    private fun formatLabel(from: LocalDate, to: LocalDate): String {
        val month = from.month.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")
        return if (from == to) {
            "${from.dayOfMonth} $month"
        } else if (from.month == to.month) {
            "${from.dayOfMonth}–${to.dayOfMonth} $month"
        } else {
            val toMonth = to.month.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")
            "${from.dayOfMonth} $month – ${to.dayOfMonth} $toMonth"
        }
    }
}
