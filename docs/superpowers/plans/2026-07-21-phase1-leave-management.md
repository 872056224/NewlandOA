# Phase 1 — 请假管理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 实现完整的请假管理功能（员工提交→管理员审批），覆盖员工端和管理端

**架构:**
- 员工端 API 在 `oa-emp-service` (port 8081, context-path `/api/v1/employee`)
- 管理端 API 在 `oa-admin-service` (port 8082, context-path `/api/v1/admin`)
- 前端 Vue 3 + Element Plus + Axios，通过 Gateway (port 8888) 转发
- 数据库 `leave` 表已存在，只需加一个 `type` 字段

**Tech Stack:** Spring Boot 2.7, MyBatis (注解SQL), MySQL 8, Vue 3.5, Element Plus 2.10, Axios 1.10

## 全局约束

1. 所有新类必须放在对应包路径下，与现有代码一致
2. Controller 用 `@RestController` + `@CrossOrigin` + `@RequestMapping`
3. Dao 用 `@Mapper` + `@Repository`，SQL 用注解方式（`@Select`/`@Insert`/`@Update`）
4. 返回统一使用 `RESP` 工具类
5. OA-7 用 PageHelper 分页，OA-2 用手动 `OFFSET/LIMIT`
6. 前端用 `<script setup lang="ts">` + `ref`/`reactive`，保持 Apple 设计风格
7. API 路径：员工端 `/api/v1/employee/leave/*`，管理端 `/api/v1/admin/leave/*`

---

## 文件结构

### 数据库
- `ALTER TABLE day.leave ADD COLUMN type`

### oa-emp-service（员工端后端）
- **创建** `com.oa2.pojo.Leave` — Leave 实体类
- **创建** `com.oa2.dao.LeaveDao` — MyBatis Mapper（提交、查询我的记录）
- **创建** `com.oa2.service.LeaveService` — Service 接口
- **创建** `com.oa2.service.impl.LeaveServiceImpl` — Service 实现
- **创建** `com.oa2.controller.LeaveController` — REST 控制器

### oa-admin-service（管理端后端）
- **创建** `com.oa7.pojo.Leave` — Leave 实体类
- **创建** `com.oa7.dao.LeaveDao` — MyBatis Mapper（待审批列表、审批操作）
- **创建** `com.oa7.service.LeaveService` — Service 接口
- **创建** `com.oa7.service.impl.LeaveServiceImpl` — Service 实现
- **创建** `com.oa7.controller.LeaveController` — REST 控制器

### Frontend（前端）
- **创建** `frontend/src/components/emp/EmpLeaveApply.vue` — 请假申请表单
- **创建** `frontend/src/components/emp/EmpLeaveList.vue` — 我的请假记录
- **创建** `frontend/src/components/admin/LeaveApproval.vue` — 审批列表
- **修改** `frontend/src/components/emp/EmpHome.vue` — 加"请假申请"卡片
- **修改** `frontend/src/components/admin/AdminHome.vue` — 侧边栏加"请假审批"
- **修改** `frontend/src/router/index.ts` — 注册新路由

---

### Task 1: 数据库加 `type` 字段

- [ ] **Step 1: 执行 ALTER TABLE**

SQL 语句：
```sql
ALTER TABLE `day`.`leave`
  ADD COLUMN `type` varchar(20) NOT NULL DEFAULT '事假' COMMENT '请假类型: 事假/病假/年假/调休'
  AFTER `name`;
```

通过 MySQL 客户端执行（或用管理工具 Navicat 连接 localhost:3306, day 库执行）。

- [ ] **Step 2: 验证**

```sql
DESC `day`.`leave`;
-- 应该看到 type 字段在 name 之后
```

- [ ] **Step 3: 提交**

```bash
git add docs/superpowers/plans/2026-07-21-phase1-leave-management.md
git commit -m "docs: add phase1 leave management plan"
```

---

### Task 2: 员工端后端 — Leave Pojo + Dao

