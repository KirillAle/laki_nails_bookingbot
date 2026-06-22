package kirillale.lakinais.bot.master

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.IdChatIdentifier
import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.booking.SalonTime
import kirillale.lakinais.booking.WorkDayDefaults
import kirillale.lakinais.bot.BookingFlowState
import kirillale.lakinais.bot.BotKeyboards
import kirillale.lakinais.bot.BotUi
import kirillale.lakinais.db.entities.AccountFormEntity
import kirillale.lakinais.db.service.BookingManagementService
import kirillale.lakinais.db.service.BookingQueryService
import kirillale.lakinais.db.service.BookingView
import kirillale.lakinais.db.service.MasterResolver
import kirillale.lakinais.db.service.ScheduleManagementService
import kirillale.lakinais.domain.role.PermissionService
import kirillale.lakinais.domain.role.StaffPermission
import kirillale.lakinais.domain.role.UserRole
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class MasterBotNavigator(
    private val bot: TelegramBot,
    private val permissionService: PermissionService,
    private val masterResolver: MasterResolver,
    private val scheduleManagementService: ScheduleManagementService,
    private val bookingQueryService: BookingQueryService,
    private val bookingManagementService: BookingManagementService,
    private val workDayDefaults: WorkDayDefaults,
    private val zoneId: ZoneId,
) {
    fun isStaff(account: AccountFormEntity): Boolean = permissionService.isStaff(account)

    private fun requirePermission(account: AccountFormEntity, permission: StaffPermission): Boolean {
        if (!permissionService.hasPermission(account, permission)) {
            return false
        }
        return true
    }

    suspend fun sendMainMenu(chatId: IdChatIdentifier, account: AccountFormEntity) {
        if (!isStaff(account)) {
            bot.sendMessage(chatId, "Меню мастера доступно только сотрудникам.")
            return
        }
        bot.sendMessage(
            chatId,
            "🛠 Меню мастера\n\nВыберите действие:",
            replyMarkup = MasterKeyboards.mainMenu(),
        )
    }

    suspend fun sendOpenPeriodMenu(chatId: IdChatIdentifier, chatIdKey: String) {
        val days = MasterFlowState.horizonDays(chatIdKey, workDayDefaults.defaultOpenHorizonDays)
        bot.sendMessage(
            chatId,
            "Открыть запись с сегодня на $days дн.\n" +
                "Рабочий день: ${workDayDefaults.workStart}–${workDayDefaults.workEnd}, " +
                "перерыв ${workDayDefaults.breakStart}–${workDayDefaults.breakEnd}.",
            replyMarkup = MasterKeyboards.openPeriodMenu(days),
        )
    }

    suspend fun confirmOpenPeriod(chatId: IdChatIdentifier, chatIdKey: String, account: AccountFormEntity) {
        if (!requirePermission(account, StaffPermission.OPEN_BOOKING_PERIOD)) return
        val masterId = resolveMasterId(account)
        val days = MasterFlowState.horizonDays(chatIdKey, workDayDefaults.defaultOpenHorizonDays)
        val today = LocalDate.now(zoneId)
        val result = scheduleManagementService.openPeriod(masterId, today, days, workDayDefaults, zoneId)
        bot.sendMessage(
            chatId,
            "Готово.\n" +
                "Период: ${result.from} — ${result.to}\n" +
                "Создано дней: ${result.created}\n" +
                "Уже были открыты: ${result.skipped}",
            replyMarkup = MasterKeyboards.mainMenu(),
        )
    }

    suspend fun sendTodayBookings(chatId: IdChatIdentifier, account: AccountFormEntity) {
        if (!requirePermission(account, StaffPermission.VIEW_BOOKINGS)) return
        val masterId = resolveMasterId(account)
        val today = LocalDate.now(zoneId)
        val bookings = bookingQueryService.listForMasterDay(masterId, today, zoneId)
        bot.sendMessage(chatId, formatBookingsHeader("Сегодня", today, bookings), replyMarkup = MasterKeyboards.bookingsList(bookings))
    }

    suspend fun sendPickDayForBookings(chatId: IdChatIdentifier, account: AccountFormEntity) {
        if (!requirePermission(account, StaffPermission.VIEW_BOOKINGS)) return
        val masterId = resolveMasterId(account)
        val today = LocalDate.now(zoneId)
        val days = scheduleManagementService.listOpenDays(masterId, today, today.plusDays(30), zoneId)
        if (days.isEmpty()) {
            bot.sendMessage(chatId, "Нет открытых дней. Сначала откройте запись.", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        bot.sendMessage(chatId, "Выберите день:", replyMarkup = MasterKeyboards.dayPicker(days, MasterCallbackData.BOOKS_DAY_PREFIX, zoneId))
    }

    suspend fun sendBookingsForSchedule(chatId: IdChatIdentifier, scheduleId: UUID) {
        val bookings = bookingQueryService.listForDay(scheduleId, zoneId)
        val header = if (bookings.isEmpty()) "На этот день записей нет." else "Записи на ${bookings.first().dateLabel}:"
        bot.sendMessage(chatId, header, replyMarkup = MasterKeyboards.bookingsList(bookings))
    }

    suspend fun sendCloseDayPicker(chatId: IdChatIdentifier, account: AccountFormEntity) {
        if (!requirePermission(account, StaffPermission.CLOSE_DAY)) return
        val masterId = resolveMasterId(account)
        val today = LocalDate.now(zoneId)
        val days = scheduleManagementService.listOpenDays(masterId, today, today.plusDays(60), zoneId)
        if (days.isEmpty()) {
            bot.sendMessage(chatId, "Нет открытых дней для закрытия.", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        bot.sendMessage(
            chatId,
            "Выберите день для закрытия (удалится из расписания; только без активных записей):",
            replyMarkup = MasterKeyboards.dayPicker(days, MasterCallbackData.CLOSE_DAY_PREFIX, zoneId),
        )
    }

    suspend fun closeDay(chatId: IdChatIdentifier, account: AccountFormEntity, scheduleId: UUID) {
        if (!requirePermission(account, StaffPermission.CLOSE_DAY)) return
        val masterId = resolveMasterId(account)
        scheduleManagementService.closeDay(masterId, scheduleId, zoneId)
            .onSuccess {
                bot.sendMessage(chatId, "День закрыт для записи.", replyMarkup = MasterKeyboards.mainMenu())
            }
            .onFailure { e ->
                bot.sendMessage(chatId, e.message ?: "Не удалось закрыть день", replyMarkup = MasterKeyboards.mainMenu())
            }
    }

    suspend fun showBooking(chatId: IdChatIdentifier, bookingId: UUID) {
        val view = bookingQueryService.getView(bookingId, zoneId) ?: run {
            bot.sendMessage(chatId, "Запись не найдена.")
            return
        }
        bot.sendMessage(
            chatId,
            buildString {
                appendLine("📋 Запись")
                appendLine("${view.dateLabel}, ${view.timeLabel}")
                appendLine(view.procedureLabel)
                appendLine("Клиент: ${view.clientName}")
                view.clientPhone?.let { appendLine("📞 $it") }
                appendLine("Статус: ${view.status}")
            },
            replyMarkup = MasterKeyboards.bookingActions(bookingId),
        )
    }

    suspend fun cancelBooking(chatId: IdChatIdentifier, account: AccountFormEntity, bookingId: UUID) {
        if (!requirePermission(account, StaffPermission.MANAGE_BOOKINGS)) return
        bookingManagementService.cancel(bookingId)
        bot.sendMessage(chatId, "Запись отменена.", replyMarkup = MasterKeyboards.mainMenu())
    }

    suspend fun confirmBooking(chatId: IdChatIdentifier, account: AccountFormEntity, bookingId: UUID) {
        if (!requirePermission(account, StaffPermission.CONFIRM_BOOKINGS)) return
        bookingManagementService.confirm(bookingId)
        bot.sendMessage(chatId, "Запись подтверждена.", replyMarkup = MasterKeyboards.mainMenu())
    }

    suspend fun blockTimeInfo(chatId: IdChatIdentifier) {
        bot.sendMessage(
            chatId,
            "Блокировка части дня (обед, личное время) — следующий шаг.\n" +
                "Пока можно закрыть весь день через «Закрыть рабочий день».",
            replyMarkup = MasterKeyboards.mainMenu(),
        )
    }

    suspend fun enterClientMode(chatId: IdChatIdentifier, chatIdKey: String) {
        BookingFlowState.enableStaffClientMode(chatIdKey)
        bot.sendMessage(
            chatId,
            "Режим клиента: выбери процедуры и нажми 🟢 ВЫБРАТЬ ДАТУ.",
            replyMarkup = BotKeyboards.procedureKeyboard(emptyList(), showMasterMenu = true),
        )
        bot.sendMessage(
            chatId,
            "Кнопка «🛠 Меню мастера» всегда под рукой внизу экрана.",
            replyMarkup = BotUi.staffClientReplyKeyboard(),
        )
    }

    private fun resolveMasterId(account: AccountFormEntity): UUID {
        if (permissionService.roleOf(account) == UserRole.MASTER) {
            return account.id
        }
        return masterResolver.resolveMasterId()
    }

    private fun formatBookingsHeader(title: String, date: LocalDate, bookings: List<BookingView>): String {
        val label = DateIntervalBuilder.formatDayLabel(date)
        if (bookings.isEmpty()) return "$title ($label): записей нет."
        return "$title ($label) — ${bookings.size} записей:"
    }
}
