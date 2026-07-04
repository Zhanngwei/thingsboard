# Monitoring MSA Netty MQTT Chinese Comments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ThingsBoard `monitoring`、`msa`、`netty-mqtt` 三个模块下全部 Java 文件补充高质量中文注释，并保持所有业务代码、变量名、方法签名、导入、注解和原有格式不变。

**Architecture:** 沿用 `rule-engine`、`application`、`common` 和 `dao` 的 comment-only 做法：先固定文件清单与审计规则，再用三模块专用注释模板批量插入类、字段、方法、复杂分支和“本类总结”说明，最后通过 diff 审计与 Maven 编译验证。`monitoring` 侧重运行时健康探测和通知，`msa` 侧重微服务部署/黑盒测试/版本控制执行器，`netty-mqtt` 侧重 MQTT 客户端协议状态机与 Netty 通道生命周期。

**Tech Stack:** Java, Maven, Spring Boot, Netty, MQTT, TestNG/JUnit, Selenium, Testcontainers, Docker Compose, ThingsBoard Monitoring, ThingsBoard MSA, PowerShell, Git.

## Global Constraints

- 只覆盖 `monitoring`、`msa`、`netty-mqtt` 模块下全部 Java 文件。
- 覆盖 `monitoring/src/main/java/**/*.java`、`msa/**/src/main/java/**/*.java`、`msa/**/src/test/java/**/*.java`、`netty-mqtt/src/main/java/**/*.java`、`netty-mqtt/src/test/java/**/*.java`。
- 不修改任何业务逻辑。
- 不修改变量名、方法名、方法参数、返回类型、可见性、继承结构、注解、导入或包名。
- 不运行格式化工具，不重排原代码。
- 不删除或替换已有英文注释，只追加中文解释。
- 每个 Java 文件必须追加或补齐“本类总结”。
- 类注释说明类目的、所属 ThingsBoard 模块、协作模块、生命周期、为什么需要该类、设计模式。
- 字段注释说明字段保存的数据、数据来源、生命周期、为什么设计成该字段。
- 方法注释说明职责、参数、返回值、调用时机、调用方、使用流程、线程安全、事务、缓存、MQTT、Actor、数据库、Rule Engine 关系。
- 复杂分支、异步回调、异常处理、消息路由、MQTT 状态迁移、WebSocket/HTTP/CoAP/LwM2M 探测、Selenium 页面操作、Docker/Testcontainers 生命周期处增加行内中文注释。
- 当前工作区已有 `rule-engine`、`application`、`common`、`dao` 注释改动；执行本计划时不得回滚、重排或修改这些范围。
- 如果 `git config user.name` 或 `git config user.email` 为空，不创建提交。

---

## File Structure

固定修改范围：
- `monitoring/**/*.java`
- `msa/**/*.java`
- `netty-mqtt/**/*.java`

当前扫描结果：
- `monitoring`: 42 个 Java 文件，全部为 main 源码。
- `msa`: 114 个 Java 文件，其中 `vc-executor` 3 个 main 源码，`black-box-tests` 111 个 test 源码。
- `netty-mqtt`: 22 个 Java 文件，其中 17 个 main 源码，5 个 test 源码。
- 合计：178 个 Java 文件，main 62 个，test 116 个。

全局审计命令模板：
```powershell
$roots = @('monitoring','msa','netty-mqtt')
$files = @($roots | ForEach-Object { rg --files $_ -g '*.java' })
$missing = @($files | Where-Object { -not (Select-String -LiteralPath $_ -Pattern '本类总结' -SimpleMatch -Quiet) })
$bad = @(git diff -- monitoring msa netty-mqtt 2>$null | Where-Object { $_ -like '+*' -and $_ -notlike '+++*' -and $_ -notmatch '^\+\s*(/\*\*?|//|\*|$)' })
$removed = @(git diff -- monitoring msa netty-mqtt 2>$null | Where-Object { $_ -like '-*' -and $_ -notlike '---*' })
git diff --check -- monitoring msa netty-mqtt
"JAVA_COUNT=$($files.Count)"
"MISSING_SUMMARY_COUNT=$($missing.Count)"
"BAD_ADDED_COUNT=$($bad.Count)"
"REMOVED_COUNT=$($removed.Count)"
```

