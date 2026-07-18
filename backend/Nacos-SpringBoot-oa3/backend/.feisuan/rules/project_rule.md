
# OA办公系统开发规范指南
本规范基于项目实际技术栈、目录结构与业务场景制定，用于保证代码质量、可维护性与微服务架构的规范性，所有开发人员需严格遵守。

---

## 一、项目基础信息
### 1.1 用户工作目录
所有开发工作均在以下工作区进行，禁止在外部路径创建项目文件：
`E:\JavaEE\wuhangligong\Nacos-SpringBoot-oa3\backend`

### 1.2 项目目录结构
项目为Maven聚合工程，父项目为`oa-microservices`，包含3个子模块，完整目录结构如下（省略element-ui前端库过深子目录）：
```
backend
├── pom.xml                  # 父项目聚合POM，统一管理依赖与构建配置
├── gateway                  # 网关服务子模块
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── com
│       │   │       └── oa
│       │   │           └── gateway
│       │   │               └── config      # 网关配置类（路由、跨域等）
│       │   └── resources                  # 网关配置文件
│       └── test
├── OA-2                     # 员工服务子模块（含AI客服能力）
│   ├── data                 # 向量库持久化存储目录
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── com
│       │   │       └── oa2
│       │   │           ├── config         # 服务配置类
│       │   │           ├── controller     # 接口控制层
│       │   │           ├── dao            # MyBatis Mapper接口层
│       │   │           ├── interceptor    # 拦截器（权限、日志等）
│       │   │           ├── pojo           # 实体类、DTO/BO/VO等对象
│       │   │           ├── repository     # 数据访问扩展层
│       │   │           ├── service        # 业务接口定义层
│       │   │           │   └── impl       # 业务接口实现层（当前为小写impl）
│       │   │           ├── util           # 公共工具类
│       │   │           ├── vector         # 向量检索相关逻辑
│       │   │           └── vectorstore    # 向量库持久化相关逻辑
│       │   └── resources
│       │       ├── ai                    # AI客服配置文件
│       │       ├── static                # 前端静态资源（含element-ui库）
│       │       ├── templates             # Thymeleaf模板文件
│       │       └── mappers               # MyBatis XML映射文件
│       └── test
└── OA-7                     # 管理员服务子模块
    └── src
        ├── main
        │   ├── java
        │   │   └── com
        │   │       └── oa7
        │   │           ├── config         # 服务配置类
        │   │           ├── controller     # 接口控制层
        │   │           ├── dao            # MyBatis Mapper接口层
        │   │           ├── interceptor    # 拦截器（权限、日志等）
        │   │           ├── pojo           # 实体类、DTO/BO/VO等对象
        │   │           ├── repository     # 数据访问扩展层
        │   │           ├── service        # 业务接口定义层
        │   │           │   └── Impl       # 业务接口实现层（当前为大写Impl，建议统一为小写impl）
        │   │           └── util           # 公共工具类
        │   └── resources
        │       ├── static                # 前端静态资源（含element-ui库）
        │       ├── templates             # Thymeleaf模板文件
        │       └── mappers               # MyBatis XML映射文件
        └── test
```

### 1.3 代码作者
hp

### 1.4 注释规范
所有代码注释统一使用中文，符合国内团队开发习惯：类、方法、字段需添加中文Javadoc注释，复杂业务逻辑需补充行内注释说明。

---

## 二、技术栈与构建规范
### 2.1 SDK与构造工具
- 开发语言：Java 21.0.11
- 构造工具：Maven 3.x（父项目配置Maven编译插件3.11.0，完整支持JDK21编译）
- 开发环境适配：Windows 10

### 2.2 核心框架版本
| 组件/框架               | 版本          | 说明                     |
|-------------------------|---------------|--------------------------|
| Spring Boot             | 2.7.18        | 业务服务基础框架         |
| Spring Cloud            | 2021.0.3      | 微服务组件基础           |
| Spring Cloud Alibaba    | 2021.0.1.0    | Nacos服务发现/配置中心   |
| Spring Cloud Gateway    | 同Spring Cloud版本 | 微服务网关          |
| MyBatis                 | 2.3.1         | ORM持久层框架            |
| Druid                   | 1.2.20        | 数据库连接池             |
| MySQL Connector         | 8.0.33        | MySQL驱动                |
| Lombok                  | 1.18.30       | 实体类简化工具           |
| Fastjson                | 1.2.83       | JSON解析工具             |
| PageHelper              | 1.4.6         | MyBatis分页插件          |
| Jedis                   | 4.4.3         | Redis客户端              |
| Commons Pool2           | 2.11.1        | 连接池公共工具           |
| Elasticsearch           | 7.x           | 全文检索/向量检索        |

### 2.3 微服务端口与路由配置
| 服务名称         | 端口  | 上下文路径               | 说明                     |
|------------------|-------|--------------------------|--------------------------|
| 网关服务         | 8888  | 无                       | 统一入口，处理路由/跨域  |
| 员工服务（OA-2） | 8081  | /api/v1/employee         | 员工信息、AI客服相关业务 |
| 管理员服务（OA-7）| 8082  | /api/v1/admin            | 系统管理相关业务         |
| AI客服服务       | 8083  | /api/v1/ai               | 当前直连，后续可注册Nacos |

---

## 三、通用开发规则
### 3.1 依赖管理规则
1. 父项目`oa-microservices`为Maven聚合工程，统一管理所有依赖的版本号，子模块（gateway、OA-2、OA-7）继承父POM，子模块中引用依赖无需声明版本；
2. 依赖仓库优先使用阿里云Maven镜像加速下载，无需额外配置本地仓库；
3. 新增第三方依赖需先在父POM的`dependencyManagement`中添加版本管理，再在对应子模块中引用；
4. 团队公共工具依赖`liuvei-common`由专人统一维护版本，子模块直接引用即可。

