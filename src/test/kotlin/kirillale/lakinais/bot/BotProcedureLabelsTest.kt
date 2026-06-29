package kirillale.lakinais.bot

import kotlin.test.Test
import kotlin.test.assertTrue

class BotProcedureLabelsTest {

    @Test
    fun inlineButtonsFitTelegramLimit() {
        BotProcedureCatalog.all().forEach { option ->
            assertTrue(
                option.inlineButtonLabel(selected = false).length <= 64,
                "too long unselected: ${option.procedureSubtype}",
            )
            assertTrue(
                option.inlineButtonLabel(selected = true).length <= 64,
                "too long selected: ${option.procedureSubtype}",
            )
        }
    }

    @Test
    fun inlineLabelContainsFullSubtype() {
        val long = BotProcedureCatalog.all().first { it.procedureSubtype.contains("пальчиков") }
        val label = long.inlineButtonLabel(selected = false)
        assertTrue(label.contains("пальчиков"))
        assertTrue(label.contains("🦶"))
    }
}
