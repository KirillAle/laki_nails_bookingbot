-- День закрыт для новых записей, но строка расписания сохраняется (история бронирований).
ALTER TABLE master_schedule
    ADD COLUMN IF NOT EXISTS is_open BOOLEAN NOT NULL DEFAULT TRUE;
