# Newland OA 项目启动指南

## 项目结构

```
d:/my_project/newland/
├── frontend/                          # Vue 前端 (Vite + TypeScript)
└── backend/
    ├── Nacos-SpringBoot-oa1/          # 网关服务 (Gateway, 端口 8888)
    ├── Nacos-SpringBoot-oa2/          # 员工服务 (OA-2, 端口 8081) - 独立版
    └── Nacos-SpringBoot-oa3/          # 管理员服务 (OA-7, 端口 8082)
```

---

## 前置依赖

| 依赖 | 位置 | 端口 |
|:---|:---|---:|
| MySQL 8.x | 系统服务 | 3306 |
| Redis | `D:\develop\Redis\redis-server.exe` | 6379 |
| Nacos 2.3.2 | `oa1/.oa-tools/nacos/` | 8848 |
| Elasticsearch 7.x | `D:\develop\elasticsearch\elasticsearch-7.13.0` | 9201 |
| JDK 21 | `C:\Program Files\Java\jdk-21` | - |
| Maven 3.9.x | `D:\develop\apache-maven-3.9.4-bin\...` | - |
| Node.js | 需安装 | - |

---

## 启动顺序

### 1. MySQL

MySQL 一般为 Windows 服务自动启动。如需手动：

```bash
net start MySQL80
```

数据库 `day` 需已建好，root 密码：`ljn050825`

### 2. Redis

```bash
"D:\develop\Redis\redis-server.exe" --port 6379
```

验证：
```bash
"D:\develop\Redis\redis-cli.exe" ping
# 返回 PONG
```

### 3. Nacos（服务注册与配置中心）

```bash
cd "d:/my_project/newland/backend/Nacos-SpringBoot-oa1/OA 管理系统/.oa-tools/nacos"

export JAVA_HOME="C:/Program Files/Java/jdk-21"

java -Xms512m -Xmx512m -Xmn256m -Dnacos.standalone=true \
  -Dloader.path="./plugins/health,./plugins/cmdb,./plugins/selector" \
  -Dnacos.home="." \
  -jar target/nacos-server.jar \
  --spring.config.additional-location=file:./conf/ \
  --logging.config=./conf/nacos-logback.xml nacos.nacos
```

验证：`http://localhost:8848/nacos`（默认账号密码 nacos/nacos）

### 4. Elasticsearch

> ⚠️ Cpolar 会占用 9200 端口，所以 ES 需要用 9201 端口

```bash
cd "D:\develop\elasticsearch\elasticsearch-7.13.0\bin"
./elasticsearch.bat -Ehttp.port=9201 -Etransport.port=9301
```

验证：
```bash
curl "http://localhost:9201/"
# 返回 JSON 即为成功
```

### 5. 后端微服务

#### 5a. 网关 (Gateway, 端口 8888)

```bash
cd "d:/my_project/newland/backend/Nacos-SpringBoot-oa1/OA 管理系统/OA 管理系统/backend/gateway"

java -jar target/gateway-1.0.0.jar \
  --spring.cloud.nacos.config.import-check.enabled=false
```

#### 5b. 员工服务 (OA-2, 端口 8081)

> ⚠️ 使用独立的 Nacos-SpringBoot-oa2 项目（含 AI 客服功能）

```bash
cd "d:/my_project/newland/backend/Nacos-SpringBoot-oa2/OA 管理系统/OA 管理系统/backend/OA-2"

java -jar target/oa-emp-service-1.0.0.jar \
  --spring.datasource.username=root \
  --spring.datasource.password=ljn050825 \
  --spring.cloud.nacos.config.import-check.enabled=false \
  "--spring.datasource.url=jdbc:mysql://localhost:3306/day?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
  --elasticsearch.host=localhost \
  --elasticsearch.port=9201
```

#### 5c. 管理员服务 (OA-7, 端口 8082)

```bash
cd "d:/my_project/newland/backend/Nacos-SpringBoot-oa3/backend/OA-7"

java -jar target/oa-admin-service-1.0.0.jar \
  --spring.datasource.username=root \
  --spring.datasource.password=ljn050825 \
  --spring.cloud.nacos.config.import-check.enabled=false \
  "--spring.datasource.url=jdbc:mysql://localhost:3306/day?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true"
```

### 6. 前端 (Vite, 端口 5173)

```bash
cd "d:/my_project/newland/frontend"
npm run dev
```

---

## 可用账号

### 员工端（OA-2，端口 8081）

| 工号 | 密码 | 说明 |
|:---:|:---:|:---|
| 129 | `123` | 通用测试账号 |
| 134 | `123` | |
| 136 | `123` | |
| 128 | `123123` | |
| 123 | `123123` | |

### 管理员端（OA-7，端口 8082）

| 用户名 | 密码 |
|:---:|:---:|
| `test` | `123` |
| `chenle` | `123123` |
| `zhangsan` | `123123` |

---

## 验证服务

| 服务 | 地址 | 验证方法 |
|:---|:---|---:|
| 前端 | `http://localhost:5173` | 浏览器打开 |
| API 网关 | `http://localhost:8888` | 返回 404 正常（路由不存在） |
| 员工登录 | `POST http://localhost:8888/api/v1/employee/login` | `{"number":129,"pwd":"123"}` → `true` |
| 管理员登录 | `POST http://localhost:8888/api/v1/admin/auth/login` | `{"name":"test","pwd":"123"}` → `true` |
| 位置解析 | `GET http://localhost:8888/api/v1/employee/location/address?coordinates=26.0745,119.2965` | → 返回地址 |
| AI客服 | `POST http://localhost:8888/api/v1/employee/chat/ask` | `{"question":"怎么签到"}` → 返回回答 |

---

## 一键启动参考（按顺序分别执行）

```bash
# 终端 1: Redis
"D:/develop/Redis/redis-server.exe" --port 6379

# 终端 2: Nacos
# 在 oa1/.oa-tools/nacos 下执行 java -jar ...

# 终端 3: Elasticsearch
# 在 D:\develop\elasticsearch\...\bin 下执行 elasticsearch.bat

# 终端 4: 后端
# 先启动 Gateway，再启动 OA-2 和 OA-7

# 终端 5: 前端
cd d:/my_project/newland/frontend && npm run dev
```

---

## 常见问题

**Q: 端口被占用怎么办？**
- 9200 被 Cpolar 占用是正常的，ES 改用 9201
- 使用 `netstat -ano | grep 端口号` 查看占用进程

**Q: Maven 依赖下载慢？**
- 配置文件 `D:\develop\newland_repo\settings.xml` 已配阿里云镜像

**Q: Redis 连不上？**
- 检查 Redis 进程是否启动：`netstat -ano | grep 6379`

**Q: 服务启动后 Nacos 找不到？**
- 确保 Nacos 先于所有微服务启动
- Nacos 需要约 15 秒完成启动

**Q: 前端显示「AI 在线 · 知识库问答」？**
- 这是正常的，AI 客服走 DeepSeek API（非本地流式服务）
- 需要确保 `application.yml` 中有配置 `deepseek.api-key`
