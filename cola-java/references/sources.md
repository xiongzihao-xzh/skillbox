# COLA 研究基线与证据

在核对 COLA 架构依据、解释模板与样例差异、复用上游代码或审查来源声明时读取本文。本文记录事实及其适用边界；执行要求由 Skill 的开发指导承载。它不是 COLA 官方 Skill，也不是上游全部组件的使用手册。

## 1. 基线、范围与核验方式

- 研究日期：2026-09-06。来源是 `alibaba/COLA`，固定提交为 [`352e1a867538d9cd40e1b11e4028fa652fc97557`][commit]，提交标题 `Add Harness`，时间 `2026-08-31T17:36:38+08:00`。
- 本次只读来源目录为 `/Users/xzh/CODE/COLA`；此绝对路径只是研究记录，不是安装后使用 Skill 的前提。复核时分支为 `master`，HEAD 与上述提交一致，`git status --short` 无输出。正文证据使用 `git show <固定提交>:<相对路径>`，不以未提交文件作为上游事实。
- 本地 remote 配置为 `https://github.com/alibaba/COLA.git`。本轮没有联网验证该提交的远端发布状态。HEAD 没有直接 tag，`git describe` 为 `COLA3.0-283-g352e1a8`；这不是“当前版本为 COLA 3.0”的证据，更不能称为官方最新发布版。
- README 自称 COLA v5；组件和 archetype 父 POM 为 `5.x-SNAPSHOT`；根 POM 是开发聚合用的 `dummy-SNAPSHOT`。概念版本、构建版本和提交标识分开记录。[README:1–17][readme]、[组件 POM:6–9][components-pom]、[archetype POM:6–9][archetypes-pom]、[根 POM:6–27][root-pom]
- 覆盖：标准 Web 六模块模板、craftsman 的代表性写读链路，以及 DTO、exception、domain-starter、catchlog 的相关实现。service 仅用于资料索引；light 已由用户明确排除开发指导，本文只记录其存在与范围边界。[archetype POM:55–59][archetypes-pom]
- 核验层次为“索引 → 文档/POM/源码交叉核对 → 跟踪写读、异常、事务和测试”。本轮完成的是静态来源研究，未在来源仓库执行 Maven、生成项目、连接数据库或运行测试；文中“有测试代码”不表示测试已通过。

为缩短路径，下文使用三个来源根别名；它们全部相对于固定提交的仓库根，链接仍指向完整路径：

| 别名 | 来源相对路径 |
| --- | --- |
| `W/` | `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/` |
| `C/` | `cola-samples/craftsman/` |
| `K/` | `cola-components/` |

## 2. 资料索引与结论分类

| 来源 | 本文采用的证据 | 适用界限 |
| --- | --- | --- |
| [README:25–61、120–125][readme] | COLA 区分架构与组件；以业务为核心、解耦外部依赖；说明 web/service 与 light 的存在 | 概览，不替代模块 POM 与实际代码；:58 仍称两个 archetype，:124 才补充 light |
| [W/pom.xml:12–32][web-parent] 和六个子 POM | 默认六模块结构、显式编译依赖、旧技术配置 | 模板中的 Customer 业务不完整，不能直接用作端到端实现 |
| [C/pom.xml:12–32][craft-parent] 和 `C/craftsman-*/src/main/` | DTO、用例、领域对象、Gateway、数据访问与转换的具体协作 | 样例依赖与包布局有别于 web；不能合并成一张假想的官方唯一依赖图 |
| [K/cola-component-dto/README.md:1–4][dto-readme] 与四个 Response 类 | 调用结果和数据承载协议 | 本身没有 HTTP 状态映射 |
| [K/cola-component-domain-starter/README.md:1–5][domain-readme] 与 `Entity` | 可由 Spring 托管的 prototype 领域实体 | 不支持“COLA Domain 一律不能依赖 Spring”的断言，也不要求每个领域对象都必须托管 |
| [K/cola-component-exception/README.md:1–7][exception-readme] 与 catchlog | 异常分类、服务边界的结果转换 | 不提供自动重试、HTTP 状态或业务事务原子性保证 |
| [LICENSE][license]、[archetype POM:14–19][archetypes-pom] | 上游许可证和复制、修改、分发条件 | 不能把来源链接当成许可证副本，不能由架构研究替整个交付物作许可结论 |

结论按以下边界使用：有上游依据的职责与协作归为 **COLA 规则**；本任务选定六模块、写 Gateway/纯查询 Infra 的默认方案是基于这些证据作出的范围选择。方法级完整业务路径、资源命名、500 行限制、销售订单取消契约属于 **项目约定**。Java 21、Boot 3、Jakarta 迁移及具体工具版本属于 **技术适配**。额外的构造器注入、不可变值对象、映射工具选型等写法，须按项目已确认约定或 **建议实践** 标注，不能伪称所有 COLA 项目唯一写法。

## 3. 六模块编译依赖与职责

### 3.1 本 Skill 采用的 web 模板

