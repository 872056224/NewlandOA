# 工资核算功能设计方案

## 一、岗位薪资标准

| 职务 | duty_id | 月薪 |
|------|---------|------|
| 董事长 | 17 | 50,000 |
| 部长 | 1 | 35,000 |
| 副部长 | 2 | 30,000 |
| 组长 | 3 | 25,000 |
| 副组长 | 4 | 20,000 |
| Java软件架构师 | 10 | 15,000 |
| Java软件工程师 | 9 | 8,000 |
| Web前端工程师 | 16 | 8,000 |
| 普通员工 | 5 | 6,000 |

## 二、计算公式

```
当月工资 = 基础月薪 - 缺时扣款 + 加班工资 + 请假工资
```

### 拆解

```
应出勤天数 = 当月工作日天数（排除节假日/休息日）
日工资 = 基础月薪 ÷ 应出勤天数
小时工资 = 日工资 ÷ 9（核心工时）
分钟工资 = 小时工资 ÷ 60
```

### 扣减项

| 项目 | 计算方式 | 说明 |
|------|---------|------|
| 缺时扣款 | 分钟工资 × 缺时总分钟数 | 迟到/早退/未打卡的分钟数 |
| 请假调整 | 日工资 × 0.8 × 请假天数 | 请假当天发80%工资，相当于扣20% |

### 加项

| 项目 | 计算方式 | 说明 |
|------|---------|------|
| 加班工资 | 小时工资 × 2 × 加班总小时数 | 所有已批准的 overtime.actual_hours 之和 |

### 最终公式

```
当月工资 = 基础月薪 
          - (分钟工资 × 缺时总分钟数) 
          + (小时工资 × 2 × 加班总小时数) 
          - (日工资 × 0.2 × 请假天数)
```

### 示例

假设部长月薪 35,000，7月应出勤23天：
- 日工资 = 35,000 ÷ 23 = 1,521.74
- 小时工资 = 1,521.74 ÷ 9 = 169.08
- 分钟工资 = 169.08 ÷ 60 = 2.82

情况A：满勤无加班 → 35,000
情况B：缺时60分钟 + 加班10小时 + 请假1天：
  - 缺时扣款 = 2.82 × 60 = 169
  - 加班工资 = 169.08 × 2 × 10 = 3,382
  - 请假扣款 = 1,521.74 × 0.2 = 304
  - 应发 = 35,000 - 169 + 3,382 - 304 = 37,909

## 三、数据库

```sql
CREATE TABLE salary_detail (
    id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL,
    year_month VARCHAR(7) NOT NULL COMMENT '年月 2026-07',
    base_salary DECIMAL(10,2) NOT NULL COMMENT '基础月薪',
    work_days INT NOT NULL COMMENT '应出勤天数',
    daily_wage DECIMAL(10,2) NOT NULL COMMENT '日工资',
    hourly_wage DECIMAL(10,2) NOT NULL COMMENT '小时工资',
    
    total_missing_minutes INT DEFAULT 0 COMMENT '缺时总分钟数',
    missing_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '缺时扣款',
    
    overtime_hours DECIMAL(5,1) DEFAULT 0 COMMENT '加班总小时数',
    overtime_pay DECIMAL(10,2) DEFAULT 0 COMMENT '加班工资',
    
    leave_days DECIMAL(4,1) DEFAULT 0 COMMENT '请假天数',
    leave_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '请假扣款',
    
    final_salary DECIMAL(10,2) NOT NULL COMMENT '实发工资',
    status VARCHAR(20) DEFAULT 'CALCULATED' COMMENT 'CALCULATED/CONFIRMED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_emp_month (emp_id, year_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

## 四、后端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/salary/calculate/{yearMonth}` | 计算某月所有员工工资（管理端，仅CHAIRMAN/HR_DIRECTOR） |
| GET | `/salary/my/{yearMonth}` | 查看自己的工资单（员工端） |
| GET | `/salary/list/{yearMonth}` | 查看某月所有工资单（管理端） |

## 五、前端页面

- **员工端**：`EmpSalary.vue` — 查看自己的当月工资明细（各项扣减/加项）
- **管理端**：`SalaryManage.vue` — 查看全员工资 + 触发核算

## 六、实施步骤

| Step | 内容 |
|------|------|
| 1 | 建表 salary_detail |
| 2 | OA-7: Salary实体 + DAO + Service（核算逻辑）+ Controller |
| 3 | OA-2: Salary查询接口 |
| 4 | 前端员工工资页面 |
| 5 | 前端管理端工资页面 |
| 6 | 路由 + 菜单 |
