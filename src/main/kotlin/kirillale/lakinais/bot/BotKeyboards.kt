package kirillale.lakinais.bot

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.RequestContactKeyboardButton
import kirillale.lakinais.booking.AvailableDate
import kirillale.lakinais.booking.DateInterval
import kirillale.lakinais.bot.master.MasterCallbackData
import kirillale.lakinais.db.model.AvailableSlot
import kirillale.lakinais.db.service.BookingAvailabilityService
import java.time.ZoneId

object BotCallbackData {
    const val CHOOSE_DATE = "date"
    const val PROCEDURE_PREFIX = "p:"
    const val INTERVAL_PREFIX = "int:"
    const val DAY_PREFIX = "day:"
    const val SLOT_PREFIX = "slot:"
    const val SPLIT = "split"
    const val PRIORITY_MANICURE = "pri:m"
    const val PRIORITY_PEDICURE = "pri:p"
    const val BACK_PROCEDURES = "back:proc"
    const val BACK_INTERVALS = "back:int"
    const val BACK_DATES_PREFIX = "back:day:"
    const val CONFIRM_YES = "cfm:y"
    const val CONFIRM_NO = "cfm:n"
    const val MY_BOOKINGS = "my:lb"
    const val CLIENT_BOOK_PREFIX = "c:b:"
    const val CLIENT_CANCEL_PREFIX = "c:cx:"
    const val CLIENT_BOOKINGS_BACK = "c:bb"
}

object BotKeyboards {

