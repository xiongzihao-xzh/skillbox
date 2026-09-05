# Domain Docs

## 布局

采用 single-context：

- CONTEXT.md：根目录领域模型和术语表。
- docs/adr/：架构决策记录。

## 阅读规则

探索代码库前，读取根目录 CONTEXT.md，以及 docs/adr/ 中与当前任务
有关的决策记录。

这些文件不存在时，直接继续；由 domain-modeling 技能在领域术语或
决策实际明确后按需创建。

## 术语与决策

在任务标题、设计建议、假设和测试名称中，使用 CONTEXT.md 定义的
领域术语。

缺少所需术语时，先判断是否应使用已有概念；确有缺口时，记录为
domain-modeling 的后续输入。

建议与现有 ADR 冲突时，明确指出对应 ADR 和重新讨论的理由。
