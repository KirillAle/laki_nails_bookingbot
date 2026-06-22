package kirillale.lakinais.bot.master

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.db.entities.MasterScheduleEntity
import kirillale.lakinais.db.service.BookingView
import java.time.LocalDate
import java.time.ZoneId

object MasterKeyboards {
    fun mainMenu(): InlineKeyboardMarkup = InlineKeyboardMarkup(
        keyboard = listOf(
            listOf(CallbackDataInlineKeyboardButton("📅 Открыть запись", MasterCallbackData.OPEN_MENU)),
            listOf(CallbackDataInlineKeyboardButton("📋 Записи на сегодня", MasterCallbackData.BOOKS_TODAY)),
            listOf(CallbackDataInlineKeyboardButton("📋 Записи на день", MasterCallbackData.BOOKS_PICK_DAY)),
            listOf(CallbackDataInlineKeyboardButton("🚫 Закрыть рабочий день", MasterCallbackData.CLOSE_PICK)),
            listOf(CallbackDataInlineKeyboardButton("⏸ Закрыть время (блок)", MasterCallbackData.BLOCK_TIME)),
            listOf(CallbackDataInlineKeyboardButton("👤 Режим клиента", MasterCallbackData.CLIENT_MODE)),
        ),
    )

    fun openPeriodMenu(horizonDays: Int): InlineKeyboardMarkup = InlineKeyboardMarkup(
        keyboard = listOf(
            listOf(
                CallbackDataInlineKeyboardButton("−1 дн.", MasterCallbackData.OPEN_MINUS),
                CallbackDataInlineKeyboardButton("Сейчас: $horizonDays дн.", MasterCallbackData.OPEN_MENU),
                CallbackDataInlineKeyboardButton("+1 дн.", MasterCallbackData.OPEN_PLUS),
            ),
            listOf(
                CallbackDataInlineKeyboardButton("7 дней", "${MasterCallbackData.OPEN_PRESET_PREFIX}7"),
                CallbackDataInlineKeyboardButton("30 дней", "${MasterCallbackData.OPEN_PRESET_PREFIX}30"),
            ),
            listOf(CallbackDataInlineKeyboardButton("✅ Открыть на $horizonDays дн.", MasterCallbackData.OPEN_CONFIRM)),
            listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU)),
        ),
    )

    fun dayPicker(
        days: List<MasterScheduleEntity>,
        callbackPrefix: String,
        zoneId: ZoneId,
    ): InlineKeyboardMarkup {
        val rows = days.map { schedule ->
            val date = schedule.date.atZone(zoneId).toLocalDate()
            listOf(
                CallbackDataInlineKeyboardButton(
                    DateIntervalBuilder.formatDayLabel(date),
                    "$callbackPrefix${schedule.id}",
                ),
            )
        }.toMutableList()
        rows += listOf(listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU)))
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun bookingsList(bookings: List<BookingView>): InlineKeyboardMarkup {
        val rows = bookings.map { view ->
            listOf(
                CallbackDataInlineKeyboardButton(
                    "${view.timeLabel} ${view.clientName}",
                    "${MasterCallbackData.BOOK_PREFIX}${view.bookingId}",
                ),
            )
        }.toMutableList()
        if (rows.isEmpty()) {
            rows += listOf(listOf(CallbackDataInlineKeyboardButton("Нет записей", MasterCallbackData.MENU)))
        }
        rows += listOf(listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU)))
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun bookingActions(bookingId: java.util.UUID): InlineKeyboardMarkup = InlineKeyboardMarkup(
        keyboard = listOf(
            listOf(CallbackDataInlineKeyboardButton("✅ Подтвердить", "${MasterCallbackData.BOOK_CONFIRM_PREFIX}$bookingId")),
            listOf(CallbackDataInlineKeyboardButton("❌ Отменить запись", "${MasterCallbackData.BOOK_CANCEL_PREFIX}$bookingId")),
            listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU)),
        ),
    )
}
