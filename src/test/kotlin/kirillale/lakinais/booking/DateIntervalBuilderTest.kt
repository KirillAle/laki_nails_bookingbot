package kirillale.lakinais.booking

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateIntervalBuilderTest {

    @Test
    fun visibleIntervalsStartFromTodayNotCalendarFirstDecade() {
        val today = LocalDate.of(2026, 6, 20)
        val intervals = DateIntervalBuilder.visibleIntervals(today)
        assertTrue(intervals.isNotEmpty())
        assertEquals(today, intervals.first().from)
        assertEquals("20–29 июн", intervals.first().label)
        assertTrue(intervals.none { it.from.dayOfMonth == 1 && it.from.month == today.month })
    }

    @Test
    fun intervalsFromAvailableDatesSkipClosedDayAtStart() {
        val dates = listOf(
            LocalDate.of(2026, 6, 30),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 2),
            LocalDate.of(2026, 7, 3),
            LocalDate.of(2026, 7, 4),
            LocalDate.of(2026, 7, 5),
        )
        val intervals = DateIntervalBuilder.intervalsFromAvailableDates(dates)
        assertEquals(1, intervals.size)
        assertEquals(LocalDate.of(2026, 6, 30), intervals.first().from)
        assertEquals(LocalDate.of(2026, 7, 5), intervals.first().to)
        assertEquals("30 июн – 5 июл", intervals.first().label)
    }

    @Test
    fun intervalsFromAvailableDatesSplitOnGap() {
        val dates = listOf(
            LocalDate.of(2026, 6, 30),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 5),
        )
        val intervals = DateIntervalBuilder.intervalsFromAvailableDates(dates)
        assertEquals(2, intervals.size)
        assertEquals("30 июн – 1 июл", intervals[0].label)
        assertEquals("5 июл", intervals[1].label)
    }
}
