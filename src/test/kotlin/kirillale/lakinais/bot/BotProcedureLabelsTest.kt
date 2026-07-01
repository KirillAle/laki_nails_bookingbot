package kirillale.lakinais.bot

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BotProcedureLabelsTest {

    @Test
    fun inlineButtonsFitTelegramLimit() {
        BotProcedureCatalog.all().forEach { option ->
            assertTrue(
                option.inlineButtonLabel(selected = false).length <= 64,
                "too long: ${option.keyboardLabel}",
            )
            assertTrue(
                option.inlineButtonLabel(selected = true).length <= 64,
                "too long selected: ${option.keyboardLabel}",
            )
        }
    }

    @Test
    fun inlineLabelsAreSingleLine() {
        BotProcedureCatalog.all().forEach { option ->
            assertTrue(
                !option.keyboardLabel.contains('\n'),
                "multiline label: ${option.keyboardLabel}",
            )
        }
    }

    @Test
    fun inlineLabelsDoNotShowDuration() {
        BotProcedureCatalog.all().forEach { option ->
            assertFalse(
                option.keyboardLabel.any { it.isDigit() },
                "duration in label: ${option.keyboardLabel}",
            )
        }
    }
}
