# Thingsboard Server DAO Layer 模块继承体系分析

> 生成范围：`dao`  
> Maven artifact：`dao`  
> Java 类型数量：652  
> 分析方式：静态扫描 `class/interface/enum/record` 的 `extends`、`implements`、方法签名和源码触点。

## 完整继承树

```text
AbstractAlarmCommentEntity
├── AlarmCommentEntity
├── AlarmCommentInfoEntity
AbstractAlarmEntity
├── AlarmEntity
├── AlarmInfoEntity
AbstractAssetEntity
├── AssetEntity
├── AssetInfoEntity
AbstractDeviceEntity
├── DeviceEntity
├── DeviceInfoEntity
AbstractEdgeEntity
├── EdgeEntity
├── EdgeInfoEntity
AbstractListeningExecutor
├── CacheExecutorService
├── JpaExecutorService
├── JpaRelationQueryExecutorService
AbstractSingleColumnStandardBasicType
├── JsonBinaryType
├── JsonStringType
AbstractTenantEntity
├── TenantEntity
├── TenantInfoEntity
AbstractTsKvEntity
├── TimescaleTsKvEntity
├── TsKvEntity
├── TsKvLatestEntity
AbstractTypeDescriptor
├── JsonTypeDescriptor
AbstractWidgetTypeEntity
├── WidgetTypeDetailsEntity
├── WidgetTypeEntity
├── WidgetTypeInfoEntity
AdminSettingsService
├── «implements» AdminSettingsServiceImpl
AggregationTimeseriesDao
├── «implements» AbstractSqlTimeseriesDao
├── «implements» ├── AbstractChunkedAggregationTimeseriesDao
├── «implements» ├── ├── JpaSqlTimeseriesDao
├── «implements» ├── TimescaleTimeseriesDao
├── «implements» CassandraBaseTimeseriesDao
Alarm> extends BaseSqlEntity
├── public
AlarmComment> extends BaseSqlEntity
├── public
AlarmCommentService
├── «implements» BaseAlarmCommentService
AlarmQueryRepository
├── «implements» DefaultAlarmQueryRepository
AlarmService
├── «implements» BaseAlarmService
ApiLimitService
├── «implements» DefaultApiLimitService
ApiUsageStateService
├── «implements» ApiUsageStateServiceImpl
Asset> extends BaseSqlEntity
├── public
AssetProfileService
├── «implements» AssetProfileServiceImpl
AssetService
├── «implements» BaseAssetService
AsyncFunction
├── «implements» AggregatePartitionsFunction
AttributesDao
├── «implements» JpaAttributeDao
AttributesService
├── «implements» BaseAttributesService
├── «implements» CachedAttributesService
AuditLogService
├── «implements» AuditLogServiceImpl
├── «implements» DummyAuditLogServiceImpl
AuditLogSink
├── «implements» DummyAuditLogSink
├── «implements» ElasticsearchAuditLogSink
BaseEntity
├── «implements» AdminSettingsEntity
├── «implements» ApiUsageStateEntity
├── «implements» AuditLogEntity
├── «implements» DeviceCredentialsEntity
├── «implements» EdgeEventEntity
├── «implements» ErrorEventEntity
├── «implements» LifecycleEventEntity
├── «implements» RpcEntity
├── «implements» RuleChainDebugEventEntity
├── «implements» RuleNodeDebugEventEntity
├── «implements» StatisticsEventEntity
├── «implements» TbResourceInfoEntity
├── «implements» UserAuthSettingsEntity
├── «implements» UserCredentialsEntity
├── «implements» public
BaseSqlEntity
├── AdminSettingsEntity
├── ApiUsageStateEntity
├── AssetProfileEntity
├── AuditLogEntity
├── ComponentDescriptorEntity
├── CustomerEntity
├── DashboardEntity
├── DashboardInfoEntity
├── DeviceCredentialsEntity
├── DeviceProfileEntity
├── EdgeEventEntity
├── NotificationEntity
├── NotificationRequestEntity
├── ├── NotificationRequestInfoEntity
├── NotificationRuleEntity
├── ├── NotificationRuleInfoEntity
├── NotificationTargetEntity
├── NotificationTemplateEntity
├── OAuth2ClientRegistrationTemplateEntity
├── OAuth2DomainEntity
├── OAuth2MobileEntity
├── OAuth2ParamsEntity
├── OAuth2RegistrationEntity
├── OtaPackageEntity
├── OtaPackageInfoEntity
├── QueueEntity
├── RpcEntity
├── RuleChainEntity
├── RuleNodeEntity
├── RuleNodeStateEntity
├── TbResourceEntity
├── TbResourceInfoEntity
├── TenantProfileEntity
├── UserAuthSettingsEntity
├── UserCredentialsEntity
├── UserEntity
├── WidgetsBundleEntity
BaseWidgetType> extends BaseSqlEntity
├── public
CaffeineTbTransactionalCache
├── AlarmTypesCaffeineCache
├── AssetCaffeineCache
├── AssetProfileCaffeineCache
├── AttributeCaffeineCache
├── DashboardTitlesCaffeineCache
├── DeviceCredentialsCaffeineCache
├── DeviceProfileCaffeineCache
├── EdgeCaffeineCache
├── EntityCountCaffeineCache
├── EntityViewCaffeineCache
├── OtaPackageCaffeineCache
├── RelationCaffeineCache
├── TenantCaffeineCache
├── TenantExistsCaffeineCache
├── TenantProfileCaffeineCache
├── UserSettingsCaffeineCache
ClientRegistrationRepository
├── «implements» HybridClientRegistrationRepository
ComponentDescriptorInsertRepository
├── «implements» AbstractComponentDescriptorInsertRepository
├── «implements» ├── SqlComponentDescriptorInsertRepository
ComponentDescriptorService
├── «implements» BaseComponentDescriptorService
CustomerService
├── «implements» CustomerServiceImpl
Dao
├── AdminSettingsDao
├── ├── «implements» JpaAdminSettingsDao
├── AlarmCommentDao
├── ├── «implements» JpaAlarmCommentDao
├── AlarmDao
├── ├── «implements» JpaAlarmDao
├── ApiUsageStateDao
├── ├── «implements» JpaApiUsageStateDao
├── AssetDao
├── ├── «implements» JpaAssetDao
├── AssetProfileDao
├── ├── «implements» JpaAssetProfileDao
├── AuditLogDao
├── ├── «implements» JpaAuditLogDao
├── ComponentDescriptorDao
├── ├── «implements» JpaBaseComponentDescriptorDao
├── CustomerDao
├── ├── «implements» JpaCustomerDao
├── DashboardDao
├── ├── «implements» JpaDashboardDao
├── DashboardInfoDao
├── ├── «implements» JpaDashboardInfoDao
├── DeviceCredentialsDao
├── ├── «implements» JpaDeviceCredentialsDao
├── DeviceDao
├── ├── «implements» JpaDeviceDao
├── DeviceProfileDao
├── ├── «implements» JpaDeviceProfileDao
├── EdgeDao
├── ├── «implements» JpaEdgeDao
├── EdgeEventDao
├── ├── «implements» JpaBaseEdgeEventDao
├── EntityViewDao
├── ├── «implements» JpaEntityViewDao
├── NotificationDao
├── ├── «implements» JpaNotificationDao
├── NotificationRequestDao
├── ├── «implements» JpaNotificationRequestDao
├── NotificationRuleDao
├── ├── «implements» JpaNotificationRuleDao
├── NotificationTargetDao
├── ├── «implements» JpaNotificationTargetDao
├── NotificationTemplateDao
├── ├── «implements» JpaNotificationTemplateDao
├── OAuth2ClientRegistrationTemplateDao
├── ├── «implements» JpaOAuth2ClientRegistrationTemplateDao
├── OAuth2DomainDao
├── ├── «implements» JpaOAuth2DomainDao
├── OAuth2MobileDao
├── ├── «implements» JpaOAuth2MobileDao
├── OAuth2ParamsDao
├── ├── «implements» JpaOAuth2ParamsDao
├── OAuth2RegistrationDao
├── ├── «implements» JpaOAuth2RegistrationDao
├── OtaPackageDao
├── ├── «implements» JpaOtaPackageDao
├── OtaPackageInfoDao
├── ├── «implements» JpaOtaPackageInfoDao
├── QueueDao
├── ├── «implements» JpaQueueDao
├── RpcDao
├── ├── «implements» JpaRpcDao
├── RuleChainDao
├── ├── «implements» JpaRuleChainDao
├── RuleNodeDao
├── ├── «implements» JpaRuleNodeDao
├── RuleNodeStateDao
├── ├── «implements» JpaRuleNodeStateDao
├── TbResourceDao
├── ├── «implements» JpaTbResourceDao
├── TbResourceInfoDao
├── ├── «implements» JpaTbResourceInfoDao
├── TenantDao
├── ├── «implements» JpaTenantDao
├── TenantProfileDao
├── ├── «implements» JpaTenantProfileDao
├── UserAuthSettingsDao
├── ├── «implements» JpaUserAuthSettingsDao
├── UserCredentialsDao
├── ├── «implements» JpaUserCredentialsDao
├── UserDao
├── ├── «implements» JpaUserDao
├── WidgetTypeDao
├── ├── «implements» JpaWidgetTypeDao
├── WidgetsBundleDao
├── ├── «implements» JpaWidgetsBundleDao
DashboardService
├── «implements» DashboardServiceImpl
Device> extends BaseSqlEntity
├── public
DeviceConnectivityService
├── «implements» DeviceConnectivityServiceImpl
DeviceCredentialsService
├── «implements» DeviceCredentialsServiceImpl
DeviceProfileService
├── «implements» DeviceProfileServiceImpl
DeviceService
├── «implements» DeviceServiceImpl
DynamicParameterizedType
├── «implements» JsonBinaryType
├── «implements» JsonStringType
├── «implements» JsonTypeDescriptor
Edge> extends BaseSqlEntity
├── public
EdgeEventService
├── «implements» BaseEdgeEventService
EdgeService
├── «implements» EdgeServiceImpl
EdgeSynchronizationManager
├── «implements» DefaultEdgeSynchronizationManager
EntityCountService
├── «implements» BaseEntityCountService
EntityDaoService
├── «implements» DefaultNotificationRequestService
├── «implements» DefaultNotificationRuleService
├── «implements» DefaultNotificationService
├── «implements» DefaultNotificationTargetService
├── «implements» DefaultNotificationTemplateService
EntityQueryDao
├── «implements» JpaEntityQueryDao
EntityQueryRepository
├── «implements» DefaultEntityQueryRepository
EntityService
├── «implements» BaseEntityService
EntityServiceRegistry
├── «implements» DefaultEntityServiceRegistry
EntityViewService
├── «implements» EntityViewServiceImpl
Event>
├── public
EventCleanupRepository
├── «implements» SqlEventCleanupRepository
EventDao
├── «implements» JpaBaseEventDao
EventEntity
├── ErrorEventEntity
├── LifecycleEventEntity
├── RuleChainDebugEventEntity
├── RuleNodeDebugEventEntity
├── StatisticsEventEntity
EventRepository
├── ErrorEventRepository
├── LifecycleEventRepository
├── RuleChainDebugEventRepository
├── RuleNodeDebugEventRepository
├── StatisticsEventRepository
EventService
├── «implements» BaseEventService
Exception
├── TenantRateLimitException
ExportableEntityDao
├── AssetDao
├── ├── «implements» JpaAssetDao
├── AssetProfileDao
├── ├── «implements» JpaAssetProfileDao
├── CustomerDao
├── ├── «implements» JpaCustomerDao
├── DashboardDao
├── ├── «implements» JpaDashboardDao
├── DeviceDao
├── ├── «implements» JpaDeviceDao
├── DeviceProfileDao
├── ├── «implements» JpaDeviceProfileDao
├── EntityViewDao
├── ├── «implements» JpaEntityViewDao
├── NotificationRuleDao
├── ├── «implements» JpaNotificationRuleDao
├── NotificationTargetDao
├── ├── «implements» JpaNotificationTargetDao
├── NotificationTemplateDao
├── ├── «implements» JpaNotificationTemplateDao
├── RuleChainDao
├── ├── «implements» JpaRuleChainDao
├── TbResourceDao
├── ├── «implements» JpaTbResourceDao
├── WidgetTypeDao
├── ├── «implements» JpaWidgetTypeDao
├── WidgetsBundleDao
├── ├── «implements» JpaWidgetsBundleDao
ExportableEntityRepository
├── AssetProfileRepository
├── AssetRepository
├── CustomerRepository
├── DashboardRepository
├── DeviceProfileRepository
├── DeviceRepository
├── EntityViewRepository
├── NotificationRuleRepository
├── NotificationTargetRepository
├── NotificationTemplateRepository
├── RuleChainRepository
├── TbResourceRepository
├── WidgetTypeRepository
├── WidgetsBundleRepository
ImageContainerDao
├── AssetProfileDao
├── ├── «implements» JpaAssetProfileDao
├── DashboardInfoDao
├── ├── «implements» JpaDashboardInfoDao
├── DeviceProfileDao
├── ├── «implements» JpaDeviceProfileDao
├── WidgetTypeDao
├── ├── «implements» JpaWidgetTypeDao
├── WidgetsBundleDao
├── ├── «implements» JpaWidgetsBundleDao
ImageService
├── «implements» BaseImageService
InsertLatestTsRepository
├── «implements» SqlLatestInsertTsRepository
InsertTsRepository
├── «implements» SqlInsertTsRepository
├── «implements» TimescaleInsertTsRepository
JpaRepository
├── AdminSettingsRepository
├── AlarmCommentRepository
├── AlarmRepository
├── ApiUsageStateRepository
├── AssetProfileRepository
├── AssetRepository
├── AttributeKvRepository
├── AuditLogRepository
├── ComponentDescriptorRepository
├── CustomerRepository
├── DashboardInfoRepository
├── DashboardRepository
├── DeviceCredentialsRepository
├── DeviceProfileRepository
├── DeviceRepository
├── EdgeEventRepository
├── EdgeRepository
├── EntityAlarmRepository
├── EntityViewRepository
├── ErrorEventRepository
├── LifecycleEventRepository
├── NotificationRepository
├── NotificationRequestRepository
├── NotificationRuleRepository
├── NotificationTargetRepository
├── NotificationTemplateRepository
├── OAuth2ClientRegistrationTemplateRepository
├── OAuth2DomainRepository
├── OAuth2MobileRepository
├── OAuth2ParamsRepository
├── OAuth2RegistrationRepository
├── OtaPackageInfoRepository
├── OtaPackageRepository
├── QueueRepository
├── RelationRepository
├── RpcRepository
├── RuleChainDebugEventRepository
├── RuleChainRepository
├── RuleNodeDebugEventRepository
├── RuleNodeRepository
├── RuleNodeStateRepository
├── StatisticsEventRepository
├── TbResourceInfoRepository
├── TbResourceRepository
├── TenantProfileRepository
├── TenantRepository
├── TsKvDictionaryRepository
├── TsKvLatestRepository
├── TsKvRepository
├── TsKvTimescaleRepository
├── UserAuthSettingsRepository
├── UserCredentialsRepository
├── UserRepository
├── UserSettingsRepository
├── WidgetTypeInfoRepository
├── WidgetTypeRepository
├── WidgetsBundleRepository
├── WidgetsBundleWidgetRepository
JpaSpecificationExecutor
├── EdgeEventRepository
├── RelationRepository
NativeDeviceRepository
├── «implements» DefaultNativeDeviceRepository
NotificationRequestService
├── «implements» DefaultNotificationRequestService
NotificationRuleService
├── «implements» DefaultNotificationRuleService
NotificationService
├── «implements» DefaultNotificationService
NotificationSettingsService
├── «implements» DefaultNotificationSettingsService
NotificationTargetService
├── «implements» DefaultNotificationTargetService
NotificationTemplateService
├── «implements» DefaultNotificationTemplateService
OAuth2ConfigTemplateService
├── «implements» OAuth2ConfigTemplateServiceImpl
OAuth2Service
├── «implements» OAuth2ServiceImpl
Object/外部框架
├── AbstractBufferedRateExecutor
├── ├── CassandraBufferedRateReadExecutor
├── ├── CassandraBufferedRateWriteExecutor
├── AbstractCachedService
├── ├── UserSettingsServiceImpl
├── AbstractChunkedAggregationTimeseriesDaoTest
├── AbstractComponentDescriptorInsertRepository
├── ├── SqlComponentDescriptorInsertRepository
├── AbstractDaoServiceTest
├── AbstractEntityService
├── ├── AbstractCachedEntityService
├── ├── ├── AssetProfileServiceImpl
├── ├── ├── BaseAlarmService
├── ├── ├── BaseAssetService
├── ├── ├── BaseEntityCountService
├── ├── ├── BaseOtaPackageService
├── ├── ├── BaseResourceService
├── ├── ├── ├── BaseImageService
├── ├── ├── DeviceCredentialsServiceImpl
├── ├── ├── DeviceProfileServiceImpl
├── ├── ├── DeviceServiceImpl
├── ├── ├── EdgeServiceImpl
├── ├── ├── EntityViewServiceImpl
├── ├── ├── TenantProfileServiceImpl
├── ├── ├── TenantServiceImpl
├── ├── ApiUsageStateServiceImpl
├── ├── BaseAlarmCommentService
├── ├── BaseEntityService
├── ├── BaseQueueService
├── ├── BaseRuleChainService
├── ├── BaseRuleNodeStateService
├── ├── CustomerServiceImpl
├── ├── DashboardServiceImpl
├── ├── DefaultNotificationRuleService
├── ├── DefaultNotificationTargetService
├── ├── DefaultNotificationTemplateService
├── ├── OAuth2ConfigTemplateServiceImpl
├── ├── OAuth2ServiceImpl
├── ├── UserServiceImpl
├── AbstractEntityViewEntity
├── ├── EntityViewEntity
├── ├── EntityViewInfoEntity
├── AbstractHasOtaPackageValidator
├── ├── DeviceDataValidator
├── ├── DeviceProfileDataValidator
├── AbstractInsertRepository
├── ├── SqlInsertTsRepository
├── ├── SqlLatestInsertTsRepository
├── ├── TimescaleInsertTsRepository
├── AbstractJpaDaoTest
├── ├── JpaAlarmCommentDaoTest
├── ├── JpaAlarmDaoTest
├── ├── JpaAssetDaoTest
├── ├── JpaAuditLogDaoTest
├── ├── JpaBaseComponentDescriptorDaoTest
├── ├── JpaBaseEventDaoTest
├── ├── JpaCustomerDaoTest
├── ├── JpaDashboardInfoDaoTest
├── ├── JpaDeviceCredentialsDaoTest
├── ├── JpaDeviceDaoTest
├── ├── JpaRpcDaoTest
├── ├── JpaRuleNodeDaoTest
├── ├── JpaTenantDaoTest
├── ├── JpaUserCredentialsDaoTest
├── ├── JpaUserDaoTest
├── ├── JpaUserSettingsDaoTest
├── ├── JpaWidgetTypeDaoTest
├── ├── JpaWidgetsBundleDaoTest
├── AbstractJsonSqlTypeDescriptor
├── ├── JsonBinarySqlTypeDescriptor
├── ├── JsonStringSqlTypeDescriptor
├── AbstractNoSqlContainer
├── ├── NoSqlDaoServiceTestSuite
├── AbstractRedisContainer
├── ├── RedisSqlTestSuite
├── AbstractServiceTest
├── ├── AdminSettingsServiceTest
├── ├── AlarmCommentServiceTest
├── ├── AlarmServiceTest
├── ├── ApiUsageStateServiceTest
├── ├── AssetProfileServiceTest
├── ├── AssetServiceTest
├── ├── BaseAttributesServiceTest
├── ├── ├── AttributesServiceSqlTest
├── ├── BaseEventServiceTest
├── ├── ├── EventServiceSqlTest
├── ├── BaseTimeseriesServiceTest
├── ├── ├── TimeseriesServiceNoSqlTest
├── ├── ├── ├── TimeseriesServiceNoSqlSetNullEnabledTest
├── ├── ├── TimeseriesServiceSqlTest
├── ├── ├── TimeseriesServiceTimescaleTest
├── ├── CustomerServiceTest
├── ├── DashboardServiceTest
├── ├── DeviceCredentialsCacheTest
├── ├── DeviceCredentialsServiceTest
├── ├── DeviceProfileServiceTest
├── ├── DeviceServiceTest
├── ├── EdgeEventServiceTest
├── ├── EdgeServiceTest
├── ├── EntitiesSchemaSqlTest
├── ├── EntityServiceRegistryTest
├── ├── EntityServiceTest
├── ├── OAuth2ConfigTemplateServiceTest
├── ├── OAuth2ServiceTest
├── ├── OtaPackageServiceTest
├── ├── QueueServiceTest
├── ├── RelationCacheTest
├── ├── RelationServiceTest
├── ├── RuleChainServiceTest
├── ├── TbCacheSerializationTest
├── ├── TenantProfileServiceTest
├── ├── TenantServiceTest
├── ├── UserServiceTest
├── ├── WidgetTypeServiceTest
├── ├── WidgetsBundleServiceTest
├── ActionEntityEvent
├── AdminSettingsDataValidatorTest
├── AdminSettingsServiceImpl
├── AggregatePartitionsFunction
├── AggregationRepository
├── AggregationResult
├── AlarmDataAdapter
├── AlarmDataValidatorTest
├── AlarmTypesCacheEvictEvent
├── AssetCacheEvictEvent
├── AssetCacheKey
├── AssetDataValidatorTest
├── AssetProfileCacheKey
├── AssetProfileDataValidatorTest
├── AssetProfileEvictEvent
├── AssetTypeFilter
├── AsyncTaskContext
├── AttributeCacheKey
├── AttributeKvCompositeKey
├── AttributeKvEntity
├── AttributeKvInsertRepository
├── ├── SqlAttributesInsertRepository
├── AttributeUtils
├── AuditLogLevelFilter
├── AuditLogLevelProperties
├── AuditLogServiceImpl
├── BaseAttributesService
├── BaseComponentDescriptorService
├── BaseEdgeEventService
├── BaseEventService
├── BaseOtaPackageDataValidator
├── ├── OtaPackageDataValidator
├── ├── OtaPackageInfoDataValidator
├── BaseOtaPackageDataValidatorTest
├── BaseRelationService
├── BaseRpcService
├── BaseTimeseriesService
├── BasicUsageInfoService
├── BufferedRateExecutorStats
├── CacheCallback
├── CachedAttributesService
├── CassandraAbstractDao
├── ├── CassandraAbstractAsyncDao
├── ├── ├── AbstractCassandraBaseTimeseriesDao
├── ├── ├── ├── CassandraBaseTimeseriesDao
├── ├── ├── ├── CassandraBaseTimeseriesLatestDao
├── CassandraBaseTimeseriesDaoPartitioningDaysAlwaysExistsTest
├── CassandraBaseTimeseriesDaoPartitioningHoursAlwaysExistsTest
├── CassandraBaseTimeseriesDaoPartitioningIndefiniteAlwaysExistsTest
├── CassandraBaseTimeseriesDaoPartitioningMinutesAlwaysExistsTest
├── CassandraBaseTimeseriesDaoPartitioningMonthsAlwaysExistsTest
├── CassandraBaseTimeseriesDaoPartitioningYearsAlwaysExistsTest
├── CassandraPartitionCacheKey
├── CassandraPartitionsCacheTest
├── CassandraTsPartitionsCache
├── ClaimDataInfo
├── ComponentDescriptorDataValidatorTest
├── ConstraintValidator
├── ├── «implements» NoXssValidator
├── ├── «implements» StringLengthValidator
├── ConstraintValidatorTest
├── CustomerDataValidatorTest
├── DaoUtil
├── DashboardDataValidatorTest
├── DashboardTitleEvictEvent
├── DataValidator
├── ├── AdminSettingsDataValidator
├── ├── AlarmCommentDataValidator
├── ├── AlarmDataValidator
├── ├── ApiUsageDataValidator
├── ├── AssetDataValidator
├── ├── AssetProfileDataValidator
├── ├── AuditLogDataValidator
├── ├── ClientRegistrationTemplateDataValidator
├── ├── ComponentDescriptorDataValidator
├── ├── CustomerDataValidator
├── ├── DashboardDataValidator
├── ├── DeviceCredentialsDataValidator
├── ├── EdgeDataValidator
├── ├── EdgeEventDataValidator
├── ├── EntityViewDataValidator
├── ├── EventDataValidator
├── ├── NotificationRequestValidator
├── ├── QueueValidator
├── ├── ResourceDataValidator
├── ├── RuleChainDataValidator
├── ├── TenantDataValidator
├── ├── TenantProfileDataValidator
├── ├── UserCredentialsDataValidator
├── ├── UserDataValidator
├── ├── WidgetTypeDataValidator
├── ├── WidgetsBundleDataValidator
├── DataValidatorTest
├── DbCallStats
├── DbCallStatsSnapshot
├── DefaultAlarmQueryRepository
├── DefaultApiLimitService
├── DefaultEdgeSynchronizationManager
├── DefaultEntityQueryRepository
├── DefaultEntityQueryRepositoryTest
├── DefaultEntityServiceRegistry
├── DefaultNativeDeviceRepository
├── DefaultNotification
├── DefaultNotificationRequestService
├── DefaultNotificationService
├── DefaultNotificationSettingsService
├── DefaultNotifications
├── DefaultQueryLogComponent
├── DefaultQueryLogComponentTest
├── DefaultRule
├── DefaultTbTenantProfileCache
├── DeleteEntityEvent
├── DeleteEntityEventTest
├── DeviceConnectivityConfiguration
├── DeviceConnectivityInfo
├── DeviceConnectivityServiceImpl
├── DeviceConnectivityUtil
├── DeviceConnectivityUtilTest
├── DeviceCredentialsEvictEvent
├── DeviceDataValidatorTest
├── DeviceProfileCacheKey
├── DeviceProfileDataValidatorTest
├── DeviceProfileEvictEvent
├── DummyAuditLogServiceImpl
├── DummyAuditLogSink
├── EdgeCacheEvictEvent
├── EdgeCacheKey
├── EdgeDataValidatorTest
├── EdgeEventInsertRepository
├── ElasticsearchAuditLogSink
├── EntityAlarmCompositeKey
├── EntityAlarmEntity
├── EntityContainer
├── EntityCountCacheEvictEvent
├── EntityCountCacheKey
├── EntityDataAdapter
├── EntityDataAdapterTest
├── EntityKeyMapping
├── EntityKeyMappingTest
├── EntityRelationEvent
├── EntityViewCacheKey
├── EntityViewCacheValue
├── EntityViewDataValidatorTest
├── EntityViewEvictEvent
├── EventInsertRepository
├── EventPartitionConfiguration
├── HybridClientRegistrationRepository
├── IdComparator
├── ImageCacheKey
├── ImageUtils
├── JpaAbstractDao
├── ├── JpaAdminSettingsDao
├── ├── JpaAlarmDao
├── ├── JpaApiUsageStateDao
├── ├── JpaAssetDao
├── ├── JpaAssetProfileDao
├── ├── JpaBaseComponentDescriptorDao
├── ├── JpaCustomerDao
├── ├── JpaDashboardDao
├── ├── JpaDashboardInfoDao
├── ├── JpaDeviceCredentialsDao
├── ├── JpaDeviceDao
├── ├── JpaDeviceProfileDao
├── ├── JpaEdgeDao
├── ├── JpaEntityViewDao
├── ├── JpaNotificationRequestDao
├── ├── JpaNotificationRuleDao
├── ├── JpaNotificationTargetDao
├── ├── JpaNotificationTemplateDao
├── ├── JpaOAuth2ClientRegistrationTemplateDao
├── ├── JpaOAuth2DomainDao
├── ├── JpaOAuth2MobileDao
├── ├── JpaOAuth2ParamsDao
├── ├── JpaOAuth2RegistrationDao
├── ├── JpaOtaPackageDao
├── ├── JpaOtaPackageInfoDao
├── ├── JpaQueueDao
├── ├── JpaRpcDao
├── ├── JpaRuleChainDao
├── ├── JpaRuleNodeDao
├── ├── JpaRuleNodeStateDao
├── ├── JpaTbResourceDao
├── ├── JpaTbResourceInfoDao
├── ├── JpaTenantDao
├── ├── JpaTenantProfileDao
├── ├── JpaUserAuthSettingsDao
├── ├── JpaUserCredentialsDao
├── ├── JpaUserDao
├── ├── JpaWidgetTypeDao
├── ├── JpaWidgetsBundleDao
├── JpaAbstractDaoListeningExecutorService
├── ├── BaseAbstractSqlTimeseriesDao
├── ├── ├── AbstractSqlTimeseriesDao
├── ├── ├── ├── AbstractChunkedAggregationTimeseriesDao
├── ├── ├── ├── ├── JpaSqlTimeseriesDao
├── ├── ├── ├── TimescaleTimeseriesDao
├── ├── ├── SqlTimeseriesLatestDao
├── ├── JpaAttributeDao
├── ├── JpaRelationDao
├── ├── JpaUserSettingsDao
├── ├── SqlEventCleanupRepository
├── JpaBaseEventDao
├── JpaDaoConfig
├── JpaEntityQueryDao
├── JpaPartitionedAbstractDao
├── ├── JpaAlarmCommentDao
├── ├── JpaAuditLogDao
├── ├── JpaBaseEdgeEventDao
├── ├── JpaNotificationDao
├── JsonNodeProcessingTask
├── JsonPathProcessingTask
├── KvUtils
├── MethodCallStats
├── MethodCallStatsSnapshot
├── ModelConstants
├── NoXssValidator
├── NoXssValidatorTest
├── OAuth2Configuration
├── OAuth2Utils
├── OtaPackageCacheEvictEvent
├── OtaPackageCacheKey
├── PaginatedRemover
├── ├── CustomerDashboardsUnassigner
├── ├── CustomerDashboardsUpdater
├── Parameter
├── PostgreSqlInitializer
├── ProcessedImage
├── QueryContext
├── QueryCursor
├── ├── TsKvQueryCursor
├── QuerySecurityContext
├── RelationActionEvent
├── RelationCacheKey
├── RelationCacheValue
├── RelationCompositeKey
├── RelationEntity
├── RelationQueueCtx
├── RelationTask
├── ResourceDataValidatorTest
├── RuleChainDataValidatorTest
├── SaveEntityEvent
├── ScheduledLogExecutorComponent
├── SearchTsKvLatestRepository
├── SimpleListenableFuture
├── SqlDaoCallsAspect
├── SqlPartition
├── SqlPartitioningRepository
├── SqlRelationInsertRepository
├── SqlTimeseriesDaoConfig
├── SqlTsDaoConfig
├── SqlTsLatestDaoConfig
├── StringLengthValidator
├── TbSqlBlockingQueue
├── TbSqlBlockingQueueParams
├── TbSqlBlockingQueueWrapper
├── TbSqlQueueElement
├── TenantDataValidatorTest
├── TenantEvictEvent
├── TenantProfileCacheKey
├── TenantProfileDataValidatorTest
├── TenantProfileEvictEvent
├── TimePaginatedRemover
├── TimeUtils
├── TimeUtilsTest
├── TimescaleDaoConfig
├── TimescaleDaoServiceTestSuite
├── TimescaleSqlInitializer
├── TimescaleTsKvCompositeKey
├── TimescaleTsLatestDaoConfig
├── TsKey
├── TsKvCompositeKey
├── TsKvDictionary
├── TsKvDictionaryCompositeKey
├── TsKvLatestCompositeKey
├── UpdateResult
├── UserSettingsEntity
├── UserSettingsEvictEvent
├── Validator
├── WidgetTypeDataValidatorTest
├── WidgetTypeIdFqnEntity
├── WidgetTypeServiceImpl
├── WidgetsBundleDataValidatorTest
├── WidgetsBundleServiceImpl
├── WidgetsBundleWidgetCompositeKey
├── WidgetsBundleWidgetEntity
├── public
OtaPackageService
├── «implements» BaseOtaPackageService
PostgreSQL10Dialect
├── ThingsboardPostgreSQLDialect
QueryLogComponent
├── «implements» DefaultQueryLogComponent
QueueService
├── «implements» BaseQueueService
RedisTbTransactionalCache
├── AlarmTypesRedisCache
├── AssetProfileRedisCache
├── AssetRedisCache
├── AttributeRedisCache
├── DashboardTitlesRedisCache
├── DeviceCredentialsRedisCache
├── DeviceProfileRedisCache
├── EdgeRedisCache
├── EntityCountRedisCache
├── EntityViewRedisCache
├── OtaPackageRedisCache
├── RelationRedisCache
├── TenantExistsRedisCache
├── TenantProfileRedisCache
├── TenantRedisCache
├── UserSettingsRedisCache
RelationDao
├── «implements» JpaRelationDao
RelationInsertRepository
├── «implements» SqlRelationInsertRepository
RelationService
├── «implements» BaseRelationService
ResourceService
├── «implements» BaseResourceService
├── «implements» ├── BaseImageService
RpcService
├── «implements» BaseRpcService
RuleChainService
├── «implements» BaseRuleChainService
RuleNodeStateService
├── «implements» BaseRuleNodeStateService
RuntimeException
├── BufferLimitException
├── DataValidationException
├── ├── DeviceCredentialsValidationException
├── ├── EntitiesLimitException
├── DatabaseException
├── IncorrectParameterException
Serializable
├── «implements» AssetCacheKey
├── «implements» AssetProfileCacheKey
├── «implements» AttributeCacheKey
├── «implements» AttributeKvCompositeKey
├── «implements» AttributeKvEntity
├── «implements» DeviceProfileCacheKey
├── «implements» EdgeCacheKey
├── «implements» EntityAlarmCompositeKey
├── «implements» EntityCountCacheKey
├── «implements» EntityViewCacheKey
├── «implements» EntityViewCacheValue
├── «implements» OtaPackageCacheKey
├── «implements» RelationCacheKey
├── «implements» RelationCacheValue
├── «implements» RelationCompositeKey
├── «implements» TenantProfileCacheKey
├── «implements» TimescaleTsKvCompositeKey
├── «implements» TsKvCompositeKey
├── «implements» TsKvDictionaryCompositeKey
├── «implements» TsKvLatestCompositeKey
├── «implements» WidgetsBundleWidgetCompositeKey
SqlParameterSource
├── «implements» QueryContext
SqlTypeDescriptor
├── «implements» AbstractJsonSqlTypeDescriptor
├── «implements» ├── JsonBinarySqlTypeDescriptor
├── «implements» ├── JsonStringSqlTypeDescriptor
TbTenantProfileCache
├── «implements» DefaultTbTenantProfileCache
Tenant> extends BaseSqlEntity
├── public
TenantEntityDao
├── AssetDao
├── ├── «implements» JpaAssetDao
├── CustomerDao
├── ├── «implements» JpaCustomerDao
├── DashboardDao
├── ├── «implements» JpaDashboardDao
├── DeviceDao
├── ├── «implements» JpaDeviceDao
├── NotificationTargetDao
├── ├── «implements» JpaNotificationTargetDao
├── RuleChainDao
├── ├── «implements» JpaRuleChainDao
├── UserDao
├── ├── «implements» JpaUserDao
TenantEntityWithDataDao
├── OtaPackageDao
├── ├── «implements» JpaOtaPackageDao
├── TbResourceDao
├── ├── «implements» JpaTbResourceDao
TenantProfileProvider
├── «implements» DefaultTbTenantProfileCache
TenantProfileService
├── «implements» TenantProfileServiceImpl
TenantService
├── «implements» TenantServiceImpl
TimeseriesDao
├── «implements» AbstractChunkedAggregationTimeseriesDao
├── «implements» ├── JpaSqlTimeseriesDao
├── «implements» CassandraBaseTimeseriesDao
├── «implements» TimescaleTimeseriesDao
TimeseriesLatestDao
├── «implements» CassandraBaseTimeseriesLatestDao
├── «implements» SqlTimeseriesLatestDao
TimeseriesService
├── «implements» BaseTimeseriesService
ToData
├── «implements» AttributeKvEntity
├── «implements» EntityAlarmEntity
├── «implements» RelationEntity
├── «implements» UserSettingsEntity
├── «implements» WidgetsBundleWidgetEntity
├── «implements» public
UsageInfoService
├── «implements» BasicUsageInfoService
UserService
├── «implements» UserServiceImpl
UserSettingsDao
├── «implements» JpaUserSettingsDao
UserSettingsService
├── «implements» UserSettingsServiceImpl
WidgetTypeService
├── «implements» WidgetTypeServiceImpl
WidgetsBundleService
├── «implements» WidgetsBundleServiceImpl
```

