-- 为 6-7 月所有工作日且无 attendance 记录的员工补上缺省记录
-- 签到 09:00 / 签退 18:00 / 位置马尾区上坂路

SET @address = '福建省福州市马尾区上坂路';

-- Step 0: 只有签退没有签到的 → 补上签到 09:00
UPDATE day.attendance
SET
  check_in_time = CAST(CONCAT(DATE_FORMAT(date, '%Y-%m-%d'), ' 09:00:00') AS DATETIME),
  check_in_address = @address,
  today_status = 'CHECKED_OUT'
WHERE check_in_time IS NULL AND check_out_time IS NOT NULL;
SELECT ROW_COUNT() AS 'fixed_checkout_only';

-- Step 1: 已有记录但签到签退都为空 → 更新
UPDATE day.attendance a
JOIN day.holiday h ON a.date = h.date AND h.type = 'WORKDAY'
SET
  a.check_in_time = CAST(CONCAT(DATE_FORMAT(a.date, '%Y-%m-%d'), ' 09:00:00') AS DATETIME),
  a.check_out_time = CAST(CONCAT(DATE_FORMAT(a.date, '%Y-%m-%d'), ' 18:00:00') AS DATETIME),
  a.check_in_address = @address,
  a.check_out_address = @address,
  a.today_status = 'CHECKED_OUT'
WHERE
  a.date BETWEEN '2026-06-01' AND CURDATE()
  AND a.check_in_time IS NULL
  AND a.check_out_time IS NULL;
SELECT ROW_COUNT() AS 'updated';

-- Step 2: 完全无记录的工作日 → 为每个员工插入一条（排除请假）
INSERT IGNORE INTO day.attendance(emp_id, date, check_in_time, check_out_time,
  check_in_address, check_out_address, today_status)
SELECT e.number, h.date,
  CAST(CONCAT(DATE_FORMAT(h.date, '%Y-%m-%d'), ' 09:00:00') AS DATETIME),
  CAST(CONCAT(DATE_FORMAT(h.date, '%Y-%m-%d'), ' 18:00:00') AS DATETIME),
  @address, @address, 'CHECKED_OUT'
FROM day.holiday h
CROSS JOIN day.emp e
WHERE h.date BETWEEN '2026-06-01' AND CURDATE()
  AND h.type = 'WORKDAY'
  AND NOT EXISTS (SELECT 1 FROM day.attendance a WHERE a.date = h.date AND a.emp_id = e.number)
  AND NOT EXISTS (  -- 排除请假
    SELECT 1 FROM day.leave l
    WHERE l.number = e.number AND l.status = '已批准'
      AND h.date >= DATE(l.start_date) AND h.date <= DATE(l.end_date)
  );
SELECT ROW_COUNT() AS 'inserted';
