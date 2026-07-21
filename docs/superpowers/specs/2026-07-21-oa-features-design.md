# OA 系统功能扩展设计文档

- **日期**: 2026-07-21
- **项目**: Newland OA 系统
- **作者**: AI 架构设计

---

## 1. 概述

本文档描述 Newland OA 系统新增功能的设计方案，涵盖三个优先级的功能模块，分三个 Phase 增量实施。

### 系统架构

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Vue 3 FE    │───▶│  Gateway     │───▶│  Nacos       │
│  (port 5173) │    │  (port 8888) │    │  (port 8848) │
└──────────────┘    └──────┬───────┘    └──────────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
      ┌────────────┐ ┌──────────┐ ┌──────────┐
      │ Emp Service │ │ Admin    │ │ AI       │
      │ (8081)      │ │ (8082)   │ │ (8083)   │
      └────────────┘ └──────────┘ └──────────┘
              │            │
              ▼            ▼
      ┌──────────────────────────┐
      │      MySQL `day` DB      │
      └──────────────────────────┘
```

---

## 2. Phase 1 — P0: 请假管理（Leave Management）

### 2.1 数据库变更

**表 `leave` 已存在，需增加 `type` 字段**

```sql
ALTER TABLE `leave`
  ADD COLUMN `type` varchar(20) NOT NULL DEFAULT '事假' COMMENT '请假类型: 事假/病假/年假/调休'
  AFTER `name`;
```

最终 `leave` 表结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | varchar(36) PK | UUID |
| `number` | int(11) | 员工编号 FK→emp |
| `name` | varchar(50) | 员工姓名 |
| `type` | varchar(20) | 请假类型：事假/病假/年假/调休 |
| `dept_name` | varchar(50) | 部门名称 |
| `start_date` | datetime | 开始时间 |
| `end_date` | datetime | 结束时间 |
| `reason` | text | 请假事由 |
| `status` | varchar(20) | 状态：待审批/已批准/已拒绝 |

### 2.2 后端接口

#### 员工端（oa-emp-service, port 8081）

| 方法 | 路径 | 请求 | 响应 | 说明 |
|------|------|------|------|------|
| POST | `/leave/apply` | `{type, startDate, endDate, reason}` | RESP | 提交请假单，自动填入 name/dept_name |
| GET | `/leave/my-list` | `?currentPage=1&pageSize=10` | RESP(data, pageNum, total) | 查看我的请假记录 |

**Controller**: `com.oa2.controller.LeaveController`
**Service**: `com.oa2.service.LeaveService` → `com.oa2.service.impl.LeaveServiceImpl`
**Dao**: `com.oa2.dao.LeaveDao`

#### 管理端（oa-admin-service, port 8082）

| 方法 | 路径 | 请求 | 响应 | 说明 |
|------|------|------|------|------|
| GET | `/leave/pending` | `?currentPage=1&pageSize=10` | RESP(data, pageNum, total) | 待审批列表 |
| PUT | `/leave/{id}/approve` | — | RESP | 通过（status→已批准） |
| PUT | `/leave/{id}/reject` | — | RESP | 拒绝（status→已拒绝） |

**Controller**: `com.oa7.controller.LeaveController`
**Service**: `com.oa7.service.LeaveService` → `com.oa7.service.impl.LeaveServiceImpl`
**Dao**: `com.oa7.dao.LeaveDao`

### 2.3 前端页面

#### 员工端

**新增路由**（`/emp-home` 子路由）：

| 路径 | 组件 | 说明 |
|------|------|------|
| `leave-apply` | `EmpLeaveApply.vue` | 请假申请表单 |
| `leave-list` | `EmpLeaveList.vue` | 我的请假记录 |

**EmpHome.vue** 卡片区新增一张"请假申请"卡片，通往 `leave-apply` 页面。

**EmpLeaveApply.vue**：
- 类型选择器（事假/病假/年假/调休）
- 日期范围选择器（开始 ~ 结束）
- 事由文本框
- 提交按钮 → POST `/api/v1/employee/leave/apply`
- 提交成功后跳转到 leave-list

**EmpLeaveList.vue**：
- el-table 展示：日期范围、类型、事由、状态、提交时间
- el-pagination 分页
- 状态可用 tag 显示：待审批（蓝色）、已批准（绿色）、已拒绝（红色）

#### 管理端

**新增路由**（`/admin-home` 子路由）：`leave-approval` → `LeaveApproval.vue`

**AdminHome.vue** 侧边栏新增菜单项"请假审批"。

**LeaveApproval.vue**：
- 默认展示待审批列表
- 每条记录显示：员工姓名、部门、类型、日期范围、事由
- 操作按钮：批准 / 拒绝
- el-tabs 切换：待审批 / 已审批

### 2.4 关键业务流程

```
员工提交请假申请 → status='待审批'
    ↓
