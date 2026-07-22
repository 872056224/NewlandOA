# OA 考勤系统 - 完整设计文档 & 进度追踪

> Version: 1.0
> Last Updated: 2026-07-22
> Based on: [业务规范](./attendance-business-spec.md)

---

## 一、项目背景

### 1.1 旧系统问题

旧系统使用 `sign` 表，每天为每位员工生成两条记录（上午 type='a'、下午 type='p'），存在以下问题：

- ❌ 签到时间用预设值（`08:30:00:000`），非真实时间
- ❌ 签到是 INSERT 新记录而非 UPDATE，造成数据重复
- ❌ 无实时/最终状态分离，只有 `state='已签到'/'未签到'`
- ❌ 无日历系统，通过星期几判断工作日
- ❌ 无日终结算，无法计算 LATE/ABSENCE 等最终状态
- ❌ 无请假 duration，不支持半天请假
- ❌ 统计全表加载到 JVM 内存

### 1.2 目标

将签到/请假/补卡/调休/出差/外勤等所有业务统一到 `attendance` 核心表，实现：
- **单一事实来源**：所有统计基于 attendance
- **双状态分离**：实时状态（today_status）+ 最终状态（attendance_status）
- **真实时间记录**：签到/签退记录实际时间
- **日终结算**：23:59 自动计算最终状态

---

## 二、系统架构

```
┌─────────────────────────────────────────────────────────┐
│                   前端 (Vue 3)                           │
│  EmpSignIn.vue  │  Dashboard.vue  │  SignList.vue       │
└────────┬────────┴───────┬─────────┴──────────┬──────────┘
         │                │                     │
         ▼                ▼                     ▼
┌─────────────────────────────────────────────────────────┐
│               API 网关 (Gateway :8888)                    │
└──────┬──────────────────────┬──────────────────┬─────────┘
       │                      │                  │
       ▼                      ▼                  ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ OA-2 (:8081) │    │ OA-7 (:8082) │    │oa-ai(:8083)  │
│ 员工服务      │    │ 管理员服务    │    │ AI 客服       │
│ Attendance   │    │ Attendance    │    │              │
│ Leave        │    │ Settlement   │    │              │
│ Makeup       │    │ Statistics   │    │              │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           ▼
              ┌─────────────────────┐
              │  MySQL day 数据库    │
              │  attendance (核心)   │
              │  leave / makeup     │
              │  holiday / admin    │
              │  emp / notification │
              └─────────────────────┘
```

---

## 三、数据库设计

### 3.1 attendance（核心考勤表）

```sql
CREATE TABLE day.attendance (
  id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
  emp_id           INT          NOT NULL        COMMENT '员工编号',
  date             DATE         NOT NULL        COMMENT '考勤日期',
  check_in_time    DATETIME                     COMMENT '实际签到时间',
  check_out_time   DATETIME                     COMMENT '实际签退时间',
  today_status     VARCHAR(20)  NOT NULL DEFAULT 'NOT_CHECKED_IN'
                   COMMENT '实时状态',
  attendance_status VARCHAR(20)                 COMMENT '最终状态',
  remark           VARCHAR(500)                 COMMENT '备注',
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_emp_date (emp_id, date),
  KEY idx_date_status (date, attendance_status),
  KEY idx_emp (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录（核心表）';
```

### 3.2 today_status（实时状态）

| 值 | 含义 | 说明 |
|:---|:---|:---|
| `NOT_CHECKED_IN` | 未签到 | 凌晨创建时的默认状态 |
| `CHECKED_IN` | 已签到 | 签到动作触发 |
| `CHECKED_OUT` | 已签退 | 签退动作触发 |
| `LEAVE` | 请假 | 请假审批通过后覆盖 |
| `MAKEUP_PENDING` | 补卡审批中 | 提交补卡后使用 |
| `DAY_OFF` | 调休 | 调休审批通过后覆盖 |

### 3.3 attendance_status（最终状态）

| 值 | 含义 | 优先级 |
|:---|:---|:---:|
| `HOLIDAY` | 节假日 | 最高 |
| `REST_DAY` | 休息日 | ↑ |
| `LEAVE` | 请假 | ↑ |
| `DAY_OFF` | 调休 | ↑ |
| `BUSINESS_TRIP` | 出差 | ↑ |
| `FIELD_WORK` | 外勤 | ↑ |
| `NORMAL` | 正常出勤 | ↑ |
| `LATE` | 迟到 | ↑ |
| `EARLY` | 早退 | ↑ |
| `LATE_EARLY` | 迟到+早退 | ↑ |
| `MISSING_CARD` | 缺卡 | ↑ |
| `ABSENCE` | 旷工 | 最低 |