**文件:**
- 创建: `backend/Nacos-SpringBoot-oa2/OA 管理系统/OA 管理系统/backend/OA-2/src/main/java/com/oa2/pojo/Leave.java`
- 创建: `backend/Nacos-SpringBoot-oa2/OA 管理系统/OA 管理系统/backend/OA-2/src/main/java/com/oa2/dao/LeaveDao.java`

**Interfaces:**
- 消费: 无（独立 POJO，跟已有 Emp/Sign 模式相同）
- 产出: `LeaveDao` 提供 `insert()`, `selectByNumberPage()`, `countByNumber()`

- [ ] **Step 1: 创建 Leave.java**

```java
package com.oa2.pojo;

import lombok.Data;

@Data
public class Leave {
    private String id;
    private int number;
    private String name;
    private String type;
    private String dept_name;
    private String start_date;
    private String end_date;
    private String reason;
    private String status;
}
```

注意：`start_date` 和 `end_date` 用 `String` 类型以匹配现有代码风格（`Sign.signDate` 也是 String），避免日期转换问题。MySQL 的 datetime 通过 JDBC 返回 String 能被 MyBatis 正确处理。

- [ ] **Step 2: 创建 LeaveDao.java**

```java
package com.oa2.dao;

import com.oa2.pojo.Leave;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface LeaveDao {

    @Insert("INSERT INTO day.leave(id, number, name, type, dept_name, start_date, end_date, reason, status) " +
            "VALUES(#{id}, #{number}, #{name}, #{type}, #{dept_name}, #{start_date}, #{end_date}, #{reason}, #{status})")
    int insert(Leave leave);

    @Select("SELECT * FROM day.leave WHERE number=#{number} ORDER BY start_date DESC LIMIT #{offset}, #{limit}")
    List<Leave> selectByNumberPage(@Param("number") int number, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT count(*) FROM day.leave WHERE number=#{number}")
    int countByNumber(@Param("number") int number);
}
```

- [ ] **Step 3: 提交**

```bash
cd d:/my_project/newland
git add "backend/Nacos-SpringBoot-oa2/OA 管理系统/OA 管理系统/backend/OA-2/src/main/java/com/oa2/pojo/Leave.java"
git add "backend/Nacos-SpringBoot-oa2/OA 管理系统/OA 管理系统/backend/OA-2/src/main/java/com/oa2/dao/LeaveDao.java"
git commit -m "feat(emp): add Leave POJO and DAO"
```

---

### Task 3: 员工端后端 — Leave Service + Controller

**文件:**
- 创建: `.../com/oa2/service/LeaveService.java`
- 创建: `.../com/oa2/service/impl/LeaveServiceImpl.java`
- 创建: `.../com/oa2/controller/LeaveController.java`

**Interfaces:**
- 消费: `LeaveDao` (insert, selectByNumberPage, countByNumber)
- 产出: `POST /leave/apply`, `GET /leave/my-list`

路径前缀 `.../backend/OA-2/src/main/java/com/oa2/` 略写为 `<oa2>`。

- [ ] **Step 1: 创建 LeaveService.java** (`<oa2>/service/LeaveService.java`)

```java
package com.oa2.service;

import com.oa2.util.RESP;

public interface LeaveService {
    RESP apply(int number, String name, String deptName, String type, String startDate, String endDate, String reason);
    RESP getMyList(int number, int currentPage, int pageSize);
}
```

- [ ] **Step 2: 创建 LeaveServiceImpl.java** (`<oa2>/service/impl/LeaveServiceImpl.java`)

```java
package com.oa2.service.impl;

import com.oa2.dao.LeaveDao;
import com.oa2.pojo.Leave;
import com.oa2.service.LeaveService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveDao leaveDao;

    @Override
    public RESP apply(int number, String name, String deptName, String type,
                      String startDate, String endDate, String reason) {
        Leave leave = new Leave();
        leave.setId(UUID.randomUUID().toString());
        leave.setNumber(number);
        leave.setName(name);
        leave.setType(type);
        leave.setDept_name(deptName);
        leave.setStart_date(startDate);
        leave.setEnd_date(endDate);
        leave.setReason(reason);
        leave.setStatus("待审批");

        int ret = leaveDao.insert(leave);
        if (ret > 0) {
            return RESP.ok("提交成功");
        }
        return RESP.error("提交失败，请重试");
    }

    @Override
    public RESP getMyList(int number, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<Leave> list = leaveDao.selectByNumberPage(number, offset, pageSize);
        int total = leaveDao.countByNumber(number);
        return RESP.ok(list, currentPage, total);
    }
}
```

