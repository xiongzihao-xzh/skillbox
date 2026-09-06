# 验证记录

验证日期：2026-09-06。本文区分已编写用例、静态检查、真实执行及尚未验证事项。用户手动开启 implement；没有自动调用正式 code-review 阶段。

## 研究与环境

- COLA 来源：`alibaba/COLA` commit `352e1a867538d9cd40e1b11e4028fa652fc97557`，研究前后工作区均干净，没有更新或修改来源仓库。交叉读取 Web 模板、craftsman 写/读用例、POM、DTO/异常/Domain 组件与许可证；[sources.md](../references/sources.md) 的 79 个固定提交文件链接及行号已与本地 Git 对象逐一核验。
- 目标为 Java 21 / 标准 Web 六模块。实测 OpenJDK 21.0.10、Maven 3.9.12；Spring Boot 3.5.16、COLA DTO/exception 5.0.0、Lombok 1.18.46、MapStruct 1.6.3、binding 0.2.0。完整依据见 [兼容性研究](../references/compatibility-sources.md)。
- CLI 实测 skills 1.5.23、Node 22.22.1；Python 标准库测试使用 Python 3.9.6。Skill 官方校验工具使用临时虚拟环境及 PyYAML 6.0.3。
- Maven 使用临时空 settings 和独立缓存；npm 使用临时缓存、`--userconfig=/dev/null`、`--ignore-scripts` 与 `DO_NOT_TRACK=1`。没有改写用户的 HOME、CODEX_HOME、全局技能或全局包配置。

## Java 示例：实际通过

在 [orders](../assets/examples/orders/README.md) 根目录执行 `mvn spotless:apply`，然后执行一次最终 `mvn verify`，结果 **BUILD SUCCESS**，7 个 reactor 项目全部成功。实际命令另带 `-B -s /private/tmp/cola-java-build/settings.xml -Dmaven.repo.local=/private/tmp/cola-java-build/m2`，隔离依赖配置与缓存。

| 测试类 | 数量 | 已观察到的行为 |
| --- | ---: | --- |
| OrderTest | 3 | 创建及金额计算；空明细、非法数量/价格/SKU 拒绝；取消、同原因重试保留时间、原因冲突、原快照不可变 |
| OrderServiceTest | 2 | 创建后可查询；修改返回 DTO 不污染存储；24 个并发取消请求只创建一份记录 |
| OrderMappingTest | 1 | 真实 MapStruct 转换跨 Domain → DO → Domain → DO → Client，核对嵌套行、金额、状态、原因和时间 |
| OrderHttpTest | 5 | RANDOM_PORT 下真实 HTTP：201/Location/GET；取消首次 201、重试 200、冲突 409、记录可查询；400/404/405/415 与 COLA 错误体；Allow/no-store；拒绝未知字段且状态不变；204/HEAD 无体；500 不泄漏内部异常 |

合计 **11 tests，0 failures，0 errors，0 skipped**。204 与未知异常由 test-only 端点验证，不增加虚假的订单业务接口；没有测试 304、认证或缓存复验。

MapStruct 在 App 和 Infrastructure 各生成一个实现，参与实际编译与映射测试。Lombok DTO 方法与 MapStruct 联合使用已由上述编译验证，不仅检查依赖声明。Spotless 3.10.2 / Google Java Format 1.30.0 检查通过。

另启动 `java -jar start/target/start-1.0.0-SNAPSHOT.jar --server.port=0`：创建返回 201、读取返回 200、总价为 25.0；测试专用 `/test/no-content` 返回 404，确认测试端点未进入生产 JAR。进程在检查后停止。构建产物移到临时验证目录，不进入 Skill 分发文件。

最终 POM 检查与示例一致：`start → adapter → app → infrastructure → domain → client`，另有 `app → client`。逐一检查生产入口、执行器、领域、Gateway、Mapper 和转换器：Controller 仅调用 Client API；写入经过 Domain Gateway；纯查询读取 Infra；Domain 无 Infra/Web 依赖；四个生产路由均在方法级声明完整业务路径。此结论覆盖本例，不代表所有 COLA 项目均已检查。

## 统计工具与规模：实际通过

执行 `python3 -m unittest cola-java/evals/test_java_line_counts.py`，**6 tests，0 failures/errors**。夹具由 TemporaryDirectory 隔离并清理，验证：

- 500 / 501 候选边界、空文件、末尾无换行、LF/CRLF/CR、空行、注释及内部类型。
- 暂存、未暂存、未跟踪和带空格路径；排除已删除与未修改文件；暂存改动又被工作区抵消的文件仍纳入统计。
- 明确生成来源排除并回报依据；文件名不决定例外；拒绝无依据排除、非 Git 默认模式和根目录外文件。

对格式化后的完整示例实际执行 `java_line_counts.py --all`，仅对 App、Infrastructure 的 `target/generated-sources` 提供明确的 MapStruct / Maven 依据：**31 个手写 Java 文件，0 个超限候选**；其中生产 27 个、测试 4 个。最长生产文件 OrderController.java 为 75 行，最长整体文件 OrderHttpTest.java 为 178 行。排除的两个文件是实际生成并编译的 OrderDtoConvertorImpl.java 和 OrderConvertorImpl.java；没有按 DTO、Entity 等名称豁免任何手写文件。

