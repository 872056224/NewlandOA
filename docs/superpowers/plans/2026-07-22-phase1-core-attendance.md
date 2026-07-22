# Phase 1: Core Attendance System Enhancement — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the attendance system with proper enums, unified recalculation service, and correct auto-creation of daily attendance records.

**Architecture:** Two independent Spring Boot services (OA-2 employee, OA-7 admin) sharing MySQL `day` database. Changes must be made in parallel where possible.

**Tech Stack:** Spring Boot 2.7.18, JDK 21, MyBatis, MySQL 8, Nacos, Elasticsearch

## Global Constraints

- All status fields use `VARCHAR` in DB storing enum `.name()` for backward compatibility
- OA-2 and OA-7 both need enum classes; shared location preferred but if not possible, duplicate
- All new services must be unit-testable
- Existing API contracts must not break (backward compatible)
- `RecalculateAttendanceService` is the single source of truth for final status determination
- All timestamps in `LocalDate`/`LocalDateTime` (java.time package)

---

### Task 1: Create Status Enum Classes

**Files:**
- Create: `OA-2/src/main/java/com/oa2/constant/TodayStatus.java`
- Create: `OA-2/src/main/java/com/oa2/constant/AttendanceStatus.java`
- Create: `OA-2/src/main/java/com/oa2/constant/HolidayType.java`
- Create: `OA-7/src/main/java/com/oa7/constant/TodayStatus.java`
- Create: `OA-7/src/main/java/com/oa7/constant/AttendanceStatus.java`
- Create: `OA-7/src/main/java/com/oa7/constant/HolidayType.java`

**Interfaces:**
- Consumes: nothing (standalone utility)
- Produces: `TodayStatus` (enum: NOT_CHECKED_IN, CHECKED_IN, CHECKED_OUT, LEAVE_PENDING, LEAVE, MAKEUP_PENDING, DAY_OFF, BUSINESS_PENDING, FIELD_PENDING)
- Produces: `AttendanceStatus` (enum: NORMAL, LATE, EARLY, LATE_EARLY, LEAVE, DAY_OFF, BUSINESS_TRIP, FIELD_WORK, MISSING_CARD, ABSENCE, HOLIDAY, REST_DAY)
- Produces: `HolidayType` (enum: WORKDAY, HOLIDAY, REST_DAY)

- [ ] **Step 1: Create TodayStatus.java (OA-2)**

```java
package com.oa2.constant;

/**
 * 实时状态 — 仅用于当天展示，不参与统计
 */
public enum TodayStatus {
    NOT_CHECKED_IN("未签到"),
    CHECKED_IN("已签到"),
    CHECKED_OUT("已签退"),
    LEAVE_PENDING("请假审批中"),
    LEAVE("已请假"),
    MAKEUP_PENDING("补卡审批中"),
    DAY_OFF("调休"),
    BUSINESS_PENDING("出差审批中"),
    FIELD_PENDING("外勤审批中");

    private final String displayName;

    TodayStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

- [ ] **Step 2: Create AttendanceStatus.java (OA-2)**

```java
package com.oa2.constant;

/**
 * 最终状态 — 用于统计、报表、工资计算
 * 优先级（高→低）：
 * HOLIDAY > REST_DAY > LEAVE > DAY_OFF > BUSINESS_TRIP > FIELD_WORK > NORMAL > LATE > EARLY > MISSING_CARD > ABSENCE
 */
public enum AttendanceStatus {
    NORMAL("正常"),
    LATE("迟到"),
    EARLY("早退"),
    LATE_EARLY("迟到早退"),
    LEAVE("请假"),
    DAY_OFF("调休"),
    BUSINESS_TRIP("出差"),
    FIELD_WORK("外勤"),
    MISSING_CARD("缺卡"),
    ABSENCE("旷工"),
    HOLIDAY("节假日"),
    REST_DAY("休息日");

    private final String displayName;

    AttendanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

- [ ] **Step 3: Create HolidayType.java (OA-2)**

```java
package com.oa2.constant;

public enum HolidayType {
    WORKDAY("工作日"),
    HOLIDAY("节假日"),
    REST_DAY("休息日");

    private final String displayName;

    HolidayType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

- [ ] **Step 4: Create identical copies for OA-7**

Create the same three files under `com.oa7.constant` package in OA-7 service.

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -pl OA-2 -am` and `mvn compile -pl OA-7 -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add status enum classes (TodayStatus, AttendanceStatus, HolidayType)"
```

---

### Task 2: Modify Attendance Entity to Use Enums

**Files:**
- Modify: `OA-2/src/main/java/com/oa2/pojo/Attendance.java`
- Modify: `OA-7/src/main/java/com/oa7/pojo/Attendance.java`
- Create: `OA-2/src/main/java/com/oa2/config/MyBatisEnumTypeHandler.java`
- Create: `OA-7/src/main/java/com/oa7/config/MyBatisEnumTypeHandler.java`
- Modify: `OA-2/src/main/resources/application.yml`
- Modify: `OA-7/src/main/resources/application.yml`

**Interfaces:**
- Consumes: Task 1 enum classes
- Produces: Updated Attendance entity with typed enum fields

- [ ] **Step 1: Create MyBatis Enum TypeHandler**

Create `OA-2/src/main/java/com/oa2/config/MyBatisEnumTypeHandler.java`:

```java
package com.oa2.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler for enums stored as VARCHAR (using enum.name())
 */
public class MyBatisEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> type;

    public MyBatisEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : Enum.valueOf(type, value);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : Enum.valueOf(type, value);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : Enum.valueOf(type, value);
    }
}
```

Create identical copy for OA-7 under `com.oa7.config`.

- [ ] **Step 2: Update Attendance.java (OA-2)**

Change field types:
```java
// Before:
private String todayStatus;
private String attendanceStatus;

// After:
private TodayStatus todayStatus;
private AttendanceStatus attendanceStatus;
```

- [ ] **Step 3: Update AttendanceDao.java (OA-2) — add type handling**

Add `@Results` annotations to specify enum type handlers:
```java
@Results(id = "attendanceMap", value = {
    @Result(property = "id", column = "id"),
    @Result(property = "empId", column = "emp_id"),
    @Result(property = "date", column = "date"),
    @Result(property = "checkInTime", column = "check_in_time"),
    @Result(property = "checkOutTime", column = "check_out_time"),
    @Result(property = "todayStatus", column = "today_status",
            typeHandler = MyBatisEnumTypeHandler.class),
    @Result(property = "attendanceStatus", column = "attendance_status",
            typeHandler = MyBatisEnumTypeHandler.class),
    @Result(property = "checkInAddress", column = "check_in_address"),
    @Result(property = "checkOutAddress", column = "check_out_address"),
    @Result(property = "remark", column = "remark"),
    @Result(property = "createdAt", column = "created_at"),
    @Result(property = "updatedAt", column = "updated_at")
})
```

- [ ] **Step 4: Fix existing DAO methods that use string comparisons**

In `AttendanceDao.java` (OA-2), the checkIn/checkOut methods use string 'CHECKED_IN', 'CHECKED_OUT'. These need to be updated to use `TodayStatus.CHECKED_IN.name()` or kept as strings since they're SQL literals.

The SQL strings like `today_status='CHECKED_OUT'` are fine — they compare against the VARCHAR value in DB, which stores `enum.name()`.

But the Java code in `AttendanceServiceImpl.java` that does:
```java
if ("CHECKED_IN".equals(existing.getTodayStatus()))
```
Needs to change to:
```java
if (TodayStatus.CHECKED_IN == existing.getTodayStatus())
```

- [ ] **Step 5: Update AttendanceServiceImpl.java (OA-2)**

Update all string comparisons to use enum equality:

```java
// Before:
if ("CHECKED_IN".equals(existing.getTodayStatus())) {
// After:
if (TodayStatus.CHECKED_IN == existing.getTodayStatus()) {

// Before:
if ("CHECKED_OUT".equals(existing.getTodayStatus())) {
// After:
if (TodayStatus.CHECKED_OUT == existing.getTodayStatus()) {

// Before:
if ("LEAVE".equals(existing.getTodayStatus())) {
// After:
if (TodayStatus.LEAVE == existing.getTodayStatus()) {
```

Also update `checkIn()` SQL today_status parameter from `'CHECKED_IN'` to `TodayStatus.CHECKED_IN.name()` via parameter.

- [ ] **Step 6: Repeat for OA-7**

Apply same changes to OA-7's `Attendance.java`, `AttendanceDao.java`, and any service files.

- [ ] **Step 7: Verify compilation**

```bash
mvn compile -pl OA-2 -am
mvn compile -pl OA-7 -am
```

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor: change Attendance entity status fields to enum types"
```

---

### Task 3: Create RecalculateAttendanceService

**Files:**
- Create: `OA-7/src/main/java/com/oa7/service/RecalculateAttendanceService.java`
- Modify: `OA-7/src/main/java/com/oa7/dao/AttendanceDao.java`
- Modify: `OA-7/src/main/java/com/oa7/dao/HolidayDao.java` (if needed)

**Interfaces:**
- Consumes: Attendance, TodayStatus, AttendanceStatus, HolidayType enums
- Produces: `recalculate(empId, date)` → AttendanceStatus
- Produces: `recalculate(empId, startDate, endDate)` → Map<LocalDate, AttendanceStatus>

- [ ] **Step 1: Add required DAO methods to AttendanceDao (OA-7)**

Add methods needed by the recalculation service:

```java
// Update todayStatus by empId and date
@Update("UPDATE day.attendance SET today_status=#{todayStatus} WHERE emp_id=#{empId} AND date=#{date}")
int updateTodayStatusByEmpAndDate(@Param("empId") int empId, @Param("date") LocalDate date,
                                   @Param("todayStatus") String todayStatus);

// Update check-in/check-out times
@Update("UPDATE day.attendance SET check_in_time=#{checkInTime}, check_out_time=#{checkOutTime} " +
        "WHERE emp_id=#{empId} AND date=#{date}")
int updateCheckTime(Attendance attendance);

// Insert with ON DUPLICATE KEY (for cases where attendance record doesn't exist yet)
@Insert("INSERT INTO day.attendance(emp_id, date, today_status) VALUES(#{empId}, #{date}, #{todayStatus}) " +
        "ON DUPLICATE KEY UPDATE today_status=#{todayStatus}")
int insertOrUpdate(@Param("empId") int empId, @Param("date") LocalDate date,
                   @Param("todayStatus") String todayStatus);
```

- [ ] **Step 2: Create RecalculateAttendanceService.java**

```java
package com.oa7.service;

import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.HolidayType;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.LeaveDao;
import com.oa7.pojo.Attendance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 考勤重算服务 — 单一事实来源
 * 
 * 所有审批通过、审批撤销、补卡/补签成功后必须调用此服务。
 * 按状态优先级确定最终考勤状态。
 */
@Service
public class RecalculateAttendanceService {

