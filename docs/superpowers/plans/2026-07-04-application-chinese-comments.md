# Application Chinese Comments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ThingsBoard `application` 模块全部 Java 文件补充高质量中文注释，并保持所有业务代码、变量名、方法签名、导入、注解和原有格式不变。

**Architecture:** 采用与 `rule-engine` 注释任务等效的架构分层推进方式：先建立文件清单和审计规则，再按启动/配置/异常/Actor/Web Controller/Service/Transport 测试等边界分批补充注释，最后做全量 diff 和 Maven 验证。`application` 文件数量较大，执行时允许使用保守语法扫描做批量注释插入，但每批必须用 diff 审计证明只新增 Java 注释。

**Tech Stack:** Java, Spring Boot, ThingsBoard Application, Akka-style Actor, REST Controller, WebSocket, Transport integration tests, Maven, PowerShell, Git.

## Global Constraints

- 只覆盖 `application` 模块下全部 Java 文件。
- 覆盖 `application/src/main/java/**/*.java` 和 `application/src/test/java/**/*.java`。
- 不修改任何业务逻辑。
- 不修改变量名、方法名、方法参数、返回类型、可见性、继承结构、注解、导入、包名。
- 不运行格式化工具，不重排原代码。
- 不删除或替换已有英文注释，只追加中文解释。
- 每个 Java 文件必须追加或补齐“本类总结”。
- 类注释必须说明类存在目的、所属 ThingsBoard 模块、协作模块、生命周期、为什么需要该类、设计模式。
- 字段注释必须说明字段保存的数据、数据来源、生命周期、为什么设计成该字段。
- 方法注释必须说明职责、参数意义、返回值意义、调用时机、调用方、使用流程、线程安全、事务、缓存、MQTT、Actor、数据库、Rule Engine 关系。
- 复杂分支、异步回调、异常处理、消息路由、状态迁移、缓存命中/失效、数据库访问、外部协议调用处增加行内中文注释。
- Git identity 当前可能缺失；若 `git config user.name` 或 `git config user.email` 为空，不创建提交。

---

## File Structure

固定修改范围：

- `application/src/main/java/**/*.java`
- `application/src/test/java/**/*.java`

当前扫描结果：

- `application/src/main/java`: 827 个 Java 文件
- `application/src/test/java`: 266 个 Java 文件
- 合计：1093 个 Java 文件

当前主代码分布：

- `service`: 694
- `controller`: 57
- `actors`: 43
- `config`: 11
- `exception`: 11
- `utils`: 4
- `install`: 3
- `ThingsboardServerApplication.java`: 1
- `ThingsboardInstallApplication.java`: 1
- `org/apache/kafka/common/network/NetworkReceive.java`: 1
- `springfox`: 1

当前测试代码分布：

- `transport`: 137
- `service`: 51
- `controller`: 41
- `edge`: 21
- `system`: 5
- `actors`: 4
- `rules`: 4
- `utils`: 1
- `queue`: 1
- `cache`: 1

注释模板：

```java
/**
 * 中文说明：
 * 1. 职责：说明该类、字段或方法存在的直接目的。
 * 2. 所属模块：说明它在 ThingsBoard Application 中的位置。
 * 3. 协作对象：说明它与 Controller、Service、DAO、缓存、Actor、Queue、Transport、MQTT、Rule Engine 等对象的关系。
 * 4. 生命周期：说明由 Spring、Actor System、Web 请求、队列消费、传输层测试或调用方管理。
 * 5. 设计原因：说明为什么抽象为当前类、字段或方法。
 * 6. 技术关联：明确是否直接涉及事务、缓存、MQTT、Actor 通信、数据库、Rule Engine。
 */
```

文件末尾总结模板：

```java
/*
 * 本类总结：
 * 1. 核心职责：概括本文件主类型承担的业务或技术职责。
 * 2. 核心流程：按调用顺序概括初始化、请求处理、消息处理、回调、清理或测试执行路径。
 * 3. 关键依赖：列出与本文件直接协作的 Spring Bean、DAO、队列、Actor、Transport、缓存或测试工具。
 * 4. 学习重点：指出阅读本文件时最应该关注的 ThingsBoard Application 机制或设计取舍。
 */
```

---

### Task 1: Baseline And Guardrails

**Files:**
- Read: `application/pom.xml`
- Modify: none

**Interfaces:**
- Consumes: 当前 `application` 文件结构。
- Produces: 后续所有批次使用的文件清单、审计命令和验证命令。

