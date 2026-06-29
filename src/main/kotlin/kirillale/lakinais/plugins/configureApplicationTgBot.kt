package kirillale.lakinais.plugins

import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContact
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.MessageDataCallbackQuery
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kirillale.lakinais.booking.DateInterval
import kirillale.lakinais.booking.schedule.WeekSchedulePlan
import kirillale.lakinais.bot.BookingFlowState
import kirillale.lakinais.bot.BookingReminderService
import kirillale.lakinais.bot.BotBookingNavigator
import kirillale.lakinais.bot.BotCallbackData
import kirillale.lakinais.bot.BotKeyboards
import kirillale.lakinais.bot.BotProcedureCatalog
import kirillale.lakinais.bot.BotUi
import kirillale.lakinais.bot.PhoneValidator
import kirillale.lakinais.bot.master.MasterBotNavigator
import kirillale.lakinais.bot.master.MasterCallbackData
import kirillale.lakinais.bot.master.MasterFlowState
import kirillale.lakinais.config.AppEnv
import kirillale.lakinais.db.service.AccountService
import kirillale.lakinais.db.service.BookingAvailabilityService
import kirillale.lakinais.db.service.BookingCreationService
import kirillale.lakinais.db.service.BookingManagementService
import kirillale.lakinais.db.service.BookingQueryService
import kirillale.lakinais.db.service.MasterResolver
import kirillale.lakinais.db.service.ScheduleManagementService
import kirillale.lakinais.domain.role.PermissionService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val log = LoggerFactory.getLogger("TgBot")

private fun Application.readZoneId(): ZoneId =
    ZoneId.of(environment.config.propertyOrNull("booking.timezone")?.getString() ?: "Asia/Tbilisi")

private fun Application.readMasterResolver(): MasterResolver {
    val masterId = environment.config.propertyOrNull("booking.masterId")?.getString()?.let(UUID::fromString)
    return MasterResolver(configuredMasterId = masterId)
}

private fun Application.readWeekSchedulePlan(): WeekSchedulePlan {
    val cfg = environment.config
    return WeekSchedulePlan.fromConfig(
        weekdayWorkStart = cfg.propertyOrNull("booking.workStart")?.getString() ?: "10:00",
        weekdayWorkEnd = cfg.propertyOrNull("booking.workEnd")?.getString() ?: "19:00",
        weekdayBreakStart = cfg.propertyOrNull("booking.breakStart")?.getString() ?: "14:00",
        weekdayBreakEnd = cfg.propertyOrNull("booking.breakEnd")?.getString() ?: "15:00",
        weekendWorkStart = cfg.propertyOrNull("booking.weekendWorkStart")?.getString(),
        weekendWorkEnd = cfg.propertyOrNull("booking.weekendWorkEnd")?.getString(),
        weekendBreakStart = cfg.propertyOrNull("booking.weekendBreakStart")?.getString(),
        weekendBreakEnd = cfg.propertyOrNull("booking.weekendBreakEnd")?.getString(),
        horizonDays = cfg.propertyOrNull("booking.defaultOpenHorizonDays")?.getString()?.toIntOrNull() ?: 30,
    )
}

