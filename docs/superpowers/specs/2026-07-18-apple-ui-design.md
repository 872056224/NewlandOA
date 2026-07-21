# Apple 风格 UI 改版设计

## 概述
将 OA 系统前端（员工端 + 管理员端共 16 个页面）从 Element Plus 默认样式改版为苹果官网风格：简约、留白、黑白灰 + 单色 CTA。

## 设计系统

### 配色
| Token | 色值 | 用途 |
|:---|:---|:---|
| `--apple-white` | `#FFFFFF` | 主背景 |
| `--apple-bg` | `#F5F5F7` | 页面背景/卡片背景 |
| `--apple-bg-secondary` | `#E8E8ED` | 分割线/次要区域 |
| `--apple-text` | `#1D1D1F` | 主要文字 |
| `--apple-text-secondary` | `#86868B` | 次要文字/说明 |
| `--apple-blue` | `#0071E3` | 唯一主色（按钮/链接） |
| `--apple-green` | `#34C759` | 成功/已签到 |
| `--apple-orange` | `#FF9500` | 警告/迟到 |
| `--apple-red` | `#FF3B30` | 危险/错误 |

### 字体
```css
font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text',
             'Helvetica Neue', Helvetica, Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;
```

### 圆角与阴影
- 卡片圆角: `12px` / `16px`
- 按钮圆角: `980px` (pill)
- 输入框圆角: `10px`
- 卡片阴影: `0 2px 12px rgba(0,0,0,0.06)`
- 悬浮阴影: `0 4px 20px rgba(0,0,0,0.1)`
- 过渡: `all 0.2s ease`

### 间距
- 页面 padding: `40px` 左右
- 卡片 padding: `24px` / `32px`
- 组件间距: `16px` / `24px` / `32px`

## 页面设计

### 1. 登录页（EmpLogin / AdminLogin）
- 居中卡片（400px 宽），白底柔和阴影
- Logo + 大标题「欢迎回来」+ 副标题
- 极淡边框输入框，focus 时蓝色描边
- Pill 蓝色按钮全宽

### 2. 首页（EmpHome / Dashboard）
- 欢迎语 + 当前时间
- 4 个功能卡片（签到、信息、记录、客服）等宽排列
- 管理员仪表盘 4 个统计卡片（员工数、部门数、职务数、今日签到）
- 悬停上浮 + 阴影

### 3. 签到页（EmpSignIn）
- 大时间显示
- 上班/下班两张对称签到卡
- 蓝色 pill 签到按钮（已签到变灰不可点）
- 简洁签到记录列表

### 4. 列表页（EmpList、DeptManage、DutyManage、SignList、UnsignedList、KbManage）
- 大标题 + 右侧蓝色新增按钮
- 极淡搜索框
- 表格去边框，只保留水平分割线
- 灰色小字表头
- 简约分页
- 弹窗白底大圆角无边框

### 5. 个人信息页（EmpInfo）
- 卡片式信息展示
- 每行「标签: 值」简洁排列
- 编辑按钮，点击弹出极简表单

### 6. 签到记录页（EmpSignMessage）
- 列表同上表格风格

### 7. 修改密码页（EmpUpdatePwd）
- 居中表单，同登录页风格

### 8. AI 客服（EmpAiChat）
- 对话气泡：用户蓝底，AI 白底
- 纯色无边框输入框
- 简洁消息列表

### 9. 知识库管理（KbManage）
- 同列表页风格

### 10. 签到统计（SignStatistics）
- ECharts 图表，配色改为苹果蓝灰系

## 实现方式
1. 重写 `base.css` / `main.css` 为 Apple 设计 tokens
2. 用 CSS 变量覆盖 Element Plus 主题
3. 逐个页面调整布局 class，去掉多余装饰性样式
4. 精简 Element Plus 组件的默认样式（表格、分页、弹窗、标签）

## 不改造
- 功能逻辑代码不动
- 后端 API 不变
- 路由不变
