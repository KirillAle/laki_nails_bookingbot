package kirillale.lakinais.bot

object PhoneValidator {
    fun normalize(raw: String): String? {
        val cleaned = raw.trim().replace(Regex("[\\s\\-()]"), "")
        if (cleaned.length < 8 || cleaned.length > 16) return null
        if (cleaned.count { it == '+' } > 1) return null
        if (cleaned.contains('+') && !cleaned.startsWith('+')) return null

        val digits = cleaned.removePrefix("+")
        if (digits.length < 7 || !digits.all { it.isDigit() }) return null

        return if (cleaned.startsWith("+")) cleaned else "+$digits"
    }
}
