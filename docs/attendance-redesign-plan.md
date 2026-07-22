# OA 考勤系统重构设计方案

> 基于 [企业级 OA 考勤系统业务规范](./attendance-business-spec.md) v1.0
> 日期：2026-07-22
> 状态：待评审

---

## 1. 当前系统问题总结

### 1.1 架构问题

| # | 问题 | 当前表现 |
|:-:|:---|:---|
| 1 | 无单一考勤记录 | `sign` 表每天有上午/下午两条记录，无 `attendance` 核心表 |
| 2 | 实时/最终状态不分 | 只有 `state='已签到'/'未签到'`，无 `today_status` 和 `attendance_status` 分离 |
| 3 | 无日历系统 | 通过星期几判断工作日，不支持调休/节假日配置 |
| 4 | 无日终结算 | 没有定时任务计算最终考勤状态 |
| 5 | 无法定状态枚举 | 缺少 LATE / EARLY / ABSENCE / MISSING_CARD 等概念 |
| 6 | 统计直接查 sign 表 | 非单一数据源，与规范冲突 |

### 1.2 数据问题

| # | 问题 | 当前表现 |
|:-:|:---|:---|
| 7 | 签到时间用预设值 | `signDate` 存的是 `08:30:00:000` 而非真实签到时间 |
| 8 | 签到用 INSERT 而非 UPDATE | AutoCreateSign 创建预设记录，员工签到再 INSERT，造成重复 |
| 9 | 补签 UPDATE 可能空执行 | 若 AutoCreateSign 未产生过记录，补签 UPDATE 影响 0 行 |
| 10 | 请假不支持半天 | leave 表无 `duration` 字段 |
| 11 | 无调休/出差/外勤模块 | 只有请假和补签 |

### 1.3 性能问题

| # | 问题 | 当前表现 |
|:-:|:---|:---|
| 12 | 统计全表加载 | `dailyStatistics()` 和 `chartData()` 调用 `SELECT * FROM sign` 在 JVM 内存中统计 |
| 13 | 管理端 approve 全表扫描 | `approve()` 先 `selectAll()` 再 Java for 循环匹配 |

---

## 2. 目标架构

### 2.1 核心原则

```
所有业务（签到/签退/请假/调休/补卡/出差/外勤/节假日）
    │
    ▼
仅影响 Attendance 记录
    │
    ▼
统计/报表/工资 基于 Attendance
```

### 2.2 新旧表关系

```
旧表                         新表
────                         ────
sign (每天2条, 无状态分离)    → attendance (每天1条, 双状态)
leave (无duration)            → leave (加 duration 字段)
retroactive_sign             → makeup_request (更名 + 规范化)
（无）                        → holiday (日历)
（无）                        → day_off_request (调休)
（无）                        → business_trip (出差)
（无）                        → field_work (外勤)
（无）                        → attendance_rule (考勤规则)
```

---

## 3. 数据库设计

### 3.1 attendance 核心表

```sql
CREATE TABLE day.attendance (
  id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
  emp_id           INT          NOT NULL        COMMENT '员工编号',
  date             DATE         NOT NULL        COMMENT '考勤日期',

  -- 签到/签退时间（实际时间）
  check_in_time    DATETIME                     COMMENT '实际签到时间',
  check_out_time   DATETIME                     COMMENT '实际签退时间',

  -- 双状态分离
  today_status     VARCHAR(20)  NOT NULL DEFAULT 'NOT_CHECKED_IN'
                   COMMENT '实时状态: NOT_CHECKED_IN/CHECKED_IN/CHECKED_OUT/LEAVE_PENDING/MAKEUP_PENDING',
  attendance_status VARCHAR(20)                 COMMENT '最终状态: NORMAL/LATE/EARLY/LEAVE/ABSENCE...',

  remark           VARCHAR(500)                 COMMENT '备注',
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_emp_date (emp_id, date),
  KEY idx_date_status (date, attendance_status),
  KEY idx_emp (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录（核心表）';
```