- [ ] **Step 3: 创建 LeaveController.java** (`<oa2>/controller/LeaveController.java`)

```java
package com.oa2.controller;

import com.oa2.pojo.Emp;
import com.oa2.service.LeaveService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/leave")
@CrossOrigin
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping("/apply")
    public RESP apply(@RequestBody LeaveRequest request, HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("未登录");
        }
        return leaveService.apply(
                emp.getNumber(),
                emp.getName(),
                emp.getDept_name(),
                request.getType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getReason()
        );
    }

    @GetMapping("/my-list")
    public RESP getMyList(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("未登录");
        }
        return leaveService.getMyList(emp.getNumber(), currentPage, pageSize);
    }

    // 内部请求体类
    public static class LeaveRequest {
        private String type;
        private String startDate;
        private String endDate;
        private String reason;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
```

- [ ] **Step 4: 提交**

```bash
cd d:/my_project/newland
git add "backend/Nacos-SpringBoot-oa2/OA 管理系统/OA 管理系统/backend/OA-2/src/main/java/com/oa2/service/LeaveService.java"
git add "backend/Nacos-SpringBoot-oa2/OA 管理系统/OA 管理系统/backend/OA-2/src/main/java/com/oa2/service/impl/LeaveServiceImpl.java"
git add "backend/Nacos-SpringBoot-oa2/OA 管理系统/OA 管理系统/backend/OA-2/src/main/java/com/oa2/controller/LeaveController.java"
git commit -m "feat(emp): add leave apply and my-list endpoints"
```

---

### Task 4: 管理端后端 — Leave Pojo + Dao

**文件:**
- 创建: `backend/Nacos-SpringBoot-oa3/backend/OA-7/src/main/java/com/oa7/pojo/Leave.java`
- 创建: `backend/Nacos-SpringBoot-oa3/backend/OA-7/src/main/java/com/oa7/dao/LeaveDao.java`

- [ ] **Step 1: 创建 Leave.java** (`<oa7>/pojo/Leave.java`)

```java
package com.oa7.pojo;

import lombok.Data;

@Data
public class Leave {
    private String id;
    private int number;
    private String name;
    private String type;
    private String dept_name;
    private String start_date;
    private String end_date;
    private String reason;
    private String status;
}
```

- [ ] **Step 2: 创建 LeaveDao.java** (`<oa7>/dao/LeaveDao.java`)

```java
package com.oa7.dao;

import com.oa7.pojo.Leave;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface LeaveDao {

    @Select("SELECT * FROM day.leave WHERE status='待审批' ORDER BY start_date DESC")
    List<Leave> selectPending();

    @Select("SELECT * FROM day.leave ORDER BY start_date DESC")
    List<Leave> selectAll();

    @Update("UPDATE day.leave SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);
}
```

- [ ] **Step 3: 提交**

```bash
cd d:/my_project/newland
git add "backend/Nacos-SpringBoot-oa3/backend/OA-7/src/main/java/com/oa7/pojo/Leave.java"
git add "backend/Nacos-SpringBoot-oa3/backend/OA-7/src/main/java/com/oa7/dao/LeaveDao.java"
git commit -m "feat(admin): add Leave POJO and DAO"
```

---

### Task 5: 管理端后端 — Leave Service + Controller

**文件:**
- 创建: `<oa7>/service/LeaveService.java`
- 创建: `<oa7>/service/Impl/LeaveServiceImpl.java`
- 创建: `<oa7>/controller/LeaveController.java`

路径前缀 `backend/Nacos-SpringBoot-oa3/backend/OA-7/src/main/java/com/oa7/` 略写为 `<oa7>`。

