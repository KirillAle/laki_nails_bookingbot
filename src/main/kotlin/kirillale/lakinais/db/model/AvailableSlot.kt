package kirillale.lakinais.db.model

import java.time.Instant
import java.util.UUID

/**
 * Один доступный слот в рамках дня (вариант А: день = одна строка master_schedule).
 * @param scheduleId id записи master_schedule (день)
 * @param startTime начало слота
 * @param endTime конец слота
 * @param durationMinutes длина слота в минутах
 */
data class AvailableSlot(
    val scheduleId: UUID,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Int
)
