# OA-3 管理员后端启动说明

## 前置依赖
启动前需确保以下服务已运行：
- MySQL（端口 3306）
- Nacos（端口 8848）
- Redis（端口 6379）

## IDEA 启动

用 IDEA 打开 `backend` 目录，在右上角 Run/Debug 下拉框选择：

| 配置名 | 对应服务 | 端口 |
|:---|:---|---:|
| `GatewayApplication` | 网关 | 8888 |
| `Oa3Application` | 管理员服务 (OA-7) | 8082 |

**启动顺序：** Gateway → OA-7

> `Oa3Application` 已在运行配置中自带参数 `--spring.datasource.password=ljn050825`，直接运行即可

## 验证

```bash
# 管理员登录
curl -s -X POST "http://localhost:8888/api/v1/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"name":"test","pwd":"123"}'
```
