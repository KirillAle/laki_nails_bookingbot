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
}

object BotKeyboards {
    private const val SELECTED_PREFIX = "✓ "

    fun procedureKeyboard(selected: List<BotProcedureOption>, showMasterMenu: Boolean = false): InlineKeyboardMarkup {
        val selectedTexts = selected.mapTo(mutableSetOf()) { it.buttonText }
        val procedureRows = BotProcedureCatalog.all().chunked(2) { row ->
            row.map { option ->
                val label = if (option.buttonText in selectedTexts) {
                    SELECTED_PREFIX + option.buttonText
                } else {
                    option.buttonText
                }
                val index = BotProcedureCatalog.all().indexOfFirst { it.buttonText == option.buttonText }
                CallbackDataInlineKeyboardButton(label, BotCallbackData.PROCEDURE_PREFIX + index)
            }
        }
        val rows = procedureRows.toMutableList()
        rows += listOf(
            CallbackDataInlineKeyboardButton("🟢 ВЫБРАТЬ ДАТУ", BotCallbackData.CHOOSE_DATE),
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

    private fun MutableList<List<CallbackDataInlineKeyboardButton>>.addStaffMasterRow(showMasterMenu: Boolean) {
        if (showMasterMenu) {
            add(listOf(CallbackDataInlineKeyboardButton("🛠 Меню мастера", MasterCallbackData.MENU)))
        }
    }

    private fun shortLabel(option: BotProcedureOption): String =
        option.procedureType + " " + option.procedureSubtype.take(20)
}
