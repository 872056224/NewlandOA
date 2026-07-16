
# 开发规范指南
为保证OA办公系统微服务项目的代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目基础信息
| 项 | 说明 |
|----|------|
| 代码作者 | hp |
| 用户工作目录 | `E:\JavaEE\wuhangligong\Nacos-SpringBoot-oa2\OA 管理系统\OA 管理系统\backend` |
| 项目类型 | OA办公系统微服务项目，基于Spring Cloud Alibaba生态 |
| 父项目 | `oa-microservices`，统一管理所有子模块的依赖版本与构建配置 |
| 子模块 | 1. `gateway`：网关服务<br>2. `OA-2`：员工服务（`oa-emp-service`）<br>（另有独立部署的管理员服务`oa-admin-service`、AI客服服务） |

## 二、项目目录结构
```markdown
backend
├── gateway  # 网关服务模块
│   └── src
│       └── main
│           ├── java
│           │   └── com
│           │       └── oa
│           │           └── gateway
│           │               └── config  # 网关配置类（路由、跨域等）
│           └── resources  # 网关资源配置文件
├── OA-2  # 员工服务模块（oa-emp-service）
│   ├── data  # 本地数据目录（AI向量库持久化等）
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── com
│       │   │       └── oa2
│       │   │           ├── config  # 业务配置类（数据源、MyBatis、ES等）
│       │   │           ├── controller  # 控制器层，处理HTTP请求
│       │   │           ├── dao  # MyBatis Mapper接口层，数据库访问
│       │   │           ├── interceptor  # 拦截器（登录、权限等）
│       │   │           ├── pojo  # 实体类层，映射数据库表
│       │   │           ├── repository  # 扩展数据访问层（如ES、缓存等）
│       │   │           ├── service  # 业务接口层
│       │   │           │   └── impl  # 业务接口实现层
│       │   │           ├── util  # 工具类
│       │   │           ├── vector  # AI向量处理相关
│       │   │           └── vectorstore  # AI向量存储相关
│       │   └── resources
│       │       ├── ai  # AI客服相关资源配置
│       │       ├── mappers  # MyBatis XML映射文件目录（application.yml配置路径）
│       │       ├── static  # 静态资源（Element UI组件库等）
│       │       │   └── element-ui  # Element UI前端组件库
│       │       └── templates  # Thymeleaf模板文件
│       └── test
│           └── java
│               └── com
│                   └── oa2  # 测试类目录
└── pom.xml  # 父项目POM，统一管理依赖版本与构建配置
```

## 三、技术栈与构建工具
| 类型 | 版本/说明 |
|------|-----------|
| 操作系统 | Windows 10 |
| JDK版本 | 21.0.11（LTS版本） |
| 构建工具 | Maven 3.8+（父项目配置Maven编译插件3.11.0，完整支持JDK21编译） |
| 主框架 | Spring Boot 2.7.18（2.7系列最终稳定版，兼容JDK21） |
| 微服务框架 | Spring Cloud 2021.0.3 + Spring Cloud Alibaba 2021.0.1.0 |
| 数据库 | MySQL 8.0.33 |
| ORM框架 | MyBatis 2.3.1 |
| 数据库连接池 | Alibaba Druid 1.2.20 |
| 分页插件 | PageHelper 1.4.6 |
| JSON处理 | Fastjson 1.2.83 |
| 工具库 | Lombok 1.18.30、自定义`liuvei-common` 1.2.0 |
| 微服务组件 | Nacos（服务发现+配置中心） |
| 网关 | Spring Cloud Gateway |
| 搜索引擎 | Elasticsearch（用于AI客服向量检索） |
| 模板引擎 | Thymeleaf |
| 前端组件库 | Element UI |

## 四、分层架构规范
### 4.1 层级职责与约束
| 层级 | 职责说明 | 开发约束与注意事项 |
|------|----------|--------------------|
| **Controller** | 处理HTTP请求与响应，定义API接口 | 1. 不得直接访问数据库，必须通过Service层调用<br>2. 接口路径需符合网关路由规则（员工服务前缀为`/api/v1/employee/`）<br>3. 统一使用`@RestController`注解，返回统一封装的Result对象 |
| **Service** | 实现业务逻辑、事务管理与数据校验 | 1. 必须通过Dao层访问数据库，禁止直接操作MyBatis XML<br>2. 返回DTO数据传输对象，禁止直接返回Pojo实体类<br>3. 接口与实现分离，实现类统一放在当前接口包下的`impl`子包中 |
| **Dao（Mapper）** | 数据库持久化操作 | 1. 继承MyBatis的`Mapper`接口，XML映射文件统一放在`resources/mappers`目录<br>2. 禁止手动拼接SQL，必须使用`#{}`预编译参数防止SQL注入，严禁使用`${}`拼接用户输入<br>3. 复杂关联查询使用`resultMap`映射，配合PageHelper分页插件避免N+1查询问题 |
| **Pojo** | 映射数据库表结构的实体类 | 1. 统一放在`pojo`包下，与数据库表字段一一对应<br>2. 不得直接返回给前端，需转换为DTO对象<br>3. 使用Lombok注解简化代码 |

### 4.2 接口与实现分离规则
所有业务接口必须定义在`service`包下，具体实现类必须放在对应接口下的`impl`子包中，禁止直接在接口包下编写实现类。

