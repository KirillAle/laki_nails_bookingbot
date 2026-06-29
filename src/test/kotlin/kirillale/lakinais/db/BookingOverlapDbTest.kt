package kirillale.lakinais.db

import kirillale.lakinais.booking.DateInterval
import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.db.service.BookingAvailabilityService
import kirillale.lakinais.db.service.BookingService
import kirillale.lakinais.db.service.ScheduleManagementService
import kirillale.lakinais.db.service.SlotAvailabilityService
import kirillale.lakinais.db.support.BookingTestFixture
import org.junit.After
import org.junit.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class BookingOverlapDbTest {

    private var fixture: BookingTestFixture? = null

    @After
    fun tearDown() {
        fixture?.cleanup()
        fixture = null
    }

    @Test
    fun rejectsOverlappingStartTime() {
        val fx = newFixture()
        fx.createBooking(LocalTime.of(12, 0), fx.procedureSixSlots)
        assertFalse(fx.isAvailable(LocalTime.of(12, 15), durationSlots = 4))
        try {
            fx.createBooking(LocalTime.of(12, 15), fx.procedureFourSlots)
            fail("Expected overlap rejection")
        } catch (_: IllegalStateException) {
            // expected
        }
    }

    @Test
    fun requiresBreakAfterBookingEnd() {
        val fx = newFixture()
        fx.createBooking(LocalTime.of(12, 0), fx.procedureSixSlots)
        assertFalse(fx.isAvailable(LocalTime.of(13, 30), durationSlots = 4))
        assertTrue(fx.isAvailable(LocalTime.of(13, 45), durationSlots = 4))
    }

    @Test
    fun slotServiceDoesNotOfferInvalidSlots() {
        val fx = newFixture()
        fx.createBooking(LocalTime.of(12, 0), fx.procedureSixSlots)
        val slots = SlotAvailabilityService().getAvailableSlotsForDay(
            schedule = fx.schedule,
            slotDurationSlots = 4,
            zoneId = fx.zoneId,
        )
        val slotTimes = slots.map { it.startTime.atZone(fx.zoneId).toLocalTime() }
        assertFalse(slotTimes.contains(LocalTime.of(12, 15)))
        assertFalse(slotTimes.contains(LocalTime.of(13, 30)))
        assertTrue(slotTimes.contains(LocalTime.of(13, 45)))
    }

    @Test
    fun shortProcedureFitsInGapBetweenBookings() {
        val fx = newFixture()
        fx.createBooking(LocalTime.of(10, 0), fx.procedureFourSlots)
        fx.createBooking(LocalTime.of(14, 0), fx.procedureFourSlots)
        assertTrue(fx.isAvailable(LocalTime.of(11, 15), durationSlots = 4))
        assertFalse(fx.isAvailable(LocalTime.of(11, 15), durationSlots = 11))
    }

    @Test
    fun cancelledBookingDoesNotBlockSlot() {
        val fx = newFixture()
        val booking = fx.createBooking(LocalTime.of(12, 0), fx.procedureSixSlots)
        BookingService().cancelBooking(booking.id)
        assertTrue(fx.isAvailable(LocalTime.of(12, 15), durationSlots = 4))
    }

    @Test
    fun closedDayIsHiddenFromClients() {
        val fx = newFixture()
        val booking = fx.createBooking(LocalTime.of(12, 0), fx.procedureSixSlots)
        BookingService().cancelBooking(booking.id)
        ScheduleManagementService().closeDay(fx.masterId, fx.schedule.id, fx.zoneId).getOrThrow()

        val availability = BookingAvailabilityService()
        val today = fx.date
        val interval = DateInterval(today, today.plusDays(10), "")
        val procedure = kirillale.lakinais.bot.BotProcedureCatalog.all().first()
        val dates = availability.getAvailableDatesInInterval(
            masterId = fx.masterId,
            interval = interval,
            procedures = listOf(procedure),
            mode = kirillale.lakinais.booking.BookingSlotMode.CONSECUTIVE,
            splitPriority = null,
            zoneId = fx.zoneId,
        )
        assertTrue(dates.none { it.scheduleId == fx.schedule.id })

        val slots = availability.getAvailableTimeSlots(
            scheduleId = fx.schedule.id,
            procedures = listOf(procedure),
            mode = kirillale.lakinais.booking.BookingSlotMode.CONSECUTIVE,
            splitPriority = null,
            zoneId = fx.zoneId,
        )
        assertTrue(slots.isEmpty())
    }

    @Test
    fun cannotCloseDayWithActiveBookings() {
        val fx = newFixture()
        fx.createBooking(LocalTime.of(12, 0), fx.procedureSixSlots)
        val result = ScheduleManagementService().closeDay(fx.masterId, fx.schedule.id, fx.zoneId)
        assertTrue(result.isFailure)
    }

    @Test
    fun dayWithoutLunchBreakUsesFullWorkday() {
        val fx = newFixture()
        val slots = SlotAvailabilityService().getAvailableSlotsForDay(
            schedule = fx.schedule,
            slotDurationSlots = 4,
            zoneId = fx.zoneId,
        )
        val first = slots.first().startTime.atZone(fx.zoneId).toLocalTime()
        val lastEnd = slots.last().endTime.atZone(fx.zoneId).toLocalTime()
        assertEquals(LocalTime.of(10, 0), first)
        assertEquals(LocalTime.of(19, 0), lastEnd)
    }

    private fun newFixture(): BookingTestFixture {
        val fx = BookingTestFixture()
        fixture = fx
        return fx
    }
}
