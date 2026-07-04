# Thingsboard Server Common Data 模块继承体系分析

> 生成范围：`common/data`  
> Maven artifact：`data`  
> Java 类型数量：667  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
AlarmModificationRequest
├── «implements» AlarmCreateOrUpdateActiveRequest
├── «implements» AlarmUpdateRequest
DeviceCredentialsFilter
├── «implements» DeviceCredentials
├── «implements» DeviceTokenCredentials
├── «implements» DeviceX509Credentials
EntityFilter
├── «implements» ApiUsageStateFilter
├── «implements» AssetTypeFilter
├── «implements» DeviceTypeFilter
├── «implements» EdgeTypeFilter
├── «implements» EntityListFilter
├── «implements» EntityNameFilter
├── «implements» EntitySearchQueryFilter
├── «implements» ├── AssetSearchQueryFilter
├── «implements» ├── DeviceSearchQueryFilter
├── «implements» ├── EdgeSearchQueryFilter
├── «implements» ├── EntityViewSearchQueryFilter
├── «implements» EntityTypeFilter
├── «implements» EntityViewTypeFilter
├── «implements» RelationsQueryFilter
├── «implements» SingleEntityFilter
Error
├── ThingsboardKafkaClientError
EventFilter
├── «implements» DebugEventFilter
├── «implements» ├── RuleChainDebugEventFilter
├── «implements» ├── RuleNodeDebugEventFilter
├── «implements» ErrorEventFilter
├── «implements» LifeCycleEventFilter
├── «implements» StatisticsEventFilter
Exception
├── ThingsboardException
ExportableEntity
├── «implements» Asset
├── «implements» ├── AssetInfo
├── «implements» AssetProfile
├── «implements» Customer
├── «implements» Dashboard
├── «implements» ├── HomeDashboard
├── «implements» Device
├── «implements» ├── DeviceInfo
├── «implements» DeviceProfile
├── «implements» EntityView
├── «implements» ├── EntityViewInfo
├── «implements» NotificationRule
├── «implements» ├── NotificationRuleInfo
├── «implements» NotificationTarget
├── «implements» NotificationTemplate
├── «implements» RuleChain
├── «implements» TbResourceInfo
├── «implements» ├── TbResource
├── «implements» WidgetTypeDetails
├── «implements» WidgetsBundle
HasAdditionalInfo
├── «implements» BaseDataWithAdditionalInfo
├── «implements» ├── Asset
├── «implements» ├── ├── AssetInfo
├── «implements» ├── Device
├── «implements» ├── ├── DeviceInfo
├── «implements» ├── Edge
├── «implements» ├── ├── EdgeInfo
├── «implements» ├── EntityView
├── «implements» ├── ├── EntityViewInfo
├── «implements» ├── OAuth2ClientRegistrationTemplate
├── «implements» ├── OAuth2Registration
├── «implements» ├── OtaPackageInfo
├── «implements» ├── ├── OtaPackage
├── «implements» ├── ├── SaveOtaPackageInfoRequest
├── «implements» ├── Queue
├── «implements» ├── RuleChain
├── «implements» ├── RuleNode
├── «implements» ├── User
HasCustomerId
├── «implements» Alarm
├── «implements» ├── AlarmInfo
├── «implements» ├── ├── AlarmData
├── «implements» Asset
├── «implements» ├── AssetInfo
├── «implements» Device
├── «implements» ├── DeviceInfo
├── «implements» Edge
├── «implements» ├── EdgeInfo
├── «implements» EntityView
├── «implements» ├── EntityViewInfo
├── «implements» User
HasName
├── «implements» Alarm
├── «implements» ├── AlarmInfo
├── «implements» ├── ├── AlarmData
├── «implements» AlarmComment
├── «implements» ├── AlarmCommentInfo
├── «implements» AssetProfile
├── «implements» BaseWidgetType
├── «implements» ├── WidgetType
├── «implements» ├── ├── WidgetTypeDetails
├── «implements» ├── WidgetTypeInfo
├── «implements» DashboardInfo
├── «implements» ├── Dashboard
├── «implements» ├── ├── HomeDashboard
├── «implements» DeviceProfile
├── «implements» EntityInfo
├── «implements» ├── AssetProfileInfo
├── «implements» ├── DeviceProfileInfo
├── «implements» EntityView
├── «implements» ├── EntityViewInfo
├── HasEmail
├── ├── «implements» ContactBased
├── ├── «implements» ├── Customer
├── ├── «implements» ├── Tenant
├── ├── «implements» ├── ├── TenantInfo
├── HasImage
├── ├── «implements» AssetProfile
├── ├── «implements» DashboardInfo
├── ├── «implements» ├── Dashboard
├── ├── «implements» ├── ├── HomeDashboard
├── ├── «implements» DeviceProfile
├── ├── «implements» WidgetTypeDetails
├── ├── «implements» WidgetsBundle
├── HasLabel
├── ├── «implements» Asset
├── ├── «implements» ├── AssetInfo
├── ├── «implements» Device
├── ├── «implements» ├── DeviceInfo
├── ├── «implements» Edge
├── ├── «implements» ├── EdgeInfo
├── «implements» NotificationRequest
├── «implements» ├── NotificationRequestInfo
├── «implements» NotificationRule
├── «implements» ├── NotificationRuleInfo
├── «implements» NotificationTarget
├── «implements» NotificationTemplate
├── «implements» OAuth2ClientRegistrationTemplate
├── «implements» OAuth2Registration
├── «implements» OtaPackageInfo
├── «implements» ├── OtaPackage
├── «implements» ├── SaveOtaPackageInfoRequest
├── «implements» Queue
├── «implements» RuleChain
├── «implements» RuleNode
├── «implements» TbResourceInfo
├── «implements» ├── TbResource
├── «implements» TenantProfile
├── «implements» User
├── «implements» WidgetTypeDetails
├── «implements» WidgetsBundle
HasOtaPackage
├── «implements» Device
├── «implements» ├── DeviceInfo
├── «implements» DeviceProfile
HasRuleEngineProfile
├── «implements» AssetProfile
├── «implements» DeviceProfile
HasSubject
├── «implements» EmailDeliveryMethodNotificationTemplate
├── «implements» MicrosoftTeamsDeliveryMethodNotificationTemplate
├── «implements» MobileAppDeliveryMethodNotificationTemplate
├── «implements» WebDeliveryMethodNotificationTemplate
HasTenantId
├── «implements» AdminSettings
├── «implements» Alarm
├── «implements» ├── AlarmInfo
├── «implements» ├── ├── AlarmData
├── «implements» ApiUsageState
├── «implements» Asset
├── «implements» ├── AssetInfo
├── «implements» AssetProfile
├── «implements» BaseWidgetType
├── «implements» ├── WidgetType
├── «implements» ├── ├── WidgetTypeDetails
├── «implements» ├── WidgetTypeInfo
├── «implements» Customer
├── «implements» DashboardInfo
├── «implements» ├── Dashboard
├── «implements» ├── ├── HomeDashboard
├── «implements» Device
├── «implements» ├── DeviceInfo
├── «implements» DeviceIdInfo
├── «implements» DeviceProfile
├── «implements» Edge
├── «implements» ├── EdgeInfo
├── «implements» EntityAlarm
├── «implements» EntityView
├── «implements» ├── EntityViewInfo
├── HasImage
├── ├── «implements» AssetProfile
├── ├── «implements» DashboardInfo
├── ├── «implements» ├── Dashboard
├── ├── «implements» ├── ├── HomeDashboard
├── ├── «implements» DeviceProfile
├── ├── «implements» WidgetTypeDetails
├── ├── «implements» WidgetsBundle
├── «implements» NotificationRequest
├── «implements» ├── NotificationRequestInfo
├── «implements» NotificationRule
├── «implements» ├── NotificationRuleInfo
├── «implements» NotificationTarget
├── «implements» NotificationTemplate
├── «implements» OtaPackageInfo
├── «implements» ├── OtaPackage
├── «implements» ├── SaveOtaPackageInfoRequest
├── «implements» Queue
├── «implements» Rpc
├── «implements» RuleChain
├── «implements» TbResourceInfo
├── «implements» ├── TbResource
├── «implements» Tenant
├── «implements» ├── TenantInfo
├── «implements» User
├── «implements» WidgetTypeDetails
├── «implements» WidgetsBundle
HasTitle
├── «implements» AbstractUserDashboardInfo
├── «implements» ├── LastVisitedDashboardInfo
├── «implements» ├── StarredDashboardInfo
├── «implements» Customer
├── «implements» DashboardInfo
├── «implements» ├── Dashboard
├── «implements» ├── ├── HomeDashboard
├── «implements» OtaPackageInfo
├── «implements» ├── OtaPackage
├── «implements» ├── SaveOtaPackageInfoRequest
├── «implements» Tenant
├── «implements» ├── TenantInfo
├── «implements» WidgetsBundle
HasUUID
├── EntityId
├── ├── «implements» AlarmId
├── ├── «implements» ApiUsageStateId
├── ├── «implements» AssetId
├── ├── «implements» AssetProfileId
├── ├── «implements» CustomerId
├── ├── «implements» DashboardId
├── ├── «implements» DeviceId
├── ├── «implements» DeviceProfileId
├── ├── «implements» EdgeId
├── ├── «implements» EntityViewId
├── ├── «implements» NotificationId
├── ├── «implements» NotificationRequestId
├── ├── «implements» NotificationRuleId
├── ├── «implements» NotificationTargetId
├── ├── «implements» NotificationTemplateId
├── ├── «implements» OtaPackageId
├── ├── «implements» QueueId
├── ├── «implements» RpcId
├── ├── «implements» RuleChainId
├── ├── «implements» RuleNodeId
├── ├── «implements» TbResourceId
├── ├── «implements» TenantId
├── ├── «implements» TenantProfileId
├── ├── «implements» UserId
├── ├── «implements» WidgetTypeId
├── ├── «implements» WidgetsBundleId
├── «implements» UUIDBased
├── «implements» ├── AdminSettingsId
├── «implements» ├── AlarmCommentId
├── «implements» ├── AlarmId
├── «implements» ├── ApiUsageStateId
├── «implements» ├── AssetId
├── «implements» ├── AssetProfileId
├── «implements» ├── AuditLogId
├── «implements» ├── ComponentDescriptorId
├── «implements» ├── CustomerId
├── «implements» ├── DashboardId
├── «implements» ├── DeviceCredentialsId
├── «implements» ├── DeviceId
├── «implements» ├── DeviceProfileId
├── «implements» ├── EdgeEventId
├── «implements» ├── EdgeId
├── «implements» ├── EntityViewId
├── «implements» ├── EventId
├── «implements» ├── NodeId
├── «implements» ├── NotificationId
├── «implements» ├── NotificationRequestId
├── «implements» ├── NotificationRuleId
├── «implements» ├── NotificationTargetId
├── «implements» ├── NotificationTemplateId
├── «implements» ├── OAuth2ClientRegistrationTemplateId
├── «implements» ├── OAuth2DomainId
├── «implements» ├── OAuth2MobileId
├── «implements» ├── OAuth2ParamsId
├── «implements» ├── OAuth2RegistrationId
├── «implements» ├── OtaPackageId
├── «implements» ├── QueueId
├── «implements» ├── RpcId
├── «implements» ├── RuleChainId
├── «implements» ├── RuleNodeId
├── «implements» ├── RuleNodeStateId
├── «implements» ├── TbResourceId
├── «implements» ├── TenantId
├── «implements» ├── TenantProfileId
├── «implements» ├── UserAuthSettingsId
├── «implements» ├── UserCredentialsId
├── «implements» ├── UserId
├── «implements» ├── WidgetTypeId
├── «implements» ├── WidgetsBundleId
HashMap
├── AutoCommitSettings
JsonDeserializer
├── EntityIdDeserializer
JsonSerializer
├── EntityIdFieldSerializer
├── EntityIdSerializer
LwM2MBootstrapClientCredential
├── «implements» AbstractLwM2MBootstrapClientCredentialWithKeys
├── «implements» ├── PSKBootstrapClientCredential
├── «implements» ├── RPKBootstrapClientCredential
├── «implements» ├── X509BootstrapClientCredential
├── «implements» NoSecBootstrapClientCredential
LwM2MClientCredential
├── «implements» AbstractLwM2MClientCredential
├── «implements» ├── AbstractLwM2MClientSecurityCredential
├── «implements» ├── ├── PSKClientCredential
├── «implements» ├── ├── RPKClientCredential
├── «implements» ├── ├── X509ClientCredential
├── «implements» ├── NoSecClientCredential
NotificationInfo
├── RuleOriginatedNotificationInfo
├── ├── «implements» AlarmAssignmentNotificationInfo
├── ├── «implements» AlarmCommentNotificationInfo
├── ├── «implements» AlarmNotificationInfo
├── ├── «implements» ApiUsageLimitNotificationInfo
├── ├── «implements» DeviceActivityNotificationInfo
├── ├── «implements» EdgeCommunicationFailureNotificationInfo
├── ├── «implements» EdgeConnectionNotificationInfo
├── ├── «implements» EntitiesLimitNotificationInfo
├── ├── «implements» EntityActionNotificationInfo
├── ├── «implements» NewPlatformVersionNotificationInfo
├── ├── «implements» RateLimitsNotificationInfo
├── ├── «implements» RuleEngineComponentLifecycleEventNotificationInfo
├── ├── «implements» RuleEngineOriginatedNotificationInfo
NotificationRecipient
├── «implements» MicrosoftTeamsNotificationTargetConfig
├── «implements» SlackConversation
├── «implements» User
Object/外部框架
├── AbstractLwM2MBootstrapClientCredentialWithKeys
├── ├── PSKBootstrapClientCredential
├── ├── RPKBootstrapClientCredential
├── ├── X509BootstrapClientCredential
├── AbstractLwM2MClientCredential
├── ├── AbstractLwM2MClientSecurityCredential
├── ├── ├── PSKClientCredential
├── ├── ├── RPKClientCredential
├── ├── ├── X509ClientCredential
├── ├── NoSecClientCredential
├── AbstractUserDashboardInfo
├── ├── LastVisitedDashboardInfo
├── ├── StarredDashboardInfo
├── AccountNotificationSettings
├── AccountTwoFaSettings
├── ActionTypeTest
├── AffectedTenantAdministratorsFilter
├── AffectedUserFilter
├── AggregationParams
├── AlarmApiCallResult
├── AlarmAssignee
├── AlarmAssigneeUpdate
├── AlarmAssignmentNotificationInfo
├── AlarmAssignmentNotificationRuleTriggerConfig
├── AlarmAssignmentTrigger
├── AlarmCommentNotificationInfo
├── AlarmCommentNotificationRuleTriggerConfig
├── AlarmCommentTrigger
├── AlarmCondition
├── AlarmConditionFilter
├── AlarmConditionFilterKey
├── AlarmCreateOrUpdateActiveRequest
├── AlarmNotificationInfo
├── AlarmNotificationRuleTriggerConfig
├── AlarmPropagationInfo
├── AlarmQuery
├── AlarmQueryV2
├── AlarmRule
├── AlarmStatusFilter
├── AlarmTrigger
├── AlarmUpdateRequest
├── AllUsersFilter
├── AllowCreateNewDevicesDeviceProfileProvisionConfiguration
├── AnyTimeSchedule
├── ApiUsageLimitNotificationInfo
├── ApiUsageLimitNotificationRuleTriggerConfig
├── ApiUsageLimitTrigger
├── ApiUsageRecordState
├── ApiUsageStateFilter
├── AssetSearchQuery
├── AssetTypeFilter
├── AttributeExportData
├── AttributeKey
├── AttributesEntityView
├── AwsSnsSmsProviderConfiguration
├── BackupCodeTwoFaProviderConfig
├── BaseAttributeKvEntry
├── BaseData
├── ├── AdminSettings
├── ├── Alarm
├── ├── ├── AlarmInfo
├── ├── ├── ├── AlarmData
├── ├── AlarmComment
├── ├── ├── AlarmCommentInfo
├── ├── ApiUsageState
├── ├── AssetProfile
├── ├── AuditLog
├── ├── BaseWidgetType
├── ├── ├── WidgetType
├── ├── ├── ├── WidgetTypeDetails
├── ├── ├── WidgetTypeInfo
├── ├── ComponentDescriptor
├── ├── DashboardInfo
├── ├── ├── Dashboard
├── ├── ├── ├── HomeDashboard
├── ├── DeviceCredentials
├── ├── DeviceProfile
├── ├── EdgeEvent
├── ├── Event
├── ├── ├── ErrorEvent
├── ├── ├── LifecycleEvent
├── ├── ├── RuleChainDebugEvent
├── ├── ├── RuleNodeDebugEvent
├── ├── ├── StatisticsEvent
├── ├── EventInfo
├── ├── Notification
├── ├── NotificationRequest
├── ├── ├── NotificationRequestInfo
├── ├── NotificationRule
├── ├── ├── NotificationRuleInfo
├── ├── NotificationTarget
├── ├── NotificationTemplate
├── ├── OAuth2Domain
├── ├── OAuth2Mobile
├── ├── OAuth2Params
├── ├── Rpc
├── ├── RuleNodeState
├── ├── TbResourceInfo
├── ├── ├── TbResource
├── ├── TenantProfile
├── ├── UserAuthSettings
├── ├── UserCredentials
├── ├── WidgetsBundle
├── BaseDataWithAdditionalInfo
├── ├── Asset
├── ├── ├── AssetInfo
├── ├── Device
├── ├── ├── DeviceInfo
├── ├── Edge
├── ├── ├── EdgeInfo
├── ├── EntityView
├── ├── ├── EntityViewInfo
├── ├── OAuth2ClientRegistrationTemplate
├── ├── OAuth2Registration
├── ├── OtaPackageInfo
├── ├── ├── OtaPackage
├── ├── ├── SaveOtaPackageInfoRequest
├── ├── Queue
├── ├── RuleChain
├── ├── RuleNode
├── ├── User
├── BasePageDataIterable
├── BaseTsKvQuery
├── ├── BaseDeleteTsKvQuery
├── ├── BaseReadTsKvQuery
├── BasicKvEntry
├── ├── BooleanDataEntry
├── ├── DoubleDataEntry
├── ├── JsonDataEntry
├── ├── LongDataEntry
├── ├── StringDataEntry
├── BasicMqttCredentials
├── BasicTsKvEntry
├── ├── AggTsKvEntry
├── BooleanFilterPredicate
├── BranchInfo
├── BulkImportRequest
├── BulkImportResult
├── Button
├── CacheConstants
├── CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration
├── ClaimRequest
├── ClearRule
├── CoapDeviceProfileTransportConfiguration
├── CollectionsUtil
├── ColumnMapping
├── ComparisonTsValue
├── ComplexFilterPredicate
├── ContactBased
├── ├── Customer
├── ├── Tenant
├── ├── ├── TenantInfo
├── CustomTimeSchedule
├── CustomTimeScheduleItem
├── CustomerUsersFilter
├── DataConstants
├── DebugEventFilter
├── ├── RuleChainDebugEventFilter
├── ├── RuleNodeDebugEventFilter
├── DefaultCoapDeviceTypeConfiguration
├── DefaultDeviceConfiguration
├── DefaultDeviceProfileConfiguration
├── DefaultDeviceProfileTransportConfiguration
├── DefaultDeviceTransportConfiguration
├── DefaultRuleChainCreateRequest
├── DefaultTenantProfileConfiguration
├── DeliveryMethodNotificationTemplate
├── ├── EmailDeliveryMethodNotificationTemplate
├── ├── MicrosoftTeamsDeliveryMethodNotificationTemplate
├── ├── MobileAppDeliveryMethodNotificationTemplate
├── ├── SlackDeliveryMethodNotificationTemplate
├── ├── SmsDeliveryMethodNotificationTemplate
├── ├── WebDeliveryMethodNotificationTemplate
├── DeviceActivityNotificationInfo
├── DeviceActivityNotificationRuleTriggerConfig
├── DeviceActivityTrigger
├── DeviceData
├── DeviceIdInfo
├── DeviceInfoFilter
├── DeviceProfileAlarm
├── DeviceProfileData
├── DeviceSearchQuery
├── DeviceTokenCredentials
├── DeviceTypeFilter
├── DeviceX509Credentials
├── DisabledDeviceProfileProvisionConfiguration
├── DurationAlarmConditionSpec
├── DynamicProtoUtils
├── DynamicProtoUtilsTest
├── DynamicValue
├── EdgeCommunicationFailureNotificationInfo
├── EdgeCommunicationFailureNotificationRuleTriggerConfig
├── EdgeCommunicationFailureTrigger
├── EdgeConnectionNotificationInfo
├── EdgeConnectionNotificationRuleTriggerConfig
├── EdgeConnectionTrigger
├── EdgeInstructions
├── EdgeSearchQuery
├── EdgeTypeFilter
├── EdgeUpgradeInfo
├── EdgeUpgradeMessage
├── EdgeUtils
├── EfentoCoapDeviceTypeConfiguration
├── EntitiesLimitNotificationInfo
├── EntitiesLimitNotificationRuleTriggerConfig
├── EntitiesLimitTrigger
├── EntityActionNotificationInfo
├── EntityActionNotificationRuleTriggerConfig
├── EntityActionTrigger
├── EntityAlarm
├── EntityCountQuery
├── ├── AbstractDataQuery
├── ├── ├── AlarmDataQuery
├── ├── ├── EntityDataQuery
├── ├── AlarmCountQuery
├── EntityData
├── EntityDataDiff
├── EntityDataInfo
├── EntityDataPageLink
├── ├── AlarmDataPageLink
├── EntityDataSortOrder
├── EntityExportData
├── ├── DeviceExportData
├── ├── RuleChainExportData
├── ├── WidgetTypeExportData
├── ├── WidgetsBundleExportData
├── EntityExportSettings
├── EntityFieldsData
├── EntityIdFactory
├── EntityIdTest
├── EntityImportResult
├── EntityImportSettings
├── EntityInfo
├── ├── AssetProfileInfo
├── ├── DeviceProfileInfo
├── EntityKey
├── EntityListFilter
├── EntityLoadError
├── EntityNameFilter
├── EntityRelation
├── ├── EntityRelationInfo
├── EntityRelationsQuery
├── EntitySearchQueryFilter
├── ├── AssetSearchQueryFilter
├── ├── DeviceSearchQueryFilter
├── ├── EdgeSearchQueryFilter
├── ├── EntityViewSearchQueryFilter
├── EntitySubtype
├── EntityTypeFilter
├── EntityTypeLoadResult
├── EntityTypeTest
├── EntityVersion
├── EntityVersionsDiff
├── EntityViewSearchQuery
├── EntityViewTypeFilter
├── ErrorEventFilter
├── FSTUtils
├── FeaturesInfo
├── FilterPredicateValue
├── HomeDashboardInfo
├── IdBased
├── ImageDescriptor
├── ImageExportData
├── JsonTransportPayloadConfiguration
├── JwtPair
├── JwtSettings
├── KeyFilter
├── LifeCycleEventFilter
├── LwM2MBootstrapClientCredentials
├── LwM2MDeviceCredentials
├── LwM2MServerSecurityConfig
├── ├── AbstractLwM2MBootstrapServerCredential
├── ├── ├── NoSecLwM2MBootstrapServerCredential
├── ├── ├── PSKLwM2MBootstrapServerCredential
├── ├── ├── RPKLwM2MBootstrapServerCredential
├── ├── ├── X509LwM2MBootstrapServerCredential
├── ├── LwM2MServerSecurityConfigDefault
├── LwM2mInstance
├── LwM2mObject
├── LwM2mResourceObserve
├── Lwm2mDeviceProfileTransportConfiguration
├── Mapping
├── MobileAppNotificationDeliveryMethodConfig
├── MobileSessionInfo
├── MqttDeviceProfileTransportConfiguration
├── MqttDeviceTransportConfiguration
├── MqttTopics
├── MultipleMappingsSnmpCommunicationConfig
├── ├── RepeatingQueryingSnmpCommunicationConfig
├── ├── ├── ClientAttributesQueryingSnmpCommunicationConfig
├── ├── ├── TelemetryQueryingSnmpCommunicationConfig
├── ├── SharedAttributesSettingSnmpCommunicationConfig
├── ├── ToDeviceRpcRequestSnmpCommunicationConfig
├── ├── ToServerRpcRequestSnmpCommunicationConfig
├── NameLabelAndCustomerDetails
├── NewPlatformVersionNotificationInfo
├── NewPlatformVersionNotificationRuleTriggerConfig
├── NewPlatformVersionTrigger
├── NoSecBootstrapClientCredential
├── NodeConnectionInfo
├── NotificationPref
├── NotificationRequestConfig
├── NotificationRequestPreview
├── NotificationRequestStats
├── NotificationRuleConfig
├── NotificationRuleRecipientsConfig
├── ├── DefaultNotificationRuleRecipientsConfig
├── ├── EscalatedNotificationRuleRecipientsConfig
├── NotificationSettings
├── NotificationTargetConfig
├── ├── MicrosoftTeamsNotificationTargetConfig
├── ├── PlatformUsersNotificationTargetConfig
├── ├── SlackNotificationTargetConfig
├── NotificationTemplateConfig
├── NumericFilterPredicate
├── OAuth2BasicMapperConfig
├── OAuth2ClientInfo
├── OAuth2CustomMapperConfig
├── OAuth2DomainInfo
├── OAuth2Info
├── OAuth2MapperConfig
├── OAuth2MobileInfo
├── OAuth2ParamsInfo
├── OAuth2RegistrationInfo
├── ObjectAttributes
├── OriginatorEntityOwnerUsersFilter
├── OtaPackageUtil
├── OtpBasedTwoFaProviderConfig
├── ├── EmailTwoFaProviderConfig
├── ├── SmsTwoFaProviderConfig
├── PageData
├── PageDataIterable
├── PageDataIterableByTenant
├── PageDataIterableByTenantIdEntityId
├── PageLink
├── ├── TimePageLink
├── PlatformTwoFaSettings
├── PowerSavingConfiguration
├── ├── CoapDeviceTransportConfiguration
├── ├── Lwm2mDeviceTransportConfiguration
├── ├── OtherConfiguration
├── ProcessingStrategy
├── ProtoTransportPayloadConfiguration
├── ProvisionDeviceCredentialsData
├── ProvisionDeviceProfileCredentials
├── RateLimitsNotificationInfo
├── RateLimitsNotificationRuleTriggerConfig
├── RateLimitsTrigger
├── ReadTsKvQueryResult
├── ReflectionUtils
├── RelationEntityTypeFilter
├── RelationsQueryFilter
├── RelationsSearchParameters
├── RepeatingAlarmConditionSpec
├── RepositorySettings
├── RepositorySettingsInfo
├── ResourceUtils
├── RpcStatusTest
├── RuleChainConnectionInfo
├── RuleChainData
├── RuleChainImportResult
├── RuleChainMetaData
├── RuleChainOutputLabelsUsage
├── RuleChainUpdateResult
├── RuleEngineComponentLifecycleEventNotificationInfo
├── RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig
├── RuleEngineComponentLifecycleEventTrigger
├── RuleEngineOriginatedNotificationInfo
├── RuleNodeUpdateResult
├── SaveDeviceWithCredentialsRequest
├── SecuritySettings
├── ShortCustomerInfo
├── SimpleAlarmConditionSpec
├── SingleEntityFilter
├── SlackConversation
├── SlackNotificationDeliveryMethodConfig
├── SmppSmsProviderConfiguration
├── SnmpDeviceProfileTransportConfiguration
├── SnmpDeviceTransportConfiguration
├── SnmpMapping
├── SortOrder
├── SpecificTimeSchedule
├── StatisticsEventFilter
├── StringFilterPredicate
├── StringUtils
├── StringUtilsTest
├── SubmitStrategy
├── SystemAdministratorsFilter
├── SystemInfo
├── SystemInfoData
├── SystemParams
├── TbDDFFileParser
├── TbImageDeleteResult
├── TbMsgTypeTest
├── TbNodeConnectionType
├── TbPair
├── TbProperty
├── TbResourceInfoFilter
├── TelemetryEntityView
├── TelemetryMappingConfiguration
├── TemplatableValue
├── TemplateUtils
├── TenantAdministratorsFilter
├── TenantProfileData
├── TenantProfileQueueConfiguration
├── TestSmsRequest
├── ToDeviceRpcRequestBody
├── TotpTwoFaProviderConfig
├── TsKvEntryAggWrapper
├── TsKvLatestRemovingResult
├── TsValue
├── TwilioSmsProviderConfiguration
├── TwoFaAccountConfig
├── ├── BackupCodeTwoFaAccountConfig
├── ├── OtpBasedTwoFaAccountConfig
├── ├── ├── EmailTwoFaAccountConfig
├── ├── ├── SmsTwoFaAccountConfig
├── ├── TotpTwoFaAccountConfig
├── TypeCastUtil
├── UUIDBased
├── ├── AdminSettingsId
├── ├── AlarmCommentId
├── ├── AlarmId
├── ├── ApiUsageStateId
├── ├── AssetId
├── ├── AssetProfileId
├── ├── AuditLogId
├── ├── ComponentDescriptorId
├── ├── CustomerId
├── ├── DashboardId
├── ├── DeviceCredentialsId
├── ├── DeviceId
├── ├── DeviceProfileId
├── ├── EdgeEventId
├── ├── EdgeId
├── ├── EntityViewId
├── ├── EventId
├── ├── NodeId
├── ├── NotificationId
├── ├── NotificationRequestId
├── ├── NotificationRuleId
├── ├── NotificationTargetId
├── ├── NotificationTemplateId
├── ├── OAuth2ClientRegistrationTemplateId
├── ├── OAuth2DomainId
├── ├── OAuth2MobileId
├── ├── OAuth2ParamsId
├── ├── OAuth2RegistrationId
├── ├── OtaPackageId
├── ├── QueueId
├── ├── RpcId
├── ├── RuleChainId
├── ├── RuleNodeId
├── ├── RuleNodeStateId
├── ├── TbResourceId
├── ├── TenantId
├── ├── TenantProfileId
├── ├── UserAuthSettingsId
├── ├── UserCredentialsId
├── ├── UserId
├── ├── WidgetTypeId
├── ├── WidgetsBundleId
├── UUIDConverter
├── UUIDConverterTest
├── UpdateMessage
├── UsageInfo
├── UserAuthDataChangedEvent
├── ├── UserCredentialsInvalidationEvent
├── ├── UserSessionInvalidationEvent
├── UserDashboardsInfo
├── UserEmailInfo
├── UserListFilter
├── UserMobileInfo
├── UserNotificationSettings
├── UserPasswordPolicy
├── UserSettings
├── UserSettingsCompositeKey
├── VcUtils
├── VersionCreateConfig
├── ├── AutoVersionCreateConfig
├── ├── EntityTypeVersionCreateConfig
├── VersionCreateRequest
├── ├── ComplexVersionCreateRequest
├── ├── SingleEntityVersionCreateRequest
├── VersionCreationResult
├── VersionLoadConfig
├── ├── EntityTypeVersionLoadConfig
├── VersionLoadRequest
├── ├── EntityTypeVersionLoadRequest
├── ├── SingleEntityVersionLoadRequest
├── VersionLoadResult
├── VersionedEntityInfo
├── WidgetsBundleWidget
├── X509CertificateChainProvisionConfiguration
├── path
RuntimeException
├── AbstractRateLimitException
├── ├── ApiUsageLimitsExceededException
├── AlreadySentException
├── TenantNotFoundException
├── TenantProfileNotFoundException
Serializable
├── «implements» AbstractUserDashboardInfo
├── «implements» ├── LastVisitedDashboardInfo
├── «implements» ├── StarredDashboardInfo
├── «implements» AlarmApiCallResult
├── «implements» AlarmAssignee
├── «implements» AlarmAssigneeUpdate
├── «implements» AlarmCondition
├── «implements» AlarmConditionFilter
├── «implements» AlarmConditionFilterKey
├── AlarmConditionSpec
├── ├── «implements» DurationAlarmConditionSpec
├── ├── «implements» RepeatingAlarmConditionSpec
├── ├── «implements» SimpleAlarmConditionSpec
├── «implements» AlarmRule
├── AlarmSchedule
├── ├── «implements» AnyTimeSchedule
├── ├── «implements» CustomTimeSchedule
├── ├── «implements» SpecificTimeSchedule
├── «implements» ApiUsageRecordState
├── «implements» AttributeKey
├── «implements» AttributesEntityView
├── «implements» BaseData
├── «implements» ├── AdminSettings
├── «implements» ├── Alarm
├── «implements» ├── ├── AlarmInfo
├── «implements» ├── ├── ├── AlarmData
├── «implements» ├── AlarmComment
├── «implements» ├── ├── AlarmCommentInfo
├── «implements» ├── ApiUsageState
├── «implements» ├── AssetProfile
├── «implements» ├── AuditLog
├── «implements» ├── BaseWidgetType
├── «implements» ├── ├── WidgetType
├── «implements» ├── ├── ├── WidgetTypeDetails
├── «implements» ├── ├── WidgetTypeInfo
├── «implements» ├── ComponentDescriptor
├── «implements» ├── DashboardInfo
├── «implements» ├── ├── Dashboard
├── «implements» ├── ├── ├── HomeDashboard
├── «implements» ├── DeviceCredentials
├── «implements» ├── DeviceProfile
├── «implements» ├── EdgeEvent
├── «implements» ├── Event
├── «implements» ├── ├── ErrorEvent
├── «implements» ├── ├── LifecycleEvent
├── «implements» ├── ├── RuleChainDebugEvent
├── «implements» ├── ├── RuleNodeDebugEvent
├── «implements» ├── ├── StatisticsEvent
├── «implements» ├── EventInfo
├── «implements» ├── Notification
├── «implements» ├── NotificationRequest
├── «implements» ├── ├── NotificationRequestInfo
├── «implements» ├── NotificationRule
├── «implements» ├── ├── NotificationRuleInfo
├── «implements» ├── NotificationTarget
├── «implements» ├── NotificationTemplate
├── «implements» ├── OAuth2Domain
├── «implements» ├── OAuth2Mobile
├── «implements» ├── OAuth2Params
├── «implements» ├── Rpc
├── «implements» ├── RuleNodeState
├── «implements» ├── TbResourceInfo
├── «implements» ├── ├── TbResource
├── «implements» ├── TenantProfile
├── «implements» ├── UserAuthSettings
├── «implements» ├── UserCredentials
├── «implements» ├── WidgetsBundle
├── «implements» ClearRule
├── CoapDeviceTypeConfiguration
├── ├── «implements» DefaultCoapDeviceTypeConfiguration
├── ├── «implements» EfentoCoapDeviceTypeConfiguration
├── «implements» ComponentLifecycleEvent
├── «implements» CustomTimeScheduleItem
├── «implements» DefaultRuleChainCreateRequest
├── DeviceConfiguration
├── ├── «implements» DefaultDeviceConfiguration
├── «implements» DeviceData
├── «implements» DeviceIdInfo
├── «implements» DeviceProfileAlarm
├── DeviceProfileConfiguration
├── ├── «implements» DefaultDeviceProfileConfiguration
├── «implements» DeviceProfileData
├── DeviceProfileProvisionConfiguration
├── ├── «implements» AllowCreateNewDevicesDeviceProfileProvisionConfiguration
├── ├── «implements» CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration
├── ├── «implements» DisabledDeviceProfileProvisionConfiguration
├── ├── «implements» X509CertificateChainProvisionConfiguration
├── DeviceProfileTransportConfiguration
├── ├── «implements» CoapDeviceProfileTransportConfiguration
├── ├── «implements» DefaultDeviceProfileTransportConfiguration
├── ├── «implements» Lwm2mDeviceProfileTransportConfiguration
├── ├── «implements» MqttDeviceProfileTransportConfiguration
├── ├── «implements» SnmpDeviceProfileTransportConfiguration
├── DeviceTransportConfiguration
├── ├── «implements» CoapDeviceTransportConfiguration
├── ├── «implements» DefaultDeviceTransportConfiguration
├── ├── «implements» Lwm2mDeviceTransportConfiguration
├── ├── «implements» MqttDeviceTransportConfiguration
├── ├── «implements» SnmpDeviceTransportConfiguration
├── «implements» DynamicValue
├── «implements» EdgeUpgradeMessage
├── EntityId
├── ├── «implements» AlarmId
├── ├── «implements» ApiUsageStateId
├── ├── «implements» AssetId
├── ├── «implements» AssetProfileId
├── ├── «implements» CustomerId
├── ├── «implements» DashboardId
├── ├── «implements» DeviceId
├── ├── «implements» DeviceProfileId
├── ├── «implements» EdgeId
├── ├── «implements» EntityViewId
├── ├── «implements» NotificationId
├── ├── «implements» NotificationRequestId
├── ├── «implements» NotificationRuleId
├── ├── «implements» NotificationTargetId
├── ├── «implements» NotificationTemplateId
├── ├── «implements» OtaPackageId
├── ├── «implements» QueueId
├── ├── «implements» RpcId
├── ├── «implements» RuleChainId
├── ├── «implements» RuleNodeId
├── ├── «implements» TbResourceId
├── ├── «implements» TenantId
├── ├── «implements» TenantProfileId
├── ├── «implements» UserId
├── ├── «implements» WidgetTypeId
├── ├── «implements» WidgetsBundleId
├── «implements» EntityKey
├── «implements» EntityLoadError
├── «implements» EntityRelation
├── «implements» ├── EntityRelationInfo
├── «implements» EntitySubtype
├── «implements» EntityTypeLoadResult
├── «implements» EntityVersion
├── «implements» FilterPredicateValue
├── HasId
├── ├── «implements» EntityInfo
├── ├── «implements» ├── AssetProfileInfo
├── ├── «implements» ├── DeviceProfileInfo
├── ├── «implements» UserEmailInfo
├── JwtToken
├── «implements» KeyFilter
├── KeyFilterPredicate
├── ├── «implements» ComplexFilterPredicate
├── ├── SimpleKeyFilterPredicate
├── ├── ├── «implements» BooleanFilterPredicate
├── ├── ├── «implements» NumericFilterPredicate
├── ├── ├── «implements» StringFilterPredicate
├── KvEntry
├── ├── AttributeKvEntry
├── ├── ├── «implements» BaseAttributeKvEntry
├── ├── «implements» BasicKvEntry
├── ├── «implements» ├── BooleanDataEntry
├── ├── «implements» ├── DoubleDataEntry
├── ├── «implements» ├── JsonDataEntry
├── ├── «implements» ├── LongDataEntry
├── ├── «implements» ├── StringDataEntry
├── ├── TsKvEntry
├── ├── ├── «implements» BasicTsKvEntry
├── ├── ├── «implements» ├── AggTsKvEntry
├── «implements» LastVisitedDashboardInfo
├── LwM2MBootstrapServerCredential
├── ├── «implements» AbstractLwM2MBootstrapServerCredential
├── ├── «implements» ├── NoSecLwM2MBootstrapServerCredential
├── ├── «implements» ├── PSKLwM2MBootstrapServerCredential
├── ├── «implements» ├── RPKLwM2MBootstrapServerCredential
├── ├── «implements» ├── X509LwM2MBootstrapServerCredential
├── NotificationDeliveryMethodConfig
├── ├── «implements» MobileAppNotificationDeliveryMethodConfig
├── ├── «implements» SlackNotificationDeliveryMethodConfig
├── «implements» NotificationRule
├── «implements» ├── NotificationRuleInfo
├── «implements» NotificationRuleConfig
├── «implements» NotificationRuleRecipientsConfig
├── «implements» ├── DefaultNotificationRuleRecipientsConfig
├── «implements» ├── EscalatedNotificationRuleRecipientsConfig
├── NotificationRuleTrigger
├── ├── «implements» AlarmAssignmentTrigger
├── ├── «implements» AlarmCommentTrigger
├── ├── «implements» AlarmTrigger
├── ├── «implements» ApiUsageLimitTrigger
├── ├── «implements» DeviceActivityTrigger
├── ├── «implements» EdgeCommunicationFailureTrigger
├── ├── «implements» EdgeConnectionTrigger
├── ├── «implements» EntitiesLimitTrigger
├── ├── «implements» EntityActionTrigger
├── ├── «implements» NewPlatformVersionTrigger
├── ├── «implements» RateLimitsTrigger
├── ├── «implements» RuleEngineComponentLifecycleEventTrigger
├── NotificationRuleTriggerConfig
├── ├── «implements» AlarmAssignmentNotificationRuleTriggerConfig
├── ├── «implements» AlarmCommentNotificationRuleTriggerConfig
├── ├── «implements» AlarmNotificationRuleTriggerConfig
├── ├── «implements» ApiUsageLimitNotificationRuleTriggerConfig
├── ├── «implements» DeviceActivityNotificationRuleTriggerConfig
├── ├── «implements» EdgeCommunicationFailureNotificationRuleTriggerConfig
├── ├── «implements» EdgeConnectionNotificationRuleTriggerConfig
├── ├── «implements» EntitiesLimitNotificationRuleTriggerConfig
├── ├── «implements» EntityActionNotificationRuleTriggerConfig
├── ├── «implements» NewPlatformVersionNotificationRuleTriggerConfig
├── ├── «implements» RateLimitsNotificationRuleTriggerConfig
├── ├── «implements» RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig
├── «implements» NotificationSettings
├── «implements» ObjectAttributes
├── «implements» PageData
├── «implements» PowerSavingConfiguration
├── «implements» ├── CoapDeviceTransportConfiguration
├── «implements» ├── Lwm2mDeviceTransportConfiguration
├── «implements» ├── OtherConfiguration
├── «implements» ProcessingStrategy
├── «implements» RepositorySettings
├── «implements» SecuritySettings
├── SnmpCommunicationConfig
├── ├── «implements» MultipleMappingsSnmpCommunicationConfig
├── ├── «implements» ├── RepeatingQueryingSnmpCommunicationConfig
├── ├── «implements» ├── ├── ClientAttributesQueryingSnmpCommunicationConfig
├── ├── «implements» ├── ├── TelemetryQueryingSnmpCommunicationConfig
├── ├── «implements» ├── SharedAttributesSettingSnmpCommunicationConfig
├── ├── «implements» ├── ToDeviceRpcRequestSnmpCommunicationConfig
├── ├── «implements» ├── ToServerRpcRequestSnmpCommunicationConfig
├── «implements» SnmpMapping
├── «implements» StarredDashboardInfo
├── «implements» SubmitStrategy
├── «implements» TelemetryEntityView
├── «implements» TelemetryMappingConfiguration
├── TenantProfileConfiguration
├── ├── «implements» DefaultTenantProfileConfiguration
├── «implements» TenantProfileData
├── «implements» TenantProfileQueueConfiguration
├── «implements» ToDeviceRpcRequestBody
├── TransportPayloadTypeConfiguration
├── ├── «implements» JsonTransportPayloadConfiguration
├── ├── «implements» ProtoTransportPayloadConfiguration
├── «implements» TwoFaAccountConfig
├── «implements» ├── BackupCodeTwoFaAccountConfig
├── «implements» ├── OtpBasedTwoFaAccountConfig
├── «implements» ├── ├── EmailTwoFaAccountConfig
├── «implements» ├── ├── SmsTwoFaAccountConfig
├── «implements» ├── TotpTwoFaAccountConfig
├── «implements» UUIDBased
├── «implements» ├── AdminSettingsId
├── «implements» ├── AlarmCommentId
├── «implements» ├── AlarmId
├── «implements» ├── ApiUsageStateId
├── «implements» ├── AssetId
├── «implements» ├── AssetProfileId
├── «implements» ├── AuditLogId
├── «implements» ├── ComponentDescriptorId
├── «implements» ├── CustomerId
├── «implements» ├── DashboardId
├── «implements» ├── DeviceCredentialsId
├── «implements» ├── DeviceId
├── «implements» ├── DeviceProfileId
├── «implements» ├── EdgeEventId
├── «implements» ├── EdgeId
├── «implements» ├── EntityViewId
├── «implements» ├── EventId
├── «implements» ├── NodeId
├── «implements» ├── NotificationId
├── «implements» ├── NotificationRequestId
├── «implements» ├── NotificationRuleId
├── «implements» ├── NotificationTargetId
├── «implements» ├── NotificationTemplateId
├── «implements» ├── OAuth2ClientRegistrationTemplateId
├── «implements» ├── OAuth2DomainId
├── «implements» ├── OAuth2MobileId
├── «implements» ├── OAuth2ParamsId
├── «implements» ├── OAuth2RegistrationId
├── «implements» ├── OtaPackageId
├── «implements» ├── QueueId
├── «implements» ├── RpcId
├── «implements» ├── RuleChainId
├── «implements» ├── RuleNodeId
├── «implements» ├── RuleNodeStateId
├── «implements» ├── TbResourceId
├── «implements» ├── TenantId
├── «implements» ├── TenantProfileId
├── «implements» ├── UserAuthSettingsId
├── «implements» ├── UserCredentialsId
├── «implements» ├── UserId
├── «implements» ├── WidgetTypeId
├── «implements» ├── WidgetsBundleId
├── «implements» UpdateMessage
├── «implements» UserAuthDataChangedEvent
├── «implements» ├── UserCredentialsInvalidationEvent
├── «implements» ├── UserSessionInvalidationEvent
├── «implements» UserDashboardsInfo
├── «implements» UserPasswordPolicy
├── «implements» UserSettings
├── «implements» UserSettingsCompositeKey
├── «implements» VersionCreateConfig
├── «implements» ├── AutoVersionCreateConfig
├── «implements» ├── EntityTypeVersionCreateConfig
├── «implements» VersionCreationResult
├── «implements» VersionLoadResult
SmsProviderConfiguration
├── «implements» AwsSnsSmsProviderConfiguration
├── «implements» SmppSmsProviderConfiguration
├── «implements» TwilioSmsProviderConfiguration
TsKvQuery
├── «implements» BaseTsKvQuery
├── «implements» ├── BaseDeleteTsKvQuery
├── «implements» ├── BaseReadTsKvQuery
├── DeleteTsKvQuery
├── ├── «implements» BaseDeleteTsKvQuery
├── ReadTsKvQuery
├── ├── «implements» BaseReadTsKvQuery
TwoFaProviderConfig
├── «implements» BackupCodeTwoFaProviderConfig
├── «implements» OtpBasedTwoFaProviderConfig
├── «implements» ├── EmailTwoFaProviderConfig
├── «implements» ├── SmsTwoFaProviderConfig
├── «implements» TotpTwoFaProviderConfig
UsersFilter
├── «implements» AffectedTenantAdministratorsFilter
├── «implements» AffectedUserFilter
├── «implements» AllUsersFilter
├── «implements» CustomerUsersFilter
├── «implements» OriginatorEntityOwnerUsersFilter
├── «implements» SystemAdministratorsFilter
├── «implements» TenantAdministratorsFilter
├── «implements» UserListFilter
```

## 继承图

```mermaid
flowchart TD
    BaseData_p0["BaseData"] -->|extends| AdminSettings_c0["AdminSettings"]
    HasTenantId_p1["HasTenantId"] -->|implements| AdminSettings_c1["AdminSettings"]
    Object______p2["Object/外部框架"] -->|extends| ApiUsageRecordState_c2["ApiUsageRecordState"]
    Serializable_p3["Serializable"] -->|implements| ApiUsageRecordState_c3["ApiUsageRecordState"]
    BaseData_p4["BaseData"] -->|extends| ApiUsageState_c4["ApiUsageState"]
    HasTenantId_p5["HasTenantId"] -->|implements| ApiUsageState_c5["ApiUsageState"]
    Object______p6["Object/外部框架"] -->|extends| BaseData_c6["BaseData"]
    Serializable_p7["Serializable"] -->|implements| BaseData_c7["BaseData"]
    Object______p8["Object/外部框架"] -->|extends| BaseDataWithAdditionalInfo_c8["BaseDataWithAdditionalInfo"]
    HasAdditionalInfo_p9["HasAdditionalInfo"] -->|implements| BaseDataWithAdditionalInfo_c9["BaseDataWithAdditionalInfo"]
    Object______p10["Object/外部框架"] -->|extends| CacheConstants_c10["CacheConstants"]
    Object______p11["Object/外部框架"] -->|extends| ClaimRequest_c11["ClaimRequest"]
    Object______p12["Object/外部框架"] -->|extends| ContactBased_c12["ContactBased"]
    HasEmail_p13["HasEmail"] -->|implements| ContactBased_c13["ContactBased"]
    ContactBased_p14["ContactBased"] -->|extends| Customer_c14["Customer"]
    HasTenantId_p15["HasTenantId"] -->|implements| Customer_c15["Customer"]
    ExportableEntity_p16["ExportableEntity"] -->|implements| Customer_c16["Customer"]
    HasTitle_p17["HasTitle"] -->|implements| Customer_c17["Customer"]
    DashboardInfo_p18["DashboardInfo"] -->|extends| Dashboard_c18["Dashboard"]
    ExportableEntity_p19["ExportableEntity"] -->|implements| Dashboard_c19["Dashboard"]
    BaseData_p20["BaseData"] -->|extends| DashboardInfo_c20["DashboardInfo"]
    HasName_p21["HasName"] -->|implements| DashboardInfo_c21["DashboardInfo"]
    HasTenantId_p22["HasTenantId"] -->|implements| DashboardInfo_c22["DashboardInfo"]
    HasTitle_p23["HasTitle"] -->|implements| DashboardInfo_c23["DashboardInfo"]
    HasImage_p24["HasImage"] -->|implements| DashboardInfo_c24["DashboardInfo"]
    Object______p25["Object/外部框架"] -->|extends| DataConstants_c25["DataConstants"]
    BaseDataWithAdditionalInfo_p26["BaseDataWithAdditionalInfo"] -->|extends| Device_c26["Device"]
    HasLabel_p27["HasLabel"] -->|implements| Device_c27["Device"]
    HasTenantId_p28["HasTenantId"] -->|implements| Device_c28["Device"]
    HasCustomerId_p29["HasCustomerId"] -->|implements| Device_c29["Device"]
    HasOtaPackage_p30["HasOtaPackage"] -->|implements| Device_c30["Device"]
    ExportableEntity_p31["ExportableEntity"] -->|implements| Device_c31["Device"]
    Object______p32["Object/外部框架"] -->|extends| DeviceIdInfo_c32["DeviceIdInfo"]
    Serializable_p33["Serializable"] -->|implements| DeviceIdInfo_c33["DeviceIdInfo"]
    HasTenantId_p34["HasTenantId"] -->|implements| DeviceIdInfo_c34["DeviceIdInfo"]
    Device_p35["Device"] -->|extends| DeviceInfo_c35["DeviceInfo"]
    Object______p36["Object/外部框架"] -->|extends| DeviceInfoFilter_c36["DeviceInfoFilter"]
    BaseData_p37["BaseData"] -->|extends| DeviceProfile_c37["DeviceProfile"]
    HasName_p38["HasName"] -->|implements| DeviceProfile_c38["DeviceProfile"]
    HasTenantId_p39["HasTenantId"] -->|implements| DeviceProfile_c39["DeviceProfile"]
    HasOtaPackage_p40["HasOtaPackage"] -->|implements| DeviceProfile_c40["DeviceProfile"]
    HasRuleEngineProfile_p41["HasRuleEngineProfile"] -->|implements| DeviceProfile_c41["DeviceProfile"]
    ExportableEntity_p42["ExportableEntity"] -->|implements| DeviceProfile_c42["DeviceProfile"]
    HasImage_p43["HasImage"] -->|implements| DeviceProfile_c43["DeviceProfile"]
    EntityInfo_p44["EntityInfo"] -->|extends| DeviceProfileInfo_c44["DeviceProfileInfo"]
    Object______p45["Object/外部框架"] -->|extends| DynamicProtoUtils_c45["DynamicProtoUtils"]
    Object______p46["Object/外部框架"] -->|extends| EdgeUpgradeInfo_c46["EdgeUpgradeInfo"]
    Object______p47["Object/外部框架"] -->|extends| EdgeUpgradeMessage_c47["EdgeUpgradeMessage"]
    Serializable_p48["Serializable"] -->|implements| EdgeUpgradeMessage_c48["EdgeUpgradeMessage"]
    Object______p49["Object/外部框架"] -->|extends| EdgeUtils_c49["EdgeUtils"]
    Object______p50["Object/外部框架"] -->|extends| EntityFieldsData_c50["EntityFieldsData"]
    JsonSerializer_p51["JsonSerializer"] -->|extends| EntityIdFieldSerializer_c51["EntityIdFieldSerializer"]
    Object______p52["Object/外部框架"] -->|extends| EntityInfo_c52["EntityInfo"]
    HasId_p53["HasId"] -->|implements| EntityInfo_c53["EntityInfo"]
    HasName_p54["HasName"] -->|implements| EntityInfo_c54["EntityInfo"]
    Object______p55["Object/外部框架"] -->|extends| EntitySubtype_c55["EntitySubtype"]
    Serializable_p56["Serializable"] -->|implements| EntitySubtype_c56["EntitySubtype"]
    BaseDataWithAdditionalInfo_p57["BaseDataWithAdditionalInfo"] -->|extends| EntityView_c57["EntityView"]
    HasName_p58["HasName"] -->|implements| EntityView_c58["EntityView"]
    HasTenantId_p59["HasTenantId"] -->|implements| EntityView_c59["EntityView"]
    HasCustomerId_p60["HasCustomerId"] -->|implements| EntityView_c60["EntityView"]
    ExportableEntity_p61["ExportableEntity"] -->|implements| EntityView_c61["EntityView"]
    EntityView_p62["EntityView"] -->|extends| EntityViewInfo_c62["EntityViewInfo"]
    BaseData_p63["BaseData"] -->|extends| EventInfo_c63["EventInfo"]
    Object______p64["Object/外部框架"] -->|extends| FSTUtils_c64["FSTUtils"]
    Object______p65["Object/外部框架"] -->|extends| FeaturesInfo_c65["FeaturesInfo"]
    HasName_p66["HasName"] -->|extends| HasEmail_c66["HasEmail"]
    HasTenantId_p67["HasTenantId"] -->|extends| HasImage_c67["HasImage"]
    HasName_p68["HasName"] -->|extends| HasImage_c68["HasImage"]
    HasName_p69["HasName"] -->|extends| HasLabel_c69["HasLabel"]
    Dashboard_p70["Dashboard"] -->|extends| HomeDashboard_c70["HomeDashboard"]
    Object______p71["Object/外部框架"] -->|extends| HomeDashboardInfo_c71["HomeDashboardInfo"]
    Object______p72["Object/外部框架"] -->|extends| ImageDescriptor_c72["ImageDescriptor"]
    Object______p73["Object/外部框架"] -->|extends| ImageExportData_c73["ImageExportData"]
    OtaPackageInfo_p74["OtaPackageInfo"] -->|extends| OtaPackage_c74["OtaPackage"]
    BaseDataWithAdditionalInfo_p75["BaseDataWithAdditionalInfo"] -->|extends| OtaPackageInfo_c75["OtaPackageInfo"]
    HasName_p76["HasName"] -->|implements| OtaPackageInfo_c76["OtaPackageInfo"]
    HasTenantId_p77["HasTenantId"] -->|implements| OtaPackageInfo_c77["OtaPackageInfo"]
    HasTitle_p78["HasTitle"] -->|implements| OtaPackageInfo_c78["OtaPackageInfo"]
    Object______p79["Object/外部框架"] -->|extends| ResourceUtils_c79["ResourceUtils"]
    Object______p80["Object/外部框架"] -->|extends| path_c80["path"]
    Object______p81["Object/外部框架"] -->|extends| SaveDeviceWithCredentialsRequest_c81["SaveDeviceWithCredentialsRequest"]
    OtaPackageInfo_p82["OtaPackageInfo"] -->|extends| SaveOtaPackageInfoRequest_c82["SaveOtaPackageInfoRequest"]
    Object______p83["Object/外部框架"] -->|extends| ShortCustomerInfo_c83["ShortCustomerInfo"]
    Object______p84["Object/外部框架"] -->|extends| StringUtils_c84["StringUtils"]
    Object______p85["Object/外部框架"] -->|extends| SystemInfo_c85["SystemInfo"]
    Object______p86["Object/外部框架"] -->|extends| SystemInfoData_c86["SystemInfoData"]
    Object______p87["Object/外部框架"] -->|extends| SystemParams_c87["SystemParams"]
    Object______p88["Object/外部框架"] -->|extends| TbImageDeleteResult_c88["TbImageDeleteResult"]
    Object______p89["Object/外部框架"] -->|extends| TbProperty_c89["TbProperty"]
    TbResourceInfo_p90["TbResourceInfo"] -->|extends| TbResource_c90["TbResource"]
    BaseData_p91["BaseData"] -->|extends| TbResourceInfo_c91["TbResourceInfo"]
    HasName_p92["HasName"] -->|implements| TbResourceInfo_c92["TbResourceInfo"]
    HasTenantId_p93["HasTenantId"] -->|implements| TbResourceInfo_c93["TbResourceInfo"]
    ExportableEntity_p94["ExportableEntity"] -->|implements| TbResourceInfo_c94["TbResourceInfo"]
    Object______p95["Object/外部框架"] -->|extends| TbResourceInfoFilter_c95["TbResourceInfoFilter"]
    ContactBased_p96["ContactBased"] -->|extends| Tenant_c96["Tenant"]
    HasTenantId_p97["HasTenantId"] -->|implements| Tenant_c97["Tenant"]
    HasTitle_p98["HasTitle"] -->|implements| Tenant_c98["Tenant"]
    Tenant_p99["Tenant"] -->|extends| TenantInfo_c99["TenantInfo"]
    BaseData_p100["BaseData"] -->|extends| TenantProfile_c100["TenantProfile"]
    HasName_p101["HasName"] -->|implements| TenantProfile_c101["TenantProfile"]
    Object______p102["Object/外部框架"] -->|extends| UUIDConverter_c102["UUIDConverter"]
    Object______p103["Object/外部框架"] -->|extends| UpdateMessage_c103["UpdateMessage"]
    Serializable_p104["Serializable"] -->|implements| UpdateMessage_c104["UpdateMessage"]
    Object______p105["Object/外部框架"] -->|extends| UsageInfo_c105["UsageInfo"]
    BaseDataWithAdditionalInfo_p106["BaseDataWithAdditionalInfo"] -->|extends| User_c106["User"]
    HasName_p107["HasName"] -->|implements| User_c107["User"]
    HasTenantId_p108["HasTenantId"] -->|implements| User_c108["User"]
    HasCustomerId_p109["HasCustomerId"] -->|implements| User_c109["User"]
    NotificationRecipient_p110["NotificationRecipient"] -->|implements| User_c110["User"]
    Object______p111["Object/外部框架"] -->|extends| UserEmailInfo_c111["UserEmailInfo"]
    HasId_p112["HasId"] -->|implements| UserEmailInfo_c112["UserEmailInfo"]
    BaseData_p113["BaseData"] -->|extends| Alarm_c113["Alarm"]
    HasName_p114["HasName"] -->|implements| Alarm_c114["Alarm"]
    HasTenantId_p115["HasTenantId"] -->|implements| Alarm_c115["Alarm"]
    HasCustomerId_p116["HasCustomerId"] -->|implements| Alarm_c116["Alarm"]
    Object______p117["Object/外部框架"] -->|extends| AlarmApiCallResult_c117["AlarmApiCallResult"]
    Serializable_p118["Serializable"] -->|implements| AlarmApiCallResult_c118["AlarmApiCallResult"]
    Object______p119["Object/外部框架"] -->|extends| AlarmAssignee_c119["AlarmAssignee"]
    Serializable_p120["Serializable"] -->|implements| AlarmAssignee_c120["AlarmAssignee"]
    Object______p121["Object/外部框架"] -->|extends| AlarmAssigneeUpdate_c121["AlarmAssigneeUpdate"]
    Serializable_p122["Serializable"] -->|implements| AlarmAssigneeUpdate_c122["AlarmAssigneeUpdate"]
    BaseData_p123["BaseData"] -->|extends| AlarmComment_c123["AlarmComment"]
    HasName_p124["HasName"] -->|implements| AlarmComment_c124["AlarmComment"]
    AlarmComment_p125["AlarmComment"] -->|extends| AlarmCommentInfo_c125["AlarmCommentInfo"]
    Object______p126["Object/外部框架"] -->|extends| AlarmCreateOrUpdateActiveRequest_c126["AlarmCreateOrUpdateActiveRequest"]
    AlarmModificationRequest_p127["AlarmModificationRequest"] -->|implements| AlarmCreateOrUpdateActiveRequest_c127["AlarmCreateOrUpdateActiveRequest"]
    Alarm_p128["Alarm"] -->|extends| AlarmInfo_c128["AlarmInfo"]
    Object______p129["Object/外部框架"] -->|extends| AlarmPropagationInfo_c129["AlarmPropagationInfo"]
    Object______p130["Object/外部框架"] -->|extends| AlarmQuery_c130["AlarmQuery"]
    Object______p131["Object/外部框架"] -->|extends| AlarmQueryV2_c131["AlarmQueryV2"]
    Object______p132["Object/外部框架"] -->|extends| AlarmStatusFilter_c132["AlarmStatusFilter"]
    Object______p133["Object/外部框架"] -->|extends| AlarmUpdateRequest_c133["AlarmUpdateRequest"]
    AlarmModificationRequest_p134["AlarmModificationRequest"] -->|implements| AlarmUpdateRequest_c134["AlarmUpdateRequest"]
    Object______p135["Object/外部框架"] -->|extends| EntityAlarm_c135["EntityAlarm"]
    HasTenantId_p136["HasTenantId"] -->|implements| EntityAlarm_c136["EntityAlarm"]
    BaseDataWithAdditionalInfo_p137["BaseDataWithAdditionalInfo"] -->|extends| Asset_c137["Asset"]
    HasLabel_p138["HasLabel"] -->|implements| Asset_c138["Asset"]
    HasTenantId_p139["HasTenantId"] -->|implements| Asset_c139["Asset"]
    HasCustomerId_p140["HasCustomerId"] -->|implements| Asset_c140["Asset"]
    ExportableEntity_p141["ExportableEntity"] -->|implements| Asset_c141["Asset"]
    Asset_p142["Asset"] -->|extends| AssetInfo_c142["AssetInfo"]
    BaseData_p143["BaseData"] -->|extends| AssetProfile_c143["AssetProfile"]
    HasName_p144["HasName"] -->|implements| AssetProfile_c144["AssetProfile"]
    HasTenantId_p145["HasTenantId"] -->|implements| AssetProfile_c145["AssetProfile"]
    HasRuleEngineProfile_p146["HasRuleEngineProfile"] -->|implements| AssetProfile_c146["AssetProfile"]
    ExportableEntity_p147["ExportableEntity"] -->|implements| AssetProfile_c147["AssetProfile"]
    HasImage_p148["HasImage"] -->|implements| AssetProfile_c148["AssetProfile"]
    EntityInfo_p149["EntityInfo"] -->|extends| AssetProfileInfo_c149["AssetProfileInfo"]
    Object______p150["Object/外部框架"] -->|extends| AssetSearchQuery_c150["AssetSearchQuery"]
    BaseData_p151["BaseData"] -->|extends| AuditLog_c151["AuditLog"]
    Object______p152["Object/外部框架"] -->|extends| DeviceSearchQuery_c152["DeviceSearchQuery"]
    Object______p153["Object/外部框架"] -->|extends| BasicMqttCredentials_c153["BasicMqttCredentials"]
    Object______p154["Object/外部框架"] -->|extends| ProvisionDeviceCredentialsData_c154["ProvisionDeviceCredentialsData"]
    Object______p155["Object/外部框架"] -->|extends| AbstractLwM2MBootstrapClientCredentialWithKeys_c155["AbstractLwM2MBootstrapClientCredentialWithKeys"]
    LwM2MBootstrapClientCredential_p156["LwM2MBootstrapClientCredential"] -->|implements| AbstractLwM2MBootstrapClientCredentialWithKeys_c156["AbstractLwM2MBootstrapClientCredentialWithKeys"]
    Object______p157["Object/外部框架"] -->|extends| AbstractLwM2MClientCredential_c157["AbstractLwM2MClientCredential"]
    LwM2MClientCredential_p158["LwM2MClientCredential"] -->|implements| AbstractLwM2MClientCredential_c158["AbstractLwM2MClientCredential"]
    AbstractLwM2MClientCredential_p159["AbstractLwM2MClientCredential"] -->|extends| AbstractLwM2MClientSecurityCredential_c159["AbstractLwM2MClientSecurityCredential"]
    Object______p160["Object/外部框架"] -->|extends| LwM2MBootstrapClientCredentials_c160["LwM2MBootstrapClientCredentials"]
    Object______p161["Object/外部框架"] -->|extends| LwM2MDeviceCredentials_c161["LwM2MDeviceCredentials"]
    Object______p162["Object/外部框架"] -->|extends| NoSecBootstrapClientCredential_c162["NoSecBootstrapClientCredential"]
    LwM2MBootstrapClientCredential_p163["LwM2MBootstrapClientCredential"] -->|implements| NoSecBootstrapClientCredential_c163["NoSecBootstrapClientCredential"]
    AbstractLwM2MClientCredential_p164["AbstractLwM2MClientCredential"] -->|extends| NoSecClientCredential_c164["NoSecClientCredential"]
    AbstractLwM2MBootstrapClientCredentialWithKeys_p165["AbstractLwM2MBootstrapClientCredentialWithKeys"] -->|extends| PSKBootstrapClientCredential_c165["PSKBootstrapClientCredential"]
    AbstractLwM2MClientSecurityCredential_p166["AbstractLwM2MClientSecurityCredential"] -->|extends| PSKClientCredential_c166["PSKClientCredential"]
    AbstractLwM2MBootstrapClientCredentialWithKeys_p167["AbstractLwM2MBootstrapClientCredentialWithKeys"] -->|extends| RPKBootstrapClientCredential_c167["RPKBootstrapClientCredential"]
    AbstractLwM2MClientSecurityCredential_p168["AbstractLwM2MClientSecurityCredential"] -->|extends| RPKClientCredential_c168["RPKClientCredential"]
    AbstractLwM2MBootstrapClientCredentialWithKeys_p169["AbstractLwM2MBootstrapClientCredentialWithKeys"] -->|extends| X509BootstrapClientCredential_c169["X509BootstrapClientCredential"]
    AbstractLwM2MClientSecurityCredential_p170["AbstractLwM2MClientSecurityCredential"] -->|extends| X509ClientCredential_c170["X509ClientCredential"]
    PowerSavingConfiguration_p171["PowerSavingConfiguration"] -->|extends| CoapDeviceTransportConfiguration_c171["CoapDeviceTransportConfiguration"]
    DeviceTransportConfiguration_p172["DeviceTransportConfiguration"] -->|implements| CoapDeviceTransportConfiguration_c172["CoapDeviceTransportConfiguration"]
    Object______p173["Object/外部框架"] -->|extends| DefaultDeviceConfiguration_c173["DefaultDeviceConfiguration"]
    DeviceConfiguration_p174["DeviceConfiguration"] -->|implements| DefaultDeviceConfiguration_c174["DefaultDeviceConfiguration"]
    Object______p175["Object/外部框架"] -->|extends| DefaultDeviceTransportConfiguration_c175["DefaultDeviceTransportConfiguration"]
    DeviceTransportConfiguration_p176["DeviceTransportConfiguration"] -->|implements| DefaultDeviceTransportConfiguration_c176["DefaultDeviceTransportConfiguration"]
    Serializable_p177["Serializable"] -->|extends| DeviceConfiguration_c177["DeviceConfiguration"]
    Object______p178["Object/外部框架"] -->|extends| DeviceData_c178["DeviceData"]
    Serializable_p179["Serializable"] -->|implements| DeviceData_c179["DeviceData"]
    Serializable_p180["Serializable"] -->|extends| DeviceTransportConfiguration_c180["DeviceTransportConfiguration"]
    PowerSavingConfiguration_p181["PowerSavingConfiguration"] -->|extends| Lwm2mDeviceTransportConfiguration_c181["Lwm2mDeviceTransportConfiguration"]
    DeviceTransportConfiguration_p182["DeviceTransportConfiguration"] -->|implements| Lwm2mDeviceTransportConfiguration_c182["Lwm2mDeviceTransportConfiguration"]
    Object______p183["Object/外部框架"] -->|extends| MqttDeviceTransportConfiguration_c183["MqttDeviceTransportConfiguration"]
    DeviceTransportConfiguration_p184["DeviceTransportConfiguration"] -->|implements| MqttDeviceTransportConfiguration_c184["MqttDeviceTransportConfiguration"]
    Object______p185["Object/外部框架"] -->|extends| PowerSavingConfiguration_c185["PowerSavingConfiguration"]
    Serializable_p186["Serializable"] -->|implements| PowerSavingConfiguration_c186["PowerSavingConfiguration"]
    Object______p187["Object/外部框架"] -->|extends| SnmpDeviceTransportConfiguration_c187["SnmpDeviceTransportConfiguration"]
    DeviceTransportConfiguration_p188["DeviceTransportConfiguration"] -->|implements| SnmpDeviceTransportConfiguration_c188["SnmpDeviceTransportConfiguration"]
    Object______p189["Object/外部框架"] -->|extends| AlarmCondition_c189["AlarmCondition"]
    Serializable_p190["Serializable"] -->|implements| AlarmCondition_c190["AlarmCondition"]
    Object______p191["Object/外部框架"] -->|extends| AlarmConditionFilter_c191["AlarmConditionFilter"]
    Serializable_p192["Serializable"] -->|implements| AlarmConditionFilter_c192["AlarmConditionFilter"]
    Object______p193["Object/外部框架"] -->|extends| AlarmConditionFilterKey_c193["AlarmConditionFilterKey"]
    Serializable_p194["Serializable"] -->|implements| AlarmConditionFilterKey_c194["AlarmConditionFilterKey"]
    Serializable_p195["Serializable"] -->|extends| AlarmConditionSpec_c195["AlarmConditionSpec"]
    Object______p196["Object/外部框架"] -->|extends| AlarmRule_c196["AlarmRule"]
    Serializable_p197["Serializable"] -->|implements| AlarmRule_c197["AlarmRule"]
    Serializable_p198["Serializable"] -->|extends| AlarmSchedule_c198["AlarmSchedule"]
    Object______p199["Object/外部框架"] -->|extends| AllowCreateNewDevicesDeviceProfileProvisionConfiguration_c199["AllowCreateNewDevicesDeviceProfileProvisionConfiguration"]
    DeviceProfileProvisionConfiguration_p200["DeviceProfileProvisionConfiguration"] -->|implements| AllowCreateNewDevicesDeviceProfileProvisionConfiguration_c200["AllowCreateNewDevicesDeviceProfileProvisionConfiguration"]
    Object______p201["Object/外部框架"] -->|extends| AnyTimeSchedule_c201["AnyTimeSchedule"]
    AlarmSchedule_p202["AlarmSchedule"] -->|implements| AnyTimeSchedule_c202["AnyTimeSchedule"]
    Object______p203["Object/外部框架"] -->|extends| CheckPreProvisionedDevicesDeviceProfileProvisionConfiguratio_c203["CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration"]
    DeviceProfileProvisionConfiguration_p204["DeviceProfileProvisionConfiguration"] -->|implements| CheckPreProvisionedDevicesDeviceProfileProvisionConfiguratio_c204["CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration"]
    Object______p205["Object/外部框架"] -->|extends| CoapDeviceProfileTransportConfiguration_c205["CoapDeviceProfileTransportConfiguration"]
    DeviceProfileTransportConfiguration_p206["DeviceProfileTransportConfiguration"] -->|implements| CoapDeviceProfileTransportConfiguration_c206["CoapDeviceProfileTransportConfiguration"]
    Serializable_p207["Serializable"] -->|extends| CoapDeviceTypeConfiguration_c207["CoapDeviceTypeConfiguration"]
    Object______p208["Object/外部框架"] -->|extends| CustomTimeSchedule_c208["CustomTimeSchedule"]
    AlarmSchedule_p209["AlarmSchedule"] -->|implements| CustomTimeSchedule_c209["CustomTimeSchedule"]
    Object______p210["Object/外部框架"] -->|extends| CustomTimeScheduleItem_c210["CustomTimeScheduleItem"]
    Serializable_p211["Serializable"] -->|implements| CustomTimeScheduleItem_c211["CustomTimeScheduleItem"]
    Object______p212["Object/外部框架"] -->|extends| DefaultCoapDeviceTypeConfiguration_c212["DefaultCoapDeviceTypeConfiguration"]
    CoapDeviceTypeConfiguration_p213["CoapDeviceTypeConfiguration"] -->|implements| DefaultCoapDeviceTypeConfiguration_c213["DefaultCoapDeviceTypeConfiguration"]
    Object______p214["Object/外部框架"] -->|extends| DefaultDeviceProfileConfiguration_c214["DefaultDeviceProfileConfiguration"]
    DeviceProfileConfiguration_p215["DeviceProfileConfiguration"] -->|implements| DefaultDeviceProfileConfiguration_c215["DefaultDeviceProfileConfiguration"]
    Object______p216["Object/外部框架"] -->|extends| DefaultDeviceProfileTransportConfiguration_c216["DefaultDeviceProfileTransportConfiguration"]
    DeviceProfileTransportConfiguration_p217["DeviceProfileTransportConfiguration"] -->|implements| DefaultDeviceProfileTransportConfiguration_c217["DefaultDeviceProfileTransportConfiguration"]
    Object______p218["Object/外部框架"] -->|extends| DeviceProfileAlarm_c218["DeviceProfileAlarm"]
    Serializable_p219["Serializable"] -->|implements| DeviceProfileAlarm_c219["DeviceProfileAlarm"]
    Serializable_p220["Serializable"] -->|extends| DeviceProfileConfiguration_c220["DeviceProfileConfiguration"]
    Object______p221["Object/外部框架"] -->|extends| DeviceProfileData_c221["DeviceProfileData"]
    Serializable_p222["Serializable"] -->|implements| DeviceProfileData_c222["DeviceProfileData"]
    Serializable_p223["Serializable"] -->|extends| DeviceProfileProvisionConfiguration_c223["DeviceProfileProvisionConfiguration"]
    Serializable_p224["Serializable"] -->|extends| DeviceProfileTransportConfiguration_c224["DeviceProfileTransportConfiguration"]
    Object______p225["Object/外部框架"] -->|extends| DisabledDeviceProfileProvisionConfiguration_c225["DisabledDeviceProfileProvisionConfiguration"]
    DeviceProfileProvisionConfiguration_p226["DeviceProfileProvisionConfiguration"] -->|implements| DisabledDeviceProfileProvisionConfiguration_c226["DisabledDeviceProfileProvisionConfiguration"]
    Object______p227["Object/外部框架"] -->|extends| DurationAlarmConditionSpec_c227["DurationAlarmConditionSpec"]
    AlarmConditionSpec_p228["AlarmConditionSpec"] -->|implements| DurationAlarmConditionSpec_c228["DurationAlarmConditionSpec"]
    Object______p229["Object/外部框架"] -->|extends| EfentoCoapDeviceTypeConfiguration_c229["EfentoCoapDeviceTypeConfiguration"]
    CoapDeviceTypeConfiguration_p230["CoapDeviceTypeConfiguration"] -->|implements| EfentoCoapDeviceTypeConfiguration_c230["EfentoCoapDeviceTypeConfiguration"]
    Object______p231["Object/外部框架"] -->|extends| JsonTransportPayloadConfiguration_c231["JsonTransportPayloadConfiguration"]
    TransportPayloadTypeConfiguration_p232["TransportPayloadTypeConfiguration"] -->|implements| JsonTransportPayloadConfiguration_c232["JsonTransportPayloadConfiguration"]
    Object______p233["Object/外部框架"] -->|extends| Lwm2mDeviceProfileTransportConfiguration_c233["Lwm2mDeviceProfileTransportConfiguration"]
    DeviceProfileTransportConfiguration_p234["DeviceProfileTransportConfiguration"] -->|implements| Lwm2mDeviceProfileTransportConfiguration_c234["Lwm2mDeviceProfileTransportConfiguration"]
    Object______p235["Object/外部框架"] -->|extends| MqttDeviceProfileTransportConfiguration_c235["MqttDeviceProfileTransportConfiguration"]
    DeviceProfileTransportConfiguration_p236["DeviceProfileTransportConfiguration"] -->|implements| MqttDeviceProfileTransportConfiguration_c236["MqttDeviceProfileTransportConfiguration"]
    Object______p237["Object/外部框架"] -->|extends| MqttTopics_c237["MqttTopics"]
    Object______p238["Object/外部框架"] -->|extends| ProtoTransportPayloadConfiguration_c238["ProtoTransportPayloadConfiguration"]
    TransportPayloadTypeConfiguration_p239["TransportPayloadTypeConfiguration"] -->|implements| ProtoTransportPayloadConfiguration_c239["ProtoTransportPayloadConfiguration"]
    Object______p240["Object/外部框架"] -->|extends| ProvisionDeviceProfileCredentials_c240["ProvisionDeviceProfileCredentials"]
    Object______p241["Object/外部框架"] -->|extends| RepeatingAlarmConditionSpec_c241["RepeatingAlarmConditionSpec"]
    AlarmConditionSpec_p242["AlarmConditionSpec"] -->|implements| RepeatingAlarmConditionSpec_c242["RepeatingAlarmConditionSpec"]
    Object______p243["Object/外部框架"] -->|extends| SimpleAlarmConditionSpec_c243["SimpleAlarmConditionSpec"]
    AlarmConditionSpec_p244["AlarmConditionSpec"] -->|implements| SimpleAlarmConditionSpec_c244["SimpleAlarmConditionSpec"]
    Object______p245["Object/外部框架"] -->|extends| SnmpDeviceProfileTransportConfiguration_c245["SnmpDeviceProfileTransportConfiguration"]
    DeviceProfileTransportConfiguration_p246["DeviceProfileTransportConfiguration"] -->|implements| SnmpDeviceProfileTransportConfiguration_c246["SnmpDeviceProfileTransportConfiguration"]
    Object______p247["Object/外部框架"] -->|extends| SpecificTimeSchedule_c247["SpecificTimeSchedule"]
    AlarmSchedule_p248["AlarmSchedule"] -->|implements| SpecificTimeSchedule_c248["SpecificTimeSchedule"]
    Serializable_p249["Serializable"] -->|extends| TransportPayloadTypeConfiguration_c249["TransportPayloadTypeConfiguration"]
    Object______p250["Object/外部框架"] -->|extends| X509CertificateChainProvisionConfiguration_c250["X509CertificateChainProvisionConfiguration"]
    DeviceProfileProvisionConfiguration_p251["DeviceProfileProvisionConfiguration"] -->|implements| X509CertificateChainProvisionConfiguration_c251["X509CertificateChainProvisionConfiguration"]
    Object______p252["Object/外部框架"] -->|extends| ObjectAttributes_c252["ObjectAttributes"]
    Serializable_p253["Serializable"] -->|implements| ObjectAttributes_c253["ObjectAttributes"]
    PowerSavingConfiguration_p254["PowerSavingConfiguration"] -->|extends| OtherConfiguration_c254["OtherConfiguration"]
    Object______p255["Object/外部框架"] -->|extends| TelemetryMappingConfiguration_c255["TelemetryMappingConfiguration"]
    Serializable_p256["Serializable"] -->|implements| TelemetryMappingConfiguration_c256["TelemetryMappingConfiguration"]
    LwM2MServerSecurityConfig_p257["LwM2MServerSecurityConfig"] -->|extends| AbstractLwM2MBootstrapServerCredential_c257["AbstractLwM2MBootstrapServerCredential"]
    LwM2MBootstrapServerCredential_p258["LwM2MBootstrapServerCredential"] -->|implements| AbstractLwM2MBootstrapServerCredential_c258["AbstractLwM2MBootstrapServerCredential"]
    Serializable_p259["Serializable"] -->|extends| LwM2MBootstrapServerCredential_c259["LwM2MBootstrapServerCredential"]
    Object______p260["Object/外部框架"] -->|extends| LwM2MServerSecurityConfig_c260["LwM2MServerSecurityConfig"]
    LwM2MServerSecurityConfig_p261["LwM2MServerSecurityConfig"] -->|extends| LwM2MServerSecurityConfigDefault_c261["LwM2MServerSecurityConfigDefault"]
    AbstractLwM2MBootstrapServerCredential_p262["AbstractLwM2MBootstrapServerCredential"] -->|extends| NoSecLwM2MBootstrapServerCredential_c262["NoSecLwM2MBootstrapServerCredential"]
    AbstractLwM2MBootstrapServerCredential_p263["AbstractLwM2MBootstrapServerCredential"] -->|extends| PSKLwM2MBootstrapServerCredential_c263["PSKLwM2MBootstrapServerCredential"]
    AbstractLwM2MBootstrapServerCredential_p264["AbstractLwM2MBootstrapServerCredential"] -->|extends| RPKLwM2MBootstrapServerCredential_c264["RPKLwM2MBootstrapServerCredential"]
    AbstractLwM2MBootstrapServerCredential_p265["AbstractLwM2MBootstrapServerCredential"] -->|extends| X509LwM2MBootstrapServerCredential_c265["X509LwM2MBootstrapServerCredential"]
    BaseDataWithAdditionalInfo_p266["BaseDataWithAdditionalInfo"] -->|extends| Edge_c266["Edge"]
    HasLabel_p267["HasLabel"] -->|implements| Edge_c267["Edge"]
    HasTenantId_p268["HasTenantId"] -->|implements| Edge_c268["Edge"]
    HasCustomerId_p269["HasCustomerId"] -->|implements| Edge_c269["Edge"]
    BaseData_p270["BaseData"] -->|extends| EdgeEvent_c270["EdgeEvent"]
    Edge_p271["Edge"] -->|extends| EdgeInfo_c271["EdgeInfo"]
    Object______p272["Object/外部框架"] -->|extends| EdgeInstructions_c272["EdgeInstructions"]
    Object______p273["Object/外部框架"] -->|extends| EdgeSearchQuery_c273["EdgeSearchQuery"]
    Object______p274["Object/外部框架"] -->|extends| EntityViewSearchQuery_c274["EntityViewSearchQuery"]
    Object______p275["Object/外部框架"] -->|extends| DebugEventFilter_c275["DebugEventFilter"]
    EventFilter_p276["EventFilter"] -->|implements| DebugEventFilter_c276["DebugEventFilter"]
    Event_p277["Event"] -->|extends| ErrorEvent_c277["ErrorEvent"]
    Object______p278["Object/外部框架"] -->|extends| ErrorEventFilter_c278["ErrorEventFilter"]
    EventFilter_p279["EventFilter"] -->|implements| ErrorEventFilter_c279["ErrorEventFilter"]
    BaseData_p280["BaseData"] -->|extends| Event_c280["Event"]
    Object______p281["Object/外部框架"] -->|extends| LifeCycleEventFilter_c281["LifeCycleEventFilter"]
    EventFilter_p282["EventFilter"] -->|implements| LifeCycleEventFilter_c282["LifeCycleEventFilter"]
    Event_p283["Event"] -->|extends| LifecycleEvent_c283["LifecycleEvent"]
    Event_p284["Event"] -->|extends| RuleChainDebugEvent_c284["RuleChainDebugEvent"]
    DebugEventFilter_p285["DebugEventFilter"] -->|extends| RuleChainDebugEventFilter_c285["RuleChainDebugEventFilter"]
    Event_p286["Event"] -->|extends| RuleNodeDebugEvent_c286["RuleNodeDebugEvent"]
    DebugEventFilter_p287["DebugEventFilter"] -->|extends| RuleNodeDebugEventFilter_c287["RuleNodeDebugEventFilter"]
    Event_p288["Event"] -->|extends| StatisticsEvent_c288["StatisticsEvent"]
    Object______p289["Object/外部框架"] -->|extends| StatisticsEventFilter_c289["StatisticsEventFilter"]
    EventFilter_p290["EventFilter"] -->|implements| StatisticsEventFilter_c290["StatisticsEventFilter"]
    RuntimeException_p291["RuntimeException"] -->|extends| AbstractRateLimitException_c291["AbstractRateLimitException"]
    AbstractRateLimitException_p292["AbstractRateLimitException"] -->|extends| ApiUsageLimitsExceededException_c292["ApiUsageLimitsExceededException"]
    RuntimeException_p293["RuntimeException"] -->|extends| TenantNotFoundException_c293["TenantNotFoundException"]
    RuntimeException_p294["RuntimeException"] -->|extends| TenantProfileNotFoundException_c294["TenantProfileNotFoundException"]
    Exception_p295["Exception"] -->|extends| ThingsboardException_c295["ThingsboardException"]
    Error_p296["Error"] -->|extends| ThingsboardKafkaClientError_c296["ThingsboardKafkaClientError"]
    UUIDBased_p297["UUIDBased"] -->|extends| AdminSettingsId_c297["AdminSettingsId"]
    UUIDBased_p298["UUIDBased"] -->|extends| AlarmCommentId_c298["AlarmCommentId"]
    UUIDBased_p299["UUIDBased"] -->|extends| AlarmId_c299["AlarmId"]
    EntityId_p300["EntityId"] -->|implements| AlarmId_c300["AlarmId"]
    UUIDBased_p301["UUIDBased"] -->|extends| ApiUsageStateId_c301["ApiUsageStateId"]
    EntityId_p302["EntityId"] -->|implements| ApiUsageStateId_c302["ApiUsageStateId"]
    UUIDBased_p303["UUIDBased"] -->|extends| AssetId_c303["AssetId"]
    EntityId_p304["EntityId"] -->|implements| AssetId_c304["AssetId"]
    UUIDBased_p305["UUIDBased"] -->|extends| AssetProfileId_c305["AssetProfileId"]
    EntityId_p306["EntityId"] -->|implements| AssetProfileId_c306["AssetProfileId"]
    UUIDBased_p307["UUIDBased"] -->|extends| AuditLogId_c307["AuditLogId"]
    UUIDBased_p308["UUIDBased"] -->|extends| ComponentDescriptorId_c308["ComponentDescriptorId"]
    UUIDBased_p309["UUIDBased"] -->|extends| CustomerId_c309["CustomerId"]
    EntityId_p310["EntityId"] -->|implements| CustomerId_c310["CustomerId"]
    UUIDBased_p311["UUIDBased"] -->|extends| DashboardId_c311["DashboardId"]
    EntityId_p312["EntityId"] -->|implements| DashboardId_c312["DashboardId"]
    UUIDBased_p313["UUIDBased"] -->|extends| DeviceCredentialsId_c313["DeviceCredentialsId"]
    UUIDBased_p314["UUIDBased"] -->|extends| DeviceId_c314["DeviceId"]
    EntityId_p315["EntityId"] -->|implements| DeviceId_c315["DeviceId"]
    UUIDBased_p316["UUIDBased"] -->|extends| DeviceProfileId_c316["DeviceProfileId"]
    EntityId_p317["EntityId"] -->|implements| DeviceProfileId_c317["DeviceProfileId"]
    UUIDBased_p318["UUIDBased"] -->|extends| EdgeEventId_c318["EdgeEventId"]
    UUIDBased_p319["UUIDBased"] -->|extends| EdgeId_c319["EdgeId"]
    EntityId_p320["EntityId"] -->|implements| EdgeId_c320["EdgeId"]
    HasUUID_p321["HasUUID"] -->|extends| EntityId_c321["EntityId"]
    Serializable_p322["Serializable"] -->|extends| EntityId_c322["EntityId"]
    JsonDeserializer_p323["JsonDeserializer"] -->|extends| EntityIdDeserializer_c323["EntityIdDeserializer"]
    Object______p324["Object/外部框架"] -->|extends| EntityIdFactory_c324["EntityIdFactory"]
    JsonSerializer_p325["JsonSerializer"] -->|extends| EntityIdSerializer_c325["EntityIdSerializer"]
    UUIDBased_p326["UUIDBased"] -->|extends| EntityViewId_c326["EntityViewId"]
    EntityId_p327["EntityId"] -->|implements| EntityViewId_c327["EntityViewId"]
    UUIDBased_p328["UUIDBased"] -->|extends| EventId_c328["EventId"]
    Serializable_p329["Serializable"] -->|extends| HasId_c329["HasId"]
    Object______p330["Object/外部框架"] -->|extends| IdBased_c330["IdBased"]
    Object______p331["Object/外部框架"] -->|extends| NameLabelAndCustomerDetails_c331["NameLabelAndCustomerDetails"]
    UUIDBased_p332["UUIDBased"] -->|extends| NodeId_c332["NodeId"]
    UUIDBased_p333["UUIDBased"] -->|extends| NotificationId_c333["NotificationId"]
    EntityId_p334["EntityId"] -->|implements| NotificationId_c334["NotificationId"]
    UUIDBased_p335["UUIDBased"] -->|extends| NotificationRequestId_c335["NotificationRequestId"]
    EntityId_p336["EntityId"] -->|implements| NotificationRequestId_c336["NotificationRequestId"]
    UUIDBased_p337["UUIDBased"] -->|extends| NotificationRuleId_c337["NotificationRuleId"]
    EntityId_p338["EntityId"] -->|implements| NotificationRuleId_c338["NotificationRuleId"]
    UUIDBased_p339["UUIDBased"] -->|extends| NotificationTargetId_c339["NotificationTargetId"]
    EntityId_p340["EntityId"] -->|implements| NotificationTargetId_c340["NotificationTargetId"]
    UUIDBased_p341["UUIDBased"] -->|extends| NotificationTemplateId_c341["NotificationTemplateId"]
    EntityId_p342["EntityId"] -->|implements| NotificationTemplateId_c342["NotificationTemplateId"]
    UUIDBased_p343["UUIDBased"] -->|extends| OAuth2ClientRegistrationTemplateId_c343["OAuth2ClientRegistrationTemplateId"]
    UUIDBased_p344["UUIDBased"] -->|extends| OAuth2DomainId_c344["OAuth2DomainId"]
    UUIDBased_p345["UUIDBased"] -->|extends| OAuth2MobileId_c345["OAuth2MobileId"]
    UUIDBased_p346["UUIDBased"] -->|extends| OAuth2ParamsId_c346["OAuth2ParamsId"]
    UUIDBased_p347["UUIDBased"] -->|extends| OAuth2RegistrationId_c347["OAuth2RegistrationId"]
    UUIDBased_p348["UUIDBased"] -->|extends| OtaPackageId_c348["OtaPackageId"]
    EntityId_p349["EntityId"] -->|implements| OtaPackageId_c349["OtaPackageId"]
    UUIDBased_p350["UUIDBased"] -->|extends| QueueId_c350["QueueId"]
    EntityId_p351["EntityId"] -->|implements| QueueId_c351["QueueId"]
    UUIDBased_p352["UUIDBased"] -->|extends| RpcId_c352["RpcId"]
    EntityId_p353["EntityId"] -->|implements| RpcId_c353["RpcId"]
    UUIDBased_p354["UUIDBased"] -->|extends| RuleChainId_c354["RuleChainId"]
    EntityId_p355["EntityId"] -->|implements| RuleChainId_c355["RuleChainId"]
    UUIDBased_p356["UUIDBased"] -->|extends| RuleNodeId_c356["RuleNodeId"]
    EntityId_p357["EntityId"] -->|implements| RuleNodeId_c357["RuleNodeId"]
    UUIDBased_p358["UUIDBased"] -->|extends| RuleNodeStateId_c358["RuleNodeStateId"]
    UUIDBased_p359["UUIDBased"] -->|extends| TbResourceId_c359["TbResourceId"]
    EntityId_p360["EntityId"] -->|implements| TbResourceId_c360["TbResourceId"]
    UUIDBased_p361["UUIDBased"] -->|extends| TenantId_c361["TenantId"]
    EntityId_p362["EntityId"] -->|implements| TenantId_c362["TenantId"]
    UUIDBased_p363["UUIDBased"] -->|extends| TenantProfileId_c363["TenantProfileId"]
    EntityId_p364["EntityId"] -->|implements| TenantProfileId_c364["TenantProfileId"]
    Object______p365["Object/外部框架"] -->|extends| UUIDBased_c365["UUIDBased"]
    HasUUID_p366["HasUUID"] -->|implements| UUIDBased_c366["UUIDBased"]
    Serializable_p367["Serializable"] -->|implements| UUIDBased_c367["UUIDBased"]
    UUIDBased_p368["UUIDBased"] -->|extends| UserAuthSettingsId_c368["UserAuthSettingsId"]
    UUIDBased_p369["UUIDBased"] -->|extends| UserCredentialsId_c369["UserCredentialsId"]
    UUIDBased_p370["UUIDBased"] -->|extends| UserId_c370["UserId"]
    EntityId_p371["EntityId"] -->|implements| UserId_c371["UserId"]
    UUIDBased_p372["UUIDBased"] -->|extends| WidgetTypeId_c372["WidgetTypeId"]
    EntityId_p373["EntityId"] -->|implements| WidgetTypeId_c373["WidgetTypeId"]
    UUIDBased_p374["UUIDBased"] -->|extends| WidgetsBundleId_c374["WidgetsBundleId"]
    EntityId_p375["EntityId"] -->|implements| WidgetsBundleId_c375["WidgetsBundleId"]
    BasicTsKvEntry_p376["BasicTsKvEntry"] -->|extends| AggTsKvEntry_c376["AggTsKvEntry"]
    Object______p377["Object/外部框架"] -->|extends| AggregationParams_c377["AggregationParams"]
    Object______p378["Object/外部框架"] -->|extends| AttributeKey_c378["AttributeKey"]
    Serializable_p379["Serializable"] -->|implements| AttributeKey_c379["AttributeKey"]
    KvEntry_p380["KvEntry"] -->|extends| AttributeKvEntry_c380["AttributeKvEntry"]
    Object______p381["Object/外部框架"] -->|extends| BaseAttributeKvEntry_c381["BaseAttributeKvEntry"]
    AttributeKvEntry_p382["AttributeKvEntry"] -->|implements| BaseAttributeKvEntry_c382["BaseAttributeKvEntry"]
    BaseTsKvQuery_p383["BaseTsKvQuery"] -->|extends| BaseDeleteTsKvQuery_c383["BaseDeleteTsKvQuery"]
    DeleteTsKvQuery_p384["DeleteTsKvQuery"] -->|implements| BaseDeleteTsKvQuery_c384["BaseDeleteTsKvQuery"]
    BaseTsKvQuery_p385["BaseTsKvQuery"] -->|extends| BaseReadTsKvQuery_c385["BaseReadTsKvQuery"]
    ReadTsKvQuery_p386["ReadTsKvQuery"] -->|implements| BaseReadTsKvQuery_c386["BaseReadTsKvQuery"]
    Object______p387["Object/外部框架"] -->|extends| BaseTsKvQuery_c387["BaseTsKvQuery"]
    TsKvQuery_p388["TsKvQuery"] -->|implements| BaseTsKvQuery_c388["BaseTsKvQuery"]
    Object______p389["Object/外部框架"] -->|extends| BasicKvEntry_c389["BasicKvEntry"]
    KvEntry_p390["KvEntry"] -->|implements| BasicKvEntry_c390["BasicKvEntry"]
    Object______p391["Object/外部框架"] -->|extends| BasicTsKvEntry_c391["BasicTsKvEntry"]
    TsKvEntry_p392["TsKvEntry"] -->|implements| BasicTsKvEntry_c392["BasicTsKvEntry"]
    BasicKvEntry_p393["BasicKvEntry"] -->|extends| BooleanDataEntry_c393["BooleanDataEntry"]
    TsKvQuery_p394["TsKvQuery"] -->|extends| DeleteTsKvQuery_c394["DeleteTsKvQuery"]
    BasicKvEntry_p395["BasicKvEntry"] -->|extends| DoubleDataEntry_c395["DoubleDataEntry"]
    BasicKvEntry_p396["BasicKvEntry"] -->|extends| JsonDataEntry_c396["JsonDataEntry"]
    Serializable_p397["Serializable"] -->|extends| KvEntry_c397["KvEntry"]
    BasicKvEntry_p398["BasicKvEntry"] -->|extends| LongDataEntry_c398["LongDataEntry"]
    TsKvQuery_p399["TsKvQuery"] -->|extends| ReadTsKvQuery_c399["ReadTsKvQuery"]
    Object______p400["Object/外部框架"] -->|extends| ReadTsKvQueryResult_c400["ReadTsKvQueryResult"]
    BasicKvEntry_p401["BasicKvEntry"] -->|extends| StringDataEntry_c401["StringDataEntry"]
    KvEntry_p402["KvEntry"] -->|extends| TsKvEntry_c402["TsKvEntry"]
    Object______p403["Object/外部框架"] -->|extends| TsKvEntryAggWrapper_c403["TsKvEntryAggWrapper"]
    Object______p404["Object/外部框架"] -->|extends| TsKvLatestRemovingResult_c404["TsKvLatestRemovingResult"]
    Object______p405["Object/外部框架"] -->|extends| LwM2mInstance_c405["LwM2mInstance"]
    Object______p406["Object/外部框架"] -->|extends| LwM2mObject_c406["LwM2mObject"]
    Object______p407["Object/外部框架"] -->|extends| LwM2mResourceObserve_c407["LwM2mResourceObserve"]
    Object______p408["Object/外部框架"] -->|extends| MobileSessionInfo_c408["MobileSessionInfo"]
    Object______p409["Object/外部框架"] -->|extends| UserMobileInfo_c409["UserMobileInfo"]
    Object______p410["Object/外部框架"] -->|extends| TbNodeConnectionType_c410["TbNodeConnectionType"]
    RuntimeException_p411["RuntimeException"] -->|extends| AlreadySentException_c411["AlreadySentException"]
    BaseData_p412["BaseData"] -->|extends| Notification_c412["Notification"]
    BaseData_p413["BaseData"] -->|extends| NotificationRequest_c413["NotificationRequest"]
    HasTenantId_p414["HasTenantId"] -->|implements| NotificationRequest_c414["NotificationRequest"]
    HasName_p415["HasName"] -->|implements| NotificationRequest_c415["NotificationRequest"]
    Object______p416["Object/外部框架"] -->|extends| NotificationRequestConfig_c416["NotificationRequestConfig"]
    NotificationRequest_p417["NotificationRequest"] -->|extends| NotificationRequestInfo_c417["NotificationRequestInfo"]
    Object______p418["Object/外部框架"] -->|extends| NotificationRequestPreview_c418["NotificationRequestPreview"]
    Object______p419["Object/外部框架"] -->|extends| NotificationRequestStats_c419["NotificationRequestStats"]
    Object______p420["Object/外部框架"] -->|extends| AlarmAssignmentNotificationInfo_c420["AlarmAssignmentNotificationInfo"]
    RuleOriginatedNotificationInfo_p421["RuleOriginatedNotificationInfo"] -->|implements| AlarmAssignmentNotificationInfo_c421["AlarmAssignmentNotificationInfo"]
    Object______p422["Object/外部框架"] -->|extends| AlarmCommentNotificationInfo_c422["AlarmCommentNotificationInfo"]
    RuleOriginatedNotificationInfo_p423["RuleOriginatedNotificationInfo"] -->|implements| AlarmCommentNotificationInfo_c423["AlarmCommentNotificationInfo"]
    Object______p424["Object/外部框架"] -->|extends| AlarmNotificationInfo_c424["AlarmNotificationInfo"]
    RuleOriginatedNotificationInfo_p425["RuleOriginatedNotificationInfo"] -->|implements| AlarmNotificationInfo_c425["AlarmNotificationInfo"]
    Object______p426["Object/外部框架"] -->|extends| ApiUsageLimitNotificationInfo_c426["ApiUsageLimitNotificationInfo"]
    RuleOriginatedNotificationInfo_p427["RuleOriginatedNotificationInfo"] -->|implements| ApiUsageLimitNotificationInfo_c427["ApiUsageLimitNotificationInfo"]
    Object______p428["Object/外部框架"] -->|extends| DeviceActivityNotificationInfo_c428["DeviceActivityNotificationInfo"]
    RuleOriginatedNotificationInfo_p429["RuleOriginatedNotificationInfo"] -->|implements| DeviceActivityNotificationInfo_c429["DeviceActivityNotificationInfo"]
    Object______p430["Object/外部框架"] -->|extends| EdgeCommunicationFailureNotificationInfo_c430["EdgeCommunicationFailureNotificationInfo"]
    RuleOriginatedNotificationInfo_p431["RuleOriginatedNotificationInfo"] -->|implements| EdgeCommunicationFailureNotificationInfo_c431["EdgeCommunicationFailureNotificationInfo"]
    Object______p432["Object/外部框架"] -->|extends| EdgeConnectionNotificationInfo_c432["EdgeConnectionNotificationInfo"]
    RuleOriginatedNotificationInfo_p433["RuleOriginatedNotificationInfo"] -->|implements| EdgeConnectionNotificationInfo_c433["EdgeConnectionNotificationInfo"]
    Object______p434["Object/外部框架"] -->|extends| EntitiesLimitNotificationInfo_c434["EntitiesLimitNotificationInfo"]
    RuleOriginatedNotificationInfo_p435["RuleOriginatedNotificationInfo"] -->|implements| EntitiesLimitNotificationInfo_c435["EntitiesLimitNotificationInfo"]
    Object______p436["Object/外部框架"] -->|extends| EntityActionNotificationInfo_c436["EntityActionNotificationInfo"]
    RuleOriginatedNotificationInfo_p437["RuleOriginatedNotificationInfo"] -->|implements| EntityActionNotificationInfo_c437["EntityActionNotificationInfo"]
    Object______p438["Object/外部框架"] -->|extends| NewPlatformVersionNotificationInfo_c438["NewPlatformVersionNotificationInfo"]
    RuleOriginatedNotificationInfo_p439["RuleOriginatedNotificationInfo"] -->|implements| NewPlatformVersionNotificationInfo_c439["NewPlatformVersionNotificationInfo"]
    Object______p440["Object/外部框架"] -->|extends| RateLimitsNotificationInfo_c440["RateLimitsNotificationInfo"]
    RuleOriginatedNotificationInfo_p441["RuleOriginatedNotificationInfo"] -->|implements| RateLimitsNotificationInfo_c441["RateLimitsNotificationInfo"]
    Object______p442["Object/外部框架"] -->|extends| RuleEngineComponentLifecycleEventNotificationInfo_c442["RuleEngineComponentLifecycleEventNotificationInfo"]
    RuleOriginatedNotificationInfo_p443["RuleOriginatedNotificationInfo"] -->|implements| RuleEngineComponentLifecycleEventNotificationInfo_c443["RuleEngineComponentLifecycleEventNotificationInfo"]
    Object______p444["Object/外部框架"] -->|extends| RuleEngineOriginatedNotificationInfo_c444["RuleEngineOriginatedNotificationInfo"]
    RuleOriginatedNotificationInfo_p445["RuleOriginatedNotificationInfo"] -->|implements| RuleEngineOriginatedNotificationInfo_c445["RuleEngineOriginatedNotificationInfo"]
    NotificationInfo_p446["NotificationInfo"] -->|extends| RuleOriginatedNotificationInfo_c446["RuleOriginatedNotificationInfo"]
    NotificationRuleRecipientsConfig_p447["NotificationRuleRecipientsConfig"] -->|extends| DefaultNotificationRuleRecipientsConfig_c447["DefaultNotificationRuleRecipientsConfig"]
    NotificationRuleRecipientsConfig_p448["NotificationRuleRecipientsConfig"] -->|extends| EscalatedNotificationRuleRecipientsConfig_c448["EscalatedNotificationRuleRecipientsConfig"]
    BaseData_p449["BaseData"] -->|extends| NotificationRule_c449["NotificationRule"]
    HasTenantId_p450["HasTenantId"] -->|implements| NotificationRule_c450["NotificationRule"]
    HasName_p451["HasName"] -->|implements| NotificationRule_c451["NotificationRule"]
    ExportableEntity_p452["ExportableEntity"] -->|implements| NotificationRule_c452["NotificationRule"]
    Serializable_p453["Serializable"] -->|implements| NotificationRule_c453["NotificationRule"]
    Object______p454["Object/外部框架"] -->|extends| NotificationRuleConfig_c454["NotificationRuleConfig"]
    Serializable_p455["Serializable"] -->|implements| NotificationRuleConfig_c455["NotificationRuleConfig"]
    NotificationRule_p456["NotificationRule"] -->|extends| NotificationRuleInfo_c456["NotificationRuleInfo"]
    Object______p457["Object/外部框架"] -->|extends| NotificationRuleRecipientsConfig_c457["NotificationRuleRecipientsConfig"]
    Serializable_p458["Serializable"] -->|implements| NotificationRuleRecipientsConfig_c458["NotificationRuleRecipientsConfig"]
    Object______p459["Object/外部框架"] -->|extends| AlarmAssignmentTrigger_c459["AlarmAssignmentTrigger"]
    NotificationRuleTrigger_p460["NotificationRuleTrigger"] -->|implements| AlarmAssignmentTrigger_c460["AlarmAssignmentTrigger"]
    Object______p461["Object/外部框架"] -->|extends| AlarmCommentTrigger_c461["AlarmCommentTrigger"]
    NotificationRuleTrigger_p462["NotificationRuleTrigger"] -->|implements| AlarmCommentTrigger_c462["AlarmCommentTrigger"]
    Object______p463["Object/外部框架"] -->|extends| AlarmTrigger_c463["AlarmTrigger"]
    NotificationRuleTrigger_p464["NotificationRuleTrigger"] -->|implements| AlarmTrigger_c464["AlarmTrigger"]
    Object______p465["Object/外部框架"] -->|extends| ApiUsageLimitTrigger_c465["ApiUsageLimitTrigger"]
    NotificationRuleTrigger_p466["NotificationRuleTrigger"] -->|implements| ApiUsageLimitTrigger_c466["ApiUsageLimitTrigger"]
    Object______p467["Object/外部框架"] -->|extends| DeviceActivityTrigger_c467["DeviceActivityTrigger"]
    NotificationRuleTrigger_p468["NotificationRuleTrigger"] -->|implements| DeviceActivityTrigger_c468["DeviceActivityTrigger"]
    Object______p469["Object/外部框架"] -->|extends| EdgeCommunicationFailureTrigger_c469["EdgeCommunicationFailureTrigger"]
    NotificationRuleTrigger_p470["NotificationRuleTrigger"] -->|implements| EdgeCommunicationFailureTrigger_c470["EdgeCommunicationFailureTrigger"]
    Object______p471["Object/外部框架"] -->|extends| EdgeConnectionTrigger_c471["EdgeConnectionTrigger"]
    NotificationRuleTrigger_p472["NotificationRuleTrigger"] -->|implements| EdgeConnectionTrigger_c472["EdgeConnectionTrigger"]
    Object______p473["Object/外部框架"] -->|extends| EntitiesLimitTrigger_c473["EntitiesLimitTrigger"]
    NotificationRuleTrigger_p474["NotificationRuleTrigger"] -->|implements| EntitiesLimitTrigger_c474["EntitiesLimitTrigger"]
    Object______p475["Object/外部框架"] -->|extends| EntityActionTrigger_c475["EntityActionTrigger"]
    NotificationRuleTrigger_p476["NotificationRuleTrigger"] -->|implements| EntityActionTrigger_c476["EntityActionTrigger"]
    Object______p477["Object/外部框架"] -->|extends| NewPlatformVersionTrigger_c477["NewPlatformVersionTrigger"]
    NotificationRuleTrigger_p478["NotificationRuleTrigger"] -->|implements| NewPlatformVersionTrigger_c478["NewPlatformVersionTrigger"]
    Serializable_p479["Serializable"] -->|extends| NotificationRuleTrigger_c479["NotificationRuleTrigger"]
    Object______p480["Object/外部框架"] -->|extends| RateLimitsTrigger_c480["RateLimitsTrigger"]
    NotificationRuleTrigger_p481["NotificationRuleTrigger"] -->|implements| RateLimitsTrigger_c481["RateLimitsTrigger"]
    Object______p482["Object/外部框架"] -->|extends| RuleEngineComponentLifecycleEventTrigger_c482["RuleEngineComponentLifecycleEventTrigger"]
    NotificationRuleTrigger_p483["NotificationRuleTrigger"] -->|implements| RuleEngineComponentLifecycleEventTrigger_c483["RuleEngineComponentLifecycleEventTrigger"]
    Object______p484["Object/外部框架"] -->|extends| AlarmAssignmentNotificationRuleTriggerConfig_c484["AlarmAssignmentNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p485["NotificationRuleTriggerConfig"] -->|implements| AlarmAssignmentNotificationRuleTriggerConfig_c485["AlarmAssignmentNotificationRuleTriggerConfig"]
    Object______p486["Object/外部框架"] -->|extends| AlarmCommentNotificationRuleTriggerConfig_c486["AlarmCommentNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p487["NotificationRuleTriggerConfig"] -->|implements| AlarmCommentNotificationRuleTriggerConfig_c487["AlarmCommentNotificationRuleTriggerConfig"]
    Object______p488["Object/外部框架"] -->|extends| AlarmNotificationRuleTriggerConfig_c488["AlarmNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p489["NotificationRuleTriggerConfig"] -->|implements| AlarmNotificationRuleTriggerConfig_c489["AlarmNotificationRuleTriggerConfig"]
    Object______p490["Object/外部框架"] -->|extends| ClearRule_c490["ClearRule"]
    Serializable_p491["Serializable"] -->|implements| ClearRule_c491["ClearRule"]
    Object______p492["Object/外部框架"] -->|extends| ApiUsageLimitNotificationRuleTriggerConfig_c492["ApiUsageLimitNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p493["NotificationRuleTriggerConfig"] -->|implements| ApiUsageLimitNotificationRuleTriggerConfig_c493["ApiUsageLimitNotificationRuleTriggerConfig"]
    Object______p494["Object/外部框架"] -->|extends| DeviceActivityNotificationRuleTriggerConfig_c494["DeviceActivityNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p495["NotificationRuleTriggerConfig"] -->|implements| DeviceActivityNotificationRuleTriggerConfig_c495["DeviceActivityNotificationRuleTriggerConfig"]
    Object______p496["Object/外部框架"] -->|extends| EdgeCommunicationFailureNotificationRuleTriggerConfig_c496["EdgeCommunicationFailureNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p497["NotificationRuleTriggerConfig"] -->|implements| EdgeCommunicationFailureNotificationRuleTriggerConfig_c497["EdgeCommunicationFailureNotificationRuleTriggerConfig"]
    Object______p498["Object/外部框架"] -->|extends| EdgeConnectionNotificationRuleTriggerConfig_c498["EdgeConnectionNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p499["NotificationRuleTriggerConfig"] -->|implements| EdgeConnectionNotificationRuleTriggerConfig_c499["EdgeConnectionNotificationRuleTriggerConfig"]
    Object______p500["Object/外部框架"] -->|extends| EntitiesLimitNotificationRuleTriggerConfig_c500["EntitiesLimitNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p501["NotificationRuleTriggerConfig"] -->|implements| EntitiesLimitNotificationRuleTriggerConfig_c501["EntitiesLimitNotificationRuleTriggerConfig"]
    Object______p502["Object/外部框架"] -->|extends| EntityActionNotificationRuleTriggerConfig_c502["EntityActionNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p503["NotificationRuleTriggerConfig"] -->|implements| EntityActionNotificationRuleTriggerConfig_c503["EntityActionNotificationRuleTriggerConfig"]
    Object______p504["Object/外部框架"] -->|extends| NewPlatformVersionNotificationRuleTriggerConfig_c504["NewPlatformVersionNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p505["NotificationRuleTriggerConfig"] -->|implements| NewPlatformVersionNotificationRuleTriggerConfig_c505["NewPlatformVersionNotificationRuleTriggerConfig"]
    Serializable_p506["Serializable"] -->|extends| NotificationRuleTriggerConfig_c506["NotificationRuleTriggerConfig"]
    Object______p507["Object/外部框架"] -->|extends| RateLimitsNotificationRuleTriggerConfig_c507["RateLimitsNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p508["NotificationRuleTriggerConfig"] -->|implements| RateLimitsNotificationRuleTriggerConfig_c508["RateLimitsNotificationRuleTriggerConfig"]
    Object______p509["Object/外部框架"] -->|extends| RuleEngineComponentLifecycleEventNotificationRuleTriggerConf_c509["RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig"]
    NotificationRuleTriggerConfig_p510["NotificationRuleTriggerConfig"] -->|implements| RuleEngineComponentLifecycleEventNotificationRuleTriggerConf_c510["RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig"]
    Object______p511["Object/外部框架"] -->|extends| AccountNotificationSettings_c511["AccountNotificationSettings"]
    Object______p512["Object/外部框架"] -->|extends| MobileAppNotificationDeliveryMethodConfig_c512["MobileAppNotificationDeliveryMethodConfig"]
    NotificationDeliveryMethodConfig_p513["NotificationDeliveryMethodConfig"] -->|implements| MobileAppNotificationDeliveryMethodConfig_c513["MobileAppNotificationDeliveryMethodConfig"]
    Serializable_p514["Serializable"] -->|extends| NotificationDeliveryMethodConfig_c514["NotificationDeliveryMethodConfig"]
    Object______p515["Object/外部框架"] -->|extends| NotificationSettings_c515["NotificationSettings"]
    Serializable_p516["Serializable"] -->|implements| NotificationSettings_c516["NotificationSettings"]
    Object______p517["Object/外部框架"] -->|extends| SlackNotificationDeliveryMethodConfig_c517["SlackNotificationDeliveryMethodConfig"]
    NotificationDeliveryMethodConfig_p518["NotificationDeliveryMethodConfig"] -->|implements| SlackNotificationDeliveryMethodConfig_c518["SlackNotificationDeliveryMethodConfig"]
    Object______p519["Object/外部框架"] -->|extends| UserNotificationSettings_c519["UserNotificationSettings"]
    Object______p520["Object/外部框架"] -->|extends| NotificationPref_c520["NotificationPref"]
    NotificationTargetConfig_p521["NotificationTargetConfig"] -->|extends| MicrosoftTeamsNotificationTargetConfig_c521["MicrosoftTeamsNotificationTargetConfig"]
    NotificationRecipient_p522["NotificationRecipient"] -->|implements| MicrosoftTeamsNotificationTargetConfig_c522["MicrosoftTeamsNotificationTargetConfig"]
    BaseData_p523["BaseData"] -->|extends| NotificationTarget_c523["NotificationTarget"]
    HasTenantId_p524["HasTenantId"] -->|implements| NotificationTarget_c524["NotificationTarget"]
    HasName_p525["HasName"] -->|implements| NotificationTarget_c525["NotificationTarget"]
    ExportableEntity_p526["ExportableEntity"] -->|implements| NotificationTarget_c526["NotificationTarget"]
    Object______p527["Object/外部框架"] -->|extends| NotificationTargetConfig_c527["NotificationTargetConfig"]
    Object______p528["Object/外部框架"] -->|extends| AffectedTenantAdministratorsFilter_c528["AffectedTenantAdministratorsFilter"]
    UsersFilter_p529["UsersFilter"] -->|implements| AffectedTenantAdministratorsFilter_c529["AffectedTenantAdministratorsFilter"]
    Object______p530["Object/外部框架"] -->|extends| AffectedUserFilter_c530["AffectedUserFilter"]
    UsersFilter_p531["UsersFilter"] -->|implements| AffectedUserFilter_c531["AffectedUserFilter"]
    Object______p532["Object/外部框架"] -->|extends| AllUsersFilter_c532["AllUsersFilter"]
    UsersFilter_p533["UsersFilter"] -->|implements| AllUsersFilter_c533["AllUsersFilter"]
    Object______p534["Object/外部框架"] -->|extends| CustomerUsersFilter_c534["CustomerUsersFilter"]
    UsersFilter_p535["UsersFilter"] -->|implements| CustomerUsersFilter_c535["CustomerUsersFilter"]
    Object______p536["Object/外部框架"] -->|extends| OriginatorEntityOwnerUsersFilter_c536["OriginatorEntityOwnerUsersFilter"]
    UsersFilter_p537["UsersFilter"] -->|implements| OriginatorEntityOwnerUsersFilter_c537["OriginatorEntityOwnerUsersFilter"]
    NotificationTargetConfig_p538["NotificationTargetConfig"] -->|extends| PlatformUsersNotificationTargetConfig_c538["PlatformUsersNotificationTargetConfig"]
    Object______p539["Object/外部框架"] -->|extends| SystemAdministratorsFilter_c539["SystemAdministratorsFilter"]
    UsersFilter_p540["UsersFilter"] -->|implements| SystemAdministratorsFilter_c540["SystemAdministratorsFilter"]
    Object______p541["Object/外部框架"] -->|extends| TenantAdministratorsFilter_c541["TenantAdministratorsFilter"]
    UsersFilter_p542["UsersFilter"] -->|implements| TenantAdministratorsFilter_c542["TenantAdministratorsFilter"]
    Object______p543["Object/外部框架"] -->|extends| UserListFilter_c543["UserListFilter"]
    UsersFilter_p544["UsersFilter"] -->|implements| UserListFilter_c544["UserListFilter"]
    Object______p545["Object/外部框架"] -->|extends| SlackConversation_c545["SlackConversation"]
    NotificationRecipient_p546["NotificationRecipient"] -->|implements| SlackConversation_c546["SlackConversation"]
    NotificationTargetConfig_p547["NotificationTargetConfig"] -->|extends| SlackNotificationTargetConfig_c547["SlackNotificationTargetConfig"]
    Object______p548["Object/外部框架"] -->|extends| DeliveryMethodNotificationTemplate_c548["DeliveryMethodNotificationTemplate"]
    DeliveryMethodNotificationTemplate_p549["DeliveryMethodNotificationTemplate"] -->|extends| EmailDeliveryMethodNotificationTemplate_c549["EmailDeliveryMethodNotificationTemplate"]
    HasSubject_p550["HasSubject"] -->|implements| EmailDeliveryMethodNotificationTemplate_c550["EmailDeliveryMethodNotificationTemplate"]
    DeliveryMethodNotificationTemplate_p551["DeliveryMethodNotificationTemplate"] -->|extends| MicrosoftTeamsDeliveryMethodNotificationTemplate_c551["MicrosoftTeamsDeliveryMethodNotificationTemplate"]
    HasSubject_p552["HasSubject"] -->|implements| MicrosoftTeamsDeliveryMethodNotificationTemplate_c552["MicrosoftTeamsDeliveryMethodNotificationTemplate"]
    Object______p553["Object/外部框架"] -->|extends| Button_c553["Button"]
    DeliveryMethodNotificationTemplate_p554["DeliveryMethodNotificationTemplate"] -->|extends| MobileAppDeliveryMethodNotificationTemplate_c554["MobileAppDeliveryMethodNotificationTemplate"]
    HasSubject_p555["HasSubject"] -->|implements| MobileAppDeliveryMethodNotificationTemplate_c555["MobileAppDeliveryMethodNotificationTemplate"]
    BaseData_p556["BaseData"] -->|extends| NotificationTemplate_c556["NotificationTemplate"]
    HasTenantId_p557["HasTenantId"] -->|implements| NotificationTemplate_c557["NotificationTemplate"]
    HasName_p558["HasName"] -->|implements| NotificationTemplate_c558["NotificationTemplate"]
    ExportableEntity_p559["ExportableEntity"] -->|implements| NotificationTemplate_c559["NotificationTemplate"]
    Object______p560["Object/外部框架"] -->|extends| NotificationTemplateConfig_c560["NotificationTemplateConfig"]
    DeliveryMethodNotificationTemplate_p561["DeliveryMethodNotificationTemplate"] -->|extends| SlackDeliveryMethodNotificationTemplate_c561["SlackDeliveryMethodNotificationTemplate"]
    DeliveryMethodNotificationTemplate_p562["DeliveryMethodNotificationTemplate"] -->|extends| SmsDeliveryMethodNotificationTemplate_c562["SmsDeliveryMethodNotificationTemplate"]
    Object______p563["Object/外部框架"] -->|extends| TemplatableValue_c563["TemplatableValue"]
    DeliveryMethodNotificationTemplate_p564["DeliveryMethodNotificationTemplate"] -->|extends| WebDeliveryMethodNotificationTemplate_c564["WebDeliveryMethodNotificationTemplate"]
    HasSubject_p565["HasSubject"] -->|implements| WebDeliveryMethodNotificationTemplate_c565["WebDeliveryMethodNotificationTemplate"]
    Object______p566["Object/外部框架"] -->|extends| OAuth2BasicMapperConfig_c566["OAuth2BasicMapperConfig"]
    Object______p567["Object/外部框架"] -->|extends| OAuth2ClientInfo_c567["OAuth2ClientInfo"]
    BaseDataWithAdditionalInfo_p568["BaseDataWithAdditionalInfo"] -->|extends| OAuth2ClientRegistrationTemplate_c568["OAuth2ClientRegistrationTemplate"]
    HasName_p569["HasName"] -->|implements| OAuth2ClientRegistrationTemplate_c569["OAuth2ClientRegistrationTemplate"]
    Object______p570["Object/外部框架"] -->|extends| OAuth2CustomMapperConfig_c570["OAuth2CustomMapperConfig"]
    BaseData_p571["BaseData"] -->|extends| OAuth2Domain_c571["OAuth2Domain"]
    Object______p572["Object/外部框架"] -->|extends| OAuth2DomainInfo_c572["OAuth2DomainInfo"]
    Object______p573["Object/外部框架"] -->|extends| OAuth2Info_c573["OAuth2Info"]
    Object______p574["Object/外部框架"] -->|extends| OAuth2MapperConfig_c574["OAuth2MapperConfig"]
    BaseData_p575["BaseData"] -->|extends| OAuth2Mobile_c575["OAuth2Mobile"]
    Object______p576["Object/外部框架"] -->|extends| OAuth2MobileInfo_c576["OAuth2MobileInfo"]
    BaseData_p577["BaseData"] -->|extends| OAuth2Params_c577["OAuth2Params"]
    Object______p578["Object/外部框架"] -->|extends| OAuth2ParamsInfo_c578["OAuth2ParamsInfo"]
    BaseDataWithAdditionalInfo_p579["BaseDataWithAdditionalInfo"] -->|extends| OAuth2Registration_c579["OAuth2Registration"]
    HasName_p580["HasName"] -->|implements| OAuth2Registration_c580["OAuth2Registration"]
    Object______p581["Object/外部框架"] -->|extends| OAuth2RegistrationInfo_c581["OAuth2RegistrationInfo"]
    Object______p582["Object/外部框架"] -->|extends| AttributesEntityView_c582["AttributesEntityView"]
    Serializable_p583["Serializable"] -->|implements| AttributesEntityView_c583["AttributesEntityView"]
    Object______p584["Object/外部框架"] -->|extends| TelemetryEntityView_c584["TelemetryEntityView"]
    Serializable_p585["Serializable"] -->|implements| TelemetryEntityView_c585["TelemetryEntityView"]
    Object______p586["Object/外部框架"] -->|extends| OtaPackageUtil_c586["OtaPackageUtil"]
    Object______p587["Object/外部框架"] -->|extends| BasePageDataIterable_c587["BasePageDataIterable"]
    Object______p588["Object/外部框架"] -->|extends| PageData_c588["PageData"]
    Serializable_p589["Serializable"] -->|implements| PageData_c589["PageData"]
    Object______p590["Object/外部框架"] -->|extends| PageDataIterable_c590["PageDataIterable"]
    Object______p591["Object/外部框架"] -->|extends| PageDataIterableByTenant_c591["PageDataIterableByTenant"]
    Object______p592["Object/外部框架"] -->|extends| PageDataIterableByTenantIdEntityId_c592["PageDataIterableByTenantIdEntityId"]
    Object______p593["Object/外部框架"] -->|extends| PageLink_c593["PageLink"]
    Object______p594["Object/外部框架"] -->|extends| SortOrder_c594["SortOrder"]
    PageLink_p595["PageLink"] -->|extends| TimePageLink_c595["TimePageLink"]
    BaseData_p596["BaseData"] -->|extends| ComponentDescriptor_c596["ComponentDescriptor"]
    Serializable_p597["Serializable"] -->|implements| ComponentLifecycleEvent_c597["ComponentLifecycleEvent"]
    EntityCountQuery_p598["EntityCountQuery"] -->|extends| AbstractDataQuery_c598["AbstractDataQuery"]
    EntityCountQuery_p599["EntityCountQuery"] -->|extends| AlarmCountQuery_c599["AlarmCountQuery"]
    AlarmInfo_p600["AlarmInfo"] -->|extends| AlarmData_c600["AlarmData"]
    EntityDataPageLink_p601["EntityDataPageLink"] -->|extends| AlarmDataPageLink_c601["AlarmDataPageLink"]
    AbstractDataQuery_p602["AbstractDataQuery"] -->|extends| AlarmDataQuery_c602["AlarmDataQuery"]
    Object______p603["Object/外部框架"] -->|extends| ApiUsageStateFilter_c603["ApiUsageStateFilter"]
    EntityFilter_p604["EntityFilter"] -->|implements| ApiUsageStateFilter_c604["ApiUsageStateFilter"]
    EntitySearchQueryFilter_p605["EntitySearchQueryFilter"] -->|extends| AssetSearchQueryFilter_c605["AssetSearchQueryFilter"]
    Object______p606["Object/外部框架"] -->|extends| AssetTypeFilter_c606["AssetTypeFilter"]
    EntityFilter_p607["EntityFilter"] -->|implements| AssetTypeFilter_c607["AssetTypeFilter"]
    Object______p608["Object/外部框架"] -->|extends| BooleanFilterPredicate_c608["BooleanFilterPredicate"]
    SimpleKeyFilterPredicate_p609["SimpleKeyFilterPredicate"] -->|implements| BooleanFilterPredicate_c609["BooleanFilterPredicate"]
    Object______p610["Object/外部框架"] -->|extends| ComparisonTsValue_c610["ComparisonTsValue"]
    Object______p611["Object/外部框架"] -->|extends| ComplexFilterPredicate_c611["ComplexFilterPredicate"]
    KeyFilterPredicate_p612["KeyFilterPredicate"] -->|implements| ComplexFilterPredicate_c612["ComplexFilterPredicate"]
    EntitySearchQueryFilter_p613["EntitySearchQueryFilter"] -->|extends| DeviceSearchQueryFilter_c613["DeviceSearchQueryFilter"]
    Object______p614["Object/外部框架"] -->|extends| DeviceTypeFilter_c614["DeviceTypeFilter"]
    EntityFilter_p615["EntityFilter"] -->|implements| DeviceTypeFilter_c615["DeviceTypeFilter"]
    Object______p616["Object/外部框架"] -->|extends| DynamicValue_c616["DynamicValue"]
    Serializable_p617["Serializable"] -->|implements| DynamicValue_c617["DynamicValue"]
    EntitySearchQueryFilter_p618["EntitySearchQueryFilter"] -->|extends| EdgeSearchQueryFilter_c618["EdgeSearchQueryFilter"]
    Object______p619["Object/外部框架"] -->|extends| EdgeTypeFilter_c619["EdgeTypeFilter"]
    EntityFilter_p620["EntityFilter"] -->|implements| EdgeTypeFilter_c620["EdgeTypeFilter"]
    Object______p621["Object/外部框架"] -->|extends| EntityCountQuery_c621["EntityCountQuery"]
    Object______p622["Object/外部框架"] -->|extends| EntityData_c622["EntityData"]
    Object______p623["Object/外部框架"] -->|extends| EntityDataPageLink_c623["EntityDataPageLink"]
    AbstractDataQuery_p624["AbstractDataQuery"] -->|extends| EntityDataQuery_c624["EntityDataQuery"]
    Object______p625["Object/外部框架"] -->|extends| EntityDataSortOrder_c625["EntityDataSortOrder"]
    Object______p626["Object/外部框架"] -->|extends| EntityKey_c626["EntityKey"]
    Serializable_p627["Serializable"] -->|implements| EntityKey_c627["EntityKey"]
    Object______p628["Object/外部框架"] -->|extends| EntityListFilter_c628["EntityListFilter"]
    EntityFilter_p629["EntityFilter"] -->|implements| EntityListFilter_c629["EntityListFilter"]
    Object______p630["Object/外部框架"] -->|extends| EntityNameFilter_c630["EntityNameFilter"]
    EntityFilter_p631["EntityFilter"] -->|implements| EntityNameFilter_c631["EntityNameFilter"]
    Object______p632["Object/外部框架"] -->|extends| EntitySearchQueryFilter_c632["EntitySearchQueryFilter"]
    EntityFilter_p633["EntityFilter"] -->|implements| EntitySearchQueryFilter_c633["EntitySearchQueryFilter"]
    Object______p634["Object/外部框架"] -->|extends| EntityTypeFilter_c634["EntityTypeFilter"]
    EntityFilter_p635["EntityFilter"] -->|implements| EntityTypeFilter_c635["EntityTypeFilter"]
    EntitySearchQueryFilter_p636["EntitySearchQueryFilter"] -->|extends| EntityViewSearchQueryFilter_c636["EntityViewSearchQueryFilter"]
    Object______p637["Object/外部框架"] -->|extends| EntityViewTypeFilter_c637["EntityViewTypeFilter"]
    EntityFilter_p638["EntityFilter"] -->|implements| EntityViewTypeFilter_c638["EntityViewTypeFilter"]
    Object______p639["Object/外部框架"] -->|extends| FilterPredicateValue_c639["FilterPredicateValue"]
    Serializable_p640["Serializable"] -->|implements| FilterPredicateValue_c640["FilterPredicateValue"]
    Object______p641["Object/外部框架"] -->|extends| KeyFilter_c641["KeyFilter"]
    Serializable_p642["Serializable"] -->|implements| KeyFilter_c642["KeyFilter"]
    Serializable_p643["Serializable"] -->|extends| KeyFilterPredicate_c643["KeyFilterPredicate"]
    Object______p644["Object/外部框架"] -->|extends| NumericFilterPredicate_c644["NumericFilterPredicate"]
    SimpleKeyFilterPredicate_p645["SimpleKeyFilterPredicate"] -->|implements| NumericFilterPredicate_c645["NumericFilterPredicate"]
    Object______p646["Object/外部框架"] -->|extends| RelationsQueryFilter_c646["RelationsQueryFilter"]
    EntityFilter_p647["EntityFilter"] -->|implements| RelationsQueryFilter_c647["RelationsQueryFilter"]
    KeyFilterPredicate_p648["KeyFilterPredicate"] -->|extends| SimpleKeyFilterPredicate_c648["SimpleKeyFilterPredicate"]
    Object______p649["Object/外部框架"] -->|extends| SingleEntityFilter_c649["SingleEntityFilter"]
    EntityFilter_p650["EntityFilter"] -->|implements| SingleEntityFilter_c650["SingleEntityFilter"]
    Object______p651["Object/外部框架"] -->|extends| StringFilterPredicate_c651["StringFilterPredicate"]
    SimpleKeyFilterPredicate_p652["SimpleKeyFilterPredicate"] -->|implements| StringFilterPredicate_c652["StringFilterPredicate"]
    Object______p653["Object/外部框架"] -->|extends| TsValue_c653["TsValue"]
    Object______p654["Object/外部框架"] -->|extends| ProcessingStrategy_c654["ProcessingStrategy"]
    Serializable_p655["Serializable"] -->|implements| ProcessingStrategy_c655["ProcessingStrategy"]
    BaseDataWithAdditionalInfo_p656["BaseDataWithAdditionalInfo"] -->|extends| Queue_c656["Queue"]
    HasName_p657["HasName"] -->|implements| Queue_c657["Queue"]
    HasTenantId_p658["HasTenantId"] -->|implements| Queue_c658["Queue"]
    Object______p659["Object/外部框架"] -->|extends| SubmitStrategy_c659["SubmitStrategy"]
    Serializable_p660["Serializable"] -->|implements| SubmitStrategy_c660["SubmitStrategy"]
    Object______p661["Object/外部框架"] -->|extends| EntityRelation_c661["EntityRelation"]
    Serializable_p662["Serializable"] -->|implements| EntityRelation_c662["EntityRelation"]
    EntityRelation_p663["EntityRelation"] -->|extends| EntityRelationInfo_c663["EntityRelationInfo"]
    Object______p664["Object/外部框架"] -->|extends| EntityRelationsQuery_c664["EntityRelationsQuery"]
    Object______p665["Object/外部框架"] -->|extends| RelationEntityTypeFilter_c665["RelationEntityTypeFilter"]
    Object______p666["Object/外部框架"] -->|extends| RelationsSearchParameters_c666["RelationsSearchParameters"]
    BaseData_p667["BaseData"] -->|extends| Rpc_c667["Rpc"]
    HasTenantId_p668["HasTenantId"] -->|implements| Rpc_c668["Rpc"]
    Object______p669["Object/外部框架"] -->|extends| ToDeviceRpcRequestBody_c669["ToDeviceRpcRequestBody"]
    Serializable_p670["Serializable"] -->|implements| ToDeviceRpcRequestBody_c670["ToDeviceRpcRequestBody"]
    Object______p671["Object/外部框架"] -->|extends| DefaultRuleChainCreateRequest_c671["DefaultRuleChainCreateRequest"]
    Serializable_p672["Serializable"] -->|implements| DefaultRuleChainCreateRequest_c672["DefaultRuleChainCreateRequest"]
    Object______p673["Object/外部框架"] -->|extends| NodeConnectionInfo_c673["NodeConnectionInfo"]
    BaseDataWithAdditionalInfo_p674["BaseDataWithAdditionalInfo"] -->|extends| RuleChain_c674["RuleChain"]
    HasName_p675["HasName"] -->|implements| RuleChain_c675["RuleChain"]
    HasTenantId_p676["HasTenantId"] -->|implements| RuleChain_c676["RuleChain"]
    ExportableEntity_p677["ExportableEntity"] -->|implements| RuleChain_c677["RuleChain"]
    Object______p678["Object/外部框架"] -->|extends| RuleChainConnectionInfo_c678["RuleChainConnectionInfo"]
    Object______p679["Object/外部框架"] -->|extends| RuleChainData_c679["RuleChainData"]
    Object______p680["Object/外部框架"] -->|extends| RuleChainImportResult_c680["RuleChainImportResult"]
    Object______p681["Object/外部框架"] -->|extends| RuleChainMetaData_c681["RuleChainMetaData"]
    Object______p682["Object/外部框架"] -->|extends| RuleChainOutputLabelsUsage_c682["RuleChainOutputLabelsUsage"]
    Object______p683["Object/外部框架"] -->|extends| RuleChainUpdateResult_c683["RuleChainUpdateResult"]
    BaseDataWithAdditionalInfo_p684["BaseDataWithAdditionalInfo"] -->|extends| RuleNode_c684["RuleNode"]
    HasName_p685["HasName"] -->|implements| RuleNode_c685["RuleNode"]
    BaseData_p686["BaseData"] -->|extends| RuleNodeState_c686["RuleNodeState"]
    Object______p687["Object/外部框架"] -->|extends| RuleNodeUpdateResult_c687["RuleNodeUpdateResult"]
    BaseData_p688["BaseData"] -->|extends| DeviceCredentials_c688["DeviceCredentials"]
    DeviceCredentialsFilter_p689["DeviceCredentialsFilter"] -->|implements| DeviceCredentials_c689["DeviceCredentials"]
    Object______p690["Object/外部框架"] -->|extends| DeviceTokenCredentials_c690["DeviceTokenCredentials"]
    DeviceCredentialsFilter_p691["DeviceCredentialsFilter"] -->|implements| DeviceTokenCredentials_c691["DeviceTokenCredentials"]
    Object______p692["Object/外部框架"] -->|extends| DeviceX509Credentials_c692["DeviceX509Credentials"]
    DeviceCredentialsFilter_p693["DeviceCredentialsFilter"] -->|implements| DeviceX509Credentials_c693["DeviceX509Credentials"]
    BaseData_p694["BaseData"] -->|extends| UserAuthSettings_c694["UserAuthSettings"]
    BaseData_p695["BaseData"] -->|extends| UserCredentials_c695["UserCredentials"]
    Object______p696["Object/外部框架"] -->|extends| UserAuthDataChangedEvent_c696["UserAuthDataChangedEvent"]
    Serializable_p697["Serializable"] -->|implements| UserAuthDataChangedEvent_c697["UserAuthDataChangedEvent"]
    UserAuthDataChangedEvent_p698["UserAuthDataChangedEvent"] -->|extends| UserCredentialsInvalidationEvent_c698["UserCredentialsInvalidationEvent"]
    UserAuthDataChangedEvent_p699["UserAuthDataChangedEvent"] -->|extends| UserSessionInvalidationEvent_c699["UserSessionInvalidationEvent"]
    Object______p700["Object/外部框架"] -->|extends| JwtPair_c700["JwtPair"]
    Object______p701["Object/外部框架"] -->|extends| JwtSettings_c701["JwtSettings"]
    Serializable_p702["Serializable"] -->|extends| JwtToken_c702["JwtToken"]
    Object______p703["Object/外部框架"] -->|extends| SecuritySettings_c703["SecuritySettings"]
    Serializable_p704["Serializable"] -->|implements| SecuritySettings_c704["SecuritySettings"]
    Object______p705["Object/外部框架"] -->|extends| UserPasswordPolicy_c705["UserPasswordPolicy"]
    Serializable_p706["Serializable"] -->|implements| UserPasswordPolicy_c706["UserPasswordPolicy"]
    Object______p707["Object/外部框架"] -->|extends| PlatformTwoFaSettings_c707["PlatformTwoFaSettings"]
    Object______p708["Object/外部框架"] -->|extends| AccountTwoFaSettings_c708["AccountTwoFaSettings"]
    TwoFaAccountConfig_p709["TwoFaAccountConfig"] -->|extends| BackupCodeTwoFaAccountConfig_c709["BackupCodeTwoFaAccountConfig"]
    OtpBasedTwoFaAccountConfig_p710["OtpBasedTwoFaAccountConfig"] -->|extends| EmailTwoFaAccountConfig_c710["EmailTwoFaAccountConfig"]
    TwoFaAccountConfig_p711["TwoFaAccountConfig"] -->|extends| OtpBasedTwoFaAccountConfig_c711["OtpBasedTwoFaAccountConfig"]
    OtpBasedTwoFaAccountConfig_p712["OtpBasedTwoFaAccountConfig"] -->|extends| SmsTwoFaAccountConfig_c712["SmsTwoFaAccountConfig"]
    TwoFaAccountConfig_p713["TwoFaAccountConfig"] -->|extends| TotpTwoFaAccountConfig_c713["TotpTwoFaAccountConfig"]
    Object______p714["Object/外部框架"] -->|extends| TwoFaAccountConfig_c714["TwoFaAccountConfig"]
    Serializable_p715["Serializable"] -->|implements| TwoFaAccountConfig_c715["TwoFaAccountConfig"]
    Object______p716["Object/外部框架"] -->|extends| BackupCodeTwoFaProviderConfig_c716["BackupCodeTwoFaProviderConfig"]
    TwoFaProviderConfig_p717["TwoFaProviderConfig"] -->|implements| BackupCodeTwoFaProviderConfig_c717["BackupCodeTwoFaProviderConfig"]
    OtpBasedTwoFaProviderConfig_p718["OtpBasedTwoFaProviderConfig"] -->|extends| EmailTwoFaProviderConfig_c718["EmailTwoFaProviderConfig"]
    Object______p719["Object/外部框架"] -->|extends| OtpBasedTwoFaProviderConfig_c719["OtpBasedTwoFaProviderConfig"]
    TwoFaProviderConfig_p720["TwoFaProviderConfig"] -->|implements| OtpBasedTwoFaProviderConfig_c720["OtpBasedTwoFaProviderConfig"]
    OtpBasedTwoFaProviderConfig_p721["OtpBasedTwoFaProviderConfig"] -->|extends| SmsTwoFaProviderConfig_c721["SmsTwoFaProviderConfig"]
    Object______p722["Object/外部框架"] -->|extends| TotpTwoFaProviderConfig_c722["TotpTwoFaProviderConfig"]
    TwoFaProviderConfig_p723["TwoFaProviderConfig"] -->|implements| TotpTwoFaProviderConfig_c723["TotpTwoFaProviderConfig"]
    Object______p724["Object/外部框架"] -->|extends| AbstractUserDashboardInfo_c724["AbstractUserDashboardInfo"]
    HasTitle_p725["HasTitle"] -->|implements| AbstractUserDashboardInfo_c725["AbstractUserDashboardInfo"]
    Serializable_p726["Serializable"] -->|implements| AbstractUserDashboardInfo_c726["AbstractUserDashboardInfo"]
    AbstractUserDashboardInfo_p727["AbstractUserDashboardInfo"] -->|extends| LastVisitedDashboardInfo_c727["LastVisitedDashboardInfo"]
    Serializable_p728["Serializable"] -->|implements| LastVisitedDashboardInfo_c728["LastVisitedDashboardInfo"]
    AbstractUserDashboardInfo_p729["AbstractUserDashboardInfo"] -->|extends| StarredDashboardInfo_c729["StarredDashboardInfo"]
    Serializable_p730["Serializable"] -->|implements| StarredDashboardInfo_c730["StarredDashboardInfo"]
    Object______p731["Object/外部框架"] -->|extends| UserDashboardsInfo_c731["UserDashboardsInfo"]
    Serializable_p732["Serializable"] -->|implements| UserDashboardsInfo_c732["UserDashboardsInfo"]
    Object______p733["Object/外部框架"] -->|extends| UserSettings_c733["UserSettings"]
    Serializable_p734["Serializable"] -->|implements| UserSettings_c734["UserSettings"]
    Object______p735["Object/外部框架"] -->|extends| UserSettingsCompositeKey_c735["UserSettingsCompositeKey"]
    Serializable_p736["Serializable"] -->|implements| UserSettingsCompositeKey_c736["UserSettingsCompositeKey"]
    Object______p737["Object/外部框架"] -->|extends| AwsSnsSmsProviderConfiguration_c737["AwsSnsSmsProviderConfiguration"]
    SmsProviderConfiguration_p738["SmsProviderConfiguration"] -->|implements| AwsSnsSmsProviderConfiguration_c738["AwsSnsSmsProviderConfiguration"]
    Object______p739["Object/外部框架"] -->|extends| SmppSmsProviderConfiguration_c739["SmppSmsProviderConfiguration"]
    SmsProviderConfiguration_p740["SmsProviderConfiguration"] -->|implements| SmppSmsProviderConfiguration_c740["SmppSmsProviderConfiguration"]
    Object______p741["Object/外部框架"] -->|extends| TestSmsRequest_c741["TestSmsRequest"]
    Object______p742["Object/外部框架"] -->|extends| TwilioSmsProviderConfiguration_c742["TwilioSmsProviderConfiguration"]
    SmsProviderConfiguration_p743["SmsProviderConfiguration"] -->|implements| TwilioSmsProviderConfiguration_c743["TwilioSmsProviderConfiguration"]
    Object______p744["Object/外部框架"] -->|extends| AttributeExportData_c744["AttributeExportData"]
    EntityExportData_p745["EntityExportData"] -->|extends| DeviceExportData_c745["DeviceExportData"]
    Object______p746["Object/外部框架"] -->|extends| EntityExportData_c746["EntityExportData"]
    Object______p747["Object/外部框架"] -->|extends| EntityExportSettings_c747["EntityExportSettings"]
    Object______p748["Object/外部框架"] -->|extends| EntityImportResult_c748["EntityImportResult"]
    Object______p749["Object/外部框架"] -->|extends| EntityImportSettings_c749["EntityImportSettings"]
    EntityExportData_p750["EntityExportData"] -->|extends| RuleChainExportData_c750["RuleChainExportData"]
    EntityExportData_p751["EntityExportData"] -->|extends| WidgetTypeExportData_c751["WidgetTypeExportData"]
    EntityExportData_p752["EntityExportData"] -->|extends| WidgetsBundleExportData_c752["WidgetsBundleExportData"]
    Object______p753["Object/外部框架"] -->|extends| BulkImportRequest_c753["BulkImportRequest"]
    Object______p754["Object/外部框架"] -->|extends| Mapping_c754["Mapping"]
    Object______p755["Object/外部框架"] -->|extends| ColumnMapping_c755["ColumnMapping"]
    Object______p756["Object/外部框架"] -->|extends| BulkImportResult_c756["BulkImportResult"]
    HashMap_p757["HashMap"] -->|extends| AutoCommitSettings_c757["AutoCommitSettings"]
    Object______p758["Object/外部框架"] -->|extends| BranchInfo_c758["BranchInfo"]
    Object______p759["Object/外部框架"] -->|extends| EntityDataDiff_c759["EntityDataDiff"]
    Object______p760["Object/外部框架"] -->|extends| EntityDataInfo_c760["EntityDataInfo"]
    Object______p761["Object/外部框架"] -->|extends| EntityLoadError_c761["EntityLoadError"]
    Serializable_p762["Serializable"] -->|implements| EntityLoadError_c762["EntityLoadError"]
    Object______p763["Object/外部框架"] -->|extends| EntityTypeLoadResult_c763["EntityTypeLoadResult"]
    Serializable_p764["Serializable"] -->|implements| EntityTypeLoadResult_c764["EntityTypeLoadResult"]
    Object______p765["Object/外部框架"] -->|extends| EntityVersion_c765["EntityVersion"]
    Serializable_p766["Serializable"] -->|implements| EntityVersion_c766["EntityVersion"]
    Object______p767["Object/外部框架"] -->|extends| EntityVersionsDiff_c767["EntityVersionsDiff"]
    Object______p768["Object/外部框架"] -->|extends| RepositorySettings_c768["RepositorySettings"]
    Serializable_p769["Serializable"] -->|implements| RepositorySettings_c769["RepositorySettings"]
    Object______p770["Object/外部框架"] -->|extends| RepositorySettingsInfo_c770["RepositorySettingsInfo"]
    Object______p771["Object/外部框架"] -->|extends| VcUtils_c771["VcUtils"]
    Object______p772["Object/外部框架"] -->|extends| VersionCreationResult_c772["VersionCreationResult"]
    Serializable_p773["Serializable"] -->|implements| VersionCreationResult_c773["VersionCreationResult"]
    Object______p774["Object/外部框架"] -->|extends| VersionLoadResult_c774["VersionLoadResult"]
    Serializable_p775["Serializable"] -->|implements| VersionLoadResult_c775["VersionLoadResult"]
    Object______p776["Object/外部框架"] -->|extends| VersionedEntityInfo_c776["VersionedEntityInfo"]
    VersionCreateConfig_p777["VersionCreateConfig"] -->|extends| AutoVersionCreateConfig_c777["AutoVersionCreateConfig"]
    VersionCreateRequest_p778["VersionCreateRequest"] -->|extends| ComplexVersionCreateRequest_c778["ComplexVersionCreateRequest"]
    VersionCreateConfig_p779["VersionCreateConfig"] -->|extends| EntityTypeVersionCreateConfig_c779["EntityTypeVersionCreateConfig"]
    VersionCreateRequest_p780["VersionCreateRequest"] -->|extends| SingleEntityVersionCreateRequest_c780["SingleEntityVersionCreateRequest"]
    Object______p781["Object/外部框架"] -->|extends| VersionCreateConfig_c781["VersionCreateConfig"]
    Serializable_p782["Serializable"] -->|implements| VersionCreateConfig_c782["VersionCreateConfig"]
    Object______p783["Object/外部框架"] -->|extends| VersionCreateRequest_c783["VersionCreateRequest"]
    VersionLoadConfig_p784["VersionLoadConfig"] -->|extends| EntityTypeVersionLoadConfig_c784["EntityTypeVersionLoadConfig"]
    VersionLoadRequest_p785["VersionLoadRequest"] -->|extends| EntityTypeVersionLoadRequest_c785["EntityTypeVersionLoadRequest"]
    VersionLoadRequest_p786["VersionLoadRequest"] -->|extends| SingleEntityVersionLoadRequest_c786["SingleEntityVersionLoadRequest"]
    Object______p787["Object/外部框架"] -->|extends| VersionLoadConfig_c787["VersionLoadConfig"]
    Object______p788["Object/外部框架"] -->|extends| VersionLoadRequest_c788["VersionLoadRequest"]
    Object______p789["Object/外部框架"] -->|extends| DefaultTenantProfileConfiguration_c789["DefaultTenantProfileConfiguration"]
    TenantProfileConfiguration_p790["TenantProfileConfiguration"] -->|implements| DefaultTenantProfileConfiguration_c790["DefaultTenantProfileConfiguration"]
    Serializable_p791["Serializable"] -->|extends| TenantProfileConfiguration_c791["TenantProfileConfiguration"]
    Object______p792["Object/外部框架"] -->|extends| TenantProfileData_c792["TenantProfileData"]
    Serializable_p793["Serializable"] -->|implements| TenantProfileData_c793["TenantProfileData"]
    Object______p794["Object/外部框架"] -->|extends| TenantProfileQueueConfiguration_c794["TenantProfileQueueConfiguration"]
    Serializable_p795["Serializable"] -->|implements| TenantProfileQueueConfiguration_c795["TenantProfileQueueConfiguration"]
    Object______p796["Object/外部框架"] -->|extends| SnmpMapping_c796["SnmpMapping"]
    Serializable_p797["Serializable"] -->|implements| SnmpMapping_c797["SnmpMapping"]
    Object______p798["Object/外部框架"] -->|extends| MultipleMappingsSnmpCommunicationConfig_c798["MultipleMappingsSnmpCommunicationConfig"]
    SnmpCommunicationConfig_p799["SnmpCommunicationConfig"] -->|implements| MultipleMappingsSnmpCommunicationConfig_c799["MultipleMappingsSnmpCommunicationConfig"]
    MultipleMappingsSnmpCommunicationConfig_p800["MultipleMappingsSnmpCommunicationConfig"] -->|extends| RepeatingQueryingSnmpCommunicationConfig_c800["RepeatingQueryingSnmpCommunicationConfig"]
    Serializable_p801["Serializable"] -->|extends| SnmpCommunicationConfig_c801["SnmpCommunicationConfig"]
    MultipleMappingsSnmpCommunicationConfig_p802["MultipleMappingsSnmpCommunicationConfig"] -->|extends| ToServerRpcRequestSnmpCommunicationConfig_c802["ToServerRpcRequestSnmpCommunicationConfig"]
    RepeatingQueryingSnmpCommunicationConfig_p803["RepeatingQueryingSnmpCommunicationConfig"] -->|extends| ClientAttributesQueryingSnmpCommunicationConfig_c803["ClientAttributesQueryingSnmpCommunicationConfig"]
    MultipleMappingsSnmpCommunicationConfig_p804["MultipleMappingsSnmpCommunicationConfig"] -->|extends| SharedAttributesSettingSnmpCommunicationConfig_c804["SharedAttributesSettingSnmpCommunicationConfig"]
    RepeatingQueryingSnmpCommunicationConfig_p805["RepeatingQueryingSnmpCommunicationConfig"] -->|extends| TelemetryQueryingSnmpCommunicationConfig_c805["TelemetryQueryingSnmpCommunicationConfig"]
    MultipleMappingsSnmpCommunicationConfig_p806["MultipleMappingsSnmpCommunicationConfig"] -->|extends| ToDeviceRpcRequestSnmpCommunicationConfig_c806["ToDeviceRpcRequestSnmpCommunicationConfig"]
    Object______p807["Object/外部框架"] -->|extends| CollectionsUtil_c807["CollectionsUtil"]
    Object______p808["Object/外部框架"] -->|extends| ReflectionUtils_c808["ReflectionUtils"]
    Object______p809["Object/外部框架"] -->|extends| TbDDFFileParser_c809["TbDDFFileParser"]
    Object______p810["Object/外部框架"] -->|extends| TbPair_c810["TbPair"]
    Object______p811["Object/外部框架"] -->|extends| TemplateUtils_c811["TemplateUtils"]
    Object______p812["Object/外部框架"] -->|extends| TypeCastUtil_c812["TypeCastUtil"]
    BaseData_p813["BaseData"] -->|extends| BaseWidgetType_c813["BaseWidgetType"]
    HasName_p814["HasName"] -->|implements| BaseWidgetType_c814["BaseWidgetType"]
    HasTenantId_p815["HasTenantId"] -->|implements| BaseWidgetType_c815["BaseWidgetType"]
    BaseWidgetType_p816["BaseWidgetType"] -->|extends| WidgetType_c816["WidgetType"]
    WidgetType_p817["WidgetType"] -->|extends| WidgetTypeDetails_c817["WidgetTypeDetails"]
    HasName_p818["HasName"] -->|implements| WidgetTypeDetails_c818["WidgetTypeDetails"]
    HasTenantId_p819["HasTenantId"] -->|implements| WidgetTypeDetails_c819["WidgetTypeDetails"]
    HasImage_p820["HasImage"] -->|implements| WidgetTypeDetails_c820["WidgetTypeDetails"]
    ExportableEntity_p821["ExportableEntity"] -->|implements| WidgetTypeDetails_c821["WidgetTypeDetails"]
    BaseWidgetType_p822["BaseWidgetType"] -->|extends| WidgetTypeInfo_c822["WidgetTypeInfo"]
    BaseData_p823["BaseData"] -->|extends| WidgetsBundle_c823["WidgetsBundle"]
    HasName_p824["HasName"] -->|implements| WidgetsBundle_c824["WidgetsBundle"]
    HasTenantId_p825["HasTenantId"] -->|implements| WidgetsBundle_c825["WidgetsBundle"]
    ExportableEntity_p826["ExportableEntity"] -->|implements| WidgetsBundle_c826["WidgetsBundle"]
    HasTitle_p827["HasTitle"] -->|implements| WidgetsBundle_c827["WidgetsBundle"]
    HasImage_p828["HasImage"] -->|implements| WidgetsBundle_c828["WidgetsBundle"]
    Object______p829["Object/外部框架"] -->|extends| WidgetsBundleWidget_c829["WidgetsBundleWidget"]
    Object______p830["Object/外部框架"] -->|extends| DynamicProtoUtilsTest_c830["DynamicProtoUtilsTest"]
    Object______p831["Object/外部框架"] -->|extends| EntityTypeTest_c831["EntityTypeTest"]
    Object______p832["Object/外部框架"] -->|extends| StringUtilsTest_c832["StringUtilsTest"]
    Object______p833["Object/外部框架"] -->|extends| UUIDConverterTest_c833["UUIDConverterTest"]
    Object______p834["Object/外部框架"] -->|extends| ActionTypeTest_c834["ActionTypeTest"]
    Object______p835["Object/外部框架"] -->|extends| EntityIdTest_c835["EntityIdTest"]
    Object______p836["Object/外部框架"] -->|extends| TbMsgTypeTest_c836["TbMsgTypeTest"]
    Object______p837["Object/外部框架"] -->|extends| RpcStatusTest_c837["RpcStatusTest"]
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

- `BaseData.ObjectMapper()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.getCreatedTime()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.setCreatedTime()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.hashCode()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.equals()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.toString()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.StringBuilder()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseDataWithAdditionalInfo.getAdditionalInfo()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.getJson()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.setAdditionalInfo()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.equals()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.hashCode()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.ByteArrayInputStream()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `ContactBased.getCountry()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.setCountry()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.getState()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.setState()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.getCity()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.setCity()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.getAddress()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.setAddress()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.getAddress2()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.setAddress2()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.getZip()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.setZip()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.getPhone()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.setPhone()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.getEmail()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.setEmail()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ExportableEntity.setId()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/ExportableEntity.java`)
- `ExportableEntity.getExternalId()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/ExportableEntity.java`)
- `ExportableEntity.setExternalId()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/ExportableEntity.java`)
- `ExportableEntity.getCreatedTime()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/ExportableEntity.java`)
- `ExportableEntity.setCreatedTime()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/ExportableEntity.java`)
- `ExportableEntity.setTenantId()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/ExportableEntity.java`)
- `FstStatsService.incrementEncode()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/FstStatsService.java`)
- `FstStatsService.incrementDecode()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/FstStatsService.java`)
- `FstStatsService.recordEncodeTime()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/FstStatsService.java`)
- `FstStatsService.recordDecodeTime()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/FstStatsService.java`)
- `HasAdditionalInfo.getAdditionalInfo()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/HasAdditionalInfo.java`)


## 哪些方法可以重写

- `AdminSettings.getId()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.getCreatedTime()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.getTenantId()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.setTenantId()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.getKey()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.setKey()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.getJsonValue()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.setJsonValue()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.hashCode()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.equals()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.toString()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `AdminSettings.StringBuilder()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `ApiUsageRecordState.getValueAsString()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageRecordState.java`)
- `ApiUsageRecordState.valueAsString()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageRecordState.java`)
- `ApiUsageRecordState.getThresholdAsString()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageRecordState.java`)
- `ApiUsageRecordState.valueAsString()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageRecordState.java`)
- `ApiUsageState.isTransportEnabled()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageState.java`)
- `ApiUsageState.isReExecEnabled()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageState.java`)
- `ApiUsageState.isDbStorageEnabled()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageState.java`)
- `ApiUsageState.isJsExecEnabled()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageState.java`)
- `ApiUsageState.isTbelExecEnabled()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageState.java`)
- `ApiUsageState.isEmailSendEnabled()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageState.java`)
- `ApiUsageState.isSmsSendEnabled()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageState.java`)
- `ApiUsageState.isAlarmCreationEnabled()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageState.java`)
- `BaseData.ObjectMapper()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.getCreatedTime()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.setCreatedTime()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.hashCode()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.equals()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.toString()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.StringBuilder()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseDataWithAdditionalInfo.getAdditionalInfo()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.getJson()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.setAdditionalInfo()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.equals()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.hashCode()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.ByteArrayInputStream()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `ContactBased.getCountry()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.setCountry()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)
- `ContactBased.getState()` (public, `common/data/src/main/java/org/thingsboard/server/common/data/ContactBased.java`)


## 哪些方法必须重写

- `AdminSettings.StringBuilder()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/AdminSettings.java`)
- `ApiUsageRecordState.valueAsString()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageRecordState.java`)
- `ApiUsageRecordState.valueAsString()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/ApiUsageRecordState.java`)
- `BaseData.ObjectMapper()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseData.StringBuilder()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseData.java`)
- `BaseDataWithAdditionalInfo.getJson()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `BaseDataWithAdditionalInfo.ByteArrayInputStream()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/BaseDataWithAdditionalInfo.java`)
- `Customer.getAdditionalInfo()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/Customer.java`)
- `Customer.ShortCustomerInfo()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/Customer.java`)
- `Customer.StringBuilder()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/Customer.java`)
- `Dashboard.getChildObjects()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/Dashboard.java`)
- `Dashboard.getChildObjects()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/Dashboard.java`)
- `Dashboard.StringBuilder()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/Dashboard.java`)
- `DashboardInfo.ShortCustomerInfo()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DashboardInfo.java`)
- `DashboardInfo.StringBuilder()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DashboardInfo.java`)
- `Device.ByteArrayInputStream()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/Device.java`)
- `Device.StringBuilder()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/Device.java`)
- `DeviceIdInfo.TenantId()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DeviceIdInfo.java`)
- `DeviceIdInfo.DeviceId()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DeviceIdInfo.java`)
- `DeviceProfile.ByteArrayInputStream()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DeviceProfile.java`)
- `DeviceProfileInfo.TenantId()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DeviceProfileInfo.java`)
- `DynamicProtoUtils.Location()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.RuntimeException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.RuntimeException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.ProtoParser()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.ProtoParser()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.IllegalArgumentException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.IllegalArgumentException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.IllegalArgumentException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.IllegalArgumentException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.getEnumElements()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.getMessageTypes()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.IllegalArgumentException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.IllegalArgumentException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.IllegalArgumentException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.IllegalArgumentException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `DynamicProtoUtils.IllegalArgumentException()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/DynamicProtoUtils.java`)
- `EdgeUtils.EdgeEvent()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/EdgeUtils.java`)
- `EdgeUtils.StringBuilder()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/EdgeUtils.java`)
- `EntityFieldsData.ObjectMapper()` (package, `common/data/src/main/java/org/thingsboard/server/common/data/EntityFieldsData.java`)


## 哪些地方使用了多态

- `BaseData` -> `AdminSettings` (extends)
- `HasTenantId` -> `AdminSettings` (implements)
- `Object/外部框架` -> `ApiUsageRecordState` (extends)
- `Serializable` -> `ApiUsageRecordState` (implements)
- `BaseData` -> `ApiUsageState` (extends)
- `HasTenantId` -> `ApiUsageState` (implements)
- `Object/外部框架` -> `BaseData` (extends)
- `Serializable` -> `BaseData` (implements)
- `Object/外部框架` -> `BaseDataWithAdditionalInfo` (extends)
- `HasAdditionalInfo` -> `BaseDataWithAdditionalInfo` (implements)
- `Object/外部框架` -> `CacheConstants` (extends)
- `Object/外部框架` -> `ClaimRequest` (extends)
- `Object/外部框架` -> `ContactBased` (extends)
- `HasEmail` -> `ContactBased` (implements)
- `ContactBased` -> `Customer` (extends)
- `HasTenantId` -> `Customer` (implements)
- `ExportableEntity` -> `Customer` (implements)
- `HasTitle` -> `Customer` (implements)
- `DashboardInfo` -> `Dashboard` (extends)
- `ExportableEntity` -> `Dashboard` (implements)
- `BaseData` -> `DashboardInfo` (extends)
- `HasName` -> `DashboardInfo` (implements)
- `HasTenantId` -> `DashboardInfo` (implements)
- `HasTitle` -> `DashboardInfo` (implements)
- `HasImage` -> `DashboardInfo` (implements)
- `Object/外部框架` -> `DataConstants` (extends)
- `BaseDataWithAdditionalInfo` -> `Device` (extends)
- `HasLabel` -> `Device` (implements)
- `HasTenantId` -> `Device` (implements)
- `HasCustomerId` -> `Device` (implements)
- `HasOtaPackage` -> `Device` (implements)
- `ExportableEntity` -> `Device` (implements)
- `Object/外部框架` -> `DeviceIdInfo` (extends)
- `Serializable` -> `DeviceIdInfo` (implements)
- `HasTenantId` -> `DeviceIdInfo` (implements)
- `Device` -> `DeviceInfo` (extends)
- `Object/外部框架` -> `DeviceInfoFilter` (extends)
- `BaseData` -> `DeviceProfile` (extends)
- `HasName` -> `DeviceProfile` (implements)
- `HasTenantId` -> `DeviceProfile` (implements)
- 其余 40 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 外部父类/接口

- `Error`
- `Exception`
- `HashMap`
- `JsonDeserializer`
- `JsonSerializer`
- `Object/外部框架`
- `RuntimeException`
- `Serializable`


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