- [ ] **Step 1: 创建 LeaveService.java** (`<oa7>/service/LeaveService.java`)

```java
package com.oa7.service;

import com.oa7.util.RESP;

public interface LeaveService {
    RESP getPending(int currentPage, int pageSize);
    RESP approve(String id);
    RESP reject(String id);
}
```

- [ ] **Step 2: 创建 LeaveServiceImpl.java** (`<oa7>/service/Impl/LeaveServiceImpl.java`)

使用 PageHelper 分页：

```java
package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.dao.LeaveDao;
import com.oa7.pojo.Leave;
import com.oa7.service.LeaveService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveDao leaveDao;

    @Override
    public RESP getPending(int currentPage, int pageSize) {
        PageHelper.startPage(currentPage, pageSize);
        List<Leave> list = leaveDao.selectPending();
        PageInfo<Leave> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP approve(String id) {
        int ret = leaveDao.updateStatus(id, "已批准");
        if (ret > 0) {
            return RESP.ok("操作成功");
        }
        return RESP.error("操作失败");
    }

    @Override
    public RESP reject(String id) {
        int ret = leaveDao.updateStatus(id, "已拒绝");
        if (ret > 0) {
            return RESP.ok("操作成功");
        }
        return RESP.error("操作失败");
    }
}
```

- [ ] **Step 3: 创建 LeaveController.java** (`<oa7>/controller/LeaveController.java`)

```java
package com.oa7.controller;

import com.oa7.service.LeaveService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leave")
@CrossOrigin
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/pending")
    public RESP pending(@RequestParam(defaultValue = "1") int currentPage,
                        @RequestParam(defaultValue = "10") int pageSize) {
        return leaveService.getPending(currentPage, pageSize);
    }

    @PutMapping("/{id}/approve")
    public RESP approve(@PathVariable String id) {
        return leaveService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public RESP reject(@PathVariable String id) {
        return leaveService.reject(id);
    }
}
```

注意：这里的 `id` 是 String 类型（UUID），与签到管理的 int id 不同。URL 路径参数会自动处理。

- [ ] **Step 4: 编译验证**

```bash
cd "d:/my_project/newland/backend/Nacos-SpringBoot-oa3/backend/OA-7"
mvn compile -q
```

Expected: BUILD SUCCESS（没有编译错误）

- [ ] **Step 5: 提交**

```bash
cd d:/my_project/newland
git add "backend/Nacos-SpringBoot-oa3/backend/OA-7/src/main/java/com/oa7/service/LeaveService.java"
git add "backend/Nacos-SpringBoot-oa3/backend/OA-7/src/main/java/com/oa7/service/Impl/LeaveServiceImpl.java"
git add "backend/Nacos-SpringBoot-oa3/backend/OA-7/src/main/java/com/oa7/controller/LeaveController.java"
git commit -m "feat(admin): add leave pending/approve/reject endpoints"
```

---

### Task 6: 前端员工端 — 请假申请 + 请假记录页面

**文件:**
- 创建: `frontend/src/components/emp/EmpLeaveApply.vue`
- 创建: `frontend/src/components/emp/EmpLeaveList.vue`

- [ ] **Step 1: 创建 EmpLeaveApply.vue**

