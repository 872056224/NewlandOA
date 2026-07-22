下面这份你可以直接保存为：

```text
docs/attendance-business-spec.md
```

然后让 Claude Code 在开发前强制阅读。它已经接近真实企业项目的《业务规范文档》了。

------

# 企业级 OA 考勤系统业务规范（Business Specification）

Version: 1.0

------

# 1. 文档目的

本文档用于定义 OA 考勤模块的业务规则。

所有开发人员、AI Agent、代码生成工具必须以本文档作为唯一业务标准。

任何功能实现不得违反本文档中的业务规则。

------

# 2. 设计原则

## 2.1 单一事实来源（Single Source of Truth）

整个考勤系统以 Attendance 为核心。

以下模块：

- 签到
- 签退
- 请假
- 调休
- 补卡
- 出差
- 外勤
- 节假日

均不得直接生成统计结果。

所有业务最终只能影响：

```text
Attendance
```

统计、报表、工资计算均基于 Attendance。

------

## 2.2 最终状态原则

每位员工每天只能存在一个最终考勤状态。

例如：

```text
2026-07-20

张三

最终状态：

NORMAL
```

而不是：

```text
签到成功
请假成功
补卡成功
```

各种业务只是影响最终状态。

------

## 2.3 实时状态与最终状态分离

系统必须同时维护：

### 实时状态

用于当天管理

例如：

```text
未签到
已签到
已签退
请假中
补卡审批中
```

### 最终状态

用于统计

例如：

```text
正常
迟到
请假
旷工
```

------

# 3. 核心业务对象

## Employee

员工

------

## Attendance

考勤记录

系统核心对象

------

## Holiday

日历配置

------

## LeaveRequest

请假申请

------

## MakeupRequest

补卡申请

------

## DayOffRequest

调休申请

------

## BusinessTrip

出差申请

------

## FieldWork

外勤申请

------

## AttendanceRule

考勤规则

------

# 4. Attendance 生命周期

## 每日零点

系统自动创建 Attendance

```text
待考勤
```

------

## 过程

员工可能：

```text
签到
签退
请假
调休
补卡
出差
外勤
```

------

## 当天结束

23:59

系统统一结算

生成最终状态

------

# 5. Attendance 数据模型

```sql
Attendance
```

| 字段              | 说明     |
| ----------------- | -------- |
| id                | 主键     |
| user_id           | 员工     |
| date              | 日期     |
| check_in_time     | 签到时间 |
| check_out_time    | 签退时间 |
| today_status      | 实时状态 |
| attendance_status | 最终状态 |
| remark            | 备注     |
| created_at        | 创建时间 |
| updated_at        | 更新时间 |

------

# 6. 实时状态定义

```java
enum TodayStatus
{
    NOT_CHECKED_IN,
    CHECKED_IN,
    CHECKED_OUT,
    LEAVE_PENDING,
    MAKEUP_PENDING,
    BUSINESS_PENDING,
    FIELD_PENDING
}
```

说明：

实时状态仅用于当天展示。

不参与统计。

------

# 7. 最终状态定义

```java
enum AttendanceStatus
{
    NORMAL,
    LATE,
    EARLY,
    LATE_EARLY,

    LEAVE,
    DAY_OFF,

    BUSINESS_TRIP,
    FIELD_WORK,

    MISSING_CARD,
    ABSENCE,

    HOLIDAY,
    REST_DAY
}
```

所有统计必须基于 AttendanceStatus。

------

# 8. Holiday（日历系统）

## 原则

禁止通过星期几判断工作日。

错误：

```java
if(dayOfWeek == SATURDAY)
```

禁止。

------

正确：

```java
查询 Holiday
```

决定当天性质。

------

## Holiday类型

```java
enum HolidayType
{
    WORKDAY,

    HOLIDAY,

    REST_DAY
}
```

------

## 示例

```text
2026-10-01

HOLIDAY
2026-10-10

WORKDAY
```

虽然是周六

但需要上班。

------

# 9. 签到规则

## 标准时间

```text
上班：

09:00

下班：

18:00
```

------

## 签到判断

```text
签到 <= 09:00

NORMAL
签到 > 09:00

LATE
```

------

## 签退判断

```text
签退 >= 18:00

正常
签退 < 18:00

EARLY
```

------

## 同时存在

```text
迟到

+

早退
```

最终：

```text
LATE_EARLY
```

