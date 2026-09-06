# 标准多模块销售订单示例

这是原创教学和验证资源：Java 21、Spring Boot 3.5.16，采用标准 COLA Web 六模块形态。可复制到独立项目后按真实业务替换包名、坐标和规则。构建组合的依据见 [技术适配](../../../references/java21-spring-boot3.md)，已执行结果见 [验证记录](../../../evals/validation.md)。

## 运行

在本目录使用 JDK 21 和 Maven 3.6.3+（交付环境使用 3.9.12）：

```bash
mvn spotless:apply
mvn verify
java -jar start/target/start-1.0.0-SNAPSHOT.jar
```

默认监听 `127.0.0.1:8080`。服务重启清空内存数据；教学实现不含数据库、认证、租户隔离、支付、库存、发货、跨进程幂等记录或真实数据库事务。部署到业务环境前实现这些实际需要的边界；不要把内存测试称为数据库事务测试。

## 请求示例

```bash
curl -i http://localhost:8080/sales/orders \
  -H 'Content-Type: application/json' \
  -d '{"lines":[{"sku":"book","quantity":2,"unitPrice":12.50}]}'

# 把 order-id 替换为创建响应 data.id；也可读取响应 Location。
curl -i http://localhost:8080/sales/orders/order-id
curl -i -X PUT http://localhost:8080/sales/orders/order-id/cancellation \
  -H 'Content-Type: application/json' -d '{"reason":"duplicate purchase"}'
curl -i http://localhost:8080/sales/orders/order-id/cancellation
```

| 请求 | 结果 |
| --- | --- |
| POST /sales/orders | 201、Location、SingleResponse<OrderDTO>；至少一条有效明细，正数量和正单价 |
| GET /sales/orders/{orderId} | 200 与订单快照；订单不存在 404 |
| PUT /sales/orders/{orderId}/cancellation | 首次 201，相同 reason 重试 200 与原记录，更改 reason 409 |
| GET /sales/orders/{orderId}/cancellation | 200 与原记录；订单或记录不存在 404 |

取消时服务器产生时间，JSON 属性顺序不影响相同内容判断；本例 reason 按解码后的字符串精确比较，不自行 trim 或改变大小写。金额以同一教学计价单位的 BigDecimal 表达；没有定义多币种和汇率规则。POST 创建本身不提供重试去重。业务 HTTP 响应显式 no-store；没有为演示 CRUD 添加无业务用途的删除接口。

## 代码入口

| 模块 | 请求沿途的关键类型 |
| --- | --- |
| orders-client | api.OrderServiceI；dto 的 Cmd/Qry；dto.data 的 DTO 和错误码 |
| orders-adapter | web.OrderController、ApiExceptionHandler |
| orders-app | order.OrderServiceImpl；executor 的创建/取消；executor.query 的读取；OrderDtoConvertor |
| orders-domain | domain.order 的 Order、OrderLine、Cancellation；gateway.OrderGateway |
| orders-infrastructure | order.OrderGatewayImpl、OrderMapper、OrderDO、OrderConvertor |
| start | Application 与真实 HTTP 测试 |

App 的写执行器通过 Domain Gateway，领域规则由不可变 Order 行为保证；纯查询直接用 Infra Mapper 取得不可变 DO，再转 Client DTO。取消在 Mapper 的按键原子更新中调用 Gateway 提供的纯领域变换，异常不落部分数据；回调不做外部 I/O。真实数据库替换该能力时须独立验证事务、锁/版本策略及重试。

Lombok 用于 Client DTO；MapStruct 生成结构转换，手工恢复方法调用领域工厂。测试使用真实内存协作；排除 test starter 中未用的 Mockito，避免无用途的动态 agent 装载。需要 mock 的目标项目应自行验证其 Java 21 配置。

## 针对性验证

```bash
# 仅运行指定用例文件，同时构建所需上游模块
mvn -pl orders-domain -am -Dtest=OrderTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl orders-app -am -Dtest=OrderServiceTest,OrderMappingTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl start -am -Dtest=OrderHttpTest -Dsurefire.failIfNoSpecifiedTests=false test
```

全量完成检查使用 `mvn verify`，不要把允许上游模块没有“指定测试”的参数用作全套测试缺失的豁免。格式化后的所有手写 Java 文件按 Skill 的职责规则检查；target/generated-sources 来自已配置的 annotation processors。