下图只表示项目模块 POM 中声明的直接依赖，箭头为“左侧依赖右侧”。这些依赖没有指定其他 scope，属于 Maven 默认的 compile 范围；本轮未运行 `dependency:tree` 或验证传递依赖的最终版本。

```text
start → adapter → app → infrastructure → domain → client
app → client
```

| 模块 | 显式项目依赖 | 定位与源码入口 |
| --- | --- | --- |
| `start` | adapter | 启动和装配；[W/start/pom.xml:15–38][web-start-pom]；[W/start/src/main/java/Application.java:15–19][web-application] 启动 Spring Boot 并扫描业务根包与 `com.alibaba.cola` |
| adapter | app | HTTP 等入口适配；[W/__rootArtifactId__-adapter/pom.xml:15–29][web-adapter-pom]；[CustomerController:15–36][web-controller] 依赖 Client 的 `CustomerServiceI` 类型并委托调用 |
| app | client、infrastructure | API 实现、用例编排和执行器；[W/__rootArtifactId__-app/pom.xml:15–44][web-app-pom]；[CustomerServiceImpl:22–38][web-service] 委托 CmdExe/QryExe |
| infrastructure | domain | 实现领域边界、数据访问和技术整合；[W/__rootArtifactId__-infrastructure/pom.xml:15–37][web-infra-pom]；[CustomerGatewayImpl:12–20][web-gateway-impl]、[CustomerMapper:8–11][web-mapper] |
| domain | client | 核心业务和对外能力接口；[W/__rootArtifactId__-domain/pom.xml:15–30][web-domain-pom]；[domain/package-info.java:4–9][web-domain-doc]、[Customer:12–40][web-customer]、[CustomerGateway:8–9][web-gateway] |
| client | 无其他业务模块 | 对外 Java API、Command/Query、数据 DTO；[W/__rootArtifactId__-client/pom.xml:15–24][web-client-pom]；[CustomerServiceI:12–16][web-api]、[CustomerAddCmd:9–12][web-cmd]、[CustomerDTO:10–19][web-dto] |

由此需要区分两类限制：POM 允许 App 编译时访问 Infra；业务代码仍按用例分工。Controller 调用应用 API、写用例经 Domain Gateway、纯查询访问 Infra 是本 Skill 选择的边界，不能把 Maven 的传递可见性解释为 Controller 可以直接用 Mapper，也不能把“App 依赖 Infra”误报为这个方案的违规。

### 3.2 craftsman 的差异

craftsman 的 `start → adapter → app → infrastructure → domain` 与 web 类似，但 App 和 Infra 均直接依赖 client；Domain **不**依赖 craftsman-client，而依赖 COLA dto、exception、domain-starter，以及 slf4j、fastjson。[C/start/pom.xml:16–35][craft-start-pom]、[adapter POM:15–29][craft-adapter-pom]、[app POM:16–44][craft-app-pom]、[infrastructure POM:15–42][craft-infra-pom]、[domain POM:15–37][craft-domain-pom]

`C/craftsman-domain/.../domain/package-info.java:3` 说领域核心应纯净且不依赖其他层，指向的是该样例的层间边界；不能据此把标准 web 的 Domain → Client 边删除，或把实际使用的框架/日志/序列化依赖说成不存在。[craftsman 领域说明][craft-domain-doc]

### 3.3 可复用职责与命名惯例

| 职责 | 核验到的名称和位置 | 不应推广成的强制结论 |
| --- | --- | --- |
| 对外应用 API / 实现 | Client `api/CustomerServiceI`；App `customer/CustomerServiceImpl`；craftsman 为 `api/MetricsServiceI` 与 App `service/MetricsServiceImpl`。[web API][web-api]、[web 实现][web-service]、[craftsman API][craft-api]、[craftsman 实现][craft-service] | 所有接口必须以 `I` 结尾、App 必须存在统一 `service` 包 |
| 写/读执行逻辑 | web `customer/executor/CustomerAddCmdExe`、`executor/query/CustomerListByNameQryExe`；craftsman `command/ATAMetricAddCmdExe`、`command/query/ATAMetricQryExe`。[web 写][web-write-stub]、[web 读][web-read-stub]、[craftsman 写][craft-write]、[craftsman 读][craft-read] | 所有业务必须逐字复用同一包名；每个微小用例都必须机械拆出执行器 |
| 边界 DTO | web `dto/data/CustomerDTO`；craftsman `dto/clientobject/ATAMetricCO`。[CustomerDTO][web-dto]、[ATAMetricCO][craft-co] | `DTO` / `CO` 是两个必须同时存在的层，或 DTO 是领域实体 |
| 领域对象与值 | web `Customer`、`CompanyType`；craftsman `MetricItem`、`ATAMetricItem`。领域承载可执行行为，如 `Customer.checkConflict()`、`ATAMetricItem.calculateScore()`。[Customer:26–38][web-customer]、[CompanyType:12–16][web-company-type]、[ATAMetricItem:50–87][craft-item] | Entity、Value Object 都必须有特定后缀；空的 [Credit][web-credit] 是完善值对象范例。标准 web 的 `CreditChecker` 也只是带职责注释的空壳，[6–9 行][web-domain-service] 不足以定义领域服务的完整实现 |
| 领域能力接口 / 技术实现 | Domain `gateway/MetricGateway`；Infra `gatewayimpl/MetricGatewayImpl`。[接口:16–22][craft-gateway]、[实现:37–66][craft-gateway-impl] | Gateway 就是 Mapper，或所有查询都必须先组装领域实体 |
| 存储对象 / 转换 | Infra `gatewayimpl/database/dataobject/MetricDO`、`convertor/MetricConvertor`；App 另有 CO → 领域对象的转换。[MetricDO:4–21][craft-do]、[MetricConvertor:12–20][craft-convertor]、[写执行器:27–29][craft-write] | `Convertor` 拼写是唯一合法命名、转换类只能位于一层，或结构映射应计算业务规则 |

