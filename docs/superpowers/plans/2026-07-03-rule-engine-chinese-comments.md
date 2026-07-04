# Rule Engine Chinese Comments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ThingsBoard `rule-engine` 模块的全部 Java 文件补充高质量中文注释，同时保持所有业务代码完全不变。

**Architecture:** 按架构层推进：先注释 `rule-engine-api` 建立统一术语，再注释 `rule-engine-components` 的共享基础设施，随后处理核心规则节点、外部集成节点，最后补齐测试代码。每批完成后用 diff 和 Maven 验证确保变更只包含注释且不破坏编译。

**Tech Stack:** Java, Maven, ThingsBoard Rule Engine, Javadoc, PowerShell, Git.

## Global Constraints

- 只覆盖 `rule-engine` 下全部 Java 文件。
- 覆盖 `src/main/java` 和 `src/test/java`。
- 不修改任何业务逻辑。
- 不修改变量名、方法名、方法参数、返回类型、可见性、继承结构、注解、导入、包名。
- 不运行格式化工具，不重排原代码。
- 不删除或替换已有英文注释，只追加中文解释。
- 每个类、字段、方法都要补充中文说明。
- 复杂分支、异步回调、异常处理、消息路由、状态迁移、缓存命中或失效、数据库访问、外部协议调用处增加行内中文注释。
- 每个 Java 文件末尾增加“本类总结”。
- Git 当前缺少提交身份配置；如果仍未配置，执行阶段只修改和验证文件，不创建提交。

---

## File Structure

修改范围固定为：

- `rule-engine/rule-engine-api/src/main/java/**/*.java`
- `rule-engine/rule-engine-api/src/test/java/**/*.java`
- `rule-engine/rule-engine-components/src/main/java/**/*.java`
- `rule-engine/rule-engine-components/src/test/java/**/*.java`

当前文件清点命令：

```powershell
rg --files rule-engine -g '*.java' | Measure-Object | Select-Object -ExpandProperty Count
```

期望结果：`305`。

当前批次分布：

- `rule-engine-api`: 32 个 Java 文件。
- `rule-engine-components/src/main/java`: 211 个 Java 文件。
- `rule-engine-components/src/test/java`: 62 个 Java 文件。

注释模板：

```java
/**
 * 中文说明：
 * 1. 职责：说明该类、字段或方法存在的直接目的。
 * 2. 所属模块：说明它在 ThingsBoard Rule Engine 中的位置。
 * 3. 协作对象：说明它与 TbContext、TbMsg、DAO、缓存、Actor、MQTT、Rule Engine 节点等对象的关系。
 * 4. 生命周期：说明由 Spring、Rule Engine、节点初始化、单次消息处理、测试用例或调用方管理。
 * 5. 设计原因：说明为什么抽象为当前类、字段或方法。
 * 6. 技术关联：明确是否直接涉及事务、缓存、MQTT、Actor 通信、数据库、Rule Engine。
 */
```

文件末尾总结模板：

```java
/*
 * 本类总结：
 * 1. 核心职责：概括本文件主类型承担的业务或技术职责。
 * 2. 核心流程：按调用顺序概括初始化、消息处理、回调、清理或测试执行路径。
 * 3. 关键依赖：列出与本文件直接协作的 ThingsBoard 服务、上下文、DAO、缓存、外部协议或测试工具。
 * 4. 学习重点：指出阅读本文件时最应该关注的 Rule Engine 机制或设计取舍。
 */
```

---

### Task 1: Baseline And Guardrails

**Files:**
- Read: `docs/superpowers/specs/2026-07-03-rule-engine-chinese-comments-design.md`
- Read: `rule-engine/pom.xml`
- Modify: none

**Interfaces:**
- Consumes: 已批准的设计规格。
- Produces: 后续所有批次使用的文件清单、验证命令和注释边界。

- [ ] **Step 1: Re-read the approved design**

Run:

```powershell
Get-Content -Raw -Encoding UTF8 docs\superpowers\specs\2026-07-03-rule-engine-chinese-comments-design.md
```

