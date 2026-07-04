# ThingsBoard Black Box Tests 模块继承体系分析

> 生成范围：`msa/black-box-tests`  
> Maven artifact：`black-box-tests`  
> Java 类型数量：119  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
CoapHandler
├── «implements» TestCoapClientCallback
IAnnotationTransformer
├── «implements» RetryTestListener
IMqttMessageListener
├── «implements» MqttMessageListener
IRetryAnalyzer
├── «implements» RetryAnalyzer
ITestListener
├── «implements» TestListener
MqttHandler
├── «implements» MqttMessageListener
Object/外部框架
├── AbstractBasePage
├── ├── AlarmDetailsViewElements
├── ├── ├── AlarmDetailsViewHelper
├── ├── AssignDeviceTabElements
├── ├── ├── AssignDeviceTabHelper
├── ├── CreateDeviceTabElements
├── ├── ├── CreateDeviceTabHelper
├── ├── CreateWidgetPopupElements
├── ├── ├── CreateWidgetPopupHelper
├── ├── LoginPageElements
├── ├── ├── LoginPageHelper
├── ├── OpenRuleChainPageElements
├── ├── ├── OpenRuleChainPageHelper
├── ├── OtherPageElements
├── ├── ├── AlarmDetailsEntityTabElements
├── ├── ├── ├── AlarmDetailsEntityTabHelper
├── ├── ├── ├── ├── AlarmWidgetElements
├── ├── ├── AssetPageElements
├── ├── ├── ├── AssetPageHelper
├── ├── ├── OtherPageElementsHelper
├── ├── ├── ├── CustomerPageElements
├── ├── ├── ├── ├── CustomerPageHelper
├── ├── ├── ├── DashboardPageElements
├── ├── ├── ├── ├── DashboardPageHelper
├── ├── ├── ├── DevicePageElements
├── ├── ├── ├── ├── DevicePageHelper
├── ├── ├── ├── EntityViewPageElements
├── ├── ├── ├── ├── EntityViewPageHelper
├── ├── ├── ├── ProfilesPageElements
├── ├── ├── ├── ├── ProfilesPageHelper
├── ├── ├── ├── RuleChainsPageElements
├── ├── ├── ├── ├── RuleChainsPageHelper
├── ├── SideBarMenuViewElements
├── ├── ├── SideBarMenuViewHelper
├── AbstractContainerTest
├── ├── AbstractDriverBaseTest
├── ├── ├── AbstractAssignTest
├── ├── ├── ├── AssignDetailsTabAssignTest
├── ├── ├── ├── AssignDetailsTabFromCustomerAssignTest
├── ├── ├── ├── AssignFromAlarmWidgetTest
├── ├── ├── AbstractDeviceTest
├── ├── ├── ├── AssignToCustomerTest
├── ├── ├── ├── CreateDeviceTest
├── ├── ├── ├── DeleteDeviceTest
├── ├── ├── ├── DeleteSeveralDevicesTest
├── ├── ├── ├── DeviceFilterTest
├── ├── ├── ├── EditDeviceTest
├── ├── ├── ├── MakeDevicePrivateTest
├── ├── ├── ├── MakeDevicePublicTest
├── ├── ├── AbstractRuleChainTest
├── ├── ├── ├── CreateRuleChainImportTest
├── ├── ├── ├── CreateRuleChainTest
├── ├── ├── ├── DeleteRuleChainTest
├── ├── ├── ├── DeleteSeveralRuleChainsTest
├── ├── ├── ├── MakeRuleChainRootTest
├── ├── ├── ├── OpenRuleChainTest
├── ├── ├── ├── RuleChainEditMenuTest
├── ├── ├── ├── SearchRuleChainTest
├── ├── ├── ├── SortByNameTest
├── ├── ├── ├── SortByTimeTest
├── ├── ├── AssetProfileEditMenuTest
├── ├── ├── CreateAssetProfileImportTest
├── ├── ├── CreateAssetProfileTest
├── ├── ├── CreateCustomerTest
├── ├── ├── CreateDeviceProfileImportTest
├── ├── ├── CreateDeviceProfileTest
├── ├── ├── CustomerEditMenuTest
├── ├── ├── DeleteAssetProfileTest
├── ├── ├── DeleteCustomerTest
├── ├── ├── DeleteDeviceProfileTest
├── ├── ├── DeleteSeveralAssetProfilesTest
├── ├── ├── DeleteSeveralCustomerTest
├── ├── ├── DeleteSeveralDeviceProfilesTest
├── ├── ├── DeviceProfileEditMenuTest
├── ├── ├── MakeAssetProfileDefaultTest
├── ├── ├── MakeDeviceProfileDefaultTest
├── ├── ├── ManageCustomersAssetsTest
├── ├── ├── ManageCustomersDashboardsTest
├── ├── ├── ManageCustomersDevicesTest
├── ├── ├── ManageCustomersEdgesTest
├── ├── ├── ManageCustomersUsersTest
├── ├── ├── SearchAssetProfileTest
├── ├── ├── SearchCustomerTest
├── ├── ├── SearchDeviceProfileTest
├── ├── ├── SortByNameTest
├── ├── CoapClientTest
├── ├── HttpClientTest
├── ├── MqttClientTest
├── ├── MqttGatewayClientTest
├── ├── MqttNodeTest
├── AttributesResponse
├── Const
├── ContainerTestSuite
├── DataProviderCredential
├── DevicePrototypes
├── DockerComposeContainerImpl
├── DockerComposeExecutor
├── EntityPrototypes
├── MqttEvent
├── MqttMessageListener
├── RetryAnalyzer
├── RetryTestListener
├── SeleniumRemoteWebDriverTest
├── TestCoapClient
├── TestCoapClientCallback
├── TestListener
├── TestProperties
├── TestRestClient
├── ThingsBoardDbInstaller
├── WsTelemetryResponse
Serializable
├── «implements» WsTelemetryResponse
WebSocketClient
├── WsClient
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| AbstractContainerTest_c0["AbstractContainerTest"]
    Object______p1["Object/外部框架"] -->|extends| ContainerTestSuite_c1["ContainerTestSuite"]
    Object______p2["Object/外部框架"] -->|extends| DockerComposeContainerImpl_c2["DockerComposeContainerImpl"]
    Object______p3["Object/外部框架"] -->|extends| DockerComposeExecutor_c3["DockerComposeExecutor"]
    Object______p4["Object/外部框架"] -->|extends| SeleniumRemoteWebDriverTest_c4["SeleniumRemoteWebDriverTest"]
    Object______p5["Object/外部框架"] -->|extends| TestCoapClient_c5["TestCoapClient"]
    Object______p6["Object/外部框架"] -->|extends| TestCoapClientCallback_c6["TestCoapClientCallback"]
    CoapHandler_p7["CoapHandler"] -->|implements| TestCoapClientCallback_c7["TestCoapClientCallback"]
    Object______p8["Object/外部框架"] -->|extends| TestListener_c8["TestListener"]
    ITestListener_p9["ITestListener"] -->|implements| TestListener_c9["TestListener"]
    Object______p10["Object/外部框架"] -->|extends| TestProperties_c10["TestProperties"]
    Object______p11["Object/外部框架"] -->|extends| TestRestClient_c11["TestRestClient"]
    Object______p12["Object/外部框架"] -->|extends| ThingsBoardDbInstaller_c12["ThingsBoardDbInstaller"]
    WebSocketClient_p13["WebSocketClient"] -->|extends| WsClient_c13["WsClient"]
    AbstractContainerTest_p14["AbstractContainerTest"] -->|extends| CoapClientTest_c14["CoapClientTest"]
    AbstractContainerTest_p15["AbstractContainerTest"] -->|extends| HttpClientTest_c15["HttpClientTest"]
    AbstractContainerTest_p16["AbstractContainerTest"] -->|extends| MqttClientTest_c16["MqttClientTest"]
    Object______p17["Object/外部框架"] -->|extends| MqttMessageListener_c17["MqttMessageListener"]
    MqttHandler_p18["MqttHandler"] -->|implements| MqttMessageListener_c18["MqttMessageListener"]
    Object______p19["Object/外部框架"] -->|extends| MqttEvent_c19["MqttEvent"]
    AbstractContainerTest_p20["AbstractContainerTest"] -->|extends| MqttGatewayClientTest_c20["MqttGatewayClientTest"]
    Object______p21["Object/外部框架"] -->|extends| AttributesResponse_c21["AttributesResponse"]
    Object______p22["Object/外部框架"] -->|extends| WsTelemetryResponse_c22["WsTelemetryResponse"]
    Serializable_p23["Serializable"] -->|implements| WsTelemetryResponse_c23["WsTelemetryResponse"]
    Object______p24["Object/外部框架"] -->|extends| DevicePrototypes_c24["DevicePrototypes"]
    AbstractContainerTest_p25["AbstractContainerTest"] -->|extends| MqttNodeTest_c25["MqttNodeTest"]
    IMqttMessageListener_p26["IMqttMessageListener"] -->|implements| MqttMessageListener_c26["MqttMessageListener"]
    Object______p27["Object/外部框架"] -->|extends| AbstractBasePage_c27["AbstractBasePage"]
    AbstractContainerTest_p28["AbstractContainerTest"] -->|extends| AbstractDriverBaseTest_c28["AbstractDriverBaseTest"]
    Object______p29["Object/外部框架"] -->|extends| RetryAnalyzer_c29["RetryAnalyzer"]
    IRetryAnalyzer_p30["IRetryAnalyzer"] -->|implements| RetryAnalyzer_c30["RetryAnalyzer"]
    Object______p31["Object/外部框架"] -->|extends| RetryTestListener_c31["RetryTestListener"]
    IAnnotationTransformer_p32["IAnnotationTransformer"] -->|implements| RetryTestListener_c32["RetryTestListener"]
    OtherPageElements_p33["OtherPageElements"] -->|extends| AlarmDetailsEntityTabElements_c33["AlarmDetailsEntityTabElements"]
    AlarmDetailsEntityTabElements_p34["AlarmDetailsEntityTabElements"] -->|extends| AlarmDetailsEntityTabHelper_c34["AlarmDetailsEntityTabHelper"]
    AbstractBasePage_p35["AbstractBasePage"] -->|extends| AlarmDetailsViewElements_c35["AlarmDetailsViewElements"]
    AlarmDetailsViewElements_p36["AlarmDetailsViewElements"] -->|extends| AlarmDetailsViewHelper_c36["AlarmDetailsViewHelper"]
    AlarmDetailsEntityTabHelper_p37["AlarmDetailsEntityTabHelper"] -->|extends| AlarmWidgetElements_c37["AlarmWidgetElements"]
    OtherPageElements_p38["OtherPageElements"] -->|extends| AssetPageElements_c38["AssetPageElements"]
    AssetPageElements_p39["AssetPageElements"] -->|extends| AssetPageHelper_c39["AssetPageHelper"]
    AbstractBasePage_p40["AbstractBasePage"] -->|extends| CreateWidgetPopupElements_c40["CreateWidgetPopupElements"]
    CreateWidgetPopupElements_p41["CreateWidgetPopupElements"] -->|extends| CreateWidgetPopupHelper_c41["CreateWidgetPopupHelper"]
    OtherPageElementsHelper_p42["OtherPageElementsHelper"] -->|extends| CustomerPageElements_c42["CustomerPageElements"]
    CustomerPageElements_p43["CustomerPageElements"] -->|extends| CustomerPageHelper_c43["CustomerPageHelper"]
    OtherPageElementsHelper_p44["OtherPageElementsHelper"] -->|extends| DashboardPageElements_c44["DashboardPageElements"]
    DashboardPageElements_p45["DashboardPageElements"] -->|extends| DashboardPageHelper_c45["DashboardPageHelper"]
    OtherPageElementsHelper_p46["OtherPageElementsHelper"] -->|extends| DevicePageElements_c46["DevicePageElements"]
    DevicePageElements_p47["DevicePageElements"] -->|extends| DevicePageHelper_c47["DevicePageHelper"]
    OtherPageElementsHelper_p48["OtherPageElementsHelper"] -->|extends| EntityViewPageElements_c48["EntityViewPageElements"]
    EntityViewPageElements_p49["EntityViewPageElements"] -->|extends| EntityViewPageHelper_c49["EntityViewPageHelper"]
    AbstractBasePage_p50["AbstractBasePage"] -->|extends| LoginPageElements_c50["LoginPageElements"]
    LoginPageElements_p51["LoginPageElements"] -->|extends| LoginPageHelper_c51["LoginPageHelper"]
    AbstractBasePage_p52["AbstractBasePage"] -->|extends| OpenRuleChainPageElements_c52["OpenRuleChainPageElements"]
    OpenRuleChainPageElements_p53["OpenRuleChainPageElements"] -->|extends| OpenRuleChainPageHelper_c53["OpenRuleChainPageHelper"]
    AbstractBasePage_p54["AbstractBasePage"] -->|extends| OtherPageElements_c54["OtherPageElements"]
    OtherPageElements_p55["OtherPageElements"] -->|extends| OtherPageElementsHelper_c55["OtherPageElementsHelper"]
    OtherPageElementsHelper_p56["OtherPageElementsHelper"] -->|extends| ProfilesPageElements_c56["ProfilesPageElements"]
    ProfilesPageElements_p57["ProfilesPageElements"] -->|extends| ProfilesPageHelper_c57["ProfilesPageHelper"]
    OtherPageElementsHelper_p58["OtherPageElementsHelper"] -->|extends| RuleChainsPageElements_c58["RuleChainsPageElements"]
    RuleChainsPageElements_p59["RuleChainsPageElements"] -->|extends| RuleChainsPageHelper_c59["RuleChainsPageHelper"]
    AbstractBasePage_p60["AbstractBasePage"] -->|extends| SideBarMenuViewElements_c60["SideBarMenuViewElements"]
    SideBarMenuViewElements_p61["SideBarMenuViewElements"] -->|extends| SideBarMenuViewHelper_c61["SideBarMenuViewHelper"]
    AbstractBasePage_p62["AbstractBasePage"] -->|extends| AssignDeviceTabElements_c62["AssignDeviceTabElements"]
    AssignDeviceTabElements_p63["AssignDeviceTabElements"] -->|extends| AssignDeviceTabHelper_c63["AssignDeviceTabHelper"]
    AbstractBasePage_p64["AbstractBasePage"] -->|extends| CreateDeviceTabElements_c64["CreateDeviceTabElements"]
    CreateDeviceTabElements_p65["CreateDeviceTabElements"] -->|extends| CreateDeviceTabHelper_c65["CreateDeviceTabHelper"]
    AbstractDriverBaseTest_p66["AbstractDriverBaseTest"] -->|extends| AbstractAssignTest_c66["AbstractAssignTest"]
    AbstractAssignTest_p67["AbstractAssignTest"] -->|extends| AssignDetailsTabAssignTest_c67["AssignDetailsTabAssignTest"]
    AbstractAssignTest_p68["AbstractAssignTest"] -->|extends| AssignDetailsTabFromCustomerAssignTest_c68["AssignDetailsTabFromCustomerAssignTest"]
    AbstractAssignTest_p69["AbstractAssignTest"] -->|extends| AssignFromAlarmWidgetTest_c69["AssignFromAlarmWidgetTest"]
    AbstractDriverBaseTest_p70["AbstractDriverBaseTest"] -->|extends| AssetProfileEditMenuTest_c70["AssetProfileEditMenuTest"]
    AbstractDriverBaseTest_p71["AbstractDriverBaseTest"] -->|extends| CreateAssetProfileImportTest_c71["CreateAssetProfileImportTest"]
    AbstractDriverBaseTest_p72["AbstractDriverBaseTest"] -->|extends| CreateAssetProfileTest_c72["CreateAssetProfileTest"]
    AbstractDriverBaseTest_p73["AbstractDriverBaseTest"] -->|extends| DeleteAssetProfileTest_c73["DeleteAssetProfileTest"]
    AbstractDriverBaseTest_p74["AbstractDriverBaseTest"] -->|extends| DeleteSeveralAssetProfilesTest_c74["DeleteSeveralAssetProfilesTest"]
    AbstractDriverBaseTest_p75["AbstractDriverBaseTest"] -->|extends| MakeAssetProfileDefaultTest_c75["MakeAssetProfileDefaultTest"]
    AbstractDriverBaseTest_p76["AbstractDriverBaseTest"] -->|extends| SearchAssetProfileTest_c76["SearchAssetProfileTest"]
    AbstractDriverBaseTest_p77["AbstractDriverBaseTest"] -->|extends| SortByNameTest_c77["SortByNameTest"]
    AbstractDriverBaseTest_p78["AbstractDriverBaseTest"] -->|extends| CreateCustomerTest_c78["CreateCustomerTest"]
    AbstractDriverBaseTest_p79["AbstractDriverBaseTest"] -->|extends| CustomerEditMenuTest_c79["CustomerEditMenuTest"]
    AbstractDriverBaseTest_p80["AbstractDriverBaseTest"] -->|extends| DeleteCustomerTest_c80["DeleteCustomerTest"]
    AbstractDriverBaseTest_p81["AbstractDriverBaseTest"] -->|extends| DeleteSeveralCustomerTest_c81["DeleteSeveralCustomerTest"]
    AbstractDriverBaseTest_p82["AbstractDriverBaseTest"] -->|extends| ManageCustomersAssetsTest_c82["ManageCustomersAssetsTest"]
    AbstractDriverBaseTest_p83["AbstractDriverBaseTest"] -->|extends| ManageCustomersDashboardsTest_c83["ManageCustomersDashboardsTest"]
    AbstractDriverBaseTest_p84["AbstractDriverBaseTest"] -->|extends| ManageCustomersDevicesTest_c84["ManageCustomersDevicesTest"]
    AbstractDriverBaseTest_p85["AbstractDriverBaseTest"] -->|extends| ManageCustomersEdgesTest_c85["ManageCustomersEdgesTest"]
    AbstractDriverBaseTest_p86["AbstractDriverBaseTest"] -->|extends| ManageCustomersUsersTest_c86["ManageCustomersUsersTest"]
    AbstractDriverBaseTest_p87["AbstractDriverBaseTest"] -->|extends| SearchCustomerTest_c87["SearchCustomerTest"]
    AbstractDriverBaseTest_p88["AbstractDriverBaseTest"] -->|extends| CreateDeviceProfileImportTest_c88["CreateDeviceProfileImportTest"]
    AbstractDriverBaseTest_p89["AbstractDriverBaseTest"] -->|extends| CreateDeviceProfileTest_c89["CreateDeviceProfileTest"]
    AbstractDriverBaseTest_p90["AbstractDriverBaseTest"] -->|extends| DeleteDeviceProfileTest_c90["DeleteDeviceProfileTest"]
    AbstractDriverBaseTest_p91["AbstractDriverBaseTest"] -->|extends| DeleteSeveralDeviceProfilesTest_c91["DeleteSeveralDeviceProfilesTest"]
    AbstractDriverBaseTest_p92["AbstractDriverBaseTest"] -->|extends| DeviceProfileEditMenuTest_c92["DeviceProfileEditMenuTest"]
    AbstractDriverBaseTest_p93["AbstractDriverBaseTest"] -->|extends| MakeDeviceProfileDefaultTest_c93["MakeDeviceProfileDefaultTest"]
    AbstractDriverBaseTest_p94["AbstractDriverBaseTest"] -->|extends| SearchDeviceProfileTest_c94["SearchDeviceProfileTest"]
    AbstractDriverBaseTest_p95["AbstractDriverBaseTest"] -->|extends| AbstractDeviceTest_c95["AbstractDeviceTest"]
    AbstractDeviceTest_p96["AbstractDeviceTest"] -->|extends| AssignToCustomerTest_c96["AssignToCustomerTest"]
    AbstractDeviceTest_p97["AbstractDeviceTest"] -->|extends| CreateDeviceTest_c97["CreateDeviceTest"]
    AbstractDeviceTest_p98["AbstractDeviceTest"] -->|extends| DeleteDeviceTest_c98["DeleteDeviceTest"]
    AbstractDeviceTest_p99["AbstractDeviceTest"] -->|extends| DeleteSeveralDevicesTest_c99["DeleteSeveralDevicesTest"]
    AbstractDeviceTest_p100["AbstractDeviceTest"] -->|extends| DeviceFilterTest_c100["DeviceFilterTest"]
    AbstractDeviceTest_p101["AbstractDeviceTest"] -->|extends| EditDeviceTest_c101["EditDeviceTest"]
    AbstractDeviceTest_p102["AbstractDeviceTest"] -->|extends| MakeDevicePrivateTest_c102["MakeDevicePrivateTest"]
    AbstractDeviceTest_p103["AbstractDeviceTest"] -->|extends| MakeDevicePublicTest_c103["MakeDevicePublicTest"]
    AbstractDriverBaseTest_p104["AbstractDriverBaseTest"] -->|extends| AbstractRuleChainTest_c104["AbstractRuleChainTest"]
    AbstractRuleChainTest_p105["AbstractRuleChainTest"] -->|extends| CreateRuleChainImportTest_c105["CreateRuleChainImportTest"]
    AbstractRuleChainTest_p106["AbstractRuleChainTest"] -->|extends| CreateRuleChainTest_c106["CreateRuleChainTest"]
    AbstractRuleChainTest_p107["AbstractRuleChainTest"] -->|extends| DeleteRuleChainTest_c107["DeleteRuleChainTest"]
    AbstractRuleChainTest_p108["AbstractRuleChainTest"] -->|extends| DeleteSeveralRuleChainsTest_c108["DeleteSeveralRuleChainsTest"]
    AbstractRuleChainTest_p109["AbstractRuleChainTest"] -->|extends| MakeRuleChainRootTest_c109["MakeRuleChainRootTest"]
    AbstractRuleChainTest_p110["AbstractRuleChainTest"] -->|extends| OpenRuleChainTest_c110["OpenRuleChainTest"]
    AbstractRuleChainTest_p111["AbstractRuleChainTest"] -->|extends| RuleChainEditMenuTest_c111["RuleChainEditMenuTest"]
    AbstractRuleChainTest_p112["AbstractRuleChainTest"] -->|extends| SearchRuleChainTest_c112["SearchRuleChainTest"]
    AbstractRuleChainTest_p113["AbstractRuleChainTest"] -->|extends| SortByNameTest_c113["SortByNameTest"]
    AbstractRuleChainTest_p114["AbstractRuleChainTest"] -->|extends| SortByTimeTest_c114["SortByTimeTest"]
    Object______p115["Object/外部框架"] -->|extends| Const_c115["Const"]
    Object______p116["Object/外部框架"] -->|extends| DataProviderCredential_c116["DataProviderCredential"]
    Object______p117["Object/外部框架"] -->|extends| EntityPrototypes_c117["EntityPrototypes"]
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

