# Apple 风格 UI 改版 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 OA 系统 16 个前端页面从 Element Plus 默认样式改为苹果官网设计风格

**Architecture:** 通过 CSS 变量覆盖 Element Plus 主题 + 全局 Apple 设计 tokens + 逐页调整布局 class，不动功能逻辑和 JS

**Tech Stack:** Vue 3 + Element Plus + TypeScript + Vite

## Global Constraints

- 不改功能逻辑代码、后端 API、路由
- 所有 Element Plus 组件通过 CSS 变量覆盖主题，不直接修改组件源码
- 字体使用系统原生 `-apple-system`, `BlinkMacSystemFont`, `SF Pro`, `PingFang SC`
- 唯一主色 `#0071E3`，仅用于按钮/链接
- 卡片圆角 `12px`，按钮圆角 `980px`
- 页面 padding 不低于 `32px`，保持大留白

---

### Task 1: 全局 CSS 变量与 Element Plus 主题覆盖

**Files:**
- Modify: `frontend/src/assets/base.css`
- Modify: `frontend/src/assets/main.css`

**Interfaces:**
- Produces: CSS 变量 `--apple-*` 可在所有组件中使用；Element Plus 组件自动继承主题

- [ ] **Step 1: 重写 `base.css` — Apple 设计 tokens**

```css
:root {
  /* 配色 */
  --apple-white: #FFFFFF;
  --apple-bg: #F5F5F7;
  --apple-bg-secondary: #E8E8ED;
  --apple-text: #1D1D1F;
  --apple-text-secondary: #86868B;
  --apple-text-tertiary: #B0B0B8;
  --apple-blue: #0071E3;
  --apple-blue-hover: #0077ED;
  --apple-blue-active: #006EDB;
  --apple-green: #34C759;
  --apple-orange: #FF9500;
  --apple-red: #FF3B30;
  --apple-border: #D2D2D7;
  --apple-shadow: 0 2px 12px rgba(0,0,0,0.06);
  --apple-shadow-hover: 0 4px 20px rgba(0,0,0,0.1);
  --apple-radius: 10px;
  --apple-radius-card: 12px;
  --apple-radius-button: 980px;
  --apple-font: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', Helvetica, Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;

  /* Element Plus 变量覆盖 */
  --el-color-primary: var(--apple-blue);
  --el-color-success: var(--apple-green);
  --el-color-warning: var(--apple-orange);
  --el-color-danger: var(--apple-red);
  --el-border-radius-base: var(--apple-radius);
  --el-border-color: var(--apple-border);
  --el-text-color-primary: var(--apple-text);
  --el-text-color-secondary: var(--apple-text-secondary);
  --el-text-color-placeholder: var(--apple-text-tertiary);
  --el-bg-color: var(--apple-white);
  --el-bg-color-page: var(--apple-bg);
  --el-fill-color-light: var(--apple-bg);
  --el-font-family: var(--apple-font);
  --el-border-radius-round: var(--apple-radius-button);
}

* { box-sizing: border-box; }

body {
  margin: 0;
  font-family: var(--apple-font);
  background: var(--apple-bg);
  color: var(--apple-text);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
```

- [ ] **Step 2: 重写 `main.css` — 全局样式**

```css
html { font-size: 16px; }

/* 标题 */
.apple-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--apple-text);
  letter-spacing: -0.5px;
  margin: 0 0 8px;
}

.apple-subtitle {
  font-size: 16px;
  color: var(--apple-text-secondary);
  margin: 0 0 32px;
}

/* 卡片 */
.apple-card {
  background: var(--apple-white);
  border-radius: var(--apple-radius-card);
  box-shadow: var(--apple-shadow);
  padding: 24px;
  transition: all 0.2s ease;
}
.apple-card:hover {
  box-shadow: var(--apple-shadow-hover);
  transform: translateY(-2px);
}

/* 输入框 */
.apple-input {
  border: 1px solid var(--apple-border) !important;
  border-radius: var(--apple-radius) !important;
  padding: 12px 16px !important;
  font-size: 15px !important;
  transition: all 0.2s ease !important;
}
.apple-input:focus {
  border-color: var(--apple-blue) !important;
  box-shadow: 0 0 0 3px rgba(0,113,227,0.15) !important;
}

/* 按钮 - pill */
.apple-btn {
  border-radius: var(--apple-radius-button) !important;
  padding: 12px 24px !important;
  font-size: 15px !important;
  font-weight: 500 !important;
  transition: all 0.2s ease !important;
}
.apple-btn-primary {
  background: var(--apple-blue) !important;
  border: none !important;
  color: white !important;
}
.apple-btn-primary:hover { background: var(--apple-blue-hover) !important; }

/* 页面容器 */
.apple-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 32px;
}
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/assets/
git commit -m "style: add Apple design tokens and global CSS"
```

---

### Task 2: 登录页改版（EmpLogin + AdminLogin）

**Files:**
- Modify: `frontend/src/components/emp/EmpLogin.vue`
- Modify: `frontend/src/components/admin/AdminLogin.vue`

- [ ] **Step 1: 修改 EmpLogin.vue — Apple 风格**