## 继承图

```mermaid
flowchart TD
    Object______p0["Object/外部框架"] -->|extends| DaoUtil_c0["DaoUtil"]
    Object______p1["Object/外部框架"] -->|extends| JpaDaoConfig_c1["JpaDaoConfig"]
    Object______p2["Object/外部框架"] -->|extends| SqlTimeseriesDaoConfig_c2["SqlTimeseriesDaoConfig"]
    Object______p3["Object/外部框架"] -->|extends| SqlTsDaoConfig_c3["SqlTsDaoConfig"]
    Object______p4["Object/外部框架"] -->|extends| SqlTsLatestDaoConfig_c4["SqlTsLatestDaoConfig"]
    PostgreSQL10Dialect_p5["PostgreSQL10Dialect"] -->|extends| ThingsboardPostgreSQLDialect_c5["ThingsboardPostgreSQLDialect"]
    Object______p6["Object/外部框架"] -->|extends| TimescaleDaoConfig_c6["TimescaleDaoConfig"]
    Object______p7["Object/外部框架"] -->|extends| TimescaleTsLatestDaoConfig_c7["TimescaleTsLatestDaoConfig"]
    Dao_p8["Dao"] -->|extends| AlarmCommentDao_c8["AlarmCommentDao"]
    Dao_p9["Dao"] -->|extends| AlarmDao_c9["AlarmDao"]
    Object______p10["Object/外部框架"] -->|extends| AlarmTypesCacheEvictEvent_c10["AlarmTypesCacheEvictEvent"]
    CaffeineTbTransactionalCache_p11["CaffeineTbTransactionalCache"] -->|extends| AlarmTypesCaffeineCache_c11["AlarmTypesCaffeineCache"]
    RedisTbTransactionalCache_p12["RedisTbTransactionalCache"] -->|extends| AlarmTypesRedisCache_c12["AlarmTypesRedisCache"]
    AbstractEntityService_p13["AbstractEntityService"] -->|extends| BaseAlarmCommentService_c13["BaseAlarmCommentService"]
    AlarmCommentService_p14["AlarmCommentService"] -->|implements| BaseAlarmCommentService_c14["BaseAlarmCommentService"]
    AbstractCachedEntityService_p15["AbstractCachedEntityService"] -->|extends| BaseAlarmService_c15["BaseAlarmService"]
    AlarmService_p16["AlarmService"] -->|implements| BaseAlarmService_c16["BaseAlarmService"]
    Object______p17["Object/外部框架"] -->|extends| DbCallStats_c17["DbCallStats"]
    Object______p18["Object/外部框架"] -->|extends| DbCallStatsSnapshot_c18["DbCallStatsSnapshot"]
    Object______p19["Object/外部框架"] -->|extends| MethodCallStats_c19["MethodCallStats"]
    Object______p20["Object/外部框架"] -->|extends| MethodCallStatsSnapshot_c20["MethodCallStatsSnapshot"]
    Object______p21["Object/外部框架"] -->|extends| SqlDaoCallsAspect_c21["SqlDaoCallsAspect"]
    Object______p22["Object/外部框架"] -->|extends| AssetCacheEvictEvent_c22["AssetCacheEvictEvent"]
    Object______p23["Object/外部框架"] -->|extends| AssetCacheKey_c23["AssetCacheKey"]
    Serializable_p24["Serializable"] -->|implements| AssetCacheKey_c24["AssetCacheKey"]
    CaffeineTbTransactionalCache_p25["CaffeineTbTransactionalCache"] -->|extends| AssetCaffeineCache_c25["AssetCaffeineCache"]
    Dao_p26["Dao"] -->|extends| AssetDao_c26["AssetDao"]
    TenantEntityDao_p27["TenantEntityDao"] -->|extends| AssetDao_c27["AssetDao"]
    ExportableEntityDao_p28["ExportableEntityDao"] -->|extends| AssetDao_c28["AssetDao"]
    Object______p29["Object/外部框架"] -->|extends| AssetProfileCacheKey_c29["AssetProfileCacheKey"]
    Serializable_p30["Serializable"] -->|implements| AssetProfileCacheKey_c30["AssetProfileCacheKey"]
    CaffeineTbTransactionalCache_p31["CaffeineTbTransactionalCache"] -->|extends| AssetProfileCaffeineCache_c31["AssetProfileCaffeineCache"]
    Dao_p32["Dao"] -->|extends| AssetProfileDao_c32["AssetProfileDao"]
    ExportableEntityDao_p33["ExportableEntityDao"] -->|extends| AssetProfileDao_c33["AssetProfileDao"]
    ImageContainerDao_p34["ImageContainerDao"] -->|extends| AssetProfileDao_c34["AssetProfileDao"]
    Object______p35["Object/外部框架"] -->|extends| AssetProfileEvictEvent_c35["AssetProfileEvictEvent"]
    RedisTbTransactionalCache_p36["RedisTbTransactionalCache"] -->|extends| AssetProfileRedisCache_c36["AssetProfileRedisCache"]
    AbstractCachedEntityService_p37["AbstractCachedEntityService"] -->|extends| AssetProfileServiceImpl_c37["AssetProfileServiceImpl"]
    AssetProfileService_p38["AssetProfileService"] -->|implements| AssetProfileServiceImpl_c38["AssetProfileServiceImpl"]
    RedisTbTransactionalCache_p39["RedisTbTransactionalCache"] -->|extends| AssetRedisCache_c39["AssetRedisCache"]
    Object______p40["Object/外部框架"] -->|extends| AssetTypeFilter_c40["AssetTypeFilter"]
    AbstractCachedEntityService_p41["AbstractCachedEntityService"] -->|extends| BaseAssetService_c41["BaseAssetService"]
    AssetService_p42["AssetService"] -->|implements| BaseAssetService_c42["BaseAssetService"]
    Object______p43["Object/外部框架"] -->|extends| AttributeCacheKey_c43["AttributeCacheKey"]
    Serializable_p44["Serializable"] -->|implements| AttributeCacheKey_c44["AttributeCacheKey"]
    CaffeineTbTransactionalCache_p45["CaffeineTbTransactionalCache"] -->|extends| AttributeCaffeineCache_c45["AttributeCaffeineCache"]
    RedisTbTransactionalCache_p46["RedisTbTransactionalCache"] -->|extends| AttributeRedisCache_c46["AttributeRedisCache"]
    Object______p47["Object/外部框架"] -->|extends| AttributeUtils_c47["AttributeUtils"]
    Object______p48["Object/外部框架"] -->|extends| BaseAttributesService_c48["BaseAttributesService"]
    AttributesService_p49["AttributesService"] -->|implements| BaseAttributesService_c49["BaseAttributesService"]
    Object______p50["Object/外部框架"] -->|extends| CachedAttributesService_c50["CachedAttributesService"]
    AttributesService_p51["AttributesService"] -->|implements| CachedAttributesService_c51["CachedAttributesService"]
    Dao_p52["Dao"] -->|extends| AuditLogDao_c52["AuditLogDao"]
    Object______p53["Object/外部框架"] -->|extends| AuditLogLevelFilter_c53["AuditLogLevelFilter"]
    Object______p54["Object/外部框架"] -->|extends| AuditLogLevelProperties_c54["AuditLogLevelProperties"]
    Object______p55["Object/外部框架"] -->|extends| AuditLogServiceImpl_c55["AuditLogServiceImpl"]
    AuditLogService_p56["AuditLogService"] -->|implements| AuditLogServiceImpl_c56["AuditLogServiceImpl"]
    Object______p57["Object/外部框架"] -->|extends| DummyAuditLogServiceImpl_c57["DummyAuditLogServiceImpl"]
    AuditLogService_p58["AuditLogService"] -->|implements| DummyAuditLogServiceImpl_c58["DummyAuditLogServiceImpl"]
    Object______p59["Object/外部框架"] -->|extends| DummyAuditLogSink_c59["DummyAuditLogSink"]
    AuditLogSink_p60["AuditLogSink"] -->|implements| DummyAuditLogSink_c60["DummyAuditLogSink"]
    Object______p61["Object/外部框架"] -->|extends| ElasticsearchAuditLogSink_c61["ElasticsearchAuditLogSink"]
    AuditLogSink_p62["AuditLogSink"] -->|implements| ElasticsearchAuditLogSink_c62["ElasticsearchAuditLogSink"]
    AbstractListeningExecutor_p63["AbstractListeningExecutor"] -->|extends| CacheExecutorService_c63["CacheExecutorService"]
    Object______p64["Object/外部框架"] -->|extends| BaseComponentDescriptorService_c64["BaseComponentDescriptorService"]
    ComponentDescriptorService_p65["ComponentDescriptorService"] -->|implements| BaseComponentDescriptorService_c65["BaseComponentDescriptorService"]
    Dao_p66["Dao"] -->|extends| ComponentDescriptorDao_c66["ComponentDescriptorDao"]
    Dao_p67["Dao"] -->|extends| CustomerDao_c67["CustomerDao"]
    TenantEntityDao_p68["TenantEntityDao"] -->|extends| CustomerDao_c68["CustomerDao"]
    ExportableEntityDao_p69["ExportableEntityDao"] -->|extends| CustomerDao_c69["CustomerDao"]
    AbstractEntityService_p70["AbstractEntityService"] -->|extends| CustomerServiceImpl_c70["CustomerServiceImpl"]
    CustomerService_p71["CustomerService"] -->|implements| CustomerServiceImpl_c71["CustomerServiceImpl"]
    Dao_p72["Dao"] -->|extends| DashboardDao_c72["DashboardDao"]
    TenantEntityDao_p73["TenantEntityDao"] -->|extends| DashboardDao_c73["DashboardDao"]
    ExportableEntityDao_p74["ExportableEntityDao"] -->|extends| DashboardDao_c74["DashboardDao"]
    Dao_p75["Dao"] -->|extends| DashboardInfoDao_c75["DashboardInfoDao"]
    ImageContainerDao_p76["ImageContainerDao"] -->|extends| DashboardInfoDao_c76["DashboardInfoDao"]
    AbstractEntityService_p77["AbstractEntityService"] -->|extends| DashboardServiceImpl_c77["DashboardServiceImpl"]
    DashboardService_p78["DashboardService"] -->|implements| DashboardServiceImpl_c78["DashboardServiceImpl"]
    PaginatedRemover_p79["PaginatedRemover"] -->|extends| CustomerDashboardsUnassigner_c79["CustomerDashboardsUnassigner"]
    PaginatedRemover_p80["PaginatedRemover"] -->|extends| CustomerDashboardsUpdater_c80["CustomerDashboardsUpdater"]
    Object______p81["Object/外部框架"] -->|extends| DashboardTitleEvictEvent_c81["DashboardTitleEvictEvent"]
    CaffeineTbTransactionalCache_p82["CaffeineTbTransactionalCache"] -->|extends| DashboardTitlesCaffeineCache_c82["DashboardTitlesCaffeineCache"]
    RedisTbTransactionalCache_p83["RedisTbTransactionalCache"] -->|extends| DashboardTitlesRedisCache_c83["DashboardTitlesRedisCache"]
    Object______p84["Object/外部框架"] -->|extends| ClaimDataInfo_c84["ClaimDataInfo"]
    Object______p85["Object/外部框架"] -->|extends| DeviceConnectivityConfiguration_c85["DeviceConnectivityConfiguration"]
    Object______p86["Object/外部框架"] -->|extends| DeviceConnectivityInfo_c86["DeviceConnectivityInfo"]
    Object______p87["Object/外部框架"] -->|extends| DeviceConnectivityServiceImpl_c87["DeviceConnectivityServiceImpl"]
    DeviceConnectivityService_p88["DeviceConnectivityService"] -->|implements| DeviceConnectivityServiceImpl_c88["DeviceConnectivityServiceImpl"]
    CaffeineTbTransactionalCache_p89["CaffeineTbTransactionalCache"] -->|extends| DeviceCredentialsCaffeineCache_c89["DeviceCredentialsCaffeineCache"]
    Dao_p90["Dao"] -->|extends| DeviceCredentialsDao_c90["DeviceCredentialsDao"]
    Object______p91["Object/外部框架"] -->|extends| DeviceCredentialsEvictEvent_c91["DeviceCredentialsEvictEvent"]
    RedisTbTransactionalCache_p92["RedisTbTransactionalCache"] -->|extends| DeviceCredentialsRedisCache_c92["DeviceCredentialsRedisCache"]
    AbstractCachedEntityService_p93["AbstractCachedEntityService"] -->|extends| DeviceCredentialsServiceImpl_c93["DeviceCredentialsServiceImpl"]
    DeviceCredentialsService_p94["DeviceCredentialsService"] -->|implements| DeviceCredentialsServiceImpl_c94["DeviceCredentialsServiceImpl"]
    Dao_p95["Dao"] -->|extends| DeviceDao_c95["DeviceDao"]
    TenantEntityDao_p96["TenantEntityDao"] -->|extends| DeviceDao_c96["DeviceDao"]
    ExportableEntityDao_p97["ExportableEntityDao"] -->|extends| DeviceDao_c97["DeviceDao"]
    Object______p98["Object/外部框架"] -->|extends| DeviceProfileCacheKey_c98["DeviceProfileCacheKey"]
    Serializable_p99["Serializable"] -->|implements| DeviceProfileCacheKey_c99["DeviceProfileCacheKey"]
    CaffeineTbTransactionalCache_p100["CaffeineTbTransactionalCache"] -->|extends| DeviceProfileCaffeineCache_c100["DeviceProfileCaffeineCache"]
    Dao_p101["Dao"] -->|extends| DeviceProfileDao_c101["DeviceProfileDao"]
    ExportableEntityDao_p102["ExportableEntityDao"] -->|extends| DeviceProfileDao_c102["DeviceProfileDao"]
    ImageContainerDao_p103["ImageContainerDao"] -->|extends| DeviceProfileDao_c103["DeviceProfileDao"]
    Object______p104["Object/外部框架"] -->|extends| DeviceProfileEvictEvent_c104["DeviceProfileEvictEvent"]
    RedisTbTransactionalCache_p105["RedisTbTransactionalCache"] -->|extends| DeviceProfileRedisCache_c105["DeviceProfileRedisCache"]
    AbstractCachedEntityService_p106["AbstractCachedEntityService"] -->|extends| DeviceProfileServiceImpl_c106["DeviceProfileServiceImpl"]
    DeviceProfileService_p107["DeviceProfileService"] -->|implements| DeviceProfileServiceImpl_c107["DeviceProfileServiceImpl"]
    AbstractCachedEntityService_p108["AbstractCachedEntityService"] -->|extends| DeviceServiceImpl_c108["DeviceServiceImpl"]
    DeviceService_p109["DeviceService"] -->|implements| DeviceServiceImpl_c109["DeviceServiceImpl"]
    Object______p110["Object/外部框架"] -->|extends| BaseEdgeEventService_c110["BaseEdgeEventService"]
    EdgeEventService_p111["EdgeEventService"] -->|implements| BaseEdgeEventService_c111["BaseEdgeEventService"]
    Object______p112["Object/外部框架"] -->|extends| DefaultEdgeSynchronizationManager_c112["DefaultEdgeSynchronizationManager"]
    EdgeSynchronizationManager_p113["EdgeSynchronizationManager"] -->|implements| DefaultEdgeSynchronizationManager_c113["DefaultEdgeSynchronizationManager"]
    Object______p114["Object/外部框架"] -->|extends| EdgeCacheEvictEvent_c114["EdgeCacheEvictEvent"]
    Object______p115["Object/外部框架"] -->|extends| EdgeCacheKey_c115["EdgeCacheKey"]
    Serializable_p116["Serializable"] -->|implements| EdgeCacheKey_c116["EdgeCacheKey"]
    CaffeineTbTransactionalCache_p117["CaffeineTbTransactionalCache"] -->|extends| EdgeCaffeineCache_c117["EdgeCaffeineCache"]
    Dao_p118["Dao"] -->|extends| EdgeDao_c118["EdgeDao"]
    Dao_p119["Dao"] -->|extends| EdgeEventDao_c119["EdgeEventDao"]
    RedisTbTransactionalCache_p120["RedisTbTransactionalCache"] -->|extends| EdgeRedisCache_c120["EdgeRedisCache"]
    AbstractCachedEntityService_p121["AbstractCachedEntityService"] -->|extends| EdgeServiceImpl_c121["EdgeServiceImpl"]
    EdgeService_p122["EdgeService"] -->|implements| EdgeServiceImpl_c122["EdgeServiceImpl"]
    AbstractEntityService_p123["AbstractEntityService"] -->|extends| AbstractCachedEntityService_c123["AbstractCachedEntityService"]
    Object______p124["Object/外部框架"] -->|extends| AbstractCachedService_c124["AbstractCachedService"]
    Object______p125["Object/外部框架"] -->|extends| AbstractEntityService_c125["AbstractEntityService"]
    AbstractCachedEntityService_p126["AbstractCachedEntityService"] -->|extends| BaseEntityCountService_c126["BaseEntityCountService"]
    EntityCountService_p127["EntityCountService"] -->|implements| BaseEntityCountService_c127["BaseEntityCountService"]
    AbstractEntityService_p128["AbstractEntityService"] -->|extends| BaseEntityService_c128["BaseEntityService"]
    EntityService_p129["EntityService"] -->|implements| BaseEntityService_c129["BaseEntityService"]
    Object______p130["Object/外部框架"] -->|extends| DefaultEntityServiceRegistry_c130["DefaultEntityServiceRegistry"]
    EntityServiceRegistry_p131["EntityServiceRegistry"] -->|implements| DefaultEntityServiceRegistry_c131["DefaultEntityServiceRegistry"]
    Object______p132["Object/外部框架"] -->|extends| EntityCountCacheEvictEvent_c132["EntityCountCacheEvictEvent"]
    Object______p133["Object/外部框架"] -->|extends| EntityCountCacheKey_c133["EntityCountCacheKey"]
    Serializable_p134["Serializable"] -->|implements| EntityCountCacheKey_c134["EntityCountCacheKey"]
    CaffeineTbTransactionalCache_p135["CaffeineTbTransactionalCache"] -->|extends| EntityCountCaffeineCache_c135["EntityCountCaffeineCache"]
    RedisTbTransactionalCache_p136["RedisTbTransactionalCache"] -->|extends| EntityCountRedisCache_c136["EntityCountRedisCache"]
    Object______p137["Object/外部框架"] -->|extends| EntityViewCacheKey_c137["EntityViewCacheKey"]
    Serializable_p138["Serializable"] -->|implements| EntityViewCacheKey_c138["EntityViewCacheKey"]
    Object______p139["Object/外部框架"] -->|extends| EntityViewCacheValue_c139["EntityViewCacheValue"]
    Serializable_p140["Serializable"] -->|implements| EntityViewCacheValue_c140["EntityViewCacheValue"]
    CaffeineTbTransactionalCache_p141["CaffeineTbTransactionalCache"] -->|extends| EntityViewCaffeineCache_c141["EntityViewCaffeineCache"]
    Dao_p142["Dao"] -->|extends| EntityViewDao_c142["EntityViewDao"]
    ExportableEntityDao_p143["ExportableEntityDao"] -->|extends| EntityViewDao_c143["EntityViewDao"]
    Object______p144["Object/外部框架"] -->|extends| EntityViewEvictEvent_c144["EntityViewEvictEvent"]
    RedisTbTransactionalCache_p145["RedisTbTransactionalCache"] -->|extends| EntityViewRedisCache_c145["EntityViewRedisCache"]
    AbstractCachedEntityService_p146["AbstractCachedEntityService"] -->|extends| EntityViewServiceImpl_c146["EntityViewServiceImpl"]
    EntityViewService_p147["EntityViewService"] -->|implements| EntityViewServiceImpl_c147["EntityViewServiceImpl"]
    Object______p148["Object/外部框架"] -->|extends| BaseEventService_c148["BaseEventService"]
    EventService_p149["EventService"] -->|implements| BaseEventService_c149["BaseEventService"]
    Object______p150["Object/外部框架"] -->|extends| ActionEntityEvent_c150["ActionEntityEvent"]
    Object______p151["Object/外部框架"] -->|extends| DeleteEntityEvent_c151["DeleteEntityEvent"]
    Object______p152["Object/外部框架"] -->|extends| RelationActionEvent_c152["RelationActionEvent"]
    Object______p153["Object/外部框架"] -->|extends| SaveEntityEvent_c153["SaveEntityEvent"]
    RuntimeException_p154["RuntimeException"] -->|extends| BufferLimitException_c154["BufferLimitException"]
    RuntimeException_p155["RuntimeException"] -->|extends| DataValidationException_c155["DataValidationException"]
    RuntimeException_p156["RuntimeException"] -->|extends| DatabaseException_c156["DatabaseException"]
    DataValidationException_p157["DataValidationException"] -->|extends| DeviceCredentialsValidationException_c157["DeviceCredentialsValidationException"]
    DataValidationException_p158["DataValidationException"] -->|extends| EntitiesLimitException_c158["EntitiesLimitException"]
    RuntimeException_p159["RuntimeException"] -->|extends| IncorrectParameterException_c159["IncorrectParameterException"]
    Object______p160["Object/外部框架"] -->|extends| public_c160["public"]
    BaseEntity_p161["BaseEntity"] -->|implements| public_c161["public"]
    Object______p162["Object/外部框架"] -->|extends| ModelConstants_c162["ModelConstants"]
    AlarmComment__extends_BaseSqlEntity_p163["AlarmComment> extends BaseSqlEntity"] -->|extends| public_c163["public"]
    Alarm__extends_BaseSqlEntity_p164["Alarm> extends BaseSqlEntity"] -->|extends| public_c164["public"]
    Asset__extends_BaseSqlEntity_p165["Asset> extends BaseSqlEntity"] -->|extends| public_c165["public"]
    Device__extends_BaseSqlEntity_p166["Device> extends BaseSqlEntity"] -->|extends| public_c166["public"]
    Edge__extends_BaseSqlEntity_p167["Edge> extends BaseSqlEntity"] -->|extends| public_c167["public"]
    Object______p168["Object/外部框架"] -->|extends| AbstractEntityViewEntity_c168["AbstractEntityViewEntity"]
    Tenant__extends_BaseSqlEntity_p169["Tenant> extends BaseSqlEntity"] -->|extends| public_c169["public"]
    ToData_p170["ToData"] -->|implements| public_c170["public"]
    BaseWidgetType__extends_BaseSqlEntity_p171["BaseWidgetType> extends BaseSqlEntity"] -->|extends| public_c171["public"]
    BaseSqlEntity_p172["BaseSqlEntity"] -->|extends| AdminSettingsEntity_c172["AdminSettingsEntity"]
    BaseEntity_p173["BaseEntity"] -->|implements| AdminSettingsEntity_c173["AdminSettingsEntity"]
    AbstractAlarmCommentEntity_p174["AbstractAlarmCommentEntity"] -->|extends| AlarmCommentEntity_c174["AlarmCommentEntity"]
    AbstractAlarmCommentEntity_p175["AbstractAlarmCommentEntity"] -->|extends| AlarmCommentInfoEntity_c175["AlarmCommentInfoEntity"]
    AbstractAlarmEntity_p176["AbstractAlarmEntity"] -->|extends| AlarmEntity_c176["AlarmEntity"]
    AbstractAlarmEntity_p177["AbstractAlarmEntity"] -->|extends| AlarmInfoEntity_c177["AlarmInfoEntity"]
    BaseSqlEntity_p178["BaseSqlEntity"] -->|extends| ApiUsageStateEntity_c178["ApiUsageStateEntity"]
    BaseEntity_p179["BaseEntity"] -->|implements| ApiUsageStateEntity_c179["ApiUsageStateEntity"]
    AbstractAssetEntity_p180["AbstractAssetEntity"] -->|extends| AssetEntity_c180["AssetEntity"]
    AbstractAssetEntity_p181["AbstractAssetEntity"] -->|extends| AssetInfoEntity_c181["AssetInfoEntity"]
    BaseSqlEntity_p182["BaseSqlEntity"] -->|extends| AssetProfileEntity_c182["AssetProfileEntity"]
    Object______p183["Object/外部框架"] -->|extends| AttributeKvCompositeKey_c183["AttributeKvCompositeKey"]
    Serializable_p184["Serializable"] -->|implements| AttributeKvCompositeKey_c184["AttributeKvCompositeKey"]
    Object______p185["Object/外部框架"] -->|extends| AttributeKvEntity_c185["AttributeKvEntity"]
    ToData_p186["ToData"] -->|implements| AttributeKvEntity_c186["AttributeKvEntity"]
    Serializable_p187["Serializable"] -->|implements| AttributeKvEntity_c187["AttributeKvEntity"]
    BaseSqlEntity_p188["BaseSqlEntity"] -->|extends| AuditLogEntity_c188["AuditLogEntity"]
    BaseEntity_p189["BaseEntity"] -->|implements| AuditLogEntity_c189["AuditLogEntity"]
    BaseSqlEntity_p190["BaseSqlEntity"] -->|extends| ComponentDescriptorEntity_c190["ComponentDescriptorEntity"]
    BaseSqlEntity_p191["BaseSqlEntity"] -->|extends| CustomerEntity_c191["CustomerEntity"]
    BaseSqlEntity_p192["BaseSqlEntity"] -->|extends| DashboardEntity_c192["DashboardEntity"]
    BaseSqlEntity_p193["BaseSqlEntity"] -->|extends| DashboardInfoEntity_c193["DashboardInfoEntity"]
    BaseSqlEntity_p194["BaseSqlEntity"] -->|extends| DeviceCredentialsEntity_c194["DeviceCredentialsEntity"]
    BaseEntity_p195["BaseEntity"] -->|implements| DeviceCredentialsEntity_c195["DeviceCredentialsEntity"]
    AbstractDeviceEntity_p196["AbstractDeviceEntity"] -->|extends| DeviceEntity_c196["DeviceEntity"]
    AbstractDeviceEntity_p197["AbstractDeviceEntity"] -->|extends| DeviceInfoEntity_c197["DeviceInfoEntity"]
    BaseSqlEntity_p198["BaseSqlEntity"] -->|extends| DeviceProfileEntity_c198["DeviceProfileEntity"]
    AbstractEdgeEntity_p199["AbstractEdgeEntity"] -->|extends| EdgeEntity_c199["EdgeEntity"]
    BaseSqlEntity_p200["BaseSqlEntity"] -->|extends| EdgeEventEntity_c200["EdgeEventEntity"]
    BaseEntity_p201["BaseEntity"] -->|implements| EdgeEventEntity_c201["EdgeEventEntity"]
    AbstractEdgeEntity_p202["AbstractEdgeEntity"] -->|extends| EdgeInfoEntity_c202["EdgeInfoEntity"]
    Object______p203["Object/外部框架"] -->|extends| EntityAlarmCompositeKey_c203["EntityAlarmCompositeKey"]
    Serializable_p204["Serializable"] -->|implements| EntityAlarmCompositeKey_c204["EntityAlarmCompositeKey"]
    Object______p205["Object/外部框架"] -->|extends| EntityAlarmEntity_c205["EntityAlarmEntity"]
    ToData_p206["ToData"] -->|implements| EntityAlarmEntity_c206["EntityAlarmEntity"]
    AbstractEntityViewEntity_p207["AbstractEntityViewEntity"] -->|extends| EntityViewEntity_c207["EntityViewEntity"]
    AbstractEntityViewEntity_p208["AbstractEntityViewEntity"] -->|extends| EntityViewInfoEntity_c208["EntityViewInfoEntity"]
    EventEntity_p209["EventEntity"] -->|extends| ErrorEventEntity_c209["ErrorEventEntity"]
    BaseEntity_p210["BaseEntity"] -->|implements| ErrorEventEntity_c210["ErrorEventEntity"]
    Event__p211["Event>"] -->|extends| public_c211["public"]
    EventEntity_p212["EventEntity"] -->|extends| LifecycleEventEntity_c212["LifecycleEventEntity"]
    BaseEntity_p213["BaseEntity"] -->|implements| LifecycleEventEntity_c213["LifecycleEventEntity"]
    BaseSqlEntity_p214["BaseSqlEntity"] -->|extends| NotificationEntity_c214["NotificationEntity"]
    BaseSqlEntity_p215["BaseSqlEntity"] -->|extends| NotificationRequestEntity_c215["NotificationRequestEntity"]
    NotificationRequestEntity_p216["NotificationRequestEntity"] -->|extends| NotificationRequestInfoEntity_c216["NotificationRequestInfoEntity"]
    BaseSqlEntity_p217["BaseSqlEntity"] -->|extends| NotificationRuleEntity_c217["NotificationRuleEntity"]
    NotificationRuleEntity_p218["NotificationRuleEntity"] -->|extends| NotificationRuleInfoEntity_c218["NotificationRuleInfoEntity"]
    BaseSqlEntity_p219["BaseSqlEntity"] -->|extends| NotificationTargetEntity_c219["NotificationTargetEntity"]
    BaseSqlEntity_p220["BaseSqlEntity"] -->|extends| NotificationTemplateEntity_c220["NotificationTemplateEntity"]
    BaseSqlEntity_p221["BaseSqlEntity"] -->|extends| OAuth2ClientRegistrationTemplateEntity_c221["OAuth2ClientRegistrationTemplateEntity"]
    BaseSqlEntity_p222["BaseSqlEntity"] -->|extends| OAuth2DomainEntity_c222["OAuth2DomainEntity"]
    BaseSqlEntity_p223["BaseSqlEntity"] -->|extends| OAuth2MobileEntity_c223["OAuth2MobileEntity"]
    BaseSqlEntity_p224["BaseSqlEntity"] -->|extends| OAuth2ParamsEntity_c224["OAuth2ParamsEntity"]
    BaseSqlEntity_p225["BaseSqlEntity"] -->|extends| OAuth2RegistrationEntity_c225["OAuth2RegistrationEntity"]
    BaseSqlEntity_p226["BaseSqlEntity"] -->|extends| OtaPackageEntity_c226["OtaPackageEntity"]
    BaseSqlEntity_p227["BaseSqlEntity"] -->|extends| OtaPackageInfoEntity_c227["OtaPackageInfoEntity"]
    BaseSqlEntity_p228["BaseSqlEntity"] -->|extends| QueueEntity_c228["QueueEntity"]
    Object______p229["Object/外部框架"] -->|extends| RelationCompositeKey_c229["RelationCompositeKey"]
    Serializable_p230["Serializable"] -->|implements| RelationCompositeKey_c230["RelationCompositeKey"]
    Object______p231["Object/外部框架"] -->|extends| RelationEntity_c231["RelationEntity"]
    ToData_p232["ToData"] -->|implements| RelationEntity_c232["RelationEntity"]
    BaseSqlEntity_p233["BaseSqlEntity"] -->|extends| RpcEntity_c233["RpcEntity"]
    BaseEntity_p234["BaseEntity"] -->|implements| RpcEntity_c234["RpcEntity"]
    EventEntity_p235["EventEntity"] -->|extends| RuleChainDebugEventEntity_c235["RuleChainDebugEventEntity"]
    BaseEntity_p236["BaseEntity"] -->|implements| RuleChainDebugEventEntity_c236["RuleChainDebugEventEntity"]
    BaseSqlEntity_p237["BaseSqlEntity"] -->|extends| RuleChainEntity_c237["RuleChainEntity"]
    EventEntity_p238["EventEntity"] -->|extends| RuleNodeDebugEventEntity_c238["RuleNodeDebugEventEntity"]
    BaseEntity_p239["BaseEntity"] -->|implements| RuleNodeDebugEventEntity_c239["RuleNodeDebugEventEntity"]
    BaseSqlEntity_p240["BaseSqlEntity"] -->|extends| RuleNodeEntity_c240["RuleNodeEntity"]
    BaseSqlEntity_p241["BaseSqlEntity"] -->|extends| RuleNodeStateEntity_c241["RuleNodeStateEntity"]
    EventEntity_p242["EventEntity"] -->|extends| StatisticsEventEntity_c242["StatisticsEventEntity"]
    BaseEntity_p243["BaseEntity"] -->|implements| StatisticsEventEntity_c243["StatisticsEventEntity"]
    BaseSqlEntity_p244["BaseSqlEntity"] -->|extends| TbResourceEntity_c244["TbResourceEntity"]
    BaseSqlEntity_p245["BaseSqlEntity"] -->|extends| TbResourceInfoEntity_c245["TbResourceInfoEntity"]
    BaseEntity_p246["BaseEntity"] -->|implements| TbResourceInfoEntity_c246["TbResourceInfoEntity"]
    AbstractTenantEntity_p247["AbstractTenantEntity"] -->|extends| TenantEntity_c247["TenantEntity"]
    AbstractTenantEntity_p248["AbstractTenantEntity"] -->|extends| TenantInfoEntity_c248["TenantInfoEntity"]
    BaseSqlEntity_p249["BaseSqlEntity"] -->|extends| TenantProfileEntity_c249["TenantProfileEntity"]
    BaseSqlEntity_p250["BaseSqlEntity"] -->|extends| UserAuthSettingsEntity_c250["UserAuthSettingsEntity"]
    BaseEntity_p251["BaseEntity"] -->|implements| UserAuthSettingsEntity_c251["UserAuthSettingsEntity"]
    BaseSqlEntity_p252["BaseSqlEntity"] -->|extends| UserCredentialsEntity_c252["UserCredentialsEntity"]
    BaseEntity_p253["BaseEntity"] -->|implements| UserCredentialsEntity_c253["UserCredentialsEntity"]
    BaseSqlEntity_p254["BaseSqlEntity"] -->|extends| UserEntity_c254["UserEntity"]
    Object______p255["Object/外部框架"] -->|extends| UserSettingsEntity_c255["UserSettingsEntity"]
    ToData_p256["ToData"] -->|implements| UserSettingsEntity_c256["UserSettingsEntity"]
    AbstractWidgetTypeEntity_p257["AbstractWidgetTypeEntity"] -->|extends| WidgetTypeDetailsEntity_c257["WidgetTypeDetailsEntity"]
    AbstractWidgetTypeEntity_p258["AbstractWidgetTypeEntity"] -->|extends| WidgetTypeEntity_c258["WidgetTypeEntity"]
    Object______p259["Object/外部框架"] -->|extends| WidgetTypeIdFqnEntity_c259["WidgetTypeIdFqnEntity"]
    AbstractWidgetTypeEntity_p260["AbstractWidgetTypeEntity"] -->|extends| WidgetTypeInfoEntity_c260["WidgetTypeInfoEntity"]
    BaseSqlEntity_p261["BaseSqlEntity"] -->|extends| WidgetsBundleEntity_c261["WidgetsBundleEntity"]
    Object______p262["Object/外部框架"] -->|extends| WidgetsBundleWidgetCompositeKey_c262["WidgetsBundleWidgetCompositeKey"]
    Serializable_p263["Serializable"] -->|implements| WidgetsBundleWidgetCompositeKey_c263["WidgetsBundleWidgetCompositeKey"]
    Object______p264["Object/外部框架"] -->|extends| WidgetsBundleWidgetEntity_c264["WidgetsBundleWidgetEntity"]
    ToData_p265["ToData"] -->|implements| WidgetsBundleWidgetEntity_c265["WidgetsBundleWidgetEntity"]
    Object______p266["Object/外部框架"] -->|extends| TsKvDictionary_c266["TsKvDictionary"]
    Object______p267["Object/外部框架"] -->|extends| TsKvDictionaryCompositeKey_c267["TsKvDictionaryCompositeKey"]
    Serializable_p268["Serializable"] -->|implements| TsKvDictionaryCompositeKey_c268["TsKvDictionaryCompositeKey"]
    Object______p269["Object/外部框架"] -->|extends| TsKvLatestCompositeKey_c269["TsKvLatestCompositeKey"]
    Serializable_p270["Serializable"] -->|implements| TsKvLatestCompositeKey_c270["TsKvLatestCompositeKey"]
    AbstractTsKvEntity_p271["AbstractTsKvEntity"] -->|extends| TsKvLatestEntity_c271["TsKvLatestEntity"]
    Object______p272["Object/外部框架"] -->|extends| TimescaleTsKvCompositeKey_c272["TimescaleTsKvCompositeKey"]
    Serializable_p273["Serializable"] -->|implements| TimescaleTsKvCompositeKey_c273["TimescaleTsKvCompositeKey"]
    AbstractTsKvEntity_p274["AbstractTsKvEntity"] -->|extends| TimescaleTsKvEntity_c274["TimescaleTsKvEntity"]
    Object______p275["Object/外部框架"] -->|extends| TsKvCompositeKey_c275["TsKvCompositeKey"]
    Serializable_p276["Serializable"] -->|implements| TsKvCompositeKey_c276["TsKvCompositeKey"]
    AbstractTsKvEntity_p277["AbstractTsKvEntity"] -->|extends| TsKvEntity_c277["TsKvEntity"]
    CassandraAbstractDao_p278["CassandraAbstractDao"] -->|extends| CassandraAbstractAsyncDao_c278["CassandraAbstractAsyncDao"]
    Object______p279["Object/外部框架"] -->|extends| CassandraAbstractDao_c279["CassandraAbstractDao"]
    AbstractBufferedRateExecutor_p280["AbstractBufferedRateExecutor"] -->|extends| CassandraBufferedRateReadExecutor_c280["CassandraBufferedRateReadExecutor"]
    AbstractBufferedRateExecutor_p281["AbstractBufferedRateExecutor"] -->|extends| CassandraBufferedRateWriteExecutor_c281["CassandraBufferedRateWriteExecutor"]
    Object______p282["Object/外部框架"] -->|extends| DefaultNotificationRequestService_c282["DefaultNotificationRequestService"]
    NotificationRequestService_p283["NotificationRequestService"] -->|implements| DefaultNotificationRequestService_c283["DefaultNotificationRequestService"]
    EntityDaoService_p284["EntityDaoService"] -->|implements| DefaultNotificationRequestService_c284["DefaultNotificationRequestService"]
    DataValidator_p285["DataValidator"] -->|extends| NotificationRequestValidator_c285["NotificationRequestValidator"]
    AbstractEntityService_p286["AbstractEntityService"] -->|extends| DefaultNotificationRuleService_c286["DefaultNotificationRuleService"]
    NotificationRuleService_p287["NotificationRuleService"] -->|implements| DefaultNotificationRuleService_c287["DefaultNotificationRuleService"]
    EntityDaoService_p288["EntityDaoService"] -->|implements| DefaultNotificationRuleService_c288["DefaultNotificationRuleService"]
    Object______p289["Object/外部框架"] -->|extends| DefaultNotificationService_c289["DefaultNotificationService"]
    NotificationService_p290["NotificationService"] -->|implements| DefaultNotificationService_c290["DefaultNotificationService"]
    EntityDaoService_p291["EntityDaoService"] -->|implements| DefaultNotificationService_c291["DefaultNotificationService"]
    Object______p292["Object/外部框架"] -->|extends| DefaultNotificationSettingsService_c292["DefaultNotificationSettingsService"]
    NotificationSettingsService_p293["NotificationSettingsService"] -->|implements| DefaultNotificationSettingsService_c293["DefaultNotificationSettingsService"]
    AbstractEntityService_p294["AbstractEntityService"] -->|extends| DefaultNotificationTargetService_c294["DefaultNotificationTargetService"]
    NotificationTargetService_p295["NotificationTargetService"] -->|implements| DefaultNotificationTargetService_c295["DefaultNotificationTargetService"]
    EntityDaoService_p296["EntityDaoService"] -->|implements| DefaultNotificationTargetService_c296["DefaultNotificationTargetService"]
    AbstractEntityService_p297["AbstractEntityService"] -->|extends| DefaultNotificationTemplateService_c297["DefaultNotificationTemplateService"]
    NotificationTemplateService_p298["NotificationTemplateService"] -->|implements| DefaultNotificationTemplateService_c298["DefaultNotificationTemplateService"]
    EntityDaoService_p299["EntityDaoService"] -->|implements| DefaultNotificationTemplateService_c299["DefaultNotificationTemplateService"]
    Object______p300["Object/外部框架"] -->|extends| DefaultNotifications_c300["DefaultNotifications"]
    Object______p301["Object/外部框架"] -->|extends| DefaultNotification_c301["DefaultNotification"]
    Object______p302["Object/外部框架"] -->|extends| DefaultRule_c302["DefaultRule"]
    Dao_p303["Dao"] -->|extends| NotificationDao_c303["NotificationDao"]
    Dao_p304["Dao"] -->|extends| NotificationRequestDao_c304["NotificationRequestDao"]
    Dao_p305["Dao"] -->|extends| NotificationRuleDao_c305["NotificationRuleDao"]
    ExportableEntityDao_p306["ExportableEntityDao"] -->|extends| NotificationRuleDao_c306["NotificationRuleDao"]
    Dao_p307["Dao"] -->|extends| NotificationTargetDao_c307["NotificationTargetDao"]
    TenantEntityDao_p308["TenantEntityDao"] -->|extends| NotificationTargetDao_c308["NotificationTargetDao"]
    ExportableEntityDao_p309["ExportableEntityDao"] -->|extends| NotificationTargetDao_c309["NotificationTargetDao"]
    Dao_p310["Dao"] -->|extends| NotificationTemplateDao_c310["NotificationTemplateDao"]
    ExportableEntityDao_p311["ExportableEntityDao"] -->|extends| NotificationTemplateDao_c311["NotificationTemplateDao"]
    Object______p312["Object/外部框架"] -->|extends| HybridClientRegistrationRepository_c312["HybridClientRegistrationRepository"]
    ClientRegistrationRepository_p313["ClientRegistrationRepository"] -->|implements| HybridClientRegistrationRepository_c313["HybridClientRegistrationRepository"]
    Dao_p314["Dao"] -->|extends| OAuth2ClientRegistrationTemplateDao_c314["OAuth2ClientRegistrationTemplateDao"]
    AbstractEntityService_p315["AbstractEntityService"] -->|extends| OAuth2ConfigTemplateServiceImpl_c315["OAuth2ConfigTemplateServiceImpl"]
    OAuth2ConfigTemplateService_p316["OAuth2ConfigTemplateService"] -->|implements| OAuth2ConfigTemplateServiceImpl_c316["OAuth2ConfigTemplateServiceImpl"]
    Object______p317["Object/外部框架"] -->|extends| OAuth2Configuration_c317["OAuth2Configuration"]
    Dao_p318["Dao"] -->|extends| OAuth2DomainDao_c318["OAuth2DomainDao"]
    Dao_p319["Dao"] -->|extends| OAuth2MobileDao_c319["OAuth2MobileDao"]
    Dao_p320["Dao"] -->|extends| OAuth2ParamsDao_c320["OAuth2ParamsDao"]
    Dao_p321["Dao"] -->|extends| OAuth2RegistrationDao_c321["OAuth2RegistrationDao"]
    AbstractEntityService_p322["AbstractEntityService"] -->|extends| OAuth2ServiceImpl_c322["OAuth2ServiceImpl"]
    OAuth2Service_p323["OAuth2Service"] -->|implements| OAuth2ServiceImpl_c323["OAuth2ServiceImpl"]
    Object______p324["Object/外部框架"] -->|extends| OAuth2Utils_c324["OAuth2Utils"]
    AbstractCachedEntityService_p325["AbstractCachedEntityService"] -->|extends| BaseOtaPackageService_c325["BaseOtaPackageService"]
    OtaPackageService_p326["OtaPackageService"] -->|implements| BaseOtaPackageService_c326["BaseOtaPackageService"]
    Object______p327["Object/外部框架"] -->|extends| OtaPackageCacheEvictEvent_c327["OtaPackageCacheEvictEvent"]
    Object______p328["Object/外部框架"] -->|extends| OtaPackageCacheKey_c328["OtaPackageCacheKey"]
    Serializable_p329["Serializable"] -->|implements| OtaPackageCacheKey_c329["OtaPackageCacheKey"]
    CaffeineTbTransactionalCache_p330["CaffeineTbTransactionalCache"] -->|extends| OtaPackageCaffeineCache_c330["OtaPackageCaffeineCache"]
    Dao_p331["Dao"] -->|extends| OtaPackageDao_c331["OtaPackageDao"]
    TenantEntityWithDataDao_p332["TenantEntityWithDataDao"] -->|extends| OtaPackageDao_c332["OtaPackageDao"]
    Dao_p333["Dao"] -->|extends| OtaPackageInfoDao_c333["OtaPackageInfoDao"]
    RedisTbTransactionalCache_p334["RedisTbTransactionalCache"] -->|extends| OtaPackageRedisCache_c334["OtaPackageRedisCache"]
    AbstractEntityService_p335["AbstractEntityService"] -->|extends| BaseQueueService_c335["BaseQueueService"]
    QueueService_p336["QueueService"] -->|implements| BaseQueueService_c336["BaseQueueService"]
    Dao_p337["Dao"] -->|extends| QueueDao_c337["QueueDao"]
    Object______p338["Object/外部框架"] -->|extends| BaseRelationService_c338["BaseRelationService"]
    RelationService_p339["RelationService"] -->|implements| BaseRelationService_c339["BaseRelationService"]
    Object______p340["Object/外部框架"] -->|extends| RelationQueueCtx_c340["RelationQueueCtx"]
    Object______p341["Object/外部框架"] -->|extends| RelationTask_c341["RelationTask"]
    Object______p342["Object/外部框架"] -->|extends| EntityRelationEvent_c342["EntityRelationEvent"]
    Object______p343["Object/外部框架"] -->|extends| RelationCacheKey_c343["RelationCacheKey"]
    Serializable_p344["Serializable"] -->|implements| RelationCacheKey_c344["RelationCacheKey"]
    Object______p345["Object/外部框架"] -->|extends| RelationCacheValue_c345["RelationCacheValue"]
    Serializable_p346["Serializable"] -->|implements| RelationCacheValue_c346["RelationCacheValue"]
    CaffeineTbTransactionalCache_p347["CaffeineTbTransactionalCache"] -->|extends| RelationCaffeineCache_c347["RelationCaffeineCache"]
    RedisTbTransactionalCache_p348["RedisTbTransactionalCache"] -->|extends| RelationRedisCache_c348["RelationRedisCache"]
    BaseResourceService_p349["BaseResourceService"] -->|extends| BaseImageService_c349["BaseImageService"]
    ImageService_p350["ImageService"] -->|implements| BaseImageService_c350["BaseImageService"]
    Object______p351["Object/外部框架"] -->|extends| UpdateResult_c351["UpdateResult"]
    AbstractCachedEntityService_p352["AbstractCachedEntityService"] -->|extends| BaseResourceService_c352["BaseResourceService"]
    ResourceService_p353["ResourceService"] -->|implements| BaseResourceService_c353["BaseResourceService"]
    Object______p354["Object/外部框架"] -->|extends| ImageCacheKey_c354["ImageCacheKey"]
    Dao_p355["Dao"] -->|extends| TbResourceDao_c355["TbResourceDao"]
    TenantEntityWithDataDao_p356["TenantEntityWithDataDao"] -->|extends| TbResourceDao_c356["TbResourceDao"]
    ExportableEntityDao_p357["ExportableEntityDao"] -->|extends| TbResourceDao_c357["TbResourceDao"]
    Dao_p358["Dao"] -->|extends| TbResourceInfoDao_c358["TbResourceInfoDao"]
    Object______p359["Object/外部框架"] -->|extends| BaseRpcService_c359["BaseRpcService"]
    RpcService_p360["RpcService"] -->|implements| BaseRpcService_c360["BaseRpcService"]
    Dao_p361["Dao"] -->|extends| RpcDao_c361["RpcDao"]
    AbstractEntityService_p362["AbstractEntityService"] -->|extends| BaseRuleChainService_c362["BaseRuleChainService"]
    RuleChainService_p363["RuleChainService"] -->|implements| BaseRuleChainService_c363["BaseRuleChainService"]
    AbstractEntityService_p364["AbstractEntityService"] -->|extends| BaseRuleNodeStateService_c364["BaseRuleNodeStateService"]
    RuleNodeStateService_p365["RuleNodeStateService"] -->|implements| BaseRuleNodeStateService_c365["BaseRuleNodeStateService"]
    Dao_p366["Dao"] -->|extends| RuleChainDao_c366["RuleChainDao"]
    TenantEntityDao_p367["TenantEntityDao"] -->|extends| RuleChainDao_c367["RuleChainDao"]
    ExportableEntityDao_p368["ExportableEntityDao"] -->|extends| RuleChainDao_c368["RuleChainDao"]
    Dao_p369["Dao"] -->|extends| RuleNodeDao_c369["RuleNodeDao"]
    Dao_p370["Dao"] -->|extends| RuleNodeStateDao_c370["RuleNodeStateDao"]
    Object______p371["Object/外部框架"] -->|extends| ConstraintValidator_c371["ConstraintValidator"]
    Object______p372["Object/外部框架"] -->|extends| DataValidator_c372["DataValidator"]
    Object______p373["Object/外部框架"] -->|extends| NoXssValidator_c373["NoXssValidator"]
    ConstraintValidator_p374["ConstraintValidator"] -->|implements| NoXssValidator_c374["NoXssValidator"]
    Object______p375["Object/外部框架"] -->|extends| PaginatedRemover_c375["PaginatedRemover"]
    Object______p376["Object/外部框架"] -->|extends| StringLengthValidator_c376["StringLengthValidator"]
    ConstraintValidator_p377["ConstraintValidator"] -->|implements| StringLengthValidator_c377["StringLengthValidator"]
    Object______p378["Object/外部框架"] -->|extends| TimePaginatedRemover_c378["TimePaginatedRemover"]
    Object______p379["Object/外部框架"] -->|extends| Validator_c379["Validator"]
    Object______p380["Object/外部框架"] -->|extends| AbstractHasOtaPackageValidator_c380["AbstractHasOtaPackageValidator"]
    DataValidator_p381["DataValidator"] -->|extends| AdminSettingsDataValidator_c381["AdminSettingsDataValidator"]
    DataValidator_p382["DataValidator"] -->|extends| AlarmCommentDataValidator_c382["AlarmCommentDataValidator"]
    DataValidator_p383["DataValidator"] -->|extends| AlarmDataValidator_c383["AlarmDataValidator"]
    DataValidator_p384["DataValidator"] -->|extends| ApiUsageDataValidator_c384["ApiUsageDataValidator"]
    DataValidator_p385["DataValidator"] -->|extends| AssetDataValidator_c385["AssetDataValidator"]
    DataValidator_p386["DataValidator"] -->|extends| AssetProfileDataValidator_c386["AssetProfileDataValidator"]
    DataValidator_p387["DataValidator"] -->|extends| AuditLogDataValidator_c387["AuditLogDataValidator"]
    Object______p388["Object/外部框架"] -->|extends| BaseOtaPackageDataValidator_c388["BaseOtaPackageDataValidator"]
    DataValidator_p389["DataValidator"] -->|extends| ClientRegistrationTemplateDataValidator_c389["ClientRegistrationTemplateDataValidator"]
    DataValidator_p390["DataValidator"] -->|extends| ComponentDescriptorDataValidator_c390["ComponentDescriptorDataValidator"]
    DataValidator_p391["DataValidator"] -->|extends| CustomerDataValidator_c391["CustomerDataValidator"]
    DataValidator_p392["DataValidator"] -->|extends| DashboardDataValidator_c392["DashboardDataValidator"]
    DataValidator_p393["DataValidator"] -->|extends| DeviceCredentialsDataValidator_c393["DeviceCredentialsDataValidator"]
    AbstractHasOtaPackageValidator_p394["AbstractHasOtaPackageValidator"] -->|extends| DeviceDataValidator_c394["DeviceDataValidator"]
    AbstractHasOtaPackageValidator_p395["AbstractHasOtaPackageValidator"] -->|extends| DeviceProfileDataValidator_c395["DeviceProfileDataValidator"]
    DataValidator_p396["DataValidator"] -->|extends| EdgeDataValidator_c396["EdgeDataValidator"]
    DataValidator_p397["DataValidator"] -->|extends| EdgeEventDataValidator_c397["EdgeEventDataValidator"]
    DataValidator_p398["DataValidator"] -->|extends| EntityViewDataValidator_c398["EntityViewDataValidator"]
    DataValidator_p399["DataValidator"] -->|extends| EventDataValidator_c399["EventDataValidator"]
    BaseOtaPackageDataValidator_p400["BaseOtaPackageDataValidator"] -->|extends| OtaPackageDataValidator_c400["OtaPackageDataValidator"]
    BaseOtaPackageDataValidator_p401["BaseOtaPackageDataValidator"] -->|extends| OtaPackageInfoDataValidator_c401["OtaPackageInfoDataValidator"]
    DataValidator_p402["DataValidator"] -->|extends| QueueValidator_c402["QueueValidator"]
    DataValidator_p403["DataValidator"] -->|extends| ResourceDataValidator_c403["ResourceDataValidator"]
    DataValidator_p404["DataValidator"] -->|extends| RuleChainDataValidator_c404["RuleChainDataValidator"]
    DataValidator_p405["DataValidator"] -->|extends| TenantDataValidator_c405["TenantDataValidator"]
    DataValidator_p406["DataValidator"] -->|extends| TenantProfileDataValidator_c406["TenantProfileDataValidator"]
    DataValidator_p407["DataValidator"] -->|extends| UserCredentialsDataValidator_c407["UserCredentialsDataValidator"]
    DataValidator_p408["DataValidator"] -->|extends| UserDataValidator_c408["UserDataValidator"]
    DataValidator_p409["DataValidator"] -->|extends| WidgetTypeDataValidator_c409["WidgetTypeDataValidator"]
    DataValidator_p410["DataValidator"] -->|extends| WidgetsBundleDataValidator_c410["WidgetsBundleDataValidator"]
    Dao_p411["Dao"] -->|extends| AdminSettingsDao_c411["AdminSettingsDao"]
    Object______p412["Object/外部框架"] -->|extends| AdminSettingsServiceImpl_c412["AdminSettingsServiceImpl"]
    AdminSettingsService_p413["AdminSettingsService"] -->|implements| AdminSettingsServiceImpl_c413["AdminSettingsServiceImpl"]
    Object______p414["Object/外部框架"] -->|extends| JpaAbstractDao_c414["JpaAbstractDao"]
    Object______p415["Object/外部框架"] -->|extends| JpaAbstractDaoListeningExecutorService_c415["JpaAbstractDaoListeningExecutorService"]
    AbstractListeningExecutor_p416["AbstractListeningExecutor"] -->|extends| JpaExecutorService_c416["JpaExecutorService"]
    Object______p417["Object/外部框架"] -->|extends| JpaPartitionedAbstractDao_c417["JpaPartitionedAbstractDao"]
    Object______p418["Object/外部框架"] -->|extends| ScheduledLogExecutorComponent_c418["ScheduledLogExecutorComponent"]
    Object______p419["Object/外部框架"] -->|extends| TbSqlBlockingQueue_c419["TbSqlBlockingQueue"]
    Object______p420["Object/外部框架"] -->|extends| TbSqlBlockingQueueParams_c420["TbSqlBlockingQueueParams"]
    Object______p421["Object/外部框架"] -->|extends| TbSqlBlockingQueueWrapper_c421["TbSqlBlockingQueueWrapper"]
    Object______p422["Object/外部框架"] -->|extends| TbSqlQueueElement_c422["TbSqlQueueElement"]
    JpaRepository_p423["JpaRepository"] -->|extends| AlarmCommentRepository_c423["AlarmCommentRepository"]
    JpaRepository_p424["JpaRepository"] -->|extends| AlarmRepository_c424["AlarmRepository"]
    JpaRepository_p425["JpaRepository"] -->|extends| EntityAlarmRepository_c425["EntityAlarmRepository"]
    JpaPartitionedAbstractDao_p426["JpaPartitionedAbstractDao"] -->|extends| JpaAlarmCommentDao_c426["JpaAlarmCommentDao"]
    AlarmCommentDao_p427["AlarmCommentDao"] -->|implements| JpaAlarmCommentDao_c427["JpaAlarmCommentDao"]
    JpaAbstractDao_p428["JpaAbstractDao"] -->|extends| JpaAlarmDao_c428["JpaAlarmDao"]
    AlarmDao_p429["AlarmDao"] -->|implements| JpaAlarmDao_c429["JpaAlarmDao"]
    JpaRepository_p430["JpaRepository"] -->|extends| AssetProfileRepository_c430["AssetProfileRepository"]
    ExportableEntityRepository_p431["ExportableEntityRepository"] -->|extends| AssetProfileRepository_c431["AssetProfileRepository"]
    JpaRepository_p432["JpaRepository"] -->|extends| AssetRepository_c432["AssetRepository"]
    ExportableEntityRepository_p433["ExportableEntityRepository"] -->|extends| AssetRepository_c433["AssetRepository"]
    JpaAbstractDao_p434["JpaAbstractDao"] -->|extends| JpaAssetDao_c434["JpaAssetDao"]
    AssetDao_p435["AssetDao"] -->|implements| JpaAssetDao_c435["JpaAssetDao"]
    JpaAbstractDao_p436["JpaAbstractDao"] -->|extends| JpaAssetProfileDao_c436["JpaAssetProfileDao"]
    AssetProfileDao_p437["AssetProfileDao"] -->|implements| JpaAssetProfileDao_c437["JpaAssetProfileDao"]
    Object______p438["Object/外部框架"] -->|extends| AttributeKvInsertRepository_c438["AttributeKvInsertRepository"]
    JpaRepository_p439["JpaRepository"] -->|extends| AttributeKvRepository_c439["AttributeKvRepository"]
    JpaAbstractDaoListeningExecutorService_p440["JpaAbstractDaoListeningExecutorService"] -->|extends| JpaAttributeDao_c440["JpaAttributeDao"]
    AttributesDao_p441["AttributesDao"] -->|implements| JpaAttributeDao_c441["JpaAttributeDao"]
    AttributeKvInsertRepository_p442["AttributeKvInsertRepository"] -->|extends| SqlAttributesInsertRepository_c442["SqlAttributesInsertRepository"]
    JpaRepository_p443["JpaRepository"] -->|extends| AuditLogRepository_c443["AuditLogRepository"]
    JpaPartitionedAbstractDao_p444["JpaPartitionedAbstractDao"] -->|extends| JpaAuditLogDao_c444["JpaAuditLogDao"]
    AuditLogDao_p445["AuditLogDao"] -->|implements| JpaAuditLogDao_c445["JpaAuditLogDao"]
    Object______p446["Object/外部框架"] -->|extends| AbstractComponentDescriptorInsertRepository_c446["AbstractComponentDescriptorInsertRepository"]
    ComponentDescriptorInsertRepository_p447["ComponentDescriptorInsertRepository"] -->|implements| AbstractComponentDescriptorInsertRepository_c447["AbstractComponentDescriptorInsertRepository"]
    JpaRepository_p448["JpaRepository"] -->|extends| ComponentDescriptorRepository_c448["ComponentDescriptorRepository"]
    JpaAbstractDao_p449["JpaAbstractDao"] -->|extends| JpaBaseComponentDescriptorDao_c449["JpaBaseComponentDescriptorDao"]
    ComponentDescriptorDao_p450["ComponentDescriptorDao"] -->|implements| JpaBaseComponentDescriptorDao_c450["JpaBaseComponentDescriptorDao"]
    AbstractComponentDescriptorInsertRepository_p451["AbstractComponentDescriptorInsertRepository"] -->|extends| SqlComponentDescriptorInsertRepository_c451["SqlComponentDescriptorInsertRepository"]
    JpaRepository_p452["JpaRepository"] -->|extends| CustomerRepository_c452["CustomerRepository"]
    ExportableEntityRepository_p453["ExportableEntityRepository"] -->|extends| CustomerRepository_c453["CustomerRepository"]
    JpaAbstractDao_p454["JpaAbstractDao"] -->|extends| JpaCustomerDao_c454["JpaCustomerDao"]
    CustomerDao_p455["CustomerDao"] -->|implements| JpaCustomerDao_c455["JpaCustomerDao"]
    JpaRepository_p456["JpaRepository"] -->|extends| DashboardInfoRepository_c456["DashboardInfoRepository"]
    JpaRepository_p457["JpaRepository"] -->|extends| DashboardRepository_c457["DashboardRepository"]
    ExportableEntityRepository_p458["ExportableEntityRepository"] -->|extends| DashboardRepository_c458["DashboardRepository"]
    JpaAbstractDao_p459["JpaAbstractDao"] -->|extends| JpaDashboardDao_c459["JpaDashboardDao"]
    DashboardDao_p460["DashboardDao"] -->|implements| JpaDashboardDao_c460["JpaDashboardDao"]
    JpaAbstractDao_p461["JpaAbstractDao"] -->|extends| JpaDashboardInfoDao_c461["JpaDashboardInfoDao"]
    DashboardInfoDao_p462["DashboardInfoDao"] -->|implements| JpaDashboardInfoDao_c462["JpaDashboardInfoDao"]
    Object______p463["Object/外部框架"] -->|extends| DefaultNativeDeviceRepository_c463["DefaultNativeDeviceRepository"]
    NativeDeviceRepository_p464["NativeDeviceRepository"] -->|implements| DefaultNativeDeviceRepository_c464["DefaultNativeDeviceRepository"]
    JpaRepository_p465["JpaRepository"] -->|extends| DeviceCredentialsRepository_c465["DeviceCredentialsRepository"]
    JpaRepository_p466["JpaRepository"] -->|extends| DeviceProfileRepository_c466["DeviceProfileRepository"]
    ExportableEntityRepository_p467["ExportableEntityRepository"] -->|extends| DeviceProfileRepository_c467["DeviceProfileRepository"]
    JpaRepository_p468["JpaRepository"] -->|extends| DeviceRepository_c468["DeviceRepository"]
    ExportableEntityRepository_p469["ExportableEntityRepository"] -->|extends| DeviceRepository_c469["DeviceRepository"]
    JpaAbstractDao_p470["JpaAbstractDao"] -->|extends| JpaDeviceCredentialsDao_c470["JpaDeviceCredentialsDao"]
    DeviceCredentialsDao_p471["DeviceCredentialsDao"] -->|implements| JpaDeviceCredentialsDao_c471["JpaDeviceCredentialsDao"]
    JpaAbstractDao_p472["JpaAbstractDao"] -->|extends| JpaDeviceDao_c472["JpaDeviceDao"]
    DeviceDao_p473["DeviceDao"] -->|implements| JpaDeviceDao_c473["JpaDeviceDao"]
    JpaAbstractDao_p474["JpaAbstractDao"] -->|extends| JpaDeviceProfileDao_c474["JpaDeviceProfileDao"]
    DeviceProfileDao_p475["DeviceProfileDao"] -->|implements| JpaDeviceProfileDao_c475["JpaDeviceProfileDao"]
    Object______p476["Object/外部框架"] -->|extends| EdgeEventInsertRepository_c476["EdgeEventInsertRepository"]
    JpaRepository_p477["JpaRepository"] -->|extends| EdgeEventRepository_c477["EdgeEventRepository"]
    JpaSpecificationExecutor_p478["JpaSpecificationExecutor"] -->|extends| EdgeEventRepository_c478["EdgeEventRepository"]
    JpaRepository_p479["JpaRepository"] -->|extends| EdgeRepository_c479["EdgeRepository"]
    JpaPartitionedAbstractDao_p480["JpaPartitionedAbstractDao"] -->|extends| JpaBaseEdgeEventDao_c480["JpaBaseEdgeEventDao"]
    EdgeEventDao_p481["EdgeEventDao"] -->|implements| JpaBaseEdgeEventDao_c481["JpaBaseEdgeEventDao"]
    JpaAbstractDao_p482["JpaAbstractDao"] -->|extends| JpaEdgeDao_c482["JpaEdgeDao"]
    EdgeDao_p483["EdgeDao"] -->|implements| JpaEdgeDao_c483["JpaEdgeDao"]
    JpaRepository_p484["JpaRepository"] -->|extends| EntityViewRepository_c484["EntityViewRepository"]
    ExportableEntityRepository_p485["ExportableEntityRepository"] -->|extends| EntityViewRepository_c485["EntityViewRepository"]
    JpaAbstractDao_p486["JpaAbstractDao"] -->|extends| JpaEntityViewDao_c486["JpaEntityViewDao"]
    EntityViewDao_p487["EntityViewDao"] -->|implements| JpaEntityViewDao_c487["JpaEntityViewDao"]
    EventRepository_p488["EventRepository"] -->|extends| ErrorEventRepository_c488["ErrorEventRepository"]
    JpaRepository_p489["JpaRepository"] -->|extends| ErrorEventRepository_c489["ErrorEventRepository"]
    Object______p490["Object/外部框架"] -->|extends| EventInsertRepository_c490["EventInsertRepository"]
    Object______p491["Object/外部框架"] -->|extends| EventPartitionConfiguration_c491["EventPartitionConfiguration"]
    Object______p492["Object/外部框架"] -->|extends| JpaBaseEventDao_c492["JpaBaseEventDao"]
    EventDao_p493["EventDao"] -->|implements| JpaBaseEventDao_c493["JpaBaseEventDao"]
    EventRepository_p494["EventRepository"] -->|extends| LifecycleEventRepository_c494["LifecycleEventRepository"]
    JpaRepository_p495["JpaRepository"] -->|extends| LifecycleEventRepository_c495["LifecycleEventRepository"]
    EventRepository_p496["EventRepository"] -->|extends| RuleChainDebugEventRepository_c496["RuleChainDebugEventRepository"]
    JpaRepository_p497["JpaRepository"] -->|extends| RuleChainDebugEventRepository_c497["RuleChainDebugEventRepository"]
    EventRepository_p498["EventRepository"] -->|extends| RuleNodeDebugEventRepository_c498["RuleNodeDebugEventRepository"]
    JpaRepository_p499["JpaRepository"] -->|extends| RuleNodeDebugEventRepository_c499["RuleNodeDebugEventRepository"]
    JpaAbstractDaoListeningExecutorService_p500["JpaAbstractDaoListeningExecutorService"] -->|extends| SqlEventCleanupRepository_c500["SqlEventCleanupRepository"]
    EventCleanupRepository_p501["EventCleanupRepository"] -->|implements| SqlEventCleanupRepository_c501["SqlEventCleanupRepository"]
    EventRepository_p502["EventRepository"] -->|extends| StatisticsEventRepository_c502["StatisticsEventRepository"]
    JpaRepository_p503["JpaRepository"] -->|extends| StatisticsEventRepository_c503["StatisticsEventRepository"]
    JpaPartitionedAbstractDao_p504["JpaPartitionedAbstractDao"] -->|extends| JpaNotificationDao_c504["JpaNotificationDao"]
    NotificationDao_p505["NotificationDao"] -->|implements| JpaNotificationDao_c505["JpaNotificationDao"]
    JpaAbstractDao_p506["JpaAbstractDao"] -->|extends| JpaNotificationRequestDao_c506["JpaNotificationRequestDao"]
    NotificationRequestDao_p507["NotificationRequestDao"] -->|implements| JpaNotificationRequestDao_c507["JpaNotificationRequestDao"]
    JpaAbstractDao_p508["JpaAbstractDao"] -->|extends| JpaNotificationRuleDao_c508["JpaNotificationRuleDao"]
    NotificationRuleDao_p509["NotificationRuleDao"] -->|implements| JpaNotificationRuleDao_c509["JpaNotificationRuleDao"]
    JpaAbstractDao_p510["JpaAbstractDao"] -->|extends| JpaNotificationTargetDao_c510["JpaNotificationTargetDao"]
    NotificationTargetDao_p511["NotificationTargetDao"] -->|implements| JpaNotificationTargetDao_c511["JpaNotificationTargetDao"]
    JpaAbstractDao_p512["JpaAbstractDao"] -->|extends| JpaNotificationTemplateDao_c512["JpaNotificationTemplateDao"]
    NotificationTemplateDao_p513["NotificationTemplateDao"] -->|implements| JpaNotificationTemplateDao_c513["JpaNotificationTemplateDao"]
    JpaRepository_p514["JpaRepository"] -->|extends| NotificationRepository_c514["NotificationRepository"]
    JpaRepository_p515["JpaRepository"] -->|extends| NotificationRequestRepository_c515["NotificationRequestRepository"]
    JpaRepository_p516["JpaRepository"] -->|extends| NotificationRuleRepository_c516["NotificationRuleRepository"]
    ExportableEntityRepository_p517["ExportableEntityRepository"] -->|extends| NotificationRuleRepository_c517["NotificationRuleRepository"]
    JpaRepository_p518["JpaRepository"] -->|extends| NotificationTargetRepository_c518["NotificationTargetRepository"]
    ExportableEntityRepository_p519["ExportableEntityRepository"] -->|extends| NotificationTargetRepository_c519["NotificationTargetRepository"]
    JpaRepository_p520["JpaRepository"] -->|extends| NotificationTemplateRepository_c520["NotificationTemplateRepository"]
    ExportableEntityRepository_p521["ExportableEntityRepository"] -->|extends| NotificationTemplateRepository_c521["NotificationTemplateRepository"]
    JpaAbstractDao_p522["JpaAbstractDao"] -->|extends| JpaOAuth2ClientRegistrationTemplateDao_c522["JpaOAuth2ClientRegistrationTemplateDao"]
    OAuth2ClientRegistrationTemplateDao_p523["OAuth2ClientRegistrationTemplateDao"] -->|implements| JpaOAuth2ClientRegistrationTemplateDao_c523["JpaOAuth2ClientRegistrationTemplateDao"]
    JpaAbstractDao_p524["JpaAbstractDao"] -->|extends| JpaOAuth2DomainDao_c524["JpaOAuth2DomainDao"]
    OAuth2DomainDao_p525["OAuth2DomainDao"] -->|implements| JpaOAuth2DomainDao_c525["JpaOAuth2DomainDao"]
    JpaAbstractDao_p526["JpaAbstractDao"] -->|extends| JpaOAuth2MobileDao_c526["JpaOAuth2MobileDao"]
    OAuth2MobileDao_p527["OAuth2MobileDao"] -->|implements| JpaOAuth2MobileDao_c527["JpaOAuth2MobileDao"]
    JpaAbstractDao_p528["JpaAbstractDao"] -->|extends| JpaOAuth2ParamsDao_c528["JpaOAuth2ParamsDao"]
    OAuth2ParamsDao_p529["OAuth2ParamsDao"] -->|implements| JpaOAuth2ParamsDao_c529["JpaOAuth2ParamsDao"]
    JpaAbstractDao_p530["JpaAbstractDao"] -->|extends| JpaOAuth2RegistrationDao_c530["JpaOAuth2RegistrationDao"]
    OAuth2RegistrationDao_p531["OAuth2RegistrationDao"] -->|implements| JpaOAuth2RegistrationDao_c531["JpaOAuth2RegistrationDao"]
    JpaRepository_p532["JpaRepository"] -->|extends| OAuth2ClientRegistrationTemplateRepository_c532["OAuth2ClientRegistrationTemplateRepository"]
    JpaRepository_p533["JpaRepository"] -->|extends| OAuth2DomainRepository_c533["OAuth2DomainRepository"]
    JpaRepository_p534["JpaRepository"] -->|extends| OAuth2MobileRepository_c534["OAuth2MobileRepository"]
    JpaRepository_p535["JpaRepository"] -->|extends| OAuth2ParamsRepository_c535["OAuth2ParamsRepository"]
    JpaRepository_p536["JpaRepository"] -->|extends| OAuth2RegistrationRepository_c536["OAuth2RegistrationRepository"]
    JpaAbstractDao_p537["JpaAbstractDao"] -->|extends| JpaOtaPackageDao_c537["JpaOtaPackageDao"]
    OtaPackageDao_p538["OtaPackageDao"] -->|implements| JpaOtaPackageDao_c538["JpaOtaPackageDao"]
    JpaAbstractDao_p539["JpaAbstractDao"] -->|extends| JpaOtaPackageInfoDao_c539["JpaOtaPackageInfoDao"]
    OtaPackageInfoDao_p540["OtaPackageInfoDao"] -->|implements| JpaOtaPackageInfoDao_c540["JpaOtaPackageInfoDao"]
    JpaRepository_p541["JpaRepository"] -->|extends| OtaPackageInfoRepository_c541["OtaPackageInfoRepository"]
    JpaRepository_p542["JpaRepository"] -->|extends| OtaPackageRepository_c542["OtaPackageRepository"]
    Object______p543["Object/外部框架"] -->|extends| AlarmDataAdapter_c543["AlarmDataAdapter"]
    Object______p544["Object/外部框架"] -->|extends| DefaultAlarmQueryRepository_c544["DefaultAlarmQueryRepository"]
    AlarmQueryRepository_p545["AlarmQueryRepository"] -->|implements| DefaultAlarmQueryRepository_c545["DefaultAlarmQueryRepository"]
    Object______p546["Object/外部框架"] -->|extends| DefaultEntityQueryRepository_c546["DefaultEntityQueryRepository"]
    EntityQueryRepository_p547["EntityQueryRepository"] -->|implements| DefaultEntityQueryRepository_c547["DefaultEntityQueryRepository"]
    Object______p548["Object/外部框架"] -->|extends| DefaultQueryLogComponent_c548["DefaultQueryLogComponent"]
    QueryLogComponent_p549["QueryLogComponent"] -->|implements| DefaultQueryLogComponent_c549["DefaultQueryLogComponent"]
    Object______p550["Object/外部框架"] -->|extends| EntityDataAdapter_c550["EntityDataAdapter"]
    Object______p551["Object/外部框架"] -->|extends| EntityKeyMapping_c551["EntityKeyMapping"]
    Object______p552["Object/外部框架"] -->|extends| JpaEntityQueryDao_c552["JpaEntityQueryDao"]
    EntityQueryDao_p553["EntityQueryDao"] -->|implements| JpaEntityQueryDao_c553["JpaEntityQueryDao"]
    Object______p554["Object/外部框架"] -->|extends| QueryContext_c554["QueryContext"]
    SqlParameterSource_p555["SqlParameterSource"] -->|implements| QueryContext_c555["QueryContext"]
    Object______p556["Object/外部框架"] -->|extends| Parameter_c556["Parameter"]
    Object______p557["Object/外部框架"] -->|extends| QuerySecurityContext_c557["QuerySecurityContext"]
    JpaAbstractDao_p558["JpaAbstractDao"] -->|extends| JpaQueueDao_c558["JpaQueueDao"]
    QueueDao_p559["QueueDao"] -->|implements| JpaQueueDao_c559["JpaQueueDao"]
    JpaRepository_p560["JpaRepository"] -->|extends| QueueRepository_c560["QueueRepository"]
    JpaAbstractDaoListeningExecutorService_p561["JpaAbstractDaoListeningExecutorService"] -->|extends| JpaRelationDao_c561["JpaRelationDao"]
    RelationDao_p562["RelationDao"] -->|implements| JpaRelationDao_c562["JpaRelationDao"]
    AbstractListeningExecutor_p563["AbstractListeningExecutor"] -->|extends| JpaRelationQueryExecutorService_c563["JpaRelationQueryExecutorService"]
    JpaRepository_p564["JpaRepository"] -->|extends| RelationRepository_c564["RelationRepository"]
    JpaSpecificationExecutor_p565["JpaSpecificationExecutor"] -->|extends| RelationRepository_c565["RelationRepository"]
    Object______p566["Object/外部框架"] -->|extends| SqlRelationInsertRepository_c566["SqlRelationInsertRepository"]
    RelationInsertRepository_p567["RelationInsertRepository"] -->|implements| SqlRelationInsertRepository_c567["SqlRelationInsertRepository"]
    JpaAbstractDao_p568["JpaAbstractDao"] -->|extends| JpaTbResourceDao_c568["JpaTbResourceDao"]
    TbResourceDao_p569["TbResourceDao"] -->|implements| JpaTbResourceDao_c569["JpaTbResourceDao"]
    JpaAbstractDao_p570["JpaAbstractDao"] -->|extends| JpaTbResourceInfoDao_c570["JpaTbResourceInfoDao"]
    TbResourceInfoDao_p571["TbResourceInfoDao"] -->|implements| JpaTbResourceInfoDao_c571["JpaTbResourceInfoDao"]
    JpaRepository_p572["JpaRepository"] -->|extends| TbResourceInfoRepository_c572["TbResourceInfoRepository"]
    JpaRepository_p573["JpaRepository"] -->|extends| TbResourceRepository_c573["TbResourceRepository"]
    ExportableEntityRepository_p574["ExportableEntityRepository"] -->|extends| TbResourceRepository_c574["TbResourceRepository"]
    JpaAbstractDao_p575["JpaAbstractDao"] -->|extends| JpaRpcDao_c575["JpaRpcDao"]
    RpcDao_p576["RpcDao"] -->|implements| JpaRpcDao_c576["JpaRpcDao"]
    JpaRepository_p577["JpaRepository"] -->|extends| RpcRepository_c577["RpcRepository"]
    JpaAbstractDao_p578["JpaAbstractDao"] -->|extends| JpaRuleChainDao_c578["JpaRuleChainDao"]
    RuleChainDao_p579["RuleChainDao"] -->|implements| JpaRuleChainDao_c579["JpaRuleChainDao"]
    JpaAbstractDao_p580["JpaAbstractDao"] -->|extends| JpaRuleNodeDao_c580["JpaRuleNodeDao"]
    RuleNodeDao_p581["RuleNodeDao"] -->|implements| JpaRuleNodeDao_c581["JpaRuleNodeDao"]
    JpaAbstractDao_p582["JpaAbstractDao"] -->|extends| JpaRuleNodeStateDao_c582["JpaRuleNodeStateDao"]
    RuleNodeStateDao_p583["RuleNodeStateDao"] -->|implements| JpaRuleNodeStateDao_c583["JpaRuleNodeStateDao"]
    JpaRepository_p584["JpaRepository"] -->|extends| RuleChainRepository_c584["RuleChainRepository"]
    ExportableEntityRepository_p585["ExportableEntityRepository"] -->|extends| RuleChainRepository_c585["RuleChainRepository"]
    JpaRepository_p586["JpaRepository"] -->|extends| RuleNodeRepository_c586["RuleNodeRepository"]
    JpaRepository_p587["JpaRepository"] -->|extends| RuleNodeStateRepository_c587["RuleNodeStateRepository"]
    JpaRepository_p588["JpaRepository"] -->|extends| AdminSettingsRepository_c588["AdminSettingsRepository"]
    JpaAbstractDao_p589["JpaAbstractDao"] -->|extends| JpaAdminSettingsDao_c589["JpaAdminSettingsDao"]
    AdminSettingsDao_p590["AdminSettingsDao"] -->|implements| JpaAdminSettingsDao_c590["JpaAdminSettingsDao"]
    JpaAbstractDao_p591["JpaAbstractDao"] -->|extends| JpaTenantDao_c591["JpaTenantDao"]
    TenantDao_p592["TenantDao"] -->|implements| JpaTenantDao_c592["JpaTenantDao"]
    JpaAbstractDao_p593["JpaAbstractDao"] -->|extends| JpaTenantProfileDao_c593["JpaTenantProfileDao"]
    TenantProfileDao_p594["TenantProfileDao"] -->|implements| JpaTenantProfileDao_c594["JpaTenantProfileDao"]
    JpaRepository_p595["JpaRepository"] -->|extends| TenantProfileRepository_c595["TenantProfileRepository"]
    JpaRepository_p596["JpaRepository"] -->|extends| TenantRepository_c596["TenantRepository"]
    JpaRepository_p597["JpaRepository"] -->|extends| ApiUsageStateRepository_c597["ApiUsageStateRepository"]
    JpaAbstractDao_p598["JpaAbstractDao"] -->|extends| JpaApiUsageStateDao_c598["JpaApiUsageStateDao"]
    ApiUsageStateDao_p599["ApiUsageStateDao"] -->|implements| JpaApiUsageStateDao_c599["JpaApiUsageStateDao"]
    JpaAbstractDao_p600["JpaAbstractDao"] -->|extends| JpaUserAuthSettingsDao_c600["JpaUserAuthSettingsDao"]
    UserAuthSettingsDao_p601["UserAuthSettingsDao"] -->|implements| JpaUserAuthSettingsDao_c601["JpaUserAuthSettingsDao"]
    JpaAbstractDao_p602["JpaAbstractDao"] -->|extends| JpaUserCredentialsDao_c602["JpaUserCredentialsDao"]
    UserCredentialsDao_p603["UserCredentialsDao"] -->|implements| JpaUserCredentialsDao_c603["JpaUserCredentialsDao"]
    JpaAbstractDao_p604["JpaAbstractDao"] -->|extends| JpaUserDao_c604["JpaUserDao"]
    UserDao_p605["UserDao"] -->|implements| JpaUserDao_c605["JpaUserDao"]
    JpaAbstractDaoListeningExecutorService_p606["JpaAbstractDaoListeningExecutorService"] -->|extends| JpaUserSettingsDao_c606["JpaUserSettingsDao"]
    UserSettingsDao_p607["UserSettingsDao"] -->|implements| JpaUserSettingsDao_c607["JpaUserSettingsDao"]
    JpaRepository_p608["JpaRepository"] -->|extends| UserAuthSettingsRepository_c608["UserAuthSettingsRepository"]
    JpaRepository_p609["JpaRepository"] -->|extends| UserCredentialsRepository_c609["UserCredentialsRepository"]
    JpaRepository_p610["JpaRepository"] -->|extends| UserRepository_c610["UserRepository"]
    JpaRepository_p611["JpaRepository"] -->|extends| UserSettingsRepository_c611["UserSettingsRepository"]
    JpaAbstractDao_p612["JpaAbstractDao"] -->|extends| JpaWidgetTypeDao_c612["JpaWidgetTypeDao"]
    WidgetTypeDao_p613["WidgetTypeDao"] -->|implements| JpaWidgetTypeDao_c613["JpaWidgetTypeDao"]
    JpaAbstractDao_p614["JpaAbstractDao"] -->|extends| JpaWidgetsBundleDao_c614["JpaWidgetsBundleDao"]
    WidgetsBundleDao_p615["WidgetsBundleDao"] -->|implements| JpaWidgetsBundleDao_c615["JpaWidgetsBundleDao"]
    JpaRepository_p616["JpaRepository"] -->|extends| WidgetTypeInfoRepository_c616["WidgetTypeInfoRepository"]
    JpaRepository_p617["JpaRepository"] -->|extends| WidgetTypeRepository_c617["WidgetTypeRepository"]
    ExportableEntityRepository_p618["ExportableEntityRepository"] -->|extends| WidgetTypeRepository_c618["WidgetTypeRepository"]
    JpaRepository_p619["JpaRepository"] -->|extends| WidgetsBundleRepository_c619["WidgetsBundleRepository"]
    ExportableEntityRepository_p620["ExportableEntityRepository"] -->|extends| WidgetsBundleRepository_c620["WidgetsBundleRepository"]
    JpaRepository_p621["JpaRepository"] -->|extends| WidgetsBundleWidgetRepository_c621["WidgetsBundleWidgetRepository"]
    AbstractSqlTimeseriesDao_p622["AbstractSqlTimeseriesDao"] -->|extends| AbstractChunkedAggregationTimeseriesDao_c622["AbstractChunkedAggregationTimeseriesDao"]
    TimeseriesDao_p623["TimeseriesDao"] -->|implements| AbstractChunkedAggregationTimeseriesDao_c623["AbstractChunkedAggregationTimeseriesDao"]
    BaseAbstractSqlTimeseriesDao_p624["BaseAbstractSqlTimeseriesDao"] -->|extends| AbstractSqlTimeseriesDao_c624["AbstractSqlTimeseriesDao"]
    AggregationTimeseriesDao_p625["AggregationTimeseriesDao"] -->|implements| AbstractSqlTimeseriesDao_c625["AbstractSqlTimeseriesDao"]
    JpaAbstractDaoListeningExecutorService_p626["JpaAbstractDaoListeningExecutorService"] -->|extends| BaseAbstractSqlTimeseriesDao_c626["BaseAbstractSqlTimeseriesDao"]
    Object______p627["Object/外部框架"] -->|extends| EntityContainer_c627["EntityContainer"]
    BaseAbstractSqlTimeseriesDao_p628["BaseAbstractSqlTimeseriesDao"] -->|extends| SqlTimeseriesLatestDao_c628["SqlTimeseriesLatestDao"]
    TimeseriesLatestDao_p629["TimeseriesLatestDao"] -->|implements| SqlTimeseriesLatestDao_c629["SqlTimeseriesLatestDao"]
    Object______p630["Object/外部框架"] -->|extends| TsKey_c630["TsKey"]
    JpaRepository_p631["JpaRepository"] -->|extends| TsKvDictionaryRepository_c631["TsKvDictionaryRepository"]
    Object______p632["Object/外部框架"] -->|extends| AbstractInsertRepository_c632["AbstractInsertRepository"]
    AbstractInsertRepository_p633["AbstractInsertRepository"] -->|extends| SqlLatestInsertTsRepository_c633["SqlLatestInsertTsRepository"]
    InsertLatestTsRepository_p634["InsertLatestTsRepository"] -->|implements| SqlLatestInsertTsRepository_c634["SqlLatestInsertTsRepository"]
    AbstractInsertRepository_p635["AbstractInsertRepository"] -->|extends| SqlInsertTsRepository_c635["SqlInsertTsRepository"]
    InsertTsRepository_p636["InsertTsRepository"] -->|implements| SqlInsertTsRepository_c636["SqlInsertTsRepository"]
    Object______p637["Object/外部框架"] -->|extends| SqlPartitioningRepository_c637["SqlPartitioningRepository"]
    AbstractInsertRepository_p638["AbstractInsertRepository"] -->|extends| TimescaleInsertTsRepository_c638["TimescaleInsertTsRepository"]
    InsertTsRepository_p639["InsertTsRepository"] -->|implements| TimescaleInsertTsRepository_c639["TimescaleInsertTsRepository"]
    Object______p640["Object/外部框架"] -->|extends| SearchTsKvLatestRepository_c640["SearchTsKvLatestRepository"]
    JpaRepository_p641["JpaRepository"] -->|extends| TsKvLatestRepository_c641["TsKvLatestRepository"]
    AbstractChunkedAggregationTimeseriesDao_p642["AbstractChunkedAggregationTimeseriesDao"] -->|extends| JpaSqlTimeseriesDao_c642["JpaSqlTimeseriesDao"]
    Object______p643["Object/外部框架"] -->|extends| AggregationRepository_c643["AggregationRepository"]
    AbstractSqlTimeseriesDao_p644["AbstractSqlTimeseriesDao"] -->|extends| TimescaleTimeseriesDao_c644["TimescaleTimeseriesDao"]
    TimeseriesDao_p645["TimeseriesDao"] -->|implements| TimescaleTimeseriesDao_c645["TimescaleTimeseriesDao"]
    JpaRepository_p646["JpaRepository"] -->|extends| TsKvTimescaleRepository_c646["TsKvTimescaleRepository"]
    JpaRepository_p647["JpaRepository"] -->|extends| TsKvRepository_c647["TsKvRepository"]
    Object______p648["Object/外部框架"] -->|extends| DefaultTbTenantProfileCache_c648["DefaultTbTenantProfileCache"]
    TbTenantProfileCache_p649["TbTenantProfileCache"] -->|implements| DefaultTbTenantProfileCache_c649["DefaultTbTenantProfileCache"]
    TenantProfileProvider_p650["TenantProfileProvider"] -->|implements| DefaultTbTenantProfileCache_c650["DefaultTbTenantProfileCache"]
    CaffeineTbTransactionalCache_p651["CaffeineTbTransactionalCache"] -->|extends| TenantCaffeineCache_c651["TenantCaffeineCache"]
    Dao_p652["Dao"] -->|extends| TenantDao_c652["TenantDao"]
    Object______p653["Object/外部框架"] -->|extends| TenantEvictEvent_c653["TenantEvictEvent"]
    CaffeineTbTransactionalCache_p654["CaffeineTbTransactionalCache"] -->|extends| TenantExistsCaffeineCache_c654["TenantExistsCaffeineCache"]
    RedisTbTransactionalCache_p655["RedisTbTransactionalCache"] -->|extends| TenantExistsRedisCache_c655["TenantExistsRedisCache"]
    Object______p656["Object/外部框架"] -->|extends| TenantProfileCacheKey_c656["TenantProfileCacheKey"]
    Serializable_p657["Serializable"] -->|implements| TenantProfileCacheKey_c657["TenantProfileCacheKey"]
    CaffeineTbTransactionalCache_p658["CaffeineTbTransactionalCache"] -->|extends| TenantProfileCaffeineCache_c658["TenantProfileCaffeineCache"]
    Dao_p659["Dao"] -->|extends| TenantProfileDao_c659["TenantProfileDao"]
    Object______p660["Object/外部框架"] -->|extends| TenantProfileEvictEvent_c660["TenantProfileEvictEvent"]
    RedisTbTransactionalCache_p661["RedisTbTransactionalCache"] -->|extends| TenantProfileRedisCache_c661["TenantProfileRedisCache"]
    AbstractCachedEntityService_p662["AbstractCachedEntityService"] -->|extends| TenantProfileServiceImpl_c662["TenantProfileServiceImpl"]
    TenantProfileService_p663["TenantProfileService"] -->|implements| TenantProfileServiceImpl_c663["TenantProfileServiceImpl"]
    RedisTbTransactionalCache_p664["RedisTbTransactionalCache"] -->|extends| TenantRedisCache_c664["TenantRedisCache"]
    AbstractCachedEntityService_p665["AbstractCachedEntityService"] -->|extends| TenantServiceImpl_c665["TenantServiceImpl"]
    TenantService_p666["TenantService"] -->|implements| TenantServiceImpl_c666["TenantServiceImpl"]
    CassandraAbstractAsyncDao_p667["CassandraAbstractAsyncDao"] -->|extends| AbstractCassandraBaseTimeseriesDao_c667["AbstractCassandraBaseTimeseriesDao"]
    Object______p668["Object/外部框架"] -->|extends| AggregatePartitionsFunction_c668["AggregatePartitionsFunction"]
    AsyncFunction_p669["AsyncFunction"] -->|implements| AggregatePartitionsFunction_c669["AggregatePartitionsFunction"]
    Object______p670["Object/外部框架"] -->|extends| AggregationResult_c670["AggregationResult"]
    Object______p671["Object/外部框架"] -->|extends| BaseTimeseriesService_c671["BaseTimeseriesService"]
    TimeseriesService_p672["TimeseriesService"] -->|implements| BaseTimeseriesService_c672["BaseTimeseriesService"]
    AbstractCassandraBaseTimeseriesDao_p673["AbstractCassandraBaseTimeseriesDao"] -->|extends| CassandraBaseTimeseriesDao_c673["CassandraBaseTimeseriesDao"]
    TimeseriesDao_p674["TimeseriesDao"] -->|implements| CassandraBaseTimeseriesDao_c674["CassandraBaseTimeseriesDao"]
    AggregationTimeseriesDao_p675["AggregationTimeseriesDao"] -->|implements| CassandraBaseTimeseriesDao_c675["CassandraBaseTimeseriesDao"]
    Object______p676["Object/外部框架"] -->|extends| CacheCallback_c676["CacheCallback"]
    AbstractCassandraBaseTimeseriesDao_p677["AbstractCassandraBaseTimeseriesDao"] -->|extends| CassandraBaseTimeseriesLatestDao_c677["CassandraBaseTimeseriesLatestDao"]
    TimeseriesLatestDao_p678["TimeseriesLatestDao"] -->|implements| CassandraBaseTimeseriesLatestDao_c678["CassandraBaseTimeseriesLatestDao"]
    Object______p679["Object/外部框架"] -->|extends| CassandraPartitionCacheKey_c679["CassandraPartitionCacheKey"]
    Object______p680["Object/外部框架"] -->|extends| CassandraTsPartitionsCache_c680["CassandraTsPartitionsCache"]
    Object______p681["Object/外部框架"] -->|extends| QueryCursor_c681["QueryCursor"]
    Object______p682["Object/外部框架"] -->|extends| SimpleListenableFuture_c682["SimpleListenableFuture"]
    Object______p683["Object/外部框架"] -->|extends| SqlPartition_c683["SqlPartition"]
    QueryCursor_p684["QueryCursor"] -->|extends| TsKvQueryCursor_c684["TsKvQueryCursor"]
    Object______p685["Object/外部框架"] -->|extends| BasicUsageInfoService_c685["BasicUsageInfoService"]
    UsageInfoService_p686["UsageInfoService"] -->|implements| BasicUsageInfoService_c686["BasicUsageInfoService"]
    Dao_p687["Dao"] -->|extends| ApiUsageStateDao_c687["ApiUsageStateDao"]
    AbstractEntityService_p688["AbstractEntityService"] -->|extends| ApiUsageStateServiceImpl_c688["ApiUsageStateServiceImpl"]
    ApiUsageStateService_p689["ApiUsageStateService"] -->|implements| ApiUsageStateServiceImpl_c689["ApiUsageStateServiceImpl"]
    Object______p690["Object/外部框架"] -->|extends| DefaultApiLimitService_c690["DefaultApiLimitService"]
    ApiLimitService_p691["ApiLimitService"] -->|implements| DefaultApiLimitService_c691["DefaultApiLimitService"]
    Dao_p692["Dao"] -->|extends| UserAuthSettingsDao_c692["UserAuthSettingsDao"]
    Dao_p693["Dao"] -->|extends| UserCredentialsDao_c693["UserCredentialsDao"]
    Dao_p694["Dao"] -->|extends| UserDao_c694["UserDao"]
    TenantEntityDao_p695["TenantEntityDao"] -->|extends| UserDao_c695["UserDao"]
    AbstractEntityService_p696["AbstractEntityService"] -->|extends| UserServiceImpl_c696["UserServiceImpl"]
    UserService_p697["UserService"] -->|implements| UserServiceImpl_c697["UserServiceImpl"]
    CaffeineTbTransactionalCache_p698["CaffeineTbTransactionalCache"] -->|extends| UserSettingsCaffeineCache_c698["UserSettingsCaffeineCache"]
    Object______p699["Object/外部框架"] -->|extends| UserSettingsEvictEvent_c699["UserSettingsEvictEvent"]
    RedisTbTransactionalCache_p700["RedisTbTransactionalCache"] -->|extends| UserSettingsRedisCache_c700["UserSettingsRedisCache"]
    AbstractCachedService_p701["AbstractCachedService"] -->|extends| UserSettingsServiceImpl_c701["UserSettingsServiceImpl"]
    UserSettingsService_p702["UserSettingsService"] -->|implements| UserSettingsServiceImpl_c702["UserSettingsServiceImpl"]
    Object______p703["Object/外部框架"] -->|extends| AbstractBufferedRateExecutor_c703["AbstractBufferedRateExecutor"]
    Object______p704["Object/外部框架"] -->|extends| AsyncTaskContext_c704["AsyncTaskContext"]
    Object______p705["Object/外部框架"] -->|extends| BufferedRateExecutorStats_c705["BufferedRateExecutorStats"]
    Object______p706["Object/外部框架"] -->|extends| DeviceConnectivityUtil_c706["DeviceConnectivityUtil"]
    Object______p707["Object/外部框架"] -->|extends| ImageUtils_c707["ImageUtils"]
    Object______p708["Object/外部框架"] -->|extends| ProcessedImage_c708["ProcessedImage"]
    Object______p709["Object/外部框架"] -->|extends| JsonNodeProcessingTask_c709["JsonNodeProcessingTask"]
    Object______p710["Object/外部框架"] -->|extends| JsonPathProcessingTask_c710["JsonPathProcessingTask"]
    Object______p711["Object/外部框架"] -->|extends| KvUtils_c711["KvUtils"]
    Exception_p712["Exception"] -->|extends| TenantRateLimitException_c712["TenantRateLimitException"]
    Object______p713["Object/外部框架"] -->|extends| TimeUtils_c713["TimeUtils"]
    Object______p714["Object/外部框架"] -->|extends| AbstractJsonSqlTypeDescriptor_c714["AbstractJsonSqlTypeDescriptor"]
    SqlTypeDescriptor_p715["SqlTypeDescriptor"] -->|implements| AbstractJsonSqlTypeDescriptor_c715["AbstractJsonSqlTypeDescriptor"]
    AbstractJsonSqlTypeDescriptor_p716["AbstractJsonSqlTypeDescriptor"] -->|extends| JsonBinarySqlTypeDescriptor_c716["JsonBinarySqlTypeDescriptor"]
    AbstractSingleColumnStandardBasicType_p717["AbstractSingleColumnStandardBasicType"] -->|extends| JsonBinaryType_c717["JsonBinaryType"]
    DynamicParameterizedType_p718["DynamicParameterizedType"] -->|implements| JsonBinaryType_c718["JsonBinaryType"]
    AbstractJsonSqlTypeDescriptor_p719["AbstractJsonSqlTypeDescriptor"] -->|extends| JsonStringSqlTypeDescriptor_c719["JsonStringSqlTypeDescriptor"]
    AbstractSingleColumnStandardBasicType_p720["AbstractSingleColumnStandardBasicType"] -->|extends| JsonStringType_c720["JsonStringType"]
    DynamicParameterizedType_p721["DynamicParameterizedType"] -->|implements| JsonStringType_c721["JsonStringType"]
    AbstractTypeDescriptor_p722["AbstractTypeDescriptor"] -->|extends| JsonTypeDescriptor_c722["JsonTypeDescriptor"]
    DynamicParameterizedType_p723["DynamicParameterizedType"] -->|implements| JsonTypeDescriptor_c723["JsonTypeDescriptor"]
    Dao_p724["Dao"] -->|extends| WidgetTypeDao_c724["WidgetTypeDao"]
    ExportableEntityDao_p725["ExportableEntityDao"] -->|extends| WidgetTypeDao_c725["WidgetTypeDao"]
    ImageContainerDao_p726["ImageContainerDao"] -->|extends| WidgetTypeDao_c726["WidgetTypeDao"]
    Object______p727["Object/外部框架"] -->|extends| WidgetTypeServiceImpl_c727["WidgetTypeServiceImpl"]
    WidgetTypeService_p728["WidgetTypeService"] -->|implements| WidgetTypeServiceImpl_c728["WidgetTypeServiceImpl"]
    Dao_p729["Dao"] -->|extends| WidgetsBundleDao_c729["WidgetsBundleDao"]
    ExportableEntityDao_p730["ExportableEntityDao"] -->|extends| WidgetsBundleDao_c730["WidgetsBundleDao"]
    ImageContainerDao_p731["ImageContainerDao"] -->|extends| WidgetsBundleDao_c731["WidgetsBundleDao"]
    Object______p732["Object/外部框架"] -->|extends| WidgetsBundleServiceImpl_c732["WidgetsBundleServiceImpl"]
    WidgetsBundleService_p733["WidgetsBundleService"] -->|implements| WidgetsBundleServiceImpl_c733["WidgetsBundleServiceImpl"]
    Object______p734["Object/外部框架"] -->|extends| AbstractDaoServiceTest_c734["AbstractDaoServiceTest"]
    Object______p735["Object/外部框架"] -->|extends| AbstractJpaDaoTest_c735["AbstractJpaDaoTest"]
    Object______p736["Object/外部框架"] -->|extends| AbstractNoSqlContainer_c736["AbstractNoSqlContainer"]
    Object______p737["Object/外部框架"] -->|extends| AbstractRedisContainer_c737["AbstractRedisContainer"]
    AbstractNoSqlContainer_p738["AbstractNoSqlContainer"] -->|extends| NoSqlDaoServiceTestSuite_c738["NoSqlDaoServiceTestSuite"]
    Object______p739["Object/外部框架"] -->|extends| PostgreSqlInitializer_c739["PostgreSqlInitializer"]
    AbstractRedisContainer_p740["AbstractRedisContainer"] -->|extends| RedisSqlTestSuite_c740["RedisSqlTestSuite"]
    Object______p741["Object/外部框架"] -->|extends| TimescaleDaoServiceTestSuite_c741["TimescaleDaoServiceTestSuite"]
    Object______p742["Object/外部框架"] -->|extends| TimescaleSqlInitializer_c742["TimescaleSqlInitializer"]
    Object______p743["Object/外部框架"] -->|extends| DeleteEntityEventTest_c743["DeleteEntityEventTest"]
    Object______p744["Object/外部框架"] -->|extends| CassandraPartitionsCacheTest_c744["CassandraPartitionsCacheTest"]
    Object______p745["Object/外部框架"] -->|extends| AbstractServiceTest_c745["AbstractServiceTest"]
    Object______p746["Object/外部框架"] -->|extends| IdComparator_c746["IdComparator"]
    AbstractServiceTest_p747["AbstractServiceTest"] -->|extends| AdminSettingsServiceTest_c747["AdminSettingsServiceTest"]
    AbstractServiceTest_p748["AbstractServiceTest"] -->|extends| AlarmCommentServiceTest_c748["AlarmCommentServiceTest"]
    AbstractServiceTest_p749["AbstractServiceTest"] -->|extends| AlarmServiceTest_c749["AlarmServiceTest"]
    AbstractServiceTest_p750["AbstractServiceTest"] -->|extends| ApiUsageStateServiceTest_c750["ApiUsageStateServiceTest"]
    AbstractServiceTest_p751["AbstractServiceTest"] -->|extends| AssetProfileServiceTest_c751["AssetProfileServiceTest"]
    AbstractServiceTest_p752["AbstractServiceTest"] -->|extends| AssetServiceTest_c752["AssetServiceTest"]
    Object______p753["Object/外部框架"] -->|extends| ConstraintValidatorTest_c753["ConstraintValidatorTest"]
    AbstractServiceTest_p754["AbstractServiceTest"] -->|extends| CustomerServiceTest_c754["CustomerServiceTest"]
    AbstractServiceTest_p755["AbstractServiceTest"] -->|extends| DashboardServiceTest_c755["DashboardServiceTest"]
    Object______p756["Object/外部框架"] -->|extends| DataValidatorTest_c756["DataValidatorTest"]
    AbstractServiceTest_p757["AbstractServiceTest"] -->|extends| DeviceCredentialsCacheTest_c757["DeviceCredentialsCacheTest"]
    AbstractServiceTest_p758["AbstractServiceTest"] -->|extends| DeviceCredentialsServiceTest_c758["DeviceCredentialsServiceTest"]
    AbstractServiceTest_p759["AbstractServiceTest"] -->|extends| DeviceProfileServiceTest_c759["DeviceProfileServiceTest"]
    AbstractServiceTest_p760["AbstractServiceTest"] -->|extends| DeviceServiceTest_c760["DeviceServiceTest"]
    AbstractServiceTest_p761["AbstractServiceTest"] -->|extends| EdgeEventServiceTest_c761["EdgeEventServiceTest"]
    AbstractServiceTest_p762["AbstractServiceTest"] -->|extends| EdgeServiceTest_c762["EdgeServiceTest"]
    AbstractServiceTest_p763["AbstractServiceTest"] -->|extends| EntityServiceRegistryTest_c763["EntityServiceRegistryTest"]
    AbstractServiceTest_p764["AbstractServiceTest"] -->|extends| EntityServiceTest_c764["EntityServiceTest"]
    Object______p765["Object/外部框架"] -->|extends| NoXssValidatorTest_c765["NoXssValidatorTest"]
    AbstractServiceTest_p766["AbstractServiceTest"] -->|extends| OAuth2ConfigTemplateServiceTest_c766["OAuth2ConfigTemplateServiceTest"]
    AbstractServiceTest_p767["AbstractServiceTest"] -->|extends| OAuth2ServiceTest_c767["OAuth2ServiceTest"]
    AbstractServiceTest_p768["AbstractServiceTest"] -->|extends| OtaPackageServiceTest_c768["OtaPackageServiceTest"]
    AbstractServiceTest_p769["AbstractServiceTest"] -->|extends| QueueServiceTest_c769["QueueServiceTest"]
    AbstractServiceTest_p770["AbstractServiceTest"] -->|extends| RelationCacheTest_c770["RelationCacheTest"]
    AbstractServiceTest_p771["AbstractServiceTest"] -->|extends| RelationServiceTest_c771["RelationServiceTest"]
    AbstractServiceTest_p772["AbstractServiceTest"] -->|extends| RuleChainServiceTest_c772["RuleChainServiceTest"]
    AbstractServiceTest_p773["AbstractServiceTest"] -->|extends| TbCacheSerializationTest_c773["TbCacheSerializationTest"]
    AbstractServiceTest_p774["AbstractServiceTest"] -->|extends| TenantProfileServiceTest_c774["TenantProfileServiceTest"]
    AbstractServiceTest_p775["AbstractServiceTest"] -->|extends| TenantServiceTest_c775["TenantServiceTest"]
    AbstractServiceTest_p776["AbstractServiceTest"] -->|extends| UserServiceTest_c776["UserServiceTest"]
    AbstractServiceTest_p777["AbstractServiceTest"] -->|extends| WidgetTypeServiceTest_c777["WidgetTypeServiceTest"]
    AbstractServiceTest_p778["AbstractServiceTest"] -->|extends| WidgetsBundleServiceTest_c778["WidgetsBundleServiceTest"]
    AbstractServiceTest_p779["AbstractServiceTest"] -->|extends| BaseAttributesServiceTest_c779["BaseAttributesServiceTest"]
    BaseAttributesServiceTest_p780["BaseAttributesServiceTest"] -->|extends| AttributesServiceSqlTest_c780["AttributesServiceSqlTest"]
    AbstractServiceTest_p781["AbstractServiceTest"] -->|extends| BaseEventServiceTest_c781["BaseEventServiceTest"]
    BaseEventServiceTest_p782["BaseEventServiceTest"] -->|extends| EventServiceSqlTest_c782["EventServiceSqlTest"]
    AbstractServiceTest_p783["AbstractServiceTest"] -->|extends| EntitiesSchemaSqlTest_c783["EntitiesSchemaSqlTest"]
    AbstractServiceTest_p784["AbstractServiceTest"] -->|extends| BaseTimeseriesServiceTest_c784["BaseTimeseriesServiceTest"]
    TimeseriesServiceNoSqlTest_p785["TimeseriesServiceNoSqlTest"] -->|extends| TimeseriesServiceNoSqlSetNullEnabledTest_c785["TimeseriesServiceNoSqlSetNullEnabledTest"]
    BaseTimeseriesServiceTest_p786["BaseTimeseriesServiceTest"] -->|extends| TimeseriesServiceNoSqlTest_c786["TimeseriesServiceNoSqlTest"]
    BaseTimeseriesServiceTest_p787["BaseTimeseriesServiceTest"] -->|extends| TimeseriesServiceTimescaleTest_c787["TimeseriesServiceTimescaleTest"]
    BaseTimeseriesServiceTest_p788["BaseTimeseriesServiceTest"] -->|extends| TimeseriesServiceSqlTest_c788["TimeseriesServiceSqlTest"]
    Object______p789["Object/外部框架"] -->|extends| AdminSettingsDataValidatorTest_c789["AdminSettingsDataValidatorTest"]
    Object______p790["Object/外部框架"] -->|extends| AlarmDataValidatorTest_c790["AlarmDataValidatorTest"]
    Object______p791["Object/外部框架"] -->|extends| AssetDataValidatorTest_c791["AssetDataValidatorTest"]
    Object______p792["Object/外部框架"] -->|extends| AssetProfileDataValidatorTest_c792["AssetProfileDataValidatorTest"]
    Object______p793["Object/外部框架"] -->|extends| BaseOtaPackageDataValidatorTest_c793["BaseOtaPackageDataValidatorTest"]
    Object______p794["Object/外部框架"] -->|extends| ComponentDescriptorDataValidatorTest_c794["ComponentDescriptorDataValidatorTest"]
    Object______p795["Object/外部框架"] -->|extends| CustomerDataValidatorTest_c795["CustomerDataValidatorTest"]
    Object______p796["Object/外部框架"] -->|extends| DashboardDataValidatorTest_c796["DashboardDataValidatorTest"]
    Object______p797["Object/外部框架"] -->|extends| DeviceDataValidatorTest_c797["DeviceDataValidatorTest"]
    Object______p798["Object/外部框架"] -->|extends| DeviceProfileDataValidatorTest_c798["DeviceProfileDataValidatorTest"]
    Object______p799["Object/外部框架"] -->|extends| EdgeDataValidatorTest_c799["EdgeDataValidatorTest"]
    Object______p800["Object/外部框架"] -->|extends| EntityViewDataValidatorTest_c800["EntityViewDataValidatorTest"]
    Object______p801["Object/外部框架"] -->|extends| ResourceDataValidatorTest_c801["ResourceDataValidatorTest"]
    Object______p802["Object/外部框架"] -->|extends| RuleChainDataValidatorTest_c802["RuleChainDataValidatorTest"]
    Object______p803["Object/外部框架"] -->|extends| TenantDataValidatorTest_c803["TenantDataValidatorTest"]
    Object______p804["Object/外部框架"] -->|extends| TenantProfileDataValidatorTest_c804["TenantProfileDataValidatorTest"]
    Object______p805["Object/外部框架"] -->|extends| WidgetTypeDataValidatorTest_c805["WidgetTypeDataValidatorTest"]
    Object______p806["Object/外部框架"] -->|extends| WidgetsBundleDataValidatorTest_c806["WidgetsBundleDataValidatorTest"]
    AbstractJpaDaoTest_p807["AbstractJpaDaoTest"] -->|extends| JpaAlarmCommentDaoTest_c807["JpaAlarmCommentDaoTest"]
    AbstractJpaDaoTest_p808["AbstractJpaDaoTest"] -->|extends| JpaAlarmDaoTest_c808["JpaAlarmDaoTest"]
    AbstractJpaDaoTest_p809["AbstractJpaDaoTest"] -->|extends| JpaAssetDaoTest_c809["JpaAssetDaoTest"]
    AbstractJpaDaoTest_p810["AbstractJpaDaoTest"] -->|extends| JpaAuditLogDaoTest_c810["JpaAuditLogDaoTest"]
    AbstractJpaDaoTest_p811["AbstractJpaDaoTest"] -->|extends| JpaBaseComponentDescriptorDaoTest_c811["JpaBaseComponentDescriptorDaoTest"]
    AbstractJpaDaoTest_p812["AbstractJpaDaoTest"] -->|extends| JpaCustomerDaoTest_c812["JpaCustomerDaoTest"]
    AbstractJpaDaoTest_p813["AbstractJpaDaoTest"] -->|extends| JpaDashboardInfoDaoTest_c813["JpaDashboardInfoDaoTest"]
    AbstractJpaDaoTest_p814["AbstractJpaDaoTest"] -->|extends| JpaDeviceCredentialsDaoTest_c814["JpaDeviceCredentialsDaoTest"]
    AbstractJpaDaoTest_p815["AbstractJpaDaoTest"] -->|extends| JpaDeviceDaoTest_c815["JpaDeviceDaoTest"]
    AbstractJpaDaoTest_p816["AbstractJpaDaoTest"] -->|extends| JpaBaseEventDaoTest_c816["JpaBaseEventDaoTest"]
    Object______p817["Object/外部框架"] -->|extends| DefaultEntityQueryRepositoryTest_c817["DefaultEntityQueryRepositoryTest"]
    Object______p818["Object/外部框架"] -->|extends| DefaultQueryLogComponentTest_c818["DefaultQueryLogComponentTest"]
    Object______p819["Object/外部框架"] -->|extends| EntityDataAdapterTest_c819["EntityDataAdapterTest"]
    Object______p820["Object/外部框架"] -->|extends| EntityKeyMappingTest_c820["EntityKeyMappingTest"]
    AbstractJpaDaoTest_p821["AbstractJpaDaoTest"] -->|extends| JpaRpcDaoTest_c821["JpaRpcDaoTest"]
    AbstractJpaDaoTest_p822["AbstractJpaDaoTest"] -->|extends| JpaRuleNodeDaoTest_c822["JpaRuleNodeDaoTest"]
    AbstractJpaDaoTest_p823["AbstractJpaDaoTest"] -->|extends| JpaTenantDaoTest_c823["JpaTenantDaoTest"]
    AbstractJpaDaoTest_p824["AbstractJpaDaoTest"] -->|extends| JpaUserCredentialsDaoTest_c824["JpaUserCredentialsDaoTest"]
    AbstractJpaDaoTest_p825["AbstractJpaDaoTest"] -->|extends| JpaUserDaoTest_c825["JpaUserDaoTest"]
    AbstractJpaDaoTest_p826["AbstractJpaDaoTest"] -->|extends| JpaUserSettingsDaoTest_c826["JpaUserSettingsDaoTest"]
    AbstractJpaDaoTest_p827["AbstractJpaDaoTest"] -->|extends| JpaWidgetTypeDaoTest_c827["JpaWidgetTypeDaoTest"]
    AbstractJpaDaoTest_p828["AbstractJpaDaoTest"] -->|extends| JpaWidgetsBundleDaoTest_c828["JpaWidgetsBundleDaoTest"]
    Object______p829["Object/外部框架"] -->|extends| AbstractChunkedAggregationTimeseriesDaoTest_c829["AbstractChunkedAggregationTimeseriesDaoTest"]
    Object______p830["Object/外部框架"] -->|extends| CassandraBaseTimeseriesDaoPartitioningDaysAlwaysExistsTest_c830["CassandraBaseTimeseriesDaoPartitioningDaysAlwaysExistsTest"]
    Object______p831["Object/外部框架"] -->|extends| CassandraBaseTimeseriesDaoPartitioningHoursAlwaysExistsTest_c831["CassandraBaseTimeseriesDaoPartitioningHoursAlwaysExistsTest"]
    Object______p832["Object/外部框架"] -->|extends| CassandraBaseTimeseriesDaoPartitioningIndefiniteAlwaysExists_c832["CassandraBaseTimeseriesDaoPartitioningIndefiniteAlwaysExistsTest"]
    Object______p833["Object/外部框架"] -->|extends| CassandraBaseTimeseriesDaoPartitioningMinutesAlwaysExistsTes_c833["CassandraBaseTimeseriesDaoPartitioningMinutesAlwaysExistsTest"]
    Object______p834["Object/外部框架"] -->|extends| CassandraBaseTimeseriesDaoPartitioningMonthsAlwaysExistsTest_c834["CassandraBaseTimeseriesDaoPartitioningMonthsAlwaysExistsTest"]
    Object______p835["Object/外部框架"] -->|extends| CassandraBaseTimeseriesDaoPartitioningYearsAlwaysExistsTest_c835["CassandraBaseTimeseriesDaoPartitioningYearsAlwaysExistsTest"]
    Object______p836["Object/外部框架"] -->|extends| DeviceConnectivityUtilTest_c836["DeviceConnectivityUtilTest"]
    Object______p837["Object/外部框架"] -->|extends| TimeUtilsTest_c837["TimeUtilsTest"]
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

- `Dao.find()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.findById()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.findByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.existsById()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.existsByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.save()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.saveAndFlush()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.removeById()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.removeAllByIds()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.getEntityType()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `DaoUtil.toPageable()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.toPageable()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.PageLink()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.EntitySubtype()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.EntitySubtype()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `ExportableEntityDao.findByTenantIdAndExternalId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findByTenantIdAndName()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.UnsupportedOperationException()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findIdsByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.getExternalIdByInternal()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityRepository.findByTenantIdAndExternalId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityRepository.java`)
- `ImageContainerDao.findByTenantAndImageLink()` (package, `dao/src/main/java/org/thingsboard/server/dao/ImageContainerDao.java`)
- `ImageContainerDao.findByImageLink()` (package, `dao/src/main/java/org/thingsboard/server/dao/ImageContainerDao.java`)
- `TenantEntityDao.countByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/TenantEntityDao.java`)
- `TenantEntityWithDataDao.sumDataSizeByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/TenantEntityWithDataDao.java`)
- `AlarmCommentDao.findAlarmCommentById()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmCommentDao.java`)
- `AlarmCommentDao.findAlarmComments()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmCommentDao.java`)
- `AlarmCommentDao.findAlarmCommentByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmCommentDao.java`)
- `AlarmDao.findLatestByOriginatorAndType()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findLatestActiveByOriginatorAndType()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findLatestByOriginatorAndTypeAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarmById()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarmByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarmInfoById()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.save()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarms()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findCustomerAlarms()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarmsV2()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)


