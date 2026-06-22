package kirillale.lakinais.db

import kirillale.lakinais.bot.BotProcedureCatalog
import kirillale.lakinais.db.service.ProcedureService

/**
 * Создаёт в БД 8 процедур из каталога бота, если их ещё нет (по паре type+subtype).
 */
fun seedProceduresIfNeeded() {
    val procedureService = ProcedureService()
    for (option in BotProcedureCatalog.all()) {
        val existing = procedureService.getByTypeAndSubtype(option.procedureType, option.procedureSubtype)
        if (existing == null) {
            procedureService.createProcedure(
                procedureType = option.procedureType,
                procedureSubtype = option.procedureSubtype,
                durationSlot = option.durationSlots,
                price = 0,
                isActive = true
            )
        }
    }
}
