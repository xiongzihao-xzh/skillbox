# 保留 COLA 的读写分流边界

cola-java 的默认完整示例采用销售订单的创建、查询和取消：写入通过 Domain 定义的 Gateway，纯查询允许 App 直接读取 Infrastructure，需要领域行为时使用 Domain。这一选择依据本地 COLA 研究基线中的 Web 模板依赖和 craftsman 读写实现，使教学规则与核验到的上游边界保持一致；统一要求查询经过 Gateway 会增加本任务未选择的项目约束。

研究基线为 `alibaba/COLA` 的 commit `352e1a867538d9cd40e1b11e4028fa652fc97557`。以下路径均相对于来源仓库，记录的是显式 POM 依赖与源码调用核查结果，尚未通过构建或运行时验证：

- `cola-archetypes/cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-app/pom.xml`：App 显式依赖 Infrastructure 和 Client。
- `cola-samples/craftsman/craftsman-app/src/main/java/com/alibaba/craftsman/command/ATAMetricAddCmdExe.java`：写入调用 Domain 的 `MetricGateway`。
- `cola-samples/craftsman/craftsman-app/src/main/java/com/alibaba/craftsman/command/query/ATAMetricQryExe.java`：查询直接使用 Infrastructure 的 `MetricMapper`，仍引用 Domain 枚举。