    fun procedureKeyboard(selected: List<BotProcedureOption>, showMasterMenu: Boolean = false): InlineKeyboardMarkup {
        val selectedKeys = selected.mapTo(mutableSetOf()) { it.procedureType to it.procedureSubtype }
        val procedureRows = BotProcedureCatalog.all().mapIndexed { index, option ->
            val isSelected = (option.procedureType to option.procedureSubtype) in selectedKeys
            listOf(
                CallbackDataInlineKeyboardButton(
                    option.inlineButtonLabel(isSelected),
                    BotCallbackData.PROCEDURE_PREFIX + index,
                ),
            )
        }
        val rows = mutableListOf(
            listOf(CallbackDataInlineKeyboardButton("📋 Мои записи", BotCallbackData.MY_BOOKINGS)),
        )
        rows += procedureRows
        rows += listOf(
            listOf(CallbackDataInlineKeyboardButton("🟢 ВЫБРАТЬ ДАТУ", BotCallbackData.CHOOSE_DATE)),
        )
        rows.addStaffMasterRow(showMasterMenu)
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun intervalKeyboard(
        intervals: List<DateInterval>,
        showSplit: Boolean,
        showMasterMenu: Boolean = false,
    ): InlineKeyboardMarkup {
        val rows = intervals.map { interval ->
            listOf(
                CallbackDataInlineKeyboardButton(
                    interval.label,
                    "${BotCallbackData.INTERVAL_PREFIX}${interval.from.toEpochDay()}:${interval.to.toEpochDay()}",
                ),
            )
        }.toMutableList()
        if (showSplit) {
            rows += listOf(CallbackDataInlineKeyboardButton("↔️ Разделить процедуры", BotCallbackData.SPLIT))
        }
        rows += listOf(CallbackDataInlineKeyboardButton("◀️ К процедурам", BotCallbackData.BACK_PROCEDURES))
        rows.addStaffMasterRow(showMasterMenu)
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun splitOnlyKeyboard(showMasterMenu: Boolean = false): InlineKeyboardMarkup {
        val rows = mutableListOf(
            listOf(CallbackDataInlineKeyboardButton("↔️ Разделить процедуры", BotCallbackData.SPLIT)),
            listOf(CallbackDataInlineKeyboardButton("◀️ К процедурам", BotCallbackData.BACK_PROCEDURES)),
        )
        rows.addStaffMasterRow(showMasterMenu)
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun splitPriorityKeyboard(
        manicure: BotProcedureOption,
        pedicure: BotProcedureOption,
        showMasterMenu: Boolean = false,
    ): InlineKeyboardMarkup {
        val rows = mutableListOf(
            listOf(
                CallbackDataInlineKeyboardButton(
                    "Сначала: ${shortLabel(manicure)}",
                    BotCallbackData.PRIORITY_MANICURE,
                ),
            ),
            listOf(
                CallbackDataInlineKeyboardButton(
                    "Сначала: ${shortLabel(pedicure)}",
                    BotCallbackData.PRIORITY_PEDICURE,
                ),
            ),
            listOf(CallbackDataInlineKeyboardButton("◀️ К процедурам", BotCallbackData.BACK_PROCEDURES)),
        )
        rows.addStaffMasterRow(showMasterMenu)
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun datesKeyboard(
        dates: List<AvailableDate>,
        interval: DateInterval,
        showSplit: Boolean,
        showMasterMenu: Boolean = false,
    ): InlineKeyboardMarkup {
        val rows = dates.chunked(2) { row ->
            row.map { availableDate ->
                CallbackDataInlineKeyboardButton(
                    availableDate.label,
                    "${BotCallbackData.DAY_PREFIX}${availableDate.scheduleId}",
                )
            }
        }.toMutableList()
        if (showSplit) {
            rows += listOf(CallbackDataInlineKeyboardButton("↔️ Разделить процедуры", BotCallbackData.SPLIT))
        }
        rows += listOf(
            CallbackDataInlineKeyboardButton("◀️ К интервалам", BotCallbackData.BACK_INTERVALS),
        )
        rows.addStaffMasterRow(showMasterMenu)
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun slotsKeyboard(
        slots: List<AvailableSlot>,
        availabilityService: BookingAvailabilityService,
        zoneId: ZoneId,
        interval: DateInterval,
        showSplit: Boolean,
        showMasterMenu: Boolean = false,
    ): InlineKeyboardMarkup {
        val rows = slots.chunked(2) { row ->
            row.map { slot ->
                val label = availabilityService.formatSlotTime(slot, zoneId)
                CallbackDataInlineKeyboardButton(
                    label,
                    "${BotCallbackData.SLOT_PREFIX}${slot.scheduleId}:${slot.startTime.epochSecond}",
                )
            }
        }.toMutableList()
        if (showSplit) {
            rows += listOf(CallbackDataInlineKeyboardButton("↔️ Разделить процедуры", BotCallbackData.SPLIT))
        }
        rows += listOf(
            CallbackDataInlineKeyboardButton(
                "◀️ К датам",
                "${BotCallbackData.BACK_DATES_PREFIX}${interval.from.toEpochDay()}:${interval.to.toEpochDay()}",
            ),
        )
        rows.addStaffMasterRow(showMasterMenu)
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun confirmationKeyboard(showMasterMenu: Boolean = false): InlineKeyboardMarkup {
        val rows = mutableListOf(
            listOf(
                CallbackDataInlineKeyboardButton("✅ Подтвердить запись", BotCallbackData.CONFIRM_YES),
                CallbackDataInlineKeyboardButton("❌ Отмена", BotCallbackData.CONFIRM_NO),
            ),
        )
        rows.addStaffMasterRow(showMasterMenu)
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun phoneRequestKeyboard(): ReplyKeyboardMarkup = ReplyKeyboardMarkup(
        keyboard = listOf(listOf(RequestContactKeyboardButton("📱 Поделиться номером"))),
        resizeKeyboard = true,
        oneTimeKeyboard = true,
    )

    fun clientBookingsList(
        bookings: List<kirillale.lakinais.db.service.BookingView>,
        showMasterMenu: Boolean = false,
    ): InlineKeyboardMarkup {
        val rows = bookings.map { view ->
            listOf(
                CallbackDataInlineKeyboardButton(
                    "${view.dateLabel} ${view.timeLabel}",
                    "${BotCallbackData.CLIENT_BOOK_PREFIX}${view.bookingId}",
                ),
            )
        }.toMutableList()
        if (rows.isEmpty()) {
            rows += listOf(listOf(CallbackDataInlineKeyboardButton("Нет активных записей", BotCallbackData.CLIENT_BOOKINGS_BACK)))
        }
        rows += listOf(listOf(CallbackDataInlineKeyboardButton("◀️ К процедурам", BotCallbackData.CLIENT_BOOKINGS_BACK)))
        rows.addStaffMasterRow(showMasterMenu)
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun clientBookingActions(bookingId: java.util.UUID, showMasterMenu: Boolean = false): InlineKeyboardMarkup {
        val rows = mutableListOf(
            listOf(
                CallbackDataInlineKeyboardButton(
                    "❌ Отменить запись",
                    "${BotCallbackData.CLIENT_CANCEL_PREFIX}$bookingId",
                ),
            ),
            listOf(CallbackDataInlineKeyboardButton("◀️ К моим записям", BotCallbackData.MY_BOOKINGS)),
            listOf(CallbackDataInlineKeyboardButton("◀️ К процедурам", BotCallbackData.CLIENT_BOOKINGS_BACK)),
        )
        rows.addStaffMasterRow(showMasterMenu)
        return InlineKeyboardMarkup(keyboard = rows)
    }

    private fun MutableList<List<CallbackDataInlineKeyboardButton>>.addStaffMasterRow(showMasterMenu: Boolean) {
        if (showMasterMenu) {
            add(listOf(CallbackDataInlineKeyboardButton("🛠 Меню мастера", MasterCallbackData.MENU)))
        }
    }

    private fun shortLabel(option: BotProcedureOption): String =
        "${option.categoryIcon} ${option.procedureSubtype.take(24)}"
}
