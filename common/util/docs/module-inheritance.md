# Thingsboard Server Common Utils 模块继承体系分析

> 生成范围：`common/util`  
> Maven artifact：`util`  
> Java 类型数量：19  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
Executor
├── ListeningExecutor
├── ├── «implements» AbstractListeningExecutor
ForkJoinWorkerThreadFactory
├── «implements» ThingsBoardForkJoinWorkerThreadFactory
Object/外部框架
├── AbstractListeningExecutor
├── AzureIotHubUtil
├── DonAsynchron
├── ExceptionUtil
├── ExceptionUtilTest
├── JacksonUtil
├── JacksonUtilTest
├── KvUtil
├── LinkedHashMapRemoveEldest
├── LinkedHashMapRemoveEldestTest
├── RegexUtils
├── SslUtil
├── SystemUtil
├── ThingsBoardExecutors
├── ThingsBoardForkJoinWorkerThreadFactory
├── ThingsBoardThreadFactory
StopWatch
├── TbStopWatch
ThreadFactory
├── «implements» ThingsBoardThreadFactory
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| AbstractListeningExecutor_c0["AbstractListeningExecutor"]
    ListeningExecutor_p1["ListeningExecutor"] -->|implements| AbstractListeningExecutor_c1["AbstractListeningExecutor"]
    Object______p2["Object/外部框架"] -->|extends| AzureIotHubUtil_c2["AzureIotHubUtil"]
    Object______p3["Object/外部框架"] -->|extends| DonAsynchron_c3["DonAsynchron"]
    Object______p4["Object/外部框架"] -->|extends| ExceptionUtil_c4["ExceptionUtil"]
    Object______p5["Object/外部框架"] -->|extends| JacksonUtil_c5["JacksonUtil"]
    Object______p6["Object/外部框架"] -->|extends| KvUtil_c6["KvUtil"]
    Object______p7["Object/外部框架"] -->|extends| LinkedHashMapRemoveEldest_c7["LinkedHashMapRemoveEldest"]
    Executor_p8["Executor"] -->|extends| ListeningExecutor_c8["ListeningExecutor"]
    Object______p9["Object/外部框架"] -->|extends| RegexUtils_c9["RegexUtils"]
    Object______p10["Object/外部框架"] -->|extends| SslUtil_c10["SslUtil"]
    Object______p11["Object/外部框架"] -->|extends| SystemUtil_c11["SystemUtil"]
    StopWatch_p12["StopWatch"] -->|extends| TbStopWatch_c12["TbStopWatch"]
    Object______p13["Object/外部框架"] -->|extends| ThingsBoardExecutors_c13["ThingsBoardExecutors"]
    Object______p14["Object/外部框架"] -->|extends| ThingsBoardForkJoinWorkerThreadFactory_c14["ThingsBoardForkJoinWorkerThreadFactory"]
    ForkJoinWorkerThreadFactory_p15["ForkJoinWorkerThreadFactory"] -->|implements| ThingsBoardForkJoinWorkerThreadFactory_c15["ThingsBoardForkJoinWorkerThreadFactory"]
    Object______p16["Object/外部框架"] -->|extends| ThingsBoardThreadFactory_c16["ThingsBoardThreadFactory"]
    ThreadFactory_p17["ThreadFactory"] -->|implements| ThingsBoardThreadFactory_c17["ThingsBoardThreadFactory"]
    Object______p18["Object/外部框架"] -->|extends| ExceptionUtilTest_c18["ExceptionUtilTest"]
    Object______p19["Object/外部框架"] -->|extends| JacksonUtilTest_c19["JacksonUtilTest"]
    Object______p20["Object/外部框架"] -->|extends| LinkedHashMapRemoveEldestTest_c20["LinkedHashMapRemoveEldestTest"]