已结合实际职责复核：领域对象维护规则、执行器组织用例、Gateway/Mapper 提供原子存储能力、Controller/Advice 处理 HTTP。没有需要登记的手写超限例外。数字结果不能代替将来变更的职责检查。

## 行为验收

[cases.md](cases.md) 已编写 C01–C15 的输入、预期和失败条件。

独立代理在临时快照中实际使用本 Skill，未读取评估答案、未被告知预期发现，未调用 code-review。四个原始 Java 文件未修改，SHA-256 前后相同；实际读文件并运行本 Skill 统计器后输出逐文件建议：

| 代表用例 | 原始输入 | 实际结果 |
| --- | --- | --- |
| C03 / C04 | 16 行 Controller，类级 `/api/v1/sales/orders`、动词路由、直接 Mapper、返回 DO、计算金额 | 识别入口越过 App、存储契约泄漏及规则位置；保留 /api/v1，建议完整方法路径、API/DTO 和真实 HTTP；未声称已修复 |
| C08 | 507 行 RuntimeConfiguration，504 行为同组常量声明 | 读取完整内容后给出声明型例外；不按数字机械拆分，不将其当生成代码排除 |
| C09 的职责判断分支 | 507 行 OrderEntity，取消规则/状态转换，加连续说明注释 | 不授予 Entity 名称豁免；记录超限未处理，区分必要注释与重复占位说明，不为多出的 7 行制造无意义拆分类 |
| C11 | 9 行 Processor 混合定价、SQL、CSV、邮件 | 即使未超限仍报告职责/分层问题，另指出实际 SQL 无 WHERE；提出领域规则、App 编排和基础设施实现的落位 |

另执行 C06：同一独立代理按普通 Java 21 项目约定，只把 formatCount 的前缀改为 `Count: `；没有引入 COLA 模块、依赖或迁移。实际执行 `javac --release 21` 及原有 CountFormatterTest，0、12、-12 三个输入通过，测试文件未改。两次代理报告保留在临时验收记录中，原始临时夹具验证后清理。

第一项代理验收是指定节选的真实静态分析行为，不是这些节选的编译或修复验证，也没有验证复杂 Entity 的真实拆分。C01/C05/C13/C14 的机制由上面的 Java 示例测试覆盖，C15 由 CLI 测试覆盖；这些测试不等于 Codex 生成能力已逐例验证。C02/C07/C10/C12 目前仅编写并检查用例，没有独立生成/修复验收结果。

## 格式、资源与本地安装：实际通过

- 实际运行环境提供的 `skill-creator/scripts/quick_validate.py cola-java`，输出 `Skill is valid!`。检查了 frontmatter、名称和脚手架残留；另读取生成的 agents/openai.yaml，核对显示名称、短描述、显式 `$cola-java` 提示与默认隐式发现策略。
- 检查 Skill 与根 README 的相对文件链接和 Markdown 标题锚点：首次检查 34 个，0 个缺失；交付文档完成后再次复核。运行说明、脚本和模板没有作者机器路径或本地 COLA 运行依赖。
- 实际运行 `skills@1.5.23 add <skillbox-root> --skill cola-java --agent codex --list`，报告 Found 1 skill / cola-java。
- 在两个独立临时项目分别运行 `add <skillbox-root>` 和 `add <skillbox-root>/cola-java`，均使用 `--skill cola-java --agent codex --copy -y`，成功复制到项目 `.agents/skills/cola-java`。
- 安装时 57 个分发文件逐一 SHA-256 与源文件一致；只安装 cola-java，没有符号链接或嵌套 .git。直接使用安装目录内的统计脚本检查其携带示例，得到 31 个手写文件，证明该工具运行不依赖本地 COLA 源仓库。后续只补充验证文档，安装验证不表示冻结未来文件内容。

## GitHub 分发

真实 remote 为 `git@github.com:xiongzihao-xzh/skillbox.git`，当前分支 main。仓库根 URL 与 `/tree/main/cola-java` 的命令在分发仓库的 [根 README](https://github.com/xiongzihao-xzh/skillbox#安装-cola-java) 单点维护。

本地实现已就绪；GitHub 来源的实际安装将在首次实现提交推送后执行，再把执行结果更新到本节。当前记录不宣称远程安装通过。

## 失败修正与未验证范围

TDD 中实际观察过缺失领域/应用实现、缺失启动配置，以及取消接口期望 201 却返回 404 的失败，再补充实现使相应测试通过。统计器补充暂存/工作区抵消夹具时先观察到漏文件的失败，再改为合并两种差异范围，6 项回归通过。

初次依赖解析受沙箱 DNS 限制，授权的联网执行后成功；这不是业务测试的 red。应用测试曾因未使用的 Mockito 自动监听器尝试动态装载 agent 而失败；示例不使用 mock，已排除该测试依赖，真实协作测试通过。没有掩盖或跳过测试。

未实测：COLA 上游工程完整构建/数据库测试、真实数据库事务与跨进程幂等、支付/库存/发货、生产认证/租户隔离、全局技能安装、所有行为用例的独立代理运行、手动 code-review 阶段。没有把教学订单规则推广成所有业务要求；没有把来源提交称为远端最新版本。
