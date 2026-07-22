-- OA 系统 Phase 4: 日报统计 - 数据库迁移脚本
-- 执行方式: mysql -u root -p day < 2026-07-22-create-report-tables.sql

USE `day`;

CREATE TABLE IF NOT EXISTS `daily_report` (
  `id` int AUTO_INCREMENT,
  `report_date` date NOT NULL,
  `total_employees` int DEFAULT 0,
  `normal_count` int DEFAULT 0,
  `late_count` int DEFAULT 0,
  `early_count` int DEFAULT 0,
  `late_early_count` int DEFAULT 0,
  `leave_count` int DEFAULT 0,
  `absence_count` int DEFAULT 0,
  `missing_card_count` int DEFAULT 0,
  `holiday_count` int DEFAULT 0,
  `attendance_rate` decimal(5,2) DEFAULT 0.00,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date` (`report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='日报统计表';