### 3.2 holiday 日历表

```sql
CREATE TABLE day.holiday (
  id     INT          AUTO_INCREMENT PRIMARY KEY,
  date   DATE         NOT NULL UNIQUE COMMENT '日期',
  type   VARCHAR(20)  NOT NULL        COMMENT 'WORKDAY/HOLIDAY/REST_DAY',
  name   VARCHAR(100)                 COMMENT '节假日名称',
  year   INT          NOT NULL        COMMENT '年份',
  KEY idx_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日历配置';
```

### 3.3 leave 请假表（扩展现有）

```sql
-- 在现有 day.leave 表上增加字段
ALTER TABLE day.leave ADD COLUMN duration VARCHAR(20) DEFAULT 'FULL_DAY'
  COMMENT 'FULL_DAY/HALF_DAY_AM(上午请)/HALF_DAY_PM(下午请)/HOUR';
ALTER TABLE day.leave ADD COLUMN start_time TIME COMMENT '按小时请假时的开始时间';
ALTER TABLE day.leave ADD COLUMN end_time TIME   COMMENT '按小时请假时的结束时间';
```

### 3.4 新增表

```sql
CREATE TABLE day.makeup_request (
  id          INT          AUTO_INCREMENT PRIMARY KEY,
  emp_id      INT          NOT NULL,
  date        DATE         NOT NULL        COMMENT '补卡日期',
  type        VARCHAR(10)  NOT NULL        COMMENT 'CHECK_IN/CHECK_OUT',
  request_time VARCHAR(20) NOT NULL        COMMENT '申请的补卡时间 HH:mm',
  reason      VARCHAR(500) NOT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version     INT          DEFAULT 0      COMMENT '乐观锁',
  KEY idx_emp_date (emp_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请';

CREATE TABLE day.day_off_request (
  id          INT          AUTO_INCREMENT PRIMARY KEY,
  emp_id      INT          NOT NULL,
  date        DATE         NOT NULL,
  reason      VARCHAR(500),
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version     INT          DEFAULT 0,
  KEY idx_emp_date (emp_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调休申请';

CREATE TABLE day.business_trip (
  id          INT          AUTO_INCREMENT PRIMARY KEY,
  emp_id      INT          NOT NULL,
  start_date  DATE         NOT NULL,
  end_date    DATE         NOT NULL,
  reason      VARCHAR(500),
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version     INT          DEFAULT 0,
  KEY idx_emp_date (emp_id, start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出差申请';

CREATE TABLE day.field_work (
  id          INT          AUTO_INCREMENT PRIMARY KEY,
  emp_id      INT          NOT NULL,
  date        DATE         NOT NULL,
  location    VARCHAR(200),
  reason      VARCHAR(500),
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version     INT          DEFAULT 0,
  KEY idx_emp_date (emp_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外勤申请';
```

---

## 4. 状态机设计

### 4.1 TodayStatus（实时状态）

```java
public enum TodayStatus {
    NOT_CHECKED_IN,   // 未签到（凌晨默认）
    CHECKED_IN,       // 已签到（签到动作触发）
    CHECKED_OUT,      // 已签退（签退动作触发）
    LEAVE,            // 请假审批通过后覆盖
    MAKEUP_PENDING,   // 补卡审批中
    DAY_OFF,          // 调休审批通过后覆盖
    BUSINESS_TRIP,    // 出差审批通过后覆盖
    FIELD_WORK        // 外勤审批通过后覆盖
}
```

实时状态流转：
```
NOT_CHECKED_IN ──签到──→ CHECKED_IN ──签退──→ CHECKED_OUT
     │                      │
     ├──请假审批通过──→ LEAVE                  ├──请假审批通过──→ LEAVE
     ├──调休审批通过──→ DAY_OFF               ├──调休审批通过──→ DAY_OFF
     └──提交补卡申请── (不变，仍 NOT_CHECKED_IN)
```