Spring 的实际边界也须如实陈述：web `Customer` 用 `com.alibaba.cola.domain.Entity`；该注解自身带 `@Component` 和 prototype `@Scope`。craftsman 的 `MetricItem` 同样使用此注解，同时使用 Lombok 和 fastjson。由此不能得出“所有 COLA Domain 都完全框架无关”，也不能反向强制新领域对象采用这些依赖。[web Customer:6–12][web-customer]、[K/.../Entity.java:15–20][domain-entity]、[craftsman MetricItem:3–19][craft-base-item]

## 4. 完整写链路：craftsman 添加 ATA 指标

这里的“完整”指已从 HTTP 入口追踪到实际 SQL 及返回/异常出口；没有把模板 stub 或未运行代码称为已验证的数据库用例。源码保留的指标业务只是上游样例，不是销售订单模型。

```text
POST /metrics/ata
  → MetricsController
  → Client MetricsServiceI.addATAMetric(ATAMetricAddCmd)
  → App MetricsServiceImpl → ATAMetricAddCmdExe
  → 构造 ATAMetricItem 并建立所属指标/人员关系
  → Domain MetricGateway.save(MetricItem)
  → Infra MetricGatewayImpl → MetricConvertor → MetricDO
  → MetricMapper.create → mybatis/MetricMapper.xml → metric 表
  ← Response.buildSuccess() → Controller 返回
```

| 步骤 | 来源相对路径、类/方法、行号与观察 |
| --- | --- |
| HTTP / API | `C/craftsman-adapter/src/main/java/com/alibaba/craftsman/web/MetricsController.java:25–27` 接收 `@RequestBody ATAMetricAddCmd` 后调用服务；`C/craftsman-client/.../api/MetricsServiceI.java:15` 定义写契约。[入口][craft-controller]、[API][craft-api] |
| 请求数据 | `C/craftsman-client/.../dto/ATAMetricAddCmd.java:16–18` 携带 `ATAMetricCO`，CO 字段和 `@NotEmpty` 在 `dto/clientobject/ATAMetricCO.java:15–22`；`ATAMetricAddCmd` 使用旧 `javax.validation`。[Cmd][craft-cmd]、[CO][craft-co] |
| App 编排 | `C/craftsman-app/.../service/MetricsServiceImpl.java:21–26,45–47` 为 `@Service @CatchAndLog`，委托 `ATAMetricAddCmdExe`；后者 `:26–31` 用 `BeanUtils.copyProperties` 复制数据、构造所属 `ATAMetric/InfluenceMetric/UserProfile` 关系，再调用 Gateway。[服务][craft-service]、[执行器][craft-write] |
| Domain | `C/craftsman-domain/.../domain/metrics/MetricItem.java:33–35` 的 `setSubMetric` 同时设置 metricOwner；`:41–44` 提供 JSON 序列化；`ATAMetricItem.java:50–87` 的评分行为属于领域，但本条添加链路没有调用 `calculateScore()`。不可声称保存前已计算或验证所有指标规则。[MetricItem][craft-base-item]、[ATAMetricItem][craft-item] |
| Gateway | `C/craftsman-domain/.../domain/gateway/MetricGateway.java:16–17` 接收领域 `MetricItem`；`C/craftsman-infrastructure/.../gatewayimpl/MetricGatewayImpl.java:54–58` 将其转为 DO 并保存。[接口][craft-gateway]、[实现][craft-gateway-impl] |
| 转换 / DO | `C/craftsman-infrastructure/.../convertor/MetricConvertor.java:12–20` 取 owner、主/子指标代码及条目 JSON，creator/modifier 被硬编码为 `test`；`gatewayimpl/database/dataobject/MetricDO.java:4–21` 承载表字段。[转换][craft-convertor]、[DO][craft-do]。硬编码审计用户不能推广到新项目 |
| 数据访问 | `C/craftsman-infrastructure/.../gatewayimpl/database/MetricMapper.java:9–12` 为 `@Mapper`；`src/main/resources/mybatis/MetricMapper.xml:25–40` INSERT 并回填生成 id；`mybatis-config.xml:8–17` 配置下划线转驼峰、DO 别名包和 XML 绑定；`TableCreationDDL.sql:1–14` 定义 `metric` 表。[Mapper][craft-mapper]、[SQL][craft-sql]、[MyBatis 配置][craft-mybatis-config]、[DDL][craft-ddl] |
| 保存后 / 返回 | `MetricGatewayImpl.java:60–65` 构造事件对象并调用发布器，但 `C/craftsman-infrastructure/.../common/event/DomainEventPublisher.java:16–18` 只有被注释的 `eventBus.fire`，是空实现。执行器 `:31` 返回 success。**没有实际事件发送、可靠消息或后续评分完成的证据。** [Gateway 实现][craft-gateway-impl]、[发布器][craft-event-publisher]、[执行器][craft-write] |