## 五、安全与性能规范
### 5.1 输入校验
- Spring Boot 2.7.x版本中JSR-303校验注解位于`javax.validation.constraints`包下，使用`@Valid`配合`@NotBlank`、`@Size`、`@Pattern`等注解进行参数校验，校验失败统一抛出`MethodArgumentNotValidException`，由全局异常处理器统一返回错误信息。
- 禁止在代码中硬编码敏感信息（如数据库密码、API密钥等），敏感信息统一配置在Nacos配置中心。

### 5.2 事务管理
- `@Transactional`注解仅用于Service层方法，禁止在Controller、Dao层使用事务注解。
- 避免在循环中频繁提交事务，大数量批量操作需分批处理，防止事务超时。

### 5.3 性能优化
- 列表查询必须使用PageHelper插件实现分页，禁止手动拼接分页SQL。
- 关联查询优先使用MyBatis的`association`懒加载，避免一次性加载全量关联数据。
- 禁止在循环中调用数据库查询方法，需提前批量查询缓存到本地集合。

## 六、代码风格规范
### 6.1 命名规范
| 类型 | 命名方式 | 示例 |
|------|----------|------|
| 类名 | UpperCamelCase | `UserServiceImpl`、`EmployeeController` |
| 方法/变量 | lowerCamelCase | `saveUser()`、`empName` |
| 常量 | UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS`、`DEFAULT_PAGE_SIZE` |
| 包名 | 全小写，多单词用`.`分隔 | `com.oa2.controller`、`com.oa2.dao` |

### 6.2 注释规范
- 所有类、方法、字段必须添加**中文Javadoc**注释，说明功能、参数、返回值、异常信息。
- 复杂业务逻辑需添加行内注释，说明实现思路与特殊处理逻辑。
- 禁止使用无意义的注释（如`// 修改`、`//  TODO`无具体描述）。

### 6.3 Lombok使用规范
- 使用`@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`注解替代手动编写getter/setter/构造方法，禁止手动编写上述方法。
- 需要自定义序列化的实体类可添加`@Builder`注解简化构建逻辑。

### 6.4 类型后缀规范（阿里巴巴风格）
| 后缀 | 用途说明 | 示例 |
|------|----------|------|
| PO/DO | 数据库实体对象 | `EmployeePo`、`UserDo` |
| DTO | 数据传输对象（前后端交互、层间传递） | `EmployeeDTO`、`LoginDTO` |
| VO | 视图展示对象（返回给前端的格式化数据） | `EmployeeVO`、`PageVO` |
| Query | 查询参数封装对象 | `EmployeeQuery`、`PageQuery` |

## 七、微服务与配置规范
### 7.1 Nacos规范
- 服务发现与配置中心统一地址为`localhost:8848`，namespace为`public`，group为`DEFAULT_GROUP`。
- 所有微服务必须注册到Nacos，配置统一在Nacos配置中心管理，禁止硬编码业务配置到本地`application.yml`。
- 不同环境的配置通过Nacos的namespace隔离，禁止跨环境共用配置。

### 7.2 网关规范
- 网关端口为`8888`，统一处理跨域、路由转发、权限校验，所有外部请求必须经过网关访问后端服务。
- 路由规则统一在`gateway`模块的`application.yml`中配置，当前路由规则：
  - 员工服务：`/api/v1/employee/**` 转发到`lb://oa-emp-service`
  - 管理员服务：`/api/v1/admin/**` 转发到`lb://oa-admin-service`
  - AI客服服务：`/api/v1/ai/**` 当前直连`http://localhost:8083`，后续切换为Nacos注册的`lb://oa-ai-service`
- 网关已配置全局跨域规则，前端无需单独处理跨域。

### 7.3 项目特有配置规范
- 员工服务端口为`8081`，上下文路径为`/api/v1/employee`，启动类为`com.oa2.Oa1Application`。
- 数据源使用Druid连接池，配置统一在`application.yml`中管理，禁止硬编码数据库连接参数。
- MyBatis配置：XML映射文件路径为`classpath:mappers/*.xml`，实体类包路径为`com.oa2.pojo`。
- Elasticsearch配置：默认地址为`localhost:9200`，AI客服相关ES操作统一放在`repository`或`vector`相关包下。
- AI客服配置：默认启用本地向量库检索（零外部依赖），向量持久化路径为`./data/vector-store.json`；如需启用RAG生成式回答，需在Nacos配置中配置DeepSeek的`api-key`。

## 八、日志规范
- 使用`@Slf4j`注解输出日志，禁止使用`System.out.println`。
- 日志级别统一在`application.yml`中配置，员工服务`com.oa2`包下默认debug级别，网关`com.oa.gateway`包下默认debug级别。
- 业务日志需包含请求参数、返回值、异常堆栈等关键信息，便于问题排查。
- 生产环境需关闭debug日志，仅保留info及以上级别日志。

## 九、依赖管理规范
- 所有依赖版本统一由父POM的`dependencyManagement`模块管理，子模块声明依赖时无需指定版本，禁止私自升级依赖版本。
- 默认使用阿里云Maven仓库拉取依赖，特殊依赖需添加仓库配置时需提交评审。

## 十、编码原则总结
| 原则 | 说明 |
|------|------|
| SOLID | 高内聚、低耦合，增强可维护性与可扩展性 |
| DRY | 避免重复代码，提高复用性，公共逻辑统一抽取到工具类或父类 |
| KISS | 保持代码简洁易懂，避免过度设计 |
| YAGNI | 不实现当前不需要的功能，避免提前优化 |
| OWASP | 防范常见安全漏洞，如SQL注入、XSS、越权访问等 |
