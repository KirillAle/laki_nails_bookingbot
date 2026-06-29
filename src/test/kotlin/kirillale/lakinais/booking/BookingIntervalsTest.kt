package kirillale.lakinais.booking

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookingIntervalsTest {

    private fun t(hour: Int, minute: Int = 0): Instant =
        Instant.parse("2026-06-25T${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:00Z")

    @Test
    fun directOverlapIsConflict() {
        assertTrue(
            BookingIntervals.conflicts(
                candidateStart = t(12, 15),
                candidateEnd = t(13, 15),
                occupiedStart = t(12, 0),
                occupiedEnd = t(13, 30),
            ),
        )
    }

    @Test
    fun startTooSoonAfterOccupiedEndIsConflict() {
        assertTrue(
            BookingIntervals.conflicts(
                candidateStart = t(13, 30),
                candidateEnd = t(14, 30),
                occupiedStart = t(12, 0),
                occupiedEnd = t(13, 30),
            ),
        )
    }

    @Test
    fun startAfterBreakTailIsAllowed() {
        assertFalse(
            BookingIntervals.conflicts(
                candidateStart = t(13, 45),
                candidateEnd = t(14, 45),
                occupiedStart = t(12, 0),
                occupiedEnd = t(13, 30),
            ),
        )
    }

    @Test
    fun candidateEndingWithExactBreakGapBeforeOccupiedIsAllowed() {
        assertFalse(
            BookingIntervals.conflicts(
                candidateStart = t(11, 0),
                candidateEnd = t(11, 45),
                occupiedStart = t(12, 0),
                occupiedEnd = t(13, 30),
            ),
        )
    }

    @Test
    fun candidateEndingAtOccupiedStartIsConflictDueToBreakTail() {
        assertTrue(
            BookingIntervals.conflicts(
                candidateStart = t(11, 0),
                candidateEnd = t(12, 0),
                occupiedStart = t(12, 0),
                occupiedEnd = t(13, 30),
            ),
        )
    }

    @Test
    fun candidateBreakTailBeforeOccupiedStartIsConflict() {
        assertTrue(
            BookingIntervals.conflicts(
                candidateStart = t(11, 0),
                candidateEnd = t(11, 46),
                occupiedStart = t(12, 0),
                occupiedEnd = t(13, 30),
            ),
        )
    }

    @Test
    fun candidateEndingExactlyAtBreakBoundaryBeforeOccupiedIsAllowed() {
        assertFalse(
            BookingIntervals.conflicts(
                candidateStart = t(11, 0),
                candidateEnd = t(11, 45),
                occupiedStart = t(12, 0),
                occupiedEnd = t(13, 30),
            ),
        )
    }

    @Test
    fun zeroBreakAllowsBackToBackInSameVisit() {
        assertFalse(
            BookingIntervals.conflicts(
                candidateStart = t(13, 0),
                candidateEnd = t(14, 0),
                occupiedStart = t(12, 0),
                occupiedEnd = t(13, 0),
                breakSlots = 0,
            ),
        )
    }

    @Test
    fun fitsBetweenBookingsRejectsTooLongProcedureInGap() {
        val occupied = listOf(t(10, 0) to t(11, 0), t(13, 0) to t(14, 0))
        assertFalse(
            BookingIntervals.fitsBetweenBookings(
                candidateStart = t(11, 15),
                durationSlots = 8,
                occupiedRanges = occupied,
            ),
        )
    }

    @Test
    fun fitsBetweenBookingsAllowsShortProcedureInGap() {
        val occupied = listOf(t(10, 0) to t(11, 0), t(13, 0) to t(14, 0))
        assertTrue(
            BookingIntervals.fitsBetweenBookings(
                candidateStart = t(11, 15),
                durationSlots = 4,
                occupiedRanges = occupied,
            ),
        )
    }
}
