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
 * 1. 职责：为每个规则节点提供消息路由、消息创建、服务访问、异步执行器、脚本引擎、状态存储和监听器注册的运行上下文。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的核心上下文边界，是具体节点与平台服务之间的主要协作入口。
 * 3. 协作对象：与 {@link TbNode}、{@link TbMsg}、规则链 Actor、DAO 服务、缓存服务、队列服务、脚本引擎、通知/RPC/遥测/告警服务协作。
 * 4. 生命周期：由 Rule Engine 运行时为节点实例提供，随节点和规则链执行环境存在；节点在 init/onMsg/destroy 等阶段调用。
 * 5. 设计原因：规则节点不能直接持有全部 Spring 服务和 Actor 细节，使用上下文可以统一权限、租户、队列、回调和服务访问边界。
 * 6. 设计模式：Facade/Context，对规则节点隐藏底层服务、缓存、数据库、队列和 Actor 调度细节。
 * 7. 技术关联：接口本身不直接执行事务、缓存、MQTT、Actor 通信或数据库操作；具体实现和返回服务可能涉及这些能力，所有方法都服务 Rule Engine。
 */
public interface TbContext {

    /*
     *
     *  METHODS TO CONTROL THE MESSAGE FLOW
     *
     */

    /**
     * Indicates that message was successfully processed by the rule node.
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using "Success" relationType.
     *
     * @param msg
     *
     * 中文说明：方法职责是把当前消息按 Success 关系发往后续节点；输入 msg 是已处理成功的 Rule Engine 消息；无返回值。
     * 调用时机为节点处理成功后，调用方是具体 TbNode；线程安全、事务、缓存、MQTT、Actor、数据库取决于上下文实现，本接口语义上会触发 Rule Engine/Actor 路由。
     */
    void tellSuccess(TbMsg msg);

    /**
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using specified relationType.
     *
     * @param msg
     * @param relationType
     *
     * 中文说明：方法职责是按指定关系发送消息；msg 是待路由消息，relationType 是规则链连线关系；无返回值。
     * 调用方为规则节点 onMsg；不直接声明事务/缓存/MQTT/数据库，实际由 Rule Engine Actor 路由实现处理。
     */
    void tellNext(TbMsg msg, String relationType);

    /**
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using one of specified relationTypes.
     *
     * @param msg
     * @param relationTypes
     *
     * 中文说明：方法职责是按多个关系发送同一消息；relationTypes 是目标关系集合；无返回值。
     * 调用时机为节点产生多分支输出时；线程安全和 Actor 通信由上下文实现保证，接口不直接涉及事务/缓存/MQTT/数据库。
     */
    void tellNext(TbMsg msg, Set<String> relationTypes);

    /**
     * Sends message to the current Rule Node with specified delay in milliseconds.
     * Note: this message is not queued and may be lost in case of a server restart.
     *
     * @param msg
     *
     * 中文说明：方法职责是延迟把消息重新投递给当前节点；msg 是原消息或派生消息，delayMs 是毫秒延迟；无返回值。
     * 调用方为需要重试/等待的节点；消息可能因重启丢失，不涉及数据库事务，通常通过 Rule Engine 调度器/Actor 实现。
     */
    void tellSelf(TbMsg msg, long delayMs);

    /**
     * Notifies Rule Engine about failure to process current message.
     *
     * @param msg - message
     * @param th  - exception
     *
     * 中文说明：方法职责是通知 Rule Engine 当前消息处理失败；msg 是失败消息，th 是失败原因；无返回值。
     * 调用时机为节点捕获异常后；会触发失败关系或错误处理，接口不直接涉及缓存/MQTT/数据库，Actor 错误流程由实现处理。
     */
    void tellFailure(TbMsg msg, Throwable th);

