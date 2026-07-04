# All Modules Call Chain And Inheritance Docs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ThingsBoard 仓库内每个 Maven module 生成模块级“完整调用链分析”和“继承体系分析”文档，每个模块目录下创建 `docs/` 并同时输出 Markdown 与 HTML 文件。

**Architecture:** 使用静态分析脚本扫描全部 `pom.xml`、Java 源码、注解、类继承/实现关系、POM 依赖和关键技术触点，生成可审计的模块级文档。调用链文档按“谁先调用、为什么调用、调用前后、数据变化、数据库、Actor、MQTT、Kafka、Rule Engine”组织，并附 Mermaid 流程图；继承文档按模块内类树、跨模块父类/接口、模板方法、可重写/必须重写方法、多态使用点和设计取舍组织，并附 Mermaid 树图。生成器只新增 `docs/module-call-chain.md`、`docs/module-call-chain.html`、`docs/module-inheritance.md`、`docs/module-inheritance.html`，不修改 Java 源码或 POM。

**Tech Stack:** Java static text analysis, Maven POM XML, PowerShell, Python standard library, Markdown, HTML, Mermaid flowchart/class diagram, Git diff audit.

## Global Constraints

- “每个模块”定义为仓库中每个非 `target`、非 `.git` 路径下的 `pom.xml` 所代表的 Maven project。
- 当前扫描结果：56 个 `pom.xml`，3646 个 Java 文件。
- 每个模块目录必须创建或复用 `docs/` 文件夹。
- 每个模块必须生成 4 个文件：
  - `docs/module-call-chain.md`
  - `docs/module-call-chain.html`
  - `docs/module-inheritance.md`
  - `docs/module-inheritance.html`
- 不修改业务代码、Java 源码、POM、资源文件或既有注释。
- 文档必须覆盖：
  - 谁最先调用这里
  - 为什么会调用
  - 调用之前发生了什么
  - 调用之后发生什么
  - 数据如何变化
  - 对数据库进行了哪些操作
  - 是否发送 Actor 消息
  - 是否发送 MQTT 消息
  - 是否写入 Kafka
  - 是否写入 Rule Engine
  - 完整流程图
  - 完整继承树
  - 每层为什么存在、抽象了什么、父子类职责、为什么不用组合/接口、模板方法、可重写/必须重写方法、多态使用点、继承体系解决的问题
- 对无 Java 源码模块，文档要明确说明它是聚合、前端、Docker/打包或 MSA 辅助模块，调用链以构建/部署/资源装配为主，继承树标记为“本模块无 Java 类型”。
- 对聚合模块，文档要列出其子模块，并说明调用链/继承体系主要由子模块承载。
- 静态分析结论必须区分“直接触点”和“间接触点/运行时进入下游模块”。
- HTML 文件必须包含与 Markdown 等价的文本内容和 Mermaid 图。
- 当前工作区已有大量中文注释变更，执行本计划时不得回滚、重排或修改这些范围。
- 若 `git config user.name` 或 `git config user.email` 为空，不创建提交。

---

## File Structure

Create:

- `.git/sdd/module_docs_generator.py`
- `.git/sdd/all-module-docs-progress.md`

Create per Maven module:

- `<module>/docs/module-call-chain.md`
- `<module>/docs/module-call-chain.html`
- `<module>/docs/module-inheritance.md`
- `<module>/docs/module-inheritance.html`

For root Maven project, use:

- `docs/module-call-chain.md`
- `docs/module-call-chain.html`
- `docs/module-inheritance.md`
- `docs/module-inheritance.html`

### Task 1: Baseline Module Inventory

**Files:**
- Read: `pom.xml`
- Read: all `**/pom.xml`
- Modify: none

**Interfaces:**
- Consumes: Maven module tree and existing source layout.
- Produces: fixed module list and expected document count.

- [ ] **Step 1: Count modules**

Run:

```powershell
$poms = @(rg --files -g 'pom.xml' -g '!**/target/**' -g '!**/.git/**')
"POM_COUNT=$($poms.Count)"
```

Expected:

```text
POM_COUNT=56
```

- [ ] **Step 2: Count Java files**

Run:

```powershell
$java = @(rg --files -g '*.java' -g '!**/target/**' -g '!**/.git/**')
"JAVA_FILES=$($java.Count)"
```

Expected:

```text
JAVA_FILES=3646
```

### Task 2: Build Static Analyzer And Generator

**Files:**
- Create: `.git/sdd/module_docs_generator.py`

**Interfaces:**
- Consumes: Maven POM files, Java source files and module paths.
- Produces: module metadata, call-chain markdown/html, inheritance markdown/html.

- [ ] **Step 1: Implement analyzer**

The script must:

