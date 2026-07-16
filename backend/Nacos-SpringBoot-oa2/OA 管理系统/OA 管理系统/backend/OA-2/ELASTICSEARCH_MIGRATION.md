# OA 员工打卡记录 Elasticsearch 迁移指南

## 概述

本文档描述了如何将 OA 系统中员工的打卡记录从 MySQL 迁移到 Elasticsearch 存储。

## 修改内容

### 1. 依赖更新
- 在 `pom.xml` 中添加了 Elasticsearch 相关依赖
- 添加了 `spring-boot-starter-data-elasticsearch`
- 添加了 `elasticsearch-rest-high-level-client`

### 2. 实体类更新
- 更新了 `Sign.java` 实体类，添加了 Elasticsearch 注解
- 添加了索引名称、字段类型、分析器等配置
- 新增了 `timestamp` 和 `dateOnly` 字段用于更好的查询和排序

### 3. 新增配置类
- `ElasticsearchConfig.java`：Elasticsearch 连接配置
- `ServiceConfig.java`：服务配置管理

### 4. 新增 Repository
- `SignElasticsearchRepository.java`：基于 Spring Data Elasticsearch 的数据访问层

### 5. 新增 Service 实现
- `SignServiceElasticsearchImpl.java`：使用 Elasticsearch 的服务实现
- 保持了与原有 MySQL 实现相同的接口

### 6. 数据迁移工具
- `DataMigrationUtil.java`：数据迁移工具类
- `DataMigrationController.java`：提供迁移相关的 REST API

## 部署步骤

### 1. 安装 Elasticsearch

确保您的环境中已安装并运行 Elasticsearch 7.x 或以上版本：

```bash
# 下载并启动 Elasticsearch
docker run -d --name elasticsearch \
  -p 9200:9200 -p 9300:9300 \
  -e "discovery.type=single-node" \
  elasticsearch:7.17.0
```

### 2. 配置文件更新

在 `application.yml` 中：

```yaml
# 存储配置 - 切换存储类型
sign:
  storage:
    type: elasticsearch  # 可选值: mysql, elasticsearch

# Elasticsearch 配置
elasticsearch:
  host: localhost
  port: 9200
```

### 3. 切换存储方式

系统会根据 `sign.storage.type` 配置自动选择使用的实现：
- `mysql`：使用原有的 MySQL 存储（默认）
- `elasticsearch`：使用新的 Elasticsearch 存储

### 4. 数据迁移（可选）

如果您有现有的 MySQL 数据需要迁移：

1. 临时将配置改为 `mysql` 以获取现有数据
2. 使用迁移工具将数据导入 Elasticsearch
3. 切换配置为 `elasticsearch`

### 5. 验证迁移

通过以下 API 验证迁移结果：

```bash
# 验证 Elasticsearch 中的数据
GET /api/v1/employee/migration/validate

# 查看当前存储状态
GET /api/v1/employee/migration/status
```

## API 变更

前端 API 接口保持不变，无需修改前端代码：

- `GET /api/v1/employee/attendance/my-records` - 获取员工签到记录
- `GET /api/v1/employee/attendance/my-records/page` - 分页查询签到记录
- `POST /api/v1/employee/attendance/check-in` - 员工签到

## 性能优势

使用 Elasticsearch 后的优势：

1. **更快的查询速度**：特别是在大数据量的情况下
2. **强大的搜索功能**：支持复杂的查询条件和聚合
3. **水平扩展能力**：可以轻松扩展到多节点集群
4. **实时性**：数据写入后几乎立即可查询

## 索引结构

Elasticsearch 中的签到记录索引结构：

```json
{
  "employee_sign_records": {
    "mappings": {
      "properties": {
        "id": { "type": "keyword" },
        "signDate": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss:SSS" },
        "number": { "type": "integer" },
        "state": { "type": "keyword" },
        "dept_name": { "type": "keyword" },
        "name": { "type": "text", "analyzer": "ik_smart" },
        "type": { "type": "keyword" },
        "sign_address": { "type": "text", "analyzer": "ik_smart" },
        "tag": { "type": "integer" },
        "timestamp": { "type": "long" },
        "dateOnly": { "type": "date", "format": "yyyy-MM-dd" }
      }
    }
  }
}
```

## 注意事项

1. 确保 Elasticsearch 服务正常运行
2. 生产环境建议配置 Elasticsearch 集群以保证高可用
3. 定期备份 Elasticsearch 数据
4. 监控 Elasticsearch 集群的性能和存储使用情况

## 回滚方案

如果需要回滚到 MySQL：

1. 将 `sign.storage.type` 改为 `mysql`
2. 重启应用
3. 系统会自动切换回 MySQL 存储

## 联系支持

如有问题，请联系开发团队。 