管理员在审批列表看到
    ├── 点击"批准" → status='已批准'
    └── 点击"拒绝" → status='已拒绝'
```

---

## 3. Phase 2 — P1: 补签 + 通知推送

### 3.1 签到补签（Retroactive Sign）

#### 数据库

```sql
CREATE TABLE `retroactive_sign` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `number` int(11) NOT NULL,
  `sign_date` varchar(50) NOT NULL COMMENT '补签的日期(当天)',
  `type` varchar(10) NOT NULL COMMENT 'a=上午/p=下午',
  `reason` varchar(500) DEFAULT NULL COMMENT '补签原因',
  `status` varchar(20) NOT NULL DEFAULT '待审批' COMMENT '待审批/已批准/已拒绝',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_number` (`number`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

#### 后端

**员工端**：
- `POST /attendance/retroactive/apply` — 提交补签申请（参数：type, reason）

**管理端**：
- `GET /attendance/retroactive/pending` — 待审批补签列表
- `PUT /attendance/retroactive/{id}/approve` — 批准补签（修改对应 sign 记录状态为"已签到"）
- `PUT /attendance/retroactive/{id}/reject` — 拒绝补签

#### 前端

**员工端**：`EmpSignIn.vue` 签到卡片下方增加"申请补签"按钮，弹出补签对话框（选择上午/下午 + 填写原因）。

**管理端**：`SignList.vue` 增加标签页"补签审批"，展示补签申请列表及通过/拒绝操作。

### 3.2 通知推送（Notification）

#### 数据库

```sql
CREATE TABLE `notification` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(20) NOT NULL COMMENT 'sign_remind/leave_approval/system',
  `title` varchar(200) NOT NULL,
  `content` text,
  `target_type` varchar(10) NOT NULL COMMENT 'emp/admin/all',
  `target_id` int(11) DEFAULT NULL COMMENT '目标用户(emp_number/admin_id, null=all)',
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `biz_id` varchar(50) DEFAULT NULL COMMENT '关联业务ID(请假ID等)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_read` (`is_read`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

#### 后端

**WebSocket 配置**（oa-emp-service 和 oa-admin-service 各加一份）：
- Spring WebSocket 端点 `/ws/notification`
- 前端通过 SockJS + STOMP 连接
- 连接时校验 session 中的用户身份，绑定 `userId`

**REST API**：
- `GET /notifications` — 获取我的未读通知（分页）
- `PUT /notifications/{id}/read` — 标记已读
- `PUT /notifications/read-all` — 全部已读
- `GET /notifications/unread-count` — 未读数量

**通知触发点**：
1. 签到提醒：`AutoCreateSign` 定时任务发现某员工上午未签到，下班前半小时推提醒
2. 审批提醒：员工提交请假单后，推送给所有管理员"新待审批"
3. 审批结果：管理员审批后，推送给提交员工结果

#### 前端

**全局通知组件**：
- 顶部导航栏增加铃铛图标 + 未读红点数量
- 点击弹出下拉通知列表（最近 10 条）
- 每条通知可点击查看详情或跳转关联页面
- WebSocket 连接：建立 STOMP 订阅 `/user/queue/notifications`

---

## 4. Phase 3 — P2: 公告通知 + 工作日历

### 4.1 公告通知（Announcement）

#### 数据库

