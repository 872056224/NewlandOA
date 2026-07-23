# OA 系统技术架构文档

> 基于项目代码分析，2026-07-23

---

## 一、系统架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (Vue 3 + Vite)                     │
│                   http://localhost:5173                      │
└────────────────────────┬────────────────────────────────────┘
                         │ proxy /api
                         ▼
┌─────────────────────────────────────────────────────────────┐
│          API 网关 (Spring Cloud Gateway)                     │
│                http://localhost:8888                         │
│                                                             │
│  /api/v1/employee/**  →  lb://oa-emp-service  (8081)       │
│  /api/v1/admin/**     →  lb://oa-admin-service  (8082)     │
│  /api/v1/ai/**        →  http://localhost:8083              │
└────┬──────────────────────┬──────────────────────┬──────────┘
     │                      │                      │
     ▼                      ▼                      ▼
┌─────────────┐   ┌──────────────┐   ┌──────────────────┐
│ 员工服务     │   │  管理服务    │   │  AI 客服服务      │
│ OA-2        │   │  OA-7        │   │  (Spring Boot 3.x)│
│ Spring      │   │  Spring      │   │  Spring AI        │
│ Boot 2.7    │   │  Boot 2.7    │   │  Ollama 本地模型  │
│ port 8081   │   │  port 8082   │   │  port 8083        │
└──────┬──────┘   └──────┬───────┘   └──────────────────┘
       │                 │
       └────────┬────────┘
                ▼
       ┌────────────────┐
       │   MySQL 数据库  │
       │   day 库        │
       │   Nacos 配置中心 │
       │   Redis 缓存    │
       │   ES 搜索引擎   │
       └────────────────┘
```

### 模块职责

| 模块 | 路径 | 端口 | 职责 |
|------|------|------|------|
| **前端** | `frontend/` | 5173 | Vue 3 管理后台 + 员工端 |
| **网关** | `OA-1/gateway/` | 8888 | 路由转发、跨域、服务发现 |
| **员工服务** | `OA-2/` | 8081 | 员工签到、请假申请、补签申请 |
| **管理服务** | `OA-7/` | 8082 | 员工管理、审批、考勤统计、节假日、RBAC |
| **AI 服务** | `oa-ai-service/` | 8083 | AI 智能客服（Spring AI + Ollama） |

---

## 二、技术栈

### 2.1 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 (LTS) | 运行环境 |
| Spring Boot | 2.7.18 | 微服务框架（管理服务/员工服务） |
| Spring Boot | 3.4.5 | AI 服务（兼容 Spring AI 1.0） |
| Spring Cloud | 2021.0.3 | 微服务治理 |
| Spring Cloud Alibaba | 2021.0.1.0 | Nacos 服务发现/配置 |
| Spring Cloud Gateway | - | API 网关 |
| Spring AI | 1.0.0 | AI 客服集成 |
| MyBatis + PageHelper | 2.3.1 / 1.4.6 | ORM + 分页 |
| MySQL | 8.0 | 关系数据库 |
| Druid | 1.2.20 | 数据库连接池 |
| Redis + Jedis | 4.4.3 | 缓存（JedisPool 手管连接） |
| Elasticsearch | - | 知识库全文搜索 |
| Lombok | 1.18.30 | 代码生成 |
| FastJSON | 1.2.83 | JSON 处理 |
| Nacos | 2.x | 服务注册/发现 + 配置中心 |

### 2.2 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.13 | UI 框架 |
| Vite | 6.2.4 | 构建工具 |
| TypeScript | 5.8 | 类型系统 |
| Element Plus | 2.10.2 | 组件库 |
| Vue Router | 4.5.0 | 路由 |
| Pinia | 3.0.1 | 状态管理 |
| Axios | 1.10.0 | HTTP 客户端 |
| ECharts | 5.6.0 | 图表 |
| ESLint + Prettier | - | 代码规范 |

---

## 三、关键技术策略

### 3.1 乐观锁解决并发冲突

**问题场景**：多个管理员同时审批同一张请假/补签/补卡单，后审批的会覆盖先审批的结果。

**解决方案**：数据库中每个需要并发控制的表都有一个 `version` 字段，更新时校验版本号。

**涉及的实体**：

| 表 | 版本字段 | 相关操作 |
|----|---------|---------|
| `day.leave` | `version` | 审批、拒绝、撤销请假 |
| `day.retroactive_sign` | `version` | 审批、拒绝、撤销补签 |
| `day.makeup_request` | `version` | 审批、拒绝、撤销补卡 |

**实现示例**（LeaveServiceImpl）：
```java
// 带乐观锁的状态更新
@Update("UPDATE day.leave SET status=#{status}, version=version+1 
         WHERE id=#{id} AND version=#{version}")
int updateStatusWithVersion(@Param("id") String id, 
                           @Param("status") String status, 
                           @Param("version") int version);
```

**工作流程**：
```
1. 管理员A读取请假单 → version=3
2. 管理员B读取同一请假单 → version=3
3. 管理员A批准 → UPDATE ... WHERE id=X AND version=3 → 成功, version→4
4. 管理员B拒绝 → UPDATE ... WHERE id=X AND version=3 → 失败, 返回0行
5. 提示"该申请已被他人处理，请刷新后重试"
```

### 3.2 延迟双删保证缓存一致性

为解决缓存与数据库的数据一致性问题，采用 **延迟双删（Delayed Double Delete）** 策略。该策略的核心思想是：更新数据库后，**先立即删除一次缓存**，**再延迟一段时间后删除一次**，确保并发场景下读请求不会把旧数据写回缓存。

#### 工作流程

```
请求A：更新数据                请求B：读取数据
  │                              │
  ├─ 1. 更新数据库               │
  ├─ 2. 删除缓存（第一次）        │
  │                              ├─ 3. 缓存未命中 → 查数据库
  │                              ├─ 4. 拿到旧数据（请求A还没完成）
  │                              ├─ 5. 将旧数据写入缓存 ← 问题！
  │                              │
  ├─ 6. 延迟 500ms               │
  ├─ 7. 删除缓存（第二次）→ 清除旧数据
  │
  后续读取 → 缓存未命中 → 查数据库 → 写入新数据 ✅
```

#### 读取流程

```
1. 查 Redis 缓存
2. 命中 → 直接返回
3. 未命中 → 查 MySQL → 写入 Redis（带过期时间）→ 返回
```

#### 写入流程

```
1. 更新 MySQL 数据库
2. 删除 Redis 缓存（第一次删除）
3. 延迟 500ms
4. 删除 Redis 缓存（第二次删除，兜底清除并发读入的旧数据）
```

#### 伪代码实现

```java
public class CacheService {
    
    @Autowired
    private JediPoolUtil jedisPoolUtil;
    
    // 延迟双删调度器
    private static final ScheduledExecutorService delayDeleter = 
        Executors.newSingleThreadScheduledExecutor();
    
    // 默认延迟 500ms
    private static final long DELETE_DELAY_MS = 500;
    
    /** 读取：先读缓存，未命中再查库 */
    public Attendance queryAttendance(int empId, LocalDate date) {
        String key = "attendance:" + empId + ":" + date;
        Jedis jedis = jedisPoolUtil.getJedis();
        try {
            // 1. 查缓存
            String cached = jedis.get(key);
            if (cached != null) {
                return JSON.parseObject(cached, Attendance.class);
            }
            // 2. 缓存未命中 → 查数据库
            Attendance data = attendanceDao.selectByEmpAndDate(empId, date);
            if (data != null) {
                // 3. 写入缓存（带过期时间，被动兜底）
                jedis.setex(key, 3600, JSON.toJSONString(data));
            }
            return data;
        } finally {
            jedis.close(); // 归还连接
        }
    }
    
    /** 写入：先更新库，再延迟双删缓存 */
    public void updateAttendance(Attendance data) {
        String key = "attendance:" + data.getEmpId() + ":" + data.getDate();
        Jedis jedis = jedisPoolUtil.getJedis();
        try {
            // 1. 先更新数据库
            attendanceDao.updateCheckTime(data);
            
            // 2. 第一次删除缓存  
            jedis.del(key);
            
            // 3. 延迟第二次删除（防并发脏读）
            delayDeleter.schedule(() -> {
                try (Jedis j = jedisPoolUtil.getJedis()) {
                    j.del(key);
                }
            }, DELETE_DELAY_MS, TimeUnit.MILLISECONDS);
            
        } finally {
            jedis.close();
        }
    }
}
```

#### 为什么延迟 500ms？

延迟时间 = **预期并发读请求完成查库 + 写缓存的最长时间**。取 500ms 是基于以下考虑：

| 因素 | 说明 |
|------|------|
| 并发读请求的查库时间 | 通常 < 50ms |
| 网络延迟 | 内网 < 10ms |
| 业务方可能的重试间隔 | 快速重试一般 > 100ms |
| **取 500ms** | 覆盖绝大多数场景，又不至于太长 |

> 如果业务对一致性要求极高，可将延迟时间调整为可配置参数，或使用 Redis 分布式锁（`SETNX`）在更新期间阻塞读请求。

#### JedisPool 连接池

通过 `JediPoolUtil` 工具类管理 Redis 连接，采用 **双重校验锁单例模式** 保证线程安全：

| 配置 | 值 | 说明 |
|------|----|------|
| maxTotal | 1000 | 最大连接数 |
| maxIdle | 30 | 最大空闲连接数 |
| maxWait | 60s | 获取连接最大等待时间 |
| testOnBorrow | true | 借用时检测连接有效性 |
```