    /**
     * Puts new message to queue for processing by the Root Rule Chain
     *
     * @param msg - message
     *
     * 中文说明：方法职责是把新消息投递到根规则链队列；onSuccess/onFailure 表示队列投递结果回调；无返回值。
     * 调用方为需要产生新根消息的节点；涉及 Rule Engine 队列，可能通过 Actor/队列系统处理，不直接涉及数据库、事务、MQTT 或缓存。
     */
    void enqueue(TbMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * Sends message to the nested rule chain.
     * Fails processing of the message if the nested rule chain is not found.
     *
     * @param msg - the message
     * @param ruleChainId - the id of a nested rule chain
     *
     * 中文说明：方法职责是把消息输入到嵌套规则链；ruleChainId 是目标规则链；无返回值。
     * 调用方为规则链节点；涉及 Rule Engine 调用栈和 Actor 路由，不直接涉及事务、缓存、MQTT 或数据库。
     */
    void input(TbMsg msg, RuleChainId ruleChainId);

    /**
     * Sends message to the caller rule chain.
     * Acknowledge the message if no caller rule chain is present in processing stack
     *
     * @param msg - the message
     * @param relationType - the relation type that will be used to route messages in the caller rule chain
     *
     * 中文说明：方法职责是从嵌套规则链输出回调用方规则链；relationType 是调用方规则链中的路由关系；无返回值。
     * 调用方为规则链输出节点；涉及 Rule Engine 调用栈和 Actor 路由，不直接涉及事务、缓存、MQTT 或数据库。
     */
    void output(TbMsg msg, String relationType);

    /**
     * Puts new message to custom queue for processing
     *
     * @param msg - message
     *
     * 中文说明：方法职责是把消息投递到指定自定义队列；queueName 是队列名称，回调表示投递结果；无返回值。
     * 调用方为需要跨队列处理的节点；涉及 Rule Engine 队列系统，接口不直接涉及事务、缓存、MQTT、Actor 细节或数据库。
     */
    void enqueue(TbMsg msg, String queueName, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 中文说明：方法职责是把消息封装为失败投递请求；failureMessage 是失败说明；无返回值。
     * 调用方为异步节点回调；线程安全由实现保证，涉及 Rule Engine 队列/失败路由，不直接涉及事务、缓存、MQTT、数据库。
     */
    void enqueueForTellFailure(TbMsg msg, String failureMessage);

    /**
     * 中文说明：方法职责是把异常封装为失败投递请求；t 是异常原因；无返回值。
     * 调用时机为异步回调失败后；可能触发 Actor/队列路由，不直接涉及事务、缓存、MQTT、数据库。
     */
    void enqueueForTellFailure(TbMsg tbMsg, Throwable t);

    /**
     * 中文说明：方法职责是异步投递 tellNext 请求；relationType 是目标关系；无返回值。
     * 调用方为异步节点或外部回调；涉及 Rule Engine 队列/Actor 路由，不直接涉及事务、缓存、MQTT、数据库。
     */
    void enqueueForTellNext(TbMsg msg, String relationType);

    /**
     * 中文说明：方法职责是异步按多个关系投递 tellNext 请求；relationTypes 是关系集合；无返回值。
     * 调用方为异步多分支节点；线程安全由实现保证，涉及 Rule Engine 路由，不直接涉及事务、缓存、MQTT、数据库。
     */
    void enqueueForTellNext(TbMsg msg, Set<String> relationTypes);

    /**
     * 中文说明：方法职责是异步投递单关系消息并接收投递结果回调；onSuccess/onFailure 表示队列投递结果；无返回值。
     * 调用方为需要异步确认的规则节点；涉及 Rule Engine 队列，不直接涉及事务、缓存、MQTT、数据库。
     */
    void enqueueForTellNext(TbMsg msg, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 中文说明：方法职责是异步投递多关系消息并接收投递结果回调；relationTypes 是目标关系集合；无返回值。
     * 调用方为异步分支节点；涉及 Rule Engine 队列/Actor 路由，不直接涉及事务、缓存、MQTT、数据库。
     */
    void enqueueForTellNext(TbMsg msg, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 中文说明：方法职责是把消息投递到指定队列并按单关系继续路由；queueName 是目标队列；无返回值。
     * 调用方为需要控制队列的节点；涉及 Rule Engine 队列，不直接涉及事务、缓存、MQTT、数据库。
     */
    void enqueueForTellNext(TbMsg msg, String queueName, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 中文说明：方法职责是把消息投递到指定队列并按多个关系继续路由；relationTypes 是目标关系集合；无返回值。
     * 调用方为跨队列多分支节点；涉及 Rule Engine 队列/Actor 路由，不直接涉及事务、缓存、MQTT、数据库。
     */
    void enqueueForTellNext(TbMsg msg, String queueName, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * 中文说明：方法职责是确认消息处理完成；tbMsg 是需要 ACK 的消息；无返回值。
     * 调用时机为规则链处理栈完成或无需继续路由时；涉及 Rule Engine 消息确认，接口不直接涉及事务、缓存、MQTT、数据库。
     */
    void ack(TbMsg tbMsg);

    /**
     * 中文说明：方法职责是创建旧版字符串类型消息；参数表示队列、类型、发起实体、元数据和数据体；返回新 TbMsg。
     * 调用方为兼容旧节点；线程安全由实现保证，不直接涉及事务、缓存、MQTT、Actor、数据库，但直接服务 Rule Engine 消息创建。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    TbMsg newMsg(String queueName, String type, EntityId originator, TbMsgMetaData metaData, String data);

    /**
     * Creates a new TbMsg instance with the specified parameters.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #newMsg(String, TbMsgType, EntityId, CustomerId, TbMsgMetaData, String)}
     * method instead.</p>
     *
     * @param queueName   the name of the queue where the message will be sent
     * @param type        the type of the message
     * @param originator  the originator of the message
     * @param customerId  the ID of the customer associated with the message
     * @param metaData    the metadata of the message
     * @param data        the data of the message
     * @return new TbMsg instance
     *
     * 中文说明：方法职责是创建带客户 ID 的旧版字符串类型消息；参数补充 customerId 表示客户上下文；返回新 TbMsg。
     * 调用时机为兼容自定义消息类型；不直接涉及事务、缓存、MQTT、Actor、数据库，但用于 Rule Engine 消息构造。
     */
    @Deprecated(since = "3.6.0")
    TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data);

    /**
     * 中文说明：方法职责是基于原消息转换出旧版字符串类型消息；参数表示新类型、发起实体、元数据和数据体；返回派生 TbMsg。
     * 调用方为兼容旧转换节点；不直接涉及事务、缓存、MQTT、Actor、数据库，直接服务 Rule Engine 消息转换。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data);

    /**
     * 中文说明：方法职责是创建枚举类型消息；queueName/type/originator/metaData/data 构成消息上下文；返回新 TbMsg。
     * 调用方为规则节点；无共享状态，接口不直接涉及事务、缓存、MQTT、Actor、数据库，服务 Rule Engine 消息创建。
     */
    TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data);

    /**
     * 中文说明：方法职责是创建带客户 ID 的枚举类型消息；customerId 用于权限和客户上下文；返回新 TbMsg。
     * 调用时机为节点生成新消息时；不直接涉及事务、缓存、MQTT、Actor、数据库，服务 Rule Engine。
     */
    TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data);

    /**
     * 中文说明：方法职责是基于原消息变更类型、发起者、元数据和数据体；返回转换后的 TbMsg。
     * 调用方为转换节点；线程安全由实现保证，不直接涉及事务、缓存、MQTT、Actor、数据库，服务 Rule Engine 消息转换。
     */
    TbMsg transformMsg(TbMsg origMsg, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data);

    /**
     * 中文说明：方法职责是在保留原消息类型和发起者的基础上替换元数据和数据体；返回转换后的 TbMsg。
     * 调用方为转换节点；不直接涉及事务、缓存、MQTT、Actor、数据库，服务 Rule Engine 消息转换。
     */
    TbMsg transformMsg(TbMsg origMsg, TbMsgMetaData metaData, String data);

    /**
     * 中文说明：方法职责是仅替换消息发起实体；originator 是新的实体 ID；返回转换后的 TbMsg。
     * 调用方为 Change Originator 节点；不直接涉及事务、缓存、MQTT、Actor、数据库，服务 Rule Engine 路由上下文变更。
     */
    TbMsg transformMsgOriginator(TbMsg origMsg, EntityId originator);

    /**
     * 中文说明：方法职责是为客户创建事件生成规则消息；customer 是新客户，ruleNodeId 是触发节点；返回 TbMsg。
     * 调用方为实体事件流程；不直接涉及 MQTT/Actor/事务，可能由实现补充 Rule Engine 元数据。
     */
    TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId);

    /**
     * 中文说明：方法职责是为设备创建事件生成规则消息；device 是新设备，ruleNodeId 是触发节点；返回 TbMsg。
     * 调用方为实体事件流程；不直接涉及 MQTT/Actor/事务，服务 Rule Engine 实体事件消息创建。
     */
    TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId);

    /**
     * 中文说明：方法职责是为资产创建事件生成规则消息；asset 是新资产，ruleNodeId 是触发节点；返回 TbMsg。
     * 调用方为实体事件流程；不直接涉及 MQTT/Actor/事务，服务 Rule Engine 实体事件消息创建。
     */
    TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId);

