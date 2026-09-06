---
name: cola-java
description: 在明确采用 Alibaba COLA 标准多模块方案的 Java 项目中指导开发、代码落位、重构与架构审查，面向 Java 21 和 Spring Boot 3。用户要求 COLA 合规检查或其他 Skill 显式指定时也适用。普通 Java 项目不自动迁移；不覆盖 COLA light。
---

# COLA Java

以标准 Web 多模块方案组织业务，用可追溯的 COLA 规则判断代码落位，并执行本 Skill 的项目约定。新项目技术基线为 Java 21 / Spring Boot 3.x；实际可编译示例见 [orders](assets/examples/orders/README.md)。

## 先确定适用范围

读取目标项目的 AGENTS、领域文档、构建文件和本次修改范围。识别实际 COLA 方案，区分新建、功能开发、局部调整与审查；已有项目沿用核验兼容的版本和持久化技术。非 COLA 项目继续按用户原任务处理；light 或其他变体说明当前未覆盖，保持任务范围。

项目约定默认强制适用于使用本 Skill 的项目。与目标项目明确规则冲突时，说明冲突并按指令优先级处理。发现存量违规不扩大为全项目迁移；改变既有对外契约仍须处于当前授权范围。

## 核心约束

- **COLA 规则**：遵循选定方案的职责和依赖。Web 模板允许 App 依赖 Infrastructure；写入经 Domain Gateway，纯查询可在 App 直接读取 Infrastructure，需要领域行为时使用 Domain。编译依赖图与调用链分开判断。
- **项目约定**：Controller 方法级写完整业务路径，保留已确认的统一前缀。路径使用真实业务模块、小写、kebab-case，集合使用复数名词；类型、父类、接口不提供业务路径前缀。
- **项目约定**：按实际结果返回 HTTP 状态，有响应体时沿用 COLA Response；204 无响应体。资源命名规则不等同于完整 REST 合规证明。
- **项目约定**：手写业务逻辑类的源文件物理总行数上限为 **500**，包括空行、注释与内部类型。超过上限先判断职责；单一声明型或数据型类可据实例外，含复杂业务行为的 Entity 仍受限。生成文件须凭明确来源排除；未超限也要检查职责与复杂度。
- **技术适配**：Java 21、Boot 3.x；只迁移受影响的 Jakarta API。组件和工具库按需使用，Lombok 保留领域封装，MapStruct 承担结构转换。

## 开发流程

1. **界定用例**：明确业务输入、结果、业务不变量、现有接口兼容性及修改文件范围。
2. **确定落位**：读 [架构](references/architecture.md) 与 [命名和落位](references/naming-and-placement.md)，列出本次类的模块、包、职责、依赖和调用方，区分写入与查询。争议结论追溯 [来源](references/sources.md)，不把其他架构流派或模板占位实现写成官方规则。
3. **预判规模**：读 [类规模与职责](references/code-quality.md)，检查要扩展的类是否接近上限、是否已混杂职责。需要拆分时沿业务能力、用例或协作边界安排，保持领域与事务行为。
4. **实现业务与边界**：规则留在相应业务职责内，输入、领域、数据和输出对象明确转换；App 编排，Infrastructure 实现边界。涉及构建或升级时读 [技术适配](references/java21-spring-boot3.md)，涉及转换或工具库时读 [工具库](references/libraries.md)。
5. **实现 HTTP 契约**：有接口变更时读 [REST API](references/rest-api.md)，落实完整路径、参数、资源行为、状态与错误；必要时提供兼容迁移方案，而非直接破坏已发布接口。
6. **验证**：补充与变更相称的行为测试；执行项目格式化、编译和测试，再按 [审查清单](references/review-checklist.md) 检查依赖、命名、路由和本次 Java 文件的职责与规模。给出实际范围与结果。
7. **交付**：列出改动、已执行验证、每个超限候选的职责和处理依据、范围外遗留问题以及未验证项。区分局部合规与全仓合规、静态检查与实际运行。

## 类规模复核

可运行随 Skill 提供的 Python 3 标准库工具（把 `<skill-dir>` 替换为本 Skill 的实际目录）：

```bash
python3 <skill-dir>/scripts/java_line_counts.py --root <project-repository-root>
```

默认检查 Git 中暂存、未暂存和未跟踪的 Java 变更；显式文件、全量检查及生成目录依据见 [工具用法](references/code-quality.md#统计工具)。工具只报告候选，退出码 0 不代表架构合规。

对每个超限文件报告路径、行数、实际职责，以及：已合理拆分；声明/数据例外及依据；或存量问题未处理及原因。新增业务类满足上限，修改不使原本合规类超限；局部修复存量问题不要求扩大为整类重构。

## 示例与维护

- [完整订单示例](assets/examples/orders/README.md)：新建项目、追踪读写与取消重试时参考；内存教学实现的限制不推广为生产约束。
- [行为验收](evals/cases.md)：修改本 Skill 的触发范围、流程或关键规则后选择相关用例验证。
- [验证记录](evals/validation.md)：核对交付时实际运行过什么及其限制。
- [版本与安装器依据](references/compatibility-sources.md)：升级示例依赖或维护安装说明时读取。安装方式见所在分发仓库的 README，不作为 Skill 运行依赖。
- [来源与归属](NOTICE.md)：直接复用上游材料前核对许可证及保留要求。
