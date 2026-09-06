# Skillbox

可复用的 Codex 开发指导 Skill。当前提供 [cola-java](cola-java/SKILL.md)：基于固定 COLA 来源研究，指导 Java 21 / Spring Boot 3 标准多模块开发、HTTP 契约与业务类规模控制。

## 安装 cola-java

使用已核验参数的 `skills@1.5.23`（Node >=22.20.0）。在目标项目目录执行，明确只选择 `cola-java` 和 Codex：

```bash
npx skills@1.5.23 add https://github.com/xiongzihao-xzh/skillbox \
  --skill cola-java --agent codex --copy -y

# 也可以直接指定 Skill 子目录
npx skills@1.5.23 add https://github.com/xiongzihao-xzh/skillbox/tree/main/cola-java \
  --skill cola-java --agent codex --copy -y
```

项目级安装写入执行目录的 `.agents/skills/`，作用于该项目。`--copy` 复制技能资源，`-y` 跳过安装器确认。用户级安装显式增加 `-g`，作用于该用户的多个项目；路径按固定 CLI 的 Codex 配置输出为准，本次不执行全局安装验证：

```bash
npx skills@1.5.23 add https://github.com/xiongzihao-xzh/skillbox \
  --skill cola-java --agent codex --copy -g -y
```

安装后在 Codex 提示中显式写 `$cola-java`，例如“使用 `$cola-java` 为当前 COLA 项目新增订单取消用例”。也可在明确采用受支持 COLA 方案的任务中自动匹配。Codex 原生发现规则与元数据格式见 [官方文档](https://learn.chatgpt.com/docs/build-skills)；安装器版本和目录发现依据见 [兼容性研究](cola-java/references/compatibility-sources.md)。

## 本地开发验证

从 skillbox 仓库根目录只读列出技能：

```bash
npx skills@1.5.23 add . --skill cola-java --agent codex --list
```

在独立临时项目执行安装（路径替换为实际 clone 位置）：

```bash
install_check_dir="$(mktemp -d)"
cd "$install_check_dir"
npx skills@1.5.23 add /absolute/path/to/skillbox \
  --skill cola-java --agent codex --copy -y
```

主目录的 `cola-java/` 是分发源；安装后由 Codex 从技能目录发现，不需要作者机器上的 COLA 仓库。示例编译与测试见 [订单示例](cola-java/assets/examples/orders/README.md)。本地发现、复制安装、GitHub 安装和行为验收的**实际执行状态**统一在 [验证记录](cola-java/evals/validation.md)；本地成功不代表远程分发已通过。

开发过程的术语与决策见 [CONTEXT.md](CONTEXT.md) 和 [设计交接稿](docs/cola-java-design.md)。`.agents/skills/` 是仓库自身的协作技能，安装本成果时始终保留 `--skill cola-java` 选择条件。