本 Controller 方法没有 `@Valid`；已读执行器也未显式调用 Validator，因而 DTO 上存在校验注解不能证明本条 HTTP 请求已触发嵌套校验。添加用例没有显式业务拒绝分支；其异常、HTTP 与事务限制见第 6 节。[Controller:25–27][craft-controller]、[执行器:26–31][craft-write]

## 5. 完整查询链路：craftsman 按人员查询 ATA 指标

```text
GET /metrics/ata?ownerId=...
  → MetricsController 构造 ATAMetricQry
  → Client MetricsServiceI.listATAMetrics
  → App MetricsServiceImpl → ATAMetricQryExe
  → Infra MetricMapper.listBySubMetric(ownerId, ATA代码)
  → mybatis/MetricMapper.xml SELECT → List<MetricDO>
  → App 从 metricItem JSON 组装 ATAMetricCO 并补 ownerId
  ← MultiResponse<ATAMetricCO>
```

`C/craftsman-adapter/.../web/MetricsController.java:18–22` 将查询参数放入 `ATAMetricQry`；Client API 在 `MetricsServiceI.java:23`，Query 定义在 `dto/ATAMetricQry.java:5–7`；App 服务在 `MetricsServiceImpl.java:85–87` 委托查询执行器。[Controller][craft-controller]、[API][craft-api]、[Query][craft-query]、[Service][craft-service]

`C/craftsman-app/.../command/query/ATAMetricQryExe.java:7–8,19–30` 直接使用 Infra 的 `MetricMapper` 和 `MetricDO`，由 mapper 查询后用 fastjson 组装 CO。它绕过领域行为和 Domain Gateway，**仍在 :4,23 引用 Domain 枚举 `SubMetricType`**，并非完全没有 Domain 编译依赖。SQL 位于 `C/craftsman-infrastructure/src/main/resources/mybatis/MetricMapper.xml:56–59`，按人员、子指标和未删除状态过滤。[查询执行器][craft-read]、[Mapper:18][craft-mapper]、[SQL][craft-sql]

这条实现支持本 Skill 的“纯查询可由 App 直接读 Infra”选择，但没有演示分页、排序白名单、授权过滤、查询缓存或查询中领域行为的替代设计。这些需求应由当前用例明确，不能由这个 List 查询推断已经具备。另一条 `UserProfileListQryExe:18–29` 也直接读取 Infra 并做 DO → CO 转换，可作交叉证据。[人员查询执行器][craft-profile-query]

## 6. 异常、HTTP 结果与事务边界

### 6.1 异常转换是服务级 AOP，HTTP 映射另有职责

`BaseException` 继承 `RuntimeException` 并带 `errCode`；`BizException` 和 `SysException` 的默认错误码分别为 `BIZ_ERROR`、`SYS_ERROR`。[BaseException:8–29][base-exception]、[BizException:8–19][biz-exception]、[SysException:8–19][sys-exception]

`K/cola-component-catchlog-starter/.../CatchLogAspect.java:29–48` 围绕带类级 `@CatchAndLog` 的 public 方法执行，捕获 `Throwable` 并返回转换结果；`:55–70` 保留已知业务/系统错误码，未知异常变为 `UNKNOWN_ERROR`。`DefaultResponseHandler.java:19–46` 对识别的 COLA Response 类型创建实例并填写失败字段；当前默认实现不设置 HTTP 状态、不执行重试，未知异常消息直接来自 `e.getMessage()`。[切面][catchlog-aspect]、[默认响应处理器][response-handler]

web 模板 `CustomerAddCmdExe.java:17–23` 会针对 `ConflictCompanyName` 抛 `BizException`；服务类 `CustomerServiceImpl.java:22–38` 使用 `@CatchAndLog`，其测试断言返回的业务错误码。这个样例证明一条业务异常 → 失败 Response 的设计路径，不能证明 HTTP 409/其他状态映射，也不能证明保存失败的回滚行为。[web 执行器][web-write-stub]、[web 服务][web-service]、[web 服务测试:52–64][web-service-test]