> **说明**：目前项目中的 Redis 主要用于 JedisPoolUtil 提供连接池管理，实际缓存一致性策略可以在此基础上实现完整的延迟双删。

### 3.3 Session 会话管理

使用 **HttpSession** 进行登录状态管理：

```java
// 登录 → 存入 Session
session.setAttribute("admin", admin);

// 拦截器 → 校验 Session
Admin admin = (Admin) session.getAttribute("admin");
if (admin == null) { /* 401 */ }

// 退出 → 销毁 Session
session.invalidate();
```

**配置**：
- Session 超时：1800 秒（`application.yml`）
- 拦截器排除路径：`/auth/login`、`/auth/register`、`/auth/logout`、`/static/**`

### 3.4 RBAC 权限控制

基于员工的组织架构职务（`duty_id` + `dept_id`）动态计算角色：

```java
public static AdminRole computeRole(int deptId, int dutyId) {
    if (dutyId == 17) return AdminRole.CHAIRMAN;          // 董事长
    if (deptId == 1 && dutyId == 1) return AdminRole.HR_DIRECTOR;  // 人事部部长
    if (dutyId == 1 || dutyId == 2) return AdminRole.DEPT_HEAD;   // 部门部长/副部长
    return null; // 普通员工 → 无管理端权限
}
```

