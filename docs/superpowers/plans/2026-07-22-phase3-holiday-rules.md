# Phase 3: Holiday & AttendanceRule Module — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement task-by-task.

**Goal:** Build complete Holiday management (with China statutory holiday calendar) and configurable AttendanceRule system.

**Architecture:** OA-7 admin service hosts Holiday + AttendanceRule CRUD. Holiday data stored in `day.holiday` table. AttendanceRule in `day.attendance_rule`.

**Tech Stack:** Spring Boot 2.7.18, JDK 21, MyBatis, MySQL 8, Vue 3 + Element Plus

## Global Constraints
- Holiday data for 2026 is complete and sourced from official State Council notice (国办发明电〔2025〕7号)
- 2027 data not yet published — admin can add manually via UI
- Holiday types: WORKDAY (调休上班), HOLIDAY (法定假日), REST_DAY (周末休息)
- AttendanceRule supports per-department override of global defaults

---

### Task 1: Holiday Entity + DAO + SQL Seed Data

**Files:**
- Modify: `OA-7/.../pojo/Holiday.java` (create if not exists)
- Modify: `OA-7/.../dao/HolidayDao.java` (add CRUD methods)
- Create: `docs/sql/2026-07-22-seed-holidays-2026.sql`

**What:** Complete Holiday entity with full CRUD DAO. Generate SQL seed data for 2026 all holiday/workday/rest_day entries.

- [ ] Create/update `Holiday.java` with: date (LocalDate), type (String: WORKDAY/HOLIDAY/REST_DAY), description (String), year (Integer)
- [ ] Add to HolidayDao: insert, batchInsert, selectByYear, selectByDateRange, update, delete
- [ ] Generate SQL seed script for 2026 (all 365 days marked as REST_DAY/WORKDAY/HOLIDAY)

### Task 2: Holiday Service + Controller (OA-7)

**Files:**
- Create: `OA-7/.../service/HolidayService.java`
- Create: `OA-7/.../service/Impl/HolidayServiceImpl.java`
- Create: `OA-7/.../controller/HolidayController.java`

**API:**
- `GET /holidays/year/{year}` — get all holidays for a year
- `GET /holidays/range?start=&end=` — get holidays in date range
- `PUT /holidays/{date}` — update a single day's type
- `POST /holidays/batch` — batch import
- `GET /holidays/calendar/{year}` — full year calendar (all days with type)

### Task 3: Holiday Admin Frontend Page

**Files:**
- Create: `frontend/src/components/admin/HolidayManage.vue`
- Modify: `frontend/src/router/index.ts` (add route)
- Modify: `frontend/src/components/admin/AdminHome.vue` (add menu item)

**Features:**
- Year selector (default current year)
- Calendar grid showing all months
- Color coding: GREEN=HOLIDAY, RED=REST_DAY, BLUE=WORKDAY
- Click to toggle between HOLIDAY ↔ WORKDAY ↔ REST_DAY
- Batch import button (pre-fills 2026 data from API)

### Task 4: AttendanceRule Entity + DAO + Service

**Files:**
- Create: `OA-7/.../pojo/AttendanceRule.java`
- Create: `OA-7/.../dao/AttendanceRuleDao.java`
- Create: `OA-7/.../service/AttendanceRuleService.java` + `Impl`
- Create: `OA-7/.../controller/AttendanceRuleController.java`

**Model:** id, ruleName, deptId (nullable=global), workStartTime, workEndTime, lateThresholdMin, earlyThresholdMin, enabled

### Task 5: AttendanceRule Frontend Page

**Files:**
- Create: `frontend/src/components/admin/AttendanceRuleManage.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/admin/AdminHome.vue`

### Task 6: Use Holiday Data in RecalculateAttendanceService

**Files:**
- Modify: `OA-7/.../service/RecalculateAttendanceService.java`

**What:** Replace string comparison with HolidayService check for holiday type determination. Already works via `holidayDao.selectHolidayTypeByDate`, just ensure it uses the enriched Holiday data.
