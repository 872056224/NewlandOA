# OA 系统管理端 RBAC 权限管理 — 完整实施方案

> 基于 `OA 系统管理端权限管理 (RBAC) 设计方案.md` + 项目代码分析

---

## 一、通知链路分析（当前现状）

### 1.1 员工提交请假/补签时的通知流程（OA-2 员工服务）

```
员工提交请假申请
  → OA-2 LeaveServiceImpl.apply()
    → notifyAdmins("leave_submitted", ...)
      → adminDao.selectAllIds()         ← 获取所有管理员ID
      → for each adminId:
          notificationService.sendNotification(adminId, type, title, content, bizId)
            → notificationDao.insert()   ← 写入 day.notification 表
            → messagingTemplate.convertAndSend(  ← WebSocket STOMP 实时推送
                "/queue/notifications/{adminId}", notification)
```

**当前问题**：通知发给 **所有管理员**（`selectAllIds()`），没有按角色/部门过滤。

### 1.2 管理员审批时的通知流程（OA-7 管理服务）

```
管理员批准请假
  → LeaveServiceImpl.approve()
    → notificationDao.insert("leave_approved", ...)  ← 通知员工本人（target = leave.number）
    → notificationDao.markAllReadByBizId()            ← 清除其他管理员的待审批红点
```

管理员审批时只通知员工本人，不涉及管理员群体通知。

---

## 二、数据库改造

### 2.1 `admin` 表增加字段

```sql
ALTER TABLE `admin`
  ADD COLUMN `emp_number` INT(11) DEFAULT NULL COMMENT '关联员工编号',
  ADD INDEX `idx_emp_number` (`emp_number`);
```

### 2.2 `duty` 表补充数据

```sql
INSERT INTO `duty` VALUES (2, '副部长');
INSERT INTO `duty` VALUES (17, '董事长');
```

### 2.3 现有的 admins 绑定 emp

需要手动为每个 admin 账号绑定对应的 emp_number（迁移脚本或手动执行）：

| admin_id | name | 建议绑定 emp_number | 说明 |
|----------|------|--------------------|------|
| 10001 | chenle | 121 (陈乐) | Java研发部，Java软件架构师 |
| 10002 | zhanghong | 154 (张虹) | 人事部，组长 |
| ... | ... | ... | (其余需要你确定绑定关系) |

> **设计原则**：老 admin 必须绑定 emp 后才能登录。绑定脚本手动执行。

---

## 三、后端核心改造（OA-7）

### 3.1 新增枚举 `AdminRole.java`

```java
package com.oa7.constant;

public enum AdminRole {
    /** 董事长 - 最高全局权限 */
    CHAIRMAN,
    /** 人事部部长 - 全局业务权限 + 受限的人事权 */
    HR_DIRECTOR,
    /** 部门部长/副部长 - 本部门数据隔离权限 */
    DEPT_HEAD
}
```

### 3.2 修改 `Admin.java` 实体

```java
@Data
public class Admin {
    private int id;
    private String name;
    private String pwd;
    private Integer empNumber;      // 新增：关联 emp.number

    // 非持久化字段（登录时联表查询填充）
    private String empName;         // 员工姓名
    private Integer deptId;         // 员工所在部门
    private Integer dutyId;         // 员工职务
    private String deptName;        // 部门名称
    private AdminRole role;         // 计算后的角色
}
```

### 3.3 新增 `AdminAuthService.java` (鉴权服务)

核心职责：
1. 登录时根据 admin 的 `emp_number` 联查 emp → 计算角色 → 存入 Session
2. 提供获取当前登录管理员信息的接口
3. 提供角色校验工具方法

```java
public interface AdminAuthService {
    AdminRole getCurrentRole(HttpSession session);
    Admin getCurrentAdmin(HttpSession session);
    boolean hasRole(HttpSession session, AdminRole... roles);
    Integer getCurrentDeptId(HttpSession session);
}
```

### 3.4 修改 `AdmServiceImpl.java` 登录逻辑

```java
@Override
public String login(Admin admin, HttpSession session) {
    // 1. 验证账号密码
    Admin admin1 = admDao.selectByName(admin);
    if (admin1 == null || !pwdMatches(admin.getPwd(), admin1.getPwd())) {
        return "false";
    }

    // 2. 检查是否绑定了员工
    if (admin1.getEmpNumber() == null) {
        return "no_emp_binding";  // 前端提示"该管理员账号未绑定员工，请联系管理员"
    }

    // 3. 联查 emp 信息
    Emp emp = empDao.selectByEmpNumber(admin1.getEmpNumber());
    if (emp == null) {
        return "emp_not_found";   // 前端提示"关联的员工不存在"
    }

    // 4. 计算角色 - 只有部长/副部长/董事长才能登录管理端
    AdminRole role = computeRole(emp.getDept_id(), emp.getDuty_id());
    if (role == null) {
        // 说明是普通员工，不允许登录管理端
        return "no_permission";
    }

    // 5. 填充 Admin 对象
    admin1.setEmpNumber(emp.getNumber());
    admin1.setEmpName(emp.getName());
    admin1.setDeptId(emp.getDept_id());
    admin1.setDutyId(emp.getDuty_id());
    admin1.setDeptName(emp.getDept_name());
    admin1.setRole(role);

    session.setAttribute("admin", admin1);
    return "true";
}
```