## 哪些方法可以重写

- `Dao.find()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.findById()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.findByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.existsById()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.existsByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.save()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.saveAndFlush()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.removeById()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.removeAllByIds()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.getEntityType()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `DaoUtil.toPageable()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.toPageable()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.PageLink()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.EntitySubtype()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.EntitySubtype()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `ExportableEntityDao.findByTenantIdAndExternalId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findByTenantIdAndName()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.UnsupportedOperationException()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findIdsByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.getExternalIdByInternal()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityRepository.findByTenantIdAndExternalId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityRepository.java`)
- `ImageContainerDao.findByTenantAndImageLink()` (package, `dao/src/main/java/org/thingsboard/server/dao/ImageContainerDao.java`)
- `ImageContainerDao.findByImageLink()` (package, `dao/src/main/java/org/thingsboard/server/dao/ImageContainerDao.java`)
- `TenantEntityDao.countByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/TenantEntityDao.java`)
- `TenantEntityWithDataDao.sumDataSizeByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/TenantEntityWithDataDao.java`)
- `ThingsboardPostgreSQLDialect.SQLFunctionTemplate()` (package, `dao/src/main/java/org/thingsboard/server/dao/ThingsboardPostgreSQLDialect.java`)
- `AlarmCommentDao.findAlarmCommentById()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmCommentDao.java`)
- `AlarmCommentDao.findAlarmComments()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmCommentDao.java`)
- `AlarmCommentDao.findAlarmCommentByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmCommentDao.java`)
- `AlarmDao.findLatestByOriginatorAndType()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findLatestActiveByOriginatorAndType()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findLatestByOriginatorAndTypeAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarmById()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarmByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarmInfoById()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.save()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarms()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findCustomerAlarms()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)


## 哪些方法必须重写

- `Dao.find()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.findById()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.findByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.existsById()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.existsByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.save()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.saveAndFlush()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.removeById()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.removeAllByIds()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `Dao.getEntityType()` (package, `dao/src/main/java/org/thingsboard/server/dao/Dao.java`)
- `DaoUtil.toPageable()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.toPageable()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.PageLink()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.EntitySubtype()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `DaoUtil.EntitySubtype()` (package, `dao/src/main/java/org/thingsboard/server/dao/DaoUtil.java`)
- `ExportableEntityDao.findByTenantIdAndExternalId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findByTenantIdAndName()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.UnsupportedOperationException()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findIdsByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.findByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityDao.getExternalIdByInternal()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityDao.java`)
- `ExportableEntityRepository.findByTenantIdAndExternalId()` (package, `dao/src/main/java/org/thingsboard/server/dao/ExportableEntityRepository.java`)
- `ImageContainerDao.findByTenantAndImageLink()` (package, `dao/src/main/java/org/thingsboard/server/dao/ImageContainerDao.java`)
- `ImageContainerDao.findByImageLink()` (package, `dao/src/main/java/org/thingsboard/server/dao/ImageContainerDao.java`)
- `TenantEntityDao.countByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/TenantEntityDao.java`)
- `TenantEntityWithDataDao.sumDataSizeByTenantId()` (package, `dao/src/main/java/org/thingsboard/server/dao/TenantEntityWithDataDao.java`)
- `ThingsboardPostgreSQLDialect.SQLFunctionTemplate()` (package, `dao/src/main/java/org/thingsboard/server/dao/ThingsboardPostgreSQLDialect.java`)
- `AlarmCommentDao.findAlarmCommentById()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmCommentDao.java`)
- `AlarmCommentDao.findAlarmComments()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmCommentDao.java`)
- `AlarmCommentDao.findAlarmCommentByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmCommentDao.java`)
- `AlarmDao.findLatestByOriginatorAndType()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findLatestActiveByOriginatorAndType()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findLatestByOriginatorAndTypeAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarmById()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarmByIdAsync()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarmInfoById()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.save()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findAlarms()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)
- `AlarmDao.findCustomerAlarms()` (package, `dao/src/main/java/org/thingsboard/server/dao/alarm/AlarmDao.java`)