- [ ] **Step 1: Confirm Java file count**

Run:

```powershell
$main = @(rg --files application\src\main\java -g '*.java')
$test = @(rg --files application\src\test\java -g '*.java')
"MAIN_COUNT=$($main.Count)"
"TEST_COUNT=$($test.Count)"
"TOTAL=$($main.Count + $test.Count)"
```

Expected:

```text
MAIN_COUNT=827
TEST_COUNT=266
TOTAL=1093
```

- [ ] **Step 2: Capture pre-change status**

Run:

```powershell
git status --short -- application docs\superpowers\plans docs\superpowers\specs
```

Expected: no pre-existing `application` source modifications. Existing `rule-engine` modifications are out of scope and must not be reverted.

- [ ] **Step 3: Establish comment-only audit**

Run after every batch:

```powershell
$bad = @(git diff -- application 2>$null | Where-Object { $_ -like '+*' -and $_ -notlike '+++*' -and $_ -notmatch '^\+\s*(/\*\*?|//|\*|$)' })
$removed = @(git diff -- application 2>$null | Where-Object { $_ -like '-*' -and $_ -notlike '---*' })
"BAD_ADDED_COUNT=$($bad.Count)"
"REMOVED_COUNT=$($removed.Count)"
```

Expected:

```text
BAD_ADDED_COUNT=0
REMOVED_COUNT=0
```

---

### Task 2: Annotate Application Bootstrap, Config, Exceptions, Utils And Install

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/ThingsboardServerApplication.java`
- Modify: `application/src/main/java/org/thingsboard/server/ThingsboardInstallApplication.java`
- Modify: `application/src/main/java/org/thingsboard/server/config/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/exception/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/utils/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/install/**/*.java`
- Modify: `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`
- Modify: `application/src/main/java/springfox/**/*.java`

**Interfaces:**
- Consumes: Spring Boot application lifecycle and ThingsBoard server configuration.
- Produces: Application bootstrap, security, Swagger, CORS, scheduling, install and error-handling terminology for later batches.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files application\src\main\java\org\thingsboard\server\config application\src\main\java\org\thingsboard\server\exception application\src\main\java\org\thingsboard\server\utils application\src\main\java\org\thingsboard\server\install application\src\main\java\org\apache application\src\main\java\springfox -g '*.java' | Sort-Object
```

Expected: bootstrap-adjacent files plus config, exception, utils, install and compatibility classes.

- [ ] **Step 2: Add comments**

For Spring configuration classes, document bean creation lifecycle, injected properties, security filter chain, CORS, OAuth2, Swagger, WebSocket and scheduling boundaries. For exception classes, document HTTP status mapping and response serialization. For install classes, document command-line install lifecycle and database initialization boundaries.

- [ ] **Step 3: Verify**

Run the comment-only audit from Task 1 and:

```powershell
git diff --check -- application
mvn -pl application -DskipTests compile
```

Expected: diff check and compile succeed, or compile failure is recorded exactly if caused by environment dependencies.

---

### Task 3: Annotate Actor And Rule Engine Runtime Bridge

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/actors/**/*.java`

**Interfaces:**
- Consumes: Actor system, tenant/device/rule-chain/rule-node lifecycle.
- Produces: Chinese explanations for Actor communication, Rule Engine message routing, queue callbacks and node context execution.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files application\src\main\java\org\thingsboard\server\actors -g '*.java' | Sort-Object
```

Expected: 43 Java files.

- [ ] **Step 2: Add comments**

For each Actor, processor and message class, comments must explicitly explain actor lifecycle, mailbox/message ownership, tenant/device/rule-chain routing, thread-safety boundaries, database/cache access through services, MQTT/Transport indirect links and Rule Engine involvement.

- [ ] **Step 3: Verify**

Run Task 1 audit and:

```powershell
git diff --check -- application\src\main\java\org\thingsboard\server\actors
mvn -pl application -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 4: Annotate REST Controllers And WebSocket Controller Layer

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/controller/**/*.java`