### 3.5 角色计算逻辑

```java
public static AdminRole computeRole(int deptId, int dutyId) {
    if (dutyId == 17) {
        return AdminRole.CHAIRMAN;
    }
    if (deptId == 1 && dutyId == 1) {
        return AdminRole.HR_DIRECTOR;
    }
    if (dutyId == 1 || dutyId == 2) {
        return AdminRole.DEPT_HEAD;
    }
    return null; // 普通员工 → 不能登录管理端
}
```

> **注意**：副部长的 dept 范围是所属部门，人事部部长是 dept_id=1 + duty_id=1 的特殊情况。董事长不限制部门。

### 3.6 新增 `RbacInterceptor.java` (权限拦截器)

替换/扩展原有的 `LoginInterceptor`，增加角色检查：

```java
public class RbacInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession();
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            // 未登录 → 401
            writeUnauthorizedResponse(response);
            return false;
        }

        // 根据请求路径检查角色权限
        String path = request.getRequestURI();
        AdminRole role = admin.getRole();

        // 考勤规则相关接口 → 仅 CHAIRMAN 和 HR_DIRECTOR 可访问
        if (path.contains("/attendance-rules") || path.contains("/holidays")) {
            if (role == AdminRole.DEPT_HEAD) {
                writeForbiddenResponse(response, "无权访问考勤规则管理");
                return false;
            }
        }

        // 部门管理 → DEPT_HEAD 只能查看（需要进一步判断，但可以在 Service 层处理）
        // 员工管理 → DEPT_HEAD 只能操作本部门（在 Service 层做数据过滤）

        return true;
    }
}
```

### 3.7 业务层数据权限改造

#### 员工管理 (`EmpServiceImpl.java`)

```java
// DEPT_HEAD 只能看到本部门员工
@Override
public RESP selectByPage(int currentPage, int pageSize, HttpSession session) {
    Admin admin = (Admin) session.getAttribute("admin");
    PageHelper.startPage(currentPage, pageSize);

    List<Emp> list;
    if (admin.getRole() == AdminRole.DEPT_HEAD) {
        list = empDao.selectByPageAndDept(admin.getDeptId()); // 新增：按部门过滤
    } else {
        list = empDao.selectByPageHelper(); // 全部
    }

    PageInfo<Emp> pageInfo = new PageInfo<>(list);
    return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
}

// DEPT_HEAD 不能修改其他部门员工，HR_DIRECTOR 不能修改董事长/部长
@Override
public RESP update(int number, Emp emp, int currentPage, int pageSize, HttpSession session) {
    Admin admin = (Admin) session.getAttribute("admin");
    Emp targetEmp = empDao.selectByEmpNumber(number);

    // 校验权限
    String checkResult = checkEmpModifyPermission(admin, targetEmp);
    if (checkResult != null) {
        return RESP.error(403, checkResult);
    }

    // ... 原更新逻辑
}
```

#### 请假审批 (`LeaveServiceImpl.java`)

```java
// DEPT_HEAD 只能看到本部门的请假申请
@Override
public RESP getPending(int currentPage, int pageSize, HttpSession session) {
    Admin admin = (Admin) session.getAttribute("admin");
    PageHelper.startPage(currentPage, pageSize);

    List<Leave> list;
    if (admin.getRole() == AdminRole.DEPT_HEAD) {
        list = leaveDao.selectPendingByDept(admin.getDeptName()); // 按部门名过滤
    } else {
        list = leaveDao.selectPending(); // 全部
    }

    PageInfo<Leave> pageInfo = new PageInfo<>(list);
    return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
}
```

> 注意：Leave 表的 dept_name 存的是部门名（如"Java研发部"），所以需要通过 dept_name 过滤。需要保证 Admin 的 deptName 与 Leave 的 dept_name 匹配。

#### 补签审批 (`RetroactiveSignServiceImpl.java`)

修改 `getPending()`：DEPT_HEAD 只看到本部门员工的补签申请。需要在 `retroactive_sign` 表联查 `emp` 表获取 dept_id，或者添加 dept_name 字段。

> **方案**：在 `RetroactiveSign` 查询时 JOIN `emp` 表，按 dept_id 过滤。

#### 补卡审批 (`MakeupRequestServiceImpl.java`)

同上，补卡申请需要按部门过滤。