### 3.4 其他表

```sql
-- 补卡申请表（替代旧的 retroactive_sign）
CREATE TABLE day.makeup_request (
  id          INT          AUTO_INCREMENT PRIMARY KEY,
  emp_id      INT          NOT NULL,
  date        DATE         NOT NULL,
  type        VARCHAR(10)  NOT NULL COMMENT 'CHECK_IN/CHECK_OUT',
  request_time VARCHAR(20) NOT NULL COMMENT 'HH:mm',
  reason      VARCHAR(500) NOT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  version     INT          DEFAULT 0,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_emp_date (emp_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 日历配置表（用于判断工作日/节假日/休息日）
CREATE TABLE day.holiday (
  id     INT          AUTO_INCREMENT PRIMARY KEY,
  date   DATE         NOT NULL UNIQUE,
  type   VARCHAR(20)  NOT NULL COMMENT 'WORKDAY/HOLIDAY/REST_DAY',
  name   VARCHAR(100),
  year   INT          NOT NULL,
  KEY idx_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.5 leave 表扩展

```sql
ALTER TABLE day.leave ADD COLUMN duration VARCHAR(20) DEFAULT 'FULL_DAY'
  COMMENT 'FULL_DAY/HALF_DAY_AM/HALF_DAY_PM';
ALTER TABLE day.leave ADD COLUMN version INT DEFAULT 0;
```

---

## 四、API 设计

### 4.1 员工端（OA-2，经网关 `/api/v1/employee`）

| Method | Path | 说明 | 状态 |
|:---:|:---|:---|:---:|
| `POST` | `/attendance/check-in?coordinates=` | 签到（记录实际时间） | ✅ |
| `POST` | `/attendance/check-out?coordinates=` | 签退（记录实际时间） | ✅ |
| `GET` | `/attendance/today` | 今日考勤状态 | ✅ |
| `GET` | `/attendance/history?page&size` | 历史考勤记录（分页） | ✅ |
| `GET` | `/attendance/my-records` | 旧兼容接口 | ✅ |
| `GET` | `/attendance/my-records/page` | 旧兼容接口 | ✅ |
| `POST` | `/leave/apply` | 提交请假（支持 duration） | ✅ |
| `GET` | `/leave/my-list` | 我的请假记录 | ✅ |
| `POST` | `/makeup/apply` | 提交补卡申请 | ✅ |
| `GET` | `/makeup/my-list` | 我的补卡记录 | ✅ |

### 4.2 管理端（OA-7，经网关 `/api/v1/admin`）

| Method | Path | 说明 | 状态 |
|:---:|:---|:---|:---:|
| `GET` | `/attendance/today/stats` | 今日实时统计（总数/已签到/迟到/请假） | ✅ |
| `GET` | `/attendance/today/signed` | 今日已签到列表 | ⬜（待改） |
| `GET` | `/attendance/daily-statistics` | 日统计 | ⬜（待改） |
| `GET` | `/attendance/daily-details?date=` | 日详情 | ⬜（待改） |
| `GET` | `/leave/pending` | 待审批请假 | ✅ |
| `PUT` | `/leave/{id}/approve` | 审批通过请假 | ✅ |
| `PUT` | `/leave/{id}/reject` | 拒绝请假 | ✅ |
| `GET` | `/attendance/retroactive/pending` | 待审批补卡 | ⬜（待改） |
| `PUT` | `/attendance/retroactive/{id}/approve` | 审批通过补卡 | ⬜（待改） |

---

## 五、定时任务

| 时间 | 任务 | 说明 | 状态 |
|:---:|:---|:---|:---:|
| 00:00 | `AutoCreateSign.create()` | 为每位员工创建当天 attendance | ⬜（待改为 attendance 表） |
| 23:59 | `AttendanceSettlementService.settle()` | 日终结算 attendance_status | ✅ |
| 01:00 | 生成日报 | - | ❌ 未实现 |
| 每月1号 | 生成月报 | - | ❌ 未实现 |

---

## 六、关键业务流程

### 6.1 签到流程

```
员工点击"签到"
  → 前端 POST /attendance/check-in?coordinates=xxx
  → 后端 UPSERT attendance（记录真实时间 check_in_time = now()）
  → 返回实际时间和状态（正常/迟到）
  → 前端展示结果
