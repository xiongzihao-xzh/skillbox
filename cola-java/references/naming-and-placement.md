# 命名和落位

下面使用示例根包 `com.example.cola`；新项目替换为实际组织包名。后缀与源模板对应关系见 [来源命名表](sources.md)，表中业务名称是示例选择，不是所有项目固定名称。

| 新增的职责 | 示例模块与包（根包之后） | 示例名称 | 依赖与调用方 |
| --- | --- | --- | --- |
| 对外应用契约 | client / api | OrderServiceI | 使用 Client 输入输出；由 Adapter 调用、App 实现 |
| 写入输入 | client / dto | OrderCreateCmd、OrderCancelCmd | 继承 COLA Command，声明输入形态，不承载规则 |
| 查询输入 | client / dto | OrderGetQry | 继承 COLA Query，由查询执行器消费 |
| 输出和数据传输 | client / dto.data | OrderDTO、CancellationDTO | 不暴露领域实体、DO 或框架存储接口 |
| 服务实现 | app / order | OrderServiceImpl | 委派明确用例给执行器；不积累所有业务分支 |
| 写执行器 | app / order.executor | OrderCreateCmdExe、OrderCancelCmdExe | 组织受控领域构造和 Gateway 协作 |
| 查询执行器 | app / order.executor.query | OrderGetQryExe | 纯查询可读 Infra，转换为 Client DTO |
| 业务状态与不变量 | domain / domain.order | Order、OrderLine、Cancellation | 自身行为保证有效状态，由 App 使用 |
| 存储业务边界 | domain / domain.order.gateway | OrderGateway | 使用 Domain 类型；Infrastructure 实现 |
| 边界实现和数据访问 | infrastructure / order | OrderGatewayImpl、OrderMapper | 连接领域与存储；Mapper 不负责业务规则 |
| 存储快照 | infrastructure / order | OrderDO | 数据载体，仅在 Infra 与合法查询路径使用 |
| Domain ↔ DO 转换 | infrastructure / order | OrderConvertor | 两端类型在本层都可见，恢复时调用领域工厂 |
| 输入/Domain/DO → DTO 转换 | app / order | OrderDtoConvertor | 仅做结构映射，领域工厂与行为保留规则 |
| HTTP 入口及错误映射 | adapter / web | OrderController、ApiExceptionHandler | 调应用 API，处理 HTTP，不接触 Mapper |
| 启动与组合测试 | start / 根包 | Application | 装配组件、时钟和实际 HTTP 测试 |

**COLA 来源惯例**：Web 模板使用 `ServiceI`、`ServiceImpl`、`Cmd`、`Qry`、`CmdExe`、`QryExe`、`Gateway`、`GatewayImpl`、`Mapper`、`DO` 和 `dto.data.*DTO`。craftsman 使用 `dto.clientobject.*CO` 和 `convertor`；并不存在经本基线证明的唯一 Converter/Assembler 后缀。示例选择 DTO 和 Convertor；已有项目遵循所选基线中一致的命名。

**项目选择**：`Create`、`Cancel` 是用例动词，`OrderLine` 是值对象语义，不机械添加 VO 后缀。不可变 record 适合本例的数据和值类型；不要求所有实体改为 record。Java 方法可以叫 `createOrder`，HTTP 路径使用资源名称，两者不是同一套命名语法。

落位时先写一句职责，再问它需要什么数据与协作：

- “取消后不能覆盖原原因”是 Domain 的状态规则；放在 Mapper 的 SQL 或 Controller 分支中会让其他调用路径绕过它。
- “首次返回 201、重试返回 200”是 Adapter 对用例结果的 HTTP 映射；Domain 不返回 ResponseEntity。
- “一条订单快照在并发更新时保持一致”是 Gateway 所需的原子能力；Infrastructure 实现该保证，领域变更仍由 Domain 行为决定。
- “将 OrderDO 转为 OrderDTO”是合法纯查询的 App 转换；让 Domain 依赖 DO 去转换会反转边界。

每次变更明确列出涉及的模块/包/类/调用方，再核对 POM 和 import。只检查类名无法发现业务逻辑藏在错误层的问题。
