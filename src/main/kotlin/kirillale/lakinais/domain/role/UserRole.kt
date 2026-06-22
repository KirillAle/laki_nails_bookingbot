package kirillale.lakinais.domain.role

enum class UserRole(val dbValue: String) {
    CLIENT("CLIENT"),
    MASTER("MASTER"),
    ADMIN("ADMIN"),
    OWNER("OWNER");

    companion object {
        fun fromDb(value: String): UserRole = when (value.trim().uppercase()) {
            "M" -> MASTER
            else -> entries.firstOrNull { it.dbValue.equals(value.trim(), ignoreCase = true) } ?: CLIENT
        }

        fun isStaff(role: UserRole): Boolean = role != CLIENT
    }
}
