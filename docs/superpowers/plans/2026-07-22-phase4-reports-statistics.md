# Phase 4: Scheduled Reports & Monthly Statistics — Implementation Plan

**Goal:** Add daily report generation (01:00), monthly report generation (1st of month), and personal/department monthly statistics APIs.

**Architecture:** OA-7 admin service. Two new DB tables. Scheduled tasks in OA-7.

## Global Constraints
- All scheduled tasks run in OA-7
- Statistics computed from `day.attendance` table
- Personal stats for employees accessible via employee API
- Department stats for admins via admin API

---

### Task 1: DB Tables + Scheduled Daily Report

**Files:**
- Create: `docs/sql/2026-07-22-create-report-tables.sql`
- Create: `OA-7/.../pojo/DailyReport.java`
- Create: `OA-7/.../dao/DailyReportDao.java`
- Create: `OA-7/.../service/DailyReportService.java`

**SQL:**
```sql
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
);

CREATE TABLE IF NOT EXISTS `monthly_report` (
  `id` int AUTO_INCREMENT,
  `year_month` varchar(7) NOT NULL,
  `emp_id` int NOT NULL,
  `work_days` int DEFAULT 0,
  `actual_days` int DEFAULT 0,
  `late_count` int DEFAULT 0,
  `early_count` int DEFAULT 0,
  `leave_count` int DEFAULT 0,
  `absence_count` int DEFAULT 0,
  `missing_card_count` int DEFAULT 0,
  `attendance_rate` decimal(5,2) DEFAULT 0.00,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_month` (`emp_id`, `year_month`)
);
```

**DailyReportService:** Scheduled at 0 0 1 * * ? (01:00 daily). Queries yesterday's attendance records, counts by attendance_status, calculates rate.

### Task 2: Monthly Report Generation

**Files:**
- Create: `OA-7/.../pojo/MonthlyReport.java`
- Create: `OA-7/.../dao/MonthlyReportDao.java`
- Create: `OA-7/.../service/MonthlyReportService.java`

**Logic:** On 1st of month at 06:00:
- Count work days in last month (exclude weekends/holidays)
- For each employee, count attendance by status
- Calculate attendance rate
- Insert into monthly_report

### Task 3: Personal + Department Statistics API

**Files:**
- Create: `OA-7/.../controller/StatisticsController.java`
- Create: `OA-7/.../service/StatisticsService.java` + `Impl`

**Endpoints:**
- `GET /statistics/personal?empId=&yearMonth=` — personal monthly stats
- `GET /statistics/department?deptId=&yearMonth=` — department aggregated stats

### Task 4: Statistics Frontend Page

**Files:**
- Modify: `frontend/src/components/admin/SignStatistics.vue` (enhance with monthly view)
- Or create new statistics dashboard page
