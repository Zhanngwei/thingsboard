# Rest Client Tools Transport Chinese Comments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ThingsBoard `rest-client`、`tools`、`transport` 三个模块下全部 Java 文件补充高质量中文注释，并保持所有业务逻辑、变量名、方法签名、导入、注解和原有格式不变。

**Architecture:** 沿用 `rule-engine`、`application`、`common`、`dao`、`monitoring`、`msa`、`netty-mqtt` 的 comment-only 做法：先固定文件清单和审计规则，再对类、字段、方法和复杂分支插入中文解释，最后通过 diff 审计与 Maven 编译验证。`rest-client` 侧重点是 REST facade、JWT 自动刷新、分页参数和 multipart 请求；`tools` 侧重点是 PostgreSQL dump 到 Cassandra SSTable 的离线迁移和 MQTT SSL 手工验证；`transport` 侧重点是各协议独立 Spring Boot transport 进程的启动参数、组件扫描、异步调度和与队列/缓存/Transport API 的协作。

**Tech Stack:** Java, Maven, Spring Boot, Spring Web RestTemplate, JWT, Cassandra CQLSSTableWriter, Apache Commons CLI/IO, Eclipse Paho MQTT, SSL/TLS, ThingsBoard Transport, PowerShell, Git.

## Global Constraints

- 只覆盖 `rest-client/**/*.java`、`tools/**/*.java`、`transport/**/*.java`。
- 本批扫描结果：`rest-client` 2 个 Java 文件，`tools` 6 个 Java 文件，`transport` 5 个 Java 文件，合计 13 个 Java 文件，全部位于 `src/main/java`。
- 不修改任何业务逻辑。
- 不修改变量名、方法名、方法参数、返回类型、可见性、继承结构、注解、导入或包名。
- 不运行格式化工具，不重排原代码。
- 不删除或替换已有英文注释，只追加中文注释。
- 每个 Java 文件必须包含“本类总结”。
- 类注释说明类目的、所属 ThingsBoard 模块、协作模块、生命周期、为什么需要该类、设计模式。
- 字段注释说明字段保存的数据、数据来源、生命周期、为什么设计成字段。
- 方法注释说明职责、参数、返回值、调用时机、调用方、使用流程、线程安全、事务、缓存、MQTT、Actor、数据库、Rule Engine 关系。
- 复杂条件分支、异常处理、认证刷新、分页参数拼接、multipart 请求构造、SSTable 写入、dump block 解析、Spring Boot transport 参数补齐处增加行内中文注释。
- 当前工作区已有其它模块注释变更，执行本计划时不得回滚、重排或修改这些范围。
- 若 `git config user.name` 或 `git config user.email` 为空，不创建提交。

---

## File Structure

修改范围：

- `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`
- `rest-client/src/main/java/org/thingsboard/rest/client/utils/RestJsonConverter.java`
- `tools/src/main/java/org/thingsboard/client/tools/MqttSslClient.java`
- `tools/src/main/java/org/thingsboard/client/tools/migrator/DictionaryParser.java`
- `tools/src/main/java/org/thingsboard/client/tools/migrator/MigratorTool.java`
- `tools/src/main/java/org/thingsboard/client/tools/migrator/PgCaMigrator.java`
- `tools/src/main/java/org/thingsboard/client/tools/migrator/RelatedEntitiesParser.java`
- `tools/src/main/java/org/thingsboard/client/tools/migrator/WriterBuilder.java`
- `transport/coap/src/main/java/org/thingsboard/server/coap/ThingsboardCoapTransportApplication.java`
- `transport/http/src/main/java/org/thingsboard/server/http/ThingsboardHttpTransportApplication.java`
- `transport/lwm2m/src/main/java/org/thingsboard/server/lwm2m/ThingsboardLwm2mTransportApplication.java`
- `transport/mqtt/src/main/java/org/thingsboard/server/mqtt/ThingsboardMqttTransportApplication.java`
- `transport/snmp/src/main/java/org/thingsboard/server/snmp/ThingsboardSnmpTransportApplication.java`

全局审计命令模板：

```powershell
$roots = @('rest-client','tools','transport')
$files = @($roots | ForEach-Object { rg --files $_ -g '*.java' })
$missing = @($files | Where-Object { -not (Select-String -LiteralPath $_ -Pattern '本类总结' -SimpleMatch -Quiet) })
$bad = @(git diff -- rest-client tools transport 2>$null | Where-Object { $_ -like '+*' -and $_ -notlike '+++*' -and $_ -notmatch '^\+\s*(/\*\*?|//|\*|$)' })
$removed = @(git diff -- rest-client tools transport 2>$null | Where-Object { $_ -like '-*' -and $_ -notlike '---*' })
"JAVA_COUNT=$($files.Count)"
"MISSING_SUMMARY_COUNT=$($missing.Count)"
"BAD_ADDED_COUNT=$($bad.Count)"
"REMOVED_COUNT=$($removed.Count)"
```

Expected:

```text
JAVA_COUNT=13
MISSING_SUMMARY_COUNT=0
BAD_ADDED_COUNT=0
REMOVED_COUNT=0
```

### Task 1: Baseline And Guardrails