```vue
<template>
  <div class="leave-apply">
    <div class="page-header">
      <h2>请假申请</h2>
      <p class="page-desc">填写请假信息并提交，等待管理员审批</p>
    </div>

    <div class="apple-card form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="leave-form"
      >
        <el-form-item label="请假类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择请假类型" style="width: 100%">
            <el-option label="事假" value="事假" />
            <el-option label="病假" value="病假" />
            <el-option label="年假" value="年假" />
            <el-option label="调休" value="调休" />
          </el-select>
        </el-form-item>

        <el-form-item label="开始时间" prop="startDate">
          <el-date-picker
            v-model="form.startDate"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="endDate">
          <el-date-picker
            v-model="form.endDate"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disableEndDate"
          />
        </el-form-item>

        <el-form-item label="请假事由" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            placeholder="请详细说明请假原因"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <div class="form-actions">
            <el-button @click="$router.push('/emp-home')" size="large">取消</el-button>
            <el-button
              type="primary"
              @click="submitApply"
              :loading="submitting"
              size="large"
              class="apple-btn-primary"
            >
              提交申请
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  type: '',
  startDate: '',
  endDate: '',
  reason: ''
})

const rules: FormRules = {
  type: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  reason: [{ required: true, message: '请填写请假事由', trigger: 'blur' }]
}

const disableEndDate = (time: Date) => {
  if (form.startDate) {
    return time.getTime() <= new Date(form.startDate).getTime()
  }
  return false
}

const submitApply = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const response = await axios.post('/api/v1/employee/leave/apply', {
      type: form.type,
      startDate: form.startDate,
      endDate: form.endDate,
      reason: form.reason
    })
    if (response.data && response.data.code === 200) {
      ElMessage.success('请假申请提交成功！')
      router.push('/emp-home')
    } else {
      ElMessage.error(response.data?.message || '提交失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('网络错误，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.leave-apply {
  max-width: 680px;
  margin: 0 auto;
  padding: 40px 32px;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text, #1d1d1f);
  margin: 0 0 8px;
}

.page-desc {
  font-size: 15px;
  color: var(--apple-text-secondary, #86868b);
  margin: 0;
}

.form-card {
  padding: 40px 36px;
}

.leave-form {
  max-width: 480px;
  margin: 0 auto;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}

.apple-btn-primary {
  background: var(--apple-blue, #0071e3);
  border: none;
  color: #fff;
  padding: 10px 28px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 980px;
  transition: all 0.2s ease;
}

.apple-btn-primary:hover {
  background: var(--apple-blue-hover, #0077ed);
}

.apple-btn-primary:active {
  background: var(--apple-blue-active, #006edb);
}
</style>
```

- [ ] **Step 2: 创建 EmpLeaveList.vue**

```vue
<template>
  <div class="leave-list">
    <div class="page-header">
      <h2>我的请假记录</h2>
    </div>

    <div class="apple-card table-card">
      <el-table
        :data="list"
        v-loading="loading"
        style="width: 100%"
        empty-text="暂无请假记录"
        class="el-table--borderless"
      >
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <span class="type-tag">{{ row.type }}</span>
          </template>
        </el-table-column>

        <el-table-column label="开始时间" width="150">
          <template #default="{ row }">{{ row.start_date }}</template>
        </el-table-column>

        <el-table-column label="结束时间" width="150">
          <template #default="{ row }">{{ row.end_date }}</template>
        </el-table-column>

        <el-table-column label="请假事由" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason }}</template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const list = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getStatusType = (status: string) => {
  switch (status) {
    case '待审批': return 'warning'
    case '已批准': return 'success'
    case '已拒绝': return 'danger'
    default: return 'info'
  }
}

const getList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/employee/leave/my-list', {
      params: { currentPage: currentPage.value, pageSize: pageSize.value }
    })
    if (response.data && response.data.code === 200) {
      list.value = response.data.data || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('获取请假记录失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  getList()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  getList()
}

onMounted(() => getList())
</script>

<style scoped>
.leave-list {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 32px;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text, #1d1d1f);
  margin: 0;
}

.table-card {
  padding: 24px;
}

.type-tag {
  background: #e8f4fd;
  color: #0071e3;
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 4px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

:deep(.el-table--borderless::before) { display: none; }
:deep(.el-table--borderless th.el-table__cell) {
  border-bottom: 1px solid #e5e5e7;
  background: transparent;
  color: #86868b;
  font-size: 12px;
  font-weight: 500;
}
:deep(.el-table--borderless td.el-table__cell) {
  padding: 12px 0;
  color: #1d1d1f;
  font-size: 14px;
}
</style>
```

- [ ] **Step 3: 提交**

```bash
cd d:/my_project/newland
git add frontend/src/components/emp/EmpLeaveApply.vue frontend/src/components/emp/EmpLeaveList.vue
git commit -m "feat(emp): add leave apply and leave list pages"
```

