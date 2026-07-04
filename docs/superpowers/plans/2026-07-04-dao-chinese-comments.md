# DAO Chinese Comments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ThingsBoard `dao` 模块下全部 Java 文件补充高质量中文注释，并保持所有业务代码、变量名、方法签名、导入、注解和原有格式不变。

**Architecture:** 沿用 `rule-engine`、`application` 和 `common` 的 comment-only 做法：先固定文件清单与审计规则，再用 dao 专用注释模板批量插入类、字段、方法、复杂分支和“本类总结”说明，最后通过 diff 审计与 Maven 编译验证。`dao` 是 ThingsBoard 持久化实现层，注释重点覆盖 SQL/JPA、Cassandra/NoSQL、时序数据、缓存、事务、实体服务和测试夹具之间的边界。

**Tech Stack:** Java, Maven, Spring, JPA/Hibernate, PostgreSQL, TimescaleDB, Cassandra, Redis/Caffeine Cache, ThingsBoard DAO, PowerShell, Git.

## Global Constraints

- 只覆盖 `dao` 模块下全部 Java 文件。
- 覆盖 `dao/src/main/java/**/*.java` 和 `dao/src/test/java/**/*.java`。
- 不修改任何业务逻辑。
- 不修改变量名、方法名、方法参数、返回类型、可见性、继承结构、注解、导入或包名。
- 不运行格式化工具，不重排原代码。
- 不删除或替换已有英文注释，只追加中文解释。
- 每个 Java 文件必须追加或补齐“本类总结”。
- 类注释说明类目的、所属 ThingsBoard 模块、协作模块、生命周期、为什么需要该类、设计模式。
- 字段注释说明字段保存的数据、数据来源、生命周期、为什么设计成该字段。
- 方法注释说明职责、参数、返回值、调用时机、调用方、使用流程、线程安全、事务、缓存、MQTT、Actor、数据库、Rule Engine 关系。
- 复杂分支、异步回调、异常处理、消息路由、缓存命中/失效、数据库访问、事务边界、SQL/Cassandra/Timescale 查询处增加行内中文注释。
- 当前工作区已有 `rule-engine`、`application`、`common` 注释改动；执行本计划时不得回滚、重排或修改这些范围。
- 如果 `git config user.name` 或 `git config user.email` 为空，不创建提交。

---

## File Structure

固定修改范围：
- `dao/src/main/java/**/*.java`
- `dao/src/test/java/**/*.java`

当前扫描结果：
- `dao/src/main/java`: 533 个 Java 文件
- `dao/src/test/java`: 106 个 Java 文件
- 合计：639 个 Java 文件

主要包分布：
- `sql`: 161
- `service`: 102
- `model`: 91
- `sqlts`: 23
- `timeseries`: 22
- `util`: 20
- `device`: 17
- `tenant`: 14
- `asset`: 13
- `notification`: 12
- `entity`: 11
- `oauth2`: 10
- `user`: 9
- `edge`: 9
- `audit`: 9
- 其余领域包：alarm、attributes、relation、resource、rule、event、queue、cache、test suite 等。

全局审计命令模板：
```powershell
$files = @(rg --files dao -g '*.java')
$missing = @($files | Where-Object { -not (Select-String -LiteralPath $_ -Pattern '本类总结' -SimpleMatch -Quiet) })
$bad = @(git diff -- dao 2>$null | Where-Object { $_ -like '+*' -and $_ -notlike '+++*' -and $_ -notmatch '^\+\s*(/\*\*?|//|\*|$)' })
$removed = @(git diff -- dao 2>$null | Where-Object { $_ -like '-*' -and $_ -notlike '---*' })
git diff --check -- dao
"JAVA_COUNT=$($files.Count)"
"MISSING_SUMMARY_COUNT=$($missing.Count)"
"BAD_ADDED_COUNT=$($bad.Count)"
"REMOVED_COUNT=$($removed.Count)"
```

Expected:
```text
JAVA_COUNT=639
MISSING_SUMMARY_COUNT=0
BAD_ADDED_COUNT=0
REMOVED_COUNT=0
```

### Task 1: Baseline And Guardrails

**Files:**
- Read: `dao/pom.xml`
- Modify: none

