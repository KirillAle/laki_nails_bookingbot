package kirillale.lakinais.bot.master

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.types.IdChatIdentifier
import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.booking.SalonTime
import kirillale.lakinais.booking.schedule.DayKindFilter
import kirillale.lakinais.booking.schedule.ScheduleMapper
import kirillale.lakinais.booking.schedule.TimePickerOptions
import kirillale.lakinais.booking.schedule.TimeRange
import kirillale.lakinais.booking.schedule.WeekSchedulePlan
import kirillale.lakinais.booking.schedule.WorkDayProfile
import kirillale.lakinais.db.service.ScheduleManagementService
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * Единый редактор графика: будни/выходные при открытии периода и правка одного дня.
 */
class MasterScheduleEditor(
    private val bot: TelegramBot,
    private val scheduleService: ScheduleManagementService,
    private val defaultPlan: WeekSchedulePlan,
    private val zoneId: ZoneId,
) {
    suspend fun sendOpenPeriodMenu(chatId: IdChatIdentifier, chatIdKey: String) {
        val days = MasterFlowState.horizonDays(chatIdKey, defaultPlan.defaultHorizonDays)
        val plan = MasterFlowState.getOpenPlan(chatIdKey, defaultPlan)
        bot.sendMessage(
            chatId,
            buildString {
                appendLine("Открыть запись с сегодня на $days дн.")
                appendLine()
                append(plan.formatSummary())
            },
            replyMarkup = MasterKeyboards.openPeriodMenu(days),
        )
    }

    suspend fun sendProfileMenu(chatId: IdChatIdentifier, chatIdKey: String, title: String) {
        val profile = currentProfile(chatIdKey) ?: return
        val session = MasterFlowState.getProfileEdit(chatIdKey)
        val applyFilter = when (session?.target) {
            MasterFlowState.ProfileEditTarget.OpenWeekday -> DayKindFilter.WEEKDAYS
            MasterFlowState.ProfileEditTarget.OpenWeekend -> DayKindFilter.WEEKENDS
            else -> null
        }
        val showSave = session?.target is MasterFlowState.ProfileEditTarget.ExistingDay
        bot.sendMessage(
            chatId,
            "$title\n${profile.formatShort()}",
            replyMarkup = MasterKeyboards.profileMenu(
                profile,
                applyFilter = applyFilter,
                showSave = showSave,
                backCallback = backCallbackForSession(chatIdKey),
            ),
        )
    }

    suspend fun sendTimePicker(chatId: IdChatIdentifier, chatIdKey: String, fieldLabel: String) {
        val session = MasterFlowState.getProfileEdit(chatIdKey) ?: return
        val field = session.pendingField ?: return
        val profile = currentProfile(chatIdKey) ?: return
        val times = filteredTimes(profile, field)
        bot.sendMessage(
            chatId,
            "Выберите $fieldLabel:",
            replyMarkup = MasterKeyboards.timePicker(
                times,
                MasterCallbackData.SCH_TIME_PREFIX,
                backCallback = MasterCallbackData.SCH_BACK_PROFILE,
            ),
        )
    }

    suspend fun handleTimeSelected(chatId: IdChatIdentifier, chatIdKey: String, time: LocalTime) {
        val session = MasterFlowState.getProfileEdit(chatIdKey) ?: return
        val field = session.pendingField ?: return
        val updated = try {
            applyField(currentProfile(chatIdKey) ?: return, field, time)
        } catch (e: IllegalArgumentException) {
            bot.sendMessage(chatId, "Недопустимое время: ${e.message}", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        try {
            persistProfile(chatIdKey, updated)
        } catch (e: IllegalArgumentException) {
            bot.sendMessage(chatId, "Недопустимое время: ${e.message}")
            return
        }
        MasterFlowState.clearPendingField(chatIdKey)
        sendProfileMenu(chatId, chatIdKey, profileTitle(session.target))
    }

    suspend fun handleFieldSelect(chatId: IdChatIdentifier, chatIdKey: String, field: MasterFlowState.ProfileField) {
        MasterFlowState.setPendingField(chatIdKey, field)
        sendTimePicker(chatId, chatIdKey, fieldLabel(field))
    }

    private fun filteredTimes(profile: WorkDayProfile, field: MasterFlowState.ProfileField): List<LocalTime> {
        val all = TimePickerOptions.quarterHours()
        return when (field) {
            MasterFlowState.ProfileField.WORK_START -> all.filter { it < profile.workEnd }
            MasterFlowState.ProfileField.WORK_END -> all.filter { it > profile.workStart }
            MasterFlowState.ProfileField.BREAK_START -> {
                val breakEnd = profile.breakInterval?.end ?: profile.workEnd
                all.filter { !it.isBefore(profile.workStart) && it < breakEnd }
            }
            MasterFlowState.ProfileField.BREAK_END -> {
                val breakStart = profile.breakInterval?.start ?: profile.workStart
                all.filter { it > breakStart && !it.isAfter(profile.workEnd) }
            }
        }
    }

    suspend fun handleBreakToggle(chatId: IdChatIdentifier, chatIdKey: String) {
        val profile = currentProfile(chatIdKey) ?: return
        val enabled = profile.breakInterval == null
        val updated = profile.withBreak(enabled)
        persistProfile(chatIdKey, updated)
        sendProfileMenu(chatId, chatIdKey, profileTitle(MasterFlowState.getProfileEdit(chatIdKey)?.target ?: return))
    }

    suspend fun saveExistingDay(
        chatId: IdChatIdentifier,
        chatIdKey: String,
        masterId: UUID,
    ) {
        val session = MasterFlowState.getProfileEdit(chatIdKey) ?: return
        val target = session.target as? MasterFlowState.ProfileEditTarget.ExistingDay ?: return
        val profile = session.dayDraft ?: return
        scheduleService.updateDayProfile(masterId, target.scheduleId, profile, zoneId)
            .onSuccess {
                bot.sendMessage(chatId, "График дня обновлён.", replyMarkup = MasterKeyboards.mainMenu())
                MasterFlowState.clearProfileEdit(chatIdKey)
            }
            .onFailure { e ->
                bot.sendMessage(chatId, e.message ?: "Не удалось сохранить", replyMarkup = MasterKeyboards.mainMenu())
            }
    }

    suspend fun applyToOpenDays(
        chatId: IdChatIdentifier,
        chatIdKey: String,
        masterId: UUID,
        filter: DayKindFilter,
    ) {
        val profile = when (filter) {
            DayKindFilter.WEEKDAYS -> MasterFlowState.getOpenPlan(chatIdKey, defaultPlan).weekday
            DayKindFilter.WEEKENDS -> MasterFlowState.getOpenPlan(chatIdKey, defaultPlan).weekend
            DayKindFilter.ALL -> MasterFlowState.getOpenPlan(chatIdKey, defaultPlan).weekday
        }
        val today = LocalDate.now(zoneId)
        val to = today.plusDays(MasterFlowState.horizonDays(chatIdKey, defaultPlan.defaultHorizonDays).toLong())
        val result = scheduleService.applyProfileToRange(masterId, today, to, filter, profile, zoneId)
        val label = when (filter) {
            DayKindFilter.WEEKDAYS -> "будни"
            DayKindFilter.WEEKENDS -> "выходные"
            DayKindFilter.ALL -> "все дни"
        }
        bot.sendMessage(
            chatId,
            "Обновлено ($label): ${result.updated} дн. (пропущено: ${result.skipped}).",
            replyMarkup = MasterKeyboards.profileMenu(
                profile,
                applyFilter = filter,
                backCallback = backCallbackForSession(chatIdKey),
            ),
        )
    }

    suspend fun startExistingDayEdit(
        chatId: IdChatIdentifier,
        chatIdKey: String,
        masterId: UUID,
        scheduleId: UUID,
    ) {
        val schedule = scheduleService.findScheduleForMaster(masterId, scheduleId) ?: run {
            bot.sendMessage(chatId, "День не найден.", replyMarkup = MasterKeyboards.mainMenu())
            return
        }
        val date = SalonTime.toLocalDate(schedule.date, zoneId)
        val profile = ScheduleMapper.fromEntity(schedule, zoneId)
        MasterFlowState.startProfileEdit(
            chatIdKey,
            MasterFlowState.ProfileEditTarget.ExistingDay(scheduleId),
            dayDraft = profile,
        )
        bot.sendMessage(
            chatId,
            "График на ${DateIntervalBuilder.formatDayLabel(date)}:\n${profile.formatShort()}",
            replyMarkup = MasterKeyboards.profileMenu(
                profile,
                applyFilter = null,
                showSave = true,
                backCallback = MasterCallbackData.MENU,
            ),
        )
    }

    fun startOpenWeekdayEdit(chatIdKey: String) {
        MasterFlowState.startProfileEdit(chatIdKey, MasterFlowState.ProfileEditTarget.OpenWeekday)
    }

    fun startOpenWeekendEdit(chatIdKey: String) {
        MasterFlowState.startProfileEdit(chatIdKey, MasterFlowState.ProfileEditTarget.OpenWeekend)
    }

    private fun currentProfile(chatIdKey: String): WorkDayProfile? {
        val session = MasterFlowState.getProfileEdit(chatIdKey) ?: return null
        return when (val target = session.target) {
            MasterFlowState.ProfileEditTarget.OpenWeekday ->
                MasterFlowState.getOpenPlan(chatIdKey, defaultPlan).weekday
            MasterFlowState.ProfileEditTarget.OpenWeekend ->
                MasterFlowState.getOpenPlan(chatIdKey, defaultPlan).weekend
            is MasterFlowState.ProfileEditTarget.ExistingDay ->
                session.dayDraft
        }
    }

    private fun persistProfile(chatIdKey: String, profile: WorkDayProfile) {
        val session = MasterFlowState.getProfileEdit(chatIdKey) ?: return
        when (session.target) {
            MasterFlowState.ProfileEditTarget.OpenWeekday ->
                MasterFlowState.updateOpenWeekday(chatIdKey, defaultPlan, profile)
            MasterFlowState.ProfileEditTarget.OpenWeekend ->
                MasterFlowState.updateOpenWeekend(chatIdKey, defaultPlan, profile)
            is MasterFlowState.ProfileEditTarget.ExistingDay ->
                MasterFlowState.updateDayDraft(chatIdKey, profile)
        }
    }

    private fun applyField(profile: WorkDayProfile, field: MasterFlowState.ProfileField, time: LocalTime): WorkDayProfile =
        when (field) {
            MasterFlowState.ProfileField.WORK_START -> profile.copy(workStart = time)
            MasterFlowState.ProfileField.WORK_END -> profile.copy(workEnd = time)
            MasterFlowState.ProfileField.BREAK_START -> {
                val br = profile.breakInterval ?: TimeRange(time, time.plusHours(1))
                profile.copy(breakInterval = br.copy(start = time))
            }
            MasterFlowState.ProfileField.BREAK_END -> {
                val br = profile.breakInterval ?: TimeRange(time.minusHours(1), time)
                profile.copy(breakInterval = br.copy(end = time))
            }
        }

    private fun profileTitle(target: MasterFlowState.ProfileEditTarget): String = when (target) {
        MasterFlowState.ProfileEditTarget.OpenWeekday -> "График будней"
        MasterFlowState.ProfileEditTarget.OpenWeekend -> "График выходных"
        is MasterFlowState.ProfileEditTarget.ExistingDay -> "График дня"
    }

    private fun backCallbackForSession(chatIdKey: String): String {
        val session = MasterFlowState.getProfileEdit(chatIdKey) ?: return MasterCallbackData.SCH_BACK_OPEN
        return when (session.target) {
            MasterFlowState.ProfileEditTarget.OpenWeekday,
            MasterFlowState.ProfileEditTarget.OpenWeekend,
            -> MasterCallbackData.SCH_BACK_OPEN
            is MasterFlowState.ProfileEditTarget.ExistingDay -> MasterCallbackData.MENU
        }
    }

    private fun fieldLabel(field: MasterFlowState.ProfileField): String = when (field) {
        MasterFlowState.ProfileField.WORK_START -> "начало работы"
        MasterFlowState.ProfileField.WORK_END -> "конец работы"
        MasterFlowState.ProfileField.BREAK_START -> "начало перерыва"
        MasterFlowState.ProfileField.BREAK_END -> "конец перерыва"
    }
}
