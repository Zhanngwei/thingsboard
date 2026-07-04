# Common Chinese Comments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ThingsBoard `common` 聚合模块下全部 Java 文件补充高质量中文注释，并保持所有业务代码、变量名、方法签名、导入、注解和原有格式不变。

**Architecture:** 沿用 `rule-engine` 和 `application` 的做法：先固定文件清单和 comment-only 审计规则，再按 Maven 子模块分批注释，最后做全量覆盖、diff 和 Maven 验证。`common` 是多模块基础设施层，注释重点覆盖数据模型、队列、传输协议、Actor、缓存、DAO API、消息协议、脚本和版本控制等跨模块边界。

**Tech Stack:** Java, Maven, ThingsBoard Common modules, DTO, Queue, Transport, Actor, Cache, DAO API, Proto, PowerShell, Git.

## Global Constraints

- 只覆盖 `common` 模块下全部 Java 文件。
- 覆盖 `common/**/src/main/java/**/*.java` 和 `common/**/src/test/java/**/*.java`。
- 不修改任何业务逻辑。
- 不修改变量名、方法名、方法参数、返回类型、可见性、继承结构、注解、导入、包名。
- 不运行格式化工具，不重排原代码。
- 不删除或替换已有英文注释，只追加中文解释。
- 每个 Java 文件必须追加或补齐“本类总结”。
- 类注释必须说明类存在目的、所属 ThingsBoard 模块、协作模块、生命周期、为什么需要该类、设计模式。
- 字段注释必须说明字段保存的数据、数据来源、生命周期、为什么设计成该字段。
- 方法注释必须说明职责、参数意义、返回值意义、调用时机、调用方、使用流程、线程安全、事务、缓存、MQTT、Actor、数据库、Rule Engine 关系。
- 复杂分支、异步回调、异常处理、消息路由、状态迁移、缓存命中/失效、数据库访问、外部协议调用处增加行内中文注释。
- 当前工作区已有 `rule-engine` 和 `application` 注释改动；执行本计划时不得回滚、重排或修改这些范围。
- Git identity 当前可能缺失；若 `git config user.name` 或 `git config user.email` 为空，不创建提交。

---

## File Structure

固定修改范围：

- `common/**/src/main/java/**/*.java`
- `common/**/src/test/java/**/*.java`

当前扫描结果：

- `common/**/src/main/java`: 1360 个 Java 文件
- `common/**/src/test/java`: 58 个 Java 文件
- 合计：1418 个 Java 文件

当前子模块分布：

- `data`: 645
- `transport`: 323
- `queue`: 140
- `dao-api`: 92
- `message`: 61
- `cache`: 34
- `script`: 29
- `actor`: 26
- `util`: 19
- `cluster-api`: 13
- `stats`: 10
- `coap-server`: 9
- `proto`: 7
- `version-control`: 7
- `edge-api`: 3

审计命令模板：

```powershell
$bad = @(git diff -- common 2>$null | Where-Object { $_ -like '+*' -and $_ -notlike '+++*' -and $_ -notmatch '^\+\s*(/\*\*?|//|\*|$)' })
$removed = @(git diff -- common 2>$null | Where-Object { $_ -like '-*' -and $_ -notlike '---*' })
"BAD_ADDED_COUNT=$($bad.Count)"
"REMOVED_COUNT=$($removed.Count)"
```

Expected:

```text
BAD_ADDED_COUNT=0
REMOVED_COUNT=0
```

---

### Task 1: Baseline And Guardrails

**Files:**
- Read: `common/pom.xml`
- Modify: none

**Interfaces:**
- Consumes: 当前 `common` Maven 聚合结构。
- Produces: 后续所有批次使用的文件清单、审计命令和验证命令。

- [ ] **Step 1: Confirm Java file count**

Run:

```powershell
$main = @(rg --files common -g '*.java' | Where-Object { $_ -match '\\src\\main\\java\\' })
$test = @(rg --files common -g '*.java' | Where-Object { $_ -match '\\src\\test\\java\\' })
"MAIN_COUNT=$($main.Count)"
"TEST_COUNT=$($test.Count)"
"TOTAL=$($main.Count + $test.Count)"
```