去掉原 template 中所有花哨颜色/背景装饰，改为：
- 全屏居中（flex），背景色 `var(--apple-bg)`
- 居中的白色卡片（宽 400px，`apple-card` class）
- 卡片内：图标/logo → 「欢迎回来」标题 → 「登录你的账号」副标题
- 输入框加 `apple-input` class
- 登录按钮为全宽 `apple-btn apple-btn-primary` style
- 员工端 vs 管理端切换：底部一行小字灰色链接

- [ ] **Step 2: 修改 AdminLogin.vue — 同上**

与 EmpLogin 保持完全一致的布局和样式，标题改为「管理员登录」

- [ ] **Step 3: 测试**

启动前端，确认两个登录页显示为苹果风格极简卡片

- [ ] **Step 4: 提交**

```bash
git add frontend/src/components/emp/EmpLogin.vue frontend/src/components/admin/AdminLogin.vue
git commit -m "style: redesign login pages with Apple-style cards"
```

---

### Task 3: 首页改版（EmpHome + Dashboard）

**Files:**
- Modify: `frontend/src/components/emp/EmpHome.vue`
- Modify: `frontend/src/components/admin/Dashboard.vue`

- [ ] **Step 1: EmpHome.vue**

改为：
- 页面顶部「上午好/下午好，用户名」大标题 + 当前时间
- 四个功能卡片（签到、信息、记录、客服），网格 2×2 或 4 列
- 每个卡片 220×140px，白底圆角，图标 + 名称 + 简短描述
- 使用 `apple-card` class

- [ ] **Step 2: Dashboard.vue**

改为：
- 统计卡片 4 列等宽：大数字 + 灰色标签
- ECharts 图配色改为 Apple 蓝灰色系
- 整体布局对齐 `apple-page` 容器

- [ ] **Step 3: 提交**

```bash
git commit -m "style: redesign home pages with Apple card grid"
```

---

### Task 4: 签到页改版（EmpSignIn）

**Files:**
- Modify: `frontend/src/components/emp/EmpSignIn.vue`
- Modify: `frontend/src/components/emp/EmpSignMessage.vue`

- [ ] **Step 1: EmpSignIn.vue**

改为：
- 顶部签到大时钟（当前时间大字显示）
- 上班/下班两张签到卡并排，白底圆角
- 未签到状态：蓝色 pill 按钮；已签到：灰色不可点击
- 下方最近签到记录精简列表

- [ ] **Step 2: EmpSignMessage.vue**

签到记录表格改为去边框风格（`el-table--borderless`），表头灰色小字

- [ ] **Step 3: 提交**

```bash
git commit -m "style: redesign sign-in page with Apple cards"
```

---

### Task 5: 列表页改版（EmpList, DeptManage, DutyManage, SignList, UnsignedList, KbManage）

**Files:**
- Modify: `frontend/src/components/admin/EmpList.vue`
- Modify: `frontend/src/components/admin/DeptManage.vue`
- Modify: `frontend/src/components/admin/DutyManage.vue`
- Modify: `frontend/src/components/admin/SignList.vue`
- Modify: `frontend/src/components/admin/UnsignedList.vue`
- Modify: `frontend/src/components/admin/KbManage.vue`

- [ ] **Step 1: 统一样式**

所有列表页统一：
- 页面用 `apple-page` 容器
- 标题用 `apple-title`
- 搜索框用 `apple-input`
- 新增按钮用 `apple-btn apple-btn-primary`
- El-Table 去掉边框 `:border="false" stripe`，表头加 class 灰色
- El-Pagination 去掉背景和边框

- [ ] **Step 2: 逐个修改 6 个组件**

重点改：类名、el-table 属性、el-button 替换为 apple-btn 类

- [ ] **Step 3: 提交**

```bash
git commit -m "style: redesign list pages with borderless tables and Apple inputs"
```

---

### Task 6: 剩余页面改版（EmpInfo, EmpUpdatePwd, EmpAiChat, SignStatistics）

**Files:**
- Modify: `frontend/src/components/emp/EmpInfo.vue`
- Modify: `frontend/src/components/emp/EmpUpdatePwd.vue`
- Modify: `frontend/src/components/emp/EmpAiChat.vue`
- Modify: `frontend/src/components/admin/SignStatistics.vue`

- [ ] **Step 1: EmpInfo.vue + EmpUpdatePwd.vue**

个人信息和改密码用卡片式布局，输入框 `apple-input`

- [ ] **Step 2: EmpAiChat.vue**

对话气泡：用户蓝底 `#0071E3` 白字，AI 白底灰字；输入框简洁无边框

- [ ] **Step 3: SignStatistics.vue**

图表配色改为 `#0071E3`、`#34C759`、`#FF9500`

- [ ] **Step 4: 提交**

```bash
git commit -m "style: redesign remaining pages with Apple design system"
```

---

### Task 7: 基础布局改版（EmpHome 侧边栏 + Header）

**Files:**
- Modify: `frontend/src/components/emp/EmpHome.vue`
- Modify: `frontend/src/components/admin/AdminHome.vue`

- [ ] **Step 1: 侧边栏与顶栏**

顶栏去掉彩色背景，纯白 + 底部极淡分割线
侧边栏去掉背景色块，灰色文字，active 时蓝色文字
图标和文字间距调整

- [ ] **Step 2: 提交**

```bash
git commit -m "style: redesign sidebar and header with Apple minimalism"
```