#### 考勤统计/签到查询 (`SignServiceImpl.java`)

DEPT_HEAD 只能看到本部门的统计。需要在 attendance 查询时 JOIN emp 表过滤。

### 3.8 新增 API 端点（供 OA-2 调用）

为了让员工服务（OA-2）在提交请假/补签后只通知相关管理员，需要新增：

```
GET /api/v1/admin/rbac/notification-targets
```

**请求参数**：`?empNumber=123`（提交申请的员工的编号）

**返回**：应接收通知的管理员列表

```json
{
  "code": 200,
  "data": [
    { "adminId": 10001, "targetNumber": 10001, "deptName": "Java研发部" },
    { "adminId": 10002, "targetNumber": 10002, "deptName": "人事部" }
  ]
}
```

**逻辑**：
1. 查询提交申请员工所在的部门 `deptId`
2. 查询所有 admin → 联表 emp → 计算角色
3. 筛选出需要接收通知的管理员：
   - CHAIRMAN（所有申请）
   - HR_DIRECTOR（所有申请）
   - DEPT_HEAD（仅当 dept_id 匹配申请人的部门时）

---

## 四、前端改造

### 4.1 登录响应扩展

登录成功时，后端返回的信息改为：

```json
{
  "code": 200,
  "data": {
    "id": 10001,
    "name": "chenle",
    "empName": "陈乐",
    "deptName": "Java研发部",
    "role": "DEPT_HEAD"
  }
}
```

### 4.2 菜单动态控制 (`AdminHome.vue`)

根据 `role` 控制侧边栏的显示隐藏：

| 菜单项 | CHAIRMAN | HR_DIRECTOR | DEPT_HEAD |
|--------|----------|-------------|-----------|
| 数据面板 | ✅ | ✅ | ✅ |
| 员工管理 | ✅ | ✅ | ✅ (仅本部门) |
| 部门管理 | ✅ | ✅ | ❌ |
| 职务管理 | ✅ | ✅ | ❌ |
| 考勤管理 | ✅ | ✅ | ✅ (仅本部门) |
| 考勤统计 | ✅ | ✅ | ✅ (仅本部门) |
| 请假审批 | ✅ | ✅ | ✅ (仅本部门) |
| 补签审批 | ✅ | ✅ | ✅ (仅本部门) |
| 知识库管理 | ✅ | ✅ | ✅ |
| 通知列表 | ✅ | ✅ | ✅ |
| 节假日管理 | ✅ | ✅ | ❌ |
| 考勤规则 | ✅ | ✅ | ❌ |

### 4.3 按钮级控制 (`EmpList.vue`)

员工列表中：
- HR_DIRECTOR：无法编辑/删除 `duty_id = 17 (董事长)` 和 `duty_id = 1 (部长)` 的员工
- DEPT_HEAD：只能编辑/删除本部门员工，无法操作董事长和其他部门员工
- CHAIRMAN：所有可操作

### 4.4 部门/职务管理控制

部门管理、职务管理页面对 DEPT_HEAD 隐藏入口和路由。

---

## 五、通知范围改造（OA-2 员工服务）

### 5.1 修改 OA-2 的 `notifyAdmins()` 方法

将原来调用 `adminDao.selectAllIds()` 改为调用 OA-7 的 RBAC API：

```java
// 修改前
private void notifyAdmins(String type, String title, String content, String bizId) {
    List<Integer> adminIds = adminDao.selectAllIds();
    for (int adminId : adminIds) {
        notificationService.sendNotification(adminId, type, title, content, bizId);
    }
}

// 修改后
private void notifyAdmins(int submitterEmpNumber, String type, String title, String content, String bizId) {
    // 调用 OA-7 的 RBAC API 获取目标管理员
    List<RbacTarget> targets = rbacClient.getNotificationTargets(submitterEmpNumber);
    for (RbacTarget target : targets) {
        notificationService.sendNotification(
            target.getAdminId(), type, title, content, bizId);
    }
}
```

### 5.2 新增 HTTP 客户端

在 OA-2 中添加 Feign 或 RestTemplate 客户端调用 OA-7 的 API。

---

## 六、完整接口文档

### 6.1 认证接口

#### POST `/auth/login` — 管理员登录

**请求**：
```json
{ "name": "chenle", "pwd": "123123" }
```

**响应**：
```
// 成功
"true"

// 失败情况
"false"           // 账号或密码错误
"no_emp_binding"  // 未绑定员工
"emp_not_found"   // 关联员工不存在
"no_permission"   // 无管理端权限（普通员工）
```

#### GET `/auth/profile` — 获取当前登录管理员信息

**响应**：
```json
{
  "code": 200,
  "data": {
    "id": 10001,
    "name": "chenle",
    "empNumber": 121,
    "empName": "陈乐",
    "deptId": 3,
    "dutyId": 10,
    "deptName": "Java研发部",
    "role": "DEPT_HEAD"
  }
}
```

