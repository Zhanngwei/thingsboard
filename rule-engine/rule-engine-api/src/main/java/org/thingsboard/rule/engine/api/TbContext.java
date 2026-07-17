/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.api;

import io.netty.channel.EventLoopGroup;
import org.thingsboard.common.util.ExecutorProvider;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;
import org.thingsboard.server.dao.nosql.TbResultSetFuture;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 13.01.18.
 */
/**
 * 中文说明：
 * 1. `TbContext` 是 ThingsBoard Rule Engine API 中定义 `Tb Context` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbContext {

    /*
     *
     *  METHODS TO CONTROL THE MESSAGE FLOW
     *
     */

    /**
     * 功能：执行 `tellSuccess` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void tellSuccess(TbMsg msg);

    /**
     * 功能：执行 `tellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationType`：类型。
     * 返回：无。
     */
    void tellNext(TbMsg msg, String relationType);

    /**
     * 功能：执行 `tellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationTypes`：类型。
     * 返回：无。
     */
    void tellNext(TbMsg msg, Set<String> relationTypes);

    /**
     * 功能：执行 `tellSelf` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `delayMs`：`delayMs` 参数。
     * 返回：无。
     */
    void tellSelf(TbMsg msg, long delayMs);

    /**
     * 功能：执行 `tellFailure` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `th`：`th` 参数。
     * 返回：无。
     */
    void tellFailure(TbMsg msg, Throwable th);

    /**
     * 功能：执行 `enqueue` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    void enqueue(TbMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 功能：执行 `input` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    void input(TbMsg msg, RuleChainId ruleChainId);

    /**
     * 功能：执行 `output` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationType`：类型。
     * 返回：无。
     */
    void output(TbMsg msg, String relationType);

    /**
     * 功能：执行 `enqueue` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `queueName`：队列名称或队列对象。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    void enqueue(TbMsg msg, String queueName, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 功能：执行 `enqueueForTellFailure` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `failureMessage`：待处理消息。
     * 返回：无。
     */
    void enqueueForTellFailure(TbMsg msg, String failureMessage);

    /**
     * 功能：执行 `enqueueForTellFailure` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `t`：`t` 参数。
     * 返回：无。
     */
    void enqueueForTellFailure(TbMsg tbMsg, Throwable t);

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationType`：类型。
     * 返回：无。
     */
    void enqueueForTellNext(TbMsg msg, String relationType);

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationTypes`：类型。
     * 返回：无。
     */
    void enqueueForTellNext(TbMsg msg, Set<String> relationTypes);

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationType`：类型。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    void enqueueForTellNext(TbMsg msg, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationTypes`：类型。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    void enqueueForTellNext(TbMsg msg, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `queueName`：队列名称或队列对象。
     * - `relationType`：类型。
     * - `onSuccess`：`onSuccess` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void enqueueForTellNext(TbMsg msg, String queueName, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `queueName`：队列名称或队列对象。
     * - `relationTypes`：类型。
     * - `onSuccess`：`onSuccess` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void enqueueForTellNext(TbMsg msg, String queueName, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 功能：执行 `ack` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * 返回：无。
     */
    void ack(TbMsg tbMsg);

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    TbMsg newMsg(String queueName, String type, EntityId originator, TbMsgMetaData metaData, String data);

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0")
    TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data);

    /**
     * 功能：转换消息。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data);

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data);

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data);

    /**
     * 功能：转换消息。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    TbMsg transformMsg(TbMsg origMsg, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data);

    /**
     * 功能：转换消息。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `metaData`：待处理数据。
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    TbMsg transformMsg(TbMsg origMsg, TbMsgMetaData metaData, String data);

    /**
     * 功能：转换消息。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `originator`：`originator` 参数。
     * 返回：处理结果。
     */
    TbMsg transformMsgOriginator(TbMsg origMsg, EntityId originator);

    /**
     * 功能：执行 `customerCreatedMsg` 对应的处理。
     * 参数：
     * - `customer`：`customer` 参数。
     * - `ruleNodeId`：规则节点ID。
     * 返回：处理结果。
     */
    TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId);

    /**
     * 功能：执行 `deviceCreatedMsg` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `ruleNodeId`：规则节点ID。
     * 返回：处理结果。
     */
    TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId);

    /**
     * 功能：执行 `assetCreatedMsg` 对应的处理。
     * 参数：
     * - `asset`：`asset` 参数。
     * - `ruleNodeId`：规则节点ID。
     * 返回：处理结果。
     */
    TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId);

    /**
     * 功能：执行 `alarmActionMsg` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `ruleNodeId`：规则节点ID。
     * - `action`：`action` 参数。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    TbMsg alarmActionMsg(Alarm alarm, RuleNodeId ruleNodeId, String action);

    /**
     * 功能：执行 `alarmActionMsg` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `ruleNodeId`：规则节点ID。
     * - `actionMsgType`：待处理消息。
     * 返回：处理结果。
     */
    TbMsg alarmActionMsg(Alarm alarm, RuleNodeId ruleNodeId, TbMsgType actionMsgType);

    /**
     * 功能：执行 `attributesUpdatedActionMsg` 对应的处理。
     * 参数：
     * - `originator`：`originator` 参数。
     * - `ruleNodeId`：规则节点ID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * 返回：处理结果。
     */
    TbMsg attributesUpdatedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<AttributeKvEntry> attributes);

    /**
     * 功能：执行 `attributesDeletedActionMsg` 对应的处理。
     * 参数：
     * - `originator`：`originator` 参数。
     * - `ruleNodeId`：规则节点ID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * 返回：处理结果。
     */
    TbMsg attributesDeletedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<String> keys);

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId);

    /*
     *
     *  METHODS TO PROCESS THE MESSAGES
     *
     */

    /**
     * 功能：执行 `schedule` 对应的处理。
     * 参数：
     * - `runnable`：`runnable` 参数。
     * - `delay`：`delay` 参数。
     * - `timeUnit`：`timeUnit` 参数。
     * 返回：无。
     */
    void schedule(Runnable runnable, long delay, TimeUnit timeUnit);

    /**
     * 功能：校验租户。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    void checkTenantEntity(EntityId entityId) throws TbNodeException;

    /**
     * 功能：判断实体。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：判断结果。
     */
    boolean isLocalEntity(EntityId entityId);

    /**
     * 功能：获取`Self Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    RuleNodeId getSelfId();

    /**
     * 功能：获取`Self`。
     * 参数：无。
     * 返回：处理结果。
     */
    RuleNode getSelf();

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：文本结果。
     */
    String getRuleChainName();

    /**
     * 功能：获取队列名称。
     * 参数：无。
     * 返回：文本结果。
     */
    String getQueueName();

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    TenantId getTenantId();

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    AttributesService getAttributesService();

    /**
     * 功能：获取客户。
     * 参数：无。
     * 返回：处理结果。
     */
    CustomerService getCustomerService();

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    TenantService getTenantService();

    /**
     * 功能：获取用户。
     * 参数：无。
     * 返回：处理结果。
     */
    UserService getUserService();

    /**
     * 功能：获取资产。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    AssetService getAssetService();

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：处理结果。
     */
    DeviceService getDeviceService();

    /**
     * 功能：获取设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    DeviceProfileService getDeviceProfileService();

    /**
     * 功能：获取资产配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    AssetProfileService getAssetProfileService();

    /**
     * 功能：获取设备凭据。
     * 参数：无。
     * 返回：处理结果。
     */
    DeviceCredentialsService getDeviceCredentialsService();

    /**
     * 功能：获取设备状态管理器。
     * 参数：无。
     * 返回：处理结果。
     */
    RuleEngineDeviceStateManager getDeviceStateManager();

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：文本结果。
     */
    String getDeviceStateNodeRateLimitConfig();

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    TbClusterService getClusterService();

    /**
     * 功能：获取仪表盘。
     * 参数：无。
     * 返回：处理结果。
     */
    DashboardService getDashboardService();

    /**
     * 功能：获取告警。
     * 参数：无。
     * 返回：处理结果。
     */
    RuleEngineAlarmService getAlarmService();

    /**
     * 功能：获取告警。
     * 参数：无。
     * 返回：处理结果。
     */
    AlarmCommentService getAlarmCommentService();

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    RuleChainService getRuleChainService();

    /**
     * 功能：获取RPC。
     * 参数：无。
     * 返回：处理结果。
     */
    RuleEngineRpcService getRpcService();

    /**
     * 功能：获取遥测。
     * 参数：无。
     * 返回：处理结果。
     */
    RuleEngineTelemetryService getTelemetryService();

    /**
     * 功能：获取时序数据。
     * 参数：无。
     * 返回：处理结果。
     */
    TimeseriesService getTimeseriesService();

    /**
     * 功能：获取关系。
     * 参数：无。
     * 返回：处理结果。
     */
    RelationService getRelationService();

    /**
     * 功能：获取实体视图。
     * 参数：无。
     * 返回：处理结果。
     */
    EntityViewService getEntityViewService();

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    ResourceService getResourceService();

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    OtaPackageService getOtaPackageService();

    /**
     * 功能：获取设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    RuleEngineDeviceProfileCache getDeviceProfileCache();

    /**
     * 功能：获取资产配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    RuleEngineAssetProfileCache getAssetProfileCache();

    /**
     * 功能：获取边缘节点。
     * 参数：无。
     * 返回：处理结果。
     */
    EdgeService getEdgeService();

    /**
     * 功能：获取边缘节点。
     * 参数：无。
     * 返回：处理结果。
     */
    EdgeEventService getEdgeEventService();

    /**
     * 功能：获取队列。
     * 参数：无。
     * 返回：处理结果。
     */
    QueueService getQueueService();

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    ListeningExecutor getMailExecutor();

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    ListeningExecutor getSmsExecutor();

    /**
     * 功能：获取回调。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    ListeningExecutor getDbCallbackExecutor();

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    ListeningExecutor getExternalCallExecutor();

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    ListeningExecutor getNotificationExecutor();

    /**
     * 功能：获取规则节点。
     * 参数：无。
     * 返回：处理结果。
     */
    ExecutorProvider getPubSubRuleNodeExecutorProvider();

    /**
     * 功能：获取服务。
     * 参数：
     * - `isSystem`：`isSystem` 参数。
     * 返回：处理结果。
     */
    MailService getMailService(boolean isSystem);

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    SmsService getSmsService();

    /**
     * 功能：获取工厂。
     * 参数：无。
     * 返回：处理结果。
     */
    SmsSenderFactory getSmsSenderFactory();

    /**
     * 功能：获取通知。
     * 参数：无。
     * 返回：处理结果。
     */
    NotificationCenter getNotificationCenter();

    /**
     * 功能：获取通知服务。
     * 参数：无。
     * 返回：处理结果。
     */
    NotificationTargetService getNotificationTargetService();

    /**
     * 功能：获取通知服务。
     * 参数：无。
     * 返回：处理结果。
     */
    NotificationTemplateService getNotificationTemplateService();

    /**
     * 功能：获取通知服务。
     * 参数：无。
     * 返回：处理结果。
     */
    NotificationRequestService getNotificationRequestService();

    /**
     * 功能：获取通知服务。
     * 参数：无。
     * 返回：处理结果。
     */
    NotificationRuleService getNotificationRuleService();

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    SlackService getSlackService();

    /**
     * 功能：判断节点实例。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean isExternalNodeForceAck();

    /**
     * 功能：保存或创建脚本执行器。
     * 参数：
     * - `script`：`script` 参数。
     * - `argNames`：名称。
     * 返回：处理结果。
     */
    @Deprecated
    ScriptEngine createJsScriptEngine(String script, String... argNames);

    /**
     * 功能：保存或创建脚本执行器。
     * 参数：
     * - `scriptLang`：`scriptLang` 参数。
     * - `script`：`script` 参数。
     * - `argNames`：名称。
     * 返回：处理结果。
     */
    ScriptEngine createScriptEngine(ScriptLanguage scriptLang, String script, String... argNames);

    /**
     * 功能：执行 `logJsEvalRequest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void logJsEvalRequest();

    /**
     * 功能：执行 `logJsEvalResponse` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void logJsEvalResponse();

    /**
     * 功能：执行 `logJsEvalFailure` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void logJsEvalFailure();

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：文本结果。
     */
    String getServiceId();

    /**
     * 功能：获取事件循环。
     * 参数：无。
     * 返回：处理结果。
     */
    EventLoopGroup getSharedEventLoop();

    /**
     * 功能：获取Cassandra 集群。
     * 参数：无。
     * 返回：处理结果。
     */
    CassandraCluster getCassandraCluster();

    /**
     * 功能：发送或提交`Cassandra Read Task`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    TbResultSetFuture submitCassandraReadTask(CassandraStatementTask task);

    /**
     * 功能：发送或提交`Cassandra Write Task`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    TbResultSetFuture submitCassandraWriteTask(CassandraStatementTask task);

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleNodeState> findRuleNodeStates(PageLink pageLink);

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    RuleNodeState findRuleNodeStateForEntity(EntityId entityId);

    /**
     * 功能：删除或清理规则节点。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    void removeRuleNodeStateForEntity(EntityId entityId);

    /**
     * 功能：保存或创建规则节点。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：处理结果。
     */
    RuleNodeState saveRuleNodeState(RuleNodeState state);

    /**
     * 功能：删除或清理规则节点。
     * 参数：无。
     * 返回：无。
     */
    void clearRuleNodeStates();

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `listener`：`listener` 参数。
     * 返回：无。
     */
    void addTenantProfileListener(Consumer<TenantProfile> listener);

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `listener`：`listener` 参数。
     * - `deviceListener`：设备信息或设备标识。
     * 返回：无。
     */
    void addDeviceProfileListeners(Consumer<DeviceProfile> listener, BiConsumer<DeviceId, DeviceProfile> deviceListener);

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `listener`：`listener` 参数。
     * - `assetListener`：`assetListener` 参数。
     * 返回：无。
     */
    void addAssetProfileListeners(Consumer<AssetProfile> listener, BiConsumer<AssetId, AssetProfile> assetListener);

    /**
     * 功能：删除或清理`Listeners`。
     * 参数：无。
     * 返回：无。
     */
    void removeListeners();

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    TenantProfile getTenantProfile();

    /**
     * 功能：获取部件。
     * 参数：无。
     * 返回：处理结果。
     */
    WidgetsBundleService getWidgetBundleService();

    /**
     * 功能：获取部件类型。
     * 参数：无。
     * 返回：处理结果。
     */
    WidgetTypeService getWidgetTypeService();

    /**
     * 功能：获取规则引擎。
     * 参数：无。
     * 返回：处理结果。
     */
    RuleEngineApiUsageStateService getRuleEngineApiUsageStateService();

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    EntityService getEntityService();

    /**
     * 功能：获取事件。
     * 参数：无。
     * 返回：处理结果。
     */
    EventService getEventService();

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    AuditLogService getAuditLogService();
}
