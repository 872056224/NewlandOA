-- OA 系统 Phase 3: 2026年法定节假日数据
-- 数据来源：国务院办公厅《关于2026年部分节假日安排的通知》（国办发明电〔2025〕7号）
-- 执行方式: mysql -u root -p day < 2026-07-22-seed-holidays-2026.sql

USE `day`;

-- 清空已有数据（谨慎操作）
-- TRUNCATE TABLE day.holiday;

-- ========================================
-- 2026年节假日数据
-- 规则：所有数据按日期逐个插入
-- ========================================

-- 元旦：1月1日（周四）至1月3日（周六），1月4日（周日）上班
INSERT IGNORE INTO day.holiday(`date`, `type`, `description`, `year`) VALUES
('2026-01-01', 'HOLIDAY', '元旦', 2026),
('2026-01-02', 'HOLIDAY', '元旦', 2026),
('2026-01-03', 'HOLIDAY', '元旦', 2026),
('2026-01-04', 'WORKDAY', '元旦调休上班', 2026);

-- 春节：2月15日（周日，腊月廿八）至2月23日（周一，正月初七），2月14日（周六）、2月28日（周六）上班
INSERT IGNORE INTO day.holiday(`date`, `type`, `description`, `year`) VALUES
('2026-02-14', 'WORKDAY', '春节调休上班', 2026),
('2026-02-15', 'HOLIDAY', '春节（除夕）', 2026),
('2026-02-16', 'HOLIDAY', '春节（初一）', 2026),
('2026-02-17', 'HOLIDAY', '春节（初二）', 2026),
('2026-02-18', 'HOLIDAY', '春节（初三）', 2026),
('2026-02-19', 'HOLIDAY', '春节（初四）', 2026),
('2026-02-20', 'HOLIDAY', '春节（初五）', 2026),
('2026-02-21', 'HOLIDAY', '春节（初六）', 2026),
('2026-02-22', 'HOLIDAY', '春节（初七）', 2026),
('2026-02-23', 'HOLIDAY', '春节（初八）', 2026),
('2026-02-28', 'WORKDAY', '春节调休上班', 2026);

-- 清明节：4月4日（周六）至4月6日（周一），无调休
INSERT IGNORE INTO day.holiday(`date`, `type`, `description`, `year`) VALUES
('2026-04-04', 'HOLIDAY', '清明节', 2026),
('2026-04-05', 'HOLIDAY', '清明节', 2026),
('2026-04-06', 'HOLIDAY', '清明节', 2026);

-- 劳动节：5月1日（周五）至5月5日（周二），5月9日（周六）上班
INSERT IGNORE INTO day.holiday(`date`, `type`, `description`, `year`) VALUES
('2026-05-01', 'HOLIDAY', '劳动节', 2026),
('2026-05-02', 'HOLIDAY', '劳动节', 2026),
('2026-05-03', 'HOLIDAY', '劳动节', 2026),
('2026-05-04', 'HOLIDAY', '劳动节', 2026),
('2026-05-05', 'HOLIDAY', '劳动节', 2026),
('2026-05-09', 'WORKDAY', '劳动节调休上班', 2026);

-- 端午节：6月19日（周五）至6月21日（周日），无调休
INSERT IGNORE INTO day.holiday(`date`, `type`, `description`, `year`) VALUES
('2026-06-19', 'HOLIDAY', '端午节', 2026),
('2026-06-20', 'HOLIDAY', '端午节', 2026),
('2026-06-21', 'HOLIDAY', '端午节', 2026);

-- 中秋节：9月25日（周五）至9月27日（周日），无调休
INSERT IGNORE INTO day.holiday(`date`, `type`, `description`, `year`) VALUES
('2026-09-25', 'HOLIDAY', '中秋节', 2026),
('2026-09-26', 'HOLIDAY', '中秋节', 2026),
('2026-09-27', 'HOLIDAY', '中秋节', 2026);

-- 国庆节：10月1日（周四）至10月7日（周三），9月20日（周日）、10月10日（周六）上班
INSERT IGNORE INTO day.holiday(`date`, `type`, `description`, `year`) VALUES
('2026-09-20', 'WORKDAY', '国庆节调休上班', 2026),
('2026-10-01', 'HOLIDAY', '国庆节', 2026),
('2026-10-02', 'HOLIDAY', '国庆节', 2026),
('2026-10-03', 'HOLIDAY', '国庆节', 2026),
('2026-10-04', 'HOLIDAY', '国庆节', 2026),
('2026-10-05', 'HOLIDAY', '国庆节', 2026),
('2026-10-06', 'HOLIDAY', '国庆节', 2026),
('2026-10-07', 'HOLIDAY', '国庆节', 2026),
('2026-10-10', 'WORKDAY', '国庆节调休上班', 2026);

-- 计算所有周末日期设为 REST_DAY（排除已配置的 HOLIDAY 和 WORKDAY）
-- 使用存储过程生成完整年历
DELIMITER $$
DROP PROCEDURE IF EXISTS fill_year_calendar$$
CREATE PROCEDURE fill_year_calendar(target_year INT)
BEGIN
    DECLARE current_date DATE;
    DECLARE end_date DATE;
    DECLARE day_type VARCHAR(20);
    
    SET current_date = DATE(CONCAT(target_year, '-01-01'));
    SET end_date = DATE(CONCAT(target_year, '-12-31'));
    
    WHILE current_date <= end_date DO
        -- 只在 holiday 表中没有该日期记录时插入
        IF NOT EXISTS (SELECT 1 FROM day.holiday WHERE `date` = current_date) THEN
            -- 判断周末
            IF DAYOFWEEK(current_date) = 1 THEN -- 周日
                SET day_type = 'REST_DAY';
                INSERT IGNORE INTO day.holiday(`date`, `type`, `description`, `year`)
                VALUES (current_date, day_type, '周末', target_year);
            ELSEIF DAYOFWEEK(current_date) = 7 THEN -- 周六
                SET day_type = 'REST_DAY';
                INSERT IGNORE INTO day.holiday(`date`, `type`, `description`, `year`)
                VALUES (current_date, day_type, '周末', target_year);
            ELSE
                SET day_type = 'WORKDAY';
                INSERT IGNORE INTO day.holiday(`date`, `type`, `description`, `year`)
                VALUES (current_date, day_type, '工作日', target_year);
            END IF;
        END IF;
        
        SET current_date = DATE_ADD(current_date, INTERVAL 1 DAY);
    END WHILE;
END$$
DELIMITER ;

-- 执行填充
CALL fill_year_calendar(2026);

-- 清理
DROP PROCEDURE IF EXISTS fill_year_calendar;

-- 验证数据
SELECT `type`, COUNT(*) as cnt FROM day.holiday WHERE `year`=2026 GROUP BY `type`;