fun Application.configureApplicationTgBot() {
    val botToken = environment.config.propertyOrNull("telegram.botToken")?.getString()
        ?: AppEnv.get("LAKI_NAILS_BOT_TOKEN")
        ?: error("Задайте LAKI_NAILS_BOT_TOKEN в environment.env или telegram.botToken в конфиге")

    val zoneId = readZoneId()
    val schedulePlan = readWeekSchedulePlan()
    val accountService = AccountService()
    val permissionService = PermissionService()
    val masterResolver = readMasterResolver()

    val bot = telegramBot(botToken)
    val clientNavigator = BotBookingNavigator(
        bot = bot,
        availabilityService = BookingAvailabilityService(),
        masterResolver = masterResolver,
        bookingCreationService = BookingCreationService(),
        bookingQueryService = BookingQueryService(),
        bookingManagementService = BookingManagementService(),
        accountService = accountService,
        zoneId = zoneId,
    )
    val masterNavigator = MasterBotNavigator(
        bot = bot,
        permissionService = permissionService,
        masterResolver = masterResolver,
        scheduleManagementService = ScheduleManagementService(),
        bookingQueryService = BookingQueryService(),
        bookingManagementService = BookingManagementService(),
        defaultSchedulePlan = schedulePlan,
        zoneId = zoneId,
    )
    val reminderService = BookingReminderService(zoneId = zoneId)
    reminderService.ensureStorage()

    CoroutineScope(Dispatchers.Default).launch {
        while (true) {
            try {
                reminderService.sendDueReminders(bot)
            } catch (e: Exception) {
                log.warn("Reminder tick failed: {}", e.message)
            }
            delay(15 * 60 * 1000L)
        }
    }

    CoroutineScope(Dispatchers.Default).launch {
        bot.buildBehaviourWithLongPolling {
            onCommand("start") { message ->
                val user = message.from ?: return@onCommand
                val chatIdKey = message.chat.id.toString()
                BookingFlowState.clear(chatIdKey)
                MasterFlowState.clear(chatIdKey)

                val account = try {
                    accountService.findOrCreateTelegramUser(
                        telegramId = user.id.chatId.toString(),
                        firstName = user.firstName ?: "",
                        lastName = user.lastName ?: "",
                        userName = user.username?.username ?: "",
                    )
                } catch (e: Exception) {
                    log.warn("Не удалось сохранить пользователя: {}", e.message)
                    null
                }

                if (account != null && masterNavigator.isStaff(account)) {
                    BookingFlowState.enableStaffClientMode(chatIdKey)
                    sendMessage(
                        message.chat.id,
                        "Добро пожаловать! Запись клиента и меню мастера — кнопками ниже.",
                        replyMarkup = BotUi.staffClientReplyKeyboard(),
                    )
                    masterNavigator.sendMainMenu(message.chat.id, account)
                } else {
                    log.warn(
                        "Клиентский /start: tg={} username={} accountId={} role={}",
                        user.id.chatId,
                        user.username?.username,
                        account?.id,
                        account?.role,
                    )
                    sendMessage(
                        message.chat.id,
                        "Нажми кнопку ниже, чтобы приступить к выбору процедур.",
                        replyMarkup = BotUi.startReplyKeyboard(),
                    )
                }
            }

            onText(initialFilter = { (it.content as? TextContent)?.text == BotUi.START_BUTTON }) { message ->
                val chatIdKey = message.chat.id.toString()
                val staffClient = BookingFlowState.isStaffClientMode(chatIdKey)
                BookingFlowState.clear(chatIdKey)
                if (staffClient) BookingFlowState.enableStaffClientMode(chatIdKey)
                clientNavigator.sendProcedureMenu(message.chat.id, chatIdKey, fresh = true)
            }

            onText(initialFilter = { (it.content as? TextContent)?.text == BotUi.MY_BOOKINGS_BUTTON }) { message ->
                val user = message.from ?: return@onText
                val chatIdKey = message.chat.id.toString()
                val account = accountService.getByTelegramId(user.id.chatId.toString()) ?: return@onText
                clientNavigator.sendMyBookings(message.chat.id, chatIdKey, account.id)
            }

            onText(initialFilter = { (it.content as? TextContent)?.text == BotUi.MASTER_MENU_BUTTON }) { message ->
                val user = message.from ?: return@onText
                val chatIdKey = message.chat.id.toString()
                val account = accountService.getByTelegramId(user.id.chatId.toString())
                if (account == null || !masterNavigator.isStaff(account)) return@onText
                BookingFlowState.clear(chatIdKey)
                masterNavigator.sendMainMenu(message.chat.id, account)
            }

            onText(initialFilter = { BookingFlowState.isAwaitingPhone(it.chat.id.toString()) }) { message ->
                val user = message.from ?: return@onText
                val chatIdKey = message.chat.id.toString()
                val text = (message.content as? TextContent)?.text ?: return@onText
                val phone = PhoneValidator.normalize(text) ?: run {
                    sendMessage(message.chat.id, "Не удалось распознать номер. Пример: +995555123456")
                    return@onText
                }
                clientNavigator.handlePhoneReceived(message.chat.id, chatIdKey, user.id.chatId.toString(), phone)
            }

            onContact { message ->
                val user = message.from ?: return@onContact
                val chatIdKey = message.chat.id.toString()
                if (!BookingFlowState.isAwaitingPhone(chatIdKey)) return@onContact
                val contact = message.content.contact
                if (contact.userId != null && contact.userId != user.id) {
                    sendMessage(message.chat.id, "Отправьте свой номер через кнопку «Поделиться номером».")
                    return@onContact
                }
                val phone = PhoneValidator.normalize(contact.phoneNumber) ?: contact.phoneNumber
                clientNavigator.handlePhoneReceived(message.chat.id, chatIdKey, user.id.chatId.toString(), phone)
            }

            onDataCallbackQuery { callback ->
                if (callback !is MessageDataCallbackQuery) return@onDataCallbackQuery
                val chatId = callback.message.chat.id
                val chatIdKey = chatId.toString()
                val data = callback.data
                val telegramId = callback.from.id.chatId.toString()

                try {
                    when {
                        data.startsWith(MasterCallbackData.PREFIX) -> {
                            answerCallbackQuery(callback)
                            val account = accountService.getByTelegramId(telegramId)
                            if (account == null || !masterNavigator.isStaff(account)) {
                                sendMessage(chatId, "Нет доступа к меню мастера.")
                                return@onDataCallbackQuery
                            }
                            handleMasterCallback(data, chatId, chatIdKey, account, masterNavigator, schedulePlan)
                        }

                        data == BotCallbackData.MY_BOOKINGS -> {
                            answerCallbackQuery(callback)
                            val account = accountService.getByTelegramId(telegramId) ?: return@onDataCallbackQuery
                            clientNavigator.sendMyBookings(chatId, chatIdKey, account.id)
                        }

                        data == BotCallbackData.CLIENT_BOOKINGS_BACK -> {
                            answerCallbackQuery(callback)
                            clientNavigator.sendProcedureMenu(chatId, chatIdKey)
                        }

                        data.startsWith(BotCallbackData.CLIENT_BOOK_PREFIX) -> {
                            answerCallbackQuery(callback)
                            val bookingId = UUID.fromString(data.removePrefix(BotCallbackData.CLIENT_BOOK_PREFIX))
                            val account = accountService.getByTelegramId(telegramId) ?: return@onDataCallbackQuery
                            clientNavigator.showClientBooking(chatId, chatIdKey, account.id, bookingId)
                        }

                        data.startsWith(BotCallbackData.CLIENT_CANCEL_PREFIX) -> {
                            answerCallbackQuery(callback)
                            val bookingId = UUID.fromString(data.removePrefix(BotCallbackData.CLIENT_CANCEL_PREFIX))
                            val account = accountService.getByTelegramId(telegramId) ?: return@onDataCallbackQuery
                            clientNavigator.cancelClientBooking(chatId, chatIdKey, account.id, bookingId)
                        }

                        data == BotCallbackData.CHOOSE_DATE -> {
                            val selected = BookingFlowState.getSelection(chatIdKey)
                            if (selected.isEmpty()) {
                                answerCallbackQuery(callback, "Сначала выбери процедуру", showAlert = true)
                            } else {
                                answerCallbackQuery(callback)
                                clientNavigator.sendIntervalsScreen(chatId, chatIdKey)
                            }
                        }

                        data == BotCallbackData.PRIORITY_MANICURE -> {
                            answerCallbackQuery(callback)
                            BookingFlowState.setSplitPriority(
                                chatIdKey,
                                BookingFlowState.getSelection(chatIdKey).first { it.procedureType == "Маникюр" },
                            )
                            clientNavigator.sendIntervalsScreen(chatId, chatIdKey)
                        }

                        data == BotCallbackData.PRIORITY_PEDICURE -> {
                            answerCallbackQuery(callback)
                            BookingFlowState.setSplitPriority(
                                chatIdKey,
                                BookingFlowState.getSelection(chatIdKey).first { it.procedureType == "Педикюр" },
                            )
                            clientNavigator.sendIntervalsScreen(chatId, chatIdKey)
                        }

                        data.startsWith(BotCallbackData.PROCEDURE_PREFIX) -> {
                            val index = data.removePrefix(BotCallbackData.PROCEDURE_PREFIX).toIntOrNull()
                            if (index == null) {
                                answerCallbackQuery(callback)
                                return@onDataCallbackQuery
                            }
                            val options = BotProcedureCatalog.all()
                            if (index !in options.indices) {
                                answerCallbackQuery(callback)
                                return@onDataCallbackQuery
                            }
                            answerCallbackQuery(callback)
                            val selected = BookingFlowState.toggle(chatIdKey, options[index])
                            editMessageReplyMarkup(
                                callback.message,
                                replyMarkup = BotKeyboards.procedureKeyboard(
                                    selected,
                                    BookingFlowState.isStaffClientMode(chatIdKey),
                                ),
                            )
                        }

                        data.startsWith(BotCallbackData.INTERVAL_PREFIX) -> {
                            answerCallbackQuery(callback)
                            val parts = data.removePrefix(BotCallbackData.INTERVAL_PREFIX).split(":")
                            if (parts.size != 2) return@onDataCallbackQuery
                            clientNavigator.sendDatesScreen(
                                chatId, chatIdKey,
                                DateInterval(LocalDate.ofEpochDay(parts[0].toLong()), LocalDate.ofEpochDay(parts[1].toLong()), ""),
                            )
                        }

                        data.startsWith(BotCallbackData.DAY_PREFIX) -> {
                            answerCallbackQuery(callback)
                            clientNavigator.sendSlotsScreen(chatId, chatIdKey, UUID.fromString(data.removePrefix(BotCallbackData.DAY_PREFIX)))
                        }

                        data.startsWith(BotCallbackData.SLOT_PREFIX) -> {
                            answerCallbackQuery(callback)
                            val payload = data.removePrefix(BotCallbackData.SLOT_PREFIX)
                            val sep = payload.lastIndexOf(':')
                            if (sep < 0) return@onDataCallbackQuery
                            clientNavigator.handleSlotSelected(
                                chatId, chatIdKey, telegramId,
                                UUID.fromString(payload.substring(0, sep)),
                                Instant.ofEpochSecond(payload.substring(sep + 1).toLong()),
                            )
                        }

                        data == BotCallbackData.CONFIRM_YES -> {
                            answerCallbackQuery(callback)
                            clientNavigator.handleConfirmYes(chatId, chatIdKey, telegramId)
                        }

                        data == BotCallbackData.CONFIRM_NO -> {
                            answerCallbackQuery(callback)
                            clientNavigator.handleConfirmNo(chatId, chatIdKey)
                        }

                        data == BotCallbackData.SPLIT -> {
                            answerCallbackQuery(callback)
                            BookingFlowState.enableSplit(chatIdKey)
                            clientNavigator.sendIntervalsScreen(chatId, chatIdKey)
                        }

                        data == BotCallbackData.BACK_PROCEDURES -> {
                            answerCallbackQuery(callback)
                            BookingFlowState.clearPending(chatIdKey)
                            clientNavigator.sendProcedureMenu(chatId, chatIdKey)
                        }

                        data == BotCallbackData.BACK_INTERVALS -> {
                            answerCallbackQuery(callback)
                            BookingFlowState.clearPending(chatIdKey)
                            clientNavigator.sendIntervalsScreen(chatId, chatIdKey)
                        }

                        data.startsWith(BotCallbackData.BACK_DATES_PREFIX) -> {
                            answerCallbackQuery(callback)
                            val parts = data.removePrefix(BotCallbackData.BACK_DATES_PREFIX).split(":")
                            if (parts.size != 2) return@onDataCallbackQuery
                            clientNavigator.sendDatesScreen(
                                chatId, chatIdKey,
                                DateInterval(LocalDate.ofEpochDay(parts[0].toLong()), LocalDate.ofEpochDay(parts[1].toLong()), ""),
                            )
                        }

                        else -> {
                            log.warn("Неизвестный callback: {}", data)
                            answerCallbackQuery(callback)
                        }
                    }
                } catch (e: Exception) {
                    log.error("Ошибка callback: {}", e.message, e)
                    answerCallbackQuery(callback, "Ошибка. Попробуйте /start", showAlert = true)
                }
            }
        }.join()
    }
}