```sql
CREATE TABLE `announcement` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `publisher` varchar(50) NOT NULL COMMENT '发布人',
  `priority` varchar(10) NOT NULL DEFAULT 'normal' COMMENT 'normal/important/urgent',
  `status` varchar(10) NOT NULL DEFAULT 'published' COMMENT 'draft/published/archived',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `publish_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

#### 后端

**管理端（oa-admin-service）**：
- `GET /announcements` — 公告列表（分页）
- `POST /announcements` — 发布公告
- `PUT /announcements/{id}` — 编辑公告
- `DELETE /announcements/{id}` — 删除公告

**员工端（oa-emp-service）**：
- `GET /announcements` — 获取已发布公告列表（分页）
- `GET /announcements/{id}` — 公告详情

#### 前端

**管理端**：新增菜单"公告管理" → `AnnouncementManage.vue`（表格 + 发布弹窗）
**员工端**：`EmpHome.vue` 首页增加公告区块（最新 3-5 条滚动），或独立菜单"公司公告"

### 4.2 工作日历（Work Calendar）

#### 后端

**员工端（oa-emp-service）**：
- `GET /calendar/data?year=2026&month=7` — 获取指定月份的签到/请假数据
  - 返回：每天的状态（signed/unsigned/leave），已有的签到时间

不需要建新表——从 `sign` 表和 `leave` 表查询聚合。

#### 前端

**新增路由**（`/emp-home` 子路由）：`work-calendar` → `EmpWorkCalendar.vue`

**侧边栏菜单**：在员工端侧边栏（目前 `EmpHome.vue` 可能没有侧边栏，需要加）或通过首页卡片进入。

**EmpWorkCalendar.vue**：
- 月视图日历，使用 Element Plus `el-calendar` 或手写
- 每天通过自定义 cell 显示：
  - 绿色圆点 → 已签到（全天正常）
  - 灰色圆点 → 未签到（缺卡）
  - 蓝色标记 → 请假中
  - 点击某天弹出当天的签到详情

---

## 5. 实施计划

### Phase 1 — P0：请假管理（预计 ~2 天）

| 步骤 | 内容 | 涉及服务 |
|------|------|---------|
| 1.1 | 数据库加 `type` 字段 | MySQL |
| 1.2 | 员工端后端：LeaveDao + LeaveService + LeaveController | oa-emp-service |
| 1.3 | 管理端后端：LeaveDao + LeaveService + LeaveController | oa-admin-service |
| 1.4 | 前端员工端：EmpLeaveApply + EmpLeaveList | Vue |
| 1.5 | 前端管理端：LeaveApproval + 侧边栏 | Vue |
| 1.6 | 联调测试 | — |

### Phase 2 — P1：补签 + 通知（预计 ~3 天）

| 步骤 | 内容 |
|------|------|
| 2.1 | 数据库建 `retroactive_sign` + `notification` 表 |
| 2.2 | 补签后端（两条服务各一套） |
| 2.3 | 补签前端 |
| 2.4 | WebSocket 后端配置 |
| 2.5 | 通知后端 API |
| 2.6 | 通知前端组件 |
| 2.7 | 触发点集成（签到提醒、审批提醒） |
| 2.8 | 联调测试 |

### Phase 3 — P2：公告 + 日历（预计 ~2 天）

| 步骤 | 内容 |
|------|------|
| 3.1 | 数据库建 `announcement` 表 |
| 3.2 | 公告后端（两条服务） |
| 3.3 | 公告前端（管理端 + 员工端） |
| 3.4 | 工作日历后端 |
| 3.5 | 工作日历前端 |
| 3.6 | 联调测试 |

---

## 6. 关键设计约束

1. **代码风格一致性**：所有新 Controller/Service/Dao 严格遵循现有代码模式——`@RestController` + `@CrossOrigin` + `@Autowired` + `RESP` 返回
2. **Session 认证**：沿用现有 `HttpSession` 机制，不引入 JWT
3. **前端风格**：保持 Apple 设计语言（CSS 变量、苹果卡片、圆角按钮）
4. **响应式数据**：所有组件用 `<script setup lang="ts">` + `ref`/`reactive`
5. **通知不阻塞**：WebSocket 连接失败不影响主功能使用
