package kirillale.lakinais.bot

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import kirillale.lakinais.db.repositiries.AccountRepository
import kirillale.lakinais.db.repositiries.BookingReminderKind
import kirillale.lakinais.db.repositiries.BookingReminderRepository
import kirillale.lakinais.db.repositiries.BookingRepository
import kirillale.lakinais.db.service.BookingQueryService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class BookingReminderService(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val bookingQueryService: BookingQueryService = BookingQueryService(),
    private val accountRepository: AccountRepository = AccountRepository(),
    private val reminderRepository: BookingReminderRepository = BookingReminderRepository(),
    private val zoneId: ZoneId,
    private val tickWindow: Duration = Duration.ofMinutes(15),
) {
    private val log = LoggerFactory.getLogger(BookingReminderService::class.java)

    private data class ReminderRule(
        val kind: BookingReminderKind,
        val lead: Duration,
        val message: (dateLabel: String, timeLabel: String, procedureLabel: String) -> String,
    )

    private val rules = listOf(
        ReminderRule(BookingReminderKind.DAYS_3, Duration.ofDays(3)) { date, time, procedure ->
            "🔔 Напоминание: через 3 дня у вас запись.\n$date, $time\n$procedure"
        },
        ReminderRule(BookingReminderKind.DAY_1, Duration.ofDays(1)) { date, time, procedure ->
            "🔔 Напоминание: завтра у вас запись.\n$date, $time\n$procedure"
        },
        ReminderRule(BookingReminderKind.HOURS_2, Duration.ofHours(2)) { date, time, procedure ->
            "🔔 Напоминание: через пару часов запись.\n$date, $time\n$procedure"
        },
    )

    fun ensureStorage() {
        reminderRepository.ensureTable()
    }

    suspend fun sendDueReminders(bot: TelegramBot) {
        val now = Instant.now()
        val active = bookingRepository.findActive()
        for (booking in active) {
            val view = bookingQueryService.getView(booking.id, zoneId) ?: continue
            if (!view.startTime.isAfter(now)) continue

            val client = accountRepository.findById(booking.clientId) ?: continue
            val telegramId = client.telegramId?.toLongOrNull() ?: continue

            for (rule in rules) {
                if (reminderRepository.wasSent(booking.id, rule.kind)) continue
                if (!isDue(now, view.startTime, rule.lead)) continue
                try {
                    bot.sendMessage(
                        ChatId(RawChatId(telegramId)),
                        rule.message(view.dateLabel, view.timeLabel, view.procedureLabel),
                    )
                    reminderRepository.markSent(booking.id, rule.kind)
                    log.info("Reminder {} sent for booking {}", rule.kind, booking.id)
                } catch (e: Exception) {
                    log.warn("Failed reminder {} for booking {}: {}", rule.kind, booking.id, e.message)
                }
            }
        }
    }

    /** Напоминание в окне [lead, lead + tickWindow] до начала записи. */
    private fun isDue(now: Instant, startsAt: Instant, lead: Duration): Boolean {
        val remindAt = startsAt.minus(lead)
        val windowEnd = remindAt.plus(tickWindow)
        return !now.isBefore(remindAt) && now.isBefore(windowEnd)
    }
}