**Interfaces:**
- Consumes: 当前 `dao` Maven 模块、Java 文件清单、Git 工作区状态。
- Produces: 后续任务使用的固定范围、审计命令和验证命令。

- [ ] **Step 1: Confirm Java file count**

Run:
```powershell
$files = @(rg --files dao -g '*.java')
$main = @($files | Where-Object { $_ -match '\\src\\main\\java\\' })
$test = @($files | Where-Object { $_ -match '\\src\\test\\java\\' })
"MAIN_COUNT=$($main.Count)"
"TEST_COUNT=$($test.Count)"
"TOTAL=$($files.Count)"
```

Expected:
```text
MAIN_COUNT=533
TEST_COUNT=106
TOTAL=639
```

- [ ] **Step 2: Capture pre-change status**

Run:
```powershell
git status --short -- dao docs\superpowers\plans docs\superpowers\specs
```

Expected: no pre-existing `dao` source modifications.

### Task 2: Annotate SQL, JPA And Entity Model Layer

**Files:**
- Modify: `dao/src/main/java/org/thingsboard/server/dao/sql/**/*.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/model/**/*.java`
- Modify: related root SQL/JPA config files under `dao/src/main/java/org/thingsboard/server/dao`.

**Interfaces:**
- Consumes: JPA entities, repositories, SQL DAO implementations, PostgreSQL/Timescale configuration.
- Produces: Chinese explanations for persistence mapping, repository delegation, transaction boundaries, SQL query behavior and entity lifecycle.

- [ ] **Step 1: List files**

Run:
```powershell
rg --files dao/src/main/java/org/thingsboard/server/dao/sql dao/src/main/java/org/thingsboard/server/dao/model -g '*.java' | Sort-Object
```

Expected: SQL and model files are listed.

- [ ] **Step 2: Add comments**

Document JPA entity mapping, repository contracts, SQL DAO query responsibilities, transaction participation, cache invalidation relationships and why SQL-specific classes exist separately from DAO API interfaces.

- [ ] **Step 3: Verify**

Run global comment-only audit and:
```powershell
git diff --check -- dao
```

Expected: comment-only diff and whitespace check success.

### Task 3: Annotate Service And Domain DAO Layer

**Files:**
- Modify: `dao/src/main/java/org/thingsboard/server/dao/service/**/*.java`
- Modify: domain service packages under `dao/src/main/java/org/thingsboard/server/dao/{alarm,asset,attributes,audit,customer,dashboard,device,edge,entity,entityview,event,notification,oauth2,ota,queue,relation,resource,rpc,rule,settings,tenant,usage,usagerecord,user,widget}/**/*.java`

**Interfaces:**
- Consumes: DAO API contracts, SQL/NoSQL DAO implementations, cache services, audit services, event services and domain DTOs from `common`.
- Produces: Chinese explanations for application-facing persistence services and cross-domain validation flows.

- [ ] **Step 1: List files**

Run:
```powershell
rg --files dao/src/main/java/org/thingsboard/server/dao/service dao/src/main/java/org/thingsboard/server/dao/device dao/src/main/java/org/thingsboard/server/dao/tenant -g '*.java' | Sort-Object
```

Expected: service and representative domain files are listed.

- [ ] **Step 2: Add comments**

Document validation, tenant/customer scoping, lifecycle of save/delete/query operations, cache eviction, audit logging, entity relation checks, Rule Engine metadata impact and transaction boundaries.

- [ ] **Step 3: Verify**

Run global comment-only audit and:
```powershell
git diff --check -- dao
```

Expected: comment-only diff and whitespace check success.

### Task 4: Annotate Timeseries, NoSQL, Eventsourcing, Cache, Aspect And Utility Layer

**Files:**
- Modify: `dao/src/main/java/org/thingsboard/server/dao/timeseries/**/*.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/sqlts/**/*.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/nosql/**/*.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/eventsourcing/**/*.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/cache/**/*.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/aspect/**/*.java`
- Modify: `dao/src/main/java/org/thingsboard/server/dao/util/**/*.java`

**Interfaces:**
- Consumes: Cassandra/Timescale/JDBC access, cache abstractions, DB call statistics and event persistence contracts.
- Produces: Chinese explanations for high-volume telemetry persistence, query aggregation, cache strategy and metrics interception.

- [ ] **Step 1: List files**

