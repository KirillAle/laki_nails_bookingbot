package kirillale.lakinais.booking.schedule

import kirillale.lakinais.booking.SalonTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleMapperTest {

    private val zoneId = ZoneId.of("Asia/Tbilisi")
    private val date = LocalDate.of(2026, 6, 25)

    @Test
    fun hasBreakWhenIntervalIsNonZero() {
        val start = SalonTime.atTime(date, LocalTime.of(14, 0), zoneId)
        val end = SalonTime.atTime(date, LocalTime.of(15, 0), zoneId)
        assertTrue(ScheduleMapper.hasBreak(start, end, zoneId))
    }

    @Test
    fun noBreakWhenStartEqualsEnd() {
        val instant = SalonTime.atTime(date, LocalTime.of(14, 0), zoneId)
        assertFalse(ScheduleMapper.hasBreak(instant, instant, zoneId))
    }

    @Test
    fun workDayProfileWithoutBreak() {
        val profile = WorkDayProfile(
            workStart = LocalTime.of(10, 0),
            workEnd = LocalTime.of(19, 0),
            breakInterval = null,
        )
        assertTrue(profile.breakInterval == null)
    }

    @Test
    fun workDayProfileToggleBreak() {
        val without = WorkDayProfile(LocalTime.of(10, 0), LocalTime.of(19, 0), breakInterval = null)
        val withBreak = without.withBreak(enabled = true)
        assertTrue(withBreak.breakInterval != null)
        val removed = withBreak.withBreak(enabled = false)
        assertTrue(removed.breakInterval == null)
    }
}
