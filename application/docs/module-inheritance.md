# ThingsBoard Server Application 模块继承体系分析

> 生成范围：`application`  
> Maven artifact：`application`  
> Java 类型数量：1176  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
AbstractAuthenticationProcessingFilter
├── JwtTokenAuthenticationProcessingFilter
├── RefreshTokenProcessingFilter
├── RestLoginProcessingFilter
├── RestPublicLoginProcessingFilter
AbstractAuthenticationToken
├── AbstractJwtAuthenticationToken
├── ├── JwtAuthenticationToken
├── ├── MfaAuthenticationToken
├── ├── RefreshAuthenticationToken
AbstractListeningExecutor
├── DbCallbackExecutorService
├── ├── DbUpgradeExecutorService
├── ExternalCallExecutorService
├── GrpcCallbackExecutorService
├── MailExecutorService
├── NotificationExecutorService
├── PasswordResetExecutorService
├── SmsExecutorService
├── VersionControlExecutor
AbstractNoSqlContainer
├── TransportNoSqlTestSuite
AbstractTbActor
├── ContextAwareActor
├── ├── AppActor
├── ├── ComponentActor
├── ├── DeviceActor
├── ├── RuleChainErrorActor
├── ├── RuleChainManagerActor
├── ├── ├── TenantActor
├── ├── StatsActor
AbstractTbQueueConsumerTemplate
├── TestConsumer
AccessControlService
├── «implements» DefaultAccessControlService
AccessDeniedHandler
├── «implements» ThingsboardErrorResponseHandler
AccountStatusException
├── UserPasswordNotValidException
ActorService
├── «implements» DefaultActorService
ApplicationListener
├── AlarmSubscriptionService
├── ├── «implements» DefaultAlarmSubscriptionService
├── DeviceStateService
├── ├── «implements» DefaultDeviceStateService
├── SubscriptionManagerService
├── ├── «implements» DefaultSubscriptionManagerService
├── TbApiUsageStateService
├── ├── «implements» DefaultTbApiUsageStateService
├── TbCoreConsumerService
├── ├── «implements» DefaultTbCoreConsumerService
├── TbRuleEngineConsumerService
├── ├── «implements» DefaultTbRuleEngineConsumerService
├── TelemetrySubscriptionService
├── ├── «implements» DefaultTelemetrySubscriptionService
ArrayList
├── GithubEmailsResponse
AuthenticationDetailsSource
├── «implements» RestAuthenticationDetailsSource
AuthenticationException
├── JwtExpiredTokenException
AuthenticationFailureHandler
├── «implements» RestAwareAuthenticationFailureHandler
AuthenticationProvider
├── «implements» JwtAuthenticationProvider
├── «implements» RefreshTokenAuthenticationProvider
├── «implements» RestAuthenticationProvider
AuthenticationServiceException
├── AuthMethodNotSupportedException
AuthenticationSuccessHandler
├── «implements» RestAwareAuthenticationSuccessHandler
AuthorizationRequestRepository
├── «implements» HttpCookieOAuth2AuthorizationRequestRepository
BaseInstanceEnabler
├── FwLwM2MDevice
├── LwM2mBinaryAppDataContainer
├── LwM2mLocation
├── LwM2mTemperatureSensor
├── Lwm2mServer
├── SimpleLwM2MDevice
├── SwLwM2MDevice
BeanPostProcessor
├── «implements» SpringfoxHandlerProviderBeanPostProcessor
CacheCleanupService
├── «implements» DefaultCacheCleanupService
CaffeineTbTransactionalCache
├── AutoCommitSettingsCaffeineCache
├── RepositorySettingsCaffeineCache
├── SessionCaffeineCache
├── VersionControlTaskCaffeineCache
ClaimDevicesService
├── «implements» ClaimDevicesServiceImpl
Closeable
├── «implements» EdgeGrpcSession
CoapHandler
├── «implements» CoapTestCallback
├── «implements» ├── TestCoapCallbackForRPC
Comparable
├── «implements» AttributeData
├── «implements» TsData
ComponentDiscoveryService
├── «implements» AnnotationComponentDiscoveryService
ComponentLifecycleListener
├── TbEntityViewService
├── ├── «implements» DefaultTbEntityViewService
ConsumerWrapper
├── «implements» ConsumerPerPartitionWrapper
├── «implements» SingleConsumerWrapper
CredentialsExpiredException
├── UserPasswordExpiredException
DataUpdateService
├── «implements» DefaultDataUpdateService
DatabaseEntitiesUpgradeService
├── «implements» SqlDatabaseUpgradeService
DatabaseSchemaService
├── «implements» CassandraAbstractDatabaseSchemaService
├── «implements» ├── CassandraKeyspaceService
├── «implements» ├── CassandraTsDatabaseSchemaService
├── «implements» ├── CassandraTsLatestDatabaseSchemaService
├── EntityDatabaseSchemaService
├── ├── «implements» SqlEntityDatabaseSchemaService
├── NoSqlKeyspaceService
├── ├── «implements» CassandraKeyspaceService
├── «implements» SqlAbstractDatabaseSchemaService
├── «implements» ├── SqlEntityDatabaseSchemaService
├── «implements» ├── SqlTsDatabaseSchemaService
├── «implements» ├── TimescaleTsDatabaseSchemaService
├── TsDatabaseSchemaService
├── ├── «implements» CassandraTsDatabaseSchemaService
├── ├── «implements» SqlTsDatabaseSchemaService
├── ├── «implements» TimescaleTsDatabaseSchemaService
├── TsLatestDatabaseSchemaService
├── ├── «implements» CassandraTsLatestDatabaseSchemaService
DatabaseTsUpgradeService
├── «implements» CassandraTsDatabaseUpgradeService
├── «implements» SqlTsDatabaseUpgradeService
├── «implements» TimescaleTsDatabaseUpgradeService
DefaultNotificationSettingsService
├── TestNotificationSettingsService
Destroyable
├── «implements» FwLwM2MDevice
├── «implements» LwM2mBinaryAppDataContainer
├── «implements» LwM2mLocation
├── «implements» LwM2mTemperatureSensor
├── «implements» SimpleLwM2MDevice
├── «implements» SwLwM2MDevice
DeviceAuthService
├── «implements» DefaultDeviceAuthService
DeviceAwareMsg
├── «implements» TransportToDeviceActorMsgWrapper
DeviceProvisionService
├── «implements» DeviceProvisionServiceImpl
DeviceSessionCacheService
├── «implements» DefaultDeviceSessionCacheService
EdgeEventFetcher
├── «implements» AdminSettingsEdgeEventFetcher
├── «implements» BasePageableEdgeEventFetcher
├── «implements» ├── AssetProfilesEdgeEventFetcher
├── «implements» ├── AssetsEdgeEventFetcher
├── «implements» ├── BaseUsersEdgeEventFetcher
├── «implements» ├── ├── CustomerUsersEdgeEventFetcher
├── «implements» ├── ├── TenantAdminUsersEdgeEventFetcher
├── «implements» ├── BaseWidgetTypesEdgeEventFetcher
├── «implements» ├── ├── SystemWidgetTypesEdgeEventFetcher
├── «implements» ├── ├── TenantWidgetTypesEdgeEventFetcher
├── «implements» ├── BaseWidgetsBundlesEdgeEventFetcher
├── «implements» ├── ├── SystemWidgetsBundlesEdgeEventFetcher
├── «implements» ├── ├── TenantWidgetsBundlesEdgeEventFetcher
├── «implements» ├── DashboardsEdgeEventFetcher
├── «implements» ├── DeviceProfilesEdgeEventFetcher
├── «implements» ├── DevicesEdgeEventFetcher
├── «implements» ├── EntityViewsEdgeEventFetcher
├── «implements» ├── OtaPackagesEdgeEventFetcher
├── «implements» ├── QueuesEdgeEventFetcher
├── «implements» ├── RuleChainsEdgeEventFetcher
├── «implements» ├── TenantEdgeEventFetcher
├── «implements» ├── TenantResourcesEdgeEventFetcher
├── «implements» CustomerEdgeEventFetcher
├── «implements» DefaultProfilesEdgeEventFetcher
├── «implements» GeneralEdgeEventFetcher
EdgeInstallInstructionsService
├── «implements» DefaultEdgeInstallInstructionsService
EdgeNotificationService
├── «implements» DefaultEdgeNotificationService
EdgeRequestsService
├── «implements» DefaultEdgeRequestsService
EdgeRpcService
├── «implements» EdgeGrpcService
EdgeRpcServiceImplBase
├── EdgeGrpcService
EdgeUpgradeInstructionsService
├── «implements» DefaultEdgeUpgradeInstructionsService
EntitiesExportImportService
├── «implements» DefaultEntitiesExportImportService
EntitiesVersionControlService
├── «implements» DefaultEntitiesVersionControlService
EntityQueryService
├── «implements» DefaultEntityQueryService
ErrorController
├── «implements» ThingsboardErrorResponseHandler
Exception
├── AccessDeniedException
├── EntityNotFoundException
├── InternalErrorException
├── InvalidParametersException
├── UnauthorizedException
ExecutorProvider
├── «implements» PubSubRuleNodeExecutorProvider
ExitCodeGenerator
├── «implements» ThingsboardInstallException
ExportableEntitiesService
├── «implements» DefaultExportableEntitiesService
FirebaseService
├── «implements» DefaultFirebaseService
FutureCallback
├── «implements» AlarmUpdateCallback
├── «implements» AttributeSaveCallback
├── «implements» VoidFutureCallback
GatewayNotificationsService
├── «implements» DefaultGatewayNotificationsService
GetTsCmd
├── «implements» EntityHistoryCmd
├── «implements» TimeSeriesCmd
GitVersionControlQueueService
├── «implements» DefaultGitVersionControlQueueService
HashMap
├── AbstractPermissions
├── ├── CustomerUserPermissions
├── ├── SysAdminPermissions
├── ├── TenantAdminPermissions
HouseKeeperService
├── «implements» InMemoryHouseKeeperServiceService
JavaMailSenderImpl
├── TbMailSender
JsInvokeService
├── «implements» MockJsInvokeService
JsInvokeStats
├── «implements» DefaultJsInvokeStats
JwtSettingsService
├── «implements» DefaultJwtSettingsService
JwtSettingsValidator
├── «implements» DefaultJwtSettingsValidator
├── «implements» InstallJwtSettingsValidator
JwtToken
├── «implements» AccessJwtToken
├── «implements» RawAccessJwtToken
LwM2MService
├── «implements» LwM2MServiceImpl
MailService
├── «implements» DefaultMailService
MqttCallback
├── «implements» MqttTestCallback
├── «implements» ├── MqttTestOneWaySequenceCallback
├── «implements» ├── MqttTestSubscribeOnTopicCallback
├── «implements» ├── ├── MqttTestRpcJsonCallback
├── «implements» ├── ├── MqttTestRpcProtoCallback
├── «implements» ├── MqttTestTwoWaySequenceCallback
├── «implements» MqttV5TestCallback
├── «implements» ├── MqttV5TestRpcCallback
├── «implements» SparkplugMqttCallback
MsgConstructor
├── AdminSettingsMsgConstructor
├── ├── «implements» AdminSettingsMsgConstructorV1
├── ├── «implements» AdminSettingsMsgConstructorV2
├── AlarmMsgConstructor
├── ├── «implements» BaseAlarmMsgConstructor
├── ├── «implements» ├── AlarmMsgConstructorV1
├── ├── «implements» ├── AlarmMsgConstructorV2
├── AssetMsgConstructor
├── ├── «implements» BaseAssetMsgConstructor
├── ├── «implements» ├── AssetMsgConstructorV1
├── ├── «implements» ├── AssetMsgConstructorV2
├── CustomerMsgConstructor
├── ├── «implements» BaseCustomerMsgConstructor
├── ├── «implements» ├── CustomerMsgConstructorV1
├── ├── «implements» ├── CustomerMsgConstructorV2
├── DashboardMsgConstructor
├── ├── «implements» BaseDashboardMsgConstructor
├── ├── «implements» ├── DashboardMsgConstructorV1
├── ├── «implements» ├── DashboardMsgConstructorV2
├── DeviceMsgConstructor
├── ├── «implements» BaseDeviceMsgConstructor
├── ├── «implements» ├── DeviceMsgConstructorV1
├── ├── «implements» ├── DeviceMsgConstructorV2
├── EntityViewMsgConstructor
├── ├── «implements» BaseEntityViewMsgConstructor
├── ├── «implements» ├── EntityViewMsgConstructorV1
├── ├── «implements» ├── EntityViewMsgConstructorV2
├── OtaPackageMsgConstructor
├── ├── «implements» BaseOtaPackageMsgConstructor
├── ├── «implements» ├── OtaPackageMsgConstructorV1
├── ├── «implements» ├── OtaPackageMsgConstructorV2
├── QueueMsgConstructor
├── ├── «implements» BaseQueueMsgConstructor
├── ├── «implements» ├── QueueMsgConstructorV1
├── ├── «implements» ├── QueueMsgConstructorV2
├── RelationMsgConstructor
├── ├── «implements» RelationMsgConstructorV1
├── ├── «implements» RelationMsgConstructorV2
├── ResourceMsgConstructor
├── ├── «implements» BaseResourceMsgConstructor
├── ├── «implements» ├── ResourceMsgConstructorV1
├── ├── «implements» ├── ResourceMsgConstructorV2
├── RuleChainMsgConstructor
├── ├── «implements» BaseRuleChainMsgConstructor
├── ├── «implements» ├── RuleChainMsgConstructorV1
├── ├── «implements» ├── RuleChainMsgConstructorV2
├── TenantMsgConstructor
├── ├── «implements» TenantMsgConstructorV1
├── ├── «implements» TenantMsgConstructorV2
├── UserMsgConstructor
├── ├── «implements» BaseUserMsgConstructor
├── ├── «implements» ├── UserMsgConstructorV1
├── ├── «implements» ├── UserMsgConstructorV2
├── WidgetMsgConstructor
├── ├── «implements» BaseWidgetMsgConstructor
├── ├── «implements» ├── WidgetMsgConstructorV1
├── ├── «implements» ├── WidgetMsgConstructorV2
NotificationCenter
├── «implements» DefaultNotificationCenter
NotificationChannel
├── «implements» DefaultNotificationCenter
├── «implements» EmailNotificationChannel
├── «implements» MicrosoftTeamsNotificationChannel
├── «implements» MobileAppNotificationChannel
├── «implements» SlackNotificationChannel
├── «implements» SmsNotificationChannel
NotificationCommandsHandler
├── «implements» DefaultNotificationCommandsHandler
NotificationRuleProcessor
├── «implements» DefaultNotificationRuleProcessor
NotificationRuleTriggerProcessor
├── «implements» AlarmAssignmentTriggerProcessor
├── «implements» AlarmCommentTriggerProcessor
├── «implements» AlarmTriggerProcessor
├── «implements» ApiUsageLimitTriggerProcessor
├── «implements» DeviceActivityTriggerProcessor
├── «implements» EdgeCommunicationFailureTriggerProcessor
├── «implements» EdgeConnectionTriggerProcessor
├── «implements» EntitiesLimitTriggerProcessor
├── «implements» EntityActionTriggerProcessor
├── «implements» NewPlatformVersionTriggerProcessor
├── «implements» RateLimitsTriggerProcessor
├── «implements» RuleEngineComponentLifecycleEventTriggerProcessor
NotificationRulesCache
├── «implements» DefaultNotificationRulesCache
NotificationSchedulerService
├── «implements» DefaultNotificationSchedulerService
OAuth2AuthorizationRequestResolver
├── «implements» CustomOAuth2AuthorizationRequestResolver
OAuth2ClientMapper
├── «implements» AppleOAuth2ClientMapper
├── «implements» BasicOAuth2ClientMapper
├── «implements» CustomOAuth2ClientMapper
├── «implements» GithubOAuth2ClientMapper
Object/外部框架
├── AbstractBulkImportService
├── ├── AssetBulkImportService
├── ├── DeviceBulkImportService
├── ├── EdgeBulkImportService
├── AbstractCassandraDatabaseUpgradeService
├── ├── CassandraTsDatabaseUpgradeService
├── AbstractCleanUpService
├── ├── AuditLogsCleanUpService
├── ├── EdgeEventsCleanUpService
├── ├── EventsCleanUpService
├── ├── NotificationsCleanUpService
├── ├── TimeseriesCleanUpService
├── AbstractConsumerService
├── ├── DefaultTbCoreConsumerService
├── ├── DefaultTbRuleEngineConsumerService
├── AbstractContextAwareMsgProcessor
├── ├── ComponentMsgProcessor
├── ├── ├── RuleChainActorMessageProcessor
├── ├── ├── RuleNodeActorMessageProcessor
├── ├── DeviceActorMessageProcessor
├── AbstractInMemoryStorageTest
├── ├── AbstractWebTest
├── ├── ├── AbstractNotifyEntityTest
├── ├── ├── ├── AbstractControllerTest
├── ├── ├── ├── ├── AbstractEdgeTest
├── ├── ├── ├── ├── ├── AlarmEdgeTest
├── ├── ├── ├── ├── ├── AssetEdgeTest
├── ├── ├── ├── ├── ├── AssetProfileEdgeTest
├── ├── ├── ├── ├── ├── CustomerEdgeTest
├── ├── ├── ├── ├── ├── DashboardEdgeTest
├── ├── ├── ├── ├── ├── DeviceEdgeTest
├── ├── ├── ├── ├── ├── DeviceProfileEdgeTest
├── ├── ├── ├── ├── ├── EdgeTest
├── ├── ├── ├── ├── ├── EntityViewEdgeTest
├── ├── ├── ├── ├── ├── OtaPackageEdgeTest
├── ├── ├── ├── ├── ├── QueueEdgeTest
├── ├── ├── ├── ├── ├── RelationEdgeTest
├── ├── ├── ├── ├── ├── ResourceEdgeTest
├── ├── ├── ├── ├── ├── RuleChainEdgeTest
├── ├── ├── ├── ├── ├── TelemetryEdgeTest
├── ├── ├── ├── ├── ├── TenantEdgeTest
├── ├── ├── ├── ├── ├── TenantProfileEdgeTest
├── ├── ├── ├── ├── ├── UserEdgeTest
├── ├── ├── ├── ├── ├── WidgetEdgeTest
├── ├── ├── ├── ├── AbstractNotificationApiTest
├── ├── ├── ├── ├── ├── NotificationApiTest
├── ├── ├── ├── ├── ├── NotificationRuleApiTest
├── ├── ├── ├── ├── ├── NotificationTargetApiTest
├── ├── ├── ├── ├── ├── NotificationTemplateApiTest
├── ├── ├── ├── ├── AbstractRuleEngineControllerTest
├── ├── ├── ├── ├── ├── AbstractRuleEngineFlowIntegrationTest
├── ├── ├── ├── ├── ├── ├── RuleEngineFlowSqlIntegrationTest
├── ├── ├── ├── ├── ├── AbstractRuleEngineLifecycleIntegrationTest
├── ├── ├── ├── ├── ├── ├── RuleEngineLifecycleSqlIntegrationTest
├── ├── ├── ├── ├── AbstractTransportIntegrationTest
├── ├── ├── ├── ├── ├── AbstractCoapIntegrationTest
├── ├── ├── ├── ├── ├── ├── AbstractCoapAttributesIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapAttributesRequestIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── ├── CoapAttributesRequestJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── ├── CoapAttributesRequestProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapAttributesUpdatesIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapAttributesUpdatesJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapAttributesUpdatesProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── AbstractCoapServerSideRpcIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapServerSideRpcDefaultIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapServerSideRpcJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapServerSideRpcProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── AbstractCoapTimeseriesIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractCoapTimeseriesJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── ├── CoapTimeseriesNoSqlJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── ├── CoapTimeseriesSqlJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractCoapTimeseriesProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── ├── CoapTimeseriesNoSqlProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── ├── CoapTimeseriesSqlProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapTimeseriesNoSqlIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapTimeseriesSqlIntegrationTest
├── ├── ├── ├── ├── ├── ├── CoapAttributesIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapAttributesJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── CoapAttributesProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── CoapClaimDeviceTest
├── ├── ├── ├── ├── ├── ├── ├── CoapClaimJsonDeviceTest
├── ├── ├── ├── ├── ├── ├── ├── CoapClaimProtoDeviceTest
├── ├── ├── ├── ├── ├── ├── CoapClientIntegrationTest
├── ├── ├── ├── ├── ├── ├── CoapProvisionJsonDeviceTest
├── ├── ├── ├── ├── ├── ├── CoapProvisionProtoDeviceTest
├── ├── ├── ├── ├── ├── AbstractLwM2MIntegrationTest
├── ├── ├── ├── ├── ├── ├── AbstractOtaLwM2MIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── OtaLwM2MIntegrationTest
├── ├── ├── ├── ├── ├── ├── AbstractRpcLwM2MIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── RpcLwm2mIntegrationCreateTest
├── ├── ├── ├── ├── ├── ├── ├── RpcLwm2mIntegrationDeleteTest
├── ├── ├── ├── ├── ├── ├── ├── RpcLwm2mIntegrationDiscoverTest
├── ├── ├── ├── ├── ├── ├── ├── RpcLwm2mIntegrationExecuteTest
├── ├── ├── ├── ├── ├── ├── ├── RpcLwm2mIntegrationObserveTest
├── ├── ├── ├── ├── ├── ├── ├── RpcLwm2mIntegrationReadTest
├── ├── ├── ├── ├── ├── ├── ├── RpcLwm2mIntegrationWriteAttributesTest
├── ├── ├── ├── ├── ├── ├── ├── RpcLwm2mIntegrationWriteTest
├── ├── ├── ├── ├── ├── ├── AbstractSecurityLwM2MIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── NoSecLwM2MIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── PskLwm2mIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── RpkLwM2MIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── X509_NoTrustLwM2MIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── X509_TrustLwM2MIntegrationTest
├── ├── ├── ├── ├── ├── AbstractMqttIntegrationTest
├── ├── ├── ├── ├── ├── ├── AbstractMqttAttributesIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttAttributesRequestBackwardCompatibilityIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttAttributesRequestIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttAttributesRequestJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttAttributesRequestProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttAttributesUpdatesBackwardCompatibilityIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttAttributesUpdatesIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttAttributesUpdatesJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttAttributesUpdatesProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── AbstractMqttClientConnectionTest
├── ├── ├── ├── ├── ├── ├── ├── MqttClientConnectionTest
├── ├── ├── ├── ├── ├── ├── AbstractMqttServerSideRpcIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttServerSideRpcBackwardCompatibilityIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttServerSideRpcDefaultIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttServerSideRpcJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttServerSideRpcProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttServerSideRpcSequenceOnAckIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttServerSideRpcSequenceOnResponseIntegrationTest
├── ├── ├── ├── ├── ├── ├── AbstractMqttTimeseriesIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractMqttTimeseriesJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttTimeseriesNoSqlJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttTimeseriesSqlJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractMqttTimeseriesProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttTimeseriesNoSqlProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttTimeseriesSqlProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttTimeseriesNoSqlIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttTimeseriesSqlIntegrationTest
├── ├── ├── ├── ├── ├── ├── AbstractMqttV5ClientConnectionTest
├── ├── ├── ├── ├── ├── ├── ├── MqttV5ClientConnectionTest
├── ├── ├── ├── ├── ├── ├── AbstractMqttV5ClientPublishTest
├── ├── ├── ├── ├── ├── ├── ├── MqttV5ClientPublishTest
├── ├── ├── ├── ├── ├── ├── AbstractMqttV5ClientSparkplugTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractMqttV5ClientSparkplugAttributesTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttV5ClientSparkplugBAttributesInProfileTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttV5ClientSparkplugBAttributesTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractMqttV5ClientSparkplugConnectionTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttV5ClientSparkplugBConnectionTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractMqttV5ClientSparkplugTelemetryTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttV5ClientSparkplugBTelemetryTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractMqttV5RpcSparkplugTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttV5RpcSparkplugTest
├── ├── ├── ├── ├── ├── ├── AbstractMqttV5ClientSubscriptionTest
├── ├── ├── ├── ├── ├── ├── ├── MqttV5ClientSubscriptionTest
├── ├── ├── ├── ├── ├── ├── AbstractMqttV5ClientUnsubscribeTest
├── ├── ├── ├── ├── ├── ├── ├── MqttV5ClientUnsubscribeTest
├── ├── ├── ├── ├── ├── ├── AbstractMqttV5Test
├── ├── ├── ├── ├── ├── ├── ├── AbstractAttributesMqttV5Test
├── ├── ├── ├── ├── ├── ├── ├── ├── AttributesPublishTest
├── ├── ├── ├── ├── ├── ├── ├── ├── AttributesUpdatesTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractMqttV5ClaimTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttV5ClaimTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractMqttV5RpcTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttV5RpcTest
├── ├── ├── ├── ├── ├── ├── ├── AbstractMqttV5TimeseriesTest
├── ├── ├── ├── ├── ├── ├── ├── ├── MqttV5TimeseriesTest
├── ├── ├── ├── ├── ├── ├── ├── MqttV5ProvisionDeviceTest
├── ├── ├── ├── ├── ├── ├── BasicMqttCredentialsTest
├── ├── ├── ├── ├── ├── ├── MqttAttributesIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttAttributesJsonIntegrationTest
├── ├── ├── ├── ├── ├── ├── ├── MqttAttributesProtoIntegrationTest
├── ├── ├── ├── ├── ├── ├── MqttClaimDeviceTest
├── ├── ├── ├── ├── ├── ├── ├── MqttClaimBackwardCompatibilityDeviceTest
├── ├── ├── ├── ├── ├── ├── ├── MqttClaimJsonDeviceTest
├── ├── ├── ├── ├── ├── ├── ├── MqttClaimProtoDeviceTest
├── ├── ├── ├── ├── ├── ├── MqttProvisionJsonDeviceTest
├── ├── ├── ├── ├── ├── ├── MqttProvisionProtoDeviceTest
├── ├── ├── ├── ├── AdminControllerTest
├── ├── ├── ├── ├── AlarmCommentControllerTest
├── ├── ├── ├── ├── AlarmControllerTest
├── ├── ├── ├── ├── AlarmsCleanUpServiceTest
├── ├── ├── ├── ├── AssetControllerTest
├── ├── ├── ├── ├── AssetProfileControllerTest
├── ├── ├── ├── ├── AuditLogControllerTest
├── ├── ├── ├── ├── AuthControllerTest
├── ├── ├── ├── ├── BaseExportImportServiceTest
├── ├── ├── ├── ├── ├── ExportImportServiceSqlTest
├── ├── ├── ├── ├── BaseHttpDeviceApiTest
├── ├── ├── ├── ├── ├── DeviceApiSqlTest
├── ├── ├── ├── ├── BaseQueueControllerTest
├── ├── ├── ├── ├── BaseRestApiLimitsTest
├── ├── ├── ├── ├── ├── RestApiLimitsSqlTest
├── ├── ├── ├── ├── BaseTbResourceServiceTest
├── ├── ├── ├── ├── ComponentDescriptorControllerTest
├── ├── ├── ├── ├── CustomerControllerTest
├── ├── ├── ├── ├── DashboardControllerTest
├── ├── ├── ├── ├── DefaultSmsServiceTest
├── ├── ├── ├── ├── DeviceConnectivityControllerTest
├── ├── ├── ├── ├── DeviceControllerTest
├── ├── ├── ├── ├── DeviceProfileControllerTest
├── ├── ├── ├── ├── DevicesStatisticsTest
├── ├── ├── ├── ├── EdgeControllerTest
├── ├── ├── ├── ├── EdgeEventControllerTest
├── ├── ├── ├── ├── EntityQueryControllerTest
├── ├── ├── ├── ├── EntityRelationControllerTest
├── ├── ├── ├── ├── EntityViewControllerTest
├── ├── ├── ├── ├── HomePageApiTest
├── ├── ├── ├── ├── ImageControllerTest
├── ├── ├── ├── ├── NashornJsInvokeServiceTest
├── ├── ├── ├── ├── Oauth2AuthenticationSuccessHandlerTest
├── ├── ├── ├── ├── OtaPackageControllerTest
├── ├── ├── ├── ├── RpcControllerTest
├── ├── ├── ├── ├── RuleChainControllerTest
├── ├── ├── ├── ├── SequentialTimeseriesPersistenceTest
├── ├── ├── ├── ├── TbResourceControllerTest
├── ├── ├── ├── ├── TbelInvokeServiceTest
├── ├── ├── ├── ├── TelemetryControllerTest
├── ├── ├── ├── ├── TenantControllerTest
├── ├── ├── ├── ├── TenantProfileControllerTest
├── ├── ├── ├── ├── TwoFactorAuthConfigTest
├── ├── ├── ├── ├── TwoFactorAuthTest
├── ├── ├── ├── ├── UserControllerTest
├── ├── ├── ├── ├── WebsocketApiTest
├── ├── ├── ├── ├── WidgetTypeControllerTest
├── ├── ├── ├── ├── WidgetsBundleControllerTest
├── AbstractNotificationSubscription
├── ├── NotificationsCountSubscription
├── ├── NotificationsSubscription
├── AbstractOAuth2ClientMapper
├── ├── AppleOAuth2ClientMapper
├── ├── BasicOAuth2ClientMapper
├── ├── CustomOAuth2ClientMapper
├── ├── GithubOAuth2ClientMapper
├── AbstractPartitionBasedService
├── ├── DefaultDeviceStateService
├── ├── DefaultNotificationSchedulerService
├── ├── DefaultTbApiUsageStateService
├── AbstractSmsSender
├── ├── AwsSmsSender
├── ├── SmppSmsSender
├── ├── TwilioSmsSender
├── AbstractSqlTsDatabaseUpgradeService
├── ├── SqlTsDatabaseUpgradeService
├── ├── TimescaleTsDatabaseUpgradeService
├── AbstractTbEntityService
├── ├── DefaultTbAlarmCommentService
├── ├── DefaultTbAlarmService
├── ├── DefaultTbAssetProfileService
├── ├── DefaultTbAssetService
├── ├── DefaultTbCustomerService
├── ├── DefaultTbDashboardService
├── ├── DefaultTbDeviceProfileService
├── ├── DefaultTbDeviceService
├── ├── DefaultTbEdgeService
├── ├── DefaultTbEntityRelationService
├── ├── DefaultTbEntityViewService
├── ├── DefaultTbImageService
├── ├── DefaultTbOtaPackageService
├── ├── DefaultTbQueueService
├── ├── DefaultTbResourceService
├── ├── DefaultTbRuleChainService
├── ├── DefaultTbTenantProfileService
├── ├── DefaultTbTenantService
├── ├── DefaultUserService
├── ├── DefaultWidgetTypeService
├── ├── DefaultWidgetsBundleService
├── AbstractTbRuleEngineSubmitStrategy
├── ├── BatchTbRuleEngineSubmitStrategy
├── ├── BurstTbRuleEngineSubmitStrategy
├── ├── SequentialByEntityIdTbRuleEngineSubmitStrategy
├── ├── ├── SequentialByOriginatorIdTbRuleEngineSubmitStrategy
├── ├── ├── SequentialByTenantIdTbRuleEngineSubmitStrategy
├── ├── SequentialTbRuleEngineSubmitStrategy
├── AccessJwtToken
├── AccessValidator
├── Action
├── ActionCard
├── ActivateUserRequest
├── ActorSystemContext
├── ActorTerminationMsg
├── AdminSettingsEdgeEventFetcher
├── AdminSettingsMsgConstructorV1
├── AdminSettingsMsgConstructorV2
├── AggHistoryCmd
├── AggKey
├── AggTimeSeriesCmd
├── AlarmAssignmentTriggerProcessor
├── AlarmCommentTriggerProcessor
├── AlarmCountUnsubscribeCmd
├── AlarmDataUnsubscribeCmd
├── AlarmSubscriptionUpdate
├── AlarmTriggerProcessor
├── AlarmUpdateCallback
├── AlarmsCleanUpService
├── AnnotationComponentDiscoveryService
├── ApiUsageLimitTriggerProcessor
├── AppInitMsg
├── AttributeData
├── AttributeSaveCallback
├── AuthCmd
├── BackupCodeTwoFaProvider
├── BaseAlarmMsgConstructor
├── ├── AlarmMsgConstructorV1
├── ├── AlarmMsgConstructorV2
├── BaseApiUsageState
├── ├── CustomerApiUsageState
├── ├── TenantApiUsageState
├── BaseAssetMsgConstructor
├── ├── AssetMsgConstructorV1
├── ├── AssetMsgConstructorV2
├── BaseController
├── ├── AbstractRpcController
├── ├── ├── RpcV1Controller
├── ├── ├── RpcV2Controller
├── ├── AdminController
├── ├── AlarmCommentController
├── ├── AlarmController
├── ├── AssetController
├── ├── AssetProfileController
├── ├── AuditLogController
├── ├── AuthController
├── ├── AutoCommitController
├── ├── ├── WidgetTypeController
├── ├── ComponentDescriptorController
├── ├── CustomerController
├── ├── DashboardController
├── ├── DeviceConnectivityController
├── ├── DeviceController
├── ├── DeviceProfileController
├── ├── EdgeController
├── ├── EdgeEventController
├── ├── EntitiesVersionControlController
├── ├── EntityQueryController
├── ├── EntityRelationController
├── ├── EntityViewController
├── ├── EventController
├── ├── ImageController
├── ├── Lwm2mController
├── ├── MailConfigTemplateController
├── ├── NotificationController
├── ├── NotificationRuleController
├── ├── NotificationTargetController
├── ├── NotificationTemplateController
├── ├── OAuth2ConfigTemplateController
├── ├── OAuth2Controller
├── ├── OtaPackageController
├── ├── QueueController
├── ├── RuleChainController
├── ├── SystemInfoController
├── ├── TbResourceController
├── ├── TelemetryController
├── ├── TenantController
├── ├── TenantProfileController
├── ├── TwoFactorAuthConfigController
├── ├── TwoFactorAuthController
├── ├── UiSettingsController
├── ├── UsageInfoController
├── ├── UserController
├── ├── WidgetsBundleController
├── BaseCustomerMsgConstructor
├── ├── CustomerMsgConstructorV1
├── ├── CustomerMsgConstructorV2
├── BaseDashboardMsgConstructor
├── ├── DashboardMsgConstructorV1
├── ├── DashboardMsgConstructorV2
├── BaseDeviceMsgConstructor
├── ├── DeviceMsgConstructorV1
├── ├── DeviceMsgConstructorV2
├── BaseEdgeProcessor
├── ├── AdminSettingsEdgeProcessor
├── ├── BaseAlarmProcessor
├── ├── ├── AlarmEdgeProcessor
├── ├── ├── ├── AlarmEdgeProcessorV1
├── ├── ├── ├── AlarmEdgeProcessorV2
├── ├── BaseAssetProcessor
├── ├── ├── AssetEdgeProcessor
├── ├── ├── ├── AssetEdgeProcessorV1
├── ├── ├── ├── AssetEdgeProcessorV2
├── ├── BaseAssetProfileProcessor
├── ├── ├── AssetProfileEdgeProcessor
├── ├── ├── ├── AssetProfileEdgeProcessorV1
├── ├── ├── ├── AssetProfileEdgeProcessorV2
├── ├── BaseDashboardProcessor
├── ├── ├── DashboardEdgeProcessor
├── ├── ├── ├── DashboardEdgeProcessorV1
├── ├── ├── ├── DashboardEdgeProcessorV2
├── ├── BaseDeviceProcessor
├── ├── ├── DeviceEdgeProcessor
├── ├── ├── ├── DeviceEdgeProcessorV1
├── ├── ├── ├── DeviceEdgeProcessorV2
├── ├── BaseDeviceProfileProcessor
├── ├── ├── DeviceProfileEdgeProcessor
├── ├── ├── ├── DeviceProfileEdgeProcessorV1
├── ├── ├── ├── DeviceProfileEdgeProcessorV2
├── ├── BaseEntityViewProcessor
├── ├── ├── EntityViewEdgeProcessor
├── ├── ├── ├── EntityViewProcessorV1
├── ├── ├── ├── EntityViewProcessorV2
├── ├── BaseRelationProcessor
├── ├── ├── RelationEdgeProcessor
├── ├── ├── ├── RelationEdgeProcessorV1
├── ├── ├── ├── RelationEdgeProcessorV2
├── ├── BaseResourceProcessor
├── ├── ├── ResourceEdgeProcessor
├── ├── ├── ├── ResourceEdgeProcessorV1
├── ├── ├── ├── ResourceEdgeProcessorV2
├── ├── BaseTelemetryProcessor
├── ├── ├── TelemetryEdgeProcessor
├── ├── CustomerEdgeProcessor
├── ├── EdgeProcessor
├── ├── ├── AlarmProcessor
├── ├── ├── ├── «implements» AlarmEdgeProcessor
├── ├── ├── ├── «implements» ├── AlarmEdgeProcessorV1
├── ├── ├── ├── «implements» ├── AlarmEdgeProcessorV2
├── ├── ├── AssetProcessor
├── ├── ├── ├── «implements» AssetEdgeProcessor
├── ├── ├── ├── «implements» ├── AssetEdgeProcessorV1
├── ├── ├── ├── «implements» ├── AssetEdgeProcessorV2
├── ├── ├── AssetProfileProcessor
├── ├── ├── ├── «implements» AssetProfileEdgeProcessor
├── ├── ├── ├── «implements» ├── AssetProfileEdgeProcessorV1
├── ├── ├── ├── «implements» ├── AssetProfileEdgeProcessorV2
├── ├── ├── DashboardProcessor
├── ├── ├── ├── «implements» DashboardEdgeProcessor
├── ├── ├── ├── «implements» ├── DashboardEdgeProcessorV1
├── ├── ├── ├── «implements» ├── DashboardEdgeProcessorV2
├── ├── ├── DeviceProcessor
├── ├── ├── ├── «implements» DeviceEdgeProcessor
├── ├── ├── ├── «implements» ├── DeviceEdgeProcessorV1
├── ├── ├── ├── «implements» ├── DeviceEdgeProcessorV2
├── ├── ├── DeviceProfileProcessor
├── ├── ├── ├── «implements» DeviceProfileEdgeProcessor
├── ├── ├── ├── «implements» ├── DeviceProfileEdgeProcessorV1
├── ├── ├── ├── «implements» ├── DeviceProfileEdgeProcessorV2
├── ├── ├── EntityViewProcessor
├── ├── ├── ├── «implements» EntityViewEdgeProcessor
├── ├── ├── ├── «implements» ├── EntityViewProcessorV1
├── ├── ├── ├── «implements» ├── EntityViewProcessorV2
├── ├── ├── RelationProcessor
├── ├── ├── ├── «implements» RelationEdgeProcessor
├── ├── ├── ├── «implements» ├── RelationEdgeProcessorV1
├── ├── ├── ├── «implements» ├── RelationEdgeProcessorV2
├── ├── ├── ResourceProcessor
├── ├── ├── ├── «implements» ResourceEdgeProcessor
├── ├── ├── ├── «implements» ├── ResourceEdgeProcessorV1
├── ├── ├── ├── «implements» ├── ResourceEdgeProcessorV2
├── ├── OtaPackageEdgeProcessor
├── ├── QueueEdgeProcessor
├── ├── RuleChainEdgeProcessor
├── ├── TenantEdgeProcessor
├── ├── TenantProfileEdgeProcessor
├── ├── UserEdgeProcessor
├── ├── WidgetBundleEdgeProcessor
├── ├── WidgetTypeEdgeProcessor
├── BaseEdgeProcessorFactory
├── ├── AlarmEdgeProcessorFactory
├── ├── AssetEdgeProcessorFactory
├── ├── AssetProfileEdgeProcessorFactory
├── ├── DashboardEdgeProcessorFactory
├── ├── DeviceEdgeProcessorFactory
├── ├── DeviceProfileEdgeProcessorFactory
├── ├── EntityViewProcessorFactory
├── ├── RelationEdgeProcessorFactory
├── ├── ResourceEdgeProcessorFactory
├── BaseEdgeProcessorTest
├── ├── AbstractAssetProcessorTest
├── ├── ├── AssetEdgeProcessorTest
├── ├── ├── AssetProfileEdgeProcessorTest
├── ├── AbstractDeviceProcessorTest
├── ├── ├── DeviceEdgeProcessorTest
├── ├── ├── DeviceProfileEdgeProcessorTest
├── BaseEntityExportService
├── ├── AssetExportService
├── ├── AssetProfileExportService
├── ├── DashboardExportService
├── ├── DeviceExportService
├── ├── DeviceProfileExportService
├── ├── EntityViewExportService
├── ├── NotificationTargetExportService
├── ├── NotificationTemplateExportService
├── ├── ResourceExportService
├── ├── RuleChainExportService
├── ├── WidgetTypeExportService
├── ├── WidgetsBundleExportService
├── BaseEntityImportService
├── ├── AssetImportService
├── ├── AssetProfileImportService
├── ├── CustomerImportService
├── ├── DashboardImportService
├── ├── DeviceImportService
├── ├── DeviceProfileImportService
├── ├── EntityViewImportService
├── ├── NotificationRuleImportService
├── ├── NotificationTargetImportService
├── ├── NotificationTemplateImportService
├── ├── ResourceImportService
├── ├── RuleChainImportService
├── ├── WidgetTypeImportService
├── ├── WidgetsBundleImportService
├── BaseEntityViewMsgConstructor
├── ├── EntityViewMsgConstructorV1
├── ├── EntityViewMsgConstructorV2
├── BaseMsgConstructorFactory
├── ├── AdminSettingsMsgConstructorFactory
├── ├── AlarmMsgConstructorFactory
├── ├── AssetMsgConstructorFactory
├── ├── CustomerMsgConstructorFactory
├── ├── DashboardMsgConstructorFactory
├── ├── DeviceMsgConstructorFactory
├── ├── EntityViewMsgConstructorFactory
├── ├── OtaPackageMsgConstructorFactory
├── ├── QueueMsgConstructorFactory
├── ├── RelationMsgConstructorFactory
├── ├── ResourceMsgConstructorFactory
├── ├── RuleChainMsgConstructorFactory
├── ├── TenantMsgConstructorFactory
├── ├── UserMsgConstructorFactory
├── ├── WidgetMsgConstructorFactory
├── BaseOtaPackageMsgConstructor
├── ├── OtaPackageMsgConstructorV1
├── ├── OtaPackageMsgConstructorV2
├── BasePageableEdgeEventFetcher
├── ├── AssetProfilesEdgeEventFetcher
├── ├── AssetsEdgeEventFetcher
├── ├── BaseUsersEdgeEventFetcher
├── ├── ├── CustomerUsersEdgeEventFetcher
├── ├── ├── TenantAdminUsersEdgeEventFetcher
├── ├── BaseWidgetTypesEdgeEventFetcher
├── ├── ├── SystemWidgetTypesEdgeEventFetcher
├── ├── ├── TenantWidgetTypesEdgeEventFetcher
├── ├── BaseWidgetsBundlesEdgeEventFetcher
├── ├── ├── SystemWidgetsBundlesEdgeEventFetcher
├── ├── ├── TenantWidgetsBundlesEdgeEventFetcher
├── ├── DashboardsEdgeEventFetcher
├── ├── DeviceProfilesEdgeEventFetcher
├── ├── DevicesEdgeEventFetcher
├── ├── EntityViewsEdgeEventFetcher
├── ├── OtaPackagesEdgeEventFetcher
├── ├── QueuesEdgeEventFetcher
├── ├── RuleChainsEdgeEventFetcher
├── ├── TenantEdgeEventFetcher
├── ├── TenantResourcesEdgeEventFetcher
├── BaseQueueMsgConstructor
├── ├── QueueMsgConstructorV1
├── ├── QueueMsgConstructorV2
├── BaseResourceMsgConstructor
├── ├── ResourceMsgConstructorV1
├── ├── ResourceMsgConstructorV2
├── BaseRuleChainMetadataConstructor
├── ├── RuleChainMetadataConstructorV330
├── ├── RuleChainMetadataConstructorV340
├── ├── RuleChainMetadataConstructorV362
├── BaseRuleChainMsgConstructor
├── ├── RuleChainMsgConstructorV1
├── ├── RuleChainMsgConstructorV2
├── BaseUserMsgConstructor
├── ├── UserMsgConstructorV1
├── ├── UserMsgConstructorV2
├── BaseWidgetMsgConstructor
├── ├── WidgetMsgConstructorV1
├── ├── WidgetMsgConstructorV2
├── BasicMapperUtils
├── CQLStatementsParser
├── CaffeineCacheDefaultConfigurationTest
├── CassandraAbstractDatabaseSchemaService
├── ├── CassandraKeyspaceService
├── ├── CassandraTsDatabaseSchemaService
├── ├── CassandraTsLatestDatabaseSchemaService
├── CassandraToSqlColumn
├── CassandraToSqlColumnData
├── CassandraToSqlTable
├── CassandraTsLatestToSqlMigrateService
├── ChangePasswordRequest
├── Choice
├── ClaimDevicesServiceImpl
├── CmdUpdate
├── ├── AlarmCountUpdate
├── ├── DataUpdate
├── ├── ├── AlarmDataUpdate
├── ├── ├── EntityDataUpdate
├── ├── EntityCountUpdate
├── ├── UnreadNotificationsCountUpdate
├── ├── UnreadNotificationsUpdate
├── CoapTestCallback
├── ├── TestCoapCallbackForRPC
├── CoapTestClient
├── CoapTestConfigProperties
├── Config
├── ConnectivityEventInfo
├── ConsumerPerPartitionWrapper
├── ContextBasedCreator
├── ├── ActorCreator
├── ├── DeviceActorCreator
├── ControllerConstants
├── CookieUtils
├── CookieUtilsTest
├── CryptoConfig
├── CsvUtils
├── CustomOAuth2AuthorizationRequestResolver
├── CustomerEdgeEventFetcher
├── DataCmd
├── ├── AlarmCountCmd
├── ├── AlarmDataCmd
├── ├── EntityCountCmd
├── ├── EntityDataCmd
├── DebugTbRateLimits
├── DefaultAccessControlService
├── DefaultCacheCleanupService
├── DefaultDataUpdateService
├── DefaultDataUpdateServiceTest
├── DefaultDeviceAuthService
├── DefaultDeviceSessionCacheService
├── DefaultDeviceStateServiceTest
├── DefaultEdgeInstallInstructionsService
├── DefaultEdgeNotificationService
├── DefaultEdgeRequestsService
├── DefaultEdgeUpgradeInstructionsService
├── DefaultEntitiesExportImportService
├── DefaultEntitiesVersionControlService
├── DefaultEntityExportService
├── DefaultEntityQueryService
├── DefaultExportableEntitiesService
├── DefaultFirebaseService
├── DefaultGatewayNotificationsService
├── DefaultGitVersionControlQueueService
├── DefaultJsInvokeStats
├── DefaultJwtSettingsService
├── DefaultJwtSettingsValidator
├── DefaultMailService
├── DefaultNotificationCommandsHandler
├── DefaultNotificationRuleProcessor
├── DefaultNotificationRulesCache
├── DefaultOtaPackageStateService
├── DefaultProfilesEdgeEventFetcher
├── DefaultQueueRoutingInfoService
├── DefaultRuleEngineDeviceStateManager
├── DefaultRuleEngineDeviceStateManagerTest
├── DefaultRuleEngineStatisticsService
├── DefaultSlackService
├── DefaultSmsSenderFactory
├── DefaultSmsService
├── DefaultSystemDataLoaderService
├── DefaultSystemSecurityService
├── DefaultTbAlarmCommentServiceTest
├── DefaultTbAlarmServiceTest
├── DefaultTbApiUsageStateServiceTest
├── DefaultTbAssetProfileCache
├── DefaultTbClusterService
├── DefaultTbClusterServiceTest
├── DefaultTbContext
├── DefaultTbCoreConsumerServiceTest
├── DefaultTbCoreDeviceRpcService
├── DefaultTbCoreToTransportService
├── DefaultTbDeviceProfileCache
├── DefaultTbEntityDataSubscriptionService
├── DefaultTbLocalSubscriptionService
├── DefaultTbMailConfigTemplateService
├── DefaultTbNotificationEntityService
├── DefaultTbRuleEngineRpcService
├── DefaultTbUserSettingsService
├── DefaultTenantRoutingInfoService
├── DefaultTokenOutdatingService
├── DefaultTransportApiService
├── DefaultTransportApiServiceTest
├── DefaultTwoFaConfigManager
├── DefaultTwoFactorAuthService
├── DefaultUpdateService
├── DefaultWebSocketService
├── DeviceActivityTriggerProcessor
├── DeviceActorMessageProcessorTest
├── DevicePackFutureHolder
├── DeviceProvisionServiceImpl
├── DeviceProvisionServiceTest
├── DeviceState
├── DeviceStateData
├── DynamicValueKey
├── DynamicValueKeySub
├── EdgeCommunicationFailureTriggerProcessor
├── EdgeConnectionTriggerProcessor
├── EdgeContextComponent
├── EdgeEventSourcingListener
├── EdgeEventStorageSettings
├── EdgeGrpcSession
├── EdgeImitator
├── EdgeMsgConstructor
├── EdgeSessionState
├── EdgeSyncCursor
├── EdgeVersionUtils
├── EmailNotificationChannel
├── EntitiesExportCtx
├── ├── ComplexEntitiesExportCtx
├── ├── EntityTypeExportCtx
├── ├── SimpleEntitiesExportCtx
├── EntitiesImportCtx
├── EntitiesLimitTriggerProcessor
├── EntityActionService
├── EntityActionTriggerProcessor
├── EntityCountUnsubscribeCmd
├── EntityData
├── EntityDataMsgConstructor
├── EntityDataUnsubscribeCmd
├── EntityHistoryCmd
├── EntityIdComparator
├── EventsCleanUpServiceTest
├── Fact
├── FirebaseContext
├── GeneralEdgeEventFetcher
├── GenericPermissionChecker
├── GetHistoryCmd
├── GithubEmailResponse
├── HashPartitionServiceTest
├── HttpCookieOAuth2AuthorizationRequestRepository
├── IdComparator
├── IdMsgPair
├── IdProvider
├── ImagesUpdater
├── ImportedEntityInfo
├── InMemoryHouseKeeperServiceService
├── Input
├── InstallJwtSettingsValidator
├── InstallScripts
├── InstallScriptsTest
├── JwtAuthenticationProvider
├── JwtHeaderTokenExtractor
├── JwtQueryTokenExtractor
├── JwtTokenFactory
├── JwtTokenFactoryTest
├── LatestValueCmd
├── LocalRequestMetaData
├── LoginRequest
├── LoginResponse
├── LwM2MLocationParams
├── LwM2MServiceImpl
├── LwM2MTestClient
├── LwM2mObjectModelUtils
├── LwM2mTransportServerHelperTest
├── Lwm2mTestHelper
├── MarkAllNotificationsAsReadCmd
├── MarkNotificationsAsReadCmd
├── Message
├── MicrosoftTeamsNotificationChannel
├── MiscUtils
├── MobileAppNotificationChannel
├── MockJsInvokeService
├── MqttTestCallback
├── ├── MqttTestOneWaySequenceCallback
├── ├── MqttTestSubscribeOnTopicCallback
├── ├── ├── MqttTestRpcJsonCallback
├── ├── ├── MqttTestRpcProtoCallback
├── ├── MqttTestTwoWaySequenceCallback
├── MqttTestClient
├── MqttTestConfigProperties
├── MqttV5TestCallback
├── ├── MqttV5TestRpcCallback
├── MqttV5TestClient
├── MvcCorsProperties
├── NetworkReceive
├── NewPlatformVersionTriggerProcessor
├── NotificationCmdsWrapper
├── NotificationProcessingContext
├── NotificationRequestUpdate
├── NotificationRuleExportService
├── NotificationUpdate
├── NotificationsCountSubCmd
├── NotificationsSubCmd
├── NotificationsSubscriptionUpdate
├── NotificationsUnsubCmd
├── OAuth2AppTokenFactory
├── OAuth2ClientMapperProvider
├── Otp
├── OtpBasedTwoFaProvider
├── ├── EmailTwoFaProvider
├── ├── SmsTwoFaProvider
├── PaginatedUpdater
├── ParsedValue
├── PendingGitRequest
├── ├── CommitGitRequest
├── ├── ContentsDiffGitRequest
├── ├── EntitiesContentGitRequest
├── ├── EntityContentGitRequest
├── ├── ListBranchesGitRequest
├── ├── ListEntitiesGitRequest
├── ├── ListVersionsGitRequest
├── ├── VersionsDiffGitRequest
├── ├── VoidGitRequest
├── ├── ├── ClearRepositoryGitRequest
├── PendingMsgHolder
├── PubSubRuleNodeExecutorProvider
├── PublicLoginRequest
├── QueueCallbackAdaptor
├── RateLimitServiceTest
├── RateLimitsTriggerProcessor
├── RawAccessJwtToken
├── ReadTsKvQueryInfo
├── RefreshTokenAuthenticationProvider
├── RefreshTokenExpCheckService
├── RefreshTokenRequest
├── ReimportTask
├── RelationMsgConstructorV1
├── RelationMsgConstructorV2
├── RemoteJsInvokeServiceTest
├── ResetPasswordEmailRequest
├── ResetPasswordRequest
├── RestAuthenticationDetails
├── RestAuthenticationDetailsSource
├── RestAuthenticationProvider
├── RestAwareAuthenticationFailureHandler
├── RestAwareAuthenticationSuccessHandler
├── RestTemplateConvertersTest
├── RetryStrategy
├── RpcCleanUpService
├── RpcSubmitStrategyTest
├── RuleChainMetadataConstructorFactory
├── RuleChainMsgConstructorTest
├── RuleEngineComponentActor
├── ├── RuleChainActor
├── ├── RuleNodeActor
├── RuleEngineComponentLifecycleEventTriggerProcessor
├── RuleNodeClassInfo
├── RuleNodeCtx
├── RuleNodeRelation
├── RuleNodeScriptEngine
├── ├── RuleNodeJsScriptEngine
├── ├── RuleNodeTbelScriptEngine
├── ScheduledRequestMetadata
├── SchedulingConfiguration
├── Section
├── SecurityPathOperationSelector
├── SessionEvent
├── SessionInfo
├── SessionInfoMetaData
├── SessionMetaData
├── SessionTimeoutCheckMsg
├── SharedEventLoopGroupService
├── SingleConsumerWrapper
├── SkipPathRequestMatcher
├── SkipStrategy
├── SlackNotificationChannel
├── SmppSmsSenderTest
├── SmsNotificationChannel
├── SparkplugMqttCallback
├── SpringfoxHandlerProviderBeanPostProcessor
├── SqlAbstractDatabaseSchemaService
├── ├── SqlEntityDatabaseSchemaService
├── ├── SqlTsDatabaseSchemaService
├── ├── TimescaleTsDatabaseSchemaService
├── SqlDatabaseUpgradeService
├── SqlEntityDatabaseSchemaServiceTest
├── StatsActorTest
├── StatsCalculationResult
├── StatsPersistMsg
├── StatsPersistMsgTest
├── StatsPersistTick
├── SubscriptionCmd
├── ├── AttributesSubscriptionCmd
├── ├── TimeseriesSubscriptionCmd
├── SubscriptionSchedulerComponent
├── SubscriptionServiceStatistics
├── SubscriptionState
├── SwaggerConfiguration
├── Target
├── TbAbstractDataSubCtx
├── ├── TbAlarmDataSubCtx
├── ├── TbEntityDataSubCtx
├── TbAbstractSubCtx
├── ├── TbAlarmCountSubCtx
├── ├── TbEntityCountSubCtx
├── TbAbstractVersionControlSettingsService
├── ├── DefaultTbAutoCommitSettingsService
├── ├── DefaultTbRepositorySettingsService
├── TbCoreConsumerStats
├── TbCoreStartupService
├── TbCoreTransportApiService
├── TbEntityLocalSubsInfo
├── TbEntityRemoteSubsInfo
├── TbEntitySubEvent
├── TbEntityTypeActorIdPredicate
├── TbEntityUpdatesInfo
├── TbMailContextComponent
├── TbMailSenderTest
├── TbMsgPackCallback
├── TbMsgPackCallbackTest
├── TbMsgPackProcessingContext
├── TbMsgPackProcessingContextTest
├── TbMsgProfilerInfo
├── TbNodeUpgradeUtils
├── TbNodeUpgradeUtilsTest
├── TbPackCallback
├── TbPackProcessingContext
├── TbQueueConsumerManagerTask
├── TbQueueConsumerTask
├── TbRpcService
├── TbRuleEngineConsumerContext
├── TbRuleEngineConsumerStats
├── TbRuleEngineProcessingDecision
├── TbRuleEngineProcessingResult
├── TbRuleEngineProcessingStrategyFactory
├── TbRuleEngineQueueConsumerManager
├── TbRuleEngineQueueConsumerManagerTest
├── TbRuleEngineSecurityConfiguration
├── TbRuleEngineSubmitStrategyFactory
├── TbRuleNodeProfilerInfo
├── TbSubscription
├── ├── TbAlarmsSubscription
├── ├── TbAttributeSubscription
├── ├── TbTimeSeriesSubscription
├── TbSubscriptionUtils
├── TbSubscriptionsInfo
├── TbTenantRuleEngineStats
├── TbTopicWithConsumerPerPartition
├── TbUrlConstants
├── TbWebSocketHandlerTest
├── TbWebSocketPingMsg
├── TbWebSocketTextMsg
├── TelemetryCmdsWrapper
├── TelemetryEdgeProcessorTest
├── TelemetrySaveCallback
├── TelemetrySubscriptionUpdate
├── TelemetryWebSocketTextMsg
├── TenantActorTest
├── TenantMsgConstructorV1
├── TenantMsgConstructorV2
├── TenantQueueKey
├── ThingsboardErrorResponse
├── ├── ThingsboardCredentialsExpiredResponse
├── ├── ThingsboardCredentialsViolationResponse
├── ThingsboardInstallApplication
├── ThingsboardInstallConfiguration
├── ThingsboardInstallService
├── ThingsboardMessageConfiguration
├── ThingsboardSecurityConfiguration
├── ThingsboardServerApplication
├── TimeSeriesCmd
├── ToDeviceRpcRequestMetadata
├── ToServerRpcRequestMetadata
├── TokenOutdatingTest
├── TotpTwoFaProvider
├── TransportToDeviceActorMsgWrapper
├── TsData
├── TwoFaAccountConfigUpdateRequest
├── TwoFaProviderInfo
├── UserPrincipal
├── ValidationCallback
├── ├── HttpValidationCallback
├── ValidationResult
├── VersionControlTaskCacheEntry
├── VoidFutureCallback
├── WebConfig
├── WebSocketConfiguration
├── WebSocketSessionRef
├── WsCmdHandler
├── WsCommandsWrapper
├── WsSessionMetaData
├── name
OncePerRequestFilter
├── RateLimitProcessingFilter
OtaPackageStateService
├── «implements» DefaultOtaPackageStateService
Permissions
├── «implements» AbstractPermissions
├── «implements» ├── CustomerUserPermissions
├── «implements» ├── SysAdminPermissions
├── «implements» ├── TenantAdminPermissions
Predicate
├── «implements» SecurityPathOperationSelector
├── «implements» TbEntityTypeActorIdPredicate
QueueRoutingInfoService
├── «implements» DefaultQueueRoutingInfoService
Receive
├── «implements» NetworkReceive
RedisTbTransactionalCache
├── AutoCommitSettingsRedisCache
├── RepositorySettingsRedisCache
├── SessionRedisCache
├── VersionControlTaskRedisCache
RequestMatcher
├── «implements» SkipPathRequestMatcher
ResponseEntityExceptionHandler
├── ThingsboardErrorResponseHandler
RuleChainAwareMsg
├── «implements» TbToRuleChainActorMsg
├── «implements» ├── RuleChainInputMsg
├── «implements» ├── RuleChainOutputMsg
├── «implements» ├── RuleChainToRuleChainMsg
RuleChainMetadataConstructor
├── «implements» BaseRuleChainMetadataConstructor
├── «implements» ├── RuleChainMetadataConstructorV330
├── «implements» ├── RuleChainMetadataConstructorV340
├── «implements» ├── RuleChainMetadataConstructorV362
RuleEngineAlarmService
├── AlarmSubscriptionService
├── ├── «implements» DefaultAlarmSubscriptionService
RuleEngineApiUsageStateService
├── TbApiUsageStateService
├── ├── «implements» DefaultTbApiUsageStateService
RuleEngineAssetProfileCache
├── TbAssetProfileCache
├── ├── «implements» DefaultTbAssetProfileCache
RuleEngineDeviceProfileCache
├── TbDeviceProfileCache
├── ├── «implements» DefaultTbDeviceProfileCache
RuleEngineDeviceStateManager
├── «implements» DefaultRuleEngineDeviceStateManager
RuleEngineRpcService
├── TbRuleEngineDeviceRpcService
├── ├── «implements» DefaultTbRuleEngineRpcService
RuleEngineStatisticsService
├── «implements» DefaultRuleEngineStatisticsService
RuleEngineTelemetryService
├── InternalTelemetryService
├── ├── TelemetrySubscriptionService
├── ├── ├── «implements» DefaultTelemetrySubscriptionService
RuntimeException
├── ImportServiceException
├── ├── MissingEntityException
├── LoadEntityException
├── ThingsboardInstallException
├── UncheckedApiException
SchedulingConfigurer
├── «implements» SchedulingConfiguration
ScriptEngine
├── «implements» RuleNodeScriptEngine
├── «implements» ├── RuleNodeJsScriptEngine
├── «implements» ├── RuleNodeTbelScriptEngine
SendHandler
├── «implements» SessionMetaData
Serializable
├── «implements» Otp
├── «implements» QueueEvent
├── «implements» RawAccessJwtToken
├── «implements» RestAuthenticationDetails
├── «implements» RuleNodeToRuleChainTellNextMsg
├── ToErrorResponseEntity
├── ├── «implements» AccessDeniedException
├── ├── «implements» EntityNotFoundException
├── ├── «implements» InternalErrorException
├── ├── «implements» InvalidParametersException
├── ├── «implements» UnauthorizedException
├── ├── «implements» UncheckedApiException
├── «implements» TransportToDeviceActorMsgWrapper
├── «implements» UserPrincipal
├── «implements» VersionControlTaskCacheEntry
SimpleTbEntityService
├── TbAssetProfileService
├── ├── «implements» DefaultTbAssetProfileService
├── TbCustomerService
├── ├── «implements» DefaultTbCustomerService
├── TbDashboardService
├── ├── «implements» DefaultTbDashboardService
├── TbDeviceProfileService
├── ├── «implements» DefaultTbDeviceProfileService
├── TbResourceService
├── ├── «implements» DefaultTbResourceService
├── TbRuleChainService
├── ├── «implements» DefaultTbRuleChainService
├── TbWidgetTypeService
├── ├── «implements» DefaultWidgetTypeService
├── TbWidgetsBundleService
├── ├── «implements» DefaultWidgetsBundleService
SimpleUrlAuthenticationFailureHandler
├── Oauth2AuthenticationFailureHandler
SimpleUrlAuthenticationSuccessHandler
├── Oauth2AuthenticationSuccessHandler
SlackService
├── «implements» DefaultSlackService
SmsSender
├── «implements» AbstractSmsSender
├── «implements» ├── AwsSmsSender
├── «implements» ├── SmppSmsSender
├── «implements» ├── TwilioSmsSender
SmsSenderFactory
├── «implements» DefaultSmsSenderFactory
SmsService
├── «implements» DefaultSmsService
SystemDataLoaderService
├── «implements» DefaultSystemDataLoaderService
SystemInfoService
├── «implements» DefaultSystemInfoService
SystemSecurityService
├── «implements» DefaultSystemSecurityService
TbActorCreator
├── «implements» ContextBasedCreator
├── «implements» ├── ActorCreator
├── «implements» ├── DeviceActorCreator
TbActorMsg
├── «implements» AppInitMsg
├── «implements» SessionTimeoutCheckMsg
├── «implements» StatsPersistMsg
├── «implements» StatsPersistTick
├── «implements» TransportToDeviceActorMsgWrapper
TbAlarmCommentService
├── «implements» DefaultTbAlarmCommentService
TbAlarmService
├── «implements» DefaultTbAlarmService
TbApiUsageStateClient
├── TbApiUsageStateService
├── ├── «implements» DefaultTbApiUsageStateService
TbApplicationEventListener
├── AbstractSubscriptionService
├── ├── DefaultAlarmSubscriptionService
├── ├── DefaultNotificationCenter
├── ├── DefaultTelemetrySubscriptionService
├── DefaultActorService
├── DefaultSubscriptionManagerService
├── DefaultSystemInfoService
TbAssetService
├── «implements» DefaultTbAssetService
TbAutoCommitSettingsService
├── «implements» DefaultTbAutoCommitSettingsService
TbCallback
├── «implements» TbPackCallback
TbClusterService
├── «implements» DefaultTbClusterService
TbContext
├── «implements» DefaultTbContext
TbCoreDeviceRpcService
├── «implements» DefaultTbCoreDeviceRpcService
TbCoreToTransportService
├── «implements» DefaultTbCoreToTransportService
TbDeviceService
├── «implements» DefaultTbDeviceService
TbEdgeService
├── «implements» DefaultTbEdgeService
TbEntityDataSubscriptionService
├── «implements» DefaultTbEntityDataSubscriptionService
TbEntityRelationService
├── «implements» DefaultTbEntityRelationService
TbImageService
├── «implements» DefaultTbImageService
TbLocalSubscriptionService
├── «implements» DefaultTbLocalSubscriptionService
TbMailConfigTemplateService
├── «implements» DefaultTbMailConfigTemplateService
TbMsgCallback
├── «implements» TbMsgPackCallback
TbNotificationEntityService
├── «implements» DefaultTbNotificationEntityService
TbOtaPackageService
├── «implements» DefaultTbOtaPackageService
TbQueueCallback
├── «implements» QueueCallbackAdaptor
TbQueueHandler
├── TransportApiService
├── ├── «implements» DefaultTransportApiService
TbQueueService
├── «implements» DefaultTbQueueService
TbRepositorySettingsService
├── «implements» DefaultTbRepositorySettingsService
TbRuleEngineActorMsg
├── RuleNodeToRuleChainTellNextMsg
├── TbToRuleChainActorMsg
├── ├── RuleChainInputMsg
├── ├── RuleChainOutputMsg
├── ├── RuleChainToRuleChainMsg
├── TbToRuleNodeActorMsg
├── ├── RuleChainToRuleNodeMsg
├── ├── RuleNodeToSelfMsg
TbRuleEngineProcessingStrategy
├── «implements» RetryStrategy
├── «implements» SkipStrategy
TbRuleEngineSubmitStrategy
├── «implements» AbstractTbRuleEngineSubmitStrategy
├── «implements» ├── BatchTbRuleEngineSubmitStrategy
├── «implements» ├── BurstTbRuleEngineSubmitStrategy
├── «implements» ├── SequentialByEntityIdTbRuleEngineSubmitStrategy
├── «implements» ├── ├── SequentialByOriginatorIdTbRuleEngineSubmitStrategy
├── «implements» ├── ├── SequentialByTenantIdTbRuleEngineSubmitStrategy
├── «implements» ├── SequentialTbRuleEngineSubmitStrategy
TbTenantProfileService
├── «implements» DefaultTbTenantProfileService
TbTenantService
├── «implements» DefaultTbTenantService
TbUserService
├── «implements» DefaultUserService
TbUserSettingsService
├── «implements» DefaultTbUserSettingsService
TbWebSocketMsg
├── «implements» TbWebSocketPingMsg
├── «implements» TbWebSocketTextMsg
TenantAwareMsg
├── «implements» TransportToDeviceActorMsgWrapper
TenantRoutingInfoService
├── «implements» DefaultTenantRoutingInfoService
TextWebSocketHandler
├── TbWebSocketHandler
TokenExtractor
├── «implements» JwtHeaderTokenExtractor
├── «implements» JwtQueryTokenExtractor
TokenOutdatingService
├── «implements» DefaultTokenOutdatingService
TsLatestMigrateService
├── «implements» CassandraTsLatestToSqlMigrateService
TwoFaConfigManager
├── «implements» DefaultTwoFaConfigManager
TwoFaProvider
├── «implements» BackupCodeTwoFaProvider
├── «implements» TotpTwoFaProvider
TwoFactorAuthService
├── «implements» DefaultTwoFactorAuthService
UpdateService
├── «implements» DefaultUpdateService
User
├── SecurityUser
WebSocketClient
├── TbTestWebSocketClient
├── ├── NotificationApiWsClient
WebSocketConfigurer
├── «implements» WebSocketConfiguration
WebSocketMsgEndpoint
├── «implements» TbWebSocketHandler
WebSocketService
├── «implements» DefaultWebSocketService
WsCmd
├── «implements» AuthCmd
├── «implements» DataCmd
├── «implements» ├── AlarmCountCmd
├── «implements» ├── AlarmDataCmd
├── «implements» ├── EntityCountCmd
├── «implements» ├── EntityDataCmd
├── «implements» MarkAllNotificationsAsReadCmd
├── «implements» MarkNotificationsAsReadCmd
├── «implements» NotificationsCountSubCmd
├── «implements» NotificationsSubCmd
├── «implements» NotificationsUnsubCmd
├── TelemetryPluginCmd
├── ├── «implements» GetHistoryCmd
├── ├── «implements» SubscriptionCmd
├── ├── «implements» ├── AttributesSubscriptionCmd
├── ├── «implements» ├── TimeseriesSubscriptionCmd
├── UnsubscribeCmd
├── ├── «implements» AlarmCountUnsubscribeCmd
├── ├── «implements» AlarmDataUnsubscribeCmd
├── ├── «implements» EntityCountUnsubscribeCmd
├── ├── «implements» EntityDataUnsubscribeCmd
├── ├── «implements» NotificationsUnsubCmd
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| NetworkReceive_c0["NetworkReceive"]
    Receive_p1["Receive"] -->|implements| NetworkReceive_c1["NetworkReceive"]
    Object______p2["Object/外部框架"] -->|extends| ThingsboardInstallApplication_c2["ThingsboardInstallApplication"]
    Object______p3["Object/外部框架"] -->|extends| ThingsboardServerApplication_c3["ThingsboardServerApplication"]
    Object______p4["Object/外部框架"] -->|extends| ActorSystemContext_c4["ActorSystemContext"]
    Object______p5["Object/外部框架"] -->|extends| TbEntityTypeActorIdPredicate_c5["TbEntityTypeActorIdPredicate"]
    Predicate_p6["Predicate"] -->|implements| TbEntityTypeActorIdPredicate_c6["TbEntityTypeActorIdPredicate"]
    ContextAwareActor_p7["ContextAwareActor"] -->|extends| AppActor_c7["AppActor"]
    ContextBasedCreator_p8["ContextBasedCreator"] -->|extends| ActorCreator_c8["ActorCreator"]
    Object______p9["Object/外部框架"] -->|extends| AppInitMsg_c9["AppInitMsg"]
    TbActorMsg_p10["TbActorMsg"] -->|implements| AppInitMsg_c10["AppInitMsg"]
    ContextAwareActor_p11["ContextAwareActor"] -->|extends| DeviceActor_c11["DeviceActor"]
    ContextBasedCreator_p12["ContextBasedCreator"] -->|extends| DeviceActorCreator_c12["DeviceActorCreator"]
    AbstractContextAwareMsgProcessor_p13["AbstractContextAwareMsgProcessor"] -->|extends| DeviceActorMessageProcessor_c13["DeviceActorMessageProcessor"]
    Object______p14["Object/外部框架"] -->|extends| SessionInfo_c14["SessionInfo"]
    Object______p15["Object/外部框架"] -->|extends| SessionInfoMetaData_c15["SessionInfoMetaData"]
    Object______p16["Object/外部框架"] -->|extends| SessionTimeoutCheckMsg_c16["SessionTimeoutCheckMsg"]
    TbActorMsg_p17["TbActorMsg"] -->|implements| SessionTimeoutCheckMsg_c17["SessionTimeoutCheckMsg"]
    Object______p18["Object/外部框架"] -->|extends| ToDeviceRpcRequestMetadata_c18["ToDeviceRpcRequestMetadata"]
    Object______p19["Object/外部框架"] -->|extends| ToServerRpcRequestMetadata_c19["ToServerRpcRequestMetadata"]
    Object______p20["Object/外部框架"] -->|extends| DefaultTbContext_c20["DefaultTbContext"]
    TbContext_p21["TbContext"] -->|implements| DefaultTbContext_c21["DefaultTbContext"]
    RuleEngineComponentActor_p22["RuleEngineComponentActor"] -->|extends| RuleChainActor_c22["RuleChainActor"]
    ComponentMsgProcessor_p23["ComponentMsgProcessor"] -->|extends| RuleChainActorMessageProcessor_c23["RuleChainActorMessageProcessor"]
    TbToRuleChainActorMsg_p24["TbToRuleChainActorMsg"] -->|extends| RuleChainInputMsg_c24["RuleChainInputMsg"]
    ContextAwareActor_p25["ContextAwareActor"] -->|extends| RuleChainManagerActor_c25["RuleChainManagerActor"]
    TbToRuleChainActorMsg_p26["TbToRuleChainActorMsg"] -->|extends| RuleChainOutputMsg_c26["RuleChainOutputMsg"]
    TbToRuleChainActorMsg_p27["TbToRuleChainActorMsg"] -->|extends| RuleChainToRuleChainMsg_c27["RuleChainToRuleChainMsg"]
    TbToRuleNodeActorMsg_p28["TbToRuleNodeActorMsg"] -->|extends| RuleChainToRuleNodeMsg_c28["RuleChainToRuleNodeMsg"]
    Object______p29["Object/外部框架"] -->|extends| RuleEngineComponentActor_c29["RuleEngineComponentActor"]
    RuleEngineComponentActor_p30["RuleEngineComponentActor"] -->|extends| RuleNodeActor_c30["RuleNodeActor"]
    ComponentMsgProcessor_p31["ComponentMsgProcessor"] -->|extends| RuleNodeActorMessageProcessor_c31["RuleNodeActorMessageProcessor"]
    Object______p32["Object/外部框架"] -->|extends| RuleNodeCtx_c32["RuleNodeCtx"]
    Object______p33["Object/外部框架"] -->|extends| RuleNodeRelation_c33["RuleNodeRelation"]
    TbRuleEngineActorMsg_p34["TbRuleEngineActorMsg"] -->|extends| RuleNodeToRuleChainTellNextMsg_c34["RuleNodeToRuleChainTellNextMsg"]
    Serializable_p35["Serializable"] -->|implements| RuleNodeToRuleChainTellNextMsg_c35["RuleNodeToRuleChainTellNextMsg"]
    TbToRuleNodeActorMsg_p36["TbToRuleNodeActorMsg"] -->|extends| RuleNodeToSelfMsg_c36["RuleNodeToSelfMsg"]
    TbRuleEngineActorMsg_p37["TbRuleEngineActorMsg"] -->|extends| TbToRuleChainActorMsg_c37["TbToRuleChainActorMsg"]
    RuleChainAwareMsg_p38["RuleChainAwareMsg"] -->|implements| TbToRuleChainActorMsg_c38["TbToRuleChainActorMsg"]
    TbRuleEngineActorMsg_p39["TbRuleEngineActorMsg"] -->|extends| TbToRuleNodeActorMsg_c39["TbToRuleNodeActorMsg"]
    ContextAwareActor_p40["ContextAwareActor"] -->|extends| ComponentActor_c40["ComponentActor"]
    AbstractTbActor_p41["AbstractTbActor"] -->|extends| ContextAwareActor_c41["ContextAwareActor"]
    Object______p42["Object/外部框架"] -->|extends| ContextBasedCreator_c42["ContextBasedCreator"]
    TbActorCreator_p43["TbActorCreator"] -->|implements| ContextBasedCreator_c43["ContextBasedCreator"]
    TbApplicationEventListener_p44["TbApplicationEventListener"] -->|extends| DefaultActorService_c44["DefaultActorService"]
    ActorService_p45["ActorService"] -->|implements| DefaultActorService_c45["DefaultActorService"]
    Object______p46["Object/外部框架"] -->|extends| AbstractContextAwareMsgProcessor_c46["AbstractContextAwareMsgProcessor"]
    Object______p47["Object/外部框架"] -->|extends| ActorTerminationMsg_c47["ActorTerminationMsg"]
    AbstractContextAwareMsgProcessor_p48["AbstractContextAwareMsgProcessor"] -->|extends| ComponentMsgProcessor_c48["ComponentMsgProcessor"]
    ContextAwareActor_p49["ContextAwareActor"] -->|extends| RuleChainErrorActor_c49["RuleChainErrorActor"]
    ContextAwareActor_p50["ContextAwareActor"] -->|extends| StatsActor_c50["StatsActor"]
    Object______p51["Object/外部框架"] -->|extends| StatsPersistMsg_c51["StatsPersistMsg"]
    TbActorMsg_p52["TbActorMsg"] -->|implements| StatsPersistMsg_c52["StatsPersistMsg"]
    Object______p53["Object/外部框架"] -->|extends| StatsPersistTick_c53["StatsPersistTick"]
    TbActorMsg_p54["TbActorMsg"] -->|implements| StatsPersistTick_c54["StatsPersistTick"]
    Object______p55["Object/外部框架"] -->|extends| DebugTbRateLimits_c55["DebugTbRateLimits"]
    RuleChainManagerActor_p56["RuleChainManagerActor"] -->|extends| TenantActor_c56["TenantActor"]
    Object______p57["Object/外部框架"] -->|extends| CryptoConfig_c57["CryptoConfig"]
    Object______p58["Object/外部框架"] -->|extends| CustomOAuth2AuthorizationRequestResolver_c58["CustomOAuth2AuthorizationRequestResolver"]
    OAuth2AuthorizationRequestResolver_p59["OAuth2AuthorizationRequestResolver"] -->|implements| CustomOAuth2AuthorizationRequestResolver_c59["CustomOAuth2AuthorizationRequestResolver"]
    Object______p60["Object/外部框架"] -->|extends| MvcCorsProperties_c60["MvcCorsProperties"]
    OncePerRequestFilter_p61["OncePerRequestFilter"] -->|extends| RateLimitProcessingFilter_c61["RateLimitProcessingFilter"]
    Object______p62["Object/外部框架"] -->|extends| SchedulingConfiguration_c62["SchedulingConfiguration"]
    SchedulingConfigurer_p63["SchedulingConfigurer"] -->|implements| SchedulingConfiguration_c63["SchedulingConfiguration"]
    Object______p64["Object/外部框架"] -->|extends| SwaggerConfiguration_c64["SwaggerConfiguration"]
    Object______p65["Object/外部框架"] -->|extends| SecurityPathOperationSelector_c65["SecurityPathOperationSelector"]
    Predicate_p66["Predicate"] -->|implements| SecurityPathOperationSelector_c66["SecurityPathOperationSelector"]
    Object______p67["Object/外部框架"] -->|extends| TbRuleEngineSecurityConfiguration_c67["TbRuleEngineSecurityConfiguration"]
    Object______p68["Object/外部框架"] -->|extends| ThingsboardMessageConfiguration_c68["ThingsboardMessageConfiguration"]
    Object______p69["Object/外部框架"] -->|extends| ThingsboardSecurityConfiguration_c69["ThingsboardSecurityConfiguration"]
    Object______p70["Object/外部框架"] -->|extends| WebConfig_c70["WebConfig"]
    Object______p71["Object/外部框架"] -->|extends| WebSocketConfiguration_c71["WebSocketConfiguration"]
    WebSocketConfigurer_p72["WebSocketConfigurer"] -->|implements| WebSocketConfiguration_c72["WebSocketConfiguration"]
    BaseController_p73["BaseController"] -->|extends| AbstractRpcController_c73["AbstractRpcController"]
    BaseController_p74["BaseController"] -->|extends| AdminController_c74["AdminController"]
    BaseController_p75["BaseController"] -->|extends| AlarmCommentController_c75["AlarmCommentController"]
    BaseController_p76["BaseController"] -->|extends| AlarmController_c76["AlarmController"]
    BaseController_p77["BaseController"] -->|extends| AssetController_c77["AssetController"]
    BaseController_p78["BaseController"] -->|extends| AssetProfileController_c78["AssetProfileController"]
    BaseController_p79["BaseController"] -->|extends| AuditLogController_c79["AuditLogController"]
    BaseController_p80["BaseController"] -->|extends| AuthController_c80["AuthController"]
    BaseController_p81["BaseController"] -->|extends| AutoCommitController_c81["AutoCommitController"]
    Object______p82["Object/外部框架"] -->|extends| BaseController_c82["BaseController"]
    BaseController_p83["BaseController"] -->|extends| ComponentDescriptorController_c83["ComponentDescriptorController"]
    Object______p84["Object/外部框架"] -->|extends| name_c84["name"]
    Object______p85["Object/外部框架"] -->|extends| ControllerConstants_c85["ControllerConstants"]
    BaseController_p86["BaseController"] -->|extends| CustomerController_c86["CustomerController"]
    BaseController_p87["BaseController"] -->|extends| DashboardController_c87["DashboardController"]
    BaseController_p88["BaseController"] -->|extends| DeviceConnectivityController_c88["DeviceConnectivityController"]
    BaseController_p89["BaseController"] -->|extends| DeviceController_c89["DeviceController"]
    BaseController_p90["BaseController"] -->|extends| DeviceProfileController_c90["DeviceProfileController"]
    BaseController_p91["BaseController"] -->|extends| EdgeController_c91["EdgeController"]
    BaseController_p92["BaseController"] -->|extends| EdgeEventController_c92["EdgeEventController"]
    BaseController_p93["BaseController"] -->|extends| EntitiesVersionControlController_c93["EntitiesVersionControlController"]
    BaseController_p94["BaseController"] -->|extends| EntityQueryController_c94["EntityQueryController"]
    BaseController_p95["BaseController"] -->|extends| EntityRelationController_c95["EntityRelationController"]
    BaseController_p96["BaseController"] -->|extends| EntityViewController_c96["EntityViewController"]
    BaseController_p97["BaseController"] -->|extends| EventController_c97["EventController"]
    ValidationCallback_p98["ValidationCallback"] -->|extends| HttpValidationCallback_c98["HttpValidationCallback"]
    BaseController_p99["BaseController"] -->|extends| ImageController_c99["ImageController"]
    BaseController_p100["BaseController"] -->|extends| Lwm2mController_c100["Lwm2mController"]
    BaseController_p101["BaseController"] -->|extends| MailConfigTemplateController_c101["MailConfigTemplateController"]
    BaseController_p102["BaseController"] -->|extends| NotificationController_c102["NotificationController"]
    BaseController_p103["BaseController"] -->|extends| NotificationRuleController_c103["NotificationRuleController"]
    BaseController_p104["BaseController"] -->|extends| NotificationTargetController_c104["NotificationTargetController"]
    BaseController_p105["BaseController"] -->|extends| NotificationTemplateController_c105["NotificationTemplateController"]
    BaseController_p106["BaseController"] -->|extends| OAuth2ConfigTemplateController_c106["OAuth2ConfigTemplateController"]
    BaseController_p107["BaseController"] -->|extends| OAuth2Controller_c107["OAuth2Controller"]
    BaseController_p108["BaseController"] -->|extends| OtaPackageController_c108["OtaPackageController"]
    BaseController_p109["BaseController"] -->|extends| QueueController_c109["QueueController"]
    AbstractRpcController_p110["AbstractRpcController"] -->|extends| RpcV1Controller_c110["RpcV1Controller"]
    AbstractRpcController_p111["AbstractRpcController"] -->|extends| RpcV2Controller_c111["RpcV2Controller"]
    BaseController_p112["BaseController"] -->|extends| RuleChainController_c112["RuleChainController"]
    BaseController_p113["BaseController"] -->|extends| SystemInfoController_c113["SystemInfoController"]
    BaseController_p114["BaseController"] -->|extends| TbResourceController_c114["TbResourceController"]
    Object______p115["Object/外部框架"] -->|extends| TbUrlConstants_c115["TbUrlConstants"]
    BaseController_p116["BaseController"] -->|extends| TelemetryController_c116["TelemetryController"]
    BaseController_p117["BaseController"] -->|extends| TenantController_c117["TenantController"]
    BaseController_p118["BaseController"] -->|extends| TenantProfileController_c118["TenantProfileController"]
    BaseController_p119["BaseController"] -->|extends| TwoFactorAuthConfigController_c119["TwoFactorAuthConfigController"]
    Object______p120["Object/外部框架"] -->|extends| TwoFaAccountConfigUpdateRequest_c120["TwoFaAccountConfigUpdateRequest"]
    BaseController_p121["BaseController"] -->|extends| TwoFactorAuthController_c121["TwoFactorAuthController"]
    Object______p122["Object/外部框架"] -->|extends| TwoFaProviderInfo_c122["TwoFaProviderInfo"]
    BaseController_p123["BaseController"] -->|extends| UiSettingsController_c123["UiSettingsController"]
    BaseController_p124["BaseController"] -->|extends| UsageInfoController_c124["UsageInfoController"]
    BaseController_p125["BaseController"] -->|extends| UserController_c125["UserController"]
    AutoCommitController_p126["AutoCommitController"] -->|extends| WidgetTypeController_c126["WidgetTypeController"]
    BaseController_p127["BaseController"] -->|extends| WidgetsBundleController_c127["WidgetsBundleController"]
    TextWebSocketHandler_p128["TextWebSocketHandler"] -->|extends| TbWebSocketHandler_c128["TbWebSocketHandler"]
    WebSocketMsgEndpoint_p129["WebSocketMsgEndpoint"] -->|implements| TbWebSocketHandler_c129["TbWebSocketHandler"]
    Object______p130["Object/外部框架"] -->|extends| SessionMetaData_c130["SessionMetaData"]
    SendHandler_p131["SendHandler"] -->|implements| SessionMetaData_c131["SessionMetaData"]
    Object______p132["Object/外部框架"] -->|extends| TbWebSocketPingMsg_c132["TbWebSocketPingMsg"]
    TbWebSocketMsg_p133["TbWebSocketMsg"] -->|implements| TbWebSocketPingMsg_c133["TbWebSocketPingMsg"]
    Object______p134["Object/外部框架"] -->|extends| TbWebSocketTextMsg_c134["TbWebSocketTextMsg"]
    TbWebSocketMsg_p135["TbWebSocketMsg"] -->|implements| TbWebSocketTextMsg_c135["TbWebSocketTextMsg"]
    Exception_p136["Exception"] -->|extends| AccessDeniedException_c136["AccessDeniedException"]
    ToErrorResponseEntity_p137["ToErrorResponseEntity"] -->|implements| AccessDeniedException_c137["AccessDeniedException"]
    Exception_p138["Exception"] -->|extends| EntityNotFoundException_c138["EntityNotFoundException"]
    ToErrorResponseEntity_p139["ToErrorResponseEntity"] -->|implements| EntityNotFoundException_c139["EntityNotFoundException"]
    Exception_p140["Exception"] -->|extends| InternalErrorException_c140["InternalErrorException"]
    ToErrorResponseEntity_p141["ToErrorResponseEntity"] -->|implements| InternalErrorException_c141["InternalErrorException"]
    Exception_p142["Exception"] -->|extends| InvalidParametersException_c142["InvalidParametersException"]
    ToErrorResponseEntity_p143["ToErrorResponseEntity"] -->|implements| InvalidParametersException_c143["InvalidParametersException"]
    ThingsboardErrorResponse_p144["ThingsboardErrorResponse"] -->|extends| ThingsboardCredentialsExpiredResponse_c144["ThingsboardCredentialsExpiredResponse"]
    ThingsboardErrorResponse_p145["ThingsboardErrorResponse"] -->|extends| ThingsboardCredentialsViolationResponse_c145["ThingsboardCredentialsViolationResponse"]
    Object______p146["Object/外部框架"] -->|extends| ThingsboardErrorResponse_c146["ThingsboardErrorResponse"]
    ResponseEntityExceptionHandler_p147["ResponseEntityExceptionHandler"] -->|extends| ThingsboardErrorResponseHandler_c147["ThingsboardErrorResponseHandler"]
    AccessDeniedHandler_p148["AccessDeniedHandler"] -->|implements| ThingsboardErrorResponseHandler_c148["ThingsboardErrorResponseHandler"]
    ErrorController_p149["ErrorController"] -->|implements| ThingsboardErrorResponseHandler_c149["ThingsboardErrorResponseHandler"]
    Serializable_p150["Serializable"] -->|extends| ToErrorResponseEntity_c150["ToErrorResponseEntity"]
    Exception_p151["Exception"] -->|extends| UnauthorizedException_c151["UnauthorizedException"]
    ToErrorResponseEntity_p152["ToErrorResponseEntity"] -->|implements| UnauthorizedException_c152["UnauthorizedException"]
    RuntimeException_p153["RuntimeException"] -->|extends| UncheckedApiException_c153["UncheckedApiException"]
    ToErrorResponseEntity_p154["ToErrorResponseEntity"] -->|implements| UncheckedApiException_c154["UncheckedApiException"]
    Object______p155["Object/外部框架"] -->|extends| ThingsboardInstallConfiguration_c155["ThingsboardInstallConfiguration"]
    RuntimeException_p156["RuntimeException"] -->|extends| ThingsboardInstallException_c156["ThingsboardInstallException"]
    ExitCodeGenerator_p157["ExitCodeGenerator"] -->|implements| ThingsboardInstallException_c157["ThingsboardInstallException"]
    Object______p158["Object/外部框架"] -->|extends| ThingsboardInstallService_c158["ThingsboardInstallService"]
    Object______p159["Object/外部框架"] -->|extends| EntityActionService_c159["EntityActionService"]
    Object______p160["Object/外部框架"] -->|extends| BaseApiUsageState_c160["BaseApiUsageState"]
    Object______p161["Object/外部框架"] -->|extends| StatsCalculationResult_c161["StatsCalculationResult"]
    BaseApiUsageState_p162["BaseApiUsageState"] -->|extends| CustomerApiUsageState_c162["CustomerApiUsageState"]
    AbstractPartitionBasedService_p163["AbstractPartitionBasedService"] -->|extends| DefaultTbApiUsageStateService_c163["DefaultTbApiUsageStateService"]
    TbApiUsageStateService_p164["TbApiUsageStateService"] -->|implements| DefaultTbApiUsageStateService_c164["DefaultTbApiUsageStateService"]
    TbApiUsageStateClient_p165["TbApiUsageStateClient"] -->|extends| TbApiUsageStateService_c165["TbApiUsageStateService"]
    RuleEngineApiUsageStateService_p166["RuleEngineApiUsageStateService"] -->|extends| TbApiUsageStateService_c166["TbApiUsageStateService"]
    ApplicationListener_p167["ApplicationListener"] -->|extends| TbApiUsageStateService_c167["TbApiUsageStateService"]
    BaseApiUsageState_p168["BaseApiUsageState"] -->|extends| TenantApiUsageState_c168["TenantApiUsageState"]
    AbstractBulkImportService_p169["AbstractBulkImportService"] -->|extends| AssetBulkImportService_c169["AssetBulkImportService"]
    Object______p170["Object/外部框架"] -->|extends| AnnotationComponentDiscoveryService_c170["AnnotationComponentDiscoveryService"]
    ComponentDiscoveryService_p171["ComponentDiscoveryService"] -->|implements| AnnotationComponentDiscoveryService_c171["AnnotationComponentDiscoveryService"]
    Object______p172["Object/外部框架"] -->|extends| RuleNodeClassInfo_c172["RuleNodeClassInfo"]
    Object______p173["Object/外部框架"] -->|extends| ClaimDevicesServiceImpl_c173["ClaimDevicesServiceImpl"]
    ClaimDevicesService_p174["ClaimDevicesService"] -->|implements| ClaimDevicesServiceImpl_c174["ClaimDevicesServiceImpl"]
    AbstractBulkImportService_p175["AbstractBulkImportService"] -->|extends| DeviceBulkImportService_c175["DeviceBulkImportService"]
    Object______p176["Object/外部框架"] -->|extends| DeviceProvisionServiceImpl_c176["DeviceProvisionServiceImpl"]
    DeviceProvisionService_p177["DeviceProvisionService"] -->|implements| DeviceProvisionServiceImpl_c177["DeviceProvisionServiceImpl"]
    Object______p178["Object/外部框架"] -->|extends| DefaultEdgeNotificationService_c178["DefaultEdgeNotificationService"]
    EdgeNotificationService_p179["EdgeNotificationService"] -->|implements| DefaultEdgeNotificationService_c179["DefaultEdgeNotificationService"]
    AbstractBulkImportService_p180["AbstractBulkImportService"] -->|extends| EdgeBulkImportService_c180["EdgeBulkImportService"]
    Object______p181["Object/外部框架"] -->|extends| EdgeContextComponent_c181["EdgeContextComponent"]
    Object______p182["Object/外部框架"] -->|extends| EdgeEventSourcingListener_c182["EdgeEventSourcingListener"]
    Object______p183["Object/外部框架"] -->|extends| DefaultEdgeInstallInstructionsService_c183["DefaultEdgeInstallInstructionsService"]
    EdgeInstallInstructionsService_p184["EdgeInstallInstructionsService"] -->|implements| DefaultEdgeInstallInstructionsService_c184["DefaultEdgeInstallInstructionsService"]
    Object______p185["Object/外部框架"] -->|extends| DefaultEdgeUpgradeInstructionsService_c185["DefaultEdgeUpgradeInstructionsService"]
    EdgeUpgradeInstructionsService_p186["EdgeUpgradeInstructionsService"] -->|implements| DefaultEdgeUpgradeInstructionsService_c186["DefaultEdgeUpgradeInstructionsService"]
    Object______p187["Object/外部框架"] -->|extends| EdgeEventStorageSettings_c187["EdgeEventStorageSettings"]
    EdgeRpcServiceImplBase_p188["EdgeRpcServiceImplBase"] -->|extends| EdgeGrpcService_c188["EdgeGrpcService"]
    EdgeRpcService_p189["EdgeRpcService"] -->|implements| EdgeGrpcService_c189["EdgeGrpcService"]
    Object______p190["Object/外部框架"] -->|extends| AttributeSaveCallback_c190["AttributeSaveCallback"]
    FutureCallback_p191["FutureCallback"] -->|implements| AttributeSaveCallback_c191["AttributeSaveCallback"]
    Object______p192["Object/外部框架"] -->|extends| EdgeGrpcSession_c192["EdgeGrpcSession"]
    Closeable_p193["Closeable"] -->|implements| EdgeGrpcSession_c193["EdgeGrpcSession"]
    Object______p194["Object/外部框架"] -->|extends| EdgeSessionState_c194["EdgeSessionState"]
    Object______p195["Object/外部框架"] -->|extends| EdgeSyncCursor_c195["EdgeSyncCursor"]
    Object______p196["Object/外部框架"] -->|extends| BaseMsgConstructorFactory_c196["BaseMsgConstructorFactory"]
    MsgConstructor_p197["MsgConstructor"] -->|extends| AlarmMsgConstructor_c197["AlarmMsgConstructor"]
    BaseMsgConstructorFactory_p198["BaseMsgConstructorFactory"] -->|extends| AlarmMsgConstructorFactory_c198["AlarmMsgConstructorFactory"]
    BaseAlarmMsgConstructor_p199["BaseAlarmMsgConstructor"] -->|extends| AlarmMsgConstructorV1_c199["AlarmMsgConstructorV1"]
    BaseAlarmMsgConstructor_p200["BaseAlarmMsgConstructor"] -->|extends| AlarmMsgConstructorV2_c200["AlarmMsgConstructorV2"]
    Object______p201["Object/外部框架"] -->|extends| BaseAlarmMsgConstructor_c201["BaseAlarmMsgConstructor"]
    AlarmMsgConstructor_p202["AlarmMsgConstructor"] -->|implements| BaseAlarmMsgConstructor_c202["BaseAlarmMsgConstructor"]
    MsgConstructor_p203["MsgConstructor"] -->|extends| AssetMsgConstructor_c203["AssetMsgConstructor"]
    BaseMsgConstructorFactory_p204["BaseMsgConstructorFactory"] -->|extends| AssetMsgConstructorFactory_c204["AssetMsgConstructorFactory"]
    BaseAssetMsgConstructor_p205["BaseAssetMsgConstructor"] -->|extends| AssetMsgConstructorV1_c205["AssetMsgConstructorV1"]
    BaseAssetMsgConstructor_p206["BaseAssetMsgConstructor"] -->|extends| AssetMsgConstructorV2_c206["AssetMsgConstructorV2"]
    Object______p207["Object/外部框架"] -->|extends| BaseAssetMsgConstructor_c207["BaseAssetMsgConstructor"]
    AssetMsgConstructor_p208["AssetMsgConstructor"] -->|implements| BaseAssetMsgConstructor_c208["BaseAssetMsgConstructor"]
    Object______p209["Object/外部框架"] -->|extends| BaseCustomerMsgConstructor_c209["BaseCustomerMsgConstructor"]
    CustomerMsgConstructor_p210["CustomerMsgConstructor"] -->|implements| BaseCustomerMsgConstructor_c210["BaseCustomerMsgConstructor"]
    MsgConstructor_p211["MsgConstructor"] -->|extends| CustomerMsgConstructor_c211["CustomerMsgConstructor"]
    BaseMsgConstructorFactory_p212["BaseMsgConstructorFactory"] -->|extends| CustomerMsgConstructorFactory_c212["CustomerMsgConstructorFactory"]
    BaseCustomerMsgConstructor_p213["BaseCustomerMsgConstructor"] -->|extends| CustomerMsgConstructorV1_c213["CustomerMsgConstructorV1"]
    BaseCustomerMsgConstructor_p214["BaseCustomerMsgConstructor"] -->|extends| CustomerMsgConstructorV2_c214["CustomerMsgConstructorV2"]
    Object______p215["Object/外部框架"] -->|extends| BaseDashboardMsgConstructor_c215["BaseDashboardMsgConstructor"]
    DashboardMsgConstructor_p216["DashboardMsgConstructor"] -->|implements| BaseDashboardMsgConstructor_c216["BaseDashboardMsgConstructor"]
    MsgConstructor_p217["MsgConstructor"] -->|extends| DashboardMsgConstructor_c217["DashboardMsgConstructor"]
    BaseMsgConstructorFactory_p218["BaseMsgConstructorFactory"] -->|extends| DashboardMsgConstructorFactory_c218["DashboardMsgConstructorFactory"]
    BaseDashboardMsgConstructor_p219["BaseDashboardMsgConstructor"] -->|extends| DashboardMsgConstructorV1_c219["DashboardMsgConstructorV1"]
    BaseDashboardMsgConstructor_p220["BaseDashboardMsgConstructor"] -->|extends| DashboardMsgConstructorV2_c220["DashboardMsgConstructorV2"]
    Object______p221["Object/外部框架"] -->|extends| BaseDeviceMsgConstructor_c221["BaseDeviceMsgConstructor"]
    DeviceMsgConstructor_p222["DeviceMsgConstructor"] -->|implements| BaseDeviceMsgConstructor_c222["BaseDeviceMsgConstructor"]
    MsgConstructor_p223["MsgConstructor"] -->|extends| DeviceMsgConstructor_c223["DeviceMsgConstructor"]
    BaseMsgConstructorFactory_p224["BaseMsgConstructorFactory"] -->|extends| DeviceMsgConstructorFactory_c224["DeviceMsgConstructorFactory"]
    BaseDeviceMsgConstructor_p225["BaseDeviceMsgConstructor"] -->|extends| DeviceMsgConstructorV1_c225["DeviceMsgConstructorV1"]
    BaseDeviceMsgConstructor_p226["BaseDeviceMsgConstructor"] -->|extends| DeviceMsgConstructorV2_c226["DeviceMsgConstructorV2"]
    Object______p227["Object/外部框架"] -->|extends| EdgeMsgConstructor_c227["EdgeMsgConstructor"]
    Object______p228["Object/外部框架"] -->|extends| BaseEntityViewMsgConstructor_c228["BaseEntityViewMsgConstructor"]
    EntityViewMsgConstructor_p229["EntityViewMsgConstructor"] -->|implements| BaseEntityViewMsgConstructor_c229["BaseEntityViewMsgConstructor"]
    MsgConstructor_p230["MsgConstructor"] -->|extends| EntityViewMsgConstructor_c230["EntityViewMsgConstructor"]
    BaseMsgConstructorFactory_p231["BaseMsgConstructorFactory"] -->|extends| EntityViewMsgConstructorFactory_c231["EntityViewMsgConstructorFactory"]
    BaseEntityViewMsgConstructor_p232["BaseEntityViewMsgConstructor"] -->|extends| EntityViewMsgConstructorV1_c232["EntityViewMsgConstructorV1"]
    BaseEntityViewMsgConstructor_p233["BaseEntityViewMsgConstructor"] -->|extends| EntityViewMsgConstructorV2_c233["EntityViewMsgConstructorV2"]
    Object______p234["Object/外部框架"] -->|extends| BaseOtaPackageMsgConstructor_c234["BaseOtaPackageMsgConstructor"]
    OtaPackageMsgConstructor_p235["OtaPackageMsgConstructor"] -->|implements| BaseOtaPackageMsgConstructor_c235["BaseOtaPackageMsgConstructor"]
    MsgConstructor_p236["MsgConstructor"] -->|extends| OtaPackageMsgConstructor_c236["OtaPackageMsgConstructor"]
    BaseMsgConstructorFactory_p237["BaseMsgConstructorFactory"] -->|extends| OtaPackageMsgConstructorFactory_c237["OtaPackageMsgConstructorFactory"]
    BaseOtaPackageMsgConstructor_p238["BaseOtaPackageMsgConstructor"] -->|extends| OtaPackageMsgConstructorV1_c238["OtaPackageMsgConstructorV1"]
    BaseOtaPackageMsgConstructor_p239["BaseOtaPackageMsgConstructor"] -->|extends| OtaPackageMsgConstructorV2_c239["OtaPackageMsgConstructorV2"]
    Object______p240["Object/外部框架"] -->|extends| BaseQueueMsgConstructor_c240["BaseQueueMsgConstructor"]
    QueueMsgConstructor_p241["QueueMsgConstructor"] -->|implements| BaseQueueMsgConstructor_c241["BaseQueueMsgConstructor"]
    MsgConstructor_p242["MsgConstructor"] -->|extends| QueueMsgConstructor_c242["QueueMsgConstructor"]
    BaseMsgConstructorFactory_p243["BaseMsgConstructorFactory"] -->|extends| QueueMsgConstructorFactory_c243["QueueMsgConstructorFactory"]
    BaseQueueMsgConstructor_p244["BaseQueueMsgConstructor"] -->|extends| QueueMsgConstructorV1_c244["QueueMsgConstructorV1"]
    BaseQueueMsgConstructor_p245["BaseQueueMsgConstructor"] -->|extends| QueueMsgConstructorV2_c245["QueueMsgConstructorV2"]
    MsgConstructor_p246["MsgConstructor"] -->|extends| RelationMsgConstructor_c246["RelationMsgConstructor"]
    BaseMsgConstructorFactory_p247["BaseMsgConstructorFactory"] -->|extends| RelationMsgConstructorFactory_c247["RelationMsgConstructorFactory"]
    Object______p248["Object/外部框架"] -->|extends| RelationMsgConstructorV1_c248["RelationMsgConstructorV1"]
    RelationMsgConstructor_p249["RelationMsgConstructor"] -->|implements| RelationMsgConstructorV1_c249["RelationMsgConstructorV1"]
    Object______p250["Object/外部框架"] -->|extends| RelationMsgConstructorV2_c250["RelationMsgConstructorV2"]
    RelationMsgConstructor_p251["RelationMsgConstructor"] -->|implements| RelationMsgConstructorV2_c251["RelationMsgConstructorV2"]
    Object______p252["Object/外部框架"] -->|extends| BaseResourceMsgConstructor_c252["BaseResourceMsgConstructor"]
    ResourceMsgConstructor_p253["ResourceMsgConstructor"] -->|implements| BaseResourceMsgConstructor_c253["BaseResourceMsgConstructor"]
    MsgConstructor_p254["MsgConstructor"] -->|extends| ResourceMsgConstructor_c254["ResourceMsgConstructor"]
    BaseMsgConstructorFactory_p255["BaseMsgConstructorFactory"] -->|extends| ResourceMsgConstructorFactory_c255["ResourceMsgConstructorFactory"]
    BaseResourceMsgConstructor_p256["BaseResourceMsgConstructor"] -->|extends| ResourceMsgConstructorV1_c256["ResourceMsgConstructorV1"]
    BaseResourceMsgConstructor_p257["BaseResourceMsgConstructor"] -->|extends| ResourceMsgConstructorV2_c257["ResourceMsgConstructorV2"]
    Object______p258["Object/外部框架"] -->|extends| BaseRuleChainMetadataConstructor_c258["BaseRuleChainMetadataConstructor"]
    RuleChainMetadataConstructor_p259["RuleChainMetadataConstructor"] -->|implements| BaseRuleChainMetadataConstructor_c259["BaseRuleChainMetadataConstructor"]
    Object______p260["Object/外部框架"] -->|extends| BaseRuleChainMsgConstructor_c260["BaseRuleChainMsgConstructor"]
    RuleChainMsgConstructor_p261["RuleChainMsgConstructor"] -->|implements| BaseRuleChainMsgConstructor_c261["BaseRuleChainMsgConstructor"]
    Object______p262["Object/外部框架"] -->|extends| RuleChainMetadataConstructorFactory_c262["RuleChainMetadataConstructorFactory"]
    BaseRuleChainMetadataConstructor_p263["BaseRuleChainMetadataConstructor"] -->|extends| RuleChainMetadataConstructorV330_c263["RuleChainMetadataConstructorV330"]
    BaseRuleChainMetadataConstructor_p264["BaseRuleChainMetadataConstructor"] -->|extends| RuleChainMetadataConstructorV340_c264["RuleChainMetadataConstructorV340"]
    BaseRuleChainMetadataConstructor_p265["BaseRuleChainMetadataConstructor"] -->|extends| RuleChainMetadataConstructorV362_c265["RuleChainMetadataConstructorV362"]
    MsgConstructor_p266["MsgConstructor"] -->|extends| RuleChainMsgConstructor_c266["RuleChainMsgConstructor"]
    BaseMsgConstructorFactory_p267["BaseMsgConstructorFactory"] -->|extends| RuleChainMsgConstructorFactory_c267["RuleChainMsgConstructorFactory"]
    BaseRuleChainMsgConstructor_p268["BaseRuleChainMsgConstructor"] -->|extends| RuleChainMsgConstructorV1_c268["RuleChainMsgConstructorV1"]
    BaseRuleChainMsgConstructor_p269["BaseRuleChainMsgConstructor"] -->|extends| RuleChainMsgConstructorV2_c269["RuleChainMsgConstructorV2"]
    MsgConstructor_p270["MsgConstructor"] -->|extends| AdminSettingsMsgConstructor_c270["AdminSettingsMsgConstructor"]
    BaseMsgConstructorFactory_p271["BaseMsgConstructorFactory"] -->|extends| AdminSettingsMsgConstructorFactory_c271["AdminSettingsMsgConstructorFactory"]
    Object______p272["Object/外部框架"] -->|extends| AdminSettingsMsgConstructorV1_c272["AdminSettingsMsgConstructorV1"]
    AdminSettingsMsgConstructor_p273["AdminSettingsMsgConstructor"] -->|implements| AdminSettingsMsgConstructorV1_c273["AdminSettingsMsgConstructorV1"]
    Object______p274["Object/外部框架"] -->|extends| AdminSettingsMsgConstructorV2_c274["AdminSettingsMsgConstructorV2"]
    AdminSettingsMsgConstructor_p275["AdminSettingsMsgConstructor"] -->|implements| AdminSettingsMsgConstructorV2_c275["AdminSettingsMsgConstructorV2"]
    Object______p276["Object/外部框架"] -->|extends| EntityDataMsgConstructor_c276["EntityDataMsgConstructor"]
    MsgConstructor_p277["MsgConstructor"] -->|extends| TenantMsgConstructor_c277["TenantMsgConstructor"]
    BaseMsgConstructorFactory_p278["BaseMsgConstructorFactory"] -->|extends| TenantMsgConstructorFactory_c278["TenantMsgConstructorFactory"]
    Object______p279["Object/外部框架"] -->|extends| TenantMsgConstructorV1_c279["TenantMsgConstructorV1"]
    TenantMsgConstructor_p280["TenantMsgConstructor"] -->|implements| TenantMsgConstructorV1_c280["TenantMsgConstructorV1"]
    Object______p281["Object/外部框架"] -->|extends| TenantMsgConstructorV2_c281["TenantMsgConstructorV2"]
    TenantMsgConstructor_p282["TenantMsgConstructor"] -->|implements| TenantMsgConstructorV2_c282["TenantMsgConstructorV2"]
    Object______p283["Object/外部框架"] -->|extends| BaseUserMsgConstructor_c283["BaseUserMsgConstructor"]
    UserMsgConstructor_p284["UserMsgConstructor"] -->|implements| BaseUserMsgConstructor_c284["BaseUserMsgConstructor"]
    MsgConstructor_p285["MsgConstructor"] -->|extends| UserMsgConstructor_c285["UserMsgConstructor"]
    BaseMsgConstructorFactory_p286["BaseMsgConstructorFactory"] -->|extends| UserMsgConstructorFactory_c286["UserMsgConstructorFactory"]
    BaseUserMsgConstructor_p287["BaseUserMsgConstructor"] -->|extends| UserMsgConstructorV1_c287["UserMsgConstructorV1"]
    BaseUserMsgConstructor_p288["BaseUserMsgConstructor"] -->|extends| UserMsgConstructorV2_c288["UserMsgConstructorV2"]
    Object______p289["Object/外部框架"] -->|extends| BaseWidgetMsgConstructor_c289["BaseWidgetMsgConstructor"]
    WidgetMsgConstructor_p290["WidgetMsgConstructor"] -->|implements| BaseWidgetMsgConstructor_c290["BaseWidgetMsgConstructor"]
    MsgConstructor_p291["MsgConstructor"] -->|extends| WidgetMsgConstructor_c291["WidgetMsgConstructor"]
    BaseMsgConstructorFactory_p292["BaseMsgConstructorFactory"] -->|extends| WidgetMsgConstructorFactory_c292["WidgetMsgConstructorFactory"]
    BaseWidgetMsgConstructor_p293["BaseWidgetMsgConstructor"] -->|extends| WidgetMsgConstructorV1_c293["WidgetMsgConstructorV1"]
    BaseWidgetMsgConstructor_p294["BaseWidgetMsgConstructor"] -->|extends| WidgetMsgConstructorV2_c294["WidgetMsgConstructorV2"]
    Object______p295["Object/外部框架"] -->|extends| AdminSettingsEdgeEventFetcher_c295["AdminSettingsEdgeEventFetcher"]
    EdgeEventFetcher_p296["EdgeEventFetcher"] -->|implements| AdminSettingsEdgeEventFetcher_c296["AdminSettingsEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p297["BasePageableEdgeEventFetcher"] -->|extends| AssetProfilesEdgeEventFetcher_c297["AssetProfilesEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p298["BasePageableEdgeEventFetcher"] -->|extends| AssetsEdgeEventFetcher_c298["AssetsEdgeEventFetcher"]
    Object______p299["Object/外部框架"] -->|extends| BasePageableEdgeEventFetcher_c299["BasePageableEdgeEventFetcher"]
    EdgeEventFetcher_p300["EdgeEventFetcher"] -->|implements| BasePageableEdgeEventFetcher_c300["BasePageableEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p301["BasePageableEdgeEventFetcher"] -->|extends| BaseUsersEdgeEventFetcher_c301["BaseUsersEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p302["BasePageableEdgeEventFetcher"] -->|extends| BaseWidgetTypesEdgeEventFetcher_c302["BaseWidgetTypesEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p303["BasePageableEdgeEventFetcher"] -->|extends| BaseWidgetsBundlesEdgeEventFetcher_c303["BaseWidgetsBundlesEdgeEventFetcher"]
    Object______p304["Object/外部框架"] -->|extends| CustomerEdgeEventFetcher_c304["CustomerEdgeEventFetcher"]
    EdgeEventFetcher_p305["EdgeEventFetcher"] -->|implements| CustomerEdgeEventFetcher_c305["CustomerEdgeEventFetcher"]
    BaseUsersEdgeEventFetcher_p306["BaseUsersEdgeEventFetcher"] -->|extends| CustomerUsersEdgeEventFetcher_c306["CustomerUsersEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p307["BasePageableEdgeEventFetcher"] -->|extends| DashboardsEdgeEventFetcher_c307["DashboardsEdgeEventFetcher"]
    Object______p308["Object/外部框架"] -->|extends| DefaultProfilesEdgeEventFetcher_c308["DefaultProfilesEdgeEventFetcher"]
    EdgeEventFetcher_p309["EdgeEventFetcher"] -->|implements| DefaultProfilesEdgeEventFetcher_c309["DefaultProfilesEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p310["BasePageableEdgeEventFetcher"] -->|extends| DeviceProfilesEdgeEventFetcher_c310["DeviceProfilesEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p311["BasePageableEdgeEventFetcher"] -->|extends| DevicesEdgeEventFetcher_c311["DevicesEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p312["BasePageableEdgeEventFetcher"] -->|extends| EntityViewsEdgeEventFetcher_c312["EntityViewsEdgeEventFetcher"]
    Object______p313["Object/外部框架"] -->|extends| GeneralEdgeEventFetcher_c313["GeneralEdgeEventFetcher"]
    EdgeEventFetcher_p314["EdgeEventFetcher"] -->|implements| GeneralEdgeEventFetcher_c314["GeneralEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p315["BasePageableEdgeEventFetcher"] -->|extends| OtaPackagesEdgeEventFetcher_c315["OtaPackagesEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p316["BasePageableEdgeEventFetcher"] -->|extends| QueuesEdgeEventFetcher_c316["QueuesEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p317["BasePageableEdgeEventFetcher"] -->|extends| RuleChainsEdgeEventFetcher_c317["RuleChainsEdgeEventFetcher"]
    BaseWidgetTypesEdgeEventFetcher_p318["BaseWidgetTypesEdgeEventFetcher"] -->|extends| SystemWidgetTypesEdgeEventFetcher_c318["SystemWidgetTypesEdgeEventFetcher"]
    BaseWidgetsBundlesEdgeEventFetcher_p319["BaseWidgetsBundlesEdgeEventFetcher"] -->|extends| SystemWidgetsBundlesEdgeEventFetcher_c319["SystemWidgetsBundlesEdgeEventFetcher"]
    BaseUsersEdgeEventFetcher_p320["BaseUsersEdgeEventFetcher"] -->|extends| TenantAdminUsersEdgeEventFetcher_c320["TenantAdminUsersEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p321["BasePageableEdgeEventFetcher"] -->|extends| TenantEdgeEventFetcher_c321["TenantEdgeEventFetcher"]
    BasePageableEdgeEventFetcher_p322["BasePageableEdgeEventFetcher"] -->|extends| TenantResourcesEdgeEventFetcher_c322["TenantResourcesEdgeEventFetcher"]
    BaseWidgetTypesEdgeEventFetcher_p323["BaseWidgetTypesEdgeEventFetcher"] -->|extends| TenantWidgetTypesEdgeEventFetcher_c323["TenantWidgetTypesEdgeEventFetcher"]
    BaseWidgetsBundlesEdgeEventFetcher_p324["BaseWidgetsBundlesEdgeEventFetcher"] -->|extends| TenantWidgetsBundlesEdgeEventFetcher_c324["TenantWidgetsBundlesEdgeEventFetcher"]
    Object______p325["Object/外部框架"] -->|extends| BaseEdgeProcessor_c325["BaseEdgeProcessor"]
    Object______p326["Object/外部框架"] -->|extends| BaseEdgeProcessorFactory_c326["BaseEdgeProcessorFactory"]
    BaseAlarmProcessor_p327["BaseAlarmProcessor"] -->|extends| AlarmEdgeProcessor_c327["AlarmEdgeProcessor"]
    AlarmProcessor_p328["AlarmProcessor"] -->|implements| AlarmEdgeProcessor_c328["AlarmEdgeProcessor"]
    BaseEdgeProcessorFactory_p329["BaseEdgeProcessorFactory"] -->|extends| AlarmEdgeProcessorFactory_c329["AlarmEdgeProcessorFactory"]
    AlarmEdgeProcessor_p330["AlarmEdgeProcessor"] -->|extends| AlarmEdgeProcessorV1_c330["AlarmEdgeProcessorV1"]
    AlarmEdgeProcessor_p331["AlarmEdgeProcessor"] -->|extends| AlarmEdgeProcessorV2_c331["AlarmEdgeProcessorV2"]
    EdgeProcessor_p332["EdgeProcessor"] -->|extends| AlarmProcessor_c332["AlarmProcessor"]
    BaseEdgeProcessor_p333["BaseEdgeProcessor"] -->|extends| BaseAlarmProcessor_c333["BaseAlarmProcessor"]
    BaseAssetProcessor_p334["BaseAssetProcessor"] -->|extends| AssetEdgeProcessor_c334["AssetEdgeProcessor"]
    AssetProcessor_p335["AssetProcessor"] -->|implements| AssetEdgeProcessor_c335["AssetEdgeProcessor"]
    BaseEdgeProcessorFactory_p336["BaseEdgeProcessorFactory"] -->|extends| AssetEdgeProcessorFactory_c336["AssetEdgeProcessorFactory"]
    AssetEdgeProcessor_p337["AssetEdgeProcessor"] -->|extends| AssetEdgeProcessorV1_c337["AssetEdgeProcessorV1"]
    AssetEdgeProcessor_p338["AssetEdgeProcessor"] -->|extends| AssetEdgeProcessorV2_c338["AssetEdgeProcessorV2"]
    EdgeProcessor_p339["EdgeProcessor"] -->|extends| AssetProcessor_c339["AssetProcessor"]
    BaseEdgeProcessor_p340["BaseEdgeProcessor"] -->|extends| BaseAssetProcessor_c340["BaseAssetProcessor"]
    BaseAssetProfileProcessor_p341["BaseAssetProfileProcessor"] -->|extends| AssetProfileEdgeProcessor_c341["AssetProfileEdgeProcessor"]
    AssetProfileProcessor_p342["AssetProfileProcessor"] -->|implements| AssetProfileEdgeProcessor_c342["AssetProfileEdgeProcessor"]
    BaseEdgeProcessorFactory_p343["BaseEdgeProcessorFactory"] -->|extends| AssetProfileEdgeProcessorFactory_c343["AssetProfileEdgeProcessorFactory"]
    AssetProfileEdgeProcessor_p344["AssetProfileEdgeProcessor"] -->|extends| AssetProfileEdgeProcessorV1_c344["AssetProfileEdgeProcessorV1"]
    AssetProfileEdgeProcessor_p345["AssetProfileEdgeProcessor"] -->|extends| AssetProfileEdgeProcessorV2_c345["AssetProfileEdgeProcessorV2"]
    EdgeProcessor_p346["EdgeProcessor"] -->|extends| AssetProfileProcessor_c346["AssetProfileProcessor"]
    BaseEdgeProcessor_p347["BaseEdgeProcessor"] -->|extends| BaseAssetProfileProcessor_c347["BaseAssetProfileProcessor"]
    BaseEdgeProcessor_p348["BaseEdgeProcessor"] -->|extends| CustomerEdgeProcessor_c348["CustomerEdgeProcessor"]
    BaseEdgeProcessor_p349["BaseEdgeProcessor"] -->|extends| BaseDashboardProcessor_c349["BaseDashboardProcessor"]
    BaseDashboardProcessor_p350["BaseDashboardProcessor"] -->|extends| DashboardEdgeProcessor_c350["DashboardEdgeProcessor"]
    DashboardProcessor_p351["DashboardProcessor"] -->|implements| DashboardEdgeProcessor_c351["DashboardEdgeProcessor"]
    BaseEdgeProcessorFactory_p352["BaseEdgeProcessorFactory"] -->|extends| DashboardEdgeProcessorFactory_c352["DashboardEdgeProcessorFactory"]
    DashboardEdgeProcessor_p353["DashboardEdgeProcessor"] -->|extends| DashboardEdgeProcessorV1_c353["DashboardEdgeProcessorV1"]
    DashboardEdgeProcessor_p354["DashboardEdgeProcessor"] -->|extends| DashboardEdgeProcessorV2_c354["DashboardEdgeProcessorV2"]
    EdgeProcessor_p355["EdgeProcessor"] -->|extends| DashboardProcessor_c355["DashboardProcessor"]
    BaseEdgeProcessor_p356["BaseEdgeProcessor"] -->|extends| BaseDeviceProcessor_c356["BaseDeviceProcessor"]
    BaseDeviceProcessor_p357["BaseDeviceProcessor"] -->|extends| DeviceEdgeProcessor_c357["DeviceEdgeProcessor"]
    DeviceProcessor_p358["DeviceProcessor"] -->|implements| DeviceEdgeProcessor_c358["DeviceEdgeProcessor"]
    BaseEdgeProcessorFactory_p359["BaseEdgeProcessorFactory"] -->|extends| DeviceEdgeProcessorFactory_c359["DeviceEdgeProcessorFactory"]
    DeviceEdgeProcessor_p360["DeviceEdgeProcessor"] -->|extends| DeviceEdgeProcessorV1_c360["DeviceEdgeProcessorV1"]
    DeviceEdgeProcessor_p361["DeviceEdgeProcessor"] -->|extends| DeviceEdgeProcessorV2_c361["DeviceEdgeProcessorV2"]
    EdgeProcessor_p362["EdgeProcessor"] -->|extends| DeviceProcessor_c362["DeviceProcessor"]
    BaseEdgeProcessor_p363["BaseEdgeProcessor"] -->|extends| BaseDeviceProfileProcessor_c363["BaseDeviceProfileProcessor"]
    BaseDeviceProfileProcessor_p364["BaseDeviceProfileProcessor"] -->|extends| DeviceProfileEdgeProcessor_c364["DeviceProfileEdgeProcessor"]
    DeviceProfileProcessor_p365["DeviceProfileProcessor"] -->|implements| DeviceProfileEdgeProcessor_c365["DeviceProfileEdgeProcessor"]
    BaseEdgeProcessorFactory_p366["BaseEdgeProcessorFactory"] -->|extends| DeviceProfileEdgeProcessorFactory_c366["DeviceProfileEdgeProcessorFactory"]
    DeviceProfileEdgeProcessor_p367["DeviceProfileEdgeProcessor"] -->|extends| DeviceProfileEdgeProcessorV1_c367["DeviceProfileEdgeProcessorV1"]
    DeviceProfileEdgeProcessor_p368["DeviceProfileEdgeProcessor"] -->|extends| DeviceProfileEdgeProcessorV2_c368["DeviceProfileEdgeProcessorV2"]
    EdgeProcessor_p369["EdgeProcessor"] -->|extends| DeviceProfileProcessor_c369["DeviceProfileProcessor"]
    BaseEdgeProcessor_p370["BaseEdgeProcessor"] -->|extends| EdgeProcessor_c370["EdgeProcessor"]
    BaseEdgeProcessor_p371["BaseEdgeProcessor"] -->|extends| BaseEntityViewProcessor_c371["BaseEntityViewProcessor"]
    BaseEntityViewProcessor_p372["BaseEntityViewProcessor"] -->|extends| EntityViewEdgeProcessor_c372["EntityViewEdgeProcessor"]
    EntityViewProcessor_p373["EntityViewProcessor"] -->|implements| EntityViewEdgeProcessor_c373["EntityViewEdgeProcessor"]
    EdgeProcessor_p374["EdgeProcessor"] -->|extends| EntityViewProcessor_c374["EntityViewProcessor"]
    BaseEdgeProcessorFactory_p375["BaseEdgeProcessorFactory"] -->|extends| EntityViewProcessorFactory_c375["EntityViewProcessorFactory"]
    EntityViewEdgeProcessor_p376["EntityViewEdgeProcessor"] -->|extends| EntityViewProcessorV1_c376["EntityViewProcessorV1"]
    EntityViewEdgeProcessor_p377["EntityViewEdgeProcessor"] -->|extends| EntityViewProcessorV2_c377["EntityViewProcessorV2"]
    BaseEdgeProcessor_p378["BaseEdgeProcessor"] -->|extends| OtaPackageEdgeProcessor_c378["OtaPackageEdgeProcessor"]
    BaseEdgeProcessor_p379["BaseEdgeProcessor"] -->|extends| QueueEdgeProcessor_c379["QueueEdgeProcessor"]
    BaseEdgeProcessor_p380["BaseEdgeProcessor"] -->|extends| BaseRelationProcessor_c380["BaseRelationProcessor"]
    BaseRelationProcessor_p381["BaseRelationProcessor"] -->|extends| RelationEdgeProcessor_c381["RelationEdgeProcessor"]
    RelationProcessor_p382["RelationProcessor"] -->|implements| RelationEdgeProcessor_c382["RelationEdgeProcessor"]
    BaseEdgeProcessorFactory_p383["BaseEdgeProcessorFactory"] -->|extends| RelationEdgeProcessorFactory_c383["RelationEdgeProcessorFactory"]
    RelationEdgeProcessor_p384["RelationEdgeProcessor"] -->|extends| RelationEdgeProcessorV1_c384["RelationEdgeProcessorV1"]
    RelationEdgeProcessor_p385["RelationEdgeProcessor"] -->|extends| RelationEdgeProcessorV2_c385["RelationEdgeProcessorV2"]
    EdgeProcessor_p386["EdgeProcessor"] -->|extends| RelationProcessor_c386["RelationProcessor"]
    BaseEdgeProcessor_p387["BaseEdgeProcessor"] -->|extends| BaseResourceProcessor_c387["BaseResourceProcessor"]
    BaseResourceProcessor_p388["BaseResourceProcessor"] -->|extends| ResourceEdgeProcessor_c388["ResourceEdgeProcessor"]
    ResourceProcessor_p389["ResourceProcessor"] -->|implements| ResourceEdgeProcessor_c389["ResourceEdgeProcessor"]
    BaseEdgeProcessorFactory_p390["BaseEdgeProcessorFactory"] -->|extends| ResourceEdgeProcessorFactory_c390["ResourceEdgeProcessorFactory"]
    ResourceEdgeProcessor_p391["ResourceEdgeProcessor"] -->|extends| ResourceEdgeProcessorV1_c391["ResourceEdgeProcessorV1"]
    ResourceEdgeProcessor_p392["ResourceEdgeProcessor"] -->|extends| ResourceEdgeProcessorV2_c392["ResourceEdgeProcessorV2"]
    EdgeProcessor_p393["EdgeProcessor"] -->|extends| ResourceProcessor_c393["ResourceProcessor"]
    BaseEdgeProcessor_p394["BaseEdgeProcessor"] -->|extends| RuleChainEdgeProcessor_c394["RuleChainEdgeProcessor"]
    BaseEdgeProcessor_p395["BaseEdgeProcessor"] -->|extends| AdminSettingsEdgeProcessor_c395["AdminSettingsEdgeProcessor"]
    BaseEdgeProcessor_p396["BaseEdgeProcessor"] -->|extends| BaseTelemetryProcessor_c396["BaseTelemetryProcessor"]
    BaseTelemetryProcessor_p397["BaseTelemetryProcessor"] -->|extends| TelemetryEdgeProcessor_c397["TelemetryEdgeProcessor"]
    BaseEdgeProcessor_p398["BaseEdgeProcessor"] -->|extends| TenantEdgeProcessor_c398["TenantEdgeProcessor"]
    BaseEdgeProcessor_p399["BaseEdgeProcessor"] -->|extends| TenantProfileEdgeProcessor_c399["TenantProfileEdgeProcessor"]
    BaseEdgeProcessor_p400["BaseEdgeProcessor"] -->|extends| UserEdgeProcessor_c400["UserEdgeProcessor"]
    BaseEdgeProcessor_p401["BaseEdgeProcessor"] -->|extends| WidgetBundleEdgeProcessor_c401["WidgetBundleEdgeProcessor"]
    BaseEdgeProcessor_p402["BaseEdgeProcessor"] -->|extends| WidgetTypeEdgeProcessor_c402["WidgetTypeEdgeProcessor"]
    Object______p403["Object/外部框架"] -->|extends| DefaultEdgeRequestsService_c403["DefaultEdgeRequestsService"]
    EdgeRequestsService_p404["EdgeRequestsService"] -->|implements| DefaultEdgeRequestsService_c404["DefaultEdgeRequestsService"]
    Object______p405["Object/外部框架"] -->|extends| EdgeVersionUtils_c405["EdgeVersionUtils"]
    Object______p406["Object/外部框架"] -->|extends| AbstractTbEntityService_c406["AbstractTbEntityService"]
    Object______p407["Object/外部框架"] -->|extends| DefaultTbNotificationEntityService_c407["DefaultTbNotificationEntityService"]
    TbNotificationEntityService_p408["TbNotificationEntityService"] -->|implements| DefaultTbNotificationEntityService_c408["DefaultTbNotificationEntityService"]
    AbstractTbEntityService_p409["AbstractTbEntityService"] -->|extends| DefaultTbAlarmCommentService_c409["DefaultTbAlarmCommentService"]
    TbAlarmCommentService_p410["TbAlarmCommentService"] -->|implements| DefaultTbAlarmCommentService_c410["DefaultTbAlarmCommentService"]
    AbstractTbEntityService_p411["AbstractTbEntityService"] -->|extends| DefaultTbAlarmService_c411["DefaultTbAlarmService"]
    TbAlarmService_p412["TbAlarmService"] -->|implements| DefaultTbAlarmService_c412["DefaultTbAlarmService"]
    AbstractTbEntityService_p413["AbstractTbEntityService"] -->|extends| DefaultTbAssetService_c413["DefaultTbAssetService"]
    TbAssetService_p414["TbAssetService"] -->|implements| DefaultTbAssetService_c414["DefaultTbAssetService"]
    AbstractTbEntityService_p415["AbstractTbEntityService"] -->|extends| DefaultTbAssetProfileService_c415["DefaultTbAssetProfileService"]
    TbAssetProfileService_p416["TbAssetProfileService"] -->|implements| DefaultTbAssetProfileService_c416["DefaultTbAssetProfileService"]
    SimpleTbEntityService_p417["SimpleTbEntityService"] -->|extends| TbAssetProfileService_c417["TbAssetProfileService"]
    AbstractTbEntityService_p418["AbstractTbEntityService"] -->|extends| DefaultTbCustomerService_c418["DefaultTbCustomerService"]
    TbCustomerService_p419["TbCustomerService"] -->|implements| DefaultTbCustomerService_c419["DefaultTbCustomerService"]
    SimpleTbEntityService_p420["SimpleTbEntityService"] -->|extends| TbCustomerService_c420["TbCustomerService"]
    AbstractTbEntityService_p421["AbstractTbEntityService"] -->|extends| DefaultTbDashboardService_c421["DefaultTbDashboardService"]
    TbDashboardService_p422["TbDashboardService"] -->|implements| DefaultTbDashboardService_c422["DefaultTbDashboardService"]
    SimpleTbEntityService_p423["SimpleTbEntityService"] -->|extends| TbDashboardService_c423["TbDashboardService"]
    AbstractTbEntityService_p424["AbstractTbEntityService"] -->|extends| DefaultTbDeviceService_c424["DefaultTbDeviceService"]
    TbDeviceService_p425["TbDeviceService"] -->|implements| DefaultTbDeviceService_c425["DefaultTbDeviceService"]
    AbstractTbEntityService_p426["AbstractTbEntityService"] -->|extends| DefaultTbDeviceProfileService_c426["DefaultTbDeviceProfileService"]
    TbDeviceProfileService_p427["TbDeviceProfileService"] -->|implements| DefaultTbDeviceProfileService_c427["DefaultTbDeviceProfileService"]
    SimpleTbEntityService_p428["SimpleTbEntityService"] -->|extends| TbDeviceProfileService_c428["TbDeviceProfileService"]
    AbstractTbEntityService_p429["AbstractTbEntityService"] -->|extends| DefaultTbEdgeService_c429["DefaultTbEdgeService"]
    TbEdgeService_p430["TbEdgeService"] -->|implements| DefaultTbEdgeService_c430["DefaultTbEdgeService"]
    AbstractTbEntityService_p431["AbstractTbEntityService"] -->|extends| DefaultTbEntityRelationService_c431["DefaultTbEntityRelationService"]
    TbEntityRelationService_p432["TbEntityRelationService"] -->|implements| DefaultTbEntityRelationService_c432["DefaultTbEntityRelationService"]
    AbstractTbEntityService_p433["AbstractTbEntityService"] -->|extends| DefaultTbEntityViewService_c433["DefaultTbEntityViewService"]
    TbEntityViewService_p434["TbEntityViewService"] -->|implements| DefaultTbEntityViewService_c434["DefaultTbEntityViewService"]
    ComponentLifecycleListener_p435["ComponentLifecycleListener"] -->|extends| TbEntityViewService_c435["TbEntityViewService"]
    AbstractTbEntityService_p436["AbstractTbEntityService"] -->|extends| DefaultTbOtaPackageService_c436["DefaultTbOtaPackageService"]
    TbOtaPackageService_p437["TbOtaPackageService"] -->|implements| DefaultTbOtaPackageService_c437["DefaultTbOtaPackageService"]
    AbstractTbEntityService_p438["AbstractTbEntityService"] -->|extends| DefaultTbQueueService_c438["DefaultTbQueueService"]
    TbQueueService_p439["TbQueueService"] -->|implements| DefaultTbQueueService_c439["DefaultTbQueueService"]
    AbstractTbEntityService_p440["AbstractTbEntityService"] -->|extends| DefaultTbTenantService_c440["DefaultTbTenantService"]
    TbTenantService_p441["TbTenantService"] -->|implements| DefaultTbTenantService_c441["DefaultTbTenantService"]
    AbstractTbEntityService_p442["AbstractTbEntityService"] -->|extends| DefaultTbTenantProfileService_c442["DefaultTbTenantProfileService"]
    TbTenantProfileService_p443["TbTenantProfileService"] -->|implements| DefaultTbTenantProfileService_c443["DefaultTbTenantProfileService"]
    Object______p444["Object/外部框架"] -->|extends| DefaultTbUserSettingsService_c444["DefaultTbUserSettingsService"]
    TbUserSettingsService_p445["TbUserSettingsService"] -->|implements| DefaultTbUserSettingsService_c445["DefaultTbUserSettingsService"]
    AbstractTbEntityService_p446["AbstractTbEntityService"] -->|extends| DefaultUserService_c446["DefaultUserService"]
    TbUserService_p447["TbUserService"] -->|implements| DefaultUserService_c447["DefaultUserService"]
    AbstractTbEntityService_p448["AbstractTbEntityService"] -->|extends| DefaultWidgetsBundleService_c448["DefaultWidgetsBundleService"]
    TbWidgetsBundleService_p449["TbWidgetsBundleService"] -->|implements| DefaultWidgetsBundleService_c449["DefaultWidgetsBundleService"]
    SimpleTbEntityService_p450["SimpleTbEntityService"] -->|extends| TbWidgetsBundleService_c450["TbWidgetsBundleService"]
    AbstractTbEntityService_p451["AbstractTbEntityService"] -->|extends| DefaultWidgetTypeService_c451["DefaultWidgetTypeService"]
    TbWidgetTypeService_p452["TbWidgetTypeService"] -->|implements| DefaultWidgetTypeService_c452["DefaultWidgetTypeService"]
    SimpleTbEntityService_p453["SimpleTbEntityService"] -->|extends| TbWidgetTypeService_c453["TbWidgetTypeService"]
    AbstractListeningExecutor_p454["AbstractListeningExecutor"] -->|extends| DbCallbackExecutorService_c454["DbCallbackExecutorService"]
    AbstractListeningExecutor_p455["AbstractListeningExecutor"] -->|extends| ExternalCallExecutorService_c455["ExternalCallExecutorService"]
    AbstractListeningExecutor_p456["AbstractListeningExecutor"] -->|extends| GrpcCallbackExecutorService_c456["GrpcCallbackExecutorService"]
    AbstractListeningExecutor_p457["AbstractListeningExecutor"] -->|extends| NotificationExecutorService_c457["NotificationExecutorService"]
    Object______p458["Object/外部框架"] -->|extends| PubSubRuleNodeExecutorProvider_c458["PubSubRuleNodeExecutorProvider"]
    ExecutorProvider_p459["ExecutorProvider"] -->|implements| PubSubRuleNodeExecutorProvider_c459["PubSubRuleNodeExecutorProvider"]
    Object______p460["Object/外部框架"] -->|extends| SharedEventLoopGroupService_c460["SharedEventLoopGroupService"]
    AbstractListeningExecutor_p461["AbstractListeningExecutor"] -->|extends| VersionControlExecutor_c461["VersionControlExecutor"]
    Object______p462["Object/外部框架"] -->|extends| DefaultGatewayNotificationsService_c462["DefaultGatewayNotificationsService"]
    GatewayNotificationsService_p463["GatewayNotificationsService"] -->|implements| DefaultGatewayNotificationsService_c463["DefaultGatewayNotificationsService"]
    Object______p464["Object/外部框架"] -->|extends| InMemoryHouseKeeperServiceService_c464["InMemoryHouseKeeperServiceService"]
    HouseKeeperService_p465["HouseKeeperService"] -->|implements| InMemoryHouseKeeperServiceService_c465["InMemoryHouseKeeperServiceService"]
    Object______p466["Object/外部框架"] -->|extends| AbstractCassandraDatabaseUpgradeService_c466["AbstractCassandraDatabaseUpgradeService"]
    Object______p467["Object/外部框架"] -->|extends| AbstractSqlTsDatabaseUpgradeService_c467["AbstractSqlTsDatabaseUpgradeService"]
    Object______p468["Object/外部框架"] -->|extends| CassandraAbstractDatabaseSchemaService_c468["CassandraAbstractDatabaseSchemaService"]
    DatabaseSchemaService_p469["DatabaseSchemaService"] -->|implements| CassandraAbstractDatabaseSchemaService_c469["CassandraAbstractDatabaseSchemaService"]
    CassandraAbstractDatabaseSchemaService_p470["CassandraAbstractDatabaseSchemaService"] -->|extends| CassandraKeyspaceService_c470["CassandraKeyspaceService"]
    NoSqlKeyspaceService_p471["NoSqlKeyspaceService"] -->|implements| CassandraKeyspaceService_c471["CassandraKeyspaceService"]
    CassandraAbstractDatabaseSchemaService_p472["CassandraAbstractDatabaseSchemaService"] -->|extends| CassandraTsDatabaseSchemaService_c472["CassandraTsDatabaseSchemaService"]
    TsDatabaseSchemaService_p473["TsDatabaseSchemaService"] -->|implements| CassandraTsDatabaseSchemaService_c473["CassandraTsDatabaseSchemaService"]
    AbstractCassandraDatabaseUpgradeService_p474["AbstractCassandraDatabaseUpgradeService"] -->|extends| CassandraTsDatabaseUpgradeService_c474["CassandraTsDatabaseUpgradeService"]
    DatabaseTsUpgradeService_p475["DatabaseTsUpgradeService"] -->|implements| CassandraTsDatabaseUpgradeService_c475["CassandraTsDatabaseUpgradeService"]
    CassandraAbstractDatabaseSchemaService_p476["CassandraAbstractDatabaseSchemaService"] -->|extends| CassandraTsLatestDatabaseSchemaService_c476["CassandraTsLatestDatabaseSchemaService"]
    TsLatestDatabaseSchemaService_p477["TsLatestDatabaseSchemaService"] -->|implements| CassandraTsLatestDatabaseSchemaService_c477["CassandraTsLatestDatabaseSchemaService"]
    DbCallbackExecutorService_p478["DbCallbackExecutorService"] -->|extends| DbUpgradeExecutorService_c478["DbUpgradeExecutorService"]
    Object______p479["Object/外部框架"] -->|extends| DefaultSystemDataLoaderService_c479["DefaultSystemDataLoaderService"]
    SystemDataLoaderService_p480["SystemDataLoaderService"] -->|implements| DefaultSystemDataLoaderService_c480["DefaultSystemDataLoaderService"]
    Object______p481["Object/外部框架"] -->|extends| TelemetrySaveCallback_c481["TelemetrySaveCallback"]
    DatabaseSchemaService_p482["DatabaseSchemaService"] -->|extends| EntityDatabaseSchemaService_c482["EntityDatabaseSchemaService"]
    Object______p483["Object/外部框架"] -->|extends| InstallScripts_c483["InstallScripts"]
    DatabaseSchemaService_p484["DatabaseSchemaService"] -->|extends| NoSqlKeyspaceService_c484["NoSqlKeyspaceService"]
    Object______p485["Object/外部框架"] -->|extends| SqlAbstractDatabaseSchemaService_c485["SqlAbstractDatabaseSchemaService"]
    DatabaseSchemaService_p486["DatabaseSchemaService"] -->|implements| SqlAbstractDatabaseSchemaService_c486["SqlAbstractDatabaseSchemaService"]
    Object______p487["Object/外部框架"] -->|extends| SqlDatabaseUpgradeService_c487["SqlDatabaseUpgradeService"]
    DatabaseEntitiesUpgradeService_p488["DatabaseEntitiesUpgradeService"] -->|implements| SqlDatabaseUpgradeService_c488["SqlDatabaseUpgradeService"]
    SqlAbstractDatabaseSchemaService_p489["SqlAbstractDatabaseSchemaService"] -->|extends| SqlEntityDatabaseSchemaService_c489["SqlEntityDatabaseSchemaService"]
    EntityDatabaseSchemaService_p490["EntityDatabaseSchemaService"] -->|implements| SqlEntityDatabaseSchemaService_c490["SqlEntityDatabaseSchemaService"]
    SqlAbstractDatabaseSchemaService_p491["SqlAbstractDatabaseSchemaService"] -->|extends| SqlTsDatabaseSchemaService_c491["SqlTsDatabaseSchemaService"]
    TsDatabaseSchemaService_p492["TsDatabaseSchemaService"] -->|implements| SqlTsDatabaseSchemaService_c492["SqlTsDatabaseSchemaService"]
    AbstractSqlTsDatabaseUpgradeService_p493["AbstractSqlTsDatabaseUpgradeService"] -->|extends| SqlTsDatabaseUpgradeService_c493["SqlTsDatabaseUpgradeService"]
    DatabaseTsUpgradeService_p494["DatabaseTsUpgradeService"] -->|implements| SqlTsDatabaseUpgradeService_c494["SqlTsDatabaseUpgradeService"]
    SqlAbstractDatabaseSchemaService_p495["SqlAbstractDatabaseSchemaService"] -->|extends| TimescaleTsDatabaseSchemaService_c495["TimescaleTsDatabaseSchemaService"]
    TsDatabaseSchemaService_p496["TsDatabaseSchemaService"] -->|implements| TimescaleTsDatabaseSchemaService_c496["TimescaleTsDatabaseSchemaService"]
    AbstractSqlTsDatabaseUpgradeService_p497["AbstractSqlTsDatabaseUpgradeService"] -->|extends| TimescaleTsDatabaseUpgradeService_c497["TimescaleTsDatabaseUpgradeService"]
    DatabaseTsUpgradeService_p498["DatabaseTsUpgradeService"] -->|implements| TimescaleTsDatabaseUpgradeService_c498["TimescaleTsDatabaseUpgradeService"]
    DatabaseSchemaService_p499["DatabaseSchemaService"] -->|extends| TsDatabaseSchemaService_c499["TsDatabaseSchemaService"]
    DatabaseSchemaService_p500["DatabaseSchemaService"] -->|extends| TsLatestDatabaseSchemaService_c500["TsLatestDatabaseSchemaService"]
    Object______p501["Object/外部框架"] -->|extends| CQLStatementsParser_c501["CQLStatementsParser"]
    Object______p502["Object/外部框架"] -->|extends| CassandraToSqlColumn_c502["CassandraToSqlColumn"]
    Object______p503["Object/外部框架"] -->|extends| CassandraToSqlColumnData_c503["CassandraToSqlColumnData"]
    Object______p504["Object/外部框架"] -->|extends| CassandraToSqlTable_c504["CassandraToSqlTable"]
    Object______p505["Object/外部框架"] -->|extends| CassandraTsLatestToSqlMigrateService_c505["CassandraTsLatestToSqlMigrateService"]
    TsLatestMigrateService_p506["TsLatestMigrateService"] -->|implements| CassandraTsLatestToSqlMigrateService_c506["CassandraTsLatestToSqlMigrateService"]
    Object______p507["Object/外部框架"] -->|extends| DefaultCacheCleanupService_c507["DefaultCacheCleanupService"]
    CacheCleanupService_p508["CacheCleanupService"] -->|implements| DefaultCacheCleanupService_c508["DefaultCacheCleanupService"]
    Object______p509["Object/外部框架"] -->|extends| DefaultDataUpdateService_c509["DefaultDataUpdateService"]
    DataUpdateService_p510["DataUpdateService"] -->|implements| DefaultDataUpdateService_c510["DefaultDataUpdateService"]
    Object______p511["Object/外部框架"] -->|extends| ImagesUpdater_c511["ImagesUpdater"]
    Object______p512["Object/外部框架"] -->|extends| PaginatedUpdater_c512["PaginatedUpdater"]
    Object______p513["Object/外部框架"] -->|extends| LwM2MServiceImpl_c513["LwM2MServiceImpl"]
    LwM2MService_p514["LwM2MService"] -->|implements| LwM2MServiceImpl_c514["LwM2MServiceImpl"]
    Object______p515["Object/外部框架"] -->|extends| DefaultMailService_c515["DefaultMailService"]
    MailService_p516["MailService"] -->|implements| DefaultMailService_c516["DefaultMailService"]
    Object______p517["Object/外部框架"] -->|extends| DefaultTbMailConfigTemplateService_c517["DefaultTbMailConfigTemplateService"]
    TbMailConfigTemplateService_p518["TbMailConfigTemplateService"] -->|implements| DefaultTbMailConfigTemplateService_c518["DefaultTbMailConfigTemplateService"]
    AbstractListeningExecutor_p519["AbstractListeningExecutor"] -->|extends| MailExecutorService_c519["MailExecutorService"]
    AbstractListeningExecutor_p520["AbstractListeningExecutor"] -->|extends| PasswordResetExecutorService_c520["PasswordResetExecutorService"]
    Object______p521["Object/外部框架"] -->|extends| RefreshTokenExpCheckService_c521["RefreshTokenExpCheckService"]
    Object______p522["Object/外部框架"] -->|extends| TbMailContextComponent_c522["TbMailContextComponent"]
    JavaMailSenderImpl_p523["JavaMailSenderImpl"] -->|extends| TbMailSender_c523["TbMailSender"]
    AbstractSubscriptionService_p524["AbstractSubscriptionService"] -->|extends| DefaultNotificationCenter_c524["DefaultNotificationCenter"]
    NotificationCenter_p525["NotificationCenter"] -->|implements| DefaultNotificationCenter_c525["DefaultNotificationCenter"]
    NotificationChannel_p526["NotificationChannel"] -->|implements| DefaultNotificationCenter_c526["DefaultNotificationCenter"]
    AbstractPartitionBasedService_p527["AbstractPartitionBasedService"] -->|extends| DefaultNotificationSchedulerService_c527["DefaultNotificationSchedulerService"]
    NotificationSchedulerService_p528["NotificationSchedulerService"] -->|implements| DefaultNotificationSchedulerService_c528["DefaultNotificationSchedulerService"]
    Object______p529["Object/外部框架"] -->|extends| ScheduledRequestMetadata_c529["ScheduledRequestMetadata"]
    Object______p530["Object/外部框架"] -->|extends| NotificationProcessingContext_c530["NotificationProcessingContext"]
    Object______p531["Object/外部框架"] -->|extends| EmailNotificationChannel_c531["EmailNotificationChannel"]
    NotificationChannel_p532["NotificationChannel"] -->|implements| EmailNotificationChannel_c532["EmailNotificationChannel"]
    Object______p533["Object/外部框架"] -->|extends| MicrosoftTeamsNotificationChannel_c533["MicrosoftTeamsNotificationChannel"]
    NotificationChannel_p534["NotificationChannel"] -->|implements| MicrosoftTeamsNotificationChannel_c534["MicrosoftTeamsNotificationChannel"]
    Object______p535["Object/外部框架"] -->|extends| Message_c535["Message"]
    Object______p536["Object/外部框架"] -->|extends| Section_c536["Section"]
    Object______p537["Object/外部框架"] -->|extends| Fact_c537["Fact"]
    Object______p538["Object/外部框架"] -->|extends| ActionCard_c538["ActionCard"]
    Object______p539["Object/外部框架"] -->|extends| Input_c539["Input"]
    Object______p540["Object/外部框架"] -->|extends| Choice_c540["Choice"]
    Object______p541["Object/外部框架"] -->|extends| Action_c541["Action"]
    Object______p542["Object/外部框架"] -->|extends| Target_c542["Target"]
    Object______p543["Object/外部框架"] -->|extends| MobileAppNotificationChannel_c543["MobileAppNotificationChannel"]
    NotificationChannel_p544["NotificationChannel"] -->|implements| MobileAppNotificationChannel_c544["MobileAppNotificationChannel"]
    Object______p545["Object/外部框架"] -->|extends| SlackNotificationChannel_c545["SlackNotificationChannel"]
    NotificationChannel_p546["NotificationChannel"] -->|implements| SlackNotificationChannel_c546["SlackNotificationChannel"]
    Object______p547["Object/外部框架"] -->|extends| SmsNotificationChannel_c547["SmsNotificationChannel"]
    NotificationChannel_p548["NotificationChannel"] -->|implements| SmsNotificationChannel_c548["SmsNotificationChannel"]
    Object______p549["Object/外部框架"] -->|extends| DefaultFirebaseService_c549["DefaultFirebaseService"]
    FirebaseService_p550["FirebaseService"] -->|implements| DefaultFirebaseService_c550["DefaultFirebaseService"]
    Object______p551["Object/外部框架"] -->|extends| FirebaseContext_c551["FirebaseContext"]
    Object______p552["Object/外部框架"] -->|extends| DefaultSlackService_c552["DefaultSlackService"]
    SlackService_p553["SlackService"] -->|implements| DefaultSlackService_c553["DefaultSlackService"]
    Object______p554["Object/外部框架"] -->|extends| DefaultNotificationRuleProcessor_c554["DefaultNotificationRuleProcessor"]
    NotificationRuleProcessor_p555["NotificationRuleProcessor"] -->|implements| DefaultNotificationRuleProcessor_c555["DefaultNotificationRuleProcessor"]
    Object______p556["Object/外部框架"] -->|extends| DefaultNotificationRulesCache_c556["DefaultNotificationRulesCache"]
    NotificationRulesCache_p557["NotificationRulesCache"] -->|implements| DefaultNotificationRulesCache_c557["DefaultNotificationRulesCache"]
    Object______p558["Object/外部框架"] -->|extends| AlarmAssignmentTriggerProcessor_c558["AlarmAssignmentTriggerProcessor"]
    NotificationRuleTriggerProcessor_p559["NotificationRuleTriggerProcessor"] -->|implements| AlarmAssignmentTriggerProcessor_c559["AlarmAssignmentTriggerProcessor"]
    Object______p560["Object/外部框架"] -->|extends| AlarmCommentTriggerProcessor_c560["AlarmCommentTriggerProcessor"]
    NotificationRuleTriggerProcessor_p561["NotificationRuleTriggerProcessor"] -->|implements| AlarmCommentTriggerProcessor_c561["AlarmCommentTriggerProcessor"]
    Object______p562["Object/外部框架"] -->|extends| AlarmTriggerProcessor_c562["AlarmTriggerProcessor"]
    NotificationRuleTriggerProcessor_p563["NotificationRuleTriggerProcessor"] -->|implements| AlarmTriggerProcessor_c563["AlarmTriggerProcessor"]
    Object______p564["Object/外部框架"] -->|extends| ApiUsageLimitTriggerProcessor_c564["ApiUsageLimitTriggerProcessor"]
    NotificationRuleTriggerProcessor_p565["NotificationRuleTriggerProcessor"] -->|implements| ApiUsageLimitTriggerProcessor_c565["ApiUsageLimitTriggerProcessor"]
    Object______p566["Object/外部框架"] -->|extends| DeviceActivityTriggerProcessor_c566["DeviceActivityTriggerProcessor"]
    NotificationRuleTriggerProcessor_p567["NotificationRuleTriggerProcessor"] -->|implements| DeviceActivityTriggerProcessor_c567["DeviceActivityTriggerProcessor"]
    Object______p568["Object/外部框架"] -->|extends| EdgeCommunicationFailureTriggerProcessor_c568["EdgeCommunicationFailureTriggerProcessor"]
    NotificationRuleTriggerProcessor_p569["NotificationRuleTriggerProcessor"] -->|implements| EdgeCommunicationFailureTriggerProcessor_c569["EdgeCommunicationFailureTriggerProcessor"]
    Object______p570["Object/外部框架"] -->|extends| EdgeConnectionTriggerProcessor_c570["EdgeConnectionTriggerProcessor"]
    NotificationRuleTriggerProcessor_p571["NotificationRuleTriggerProcessor"] -->|implements| EdgeConnectionTriggerProcessor_c571["EdgeConnectionTriggerProcessor"]
    Object______p572["Object/外部框架"] -->|extends| EntitiesLimitTriggerProcessor_c572["EntitiesLimitTriggerProcessor"]
    NotificationRuleTriggerProcessor_p573["NotificationRuleTriggerProcessor"] -->|implements| EntitiesLimitTriggerProcessor_c573["EntitiesLimitTriggerProcessor"]
    Object______p574["Object/外部框架"] -->|extends| EntityActionTriggerProcessor_c574["EntityActionTriggerProcessor"]
    NotificationRuleTriggerProcessor_p575["NotificationRuleTriggerProcessor"] -->|implements| EntityActionTriggerProcessor_c575["EntityActionTriggerProcessor"]
    Object______p576["Object/外部框架"] -->|extends| NewPlatformVersionTriggerProcessor_c576["NewPlatformVersionTriggerProcessor"]
    NotificationRuleTriggerProcessor_p577["NotificationRuleTriggerProcessor"] -->|implements| NewPlatformVersionTriggerProcessor_c577["NewPlatformVersionTriggerProcessor"]
    Object______p578["Object/外部框架"] -->|extends| RateLimitsTriggerProcessor_c578["RateLimitsTriggerProcessor"]
    NotificationRuleTriggerProcessor_p579["NotificationRuleTriggerProcessor"] -->|implements| RateLimitsTriggerProcessor_c579["RateLimitsTriggerProcessor"]
    Object______p580["Object/外部框架"] -->|extends| RuleEngineComponentLifecycleEventTriggerProcessor_c580["RuleEngineComponentLifecycleEventTriggerProcessor"]
    NotificationRuleTriggerProcessor_p581["NotificationRuleTriggerProcessor"] -->|implements| RuleEngineComponentLifecycleEventTriggerProcessor_c581["RuleEngineComponentLifecycleEventTriggerProcessor"]
    Object______p582["Object/外部框架"] -->|extends| DefaultOtaPackageStateService_c582["DefaultOtaPackageStateService"]
    OtaPackageStateService_p583["OtaPackageStateService"] -->|implements| DefaultOtaPackageStateService_c583["DefaultOtaPackageStateService"]
    Object______p584["Object/外部框架"] -->|extends| AbstractPartitionBasedService_c584["AbstractPartitionBasedService"]
    Object______p585["Object/外部框架"] -->|extends| TbCoreStartupService_c585["TbCoreStartupService"]
    Object______p586["Object/外部框架"] -->|extends| DefaultTbAssetProfileCache_c586["DefaultTbAssetProfileCache"]
    TbAssetProfileCache_p587["TbAssetProfileCache"] -->|implements| DefaultTbAssetProfileCache_c587["DefaultTbAssetProfileCache"]
    Object______p588["Object/外部框架"] -->|extends| DefaultTbDeviceProfileCache_c588["DefaultTbDeviceProfileCache"]
    TbDeviceProfileCache_p589["TbDeviceProfileCache"] -->|implements| DefaultTbDeviceProfileCache_c589["DefaultTbDeviceProfileCache"]
    RuleEngineAssetProfileCache_p590["RuleEngineAssetProfileCache"] -->|extends| TbAssetProfileCache_c590["TbAssetProfileCache"]
    RuleEngineDeviceProfileCache_p591["RuleEngineDeviceProfileCache"] -->|extends| TbDeviceProfileCache_c591["TbDeviceProfileCache"]
    Object______p592["Object/外部框架"] -->|extends| DefaultEntityQueryService_c592["DefaultEntityQueryService"]
    EntityQueryService_p593["EntityQueryService"] -->|implements| DefaultEntityQueryService_c593["DefaultEntityQueryService"]
    Object______p594["Object/外部框架"] -->|extends| DefaultQueueRoutingInfoService_c594["DefaultQueueRoutingInfoService"]
    QueueRoutingInfoService_p595["QueueRoutingInfoService"] -->|implements| DefaultQueueRoutingInfoService_c595["DefaultQueueRoutingInfoService"]
    Object______p596["Object/外部框架"] -->|extends| DefaultTbClusterService_c596["DefaultTbClusterService"]
    TbClusterService_p597["TbClusterService"] -->|implements| DefaultTbClusterService_c597["DefaultTbClusterService"]
    AbstractConsumerService_p598["AbstractConsumerService"] -->|extends| DefaultTbCoreConsumerService_c598["DefaultTbCoreConsumerService"]
    TbCoreConsumerService_p599["TbCoreConsumerService"] -->|implements| DefaultTbCoreConsumerService_c599["DefaultTbCoreConsumerService"]
    Object______p600["Object/外部框架"] -->|extends| PendingMsgHolder_c600["PendingMsgHolder"]
    AbstractConsumerService_p601["AbstractConsumerService"] -->|extends| DefaultTbRuleEngineConsumerService_c601["DefaultTbRuleEngineConsumerService"]
    TbRuleEngineConsumerService_p602["TbRuleEngineConsumerService"] -->|implements| DefaultTbRuleEngineConsumerService_c602["DefaultTbRuleEngineConsumerService"]
    Object______p603["Object/外部框架"] -->|extends| DefaultTenantRoutingInfoService_c603["DefaultTenantRoutingInfoService"]
    TenantRoutingInfoService_p604["TenantRoutingInfoService"] -->|implements| DefaultTenantRoutingInfoService_c604["DefaultTenantRoutingInfoService"]
    ApplicationListener_p605["ApplicationListener"] -->|extends| TbCoreConsumerService_c605["TbCoreConsumerService"]
    Object______p606["Object/外部框架"] -->|extends| TbCoreConsumerStats_c606["TbCoreConsumerStats"]
    Object______p607["Object/外部框架"] -->|extends| TbMsgPackCallback_c607["TbMsgPackCallback"]
    TbMsgCallback_p608["TbMsgCallback"] -->|implements| TbMsgPackCallback_c608["TbMsgPackCallback"]
    Object______p609["Object/外部框架"] -->|extends| TbMsgPackProcessingContext_c609["TbMsgPackProcessingContext"]
    Object______p610["Object/外部框架"] -->|extends| TbMsgProfilerInfo_c610["TbMsgProfilerInfo"]
    Object______p611["Object/外部框架"] -->|extends| TbPackCallback_c611["TbPackCallback"]
    TbCallback_p612["TbCallback"] -->|implements| TbPackCallback_c612["TbPackCallback"]
    Object______p613["Object/外部框架"] -->|extends| TbPackProcessingContext_c613["TbPackProcessingContext"]
    ApplicationListener_p614["ApplicationListener"] -->|extends| TbRuleEngineConsumerService_c614["TbRuleEngineConsumerService"]
    Object______p615["Object/外部框架"] -->|extends| TbRuleEngineConsumerStats_c615["TbRuleEngineConsumerStats"]
    Object______p616["Object/外部框架"] -->|extends| TbRuleNodeProfilerInfo_c616["TbRuleNodeProfilerInfo"]
    Object______p617["Object/外部框架"] -->|extends| TbTenantRuleEngineStats_c617["TbTenantRuleEngineStats"]
    Object______p618["Object/外部框架"] -->|extends| TbTopicWithConsumerPerPartition_c618["TbTopicWithConsumerPerPartition"]
    Object______p619["Object/外部框架"] -->|extends| AbstractConsumerService_c619["AbstractConsumerService"]
    Object______p620["Object/外部框架"] -->|extends| AbstractTbRuleEngineSubmitStrategy_c620["AbstractTbRuleEngineSubmitStrategy"]
    TbRuleEngineSubmitStrategy_p621["TbRuleEngineSubmitStrategy"] -->|implements| AbstractTbRuleEngineSubmitStrategy_c621["AbstractTbRuleEngineSubmitStrategy"]
    AbstractTbRuleEngineSubmitStrategy_p622["AbstractTbRuleEngineSubmitStrategy"] -->|extends| BatchTbRuleEngineSubmitStrategy_c622["BatchTbRuleEngineSubmitStrategy"]
    AbstractTbRuleEngineSubmitStrategy_p623["AbstractTbRuleEngineSubmitStrategy"] -->|extends| BurstTbRuleEngineSubmitStrategy_c623["BurstTbRuleEngineSubmitStrategy"]
    Object______p624["Object/外部框架"] -->|extends| IdMsgPair_c624["IdMsgPair"]
    AbstractTbRuleEngineSubmitStrategy_p625["AbstractTbRuleEngineSubmitStrategy"] -->|extends| SequentialByEntityIdTbRuleEngineSubmitStrategy_c625["SequentialByEntityIdTbRuleEngineSubmitStrategy"]
    SequentialByEntityIdTbRuleEngineSubmitStrategy_p626["SequentialByEntityIdTbRuleEngineSubmitStrategy"] -->|extends| SequentialByOriginatorIdTbRuleEngineSubmitStrategy_c626["SequentialByOriginatorIdTbRuleEngineSubmitStrategy"]
    SequentialByEntityIdTbRuleEngineSubmitStrategy_p627["SequentialByEntityIdTbRuleEngineSubmitStrategy"] -->|extends| SequentialByTenantIdTbRuleEngineSubmitStrategy_c627["SequentialByTenantIdTbRuleEngineSubmitStrategy"]
    AbstractTbRuleEngineSubmitStrategy_p628["AbstractTbRuleEngineSubmitStrategy"] -->|extends| SequentialTbRuleEngineSubmitStrategy_c628["SequentialTbRuleEngineSubmitStrategy"]
    Object______p629["Object/外部框架"] -->|extends| TbRuleEngineProcessingDecision_c629["TbRuleEngineProcessingDecision"]
    Object______p630["Object/外部框架"] -->|extends| TbRuleEngineProcessingResult_c630["TbRuleEngineProcessingResult"]
    Object______p631["Object/外部框架"] -->|extends| TbRuleEngineProcessingStrategyFactory_c631["TbRuleEngineProcessingStrategyFactory"]
    Object______p632["Object/外部框架"] -->|extends| RetryStrategy_c632["RetryStrategy"]
    TbRuleEngineProcessingStrategy_p633["TbRuleEngineProcessingStrategy"] -->|implements| RetryStrategy_c633["RetryStrategy"]
    Object______p634["Object/外部框架"] -->|extends| SkipStrategy_c634["SkipStrategy"]
    TbRuleEngineProcessingStrategy_p635["TbRuleEngineProcessingStrategy"] -->|implements| SkipStrategy_c635["SkipStrategy"]
    Object______p636["Object/外部框架"] -->|extends| TbRuleEngineSubmitStrategyFactory_c636["TbRuleEngineSubmitStrategyFactory"]
    Serializable_p637["Serializable"] -->|implements| QueueEvent_c637["QueueEvent"]
    Object______p638["Object/外部框架"] -->|extends| TbQueueConsumerManagerTask_c638["TbQueueConsumerManagerTask"]
    Object______p639["Object/外部框架"] -->|extends| TbQueueConsumerTask_c639["TbQueueConsumerTask"]
    Object______p640["Object/外部框架"] -->|extends| TbRuleEngineConsumerContext_c640["TbRuleEngineConsumerContext"]
    Object______p641["Object/外部框架"] -->|extends| TbRuleEngineQueueConsumerManager_c641["TbRuleEngineQueueConsumerManager"]
    Object______p642["Object/外部框架"] -->|extends| ConsumerPerPartitionWrapper_c642["ConsumerPerPartitionWrapper"]
    ConsumerWrapper_p643["ConsumerWrapper"] -->|implements| ConsumerPerPartitionWrapper_c643["ConsumerPerPartitionWrapper"]
    Object______p644["Object/外部框架"] -->|extends| SingleConsumerWrapper_c644["SingleConsumerWrapper"]
    ConsumerWrapper_p645["ConsumerWrapper"] -->|implements| SingleConsumerWrapper_c645["SingleConsumerWrapper"]
    AbstractTbEntityService_p646["AbstractTbEntityService"] -->|extends| DefaultTbImageService_c646["DefaultTbImageService"]
    TbImageService_p647["TbImageService"] -->|implements| DefaultTbImageService_c647["DefaultTbImageService"]
    AbstractTbEntityService_p648["AbstractTbEntityService"] -->|extends| DefaultTbResourceService_c648["DefaultTbResourceService"]
    TbResourceService_p649["TbResourceService"] -->|implements| DefaultTbResourceService_c649["DefaultTbResourceService"]
    SimpleTbEntityService_p650["SimpleTbEntityService"] -->|extends| TbResourceService_c650["TbResourceService"]
    Object______p651["Object/外部框架"] -->|extends| DefaultTbCoreDeviceRpcService_c651["DefaultTbCoreDeviceRpcService"]
    TbCoreDeviceRpcService_p652["TbCoreDeviceRpcService"] -->|implements| DefaultTbCoreDeviceRpcService_c652["DefaultTbCoreDeviceRpcService"]
    Object______p653["Object/外部框架"] -->|extends| DefaultTbRuleEngineRpcService_c653["DefaultTbRuleEngineRpcService"]
    TbRuleEngineDeviceRpcService_p654["TbRuleEngineDeviceRpcService"] -->|implements| DefaultTbRuleEngineRpcService_c654["DefaultTbRuleEngineRpcService"]
    Object______p655["Object/外部框架"] -->|extends| LocalRequestMetaData_c655["LocalRequestMetaData"]
    Object______p656["Object/外部框架"] -->|extends| TbRpcService_c656["TbRpcService"]
    RuleEngineRpcService_p657["RuleEngineRpcService"] -->|extends| TbRuleEngineDeviceRpcService_c657["TbRuleEngineDeviceRpcService"]
    AbstractTbEntityService_p658["AbstractTbEntityService"] -->|extends| DefaultTbRuleChainService_c658["DefaultTbRuleChainService"]
    TbRuleChainService_p659["TbRuleChainService"] -->|implements| DefaultTbRuleChainService_c659["DefaultTbRuleChainService"]
    SimpleTbEntityService_p660["SimpleTbEntityService"] -->|extends| TbRuleChainService_c660["TbRuleChainService"]
    RuleNodeScriptEngine_p661["RuleNodeScriptEngine"] -->|extends| RuleNodeJsScriptEngine_c661["RuleNodeJsScriptEngine"]
    Object______p662["Object/外部框架"] -->|extends| RuleNodeScriptEngine_c662["RuleNodeScriptEngine"]
    ScriptEngine_p663["ScriptEngine"] -->|implements| RuleNodeScriptEngine_c663["RuleNodeScriptEngine"]
    RuleNodeScriptEngine_p664["RuleNodeScriptEngine"] -->|extends| RuleNodeTbelScriptEngine_c664["RuleNodeTbelScriptEngine"]
    Object______p665["Object/外部框架"] -->|extends| AccessValidator_c665["AccessValidator"]
    Object______p666["Object/外部框架"] -->|extends| ValidationCallback_c666["ValidationCallback"]
    Object______p667["Object/外部框架"] -->|extends| ValidationResult_c667["ValidationResult"]
    AbstractAuthenticationToken_p668["AbstractAuthenticationToken"] -->|extends| AbstractJwtAuthenticationToken_c668["AbstractJwtAuthenticationToken"]
    Object______p669["Object/外部框架"] -->|extends| DefaultTokenOutdatingService_c669["DefaultTokenOutdatingService"]
    TokenOutdatingService_p670["TokenOutdatingService"] -->|implements| DefaultTokenOutdatingService_c670["DefaultTokenOutdatingService"]
    AbstractJwtAuthenticationToken_p671["AbstractJwtAuthenticationToken"] -->|extends| JwtAuthenticationToken_c671["JwtAuthenticationToken"]
    AbstractJwtAuthenticationToken_p672["AbstractJwtAuthenticationToken"] -->|extends| MfaAuthenticationToken_c672["MfaAuthenticationToken"]
    AbstractJwtAuthenticationToken_p673["AbstractJwtAuthenticationToken"] -->|extends| RefreshAuthenticationToken_c673["RefreshAuthenticationToken"]
    Object______p674["Object/外部框架"] -->|extends| JwtAuthenticationProvider_c674["JwtAuthenticationProvider"]
    AuthenticationProvider_p675["AuthenticationProvider"] -->|implements| JwtAuthenticationProvider_c675["JwtAuthenticationProvider"]
    AbstractAuthenticationProcessingFilter_p676["AbstractAuthenticationProcessingFilter"] -->|extends| JwtTokenAuthenticationProcessingFilter_c676["JwtTokenAuthenticationProcessingFilter"]
    Object______p677["Object/外部框架"] -->|extends| RefreshTokenAuthenticationProvider_c677["RefreshTokenAuthenticationProvider"]
    AuthenticationProvider_p678["AuthenticationProvider"] -->|implements| RefreshTokenAuthenticationProvider_c678["RefreshTokenAuthenticationProvider"]
    AbstractAuthenticationProcessingFilter_p679["AbstractAuthenticationProcessingFilter"] -->|extends| RefreshTokenProcessingFilter_c679["RefreshTokenProcessingFilter"]
    Object______p680["Object/外部框架"] -->|extends| RefreshTokenRequest_c680["RefreshTokenRequest"]
    Object______p681["Object/外部框架"] -->|extends| SkipPathRequestMatcher_c681["SkipPathRequestMatcher"]
    RequestMatcher_p682["RequestMatcher"] -->|implements| SkipPathRequestMatcher_c682["SkipPathRequestMatcher"]
    Object______p683["Object/外部框架"] -->|extends| JwtHeaderTokenExtractor_c683["JwtHeaderTokenExtractor"]
    TokenExtractor_p684["TokenExtractor"] -->|implements| JwtHeaderTokenExtractor_c684["JwtHeaderTokenExtractor"]
    Object______p685["Object/外部框架"] -->|extends| JwtQueryTokenExtractor_c685["JwtQueryTokenExtractor"]
    TokenExtractor_p686["TokenExtractor"] -->|implements| JwtQueryTokenExtractor_c686["JwtQueryTokenExtractor"]
    Object______p687["Object/外部框架"] -->|extends| DefaultJwtSettingsService_c687["DefaultJwtSettingsService"]
    JwtSettingsService_p688["JwtSettingsService"] -->|implements| DefaultJwtSettingsService_c688["DefaultJwtSettingsService"]
    Object______p689["Object/外部框架"] -->|extends| DefaultJwtSettingsValidator_c689["DefaultJwtSettingsValidator"]
    JwtSettingsValidator_p690["JwtSettingsValidator"] -->|implements| DefaultJwtSettingsValidator_c690["DefaultJwtSettingsValidator"]
    Object______p691["Object/外部框架"] -->|extends| InstallJwtSettingsValidator_c691["InstallJwtSettingsValidator"]
    JwtSettingsValidator_p692["JwtSettingsValidator"] -->|implements| InstallJwtSettingsValidator_c692["InstallJwtSettingsValidator"]
    Object______p693["Object/外部框架"] -->|extends| DefaultTwoFactorAuthService_c693["DefaultTwoFactorAuthService"]
    TwoFactorAuthService_p694["TwoFactorAuthService"] -->|implements| DefaultTwoFactorAuthService_c694["DefaultTwoFactorAuthService"]
    Object______p695["Object/外部框架"] -->|extends| DefaultTwoFaConfigManager_c695["DefaultTwoFaConfigManager"]
    TwoFaConfigManager_p696["TwoFaConfigManager"] -->|implements| DefaultTwoFaConfigManager_c696["DefaultTwoFaConfigManager"]
    Object______p697["Object/外部框架"] -->|extends| BackupCodeTwoFaProvider_c697["BackupCodeTwoFaProvider"]
    TwoFaProvider_p698["TwoFaProvider"] -->|implements| BackupCodeTwoFaProvider_c698["BackupCodeTwoFaProvider"]
    OtpBasedTwoFaProvider_p699["OtpBasedTwoFaProvider"] -->|extends| EmailTwoFaProvider_c699["EmailTwoFaProvider"]
    Object______p700["Object/外部框架"] -->|extends| OtpBasedTwoFaProvider_c700["OtpBasedTwoFaProvider"]
    Object______p701["Object/外部框架"] -->|extends| Otp_c701["Otp"]
    Serializable_p702["Serializable"] -->|implements| Otp_c702["Otp"]
    OtpBasedTwoFaProvider_p703["OtpBasedTwoFaProvider"] -->|extends| SmsTwoFaProvider_c703["SmsTwoFaProvider"]
    Object______p704["Object/外部框架"] -->|extends| TotpTwoFaProvider_c704["TotpTwoFaProvider"]
    TwoFaProvider_p705["TwoFaProvider"] -->|implements| TotpTwoFaProvider_c705["TotpTwoFaProvider"]
    Object______p706["Object/外部框架"] -->|extends| AbstractOAuth2ClientMapper_c706["AbstractOAuth2ClientMapper"]
    AbstractOAuth2ClientMapper_p707["AbstractOAuth2ClientMapper"] -->|extends| AppleOAuth2ClientMapper_c707["AppleOAuth2ClientMapper"]
    OAuth2ClientMapper_p708["OAuth2ClientMapper"] -->|implements| AppleOAuth2ClientMapper_c708["AppleOAuth2ClientMapper"]
    Object______p709["Object/外部框架"] -->|extends| BasicMapperUtils_c709["BasicMapperUtils"]
    AbstractOAuth2ClientMapper_p710["AbstractOAuth2ClientMapper"] -->|extends| BasicOAuth2ClientMapper_c710["BasicOAuth2ClientMapper"]
    OAuth2ClientMapper_p711["OAuth2ClientMapper"] -->|implements| BasicOAuth2ClientMapper_c711["BasicOAuth2ClientMapper"]
    Object______p712["Object/外部框架"] -->|extends| CookieUtils_c712["CookieUtils"]
    AbstractOAuth2ClientMapper_p713["AbstractOAuth2ClientMapper"] -->|extends| CustomOAuth2ClientMapper_c713["CustomOAuth2ClientMapper"]
    OAuth2ClientMapper_p714["OAuth2ClientMapper"] -->|implements| CustomOAuth2ClientMapper_c714["CustomOAuth2ClientMapper"]
    AbstractOAuth2ClientMapper_p715["AbstractOAuth2ClientMapper"] -->|extends| GithubOAuth2ClientMapper_c715["GithubOAuth2ClientMapper"]
    OAuth2ClientMapper_p716["OAuth2ClientMapper"] -->|implements| GithubOAuth2ClientMapper_c716["GithubOAuth2ClientMapper"]
    ArrayList_p717["ArrayList"] -->|extends| GithubEmailsResponse_c717["GithubEmailsResponse"]
    Object______p718["Object/外部框架"] -->|extends| GithubEmailResponse_c718["GithubEmailResponse"]
    Object______p719["Object/外部框架"] -->|extends| HttpCookieOAuth2AuthorizationRequestRepository_c719["HttpCookieOAuth2AuthorizationRequestRepository"]
    AuthorizationRequestRepository_p720["AuthorizationRequestRepository"] -->|implements| HttpCookieOAuth2AuthorizationRequestRepository_c720["HttpCookieOAuth2AuthorizationRequestRepository"]
    Object______p721["Object/外部框架"] -->|extends| OAuth2ClientMapperProvider_c721["OAuth2ClientMapperProvider"]
    SimpleUrlAuthenticationFailureHandler_p722["SimpleUrlAuthenticationFailureHandler"] -->|extends| Oauth2AuthenticationFailureHandler_c722["Oauth2AuthenticationFailureHandler"]
    SimpleUrlAuthenticationSuccessHandler_p723["SimpleUrlAuthenticationSuccessHandler"] -->|extends| Oauth2AuthenticationSuccessHandler_c723["Oauth2AuthenticationSuccessHandler"]
    Object______p724["Object/外部框架"] -->|extends| LoginRequest_c724["LoginRequest"]
    Object______p725["Object/外部框架"] -->|extends| LoginResponse_c725["LoginResponse"]
    Object______p726["Object/外部框架"] -->|extends| PublicLoginRequest_c726["PublicLoginRequest"]
    Object______p727["Object/外部框架"] -->|extends| RestAuthenticationDetails_c727["RestAuthenticationDetails"]
    Serializable_p728["Serializable"] -->|implements| RestAuthenticationDetails_c728["RestAuthenticationDetails"]
    Object______p729["Object/外部框架"] -->|extends| RestAuthenticationDetailsSource_c729["RestAuthenticationDetailsSource"]
    AuthenticationDetailsSource_p730["AuthenticationDetailsSource"] -->|implements| RestAuthenticationDetailsSource_c730["RestAuthenticationDetailsSource"]
    Object______p731["Object/外部框架"] -->|extends| RestAuthenticationProvider_c731["RestAuthenticationProvider"]
    AuthenticationProvider_p732["AuthenticationProvider"] -->|implements| RestAuthenticationProvider_c732["RestAuthenticationProvider"]
    Object______p733["Object/外部框架"] -->|extends| RestAwareAuthenticationFailureHandler_c733["RestAwareAuthenticationFailureHandler"]
    AuthenticationFailureHandler_p734["AuthenticationFailureHandler"] -->|implements| RestAwareAuthenticationFailureHandler_c734["RestAwareAuthenticationFailureHandler"]
    Object______p735["Object/外部框架"] -->|extends| RestAwareAuthenticationSuccessHandler_c735["RestAwareAuthenticationSuccessHandler"]
    AuthenticationSuccessHandler_p736["AuthenticationSuccessHandler"] -->|implements| RestAwareAuthenticationSuccessHandler_c736["RestAwareAuthenticationSuccessHandler"]
    AbstractAuthenticationProcessingFilter_p737["AbstractAuthenticationProcessingFilter"] -->|extends| RestLoginProcessingFilter_c737["RestLoginProcessingFilter"]
    AbstractAuthenticationProcessingFilter_p738["AbstractAuthenticationProcessingFilter"] -->|extends| RestPublicLoginProcessingFilter_c738["RestPublicLoginProcessingFilter"]
    Object______p739["Object/外部框架"] -->|extends| DefaultDeviceAuthService_c739["DefaultDeviceAuthService"]
    DeviceAuthService_p740["DeviceAuthService"] -->|implements| DefaultDeviceAuthService_c740["DefaultDeviceAuthService"]
    AuthenticationServiceException_p741["AuthenticationServiceException"] -->|extends| AuthMethodNotSupportedException_c741["AuthMethodNotSupportedException"]
    AuthenticationException_p742["AuthenticationException"] -->|extends| JwtExpiredTokenException_c742["JwtExpiredTokenException"]
    CredentialsExpiredException_p743["CredentialsExpiredException"] -->|extends| UserPasswordExpiredException_c743["UserPasswordExpiredException"]
    AccountStatusException_p744["AccountStatusException"] -->|extends| UserPasswordNotValidException_c744["UserPasswordNotValidException"]
    Object______p745["Object/外部框架"] -->|extends| ActivateUserRequest_c745["ActivateUserRequest"]
    Object______p746["Object/外部框架"] -->|extends| ChangePasswordRequest_c746["ChangePasswordRequest"]
    Object______p747["Object/外部框架"] -->|extends| ResetPasswordEmailRequest_c747["ResetPasswordEmailRequest"]
    Object______p748["Object/外部框架"] -->|extends| ResetPasswordRequest_c748["ResetPasswordRequest"]
    User_p749["User"] -->|extends| SecurityUser_c749["SecurityUser"]
    Object______p750["Object/外部框架"] -->|extends| UserPrincipal_c750["UserPrincipal"]
    Serializable_p751["Serializable"] -->|implements| UserPrincipal_c751["UserPrincipal"]
    Object______p752["Object/外部框架"] -->|extends| AccessJwtToken_c752["AccessJwtToken"]
    JwtToken_p753["JwtToken"] -->|implements| AccessJwtToken_c753["AccessJwtToken"]
    Object______p754["Object/外部框架"] -->|extends| JwtTokenFactory_c754["JwtTokenFactory"]
    Object______p755["Object/外部框架"] -->|extends| OAuth2AppTokenFactory_c755["OAuth2AppTokenFactory"]
    Object______p756["Object/外部框架"] -->|extends| RawAccessJwtToken_c756["RawAccessJwtToken"]
    JwtToken_p757["JwtToken"] -->|implements| RawAccessJwtToken_c757["RawAccessJwtToken"]
    Serializable_p758["Serializable"] -->|implements| RawAccessJwtToken_c758["RawAccessJwtToken"]
    HashMap_p759["HashMap"] -->|extends| AbstractPermissions_c759["AbstractPermissions"]
    Permissions_p760["Permissions"] -->|implements| AbstractPermissions_c760["AbstractPermissions"]
    AbstractPermissions_p761["AbstractPermissions"] -->|extends| CustomerUserPermissions_c761["CustomerUserPermissions"]
    Object______p762["Object/外部框架"] -->|extends| DefaultAccessControlService_c762["DefaultAccessControlService"]
    AccessControlService_p763["AccessControlService"] -->|implements| DefaultAccessControlService_c763["DefaultAccessControlService"]
    Object______p764["Object/外部框架"] -->|extends| GenericPermissionChecker_c764["GenericPermissionChecker"]
    AbstractPermissions_p765["AbstractPermissions"] -->|extends| SysAdminPermissions_c765["SysAdminPermissions"]
    AbstractPermissions_p766["AbstractPermissions"] -->|extends| TenantAdminPermissions_c766["TenantAdminPermissions"]
    Object______p767["Object/外部框架"] -->|extends| DefaultSystemSecurityService_c767["DefaultSystemSecurityService"]
    SystemSecurityService_p768["SystemSecurityService"] -->|implements| DefaultSystemSecurityService_c768["DefaultSystemSecurityService"]
    Object______p769["Object/外部框架"] -->|extends| DefaultDeviceSessionCacheService_c769["DefaultDeviceSessionCacheService"]
    DeviceSessionCacheService_p770["DeviceSessionCacheService"] -->|implements| DefaultDeviceSessionCacheService_c770["DefaultDeviceSessionCacheService"]
    CaffeineTbTransactionalCache_p771["CaffeineTbTransactionalCache"] -->|extends| SessionCaffeineCache_c771["SessionCaffeineCache"]
    RedisTbTransactionalCache_p772["RedisTbTransactionalCache"] -->|extends| SessionRedisCache_c772["SessionRedisCache"]
    Object______p773["Object/外部框架"] -->|extends| AbstractSmsSender_c773["AbstractSmsSender"]
    SmsSender_p774["SmsSender"] -->|implements| AbstractSmsSender_c774["AbstractSmsSender"]
    Object______p775["Object/外部框架"] -->|extends| DefaultSmsSenderFactory_c775["DefaultSmsSenderFactory"]
    SmsSenderFactory_p776["SmsSenderFactory"] -->|implements| DefaultSmsSenderFactory_c776["DefaultSmsSenderFactory"]
    Object______p777["Object/外部框架"] -->|extends| DefaultSmsService_c777["DefaultSmsService"]
    SmsService_p778["SmsService"] -->|implements| DefaultSmsService_c778["DefaultSmsService"]
    AbstractListeningExecutor_p779["AbstractListeningExecutor"] -->|extends| SmsExecutorService_c779["SmsExecutorService"]
    AbstractSmsSender_p780["AbstractSmsSender"] -->|extends| AwsSmsSender_c780["AwsSmsSender"]
    AbstractSmsSender_p781["AbstractSmsSender"] -->|extends| SmppSmsSender_c781["SmppSmsSender"]
    AbstractSmsSender_p782["AbstractSmsSender"] -->|extends| TwilioSmsSender_c782["TwilioSmsSender"]
    AbstractPartitionBasedService_p783["AbstractPartitionBasedService"] -->|extends| DefaultDeviceStateService_c783["DefaultDeviceStateService"]
    DeviceStateService_p784["DeviceStateService"] -->|implements| DefaultDeviceStateService_c784["DefaultDeviceStateService"]
    Object______p785["Object/外部框架"] -->|extends| DevicePackFutureHolder_c785["DevicePackFutureHolder"]
    Object______p786["Object/外部框架"] -->|extends| DefaultRuleEngineDeviceStateManager_c786["DefaultRuleEngineDeviceStateManager"]
    RuleEngineDeviceStateManager_p787["RuleEngineDeviceStateManager"] -->|implements| DefaultRuleEngineDeviceStateManager_c787["DefaultRuleEngineDeviceStateManager"]
    Object______p788["Object/外部框架"] -->|extends| ConnectivityEventInfo_c788["ConnectivityEventInfo"]
    Object______p789["Object/外部框架"] -->|extends| DeviceState_c789["DeviceState"]
    Object______p790["Object/外部框架"] -->|extends| DeviceStateData_c790["DeviceStateData"]
    ApplicationListener_p791["ApplicationListener"] -->|extends| DeviceStateService_c791["DeviceStateService"]
    Object______p792["Object/外部框架"] -->|extends| DefaultJsInvokeStats_c792["DefaultJsInvokeStats"]
    JsInvokeStats_p793["JsInvokeStats"] -->|implements| DefaultJsInvokeStats_c793["DefaultJsInvokeStats"]
    Object______p794["Object/外部框架"] -->|extends| DefaultRuleEngineStatisticsService_c794["DefaultRuleEngineStatisticsService"]
    RuleEngineStatisticsService_p795["RuleEngineStatisticsService"] -->|implements| DefaultRuleEngineStatisticsService_c795["DefaultRuleEngineStatisticsService"]
    Object______p796["Object/外部框架"] -->|extends| TenantQueueKey_c796["TenantQueueKey"]
    TbApplicationEventListener_p797["TbApplicationEventListener"] -->|extends| DefaultSubscriptionManagerService_c797["DefaultSubscriptionManagerService"]
    SubscriptionManagerService_p798["SubscriptionManagerService"] -->|implements| DefaultSubscriptionManagerService_c798["DefaultSubscriptionManagerService"]
    Object______p799["Object/外部框架"] -->|extends| DefaultTbEntityDataSubscriptionService_c799["DefaultTbEntityDataSubscriptionService"]
    TbEntityDataSubscriptionService_p800["TbEntityDataSubscriptionService"] -->|implements| DefaultTbEntityDataSubscriptionService_c800["DefaultTbEntityDataSubscriptionService"]
    Object______p801["Object/外部框架"] -->|extends| DefaultTbLocalSubscriptionService_c801["DefaultTbLocalSubscriptionService"]
    TbLocalSubscriptionService_p802["TbLocalSubscriptionService"] -->|implements| DefaultTbLocalSubscriptionService_c802["DefaultTbLocalSubscriptionService"]
    Object______p803["Object/外部框架"] -->|extends| ReadTsKvQueryInfo_c803["ReadTsKvQueryInfo"]
    ApplicationListener_p804["ApplicationListener"] -->|extends| SubscriptionManagerService_c804["SubscriptionManagerService"]
    Object______p805["Object/外部框架"] -->|extends| SubscriptionSchedulerComponent_c805["SubscriptionSchedulerComponent"]
    Object______p806["Object/外部框架"] -->|extends| SubscriptionServiceStatistics_c806["SubscriptionServiceStatistics"]
    Object______p807["Object/外部框架"] -->|extends| TbAbstractDataSubCtx_c807["TbAbstractDataSubCtx"]
    Object______p808["Object/外部框架"] -->|extends| TbAbstractSubCtx_c808["TbAbstractSubCtx"]
    Object______p809["Object/外部框架"] -->|extends| DynamicValueKeySub_c809["DynamicValueKeySub"]
    Object______p810["Object/外部框架"] -->|extends| DynamicValueKey_c810["DynamicValueKey"]
    TbAbstractSubCtx_p811["TbAbstractSubCtx"] -->|extends| TbAlarmCountSubCtx_c811["TbAlarmCountSubCtx"]
    TbAbstractDataSubCtx_p812["TbAbstractDataSubCtx"] -->|extends| TbAlarmDataSubCtx_c812["TbAlarmDataSubCtx"]
    TbSubscription_p813["TbSubscription"] -->|extends| TbAlarmsSubscription_c813["TbAlarmsSubscription"]
    TbSubscription_p814["TbSubscription"] -->|extends| TbAttributeSubscription_c814["TbAttributeSubscription"]
    TbAbstractSubCtx_p815["TbAbstractSubCtx"] -->|extends| TbEntityCountSubCtx_c815["TbEntityCountSubCtx"]
    TbAbstractDataSubCtx_p816["TbAbstractDataSubCtx"] -->|extends| TbEntityDataSubCtx_c816["TbEntityDataSubCtx"]
    Object______p817["Object/外部框架"] -->|extends| TbEntityLocalSubsInfo_c817["TbEntityLocalSubsInfo"]
    Object______p818["Object/外部框架"] -->|extends| TbEntityRemoteSubsInfo_c818["TbEntityRemoteSubsInfo"]
    Object______p819["Object/外部框架"] -->|extends| TbEntitySubEvent_c819["TbEntitySubEvent"]
    Object______p820["Object/外部框架"] -->|extends| TbEntityUpdatesInfo_c820["TbEntityUpdatesInfo"]
    Object______p821["Object/外部框架"] -->|extends| TbSubscription_c821["TbSubscription"]
    Object______p822["Object/外部框架"] -->|extends| TbSubscriptionUtils_c822["TbSubscriptionUtils"]
    Object______p823["Object/外部框架"] -->|extends| TbSubscriptionsInfo_c823["TbSubscriptionsInfo"]
    TbSubscription_p824["TbSubscription"] -->|extends| TbTimeSeriesSubscription_c824["TbTimeSeriesSubscription"]
    Object______p825["Object/外部框架"] -->|extends| DefaultEntitiesExportImportService_c825["DefaultEntitiesExportImportService"]
    EntitiesExportImportService_p826["EntitiesExportImportService"] -->|implements| DefaultEntitiesExportImportService_c826["DefaultEntitiesExportImportService"]
    Object______p827["Object/外部框架"] -->|extends| DefaultExportableEntitiesService_c827["DefaultExportableEntitiesService"]
    ExportableEntitiesService_p828["ExportableEntitiesService"] -->|implements| DefaultExportableEntitiesService_c828["DefaultExportableEntitiesService"]
    BaseEntityExportService_p829["BaseEntityExportService"] -->|extends| AssetExportService_c829["AssetExportService"]
    BaseEntityExportService_p830["BaseEntityExportService"] -->|extends| AssetProfileExportService_c830["AssetProfileExportService"]
    Object______p831["Object/外部框架"] -->|extends| BaseEntityExportService_c831["BaseEntityExportService"]
    BaseEntityExportService_p832["BaseEntityExportService"] -->|extends| DashboardExportService_c832["DashboardExportService"]
    Object______p833["Object/外部框架"] -->|extends| DefaultEntityExportService_c833["DefaultEntityExportService"]
    BaseEntityExportService_p834["BaseEntityExportService"] -->|extends| DeviceExportService_c834["DeviceExportService"]
    BaseEntityExportService_p835["BaseEntityExportService"] -->|extends| DeviceProfileExportService_c835["DeviceProfileExportService"]
    BaseEntityExportService_p836["BaseEntityExportService"] -->|extends| EntityViewExportService_c836["EntityViewExportService"]
    Object______p837["Object/外部框架"] -->|extends| NotificationRuleExportService_c837["NotificationRuleExportService"]
    BaseEntityExportService_p838["BaseEntityExportService"] -->|extends| NotificationTargetExportService_c838["NotificationTargetExportService"]
    BaseEntityExportService_p839["BaseEntityExportService"] -->|extends| NotificationTemplateExportService_c839["NotificationTemplateExportService"]
    BaseEntityExportService_p840["BaseEntityExportService"] -->|extends| ResourceExportService_c840["ResourceExportService"]
    BaseEntityExportService_p841["BaseEntityExportService"] -->|extends| RuleChainExportService_c841["RuleChainExportService"]
    BaseEntityExportService_p842["BaseEntityExportService"] -->|extends| WidgetTypeExportService_c842["WidgetTypeExportService"]
    BaseEntityExportService_p843["BaseEntityExportService"] -->|extends| WidgetsBundleExportService_c843["WidgetsBundleExportService"]
    Object______p844["Object/外部框架"] -->|extends| AbstractBulkImportService_c844["AbstractBulkImportService"]
    Object______p845["Object/外部框架"] -->|extends| EntityData_c845["EntityData"]
    Object______p846["Object/外部框架"] -->|extends| ParsedValue_c846["ParsedValue"]
    Object______p847["Object/外部框架"] -->|extends| ImportedEntityInfo_c847["ImportedEntityInfo"]
    BaseEntityImportService_p848["BaseEntityImportService"] -->|extends| AssetImportService_c848["AssetImportService"]
    BaseEntityImportService_p849["BaseEntityImportService"] -->|extends| AssetProfileImportService_c849["AssetProfileImportService"]
    Object______p850["Object/外部框架"] -->|extends| BaseEntityImportService_c850["BaseEntityImportService"]
    Object______p851["Object/外部框架"] -->|extends| IdProvider_c851["IdProvider"]
    BaseEntityImportService_p852["BaseEntityImportService"] -->|extends| CustomerImportService_c852["CustomerImportService"]
    BaseEntityImportService_p853["BaseEntityImportService"] -->|extends| DashboardImportService_c853["DashboardImportService"]
    BaseEntityImportService_p854["BaseEntityImportService"] -->|extends| DeviceImportService_c854["DeviceImportService"]
    BaseEntityImportService_p855["BaseEntityImportService"] -->|extends| DeviceProfileImportService_c855["DeviceProfileImportService"]
    BaseEntityImportService_p856["BaseEntityImportService"] -->|extends| EntityViewImportService_c856["EntityViewImportService"]
    RuntimeException_p857["RuntimeException"] -->|extends| ImportServiceException_c857["ImportServiceException"]
    ImportServiceException_p858["ImportServiceException"] -->|extends| MissingEntityException_c858["MissingEntityException"]
    BaseEntityImportService_p859["BaseEntityImportService"] -->|extends| NotificationRuleImportService_c859["NotificationRuleImportService"]
    BaseEntityImportService_p860["BaseEntityImportService"] -->|extends| NotificationTargetImportService_c860["NotificationTargetImportService"]
    BaseEntityImportService_p861["BaseEntityImportService"] -->|extends| NotificationTemplateImportService_c861["NotificationTemplateImportService"]
    BaseEntityImportService_p862["BaseEntityImportService"] -->|extends| ResourceImportService_c862["ResourceImportService"]
    BaseEntityImportService_p863["BaseEntityImportService"] -->|extends| RuleChainImportService_c863["RuleChainImportService"]
    BaseEntityImportService_p864["BaseEntityImportService"] -->|extends| WidgetTypeImportService_c864["WidgetTypeImportService"]
    BaseEntityImportService_p865["BaseEntityImportService"] -->|extends| WidgetsBundleImportService_c865["WidgetsBundleImportService"]
    Object______p866["Object/外部框架"] -->|extends| DefaultEntitiesVersionControlService_c866["DefaultEntitiesVersionControlService"]
    EntitiesVersionControlService_p867["EntitiesVersionControlService"] -->|implements| DefaultEntitiesVersionControlService_c867["DefaultEntitiesVersionControlService"]
    Object______p868["Object/外部框架"] -->|extends| DefaultGitVersionControlQueueService_c868["DefaultGitVersionControlQueueService"]
    GitVersionControlQueueService_p869["GitVersionControlQueueService"] -->|implements| DefaultGitVersionControlQueueService_c869["DefaultGitVersionControlQueueService"]
    RuntimeException_p870["RuntimeException"] -->|extends| LoadEntityException_c870["LoadEntityException"]
    Object______p871["Object/外部框架"] -->|extends| TbAbstractVersionControlSettingsService_c871["TbAbstractVersionControlSettingsService"]
    Object______p872["Object/外部框架"] -->|extends| VersionControlTaskCacheEntry_c872["VersionControlTaskCacheEntry"]
    Serializable_p873["Serializable"] -->|implements| VersionControlTaskCacheEntry_c873["VersionControlTaskCacheEntry"]
    CaffeineTbTransactionalCache_p874["CaffeineTbTransactionalCache"] -->|extends| VersionControlTaskCaffeineCache_c874["VersionControlTaskCaffeineCache"]
    RedisTbTransactionalCache_p875["RedisTbTransactionalCache"] -->|extends| VersionControlTaskRedisCache_c875["VersionControlTaskRedisCache"]
    CaffeineTbTransactionalCache_p876["CaffeineTbTransactionalCache"] -->|extends| AutoCommitSettingsCaffeineCache_c876["AutoCommitSettingsCaffeineCache"]
    RedisTbTransactionalCache_p877["RedisTbTransactionalCache"] -->|extends| AutoCommitSettingsRedisCache_c877["AutoCommitSettingsRedisCache"]
    TbAbstractVersionControlSettingsService_p878["TbAbstractVersionControlSettingsService"] -->|extends| DefaultTbAutoCommitSettingsService_c878["DefaultTbAutoCommitSettingsService"]
    TbAutoCommitSettingsService_p879["TbAutoCommitSettingsService"] -->|implements| DefaultTbAutoCommitSettingsService_c879["DefaultTbAutoCommitSettingsService"]
    VoidGitRequest_p880["VoidGitRequest"] -->|extends| ClearRepositoryGitRequest_c880["ClearRepositoryGitRequest"]
    PendingGitRequest_p881["PendingGitRequest"] -->|extends| CommitGitRequest_c881["CommitGitRequest"]
    EntitiesExportCtx_p882["EntitiesExportCtx"] -->|extends| ComplexEntitiesExportCtx_c882["ComplexEntitiesExportCtx"]
    PendingGitRequest_p883["PendingGitRequest"] -->|extends| ContentsDiffGitRequest_c883["ContentsDiffGitRequest"]
    PendingGitRequest_p884["PendingGitRequest"] -->|extends| EntitiesContentGitRequest_c884["EntitiesContentGitRequest"]
    Object______p885["Object/外部框架"] -->|extends| EntitiesExportCtx_c885["EntitiesExportCtx"]
    Object______p886["Object/外部框架"] -->|extends| EntitiesImportCtx_c886["EntitiesImportCtx"]
    PendingGitRequest_p887["PendingGitRequest"] -->|extends| EntityContentGitRequest_c887["EntityContentGitRequest"]
    EntitiesExportCtx_p888["EntitiesExportCtx"] -->|extends| EntityTypeExportCtx_c888["EntityTypeExportCtx"]
    PendingGitRequest_p889["PendingGitRequest"] -->|extends| ListBranchesGitRequest_c889["ListBranchesGitRequest"]
    PendingGitRequest_p890["PendingGitRequest"] -->|extends| ListEntitiesGitRequest_c890["ListEntitiesGitRequest"]
    PendingGitRequest_p891["PendingGitRequest"] -->|extends| ListVersionsGitRequest_c891["ListVersionsGitRequest"]
    Object______p892["Object/外部框架"] -->|extends| PendingGitRequest_c892["PendingGitRequest"]
    Object______p893["Object/外部框架"] -->|extends| ReimportTask_c893["ReimportTask"]
    EntitiesExportCtx_p894["EntitiesExportCtx"] -->|extends| SimpleEntitiesExportCtx_c894["SimpleEntitiesExportCtx"]
    PendingGitRequest_p895["PendingGitRequest"] -->|extends| VersionsDiffGitRequest_c895["VersionsDiffGitRequest"]
    PendingGitRequest_p896["PendingGitRequest"] -->|extends| VoidGitRequest_c896["VoidGitRequest"]
    TbAbstractVersionControlSettingsService_p897["TbAbstractVersionControlSettingsService"] -->|extends| DefaultTbRepositorySettingsService_c897["DefaultTbRepositorySettingsService"]
    TbRepositorySettingsService_p898["TbRepositorySettingsService"] -->|implements| DefaultTbRepositorySettingsService_c898["DefaultTbRepositorySettingsService"]
    CaffeineTbTransactionalCache_p899["CaffeineTbTransactionalCache"] -->|extends| RepositorySettingsCaffeineCache_c899["RepositorySettingsCaffeineCache"]
    RedisTbTransactionalCache_p900["RedisTbTransactionalCache"] -->|extends| RepositorySettingsRedisCache_c900["RepositorySettingsRedisCache"]
    TbApplicationEventListener_p901["TbApplicationEventListener"] -->|extends| DefaultSystemInfoService_c901["DefaultSystemInfoService"]
    SystemInfoService_p902["SystemInfoService"] -->|implements| DefaultSystemInfoService_c902["DefaultSystemInfoService"]
    TbApplicationEventListener_p903["TbApplicationEventListener"] -->|extends| AbstractSubscriptionService_c903["AbstractSubscriptionService"]
    RuleEngineAlarmService_p904["RuleEngineAlarmService"] -->|extends| AlarmSubscriptionService_c904["AlarmSubscriptionService"]
    ApplicationListener_p905["ApplicationListener"] -->|extends| AlarmSubscriptionService_c905["AlarmSubscriptionService"]
    Object______p906["Object/外部框架"] -->|extends| AttributeData_c906["AttributeData"]
    Comparable_p907["Comparable"] -->|implements| AttributeData_c907["AttributeData"]
    AbstractSubscriptionService_p908["AbstractSubscriptionService"] -->|extends| DefaultAlarmSubscriptionService_c908["DefaultAlarmSubscriptionService"]
    AlarmSubscriptionService_p909["AlarmSubscriptionService"] -->|implements| DefaultAlarmSubscriptionService_c909["DefaultAlarmSubscriptionService"]
    Object______p910["Object/外部框架"] -->|extends| AlarmUpdateCallback_c910["AlarmUpdateCallback"]
    FutureCallback_p911["FutureCallback"] -->|implements| AlarmUpdateCallback_c911["AlarmUpdateCallback"]
    AbstractSubscriptionService_p912["AbstractSubscriptionService"] -->|extends| DefaultTelemetrySubscriptionService_c912["DefaultTelemetrySubscriptionService"]
    TelemetrySubscriptionService_p913["TelemetrySubscriptionService"] -->|implements| DefaultTelemetrySubscriptionService_c913["DefaultTelemetrySubscriptionService"]
    Object______p914["Object/外部框架"] -->|extends| VoidFutureCallback_c914["VoidFutureCallback"]
    FutureCallback_p915["FutureCallback"] -->|implements| VoidFutureCallback_c915["VoidFutureCallback"]
    RuleEngineTelemetryService_p916["RuleEngineTelemetryService"] -->|extends| InternalTelemetryService_c916["InternalTelemetryService"]
    InternalTelemetryService_p917["InternalTelemetryService"] -->|extends| TelemetrySubscriptionService_c917["TelemetrySubscriptionService"]
    ApplicationListener_p918["ApplicationListener"] -->|extends| TelemetrySubscriptionService_c918["TelemetrySubscriptionService"]
    Object______p919["Object/外部框架"] -->|extends| TsData_c919["TsData"]
    Comparable_p920["Comparable"] -->|implements| TsData_c920["TsData"]
    Object______p921["Object/外部框架"] -->|extends| DefaultTbCoreToTransportService_c921["DefaultTbCoreToTransportService"]
    TbCoreToTransportService_p922["TbCoreToTransportService"] -->|implements| DefaultTbCoreToTransportService_c922["DefaultTbCoreToTransportService"]
    Object______p923["Object/外部框架"] -->|extends| QueueCallbackAdaptor_c923["QueueCallbackAdaptor"]
    TbQueueCallback_p924["TbQueueCallback"] -->|implements| QueueCallbackAdaptor_c924["QueueCallbackAdaptor"]
    Object______p925["Object/外部框架"] -->|extends| DefaultTransportApiService_c925["DefaultTransportApiService"]
    TransportApiService_p926["TransportApiService"] -->|implements| DefaultTransportApiService_c926["DefaultTransportApiService"]
    Object______p927["Object/外部框架"] -->|extends| TbCoreTransportApiService_c927["TbCoreTransportApiService"]
    TbQueueHandler_p928["TbQueueHandler"] -->|extends| TransportApiService_c928["TransportApiService"]
    Object______p929["Object/外部框架"] -->|extends| TransportToDeviceActorMsgWrapper_c929["TransportToDeviceActorMsgWrapper"]
    TbActorMsg_p930["TbActorMsg"] -->|implements| TransportToDeviceActorMsgWrapper_c930["TransportToDeviceActorMsgWrapper"]
    DeviceAwareMsg_p931["DeviceAwareMsg"] -->|implements| TransportToDeviceActorMsgWrapper_c931["TransportToDeviceActorMsgWrapper"]
    TenantAwareMsg_p932["TenantAwareMsg"] -->|implements| TransportToDeviceActorMsgWrapper_c932["TransportToDeviceActorMsgWrapper"]
    Serializable_p933["Serializable"] -->|implements| TransportToDeviceActorMsgWrapper_c933["TransportToDeviceActorMsgWrapper"]
    Object______p934["Object/外部框架"] -->|extends| AbstractCleanUpService_c934["AbstractCleanUpService"]
    Object______p935["Object/外部框架"] -->|extends| AlarmsCleanUpService_c935["AlarmsCleanUpService"]
    AbstractCleanUpService_p936["AbstractCleanUpService"] -->|extends| AuditLogsCleanUpService_c936["AuditLogsCleanUpService"]
    AbstractCleanUpService_p937["AbstractCleanUpService"] -->|extends| EdgeEventsCleanUpService_c937["EdgeEventsCleanUpService"]
    AbstractCleanUpService_p938["AbstractCleanUpService"] -->|extends| EventsCleanUpService_c938["EventsCleanUpService"]
    AbstractCleanUpService_p939["AbstractCleanUpService"] -->|extends| NotificationsCleanUpService_c939["NotificationsCleanUpService"]
    AbstractCleanUpService_p940["AbstractCleanUpService"] -->|extends| TimeseriesCleanUpService_c940["TimeseriesCleanUpService"]
    Object______p941["Object/外部框架"] -->|extends| RpcCleanUpService_c941["RpcCleanUpService"]
    Object______p942["Object/外部框架"] -->|extends| DefaultUpdateService_c942["DefaultUpdateService"]
    UpdateService_p943["UpdateService"] -->|implements| DefaultUpdateService_c943["DefaultUpdateService"]
    Object______p944["Object/外部框架"] -->|extends| AuthCmd_c944["AuthCmd"]
    WsCmd_p945["WsCmd"] -->|implements| AuthCmd_c945["AuthCmd"]
    Object______p946["Object/外部框架"] -->|extends| DefaultWebSocketService_c946["DefaultWebSocketService"]
    WebSocketService_p947["WebSocketService"] -->|implements| DefaultWebSocketService_c947["DefaultWebSocketService"]
    Object______p948["Object/外部框架"] -->|extends| WsCmdHandler_c948["WsCmdHandler"]
    Object______p949["Object/外部框架"] -->|extends| SessionEvent_c949["SessionEvent"]
    Object______p950["Object/外部框架"] -->|extends| WebSocketSessionRef_c950["WebSocketSessionRef"]
    Object______p951["Object/外部框架"] -->|extends| WsCommandsWrapper_c951["WsCommandsWrapper"]
    Object______p952["Object/外部框架"] -->|extends| WsSessionMetaData_c952["WsSessionMetaData"]
    Object______p953["Object/外部框架"] -->|extends| DefaultNotificationCommandsHandler_c953["DefaultNotificationCommandsHandler"]
    NotificationCommandsHandler_p954["NotificationCommandsHandler"] -->|implements| DefaultNotificationCommandsHandler_c954["DefaultNotificationCommandsHandler"]
    Object______p955["Object/外部框架"] -->|extends| MarkAllNotificationsAsReadCmd_c955["MarkAllNotificationsAsReadCmd"]
    WsCmd_p956["WsCmd"] -->|implements| MarkAllNotificationsAsReadCmd_c956["MarkAllNotificationsAsReadCmd"]
    Object______p957["Object/外部框架"] -->|extends| MarkNotificationsAsReadCmd_c957["MarkNotificationsAsReadCmd"]
    WsCmd_p958["WsCmd"] -->|implements| MarkNotificationsAsReadCmd_c958["MarkNotificationsAsReadCmd"]
    Object______p959["Object/外部框架"] -->|extends| NotificationCmdsWrapper_c959["NotificationCmdsWrapper"]
    Object______p960["Object/外部框架"] -->|extends| NotificationsCountSubCmd_c960["NotificationsCountSubCmd"]
    WsCmd_p961["WsCmd"] -->|implements| NotificationsCountSubCmd_c961["NotificationsCountSubCmd"]
    Object______p962["Object/外部框架"] -->|extends| NotificationsSubCmd_c962["NotificationsSubCmd"]
    WsCmd_p963["WsCmd"] -->|implements| NotificationsSubCmd_c963["NotificationsSubCmd"]
    Object______p964["Object/外部框架"] -->|extends| NotificationsUnsubCmd_c964["NotificationsUnsubCmd"]
    UnsubscribeCmd_p965["UnsubscribeCmd"] -->|implements| NotificationsUnsubCmd_c965["NotificationsUnsubCmd"]
    WsCmd_p966["WsCmd"] -->|implements| NotificationsUnsubCmd_c966["NotificationsUnsubCmd"]
    CmdUpdate_p967["CmdUpdate"] -->|extends| UnreadNotificationsCountUpdate_c967["UnreadNotificationsCountUpdate"]
    CmdUpdate_p968["CmdUpdate"] -->|extends| UnreadNotificationsUpdate_c968["UnreadNotificationsUpdate"]
    Object______p969["Object/外部框架"] -->|extends| AbstractNotificationSubscription_c969["AbstractNotificationSubscription"]
    Object______p970["Object/外部框架"] -->|extends| NotificationRequestUpdate_c970["NotificationRequestUpdate"]
    Object______p971["Object/外部框架"] -->|extends| NotificationUpdate_c971["NotificationUpdate"]
    AbstractNotificationSubscription_p972["AbstractNotificationSubscription"] -->|extends| NotificationsCountSubscription_c972["NotificationsCountSubscription"]
    AbstractNotificationSubscription_p973["AbstractNotificationSubscription"] -->|extends| NotificationsSubscription_c973["NotificationsSubscription"]
    Object______p974["Object/外部框架"] -->|extends| NotificationsSubscriptionUpdate_c974["NotificationsSubscriptionUpdate"]
    Object______p975["Object/外部框架"] -->|extends| TelemetryWebSocketTextMsg_c975["TelemetryWebSocketTextMsg"]
    Object______p976["Object/外部框架"] -->|extends| TelemetryCmdsWrapper_c976["TelemetryCmdsWrapper"]
    SubscriptionCmd_p977["SubscriptionCmd"] -->|extends| AttributesSubscriptionCmd_c977["AttributesSubscriptionCmd"]
    Object______p978["Object/外部框架"] -->|extends| GetHistoryCmd_c978["GetHistoryCmd"]
    TelemetryPluginCmd_p979["TelemetryPluginCmd"] -->|implements| GetHistoryCmd_c979["GetHistoryCmd"]
    Object______p980["Object/外部框架"] -->|extends| SubscriptionCmd_c980["SubscriptionCmd"]
    TelemetryPluginCmd_p981["TelemetryPluginCmd"] -->|implements| SubscriptionCmd_c981["SubscriptionCmd"]
    WsCmd_p982["WsCmd"] -->|extends| TelemetryPluginCmd_c982["TelemetryPluginCmd"]
    SubscriptionCmd_p983["SubscriptionCmd"] -->|extends| TimeseriesSubscriptionCmd_c983["TimeseriesSubscriptionCmd"]
    Object______p984["Object/外部框架"] -->|extends| AggHistoryCmd_c984["AggHistoryCmd"]
    Object______p985["Object/外部框架"] -->|extends| AggKey_c985["AggKey"]
    Object______p986["Object/外部框架"] -->|extends| AggTimeSeriesCmd_c986["AggTimeSeriesCmd"]
    DataCmd_p987["DataCmd"] -->|extends| AlarmCountCmd_c987["AlarmCountCmd"]
    Object______p988["Object/外部框架"] -->|extends| AlarmCountUnsubscribeCmd_c988["AlarmCountUnsubscribeCmd"]
    UnsubscribeCmd_p989["UnsubscribeCmd"] -->|implements| AlarmCountUnsubscribeCmd_c989["AlarmCountUnsubscribeCmd"]
    CmdUpdate_p990["CmdUpdate"] -->|extends| AlarmCountUpdate_c990["AlarmCountUpdate"]
    DataCmd_p991["DataCmd"] -->|extends| AlarmDataCmd_c991["AlarmDataCmd"]
    Object______p992["Object/外部框架"] -->|extends| AlarmDataUnsubscribeCmd_c992["AlarmDataUnsubscribeCmd"]
    UnsubscribeCmd_p993["UnsubscribeCmd"] -->|implements| AlarmDataUnsubscribeCmd_c993["AlarmDataUnsubscribeCmd"]
    DataUpdate_p994["DataUpdate"] -->|extends| AlarmDataUpdate_c994["AlarmDataUpdate"]
    Object______p995["Object/外部框架"] -->|extends| CmdUpdate_c995["CmdUpdate"]
    Object______p996["Object/外部框架"] -->|extends| DataCmd_c996["DataCmd"]
    WsCmd_p997["WsCmd"] -->|implements| DataCmd_c997["DataCmd"]
    CmdUpdate_p998["CmdUpdate"] -->|extends| DataUpdate_c998["DataUpdate"]
    DataCmd_p999["DataCmd"] -->|extends| EntityCountCmd_c999["EntityCountCmd"]
    Object______p1000["Object/外部框架"] -->|extends| EntityCountUnsubscribeCmd_c1000["EntityCountUnsubscribeCmd"]
    UnsubscribeCmd_p1001["UnsubscribeCmd"] -->|implements| EntityCountUnsubscribeCmd_c1001["EntityCountUnsubscribeCmd"]
    CmdUpdate_p1002["CmdUpdate"] -->|extends| EntityCountUpdate_c1002["EntityCountUpdate"]
    DataCmd_p1003["DataCmd"] -->|extends| EntityDataCmd_c1003["EntityDataCmd"]
    Object______p1004["Object/外部框架"] -->|extends| EntityDataUnsubscribeCmd_c1004["EntityDataUnsubscribeCmd"]
    UnsubscribeCmd_p1005["UnsubscribeCmd"] -->|implements| EntityDataUnsubscribeCmd_c1005["EntityDataUnsubscribeCmd"]
    DataUpdate_p1006["DataUpdate"] -->|extends| EntityDataUpdate_c1006["EntityDataUpdate"]
    Object______p1007["Object/外部框架"] -->|extends| EntityHistoryCmd_c1007["EntityHistoryCmd"]
    GetTsCmd_p1008["GetTsCmd"] -->|implements| EntityHistoryCmd_c1008["EntityHistoryCmd"]
    Object______p1009["Object/外部框架"] -->|extends| LatestValueCmd_c1009["LatestValueCmd"]
    Object______p1010["Object/外部框架"] -->|extends| TimeSeriesCmd_c1010["TimeSeriesCmd"]
    GetTsCmd_p1011["GetTsCmd"] -->|implements| TimeSeriesCmd_c1011["TimeSeriesCmd"]
    WsCmd_p1012["WsCmd"] -->|extends| UnsubscribeCmd_c1012["UnsubscribeCmd"]
    Object______p1013["Object/外部框架"] -->|extends| AlarmSubscriptionUpdate_c1013["AlarmSubscriptionUpdate"]
    Object______p1014["Object/外部框架"] -->|extends| SubscriptionState_c1014["SubscriptionState"]
    Object______p1015["Object/外部框架"] -->|extends| TelemetrySubscriptionUpdate_c1015["TelemetrySubscriptionUpdate"]
    Object______p1016["Object/外部框架"] -->|extends| SpringfoxHandlerProviderBeanPostProcessor_c1016["SpringfoxHandlerProviderBeanPostProcessor"]
    BeanPostProcessor_p1017["BeanPostProcessor"] -->|implements| SpringfoxHandlerProviderBeanPostProcessor_c1017["SpringfoxHandlerProviderBeanPostProcessor"]
    Object______p1018["Object/外部框架"] -->|extends| CsvUtils_c1018["CsvUtils"]
    Object______p1019["Object/外部框架"] -->|extends| LwM2mObjectModelUtils_c1019["LwM2mObjectModelUtils"]
    Object______p1020["Object/外部框架"] -->|extends| MiscUtils_c1020["MiscUtils"]
    Object______p1021["Object/外部框架"] -->|extends| TbNodeUpgradeUtils_c1021["TbNodeUpgradeUtils"]
    Object______p1022["Object/外部框架"] -->|extends| DeviceActorMessageProcessorTest_c1022["DeviceActorMessageProcessorTest"]
    Object______p1023["Object/外部框架"] -->|extends| StatsActorTest_c1023["StatsActorTest"]
    Object______p1024["Object/外部框架"] -->|extends| StatsPersistMsgTest_c1024["StatsPersistMsgTest"]
    Object______p1025["Object/外部框架"] -->|extends| TenantActorTest_c1025["TenantActorTest"]
    Object______p1026["Object/外部框架"] -->|extends| CaffeineCacheDefaultConfigurationTest_c1026["CaffeineCacheDefaultConfigurationTest"]
    AbstractNotifyEntityTest_p1027["AbstractNotifyEntityTest"] -->|extends| AbstractControllerTest_c1027["AbstractControllerTest"]
    Object______p1028["Object/外部框架"] -->|extends| AbstractInMemoryStorageTest_c1028["AbstractInMemoryStorageTest"]
    AbstractWebTest_p1029["AbstractWebTest"] -->|extends| AbstractNotifyEntityTest_c1029["AbstractNotifyEntityTest"]
    AbstractControllerTest_p1030["AbstractControllerTest"] -->|extends| AbstractRuleEngineControllerTest_c1030["AbstractRuleEngineControllerTest"]
    AbstractInMemoryStorageTest_p1031["AbstractInMemoryStorageTest"] -->|extends| AbstractWebTest_c1031["AbstractWebTest"]
    Object______p1032["Object/外部框架"] -->|extends| IdComparator_c1032["IdComparator"]
    Object______p1033["Object/外部框架"] -->|extends| EntityIdComparator_c1033["EntityIdComparator"]
    AbstractControllerTest_p1034["AbstractControllerTest"] -->|extends| AdminControllerTest_c1034["AdminControllerTest"]
    AbstractControllerTest_p1035["AbstractControllerTest"] -->|extends| AlarmCommentControllerTest_c1035["AlarmCommentControllerTest"]
    Object______p1036["Object/外部框架"] -->|extends| Config_c1036["Config"]
    AbstractControllerTest_p1037["AbstractControllerTest"] -->|extends| AlarmControllerTest_c1037["AlarmControllerTest"]
    AbstractControllerTest_p1038["AbstractControllerTest"] -->|extends| AssetControllerTest_c1038["AssetControllerTest"]
    AbstractControllerTest_p1039["AbstractControllerTest"] -->|extends| AssetProfileControllerTest_c1039["AssetProfileControllerTest"]
    AbstractControllerTest_p1040["AbstractControllerTest"] -->|extends| AuditLogControllerTest_c1040["AuditLogControllerTest"]
    AbstractControllerTest_p1041["AbstractControllerTest"] -->|extends| AuthControllerTest_c1041["AuthControllerTest"]
    AbstractControllerTest_p1042["AbstractControllerTest"] -->|extends| BaseQueueControllerTest_c1042["BaseQueueControllerTest"]
    AbstractControllerTest_p1043["AbstractControllerTest"] -->|extends| ComponentDescriptorControllerTest_c1043["ComponentDescriptorControllerTest"]
    AbstractControllerTest_p1044["AbstractControllerTest"] -->|extends| CustomerControllerTest_c1044["CustomerControllerTest"]
    AbstractControllerTest_p1045["AbstractControllerTest"] -->|extends| DashboardControllerTest_c1045["DashboardControllerTest"]
    AbstractControllerTest_p1046["AbstractControllerTest"] -->|extends| DeviceConnectivityControllerTest_c1046["DeviceConnectivityControllerTest"]
    AbstractControllerTest_p1047["AbstractControllerTest"] -->|extends| DeviceControllerTest_c1047["DeviceControllerTest"]
    AbstractControllerTest_p1048["AbstractControllerTest"] -->|extends| DeviceProfileControllerTest_c1048["DeviceProfileControllerTest"]
    AbstractControllerTest_p1049["AbstractControllerTest"] -->|extends| EdgeControllerTest_c1049["EdgeControllerTest"]
    AbstractControllerTest_p1050["AbstractControllerTest"] -->|extends| EdgeEventControllerTest_c1050["EdgeEventControllerTest"]
    AbstractControllerTest_p1051["AbstractControllerTest"] -->|extends| EntityQueryControllerTest_c1051["EntityQueryControllerTest"]
    AbstractControllerTest_p1052["AbstractControllerTest"] -->|extends| EntityRelationControllerTest_c1052["EntityRelationControllerTest"]
    AbstractControllerTest_p1053["AbstractControllerTest"] -->|extends| EntityViewControllerTest_c1053["EntityViewControllerTest"]
    AbstractControllerTest_p1054["AbstractControllerTest"] -->|extends| HomePageApiTest_c1054["HomePageApiTest"]
    AbstractControllerTest_p1055["AbstractControllerTest"] -->|extends| ImageControllerTest_c1055["ImageControllerTest"]
    AbstractControllerTest_p1056["AbstractControllerTest"] -->|extends| OtaPackageControllerTest_c1056["OtaPackageControllerTest"]
    AbstractControllerTest_p1057["AbstractControllerTest"] -->|extends| RpcControllerTest_c1057["RpcControllerTest"]
    AbstractControllerTest_p1058["AbstractControllerTest"] -->|extends| RuleChainControllerTest_c1058["RuleChainControllerTest"]
    AbstractControllerTest_p1059["AbstractControllerTest"] -->|extends| TbResourceControllerTest_c1059["TbResourceControllerTest"]
    WebSocketClient_p1060["WebSocketClient"] -->|extends| TbTestWebSocketClient_c1060["TbTestWebSocketClient"]
    AbstractControllerTest_p1061["AbstractControllerTest"] -->|extends| TelemetryControllerTest_c1061["TelemetryControllerTest"]
    AbstractControllerTest_p1062["AbstractControllerTest"] -->|extends| TenantControllerTest_c1062["TenantControllerTest"]
    AbstractControllerTest_p1063["AbstractControllerTest"] -->|extends| TenantProfileControllerTest_c1063["TenantProfileControllerTest"]
    AbstractControllerTest_p1064["AbstractControllerTest"] -->|extends| TwoFactorAuthConfigTest_c1064["TwoFactorAuthConfigTest"]
    AbstractControllerTest_p1065["AbstractControllerTest"] -->|extends| TwoFactorAuthTest_c1065["TwoFactorAuthTest"]
    AbstractControllerTest_p1066["AbstractControllerTest"] -->|extends| UserControllerTest_c1066["UserControllerTest"]
    AbstractControllerTest_p1067["AbstractControllerTest"] -->|extends| WebsocketApiTest_c1067["WebsocketApiTest"]
    AbstractControllerTest_p1068["AbstractControllerTest"] -->|extends| WidgetTypeControllerTest_c1068["WidgetTypeControllerTest"]
    AbstractControllerTest_p1069["AbstractControllerTest"] -->|extends| WidgetsBundleControllerTest_c1069["WidgetsBundleControllerTest"]
    Object______p1070["Object/外部框架"] -->|extends| TbWebSocketHandlerTest_c1070["TbWebSocketHandlerTest"]
    AbstractControllerTest_p1071["AbstractControllerTest"] -->|extends| AbstractEdgeTest_c1071["AbstractEdgeTest"]
    AbstractEdgeTest_p1072["AbstractEdgeTest"] -->|extends| AlarmEdgeTest_c1072["AlarmEdgeTest"]
    AbstractEdgeTest_p1073["AbstractEdgeTest"] -->|extends| AssetEdgeTest_c1073["AssetEdgeTest"]
    AbstractEdgeTest_p1074["AbstractEdgeTest"] -->|extends| AssetProfileEdgeTest_c1074["AssetProfileEdgeTest"]
    AbstractEdgeTest_p1075["AbstractEdgeTest"] -->|extends| CustomerEdgeTest_c1075["CustomerEdgeTest"]
    AbstractEdgeTest_p1076["AbstractEdgeTest"] -->|extends| DashboardEdgeTest_c1076["DashboardEdgeTest"]
    AbstractEdgeTest_p1077["AbstractEdgeTest"] -->|extends| DeviceEdgeTest_c1077["DeviceEdgeTest"]
    AbstractEdgeTest_p1078["AbstractEdgeTest"] -->|extends| DeviceProfileEdgeTest_c1078["DeviceProfileEdgeTest"]
    AbstractEdgeTest_p1079["AbstractEdgeTest"] -->|extends| EdgeTest_c1079["EdgeTest"]
    AbstractEdgeTest_p1080["AbstractEdgeTest"] -->|extends| EntityViewEdgeTest_c1080["EntityViewEdgeTest"]
    AbstractEdgeTest_p1081["AbstractEdgeTest"] -->|extends| OtaPackageEdgeTest_c1081["OtaPackageEdgeTest"]
    AbstractEdgeTest_p1082["AbstractEdgeTest"] -->|extends| QueueEdgeTest_c1082["QueueEdgeTest"]
    AbstractEdgeTest_p1083["AbstractEdgeTest"] -->|extends| RelationEdgeTest_c1083["RelationEdgeTest"]
    AbstractEdgeTest_p1084["AbstractEdgeTest"] -->|extends| ResourceEdgeTest_c1084["ResourceEdgeTest"]
    AbstractEdgeTest_p1085["AbstractEdgeTest"] -->|extends| RuleChainEdgeTest_c1085["RuleChainEdgeTest"]
    AbstractEdgeTest_p1086["AbstractEdgeTest"] -->|extends| TelemetryEdgeTest_c1086["TelemetryEdgeTest"]
    AbstractEdgeTest_p1087["AbstractEdgeTest"] -->|extends| TenantEdgeTest_c1087["TenantEdgeTest"]
    AbstractEdgeTest_p1088["AbstractEdgeTest"] -->|extends| TenantProfileEdgeTest_c1088["TenantProfileEdgeTest"]
    AbstractEdgeTest_p1089["AbstractEdgeTest"] -->|extends| UserEdgeTest_c1089["UserEdgeTest"]
    AbstractEdgeTest_p1090["AbstractEdgeTest"] -->|extends| WidgetEdgeTest_c1090["WidgetEdgeTest"]
    Object______p1091["Object/外部框架"] -->|extends| EdgeImitator_c1091["EdgeImitator"]
    Object______p1092["Object/外部框架"] -->|extends| HashPartitionServiceTest_c1092["HashPartitionServiceTest"]
    AbstractRuleEngineControllerTest_p1093["AbstractRuleEngineControllerTest"] -->|extends| AbstractRuleEngineFlowIntegrationTest_c1093["AbstractRuleEngineFlowIntegrationTest"]
    AbstractRuleEngineFlowIntegrationTest_p1094["AbstractRuleEngineFlowIntegrationTest"] -->|extends| RuleEngineFlowSqlIntegrationTest_c1094["RuleEngineFlowSqlIntegrationTest"]
    AbstractRuleEngineControllerTest_p1095["AbstractRuleEngineControllerTest"] -->|extends| AbstractRuleEngineLifecycleIntegrationTest_c1095["AbstractRuleEngineLifecycleIntegrationTest"]
    AbstractRuleEngineLifecycleIntegrationTest_p1096["AbstractRuleEngineLifecycleIntegrationTest"] -->|extends| RuleEngineLifecycleSqlIntegrationTest_c1096["RuleEngineLifecycleSqlIntegrationTest"]
    Object______p1097["Object/外部框架"] -->|extends| DefaultTbApiUsageStateServiceTest_c1097["DefaultTbApiUsageStateServiceTest"]
    Object______p1098["Object/外部框架"] -->|extends| DeviceProvisionServiceTest_c1098["DeviceProvisionServiceTest"]
    Object______p1099["Object/外部框架"] -->|extends| RuleChainMsgConstructorTest_c1099["RuleChainMsgConstructorTest"]
    Object______p1100["Object/外部框架"] -->|extends| BaseEdgeProcessorTest_c1100["BaseEdgeProcessorTest"]
    BaseEdgeProcessorTest_p1101["BaseEdgeProcessorTest"] -->|extends| AbstractAssetProcessorTest_c1101["AbstractAssetProcessorTest"]
    AbstractAssetProcessorTest_p1102["AbstractAssetProcessorTest"] -->|extends| AssetEdgeProcessorTest_c1102["AssetEdgeProcessorTest"]
    AbstractAssetProcessorTest_p1103["AbstractAssetProcessorTest"] -->|extends| AssetProfileEdgeProcessorTest_c1103["AssetProfileEdgeProcessorTest"]
    BaseEdgeProcessorTest_p1104["BaseEdgeProcessorTest"] -->|extends| AbstractDeviceProcessorTest_c1104["AbstractDeviceProcessorTest"]
    AbstractDeviceProcessorTest_p1105["AbstractDeviceProcessorTest"] -->|extends| DeviceEdgeProcessorTest_c1105["DeviceEdgeProcessorTest"]
    AbstractDeviceProcessorTest_p1106["AbstractDeviceProcessorTest"] -->|extends| DeviceProfileEdgeProcessorTest_c1106["DeviceProfileEdgeProcessorTest"]
    Object______p1107["Object/外部框架"] -->|extends| TelemetryEdgeProcessorTest_c1107["TelemetryEdgeProcessorTest"]
    Object______p1108["Object/外部框架"] -->|extends| DefaultTbAlarmServiceTest_c1108["DefaultTbAlarmServiceTest"]
    Object______p1109["Object/外部框架"] -->|extends| DefaultTbAlarmCommentServiceTest_c1109["DefaultTbAlarmCommentServiceTest"]
    Object______p1110["Object/外部框架"] -->|extends| InstallScriptsTest_c1110["InstallScriptsTest"]
    Object______p1111["Object/外部框架"] -->|extends| SqlEntityDatabaseSchemaServiceTest_c1111["SqlEntityDatabaseSchemaServiceTest"]
    Object______p1112["Object/外部框架"] -->|extends| DefaultDataUpdateServiceTest_c1112["DefaultDataUpdateServiceTest"]
    Object______p1113["Object/外部框架"] -->|extends| RateLimitServiceTest_c1113["RateLimitServiceTest"]
    Object______p1114["Object/外部框架"] -->|extends| TbMailSenderTest_c1114["TbMailSenderTest"]
    AbstractControllerTest_p1115["AbstractControllerTest"] -->|extends| AbstractNotificationApiTest_c1115["AbstractNotificationApiTest"]
    AbstractNotificationApiTest_p1116["AbstractNotificationApiTest"] -->|extends| NotificationApiTest_c1116["NotificationApiTest"]
    TbTestWebSocketClient_p1117["TbTestWebSocketClient"] -->|extends| NotificationApiWsClient_c1117["NotificationApiWsClient"]
    AbstractNotificationApiTest_p1118["AbstractNotificationApiTest"] -->|extends| NotificationRuleApiTest_c1118["NotificationRuleApiTest"]
    AbstractNotificationApiTest_p1119["AbstractNotificationApiTest"] -->|extends| NotificationTargetApiTest_c1119["NotificationTargetApiTest"]
    AbstractNotificationApiTest_p1120["AbstractNotificationApiTest"] -->|extends| NotificationTemplateApiTest_c1120["NotificationTemplateApiTest"]
    DefaultNotificationSettingsService_p1121["DefaultNotificationSettingsService"] -->|extends| TestNotificationSettingsService_c1121["TestNotificationSettingsService"]
    Object______p1122["Object/外部框架"] -->|extends| DefaultTbClusterServiceTest_c1122["DefaultTbClusterServiceTest"]
    Object______p1123["Object/外部框架"] -->|extends| DefaultTbCoreConsumerServiceTest_c1123["DefaultTbCoreConsumerServiceTest"]
    Object______p1124["Object/外部框架"] -->|extends| TbMsgPackCallbackTest_c1124["TbMsgPackCallbackTest"]
    Object______p1125["Object/外部框架"] -->|extends| TbMsgPackProcessingContextTest_c1125["TbMsgPackProcessingContextTest"]
    Object______p1126["Object/外部框架"] -->|extends| TbRuleEngineQueueConsumerManagerTest_c1126["TbRuleEngineQueueConsumerManagerTest"]
    AbstractTbQueueConsumerTemplate_p1127["AbstractTbQueueConsumerTemplate"] -->|extends| TestConsumer_c1127["TestConsumer"]
    AbstractControllerTest_p1128["AbstractControllerTest"] -->|extends| BaseTbResourceServiceTest_c1128["BaseTbResourceServiceTest"]
    Object______p1129["Object/外部框架"] -->|extends| RpcSubmitStrategyTest_c1129["RpcSubmitStrategyTest"]
    Object______p1130["Object/外部框架"] -->|extends| MockJsInvokeService_c1130["MockJsInvokeService"]
    JsInvokeService_p1131["JsInvokeService"] -->|implements| MockJsInvokeService_c1131["MockJsInvokeService"]
    AbstractControllerTest_p1132["AbstractControllerTest"] -->|extends| NashornJsInvokeServiceTest_c1132["NashornJsInvokeServiceTest"]
    Object______p1133["Object/外部框架"] -->|extends| RemoteJsInvokeServiceTest_c1133["RemoteJsInvokeServiceTest"]
    AbstractControllerTest_p1134["AbstractControllerTest"] -->|extends| TbelInvokeServiceTest_c1134["TbelInvokeServiceTest"]
    Object______p1135["Object/外部框架"] -->|extends| JwtTokenFactoryTest_c1135["JwtTokenFactoryTest"]
    Object______p1136["Object/外部框架"] -->|extends| TokenOutdatingTest_c1136["TokenOutdatingTest"]
    Object______p1137["Object/外部框架"] -->|extends| CookieUtilsTest_c1137["CookieUtilsTest"]
    AbstractControllerTest_p1138["AbstractControllerTest"] -->|extends| Oauth2AuthenticationSuccessHandlerTest_c1138["Oauth2AuthenticationSuccessHandlerTest"]
    AbstractControllerTest_p1139["AbstractControllerTest"] -->|extends| DefaultSmsServiceTest_c1139["DefaultSmsServiceTest"]
    Object______p1140["Object/外部框架"] -->|extends| SmppSmsSenderTest_c1140["SmppSmsSenderTest"]
    AbstractControllerTest_p1141["AbstractControllerTest"] -->|extends| SequentialTimeseriesPersistenceTest_c1141["SequentialTimeseriesPersistenceTest"]
    Object______p1142["Object/外部框架"] -->|extends| DefaultDeviceStateServiceTest_c1142["DefaultDeviceStateServiceTest"]
    Object______p1143["Object/外部框架"] -->|extends| DefaultRuleEngineDeviceStateManagerTest_c1143["DefaultRuleEngineDeviceStateManagerTest"]
    AbstractControllerTest_p1144["AbstractControllerTest"] -->|extends| DevicesStatisticsTest_c1144["DevicesStatisticsTest"]
    AbstractControllerTest_p1145["AbstractControllerTest"] -->|extends| BaseExportImportServiceTest_c1145["BaseExportImportServiceTest"]
    BaseExportImportServiceTest_p1146["BaseExportImportServiceTest"] -->|extends| ExportImportServiceSqlTest_c1146["ExportImportServiceSqlTest"]
    Object______p1147["Object/外部框架"] -->|extends| DefaultTransportApiServiceTest_c1147["DefaultTransportApiServiceTest"]
    AbstractControllerTest_p1148["AbstractControllerTest"] -->|extends| AlarmsCleanUpServiceTest_c1148["AlarmsCleanUpServiceTest"]
    Object______p1149["Object/外部框架"] -->|extends| EventsCleanUpServiceTest_c1149["EventsCleanUpServiceTest"]
    AbstractControllerTest_p1150["AbstractControllerTest"] -->|extends| BaseHttpDeviceApiTest_c1150["BaseHttpDeviceApiTest"]
    AbstractControllerTest_p1151["AbstractControllerTest"] -->|extends| BaseRestApiLimitsTest_c1151["BaseRestApiLimitsTest"]
    Object______p1152["Object/外部框架"] -->|extends| RestTemplateConvertersTest_c1152["RestTemplateConvertersTest"]
    BaseHttpDeviceApiTest_p1153["BaseHttpDeviceApiTest"] -->|extends| DeviceApiSqlTest_c1153["DeviceApiSqlTest"]
    BaseRestApiLimitsTest_p1154["BaseRestApiLimitsTest"] -->|extends| RestApiLimitsSqlTest_c1154["RestApiLimitsSqlTest"]
    AbstractControllerTest_p1155["AbstractControllerTest"] -->|extends| AbstractTransportIntegrationTest_c1155["AbstractTransportIntegrationTest"]
    AbstractNoSqlContainer_p1156["AbstractNoSqlContainer"] -->|extends| TransportNoSqlTestSuite_c1156["TransportNoSqlTestSuite"]
    AbstractTransportIntegrationTest_p1157["AbstractTransportIntegrationTest"] -->|extends| AbstractCoapIntegrationTest_c1157["AbstractCoapIntegrationTest"]
    Object______p1158["Object/外部框架"] -->|extends| CoapTestCallback_c1158["CoapTestCallback"]
    CoapHandler_p1159["CoapHandler"] -->|implements| CoapTestCallback_c1159["CoapTestCallback"]
    Object______p1160["Object/外部框架"] -->|extends| CoapTestClient_c1160["CoapTestClient"]
    Object______p1161["Object/外部框架"] -->|extends| CoapTestConfigProperties_c1161["CoapTestConfigProperties"]
    AbstractCoapIntegrationTest_p1162["AbstractCoapIntegrationTest"] -->|extends| AbstractCoapAttributesIntegrationTest_c1162["AbstractCoapAttributesIntegrationTest"]
    AbstractCoapAttributesIntegrationTest_p1163["AbstractCoapAttributesIntegrationTest"] -->|extends| CoapAttributesRequestIntegrationTest_c1163["CoapAttributesRequestIntegrationTest"]
    CoapAttributesRequestIntegrationTest_p1164["CoapAttributesRequestIntegrationTest"] -->|extends| CoapAttributesRequestJsonIntegrationTest_c1164["CoapAttributesRequestJsonIntegrationTest"]
    CoapAttributesRequestIntegrationTest_p1165["CoapAttributesRequestIntegrationTest"] -->|extends| CoapAttributesRequestProtoIntegrationTest_c1165["CoapAttributesRequestProtoIntegrationTest"]
    AbstractCoapAttributesIntegrationTest_p1166["AbstractCoapAttributesIntegrationTest"] -->|extends| CoapAttributesUpdatesIntegrationTest_c1166["CoapAttributesUpdatesIntegrationTest"]
    AbstractCoapAttributesIntegrationTest_p1167["AbstractCoapAttributesIntegrationTest"] -->|extends| CoapAttributesUpdatesJsonIntegrationTest_c1167["CoapAttributesUpdatesJsonIntegrationTest"]
    AbstractCoapAttributesIntegrationTest_p1168["AbstractCoapAttributesIntegrationTest"] -->|extends| CoapAttributesUpdatesProtoIntegrationTest_c1168["CoapAttributesUpdatesProtoIntegrationTest"]
    AbstractCoapIntegrationTest_p1169["AbstractCoapIntegrationTest"] -->|extends| CoapClaimDeviceTest_c1169["CoapClaimDeviceTest"]
    CoapClaimDeviceTest_p1170["CoapClaimDeviceTest"] -->|extends| CoapClaimJsonDeviceTest_c1170["CoapClaimJsonDeviceTest"]
    CoapClaimDeviceTest_p1171["CoapClaimDeviceTest"] -->|extends| CoapClaimProtoDeviceTest_c1171["CoapClaimProtoDeviceTest"]
    AbstractCoapIntegrationTest_p1172["AbstractCoapIntegrationTest"] -->|extends| CoapClientIntegrationTest_c1172["CoapClientIntegrationTest"]
    CoapTestCallback_p1173["CoapTestCallback"] -->|extends| TestCoapCallbackForRPC_c1173["TestCoapCallbackForRPC"]
    AbstractCoapIntegrationTest_p1174["AbstractCoapIntegrationTest"] -->|extends| CoapProvisionJsonDeviceTest_c1174["CoapProvisionJsonDeviceTest"]
    AbstractCoapIntegrationTest_p1175["AbstractCoapIntegrationTest"] -->|extends| CoapProvisionProtoDeviceTest_c1175["CoapProvisionProtoDeviceTest"]
    AbstractCoapIntegrationTest_p1176["AbstractCoapIntegrationTest"] -->|extends| AbstractCoapServerSideRpcIntegrationTest_c1176["AbstractCoapServerSideRpcIntegrationTest"]
    AbstractCoapServerSideRpcIntegrationTest_p1177["AbstractCoapServerSideRpcIntegrationTest"] -->|extends| CoapServerSideRpcDefaultIntegrationTest_c1177["CoapServerSideRpcDefaultIntegrationTest"]
    AbstractCoapServerSideRpcIntegrationTest_p1178["AbstractCoapServerSideRpcIntegrationTest"] -->|extends| CoapServerSideRpcJsonIntegrationTest_c1178["CoapServerSideRpcJsonIntegrationTest"]
    AbstractCoapServerSideRpcIntegrationTest_p1179["AbstractCoapServerSideRpcIntegrationTest"] -->|extends| CoapServerSideRpcProtoIntegrationTest_c1179["CoapServerSideRpcProtoIntegrationTest"]
    AbstractCoapIntegrationTest_p1180["AbstractCoapIntegrationTest"] -->|extends| CoapAttributesIntegrationTest_c1180["CoapAttributesIntegrationTest"]
    CoapAttributesIntegrationTest_p1181["CoapAttributesIntegrationTest"] -->|extends| CoapAttributesJsonIntegrationTest_c1181["CoapAttributesJsonIntegrationTest"]
    CoapAttributesIntegrationTest_p1182["CoapAttributesIntegrationTest"] -->|extends| CoapAttributesProtoIntegrationTest_c1182["CoapAttributesProtoIntegrationTest"]
    AbstractCoapIntegrationTest_p1183["AbstractCoapIntegrationTest"] -->|extends| AbstractCoapTimeseriesIntegrationTest_c1183["AbstractCoapTimeseriesIntegrationTest"]
    AbstractCoapTimeseriesIntegrationTest_p1184["AbstractCoapTimeseriesIntegrationTest"] -->|extends| AbstractCoapTimeseriesJsonIntegrationTest_c1184["AbstractCoapTimeseriesJsonIntegrationTest"]
    AbstractCoapTimeseriesIntegrationTest_p1185["AbstractCoapTimeseriesIntegrationTest"] -->|extends| AbstractCoapTimeseriesProtoIntegrationTest_c1185["AbstractCoapTimeseriesProtoIntegrationTest"]
    AbstractCoapTimeseriesIntegrationTest_p1186["AbstractCoapTimeseriesIntegrationTest"] -->|extends| CoapTimeseriesNoSqlIntegrationTest_c1186["CoapTimeseriesNoSqlIntegrationTest"]
    AbstractCoapTimeseriesJsonIntegrationTest_p1187["AbstractCoapTimeseriesJsonIntegrationTest"] -->|extends| CoapTimeseriesNoSqlJsonIntegrationTest_c1187["CoapTimeseriesNoSqlJsonIntegrationTest"]
    AbstractCoapTimeseriesProtoIntegrationTest_p1188["AbstractCoapTimeseriesProtoIntegrationTest"] -->|extends| CoapTimeseriesNoSqlProtoIntegrationTest_c1188["CoapTimeseriesNoSqlProtoIntegrationTest"]
    AbstractCoapTimeseriesIntegrationTest_p1189["AbstractCoapTimeseriesIntegrationTest"] -->|extends| CoapTimeseriesSqlIntegrationTest_c1189["CoapTimeseriesSqlIntegrationTest"]
    AbstractCoapTimeseriesJsonIntegrationTest_p1190["AbstractCoapTimeseriesJsonIntegrationTest"] -->|extends| CoapTimeseriesSqlJsonIntegrationTest_c1190["CoapTimeseriesSqlJsonIntegrationTest"]
    AbstractCoapTimeseriesProtoIntegrationTest_p1191["AbstractCoapTimeseriesProtoIntegrationTest"] -->|extends| CoapTimeseriesSqlProtoIntegrationTest_c1191["CoapTimeseriesSqlProtoIntegrationTest"]
    AbstractTransportIntegrationTest_p1192["AbstractTransportIntegrationTest"] -->|extends| AbstractLwM2MIntegrationTest_c1192["AbstractLwM2MIntegrationTest"]
    Object______p1193["Object/外部框架"] -->|extends| Lwm2mTestHelper_c1193["Lwm2mTestHelper"]
    BaseInstanceEnabler_p1194["BaseInstanceEnabler"] -->|extends| FwLwM2MDevice_c1194["FwLwM2MDevice"]
    Destroyable_p1195["Destroyable"] -->|implements| FwLwM2MDevice_c1195["FwLwM2MDevice"]
    Object______p1196["Object/外部框架"] -->|extends| LwM2MLocationParams_c1196["LwM2MLocationParams"]
    Object______p1197["Object/外部框架"] -->|extends| LwM2MTestClient_c1197["LwM2MTestClient"]
    BaseInstanceEnabler_p1198["BaseInstanceEnabler"] -->|extends| LwM2mBinaryAppDataContainer_c1198["LwM2mBinaryAppDataContainer"]
    Destroyable_p1199["Destroyable"] -->|implements| LwM2mBinaryAppDataContainer_c1199["LwM2mBinaryAppDataContainer"]
    BaseInstanceEnabler_p1200["BaseInstanceEnabler"] -->|extends| LwM2mLocation_c1200["LwM2mLocation"]
    Destroyable_p1201["Destroyable"] -->|implements| LwM2mLocation_c1201["LwM2mLocation"]
    BaseInstanceEnabler_p1202["BaseInstanceEnabler"] -->|extends| LwM2mTemperatureSensor_c1202["LwM2mTemperatureSensor"]
    Destroyable_p1203["Destroyable"] -->|implements| LwM2mTemperatureSensor_c1203["LwM2mTemperatureSensor"]
    BaseInstanceEnabler_p1204["BaseInstanceEnabler"] -->|extends| Lwm2mServer_c1204["Lwm2mServer"]
    BaseInstanceEnabler_p1205["BaseInstanceEnabler"] -->|extends| SimpleLwM2MDevice_c1205["SimpleLwM2MDevice"]
    Destroyable_p1206["Destroyable"] -->|implements| SimpleLwM2MDevice_c1206["SimpleLwM2MDevice"]
    BaseInstanceEnabler_p1207["BaseInstanceEnabler"] -->|extends| SwLwM2MDevice_c1207["SwLwM2MDevice"]
    Destroyable_p1208["Destroyable"] -->|implements| SwLwM2MDevice_c1208["SwLwM2MDevice"]
    AbstractLwM2MIntegrationTest_p1209["AbstractLwM2MIntegrationTest"] -->|extends| AbstractOtaLwM2MIntegrationTest_c1209["AbstractOtaLwM2MIntegrationTest"]
    AbstractOtaLwM2MIntegrationTest_p1210["AbstractOtaLwM2MIntegrationTest"] -->|extends| OtaLwM2MIntegrationTest_c1210["OtaLwM2MIntegrationTest"]
    AbstractLwM2MIntegrationTest_p1211["AbstractLwM2MIntegrationTest"] -->|extends| AbstractRpcLwM2MIntegrationTest_c1211["AbstractRpcLwM2MIntegrationTest"]
    AbstractRpcLwM2MIntegrationTest_p1212["AbstractRpcLwM2MIntegrationTest"] -->|extends| RpcLwm2mIntegrationCreateTest_c1212["RpcLwm2mIntegrationCreateTest"]
    AbstractRpcLwM2MIntegrationTest_p1213["AbstractRpcLwM2MIntegrationTest"] -->|extends| RpcLwm2mIntegrationDeleteTest_c1213["RpcLwm2mIntegrationDeleteTest"]
    AbstractRpcLwM2MIntegrationTest_p1214["AbstractRpcLwM2MIntegrationTest"] -->|extends| RpcLwm2mIntegrationDiscoverTest_c1214["RpcLwm2mIntegrationDiscoverTest"]
    AbstractRpcLwM2MIntegrationTest_p1215["AbstractRpcLwM2MIntegrationTest"] -->|extends| RpcLwm2mIntegrationExecuteTest_c1215["RpcLwm2mIntegrationExecuteTest"]
    AbstractRpcLwM2MIntegrationTest_p1216["AbstractRpcLwM2MIntegrationTest"] -->|extends| RpcLwm2mIntegrationObserveTest_c1216["RpcLwm2mIntegrationObserveTest"]
    AbstractRpcLwM2MIntegrationTest_p1217["AbstractRpcLwM2MIntegrationTest"] -->|extends| RpcLwm2mIntegrationReadTest_c1217["RpcLwm2mIntegrationReadTest"]
    AbstractRpcLwM2MIntegrationTest_p1218["AbstractRpcLwM2MIntegrationTest"] -->|extends| RpcLwm2mIntegrationWriteAttributesTest_c1218["RpcLwm2mIntegrationWriteAttributesTest"]
    AbstractRpcLwM2MIntegrationTest_p1219["AbstractRpcLwM2MIntegrationTest"] -->|extends| RpcLwm2mIntegrationWriteTest_c1219["RpcLwm2mIntegrationWriteTest"]
    AbstractLwM2MIntegrationTest_p1220["AbstractLwM2MIntegrationTest"] -->|extends| AbstractSecurityLwM2MIntegrationTest_c1220["AbstractSecurityLwM2MIntegrationTest"]
    AbstractSecurityLwM2MIntegrationTest_p1221["AbstractSecurityLwM2MIntegrationTest"] -->|extends| NoSecLwM2MIntegrationTest_c1221["NoSecLwM2MIntegrationTest"]
    AbstractSecurityLwM2MIntegrationTest_p1222["AbstractSecurityLwM2MIntegrationTest"] -->|extends| PskLwm2mIntegrationTest_c1222["PskLwm2mIntegrationTest"]
    AbstractSecurityLwM2MIntegrationTest_p1223["AbstractSecurityLwM2MIntegrationTest"] -->|extends| RpkLwM2MIntegrationTest_c1223["RpkLwM2MIntegrationTest"]
    AbstractSecurityLwM2MIntegrationTest_p1224["AbstractSecurityLwM2MIntegrationTest"] -->|extends| X509_NoTrustLwM2MIntegrationTest_c1224["X509_NoTrustLwM2MIntegrationTest"]
    AbstractSecurityLwM2MIntegrationTest_p1225["AbstractSecurityLwM2MIntegrationTest"] -->|extends| X509_TrustLwM2MIntegrationTest_c1225["X509_TrustLwM2MIntegrationTest"]
    Object______p1226["Object/外部框架"] -->|extends| LwM2mTransportServerHelperTest_c1226["LwM2mTransportServerHelperTest"]
    AbstractTransportIntegrationTest_p1227["AbstractTransportIntegrationTest"] -->|extends| AbstractMqttIntegrationTest_c1227["AbstractMqttIntegrationTest"]
    Object______p1228["Object/外部框架"] -->|extends| MqttTestConfigProperties_c1228["MqttTestConfigProperties"]
    Object______p1229["Object/外部框架"] -->|extends| MqttTestCallback_c1229["MqttTestCallback"]
    MqttCallback_p1230["MqttCallback"] -->|implements| MqttTestCallback_c1230["MqttTestCallback"]
    Object______p1231["Object/外部框架"] -->|extends| MqttTestClient_c1231["MqttTestClient"]
    MqttTestCallback_p1232["MqttTestCallback"] -->|extends| MqttTestSubscribeOnTopicCallback_c1232["MqttTestSubscribeOnTopicCallback"]
    AbstractMqttIntegrationTest_p1233["AbstractMqttIntegrationTest"] -->|extends| AbstractMqttAttributesIntegrationTest_c1233["AbstractMqttAttributesIntegrationTest"]
    AbstractMqttAttributesIntegrationTest_p1234["AbstractMqttAttributesIntegrationTest"] -->|extends| MqttAttributesRequestBackwardCompatibilityIntegrationTest_c1234["MqttAttributesRequestBackwardCompatibilityIntegrationTest"]
    AbstractMqttAttributesIntegrationTest_p1235["AbstractMqttAttributesIntegrationTest"] -->|extends| MqttAttributesRequestIntegrationTest_c1235["MqttAttributesRequestIntegrationTest"]
    AbstractMqttAttributesIntegrationTest_p1236["AbstractMqttAttributesIntegrationTest"] -->|extends| MqttAttributesRequestJsonIntegrationTest_c1236["MqttAttributesRequestJsonIntegrationTest"]
    AbstractMqttAttributesIntegrationTest_p1237["AbstractMqttAttributesIntegrationTest"] -->|extends| MqttAttributesRequestProtoIntegrationTest_c1237["MqttAttributesRequestProtoIntegrationTest"]
    AbstractMqttAttributesIntegrationTest_p1238["AbstractMqttAttributesIntegrationTest"] -->|extends| MqttAttributesUpdatesBackwardCompatibilityIntegrationTest_c1238["MqttAttributesUpdatesBackwardCompatibilityIntegrationTest"]
    AbstractMqttAttributesIntegrationTest_p1239["AbstractMqttAttributesIntegrationTest"] -->|extends| MqttAttributesUpdatesIntegrationTest_c1239["MqttAttributesUpdatesIntegrationTest"]
    AbstractMqttAttributesIntegrationTest_p1240["AbstractMqttAttributesIntegrationTest"] -->|extends| MqttAttributesUpdatesJsonIntegrationTest_c1240["MqttAttributesUpdatesJsonIntegrationTest"]
    AbstractMqttAttributesIntegrationTest_p1241["AbstractMqttAttributesIntegrationTest"] -->|extends| MqttAttributesUpdatesProtoIntegrationTest_c1241["MqttAttributesUpdatesProtoIntegrationTest"]
    MqttClaimDeviceTest_p1242["MqttClaimDeviceTest"] -->|extends| MqttClaimBackwardCompatibilityDeviceTest_c1242["MqttClaimBackwardCompatibilityDeviceTest"]
    AbstractMqttIntegrationTest_p1243["AbstractMqttIntegrationTest"] -->|extends| MqttClaimDeviceTest_c1243["MqttClaimDeviceTest"]
    MqttClaimDeviceTest_p1244["MqttClaimDeviceTest"] -->|extends| MqttClaimJsonDeviceTest_c1244["MqttClaimJsonDeviceTest"]
    MqttClaimDeviceTest_p1245["MqttClaimDeviceTest"] -->|extends| MqttClaimProtoDeviceTest_c1245["MqttClaimProtoDeviceTest"]
    AbstractMqttIntegrationTest_p1246["AbstractMqttIntegrationTest"] -->|extends| AbstractMqttClientConnectionTest_c1246["AbstractMqttClientConnectionTest"]
    AbstractMqttClientConnectionTest_p1247["AbstractMqttClientConnectionTest"] -->|extends| MqttClientConnectionTest_c1247["MqttClientConnectionTest"]
    AbstractMqttIntegrationTest_p1248["AbstractMqttIntegrationTest"] -->|extends| BasicMqttCredentialsTest_c1248["BasicMqttCredentialsTest"]
    AbstractMqttIntegrationTest_p1249["AbstractMqttIntegrationTest"] -->|extends| MqttProvisionJsonDeviceTest_c1249["MqttProvisionJsonDeviceTest"]
    AbstractMqttIntegrationTest_p1250["AbstractMqttIntegrationTest"] -->|extends| MqttProvisionProtoDeviceTest_c1250["MqttProvisionProtoDeviceTest"]
    AbstractMqttIntegrationTest_p1251["AbstractMqttIntegrationTest"] -->|extends| AbstractMqttServerSideRpcIntegrationTest_c1251["AbstractMqttServerSideRpcIntegrationTest"]
    MqttTestSubscribeOnTopicCallback_p1252["MqttTestSubscribeOnTopicCallback"] -->|extends| MqttTestRpcJsonCallback_c1252["MqttTestRpcJsonCallback"]
    MqttTestSubscribeOnTopicCallback_p1253["MqttTestSubscribeOnTopicCallback"] -->|extends| MqttTestRpcProtoCallback_c1253["MqttTestRpcProtoCallback"]
    MqttTestCallback_p1254["MqttTestCallback"] -->|extends| MqttTestOneWaySequenceCallback_c1254["MqttTestOneWaySequenceCallback"]
    MqttTestCallback_p1255["MqttTestCallback"] -->|extends| MqttTestTwoWaySequenceCallback_c1255["MqttTestTwoWaySequenceCallback"]
    AbstractMqttServerSideRpcIntegrationTest_p1256["AbstractMqttServerSideRpcIntegrationTest"] -->|extends| MqttServerSideRpcBackwardCompatibilityIntegrationTest_c1256["MqttServerSideRpcBackwardCompatibilityIntegrationTest"]
    AbstractMqttServerSideRpcIntegrationTest_p1257["AbstractMqttServerSideRpcIntegrationTest"] -->|extends| MqttServerSideRpcDefaultIntegrationTest_c1257["MqttServerSideRpcDefaultIntegrationTest"]
    AbstractMqttServerSideRpcIntegrationTest_p1258["AbstractMqttServerSideRpcIntegrationTest"] -->|extends| MqttServerSideRpcJsonIntegrationTest_c1258["MqttServerSideRpcJsonIntegrationTest"]
    AbstractMqttServerSideRpcIntegrationTest_p1259["AbstractMqttServerSideRpcIntegrationTest"] -->|extends| MqttServerSideRpcProtoIntegrationTest_c1259["MqttServerSideRpcProtoIntegrationTest"]
    AbstractMqttServerSideRpcIntegrationTest_p1260["AbstractMqttServerSideRpcIntegrationTest"] -->|extends| MqttServerSideRpcSequenceOnAckIntegrationTest_c1260["MqttServerSideRpcSequenceOnAckIntegrationTest"]
    AbstractMqttServerSideRpcIntegrationTest_p1261["AbstractMqttServerSideRpcIntegrationTest"] -->|extends| MqttServerSideRpcSequenceOnResponseIntegrationTest_c1261["MqttServerSideRpcSequenceOnResponseIntegrationTest"]
    AbstractMqttIntegrationTest_p1262["AbstractMqttIntegrationTest"] -->|extends| MqttAttributesIntegrationTest_c1262["MqttAttributesIntegrationTest"]
    MqttAttributesIntegrationTest_p1263["MqttAttributesIntegrationTest"] -->|extends| MqttAttributesJsonIntegrationTest_c1263["MqttAttributesJsonIntegrationTest"]
    MqttAttributesIntegrationTest_p1264["MqttAttributesIntegrationTest"] -->|extends| MqttAttributesProtoIntegrationTest_c1264["MqttAttributesProtoIntegrationTest"]
    AbstractMqttIntegrationTest_p1265["AbstractMqttIntegrationTest"] -->|extends| AbstractMqttTimeseriesIntegrationTest_c1265["AbstractMqttTimeseriesIntegrationTest"]
    AbstractMqttTimeseriesIntegrationTest_p1266["AbstractMqttTimeseriesIntegrationTest"] -->|extends| AbstractMqttTimeseriesJsonIntegrationTest_c1266["AbstractMqttTimeseriesJsonIntegrationTest"]
    AbstractMqttTimeseriesIntegrationTest_p1267["AbstractMqttTimeseriesIntegrationTest"] -->|extends| AbstractMqttTimeseriesProtoIntegrationTest_c1267["AbstractMqttTimeseriesProtoIntegrationTest"]
    AbstractMqttTimeseriesIntegrationTest_p1268["AbstractMqttTimeseriesIntegrationTest"] -->|extends| MqttTimeseriesNoSqlIntegrationTest_c1268["MqttTimeseriesNoSqlIntegrationTest"]
    AbstractMqttTimeseriesJsonIntegrationTest_p1269["AbstractMqttTimeseriesJsonIntegrationTest"] -->|extends| MqttTimeseriesNoSqlJsonIntegrationTest_c1269["MqttTimeseriesNoSqlJsonIntegrationTest"]
    AbstractMqttTimeseriesProtoIntegrationTest_p1270["AbstractMqttTimeseriesProtoIntegrationTest"] -->|extends| MqttTimeseriesNoSqlProtoIntegrationTest_c1270["MqttTimeseriesNoSqlProtoIntegrationTest"]
    AbstractMqttTimeseriesIntegrationTest_p1271["AbstractMqttTimeseriesIntegrationTest"] -->|extends| MqttTimeseriesSqlIntegrationTest_c1271["MqttTimeseriesSqlIntegrationTest"]
    AbstractMqttTimeseriesJsonIntegrationTest_p1272["AbstractMqttTimeseriesJsonIntegrationTest"] -->|extends| MqttTimeseriesSqlJsonIntegrationTest_c1272["MqttTimeseriesSqlJsonIntegrationTest"]
    AbstractMqttTimeseriesProtoIntegrationTest_p1273["AbstractMqttTimeseriesProtoIntegrationTest"] -->|extends| MqttTimeseriesSqlProtoIntegrationTest_c1273["MqttTimeseriesSqlProtoIntegrationTest"]
    AbstractMqttIntegrationTest_p1274["AbstractMqttIntegrationTest"] -->|extends| AbstractMqttV5Test_c1274["AbstractMqttV5Test"]
    Object______p1275["Object/外部框架"] -->|extends| MqttV5TestCallback_c1275["MqttV5TestCallback"]
    MqttCallback_p1276["MqttCallback"] -->|implements| MqttV5TestCallback_c1276["MqttV5TestCallback"]
    Object______p1277["Object/外部框架"] -->|extends| MqttV5TestClient_c1277["MqttV5TestClient"]
    AbstractMqttV5Test_p1278["AbstractMqttV5Test"] -->|extends| AbstractAttributesMqttV5Test_c1278["AbstractAttributesMqttV5Test"]
    AbstractAttributesMqttV5Test_p1279["AbstractAttributesMqttV5Test"] -->|extends| AttributesUpdatesTest_c1279["AttributesUpdatesTest"]
    AbstractAttributesMqttV5Test_p1280["AbstractAttributesMqttV5Test"] -->|extends| AttributesPublishTest_c1280["AttributesPublishTest"]
    AbstractMqttV5Test_p1281["AbstractMqttV5Test"] -->|extends| AbstractMqttV5ClaimTest_c1281["AbstractMqttV5ClaimTest"]
    AbstractMqttV5ClaimTest_p1282["AbstractMqttV5ClaimTest"] -->|extends| MqttV5ClaimTest_c1282["MqttV5ClaimTest"]
    AbstractMqttIntegrationTest_p1283["AbstractMqttIntegrationTest"] -->|extends| AbstractMqttV5ClientConnectionTest_c1283["AbstractMqttV5ClientConnectionTest"]
    AbstractMqttV5ClientConnectionTest_p1284["AbstractMqttV5ClientConnectionTest"] -->|extends| MqttV5ClientConnectionTest_c1284["MqttV5ClientConnectionTest"]
    AbstractMqttIntegrationTest_p1285["AbstractMqttIntegrationTest"] -->|extends| AbstractMqttV5ClientPublishTest_c1285["AbstractMqttV5ClientPublishTest"]
    AbstractMqttV5ClientPublishTest_p1286["AbstractMqttV5ClientPublishTest"] -->|extends| MqttV5ClientPublishTest_c1286["MqttV5ClientPublishTest"]
    AbstractMqttIntegrationTest_p1287["AbstractMqttIntegrationTest"] -->|extends| AbstractMqttV5ClientSubscriptionTest_c1287["AbstractMqttV5ClientSubscriptionTest"]
    AbstractMqttV5ClientSubscriptionTest_p1288["AbstractMqttV5ClientSubscriptionTest"] -->|extends| MqttV5ClientSubscriptionTest_c1288["MqttV5ClientSubscriptionTest"]
    AbstractMqttIntegrationTest_p1289["AbstractMqttIntegrationTest"] -->|extends| AbstractMqttV5ClientUnsubscribeTest_c1289["AbstractMqttV5ClientUnsubscribeTest"]
    AbstractMqttV5ClientUnsubscribeTest_p1290["AbstractMqttV5ClientUnsubscribeTest"] -->|extends| MqttV5ClientUnsubscribeTest_c1290["MqttV5ClientUnsubscribeTest"]
    AbstractMqttV5Test_p1291["AbstractMqttV5Test"] -->|extends| MqttV5ProvisionDeviceTest_c1291["MqttV5ProvisionDeviceTest"]
    AbstractMqttV5Test_p1292["AbstractMqttV5Test"] -->|extends| AbstractMqttV5RpcTest_c1292["AbstractMqttV5RpcTest"]
    MqttV5TestCallback_p1293["MqttV5TestCallback"] -->|extends| MqttV5TestRpcCallback_c1293["MqttV5TestRpcCallback"]
    AbstractMqttV5RpcTest_p1294["AbstractMqttV5RpcTest"] -->|extends| MqttV5RpcTest_c1294["MqttV5RpcTest"]
    AbstractMqttV5Test_p1295["AbstractMqttV5Test"] -->|extends| AbstractMqttV5TimeseriesTest_c1295["AbstractMqttV5TimeseriesTest"]
    AbstractMqttV5TimeseriesTest_p1296["AbstractMqttV5TimeseriesTest"] -->|extends| MqttV5TimeseriesTest_c1296["MqttV5TimeseriesTest"]
    AbstractMqttIntegrationTest_p1297["AbstractMqttIntegrationTest"] -->|extends| AbstractMqttV5ClientSparkplugTest_c1297["AbstractMqttV5ClientSparkplugTest"]
    Object______p1298["Object/外部框架"] -->|extends| SparkplugMqttCallback_c1298["SparkplugMqttCallback"]
    MqttCallback_p1299["MqttCallback"] -->|implements| SparkplugMqttCallback_c1299["SparkplugMqttCallback"]
    AbstractMqttV5ClientSparkplugTest_p1300["AbstractMqttV5ClientSparkplugTest"] -->|extends| AbstractMqttV5ClientSparkplugAttributesTest_c1300["AbstractMqttV5ClientSparkplugAttributesTest"]
    AbstractMqttV5ClientSparkplugAttributesTest_p1301["AbstractMqttV5ClientSparkplugAttributesTest"] -->|extends| MqttV5ClientSparkplugBAttributesInProfileTest_c1301["MqttV5ClientSparkplugBAttributesInProfileTest"]
    AbstractMqttV5ClientSparkplugAttributesTest_p1302["AbstractMqttV5ClientSparkplugAttributesTest"] -->|extends| MqttV5ClientSparkplugBAttributesTest_c1302["MqttV5ClientSparkplugBAttributesTest"]
    AbstractMqttV5ClientSparkplugTest_p1303["AbstractMqttV5ClientSparkplugTest"] -->|extends| AbstractMqttV5ClientSparkplugConnectionTest_c1303["AbstractMqttV5ClientSparkplugConnectionTest"]
    AbstractMqttV5ClientSparkplugConnectionTest_p1304["AbstractMqttV5ClientSparkplugConnectionTest"] -->|extends| MqttV5ClientSparkplugBConnectionTest_c1304["MqttV5ClientSparkplugBConnectionTest"]
    AbstractMqttV5ClientSparkplugTest_p1305["AbstractMqttV5ClientSparkplugTest"] -->|extends| AbstractMqttV5RpcSparkplugTest_c1305["AbstractMqttV5RpcSparkplugTest"]
    AbstractMqttV5RpcSparkplugTest_p1306["AbstractMqttV5RpcSparkplugTest"] -->|extends| MqttV5RpcSparkplugTest_c1306["MqttV5RpcSparkplugTest"]
    AbstractMqttV5ClientSparkplugTest_p1307["AbstractMqttV5ClientSparkplugTest"] -->|extends| AbstractMqttV5ClientSparkplugTelemetryTest_c1307["AbstractMqttV5ClientSparkplugTelemetryTest"]
    AbstractMqttV5ClientSparkplugTelemetryTest_p1308["AbstractMqttV5ClientSparkplugTelemetryTest"] -->|extends| MqttV5ClientSparkplugBTelemetryTest_c1308["MqttV5ClientSparkplugBTelemetryTest"]
    Object______p1309["Object/外部框架"] -->|extends| TbNodeUpgradeUtilsTest_c1309["TbNodeUpgradeUtilsTest"]
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

- `RuleChainManagerActor.initRuleChains()` (protected, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleChainManagerActor.destroyRuleChains()` (protected, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleChainManagerActor.TbEntityActorId()` (package, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleChainManagerActor.visit()` (protected, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleChainManagerActor.getOrCreateActor()` (protected, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleChainManagerActor.getOrCreateActor()` (package, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleChainManagerActor.getOrCreateActor()` (protected, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleChainManagerActor.RuleEngineException()` (package, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleChainManagerActor.getEntityActorRef()` (protected, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleChainManagerActor.broadcast()` (protected, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleChainManagerActor.TbEntityTypeActorIdPredicate()` (package, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleChainManagerActor.java`)
- `RuleEngineComponentActor.logLifecycleEvent()` (protected, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleEngineComponentActor.java`)
- `RuleEngineComponentActor.destroy()` (public, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleEngineComponentActor.java`)
- `RuleEngineComponentActor.getRuleChainId()` (package, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleEngineComponentActor.java`)
- `RuleEngineComponentActor.getRuleChainName()` (package, `application/src/main/java/org/thingsboard/server/actors/ruleChain/RuleEngineComponentActor.java`)
- `TbToRuleChainActorMsg.getRuleChainId()` (public, `application/src/main/java/org/thingsboard/server/actors/ruleChain/TbToRuleChainActorMsg.java`)
- `TbToRuleChainActorMsg.onTbActorStopped()` (public, `application/src/main/java/org/thingsboard/server/actors/ruleChain/TbToRuleChainActorMsg.java`)
- `TbToRuleChainActorMsg.RuleEngineException()` (package, `application/src/main/java/org/thingsboard/server/actors/ruleChain/TbToRuleChainActorMsg.java`)
- `TbToRuleNodeActorMsg.onTbActorStopped()` (public, `application/src/main/java/org/thingsboard/server/actors/ruleChain/TbToRuleNodeActorMsg.java`)
- `TbToRuleNodeActorMsg.RuleNodeException()` (package, `application/src/main/java/org/thingsboard/server/actors/ruleChain/TbToRuleNodeActorMsg.java`)
- `ComponentActor.createProcessor()` (protected, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.TbActorException()` (package, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.destroy()` (public, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.onComponentLifecycleMsg()` (protected, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.onClusterEventMsg()` (protected, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.onStatsPersistTick()` (protected, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.StatsPersistMsg()` (package, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.increaseMessagesProcessedCount()` (protected, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.logAndPersist()` (protected, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.getErrorPersistFrequency()` (package, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.logLifecycleEvent()` (protected, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ComponentActor.getErrorPersistFrequency()` (package, `application/src/main/java/org/thingsboard/server/actors/service/ComponentActor.java`)
- `ContextAwareActor.process()` (public, `application/src/main/java/org/thingsboard/server/actors/service/ContextAwareActor.java`)
- `ContextAwareActor.doProcess()` (package, `application/src/main/java/org/thingsboard/server/actors/service/ContextAwareActor.java`)
- `ContextAwareActor.onProcessFailure()` (public, `application/src/main/java/org/thingsboard/server/actors/service/ContextAwareActor.java`)
- `ContextAwareActor.doProcessFailure()` (package, `application/src/main/java/org/thingsboard/server/actors/service/ContextAwareActor.java`)
- `ContextAwareActor.doProcessFailure()` (protected, `application/src/main/java/org/thingsboard/server/actors/service/ContextAwareActor.java`)
- `AbstractContextAwareMsgProcessor.schedulePeriodicMsgWithDelay()` (protected, `application/src/main/java/org/thingsboard/server/actors/shared/AbstractContextAwareMsgProcessor.java`)
- `AbstractContextAwareMsgProcessor.scheduleMsgWithDelay()` (protected, `application/src/main/java/org/thingsboard/server/actors/shared/AbstractContextAwareMsgProcessor.java`)
- `ActorTerminationMsg.getId()` (public, `application/src/main/java/org/thingsboard/server/actors/shared/ActorTerminationMsg.java`)


## 哪些方法可以重写

- `NetworkReceive.source()` (public, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.complete()` (public, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.EOFException()` (package, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.InvalidReceiveException()` (package, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.ThingsboardKafkaClientError()` (package, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.EOFException()` (package, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.requiredMemoryAmountKnown()` (public, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.memoryAllocated()` (public, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.payload()` (public, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.bytesRead()` (public, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.size()` (public, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.payload()` (package, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `ThingsboardInstallApplication.SpringApplication()` (package, `application/src/main/java/org/thingsboard/server/ThingsboardInstallApplication.java`)
- `ThingsboardServerApplication.updateArguments()` (package, `application/src/main/java/org/thingsboard/server/ThingsboardServerApplication.java`)
- `ActorSystemContext.onSuccess()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.onFailure()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.onSuccess()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.onFailure()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.getDebugPerTenantLimits()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.init()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.printStats()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.getScheduler()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.persistError()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.persistLifecycleEvent()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.StringWriter()` (package, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.PrintWriter()` (package, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.resolve()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.resolve()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.getServiceId()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.persistDebugInput()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.persistDebugInput()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.persistDebugOutput()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.persistDebugOutput()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.persistDebugOutput()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.DebugTbRateLimits()` (package, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.Exception()` (package, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.tell()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.tellWithHighPriority()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.schedulePeriodicMsgWithDelay()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.scheduleMsgWithDelay()` (public, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)


## 哪些方法必须重写

- `NetworkReceive.EOFException()` (package, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.InvalidReceiveException()` (package, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.ThingsboardKafkaClientError()` (package, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.EOFException()` (package, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `NetworkReceive.payload()` (package, `application/src/main/java/org/apache/kafka/common/network/NetworkReceive.java`)
- `ThingsboardInstallApplication.SpringApplication()` (package, `application/src/main/java/org/thingsboard/server/ThingsboardInstallApplication.java`)
- `ThingsboardServerApplication.updateArguments()` (package, `application/src/main/java/org/thingsboard/server/ThingsboardServerApplication.java`)
- `ActorSystemContext.StringWriter()` (package, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.PrintWriter()` (package, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.DebugTbRateLimits()` (package, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `ActorSystemContext.Exception()` (package, `application/src/main/java/org/thingsboard/server/actors/ActorSystemContext.java`)
- `AppActor.RuleEngineException()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `AppActor.TbEntityActorId()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `AppActor.TbEntityActorId()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `AppActor.doProcessFailure()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `AppActor.TbEntityActorId()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `ActorCreator.RuleEngineException()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `ActorCreator.TbEntityActorId()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `ActorCreator.TbEntityActorId()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `ActorCreator.doProcessFailure()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `ActorCreator.TbEntityActorId()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `ActorCreator.AppActor()` (package, `application/src/main/java/org/thingsboard/server/actors/app/AppActor.java`)
- `DeviceActor.DeviceActorMessageProcessor()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActor.java`)
- `DeviceActor.TbActorException()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActor.java`)
- `DeviceActorCreator.TbEntityActorId()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorCreator.java`)
- `DeviceActorCreator.DeviceActor()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorCreator.java`)
- `DeviceActorMessageProcessor.TbMsgMetaData()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.EdgeId()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.FromDeviceRpcResponse()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.FromDeviceRpcResponse()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.Rpc()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.UUID()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.ToDeviceRpcRequestMetadata()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.DeviceActorServerSideRpcTimeoutMsg()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.RpcId()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.FromDeviceRpcResponse()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.FromDeviceRpcResponse()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.DeviceId()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.FromDeviceRpcResponse()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)
- `DeviceActorMessageProcessor.RpcId()` (package, `application/src/main/java/org/thingsboard/server/actors/device/DeviceActorMessageProcessor.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `NetworkReceive` (extends)
- `Receive` -> `NetworkReceive` (implements)
- `Object/外部框架` -> `ThingsboardInstallApplication` (extends)
- `Object/外部框架` -> `ThingsboardServerApplication` (extends)
- `Object/外部框架` -> `ActorSystemContext` (extends)
- `Object/外部框架` -> `TbEntityTypeActorIdPredicate` (extends)
- `Predicate` -> `TbEntityTypeActorIdPredicate` (implements)
- `ContextAwareActor` -> `AppActor` (extends)
- `ContextBasedCreator` -> `ActorCreator` (extends)
- `Object/外部框架` -> `AppInitMsg` (extends)
- `TbActorMsg` -> `AppInitMsg` (implements)
- `ContextAwareActor` -> `DeviceActor` (extends)
- `ContextBasedCreator` -> `DeviceActorCreator` (extends)
- `AbstractContextAwareMsgProcessor` -> `DeviceActorMessageProcessor` (extends)
- `Object/外部框架` -> `SessionInfo` (extends)
- `Object/外部框架` -> `SessionInfoMetaData` (extends)
- `Object/外部框架` -> `SessionTimeoutCheckMsg` (extends)
- `TbActorMsg` -> `SessionTimeoutCheckMsg` (implements)
- `Object/外部框架` -> `ToDeviceRpcRequestMetadata` (extends)
- `Object/外部框架` -> `ToServerRpcRequestMetadata` (extends)
- `Object/外部框架` -> `DefaultTbContext` (extends)
- `TbContext` -> `DefaultTbContext` (implements)
- `RuleEngineComponentActor` -> `RuleChainActor` (extends)
- `ComponentMsgProcessor` -> `RuleChainActorMessageProcessor` (extends)
- `TbToRuleChainActorMsg` -> `RuleChainInputMsg` (extends)
- `ContextAwareActor` -> `RuleChainManagerActor` (extends)
- `TbToRuleChainActorMsg` -> `RuleChainOutputMsg` (extends)
- `TbToRuleChainActorMsg` -> `RuleChainToRuleChainMsg` (extends)
- `TbToRuleNodeActorMsg` -> `RuleChainToRuleNodeMsg` (extends)
- `Object/外部框架` -> `RuleEngineComponentActor` (extends)
- `RuleEngineComponentActor` -> `RuleNodeActor` (extends)
- `ComponentMsgProcessor` -> `RuleNodeActorMessageProcessor` (extends)
- `Object/外部框架` -> `RuleNodeCtx` (extends)
- `Object/外部框架` -> `RuleNodeRelation` (extends)
- `TbRuleEngineActorMsg` -> `RuleNodeToRuleChainTellNextMsg` (extends)
- `Serializable` -> `RuleNodeToRuleChainTellNextMsg` (implements)
- `TbToRuleNodeActorMsg` -> `RuleNodeToSelfMsg` (extends)
- `TbRuleEngineActorMsg` -> `TbToRuleChainActorMsg` (extends)
- `RuleChainAwareMsg` -> `TbToRuleChainActorMsg` (implements)
- `TbRuleEngineActorMsg` -> `TbToRuleNodeActorMsg` (extends)
- 其余 40 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 外部父类/接口

- `AbstractAuthenticationProcessingFilter`
- `AbstractAuthenticationToken`
- `AbstractListeningExecutor`
- `AbstractNoSqlContainer`
- `AbstractTbActor`
- `AbstractTbQueueConsumerTemplate`
- `AccessDeniedHandler`
- `AccountStatusException`
- `ApplicationListener`
- `ArrayList`
- `AuthenticationDetailsSource`
- `AuthenticationException`
- `AuthenticationFailureHandler`
- `AuthenticationProvider`
- `AuthenticationServiceException`
- `AuthenticationSuccessHandler`
- `AuthorizationRequestRepository`
- `BaseInstanceEnabler`
- `BeanPostProcessor`
- `CaffeineTbTransactionalCache`
- `ClaimDevicesService`
- `Closeable`
- `CoapHandler`
- `Comparable`
- `ComponentLifecycleListener`
- `CredentialsExpiredException`
- `DefaultNotificationSettingsService`
- `Destroyable`
- `DeviceAuthService`
- `DeviceAwareMsg`
- `DeviceProvisionService`
- `EdgeRpcServiceImplBase`
- `ErrorController`
- `Exception`
- `ExecutorProvider`
- `ExitCodeGenerator`
- `FirebaseService`
- `FutureCallback`
- `HashMap`
- `HouseKeeperService`
- 其余 53 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