**Files:**
- Read: `rest-client/pom.xml`
- Read: `tools/pom.xml`
- Read: `transport/pom.xml`
- Read: `transport/*/pom.xml`
- Modify: none

**Interfaces:**
- Consumes: 当前三模块 Java 文件清单、Maven 聚合结构、Git 工作区状态。
- Produces: 后续任务使用的固定范围、审计命令和验证命令。

- [ ] **Step 1: Confirm Java file count**

Run:

```powershell
$roots = @('rest-client','tools','transport')
$files = @($roots | ForEach-Object { rg --files $_ -g '*.java' })
$main = @($files | Where-Object { $_ -match '\\src\\main\\java\\' })
$test = @($files | Where-Object { $_ -match '\\src\\test\\java\\' })
"MAIN_COUNT=$($main.Count)"
"TEST_COUNT=$($test.Count)"
"TOTAL=$($files.Count)"
```

Expected:

```text
MAIN_COUNT=13
TEST_COUNT=0
TOTAL=13
```

- [ ] **Step 2: Capture pre-change status**

Run:

```powershell
git status --short -- rest-client tools transport docs\superpowers\plans docs\superpowers\specs
```

Expected: no pre-existing source modifications in `rest-client`、`tools`、`transport`.

### Task 2: Annotate Rest Client Module

**Files:**
- Modify: `rest-client/src/main/java/**/*.java`

**Interfaces:**
- Consumes: ThingsBoard REST API endpoints, `RestTemplate`, JWT token pair, DTO/page/query model classes.
- Produces: Chinese explanations for REST facade lifecycle, authentication refresh, URL/page params, upload/download and JSON-to-KV conversion.

- [ ] **Step 1: Add comments**

Document `RestClient` as a client-side facade/adapter over ThingsBoard REST API, including token refresh lifecycle, retry boundary, thread safety, database/Rule Engine indirectness and cache absence. Document `RestJsonConverter` as JSON telemetry/attribute adapter.

- [ ] **Step 2: Verify**

Run:

```powershell
git diff --check -- rest-client
mvn -pl rest-client -DskipTests compile
```

Expected: comment-only diff and compile success.

### Task 3: Annotate Tools Module

**Files:**
- Modify: `tools/src/main/java/**/*.java`

**Interfaces:**
- Consumes: PostgreSQL dump files, Cassandra SSTable writer schema, related entity map, telemetry dictionary, CLI args, SSL keystore and Paho MQTT client.
- Produces: Chinese explanations for offline migration pipeline, parser lifecycle, writer factory methods and manual MQTT SSL verification.

- [ ] **Step 1: Add comments**

Document parser/writer/migrator roles, dump block scanning, partition generation, string-to-number casting, CQL schema choice, command-line parsing, file lifecycle and MQTT SSL test lifecycle.

- [ ] **Step 2: Verify**

Run:

```powershell
git diff --check -- tools
mvn -pl tools -DskipTests compile
```

Expected: comment-only diff and compile success, or exact environmental/dependency failure is recorded.

### Task 4: Annotate Transport Startup Modules

**Files:**
- Modify: `transport/*/src/main/java/**/*.java`

**Interfaces:**
- Consumes: Spring Boot app args, protocol-specific component scan packages, common transport, queue and cache modules.
- Produces: Chinese explanations for standalone HTTP/MQTT/CoAP/LwM2M/SNMP transport process startup and config-name defaulting.

- [ ] **Step 1: Add comments**

Document each transport application as a standalone Spring Boot process entry point, including async/scheduling lifecycle, config argument patching, package scanning, MQTT/protocol relation, Actor/Rule Engine indirect flow via transport queue and database/cache boundaries.

- [ ] **Step 2: Verify**

Run:

```powershell
git diff --check -- transport
mvn -pl transport -DskipTests compile
```

Expected: comment-only diff and compile success.

### Task 5: Final Audit And Verification

**Files:**
- Read: all modified Java files under `rest-client`、`tools`、`transport`
- Modify: only comments if audit finds missing required explanations

**Interfaces:**
- Consumes: all previous batches.
- Produces: final verified three-module comment-only change set.

- [ ] **Step 1: Confirm summary coverage and comment-only rule**

Run the global audit command. Expected:

```text
JAVA_COUNT=13
MISSING_SUMMARY_COUNT=0
BAD_ADDED_COUNT=0
REMOVED_COUNT=0
```

- [ ] **Step 2: Check whitespace and scope**

Run:

```powershell
git diff --check -- rest-client tools transport
$names = @(git diff --name-only -- rest-client tools transport 2>$null)
"TARGET_DIFF_FILE_COUNT=$($names.Count)"
$nonJava = @($names | Where-Object { $_ -notmatch '\.java$' })
"TARGET_NON_JAVA_DIFF_COUNT=$($nonJava.Count)"
$nonJava
```

Expected: diff check exit code 0, changed files under target modules are Java files only.

- [ ] **Step 3: Compile**

Run:

```powershell
mvn -pl rest-client,tools,transport -DskipTests compile
```

Expected: compile succeeds, or any environmental/dependency failures are recorded with exact cause.

- [ ] **Step 4: Report completion**

Report:

- Number of Java files annotated.
- Diff shortstat and comment-only audit result.
- Verification commands run and outcomes.
- Any Maven caveats.
- Whether Git commit was skipped because identity is missing.