Expected: 文档明确要求只增加中文注释，覆盖 `rule-engine` 全部 Java 文件。

- [ ] **Step 2: Confirm the file count**

Run:

```powershell
rg --files rule-engine -g '*.java' | Measure-Object | Select-Object -ExpandProperty Count
```

Expected: `305`。

- [ ] **Step 3: Capture pre-change status**

Run:

```powershell
git status --short
```

Expected: 只看到已批准的设计/计划文档变更，或者用户已明确允许保留的其它变更。

- [ ] **Step 4: Establish diff audit rule**

Run after each source batch:

```powershell
git diff -- rule-engine | rg -n "^[+-](?![+-])" 
```

Expected: 新增或删除行只属于 Java 注释、Javadoc 或空白分隔；如果出现非注释代码变更，立即停止并修正。

---

### Task 2: Annotate rule-engine-api

**Files:**
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/EmptyNodeConfiguration.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/MailService.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/NodeConfiguration.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/NodeDefinition.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/NotificationCenter.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleEngineAlarmService.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleEngineApiUsageStateService.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleEngineAssetProfileCache.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleEngineDeviceProfileCache.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleEngineDeviceRpcRequest.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleEngineDeviceRpcResponse.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleEngineDeviceStateManager.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleEngineRpcService.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleEngineTelemetryService.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/RuleNode.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/ScriptEngine.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/SmsService.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/TbContext.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/TbEmail.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/TbNode.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/TbNodeConfiguration.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/TbNodeException.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/TbNodeState.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/notification/FirebaseService.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/notification/SlackService.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/sms/SmsSender.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/sms/SmsSenderFactory.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/sms/exception/SmsException.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/sms/exception/SmsParseException.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/sms/exception/SmsSendException.java`
- Modify: `rule-engine/rule-engine-api/src/main/java/org/thingsboard/rule/engine/api/util/TbNodeUtils.java`
- Modify: `rule-engine/rule-engine-api/src/test/java/org/thingsboard/rule/engine/api/util/TbNodeUtilsTest.java`

**Interfaces:**
- Consumes: Rule Engine API abstractions already present in source.
- Produces: 中文术语基准，后续组件注释必须沿用 `TbNode`、`TbContext`、`TbMsg`、节点生命周期、回调和外部服务边界的解释。

- [ ] **Step 1: Read the API files**

Run:

```powershell
rg --files rule-engine\rule-engine-api -g '*.java' | Sort-Object
```

Expected: 输出 32 个 Java 文件，与本任务文件列表一致。

- [ ] **Step 2: Add class, field, method, inline and summary comments**

Use the templates in `File Structure`. For API interfaces, method注释必须明确“由 Rule Engine 运行时或规则节点调用，接口本身不直接执行事务、缓存、MQTT、Actor、数据库操作，具体行为由实现类决定”。For DTO and configuration classes, field注释必须说明数据来自节点配置、服务调用或测试构造。

- [ ] **Step 3: Verify API diff contains comments only**

Run:

```powershell
git diff -- rule-engine/rule-engine-api | rg -n "^[+-](?![+-])"
```

Expected: 所有新增行均为 `/**`、`*`、`*/`、`//`、`/*`、空白分隔，或“本类总结”注释内容；不出现 Java 语句、导入、注解、字段、方法签名的变更。

- [ ] **Step 4: Run API tests**

Run:

```powershell
mvn -pl rule-engine/rule-engine-api test
```

Expected: Maven build succeeds. If dependency resolution or environment setup fails before compiling this module, record the exact failure and still run `git diff --check`.

- [ ] **Step 5: Commit when Git identity exists**

Run:

```powershell
git config user.name
git config user.email
```

Expected: If both values exist, commit this task with `docs: annotate rule-engine api in chinese`. If either value is missing, do not commit and report that commit is blocked by Git identity.

---

### Task 3: Annotate Shared Component Infrastructure