**三层权限校验**：
1. **拦截器层**（URL 级别）：`RbacInterceptor` 拦截请求 URL，校验角色是否有权访问
2. **服务层**（数据隔离）：`EmpServiceImpl`/`LeaveServiceImpl` 按 `deptId` 过滤数据
3. **前端层**（菜单/按钮）：根据 `role` 控制菜单显隐和按钮禁用

### 3.5 WebSocket 实时通知

使用 **STOMP over WebSocket** 实现实时推送：

```
员工提交请假/补签/补卡
  → OA-2 调用 adminDao.selectNotifyTargetIds(applicantNumber)
    → 查询符合条件的 admin：董事长 + 人事部部长 + 本部门部长/副部长
  → notificationService.sendNotification(adminId, ...)
    → 写入 day.notification 表
    → messagingTemplate.convertAndSend("/queue/notifications/{adminId}", notification)
      → 前端 STOMP 客户端接收 → 铃铛角标更新
```

**通知类型**：
| type | 说明 | 触发时机 |
|------|------|---------|
| `leave_submitted` | 新请假申请 | 员工提交请假 |
| `leave_approved/rejected/revoked` | 请假审批结果 | 管理员处理 |
| `retroactive_submitted` | 新补签申请 | 员工提交补签 |
| `makeup_submitted` | 新补卡申请 | 员工提交补卡 |

### 3.6 考勤自动结算

通过 `AutoCreateAttendanceService` 和 `AttendanceSettlementService` 实现：

```
每天凌晨（定时任务）
  → AutoCreateAttendanceService
    → 为所有员工创建当天考勤记录（today_status = NOT_CHECKED_IN）
  
每5分钟（定时任务）
  → AttendanceSettlementService
    → 检查当天已签退的员工
    → 计算缺时时长（missing_duration）
    → 结算考勤状态（attendance_status = NORMAL/LATE/...）

审批通过/拒绝时
  → RecalculateAttendanceService.recalculate(empId, date)
    → 重算指定员工指定日期的考勤状态
```

### 3.7 考勤规则读取策略

```
获取某部门生效规则：
  1. 查询 day.attendance_rule WHERE dept_id = #{deptId}
  2. 如果该部门没有专用规则 → 回退到全局默认规则 (dept_id IS NULL)
  3. 如果全局规则也不存在 → 使用硬编码默认值 (09:00-18:00, 30分钟宽限)
```

### 3.8 节假日与考勤联动

```
改节假日类型 → HolidayServiceImpl.update()
  → 更新 holiday 表
  → 如果是已过去的日期 → 触发 recalculateAllAttendance(date)
    → 对所有员工重算该日考勤
```

---

## 四、数据库设计

### 4.1 核心表关系