- `AbstractContainerTest.ObjectMapper()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.beforeSuite()` (public, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.TestRestClient()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.afterSuite()` (public, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.WsClient()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.Random()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.getExpectedLatestValues()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createGatewayConnectPayload()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createGatewayPayload()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createGatewayTelemetryArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createGatewayTelemetryArray()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createPayload()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createPayload()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.toString()` (public, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.updateDeviceProfileWithProvisioningStrategy()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.AllowCreateNewDevicesDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.DisabledDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)


## 哪些方法可以重写

- `AbstractContainerTest.ObjectMapper()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.beforeSuite()` (public, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.TestRestClient()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.afterSuite()` (public, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.WsClient()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.Random()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.getExpectedLatestValues()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createGatewayConnectPayload()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createGatewayPayload()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createGatewayTelemetryArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createGatewayTelemetryArray()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createPayload()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createPayload()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.toString()` (public, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.updateDeviceProfileWithProvisioningStrategy()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.AllowCreateNewDevicesDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.DisabledDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.ObjectMapper()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.beforeSuite()` (public, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.TestRestClient()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.afterSuite()` (public, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.WsClient()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.Random()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.getExpectedLatestValues()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.createGatewayConnectPayload()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.createGatewayPayload()` (protected, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)


## 哪些方法必须重写

- `AbstractContainerTest.ObjectMapper()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.TestRestClient()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.WsClient()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.Random()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.createGatewayTelemetryArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.AllowCreateNewDevicesDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `AbstractContainerTest.DisabledDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.ObjectMapper()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.TestRestClient()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.WsClient()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.Random()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.createGatewayTelemetryArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonArray()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.JsonObject()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.AllowCreateNewDevicesDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `CmdsType.DisabledDeviceProfileProvisionConfiguration()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/AbstractContainerTest.java`)
- `ContainerTestSuite.ThingsBoardDbInstaller()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/ContainerTestSuite.java`)
- `ContainerTestSuite.File()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/ContainerTestSuite.java`)
- `ContainerTestSuite.File()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/ContainerTestSuite.java`)
- `ContainerTestSuite.File()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/ContainerTestSuite.java`)
- `ContainerTestSuite.File()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/ContainerTestSuite.java`)
- `ContainerTestSuite.File()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/ContainerTestSuite.java`)
- `ContainerTestSuite.RuntimeException()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/ContainerTestSuite.java`)
- `ContainerTestSuite.File()` (package, `msa/black-box-tests/src/test/java/org/thingsboard/server/msa/ContainerTestSuite.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `AbstractContainerTest` (extends)
- `Object/外部框架` -> `ContainerTestSuite` (extends)
- `Object/外部框架` -> `DockerComposeContainerImpl` (extends)
- `Object/外部框架` -> `DockerComposeExecutor` (extends)
- `Object/外部框架` -> `SeleniumRemoteWebDriverTest` (extends)
- `Object/外部框架` -> `TestCoapClient` (extends)
- `Object/外部框架` -> `TestCoapClientCallback` (extends)
- `CoapHandler` -> `TestCoapClientCallback` (implements)
- `Object/外部框架` -> `TestListener` (extends)
- `ITestListener` -> `TestListener` (implements)
- `Object/外部框架` -> `TestProperties` (extends)
- `Object/外部框架` -> `TestRestClient` (extends)
- `Object/外部框架` -> `ThingsBoardDbInstaller` (extends)
- `WebSocketClient` -> `WsClient` (extends)
- `AbstractContainerTest` -> `CoapClientTest` (extends)
- `AbstractContainerTest` -> `HttpClientTest` (extends)
- `AbstractContainerTest` -> `MqttClientTest` (extends)
- `Object/外部框架` -> `MqttMessageListener` (extends)
- `MqttHandler` -> `MqttMessageListener` (implements)
- `Object/外部框架` -> `MqttEvent` (extends)
- `AbstractContainerTest` -> `MqttGatewayClientTest` (extends)
- `Object/外部框架` -> `AttributesResponse` (extends)
- `Object/外部框架` -> `WsTelemetryResponse` (extends)
- `Serializable` -> `WsTelemetryResponse` (implements)
- `Object/外部框架` -> `DevicePrototypes` (extends)
- `AbstractContainerTest` -> `MqttNodeTest` (extends)
- `IMqttMessageListener` -> `MqttMessageListener` (implements)
- `Object/外部框架` -> `AbstractBasePage` (extends)
- `AbstractContainerTest` -> `AbstractDriverBaseTest` (extends)
- `Object/外部框架` -> `RetryAnalyzer` (extends)
- `IRetryAnalyzer` -> `RetryAnalyzer` (implements)
- `Object/外部框架` -> `RetryTestListener` (extends)
- `IAnnotationTransformer` -> `RetryTestListener` (implements)
- `OtherPageElements` -> `AlarmDetailsEntityTabElements` (extends)
- `AlarmDetailsEntityTabElements` -> `AlarmDetailsEntityTabHelper` (extends)
- `AbstractBasePage` -> `AlarmDetailsViewElements` (extends)
- `AlarmDetailsViewElements` -> `AlarmDetailsViewHelper` (extends)
- `AlarmDetailsEntityTabHelper` -> `AlarmWidgetElements` (extends)
- `OtherPageElements` -> `AssetPageElements` (extends)
- `AssetPageElements` -> `AssetPageHelper` (extends)
- 其余 40 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 外部父类/接口

- `CoapHandler`
- `IAnnotationTransformer`
- `IMqttMessageListener`
- `IRetryAnalyzer`
- `ITestListener`
- `MqttHandler`
- `Object/外部框架`
- `Serializable`
- `WebSocketClient`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