**Files:**
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/util/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/external/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/data/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/debug/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/delay/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/transaction/*.java`
- Modify: shared abstract/helper classes under `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/transform/*.java`

**Interfaces:**
- Consumes: API terminology from Task 2.
- Produces: Reusable explanations for entity loading, callbacks, shared node templates and helper state used by concrete nodes.

- [ ] **Step 1: List shared files**

Run:

```powershell
rg --files rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\util rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\external rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\data rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\debug rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\delay rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\transaction -g '*.java' | Sort-Object
```

Expected: 输出共享基础设施文件。

- [ ] **Step 2: Annotate shared classes**

For loaders and callback wrappers, method注释必须说明异步回调、数据库读取、缓存读取、Rule Engine 消息流和线程安全边界。For transaction nodes, 注释必须明确是否直接开启数据库事务，还是只在规则链中表达同步边界。

- [ ] **Step 3: Verify shared diff contains comments only**

Run:

```powershell
git diff -- rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/util rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/external rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/data rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/debug rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/delay rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/transaction | rg -n "^[+-](?![+-])"
```

Expected: 只出现注释变更。

- [ ] **Step 4: Run component compile or tests**

Run:

```powershell
mvn -pl rule-engine/rule-engine-components -DskipTests compile
```

Expected: Maven compile succeeds, or records exact environment failure.

---

### Task 4: Annotate Core Rule Nodes

**Files:**
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/metadata/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/filter/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/transform/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/telemetry/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/profile/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/flow/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/deduplication/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/math/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/geo/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/credentials/*.java`

**Interfaces:**
- Consumes: API and shared component explanations from Tasks 2 and 3.
- Produces: Complete Chinese documentation for message enrichment, filtering, transformation, action execution, telemetry persistence, profile processing, relation routing and deduplication nodes.

- [ ] **Step 1: List core node files**

Run:

```powershell
rg --files rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\metadata rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\filter rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\transform rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\action rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\telemetry rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\profile rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\flow rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\deduplication rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\math rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\geo rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\credentials -g '*.java' | Sort-Object
```

Expected: 输出核心规则节点生产代码文件。

- [ ] **Step 2: Annotate core nodes**

For every `Tb*Node`, class注释必须说明节点在规则链中的输入关系、输出关系、失败关系、配置对象、调用方和生命周期。For `onMsg` or equivalent processing methods, method注释必须说明是否触发数据库、缓存、Rule Engine、Actor、MQTT、事务。For configuration classes, field注释必须说明配置来自规则节点 JSON。

- [ ] **Step 3: Verify core diff contains comments only**

Run:

```powershell
git diff -- rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/metadata rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/filter rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/transform rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/action rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/telemetry rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/profile rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/flow rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/deduplication rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/math rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/geo rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/credentials | rg -n "^[+-](?![+-])"
```

Expected: 只出现注释变更。

- [ ] **Step 4: Run component compile**

Run:

```powershell
mvn -pl rule-engine/rule-engine-components -DskipTests compile
```

Expected: Maven compile succeeds, or records exact environment failure.

---

### Task 5: Annotate External Integration Nodes

**Files:**
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/mqtt/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/kafka/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/rabbitmq/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/rest/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/aws/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/gcp/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/mail/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/sms/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/notification/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/rpc/*.java`
- Modify: `rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/edge/*.java`

**Interfaces:**
- Consumes: Rule Engine node lifecycle and shared callback explanations from previous tasks.
- Produces: Complete Chinese documentation for nodes that call external protocols, notification services, RPC, Edge and cloud integrations.

- [ ] **Step 1: List external node files**

Run:

```powershell
rg --files rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\mqtt rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\kafka rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\rabbitmq rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\rest rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\aws rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\gcp rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\mail rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\sms rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\notification rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\rpc rule-engine\rule-engine-components\src\main\java\org\thingsboard\rule\engine\edge -g '*.java' | Sort-Object
```

Expected: 输出外部集成节点生产代码文件。

- [ ] **Step 2: Annotate external integration nodes**

For MQTT nodes, method注释必须明确是否直接使用 MQTT 客户端、连接生命周期、QoS/Topic 配置来源和 Rule Engine 消息确认关系。For Kafka/RabbitMQ/REST/AWS/GCP/Mail/SMS/Notification/RPC/Edge nodes, method注释必须明确外部调用边界、异步回调、失败路由、线程安全和数据库/缓存间接依赖。

- [ ] **Step 3: Verify external diff contains comments only**

Run:

```powershell
git diff -- rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/mqtt rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/kafka rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/rabbitmq rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/rest rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/aws rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/gcp rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/mail rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/sms rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/notification rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/rpc rule-engine/rule-engine-components/src/main/java/org/thingsboard/rule/engine/edge | rg -n "^[+-](?![+-])"
```

Expected: 只出现注释变更。

- [ ] **Step 4: Run component compile**

Run:

```powershell
mvn -pl rule-engine/rule-engine-components -DskipTests compile
```

Expected: Maven compile succeeds, or records exact environment failure.

---

### Task 6: Annotate Component Tests

**Files:**
- Modify: `rule-engine/rule-engine-components/src/test/java/**/*.java`

**Interfaces:**
- Consumes: Production code annotations from Tasks 3, 4 and 5.
- Produces: Chinese explanations for test fixture setup, mocked services, validated Rule Engine behavior and assertion intent.

- [ ] **Step 1: List component test files**

Run:

```powershell
rg --files rule-engine\rule-engine-components\src\test\java -g '*.java' | Sort-Object
```

Expected: 输出 62 个测试 Java 文件。

- [ ] **Step 2: Annotate tests**

For every test class, class注释必须说明测试目标、所属生产节点、Mock 依赖来源、被验证流程和为什么该测试存在。For every test method, method注释必须说明输入数据、期望输出、调用时机、是否涉及数据库、缓存、MQTT、Actor、Rule Engine。

- [ ] **Step 3: Verify test diff contains comments only**

Run:

```powershell
git diff -- rule-engine/rule-engine-components/src/test/java | rg -n "^[+-](?![+-])"
```

Expected: 只出现注释变更。

- [ ] **Step 4: Run component tests**

Run:

```powershell
mvn -pl rule-engine/rule-engine-components test
```

Expected: Maven tests succeed, or records exact environment failure.

---

### Task 7: Final Audit

**Files:**
- Read: all modified files under `rule-engine`
- Modify: only comments if audit finds missing required explanations

**Interfaces:**
- Consumes: All previous annotation batches.
- Produces: Final verified `rule-engine` comment-only change set.

- [ ] **Step 1: Confirm all rule-engine Java files are still present**

Run:

```powershell
rg --files rule-engine -g '*.java' | Measure-Object | Select-Object -ExpandProperty Count
```

Expected: `305`。

- [ ] **Step 2: Check for whitespace errors**

Run:

```powershell
git diff --check
```

Expected: no output and exit code `0`.

- [ ] **Step 3: Check diff scope**

Run:

```powershell
git diff --name-only -- rule-engine
```

Expected: only Java files under `rule-engine`.

- [ ] **Step 4: Check comment-only rule**

Run:

```powershell
git diff -- rule-engine | rg -n "^[+-](?![+-])"
```

Expected: all changed lines are comments or blank lines. Any changed Java statement, import, annotation, field declaration, method signature or literal value must be reverted by editing the file back to its original code while retaining comments.

- [ ] **Step 5: Run final module tests**

Run:

```powershell
mvn -pl rule-engine test
```

Expected: Maven tests succeed, or records exact environment failure. If full tests are too slow, run:

```powershell
mvn -pl rule-engine/rule-engine-api,rule-engine/rule-engine-components test
```

Expected: Maven tests succeed, or records exact environment failure.

- [ ] **Step 6: Report completion**

Report:

- Number of Java files annotated.
- Verification commands run.
- Any tests not run and why.
- Whether Git commit was skipped because identity is missing.