**Interfaces:**
- Consumes: Spring MVC, security principal, DTO validation, service layer.
- Produces: Chinese explanations for HTTP API entry points and WebSocket plugin message handling.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files application\src\main\java\org\thingsboard\server\controller -g '*.java' | Sort-Object
```

Expected: 57 Java files.

- [ ] **Step 2: Add comments**

For each controller, document API purpose, request/response DTOs, permission checks, tenant/customer/user scope, service/DAO delegation, transaction boundary, cache involvement and whether the endpoint indirectly reaches Actor, MQTT, Transport or Rule Engine flows.

- [ ] **Step 3: Verify**

Run Task 1 audit and:

```powershell
git diff --check -- application\src\main\java\org\thingsboard\server\controller
mvn -pl application -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 5: Annotate Core Service Infrastructure

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/service/apiusage/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/component/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/executors/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/partition/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/queue/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/rule/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/script/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/state/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/stats/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/transport/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/ws/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/install/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/subscription/**/*.java`

**Interfaces:**
- Consumes: Spring service lifecycle, queue APIs, Actor system context, WebSocket sessions and Rule Engine component discovery.
- Produces: Chinese explanations for cross-cutting application runtime services.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files application\src\main\java\org\thingsboard\server\service -g '*.java' | Sort-Object
```

Expected: 694 Java files under service; this task handles infrastructure and small shared packages, while Tasks 6-8 handle security/entity-edge-sync/business groups.

- [ ] **Step 2: Add comments**

Document queue ownership, async callbacks, cache/state services, component discovery, WebSocket session lifecycle, script execution boundary, statistics persistence and transport API delegation.

- [ ] **Step 3: Verify**

Run Task 1 audit and:

```powershell
git diff --check -- application\src\main\java\org\thingsboard\server\service
mvn -pl application -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 6: Annotate Security, Auth, Profile And User-Facing Services

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/service/security/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/profile/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/session/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/mail/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/sms/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/notification/**/*.java`

**Interfaces:**
- Consumes: Spring Security, JWT, OAuth2, 2FA, mail/SMS/Slack/Firebase notification services.
- Produces: Chinese explanations for authentication, authorization, credentials, notification delivery and profile-level service flows.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files application\src\main\java\org\thingsboard\server\service\security application\src\main\java\org\thingsboard\server\service\profile application\src\main\java\org\thingsboard\server\service\session application\src\main\java\org\thingsboard\server\service\mail application\src\main\java\org\thingsboard\server\service\sms application\src\main\java\org\thingsboard\server\service\notification -g '*.java' | Sort-Object
```

Expected: security and user-facing service files.

- [ ] **Step 2: Add comments**

Document token lifecycle, OAuth2 state, password policy, authority checks, user/session ownership, notification template resolution, external mail/SMS/Slack/Firebase boundaries, database/cache involvement and transaction expectations.

- [ ] **Step 3: Verify**

Run Task 1 audit and:

```powershell
git diff --check -- application\src\main\java\org\thingsboard\server\service\security application\src\main\java\org\thingsboard\server\service\notification
mvn -pl application -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 7: Annotate Entity, Telemetry, RPC, Resource And Business Services

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/service/entitiy/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/telemetry/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/rpc/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/resource/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/device/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/asset/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/ota/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/lwm2m/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/gateway_device/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/query/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/ttl/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/update/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/action/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/housekeeper/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/system/**/*.java`

**Interfaces:**
- Consumes: DAO layer, telemetry subsystem, RPC services, resource storage and entity lifecycle services.
- Produces: Chinese explanations for business service orchestration and persistent state changes.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files application\src\main\java\org\thingsboard\server\service\entitiy application\src\main\java\org\thingsboard\server\service\telemetry application\src\main\java\org\thingsboard\server\service\rpc application\src\main\java\org\thingsboard\server\service\resource application\src\main\java\org\thingsboard\server\service\device application\src\main\java\org\thingsboard\server\service\asset application\src\main\java\org\thingsboard\server\service\ota application\src\main\java\org\thingsboard\server\service\lwm2m application\src\main\java\org\thingsboard\server\service\gateway_device application\src\main\java\org\thingsboard\server\service\query application\src\main\java\org\thingsboard\server\service\ttl application\src\main\java\org\thingsboard\server\service\update application\src\main\java\org\thingsboard\server\service\action application\src\main\java\org\thingsboard\server\service\housekeeper application\src\main\java\org\thingsboard\server\service\system -g '*.java' | Sort-Object
```

Expected: entity and business orchestration service files.

- [ ] **Step 2: Add comments**

Document entity validation, DAO delegation, cache invalidation, audit/event publication, telemetry persistence, RPC routing, LwM2M and gateway device boundaries, transaction expectations and Rule Engine side effects.

- [ ] **Step 3: Verify**

Run Task 1 audit and:

```powershell
git diff --check -- application\src\main\java\org\thingsboard\server\service
mvn -pl application -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 8: Annotate Edge And Sync Services

**Files:**
- Modify: `application/src/main/java/org/thingsboard/server/service/edge/**/*.java`
- Modify: `application/src/main/java/org/thingsboard/server/service/sync/**/*.java`

**Interfaces:**
- Consumes: Edge event model, Edge RPC constructors/processors, version-control synchronization.
- Produces: Chinese explanations for cloud-to-edge synchronization, protocol-version branching and entity replication.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files application\src\main\java\org\thingsboard\server\service\edge application\src\main\java\org\thingsboard\server\service\sync -g '*.java' | Sort-Object
```

Expected: 270 Java files.

- [ ] **Step 2: Add comments**

Document Edge RPC constructor/processor Factory and Strategy patterns, protobuf message mapping, cloud/edge routing, sync event sourcing, database and queue side effects, thread-safety and version compatibility decisions.

- [ ] **Step 3: Verify**

Run Task 1 audit and:

```powershell
git diff --check -- application\src\main\java\org\thingsboard\server\service\edge application\src\main\java\org\thingsboard\server\service\sync
mvn -pl application -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 9: Annotate Application Tests

**Files:**
- Modify: `application/src/test/java/**/*.java`

**Interfaces:**
- Consumes: production code annotations from Tasks 2-8.
- Produces: Chinese explanations for integration tests, transport tests, controller tests, service tests and edge sync tests.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files application\src\test\java -g '*.java' | Sort-Object
```

Expected: 266 Java files.

- [ ] **Step 2: Add comments**

For every test class, document tested component, mock/fixture source, expected flow and why the test exists. For every test method, document input data, expected output, invocation timing, and whether database, cache, MQTT, Actor, Rule Engine or transport protocol is involved.

- [ ] **Step 3: Verify**

Run Task 1 audit and:

```powershell
git diff --check -- application\src\test\java
mvn -pl application -DskipTests test-compile
```

Expected: comment-only diff and test compilation success.

---

### Task 10: Final Audit

**Files:**
- Read: all modified Java files under `application`
- Modify: only comments if audit finds missing required explanations

**Interfaces:**
- Consumes: all previous batches.
- Produces: final verified `application` comment-only change set.

- [ ] **Step 1: Confirm all application Java files are present**

Run:

```powershell
rg --files application -g '*.java' | Measure-Object | Select-Object -ExpandProperty Count
```

Expected: `1093`.

- [ ] **Step 2: Confirm summary coverage**

Run:

```powershell
$files = @(rg --files application -g '*.java')
$missing = @($files | Where-Object { -not (Select-String -LiteralPath $_ -Pattern '本类总结' -SimpleMatch -Quiet) })
"MISSING_SUMMARY_COUNT=$($missing.Count)"
$missing
```

Expected:

```text
MISSING_SUMMARY_COUNT=0
```

- [ ] **Step 3: Check comment-only rule**

Run:

```powershell
$bad = @(git diff -- application 2>$null | Where-Object { $_ -like '+*' -and $_ -notlike '+++*' -and $_ -notmatch '^\+\s*(/\*\*?|//|\*|$)' })
$removed = @(git diff -- application 2>$null | Where-Object { $_ -like '-*' -and $_ -notlike '---*' })
"BAD_ADDED_COUNT=$($bad.Count)"
"REMOVED_COUNT=$($removed.Count)"
$bad | Select-Object -First 40
$removed | Select-Object -First 40
```

Expected:

```text
BAD_ADDED_COUNT=0
REMOVED_COUNT=0
```

- [ ] **Step 4: Check whitespace and scope**

Run:

```powershell
git diff --check -- application
$names = @(git diff --name-only -- application 2>$null)
"APPLICATION_DIFF_FILE_COUNT=$($names.Count)"
$nonJava = @($names | Where-Object { $_ -notmatch '\.java$' })
"APPLICATION_NON_JAVA_DIFF_COUNT=$($nonJava.Count)"
$nonJava
```

Expected: diff check exit code 0, changed files under `application` are Java files only.

- [ ] **Step 5: Run final module verification**

Run:

```powershell
mvn -pl application -DskipTests compile
mvn -pl application -DskipTests test-compile
```

Expected: both commands succeed. Full `mvn -pl application test` may require integration-test infrastructure; if not run, record why.

- [ ] **Step 6: Report completion**

Report:

- Number of Java files annotated.
- Verification commands run.
- Any tests not run and why.
- Whether Git commit was skipped because identity is missing.