DTO 组件的作用是应用调用协议。四类都在 `com.alibaba.cola.dto`，没有 HTTP status 属性或状态映射：

| 类型 | 已核查字段/访问器 |
| --- | --- |
| [Response:8–58][response] | `success`、`errCode`、`errMessage`，成功/失败工厂 |
| [SingleResponse:8–40][single-response] | 继承 Response；单条 `T data` |
| [MultiResponse:14–60][multi-response] | 继承 Response；集合 data，空值 getter 返回空列表；还有 `isEmpty/isNotEmpty` |
| [PageResponse:14–122][page-response] | 继承 Response；`totalCount/pageSize/pageIndex/data`，页码/页大小至少为 1；还有 `getTotalPages/isEmpty/isNotEmpty` |

字段表不是经过实测的 JSON schema；额外 getter、序列化配置和实际类型均可能影响输出。默认示例“有响应体时保留 COLA 包装，HTTP 状态反映结果，204 不含响应体”是已确认项目契约，不能宣称由 COLA 类自动实现。HTTP 无响应体约束来自 [RFC 9110 §15.3.5](https://www.rfc-editor.org/rfc/rfc9110.html#section-15.3.5)；Framework 支持将响应对象与状态一起返回，见 [Spring Framework 6.0.x ResponseEntity](https://docs.spring.io/spring-framework/docs/6.0.x/javadoc-api/org/springframework/http/ResponseEntity.html)。

### 6.2 没有从样例证明生产事务保障

在固定提交的 `cola-archetypes/cola-archetype-web/`、`cola-samples/craftsman/` 和 `cola-components/cola-component-catchlog-starter/` 范围忽略大小写检索 `Transactional|TransactionTemplate|TransactionManager|rollback|commit\(|setAutoCommit|EnableTransactionManagement|<tx:`，命中仅为测试 JDBC commit、测试配置和旧 mock 清单中的事务 Bean 名；所追踪生产服务、执行器、Gateway、发布器中没有显式用例级事务声明。

正向证据：`MetricGatewayImpl.save:54–65` 顺序执行存储和空发布器；`MybatisTest.java:30–41` 的手动 `sqlSession.commit()` 位于已注释 `@Test` 的方法。测试清单中出现事务管理器类型，不等于当前业务用例启用了事务。[Gateway 实现][craft-gateway-impl]、[发布器][craft-event-publisher]、[MyBatis 测试][craft-mybatis-test]

因此本研究结论是：**没有用例级回滚、多次写入原子性或数据库与消息一致性的已验证证据**，而不是“数据库完全没有事务”。采用数据库的新用例必须单独明确事务边界、代理实际生效位置、失败传播与回滚检查；不能从只返回失败 Response、存在 Spring 事务依赖、或内存示例测试推出数据库原子性。

尤其需区分“异常被 AOP 转换成失败返回值”和“异常传播到事务拦截器”：该差异是否影响回滚要以实际装配和测试为准。`CatchLogAspect` 虽有 `@Order(1)`，本基线没有完整用例事务配置和回滚测试，不能仅凭此注解断言一定回滚或一定提交。[切面:23,40–48][catchlog-aspect]

## 7. 测试来源与实际覆盖界限

| 来源相对路径与位置 | 存在的检查 | 不能据此声称 |
| --- | --- | --- |
| [C/craftsman-domain/src/test/java/com/alibaba/craftsman/domain/ATAMetricTest.java:18–62][craft-domain-test] | JUnit 4 的评分值、JSON 转换与指标合计断言 | 本轮已运行通过，或已验证 HTTP、数据库、所有非法输入 |
| [W/start/src/test/java/test/CustomerServiceTest.java:24–64][web-service-test] | `@SpringBootTest` + JUnit 4；调用 Java API，断言 success/冲突错误码 | HTTP 状态与最终 JSON、Customer 真正保存、事务回滚 |
| [W/__rootArtifactId__-domain/src/test/java/domain/CustomerEntityTest.java:7–11][web-domain-test] | 只有提示打印，无 `@Test`/断言 | 已有可执行的完整领域规则测试 |
| [C/craftsman-infrastructure/src/test/java/com/alibaba/craftsman/gatewayimpl/MybatisTest.java:19–41][craft-mybatis-test] | 获取 SqlSession、示例 INSERT/commit 代码 | 插入测试默认启用；`:30` 的 `@Test` 已注释 |
| [C/start/src/test/java/com/alibaba/craftsman/gatewayimpl/MetricTunnelTest.java:18–37][craft-tunnel-test] | CRUD 代码含断言 | 默认会被 JUnit 执行；方法未标注 `@Test`，类也没有测试运行配置 |
| [C/craftsman-app/src/test/java/com/alibaba/craftsman/app/ContextInterceptorTest.java:15–31][craft-app-test] | 创建命令数据；真正的拦截调用已注释 | 应用协作已有有效覆盖 |
| [K/cola-component-catchlog-starter/src/test/java/com/alibaba/cola/catchlog/test/CatchLogTest.java:12–66][catchlog-test] | Spring 上下文中调用多种成功/异常分支 | 已断言 HTTP 或事务语义；所读测试只调用方法，没有结果断言 |
| [.github/workflows/ci.yaml:12–27][fast-ci]、[ci_by_multiply_java_versions.yaml:18–33][strong-ci] | 配置 Java 17 的 Maven 构建及 17/21/22 的另一工作流入口 | 本地/远端 CI 当前通过，或旧 web 模板已经是 Java 21 / Boot 3 示例 |

根 POM 聚合 components、archetypes 和 craftsman，charge 被注释排除；“执行根构建”也不等于构建全部样例。[根 POM:18–26][root-pom]

本轮未运行上述测试，也未以它们代替新销售订单示例的领域、应用、映射、HTTP、序列化和取消并发测试。真实数据库事务仍在本任务实测范围之外；这项限制由示例采用内存实现决定，必须保留在交付验证说明中。

## 8. 文档、模板与实现差异清单

| 差异 | 采用方式 |
| --- | --- |
| README 的 archetype 概览列 web/service，而版本说明和父 POM 包含 light。[README:58–61,122–125][readme]、[父 POM:55–59][archetypes-pom] | 以实际目录/POM 识别变体；本 Skill 只指导已选六模块，发现 light 时说明范围 |
| README 5.0 写 JDK 17 / Boot 3；web 模板实际 source=1.8、Boot 2.7.2、MyBatis starter 2.2.2，craftsman Boot 2.7.5。[web POM:12–22][web-parent]、[craftsman POM:12–22][craft-parent] | 提炼架构后另做 Java 21 / Boot 3 适配，不能复制这些旧 POM 作为目标构建 |
| components 父 POM 已是 Java 17 / Boot 3.3.0；旧样例仍引用 `5.x-SNAPSHOT`。[组件 POM:68–74][components-pom]、[web POM:19–22][web-parent] | 同仓版本并不统一；最终示例的已验证依赖版本在构建与验证文档中记录，本文不猜最终解析结果 |
| web 写执行器只检查名字冲突；查询硬编码 Frank；Gateway 读取 DO 后直接返回 null。[CustomerAddCmdExe:17–23][web-write-stub]、[CustomerListByNameQryExe:16–21][web-read-stub]、[CustomerGatewayImpl:17–20][web-gateway-impl] | 作为角色示意，完整持久化链路使用 craftsman 交叉核验；新示例必须补齐自身实现 |
| web 与 craftsman 的 Domain → Client 边、Infra → Client 边不同。[web Domain POM][web-domain-pom]、[craftsman Domain POM][craft-domain-pom]、[craftsman Infra POM][craft-infra-pom] | 六模块图按选定 web 模板绘制，运行调用链单独使用已核查样例 |
| `CatchAndLog` 注解可标 METHOD/TYPE，但当前切点是 `@within(CatchAndLog)`。[注解:15–17][catchlog-annotation]、[切面:29][catchlog-aspect] | 本次只确认类级使用的链路，不承诺仅在单个方法上添加该注解一定被这个切点覆盖 |
| catchlog README 的示意 ResponseHandler 带 Mtop 分支，当前 DefaultResponseHandler 只实现所识别 COLA Response 分支。[README:12–27][catchlog-readme]、[DefaultResponseHandler:19–46][response-handler] | 兼容承诺跟实际处理器和测试走，不能仅凭 README 的示意代码扩展支持范围 |
| exception README 把系统/未知异常列为可 Retry，但所读异常类和切面没有重试执行逻辑。[README:4–7][exception-readme]、[切面:51–70][catchlog-aspect] | 当作异常分类意图；具体重试条件和幂等性需由真实操作确定 |
| 所追踪 Gateway 调用的事件发布器为空，事务/HTTP/数据库测试证据有限。[发布器][craft-event-publisher]、第 6–7 节 | 明确未实现、未启用、未实测的区别，不把方法名或测试文件存在当成功证据 |

## 9. 许可证与归属

固定源树中按 `LICENSE/NOTICE/COPYING/COPYRIGHT` 名称索引，仅发现根 `LICENSE`；它的标题为 GNU Lesser General Public License 2.1，README 徽章和 archetype/组件 POM 也声明 LGPL 2.1。许可证附录包含给新库使用的示例声明，不能把示例中的占位版权人或“or later”措辞擅自写成整个 COLA 项目的额外授权。[LICENSE:1–7,460–489][license]、[README:5][readme]、[archetype POM:14–19][archetypes-pom]、[components POM:14–19][components-pom]

复用上游代码时，先区分复制/修改源码与仅通过依赖使用库，按实际分发物核对：

1. 对复制分发的受覆盖源码，保留已有版权、许可、无担保声明，随分发附完整许可文本。只放 GitHub 链接不替代许可副本。[LICENSE §1:150–156][license]
2. 对修改的受覆盖文件，显著注明修改和日期，并遵循 §2 对衍生作品的条件；保留既有作者归属信息，不把上游代码改标为本项目原创。[LICENSE §2:162–173][license]。例如所读 `MetricConvertor` 和 `ATAMetricItem` 源码各自保留作者信息。[转换类:7–10][craft-convertor]、[指标类:8–16][craft-item]
3. 分发包含库的对象码/可执行物时，还要核对相应源码、用户修改/调试权利以及适用的重新链接/共享库条件；源码引用笔记不能替代这一步。[LICENSE §4–6:227–316][license]
4. 本文引用和概括架构事实，没有复制整段上游业务实现；是否有其他交付文件复用了源码，应按实际差异清单审查。独立编写的代码、单纯使用依赖的代码及包含库的分发物适用情形不同，不据本文给整个 skillbox 笼统再许可。[LICENSE §2:190–209、§5:240–250][license]

`LICENSE:4` 的 Free Software Foundation 版权针对许可文本本身，不应当作 COLA 源码版权人；许可文本应原样保留。[LICENSE:4–7][license]

## 10. 本文完成标准与剩余核验

已完成：固定提交与工作区复核、资料索引、标准 web 与 craftsman 显式模块依赖区分、代表写入/查询从 API 到 SQL 的静态跟踪、转换和领域行为定位、异常/响应转换、显式事务证据检索、代表测试的启用状态检查，以及上游许可条文定位。每项结论以固定提交文件定位或清楚标注的检索范围为依据。

未由本文证明：远端发布真实性/最新性、上游工程当前可编译或测试通过、所有 COLA 组件兼容性、全部业务路径正确、真实数据库事务/消息一致性、最终示例依赖版本、HTTP JSON 序列化及本 Skill 行为验收。实现阶段各自提供实际构建和测试证据；不得将本文的静态来源结论改写为运行成功声明。

<!-- 所有上游链接固定到同一提交；这些定义避免将可变 master 链接误当证据。 -->
[commit]: https://github.com/alibaba/COLA/commit/352e1a867538d9cd40e1b11e4028fa652fc97557
[readme]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/README.md
[root-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/pom.xml#L6-L27
[archetypes-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/pom.xml#L6-L64
[components-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/pom.xml#L6-L74
[web-parent]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/pom.xml#L12-L32
[web-start-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/start/pom.xml#L15-L38
[web-adapter-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/pom.xml#L15-L29
[web-app-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-app/pom.xml#L15-L44
[web-infra-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/pom.xml#L15-L37
[web-domain-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/pom.xml#L15-L30
[web-client-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-client/pom.xml#L15-L24
[web-application]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/start/src/main/java/Application.java#L15-L19
[web-api]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-client/src/main/java/api/CustomerServiceI.java#L12-L16
[web-cmd]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-client/src/main/java/dto/CustomerAddCmd.java#L9-L12
[web-dto]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-client/src/main/java/dto/data/CustomerDTO.java#L8-L19
[web-controller]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-adapter/src/main/java/web/CustomerController.java#L15-L36
[web-service]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-app/src/main/java/customer/CustomerServiceImpl.java#L19-L38
[web-write-stub]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-app/src/main/java/customer/executor/CustomerAddCmdExe.java#L14-L24
[web-read-stub]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-app/src/main/java/customer/executor/query/CustomerListByNameQryExe.java#L14-L22
[web-domain-doc]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/package-info.java#L4-L9
[web-customer]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/customer/Customer.java#L6-L40
[web-company-type]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/customer/CompanyType.java#L12-L16
[web-credit]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/customer/Credit.java#L9-L13
[web-domain-service]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/customer/domainservice/CreditChecker.java#L6-L9
[web-gateway]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/main/java/domain/customer/gateway/CustomerGateway.java#L8-L9
[web-gateway-impl]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/customer/CustomerGatewayImpl.java#L12-L20
[web-mapper]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/java/customer/CustomerMapper.java#L8-L11
[web-service-test]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/start/src/test/java/test/CustomerServiceTest.java#L24-L64
[web-domain-test]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-domain/src/test/java/domain/CustomerEntityTest.java#L7-L11
[craft-parent]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/pom.xml#L12-L32
[craft-start-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/start/pom.xml#L16-L35
[craft-adapter-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-adapter/pom.xml#L15-L29
[craft-app-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-app/pom.xml#L16-L44
[craft-infra-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-infrastructure/pom.xml#L15-L42
[craft-domain-pom]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-domain/pom.xml#L15-L37
[craft-domain-doc]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-domain/src/main/java/com/alibaba/craftsman/domain/package-info.java#L1-L7
[craft-controller]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-adapter/src/main/java/com/alibaba/craftsman/web/MetricsController.java#L12-L28
[craft-api]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-client/src/main/java/com/alibaba/craftsman/api/MetricsServiceI.java#L14-L24
[craft-cmd]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-client/src/main/java/com/alibaba/craftsman/dto/ATAMetricAddCmd.java#L6-L19
[craft-co]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-client/src/main/java/com/alibaba/craftsman/dto/clientobject/ATAMetricCO.java#L14-L23
[craft-query]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-client/src/main/java/com/alibaba/craftsman/dto/ATAMetricQry.java#L5-L7
[craft-service]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-app/src/main/java/com/alibaba/craftsman/service/MetricsServiceImpl.java#L21-L88
[craft-write]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-app/src/main/java/com/alibaba/craftsman/command/ATAMetricAddCmdExe.java#L20-L32
[craft-read]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-app/src/main/java/com/alibaba/craftsman/command/query/ATAMetricQryExe.java#L3-L30
[craft-profile-query]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-app/src/main/java/com/alibaba/craftsman/command/query/UserProfileListQryExe.java#L3-L29
[craft-base-item]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-domain/src/main/java/com/alibaba/craftsman/domain/metrics/MetricItem.java#L3-L44
[craft-item]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-domain/src/main/java/com/alibaba/craftsman/domain/metrics/techinfluence/ATAMetricItem.java#L8-L87
[craft-gateway]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-domain/src/main/java/com/alibaba/craftsman/domain/gateway/MetricGateway.java#L16-L22
[craft-gateway-impl]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-infrastructure/src/main/java/com/alibaba/craftsman/gatewayimpl/MetricGatewayImpl.java#L37-L66
[craft-convertor]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-infrastructure/src/main/java/com/alibaba/craftsman/convertor/MetricConvertor.java#L7-L20
[craft-do]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-infrastructure/src/main/java/com/alibaba/craftsman/gatewayimpl/database/dataobject/MetricDO.java#L4-L21
[craft-mapper]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-infrastructure/src/main/java/com/alibaba/craftsman/gatewayimpl/database/MetricMapper.java#L9-L22
[craft-sql]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-infrastructure/src/main/resources/mybatis/MetricMapper.xml#L19-L63
[craft-mybatis-config]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-infrastructure/src/main/resources/mybatis-config.xml#L8-L18
[craft-ddl]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-infrastructure/src/main/resources/TableCreationDDL.sql#L1-L14
[craft-event-publisher]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-infrastructure/src/main/java/com/alibaba/craftsman/common/event/DomainEventPublisher.java#L5-L18
[craft-domain-test]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-domain/src/test/java/com/alibaba/craftsman/domain/ATAMetricTest.java#L18-L62
[craft-mybatis-test]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-infrastructure/src/test/java/com/alibaba/craftsman/gatewayimpl/MybatisTest.java#L19-L41
[craft-tunnel-test]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/start/src/test/java/com/alibaba/craftsman/gatewayimpl/MetricTunnelTest.java#L18-L37
[craft-app-test]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-samples/craftsman/craftsman-app/src/test/java/com/alibaba/craftsman/app/ContextInterceptorTest.java#L15-L31
[dto-readme]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-dto/README.md#L1-L4
[response]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-dto/src/main/java/com/alibaba/cola/dto/Response.java#L8-L58
[single-response]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-dto/src/main/java/com/alibaba/cola/dto/SingleResponse.java#L8-L40
[multi-response]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-dto/src/main/java/com/alibaba/cola/dto/MultiResponse.java#L14-L60
[page-response]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-dto/src/main/java/com/alibaba/cola/dto/PageResponse.java#L14-L122
[domain-readme]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-domain-starter/README.md#L1-L5
[domain-entity]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-domain-starter/src/main/java/com/alibaba/cola/domain/Entity.java#L15-L20
[exception-readme]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-exception/README.md#L1-L7
[base-exception]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-exception/src/main/java/com/alibaba/cola/exception/BaseException.java#L8-L29
[biz-exception]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-exception/src/main/java/com/alibaba/cola/exception/BizException.java#L8-L19
[sys-exception]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-exception/src/main/java/com/alibaba/cola/exception/SysException.java#L8-L19
[catchlog-readme]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-catchlog-starter/README.md#L1-L36
[catchlog-annotation]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-catchlog-starter/src/main/java/com/alibaba/cola/catchlog/CatchAndLog.java#L15-L17
[catchlog-aspect]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-catchlog-starter/src/main/java/com/alibaba/cola/catchlog/CatchLogAspect.java#L23-L70
[response-handler]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-catchlog-starter/src/main/java/com/alibaba/cola/catchlog/DefaultResponseHandler.java#L19-L46
[catchlog-test]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/cola-components/cola-component-catchlog-starter/src/test/java/com/alibaba/cola/catchlog/test/CatchLogTest.java#L12-L66
[fast-ci]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/.github/workflows/ci.yaml#L12-L27
[strong-ci]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/.github/workflows/ci_by_multiply_java_versions.yaml#L18-L33
[license]: https://github.com/alibaba/COLA/blob/352e1a867538d9cd40e1b11e4028fa652fc97557/LICENSE