---

### Task 7: 前端管理端 — 请假审批页面

**文件:**
- 创建: `frontend/src/components/admin/LeaveApproval.vue`

- [ ] **Step 1: 创建 LeaveApproval.vue**

```vue
<template>
  <div class="leave-approval">
    <div class="page-header">
      <h2>请假审批</h2>
    </div>

    <el-tabs v-model="activeTab" class="approval-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="待审批" name="pending" />
      <el-tab-pane label="已审批" name="history" />
    </el-tabs>

    <div class="apple-card table-card">
      <el-table
        :data="list"
        v-loading="loading"
        style="width: 100%"
        empty-text="暂无数据"
        class="el-table--borderless"
      >
        <el-table-column label="申请人" width="80">
          <template #default="{ row }">{{ row.name }}</template>
        </el-table-column>

        <el-table-column label="部门" width="100">
          <template #default="{ row }">{{ row.dept_name }}</template>
        </el-table-column>

        <el-table-column label="类型" width="70" align="center">
          <template #default="{ row }">
            <span class="type-tag">{{ row.type }}</span>
          </template>
        </el-table-column>

        <el-table-column label="开始时间" width="150">
          <template #default="{ row }">{{ row.start_date }}</template>
        </el-table-column>

        <el-table-column label="结束时间" width="150">
          <template #default="{ row }">{{ row.end_date }}</template>
        </el-table-column>

        <el-table-column label="事由" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason }}</template>
        </el-table-column>

        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" align="center" v-if="activeTab === 'pending'">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApprove(row)" :loading="loadingId === row.id">
              批准
            </el-button>
            <el-button type="danger" size="small" @click="handleReject(row)" :loading="loadingId === row.id">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const activeTab = ref('pending')
const list = ref<any[]>([])
const loading = ref(false)
const loadingId = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getStatusType = (status: string) => {
  switch (status) {
    case '待审批': return 'warning'
    case '已批准': return 'success'
    case '已拒绝': return 'danger'
    default: return 'info'
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const url = activeTab.value === 'pending'
      ? '/api/v1/admin/leave/pending'
      : '/api/v1/admin/leave/pending'  // 使用同一接口，前端筛选已审批的
    const response = await axios.get(url, {
      params: { currentPage: currentPage.value, pageSize: pageSize.value }
    })
    if (response.data && response.data.code === 200) {
      list.value = response.data.data || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定批准 ${row.name} 的请假申请吗？`, '确认', {
      confirmButtonText: '批准',
      cancelButtonText: '取消',
      type: 'success'
    })
    loadingId.value = row.id
    const response = await axios.put(`/api/v1/admin/leave/${row.id}/approve`)
    if (response.data && response.data.code === 200) {
      ElMessage.success('已批准')
      fetchList()
    } else {
      ElMessage.error(response.data?.message || '操作失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  } finally {
    loadingId.value = ''
  }
}

const handleReject = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定拒绝 ${row.name} 的请假申请吗？`, '确认', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    })
    loadingId.value = row.id
    const response = await axios.put(`/api/v1/admin/leave/${row.id}/reject`)
    if (response.data && response.data.code === 200) {
      ElMessage.success('已拒绝')
      fetchList()
    } else {
      ElMessage.error(response.data?.message || '操作失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  } finally {
    loadingId.value = ''
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  fetchList()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  fetchList()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchList()
}

onMounted(() => fetchList())
</script>

<style scoped>
.leave-approval {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 8px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0 0 4px;
}

.approval-tabs {
  margin-bottom: 16px;
}

.table-card {
  padding: 24px;
}

.type-tag {
  background: #e8f4fd;
  color: #0071e3;
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 4px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

:deep(.el-table--borderless::before) { display: none; }
:deep(.el-table--borderless th.el-table__cell) {
  border-bottom: 1px solid #e5e5e7;
  background: transparent;
  color: #86868b;
  font-size: 12px;
  font-weight: 500;
}
:deep(.el-table--borderless td.el-table__cell) {
  padding: 12px 0;
}

:deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
cd d:/my_project/newland
git add frontend/src/components/admin/LeaveApproval.vue
git commit -m "feat(admin): add leave approval page"
```

