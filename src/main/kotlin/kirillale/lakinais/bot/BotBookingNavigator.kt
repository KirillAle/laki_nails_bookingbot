package kirillale.lakinais.bot

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import kirillale.lakinais.booking.BookingSlotMode
import kirillale.lakinais.booking.DateInterval
import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.booking.IntervalSearchResult
import kirillale.lakinais.db.service.AccountService
import kirillale.lakinais.db.service.BookingAvailabilityService
import kirillale.lakinais.db.service.BookingCreationService
import kirillale.lakinais.db.service.BookingLimitExceededException
import kirillale.lakinais.db.service.BookingManagementService
import kirillale.lakinais.db.service.BookingQueryService
import kirillale.lakinais.db.service.MasterResolver
import kirillale.lakinais.db.service.SlotTakenException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class BotBookingNavigator(
    private val bot: TelegramBot,
    private val availabilityService: BookingAvailabilityService,
    private val masterResolver: MasterResolver,
    private val bookingCreationService: BookingCreationService,
    private val bookingQueryService: BookingQueryService,
    private val bookingManagementService: BookingManagementService,
    private val accountService: AccountService,
    private val zoneId: ZoneId,
) {
    private fun staffMenu(chatIdKey: String): Boolean = BookingFlowState.isStaffClientMode(chatIdKey)

    suspend fun sendIntervalsScreen(chatId: IdChatIdentifier, chatIdKey: String) {
        val selected = BookingFlowState.getSelection(chatIdKey)
        val state = BookingFlowState.getOrCreate(chatIdKey)
        val masterId = masterResolver.resolveMasterId()

        if (state.mode == BookingSlotMode.SPLIT && selected.size == 2 && state.splitPriority == null) {
            val manicure = selected.first { it.procedureType == "Маникюр" }
            val pedicure = selected.first { it.procedureType == "Педикюр" }
            bot.sendMessage(
                chatId,
                "Подряд записаться не получится или вы решили разделить процедуры.\n" +
                    "Что записываем в первую очередь?",
                replyMarkup = BotKeyboards.splitPriorityKeyboard(manicure, pedicure, staffMenu(chatIdKey)),
            )
            return
        }

        when (val result = availabilityService.searchIntervals(
                masterId, selected, state.mode, state.splitPriority, zoneId,
            )
        ) {
            is IntervalSearchResult.ConsecutiveUnavailable -> {
                bot.sendMessage(
                    chatId,
                    "К сожалению, нет свободных окон, куда влезают обе процедуры подряд.\n" +
                        "Можно разделить: записаться на маникюр и педикюр в разное время.",
                    replyMarkup = BotKeyboards.splitOnlyKeyboard(staffMenu(chatIdKey)),
                )
            }
            is IntervalSearchResult.NothingAvailable -> {
                val hint = if (state.mode == BookingSlotMode.SPLIT && state.splitPriority != null) {
                    "для «${state.splitPriority!!.buttonText}»"
                } else {
                    "для выбранных процедур"
                }
                bot.sendMessage(
                    chatId,
                    "Сейчас нет свободных дат $hint в ближайшие 30 дней.",
                    replyMarkup = BotKeyboards.procedureKeyboard(selected, staffMenu(chatIdKey)),
                )
            }
            is IntervalSearchResult.Intervals -> {
                val modeHint = when {
                    state.mode == BookingSlotMode.SPLIT && state.splitPriority != null ->
                        "\nСейчас ищем окна для: ${state.splitPriority!!.buttonText}"
                    selected.size == 2 && state.mode == BookingSlotMode.CONSECUTIVE ->
                        "\nПоказаны интервалы, где можно записаться на обе процедуры подряд."
                    else -> ""
                }
                bot.sendMessage(
                    chatId,
                    "Выбери интервал дат:$modeHint",
                    replyMarkup = BotKeyboards.intervalKeyboard(
                        result.intervals,
                        showSplitOption(state, selected),
                        staffMenu(chatIdKey),
                    ),
                )
            }
        }
    }

    suspend fun sendDatesScreen(chatId: IdChatIdentifier, chatIdKey: String, interval: DateInterval) {
        val selected = BookingFlowState.getSelection(chatIdKey)
        val state = BookingFlowState.getOrCreate(chatIdKey)
        BookingFlowState.setCurrentInterval(chatIdKey, interval)

        val dates = availabilityService.getAvailableDatesInInterval(
            masterResolver.resolveMasterId(), interval, selected,
            state.mode, state.splitPriority, zoneId,
        )
        val label = intervalLabel(interval)

        if (dates.isEmpty()) {
            val intervals = (availabilityService.searchIntervals(
                masterResolver.resolveMasterId(), selected, state.mode, state.splitPriority, zoneId,
            ) as? IntervalSearchResult.Intervals)?.intervals.orEmpty()
            bot.sendMessage(
                chatId,
                "В интервале «$label» нет свободных дат.",
                replyMarkup = BotKeyboards.intervalKeyboard(
                    intervals,
                    showSplitOption(state, selected),
                    staffMenu(chatIdKey),
                ),
            )
            return
        }

        bot.sendMessage(
            chatId,
            "Выбери дату в интервале «$label»:",
            replyMarkup = BotKeyboards.datesKeyboard(
                dates, interval, showSplitOption(state, selected), staffMenu(chatIdKey),
            ),
        )
    }

    suspend fun sendSlotsScreen(chatId: IdChatIdentifier, chatIdKey: String, scheduleId: UUID) {
        val selected = BookingFlowState.getSelection(chatIdKey)
        val state = BookingFlowState.getOrCreate(chatIdKey)
        val interval = state.currentInterval ?: run {
            bot.sendMessage(chatId, "Сессия устарела. Начните сначала: /start")
            return
        }

        val slots = availabilityService.getAvailableTimeSlots(
            scheduleId, selected, state.mode, state.splitPriority, zoneId,
        )

        if (slots.isEmpty()) {
            bot.sendMessage(
                chatId,
                "На эту дату окна уже заняли. Выбери другую дату.",
                replyMarkup = BotKeyboards.datesKeyboard(
                    availabilityService.getAvailableDatesInInterval(
                        masterResolver.resolveMasterId(), interval, selected,
                        state.mode, state.splitPriority, zoneId,
                    ),
                    interval,
                    showSplitOption(state, selected),
                    staffMenu(chatIdKey),
                ),
            )
            return
        }

        bot.sendMessage(
            chatId,
            "Выбери время:",
            replyMarkup = BotKeyboards.slotsKeyboard(
                slots, availabilityService, zoneId, interval,
                showSplitOption(state, selected),
                staffMenu(chatIdKey),
            ),
        )
    }

    suspend fun handleSlotSelected(
        chatId: IdChatIdentifier,
        chatIdKey: String,
        telegramId: String,
        scheduleId: UUID,
        slotStart: Instant,
    ) {
        val state = BookingFlowState.getOrCreate(chatIdKey)
        val selected = BookingFlowState.getSelection(chatIdKey)
        val slot = availabilityService.getAvailableTimeSlots(
            scheduleId, selected, state.mode, state.splitPriority, zoneId,
        ).find { it.startTime == slotStart }

        if (slot == null) {
            bot.sendMessage(chatId, "Это окно уже заняли. Выберите другое время.")
            sendSlotsScreen(chatId, chatIdKey, scheduleId)
            return
        }

        BookingFlowState.setPendingSlot(chatIdKey, scheduleId, slotStart)
        proceedToPhoneOrConfirm(chatId, chatIdKey, telegramId)
    }

    suspend fun handlePhoneReceived(
        chatId: IdChatIdentifier,
        chatIdKey: String,
        telegramId: String,
        phone: String,
    ) {
        if (!BookingFlowState.hasPendingBooking(chatIdKey)) {
            bot.sendMessage(chatId, "Сначала выберите дату и время.")
            return
        }
        val account = accountService.getByTelegramId(telegramId) ?: run {
            bot.sendMessage(chatId, "Сначала нажмите /start")
            return
        }
        accountService.updatePhone(account.id, phone)
        bot.sendMessage(chatId, "Номер сохранён: $phone", replyMarkup = ReplyKeyboardRemove())
        sendConfirmation(chatId, chatIdKey, phone)
    }

    suspend fun handleConfirmYes(chatId: IdChatIdentifier, chatIdKey: String, telegramId: String) {
        if (!BookingFlowState.hasPendingBooking(chatIdKey)) {
            bot.sendMessage(chatId, "Нет активной записи для подтверждения. Начните сначала: /start")
            return
        }

        val state = BookingFlowState.getOrCreate(chatIdKey)
        val scheduleId = state.pendingScheduleId!!
        val slotStart = state.pendingSlotStart!!

        val account = accountService.getByTelegramId(telegramId) ?: run {
            bot.sendMessage(chatId, "Сначала нажмите /start")
            return
        }
        val phone = account.phone
        if (phone.isNullOrBlank()) {
            requestPhone(chatId, chatIdKey)
            return
        }

        val selected = BookingFlowState.getSelection(chatIdKey)
        val plan = try {
            bookingCreationService.buildPlan(selected, state.mode, state.splitPriority, scheduleId, slotStart)
        } catch (e: Exception) {
            bot.sendMessage(chatId, "Не удалось подготовить запись: ${e.message}")
            return
        }

        try {
            bookingCreationService.createFromPlan(account.id, plan)
        } catch (e: BookingLimitExceededException) {
            bot.sendMessage(chatId, e.message ?: "Слишком много активных записей")
            BookingFlowState.clearPending(chatIdKey)
            return
        } catch (e: SlotTakenException) {
            bot.sendMessage(chatId, e.message ?: "Слот занят")
            sendSlotsScreen(chatId, chatIdKey, scheduleId)
            return
        } catch (e: Exception) {
            bot.sendMessage(chatId, "Ошибка при создании записи: ${e.message}")
            return
        }

        val wasStaffClient = staffMenu(chatIdKey)
        bot.sendMessage(
            chatId,
            BookingConfirmationFormatter.formatSuccess(plan, phone, availabilityService, zoneId),
            replyMarkup = if (wasStaffClient) BotUi.staffClientReplyKeyboard() else BotUi.startReplyKeyboard(),
        )

        if (state.mode == BookingSlotMode.SPLIT && selected.size == 2 && !state.splitFirstDone) {
            BookingFlowState.markSplitFirstDone(chatIdKey)
            val second = BookingFlowState.getOrCreate(chatIdKey).splitPriority
            bot.sendMessage(chatId, "Теперь выберем время для: ${second?.buttonText ?: "второй процедуры"}")
            sendIntervalsScreen(chatId, chatIdKey)
        } else {
            BookingFlowState.clear(chatIdKey)
            if (wasStaffClient) BookingFlowState.enableStaffClientMode(chatIdKey)
        }
    }

    suspend fun sendMyBookings(chatId: IdChatIdentifier, chatIdKey: String, accountId: UUID) {
        val bookings = bookingQueryService.listActiveForClient(accountId, zoneId)
        val header = if (bookings.isEmpty()) {
            "У вас нет активных записей."
        } else {
            "Ваши записи (${bookings.size}):"
        }
        bot.sendMessage(
            chatId,
            header,
            replyMarkup = BotKeyboards.clientBookingsList(bookings, staffMenu(chatIdKey)),
        )
    }

    suspend fun showClientBooking(chatId: IdChatIdentifier, chatIdKey: String, accountId: UUID, bookingId: UUID) {
        val view = bookingQueryService.listActiveForClient(accountId, zoneId)
            .firstOrNull { it.bookingId == bookingId }
        if (view == null) {
            bot.sendMessage(chatId, "Запись не найдена или уже отменена.")
            return
        }
        bot.sendMessage(
            chatId,
            buildString {
                appendLine("📋 Ваша запись")
                appendLine("${view.dateLabel}, ${view.timeLabel}")
                appendLine(view.procedureLabel)
                appendLine("Статус: ${clientStatusLabel(view.status)}")
                appendLine()
                appendLine("Чтобы перенести — отмените запись и выберите новое время.")
            },
            replyMarkup = BotKeyboards.clientBookingActions(bookingId, staffMenu(chatIdKey)),
        )
    }

    suspend fun cancelClientBooking(chatId: IdChatIdentifier, chatIdKey: String, accountId: UUID, bookingId: UUID) {
        bookingManagementService.cancelForClient(accountId, bookingId)
            .onSuccess {
                bot.sendMessage(chatId, "Запись отменена.")
                sendMyBookings(chatId, chatIdKey, accountId)
            }
            .onFailure { e ->
                bot.sendMessage(chatId, e.message ?: "Не удалось отменить запись.")
            }
    }

    suspend fun sendProcedureMenu(chatId: IdChatIdentifier, chatIdKey: String, fresh: Boolean = false) {
        val selected = if (fresh) emptyList() else BookingFlowState.getSelection(chatIdKey)
        bot.sendMessage(
            chatId,
            "Выбери процедуры (до 1 маникюра и 1 педикюра), затем нажми 🟢 ВЫБРАТЬ ДАТУ.",
            replyMarkup = BotKeyboards.procedureKeyboard(selected, staffMenu(chatIdKey)),
        )
    }

    suspend fun handleConfirmNo(chatId: IdChatIdentifier, chatIdKey: String) {
        val state = BookingFlowState.getOrCreate(chatIdKey)
        val scheduleId = state.pendingScheduleId
        BookingFlowState.clearPending(chatIdKey)
        bot.sendMessage(chatId, "Запись отменена.", replyMarkup = ReplyKeyboardRemove())
        if (scheduleId != null) {
            sendSlotsScreen(chatId, chatIdKey, scheduleId)
        } else {
            sendIntervalsScreen(chatId, chatIdKey)
        }
    }

    private fun clientStatusLabel(status: String): String = when (status) {
        "PENDING" -> "ожидает подтверждения мастера"
        "CONFIRMED" -> "подтверждена"
        "CANCELLED" -> "отменена"
        else -> status
    }

    private suspend fun proceedToPhoneOrConfirm(
        chatId: IdChatIdentifier,
        chatIdKey: String,
        telegramId: String,
    ) {
        val phone = accountService.getByTelegramId(telegramId)?.phone
        if (phone.isNullOrBlank()) {
            requestPhone(chatId, chatIdKey)
        } else {
            sendConfirmation(chatId, chatIdKey, phone)
        }
    }

    private suspend fun requestPhone(chatId: IdChatIdentifier, chatIdKey: String) {
        BookingFlowState.setAwaitingPhone(chatIdKey)
        bot.sendMessage(
            chatId,
            "Для подтверждения записи нужен номер телефона.\n" +
                "Нажмите кнопку ниже или введите номер вручную (например +995555123456).",
            replyMarkup = BotKeyboards.phoneRequestKeyboard(),
        )
    }

    private suspend fun sendConfirmation(chatId: IdChatIdentifier, chatIdKey: String, phone: String) {
        val state = BookingFlowState.getOrCreate(chatIdKey)
        val scheduleId = state.pendingScheduleId
        val slotStart = state.pendingSlotStart
        if (scheduleId == null || slotStart == null) {
            bot.sendMessage(chatId, "Сессия устарела. Начните сначала: /start")
            return
        }

        val plan = bookingCreationService.buildPlan(
            BookingFlowState.getSelection(chatIdKey),
            state.mode,
            state.splitPriority,
            scheduleId,
            slotStart,
        )

        BookingFlowState.setAwaitingConfirm(chatIdKey)
        bot.sendMessage(
            chatId,
            BookingConfirmationFormatter.formatConfirmation(plan, phone, availabilityService, zoneId),
            replyMarkup = BotKeyboards.confirmationKeyboard(staffMenu(chatIdKey)),
        )
    }

    private fun showSplitOption(state: ChatBookingState, selected: List<BotProcedureOption>): Boolean =
        selected.size == 2 && state.mode == BookingSlotMode.CONSECUTIVE && !state.splitFirstDone

    private fun intervalLabel(interval: DateInterval): String {
        if (interval.label.isNotBlank()) return interval.label
        return DateIntervalBuilder.visibleIntervals(LocalDate.now(zoneId))
            .firstOrNull { it.from == interval.from && it.to == interval.to }
            ?.label
            ?: "${interval.from.dayOfMonth}–${interval.to.dayOfMonth}"
    }
}