#### POST `/auth/logout` — 退出登录

### 6.2 RBAC 辅助接口（供 OA-2 调用）

#### GET `/rbac/notification-targets` — 获取通知目标管理员

**请求参数**：`?empNumber=123`

**响应**：
```json
{
  "code": 200,
  "data": [
    { "adminId": 10001, "adminName": "chenle", "targetNumber": 10001, "deptName": "Java研发部" },
    { "adminId": 10007, "adminName": "zhangsan", "targetNumber": 10007, "deptName": "人事部" }
  ]
}
```

### 6.3 员工管理接口（增加 Session 参数）

#### GET `/employees` — 员工列表（数据隔离）

**请求参数**：`currentPage=1&pageSize=10`

**响应**：原有格式，但 DEPT_HEAD 只看到本部门员工

**后端修改**：`EmpController.list()` 增加 `HttpSession` 参数

#### POST `/employees` — 新增员工（角色校验）

- CHAIRMAN：允许
- HR_DIRECTOR：允许（但不能新建董事长/部长）
- DEPT_HEAD：只能在本部门新增普通员工

#### PUT `/employees/{number}` — 修改员工（角色校验）

- CHAIRMAN：允许所有
- HR_DIRECTOR：不能修改董事长(duty_id=17)和部长(duty_id=1)
- DEPT_HEAD：只能修改本部门员工，且不能修改职务为自己或更高

#### DELETE `/employees/{number}` — 删除员工（角色校验同上）

### 6.4 请假审批接口（数据隔离）

#### GET `/leave/pending` — 待审批列表

- CHAIRMAN/HR_DIRECTOR：全部
- DEPT_HEAD：仅本部门

#### GET `/leave/list` — 历史列表，同上

#### PUT `/leave/{id}/approve` — 批准

**增加校验**：DEPT_HEAD 只能批准本部门的申请

#### PUT `/leave/{id}/reject` — 拒绝，同上

### 6.5 补签审批接口（数据隔离）

#### GET `/attendance/retroactive/pending`

- DEPT_HEAD：仅本部门

#### PUT `/attendance/retroactive/{id}/approve`

**增加校验**：DEPT_HEAD 只能操作本部门

### 6.6 补卡审批接口

#### GET `/makeup/pending`

**修改**：DEPT_HEAD 数据隔离

### 6.7 考勤规则接口（角色限制）

#### GET/POST/PUT/DELETE `/attendance-rules`

- 仅 CHAIRMAN 和 HR_DIRECTOR 可访问
- DEPT_HEAD 返回 403

### 6.8 节假日接口（角色限制）

#### GET/POST/PUT `/holidays/**`

- 仅 CHAIRMAN 和 HR_DIRECTOR 可访问

### 6.9 部门管理接口

#### GET/POST/PUT `/departments/**`

- CHAIRMAN 和 HR_DIRECTOR 可管理
- DEPT_HEAD 不可访问

### 6.10 职务管理接口

#### GET/POST/PUT `/duties/**`

- CHAIRMAN 和 HR_DIRECTOR 可管理
- DEPT_HEAD 只读（list），不可增删改

---

## 七、Dao 层新增 SQL

### EmpDao 新增

```java
// 按部门分页查询员工
@Select("SELECT emp.*, dept_name, duty_name FROM day.emp " +
        "LEFT JOIN department ON department.dept_id = emp.dept_id " +
        "LEFT JOIN duty ON emp.duty_id = duty.duty_id " +
        "WHERE emp.dept_id = #{deptId} ORDER BY number")
List<Emp> selectByPageAndDept(@Param("deptId") int deptId);
```

### LeaveDao 新增

```java
// 按部门名查询待审批
@Select("SELECT * FROM day.leave WHERE status='待审批' AND dept_name=#{deptName} ORDER BY start_date DESC")
List<Leave> selectPendingByDept(@Param("deptName") String deptName);

// 按部门名查询历史
@Select("SELECT * FROM day.leave WHERE dept_name=#{deptName} ORDER BY start_date DESC")
List<Leave> selectAllByDept(@Param("deptName") String deptName);
```

### RetroactiveSignDao 新增

```java
// 联查 emp 表获取 dept_id
@Select("SELECT rs.* FROM day.retroactive_sign rs " +
        "LEFT JOIN day.emp e ON e.number = rs.number " +
        "WHERE rs.status='待审批' AND e.dept_id = #{deptId} " +
        "ORDER BY rs.create_time DESC")
List<RetroactiveSign> selectPendingByDept(@Param("deptId") int deptId);
```

### MakeupRequestDao 新增

