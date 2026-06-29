package kirillale.lakinais.booking.schedule

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Рабочий день: часы работы и опциональный перерыв. */
data class WorkDayProfile(
    val workStart: LocalTime,
    val workEnd: LocalTime,
    val breakInterval: TimeRange? = null,
) {
    init {
        require(workEnd > workStart) { "Конец работы должен быть позже начала" }
        breakInterval?.let { br ->
            require(br.end > br.start) { "Конец перерыва должен быть позже начала" }
            require(!br.start.isBefore(workStart) && !br.end.isAfter(workEnd)) {
                "Перерыв должен быть внутри рабочего времени"
            }
        }
    }

    fun withBreak(enabled: Boolean, defaultBreak: TimeRange? = null): WorkDayProfile = when {
        enabled && breakInterval == null -> copy(breakInterval = defaultBreak ?: DEFAULT_BREAK)
        !enabled -> copy(breakInterval = null)
        else -> this
    }

    fun formatShort(): String {
        val work = "${fmt(workStart)}–${fmt(workEnd)}"
        val br = breakInterval?.let { ", перерыв ${fmt(it.start)}–${fmt(it.end)}" } ?: ", без перерыва"
        return work + br
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("HH:mm")
        private val DEFAULT_BREAK = TimeRange(LocalTime.of(14, 0), LocalTime.of(15, 0))

        fun fmt(time: LocalTime): String = formatter.format(time)

        fun parse(
            workStart: String,
            workEnd: String,
            breakStart: String? = null,
            breakEnd: String? = null,
        ): WorkDayProfile {
            val breakInterval = if (breakStart != null && breakEnd != null) {
                TimeRange(LocalTime.parse(breakStart), LocalTime.parse(breakEnd))
            } else {
                null
            }
            return WorkDayProfile(
                workStart = LocalTime.parse(workStart),
                workEnd = LocalTime.parse(workEnd),
                breakInterval = breakInterval,
            )
        }
    }
}

data class TimeRange(val start: LocalTime, val end: LocalTime) {
    init {
        require(end > start) { "Конец интервала должен быть позже начала" }
    }
}
