package kirillale.lakinais.booking

import java.time.LocalDate
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
    const val INTERVAL_CHUNK_DAYS = 10L

    private val locale = Locale("ru")

    /** Интервалы по 10 дней от сегодня, а не календарные 1–10 / 11–20 месяца. */
    fun visibleIntervals(today: LocalDate): List<DateInterval> {
        val horizonEnd = today.plusDays(BOOKING_HORIZON_DAYS)
        return buildChunkedIntervals(today, horizonEnd)
    }

    /**
     * Интервалы только по датам, где есть запись: без закрытых/пустых дней в подписи.
     * Разрыв в 1+ день → новый интервал; длинная цепочка режется по [INTERVAL_CHUNK_DAYS].
     */
    fun intervalsFromAvailableDates(dates: List<LocalDate>): List<DateInterval> {
        if (dates.isEmpty()) return emptyList()
        val sorted = dates.distinct().sorted()
        val consecutiveGroups = mutableListOf<List<LocalDate>>()
        var group = mutableListOf(sorted.first())
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1].plusDays(1)) {
                group.add(sorted[i])
            } else {
                consecutiveGroups.add(group)
                group = mutableListOf(sorted[i])
            }
        }
        consecutiveGroups.add(group)
        return consecutiveGroups.flatMap { chunkConsecutiveRun(it) }
    }

    fun formatLabel(from: LocalDate, to: LocalDate): String {
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

    private fun buildChunkedIntervals(from: LocalDate, to: LocalDate): List<DateInterval> {
        val result = mutableListOf<DateInterval>()
        var chunkStart = from
        while (!chunkStart.isAfter(to)) {
            val chunkEnd = minOf(chunkStart.plusDays(INTERVAL_CHUNK_DAYS - 1), to)
            result += DateInterval(
                from = chunkStart,
                to = chunkEnd,
                label = formatLabel(chunkStart, chunkEnd),
            )
            chunkStart = chunkEnd.plusDays(1)
        }
        return result
    }

    private fun chunkConsecutiveRun(days: List<LocalDate>): List<DateInterval> {
        if (days.isEmpty()) return emptyList()
        val result = mutableListOf<DateInterval>()
        var index = 0
        while (index < days.size) {
            val endIndex = minOf(index + INTERVAL_CHUNK_DAYS.toInt() - 1, days.size - 1)
            val from = days[index]
            val to = days[endIndex]
            result += DateInterval(from, to, formatLabel(from, to))
            index = endIndex + 1
        }
        return result
    }

    fun formatDayLabel(date: LocalDate): String {
        val month = date.month.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        return "${date.dayOfMonth} $month, $weekday"
    }
}