```java
// 联查 emp 表获取 dept_id
@Select("SELECT mr.* FROM day.makeup_request mr " +
        "LEFT JOIN day.emp e ON e.number = mr.emp_id " +
        "WHERE mr.status='PENDING' AND e.dept_id = #{deptId} " +
        "ORDER BY mr.created_at DESC")
List<MakeupRequest> selectPendingByDept(@Param("deptId") int deptId);
```

### AdmDao 新增

```java
// 联查 admin + emp 获取角色信息
@Select("SELECT a.*, e.name as empName, e.dept_id, e.duty_id, d.dept_name " +
        "FROM day.admin a " +
        "LEFT JOIN day.emp e ON e.number = a.emp_number " +
        "LEFT JOIN day.department d ON d.dept_id = e.dept_id " +
        "WHERE a.id = #{id}")
Admin selectAdminWithEmp(@Param("id") int id);

// 获取所有有管理权限的管理员（用于通知）
@Select("SELECT a.id, a.emp_number FROM day.admin a " +
        "INNER JOIN day.emp e ON e.number = a.emp_number " +
        "WHERE a.emp_number IS NOT NULL " +
        "AND e.duty_id IN (1, 2, 17)")
List<Map<String, Object>> selectAllEligibleAdmins();
```

---

## 八、测试文档

### 8.1 单元测试

#### 8.1.1 角色计算测试

```
@Test
public void testComputeRole_Chairman() {
    assertEquals(AdminRole.CHAIRMAN, AdminAuthUtil.computeRole(3, 17));
}

@Test
public void testComputeRole_HRDirector() {
    assertEquals(AdminRole.HR_DIRECTOR, AdminAuthUtil.computeRole(1, 1));
}

@Test
public void testComputeRole_DeptHead() {
    assertEquals(AdminRole.DEPT_HEAD, AdminAuthUtil.computeRole(3, 1));
    assertEquals(AdminRole.DEPT_HEAD, AdminAuthUtil.computeRole(2, 2));
}

@Test
public void testComputeRole_NormalEmp() {
    assertNull(AdminAuthUtil.computeRole(3, 5));
    assertNull(AdminAuthUtil.computeRole(3, 9));
}
```

#### 8.1.2 登录逻辑测试

| 场景 | 输入 | 预期结果 |
|------|------|---------|
| 账号密码正确 + 绑定董事长 | {name: "admin1", pwd: "xxx"} | "true", session.role = CHAIRMAN |
| 账号密码正确 + 绑定人事部部长 | {name: "admin2", pwd: "xxx"} | "true", session.role = HR_DIRECTOR |
| 账号密码正确 + 绑定部门部长 | {name: "admin3", pwd: "xxx"} | "true", session.role = DEPT_HEAD |
| 账号密码正确 + 绑定普通员工 | {name: "admin4", pwd: "xxx"} | "no_permission" |
| 账号密码正确 + 未绑定员工 | {name: "admin5", pwd: "xxx"} | "no_emp_binding" |
| 账号或密码错误 | 任意错误凭据 | "false" |

### 8.2 接口测试

#### 8.2.1 登录认证测试

```
用例 1：绑定董事长身份的管理员登录
  POST /api/v1/admin/auth/login
  Body: { "name": "ceo_admin", "pwd": "xxx" }
  → 预期: "true", Session 中 role = CHAIRMAN

用例 2：绑定普通员工的管理员登录
  POST /api/v1/admin/auth/login
  Body: { "name": "normal_admin", "pwd": "xxx" }
  → 预期: "no_permission"
```

#### 8.2.2 数据隔离测试

```
用例 3：DEPT_HEAD 查看员工列表
  GET /api/v1/admin/employees?currentPage=1&pageSize=10
  Headers: Session(role=DEPT_HEAD, deptId=3)
  → 预期: 只返回 dept_id=3 的员工

用例 4：HR_DIRECTOR 查看员工列表（无过滤）
  GET /api/v1/admin/employees?currentPage=1&pageSize=10
  Headers: Session(role=HR_DIRECTOR)
  → 预期: 返回所有员工

用例 5：DEPT_HEAD 查看请假待审批
  GET /api/v1/admin/leave/pending?currentPage=1&pageSize=10
  Headers: Session(role=DEPT_HEAD, deptName="Java研发部")
  → 预期: 只返回 dept_name="Java研发部" 的待审批

用例 6：CHAIRMAN 查看请假待审批（无过滤）
  GET /api/v1/admin/leave/pending?currentPage=1&pageSize=10
  Headers: Session(role=CHAIRMAN)
  → 预期: 返回所有待审批
```

#### 8.2.3 角色限制测试

```
用例 7：DEPT_HEAD 访问考勤规则管理 → 403
  GET /api/v1/admin/attendance-rules
  Headers: Session(role=DEPT_HEAD)
  → 预期: 403

用例 8：DEPT_HEAD 访问节假日管理 → 403
  GET /api/v1/admin/holidays/year/2026
  Headers: Session(role=DEPT_HEAD)
  → 预期: 403

用例 9：DEPT_HEAD 访问部门管理 → 403
  GET /api/v1/admin/departments?currentPage=1&pageSize=10
  Headers: Session(role=DEPT_HEAD)
  → 预期: 403
```

