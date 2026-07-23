-- 对工作日且 ≤ 今天的 attendance 记录中，check_in/check_out 都为空（完全没打卡）的
-- 填充 09:00-18:00 签到签退，位置马尾区上坂路

SET @address = '福建省福州市马尾区上坂路';

UPDATE day.attendance a
JOIN day.holiday h ON a.date = h.date AND h.type = 'WORKDAY'
SET
  a.check_in_time = CONCAT(a.date, ' 09:00:00'),
  a.check_out_time = CONCAT(a.date, ' 18:00:00'),
  a.check_in_address = @address,
  a.check_out_address = @address,
  a.today_status = 'CHECKED_OUT'
WHERE
  a.date <= CURDATE()
  AND a.check_in_time IS NULL
  AND a.check_out_time IS NULL;

SELECT ROW_COUNT() AS 'affected_rows';