Expected:

```text
MAIN_COUNT=1360
TEST_COUNT=58
TOTAL=1418
```

- [ ] **Step 2: Capture pre-change status**

Run:

```powershell
git status --short -- common docs\superpowers\plans docs\superpowers\specs
```

Expected: no pre-existing `common` source modifications.

---

### Task 2: Annotate Data Module

**Files:**
- Modify: `common/data/src/main/java/**/*.java`
- Modify: `common/data/src/test/java/**/*.java`

**Interfaces:**
- Consumes: ThingsBoard common data model, IDs, DTOs, queries, device profiles, notification models and security DTOs.
- Produces: Chinese explanations for the data contracts used by DAO, REST, Transport, Rule Engine and Edge sync.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files common\data -g '*.java' | Sort-Object
```

Expected: 645 Java files.

- [ ] **Step 2: Add comments**

Document DTO lifecycle, serialization/deserialization sources, validation boundaries, tenant/customer/entity scope, database persistence relationship, cache keys and Rule Engine/Transport usage.

- [ ] **Step 3: Verify**

Run the global comment-only audit and:

```powershell
git diff --check -- common\data
mvn -pl common/data -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 3: Annotate Transport Modules

**Files:**
- Modify: `common/transport/transport-api/src/main/java/**/*.java`
- Modify: `common/transport/mqtt/src/main/java/**/*.java`
- Modify: `common/transport/coap/src/main/java/**/*.java`
- Modify: `common/transport/lwm2m/src/main/java/**/*.java`
- Modify: `common/transport/http/src/main/java/**/*.java`
- Modify: `common/transport/snmp/src/main/java/**/*.java`
- Modify: related `src/test/java` files under `common/transport`.

**Interfaces:**
- Consumes: MQTT, HTTP, CoAP, LwM2M, SNMP and shared transport API contracts.
- Produces: Chinese explanations for device-session lifecycle, protocol adaptation, credentials, telemetry, attributes and RPC transport boundaries.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files common\transport -g '*.java' | Sort-Object
```

Expected: 323 Java files.

- [ ] **Step 2: Add comments**

Document protocol-specific message mapping, MQTT topic/QoS/session semantics, CoAP/LwM2M/SNMP request handling, transport-to-core queue handoff, Actor/Rule Engine indirect effects and thread-safety boundaries.

- [ ] **Step 3: Verify**

Run the global audit and:

```powershell
git diff --check -- common\transport
mvn -pl common/transport -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 4: Annotate Queue, Cluster API And Message Modules

**Files:**
- Modify: `common/queue/src/main/java/**/*.java`
- Modify: `common/queue/src/test/java/**/*.java`
- Modify: `common/cluster-api/src/main/java/**/*.java`
- Modify: `common/message/src/main/java/**/*.java`
- Modify: `common/message/src/test/java/**/*.java`

**Interfaces:**
- Consumes: queue producers/consumers, transport/core/rule-engine messages and cluster callbacks.
- Produces: Chinese explanations for asynchronous messaging contracts and serialization boundaries.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files common\queue common\cluster-api common\message -g '*.java' | Sort-Object
```

Expected: queue, cluster-api and message Java files.

- [ ] **Step 2: Add comments**

Document producer/consumer lifecycle, acknowledgements, callback threading, partitioning, protobuf/JSON serialization, message headers, MQTT/Transport/Actor/Rule Engine routing relationships.

- [ ] **Step 3: Verify**

Run the global audit and:

```powershell
git diff --check -- common\queue common\cluster-api common\message
mvn -pl common/queue,common/cluster-api,common/message -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 5: Annotate Actor, Cache, DAO API And CoAP Server

**Files:**
- Modify: `common/actor/src/main/java/**/*.java`
- Modify: `common/actor/src/test/java/**/*.java`
- Modify: `common/cache/src/main/java/**/*.java`
- Modify: `common/cache/src/test/java/**/*.java`
- Modify: `common/dao-api/src/main/java/**/*.java`
- Modify: `common/coap-server/src/main/java/**/*.java`

