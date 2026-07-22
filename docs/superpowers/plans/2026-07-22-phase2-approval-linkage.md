# Phase 2: Approval Linkage & Status Recalculation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement task-by-task.

**Goal:** Connect OA-7 admin approval flows (leave, retroactive sign, makeup approval/revoke) to `RecalculateAttendanceService`, add admin dashboard statistics APIs, and implement approval revocation.

**Architecture:** Two services (OA-2 employee, OA-7 admin) sharing MySQL `day` database. All approval logic lives in OA-7. `RecalculateAttendanceService` (created in Phase 1) is the single recalculation entry point.

**Tech Stack:** Spring Boot 2.7.18, JDK 21, MyBatis, MySQL 8, Nacos

## Global Constraints

- All approval/revoke operations MUST call `RecalculateAttendanceService.recalculate()` after success
- Use existing `RecalculateAttendanceService` from Phase 1 (do not duplicate logic)
- OA-2's `MakeupRequest` entity and DAO can be leveraged (they share the same `day.makeup_request` table)
- Optimistic locking (`version` column) must be used for all approval operations
- Notifications must be sent on approval/revoke events
- All API responses use the existing `RESP` format (`{code, data, total?, message?}`)

---

### Task 1: Leave Approval Triggers RecalculateAttendanceService

**Files:**
- Modify: `OA-7/.../service/Impl/LeaveServiceImpl.java`

**What:** After `approve(id)` succeeds, call `recalculateAttendanceService.recalculate(empId, startDate, endDate)`. After `reject(id)` succeeds, clear `today_status=LEAVE` from attendance records and recalculate.

- [ ] Inject `RecalculateAttendanceService` into `LeaveServiceImpl`
- [ ] In `approve()`, after optimistic lock success, parse leave's start/end date, call `recalculate(empId, startDate, endDate)`
- [ ] In `reject()`, after optimistic lock success, clear `today_status` from attendance records for the leave date range, then recalculate
- [ ] Compile OA-7: `mvn compile -pl OA-7 -am`

---

### Task 2: Retroactive Sign Approval Triggers RecalculateAttendanceService

**Files:**
- Modify: `OA-7/.../service/Impl/RetroactiveSignServiceImpl.java`

**What:** After `approve(id)`, update the attendance record's check-in or check-out time (based on retroactive type), then recalculate. After `reject(id)`, no attendance change needed (notify only).

- [ ] Inject `RecalculateAttendanceService` and `AttendanceDao`
- [ ] In `approve()`, after optimistic lock:
  - Query `Attendance` by empId + sign_date
  - If type='a' (AM), set `check_in_time` to the day's 09:00
  - If type='p' (PM), set `check_out_time` to the day's 18:00
  - Update Attendance record via `AttendanceDao`
  - Call `recalculate(empId, signDate)`
- [ ] In `reject()`, just notify (no attendance change)
- [ ] Compile OA-7

---

### Task 3: Add MakeupRequest Approval in OA-7

**Files:**
- Create: `OA-7/.../controller/MakeupRequestController.java`
- Create: `OA-7/.../pojo/MakeupRequest.java` (if not exist — leverage OA-2 model)
- Create: `OA-7/.../dao/MakeupRequestDao.java`
- Create: `OA-7/.../service/MakeupRequestService.java` + `Impl`
- Modify: `OA-7/.../service/Impl/RetroactiveSignServiceImpl.java` or separate service

**What:** Add complete approval flow for MakeupRequest (补卡) in OA-7 admin service.

- [ ] Create `MakeupRequest.java` pojo matching OA-2's version but in OA-7 package
- [ ] Create `MakeupRequestDao.java` with: `selectPending()`, `selectById()`, `updateStatusWithVersion()`
- [ ] Create `MakeupRequestService` + `Impl` with: `getPending(page,size)`, `approve(id)`, `reject(id)`
- [ ] In `approve()`: update the attendance record's `check_in_time` or `check_out_time` based on makeup type, then recalculate
- [ ] Create `MakeupRequestController.java` with: `GET /pending`, `PUT /{id}/approve`, `PUT /{id}/reject`
- [ ] Compile OA-7

---

### Task 4: Approval Revoke (Leave + Retroactive)

**Files:**
- Modify: `OA-7/.../controller/LeaveController.java`
- Modify: `OA-7/.../controller/RetroactiveSignController.java`
- Modify: `OA-7/.../controller/MakeupRequestController.java` (if created)

**What:** Add `PUT /leave/{id}/revoke`, `PUT /attendance/retroactive/{id}/revoke`, `PUT /makeup/{id}/revoke` endpoints.

Revoke logic:
- Only ALLOWED status can be revoked (已批准 → revoked back to 待审批)
- On revoke: restore the attendance record to its pre-approval state, then recalculate
- Send notification to the employee
- Mark admin notifications as read

- [ ] Add `revoke(id)` method to `LeaveServiceImpl`:
  - Check status is "已批准"
  - Update status back to "待审批" with version check
  - Clear `today_status=LEAVE` from attendance for date range
  - Call `recalculate(empId, startDate, endDate)`
  - Notify employee
- [ ] Add `PUT /leave/{id}/revoke` controller endpoint
- [ ] Add `revoke(id)` to `RetroactiveSignServiceImpl`:
  - Reverse the check-in/check-out update
  - Call recalculate
- [ ] Add `PUT /attendance/retroactive/{id}/revoke` endpoint
- [ ] Compile OA-7

---

### Task 5: Admin Dashboard — Today's Real-time Statistics

**Files:**
- Modify: `OA-7/.../controller/SignController.java`
- Modify: `OA-7/.../dao/AttendanceDao.java`
- Modify: `OA-7/.../controller/AdmController.java` (or new endpoint)

**What:** Add `GET /attendance/today/realtime-stats` returning comprehensive today's stats for admin dashboard.

- [ ] Add DAO methods:
  - `countNotCheckedInByDate(date)` — `today_status='NOT_CHECKED_IN'`
  - `countMakeupPendingByDate(date)` — total pending makeup requests for today
  - `countByTodayStatus(date, status)` — generic count by today_status
- [ ] Add endpoint returning: `{ total, checkedIn, notCheckedIn, late, onLeave, makeupPending }`
- [ ] Compile OA-7

---

### Task 6: Admin Dashboard — Yesterday's Statistics

**Files:**
- Modify: `OA-7/.../controller/SignController.java`
- Modify: `OA-7/.../dao/AttendanceDao.java`

**What:** Add `GET /attendance/yesterday/stats` returning yesterday's final status distribution.

- [ ] Add DAO method:
  - `countGroupByStatus(date)` — `SELECT attendance_status, COUNT(*) FROM day.attendance WHERE date=#{date} GROUP BY attendance_status`
- [ ] Add endpoint returning: `{ date, normal, late, early, lateEarly, leave, dayOff, absence, missingCard, holiday }`
- [ ] Compile OA-7
