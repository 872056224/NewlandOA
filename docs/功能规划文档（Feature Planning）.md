# 企业级 OA 考勤系统 — 功能规划文档

> Version: 1.0  
> Date: 2026-07-22  
> Based on: 《企业级 OA 考勤系统业务规范（Business Specification）》  
> Author: AI Agent

---

## 目录

1. [项目架构](#1-项目架构)
2. [当前实现状态总览](#2-当前实现状态总览)
3. [与业务规范的差距分析](#3-与业务规范的差距分析)
4. [Phase 1：核心考勤体系完善（高优先级）](#4-phase-1核心考勤体系完善高优先级)
5. [Phase 2：审批联动与状态重算（高优先级）](#5-phase-2审批联动与状态重算高优先级)
6. [Phase 3：节假日与考勤规则模块（中优先级）](#6-phase-3节假日与考勤规则模块中优先级)
7. [Phase 4：定时任务与月度统计（中优先级）](#7-phase-4定时任务与月度统计中优先级)
8. [Phase 5：出差与外勤模块（低优先级）](#8-phase-5出差与外勤模块低优先级)
9. [Phase 6：权限完善与边界场景（持续优化）](#9-phase-6权限完善与边界场景持续优化)
10. [数据库变更清单](#10-数据库变更清单)
11. [API 总览与规划](#11-api-总览与规划)
12. [开放问题](#12-开放问题)

---

## 1. 项目架构

### 1.1 当前架构

```
┌──────────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 + Vite)                        │
│              Port 5173 │ Element Plus + ECharts               │
└──────────┬───────────────────────────────────┬───────────────┘
           │ /api/v1/employee/*                 │ /api/v1/admin/*
           ▼                                    ▼
┌──────────────────────┐      ┌──────────────────────────────┐
│  Gateway (Spring Cloud)  │      │                               │
│     Port 8888            │      │   oa-ai-service (Port 8083)   │
│    路由规则:              │      │   Spring AI + Ollama           │
│   /api/v1/employee → OA-2│      │   知识库 RAG                    │
│   /api/v1/admin    → OA-7│      └──────────────────────────────┘
│   /api/v1/ai       → AI  │
└──────┬───────────────────┘
       │
       ├── OA-2（员工服务 · Port 8081）
       │   签到 · 签退 · 请假申请 · 补卡申请 · 补签申请 · 通知 · AI 知识库
       │
       └── OA-7（管理员服务 · Port 8082）
            员工管理 · 部门/职务管理 · 审批(请假/补签) · 考勤统计 · 知识库管理
            定时任务(凌晨创建考勤 · 日终结算)
       └── 共享数据库: day（MySQL 3306）
```

### 1.2 架构决策

| 决策项 | 决定 |
|--------|------|
| 后端架构 | **保持两个独立服务**：OA-2（员工端）和 OA-7（管理端），共用 `day` 数据库 |
| 跨服务通信 | 共享数据库（非 API 调用），OA-2 和 OA-7 直接读写同一数据库 |
| 调休设计 | **保持现有设计**：调休作为请假的一种类型（`type='调休'`），不拆分为独立模块 |
| 考勤规则 | **需要可配置规则**：设计 `AttendanceRule` 实体 + 管理界面 |
| 前端 | Vue 3 + Element Plus + ECharts，无独立 API 层，组件内直接调用 axios |
| 认证 | Session-Based（HttpSession），无 JWT |
| 通知 | 数据库通知表 + STOMP WebSocket 实时推送 |

### 1.3 团队约定

- **所有新增 API 必须遵循统一响应格式**：`{ code: 200, data: ..., total?: number, message?: string }`
- **状态字段禁止硬编码字符串**：统一使用 `AttendanceStatus`、`TodayStatus` 等常量/枚举类
- **审批通过/撤销/补卡成功后必须触发考勤重算**：调用 `RecalculateAttendance()`
- **模块间依赖**：OA-7 的审批操作需同步更新 OA-2 的 attendance 表
- **所有定时任务统一放在 OA-7**（管理服务端）

---

## 2. 当前实现状态总览

### 2.1 已经实现的功能

#### 员工端功能 (OA-2)

| 功能模块 | 状态 | 说明 |
|---------|------|------|
| 员工登录/退出 | ✅ 已完成 | Session 认证，MD5 密码 |
| 个人信息查看/编辑 | ✅ 已完成 | |
| 修改密码 | ✅ 已完成 | |
| 签到 | ✅ 已完成 | 含地址解析、防重复签到、请假检测 |
| 签退 | ✅ 已完成 | 必须先签到，含地址解析 |
| 今日考勤状态 | ✅ 已完成 | 返回实时状态和签到/签退时间 |
| 历史考勤记录（分页） | ✅ 已完成 | |
| 请假申请 | ✅ 已完成 | 支持 FULL_DAY / HALF_DAY_AM / HALF_DAY_PM |
| 请假记录列表 | ✅ 已完成 | |
| 今日请假状态检测 | ✅ 已完成 | 签到页展示请假中横幅 |
| 补卡申请（MakeupRequest） | ✅ 已完成 | CHECK_IN / CHECK_OUT 类型 |
| 补签申请（RetroactiveSign） | ✅ 已完成 | 按日期+时段(a/p) |
| 补签记录列表 | ✅ 已完成 | |
| 通知中心 | ✅ 已完成 | 列表、未读数、标记已读、WebSocket |
| AI 客服聊天 | ✅ 已完成 | Ollama 本地大模型 + RAG 知识库 |

#### 管理端功能 (OA-7)

| 功能模块 | 状态 | 说明 |
|---------|------|------|
| 管理员登录/退出 | ✅ 已完成 | |
| 数据面板（Dashboard） | ✅ 已完成 | 员工数、部门数、职务数、今日签到数 |
| 员工 CRUD | ✅ 已完成 | 增删改查+部门/职务下拉 |
| 部门 CRUD | ✅ 已完成 | 含部门人数统计 |
| 职务 CRUD | ✅ 已完成 | |
| 每日签到统计列表 | ✅ 已完成 | 分页日期列表+每日明细 |
| 签到统计图表（ECharts） | ✅ 已完成 | 近5日柱状图 |
| 今日已签到列表 | ✅ 已完成 | |
| 今日未签到列表 | ✅ 已完成 | 支持管理员替员工补签 |
| 请假审批 | ✅ 已完成 | 待审批/已审批标签页，批准/拒绝+乐观锁+通知 |
| 补签审批 | ✅ 已完成 | 批准/拒绝+乐观锁+通知+更新签到状态 |
| 通知管理 | ✅ 已完成 | 列表、未读数、标记已读 |
| 知识库 CRUD | ✅ 已完成 | 支持重建向量索引 |
| 考勤日终结算（23:59） | ✅ 已完成 | 在 OA-7 定时执行 |
| 凌晨创建考勤记录（00:00） | ✅ 已完成 | 但创建的是旧版 `sign` 记录 |

#### AI 服务 (oa-ai-service)

| 功能模块 | 状态 | 说明 |
|---------|------|------|
| 大模型聊天（同步/流式） | ✅ 已完成 | Ollama qwen2.5:7b |
| RAG 知识库问答 | ✅ 已完成 | 文档向量化+相似度检索 |
| 健康检测 | ✅ 已完成 | |
| 推荐问题 | ✅ 已完成 | |
| 动态添加知识 | ✅ 已完成 | |

### 2.2 部分实现但需完善的功能

| 功能模块 | 现状 | 问题 |
|---------|------|------|
| Attendance 数据模型 | 有实体但字段用 String | 缺少枚举约束，todayStatus/attendanceStatus 用字符串 |
| 请假审批通过后更新考勤 | OA-7 批准/拒绝后未更新 attendance 表 | 应该触发 RecalculateAttendance |
| 补签审批通过后更新考勤 | 只更新了 sign 表，未更新 attendance 表 | 应该触发 RecalculateAttendance |
| 日终结算 | OA-7 有，但 OA-2 没有 | 两地代码不统一 |
| 假期管理 | 只有 HolidayDao.selectHolidayTypeByDate | 缺少完整 CRUD 和管理界面 |
| 考勤规则 | 上下班时间硬编码 08:30-17:30 | 需要可配置 |
| 状态优先级 | 结算服务中实现了部分规则 | 但边界场景（如请假+签到）未按优先级覆盖 |

### 2.3 完全缺失的功能

| 功能模块 | 说明 |
|---------|------|
| 管理员首页今日实时统计 | 应展示：已签到/未签到/迟到/请假中/补卡审批中 |
| 昨日考勤统计 | 按最终状态统计：NORMAL/LATE/EARLY/LEAVE/ABSENCE |
| 个人月度统计 | 出勤天数/迟到次数/请假次数/缺卡次数/旷工次数/出勤率 |
| 部门月度统计 | 部门出勤率/迟到率/请假率/旷工率 |
| 月度报表生成（定时） | 每月1号生成上月报表 |
| 日报生成（定时） | 每天01:00生成前一日日报 |
| 出差模块（BusinessTrip） | 完整的出差申请+审批+状态 |
| 外勤模块（FieldWork） | 完整的外勤申请+审批+状态 |
| AttendanceRule 实体 | 可配置的考勤规则 |
| Holiday 管理页面 | 管理员管理节假日/调休日 |
| Attendance 自动创建（00:00） | 凌晨创建当天 Attendance 记录 |
| 状态枚举 | TodayStatus/AttendanceStatus 枚举类 |
| 考勤重算服务（RecalculateAttendance） | 统一的考勤重算入口 |
| 审批撤销 | 已批准的申请可以撤销并重算考勤 |

---

## 3. 与业务规范的差距分析

### 3.1 核心原则落实

| 业务规范要求 | 当前状态 | 差距 |
|------------|---------|------|
| Attendance 为唯一核心对象 | ✅ Attendance 实体已存在 | 但统计功能未完全基于 Attendance |
| 最终状态原则（每人每天一个最终状态） | ⚠️ 部分实现 | 结算服务输出 AttendanceStatus，但未在前端展示 |
| 实时状态与最终状态分离 | ⚠️ 部分实现 | todayStatus 和 attendanceStatus 字段已分离，但前端未展示最终状态 |
| 禁止通过星期几判断工作日 | ⚠️ 未执行 | 代码中没有出现，但也未实现 Holiday 查询 |
| 审批/补签通过后触发 RecalculateAttendance | ❌ 未实现 | OA-7 审批操作未调用 Attendance 重算 |

### 3.2 状态定义

| 规范中的枚举值 | 代码实现 | 状态 |
|---------------|---------|------|
| TodayStatus | String 类型，无枚举 | ❌ 缺失 |
| AttendanceStatus | String 类型，无枚举 | ❌ 缺失 |
| HolidayType | 无对应的 Java 枚举 | ❌ 缺失 |

### 3.3 边界场景

| 边界场景 | 当前实现 | 状态 |
|---------|---------|------|
| 场景1：迟到后请假通过 → LEAVE | LeaveServiceImpl 更新 today_status=LEAVE | ✅ 通过 |
| 场景2：全天请假+签到 → LEAVE | 请假后 today_status=LEAVE，签到被拦截 | ✅ 通过 |
| 场景3：节假日签到 → HOLIDAY | 结算时覆盖 | ✅ 通过 |
| 场景4：调休后签到 → DAY_OFF | 调休未独立实现 | ❌ 未通过 |
| 场景5：补卡审批通过后重算 | 未触发重算 | ❌ 未通过 |
| 场景6：审批撤销后重算 | 无撤销功能 | ❌ 未通过 |

---

## 4. Phase 1：核心考勤体系完善（高优先级）

### 4.1 创建状态枚举类

#### 后端：新建枚举类 (OA-2 + OA-7 各一份，或抽取公共 jar)

**文件：** `com.oa2/oa7/constant/TodayStatus.java`

```java
public enum TodayStatus {
    NOT_CHECKED_IN("未签到"),
    CHECKED_IN("已签到"),
    CHECKED_OUT("已签退"),
    LEAVE_PENDING("请假审批中"),
    LEAVE("已请假"),
    MAKEUP_PENDING("补卡审批中"),
    BUSINESS_PENDING("出差审批中"),
    FIELD_PENDING("外勤审批中"),
    DAY_OFF("调休");

    private final String displayName;
    // constructor + getter
}
```

**文件：** `com.oa2/oa7/constant/AttendanceStatus.java`

```java
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
    // constructor + getter
}
```

**文件：** `com.oa2/oa7/constant/HolidayType.java`

```java
public enum HolidayType {
    WORKDAY("工作日"),
    HOLIDAY("节假日"),
    REST_DAY("休息日");
}
```

### 4.2 修改 Attendance 实体字段类型

**变更：** 将 `todayStatus` 和 `attendanceStatus` 从 `String` 改为对应枚举

`Attendance.java` (OA-2 + OA-7)：
```java
private TodayStatus todayStatus;
private AttendanceStatus attendanceStatus;
```

**DAO 层适配：** MyBatis 枚举类型处理器，添加 `@Enumerated(EnumType.STRING)` 或自定义 TypeHandler

### 4.3 凌晨自动创建 Attendance 记录

**现状：** OA-7 的 `AutoCreateSign` 在 `day.sign` 表创建旧版签到记录  
**目标：** 在 `day.attendance` 表创建考勤记录，替代旧版 sign 记录

#### 后端 (OA-7) — 修改 AutoCreateSign

**文件：** `AutoCreateSign.java`

```java
@Scheduled(cron = "0 0 0 * * ?")
public void createDailyAttendance() {
    // 1. 获取所有员工
    List<Integer> empNumbers = empDao.selectAllEmpNumber();
    LocalDate today = LocalDate.now();

    for (int number : empNumbers) {
        // 2. 检查 Holiday 表判断今天性质
        String holidayType = holidayDao.selectHolidayTypeByDate(today);

        // 3. 检查今天是否有已批准的请假/调休
        boolean hasApprovedLeave = leaveDao.countApprovedLeaveToday(number, today.toString()) > 0;

        // 4. 创建 Attendance 记录
        Attendance att = new Attendance();
        att.setEmpId(number);
        att.setDate(today);

        if ("HOLIDAY".equals(holidayType)) {
            att.setTodayStatus(TodayStatus.NOT_CHECKED_IN);
            att.setAttendanceStatus(AttendanceStatus.HOLIDAY);
        } else if ("REST_DAY".equals(holidayType)) {
            att.setTodayStatus(TodayStatus.NOT_CHECKED_IN);
            att.setAttendanceStatus(AttendanceStatus.REST_DAY);
        } else if (hasApprovedLeave) {
            att.setTodayStatus(TodayStatus.LEAVE);
            att.setAttendanceStatus(AttendanceStatus.LEAVE);
        } else {
            att.setTodayStatus(TodayStatus.NOT_CHECKED_IN);
            att.setAttendanceStatus(null); // 日终结算时再确定
        }

        attendanceDao.insert(att);
    }
}
```

**数据库变更：** `attendance` 表新增 `ON DUPLICATE KEY` 或使用 `INSERT IGNORE` 避免重复

### 4.4 统一考勤重算服务

#### 后端 (OA-7) — 新建 RecalculateAttendance 服务

**文件：** `com.oa7.service.RecalculateAttendanceService.java`

```java
/**
 * 考勤重算服务 — 所有审批通过/撤销/补卡成功后必须调用此服务
 */
@Service
public class RecalculateAttendanceService {

    @Autowired private AttendanceDao attendanceDao;
    @Autowired private HolidayDao holidayDao;
    @Autowired private LeaveDao leaveDao;
    @Autowired private EmpDao empDao;

    /**
     * 对指定员工+日期范围进行考勤重算
     */
    public void recalculate(int empId, LocalDate startDate, LocalDate endDate) {
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            recalculate(empId, date);
        }
    }

    /**
     * 对指定员工+单日进行考勤重算（按状态优先级）
     */
    public AttendanceStatus recalculate(int empId, LocalDate date) {
        // 1. 查询当天 attendance 记录
        Attendance att = attendanceDao.selectByEmpAndDate(empId, date);
        if (att == null) return null;

        // 2. 查询 Holiday
        String holidayType = holidayDao.selectHolidayTypeByDate(date);

        // 3. 按优先级确定最终状态
        AttendanceStatus finalStatus = determineFinalStatus(att, holidayType, empId, date);

        // 4. 更新 attendance 表
        attendanceDao.updateAttendanceStatus(att.getId(), finalStatus.name());

        return finalStatus;
    }

    /**
     * 状态优先级（高 → 低）：
     * HOLIDAY > REST_DAY > LEAVE > DAY_OFF > BUSINESS_TRIP > FIELD_WORK
     * > NORMAL > LATE > EARLY > MISSING_CARD > ABSENCE
     */
    private AttendanceStatus determineFinalStatus(Attendance att, String holidayType, int empId, LocalDate date) {
        // 节假日类型优先
        if ("HOLIDAY".equals(holidayType)) return AttendanceStatus.HOLIDAY;
        if ("REST_DAY".equals(holidayType)) return AttendanceStatus.REST_DAY;

        // 查询当天是否有已批准的请假
        boolean hasApprovedLeave = leaveDao.countApprovedLeaveToday(empId, date.toString()) > 0;
        if (hasApprovedLeave) return AttendanceStatus.LEAVE;

        // 检查 today_status
        if (att.getTodayStatus() == TodayStatus.LEAVE) return AttendanceStatus.LEAVE;
        if (att.getTodayStatus() == TodayStatus.DAY_OFF) return AttendanceStatus.DAY_OFF;

        // 有签到签退 → 判迟到早退
        if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
            // 从 AttendanceRule 获取规则（或默认 08:30-17:30）
            // ... 迟早早退判断逻辑 ...
        }

        if (att.getCheckInTime() != null && att.getCheckOutTime() == null) return AttendanceStatus.MISSING_CARD;
        if (att.getCheckInTime() == null && att.getCheckOutTime() != null) return AttendanceStatus.ABSENCE;
        return AttendanceStatus.ABSENCE;
    }
}
```

**调用时机：** 所有审批操作完成后调用：

| 操作 | 调用方式 |
|------|---------|
| 请假审批通过 | `recalculate(empId, startDate, endDate)` |
| 请假审批撤销 | `recalculate(empId, startDate, endDate)` |
| 补卡审批通过 | `recalculate(empId, date, date)` |
| 补签审批通过 | `recalculate(empId, signDate, signDate)` |
| 补卡审批撤销 | `recalculate(empId, date, date)` |
| 日终结算 | 全量 `recalculate` |

### 4.5 改进日终结算服务

**现状：** 结算服务位于 OA-7，逻辑较完整但与重算服务重复  
**目标：** 将结算逻辑委托给 `RecalculateAttendanceService`，统一入口

**文件：** `AttendanceSettlementService.java`（OA-7）

```java
@Scheduled(cron = "0 59 23 * * ?")
public void settleTodayAttendance() {
    LocalDate today = LocalDate.now();
    List<Attendance> records = attendanceDao.selectByDate(today);
    for (Attendance record : records) {
        recalculateService.recalculate(record.getEmpId(), today);
    }
}
```

---

## 5. Phase 2：审批联动与状态重算（高优先级）

### 5.1 请假审批联动考勤重算

#### 后端 (OA-7) — 修改 LeaveServiceImpl

**文件：** `LeaveServiceImpl.java`(OA-7)

**`approve()` 方法增加：**
```java
@Override
public RESP approve(String id) {
    // ... 现有逻辑：乐观锁更新状态、通知员工 ...

    // 新增：考勤重算
    if (leave != null) {
        LocalDate startDate = LocalDate.parse(leave.getStart_date().substring(0, 10));
        LocalDate endDate = LocalDate.parse(leave.getEnd_date().substring(0, 10));
        recalculateAttendanceService.recalculate(leave.getNumber(), startDate, endDate);
    }

    return RESP.ok("操作成功");
}
```

**`reject()` 方法增加：**
```java
@Override
public RESP reject(String id) {
    // ... 现有逻辑：拒绝后通知员工 ...

    // 新增：拒绝后也需要重算（可能之前有 today_status=LEAVE 需要清除）
    if (leave != null) {
        LocalDate startDate = LocalDate.parse(leave.getStart_date().substring(0, 10));
        LocalDate endDate = LocalDate.parse(leave.getEnd_date().substring(0, 10));
        // 清除 today_status 中的 LEAVE 标记
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            Attendance att = attendanceDao.selectByEmpAndDate(leave.getNumber(), d);
            if (att != null && att.getTodayStatus() == TodayStatus.LEAVE) {
                // 如果员工没有签到，置为 NOT_CHECKED_IN
                if (att.getCheckInTime() == null) {
                    attendanceDao.updateTodayStatusByEmpAndDate(leave.getNumber(), d, TodayStatus.NOT_CHECKED_IN);
                }
            }
        }
        recalculateAttendanceService.recalculate(leave.getNumber(), startDate, endDate);
    }

    return RESP.ok("操作成功");
}
```

### 5.2 补签/补卡审批联动考勤重算

#### 后端 (OA-7) — 修改 RetroactiveSignServiceImpl

**文件：** `RetroactiveSignServiceImpl.java`(OA-7)

**`approve()` 方法增加：**
```java
@Override
public RESP approve(int id) {
    // ... 现有逻辑：更新 sign 表状态 ...

    // 新增：更新 attendance 表签到/签退时间（根据补签类型）
    if (sign != null) {
        LocalDate signDate = LocalDate.parse(sign.getSign_date());
        Attendance att = attendanceDao.selectByEmpAndDate(sign.getNumber(), signDate);
        if (att == null) {
            // 如果没有 attendance 记录，创建一个
            att = new Attendance();
            att.setEmpId(sign.getNumber());
            att.setDate(signDate);
            attendanceDao.insert(att);
        }

        // 根据补签类型更新签到或签退时间
        if ("a".equals(sign.getType())) {
            // 补上午签到：设置为上班时间
            LocalTime defaultStart = attendanceRuleService.getStartTime(); // 从规则获取
            att.setCheckInTime(LocalDateTime.of(signDate, defaultStart));
        } else if ("p".equals(sign.getType())) {
            // 补下午签退：设置为下班时间
            LocalTime defaultEnd = attendanceRuleService.getEndTime();
            att.setCheckOutTime(LocalDateTime.of(signDate, defaultEnd));
        }

        attendanceDao.updateCheckTime(att);

        // 考勤重算
        recalculateAttendanceService.recalculate(sign.getNumber(), signDate);
    }

    return RESP.ok("操作成功");
}
```

#### 后端 (OA-7) — 补卡审批（MakeupRequest）

**现状：** OA-7 没有 MakeupRequest 的审批接口  
**目标：** 在 OA-7 增加补卡审批功能，功能与补签类似

**新增接口：**
- `GET /api/v1/admin/makeup/pending` — 待审批补卡列表
- `PUT /api/v1/admin/makeup/{id}/approve` — 批准（更新 attendance 的 check_in_time/check_out_time + 重算）
- `PUT /api/v1/admin/makeup/{id}/reject` — 拒绝

### 5.3 审批撤销功能

**现状：** 已批准的申请无法撤销  
**目标：** 增加撤销操作，撤销后触发考勤重算

#### 后端 (OA-7) — 新增撤销接口

**请假撤销：**
- `PUT /api/v1/admin/leave/{id}/revoke` — 撤销已批准的请假

**补签撤销：**
- `PUT /api/v1/admin/attendance/retroactive/{id}/revoke` — 撤销已批准的补签

**规则：** 撤销后将 attendance 的对应变更还原，然后触发考勤重算

### 5.4 管理员首页 — 今日实时统计

#### 后端 (OA-7) — 补充 SignController

**现状：** `GET /attendance/today/stats` 已有部分统计  
**目标：** 增加更完整的实时统计，专门用于管理员首页

**新增/补充接口：**
```java
// 今日考勤实时统计（管理员首页顶部展示）
@GetMapping("/today/realtime-stats")
public RESP todayRealtimeStats() {
    LocalDate today = LocalDate.now();
    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("total", attendanceDao.countByDate(today));                    // 总人数
    stats.put("checkedIn", attendanceDao.countCheckedInByDate(today));       // 已签到
    stats.put("notCheckedIn", total - checkedIn - onLeave);                  // 未签到
    stats.put("late", attendanceDao.countLateByDate(today));                // 迟到
    stats.put("onLeave", attendanceDao.countLeaveByDate(today));            // 请假中
    stats.put("makeupPending", attendanceDao.countMakeupPendingByDate(today)); // 补卡审批中
    return RESP.ok(stats);
}
```

**DAO 新增方法：**
```java
@Select("SELECT COUNT(*) FROM day.attendance WHERE date=#{date} AND today_status IN ('NOT_CHECKED_IN')")
int countNotCheckedInByDate(LocalDate date);

@Select("SELECT COUNT(*) FROM day.attendance WHERE date=#{date} AND today_status='MAKEUP_PENDING'")
int countMakeupPendingByDate(LocalDate date);
```

#### 前端 — 新增今日统计组件

**文件：** 修改 `Dashboard.vue`，或新建实时统计卡片区域

在数据面板顶部增加一行卡片：
- 总人数 / 已签到 / 未签到 / 迟到 / 请假中 / 补卡审批中

### 5.5 昨日考勤统计

#### 后端 (OA-7) — 新增接口

```java
@GetMapping("/yesterday/stats")
public RESP yesterdayStats() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    // 按 attendance_status 分组统计
    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("date", yesterday.toString());
    stats.put("normal", attendanceDao.countByStatus(yesterday, "NORMAL"));
    stats.put("late", attendanceDao.countByStatus(yesterday, "LATE"));
    stats.put("early", attendanceDao.countByStatus(yesterday, "EARLY"));
    stats.put("lateEarly", attendanceDao.countByStatus(yesterday, "LATE_EARLY"));
    stats.put("leave", attendanceDao.countByStatus(yesterday, "LEAVE"));
    stats.put("dayOff", attendanceDao.countByStatus(yesterday, "DAY_OFF"));
    stats.put("absence", attendanceDao.countByStatus(yesterday, "ABSENCE"));
    stats.put("missingCard", attendanceDao.countByStatus(yesterday, "MISSING_CARD"));
    stats.put("holiday", attendanceDao.countByStatus(yesterday, "HOLIDAY"));
    return RESP.ok(stats);
}
```

**DAO 新增方法：**
```java
@Select("SELECT COUNT(*) FROM day.attendance WHERE date=#{date} AND attendance_status=#{status}")
int countByStatus(@Param("date") LocalDate date, @Param("status") String status);
```

#### 前端 — 昨日统计展示

在 Dashboard 增加"昨日考勤"卡片区域，用饼图或列表展示各状态分布。

---

## 6. Phase 3：节假日与考勤规则模块（中优先级）

### 6.1 Holiday CRUD 完整实现

#### 后端 (OA-7) — 新建 Holiday 实体和完整 CRUD

**文件：** `com.oa7.pojo.Holiday.java`
```java
@Data
public class Holiday {
    private LocalDate date;
    private HolidayType type;    // WORKDAY / HOLIDAY / REST_DAY
    private String description;  // 节假日说明（如"国庆节"）
    private Integer year;        // 年份，便于查询
}
```

**文件：** `com.oa7.dao.HolidayDao.java` — 增加 CRUD 方法
```java
@Select("SELECT * FROM day.holiday WHERE year=#{year} ORDER BY date")
List<Holiday> selectByYear(int year);

@Insert("INSERT INTO day.holiday(date, type, description, year) VALUES(...)")
int insert(Holiday holiday);

@Update("UPDATE day.holiday SET type=#{type}, description=#{description} WHERE date=#{date}")
int update(Holiday holiday);

@Delete("DELETE FROM day.holiday WHERE date=#{date}")
int delete(LocalDate date);

@Select("SELECT * FROM day.holiday WHERE date BETWEEN #{start} AND #{end} ORDER BY date")
List<Holiday> selectByDateRange(LocalDate start, LocalDate end);
```

**文件：** `com.oa7.service.HolidayService.java` — 业务逻辑
- 批量导入国家法定节假日（支持 CSV/JSON 导入）
- 获取指定日期范围的所有节日类型
- 判断某天是否为工作日

**文件：** `com.oa7.controller.HolidayController.java`

```java
@RestController
@RequestMapping("/holidays")
public class HolidayController {
    @GetMapping("/year/{year}")     // 获取某年所有假期配置
    @PostMapping("/batch")          // 批量导入
    @PutMapping("/{date}")          // 修改某天的类型
    @DeleteMapping("/{date}")       // 删除某天的配置
    @GetMapping("/range")           // 查询日期范围内的假期
}
```

#### 前端 — 节假日管理页面

**文件：** `src/components/admin/HolidayManage.vue`（新建）

功能需求：
- 年度日历视图展示全年假期/工作日/休息日
- 支持点击切换日期类型（WORKDAY ↔ HOLIDAY ↔ REST_DAY）
- 支持批量导入节假日数据（预设国家法定假日按钮）
- 支持按年查询

**路由：** `/admin-home/holiday-manage`

### 6.2 AttendanceRule 考勤规则

#### 后端 — 新建 AttendanceRule 实体和 CRUD

**文件：** `com.oa2/oa7.pojo.AttendanceRule.java`（两边共用）
```java
@Data
public class AttendanceRule {
    private Integer id;
    private String ruleName;            // 规则名称（默认规则/部门规则）
    private Integer deptId;             // 部门ID（null=全局规则）
    private LocalTime workStartTime;    // 上班时间（默认 09:00）
    private LocalTime workEndTime;      // 下班时间（默认 18:00）
    private Integer lateThresholdMin;   // 迟到阈值分钟（超过此值才算迟到）
    private Integer earlyThresholdMin;  // 早退阈值分钟
    private Integer maxCheckInRadius;   // 签到最大半径（米），0=不限制
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**文件：** `com.oa7.controller.AttendanceRuleController.java`

```java
@RestController
@RequestMapping("/attendance-rules")
public class AttendanceRuleController {
    @GetMapping("/default")      // 获取默认规则
    @PutMapping("/default")      // 更新默认规则
    @GetMapping("/dept/{deptId}") // 获取部门规则
    @PutMapping("/dept/{deptId}") // 更新部门规则
}
```

#### 前端 — 考勤规则管理页面

**文件：** `src/components/admin/AttendanceRuleManage.vue`（新建）

功能需求：
- 配置上下班时间（time picker）
- 配置迟到/早退阈值（number input，分钟）
- 支持全局默认规则 + 部门规则
- 实时预览规则效果（如"09:15签到 → 迟到15分钟"）

**路由：** `/admin-home/attendance-rule`

---

## 7. Phase 4：定时任务与月度统计（中优先级）

### 7.1 定时任务完善

#### 后端 (OA-7) — 新增日报/月报生成

**文件：** `com.oa7.service.DailyReportService.java`（新建）

```java
@Scheduled(cron = "0 0 1 * * ?")   // 每天 01:00
public void generateDailyReport() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    // 1. 统计昨天各考勤状态人数
    // 2. 统计出勤率 = 正常出勤人数 / 应出勤人数
    // 3. 写入 daily_report 表（如需要）
    // 4. 生成通知推送给管理员
}
```

**文件：** `com.oa7.service.MonthlyReportService.java`（新建）

```java
@Scheduled(cron = "0 0 6 1 * ?")   // 每月1号 06:00
public void generateMonthlyReport() {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    // 1. 统计上月每位员工的考勤汇总
    // 2. 统计各部门的考勤率
    // 3. 写入 monthly_report 表
    // 4. 生成通知推送给管理员
}
```

**数据库新增表：**
```sql
CREATE TABLE IF NOT EXISTS `daily_report` (
    `id` int AUTO_INCREMENT,
    `report_date` date NOT NULL,
    `total_employees` int,
    `normal_count` int,
    `late_count` int,
    `early_count` int,
    `leave_count` int,
    `absence_count` int,
    `missing_card_count` int,
    `attendance_rate` decimal(5,2),
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date` (`report_date`)
);

CREATE TABLE IF NOT EXISTS `monthly_report` (
    `id` int AUTO_INCREMENT,
    `year_month` varchar(7) NOT NULL,     -- "2026-07"
    `emp_id` int NOT NULL,
    `work_days` int,                      -- 应出勤天数
    `actual_days` int,                    -- 实际出勤天数
    `late_count` int,
    `early_count` int,
    `leave_count` int,
    `absence_count` int,
    `missing_card_count` int,
    `attendance_rate` decimal(5,2),
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_emp_month` (`emp_id`, `year_month`)
);
```

### 7.2 个人月度统计

#### 后端 (OA-7) — 新增接口

```java
@GetMapping("/statistics/personal")
public RESP personalStatistics(
    @RequestParam int empId,
    @RequestParam String yearMonth  // "2026-07"
) {
    // 从 monthly_report 表获取
}
```

#### 前端 — 员工端月度统计页面

**文件：** `src/components/emp/EmpMonthlyStats.vue`（新建，或在 EmpHome 增加入口）

展示：出勤天数、迟到次数、请假次数、调休次数、缺卡次数、旷工次数、出勤率

**路由：** `/emp-home/monthly-stats`

### 7.3 部门月度统计

#### 后端 (OA-7) — 新增接口

```java
@GetMapping("/statistics/department")
public RESP departmentStatistics(
    @RequestParam int deptId,
    @RequestParam String yearMonth
) {
    // 部门出勤率/迟到率/请假率/旷工率
}
```

#### 前端 — 管理端部门考勤统计

**文件：** 修改 `SignStatistics.vue` 或新建页面，增加月度部门考勤率趋势图

---

## 8. Phase 5：出差与外勤模块（低优先级）

### 8.1 出差模块

#### 后端 — 新建 BusinessTrip 实体和服务

**POJO：** `BusinessTrip.java`（OA-2）
```java
@Data
public class BusinessTrip {
    private String id;
    private int number;
    private String name;
    private String deptName;
    private String destination;      // 出差地点
    private String startDate;
    private String endDate;
    private String reason;
    private String status;           // 待审批 / 已批准 / 已拒绝
    private int version;
}
```

**Service：** 申请 → 审批通过 → 更新 attendance.todayStatus=BUSINESS_PENDING / attendance_status=BUSINESS_TRIP

**审批：** OA-7 增加出差审批功能

**前端：** 员工出差申请表 + 管理员审批页

### 8.2 外勤模块

与出差模块类似，状态对应 `FIELD_PENDING` / `FIELD_WORK`。

外勤与出差的区别：
- 外勤通常为半天/一天内（无需过夜）
- 出差通常跨天
- 外勤允许签到（在外地签到），出差则免签到

---

## 9. Phase 6：权限完善与边界场景（持续优化）

### 9.1 权限模型完善

**现状：** 仅区分员工和管理员，无中间角色  
**目标：** 增加"主管"角色，支持审批权限

| 角色 | 权限 |
|------|------|
| 员工 | 签到/签退/请假/补卡/补签/查看个人记录 |
| 主管 | 审批下属请假/补卡/补签（新增角色和页面） |
| 管理员 | 查看统计/管理 Holiday/修改规则/管理员工 |

### 9.2 所有边界场景覆盖

| 场景 | 处理逻辑 | 优先级 |
|------|---------|--------|
| 迟到→请假审批通过→最终LEAVE | 重算时优先检查 LEAVE | Phase 1 |
| 请假+签到→最终LEAVE | 重算时签到无效 | Phase 1 |
| 节假日签到→HOLIDAY | 重算时 Holiday 优先级最高 | Phase 1 |
| 补卡审批通过→重算 | 补卡后更新签到时间→重算 | Phase 2 |
| 审批撤销→重算 | 还原变更→重算 | Phase 2 |
| 连续多天请假→中间有周末 | 周末自动 REST_DAY | Phase 3 |

### 9.3 现有代码清理

- 移除 OA-2 中已废弃的 `SignController`（如果有）
- 统一 OA-2 和 OA-7 中共用的实体类（建议抽取 common jar）
- 统一 RESP 响应格式
- 配置统一的异常处理

---

## 10. 数据库变更清单

### 10.1 已有表变更

| 表名 | 变更类型 | 说明 |
|------|---------|------|
| `attendance` | 字段类型调整 | `today_status` 和 `attendance_status` 字段建议用 VARCHAR 存枚举名（兼容现有数据） |
| `attendance` | 新增索引 | `KEY idx_emp_date (emp_id, date)` |
| `leave` | 补充字段 | OA-7 需要增加 `duration` 字段匹配 OA-2 |
| `makeup_request` | 审批状态同步 | 确保 OA-7 有对应的审批表数据 |

### 10.2 新增表

| 表名 | 说明 | 所属 Phase |
|------|------|-----------|
| `holiday` | 如果不存在则创建 | Phase 3 |
| `daily_report` | 日报统计 | Phase 4 |
| `monthly_report` | 月报统计 | Phase 4 |
| `attendance_rule` | 考勤规则配置 | Phase 3 |
| `business_trip` | 出差申请 | Phase 5 |
| `field_work` | 外勤申请 | Phase 5 |

### 10.3 数据迁移注意事项

- `attendance` 表现有数据的 `today_status` 和 `attendance_status` 为 VARCHAR 字符串，使用枚举名存储兼容
- 旧版 `sign` 表数据保留不动（已有历史数据），新功能统一走 `attendance` 表
- 枚举值转换：代码层用枚举，数据库层用字符串（`EnumType.STRING`）

---

## 11. API 总览与规划

### 11.1 已有 API（无需修改）

| 路径 | 方法 | 说明 | 所属服务 |
|------|------|------|---------|
| `/api/v1/employee/login` | POST | 员工登录 | OA-2 |
| `/api/v1/employee/logout` | POST | 员工退出 | OA-2 |
| `/api/v1/employee/profile` | GET | 员工信息 | OA-2 |
| `/api/v1/employee/profile` | PUT | 更新信息 | OA-2 |
| `/api/v1/employee/password` | PUT | 修改密码 | OA-2 |
| `/api/v1/employee/attendance/check-in` | POST | 签到 | OA-2 |
| `/api/v1/employee/attendance/check-out` | POST | 签退 | OA-2 |
| `/api/v1/employee/attendance/today` | GET | 今日状态 | OA-2 |
| `/api/v1/employee/attendance/my-records/page` | GET | 考勤记录 | OA-2 |
| `/api/v1/employee/leave/apply` | POST | 请假申请 | OA-2 |
| `/api/v1/employee/leave/my-list` | GET | 我的请假 | OA-2 |
| `/api/v1/employee/leave/today-status` | GET | 今日请假状态 | OA-2 |
| `/api/v1/employee/attendance/retroactive/apply` | POST | 补签申请 | OA-2 |
| `/api/v1/employee/attendance/retroactive/my-list` | GET | 补签记录 | OA-2 |
| `/api/v1/employee/notifications` | GET | 通知列表 | OA-2 |
| `/api/v1/employee/notifications/unread-count` | GET | 未读数 | OA-2 |
| `/api/v1/employee/notifications/{id}/read` | PUT | 标记已读 | OA-2 |
| `/api/v1/employee/notifications/read-all` | PUT | 全部已读 | OA-2 |
| `/api/v1/employee/location/address` | GET | 地址解析 | OA-2 |
| `/api/v1/admin/auth/login` | POST | 管理员登录 | OA-7 |
| `/api/v1/admin/auth/profile` | GET | 管理员信息 | OA-7 |
| `/api/v1/admin/auth/logout` | POST | 管理员退出 | OA-7 |
| `/api/v1/admin/employees` | GET/POST | 员工管理 | OA-7 |
| `/api/v1/admin/employees/{number}` | PUT/DELETE | 员工管理 | OA-7 |
| `/api/v1/admin/departments` | GET/POST | 部门管理 | OA-7 |
| `/api/v1/admin/departments/{id}` | PUT | 部门管理 | OA-7 |
| `/api/v1/admin/duties` | GET/POST | 职务管理 | OA-7 |
| `/api/v1/admin/duties/{id}` | PUT | 职务管理 | OA-7 |
| `/api/v1/admin/leave/pending` | GET | 待审批请假 | OA-7 |
| `/api/v1/admin/leave/list` | GET | 已审批列表 | OA-7 |
| `/api/v1/admin/leave/{id}/approve` | PUT | 批准请假 | OA-7 |
| `/api/v1/admin/leave/{id}/reject` | PUT | 拒绝请假 | OA-7 |
| `/api/v1/admin/attendance/today/signed` | GET | 今日已签到 | OA-7 |
| `/api/v1/admin/attendance/today/unsigned` | GET | 今日未签到 | OA-7 |
| `/api/v1/admin/attendance/daily-statistics` | GET | 每日统计 | OA-7 |
| `/api/v1/admin/attendance/daily-details` | GET | 每日详情 | OA-7 |
| `/api/v1/admin/attendance/statistics/chart` | GET | 图表数据 | OA-7 |
| `/api/v1/admin/attendance/unsigned` | GET | 未签到列表 | OA-7 |
| `/api/v1/admin/attendance/retroactive/pending` | GET | 待审批补签 | OA-7 |
| `/api/v1/admin/attendance/retroactive/{id}/approve` | PUT | 批准补签 | OA-7 |
| `/api/v1/admin/attendance/retroactive/{id}/reject` | PUT | 拒绝补签 | OA-7 |
| `/api/v1/admin/notification` | GET | 通知列表 | OA-7 |
| `/api/v1/admin/notification/unread-count` | GET | 未读数 | OA-7 |

### 11.2 新增/修改 API 总览

#### Phase 1

| 路径 | 方法 | 说明 | 服务 |
|------|------|------|------|
| 无新增 API | | 主要是后端重构枚举和重算服务 | |

#### Phase 2

| 路径 | 方法 | 说明 | 服务 |
|------|------|------|------|
| `/api/v1/admin/leave/{id}/revoke` | PUT | 撤销请假审批 | OA-7 |
| `/api/v1/admin/attendance/retroactive/{id}/revoke` | PUT | 撤销补签 | OA-7 |
| `/api/v1/admin/makeup/pending` | GET | 待审批补卡 | OA-7 |
| `/api/v1/admin/makeup/{id}/approve` | PUT | 批准补卡 | OA-7 |
| `/api/v1/admin/makeup/{id}/reject` | PUT | 拒绝补卡 | OA-7 |
| `/api/v1/admin/attendance/today/realtime-stats` | GET | 实时统计 | OA-7 |
| `/api/v1/admin/attendance/yesterday/stats` | GET | 昨日统计 | OA-7 |
| `/api/v1/admin/attendance/status/count` | GET | 按状态统计 | OA-7 |

#### Phase 3

| 路径 | 方法 | 说明 | 服务 |
|------|------|------|------|
| `/api/v1/admin/holidays/year/{year}` | GET | 某年假期 | OA-7 |
| `/api/v1/admin/holidays/batch` | POST | 批量导入 | OA-7 |
| `/api/v1/admin/holidays/{date}` | PUT | 修改假期 | OA-7 |
| `/api/v1/admin/holidays/{date}` | DELETE | 删除假期 | OA-7 |
| `/api/v1/admin/holidays/range` | GET | 日期范围查询 | OA-7 |
| `/api/v1/admin/attendance-rules/default` | GET/PUT | 默认考勤规则 | OA-7 |
| `/api/v1/admin/attendance-rules/dept/{deptId}` | GET/PUT | 部门考勤规则 | OA-7 |

#### Phase 4

| 路径 | 方法 | 说明 | 服务 |
|------|------|------|------|
| `/api/v1/admin/statistics/daily?date=` | GET | 日报详情 | OA-7 |
| `/api/v1/admin/statistics/monthly?yearMonth=` | GET | 月报(部门) | OA-7 |
| `/api/v1/employee/statistics/personal?yearMonth=` | GET | 个人月报 | OA-7 |
| `/api/v1/admin/statistics/department?deptId=&yearMonth=` | GET | 部门月报 | OA-7 |

#### Phase 5

| 路径 | 方法 | 说明 | 服务 |
|------|------|------|------|
| `/api/v1/employee/business-trip/apply` | POST | 出差申请 | OA-2 |
| `/api/v1/employee/business-trip/my-list` | GET | 出差记录 | OA-2 |
| `/api/v1/admin/business-trip/pending` | GET | 待审批出差 | OA-7 |
| `/api/v1/admin/business-trip/{id}/approve` | PUT | 批准出差 | OA-7 |
| ...外勤类似... | | | |

---

## 12. 开放问题

以下问题在开发过程中需要进一步澄清：

1. **日终结算 vs 实时重算**：日终结算（23:59）是批量全量重算，审批通过是单次增量重算。两者之间是否存在冲突？建议：日终结算统一处理当天所有记录，审批触发的重算在审批通过后立即执行，日终结算时再次覆盖确认。

2. **Attendance 记录创建时机**：是凌晨00:00统一创建当天全部员工的记录，还是员工首次签到时按需创建（UPSERT）？当前签到接口使用 `INSERT ... ON DUPLICATE KEY UPDATE` 支持按需创建，但凌晨创建便于统计"未签到"人数。建议：两者并存——凌晨提前创建（标注 NOT_CHECKED_IN），签到接口用 UPSERT 更新。

3. **旧版 sign 表数据迁移**：现有 `day.sign` 表有大量历史数据，新 `day.attendance` 表从何时开始使用？建议：从当前日期开始使用新表，历史数据留在 sign 表中供查询。或者写迁移脚本将历史 sign 数据转入 attendance。

4. **考核规则优先级**：当存在全局规则和部门规则时，如何确定优先级？（部门规则覆盖全局规则？）

5. **出差/外勤的签到行为**：出差/外勤状态下是否允许签到？不允许的话应在前端限制，允许的话签到记录是否需要特殊标记？

<flowchart>
### 实施路线图

```
Phase 1 ──── 核心考勤体系完善 ─────────────────────────────────
  ├─ 创建状态枚举类 (TodayStatus, AttendanceStatus, HolidayType)
  ├─ 修改 Attendance 实体字段类型
  ├─ 凌晨自动创建 Attendance 记录
  └─ 统一考勤重算服务 (RecalculateAttendance)

Phase 2 ──── 审批联动与状态重算 ────────────────────────────────
  ├─ 请假审批/拒绝联动考勤重算
  ├─ 补签/补卡审批联动考勤重算
  ├─ 审批撤销功能 (revoke)
  ├─ 管理员首页今日实时统计
  └─ 昨日考勤统计

Phase 3 ──── 节假日与考勤规则模块 ──────────────────────────────
  ├─ Holiday 实体 + CRUD + 管理页面
  ├─ AttendanceRule 实体 + CRUD + 管理页面
  └─ 规则驱动考勤判断逻辑

Phase 4 ──── 定时任务与月度统计 ────────────────────────────────
  ├─ 日报生成定时任务 (01:00)
  ├─ 月报生成定时任务 (每月1号)
  ├─ 个人月度统计 API + 前端
  └─ 部门月度统计 API + 前端

Phase 5 ──── 出差与外勤模块（可选）────────────────────────────
  ├─ BusinessTrip 出差申请+审批
  └─ FieldWork 外勤申请+审批

Phase 6 ──── 权限完善与边界场景（持续）────────────────────────
  ├─ 主管角色 + 审批权限
  ├─ 所有边界场景覆盖
  └─ 代码清理与重构
```
</flowchart>

---

> **本文件为项目整体功能规划文档，所有后续开发应以此文档为指导。**  
> 每个 Phase 开始前应编写详细的实现计划（Implementation Plan）。  
> 如有与业务规范不一致之处，以业务规范文档为准。
