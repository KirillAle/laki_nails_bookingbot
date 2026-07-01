package kirillale.lakinais.bot.master

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.booking.SalonTime
import kirillale.lakinais.booking.schedule.DayKindFilter
import kirillale.lakinais.booking.schedule.WorkDayProfile
import kirillale.lakinais.db.entities.MasterScheduleEntity
import kirillale.lakinais.db.entities.MasterTimeBlockEntity
import kirillale.lakinais.db.service.BookingView
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object MasterKeyboards {
    fun mainMenu(): InlineKeyboardMarkup = InlineKeyboardMarkup(
        keyboard = listOf(
            listOf(CallbackDataInlineKeyboardButton("📅 Открыть запись", MasterCallbackData.OPEN_MENU)),
            listOf(CallbackDataInlineKeyboardButton("📋 Записи на сегодня", MasterCallbackData.BOOKS_TODAY)),
            listOf(CallbackDataInlineKeyboardButton("📋 Все ближайшие записи", MasterCallbackData.BOOKS_UPCOMING)),
            listOf(CallbackDataInlineKeyboardButton("📋 Записи на день", MasterCallbackData.BOOKS_PICK_DAY)),
            listOf(CallbackDataInlineKeyboardButton("⚙️ График дня", MasterCallbackData.EDIT_DAY_PICK)),
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
            listOf(CallbackDataInlineKeyboardButton("⚙️ Будни", MasterCallbackData.OPEN_CFG_WEEKDAY)),
            listOf(CallbackDataInlineKeyboardButton("⚙️ Выходные", MasterCallbackData.OPEN_CFG_WEEKEND)),
            listOf(CallbackDataInlineKeyboardButton("✅ Открыть на $horizonDays дн.", MasterCallbackData.OPEN_CONFIRM)),
            listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU)),
        ),
    )

    fun profileMenu(
        profile: WorkDayProfile,
        applyFilter: DayKindFilter? = null,
        showSave: Boolean = false,
        backCallback: String = MasterCallbackData.SCH_BACK_OPEN,
    ): InlineKeyboardMarkup {
        val breakLabel = if (profile.breakInterval != null) "🔕 Убрать перерыв" else "🔔 Добавить перерыв"
        val rows = mutableListOf<List<CallbackDataInlineKeyboardButton>>()
        rows += listOf(CallbackDataInlineKeyboardButton("🕐 Начало", "${MasterCallbackData.SCH_FIELD_PREFIX}ws"))
        rows += listOf(CallbackDataInlineKeyboardButton("🕘 Конец", "${MasterCallbackData.SCH_FIELD_PREFIX}we"))
        rows += listOf(CallbackDataInlineKeyboardButton(breakLabel, MasterCallbackData.SCH_BREAK_TOGGLE))
        if (profile.breakInterval != null) {
            rows += listOf(
                CallbackDataInlineKeyboardButton("☕ Начало перерыва", "${MasterCallbackData.SCH_FIELD_PREFIX}bs"),
                CallbackDataInlineKeyboardButton("☕ Конец перерыва", "${MasterCallbackData.SCH_FIELD_PREFIX}be"),
            )
        }
        when (applyFilter) {
            DayKindFilter.WEEKDAYS -> rows += listOf(
                CallbackDataInlineKeyboardButton("📅 Применить к открытым будням", MasterCallbackData.SCH_APPLY_WEEKDAYS),
            )
            DayKindFilter.WEEKENDS -> rows += listOf(
                CallbackDataInlineKeyboardButton("📅 Применить к открытым выходным", MasterCallbackData.SCH_APPLY_WEEKENDS),
            )
            else -> Unit
        }
        if (showSave) {
            rows += listOf(CallbackDataInlineKeyboardButton("✅ Сохранить день", MasterCallbackData.SCH_SAVE_DAY))
        }
        rows += listOf(CallbackDataInlineKeyboardButton("◀️ Назад", backCallback))
        rows += listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU))
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun timePicker(
        times: List<LocalTime>,
        callbackPrefix: String,
        backCallback: String,
    ): InlineKeyboardMarkup {
        val rows = mutableListOf<List<CallbackDataInlineKeyboardButton>>()
        rows += timeRows(times, callbackPrefix)
        rows += listOf(CallbackDataInlineKeyboardButton("◀️ Назад", backCallback))
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun dayPicker(
        days: List<MasterScheduleEntity>,
        callbackPrefix: String,
        zoneId: ZoneId,
    ): InlineKeyboardMarkup {
        val buttons = days.map { schedule ->
            val date = schedule.date.atZone(zoneId).toLocalDate()
            CallbackDataInlineKeyboardButton(
                DateIntervalBuilder.formatDayLabel(date),
                "$callbackPrefix${schedule.id}",
            )
        }
        val rows = buttonsToRows(buttons, columns = 1).toMutableList()
        rows += listOf(listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU)))
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun bookingsDayPicker(
        days: List<MasterScheduleEntity>,
        bookingCountByScheduleId: Map<java.util.UUID, Int>,
        zoneId: ZoneId,
    ): InlineKeyboardMarkup {
        val buttons = days.map { schedule ->
            val date = schedule.date.atZone(zoneId).toLocalDate()
            CallbackDataInlineKeyboardButton(
                "${DateIntervalBuilder.formatDayLabel(date)} (${bookingCountByScheduleId[schedule.id] ?: 0})",
                "${MasterCallbackData.BOOKS_DAY_PREFIX}${schedule.id}",
            )
        }
        val rows = buttonsToRows(buttons, columns = 2).toMutableList()
        rows += listOf(listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU)))
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun bookingsList(bookings: List<BookingView>): InlineKeyboardMarkup {
        val rows = bookings.map { view ->
            listOf(
                CallbackDataInlineKeyboardButton(
                    "${view.dateLabel} ${view.timeLabel} · ${view.clientName}",
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

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun blocksList(blocks: List<MasterTimeBlockEntity>, zoneId: ZoneId): List<List<CallbackDataInlineKeyboardButton>> {
        if (blocks.isEmpty()) return emptyList()
        return blocks
            .sortedBy { it.startTime }
            .map { b ->
                val st = timeFormatter.format(SalonTime.toLocalTime(b.startTime, zoneId))
                val en = timeFormatter.format(SalonTime.toLocalTime(b.endTime, zoneId))
                val label = buildString {
                    append("⛔ $st–$en")
                    b.reason?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it.take(16)) }
                }
                listOf(CallbackDataInlineKeyboardButton("Снять: $label", "${MasterCallbackData.BLOCK_DELETE_PREFIX}${b.id}"))
            }
    }

    fun blockPickStart(
        times: List<LocalTime>,
        existingBlocksRows: List<List<CallbackDataInlineKeyboardButton>> = emptyList(),
    ): InlineKeyboardMarkup {
        val rows = mutableListOf<List<CallbackDataInlineKeyboardButton>>()
        rows += existingBlocksRows
        rows += timeRows(times, MasterCallbackData.BLOCK_PICK_START_PREFIX)
        rows += listOf(listOf(CallbackDataInlineKeyboardButton("◀️ Назад", MasterCallbackData.BLOCK_BACK_TO_DAYS)))
        rows += listOf(listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU)))
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun blockPickEnd(times: List<LocalTime>): InlineKeyboardMarkup {
        val rows = mutableListOf<List<CallbackDataInlineKeyboardButton>>()
        rows += timeRows(times, MasterCallbackData.BLOCK_PICK_END_PREFIX)
        rows += listOf(listOf(CallbackDataInlineKeyboardButton("◀️ Назад", MasterCallbackData.BLOCK_BACK_TO_DAYS)))
        rows += listOf(listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU)))
        return InlineKeyboardMarkup(keyboard = rows)
    }

    fun blockConfirm(): InlineKeyboardMarkup = InlineKeyboardMarkup(
        keyboard = listOf(
            listOf(CallbackDataInlineKeyboardButton("✅ Закрыть время", MasterCallbackData.BLOCK_CONFIRM)),
            listOf(CallbackDataInlineKeyboardButton("◀️ Назад", MasterCallbackData.BLOCK_BACK_TO_DAYS)),
            listOf(CallbackDataInlineKeyboardButton("◀️ В меню", MasterCallbackData.MENU)),
        ),
    )

    private fun timeRows(times: List<LocalTime>, prefix: String): List<List<CallbackDataInlineKeyboardButton>> =
        buttonsToRows(
            times.map { t ->
                val label = timeFormatter.format(t)
                CallbackDataInlineKeyboardButton(label, prefix + label)
            },
            columns = 2,
        )

    private fun buttonsToRows(
        buttons: List<CallbackDataInlineKeyboardButton>,
        columns: Int,
    ): List<List<CallbackDataInlineKeyboardButton>> {
        if (buttons.isEmpty()) return emptyList()
        val cols = columns.coerceAtLeast(1)
        val rows = mutableListOf<List<CallbackDataInlineKeyboardButton>>()
        var i = 0
        while (i < buttons.size) {
            val row = buttons.subList(i, minOf(i + cols, buttons.size))
            rows += row
            i += cols
        }
        return rows
    }
}