Expected:
```text
JAVA_COUNT=178
MISSING_SUMMARY_COUNT=0
BAD_ADDED_COUNT=0
REMOVED_COUNT=0
```

### Task 1: Baseline And Guardrails

**Files:**
- Read: `monitoring/pom.xml`
- Read: `msa/pom.xml`
- Read: `msa/vc-executor/pom.xml`
- Read: `msa/black-box-tests/pom.xml`
- Read: `netty-mqtt/pom.xml`
- Modify: none

**Interfaces:**
- Consumes: 当前三个模块 Maven 结构、Java 文件清单、Git 工作区状态。
- Produces: 后续任务使用的固定范围、审计命令和验证命令。

- [ ] **Step 1: Confirm Java file count**

Run:
```powershell
$roots = @('monitoring','msa','netty-mqtt')
$files = @($roots | ForEach-Object { rg --files $_ -g '*.java' })
$main = @($files | Where-Object { $_ -match '\\src\\main\\java\\' })
$test = @($files | Where-Object { $_ -match '\\src\\test\\java\\' })
"MAIN_COUNT=$($main.Count)"
"TEST_COUNT=$($test.Count)"
"TOTAL=$($files.Count)"
```

Expected:
```text
MAIN_COUNT=62
TEST_COUNT=116
TOTAL=178
```

- [ ] **Step 2: Capture pre-change status**

Run:
```powershell
git status --short -- monitoring msa netty-mqtt docs\superpowers\plans docs\superpowers\specs
```

Expected: no pre-existing `monitoring`、`msa`、`netty-mqtt` source modifications.

### Task 2: Annotate Monitoring Module

**Files:**
- Modify: `monitoring/src/main/java/**/*.java`

**Interfaces:**
- Consumes: monitoring config, transport health checkers, ThingsBoard REST/WebSocket clients and notification channels.
- Produces: Chinese explanations for health-check lifecycle, latency reporting, protocol probes and alert notifications.

- [ ] **Step 1: List files**

Run:
```powershell
rg --files monitoring -g '*.java' | Sort-Object
```

Expected: 42 Java files.

- [ ] **Step 2: Add comments**

Document REST/WebSocket/MQTT/HTTP/CoAP/LwM2M probe flow, target config lifecycle, latency collection, Slack notification behavior, thread safety and indirect impact on operational monitoring.

- [ ] **Step 3: Verify**

Run global comment-only audit and:
```powershell
git diff --check -- monitoring
mvn -pl monitoring -DskipTests compile
```

Expected: comment-only diff and compile success.

### Task 3: Annotate MSA Java Modules

**Files:**
- Modify: `msa/vc-executor/src/main/java/**/*.java`
- Modify: `msa/black-box-tests/src/test/java/**/*.java`

**Interfaces:**
- Consumes: microservice Docker Compose environment, Selenium UI pages, TestNG/JUnit tests, REST clients, protocol clients and version-control queue routing services.
- Produces: Chinese explanations for MSA black-box test lifecycle and version-control executor routing.

- [ ] **Step 1: List files**

Run:
```powershell
rg --files msa -g '*.java' | Sort-Object
```

Expected: 114 Java files.

- [ ] **Step 2: Add comments**

Document container startup, Docker Compose executor lifecycle, REST/MQTT/CoAP/HTTP connectivity tests, Selenium page-object flows, retry/listener behavior, tenant/device/rule-chain fixture data and VC executor queue routing semantics.

- [ ] **Step 3: Verify**

Run global comment-only audit and:
```powershell
git diff --check -- msa
mvn -f msa/pom.xml -pl vc-executor -DskipTests compile
mvn -f msa/pom.xml -pl black-box-tests -DskipTests test-compile
```

Expected: comment-only diff and compile/test-compile success, or exact environmental/dependency failures are recorded.

### Task 4: Annotate Netty MQTT Module