### 3.2 微服务规范
1. 所有业务服务统一注册到Nacos（地址：localhost:8848），配置统一从Nacos配置中心读取，禁止硬编码配置到代码中；
2. 网关为唯一对外入口，统一处理跨域、路由转发、限流熔断等通用逻辑，各服务无需单独处理跨域；
3. 服务间调用优先通过网关转发，如需直接调用可使用Nacos服务名+LoadBalancer实现负载均衡；
4. 各服务需保持无状态，方便横向扩展；
5. 敏感配置（如数据库密码、第三方接口密钥）生产环境需加密存储，禁止明文提交到代码仓库。

### 3.3 分层架构规范
结合项目实际包结构，分层职责与约束如下：
| 层级        | 包路径示例                  | 职责说明                         | 开发约束与注意事项                                               |
|-------------|-----------------------------|----------------------------------|----------------------------------------------------------------|
| Controller  | com.oa2.controller          | 处理HTTP请求与响应，定义API接口   | 不得直接访问数据库，必须通过Service层调用；接口需添加权限校验注解 |
| Service     | com.oa2.service             | 业务接口定义层                   | 所有业务逻辑必须通过接口定义，禁止直接写实现类                  |
| Service实现 | com.oa2.service.impl        | 实现Service接口定义的业务逻辑     | 实现类需放在接口所在包下的impl子包中（当前OA-2为小写impl、OA-7为大写Impl，建议统一规范为小写impl，符合Java包命名惯例） |
| Dao         | com.oa2.dao                 | MyBatis Mapper接口层，定义数据库操作方法 | 继承BaseMapper或自定义Mapper接口；XML映射文件统一放在resources/mappers目录下；禁止在Dao层编写业务逻辑 |
| Pojo        | com.oa2.pojo                | 实体类、数据传输对象等通用对象     | 数据库实体类不得直接返回给前端，需转换为DTO/BO；包名统一为pojo |
| 拦截器      | com.oa2.interceptor         | 通用逻辑处理层                   | 跨域、登录校验、操作日志等通用逻辑优先在拦截器实现，避免重复代码 |
| 工具类      | com.oa2.util                | 公共工具方法封装                   | 工具方法需静态化，添加单元测试覆盖                               |
| 向量相关    | com.oa2.vector/vectorstore   | AI客服向量库检索相关逻辑           | 向量持久化路径统一为项目data目录下的vector-store.json |

### 3.4 安全与性能规范
1. **输入校验**：使用`javax.validation.constraints`下的校验注解（如`@NotBlank`、`@Size`等，项目基于Spring Boot 2.7，使用javax校验规范）；禁止手动拼接SQL字符串，MyBatis查询优先使用`#{}`预编译参数，禁止使用`${}`直接拼接用户输入，避免SQL注入；
2. **事务管理**：`@Transactional`注解仅用于Service层方法，避免在循环、接口方法中频繁开启事务，影响性能；
3. **敏感信息**：数据库密码、Redis密码、第三方接口密钥等禁止硬编码在代码中，优先从Nacos配置中心读取；
4. **分页规范**：列表查询统一使用PageHelper插件实现分页，禁止手动拼接分页SQL；
5. **缓存规范**：Redis缓存需设置过期时间，避免缓存雪崩、穿透、击穿问题；禁止在缓存中存储敏感明文数据；
6. **AI配置规范**：AI客服相关配置统一在员工服务的`application.yml`的`ai`节点下配置，大模型默认关闭，需要开启生成式回答时再配置API密钥。

### 3.5 代码风格规范
1. **命名规范**：
   | 类型       | 命名方式             | 示例                  |
   |------------|----------------------|-----------------------|
   | 类名       | UpperCamelCase       | UserServiceImpl       |
   | 方法/变量  | lowerCamelCase       | saveUser()            |
   | 常量       | UPPER_SNAKE_CASE     | MAX_LOGIN_ATTEMPTS    |
   | 包名       | 全小写，单词间用.分隔 | com.oa2.service       |
2. **注释规范**：所有类、方法、字段需添加中文Javadoc注释，复杂业务逻辑需补充行内注释说明；
3. **实体类简化**：使用Lombok注解`@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`替代手动编写getter/setter/构造方法；
4. **日志规范**：使用`@Slf4j`注解输出日志，禁止使用`System.out.println`打印日志；日志级别根据场景使用debug/info/warn/error，生产环境关闭debug日志；
5. **通用对象规范**：数据传输用DTO、业务封装用BO、查询参数用Query、视图返回用VO，禁止直接返回Pojo实体给前端。

### 3.6 构建与部署规范
1. 所有模块使用Maven进行构建，打包插件使用spring-boot-maven-plugin，生成的可执行jar包直接通过`java -jar`命令启动；
2. 本地开发环境可直接运行启动类，生产环境需使用打包后的jar包部署，禁止直接使用IDE启动生产服务；
3. 服务启动前需确保Nacos、MySQL、Redis、Elasticsearch等依赖服务已正常启动。

---

## 四、编码原则
1. **SOLID**：高内聚、低耦合，增强可维护性与可扩展性；
2. **DRY**：避免重复代码，提高复用性，公共逻辑优先封装到工具类或拦截器中；
3. **KISS**：保持代码简洁易懂，避免过度设计；
4. **YAGNI**：不实现当前不需要的功能，避免代码冗余；
5. **OWASP**：防范常见安全漏洞，如SQL注入、XSS、敏感信息泄露等；
6. **单一职责**：每个微服务只负责独立的业务域，避免服务功能交叉。
