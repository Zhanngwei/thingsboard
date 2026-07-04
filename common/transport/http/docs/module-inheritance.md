# Thingsboard HTTP Transport Common 模块继承体系分析

> 生成范围：`common/transport/http`  
> Maven artifact：`http`  
> Java 类型数量：8  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
Object/外部框架
├── DeviceApiController
├── DeviceAuthCallback
├── DeviceProvisionCallback
├── GetOtaPackageCallback
├── HttpOkCallback
├── HttpSessionListener
├── SessionCloseOnErrorCallback
SessionMsgListener
├── «implements» HttpSessionListener
TbTransportService
├── «implements» DeviceApiController
TransportContext
├── HttpTransportContext
TransportServiceCallback
├── «implements» DeviceAuthCallback
├── «implements» DeviceProvisionCallback
├── «implements» GetOtaPackageCallback
├── «implements» HttpOkCallback
├── «implements» SessionCloseOnErrorCallback
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| DeviceApiController_c0["DeviceApiController"]
    TbTransportService_p1["TbTransportService"] -->|implements| DeviceApiController_c1["DeviceApiController"]
    Object______p2["Object/外部框架"] -->|extends| DeviceAuthCallback_c2["DeviceAuthCallback"]
    TransportServiceCallback_p3["TransportServiceCallback"] -->|implements| DeviceAuthCallback_c3["DeviceAuthCallback"]
    Object______p4["Object/外部框架"] -->|extends| DeviceProvisionCallback_c4["DeviceProvisionCallback"]
    TransportServiceCallback_p5["TransportServiceCallback"] -->|implements| DeviceProvisionCallback_c5["DeviceProvisionCallback"]
    Object______p6["Object/外部框架"] -->|extends| GetOtaPackageCallback_c6["GetOtaPackageCallback"]
    TransportServiceCallback_p7["TransportServiceCallback"] -->|implements| GetOtaPackageCallback_c7["GetOtaPackageCallback"]
    Object______p8["Object/外部框架"] -->|extends| SessionCloseOnErrorCallback_c8["SessionCloseOnErrorCallback"]
    TransportServiceCallback_p9["TransportServiceCallback"] -->|implements| SessionCloseOnErrorCallback_c9["SessionCloseOnErrorCallback"]
    Object______p10["Object/外部框架"] -->|extends| HttpOkCallback_c10["HttpOkCallback"]
    TransportServiceCallback_p11["TransportServiceCallback"] -->|implements| HttpOkCallback_c11["HttpOkCallback"]
    Object______p12["Object/外部框架"] -->|extends| HttpSessionListener_c12["HttpSessionListener"]
    SessionMsgListener_p13["SessionMsgListener"] -->|implements| HttpSessionListener_c13["HttpSessionListener"]
    TransportContext_p14["TransportContext"] -->|extends| HttpTransportContext_c14["HttpTransportContext"]
```


## 每一层为什么存在

- 外部/上层父类或接口层：提供框架生命周期、Java 标准契约、Spring/Netty/DAO/Rule Engine 等扩展点。
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

- 未发现明显抽象类模板方法；本模块可能主要使用具体类、接口或外部框架回调。


## 哪些方法可以重写

- `DeviceApiController.getDeviceAttributes()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.postDeviceAttributes()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.JsonParser()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.postTelemetry()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.JsonParser()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.claimDevice()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.DeviceId()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpOkCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.subscribeToCommands()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpOkCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.postRpcRequest()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.JsonParser()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.subscribeToAttributes()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.getFirmware()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.getOtaPackageCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.getSoftware()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.getOtaPackageCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.provisionDevice()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.DeviceProvisionCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.GetOtaPackageCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.onSuccess()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.onError()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.onSuccess()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.onError()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.onSuccess()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.UUID()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.ByteArrayResource()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.onError()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.onSuccess()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.onError()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpOkCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.onSuccess()` (public, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)


## 哪些方法必须重写

- `DeviceApiController.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.JsonParser()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.JsonParser()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.DeviceId()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpOkCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpOkCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.JsonParser()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.getOtaPackageCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.getOtaPackageCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.DeviceProvisionCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.GetOtaPackageCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.UUID()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.ByteArrayResource()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceApiController.UUID()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.JsonParser()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.JsonParser()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.DeviceId()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.HttpOkCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.HttpOkCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.JsonParser()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.HttpSessionListener()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.SessionCloseOnErrorCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.getOtaPackageCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.getOtaPackageCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.DeviceProvisionCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.GetOtaPackageCallback()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)
- `DeviceAuthCallback.UUID()` (package, `common/transport/http/src/main/java/org/thingsboard/server/transport/http/DeviceApiController.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `DeviceApiController` (extends)
- `TbTransportService` -> `DeviceApiController` (implements)
- `Object/外部框架` -> `DeviceAuthCallback` (extends)
- `TransportServiceCallback` -> `DeviceAuthCallback` (implements)
- `Object/外部框架` -> `DeviceProvisionCallback` (extends)
- `TransportServiceCallback` -> `DeviceProvisionCallback` (implements)
- `Object/外部框架` -> `GetOtaPackageCallback` (extends)
- `TransportServiceCallback` -> `GetOtaPackageCallback` (implements)
- `Object/外部框架` -> `SessionCloseOnErrorCallback` (extends)
- `TransportServiceCallback` -> `SessionCloseOnErrorCallback` (implements)
- `Object/外部框架` -> `HttpOkCallback` (extends)
- `TransportServiceCallback` -> `HttpOkCallback` (implements)
- `Object/外部框架` -> `HttpSessionListener` (extends)
- `SessionMsgListener` -> `HttpSessionListener` (implements)
- `TransportContext` -> `HttpTransportContext` (extends)


## 外部父类/接口

- `Object/外部框架`
- `SessionMsgListener`
- `TbTransportService`
- `TransportContext`
- `TransportServiceCallback`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