**Interfaces:**
- Consumes: Actor framework, cache abstraction, DAO service interfaces and CoAP server adapter.
- Produces: Chinese explanations for core runtime infrastructure shared by application and rule engine.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files common\actor common\cache common\dao-api common\coap-server -g '*.java' | Sort-Object
```

Expected: actor, cache, DAO API and CoAP server Java files.

- [ ] **Step 2: Add comments**

Document Actor mailbox lifecycle, cache transaction semantics, Redis/Caffeine differences, DAO API persistence boundaries, Cassandra helper contracts, CoAP DTLS/session lifecycle and thread-safety.

- [ ] **Step 3: Verify**

Run the global audit and:

```powershell
git diff --check -- common\actor common\cache common\dao-api common\coap-server
mvn -pl common/actor,common/cache,common/dao-api,common/coap-server -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 6: Annotate Utility, Stats, Edge API, Version Control, Script And Proto Modules

**Files:**
- Modify: `common/util/src/main/java/**/*.java`
- Modify: `common/util/src/test/java/**/*.java`
- Modify: `common/stats/src/main/java/**/*.java`
- Modify: `common/edge-api/src/main/java/**/*.java`
- Modify: `common/version-control/src/main/java/**/*.java`
- Modify: `common/script/**/*.java`
- Modify: `common/proto/src/main/java/**/*.java`
- Modify: `common/proto/src/test/java/**/*.java`

**Interfaces:**
- Consumes: shared utilities, stats, edge contracts, version-control DTOs, script engine APIs and proto helper classes.
- Produces: Chinese explanations for shared helper contracts used across server modules.

- [ ] **Step 1: List files**

Run:

```powershell
rg --files common\util common\stats common\edge-api common\version-control common\script common\proto -g '*.java' | Sort-Object
```

Expected: utility, stats, edge-api, version-control, script and proto Java files.

- [ ] **Step 2: Add comments**

Document utility purity/side effects, stats collection lifecycle, edge DTO boundaries, version-control serialization, script invocation APIs and proto conversion helpers.

- [ ] **Step 3: Verify**

Run the global audit and:

```powershell
git diff --check -- common\util common\stats common\edge-api common\version-control common\script common\proto
mvn -pl common/util,common/stats,common/edge-api,common/version-control,common/script,common/proto -DskipTests compile
```

Expected: comment-only diff and compile success.

---

### Task 7: Final Audit

**Files:**
- Read: all modified Java files under `common`
- Modify: only comments if audit finds missing required explanations

**Interfaces:**
- Consumes: all previous batches.
- Produces: final verified `common` comment-only change set.

- [ ] **Step 1: Confirm Java count and summary coverage**

Run:

```powershell
$files = @(rg --files common -g '*.java')
$missing = @($files | Where-Object { -not (Select-String -LiteralPath $_ -Pattern '本类总结' -SimpleMatch -Quiet) })
"JAVA_COUNT=$($files.Count)"
"MISSING_SUMMARY_COUNT=$($missing.Count)"
$missing
```

Expected:

```text
JAVA_COUNT=1418
MISSING_SUMMARY_COUNT=0
```

- [ ] **Step 2: Check comment-only rule**

Run:

```powershell
$bad = @(git diff -- common 2>$null | Where-Object { $_ -like '+*' -and $_ -notlike '+++*' -and $_ -notmatch '^\+\s*(/\*\*?|//|\*|$)' })
$removed = @(git diff -- common 2>$null | Where-Object { $_ -like '-*' -and $_ -notlike '---*' })
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

- [ ] **Step 3: Check whitespace, scope and compile**

Run:

```powershell
git diff --check -- common
$names = @(git diff --name-only -- common 2>$null)
"COMMON_DIFF_FILE_COUNT=$($names.Count)"
$nonJava = @($names | Where-Object { $_ -notmatch '\.java$' })
"COMMON_NON_JAVA_DIFF_COUNT=$($nonJava.Count)"
$nonJava
mvn -pl common -DskipTests compile
mvn -pl common -DskipTests test-compile
```

Expected: diff check exit code 0, changed files under `common` are Java files only, compile and test-compile succeed.

- [ ] **Step 4: Report completion**

Report:

- Number of Java files annotated.
- Verification commands run.
- Any tests not run and why.
- Whether Git commit was skipped because identity is missing.