**Files:**
- Modify: `netty-mqtt/src/main/java/**/*.java`
- Modify: `netty-mqtt/src/test/java/**/*.java`

**Interfaces:**
- Consumes: Netty channel pipeline, MQTT packets, pending publish/subscription state and integration test server.
- Produces: Chinese explanations for MQTT client protocol lifecycle, retransmission, ping, QoS state and integration testing.

- [ ] **Step 1: List files**

Run:
```powershell
rg --files netty-mqtt -g '*.java' | Sort-Object
```

Expected: 22 Java files.

- [ ] **Step 2: Add comments**

Document CONNECT/CONNACK, publish QoS state, pending operations, retransmission, ping timeout, callback threading, Netty handler lifecycle and integration test broker behavior.

- [ ] **Step 3: Verify**

Run global comment-only audit and:
```powershell
git diff --check -- netty-mqtt
mvn -pl netty-mqtt -DskipTests compile
mvn -pl netty-mqtt -DskipTests test-compile
```

Expected: comment-only diff and compile/test-compile success.

### Task 5: Final Audit And Maven Verification

**Files:**
- Read: all modified Java files under `monitoring`、`msa`、`netty-mqtt`
- Modify: only comments if audit finds missing required explanations

**Interfaces:**
- Consumes: all previous batches.
- Produces: final verified three-module comment-only change set.

- [ ] **Step 1: Confirm summary coverage and comment-only rule**

Run:
```powershell
$roots = @('monitoring','msa','netty-mqtt')
$files = @($roots | ForEach-Object { rg --files $_ -g '*.java' })
$missing = @($files | Where-Object { -not (Select-String -LiteralPath $_ -Pattern '本类总结' -SimpleMatch -Quiet) })
$bad = @(git diff -- monitoring msa netty-mqtt 2>$null | Where-Object { $_ -like '+*' -and $_ -notlike '+++*' -and $_ -notmatch '^\+\s*(/\*\*?|//|\*|$)' })
$removed = @(git diff -- monitoring msa netty-mqtt 2>$null | Where-Object { $_ -like '-*' -and $_ -notlike '---*' })
"JAVA_COUNT=$($files.Count)"
"MISSING_SUMMARY_COUNT=$($missing.Count)"
"BAD_ADDED_COUNT=$($bad.Count)"
"REMOVED_COUNT=$($removed.Count)"
```

Expected:
```text
JAVA_COUNT=178
MISSING_SUMMARY_COUNT=0
BAD_ADDED_COUNT=0
REMOVED_COUNT=0
```

- [ ] **Step 2: Check whitespace and scope**

Run:
```powershell
git diff --check -- monitoring msa netty-mqtt
$names = @(git diff --name-only -- monitoring msa netty-mqtt 2>$null)
"TARGET_DIFF_FILE_COUNT=$($names.Count)"
$nonJava = @($names | Where-Object { $_ -notmatch '\.java$' })
"TARGET_NON_JAVA_DIFF_COUNT=$($nonJava.Count)"
$nonJava
```

Expected: diff check exit code 0, changed files under target modules are Java files only.

- [ ] **Step 3: Compile and test-compile**

Run:
```powershell
mvn -pl monitoring,netty-mqtt -DskipTests compile
mvn -pl netty-mqtt -DskipTests test-compile
mvn -f msa/pom.xml -pl vc-executor -DskipTests compile
mvn -f msa/pom.xml -pl black-box-tests -DskipTests test-compile
```

Expected: compile and test-compile succeed, or any environmental failures are recorded with exact cause.

- [ ] **Step 4: Optional full tests**

Run if feasible:
```powershell
mvn -pl netty-mqtt test
mvn -f msa/pom.xml -pl black-box-tests test
```

Expected: pass, or record exact environmental failure such as Docker/Testcontainers, browser/Selenium, port conflict, network, certificate or external service assumptions.

- [ ] **Step 5: Report completion**

Report:
- Number of Java files annotated.
- Diff shortstat and comment-only audit result.
- Verification commands run and outcomes.
- Any full-test caveats.
- Whether Git commit was skipped because identity is missing.
