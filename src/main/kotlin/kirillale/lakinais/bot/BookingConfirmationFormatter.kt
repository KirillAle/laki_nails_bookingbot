package kirillale.lakinais.bot

import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.db.model.AvailableSlot
import kirillale.lakinais.db.service.BookingAvailabilityService
import kirillale.lakinais.db.service.BookingPlanItem
import java.math.BigDecimal
import java.time.ZoneId

object BookingConfirmationFormatter {
    private const val SLOT_MINUTES = 15

    fun formatConfirmation(
        plan: List<BookingPlanItem>,
        phone: String,
        availabilityService: BookingAvailabilityService,
        zoneId: ZoneId,
    ): String {
        val dateLabel = DateIntervalBuilder.formatDayLabel(
            plan.first().startTime.atZone(zoneId).toLocalDate(),
        )
        val proceduresBlock = plan.joinToString("\n") { item ->
            val time = availabilityService.formatSlotTime(toSlot(item), zoneId)
            "• ${item.option.buttonText} — ${formatPrice(item.price)}\n  🕐 $time"
        }
        val total = plan.fold(BigDecimal.ZERO) { acc, item -> acc + item.price }
        val totalLine = if (total > BigDecimal.ZERO) {
            "\n💰 Итого: ${formatPrice(total)}"
        } else {
            "\n💰 Стоимость уточняется у мастера"
        }

        return buildString {
            appendLine("📋 Подтверждение записи")
            appendLine()
            appendLine("Процедуры:")
            appendLine(proceduresBlock)
            appendLine()
            appendLine("📅 $dateLabel")
            appendLine("📞 $phone")
            append(totalLine)
        }
    }

    fun formatSuccess(
        plan: List<BookingPlanItem>,
        phone: String,
        availabilityService: BookingAvailabilityService,
        zoneId: ZoneId,
    ): String {
        val dateLabel = DateIntervalBuilder.formatDayLabel(
            plan.first().startTime.atZone(zoneId).toLocalDate(),
        )
        val procedures = plan.joinToString("\n") { item ->
            val time = availabilityService.formatSlotTime(toSlot(item), zoneId)
            "✅ ${item.option.buttonText}\n   📅 $dateLabel, 🕐 $time"
        }
        return buildString {
            appendLine("🎉 Запись создана!")
            appendLine()
            append(procedures)
            appendLine()
            appendLine("📞 $phone")
            appendLine()
            appendLine("Статус: ожидает подтверждения мастера.")
        }
    }

    private fun toSlot(item: BookingPlanItem): AvailableSlot {
        val durationMinutes = item.option.durationSlots * SLOT_MINUTES
        return AvailableSlot(
            scheduleId = item.scheduleId,
            startTime = item.startTime,
            endTime = item.startTime.plusSeconds(durationMinutes * 60L),
            durationMinutes = durationMinutes,
        )
    }

    private fun formatPrice(price: BigDecimal): String =
        if (price > BigDecimal.ZERO) "${price.toInt()} ₾" else "уточняется"
}