```
admin ──→ emp ──→ department
  │          │        │
  │          │        └── duty (职务字典)
  │          │
  │          ├── attendance (每日考勤)
  │          ├── leave (请假单)
  │          ├── retroactive_sign (补签申请)
  │          ├── makeup_request (补卡申请)
  │          ├── sign (旧签到系统)
  │          └── notification (通知)
  │
  └── notification (管理员通知)
```

### 4.2 关键表结构

#### admin（管理员）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | int PK | 管理员ID |
| name | varchar(10) | 登录名 |
| pwd | varchar(50) | MD5 密码 |
| emp_number | int (nullable) | 关联员工编号 (RBAC) |

#### emp（员工）
| 字段 | 类型 | 说明 |
|------|------|------|
| number | int PK | 员工编号 |
| name | varchar(10) | 姓名 |
| pwd | varchar(50) | 登录密码 |
| dept_id | int FK | 所属部门 |
| duty_id | int FK | 职务（1=部长,2=副部长,9=员工,17=董事长） |

#### attendance（考勤）
| 字段 | 类型 | 说明 |
|------|------|------|
| emp_id | int | 员工编号 |
| date | date | 日期 |
| check_in_time | datetime | 签到时间 |
| check_out_time | datetime | 签退时间 |
| today_status | enum | NOT_CHECKED_IN/CHECKED_IN/CHECKED_OUT/LEAVE |
| attendance_status | enum | NORMAL/LATE/EARLY_LEAVE/ABSENT |
| missing_duration | int | 缺时时长(分钟) |

#### leave（请假）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(36) | UUID 主键 |
| number | int | 员工编号 |
| status | varchar(20) | 待审批/已批准/已拒绝 |
| version | int | 乐观锁版本号 |
| dept_name | varchar(50) | 申请人部门名 |

### 4.3 乐观锁表

| 表 | 主键类型 | 版本字段 |
|----|---------|---------|
| leave | varchar(36) UUID | version |
| retroactive_sign | int AUTO | version |
| makeup_request | int AUTO | version |

---

## 五、RBAC 权限矩阵

### 5.1 角色判定

| 角色 | 条件 | 可登录管理端 |
|------|------|------------|
| CHAIRMAN | duty_id=17 | ✅ |
| HR_DIRECTOR | dept_id=1 AND duty_id=1 | ✅ |
| DEPT_HEAD | duty_id=1 OR 2 | ✅ |
| 普通员工 | 其他 | ❌ |

### 5.2 权限矩阵

| 功能模块 | CHAIRMAN | HR_DIRECTOR | DEPT_HEAD |
|---------|----------|-------------|-----------|
| 员工管理（查看） | 全部 | 全部 | 仅本部门 |
| 员工管理（增删改） | ✅ | 限普通员工 | 限本部门+普通员工 |
| 请假审批（查看） | 全部 | 全部 | 仅本部门 |
| 请假审批（操作） | ✅ | ✅ | 仅本部门 |
| 补签/补卡审批 | ✅ | ✅ | 仅本部门 |
| 考勤统计 | 全部 | 全部 | 仅本部门 |
| 节假日管理（查看） | ✅ | ✅ | ✅ |
| 节假日管理（修改） | ✅ | ✅ | ❌ |
| 部门/职务管理 | ✅ | ✅ | ❌ |
| 考勤规则 | ✅ | ✅ | ❌ |

---

## 六、部署与启动

### 6.1 依赖服务

| 服务 | 端口 | 用途 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Nacos | 8848 | 注册中心/配置中心 |
| Elasticsearch | 9201 | 知识库搜索 |
| Ollama | 11434 | AI 大模型（可选） |

### 6.2 启动顺序

```
1. MySQL + Redis + Nacos + ES（基础设施）
2. Gateway (端口 8888)
3. Employee Service OA-2 (端口 8081)
4. Admin Service OA-7 (端口 8082)
5. AI Service (端口 8083, 可选)
6. Frontend (端口 5173)
```

### 6.3 前端代理

```typescript
// vite.config.ts
server: {
    proxy: {
        '/api': {
            target: 'http://localhost:8888',  // → 网关
            changeOrigin: true,
        },
    },
}
```

---

## 七、技术债务与改进建议

| 项目 | 现状 | 建议 |
|------|------|------|
| 认证方式 | Session | 可迁移到 JWT Token 实现无状态认证 |
| 缓存一致性 | 基本读写 | 可补充延迟双删 / Redis 分布式锁 |
| 接口文档 | 无 | 可引入 Swagger/Knife4j |
| 单元测试 | 无 | 关键业务加测试 |
| 日志链路 | 无 TraceId | 可引入 Sleuth + Zipkin |
| 接口版本 | 无版本号 | 接口路径可加 /v1/ 前缀规范 |
