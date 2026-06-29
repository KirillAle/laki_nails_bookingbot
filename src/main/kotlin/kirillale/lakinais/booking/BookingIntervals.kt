package kirillale.lakinais.booking

import java.time.Instant

object BookingIntervals {
    const val SLOT_MINUTES = 15
    const val BREAK_SLOTS_BETWEEN_BOOKINGS = 1

    fun slotEnd(startTime: Instant, durationSlots: Int): Instant =
        startTime.plusSeconds(durationSlots.toLong() * SLOT_MINUTES * 60L)

    fun breakSeconds(breakSlots: Int): Long =
        breakSlots.toLong() * SLOT_MINUTES * 60L

    /**
     * Конфликт кандидата с уже занятым интервалом.
     * Перерыв между отдельными записями — только **после** конца занятого интервала.
     * Для соседних процедур в одном визите передайте [breakSlots] = 0.
     */
    fun conflicts(
        candidateStart: Instant,
        candidateEnd: Instant,
        occupiedStart: Instant,
        occupiedEnd: Instant,
        breakSlots: Int = BREAK_SLOTS_BETWEEN_BOOKINGS,
    ): Boolean {
        if (breakSlots == 0) {
            return candidateStart < occupiedEnd && candidateEnd > occupiedStart
        }
        val breakSec = breakSeconds(breakSlots)
        val occupiedTailEnd = occupiedEnd.plusSeconds(breakSec)
        if (candidateStart < occupiedTailEnd && candidateEnd > occupiedStart) {
            return true
        }
        if (candidateEnd <= occupiedStart && candidateEnd.plusSeconds(breakSec) > occupiedStart) {
            return true
        }
        return false
    }

    fun conflictsAny(
        candidateStart: Instant,
        candidateEnd: Instant,
        occupiedRanges: Iterable<Pair<Instant, Instant>>,
        breakSlots: Int = BREAK_SLOTS_BETWEEN_BOOKINGS,
    ): Boolean = occupiedRanges.any { (start, end) ->
        conflicts(candidateStart, candidateEnd, start, end, breakSlots)
    }

    /** Умещается ли процедура в интервал с учётом перерыва после неё перед следующей записью. */
    fun fitsBetweenBookings(
        candidateStart: Instant,
        durationSlots: Int,
        occupiedRanges: Iterable<Pair<Instant, Instant>>,
        breakSlots: Int = BREAK_SLOTS_BETWEEN_BOOKINGS,
    ): Boolean {
        val candidateEnd = slotEnd(candidateStart, durationSlots)
        return !conflictsAny(candidateStart, candidateEnd, occupiedRanges, breakSlots)
    }
}