    private static final Logger log = LoggerFactory.getLogger(RecalculateAttendanceService.class);

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private HolidayDao holidayDao;

    @Autowired
    private LeaveDao leaveDao;

    /** 默认上班时间 09:00 */
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    /** 默认下班时间 18:00 */
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);

    /**
     * 对指定员工+单日进行考勤重算
     * @return 计算后的最终状态，如果无记录返回 null
     */
    public AttendanceStatus recalculate(int empId, LocalDate date) {
        Attendance att = attendanceDao.selectByEmpAndDate(empId, date);
        if (att == null) {
            log.warn("考勤重算：员工 {} 在 {} 无考勤记录，跳过", empId, date);
            return null;
        }

        // 查询 Holiday 类型
        String holidayTypeStr = holidayDao.selectHolidayTypeByDate(date);
        HolidayType holidayType = null;
        if (holidayTypeStr != null) {
            try {
                holidayType = HolidayType.valueOf(holidayTypeStr);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        AttendanceStatus finalStatus = determineFinalStatus(att, holidayType, empId, date);

        // 更新数据库
        attendanceDao.updateAttendanceStatus(att.getId(), finalStatus.name());
        log.debug("考勤重算：员工 {} 日期 {} 状态 => {}", empId, date, finalStatus);

        return finalStatus;
    }

    /**
     * 对指定员工+日期范围进行考勤重算
     * @return 日期 → 最终状态的映射
     */
    public Map<LocalDate, AttendanceStatus> recalculate(int empId, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, AttendanceStatus> results = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            AttendanceStatus status = recalculate(empId, date);
            results.put(date, status);
        }
        return results;
    }

    /**
     * 按状态优先级确定最终状态
     * 
     * 优先级（高→低）：
     * HOLIDAY > REST_DAY > LEAVE > DAY_OFF > BUSINESS_TRIP > FIELD_WORK 
     * > NORMAL > LATE > EARLY > LATE_EARLY > MISSING_CARD > ABSENCE
     */
    private AttendanceStatus determineFinalStatus(Attendance att, HolidayType holidayType,
                                                   int empId, LocalDate date) {
        // 1. 节假日类型优先
        if (holidayType == HolidayType.HOLIDAY) {
            return AttendanceStatus.HOLIDAY;
        }
        if (holidayType == HolidayType.REST_DAY) {
            return AttendanceStatus.REST_DAY;
        }

        // 2. 检查 today_status
        TodayStatus todayStatus = att.getTodayStatus();

        // 3. 查询当天是否有已批准的请假（通过 leave 表）
        boolean hasApprovedLeave = checkApprovedLeave(empId, date);
        if (hasApprovedLeave || todayStatus == TodayStatus.LEAVE) {
            return AttendanceStatus.LEAVE;
        }

        // 4. 调休
        if (todayStatus == TodayStatus.DAY_OFF) {
            return AttendanceStatus.DAY_OFF;
        }

        // 5. 正常签到签退判断
        if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
            LocalTime checkIn = att.getCheckInTime().toLocalTime();
            LocalTime checkOut = att.getCheckOutTime().toLocalTime();

            boolean late = checkIn.isAfter(DEFAULT_START_TIME);
            boolean early = checkOut.isBefore(DEFAULT_END_TIME);

            if (late && early) return AttendanceStatus.LATE_EARLY;
            if (late) return AttendanceStatus.LATE;
            if (early) return AttendanceStatus.EARLY;
            return AttendanceStatus.NORMAL;
        }

        // 6. 仅签到（未签退）→ 缺卡
        if (att.getCheckInTime() != null && att.getCheckOutTime() == null) {
            return AttendanceStatus.MISSING_CARD;
        }

        // 7. 仅签退（未签到）
        if (att.getCheckInTime() == null && att.getCheckOutTime() != null) {
            return AttendanceStatus.ABSENCE;
        }

        // 8. 未签到 → 旷工
        return AttendanceStatus.ABSENCE;
    }