Run:
```powershell
rg --files dao/src/main/java/org/thingsboard/server/dao/timeseries dao/src/main/java/org/thingsboard/server/dao/sqlts dao/src/main/java/org/thingsboard/server/dao/nosql dao/src/main/java/org/thingsboard/server/dao/cache dao/src/main/java/org/thingsboard/server/dao/aspect dao/src/main/java/org/thingsboard/server/dao/util -g '*.java' | Sort-Object
```

Expected: timeseries, sqlts, nosql, cache, aspect and util files are listed.

- [ ] **Step 2: Add comments**

Document time partitioning, latest-value persistence, aggregation queries, Cassandra futures, SQL/Timescale differences, cache key/eviction lifecycle, DB call stats and why these concerns are separated from domain services.

- [ ] **Step 3: Verify**

Run global comment-only audit and:
```powershell
git diff --check -- dao
```

Expected: comment-only diff and whitespace check success.

### Task 5: Annotate DAO Tests And Test Infrastructure

**Files:**
- Modify: `dao/src/test/java/**/*.java`

**Interfaces:**
- Consumes: DAO service test suites, SQL/NoSQL containers, DBUnit fixtures and Spring test context.
- Produces: Chinese explanations for test lifecycle, database container setup, fixture loading and environment assumptions.

- [ ] **Step 1: List files**

Run:
```powershell
rg --files dao/src/test/java -g '*.java' | Sort-Object
```

Expected: 106 Java test files.

- [ ] **Step 2: Add comments**

Document test fixture origin, database/container lifecycle, transaction rollback behavior, asynchronous waits, cache cleanup and why tests use shared base classes/suites.

- [ ] **Step 3: Verify**

Run global comment-only audit and:
```powershell
git diff --check -- dao
```

Expected: comment-only diff and whitespace check success.

### Task 6: Final Audit And Maven Verification

**Files:**
- Read: all modified Java files under `dao`
- Modify: only comments if audit finds missing required explanations

**Interfaces:**
- Consumes: all previous batches.
- Produces: final verified `dao` comment-only change set.

- [ ] **Step 1: Confirm summary coverage and comment-only rule**

Run:
```powershell
$files = @(rg --files dao -g '*.java')
$missing = @($files | Where-Object { -not (Select-String -LiteralPath $_ -Pattern '本类总结' -SimpleMatch -Quiet) })
$bad = @(git diff -- dao 2>$null | Where-Object { $_ -like '+*' -and $_ -notlike '+++*' -and $_ -notmatch '^\+\s*(/\*\*?|//|\*|$)' })
$removed = @(git diff -- dao 2>$null | Where-Object { $_ -like '-*' -and $_ -notlike '---*' })
"JAVA_COUNT=$($files.Count)"
"MISSING_SUMMARY_COUNT=$($missing.Count)"
"BAD_ADDED_COUNT=$($bad.Count)"
"REMOVED_COUNT=$($removed.Count)"
```

Expected:
```text
JAVA_COUNT=639
MISSING_SUMMARY_COUNT=0
BAD_ADDED_COUNT=0
REMOVED_COUNT=0
```

- [ ] **Step 2: Check whitespace and scope**

Run:
```powershell
git diff --check -- dao
$names = @(git diff --name-only -- dao 2>$null)
"DAO_DIFF_FILE_COUNT=$($names.Count)"
$nonJava = @($names | Where-Object { $_ -notmatch '\.java$' })
"DAO_NON_JAVA_DIFF_COUNT=$($nonJava.Count)"
$nonJava
```

Expected: diff check exit code 0, changed files under `dao` are Java files only.

- [ ] **Step 3: Compile and test-compile**

Run:
```powershell
mvn -pl dao -DskipTests compile
mvn -pl dao -DskipTests test-compile
```

Expected: compile and test-compile succeed, or any environmental failures are recorded with exact cause.

- [ ] **Step 4: Optional full tests**

Run if feasible:
```powershell
mvn -pl dao test
```

Expected: pass, or record exact environmental failure such as database/container availability, port conflict, certificate, timezone or fixture assumptions.

- [ ] **Step 5: Report completion**

Report:
- Number of Java files annotated.
- Diff shortstat and comment-only audit result.
- Verification commands run and outcomes.
- Any full-test caveats.
- Whether Git commit was skipped because identity is missing.