#### 8.2.4 人事权限限制测试

```
用例 10：HR_DIRECTOR 修改董事长 → 403
  PUT /api/v1/admin/employees/121
  Headers: Session(role=HR_DIRECTOR)
  Body: { name: "xxx" }
  (假设 emp 121 的 duty_id=17)
  → 预期: 403

用例 11：HR_DIRECTOR 修改部长 → 403
  PUT /api/v1/admin/employees/xxx
  Headers: Session(role=HR_DIRECTOR)
  (假设目标 emp 的 duty_id=1)
  → 预期: 403

用例 12：HR_DIRECTOR 修改普通员工 → 200
  PUT /api/v1/admin/employees/xxx
  Headers: Session(role=HR_DIRECTOR)
  (假设目标 emp 的 duty_id=5)
  → 预期: 200

用例 13：DEPT_HEAD 修改其他部门员工 → 403
  PUT /api/v1/admin/employees/xxx
  Headers: Session(role=DEPT_HEAD, deptId=3)
  (假设目标 emp 的 dept_id=5)
  → 预期: 403
```

#### 8.2.5 通知目标 API 测试

```
用例 14：查询请假通知目标
  GET /api/v1/admin/rbac/notification-targets?empNumber=123
  (假设 emp 123 在 Java研发部 dept_id=3)
  → 预期: 返回 [
      { adminId: 董事长admin },     // 董事长
      { adminId: 人事部部长admin }, // 人事部部长
      { adminId: Java研发部部长admin } // 本部门部长/副部长
    ]
```

### 8.3 集成测试场景

| 场景 | 步骤 | 预期 |
|------|------|------|
| **S1: 董事长全流程** | 1. 董事长登录<br>2. 查看所有员工<br>3. 查看所有考勤规则<br>4. 审批所有部门请假 | 所有操作成功，无数据隔离 |
| **S2: 人事部部长全流程** | 1. 人事部部长登录<br>2. 查看所有员工<br>3. 修改普通员工信息<br>4. 尝试修改董事长/部长信息<br>5. 管理考勤规则 | 1-3,5成功，4失败(403) |
| **S3: 部门部长全流程** | 1. Java研发部部长登录<br>2. 查看员工列表（只看到本部门）<br>3. 查看请假待审批（只看到本部门）<br>4. 查看考勤规则<br>5. 查看部门管理 | 2-3只看到本部门数据<br>4-5返回403 |
| **S4: 普通员工登录** | 1. 普通员工身份绑定 admin<br>2. 尝试登录管理端 | "no_permission"，跳转失败 |
| **S5: 通知范围验证** | 1. Java研发部员工提交请假<br>2. OA-2 调用通知目标 API<br>3. Java研发部部长收到通知<br>4. 销售部部长不收到通知 | 3有通知，4无通知 |

### 8.4 前端测试

| 场景 | 步骤 | 预期 |
|------|------|------|
| DEPT_HEAD 登录 | 登录后查看侧边栏 | 考勤规则、节假日管理、部门管理、职务管理 菜单隐藏 |
| 员工列表角色控制 | DEPT_HEAD 打开员工列表 | 只能看到本部门员工数据 |
| 按钮禁用 | HR_DIRECTOR 查看员工列表 | 董事长和部长的编辑/删除按钮禁用 |

---

## 九、数据填充方案

### 9.1 填充逻辑

每个部门配备 1 部长 + 1 副部长 + 3 普通员工，董事长单独设立，密码统一为 `123`。

**部门覆盖**：人事部(1)、后勤部(2)、Java研发部(3)、商品部(4)、销售部(5)、行政部(6)、大数据研发部(17)、前端研发部(18)、测试部(19) — 共 9 个部门

### 9.2 员工数据设计

#### 董事长（独立于部门之外）

| admin名 | 员工姓名 | duty_id | dept_id | 说明 |
|---------|---------|---------|---------|------|
| chenle | 陈乐 | 17 (董事长) | 3 (随便) | admin 10001 绑定 |

#### 人事部 (dept_id=1)

| 员工姓名 | duty_id | 职务 | admin绑定 |
|---------|---------|------|----------|
| 荀彧 | 1 | 部长 | 新建 admin (hr_xunyu) |
| 荀攸 | 2 | 副部长 | 新建 admin |
| 郭嘉 | 9 | Java软件工程师 | 无 |
| 程昱 | 9 | Java软件工程师 | 无 |
| 贾诩 | 9 | Java软件工程师 | 无 |

#### 后勤部 (dept_id=2)

