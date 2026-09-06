# Java 21 / Spring Boot 3 技术适配

以下属于技术适配，架构选择见 [architecture.md](architecture.md)。新建项目可从 [完整示例](../assets/examples/orders/README.md) 复制到目标目录，再替换 groupId、artifactId 和包名；按真实用例裁剪教学规则，逐个核对 POM 和入口路径，不修改本 Skill 内的模板来充当业务项目。

## 已固定的构建组合

| 项目 | 示例版本或配置 |
| --- | --- |
| Java | 21，`java.version` 与 `maven.compiler.release` 为 21 |
| Spring Boot parent | 3.5.16 |
| COLA DTO / exception | 5.0.0，按需使用这两个组件 |
| Lombok | 1.18.46，由 Boot 管理，编译期 provided |
| MapStruct / processor | 1.6.3 |
| lombok-mapstruct-binding | 0.2.0 |
| Compiler / Surefire | Boot 管理的 3.14.1 / 3.5.6 |
| 格式化 | Spotless 3.10.2 / Google Java Format 1.30.0 |

一手版本证据见 [compatibility-sources.md](compatibility-sources.md)，实际执行记录见 [validation.md](../evals/validation.md)。源码研究使用固定提交，编译使用已发布组件，两者不是同一个版本标识。已有项目先验证兼容性并沿用其版本，不为应用 Skill 而主动升级业务项目。新选版本查正式发布与 Java 兼容范围，再编译验证；固定在 Boot 3.x。

## 构建与迁移检查

- 示例继承 Boot parent 取得依赖与插件管理；若企业父 POM 只导入 Boot BOM，另行管理 compiler、test 和打包插件。保留 dependency convergence，先解释冲突再覆盖受管版本。
- 用实际 JDK 21 执行构建；只改 `source/target` 字符串不证明构建环境一致。`release=21` 不开启 preview。
- Web 模板中的 `javax.annotation.Resource`、旧 Servlet/Validation 等迁移到对应 Jakarta API；`javax.annotation.processing.Generated`、`javax.sql`、`javax.crypto` 等 Java SE 包仍保留。依赖旧 EE API 的第三方库需逐项升级或替换。
- 旧 MyBatis starter、JUnit 4 runner、Spring Boot 2 测试注解和依赖不能盲抄。持久化仍沿用目标项目选择；本例用内存，不声明 MyBatis/JPA 是唯一架构要求。
- 本例使用构造器注入，Domain 普通 Java。是否引入 COLA domain-starter、catchlog、扩展点或状态机由业务需要决定，不能为展示能力默认加入。
- 内存原子更新与数据库事务是不同保证。数据库实现需测试事务管理器、代理边界、回滚、并发控制及外部副作用；详见 [架构](architecture.md#校验异常事务和测试)。

## 编译与运行

示例根目录的标准命令：

```bash
mvn spotless:apply
mvn verify
java -jar start/target/start-1.0.0-SNAPSHOT.jar
```

临时验证可复制示例到独立目录，使用 `-Dmaven.repo.local=<temporary-cache>`，保持用户全局配置。构建失败应区分源码错误、依赖不可获取与环境限制，并记录真实输出；网络失败不能当作 TDD 的业务失败。