## 哪些地方使用了多态

- `Object/外部框架` -> `DaoUtil` (extends)
- `Object/外部框架` -> `JpaDaoConfig` (extends)
- `Object/外部框架` -> `SqlTimeseriesDaoConfig` (extends)
- `Object/外部框架` -> `SqlTsDaoConfig` (extends)
- `Object/外部框架` -> `SqlTsLatestDaoConfig` (extends)
- `PostgreSQL10Dialect` -> `ThingsboardPostgreSQLDialect` (extends)
- `Object/外部框架` -> `TimescaleDaoConfig` (extends)
- `Object/外部框架` -> `TimescaleTsLatestDaoConfig` (extends)
- `Dao` -> `AlarmCommentDao` (extends)
- `Dao` -> `AlarmDao` (extends)
- `Object/外部框架` -> `AlarmTypesCacheEvictEvent` (extends)
- `CaffeineTbTransactionalCache` -> `AlarmTypesCaffeineCache` (extends)
- `RedisTbTransactionalCache` -> `AlarmTypesRedisCache` (extends)
- `AbstractEntityService` -> `BaseAlarmCommentService` (extends)
- `AlarmCommentService` -> `BaseAlarmCommentService` (implements)
- `AbstractCachedEntityService` -> `BaseAlarmService` (extends)
- `AlarmService` -> `BaseAlarmService` (implements)
- `Object/外部框架` -> `DbCallStats` (extends)
- `Object/外部框架` -> `DbCallStatsSnapshot` (extends)
- `Object/外部框架` -> `MethodCallStats` (extends)
- `Object/外部框架` -> `MethodCallStatsSnapshot` (extends)
- `Object/外部框架` -> `SqlDaoCallsAspect` (extends)
- `Object/外部框架` -> `AssetCacheEvictEvent` (extends)
- `Object/外部框架` -> `AssetCacheKey` (extends)
- `Serializable` -> `AssetCacheKey` (implements)
- `CaffeineTbTransactionalCache` -> `AssetCaffeineCache` (extends)
- `Dao` -> `AssetDao` (extends)
- `TenantEntityDao` -> `AssetDao` (extends)
- `ExportableEntityDao` -> `AssetDao` (extends)
- `Object/外部框架` -> `AssetProfileCacheKey` (extends)
- `Serializable` -> `AssetProfileCacheKey` (implements)
- `CaffeineTbTransactionalCache` -> `AssetProfileCaffeineCache` (extends)
- `Dao` -> `AssetProfileDao` (extends)
- `ExportableEntityDao` -> `AssetProfileDao` (extends)
- `ImageContainerDao` -> `AssetProfileDao` (extends)
- `Object/外部框架` -> `AssetProfileEvictEvent` (extends)
- `RedisTbTransactionalCache` -> `AssetProfileRedisCache` (extends)
- `AbstractCachedEntityService` -> `AssetProfileServiceImpl` (extends)
- `AssetProfileService` -> `AssetProfileServiceImpl` (implements)
- `RedisTbTransactionalCache` -> `AssetRedisCache` (extends)
- 其余 40 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 外部父类/接口

