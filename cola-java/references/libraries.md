# 工具库和对象转换

**建议实践**：先用 Java 21 标准库及现有项目依赖。Lombok、MapStruct、Apache Commons、Guava 都是可选能力；为同一简单操作重复引入多套工具库会增加维护成本。本例实际使用 Lombok 与 MapStruct，其他库不默认加入。

## Lombok

DTO 是可变传输载体时可用 Getter/Setter 或 Data；对继承 COLA DTO/Command/Query 的类型明确 equals/hashCode 的继承语义。领域实体的身份、构造与状态转换由业务控制，不因节省样板就开放全部 setter 或对全部字段生成身份相等判断。

示例的 Client DTO 使用 Lombok，Order 保留私有构造与行为方法，值对象和存储快照在合适处使用不可变 record。这个选择是示例实践，不把 record 强制推广到所有项目。

## MapStruct

按字段做结构映射；价格、状态转换、权限或跨聚合规则由业务对象或用例负责。转换类放在同时允许依赖两端类型的层：App 处理 Client 与 Domain/查询 DO，Infrastructure 处理 Domain 与存储 DO。Domain 恢复从受控工厂进入，构造时仍校验不变量。

示例把 `unmappedTargetPolicy` 设为 ERROR，让新增目标字段在编译时暴露遗漏；生成源需要实际存在并通过映射测试。业务含义不同而字段同名时显式指定转换，不能把自动映射视为正确性证明。

联合使用时，示例 parent 的 `annotationProcessorPaths` 包含：

```xml
<!-- 配置片段；完整可编译 POM 在 assets/examples/orders/pom.xml -->
<annotationProcessorPaths>
  <path><groupId>org.mapstruct</groupId><artifactId>mapstruct-processor</artifactId><version>${mapstruct.version}</version></path>
  <path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>${lombok.version}</version></path>
  <path><groupId>org.projectlombok</groupId><artifactId>lombok-mapstruct-binding</artifactId><version>${lombok-mapstruct-binding.version}</version></path>
</annotationProcessorPaths>
```

这按官方集成示例表达，排列不等于 annotation processor 的执行顺序保证；binding 配合处理机制让 MapStruct 看见 Lombok 生成的方法。MapStruct 注解库与 processor 版本保持一致，具体版本与官方依据见 [兼容性记录](compatibility-sources.md#maven-与注解处理器配置)。

验证映射应使用有辨识度的值，例如两条不同数量/价格的明细和固定取消时间，检查往返后的字段、金额、状态及嵌套对象；避免只断言 mapper bean 能注入。数据库或外部接口的真实读写仍由对应测试负责。