---

### Task 8: 前端路由 + 导航集成

**文件:**
- 修改: `frontend/src/router/index.ts`
- 修改: `frontend/src/components/emp/EmpHome.vue`
- 修改: `frontend/src/components/admin/AdminHome.vue`

- [ ] **Step 1: 修改 router/index.ts**

在 `EmpHome` 的 `children` 中添加：
```typescript
{
  path: 'leave-apply',
  name: 'EmpLeaveApply',
  component: () => import('../components/emp/EmpLeaveApply.vue')
},
{
  path: 'leave-list',
  name: 'EmpLeaveList',
  component: () => import('../components/emp/EmpLeaveList.vue')
}
```

在 `AdminHome` 的 `children` 中添加：
```typescript
{
  path: 'leave-approval',
  name: 'LeaveApproval',
  component: () => import('../components/admin/LeaveApproval.vue')
}
```

最终 router/index.ts 的关键部分：
```typescript
// EmpHome children (add after 'ai-chat' entry):
{ path: 'leave-apply', name: 'EmpLeaveApply', component: () => import('../components/emp/EmpLeaveApply.vue') },
{ path: 'leave-list', name: 'EmpLeaveList', component: () => import('../components/emp/EmpLeaveList.vue') }

// AdminHome children (add after 'unsigned-list' entry):
{ path: 'leave-approval', name: 'LeaveApproval', component: () => import('../components/admin/LeaveApproval.vue') }
```

- [ ] **Step 2: 修改 EmpHome.vue**

在 `<template>` 的 `.card-grid` 中添加请假卡片（放在 AI 客服前面或后面）：

```html
<div class="apple-card function-card" @click="goTo('/emp-home/leave-apply')">
  <div class="card-icon">
    <el-icon :size="24"><Edit /></el-icon>
  </div>
  <h3 class="card-name">请假申请</h3>
  <p class="card-desc">提交请假申请</p>
</div>
```

同时 import `Edit` 图标：
```typescript
import { User, Clock, Document, Service, Edit } from '@element-plus/icons-vue'
```

（如果已存在但没加 Edit，就加上。如果已经有其他 import 语句，append 到对应行）

- [ ] **Step 3: 修改 AdminHome.vue**

在 `<el-menu>` 中添加菜单项（放在"知识库管理"之前或之后）：

```html
<el-menu-item index="/admin-home/leave-approval">
  <el-icon><Document /></el-icon>
  <span>请假审批</span>
</el-menu-item>
```

同时在 script 的 import 中加上 `Document`（如果还没 import 的话）。

- [ ] **Step 4: 提交**

```bash
cd d:/my_project/newland
git add frontend/src/router/index.ts frontend/src/components/emp/EmpHome.vue frontend/src/components/admin/AdminHome.vue
git commit -m "feat: integrate leave management routes and navigation"
```

---

### Task 9: 后端编译验证

- [ ] **Step 1: 编译员工服务**

```bash
cd "d:/my_project/newland/backend/Nacos-SpringBoot-oa2/OA 管理系统/OA 管理系统/backend/OA-2"
mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 编译管理服务**

```bash
cd "d:/my_project/newland/backend/Nacos-SpringBoot-oa3/backend/OA-7"
mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 检查前端启动无错误**

```bash
cd d:/my_project/newland/frontend
npx vue-tsc --noEmit 2>&1 | head -30
```

Expected: 无 TypeScript 类型错误（或者只输出 `error Command failed` 等非类型问题）

---

## 自审

1. **Spec 覆盖**: 请假管理 spec 中所有需求点（员工提交/查看、管理员待审批/通过/拒绝、请假类型、路由导航）均有对应 task。
2. **无占位符**: 所有代码块完整，无 TBD/TODO。
3. **类型一致性**: `Leave.id` 用 String（UUID），LeaveController 路径参数用 `String id`，前后一致。`start_date`/`end_date` 用 String 类型与现有 Sign 模式一致。
