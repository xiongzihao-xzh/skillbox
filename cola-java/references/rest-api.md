# REST 与 HTTP 契约

## 标准语义与项目约定分开

REST 是架构风格：客户端/服务端、无状态、可标记的缓存、统一接口、分层系统，以及可选的按需代码。统一接口还包括资源标识、通过表示操作、自描述消息、超媒体驱动状态。因此本例展示资源导向 HTTP 契约，不据几个路径宣称完整 REST 合规。[Fielding 第 5 章](https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)

**项目约定**：路径体现真实业务模块，使用小写与 kebab-case，集合使用复数名词，如 `/sales/orders`、`/support/service-requests`。`adapter/app/domain` 是技术层，不是业务模块；Java 方法仍可使用 `createOrder` 等动词。

Controller 类型上不声明业务路径，完整路径写在方法映射上；既有 `/api`、版本等已确认前缀也写入完整方法路径。对父类、接口及组合注解同样检查，防止隐含前缀。

```java
// 路由片段，仅说明方法级声明；完整实现见订单示例。
@RestController
class OrderController {
    @GetMapping("/sales/orders/{orderId}")
    OrderDTO getOrder(@PathVariable String orderId) { /* 委派应用 API */ return null; }
}
```

违规反例是 `@RequestMapping("/sales/orders")` 放在类上，而方法仅写 `@GetMapping("/{orderId}")`。方法级完整路径、复数名词和 kebab-case 都是项目约定，不是 Spring、HTTP 或 REST 强制语法。

## 资源、方法与参数

| 用意 | 形式 | 语义和检查点 |
| --- | --- | --- |
| 创建集合成员 | POST /sales/orders | 服务器确定新资源；成功 201 与 Location；默认无幂等保证 |
| 查询单项 | GET /sales/orders/{orderId} | 安全、幂等；200，缺失 404；不通过 GET 写业务状态 |
| 查询集合 | GET /sales/orders?status=CREATED | 空集合 200 与空列表，不是 404 |
| 完整替换资源状态 | PUT /sales/orders/{orderId} | 幂等，发送约定的完整可写表示；不是“只给一个字段”的通用更新 |
| 局部修改 | PATCH /sales/orders/{orderId} | 声明 patch 媒体类型、允许字段、null/缺失含义和并发条件；方法本身不保证幂等 |
| 删除资源 | DELETE /sales/orders/{orderId} | 幂等效果；成功可 204；相同效果不要求每次响应码相同 |

仅实现业务实际需要的操作，不为表格补齐所有 CRUD。路径参数标识资源，查询参数表达过滤、分页与表示选择。查询字段、排序字段应有允许集合，不能把用户输入直接拼进存储查询。HTTP 方法与状态的依据见 [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html#section-9)；PATCH 须原子应用一组修改，失败不留下部分状态，[RFC 5789 §2](https://www.rfc-editor.org/rfc/rfc5789.html#section-2)。

**项目默认分页约定**：`pageIndex` 从 1 开始，`pageSize` 默认 20、范围 1–100；`sort=createdAt,desc`，同值加稳定 ID 排序；`status` 等过滤字段由业务允许集合限定。先校验再查询，越界页返回空列表。已有契约优先沿用；大数据集可选游标分页，明确排序和游标失效条件。这些不是 HTTP 标准，也不是必须使用 COLA PageQuery 默认值；COLA PageResponse 与分页边界仍须实际映射。

## 业务行为的资源化

销售订单的取消拥有原因和首次时间，且每单至多一份，因此建模为单例子资源 `PUT /sales/orders/{orderId}/cancellation`。`cancellation` 是单例，不是应取复数的集合。首次创建 201 与 Location；同一业务输入重试 200、返回原原因和时间；更改已记录原因 409。`GET` 该子资源可取回记录；订单或记录不存在为 404。这是项目契约，不是 RFC 指定的取消方式。

订单状态与取消记录在一次原子更新中变化；仅把 URI 的动词改名无法保证这个契约。POST 新建订单仍无幂等保证；真实业务需要重试去重时设计并持久化请求键与结果，不能依靠内存标志证明跨重启效果。

审批若包含审批人、意见、时间和多次记录，可建模为 `POST /procurement/purchase-orders/{id}/approvals`；发布若只改变状态，可用定义明确的 PATCH，若生成有版本的发布结果则考虑 `POST /content/articles/{id}/publications`。先确定资源身份、生命周期、并发与失败语义，再选形式；这些是条件性建议。

## HTTP 状态、包装与错误

| 实际结果 | HTTP / 本例约定 |
| --- | --- |
| 查询或重试成功、有表示 | 200 + 成功包装 |
| 创建新资源 | 201 + 成功包装 + Location |
| 成功且无表示 | 204，无响应体 |
| JSON、类型、Bean Validation 错误 | 400 + INVALID_REQUEST |
| 资源不存在 | 404 + ORDER_NOT_FOUND（取消记录缺失另用明确码） |
| 请求与已记录状态冲突 | 409 + CANCELLATION_CONFLICT |
| 形态有效但违反领域规则 | 422 + 对应业务码 |
| 未预期服务端故障 | 500 + INTERNAL_ERROR；服务端记日志，客户端不暴露堆栈 |

未认证/无权限按认证方案使用 401（含适用 challenge）/403，方法不支持用 405 并保留 Allow，媒体类型不支持用 415，协商失败用 406。不要把框架错误全部改成 200 或 500。[状态定义](https://www.rfc-editor.org/rfc/rfc9110.html#section-15)

在进入写用例前完成可提前判断的协议检查。方法映射声明实际支持的响应类型（本例 `produces = "application/json"`），使不支持的 Accept 在执行取消等业务前返回 406；测试同时查询资源，确认失败请求没有改变状态或创建记录。

Bean Validation 检查的是反序列化后的值。对本例的整数数量，关闭 `spring.jackson.deserialization.accept-float-as-int`，让 `2.9` 等浮点输入返回 400；仅用 `@Positive` 无法发现已经被截断的数量。其他对象按自身输入契约选择转换规则。

本例有响应体时用 COLA SingleResponse/Response；错误码是稳定业务契约，消息可读但不供客户端分支判断。成功包装的数据与内部用例元数据分开：取消是否首次用于 Adapter 选择状态，HTTP data 仍是取消记录。204/HEAD/304 遵循无响应体语义，不能由统一包装器追加 JSON。

可以选 Problem Details，但它不是本例默认；若目标项目采用，保持其 status 与实际状态一致。响应 Java 类与 HTTP 契约是独立层次。[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html#section-3.1.2)

## 无状态与缓存

每个请求携带理解它所需的身份、目标和输入；服务端保存业务数据不违反无状态，但不能依赖上次请求的会话步骤猜测当前操作。认证和租户隔离按真实项目配置；示例不包含认证，不能当作生产权限实现。

本例响应显式 `Cache-Control: no-store`。在真实查询接口中按敏感性和时效性决定 private/public、max-age，必要时使用 ETag/Last-Modified、条件请求和 304；写入后明确失效策略。no-cache 要求复验，并非禁止存储；no-store 不代替传输安全或访问控制。[RFC 9111](https://www.rfc-editor.org/rfc/rfc9111.html#section-5.2)