- parse POM XML for artifactId, packaging, name, child modules and dependencies;
- assign Java files to nearest Maven module based on `src/main/java` or `src/test/java`;
- strip comments before scanning code constructs;
- detect package, top-level class/interface/enum/record names, `extends`, `implements`, annotations and method names;
- build a global class index and per-module inheritance forest;
- detect entry points: `main`, Spring Boot applications, Controllers, Services, Components, Repositories, Kafka listeners, tests, rule nodes, transport handlers, Netty handlers and migration commands;
- detect direct technical touches by keywords/imports: database/DAO/JPA/Cassandra, Actor, MQTT, Kafka, Rule Engine, cache, REST, WebSocket, transport, queue;
- render Mermaid diagrams and HTML wrappers.

- [ ] **Step 2: Verify script syntax**

Run:

```powershell
python -m py_compile .git\sdd\module_docs_generator.py
```

Expected: exit code 0.

### Task 3: Generate Module Docs

**Files:**
- Create/overwrite generated files under each module `docs/`.

**Interfaces:**
- Consumes: analyzer output.
- Produces: 224 generated documentation files for 56 modules.

- [ ] **Step 1: Run generator**

Run:

```powershell
python .git\sdd\module_docs_generator.py
```

Expected:

```text
MODULES=56
GENERATED_FILES=224
```

- [ ] **Step 2: Spot check key modules**

Read snippets from:

- `application/docs/module-call-chain.md`
- `dao/docs/module-call-chain.md`
- `common/transport/mqtt/docs/module-call-chain.md`
- `rule-engine/rule-engine-components/docs/module-inheritance.md`
- `transport/mqtt/docs/module-call-chain.html`

Expected: each file contains Chinese sections and Mermaid diagrams.

### Task 4: Audit Coverage And Scope

**Files:**
- Read: generated docs
- Modify: generated docs only if audit finds missing content

**Interfaces:**
- Consumes: generated file set and Git diff.
- Produces: verified documentation coverage.

- [ ] **Step 1: Count generated docs**

Run:

```powershell
$poms = @(rg --files -g 'pom.xml' -g '!**/target/**' -g '!**/.git/**')
$missing = @()
foreach ($pom in $poms) {
  $dir = Split-Path $pom -Parent
  if ([string]::IsNullOrEmpty($dir)) { $dir = '.' }
  foreach ($name in @('module-call-chain.md','module-call-chain.html','module-inheritance.md','module-inheritance.html')) {
    $path = if ($dir -eq '.') { Join-Path 'docs' $name } else { Join-Path (Join-Path $dir 'docs') $name }
    if (-not (Test-Path $path)) { $missing += $path }
  }
}
"MODULES=$($poms.Count)"
"EXPECTED_DOCS=$($poms.Count * 4)"
"MISSING_DOCS=$($missing.Count)"
```

Expected:

```text
MODULES=56
EXPECTED_DOCS=224
MISSING_DOCS=0
```

- [ ] **Step 2: Validate required sections**

Run:

```powershell
$docs = @(rg --files -g 'module-call-chain.md' -g 'module-inheritance.md' -g '!**/target/**' -g '!**/.git/**')
$required = @('完整流程图','谁最先调用这里','数据库','Actor','MQTT','Kafka','Rule Engine','完整继承树','模板方法','多态')
$bad = @()
foreach ($doc in $docs) {
  $text = Get-Content -LiteralPath $doc -Raw
  foreach ($term in $required) {
    if ($text -notmatch [regex]::Escape($term)) { $bad += "$doc missing $term" }
  }
}
"DOC_MD_COUNT=$($docs.Count)"
"BAD_SECTION_COUNT=$($bad.Count)"
```

Expected:

```text
DOC_MD_COUNT=112
BAD_SECTION_COUNT=0
```

- [ ] **Step 3: Check diff scope**

Run:

```powershell
git diff --check -- . ':!**/target/**'
$names = @(git diff --name-only -- . 2>$null | Where-Object { $_ -match '(^|/)docs/module-(call-chain|inheritance)\.(md|html)$' })
"GENERATED_DIFF_DOCS=$($names.Count)"
```

Expected: `git diff --check` exit code 0 and generated docs present.

### Task 5: Report Completion

**Files:**
- Create: `.git/sdd/all-module-docs-progress.md`

**Interfaces:**
- Consumes: audit and generation results.
- Produces: final user-facing summary.

- [ ] **Step 1: Write progress record**

Record:

- module count;
- generated file count;
- audit results;
- important static-analysis limitations;
- Git identity status.

- [ ] **Step 2: Final response**

Report:

- docs generated;
- key locations;
- verification commands and outcomes;
- known limitation: static module-level analysis, not runtime tracing;
- no commit if Git identity is missing.