    /**
     * 检查员工当天是否有已批准的请假
     */
    private boolean checkApprovedLeave(int empId, LocalDate date) {
        try {
            int count = leaveDao.countApprovedLeaveToday(empId, date.toString());
            return count > 0;
        } catch (Exception e) {
            log.warn("查询请假状态失败: empId={}, date={}", empId, date, e);
            return false;
        }
    }
}
```

- [ ] **Step 3: Write integration test**

Create test file in OA-7 test directory. Test that recalculation works for all status paths.

- [ ] **Step 4: Compile and verify**

```bash
mvn compile -pl OA-7 -am
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add RecalculateAttendanceService as single source of truth for final status"
```

---

### Task 4: Fix AutoCreateSign to Create Attendance Records

**Files:**
- Create: `OA-7/src/main/java/com/oa7/service/AutoCreateAttendanceService.java`
- Modify: `OA-7/src/main/java/com/oa7/dao/AttendanceDao.java` (add insert method)
- Modify: `OA-7/src/main/java/com/oa7/dao/EmpDao.java` (add selectAllActiveEmpNumbers)

**Interfaces:**
- Consumes: AttendanceDao, HolidayDao, LeaveDao, EmpDao
- Produces: Attendance records created at midnight for all employees

- [ ] **Step 1: Add batch insert method to AttendanceDao (OA-7)**

```java
@Insert("INSERT INTO day.attendance(emp_id, date, today_status) VALUES(#{empId}, #{date}, #{todayStatus}) " +
        "ON DUPLICATE KEY UPDATE today_status=#{todayStatus}")
int insertOrUpdate(@Param("empId") int empId, @Param("date") LocalDate date,
                   @Param("todayStatus") String todayStatus);

@Insert("<script>" +
        "INSERT INTO day.attendance(emp_id, date, today_status) VALUES " +
        "<foreach collection='list' item='item' separator=','>" +
        "(#{item.empId}, #{item.date}, #{item.todayStatus})" +
        "</foreach> " +
        "ON DUPLICATE KEY UPDATE today_status=VALUES(today_status)" +
        "</script>")
int batchInsertOrUpdate(@Param("list") List<Attendance> list);
```

- [ ] **Step 2: Create AutoCreateAttendanceService.java**

```java
package com.oa7.service;

import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.EmpDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.LeaveDao;
import com.oa7.pojo.Attendance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 每日凌晨自动创建考勤记录
 * 
 * 00:00 执行，为所有员工创建当天的 Attendance 记录。
 * 提前判断节假日和已批准的请假，设置对应的初始状态。
 */