```


## 每一层为什么存在

- 外部/上层父类或接口层：提供框架生命周期、Java 标准契约、Spring/Netty/DAO/Rule Engine 等扩展点。
- 接口层：定义跨模块契约，让调用方依赖稳定 API，而不是具体实现。
- 抽象类层：沉淀公共状态、校验、模板流程和默认实现，把变化点留给子类。
- 具体类层：完成协议、DAO、Controller、Rule Node、工具或测试场景中的最终业务动作。

## 抽象了什么

- 父类/接口抽象公共生命周期、输入输出契约、错误处理、协议适配、DAO 查询形态、消息处理模板或测试夹具。
- 子类保留具体协议、实体类型、规则节点行为、数据库实现、页面/测试步骤或命令参数差异。
- 对聚合或无 Java 模块，抽象体现在 Maven 子模块划分和构建生命周期，而不是 Java 继承。

## 父类负责什么

- 提供稳定方法签名、共享字段、默认流程、通用校验、资源释放和框架回调入口。
- 在 Spring、Netty、DAO、Rule Engine、Transport 等框架中，父类还负责让运行时可以通过统一类型调度不同实现。

## 子类负责什么

- 实现具体业务差异，例如协议解析、实体 DAO、规则节点处理、Controller API、客户端命令或测试用例。
- 覆盖父类预留的扩展点，把模块特有数据转换、数据库查询、消息发送或外部调用补进去。

## 为什么不用组合

- 继承用于框架生命周期和模板方法：运行时需要把子类当作父类处理，例如 Spring Bean、Netty Handler、DAO Repository、Rule Node 或测试基类。
- 组合适合注入协作者，本模块中仍然通过字段依赖使用组合；但当需要统一回调签名、共享模板流程或多态派发时，单纯组合不能替代继承。

## 为什么不用接口

- 接口只能表达契约，不能集中保存公共状态、默认校验、资源关闭和模板流程。
- 当模块只需要契约时会使用接口；当多个实现还需要共享代码、默认行为或受保护扩展点时使用抽象类。

## 模板方法

- `AbstractListeningExecutor.init()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.destroy()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.executeAsync()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.executeAsync()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.execute()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.executor()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.getThreadPollSize()` (package, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `ExecutorProvider.getExecutor()` (package, `common/util/src/main/java/org/thingsboard/common/util/ExecutorProvider.java`)
- `ListeningExecutor.executeAsync()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)
- `ListeningExecutor.executeAsync()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)
- `ListeningExecutor.submit()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)
- `ListeningExecutor.executeAsync()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)
- `ListeningExecutor.submit()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)
- `ListeningExecutor.executeAsync()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)


## 哪些方法可以重写

- `AbstractListeningExecutor.init()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.destroy()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.executeAsync()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.executeAsync()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.execute()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.executor()` (public, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AbstractListeningExecutor.getThreadPollSize()` (package, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AzureIotHubUtil.SecretKeySpec()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `AzureIotHubUtil.RuntimeException()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `AzureIotHubUtil.RuntimeException()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `AzureIotHubUtil.RuntimeException()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `AzureIotHubUtil.RuntimeException()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `AzureIotHubUtil.String()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `DonAsynchron.onSuccess()` (public, `common/util/src/main/java/org/thingsboard/common/util/DonAsynchron.java`)
- `DonAsynchron.onFailure()` (public, `common/util/src/main/java/org/thingsboard/common/util/DonAsynchron.java`)
- `DonAsynchron.submit()` (package, `common/util/src/main/java/org/thingsboard/common/util/DonAsynchron.java`)
- `ExceptionUtil.StringWriter()` (package, `common/util/src/main/java/org/thingsboard/common/util/ExceptionUtil.java`)
- `ExceptionUtil.PrintWriter()` (package, `common/util/src/main/java/org/thingsboard/common/util/ExceptionUtil.java`)
- `ExecutorProvider.getExecutor()` (package, `common/util/src/main/java/org/thingsboard/common/util/ExecutorProvider.java`)
- `JacksonUtil.ObjectMapper()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.ObjectMapper()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.RuntimeException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.toJsonNode()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.newObjectNode()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.newArrayNode()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.fromString()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)


## 哪些方法必须重写

- `AbstractListeningExecutor.getThreadPollSize()` (package, `common/util/src/main/java/org/thingsboard/common/util/AbstractListeningExecutor.java`)
- `AzureIotHubUtil.SecretKeySpec()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `AzureIotHubUtil.RuntimeException()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `AzureIotHubUtil.RuntimeException()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `AzureIotHubUtil.RuntimeException()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `AzureIotHubUtil.RuntimeException()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `AzureIotHubUtil.String()` (package, `common/util/src/main/java/org/thingsboard/common/util/AzureIotHubUtil.java`)
- `DonAsynchron.submit()` (package, `common/util/src/main/java/org/thingsboard/common/util/DonAsynchron.java`)
- `ExceptionUtil.StringWriter()` (package, `common/util/src/main/java/org/thingsboard/common/util/ExceptionUtil.java`)
- `ExceptionUtil.PrintWriter()` (package, `common/util/src/main/java/org/thingsboard/common/util/ExceptionUtil.java`)
- `ExecutorProvider.getExecutor()` (package, `common/util/src/main/java/org/thingsboard/common/util/ExecutorProvider.java`)
- `JacksonUtil.ObjectMapper()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.ObjectMapper()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.RuntimeException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.toJsonNode()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.newObjectNode()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.newArrayNode()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.fromString()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.IllegalArgumentException()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `JacksonUtil.toJsonNode()` (package, `common/util/src/main/java/org/thingsboard/common/util/JacksonUtil.java`)
- `ListeningExecutor.executeAsync()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)
- `ListeningExecutor.executeAsync()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)
- `ListeningExecutor.submit()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)
- `ListeningExecutor.executeAsync()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)
- `ListeningExecutor.submit()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)
- `ListeningExecutor.executeAsync()` (package, `common/util/src/main/java/org/thingsboard/common/util/ListeningExecutor.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `AbstractListeningExecutor` (extends)
- `ListeningExecutor` -> `AbstractListeningExecutor` (implements)
- `Object/外部框架` -> `AzureIotHubUtil` (extends)
- `Object/外部框架` -> `DonAsynchron` (extends)
- `Object/外部框架` -> `ExceptionUtil` (extends)
- `Object/外部框架` -> `JacksonUtil` (extends)
- `Object/外部框架` -> `KvUtil` (extends)
- `Object/外部框架` -> `LinkedHashMapRemoveEldest` (extends)
- `Executor` -> `ListeningExecutor` (extends)
- `Object/外部框架` -> `RegexUtils` (extends)
- `Object/外部框架` -> `SslUtil` (extends)
- `Object/外部框架` -> `SystemUtil` (extends)
- `StopWatch` -> `TbStopWatch` (extends)
- `Object/外部框架` -> `ThingsBoardExecutors` (extends)
- `Object/外部框架` -> `ThingsBoardForkJoinWorkerThreadFactory` (extends)
- `ForkJoinWorkerThreadFactory` -> `ThingsBoardForkJoinWorkerThreadFactory` (implements)
- `Object/外部框架` -> `ThingsBoardThreadFactory` (extends)
- `ThreadFactory` -> `ThingsBoardThreadFactory` (implements)
- `Object/外部框架` -> `ExceptionUtilTest` (extends)
- `Object/外部框架` -> `JacksonUtilTest` (extends)
- `Object/外部框架` -> `LinkedHashMapRemoveEldestTest` (extends)


## 外部父类/接口

- `Executor`
- `ForkJoinWorkerThreadFactory`
- `Object/外部框架`
- `StopWatch`
- `ThreadFactory`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
