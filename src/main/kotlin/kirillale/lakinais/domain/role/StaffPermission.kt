package kirillale.lakinais.domain.role

/**
 * Права сотрудника салона. Сейчас у MASTER/ADMIN/OWNER — полный набор (один салон).
 * Позже: разные роли → разные подмножества; привязка к organization_id.
 */
enum class StaffPermission {
    OPEN_BOOKING_PERIOD,
    MANAGE_WORK_DAY,
    BLOCK_TIME,
    CLOSE_DAY,
    VIEW_BOOKINGS,
    MANAGE_BOOKINGS,
    CONFIRM_BOOKINGS,
    MANAGE_STAFF,
}