@Configuration
@EnableScheduling
public class AutoCreateAttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AutoCreateAttendanceService.class);

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private EmpDao empDao;

    @Autowired
    private HolidayDao holidayDao;

    @Autowired
    private LeaveDao leaveDao;

    @Scheduled(cron = "0 5 0 * * ?")  // 00:05 执行（留一点时间余量）
    public void createDailyAttendance() {
        LocalDate today = LocalDate.now();
        log.info("开始创建今日考勤记录，日期: {}", today);

        try {
            // 获取所有员工
            List<Integer> empNumbers = empDao.selectAllEmpNumber();
            if (empNumbers.isEmpty()) {
                log.warn("无员工数据，跳过创建考勤记录");
                return;
            }

            // 查询 holiday
            String holidayType = holidayDao.selectHolidayTypeByDate(today);

            List<Attendance> batch = new ArrayList<>();
            int createdCount = 0;

            for (int empId : empNumbers) {
                Attendance att = new Attendance();
                att.setEmpId(empId);
                att.setDate(today);

                if ("HOLIDAY".equals(holidayType)) {
                    att.setTodayStatus(TodayStatus.NOT_CHECKED_IN.name());
                } else if ("REST_DAY".equals(holidayType)) {
                    att.setTodayStatus(TodayStatus.NOT_CHECKED_IN.name());
                } else {
                    // 检查是否有已批准的请假
                    boolean hasLeave = leaveDao.countApprovedLeaveToday(empId, today.toString()) > 0;
                    if (hasLeave) {
                        att.setTodayStatus(TodayStatus.LEAVE.name());
                    } else {
                        att.setTodayStatus(TodayStatus.NOT_CHECKED_IN.name());
                    }
                }
                batch.add(att);
            }

            // 批量插入（使用 ON DUPLICATE KEY 避免重复）
            attendanceDao.batchInsertOrUpdate(batch);
            createdCount = batch.size();
            log.info("今日考勤记录创建完成，共 {} 条，日期: {}", createdCount, today);

        } catch (Exception e) {
            log.error("创建今日考勤记录异常，日期: {}", today, e);
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
mvn compile -pl OA-7 -am
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add AutoCreateAttendanceService for midnight attendance creation"
```

---

### Task 5: Refactor AttendanceSettlementService to Use RecalculateAttendanceService

**Files:**
- Modify: `OA-7/src/main/java/com/oa7/service/AttendanceSettlementService.java`

**Interfaces:**
- Consumes: RecalculateAttendanceService (Task 3)
- Produces: Simplified settlement logic delegating to recalculate service

- [ ] **Step 1: Simplify AttendanceSettlementService**

```java
package com.oa7.service;

import com.oa7.dao.AttendanceDao;
import com.oa7.pojo.Attendance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.List;

/**
 * 日终考勤结算服务
 * - 每天 23:59 执行
 * - 委托 RecalculateAttendanceService 进行状态重算
 */
@Configuration
@EnableScheduling
public class AttendanceSettlementService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceSettlementService.class);

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private RecalculateAttendanceService recalculateService;

    @Scheduled(cron = "0 59 23 * * ?")
    public void settleTodayAttendance() {
        LocalDate today = LocalDate.now();
        log.info("开始执行日终考勤结算，日期: {}", today);

        try {
            List<Attendance> records = attendanceDao.selectByDate(today);
            if (records.isEmpty()) {
                log.info("当天无考勤记录，跳过结算");
                return;
            }

            int updatedCount = 0;
            for (Attendance record : records) {
                recalculateService.recalculate(record.getEmpId(), today);
                updatedCount++;
            }

            log.info("日终考勤结算完成，共处理 {} 条记录，日期: {}", updatedCount, today);
        } catch (Exception e) {
            log.error("日终考勤结算异常，日期: {}", today, e);
        }
    }
}
```

The old `determineStatus()` method is removed — it's now in `RecalculateAttendanceService`.

- [ ] **Step 2: Verify compilation**

```bash
mvn compile -pl OA-7 -am
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor: AttendanceSettlementService delegates to RecalculateAttendanceService"
```

---

### Task 6: Cross-Service Notification (Bridging OA-2 and OA-7)

**Note:** Since OA-2 and OA-7 share the same database, the recalculation service runs in OA-7 where the scheduled tasks live. But OA-2 also needs enum awareness. This task ensures the two services stay in sync.

**Files:**
- No new files — architecture note only

**Key rules:**
1. `RecalculateAttendanceService` lives in **OA-7** (where scheduled tasks run)
2. OA-2 imports the new enum classes for type safety
3. OA-2 controllers and services keep their current logic; OA-7's recalculation at 23:59 corrects any drift
4. Future Phase 2 will add OA-7 approval → immediate recalculation calls
5. OA-2's approval-related code (if any) should delegate to OA-7 via shared database

- [ ] **Step 1: Document the cross-service contract**

Add comments to key interfaces clarifying data ownership.

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "docs: add cross-service architecture notes for Phase 1"
```

---

## Self-Review Checklist

- [ ] Every spec requirement in Phase 1 has a corresponding task
- [ ] No placeholders (TBD/TODO) in code
- [ ] Method signatures consistent across tasks
- [ ] Enum types used correctly in all comparisons
- [ ] OA-2 and OA-7 both have enum classes and type handlers
- [ ] Backward compatible — existing API responses unchanged
- [ ] Database VARCHAR storage unchanged (enum.name() → String)
