package kirillale.lakinais.domain.role

import kirillale.lakinais.db.entities.AccountFormEntity

class PermissionService {

    private val fullStaff = StaffPermission.entries.toSet()

    private val masterPermissions = setOf(
        StaffPermission.OPEN_BOOKING_PERIOD,
        StaffPermission.MANAGE_WORK_DAY,
        StaffPermission.BLOCK_TIME,
        StaffPermission.CLOSE_DAY,
        StaffPermission.VIEW_BOOKINGS,
        StaffPermission.MANAGE_BOOKINGS,
        StaffPermission.CONFIRM_BOOKINGS,
    )

    fun roleOf(account: AccountFormEntity): UserRole = UserRole.fromDb(account.role)

    fun permissionsFor(role: UserRole): Set<StaffPermission> = when (role) {
        UserRole.CLIENT -> emptySet()
        UserRole.MASTER -> masterPermissions
        UserRole.ADMIN, UserRole.OWNER -> fullStaff
    }

    fun hasPermission(account: AccountFormEntity, permission: StaffPermission): Boolean =
        permission in permissionsFor(roleOf(account))

    fun isStaff(account: AccountFormEntity): Boolean = UserRole.isStaff(roleOf(account))
}