### 4.2 AttendanceStatus（最终状态）

```java
public enum AttendanceStatus {
    // 正常出勤
    NORMAL,        // 正常
    LATE,          // 迟到
    EARLY,         // 早退
    LATE_EARLY,    // 迟到+早退

    // 免签到类型
    LEAVE,         // 请假
    DAY_OFF,       // 调休
    BUSINESS_TRIP, // 出差
    FIELD_WORK,    // 外勤

    // 异常类型
    MISSING_CARD,  // 缺卡（仅签到/仅签退）
    ABSENCE,       // 旷工

    // 非工作日
    HOLIDAY,       // 节假日
    REST_DAY       // 休息日
}
```

### 4.3 结算规则（23:59 定时任务）

```
输入: attendance 记录 + 当天 Holiday 类型 + 审批通过的业务申请
输出: attendance_status

结算优先级（高 → 低）:

1. HOLIDAY               → attendance_status = HOLIDAY
2. REST_DAY              → attendance_status = REST_DAY
3. LEAVE（全天请假）      → attendance_status = LEAVE
4. DAY_OFF               → attendance_status = DAY_OFF
5. BUSINESS_TRIP         → attendance_status = BUSINESS_TRIP
6. FIELD_WORK            → attendance_status = FIELD_WORK
7. 已签到 && 已签退:
   a. 签到时间 > 09:00 && 签退时间 >= 18:00 → LATE
   b. 签到时间 <= 09:00 && 签退时间 < 18:00 → EARLY
   c. 签到时间 > 09:00 && 签退时间 < 18:00 → LATE_EARLY
   d. 签到时间 <= 09:00 && 签退时间 >= 18:00 → NORMAL
8. 已签到 && 未签退     → MISSING_CARD
9. 未签到 && 已签退     → MISSING_CARD
10. NOT_CHECKED_IN
    a. 有 MAKEUP_REQUEST 审批中 → (等待结算)
    b. 无任何申请             → ABSENCE
```

### 4.4 RecalculateAttendance()

```
触发时机：
  - 签到/签退动作
  - 请假审批通过/拒绝/撤销
  - 补卡审批通过/拒绝
  - 调休审批通过/拒绝
  - 出差审批通过/拒绝
  - 外勤审批通过/拒绝

逻辑：
  1. 重新加载该员工指定日期的 attendance
  2. 重新加载当日 holiday 类型
  3. 重新加载所有已审批的业务申请
  4. 按结算规则重新计算 attendance_status
  5. 更新 attendance 记录

注意：
  - 当日未结算前，实时状态(today_status)优先展示
  - 已结算后，最终状态(attendance_status)覆盖展示
  - 重新计算必须考虑状态优先级
```

---

## 5. 边界场景处理（对应规范第22章）

| 场景 | 输入 | 结算结果 | 说明 |
|:---|:---|:---:|:---|
| 1 | 09:20签到, 10:00请假批准 | LEAVE | 请假优先级 > LATE |
| 2 | 全天请假, 员工仍签到 | LEAVE | 签到不改变已审批的请假状态 |
| 3 | 国庆节签到 | HOLIDAY | Holiday 优先级最高 |
| 4 | 调休后签到 | DAY_OFF | 调休优先级 > 签到 |
| 5 | 补卡审批通过 | 重新计算 | 触发 RecalculateAttendance() |
| 6 | 审批撤销 | 重新计算 | 触发 RecalculateAttendance() |

---

## 6. API 设计

### 6.1 员工端

| Method | Path | 说明 |
|:---:|:---|:---|
| POST | `/api/v1/employee/attendance/check-in` | 签到（记录实际时间） |
| POST | `/api/v1/employee/attendance/check-out` | 签退（记录实际时间） |
| GET | `/api/v1/employee/attendance/today` | 今日考勤状态 |
| GET | `/api/v1/employee/attendance/history` | 历史考勤记录（分页） |