private suspend fun handleMasterCallback(
    data: String,
    chatId: dev.inmo.tgbotapi.types.IdChatIdentifier,
    chatIdKey: String,
    account: kirillale.lakinais.db.entities.AccountFormEntity,
    masterNavigator: MasterBotNavigator,
    schedulePlan: WeekSchedulePlan,
) {
    val editor = masterNavigator.scheduleEditor()
    when {
        data == MasterCallbackData.MENU -> {
            BookingFlowState.clear(chatIdKey)
            masterNavigator.sendMainMenu(chatId, account)
        }
        data == MasterCallbackData.OPEN_MENU -> masterNavigator.sendOpenPeriodMenu(chatId, chatIdKey)
        data == MasterCallbackData.OPEN_CONFIRM -> masterNavigator.confirmOpenPeriod(chatId, chatIdKey, account)
        data == MasterCallbackData.OPEN_MINUS -> {
            MasterFlowState.adjustHorizon(chatIdKey, schedulePlan.defaultHorizonDays, -1)
            masterNavigator.sendOpenPeriodMenu(chatId, chatIdKey)
        }
        data == MasterCallbackData.OPEN_PLUS -> {
            MasterFlowState.adjustHorizon(chatIdKey, schedulePlan.defaultHorizonDays, 1)
            masterNavigator.sendOpenPeriodMenu(chatId, chatIdKey)
        }
        data == MasterCallbackData.OPEN_CFG_WEEKDAY -> masterNavigator.onOpenWeekdayConfig(chatId, chatIdKey)
        data == MasterCallbackData.OPEN_CFG_WEEKEND -> masterNavigator.onOpenWeekendConfig(chatId, chatIdKey)
        data == MasterCallbackData.SCH_BACK_OPEN -> masterNavigator.sendOpenPeriodMenu(chatId, chatIdKey)
        data == MasterCallbackData.SCH_BACK_PROFILE -> {
            val session = MasterFlowState.getProfileEdit(chatIdKey) ?: return
            editor.sendProfileMenu(chatId, chatIdKey, profileTitleFor(session.target))
        }
        data == MasterCallbackData.SCH_BREAK_TOGGLE -> editor.handleBreakToggle(chatId, chatIdKey)
        data == MasterCallbackData.SCH_SAVE_DAY -> editor.saveExistingDay(chatId, chatIdKey, masterNavigator.resolveMasterIdFor(account))
        data == MasterCallbackData.SCH_APPLY_WEEKDAYS -> editor.applyToOpenDays(chatId, chatIdKey, masterNavigator.resolveMasterIdFor(account), kirillale.lakinais.booking.schedule.DayKindFilter.WEEKDAYS)
        data == MasterCallbackData.SCH_APPLY_WEEKENDS -> editor.applyToOpenDays(chatId, chatIdKey, masterNavigator.resolveMasterIdFor(account), kirillale.lakinais.booking.schedule.DayKindFilter.WEEKENDS)
        data.startsWith(MasterCallbackData.SCH_FIELD_PREFIX) -> {
            val code = data.removePrefix(MasterCallbackData.SCH_FIELD_PREFIX)
            val field = when (code) {
                "ws" -> MasterFlowState.ProfileField.WORK_START
                "we" -> MasterFlowState.ProfileField.WORK_END
                "bs" -> MasterFlowState.ProfileField.BREAK_START
                "be" -> MasterFlowState.ProfileField.BREAK_END
                else -> return
            }
            editor.handleFieldSelect(chatId, chatIdKey, field)
        }
        data.startsWith(MasterCallbackData.SCH_TIME_PREFIX) -> {
            val time = java.time.LocalTime.parse(data.removePrefix(MasterCallbackData.SCH_TIME_PREFIX))
            editor.handleTimeSelected(chatId, chatIdKey, time)
        }
        data == MasterCallbackData.EDIT_DAY_PICK -> masterNavigator.sendEditDayPicker(chatId, account)
        data.startsWith(MasterCallbackData.EDIT_DAY_PREFIX) -> {
            val scheduleId = UUID.fromString(data.removePrefix(MasterCallbackData.EDIT_DAY_PREFIX))
            masterNavigator.onEditDaySelected(chatId, chatIdKey, account, scheduleId)
        }
        data.startsWith(MasterCallbackData.OPEN_PRESET_PREFIX) -> {
            val days = data.removePrefix(MasterCallbackData.OPEN_PRESET_PREFIX).toIntOrNull() ?: return
            MasterFlowState.setHorizon(chatIdKey, days)
            masterNavigator.sendOpenPeriodMenu(chatId, chatIdKey)
        }
        data == MasterCallbackData.BOOKS_TODAY -> masterNavigator.sendTodayBookings(chatId, account)
        data == MasterCallbackData.BOOKS_UPCOMING -> masterNavigator.sendUpcomingBookings(chatId, account)
        data == MasterCallbackData.BOOKS_PICK_DAY -> masterNavigator.sendPickDayForBookings(chatId, account)
        data.startsWith(MasterCallbackData.BOOKS_DAY_PREFIX) -> {
            val scheduleId = UUID.fromString(data.removePrefix(MasterCallbackData.BOOKS_DAY_PREFIX))
            masterNavigator.sendBookingsForSchedule(chatId, scheduleId)
        }
        data.startsWith(MasterCallbackData.BOOK_PREFIX) -> {
            val bookingId = UUID.fromString(data.removePrefix(MasterCallbackData.BOOK_PREFIX))
            masterNavigator.showBooking(chatId, bookingId)
        }
        data.startsWith(MasterCallbackData.BOOK_CANCEL_PREFIX) -> {
            val bookingId = UUID.fromString(data.removePrefix(MasterCallbackData.BOOK_CANCEL_PREFIX))
            masterNavigator.cancelBooking(chatId, account, bookingId)
        }
        data.startsWith(MasterCallbackData.BOOK_CONFIRM_PREFIX) -> {
            val bookingId = UUID.fromString(data.removePrefix(MasterCallbackData.BOOK_CONFIRM_PREFIX))
            masterNavigator.confirmBooking(chatId, account, bookingId)
        }
        data == MasterCallbackData.CLOSE_PICK -> masterNavigator.sendCloseDayPicker(chatId, account)
        data.startsWith(MasterCallbackData.CLOSE_DAY_PREFIX) -> {
            val scheduleId = UUID.fromString(data.removePrefix(MasterCallbackData.CLOSE_DAY_PREFIX))
            masterNavigator.closeDay(chatId, account, scheduleId)
        }
        data == MasterCallbackData.BLOCK_TIME -> masterNavigator.sendBlockDayPicker(chatId, account)
        data == MasterCallbackData.BLOCK_BACK_TO_DAYS -> masterNavigator.sendBlockDayPicker(chatId, account)
        data.startsWith(MasterCallbackData.BLOCK_PICK_DAY_PREFIX) -> {
            val scheduleId = UUID.fromString(data.removePrefix(MasterCallbackData.BLOCK_PICK_DAY_PREFIX))
            masterNavigator.sendBlockStartPicker(chatId, chatIdKey, account, scheduleId)
        }
        data.startsWith(MasterCallbackData.BLOCK_PICK_START_PREFIX) -> {
            val t = data.removePrefix(MasterCallbackData.BLOCK_PICK_START_PREFIX)
            val time = java.time.LocalTime.parse(t)
            masterNavigator.handleBlockStartPicked(chatId, chatIdKey, account, time)
        }
        data.startsWith(MasterCallbackData.BLOCK_PICK_END_PREFIX) -> {
            val t = data.removePrefix(MasterCallbackData.BLOCK_PICK_END_PREFIX)
            val time = java.time.LocalTime.parse(t)
            masterNavigator.handleBlockEndPicked(chatId, chatIdKey, account, time)
        }
        data == MasterCallbackData.BLOCK_CONFIRM -> masterNavigator.confirmBlock(chatId, chatIdKey, account)
        data.startsWith(MasterCallbackData.BLOCK_DELETE_PREFIX) -> {
            val blockId = UUID.fromString(data.removePrefix(MasterCallbackData.BLOCK_DELETE_PREFIX))
            masterNavigator.deleteBlock(chatId, account, blockId)
        }
        data == MasterCallbackData.CLIENT_MODE -> {
            BookingFlowState.clear(chatIdKey)
            masterNavigator.enterClientMode(chatId, chatIdKey)
        }
    }
}

private fun profileTitleFor(target: MasterFlowState.ProfileEditTarget): String = when (target) {
    MasterFlowState.ProfileEditTarget.OpenWeekday -> "График будней"
    MasterFlowState.ProfileEditTarget.OpenWeekend -> "График выходных"
    is MasterFlowState.ProfileEditTarget.ExistingDay -> "График дня"
}
