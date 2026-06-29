package kirillale.lakinais.bot.master

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.IdChatIdentifier
import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.booking.SalonTime
import kirillale.lakinais.booking.schedule.WeekSchedulePlan
import kirillale.lakinais.bot.BookingFlowState
import kirillale.lakinais.bot.BotKeyboards
import kirillale.lakinais.bot.BotUi
import kirillale.lakinais.db.entities.AccountFormEntity
import kirillale.lakinais.db.service.BookingManagementService
import kirillale.lakinais.db.service.BookingQueryService
import kirillale.lakinais.db.service.BookingView
import kirillale.lakinais.db.service.MasterResolver
import kirillale.lakinais.db.service.ScheduleManagementService
import kirillale.lakinais.db.service.MasterTimeBlockService
import kirillale.lakinais.domain.role.PermissionService
import kirillale.lakinais.domain.role.StaffPermission
import java.time.LocalTime
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
    private val masterTimeBlockService: MasterTimeBlockService = MasterTimeBlockService(),
    private val defaultSchedulePlan: WeekSchedulePlan,
    private val zoneId: ZoneId,
) {
    private val scheduleEditor = MasterScheduleEditor(bot, scheduleManagementService, defaultSchedulePlan, zoneId)
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
        scheduleEditor.sendOpenPeriodMenu(chatId, chatIdKey)
    }

    suspend fun confirmOpenPeriod(chatId: IdChatIdentifier, chatIdKey: String, account: AccountFormEntity) {
        if (!requirePermission(account, StaffPermission.OPEN_BOOKING_PERIOD)) return
        val masterId = resolveMasterId(account)
        val days = MasterFlowState.horizonDays(chatIdKey, defaultSchedulePlan.defaultHorizonDays)
        val plan = MasterFlowState.getOpenPlan(chatIdKey, defaultSchedulePlan)
        val today = LocalDate.now(zoneId)
        val result = scheduleManagementService.openPeriod(masterId, today, days, plan, zoneId)
        bot.sendMessage(
            chatId,
            "Готово.\n" +
                "Период: ${result.from} — ${result.to}\n" +
                "Создано дней: ${result.created}\n" +
                "Уже были открыты: ${result.skipped}\n\n" +
                plan.formatSummary(),
            replyMarkup = MasterKeyboards.mainMenu(),
        )
    }

    suspend fun sendEditDayPicker(chatId: IdChatIdentifier, account: AccountFormEntity) {
        if (!requirePermission(account, StaffPermission.MANAGE_WORK_DAY)) return
        val masterId = resolveMasterId(account)
        val today = LocalDate.now(zoneId)
        val days = scheduleManagementService.listOpenDays(masterId, today, today.plusDays(60), zoneId)
        if (days.isEmpty()) {
            bot.sendMessage(chatId, "Нет открытых дней.", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        bot.sendMessage(
            chatId,
            "Выберите день для настройки графика:",
            replyMarkup = MasterKeyboards.dayPicker(days, MasterCallbackData.EDIT_DAY_PREFIX, zoneId),
        )
    }

    suspend fun sendUpcomingBookings(chatId: IdChatIdentifier, account: AccountFormEntity) {
        if (!requirePermission(account, StaffPermission.VIEW_BOOKINGS)) return
        val masterId = resolveMasterId(account)
        val today = LocalDate.now(zoneId)
        val bookings = bookingQueryService.listUpcomingForMaster(masterId, zoneId, today)
        val header = if (bookings.isEmpty()) {
            "Ближайших записей нет."
        } else {
            "Ближайшие записи (${bookings.size}):"
        }
        bot.sendMessage(chatId, header, replyMarkup = MasterKeyboards.bookingsList(bookings))
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
                bot.sendMessage(chatId, "День закрыт — клиенты его больше не видят для записи.", replyMarkup = MasterKeyboards.mainMenu())
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

    fun resolveMasterIdFor(account: AccountFormEntity): UUID = resolveMasterId(account)

    suspend fun onOpenWeekdayConfig(chatId: IdChatIdentifier, chatIdKey: String) {
        scheduleEditor.startOpenWeekdayEdit(chatIdKey)
        scheduleEditor.sendProfileMenu(chatId, chatIdKey, "График будней")
    }

    suspend fun onOpenWeekendConfig(chatId: IdChatIdentifier, chatIdKey: String) {
        scheduleEditor.startOpenWeekendEdit(chatIdKey)
        scheduleEditor.sendProfileMenu(chatId, chatIdKey, "График выходных")
    }

    suspend fun onEditDaySelected(chatId: IdChatIdentifier, chatIdKey: String, account: AccountFormEntity, scheduleId: UUID) {
        if (!requirePermission(account, StaffPermission.MANAGE_WORK_DAY)) return
        scheduleEditor.startExistingDayEdit(chatId, chatIdKey, resolveMasterId(account), scheduleId)
    }

    fun scheduleEditor(): MasterScheduleEditor = scheduleEditor

    suspend fun sendBlockDayPicker(chatId: IdChatIdentifier, account: AccountFormEntity) {
        if (!requirePermission(account, StaffPermission.BLOCK_TIME)) return
        val masterId = resolveMasterId(account)
        val today = LocalDate.now(zoneId)
        val days = scheduleManagementService.listOpenDays(masterId, today, today.plusDays(60), zoneId)
        if (days.isEmpty()) {
            bot.sendMessage(chatId, "Нет открытых дней. Сначала откройте запись.", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        bot.sendMessage(
            chatId,
            "Выберите день для блокировки времени:",
            replyMarkup = MasterKeyboards.dayPicker(days, MasterCallbackData.BLOCK_PICK_DAY_PREFIX, zoneId),
        )
    }

    suspend fun sendBlockStartPicker(chatId: IdChatIdentifier, chatIdKey: String, account: AccountFormEntity, scheduleId: UUID) {
        if (!requirePermission(account, StaffPermission.BLOCK_TIME)) return
        val masterId = resolveMasterId(account)
        val schedule = scheduleManagementService.findScheduleForMaster(masterId, scheduleId)
            ?: run {
                bot.sendMessage(chatId, "День не найден.", replyMarkup = MasterKeyboards.mainMenu())
                return
            }
        MasterFlowState.startBlockDraft(chatIdKey, scheduleId)
        val blocks = masterTimeBlockService.getByMasterIdAndDate(masterId, schedule.date)
        val existing = MasterKeyboards.blocksList(blocks, zoneId)
        val times = workDayTimes(schedule)
        bot.sendMessage(
            chatId,
            "Выберите начало блокировки на ${DateIntervalBuilder.formatDayLabel(SalonTime.toLocalDate(schedule.date, zoneId))}:",
            replyMarkup = MasterKeyboards.blockPickStart(times, existing),
        )
    }

    suspend fun handleBlockStartPicked(chatId: IdChatIdentifier, chatIdKey: String, account: AccountFormEntity, start: LocalTime) {
        if (!requirePermission(account, StaffPermission.BLOCK_TIME)) return
        MasterFlowState.setBlockStart(chatIdKey, start)
        val draft = MasterFlowState.getBlockDraft(chatIdKey) ?: return
        val masterId = resolveMasterId(account)
        val schedule = scheduleManagementService.findScheduleForMaster(masterId, draft.scheduleId)
            ?: run {
                bot.sendMessage(chatId, "День не найден.", replyMarkup = MasterKeyboards.mainMenu())
                return
            }
        val times = workDayTimes(schedule).filter { it.isAfter(start) }
        bot.sendMessage(chatId, "Выберите конец блокировки:", replyMarkup = MasterKeyboards.blockPickEnd(times))
    }

    suspend fun handleBlockEndPicked(chatId: IdChatIdentifier, chatIdKey: String, account: AccountFormEntity, end: LocalTime) {
        if (!requirePermission(account, StaffPermission.BLOCK_TIME)) return
        MasterFlowState.setBlockEnd(chatIdKey, end)
        val draft = MasterFlowState.getBlockDraft(chatIdKey) ?: return
        val masterId = resolveMasterId(account)
        val schedule = scheduleManagementService.findScheduleForMaster(masterId, draft.scheduleId)
            ?: run {
                bot.sendMessage(chatId, "День не найден.", replyMarkup = MasterKeyboards.mainMenu())
                return
            }
        val start = draft.start ?: return
        if (!end.isAfter(start)) {
            bot.sendMessage(chatId, "Конец должен быть позже начала.", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        val dateLabel = DateIntervalBuilder.formatDayLabel(SalonTime.toLocalDate(schedule.date, zoneId))
        bot.sendMessage(
            chatId,
            "Закрыть время: $dateLabel, ${start}–${end}\nПодтвердить?",
            replyMarkup = MasterKeyboards.blockConfirm(),
        )
    }

    suspend fun confirmBlock(chatId: IdChatIdentifier, chatIdKey: String, account: AccountFormEntity) {
        if (!requirePermission(account, StaffPermission.BLOCK_TIME)) return
        val draft = MasterFlowState.getBlockDraft(chatIdKey) ?: run {
            bot.sendMessage(chatId, "Нет выбранного блока. Начните заново.", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        val start = draft.start
        val end = draft.end
        if (start == null || end == null || !end.isAfter(start)) {
            bot.sendMessage(chatId, "Неверный интервал. Начните заново.", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        val masterId = resolveMasterId(account)
        val schedule = scheduleManagementService.findScheduleForMaster(masterId, draft.scheduleId)
            ?: run {
                bot.sendMessage(chatId, "День не найден.", replyMarkup = MasterKeyboards.mainMenu())
                return
            }
        val day = SalonTime.toLocalDate(schedule.date, zoneId)
        val startInstant = SalonTime.atTime(day, start, zoneId)
        val endInstant = SalonTime.atTime(day, end, zoneId)
        masterTimeBlockService.createTimeBlock(
            masterId = masterId,
            date = schedule.date,
            startTime = startInstant,
            endTime = endInstant,
            reason = null,
        )
        bot.sendMessage(chatId, "Время закрыто: ${start}–${end}", replyMarkup = MasterKeyboards.mainMenu())
    }

    suspend fun deleteBlock(chatId: IdChatIdentifier, account: AccountFormEntity, blockId: UUID) {
        if (!requirePermission(account, StaffPermission.BLOCK_TIME)) return
        val masterId = resolveMasterId(account)
        val block = masterTimeBlockService.getById(blockId) ?: run {
            bot.sendMessage(chatId, "Блок не найден.", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        if (block.masterId != masterId) {
            bot.sendMessage(chatId, "Нет доступа к этому блоку.", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        masterTimeBlockService.deleteById(blockId)
        bot.sendMessage(chatId, "Блок снят.", replyMarkup = MasterKeyboards.mainMenu())
    }

    suspend fun enterClientMode(chatId: IdChatIdentifier, chatIdKey: String) {
        BookingFlowState.enableStaffClientMode(chatIdKey)
        bot.sendMessage(
            chatId,
            "Режим клиента: выбери процедуры и нажми 🟢 ВЫБРАТЬ ДАТУ.",
            replyMarkup = BotKeyboards.procedureKeyboard(emptyList(), showMasterMenu = true),
        )
    }

    private fun resolveMasterId(@Suppress("UNUSED_PARAMETER") account: AccountFormEntity): UUID =
        masterResolver.resolveMasterId()

    private fun formatBookingsHeader(title: String, date: LocalDate, bookings: List<BookingView>): String {
        val label = DateIntervalBuilder.formatDayLabel(date)
        if (bookings.isEmpty()) return "$title ($label): записей нет."
        return "$title ($label) — ${bookings.size} записей:"
    }

    private fun workDayTimes(schedule: kirillale.lakinais.db.entities.MasterScheduleEntity): List<LocalTime> {
        val start = SalonTime.toLocalTime(schedule.timeStart, zoneId)
        val end = SalonTime.toLocalTime(schedule.timeEnd, zoneId)
        val times = mutableListOf<LocalTime>()
        var t = start
        while (t.isBefore(end)) {
            times += t
            t = t.plusMinutes(15)
        }
        return times
    }
}
