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

CREATE TABLE IF NOT EXISTS `monthly_report` (
  `id` int AUTO_INCREMENT,
  `year_month` varchar(7) NOT NULL COMMENT '格式: YYYY-MM',
  `emp_id` int NOT NULL COMMENT '员工编号',
  `emp_name` varchar(50) DEFAULT NULL COMMENT '员工姓名',
  `dept_id` int DEFAULT NULL COMMENT '部门ID',
  `work_days` int DEFAULT 0 COMMENT '应出勤天数',
  `actual_days` int DEFAULT 0 COMMENT '实际出勤天数',
  `late_count` int DEFAULT 0,
  `early_count` int DEFAULT 0,
  `leave_count` int DEFAULT 0,
  `absence_count` int DEFAULT 0,
  `missing_card_count` int DEFAULT 0,
  `attendance_rate` decimal(5,2) DEFAULT 0.00 COMMENT '出勤率%',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_month` (`emp_id`, `year_month`),
  KEY `idx_dept_month` (`dept_id`, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='月度考勤统计表';
