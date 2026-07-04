# Thingsboard Rest Client 模块继承体系分析

> 生成范围：`rest-client`  
> Maven artifact：`rest-client`  
> Java 类型数量：2  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
Closeable
├── «implements» RestClient
Object/外部框架
├── RestClient
├── RestJsonConverter
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| RestClient_c0["RestClient"]
    Closeable_p1["Closeable"] -->|implements| RestClient_c1["RestClient"]
    Object______p2["Object/外部框架"] -->|extends| RestJsonConverter_c2["RestJsonConverter"]
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

- `RestClient.RestTemplate()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.RestTemplate()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.HttpRequestWrapper()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getRestTemplate()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getToken()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getRefreshToken()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.refreshToken()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.login()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.setTokenInfo()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getAdminSettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.saveAdminSettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.sendTestMail()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.sendTestSms()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getSecuritySettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.saveSecuritySettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getJwtSettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.saveJwtSettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getRepositorySettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.repositorySettingsExists()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.saveRepositorySettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.deleteRepositorySettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.checkRepositoryAccess()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getAutoCommitSettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.autoCommitSettingsExists()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.saveAutoCommitSettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.deleteAutoCommitSettings()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.checkUpdates()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getSystemInfo()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getAlarmById()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getAlarmInfoById()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.saveAlarm()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.deleteAlarm()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.ackAlarm()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.clearAlarm()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.assignAlarm()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.unassignAlarm()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getAlarms()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getHighestAlarmSeverity()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.createAlarm()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getAlarmTypes()` (public, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)


## 哪些方法必须重写

- `RestClient.RestTemplate()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.RestTemplate()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.HttpRequestWrapper()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.Asset()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.activateUser()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getComponentDescriptorsByType()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getComponentDescriptorsByTypes()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.Customer()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.saveDevice()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.SaveDeviceWithCredentialsRequest()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.Device()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.doCreateDevice()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.doCreateDevice()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.doCreateDevice()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.saveDeviceCredentials()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.EntityRelation()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.StringBuilder()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getRuleChains()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.listToString()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getLatestTimeseries()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getTimeseries()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getTimeseries()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.StringBuilder()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.listToString()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.listToString()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getWidgetsBundles()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.saveWidgetType()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getWidgetTypes()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.getBundleWidgetTypesInfos()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.listToString()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.listToString()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.HttpHeaders()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestClient.ByteArrayResource()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/RestClient.java`)
- `RestJsonConverter.BaseAttributeKvEntry()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/utils/RestJsonConverter.java`)
- `RestJsonConverter.BasicTsKvEntry()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/utils/RestJsonConverter.java`)
- `RestJsonConverter.BooleanDataEntry()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/utils/RestJsonConverter.java`)
- `RestJsonConverter.parseNumericValue()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/utils/RestJsonConverter.java`)
- `RestJsonConverter.StringDataEntry()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/utils/RestJsonConverter.java`)
- `RestJsonConverter.RuntimeException()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/utils/RestJsonConverter.java`)
- `RestJsonConverter.JsonDataEntry()` (package, `rest-client/src/main/java/org/thingsboard/rest/client/utils/RestJsonConverter.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `RestClient` (extends)
- `Closeable` -> `RestClient` (implements)
- `Object/外部框架` -> `RestJsonConverter` (extends)


## 外部父类/接口

- `Closeable`
- `Object/外部框架`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