```

### 6.2 签退流程

```
员工点击"签退"
  → 前端 POST /attendance/check-out?coordinates=xxx
  → 后端 UPDATE attendance（记录真实时间 check_out_time = now()）
  → 返回实际时间和状态（正常/早退）
  → 前端展示结果
```

### 6.3 请假流程

```
员工提交请假（含 duration = FULL_DAY / HALF_DAY_AM / HALF_DAY_PM）
  → OA-2 insert leave 记录
  → 更新对应日期 attendance.today_status = 'LEAVE'
  → 通知所有管理员
  → 管理员审批/拒绝（带乐观锁）
  → 审批后通知员工 + 标记管理员通知已读
  → 触发 RecalculateAttendance()
```

### 6.4 日终结算

```
23:59 定时任务触发
  → 查询当天所有 attendance 记录
  → 查询当天 holiday 类型
  → 对每条记录按优先级计算 attendance_status
  → 更新 attendance.attendance_status
```

### 6.5 状态优先级

```
HOLIDAY > REST_DAY > LEAVE > DAY_OFF > [签退判定] > ... > ABSENCE

签退判定内部优先级：
  check_in > 08:30 && check_out >= 17:30 → LATE
  check_in <= 08:30 && check_out < 17:30 → EARLY
  check_in > 08:30 && check_out < 17:30 → LATE_EARLY
  其他                             → NORMAL
```

---

## 七、边界场景处理

| # | 场景 | 结果 | 状态 |
|:---:|:---|:---:|:---:|
| 1 | 09:20签到, 10:00请假审批通过 | `LEAVE` | ✅ |
| 2 | 全天请假, 员工仍签到 | `LEAVE`（签到无效） | ✅ |
| 3 | 节假日签到 | `HOLIDAY` | ⬜（需 Phase 2） |
| 4 | 调休后签到 | `DAY_OFF` | ❌（需 Phase 3） |
| 5 | 仅签退（未签到） | `ABSENCE`（旷工） | ✅ |
| 6 | 仅签到（未签退） | `MISSING_CARD`（缺卡） | ✅ |
| 7 | 补卡审批通过 | 重新计算 | ⬜（待实现 RecalculateAttendance） |
| 8 | 审批撤销 | 重新计算 | ⬜（待实现 RecalculateAttendance） |

---

## 八、当前进度（2026-07-22）

### Phase 1：核心改造（已完成 ✅）

| 模块 | 状态 | 说明 |
|:---|:---:|:---|
| attendance 表 | ✅ | 已创建，数据已从 sign 表迁移（7月至今） |
| 签到/签退接口 | ✅ | INSERT → UPSERT，记录真实时间 |
| 请假 duration | ✅ | 支持 FULL_DAY/HALF_DAY_AM/HALF_DAY_PM |
| 补卡（替代补签） | ✅ | 新建 `makeup_request` + 接口 |
| 日终结算 | ✅ | 23:59 定时任务，按优先级结算 |
| MyBatis 驼峰 | ✅ | 开启全局 `map-underscore-to-camel-case` |
| 乐观锁 | ✅ | leave 和 retroactive_sign 已加 version |
| 管理员通知 | ✅ | 提交请假/补卡时通知所有管理员 |

### Phase 1 剩余项（待完成 🔄）

| 模块 | 优先级 | 说明 |
|:---|:---:|:---|
| AutoCreateSign 适配 attendance | 高 | 目前还在写旧 `sign` 表，需改为创建 `attendance` 记录 |
| 管理端统计全量改 attendance | 中 | `dailyStatistics`、`chartData` 等方法还在读旧 `sign` 表 |
| 管理端补卡审批 | 中 | 需改为审批 `makeup_request` 表 |
| RecalculateAttendance | 中 | 补卡/撤销时触发重新结算 |
| 前端管理端 Dashboard 适配 | 低 | 今日统计改读新 `/today/stats` |

### Phase 2：日历系统（待开始 📅）

| 模块 | 说明 |
|:---|:---|
| Holiday 管理界面 | 增删改查 |
| 节假日导入 | 年度节假日批量导入 |
| 替换星期几判断 | AutoCreateSign 等 |

### Phase 3：调休/出差/外勤/报表（待开始 📅）

| 模块 | 说明 |
|:---|:---|
| 调休模块 | day_off_request 表 + 审批 |
| 出差模块 | business_trip 表 + 审批 |
| 外勤模块 | field_work 表 + 审批 |
| 月度考勤报表 | 个人/部门维度 |

---

## 九、已部署服务

| 服务 | 端口 | 当前状态 |
|:---|:---:|:---:|
| MySQL | 3306 | ✅ 运行中 |
| Nacos | 8848 | ✅ 运行中 |
| Gateway | 8888 | ✅ 运行中 |
| OA-2（员工服务） | 8081 | ✅ **已更新（attendance 版）** |
| OA-7（管理员服务） | 8082 | ✅ **已更新（attendance 版）** |
| oa-ai-service | 8083 | ✅ 运行中 |
| 前端 Vite | 5173 | ✅ 运行中 |
| Elasticsearch | 9201 | ✅ 运行中 |

---

## 十、验证方式

```bash
# 1. 登录员工
curl -c /tmp/c.txt -X POST http://localhost:8888/api/v1/employee/login \
  -H "Content-Type: application/json" -d '{"number":129,"pwd":"123"}'

