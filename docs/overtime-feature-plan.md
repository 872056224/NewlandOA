# 加班申请功能设计方案

## 一、数据表设计

```sql
CREATE TABLE overtime_request (
    id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL COMMENT '申请人',
    overtime_date DATE NOT NULL COMMENT '加班日期',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    total_hours DECIMAL(4,1) NOT NULL COMMENT '申请总时长(小时)',
    actual_hours DECIMAL(4,1) DEFAULT NULL COMMENT '核定时长(审批后可手动调整)',
    reason VARCHAR(200) DEFAULT '' COMMENT '加班事由',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    reject_reason VARCHAR(200) DEFAULT '' COMMENT '拒绝原因',
    version INT DEFAULT 0 COMMENT '乐观锁',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_emp (emp_id),
    INDEX idx_status (status),
    INDEX idx_date (overtime_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

## 二、业务规则

### 申请条件
- **只能申请非工作日**：周末（周六/周日）或节假日（Holiday表标记为HOLIDAY/REST_DAY）
- **必须提前申请**：不能申请已过去的日期，只能申请当天及未来
- **时长计算**：`end_time - start_time`，按小时计算（如09:00~21:00 = 12小时）
- **加班时长单独统计**：不计入考勤缺时，单独累加到`月加班时长`

### 审批流程
- **审核人范围**：
  - 董事长（CHAIRMAN）— 所有加班单
  - 人事部部长/副部长（HR_DIRECTOR / 人事部DEPT_HEAD）— 所有加班单
  - 本部门部长/副部长（同dept_id的DEPT_HEAD）— 仅本部门
- **审核操作**：
  - **批准**：通过申请，`actual_hours = total_hours`
  - **批准并核减**：手动输入核定时长，`actual_hours = 核减后的时长`
  - **拒绝**：不通过，填拒绝原因

### 工时统计
- 每月统计：`SELECT emp_id, SUM(actual_hours) FROM overtime_request WHERE status='APPROVED' AND overtime_date BETWEEN 月初 AND 月末`
- 员工端可查看当月加班总时长
- 管理端可在月统计中查看

## 三、接口设计

### 员工端（OA-2）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/attendance/overtime/apply` | 提交加班申请 |
| GET | `/attendance/overtime/my-list` | 我的加班记录（分页） |
| GET | `/attendance/overtime/monthly-hours` | 当月加班总时长 |

### 管理端（OA-7）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/attendance/overtime/pending` | 待审批列表（数据隔离） |
| PUT | `/attendance/overtime/{id}/approve` | 批准（可选传 actualHours 核减） |
| PUT | `/attendance/overtime/{id}/reject` | 拒绝 |

### 通知
- 提交申请 → 通知相关审核人（同请假通知规则）
- 审批通过/拒绝 → 通知员工本人

## 四、前端改动

### 员工端
- **EmpOvertimeApply.vue**（新建）：加班申请表单
  - 日期选择器：只能选非工作日
  - 时间段选择：开始/结束时间
  - 事由输入
  - 自动计算时长（如09:00~21:00 = 12小时）
- **EmpOvertimeList.vue**（新建）：加班记录列表
  - 显示历史申请及审批状态
  - 显示当月加班总时长
- 在员工首页增加「加班申请」入口

### 管理端
- **OvertimeApproval.vue**（新建）：加班审批列表
  - 待审批列表 + 已审批历史
  - 批准、核减、拒绝操作
  - 核减时可输入实际时长
- 在管理端侧边栏增加「加班审批」菜单
- 权限：CHAIRMAN/HR_DIRECTOR/本部门DEPT_HEAD 可见

## 五、通知范围

与请假通知规则一致：
```
员工提交加班申请
  → OA-2 调用 selectNotifyTargetIds(empId)
    → 通知：董事长 + 人事部正副部长 + 本部门正副部长
```

```
审核通过/拒绝
  → OA-7 通知员工本人
```

## 六、数据库迁移SQL

```sql
CREATE TABLE IF NOT EXISTS overtime_request (
    id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL,
    overtime_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    total_hours DECIMAL(4,1) NOT NULL,
    actual_hours DECIMAL(4,1) DEFAULT NULL,
    reason VARCHAR(200) DEFAULT '',
    status VARCHAR(20) DEFAULT 'PENDING',
    reject_reason VARCHAR(200) DEFAULT '',
    version INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_emp (emp_id),
    INDEX idx_status (status),
    INDEX idx_date (overtime_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

## 七、实施步骤

| Step | 内容 | 涉及 |
|------|------|------|
| 1 | 数据库建表 | SQL |
| 2 | OA-7: Overtime实体 + DAO + Service + Controller | 后端 |
| 3 | OA-7: 加班审批管理端页面 | 前端 |
| 4 | OA-2: 加班申请员工端页面 | 前端 |
| 5 | OA-2: 加班接口 + 通知联动 | 后端 |
| 6 | 路由 + 菜单 + 权限控制 | 前端 |
| 7 | 月加班时长统计 | 后端 |
