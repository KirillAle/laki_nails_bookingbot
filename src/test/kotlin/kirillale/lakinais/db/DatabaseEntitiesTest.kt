package kirillale.lakinais.db

import kirillale.lakinais.db.entities.*
import kirillale.lakinais.db.repositiries.*
import kirillale.lakinais.db.service.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseEntitiesTest {

    @Test
    fun testAccountRepository() {
        println("\n=== Тест AccountRepository ===")
        val repository = AccountRepository()
        
        // Создание аккаунта
        val account = repository.createAccount(
            telegramId = "test_telegram_${System.currentTimeMillis()}",
            firstName = "Test",
            lastName = "User",
            userName = "testuser",
            role = "CLIENT"
        )
        assertNotNull(account)
        println("✓ Создан аккаунт: ${account.id}")
        
        // Поиск по Telegram ID
        val found = repository.findByTelegramId(account.telegramId)
        assertNotNull(found)
        println("✓ Найден аккаунт по telegramId: ${found.id}")
        
        // Поиск по ID
        val foundById = repository.findById(account.id)
        assertNotNull(foundById)
        println("✓ Найден аккаунт по id: ${foundById.id}")
        
        // Поиск по роли
        val clients = repository.findByRole("CLIENT")
        assertTrue(clients.isNotEmpty())
        println("✓ Найдено клиентов: ${clients.size}")
    }

    @Test
    fun testProcedureRepository() {
        println("\n=== Тест ProcedureRepository ===")
        val repository = ProcedureRepository()
        
        // Создание процедуры
        val procedure = repository.createProcedure(
            procedureType = "Маникюр",
            procedureSubtype = "Классический",
            durationSlot = 2, // 2 таймслота = 30 минут
            price = 1500,
            isActive = true
        )
        assertNotNull(procedure)
        println("✓ Создана процедура: ${procedure.id} - ${procedure.procedureType}")
        
        // Поиск по ID
        val found = repository.findById(procedure.id)
        assertNotNull(found)
        println("✓ Найдена процедура по id: ${found.id}")
        
        // Поиск по типу
        val byType = repository.findByType("Маникюр")
        assertTrue(byType.isNotEmpty())
        println("✓ Найдено процедур типа 'Маникюр': ${byType.size}")
        
        // Поиск активных
        val active = repository.findActive()
        assertTrue(active.isNotEmpty())
        println("✓ Найдено активных процедур: ${active.size}")
        
        // Деактивация
        val deactivated = repository.updateActiveStatus(procedure.id, false)
        assertNotNull(deactivated)
        assertTrue(!deactivated.isActive)
        println("✓ Процедура деактивирована")
    }

    @Test
    fun testTimeSlotRepository() {
        println("\n=== Тест TimeSlotRepository ===")
        val repository = TimeSlotRepository()
        
        // Создание таймслота
        val timeSlot = repository.createTimeSlot(15)
        assertNotNull(timeSlot)
        println("✓ Создан таймслот: ${timeSlot.id} - ${timeSlot.minutes} минут")
        
        // Поиск по ID
        val found = repository.findById(timeSlot.id)
        assertNotNull(found)
        println("✓ Найден таймслот по id: ${found.id}")
        
        // Поиск по минутам
        val foundByMinutes = repository.findByMinutes(15)
        assertNotNull(foundByMinutes)
        println("✓ Найден таймслот по минутам: ${foundByMinutes.id}")
        
        // Получение всех
        val all = repository.findAll()
        assertTrue(all.isNotEmpty())
        println("✓ Всего таймслотов: ${all.size}")
    }

    @Test
    fun testMasterScheduleRepository() {
        println("\n=== Тест MasterScheduleRepository ===")
        val accountRepo = AccountRepository()
        val repository = MasterScheduleRepository()
        
        // Создаем мастера
        val master = accountRepo.createAccount(
            telegramId = "master_${System.currentTimeMillis()}",
            firstName = "Master",
            lastName = "Test",
            userName = "master",
            role = "MASTER"
        )
        
        // Создание расписания
        val now = Instant.now()
        val schedule = repository.createSchedule(
            masterId = master.id,
            date = now,
            timeStart = now,
            timeEnd = now.plusSeconds(3600), // +1 час
            breakStart = now.plusSeconds(1800), // +30 минут
            breakEnd = now.plusSeconds(2100) // +35 минут
        )
        assertNotNull(schedule)
        println("✓ Создано расписание: ${schedule.id}")
        
        // Поиск по ID
        val found = repository.findById(schedule.id)
        assertNotNull(found)
        println("✓ Найдено расписание по id: ${found.id}")
        
        // Поиск по мастеру
        val byMaster = repository.findByMasterId(master.id)
        assertTrue(byMaster.isNotEmpty())
        println("✓ Найдено расписаний мастера: ${byMaster.size}")
        
        // Поиск по мастеру и дате
        val byMasterAndDate = repository.findByMasterIdAndDate(master.id, now)
        assertTrue(byMasterAndDate.isNotEmpty())
        println("✓ Найдено расписаний на дату: ${byMasterAndDate.size}")
    }

    @Test
    fun testMasterProcedureRepository() {
        println("\n=== Тест MasterProcedureRepository ===")
        val accountRepo = AccountRepository()
        val procedureRepo = ProcedureRepository()
        val repository = MasterProcedureRepository()
        
        // Создаем мастера
        val master = accountRepo.createAccount(
            telegramId = "master_proc_${System.currentTimeMillis()}",
            firstName = "Master",
            lastName = "Proc",
            userName = "masterproc",
            role = "MASTER"
        )
        
        // Создаем процедуру
        val procedure = procedureRepo.createProcedure(
            procedureType = "Педикюр",
            procedureSubtype = "Классический",
            durationSlot = 3,
            price = 2000,
            isActive = true
        )
        
        // Создание связи мастер-процедура
        val masterProcedure = repository.createMasterProcedure(
            masterId = master.id,
            procedureId = procedure.id
        )
        assertNotNull(masterProcedure)
        println("✓ Создана связь мастер-процедура: ${masterProcedure.id}")
        
        // Поиск по ID
        val found = repository.findById(masterProcedure.id)
        assertNotNull(found)
        println("✓ Найдена связь по id: ${found.id}")
        
        // Поиск по мастеру
        val byMaster = repository.findByMasterId(master.id)
        assertTrue(byMaster.isNotEmpty())
        println("✓ Найдено процедур мастера: ${byMaster.size}")
        
        // Поиск по процедуре
        val byProcedure = repository.findByProcedureId(procedure.id)
        assertTrue(byProcedure.isNotEmpty())
        println("✓ Найдено мастеров для процедуры: ${byProcedure.size}")
        
        // Поиск по мастеру и процедуре
        val byBoth = repository.findByMasterIdAndProcedureId(master.id, procedure.id)
        assertNotNull(byBoth)
        println("✓ Найдена связь по мастеру и процедуре: ${byBoth.id}")
    }

    @Test
    fun testMasterTimeBlockRepository() {
        println("\n=== Тест MasterTimeBlockRepository ===")
        val accountRepo = AccountRepository()
        val repository = MasterTimeBlockRepository()
        
        // Создаем мастера
        val master = accountRepo.createAccount(
            telegramId = "master_block_${System.currentTimeMillis()}",
            firstName = "Master",
            lastName = "Block",
            userName = "masterblock",
            role = "MASTER"
        )
        
        // Создание блокировки времени
        val now = Instant.now()
        val timeBlock = repository.createTimeBlock(
            masterId = master.id,
            date = now,
            startTime = now.plusSeconds(7200), // +2 часа
            endTime = now.plusSeconds(10800), // +3 часа
            reason = "Обед"
        )
        assertNotNull(timeBlock)
        println("✓ Создана блокировка времени: ${timeBlock.id}")
        
        // Поиск по ID
        val found = repository.findById(timeBlock.id)
        assertNotNull(found)
        println("✓ Найдена блокировка по id: ${found.id}")
        
        // Поиск по мастеру
        val byMaster = repository.findByMasterId(master.id)
        assertTrue(byMaster.isNotEmpty())
        println("✓ Найдено блокировок мастера: ${byMaster.size}")
        
        // Поиск по мастеру и дате
        val byMasterAndDate = repository.findByMasterIdAndDate(master.id, now)
        assertTrue(byMasterAndDate.isNotEmpty())
        println("✓ Найдено блокировок на дату: ${byMasterAndDate.size}")
    }

    @Test
    fun testBookingRepository() {
        println("\n=== Тест BookingRepository ===")
        val accountRepo = AccountRepository()
        val scheduleRepo = MasterScheduleRepository()
        val procedureRepo = ProcedureRepository()
        val repository = BookingRepository()
        
        // Создаем клиента
        val client = accountRepo.createAccount(
            telegramId = "client_booking_${System.currentTimeMillis()}",
            firstName = "Client",
            lastName = "Booking",
            userName = "clientbooking",
            role = "CLIENT"
        )
        
        // Создаем мастера
        val master = accountRepo.createAccount(
            telegramId = "master_booking_${System.currentTimeMillis()}",
            firstName = "Master",
            lastName = "Booking",
            userName = "masterbooking",
            role = "MASTER"
        )
        
        // Создаем расписание
        val now = Instant.now()
        val schedule = scheduleRepo.createSchedule(
            masterId = master.id,
            date = now,
            timeStart = now,
            timeEnd = now.plusSeconds(3600),
            breakStart = now.plusSeconds(1800),
            breakEnd = now.plusSeconds(2100)
        )
        
        // Создаем процедуру
        val procedure = procedureRepo.createProcedure(
            procedureType = "Маникюр",
            procedureSubtype = "Классический",
            durationSlot = 2,
            price = 1500,
            isActive = true
        )
        
        // Создание бронирования
        val booking = repository.createBooking(
            clientId = client.id,
            scheduleId = schedule.id,
            procedureId = procedure.id,
            statusName = "PENDING",
            priceSnapshot = BigDecimal("1500.00"),
            startTime = now
        )
        assertNotNull(booking)
        println("✓ Создано бронирование: ${booking.id}")
        
        // Поиск по ID
        val found = repository.findById(booking.id)
        assertNotNull(found)
        println("✓ Найдено бронирование по id: ${found.id}")
        
        // Поиск по клиенту
        val byClient = repository.findByClientId(client.id)
        assertTrue(byClient.isNotEmpty())
        println("✓ Найдено бронирований клиента: ${byClient.size}")
        
        // Поиск по расписанию (вариант А: на один день может быть несколько бронирований)
        val bySchedule = repository.findByScheduleId(schedule.id)
        assertTrue(bySchedule.isNotEmpty())
        println("✓ Найдено бронирование по расписанию: ${bySchedule.first().id}")
        
        // Поиск по статусу
        val byStatus = repository.findByStatus("PENDING")
        assertTrue(byStatus.isNotEmpty())
        println("✓ Найдено бронирований со статусом PENDING: ${byStatus.size}")
        
        // Обновление статуса
        val updated = repository.updateStatus(booking.id, "CONFIRMED")
        assertNotNull(updated)
        assertTrue(updated.statusName == "CONFIRMED")
        println("✓ Статус обновлен на CONFIRMED")
    }

    @Test
    fun testAllServices() {
        println("\n=== Тест всех сервисов ===")
        
        val accountService = AccountService()
        val procedureService = ProcedureService()
        val timeSlotService = TimeSlotService()
        val masterScheduleService = MasterScheduleService()
        val masterProcedureService = MasterProcedureService()
        val masterTimeBlockService = MasterTimeBlockService()
        val bookingService = BookingService()
        
        // Тест AccountService
        val account = accountService.findOrCreateTelegramUser(
            telegramId = "service_test_${System.currentTimeMillis()}",
            firstName = "Service",
            lastName = "Test",
            userName = "servicetest"
        )
        assertNotNull(account)
        println("✓ AccountService: создан/найден аккаунт")
        
        // Тест ProcedureService
        val procedure = procedureService.createProcedure(
            procedureType = "Тест",
            procedureSubtype = "Тест",
            durationSlot = 1,
            price = 1000
        )
        assertNotNull(procedure)
        println("✓ ProcedureService: создана процедура")
        
        val activeProcedures = procedureService.getActiveProcedures()
        assertTrue(activeProcedures.isNotEmpty())
        println("✓ ProcedureService: найдено активных процедур: ${activeProcedures.size}")
        
        // Тест TimeSlotService
        val timeSlot = timeSlotService.getStandardTimeSlot()
        assertNotNull(timeSlot)
        println("✓ TimeSlotService: получен стандартный таймслот (15 мин)")
        
        // Тест MasterScheduleService
        val now = Instant.now()
        val schedule = masterScheduleService.createSchedule(
            masterId = account.id,
            date = now,
            timeStart = now,
            timeEnd = now.plusSeconds(3600),
            breakStart = now.plusSeconds(1800),
            breakEnd = now.plusSeconds(2100)
        )
        assertNotNull(schedule)
        println("✓ MasterScheduleService: создано расписание")
        
        // Тест MasterProcedureService
        val masterProc = masterProcedureService.createMasterProcedure(
            masterId = account.id,
            procedureId = procedure.id
        )
        assertNotNull(masterProc)
        println("✓ MasterProcedureService: создана связь мастер-процедура")
        
        // Тест MasterTimeBlockService
        val timeBlock = masterTimeBlockService.createTimeBlock(
            masterId = account.id,
            date = now,
            startTime = now.plusSeconds(7200),
            endTime = now.plusSeconds(10800),
            reason = "Тест"
        )
        assertNotNull(timeBlock)
        println("✓ MasterTimeBlockService: создана блокировка времени")
        
        // Тест BookingService
        val booking = bookingService.createBooking(
            clientId = account.id,
            scheduleId = schedule.id,
            procedureId = procedure.id,
            initialStatus = "PENDING",
            priceSnapshot = BigDecimal("1500.00"),
            startTime = now
        )
        assertNotNull(booking)
        println("✓ BookingService: создано бронирование")
        
        val confirmed = bookingService.confirmBooking(booking.id)
        assertNotNull(confirmed)
        println("✓ BookingService: бронирование подтверждено")
        
        println("\n=== Все тесты сервисов пройдены успешно ===")
    }
}