| 员工姓名 | duty_id | 职务 | admin绑定 |
|---------|---------|------|----------|
| 张飞 | 1 | 部长 | 新建 admin |
| 赵云 | 2 | 副部长 | 新建 admin |
| 廖化 | 9 | 普通员工 | 无 |
| 周仓 | 9 | 普通员工 | 无 |
| 关平 | 9 | 普通员工 | 无 |

#### Java研发部 (dept_id=3)

| 员工姓名 | duty_id | 职务 | admin绑定 |
|---------|---------|------|----------|
| 周瑜 | 1 | 部长 | 新建 admin |
| 陆逊 | 2 | 副部长 | 新建 admin |
| 吕蒙 | 9 | 普通员工 | 无 |
| 甘宁 | 9 | 普通员工 | 无 |
| 黄盖 | 9 | 普通员工 | 无 |

#### 商品部 (dept_id=4)

| 员工姓名 | duty_id | 职务 | admin绑定 |
|---------|---------|------|----------|
| 关羽 | 1 | 部长 | 新建 admin |
| 张辽 | 2 | 副部长 | 新建 admin |
| 徐晃 | 9 | 普通员工 | 无 |
| 于禁 | 9 | 普通员工 | 无 |
| 乐进 | 9 | 普通员工 | 无 |

#### 销售部 (dept_id=5)

| 员工姓名 | duty_id | 职务 | admin绑定 |
|---------|---------|------|----------|
| 吕布 | 1 | 部长 | 新建 admin |
| 高顺 | 2 | 副部长 | 新建 admin |
| 张郃 | 9 | 普通员工 | 无 |
| 夏侯渊 | 9 | 普通员工 | 无 |
| 曹仁 | 9 | 普通员工 | 无 |

#### 行政部 (dept_id=6)

| 员工姓名 | duty_id | 职务 | admin绑定 |
|---------|---------|------|----------|
| 诸葛亮 | 1 | 部长 | 新建 admin |
| 庞统 | 2 | 副部长 | 新建 admin |
| 马谡 | 9 | 普通员工 | 无 |
| 姜维 | 9 | 普通员工 | 无 |
| 法正 | 9 | 普通员工 | 无 |

#### 大数据研发部 (dept_id=17)

| 员工姓名 | duty_id | 职务 | admin绑定 |
|---------|---------|------|----------|
| 司马懿 | 1 | 部长 | 新建 admin |
| 邓艾 | 2 | 副部长 | 新建 admin |
| 钟会 | 9 | 普通员工 | 无 |
| 陈泰 | 9 | 普通员工 | 无 |
| 郭淮 | 9 | 普通员工 | 无 |

#### 前端研发部 (dept_id=18)

| 员工姓名 | duty_id | 职务 | admin绑定 |
|---------|---------|------|----------|
| 孙策 | 1 | 部长 | 新建 admin |
| 孙权 | 2 | 副部长 | 新建 admin |
| 周泰 | 9 | 普通员工 | 无 |
| 蒋钦 | 9 | 普通员工 | 无 |
| 凌统 | 9 | 普通员工 | 无 |

#### 测试部 (dept_id=19)

| 员工姓名 | duty_id | 职务 | admin绑定 |
|---------|---------|------|----------|
| 典韦 | 1 | 部长 | 新建 admin |
| 许褚 | 2 | 副部长 | 新建 admin |
| 曹洪 | 9 | 普通员工 | 无 |
| 曹彰 | 9 | 普通员工 | 无 |
| 曹真 | 9 | 普通员工 | 无 |

### 9.3 管理员绑定汇总

| admin 账号 | 密码 | 绑定员工 | 角色 |
|-----------|------|---------|------|
| chenle | 123123 | 陈乐 (董事长) | CHAIRMAN |
| hr_xunyu | 123 | 荀彧 (人事部部长) | HR_DIRECTOR |
| hr_xunyou | 123 | 荀攸 (人事部副部长) | DEPT_HEAD |
| hq_zhangfei | 123 | 张飞 (后勤部部长) | DEPT_HEAD |
| hq_zhaoyun | 123 | 赵云 (后勤部副部长) | DEPT_HEAD |
| java_zhouyu | 123 | 周瑜 (Java研发部部长) | DEPT_HEAD |
| java_luxun | 123 | 陆逊 (Java研发部副部长) | DEPT_HEAD |
| goods_guanyu | 123 | 关羽 (商品部部长) | DEPT_HEAD |
| goods_zhangliao | 123 | 张辽 (商品部副部长) | DEPT_HEAD |
| sales_lvbu | 123 | 吕布 (销售部部长) | DEPT_HEAD |
| sales_gaoshun | 123 | 高顺 (销售部副部长) | DEPT_HEAD |
| admin_zhugeliang | 123 | 诸葛亮 (行政部部长) | DEPT_HEAD |
| admin_pangtong | 123 | 庞统 (行政部副部长) | DEPT_HEAD |
| bigdata_simayi | 123 | 司马懿 (大数据研发部部长) | DEPT_HEAD |
| bigdata_dengai | 123 | 邓艾 (大数据研发部副部长) | DEPT_HEAD |
| front_sunce | 123 | 孙策 (前端研发部部长) | DEPT_HEAD |
| front_sunquan | 123 | 孙权 (前端研发部副部长) | DEPT_HEAD |
| test_dianwei | 123 | 典韦 (测试部部长) | DEPT_HEAD |
| test_xuchu | 123 | 许褚 (测试部副部长) | DEPT_HEAD |