# 2. 查今日考勤
curl -b /tmp/c.txt http://localhost:8888/api/v1/employee/attendance/today

# 3. 签到（记录真实时间）
curl -b /tmp/c.txt -X POST "http://localhost:8888/api/v1/employee/attendance/check-in"

# 4. 签退
curl -b /tmp/c.txt -X POST "http://localhost:8888/api/v1/employee/attendance/check-out"

# 5. 提交补卡
curl -b /tmp/c.txt -X POST http://localhost:8888/api/v1/employee/makeup/apply \
  -H "Content-Type: application/json" \
  -d '{"date":"2026-07-22","type":"CHECK_IN","requestTime":"09:00","reason":"忘记签到"}'

# 6. 管理端今日统计
curl http://localhost:8888/api/v1/admin/attendance/today/stats
```

---

## 附录：关键文件索引

### OA-2 新建文件
| 文件 | 路径 |
|:---|:---|
| Attendance.java | `backend/.../OA-2/src/main/java/com/oa2/pojo/Attendance.java` |
| AttendanceDao.java | `backend/.../OA-2/src/main/java/com/oa2/dao/AttendanceDao.java` |
| AttendanceService.java | `backend/.../OA-2/src/main/java/com/oa2/service/AttendanceService.java` |
| AttendanceServiceImpl.java | `backend/.../OA-2/src/main/java/com/oa2/service/impl/AttendanceServiceImpl.java` |
| AttendanceController.java | `backend/.../OA-2/src/main/java/com/oa2/controller/AttendanceController.java` |
| MakeupRequest.java | `backend/.../OA-2/src/main/java/com/oa2/pojo/MakeupRequest.java` |
| MakeupRequestDao.java | `backend/.../OA-2/src/main/java/com/oa2/dao/MakeupRequestDao.java` |
| MakeupRequestService.java | `backend/.../OA-2/src/main/java/com/oa2/service/MakeupRequestService.java` |
| MakeupRequestServiceImpl.java | `backend/.../OA-2/src/main/java/com/oa2/service/impl/MakeupRequestServiceImpl.java` |
| MakeupRequestController.java | `backend/.../OA-2/src/main/java/com/oa2/controller/MakeupRequestController.java` |

### OA-7 新建/修改文件
| 文件 | 路径 |
|:---|:---|
| Attendance.java | `backend/.../OA-7/src/main/java/com/oa7/pojo/Attendance.java` |
| AttendanceDao.java | `backend/.../OA-7/src/main/java/com/oa7/dao/AttendanceDao.java` |
| HolidayDao.java | `backend/.../OA-7/src/main/java/com/oa7/dao/HolidayDao.java` |
| AttendanceSettlementService.java | `backend/.../OA-7/src/main/java/com/oa7/service/AttendanceSettlementService.java` |
| SignController.java | 新增 `GET /today/stats` |
| SignServiceImpl.java | 注入 AttendanceDao |

### 前端修改文件
| 文件 | 路径 |
|:---|:---|
| EmpSignIn.vue | `frontend/src/components/emp/EmpSignIn.vue` |