其他接口（请假/补卡/调休）沿用现有路由模式。

### 6.2 管理端

| Method | Path | 说明 |
|:---:|:---|:---|
| GET | `/api/v1/admin/attendance/today/status` | 今日实时统计（已签到/未签到/迟到/请假中） |
| GET | `/api/v1/admin/attendance/yesterday/summary` | 昨日最终统计 |
| GET | `/api/v1/admin/attendance/monthly?emp=xxx&month=2026-07` | 个人月度统计 |
| GET | `/api/v1/admin/attendance/department/monthly?dept=xxx&month=2026-07` | 部门月度统计 |

---

## 7. 定时任务

| 时间 | 任务 | 说明 |
|:---:|:---|:---|
| 00:00 | `createDailyAttendance()` | 为每位员工创建当天 `attendance`，`today_status=NOT_CHECKED_IN`。跳过在职但禁用员工 |
| 23:59 | `settleDailyAttendance()` | 遍历当天 `attendance`，按结算规则写入 `attendance_status` |
| 01:00 | `generateDailyReport()` | 生成昨日考勤日报 |
| 每月1日 02:00 | `generateMonthlyReport()` | 生成上月月报 |

---

## 8. 数据迁移方案

### 8.1 迁移步骤

1. **建新表** — 创建 `attendance`、`holiday`、`makeup_request` 等新表
2. **双写过渡** — 旧代码继续运行，新代码同时写旧表和新表
3. **历史数据迁移** — 将现有 `sign` 表中的历史记录转换为 `attendance` 记录
4. **切换读路径** — 前端改为读 `attendance` 表
5. **下线旧表** — 停用 `sign` 表及相关旧接口

### 8.2 sign → attendance 转换规则

```
sign 中每天有两条记录(type='a'/'p')，合并为一条 attendance：

check_in_time = type='a' 且 state='已签到' 的记录 signDate → attendance.check_in_time
check_out_time = type='p' 且 state='已签到' 的记录 signDate → attendance.check_out_time

attendance_status 通过结算规则重新计算得出
```

---

## 9. 需要你裁决的问题

### 9.1 签到/签退时间标准

规范中写的是 **09:00~18:00**，但现有系统用的是 **08:30~17:30**。以哪个为准？

### 9.2 审批层级

现在的系统请假/补签审批只有一个层级（管理员审批）。但规范第21章提到"主管"和"管理员"分离。是否需要引入主管审批层级？还是保持现状（管理员直接审批）？

### 9.3 补卡(补签)的命名

规范中用"补卡"，现有系统用"补签"。是否统一为"补卡"？

### 9.4 旧数据保留

现有的 `sign` 表历史数据（签到记录）是否要迁移到新 `attendance` 表？还是从新系统上线日开始全新记录？

### 9.5 实施节奏

重构涉及大量改动。你倾向于：
- **A) 大爆炸式** — 全部改完一次性上线
- **B) 分阶段** — 先上 `attendance` 核心表 + 签到/签退/请假，再逐步加节假日/调休/出差/外勤

---

## 10. 实施路径（按阶段）

### Phase 1：核心改造
- 创建 `attendance` 表 + `holiday` 表
- 改造签到/签退接口（用 INSERT → UPDATE）
- 改造 AutoCreateSign 定时任务（创建 `attendance`）
- 日终结算定时任务
- 管理端统计改为读 `attendance`

### Phase 2：补齐业务
- 请假增加 duration 字段（支持半天）
- 补签 → 补卡规范化
- 新增调休/出差/外勤模块

### Phase 3：日历系统
- Holiday 管理界面
- 节假日导入（可配置）
- 替换所有 "星期几判断"

### Phase 4：报表
- 月度考勤报表（个人/部门）
- 考勤分析图表