### 9.4 7月考勤数据生成

**范围**：2026-07-01 ~ 2026-07-23（到今天）

**规则**：
- 所有新员工（不含原有员工）生成考勤记录
- 工作日（周一至周五）：`check_in=09:00`, `check_out=18:00`, `today_status=CHECKED_OUT`, `attendance_status=NORMAL`
- 周末：不生成考勤记录（由系统自动处理）
- 签到地址：福建省福州市马尾区

**2026年7月工作日历**：
```
七月 2026
日 一 二 三 四 五 六
          1  2  3  4
5  6  7  8  9 10 11
12 13 14 15 16 17 18
19 20 21 22 23 24 25
```

→ 需生成考勤的天数：7月1日(三)~7月3日(五) + 7月6日(一)~7月10日(五) + 7月13日(一)~7月17日(五) + 7月20日(一)~7月23日(三) = **17个工作日**

**新员工数量**：9部门 × 5人/部门 + 1董事长 = 46人（减去保留的已有员工后实际新增约34-38人）

新员工约 38 人 × 17 天 ≈ **646 条考勤记录**

---

## 十、实施步骤

| Step | 内容 | 涉及文件 | 优先级 |
|------|------|---------|--------|
| 1 | 数据库变更：admin 加 emp_number，duty 加两条记录 | SQL 脚本 | P0 |
| 2 | 后端：新增 AdminRole 枚举 | `constant/AdminRole.java` | P0 |
| 3 | 后端：修改 Admin 实体 | `pojo/Admin.java` | P0 |
| 4 | 后端：修改 AdmDao（联查 emp） | `dao/AdmDao.java` | P0 |
| 5 | 后端：修改 AdmServiceImpl 登录逻辑 | `service/Impl/AdmServiceImpl.java` | P0 |
| 6 | 后端：新增 AdminAuthUtil 角色计算工具 | `util/AdminAuthUtil.java` | P0 |
| 7 | 后端：新增 RbacInterceptor 权限拦截器 | `interceptor/RbacInterceptor.java` | P0 |
| 8 | 后端：修改 InterceptorConfig | `config/InterceptorConfig.java` | P0 |
| 9 | 后端：EmpController + Service 加 Session 参数和数据过滤 | 多个文件 | P1 |
| 10 | 后端：LeaveController + Service 数据隔离 | 多个文件 | P1 |
| 11 | 后端：RetroactiveSignController + Service 数据隔离 | 多个文件 | P1 |
| 12 | 后端：MakeupRequestController + Service 数据隔离 | 多个文件 | P1 |
| 13 | 后端：AttendanceRule/Holiday/Dept/Duty 角色限制 | 多个文件 | P1 |
| 14 | 后端：新增通知目标 API | `controller/RbacController.java` | P1 |
| 15 | 前端：AdminHome.vue 菜单动态控制 | `AdminHome.vue` | P1 |
| 16 | 前端：EmpList.vue 按钮级控制 | `EmpList.vue` | P2 |
| 17 | 前端：登录响应适配 | `AdminLogin.vue`, `AdminHome.vue` | P0 |
| 18 | OA-2：修改 notifyAdmins 调用 RBAC API | `LeaveServiceImpl.java` 等 | P1 |
| 19 | 测试：编写单元测试 + 接口测试 | 测试文件 | P2 |
| 20 | 绑定：admin 关联 emp 数据迁移 | 迁移脚本 | P0 |

---

## 十、风险与注意事项

1. **leave.dept_name 字符串匹配**：Leave 表的 `dept_name` 是字符串（如"Java研发部"），Department 表的 `dept_name` 也是字符串，需要确保一致
2. **retroactive_sign 没有 dept_name**：需要 JOIN emp 表获取部门信息
3. **Session vs Token**：当前使用 Session 鉴权，后续可考虑迁移到 JWT Token
4. **OA-2 服务调用**：如果 OA-2 与服务注册中心（Nacos）集成，建议通过 Feign 调用；否则使用 RestTemplate + 硬编码 URL
5. **兼容性**：现有的 admin 在不绑定 emp_number 前会自动被拒绝登录，需要先完成绑定迁移
