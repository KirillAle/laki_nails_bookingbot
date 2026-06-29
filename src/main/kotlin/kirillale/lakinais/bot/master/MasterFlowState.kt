package kirillale.lakinais.bot.master

import kirillale.lakinais.booking.schedule.WeekSchedulePlan
import kirillale.lakinais.booking.schedule.WorkDayProfile
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object MasterFlowState {
    private val openHorizonByChat = ConcurrentHashMap<String, AtomicInteger>()
    private val openPlanByChat = ConcurrentHashMap<String, WeekSchedulePlan>()
    private val blockDraftByChat = ConcurrentHashMap<String, BlockDraft>()
    private val profileEditByChat = ConcurrentHashMap<String, ProfileEditSession>()

    data class BlockDraft(
        val scheduleId: UUID,
        val start: LocalTime? = null,
        val end: LocalTime? = null,
    )

    sealed class ProfileEditTarget {
        data object OpenWeekday : ProfileEditTarget()
        data object OpenWeekend : ProfileEditTarget()
        data class ExistingDay(val scheduleId: UUID) : ProfileEditTarget()
    }

    enum class ProfileField {
        WORK_START,
        WORK_END,
        BREAK_START,
        BREAK_END,
    }

    data class ProfileEditSession(
        val target: ProfileEditTarget,
        val pendingField: ProfileField? = null,
        val dayDraft: WorkDayProfile? = null,
    )

    fun horizonDays(chatId: String, default: Int): Int =
        openHorizonByChat.getOrPut(chatId) { AtomicInteger(default) }.get()

    fun adjustHorizon(chatId: String, default: Int, delta: Int) {
        val holder = openHorizonByChat.getOrPut(chatId) { AtomicInteger(default) }
        holder.updateAndGet { current -> (current + delta).coerceIn(1, 90) }
    }

    fun setHorizon(chatId: String, days: Int) {
        openHorizonByChat.getOrPut(chatId) { AtomicInteger(days) }.set(days.coerceIn(1, 90))
    }

    fun getOpenPlan(chatId: String, defaultPlan: WeekSchedulePlan): WeekSchedulePlan =
        openPlanByChat.getOrPut(chatId) { defaultPlan }

    fun setOpenPlan(chatId: String, plan: WeekSchedulePlan) {
        openPlanByChat[chatId] = plan
    }

    fun updateOpenWeekday(chatId: String, defaultPlan: WeekSchedulePlan, profile: WorkDayProfile) {
        val plan = getOpenPlan(chatId, defaultPlan).withWeekday(profile)
        openPlanByChat[chatId] = plan
    }

    fun updateOpenWeekend(chatId: String, defaultPlan: WeekSchedulePlan, profile: WorkDayProfile) {
        val plan = getOpenPlan(chatId, defaultPlan).withWeekend(profile)
        openPlanByChat[chatId] = plan
    }

    fun updateDayDraft(chatId: String, profile: WorkDayProfile) {
        val session = profileEditByChat[chatId] ?: return
        profileEditByChat[chatId] = session.copy(dayDraft = profile)
    }

    fun getDayDraft(chatId: String): WorkDayProfile? = profileEditByChat[chatId]?.dayDraft

    fun startProfileEdit(chatId: String, target: ProfileEditTarget, dayDraft: WorkDayProfile? = null) {
        profileEditByChat[chatId] = ProfileEditSession(target = target, dayDraft = dayDraft)
    }

    fun setPendingField(chatId: String, field: ProfileField) {
        val session = profileEditByChat[chatId] ?: return
        profileEditByChat[chatId] = session.copy(pendingField = field)
    }

    fun clearPendingField(chatId: String) {
        val session = profileEditByChat[chatId] ?: return
        profileEditByChat[chatId] = session.copy(pendingField = null)
    }

    fun getProfileEdit(chatId: String): ProfileEditSession? = profileEditByChat[chatId]

    fun clearProfileEdit(chatId: String) {
        profileEditByChat.remove(chatId)
    }

    fun clear(chatId: String) {
        openHorizonByChat.remove(chatId)
        openPlanByChat.remove(chatId)
        blockDraftByChat.remove(chatId)
        profileEditByChat.remove(chatId)
    }

    fun startBlockDraft(chatId: String, scheduleId: UUID) {
        blockDraftByChat[chatId] = BlockDraft(scheduleId = scheduleId)
    }

    fun setBlockStart(chatId: String, start: LocalTime) {
        val current = blockDraftByChat[chatId] ?: return
        blockDraftByChat[chatId] = current.copy(start = start, end = null)
    }

    fun setBlockEnd(chatId: String, end: LocalTime) {
        val current = blockDraftByChat[chatId] ?: return
        blockDraftByChat[chatId] = current.copy(end = end)
    }

    fun getBlockDraft(chatId: String): BlockDraft? = blockDraftByChat[chatId]
}
