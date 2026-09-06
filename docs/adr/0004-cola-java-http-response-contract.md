# COLA 响应包装配合真实 HTTP 状态

默认完整示例在有响应体时沿用 COLA 的 Response 系列，HTTP 状态码按实际请求结果返回；204 响应保持无响应体。这保留了选定 COLA 基线中的应用结果表达，同时使 HTTP 客户端能够依据状态码判断结果；新示例不采用“成功直接返回资源、失败使用 ProblemDetail”的默认格式，已有项目仍先核对其已确认的契约与当前授权范围。

固定研究基线中的 `com.alibaba.cola.dto.Response`、`SingleResponse`、`MultiResponse` 和 `PageResponse` 本身不包含 HTTP 状态映射，映射职责需要明确落位。HTTP 204 不允许响应体的依据是 [RFC 9110 §15.3.5](https://www.rfc-editor.org/rfc/rfc9110.html#section-15.3.5)；最终 JSON 字段与状态映射仍须在实现阶段通过序列化和 HTTP 测试核验。