- `AbstractAlarmCommentEntity`
- `AbstractAlarmEntity`
- `AbstractAssetEntity`
- `AbstractDeviceEntity`
- `AbstractEdgeEntity`
- `AbstractListeningExecutor`
- `AbstractSingleColumnStandardBasicType`
- `AbstractTenantEntity`
- `AbstractTsKvEntity`
- `AbstractTypeDescriptor`
- `AbstractWidgetTypeEntity`
- `AdminSettingsService`
- `Alarm> extends BaseSqlEntity`
- `AlarmComment> extends BaseSqlEntity`
- `AlarmCommentService`
- `AlarmService`
- `ApiLimitService`
- `ApiUsageStateService`
- `Asset> extends BaseSqlEntity`
- `AssetProfileService`
- `AssetService`
- `AsyncFunction`
- `AttributesService`
- `AuditLogService`
- `BaseSqlEntity`
- `BaseWidgetType> extends BaseSqlEntity`
- `CaffeineTbTransactionalCache`
- `ClientRegistrationRepository`
- `ComponentDescriptorService`
- `CustomerService`
- `DashboardService`
- `Device> extends BaseSqlEntity`
- `DeviceConnectivityService`
- `DeviceCredentialsService`
- `DeviceProfileService`
- `DeviceService`
- `DynamicParameterizedType`
- `Edge> extends BaseSqlEntity`
- `EdgeEventService`
- `EdgeService`
- 其余 46 项已省略；完整源码证据可通过本模块 Java 文件继续追踪。


## 整个继承体系解决了什么问题

该继承体系把“稳定生命周期/契约”和“模块特定行为”分开：父类或接口负责统一入口、共享流程和多态调度，子类负责差异化协议、实体、规则、DAO、测试或工具行为。这样可以让上游模块通过父类型调用下游实现，同时保持每个实现只关注自己的变化点。
