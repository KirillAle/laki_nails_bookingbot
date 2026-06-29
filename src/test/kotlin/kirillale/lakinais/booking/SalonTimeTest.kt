package kirillale.lakinais.booking

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class SalonTimeTest {

    private val zoneId = ZoneId.of("Asia/Tbilisi")

    @Test
    fun bookingStartOnDayCombinesScheduleDateAndStoredTime() {
        val day = LocalDate.of(2099, 6, 25)
        val scheduleDay = SalonTime.dayInstant(day, zoneId)
        val storedTimeOnly = SalonTime.atTime(LocalDate.of(1970, 1, 1), LocalTime.of(12, 0), zoneId)
        val resolved = SalonTime.bookingStartOnDay(scheduleDay, storedTimeOnly, zoneId)
        assertEquals(SalonTime.atTime(day, LocalTime.of(12, 0), zoneId), resolved)
    }
}
