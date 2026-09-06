# Java 21 兼容性与安装工具研究基线

核验日期：2026-09-06。本记录保存 `cola-java` 的技术适配依据与工具版本选择；这些版本选择属于本任务的技术适配，不是 COLA 架构规则。销售订单示例的实际构建、运行和技能安装结果应另行记录。

## 固定版本

| 项目 | 选择 | 一手依据与含义 |
| --- | --- | --- |
| Java | 21 | 本机 `java -version` 为 OpenJDK 21.0.10；Maven 使用同一 Java 主版本。 |
| Maven | 3.9.12 | 本机 `mvn --version` 的实际输出。Spring Boot 3.5.16 要求 Maven 至少 3.6.3，Java 范围为 17–25，覆盖此组合。[系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html) |
| Spring Boot | 3.5.16 | 对 Maven Central 元数据中的数字版本按版本号排序，3.5.x 的最高已发布版本为 3.5.16；同时存在对应正式发布页。[发布元数据](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/maven-metadata.xml)、[正式发布](https://github.com/spring-projects/spring-boot/releases/tag/v3.5.16) |
| Lombok | 1.18.46 | Spring Boot 3.5.16 BOM 的 `lombok.version`。Lombok 自 1.18.30 起加入 JDK 21 支持；这里保持 Boot 的版本组合。[固定版本 BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom)、[Lombok changelog](https://projectlombok.org/changelog) |
| MapStruct / processor | 1.6.3 | 官方 stable 文档与 1.6.x 发布元数据一致；元数据的 `release` 当前指向 1.7.0.Beta2，因此不能直接把该字段当作稳定版选择。[稳定版手册](https://mapstruct.org/documentation/stable/reference/html/)、[发布元数据](https://repo.maven.apache.org/maven2/org/mapstruct/mapstruct/maven-metadata.xml) |
| lombok-mapstruct-binding | 0.2.0 | 官方 Lombok 集成示例使用该版本；Maven Central 已发布。[集成说明](https://mapstruct.org/documentation/stable/reference/html/#_lombok)、[发布元数据](https://repo.maven.apache.org/maven2/org/projectlombok/lombok-mapstruct-binding/maven-metadata.xml) |
| cola-component-dto | 5.0.0 | `com.alibaba.cola:cola-component-dto:5.0.0` 的 POM 可读取，JAR 的 HTTP HEAD 返回 200。[发布 POM](https://repo.maven.apache.org/maven2/com/alibaba/cola/cola-component-dto/5.0.0/cola-component-dto-5.0.0.pom)、[发布元数据](https://repo.maven.apache.org/maven2/com/alibaba/cola/cola-component-dto/maven-metadata.xml) |
| cola-component-exception | 5.0.0 | `com.alibaba.cola:cola-component-exception:5.0.0` 的 POM 可读取，JAR 的 HTTP HEAD 返回 200。[发布 POM](https://repo.maven.apache.org/maven2/com/alibaba/cola/cola-component-exception/5.0.0/cola-component-exception-5.0.0.pom)、[发布元数据](https://repo.maven.apache.org/maven2/com/alibaba/cola/cola-component-exception/maven-metadata.xml) |
| maven-compiler-plugin | 3.14.1 | Spring Boot 3.5.16 BOM 的 `maven-compiler-plugin.version`。[固定版本 BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom) |
| maven-surefire-plugin | 3.5.6 | Spring Boot 3.5.16 BOM 的 `maven-surefire-plugin.version`；对应 Failsafe 也为 3.5.6。[固定版本 BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom) |
| spotless-maven-plugin | 3.10.2 | Maven Central 当前正式发布版本；固定版本说明要求运行 Maven 的 JRE 至少为 17。[发布元数据](https://repo.maven.apache.org/maven2/com/diffplug/spotless/spotless-maven-plugin/maven-metadata.xml)、[3.10.2 说明](https://github.com/diffplug/spotless/blob/maven/3.10.2/plugin-maven/README.md#requirements) |
| google-java-format | 1.30.0 | 显式固定 Spotless 3.10.2 源码对 JVM 21+ 推荐的 formatter 版本，避免 formatter 随环境默认值变化。[固定版本 JVM 适配表](https://github.com/diffplug/spotless/blob/maven/3.10.2/lib/src/main/java/com/diffplug/spotless/java/GoogleJavaFormatStep.java) |

## Maven 与注解处理器配置

示例可继承 `spring-boot-starter-parent:3.5.16`，显式设置 `java.version=21` 和 `maven.compiler.release=21`。若企业父 POM 只导入 Spring Boot BOM，还需显式管理构建插件；导入 BOM 不会取得 Boot 的 plugin management。[Spring Boot Maven 用法](https://docs.spring.io/spring-boot/3.5/maven-plugin/using.html)

Compiler Plugin 自 3.6 起支持 `release` 配置；它同时约束语言、目标字节码与公开 Java API。使用当前 JDK 21 编译时，目标值设为 `21`。[Compiler 官方说明](https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-release.html) Surefire 的版本要求表把 3.3.0–3.6.0 列为 Maven 3.6.3、JDK 8 起；采用 Boot 管理的 3.5.6，并通过实际 JUnit 测试确认示例执行结果。[Surefire 要求](https://maven.apache.org/surefire/maven-surefire-plugin/plugin-info.html)

`annotationProcessorPaths` 显式包含以下三个坐标，按 MapStruct 官方 Maven 示例的列表顺序表达：

```text
org.mapstruct:mapstruct-processor:1.6.3
org.projectlombok:lombok:1.18.46
org.projectlombok:lombok-mapstruct-binding:0.2.0
```

Lombok 常规依赖使用 `provided`；MapStruct 注解库与 processor 使用同一个版本。Lombok 1.18.16 起需要增加 binding。[MapStruct 集成示例](https://mapstruct.org/documentation/stable/reference/html/#_lombok)

官方 FAQ 说明 MapStruct 会等 Lombok 完成 AST 修改后才生成 mapper。上述列表展示配置顺序；协作依赖 binding 等处理机制，不能把“Lombok 在列表第一位”写成充分条件或官方强制顺序。验收时应确认 Lombok 生成的方法可供 MapStruct 使用，并且 mapper 实现确实生成和参与测试。[MapStruct FAQ](https://mapstruct.org/faq/#Can-I-use-MapStruct-together-with-Project-Lombok)

格式检查建议显式配置 Spotless 的 `googleJavaFormat` 为 `1.30.0`，使用同一 JDK 21 运行 Maven 和 formatter。版本选择依据固定版本的 JVM 适配表；示例仍需实际执行格式检查。[Spotless 3.10.2 源码](https://github.com/diffplug/spotless/blob/maven/3.10.2/lib/src/main/java/com/diffplug/spotless/java/GoogleJavaFormatStep.java)

## skills CLI 的固定版本和参数

npm registry 的 `skills` 最新发布版本为 **1.5.23**，包声明 Node **>=22.20.0**，仓库指向 `vercel-labs/skills`。本机 Node 22.22.1、npm 10.9.4 满足运行要求。已实际运行固定版本的 `--version`（输出 `1.5.23`）和 `--help`（退出码 0）。[固定版本包元数据](https://registry.npmjs.org/skills/1.5.23)、[固定版本源码](https://github.com/vercel-labs/skills/tree/v1.5.23)

| 参数 | 已核验的含义 |
| --- | --- |
| `add <source>` | 支持 GitHub 仓库、URL 和本地路径。 |
| `--skill cola-java` | 按 Skill 名称选择。 |
| `--agent codex` | 指定 Codex。该版本记录的项目目录为 `.agents/skills/`，全局目录为 `~/.codex/skills/`。 |
| `--list` / `-l` | 列出源中的可用 Skill，不执行技能安装。它不能单独证明实际复制安装成功。 |
| `--global` / `-g` | 选择用户级安装范围。 |
| `--copy` | 使用复制方式。 |
| `--yes` / `-y` | 跳过安装确认提示。 |

以上均已与 CLI 帮助和 `v1.5.23` 的参数解析源码交叉核对。[README 参数表](https://github.com/vercel-labs/skills/blob/v1.5.23/README.md#options)、[add 源码](https://github.com/vercel-labs/skills/blob/v1.5.23/src/add.ts)

`discoverSkills` 会扫描仓库根目录直属的子目录，因此 `cola-java/SKILL.md` 属于默认可发现布局。已有 `skills-lock.json` 标记的项目安装技能会经过过滤；无需为根目录直属的 `cola-java` 追加 `--full-depth`。这是基于固定版本发现函数的代码核验，仍需在完成后的仓库进行实际发现与安装验收。[发现逻辑](https://github.com/vercel-labs/skills/blob/v1.5.23/src/skills.ts)

在目标仓库内容完成后，可使用固定 CLI 版本进行只读发现：

```bash
npx skills@1.5.23 add /absolute/path/to/skillbox --skill cola-java --agent codex --list
npx skills@1.5.23 add xiongzihao-xzh/skillbox --skill cola-java --agent codex --list
```

实际安装命令的固定版本形式如下。执行目录决定项目级安装位置；全局安装显式加 `-g`。本研究没有执行这些安装命令。

```bash
npx skills@1.5.23 add /absolute/path/to/skillbox --skill cola-java --agent codex --copy -y
npx skills@1.5.23 add xiongzihao-xzh/skillbox --skill cola-java --agent codex --copy -y
npx skills@1.5.23 add xiongzihao-xzh/skillbox --skill cola-java --agent codex --copy -g -y
```

## 研究阶段的核验与复现边界

- 已只读获取 Maven Central 的发布元数据、Boot 3.5.16 BOM、两个 COLA 5.0.0 POM，并检查两个 COLA JAR 的 HTTP 可用性。
- 已读取版本固定的官方文档或源码，检查 MapStruct/binding、Spotless/JVM 适配及 skills 参数解析和发现逻辑。
- 已把 skills CLI 及其 npm/Node 缓存置于 `/private/tmp`，禁用 telemetry，实际执行 `--help` 和 `--version`。没有改动 `HOME`、`CODEX_HOME`，没有安装任何技能或全局 npm 包。
- 本研究阶段没有执行最终示例构建或技能安装；后续实现阶段的编译、MapStruct 生成、测试、formatter、启动以及本地/GitHub 安装结果统一见 [最终验证记录](../evals/validation.md)。

仅重跑 CLI 帮助时，可复用以下临时缓存方式；它会获取固定 CLI 包并运行帮助，不执行 `add`：

```bash
compat_tmp="$(mktemp -d /private/tmp/cola-java-compat.XXXXXX)"
DO_NOT_TRACK=1 NODE_COMPILE_CACHE="$compat_tmp/node-cache" \
  npm --userconfig=/dev/null --registry=https://registry.npmjs.org \
  --cache="$compat_tmp/npm-cache" --ignore-scripts --no-audit --no-fund \
  exec --yes --package=skills@1.5.23 -- skills --help
```

核验当天 Boot 发布元数据的 `lastUpdated` 为 `20260820133516`。升级时重新查询并记录日期、所选具体版本及验证结果；不能让“最新版”替代研究基线。
