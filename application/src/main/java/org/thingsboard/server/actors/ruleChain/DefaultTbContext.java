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
package org.thingsboard.server.actors.ruleChain;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Arrays;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.RuleEngineApiUsageStateService;
import org.thingsboard.rule.engine.api.RuleEngineAssetProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.rule.engine.util.TenantIdLoader;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasRuleEngineProfile;
import org.thingsboard.server.common.data.StringUtils;
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
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.TbMsgProcessingStackItem;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
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
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.service.executors.PubSubRuleNodeExecutorProvider;
import org.thingsboard.server.queue.common.SimpleTbQueueCallback;
import org.thingsboard.server.service.script.RuleNodeJsScriptEngine;
import org.thingsboard.server.service.script.RuleNodeTbelScriptEngine;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_DELETED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_UPDATED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_CREATED;

/**
 * Created by ashvayka on 19.03.18.
 */
/**
 * 中文说明：
 * 1. `DefaultTbContext` 是 ThingsBoard Application 中承载 `Tb Context` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbContext`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
class DefaultTbContext implements TbContext {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final ActorSystemContext mainCtx;
    private final String ruleChainName;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final RuleNodeCtx nodeCtx;

    /**
     * 功能：创建 `DefaultTbContext` 实例，并初始化必要字段。
     * 参数：
     * - `mainCtx`：处理上下文。
     * - `ruleChainName`：名称。
     * - `nodeCtx`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public DefaultTbContext(ActorSystemContext mainCtx, String ruleChainName, RuleNodeCtx nodeCtx) {
        this.mainCtx = mainCtx;
        this.ruleChainName = ruleChainName;
        this.nodeCtx = nodeCtx;
    }

    /**
     * 功能：执行 `tellSuccess` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void tellSuccess(TbMsg msg) {
        tellNext(msg, Collections.singleton(TbNodeConnectionType.SUCCESS), null);
    }

    /**
     * 功能：执行 `tellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationType`：类型。
     * 返回：无。
     */
    @Override
    public void tellNext(TbMsg msg, String relationType) {
        tellNext(msg, Collections.singleton(relationType), null);
    }

    /**
     * 功能：执行 `tellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationTypes`：类型。
     * 返回：无。
     */
    @Override
    public void tellNext(TbMsg msg, Set<String> relationTypes) {
        tellNext(msg, relationTypes, null);
    }

    /**
     * 功能：执行 `tellNext` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationTypes`：类型。
     * - `th`：`th` 参数。
     * 返回：无。
     */
    private void tellNext(TbMsg msg, Set<String> relationTypes, Throwable th) {
        if (nodeCtx.getSelf().isDebugMode()) {
            relationTypes.forEach(relationType -> mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, relationType, th));
        }
        msg.getCallback().onProcessingEnd(nodeCtx.getSelf().getId());
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId(), relationTypes, msg, th != null ? th.getMessage() : null));
    }

    /**
     * 功能：执行 `tellSelf` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `delayMs`：`delayMs` 参数。
     * 返回：无。
     */
    @Override
    public void tellSelf(TbMsg msg, long delayMs) {
        //TODO: add persistence layer
        scheduleMsgWithDelay(new RuleNodeToSelfMsg(this, msg), delayMs, nodeCtx.getSelfActor());
    }

    /**
     * 功能：执行 `input` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    @Override
    public void input(TbMsg msg, RuleChainId ruleChainId) {
        msg.pushToStack(nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId());
        nodeCtx.getChainActor().tell(new RuleChainInputMsg(ruleChainId, msg));
    }

    /**
     * 功能：执行 `output` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `relationType`：类型。
     * 返回：无。
     */
    @Override
    public void output(TbMsg msg, String relationType) {
        TbMsgProcessingStackItem item = msg.popFormStack();
        if (item == null) {
            ack(msg);
        } else {
            if (nodeCtx.getSelf().isDebugMode()) {
                mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, relationType);
            }
            nodeCtx.getChainActor().tell(new RuleChainOutputMsg(item.getRuleChainId(), item.getRuleNodeId(), relationType, msg));
        }
    }

    /**
     * 功能：执行 `enqueue` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    @Override
    public void enqueue(TbMsg tbMsg, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getQueueName(), getTenantId(), tbMsg.getOriginator());
        enqueue(tpi, tbMsg, onFailure, onSuccess);
    }

    /**
     * 功能：执行 `enqueue` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `queueName`：队列名称或队列对象。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    @Override
    public void enqueue(TbMsg tbMsg, String queueName, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg, queueName);
        enqueue(tpi, tbMsg, onFailure, onSuccess);
    }

    /**
     * 功能：执行 `enqueue` 对应的处理。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `tbMsg`：待处理消息。
     * - `onFailure`：`onFailure` 参数。
     * - `onSuccess`：`onSuccess` 参数。
     * 返回：无。
     */
    private void enqueue(TopicPartitionInfo tpi, TbMsg tbMsg, Consumer<Throwable> onFailure, Runnable onSuccess) {
        if (!tbMsg.isValid()) {
            log.trace("[{}] Skip invalid message: {}", getTenantId(), tbMsg);
            if (onFailure != null) {
                onFailure.accept(new IllegalArgumentException("Source message is no longer valid!"));
            }
            return;
        }
        TransportProtos.ToRuleEngineMsg msg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(getTenantId().getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg)).build();
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), tbMsg, "To Root Rule Chain");
        }
        mainCtx.getClusterService().pushMsgToRuleEngine(tpi, tbMsg.getId(), msg, new SimpleTbQueueCallback(
                metadata -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                },
                t -> {
                    if (onFailure != null) {
                        onFailure.accept(t);
                    } else {
                        log.debug("[{}] Failed to put item into queue!", nodeCtx.getTenantId().getId(), t);
                    }
                }));
    }

    /**
     * 功能：执行 `enqueueForTellFailure` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `failureMessage`：待处理消息。
     * 返回：无。
     */
    @Override
    public void enqueueForTellFailure(TbMsg tbMsg, String failureMessage) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(TbNodeConnectionType.FAILURE), failureMessage, null, null);
    }

    /**
     * 功能：执行 `enqueueForTellFailure` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `th`：`th` 参数。
     * 返回：无。
     */
    @Override
    public void enqueueForTellFailure(TbMsg tbMsg, Throwable th) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(TbNodeConnectionType.FAILURE), getFailureMessage(th), null, null);
    }

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `relationType`：类型。
     * 返回：无。
     */
    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String relationType) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(relationType), null, null, null);
    }

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `relationTypes`：类型。
     * 返回：无。
     */
    @Override
    public void enqueueForTellNext(TbMsg tbMsg, Set<String> relationTypes) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, relationTypes, null, null, null);
    }

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `relationType`：类型。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(relationType), null, onSuccess, onFailure);
    }

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `relationTypes`：类型。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    @Override
    public void enqueueForTellNext(TbMsg tbMsg, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, relationTypes, null, onSuccess, onFailure);
    }

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `queueName`：队列名称或队列对象。
     * - `relationType`：类型。
     * - `onSuccess`：`onSuccess` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String queueName, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg, queueName);
        enqueueForTellNext(tpi, queueName, tbMsg, Collections.singleton(relationType), null, onSuccess, onFailure);
    }

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `queueName`：队列名称或队列对象。
     * - `relationTypes`：类型。
     * - `onSuccess`：`onSuccess` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String queueName, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg, queueName);
        enqueueForTellNext(tpi, queueName, tbMsg, relationTypes, null, onSuccess, onFailure);
    }

    /**
     * 功能：执行 `resolvePartition` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `queueName`：队列名称或队列对象。
     * 返回：处理结果。
     */
    private TopicPartitionInfo resolvePartition(TbMsg tbMsg, String queueName) {
        return mainCtx.resolve(ServiceType.TB_RULE_ENGINE, queueName, getTenantId(), tbMsg.getOriginator());
    }

    /**
     * 功能：执行 `resolvePartition` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * 返回：处理结果。
     */
    private TopicPartitionInfo resolvePartition(TbMsg tbMsg) {
        return resolvePartition(tbMsg, tbMsg.getQueueName());
    }

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `source`：`source` 参数。
     * - `relationTypes`：类型。
     * - `failureMessage`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void enqueueForTellNext(TopicPartitionInfo tpi, TbMsg source, Set<String> relationTypes, String failureMessage, Runnable onSuccess, Consumer<Throwable> onFailure) {
        enqueueForTellNext(tpi, source.getQueueName(), source, relationTypes, failureMessage, onSuccess, onFailure);
    }

    /**
     * 功能：执行 `enqueueForTellNext` 对应的处理。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `queueName`：队列名称或队列对象。
     * - `source`：`source` 参数。
     * - `relationTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void enqueueForTellNext(TopicPartitionInfo tpi, String queueName, TbMsg source, Set<String> relationTypes, String failureMessage, Runnable onSuccess, Consumer<Throwable> onFailure) {
        if (!source.isValid()) {
            log.trace("[{}] Skip invalid message: {}", getTenantId(), source);
            if (onFailure != null) {
                onFailure.accept(new IllegalArgumentException("Source message is no longer valid!"));
            }
            return;
        }
        RuleChainId ruleChainId = nodeCtx.getSelf().getRuleChainId();
        RuleNodeId ruleNodeId = nodeCtx.getSelf().getId();
        TbMsg tbMsg = TbMsg.newMsg(source, queueName, ruleChainId, ruleNodeId);
        TransportProtos.ToRuleEngineMsg.Builder msg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(getTenantId().getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg))
                .addAllRelationTypes(relationTypes);
        if (failureMessage != null) {
            msg.setFailureMessage(failureMessage);
        }
        if (nodeCtx.getSelf().isDebugMode()) {
            relationTypes.forEach(relationType ->
                    mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), tbMsg, relationType, null, failureMessage));
        }
        mainCtx.getClusterService().pushMsgToRuleEngine(tpi, tbMsg.getId(), msg.build(), new SimpleTbQueueCallback(
                metadata -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                },
                t -> {
                    if (onFailure != null) {
                        onFailure.accept(t);
                    } else {
                        log.debug("[{}] Failed to put item into queue!", nodeCtx.getTenantId().getId(), t);
                    }
                }));
    }

    /**
     * 功能：执行 `ack` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void ack(TbMsg tbMsg) {
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), tbMsg, "ACK", null);
        }
        tbMsg.getCallback().onProcessingEnd(nodeCtx.getSelf().getId());
        tbMsg.getCallback().onSuccess();
    }

    /**
     * 功能：判断实体。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：判断结果。
     */
    @Override
    public boolean isLocalEntity(EntityId entityId) {
        return mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getQueueName(), getTenantId(), entityId).isMyPartition();
    }

    /**
     * 功能：执行 `scheduleMsgWithDelay` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `delayInMs`：`delayInMs` 参数。
     * - `target`：`target` 参数。
     * 返回：无。
     */
    private void scheduleMsgWithDelay(TbActorMsg msg, long delayInMs, TbActorRef target) {
        mainCtx.scheduleMsgWithDelay(target, msg, delayInMs);
    }

    /**
     * 功能：执行 `tellFailure` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `th`：`th` 参数。
     * 返回：无。
     */
    @Override
    public void tellFailure(TbMsg msg, Throwable th) {
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, TbNodeConnectionType.FAILURE, th);
        }
        String failureMessage = getFailureMessage(th);
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getRuleChainId(),
                nodeCtx.getSelf().getId(), Collections.singleton(TbNodeConnectionType.FAILURE),
                msg, failureMessage));
    }

    /**
     * 功能：更新`Self`。
     * 参数：
     * - `self`：`self` 参数。
     * 返回：无。
     */
    public void updateSelf(RuleNode self) {
        nodeCtx.setSelf(self);
    }

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
    @Override
    public TbMsg newMsg(String queueName, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(queueName, type, originator, null, metaData, data);
    }

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
    @Override
    public TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return TbMsg.newMsg(queueName, type, originator, customerId, metaData, data, nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId());
    }

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
    @Override
    public TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return TbMsg.transformMsg(origMsg, type, originator, metaData, data);
    }

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
    @Override
    public TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(queueName, type, originator, null, metaData, data);
    }

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
    @Override
    public TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return TbMsg.newMsg(queueName, type, originator, customerId, metaData, data, nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId());
    }

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
    @Override
    public TbMsg transformMsg(TbMsg origMsg, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data) {
        return TbMsg.transformMsg(origMsg, type, originator, metaData, data);
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `metaData`：待处理数据。
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    @Override
    public TbMsg transformMsg(TbMsg origMsg, TbMsgMetaData metaData, String data) {
        return TbMsg.transformMsg(origMsg, metaData, data);
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `originator`：`originator` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbMsg transformMsgOriginator(TbMsg origMsg, EntityId originator) {
        return TbMsg.transformMsgOriginator(origMsg, originator);
    }

    /**
     * 功能：执行 `customerCreatedMsg` 对应的处理。
     * 参数：
     * - `customer`：`customer` 参数。
     * - `ruleNodeId`：规则节点ID。
     * 返回：处理结果。
     */
    @Override
    public TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId) {
        return entityActionMsg(customer, customer.getId(), ruleNodeId, ENTITY_CREATED);
    }

    /**
     * 功能：执行 `deviceCreatedMsg` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `ruleNodeId`：规则节点ID。
     * 返回：处理结果。
     */
    @Override
    public TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId) {
        DeviceProfile deviceProfile = null;
        if (device.getDeviceProfileId() != null) {
            deviceProfile = mainCtx.getDeviceProfileCache().find(device.getDeviceProfileId());
        }
        return entityActionMsg(device, device.getId(), ruleNodeId, ENTITY_CREATED, deviceProfile);
    }

    /**
     * 功能：执行 `assetCreatedMsg` 对应的处理。
     * 参数：
     * - `asset`：`asset` 参数。
     * - `ruleNodeId`：规则节点ID。
     * 返回：处理结果。
     */
    @Override
    public TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId) {
        AssetProfile assetProfile = null;
        if (asset.getAssetProfileId() != null) {
            assetProfile = mainCtx.getAssetProfileCache().find(asset.getAssetProfileId());
        }
        return entityActionMsg(asset, asset.getId(), ruleNodeId, ENTITY_CREATED, assetProfile);
    }

    /**
     * 功能：执行 `alarmActionMsg` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `ruleNodeId`：规则节点ID。
     * - `action`：`action` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbMsg alarmActionMsg(Alarm alarm, RuleNodeId ruleNodeId, String action) {
        EntityId originator = alarm.getOriginator();
        HasRuleEngineProfile profile = getRuleEngineProfile(originator);
        return entityActionMsg(alarm, originator, ruleNodeId, action, profile);
    }

    /**
     * 功能：执行 `alarmActionMsg` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `ruleNodeId`：规则节点ID。
     * - `actionMsgType`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    public TbMsg alarmActionMsg(Alarm alarm, RuleNodeId ruleNodeId, TbMsgType actionMsgType) {
        EntityId originator = alarm.getOriginator();
        HasRuleEngineProfile profile = getRuleEngineProfile(originator);
        return entityActionMsg(alarm, originator, ruleNodeId, actionMsgType, profile);
    }

    /**
     * 功能：获取规则引擎。
     * 参数：
     * - `originator`：`originator` 参数。
     * 返回：处理结果。
     */
    private HasRuleEngineProfile getRuleEngineProfile(EntityId originator) {
        HasRuleEngineProfile profile = null;
        if (EntityType.DEVICE.equals(originator.getEntityType())) {
            DeviceId deviceId = new DeviceId(originator.getId());
            profile = mainCtx.getDeviceProfileCache().get(getTenantId(), deviceId);
        } else if (EntityType.ASSET.equals(originator.getEntityType())) {
            AssetId assetId = new AssetId(originator.getId());
            profile = mainCtx.getAssetProfileCache().get(getTenantId(), assetId);
        }
        return profile;
    }

    /**
     * 功能：执行 `attributesUpdatedActionMsg` 对应的处理。
     * 参数：
     * - `originator`：`originator` 参数。
     * - `ruleNodeId`：规则节点ID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * 返回：处理结果。
     */
    @Override
    public TbMsg attributesUpdatedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<AttributeKvEntry> attributes) {
        ObjectNode entityNode = JacksonUtil.newObjectNode();
        if (attributes != null) {
            attributes.forEach(attributeKvEntry -> JacksonUtil.addKvEntry(entityNode, attributeKvEntry));
        }
        return attributesActionMsg(originator, ruleNodeId, scope, ATTRIBUTES_UPDATED, JacksonUtil.toString(entityNode));
    }

    /**
     * 功能：执行 `attributesDeletedActionMsg` 对应的处理。
     * 参数：
     * - `originator`：`originator` 参数。
     * - `ruleNodeId`：规则节点ID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * 返回：处理结果。
     */
    @Override
    public TbMsg attributesDeletedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<String> keys) {
        ObjectNode entityNode = JacksonUtil.newObjectNode();
        ArrayNode attrsArrayNode = entityNode.putArray("attributes");
        if (keys != null) {
            keys.forEach(attrsArrayNode::add);
        }
        return attributesActionMsg(originator, ruleNodeId, scope, ATTRIBUTES_DELETED, JacksonUtil.toString(entityNode));
    }

    /**
     * 功能：执行 `attributesActionMsg` 对应的处理。
     * 参数：
     * - `originator`：`originator` 参数。
     * - `ruleNodeId`：规则节点ID。
     * - `scope`：`scope` 参数。
     * - `actionMsgType`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private TbMsg attributesActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, TbMsgType actionMsgType, String msgData) {
        TbMsgMetaData tbMsgMetaData = getActionMetaData(ruleNodeId);
        tbMsgMetaData.putValue("scope", scope);
        HasRuleEngineProfile profile = getRuleEngineProfile(originator);
        return entityActionMsg(originator, tbMsgMetaData, msgData, actionMsgType, profile);
    }

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    @Override
    public void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId) {
        mainCtx.getClusterService().onEdgeEventUpdate(tenantId, edgeId);
    }

    /**
     * 功能：执行 `entityActionMsg` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `id`：`id`ID。
     * - `ruleNodeId`：规则节点ID。
     * - `actionMsgType`：待处理消息。
     * 返回：处理结果。
     */
    public <E, I extends EntityId> TbMsg entityActionMsg(E entity, I id, RuleNodeId ruleNodeId, TbMsgType actionMsgType) {
        return entityActionMsg(entity, id, ruleNodeId, actionMsgType, null);
    }

    /**
     * 功能：执行 `entityActionMsg` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `id`：`id`ID。
     * - `ruleNodeId`：规则节点ID。
     * - `action`：`action` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public <E, I extends EntityId, K extends HasRuleEngineProfile> TbMsg entityActionMsg(E entity, I id, RuleNodeId ruleNodeId, String action, K profile) {
        try {
            return entityActionMsg(id, getActionMetaData(ruleNodeId), JacksonUtil.toString(JacksonUtil.valueToTree(entity)), action, profile);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to process " + id.getEntityType().name().toLowerCase() + " " + action + " msg: " + e);
        }
    }

    /**
     * 功能：执行 `entityActionMsg` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `msgMetaData`：待处理消息。
     * - `msgData`：待处理消息。
     * - `action`：`action` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    private <I extends EntityId, K extends HasRuleEngineProfile> TbMsg entityActionMsg(I id, TbMsgMetaData msgMetaData, String msgData, String action, K profile) {
        String defaultQueueName = null;
        RuleChainId defaultRuleChainId = null;
        if (profile != null) {
            defaultQueueName = profile.getDefaultQueueName();
            defaultRuleChainId = profile.getDefaultRuleChainId();
        }
        return TbMsg.newMsg(defaultQueueName, action, id, msgMetaData, msgData, defaultRuleChainId, null);
    }

    /**
     * 功能：执行 `entityActionMsg` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `id`：`id`ID。
     * - `ruleNodeId`：规则节点ID。
     * - `actionMsgType`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public <E, I extends EntityId, K extends HasRuleEngineProfile> TbMsg entityActionMsg(E entity, I id, RuleNodeId ruleNodeId, TbMsgType actionMsgType, K profile) {
        try {
            return entityActionMsg(id, getActionMetaData(ruleNodeId), JacksonUtil.toString(JacksonUtil.valueToTree(entity)), actionMsgType, profile);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to process " + id.getEntityType().name().toLowerCase() + " " + actionMsgType.name() + " msg: " + e);
        }
    }

    /**
     * 功能：执行 `entityActionMsg` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `msgMetaData`：待处理消息。
     * - `msgData`：待处理消息。
     * - `actionMsgType`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private <I extends EntityId, K extends HasRuleEngineProfile> TbMsg entityActionMsg(I id, TbMsgMetaData msgMetaData, String msgData, TbMsgType actionMsgType, K profile) {
        String defaultQueueName = null;
        RuleChainId defaultRuleChainId = null;
        if (profile != null) {
            defaultQueueName = profile.getDefaultQueueName();
            defaultRuleChainId = profile.getDefaultRuleChainId();
        }
        return TbMsg.newMsg(defaultQueueName, actionMsgType, id, msgMetaData, msgData, defaultRuleChainId, null);
    }

    /**
     * 功能：获取`Self Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleNodeId getSelfId() {
        return nodeCtx.getSelf().getId();
    }

    /**
     * 功能：获取`Self`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleNode getSelf() {
        return nodeCtx.getSelf();
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getRuleChainName() {
        return ruleChainName;
    }

    /**
     * 功能：获取队列名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getQueueName() {
        return getSelf().getQueueName();
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TenantId getTenantId() {
        return nodeCtx.getTenantId();
    }

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListeningExecutor getMailExecutor() {
        return mainCtx.getMailExecutor();
    }

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListeningExecutor getSmsExecutor() {
        return mainCtx.getSmsExecutor();
    }

    /**
     * 功能：获取回调。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListeningExecutor getDbCallbackExecutor() {
        return mainCtx.getDbCallbackExecutor();
    }

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListeningExecutor getExternalCallExecutor() {
        return mainCtx.getExternalCallExecutorService();
    }

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListeningExecutor getNotificationExecutor() {
        return mainCtx.getNotificationExecutor();
    }

    /**
     * 功能：获取规则节点。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public PubSubRuleNodeExecutorProvider getPubSubRuleNodeExecutorProvider() {
        return mainCtx.getPubSubRuleNodeExecutorProvider();
    }

    /**
     * 功能：保存或创建脚本执行器。
     * 参数：
     * - `script`：`script` 参数。
     * - `argNames`：名称。
     * 返回：处理结果。
     */
    @Override
    @Deprecated
    public ScriptEngine createJsScriptEngine(String script, String... argNames) {
        return new RuleNodeJsScriptEngine(getTenantId(), mainCtx.getJsInvokeService(), script, argNames);
    }

    /**
     * 功能：保存或创建脚本执行器。
     * 参数：
     * - `script`：`script` 参数。
     * - `argNames`：名称。
     * 返回：处理结果。
     */
    private ScriptEngine createTbelScriptEngine(String script, String... argNames) {
        if (mainCtx.getTbelInvokeService() == null) {
            throw new RuntimeException("TBEL execution is disabled!");
        }
        return new RuleNodeTbelScriptEngine(getTenantId(), mainCtx.getTbelInvokeService(), script, argNames);
    }

    /**
     * 功能：保存或创建脚本执行器。
     * 参数：
     * - `scriptLang`：`scriptLang` 参数。
     * - `script`：`script` 参数。
     * - `argNames`：名称。
     * 返回：处理结果。
     */
    @Override
    public ScriptEngine createScriptEngine(ScriptLanguage scriptLang, String script, String... argNames) {
        if (scriptLang == null) {
            scriptLang = ScriptLanguage.JS;
        }
        if (StringUtils.isBlank(script)) {
            throw new RuntimeException(scriptLang.name() + " script is blank!");
        }
        switch (scriptLang) {
            case JS:
                return createJsScriptEngine(script, argNames);
            case TBEL:
                if (Arrays.isNullOrEmpty(argNames)) {
                    return createTbelScriptEngine(script, "msg", "metadata", "msgType");
                } else {
                    return createTbelScriptEngine(script, argNames);
                }
            default:
                throw new RuntimeException("Unsupported script language: " + scriptLang.name());
        }
    }

    /**
     * 功能：执行 `logJsEvalRequest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void logJsEvalRequest() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeStats().incrementRequests();
        }
    }

    /**
     * 功能：执行 `logJsEvalResponse` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void logJsEvalResponse() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeStats().incrementResponses();
        }
    }

    /**
     * 功能：执行 `logJsEvalFailure` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void logJsEvalFailure() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeStats().incrementFailures();
        }
    }

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getServiceId() {
        return mainCtx.getServiceInfoProvider().getServiceId();
    }

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public AttributesService getAttributesService() {
        return mainCtx.getAttributesService();
    }

    /**
     * 功能：获取客户。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CustomerService getCustomerService() {
        return mainCtx.getCustomerService();
    }

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TenantService getTenantService() {
        return mainCtx.getTenantService();
    }

    /**
     * 功能：获取用户。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public UserService getUserService() {
        return mainCtx.getUserService();
    }

    /**
     * 功能：获取资产。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetService getAssetService() {
        return mainCtx.getAssetService();
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DeviceService getDeviceService() {
        return mainCtx.getDeviceService();
    }

    /**
     * 功能：获取设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfileService getDeviceProfileService() {
        return mainCtx.getDeviceProfileService();
    }

    /**
     * 功能：获取资产配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfileService getAssetProfileService() {
        return mainCtx.getAssetProfileService();
    }

    /**
     * 功能：获取设备凭据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DeviceCredentialsService getDeviceCredentialsService() {
        return mainCtx.getDeviceCredentialsService();
    }

    /**
     * 功能：获取设备状态管理器。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleEngineDeviceStateManager getDeviceStateManager() {
        return mainCtx.getDeviceStateManager();
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getDeviceStateNodeRateLimitConfig() {
        return mainCtx.getDeviceStateNodeRateLimitConfig();
    }

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbClusterService getClusterService() {
        return mainCtx.getClusterService();
    }

    /**
     * 功能：获取仪表盘。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DashboardService getDashboardService() {
        return mainCtx.getDashboardService();
    }

    /**
     * 功能：获取告警。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleEngineAlarmService getAlarmService() {
        return mainCtx.getAlarmService();
    }

    /**
     * 功能：获取告警。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public AlarmCommentService getAlarmCommentService() {
        return mainCtx.getAlarmCommentService();
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleChainService getRuleChainService() {
        return mainCtx.getRuleChainService();
    }

    /**
     * 功能：获取时序数据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TimeseriesService getTimeseriesService() {
        return mainCtx.getTsService();
    }

    /**
     * 功能：获取遥测。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleEngineTelemetryService getTelemetryService() {
        return mainCtx.getTsSubService();
    }

    /**
     * 功能：获取关系。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RelationService getRelationService() {
        return mainCtx.getRelationService();
    }

    /**
     * 功能：获取实体视图。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityViewService getEntityViewService() {
        return mainCtx.getEntityViewService();
    }

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public ResourceService getResourceService() {
        return mainCtx.getResourceService();
    }

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public OtaPackageService getOtaPackageService() {
        return mainCtx.getOtaPackageService();
    }

    /**
     * 功能：获取设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleEngineDeviceProfileCache getDeviceProfileCache() {
        return mainCtx.getDeviceProfileCache();
    }

    /**
     * 功能：获取资产配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public RuleEngineAssetProfileCache getAssetProfileCache() {
        return mainCtx.getAssetProfileCache();
    }

    /**
     * 功能：获取边缘节点。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EdgeService getEdgeService() {
        return mainCtx.getEdgeService();
    }

    /**
     * 功能：获取边缘节点。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EdgeEventService getEdgeEventService() {
        return mainCtx.getEdgeEventService();
    }

    /**
     * 功能：获取队列。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public QueueService getQueueService() {
        return mainCtx.getQueueService();
    }

    /**
     * 功能：获取事件循环。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EventLoopGroup getSharedEventLoop() {
        return mainCtx.getSharedEventLoopGroupService().getSharedEventLoopGroup();
    }

    /**
     * 功能：获取服务。
     * 参数：
     * - `isSystem`：`isSystem` 参数。
     * 返回：处理结果。
     */
    @Override
    public MailService getMailService(boolean isSystem) {
        if (!isSystem || mainCtx.isAllowSystemMailService()) {
            return mainCtx.getMailService();
        } else {
            throw new RuntimeException("Access to System Mail Service is forbidden!");
        }
    }

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public SmsService getSmsService() {
        if (mainCtx.isAllowSystemSmsService()) {
            return mainCtx.getSmsService();
        } else {
            throw new RuntimeException("Access to System SMS Service is forbidden!");
        }
    }

    /**
     * 功能：获取工厂。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public SmsSenderFactory getSmsSenderFactory() {
        return mainCtx.getSmsSenderFactory();
    }

    /**
     * 功能：获取通知。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationCenter getNotificationCenter() {
        return mainCtx.getNotificationCenter();
    }

    /**
     * 功能：获取通知服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationTargetService getNotificationTargetService() {
        return mainCtx.getNotificationTargetService();
    }

    /**
     * 功能：获取通知服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationTemplateService getNotificationTemplateService() {
        return mainCtx.getNotificationTemplateService();
    }

    /**
     * 功能：获取通知服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRequestService getNotificationRequestService() {
        return mainCtx.getNotificationRequestService();
    }

    /**
     * 功能：获取通知服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRuleService getNotificationRuleService() {
        return mainCtx.getNotificationRuleService();
    }

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public SlackService getSlackService() {
        return mainCtx.getSlackService();
    }

    /**
     * 功能：判断节点实例。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isExternalNodeForceAck() {
        return mainCtx.isExternalNodeForceAck();
    }

    /**
     * 功能：获取RPC。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleEngineRpcService getRpcService() {
        return mainCtx.getTbRuleEngineDeviceRpcService();
    }

    /**
     * 功能：获取Cassandra 集群。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CassandraCluster getCassandraCluster() {
        return mainCtx.getCassandraCluster();
    }

    /**
     * 功能：发送或提交`Cassandra Read Task`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public TbResultSetFuture submitCassandraReadTask(CassandraStatementTask task) {
        return mainCtx.getCassandraBufferedRateReadExecutor().submit(task);
    }

    /**
     * 功能：发送或提交`Cassandra Write Task`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public TbResultSetFuture submitCassandraWriteTask(CassandraStatementTask task) {
        return mainCtx.getCassandraBufferedRateWriteExecutor().submit(task);
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleNodeState> findRuleNodeStates(PageLink pageLink) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}] Fetch Rule Node States.", getTenantId(), getSelfId());
        }
        return mainCtx.getRuleNodeStateService().findByRuleNodeId(getTenantId(), getSelfId(), pageLink);
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    @Override
    public RuleNodeState findRuleNodeStateForEntity(EntityId entityId) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Fetch Rule Node State for entity.", getTenantId(), getSelfId(), entityId);
        }
        return mainCtx.getRuleNodeStateService().findByRuleNodeIdAndEntityId(getTenantId(), getSelfId(), entityId);
    }

    /**
     * 功能：保存或创建规则节点。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleNodeState saveRuleNodeState(RuleNodeState state) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Persist Rule Node State for entity: {}", getTenantId(), getSelfId(), state.getEntityId(), state.getStateData());
        }
        state.setRuleNodeId(getSelfId());
        return mainCtx.getRuleNodeStateService().save(getTenantId(), state);
    }

    /**
     * 功能：删除或清理规则节点。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void clearRuleNodeStates() {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}] Going to clear rule node states", getTenantId(), getSelfId());
        }
        mainCtx.getRuleNodeStateService().removeByRuleNodeId(getTenantId(), getSelfId());
    }

    /**
     * 功能：删除或清理规则节点。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Override
    public void removeRuleNodeStateForEntity(EntityId entityId) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Remove Rule Node State for entity.", getTenantId(), getSelfId(), entityId);
        }
        mainCtx.getRuleNodeStateService().removeByRuleNodeIdAndEntityId(getTenantId(), getSelfId(), entityId);
    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `listener`：`listener` 参数。
     * 返回：无。
     */
    @Override
    public void addTenantProfileListener(Consumer<TenantProfile> listener) {
        mainCtx.getTenantProfileCache().addListener(getTenantId(), getSelfId(), listener);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `profileListener`：`profileListener` 参数。
     * - `deviceListener`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void addDeviceProfileListeners(Consumer<DeviceProfile> profileListener, BiConsumer<DeviceId, DeviceProfile> deviceListener) {
        mainCtx.getDeviceProfileCache().addListener(getTenantId(), getSelfId(), profileListener, deviceListener);
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `profileListener`：`profileListener` 参数。
     * - `assetListener`：`assetListener` 参数。
     * 返回：无。
     */
    @Override
    public void addAssetProfileListeners(Consumer<AssetProfile> profileListener, BiConsumer<AssetId, AssetProfile> assetListener) {
        mainCtx.getAssetProfileCache().addListener(getTenantId(), getSelfId(), profileListener, assetListener);
    }

    /**
     * 功能：删除或清理`Listeners`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void removeListeners() {
        mainCtx.getDeviceProfileCache().removeListener(getTenantId(), getSelfId());
        mainCtx.getAssetProfileCache().removeListener(getTenantId(), getSelfId());
        mainCtx.getTenantProfileCache().removeListener(getTenantId(), getSelfId());
    }

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TenantProfile getTenantProfile() {
        return mainCtx.getTenantProfileCache().get(getTenantId());
    }

    /**
     * 功能：获取部件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public WidgetsBundleService getWidgetBundleService() {
        return mainCtx.getWidgetsBundleService();
    }

    /**
     * 功能：获取部件类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public WidgetTypeService getWidgetTypeService() {
        return mainCtx.getWidgetTypeService();
    }

    /**
     * 功能：获取规则引擎。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleEngineApiUsageStateService getRuleEngineApiUsageStateService() {
        return mainCtx.getApiUsageStateService();
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityService getEntityService() {
        return mainCtx.getEntityService();
    }

    /**
     * 功能：获取事件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EventService getEventService() {
        return mainCtx.getEventService();
    }

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public AuditLogService getAuditLogService() {
        return mainCtx.getAuditLogService();
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * 返回：处理结果。
     */
    private TbMsgMetaData getActionMetaData(RuleNodeId ruleNodeId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ruleNodeId", ruleNodeId.toString());
        return metaData;
    }


    /**
     * 功能：执行 `schedule` 对应的处理。
     * 参数：
     * - `runnable`：`runnable` 参数。
     * - `delay`：`delay` 参数。
     * - `timeUnit`：`timeUnit` 参数。
     * 返回：无。
     */
    @Override
    public void schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        mainCtx.getScheduler().schedule(runnable, delay, timeUnit);
    }

    /**
     * 功能：校验租户。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Override
    public void checkTenantEntity(EntityId entityId) throws TbNodeException {
        if (!this.getTenantId().equals(TenantIdLoader.findTenantId(this, entityId))) {
            throw new TbNodeException("Entity with id: '" + entityId + "' specified in the configuration doesn't belong to the current tenant.", true);
        }
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `th`：`th` 参数。
     * 返回：文本结果。
     */
    private static String getFailureMessage(Throwable th) {
        String failureMessage;
        if (th != null) {
            if (!StringUtils.isEmpty(th.getMessage())) {
                failureMessage = th.getMessage();
            } else {
                failureMessage = th.getClass().getSimpleName();
            }
        } else {
            failureMessage = null;
        }
        return failureMessage;
    }

}
