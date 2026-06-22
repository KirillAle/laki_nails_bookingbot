-- Тестовое открытие записи: 14 рабочих дней вперёд (Asia/Tbilisi)
-- Рабочий день: 10:00–19:00, перерыв 14:00–15:00
--
-- Перед запуском:
-- 1) Убедитесь, что есть аккаунт с role = 'MASTER'
-- 2) Выполните 001_add_phone_to_account.sql (если ещё не делали)
--
-- Запуск: выполните этот файл в psql / DBeaver / pgAdmin

WITH master AS (
    SELECT id FROM account_form WHERE role = 'MASTER' ORDER BY created_at LIMIT 1
),
days AS (
    SELECT generate_series(
        (timezone('Asia/Tbilisi', now()))::date,
        (timezone('Asia/Tbilisi', now()))::date + 14,
        interval '1 day'
    )::date AS work_date
)
INSERT INTO master_schedule (id, master_id, date, time_start, time_end, break_start, break_end)
SELECT
    gen_random_uuid(),
    master.id,
    (work_date::timestamp AT TIME ZONE 'Asia/Tbilisi'),
    ((work_date + time '10:00')::timestamp AT TIME ZONE 'Asia/Tbilisi'),
    ((work_date + time '19:00')::timestamp AT TIME ZONE 'Asia/Tbilisi'),
    ((work_date + time '14:00')::timestamp AT TIME ZONE 'Asia/Tbilisi'),
    ((work_date + time '15:00')::timestamp AT TIME ZONE 'Asia/Tbilisi')
FROM days
CROSS JOIN master
WHERE master.id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM master_schedule ms
    WHERE ms.master_id = master.id
      AND (ms.date AT TIME ZONE 'Asia/Tbilisi')::date = days.work_date
);

-- Проверка
SELECT
    ms.id,
    (ms.date AT TIME ZONE 'Asia/Tbilisi')::date AS day,
    (ms.time_start AT TIME ZONE 'Asia/Tbilisi')::time AS work_from,
    (ms.time_end AT TIME ZONE 'Asia/Tbilisi')::time AS work_to
FROM master_schedule ms
JOIN account_form af ON af.id = ms.master_id
WHERE af.role = 'MASTER'
ORDER BY day;