------

# 10. 请假规则

## 全天请假

审批通过：

```text
Attendance

↓

LEAVE
```

无需签到。

------

## 半天请假

允许：

```text
上午请假

下午签到
```

建议增加：

```text
leave_duration
```

记录：

```text
FULL_DAY

HALF_DAY

HOUR
```

------

# 11. 调休规则

审批通过：

```text
DAY_OFF
```

无需签到。

------

# 12. 补卡规则

员工提交：

```text
补卡时间

补卡原因
```

------

审批通过：

更新：

```text
check_in_time
```

或：

```text
check_out_time
```

------

重新计算状态。

例如：

```text
原状态：

MISSING_CARD
```

补卡：

```text
08:55
```

重新计算：

```text
NORMAL
```

------

# 13. 出差规则

审批通过：

```text
BUSINESS_TRIP
```

无需签到。

------

# 14. 外勤规则

审批通过：

```text
FIELD_WORK
```

无需签到。

------

# 15. 缺卡规则

情况：

```text
仅签到

未签退
```

或：

```text
仅签退

未签到
```

最终：

```text
MISSING_CARD
```

允许后续补卡。

------

# 16. 旷工规则

旷工不属于实时状态。

仅在结算时产生。

------

## 每日23:59

检查：

```text
未签到

未请假

未调休

未补卡

非节假日

非休息日
```

满足：

```text
ABSENCE
```

------

# 17. 状态优先级

高优先级覆盖低优先级。

```text
HOLIDAY

>

REST_DAY

>

LEAVE

>

DAY_OFF

>

BUSINESS_TRIP

>

FIELD_WORK

>

NORMAL

>

LATE

>

EARLY

>

MISSING_CARD

>

ABSENCE
```

------

# 18. 管理员首页

## 今日实时统计

统计：

```text
已签到

未签到

迟到

请假中

补卡审批中
```

注意：

当天不统计旷工。

------

## 昨日统计

统计：

```text
NORMAL

LATE

EARLY

LEAVE

DAY_OFF

ABSENCE
```

------

# 19. 月度统计

个人统计：

```text
出勤天数

迟到次数

请假次数

调休次数

缺卡次数

旷工次数

出勤率
```

------

部门统计：

```text
部门出勤率

部门迟到率

部门请假率

部门旷工率
```

------

# 20. 定时任务

## 00:00

创建 Attendance

------

## 23:59

结算 Attendance

------

## 01:00

生成日报

------

## 每月1号

生成月报

------

# 21. 权限设计

## 员工

允许：

```text
签到
签退
请假
补卡
查看个人记录
```

------

## 主管

允许：

```text
审批请假

审批补卡

审批调休
```

------

## 管理员

允许：

```text
查看统计

管理Holiday

修改规则
```

------

# 22. 边界场景（必须实现）

## 场景1

09:20签到

10:00请假审批通过

最终：

```text
LEAVE
```

------

## 场景2

全天请假

员工仍然签到

最终：

```text
LEAVE
```

签到无效。

------

## 场景3

国庆节签到

最终：

```text
HOLIDAY
```

签到无效。

------

## 场景4

调休后签到

最终：

```text
DAY_OFF
```

签到无效。

------

## 场景5

补卡审批通过

必须重新计算 Attendance。

------

## 场景6

审批撤销

必须重新计算 Attendance。

------

# 23. 开发约束（强制）

任何模块：

禁止直接修改统计数据。

------

所有统计：

必须来自 Attendance。

------

禁止通过星期几判断工作日。

必须查询 Holiday。

------

任何审批通过、审批撤销、补卡成功后：

必须触发：

```text
RecalculateAttendance()
```

重新计算考勤结果。

------

# 24. 核心原则总结

整个系统只有一个核心对象：

```text
Attendance
```

所有业务：

```text
签到
签退
请假
调休
补卡
出差
外勤
节假日
```

最终都只是影响：

```text
AttendanceStatus
```

统计、报表、工资、绩效均以 Attendance 为唯一可信数据源。

------

不过如果你准备把这个项目做成简历项目甚至毕业设计，我建议再补 **第25章《数据库详细设计》+ 第26章《状态机设计》+ 第27章《接口清单（API）》**，这样 Claude Code 基本能直接生成完整后端，而不是只生成业务骨架。对于 Spring Boot 项目，这三章的价值甚至比前面 24 章还高。