    /**
     * 中文说明：方法职责是创建旧版字符串告警动作消息；action 是动作名；返回 TbMsg。
     * 调用方为兼容旧告警流程；不直接涉及缓存/MQTT/数据库，服务 Rule Engine 告警消息创建。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    TbMsg alarmActionMsg(Alarm alarm, RuleNodeId ruleNodeId, String action);

    /**
     * 中文说明：方法职责是创建枚举类型告警动作消息；actionMsgType 是告警动作消息类型；返回 TbMsg。
     * 调用方为告警节点或告警服务；不直接涉及事务、缓存、MQTT、Actor、数据库，服务 Rule Engine 告警流程。
     */
    TbMsg alarmActionMsg(Alarm alarm, RuleNodeId ruleNodeId, TbMsgType actionMsgType);

    /**
     * 中文说明：方法职责是为属性更新生成动作消息；scope 和 attributes 表示属性范围与值；返回 TbMsg。
     * 调用方为属性服务或规则节点；不直接访问数据库，服务 Rule Engine 属性事件流程。
     */
    TbMsg attributesUpdatedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<AttributeKvEntry> attributes);

    /**
     * 中文说明：方法职责是为属性删除生成动作消息；keys 是被删除属性键集合；返回 TbMsg。
     * 调用方为属性服务或规则节点；不直接涉及事务、缓存、MQTT、Actor、数据库，服务 Rule Engine 属性事件流程。
     */
    TbMsg attributesDeletedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<String> keys);

    /**
     * 中文说明：方法职责是通知 Edge 事件发生更新；tenantId/edgeId 表示租户和边缘实例；无返回值。
     * 调用方为 Edge 相关节点或服务；接口不直接涉及 MQTT/数据库，具体实现可能通过队列/Actor 通知 Edge 流程。
     */
    void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId);

    /*
     *
     *  METHODS TO PROCESS THE MESSAGES
     *
     */

    /**
     * 中文说明：方法职责是延迟执行任务；runnable 是任务，delay/timeUnit 是延迟时间；无返回值。
     * 调用方为需要定时逻辑的节点；线程安全由调度实现保证，不直接涉及事务、缓存、MQTT、数据库，服务 Rule Engine 调度流程。
     */
    void schedule(Runnable runnable, long delay, TimeUnit timeUnit);

    /**
     * 中文说明：方法职责是校验实体是否属于当前租户；entityId 是待校验实体；无返回值，失败抛出 TbNodeException。
     * 调用方为访问实体前的节点；实现可能查数据库/缓存，不涉及 MQTT，直接服务 Rule Engine 租户隔离。
     */
    void checkTenantEntity(EntityId entityId) throws TbNodeException;

    /**
     * 中文说明：方法职责是判断实体是否由当前服务本地负责；entityId 是实体标识；返回 true 表示本地实体。
     * 调用方为需要分区/本地性判断的节点；实现可能依赖缓存或集群状态，不直接涉及事务、MQTT、数据库。
     */
    boolean isLocalEntity(EntityId entityId);

    /**
     * 中文说明：方法职责是返回当前规则节点 ID；无输入；返回 RuleNodeId。
     * 调用方为节点记录状态、生成事件或日志；线程安全，接口不直接涉及事务、缓存、MQTT、Actor、数据库，服务 Rule Engine。
     */
    RuleNodeId getSelfId();

    /**
     * 中文说明：方法职责是返回当前规则节点数据对象；无输入；返回 RuleNode。
     * 调用方为节点读取自身配置或元数据；实现可能读取缓存，不直接涉及 MQTT/数据库事务，服务 Rule Engine。
     */
    RuleNode getSelf();

    /**
     * 中文说明：方法职责是返回当前规则链名称；无输入；返回字符串名称。
     * 调用方为日志、审计或消息构造；线程安全取决于实现，不直接涉及事务、缓存、MQTT、Actor、数据库。
     */
    String getRuleChainName();

    /**
     * 中文说明：方法职责是返回当前节点所在队列名称；无输入；返回队列名。
     * 调用方为创建新消息或跨队列路由的节点；不直接涉及数据库/事务/MQTT，服务 Rule Engine 队列流程。
     */
    String getQueueName();

    /**
     * 中文说明：方法职责是返回当前租户 ID；无输入；返回 TenantId。
     * 调用方为所有需要租户隔离的规则节点；线程安全，不直接涉及事务、缓存、MQTT、Actor、数据库。
     */
    TenantId getTenantId();

    /**
     * 中文说明：方法职责是返回属性服务；无输入；返回 AttributesService。
     * 调用方为属性相关节点；接口本身不访问数据库，返回服务实现可能涉及数据库、缓存和事务，不涉及 MQTT，服务 Rule Engine。
     */
    AttributesService getAttributesService();

    /**
     * 中文说明：方法职责是返回客户服务；无输入；返回 CustomerService。
     * 调用方为元数据和实体查询节点；返回服务可能访问数据库/缓存，不直接涉及 MQTT/Actor，服务 Rule Engine。
     */
    CustomerService getCustomerService();

    /**
     * 中文说明：方法职责是返回租户服务；无输入；返回 TenantService。
     * 调用方为租户详情节点和系统流程；返回服务可能访问数据库/缓存/事务，不涉及 MQTT，服务 Rule Engine。
     */
    TenantService getTenantService();

    /**
     * 中文说明：方法职责是返回用户服务；无输入；返回 UserService。
     * 调用方为用户元数据或通知节点；返回服务可能访问数据库/缓存，不直接涉及 MQTT/Actor。
     */
    UserService getUserService();

    /**
     * 中文说明：方法职责是返回资产服务；无输入；返回 AssetService。
     * 调用方为资产元数据节点；返回服务可能访问数据库/缓存，不直接涉及 MQTT，服务 Rule Engine。
     */
    AssetService getAssetService();

    /**
     * 中文说明：方法职责是返回设备服务；无输入；返回 DeviceService。
     * 调用方为设备元数据和状态节点；返回服务可能访问数据库/缓存，不直接涉及 MQTT，服务 Rule Engine。
     */
    DeviceService getDeviceService();

    /**
     * 中文说明：方法职责是返回设备配置服务；无输入；返回 DeviceProfileService。
     * 调用方为设备配置节点；返回服务可能访问数据库/缓存，不直接涉及 MQTT/Actor，服务 Rule Engine。
     */
    DeviceProfileService getDeviceProfileService();

    /**
     * 中文说明：方法职责是返回资产配置服务；无输入；返回 AssetProfileService。
     * 调用方为资产配置相关节点；返回服务可能访问数据库/缓存，不直接涉及 MQTT/Actor。
     */
    AssetProfileService getAssetProfileService();

    /**
     * 中文说明：方法职责是返回设备凭据服务；无输入；返回 DeviceCredentialsService。
     * 调用方为凭据获取节点；返回服务可能访问数据库/缓存，不直接涉及 MQTT，服务 Rule Engine。
     */
    DeviceCredentialsService getDeviceCredentialsService();

    /**
     * 中文说明：方法职责是返回 Rule Engine 设备状态管理器；无输入；返回 RuleEngineDeviceStateManager。
     * 调用方为设备状态节点；实现可能涉及缓存/数据库和异步回调，不直接涉及 MQTT，服务 Rule Engine。
     */
    RuleEngineDeviceStateManager getDeviceStateManager();

    /**
     * 中文说明：方法职责是返回设备状态节点限流配置；无输入；返回配置字符串。
     * 调用方为设备状态节点初始化；可能来自配置缓存或环境配置，不涉及数据库事务/MQTT/Actor。
     */
    String getDeviceStateNodeRateLimitConfig();

    /**
     * 中文说明：方法职责是返回集群服务；无输入；返回 TbClusterService。
     * 调用方为需要跨服务通信的节点；返回服务可能涉及集群消息/Actor，不直接涉及 MQTT 或数据库。
     */
    TbClusterService getClusterService();

    /**
     * 中文说明：方法职责是返回仪表盘服务；无输入；返回 DashboardService。
     * 调用方为仪表盘相关节点或通知流程；返回服务可能访问数据库/缓存，不直接涉及 MQTT。
     */
    DashboardService getDashboardService();

    /**
     * 中文说明：方法职责是返回告警服务；无输入；返回 RuleEngineAlarmService。
     * 调用方为告警节点；服务实现通常涉及数据库、缓存和事务，不直接涉及 MQTT，直接服务 Rule Engine。
     */
    RuleEngineAlarmService getAlarmService();

    /**
     * 中文说明：方法职责是返回告警评论服务；无输入；返回 AlarmCommentService。
     * 调用方为告警扩展节点或管理流程；返回服务可能访问数据库，不直接涉及 MQTT/Actor。
     */
    AlarmCommentService getAlarmCommentService();

    /**
     * 中文说明：方法职责是返回规则链服务；无输入；返回 RuleChainService。
     * 调用方为规则链节点和元数据流程；返回服务可能访问数据库/缓存，并直接服务 Rule Engine。
     */
    RuleChainService getRuleChainService();

    /**
     * 中文说明：方法职责是返回 RPC 服务；无输入；返回 RuleEngineRpcService。
     * 调用方为 RPC 节点；实现可能涉及设备传输、持久化和集群通信，不直接由接口操作 MQTT/Actor。
     */
    RuleEngineRpcService getRpcService();

    /**
     * 中文说明：方法职责是返回遥测服务；无输入；返回 RuleEngineTelemetryService。
     * 调用方为遥测和属性节点；实现通常涉及数据库、缓存、订阅通知，不直接由接口处理 MQTT/Actor。
     */
    RuleEngineTelemetryService getTelemetryService();

    /**
     * 中文说明：方法职责是返回时序服务；无输入；返回 TimeseriesService。
     * 调用方为遥测查询节点；返回服务通常访问时序数据库和 latest 缓存，不直接涉及 MQTT。
     */
    TimeseriesService getTimeseriesService();

    /**
     * 中文说明：方法职责是返回关系服务；无输入；返回 RelationService。
     * 调用方为关系查询节点；返回服务可能访问数据库/缓存，不直接涉及 MQTT/Actor。
     */
    RelationService getRelationService();

    /**
     * 中文说明：方法职责是返回实体视图服务；无输入；返回 EntityViewService。
     * 调用方为实体视图相关节点；返回服务可能访问数据库/缓存，不直接涉及 MQTT。
     */
    EntityViewService getEntityViewService();

    /**
     * 中文说明：方法职责是返回资源服务；无输入；返回 ResourceService。
     * 调用方为脚本、资源或外部节点；返回服务可能访问数据库/文件存储/缓存，不直接涉及 MQTT。
     */
    ResourceService getResourceService();

    /**
     * 中文说明：方法职责是返回 OTA 包服务；无输入；返回 OtaPackageService。
     * 调用方为 OTA 相关节点；返回服务可能访问数据库/文件存储，不直接涉及 MQTT/Actor。
     */
    OtaPackageService getOtaPackageService();

    /**
     * 中文说明：方法职责是返回设备配置缓存；无输入；返回 RuleEngineDeviceProfileCache。
     * 调用方为设备相关节点；直接涉及缓存服务，可能间接访问数据库，不涉及 MQTT。
     */
    RuleEngineDeviceProfileCache getDeviceProfileCache();

    /**
     * 中文说明：方法职责是返回资产配置缓存；无输入；返回 RuleEngineAssetProfileCache。
     * 调用方为资产相关节点；直接涉及缓存服务，可能间接访问数据库，不涉及 MQTT。
     */
    RuleEngineAssetProfileCache getAssetProfileCache();

    /**
     * 中文说明：方法职责是返回 Edge 服务；无输入；返回 EdgeService。
     * 调用方为 Edge 相关节点；返回服务可能访问数据库和 Edge 事件队列，不直接涉及 MQTT。
     */
    EdgeService getEdgeService();

    /**
     * 中文说明：方法职责是返回 Edge 事件服务；无输入；返回 EdgeEventService。
     * 调用方为 Edge 推送节点；实现可能访问数据库/队列，不直接涉及 MQTT，服务 Rule Engine Edge 流程。
     */
    EdgeEventService getEdgeEventService();

    /**
     * 中文说明：方法职责是返回队列服务；无输入；返回 QueueService。
     * 调用方为需要队列元数据或投递能力的节点；可能涉及消息队列/Actor，不直接访问数据库。
     */
    QueueService getQueueService();

    /**
     * 中文说明：方法职责是返回邮件发送执行器；无输入；返回 ListeningExecutor。
     * 调用方为邮件节点；线程安全由执行器实现保证，不直接涉及数据库/MQTT/Actor，服务 Rule Engine 异步外部调用。
     */
    ListeningExecutor getMailExecutor();

    /**
     * 中文说明：方法职责是返回短信发送执行器；无输入；返回 ListeningExecutor。
     * 调用方为短信节点；不直接涉及事务、缓存、MQTT、数据库，服务 Rule Engine 异步短信调用。
     */
    ListeningExecutor getSmsExecutor();

    /**
     * 中文说明：方法职责是返回数据库回调执行器；无输入；返回 ListeningExecutor。
     * 调用方为需要处理 DAO Future 回调的节点；间接服务数据库异步回调，不直接涉及 MQTT。
     */
    ListeningExecutor getDbCallbackExecutor();

    /**
     * 中文说明：方法职责是返回外部调用执行器；无输入；返回 ListeningExecutor。
     * 调用方为 REST、MQTT、Kafka 等外部节点；不直接涉及数据库事务，可能执行外部协议调用。
     */
    ListeningExecutor getExternalCallExecutor();

    /**
     * 中文说明：方法职责是返回通知执行器；无输入；返回 ListeningExecutor。
     * 调用方为通知节点；服务异步通知投递，不直接涉及 MQTT/数据库，具体实现可能调用外部渠道。
     */
    ListeningExecutor getNotificationExecutor();

    /**
     * 中文说明：方法职责是返回发布订阅规则节点执行器提供者；无输入；返回 ExecutorProvider。
     * 调用方为 Pub/Sub 类节点；线程安全由执行器提供者保证，可能涉及外部消息系统，不直接涉及数据库事务。
     */
    ExecutorProvider getPubSubRuleNodeExecutorProvider();

    /**
     * 中文说明：方法职责是返回邮件服务；isSystem 指示使用系统配置还是租户配置；返回 MailService。
     * 调用方为邮件节点和系统流程；实现可能读取配置缓存并调用 SMTP，不直接涉及 MQTT/Actor。
     */
    MailService getMailService(boolean isSystem);

    /**
     * 中文说明：方法职责是返回短信服务；无输入；返回 SmsService。
     * 调用方为短信节点；实现可能读取配置缓存并调用外部短信 API，不直接涉及 MQTT/Actor。
     */
    SmsService getSmsService();

    /**
     * 中文说明：方法职责是返回短信发送器工厂；无输入；返回 SmsSenderFactory。
     * 调用方为短信服务或测试配置流程；不直接涉及数据库/MQTT/Actor，服务外部短信客户端创建。
     */
    SmsSenderFactory getSmsSenderFactory();

    /**
     * 中文说明：方法职责是返回通知中心；无输入；返回 NotificationCenter。
     * 调用方为通知节点；实现可能涉及数据库、缓存和外部渠道，不直接由接口处理 MQTT/Actor。
     */
    NotificationCenter getNotificationCenter();

    /**
     * 中文说明：方法职责是返回通知目标服务；无输入；返回 NotificationTargetService。
     * 调用方为通知节点和配置流程；实现可能访问数据库/缓存，不直接涉及 MQTT。
     */
    NotificationTargetService getNotificationTargetService();

    /**
     * 中文说明：方法职责是返回通知模板服务；无输入；返回 NotificationTemplateService。
     * 调用方为通知节点；实现可能访问数据库/缓存，不直接涉及 MQTT/Actor。
     */
    NotificationTemplateService getNotificationTemplateService();

    /**
     * 中文说明：方法职责是返回通知请求服务；无输入；返回 NotificationRequestService。
     * 调用方为通知中心和节点；实现通常访问数据库，不直接涉及 MQTT/Actor。
     */
    NotificationRequestService getNotificationRequestService();

    /**
     * 中文说明：方法职责是返回通知规则服务；无输入；返回 NotificationRuleService。
     * 调用方为通知相关流程；实现可能访问数据库/缓存，不直接涉及 MQTT。
     */
    NotificationRuleService getNotificationRuleService();

    /**
     * 中文说明：方法职责是返回 Slack 服务；无输入；返回 SlackService。
     * 调用方为 Slack 通知节点；实现可能访问配置缓存并调用外部 Slack API，不直接涉及 MQTT/Actor。
     */
    SlackService getSlackService();

    /**
     * 中文说明：方法职责是判断外部节点是否强制 ACK；无输入；返回布尔配置。
     * 调用方为外部调用节点；线程安全取决于配置读取实现，不直接涉及事务、缓存、MQTT、数据库。
     */
    boolean isExternalNodeForceAck();

    /**
     * Creates JS Script Engine
     * @deprecated
     * <p> Use {@link #createScriptEngine} instead.
     *
     * 中文说明：方法职责是创建旧版 JavaScript 脚本引擎；script 是脚本文本，argNames 是参数名；返回 ScriptEngine。
     * 调用方为兼容旧脚本节点；实现可能使用脚本运行时缓存，不直接涉及事务、MQTT、Actor、数据库，服务 Rule Engine 脚本流程。
     */
    @Deprecated
    ScriptEngine createJsScriptEngine(String script, String... argNames);

    /**
     * 中文说明：方法职责是按脚本语言创建脚本引擎；scriptLang 是语言，script 是脚本文本，argNames 是参数名；返回 ScriptEngine。
     * 调用方为脚本节点初始化；实现可能涉及脚本编译缓存和执行器，不直接涉及 MQTT/数据库事务，服务 Rule Engine。
     */
    ScriptEngine createScriptEngine(ScriptLanguage scriptLang, String script, String... argNames);

    /**
     * 中文说明：方法职责是记录一次 JS 脚本执行请求；无输入无返回。
     * 调用方为脚本节点；实现可能更新指标/配额缓存，不直接涉及数据库、MQTT、Actor，服务 Rule Engine API 使用统计。
     */
    void logJsEvalRequest();

    /**
     * 中文说明：方法职责是记录一次 JS 脚本执行成功响应；无输入无返回。
     * 调用方为脚本节点回调；实现可能更新指标/配额，不直接涉及事务、MQTT、数据库。
     */
    void logJsEvalResponse();

    /**
     * 中文说明：方法职责是记录一次 JS 脚本执行失败；无输入无返回。
     * 调用方为脚本节点异常处理；实现可能更新指标/配额，不直接涉及 MQTT/Actor/数据库事务。
     */
    void logJsEvalFailure();

    /**
     * 中文说明：方法职责是返回当前服务实例 ID；无输入；返回字符串。
     * 调用方为 RPC、集群和外部节点；不直接涉及事务、缓存、MQTT、数据库，可能用于 Actor/集群路由。
     */
    String getServiceId();

    /**
     * 中文说明：方法职责是返回共享 Netty EventLoopGroup；无输入；返回 EventLoopGroup。
     * 调用方为网络外部节点；线程安全由 Netty 保证，可能涉及 MQTT/HTTP 等外部协议，不直接涉及数据库事务。
     */
    EventLoopGroup getSharedEventLoop();

    /**
     * 中文说明：方法职责是返回 Cassandra 集群访问对象；无输入；返回 CassandraCluster。
     * 调用方为需要直接提交 Cassandra 任务的节点或服务；直接关联数据库，不涉及 MQTT，事务语义由 Cassandra 操作决定。
     */
    CassandraCluster getCassandraCluster();

    /**
     * 中文说明：方法职责是提交 Cassandra 读任务；task 是 CQL/语句任务；返回异步结果。
     * 调用方为需要异步读 Cassandra 的节点；涉及数据库和回调执行，不直接涉及 MQTT/Actor。
     */
    TbResultSetFuture submitCassandraReadTask(CassandraStatementTask task);

    /**
     * 中文说明：方法职责是提交 Cassandra 写任务；task 是 CQL/语句任务；返回异步结果。
     * 调用方为需要异步写 Cassandra 的节点；涉及数据库写入，事务取决于 Cassandra，不直接涉及 MQTT/Actor。
     */
    TbResultSetFuture submitCassandraWriteTask(CassandraStatementTask task);

    /**
     * 中文说明：方法职责是分页查询当前节点状态；pageLink 是分页条件；返回 RuleNodeState 分页。
     * 调用方为需要读取节点状态的规则节点；实现通常访问数据库，不直接涉及 MQTT/Actor。
     */
    PageData<RuleNodeState> findRuleNodeStates(PageLink pageLink);

    /**
     * 中文说明：方法职责是按实体查询当前节点状态；entityId 是实体标识；返回 RuleNodeState。
     * 调用方为状态型节点；实现通常访问数据库或缓存，不直接涉及 MQTT。
     */
    RuleNodeState findRuleNodeStateForEntity(EntityId entityId);

    /**
     * 中文说明：方法职责是删除指定实体的当前节点状态；entityId 是实体标识；无返回值。
     * 调用方为状态清理节点；实现通常访问数据库，不直接涉及 MQTT/Actor。
     */
    void removeRuleNodeStateForEntity(EntityId entityId);

    /**
     * 中文说明：方法职责是保存当前节点状态；state 是待保存状态；返回保存后的 RuleNodeState。
     * 调用方为需要持久化状态的节点；实现通常访问数据库/缓存，线程安全由实现保证。
     */
    RuleNodeState saveRuleNodeState(RuleNodeState state);

    /**
     * 中文说明：方法职责是清空当前节点所有状态；无输入无返回。
     * 调用方为节点重置或销毁流程；实现通常访问数据库，不直接涉及 MQTT/Actor。
     */
    void clearRuleNodeStates();

    /**
     * 中文说明：方法职责是注册租户配置变更监听；listener 是回调；无返回。
     * 调用方为依赖租户配置的节点；实现涉及配置缓存监听，不直接涉及 MQTT/数据库事务。
     */
    void addTenantProfileListener(Consumer<TenantProfile> listener);

    /**
     * 中文说明：方法职责是注册设备配置变更监听；listener/deviceListener 分别处理配置和设备关联变更；无返回。
     * 调用方为设备状态/配置节点；实现涉及缓存监听，不直接涉及 MQTT，可能间接读取数据库。
     */
    void addDeviceProfileListeners(Consumer<DeviceProfile> listener, BiConsumer<DeviceId, DeviceProfile> deviceListener);

    /**
     * 中文说明：方法职责是注册资产配置变更监听；listener/assetListener 分别处理配置和资产关联变更；无返回。
     * 调用方为资产配置相关节点；实现涉及缓存监听，不直接涉及 MQTT/Actor。
     */
    void addAssetProfileListeners(Consumer<AssetProfile> listener, BiConsumer<AssetId, AssetProfile> assetListener);

    /**
     * 中文说明：方法职责是移除当前上下文注册的监听器；无输入无返回。
     * 调用方为节点 destroy 生命周期；实现涉及缓存监听清理，不直接涉及事务、MQTT、数据库。
     */
    void removeListeners();

    /**
     * 中文说明：方法职责是返回当前租户配置；无输入；返回 TenantProfile。
     * 调用方为需要租户级配置的节点；实现可能读取缓存，不直接涉及 MQTT/Actor/数据库事务。
     */
    TenantProfile getTenantProfile();

    /**
     * 中文说明：方法职责是返回部件包服务；无输入；返回 WidgetsBundleService。
     * 调用方为仪表盘/通知相关流程；实现可能访问数据库，不直接涉及 MQTT。
     */
    WidgetsBundleService getWidgetBundleService();

    /**
     * 中文说明：方法职责是返回部件类型服务；无输入；返回 WidgetTypeService。
     * 调用方为 UI/仪表盘相关流程；实现可能访问数据库/缓存，不直接涉及 Rule Engine 消息路由。
     */
    WidgetTypeService getWidgetTypeService();

    /**
     * 中文说明：方法职责是返回 Rule Engine API 使用状态服务；无输入；返回 RuleEngineApiUsageStateService。
     * 调用方为限额或统计相关节点；实现可能访问数据库/缓存，不直接涉及 MQTT/Actor。
     */
    RuleEngineApiUsageStateService getRuleEngineApiUsageStateService();

    /**
     * 中文说明：方法职责是返回通用实体服务；无输入；返回 EntityService。
     * 调用方为元数据和实体查询节点；实现可能访问数据库/缓存，不直接涉及 MQTT。
     */
    EntityService getEntityService();

    /**
     * 中文说明：方法职责是返回事件服务；无输入；返回 EventService。
     * 调用方为事件创建或查询节点；实现通常访问数据库，不直接涉及 MQTT/Actor。
     */
    EventService getEventService();

    /**
     * 中文说明：方法职责是返回审计日志服务；无输入；返回 AuditLogService。
     * 调用方为需要记录审计的规则流程；实现通常访问数据库，不直接涉及 MQTT，可能在事务边界内执行。
     */
    AuditLogService getAuditLogService();
}

/*
 * 本类总结：
 * 1. 核心职责：为规则节点提供消息路由、服务访问、异步执行、脚本执行、状态存储和监听器管理的统一上下文。
 * 2. 核心流程：节点在 init 获取配置和服务，在 onMsg 中创建/路由消息并调用平台服务，在 destroy 中移除监听和释放资源。
 * 3. 关键依赖：TbNode、TbMsg、规则链 Actor、DAO 服务、缓存、队列、脚本引擎、告警/遥测/RPC/通知服务。
 * 4. 学习重点：TbContext 是规则节点与 ThingsBoard 平台能力之间的 Facade，节点通过它使用服务而不是直接耦合底层实现。
 */
