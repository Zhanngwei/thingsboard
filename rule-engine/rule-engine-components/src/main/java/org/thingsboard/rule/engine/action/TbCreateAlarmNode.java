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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.List;

/**
 * 中文说明：`TbCreateAlarmNode` 是创建告警节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbCreateAlarmNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create alarm", relationTypes = {"Created", "Updated", "False"},
        configClazz = TbCreateAlarmNodeConfiguration.class,
        nodeDescription = "Create or Update Alarm",
        nodeDetails =
                "Details - JS function that creates JSON object based on incoming message. This object will be added into Alarm.details field.\n" +
                        "Node output:\n" +
                        "If alarm was not created, original message is returned. Otherwise new Message returned with type 'ALARM', Alarm object in 'msg' property and 'metadata' will contains one of those properties 'isNewAlarm/isExistingAlarm'. " +
                        "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                        "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateAlarmConfig",
        icon = "notifications_active"
)
public class TbCreateAlarmNode extends TbAbstractAlarmNode<TbCreateAlarmNodeConfiguration> {

    /**
     * 关系列表，用于保存一组待处理对象。
     */
    private List<String> relationTypes;
    /**
     * 告警对象，用于描述当前业务场景。
     */
    private AlarmSeverity notDynamicAlarmSeverity;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx, configuration);
        if (!this.config.isDynamicSeverity()) {
            this.notDynamicAlarmSeverity = EnumUtils.getEnum(AlarmSeverity.class, this.config.getSeverity());
            if (this.notDynamicAlarmSeverity == null) {
                throw new TbNodeException("Incorrect Alarm Severity value: " + this.config.getSeverity(), true);
            }
        }
    }


    /**
     * 功能：获取告警。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    protected TbCreateAlarmNodeConfiguration loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        TbCreateAlarmNodeConfiguration nodeConfiguration = TbNodeUtils.convert(configuration, TbCreateAlarmNodeConfiguration.class);
        relationTypes = nodeConfiguration.getRelationTypes();
        return nodeConfiguration;
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<TbAlarmResult> processAlarm(TbContext ctx, TbMsg msg) {
        String alarmType;
        final Alarm msgAlarm;

        if (!config.isUseMessageAlarmData()) {
            alarmType = TbNodeUtils.processPattern(this.config.getAlarmType(), msg);
            msgAlarm = null;
        } else {
            try {
                msgAlarm = getAlarmFromMessage(ctx, msg);
                alarmType = msgAlarm.getType();
            } catch (IOException e) {
                ctx.tellFailure(msg, e);
                return null;
            }
        }

        Alarm existingAlarm = ctx.getAlarmService().findLatestActiveByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), alarmType);
        if (existingAlarm == null || existingAlarm.getStatus().isCleared()) {
            return createNewAlarm(ctx, msg, msgAlarm);
        } else {
            return updateAlarm(ctx, msg, existingAlarm, msgAlarm);
        }
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private Alarm getAlarmFromMessage(TbContext ctx, TbMsg msg) throws IOException {
        Alarm msgAlarm;
        msgAlarm = JacksonUtil.fromString(msg.getData(), Alarm.class);
        msgAlarm.setTenantId(ctx.getTenantId());
        if (msgAlarm.getOriginator() == null) {
            msgAlarm.setOriginator(msg.getOriginator());
        }
        return msgAlarm;
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `msgAlarm`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TbAlarmResult> createNewAlarm(TbContext ctx, TbMsg msg, Alarm msgAlarm) {
        ListenableFuture<JsonNode> asyncDetails;
        boolean buildDetails = !config.isUseMessageAlarmData() || config.isOverwriteAlarmDetails();
        if (buildDetails) {
            ctx.logJsEvalRequest();
            asyncDetails = buildAlarmDetails(msg, null);
        } else {
            asyncDetails = Futures.immediateFuture(null);
        }
        ListenableFuture<Alarm> asyncAlarm = Futures.transform(asyncDetails, details -> {
            if (buildDetails) {
                ctx.logJsEvalResponse();
            }
            Alarm newAlarm;
            if (msgAlarm != null) {
                newAlarm = msgAlarm;
                if (buildDetails) {
                    newAlarm.setDetails(details);
                }
            } else {
                newAlarm = buildAlarm(msg, details, ctx.getTenantId());
            }
            return newAlarm;
        }, MoreExecutors.directExecutor());
        ListenableFuture<AlarmApiCallResult> asyncCreated = Futures.transform(asyncAlarm,
                alarm -> ctx.getAlarmService().createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(alarm)), ctx.getDbCallbackExecutor());
        return Futures.transform(asyncCreated, TbAlarmResult::fromAlarmResult, MoreExecutors.directExecutor());
    }

    /**
     * 功能：更新告警。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `existingAlarm`：`existingAlarm` 参数。
     * - `msgAlarm`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TbAlarmResult> updateAlarm(TbContext ctx, TbMsg msg, Alarm existingAlarm, Alarm msgAlarm) {
        ListenableFuture<JsonNode> asyncDetails;
        boolean buildDetails = !config.isUseMessageAlarmData() || config.isOverwriteAlarmDetails();
        if (buildDetails) {
            ctx.logJsEvalRequest();
            asyncDetails = buildAlarmDetails(msg, existingAlarm.getDetails());
        } else {
            asyncDetails = Futures.immediateFuture(null);
        }
        ListenableFuture<AlarmApiCallResult> asyncUpdated = Futures.transform(asyncDetails, details -> {
            if (buildDetails) {
                ctx.logJsEvalResponse();
            }
            if (msgAlarm != null) {
                existingAlarm.setSeverity(msgAlarm.getSeverity());
                existingAlarm.setPropagate(msgAlarm.isPropagate());
                existingAlarm.setPropagateToOwner(msgAlarm.isPropagateToOwner());
                existingAlarm.setPropagateToTenant(msgAlarm.isPropagateToTenant());
                existingAlarm.setPropagateRelationTypes(msgAlarm.getPropagateRelationTypes());
                if (buildDetails) {
                    existingAlarm.setDetails(details);
                } else {
                    existingAlarm.setDetails(msgAlarm.getDetails());
                }
            } else {
                existingAlarm.setSeverity(processAlarmSeverity(msg));
                existingAlarm.setPropagate(config.isPropagate());
                existingAlarm.setPropagateToOwner(config.isPropagateToOwner());
                existingAlarm.setPropagateToTenant(config.isPropagateToTenant());
                existingAlarm.setPropagateRelationTypes(relationTypes);
                existingAlarm.setDetails(details);
            }
            existingAlarm.setEndTs(System.currentTimeMillis());
            return ctx.getAlarmService().updateAlarm(AlarmUpdateRequest.fromAlarm(existingAlarm));
        }, ctx.getDbCallbackExecutor());
        return Futures.transform(asyncUpdated, TbAlarmResult::fromAlarmResult, MoreExecutors.directExecutor());
    }

    /**
     * 功能：构建告警。
     * 参数：
     * - `msg`：待处理消息。
     * - `details`：`details` 参数。
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    private Alarm buildAlarm(TbMsg msg, JsonNode details, TenantId tenantId) {
        long ts = msg.getMetaDataTs();
        return Alarm.builder()
                .tenantId(tenantId)
                .originator(msg.getOriginator())
                .cleared(false)
                .acknowledged(false)
                .severity(this.config.isDynamicSeverity() ? processAlarmSeverity(msg) : notDynamicAlarmSeverity)
                .propagate(config.isPropagate())
                .propagateToOwner(config.isPropagateToOwner())
                .propagateToTenant(config.isPropagateToTenant())
                .type(TbNodeUtils.processPattern(this.config.getAlarmType(), msg))
                .propagateRelationTypes(relationTypes)
                .startTs(ts)
                .endTs(ts)
                .details(details)
                .build();
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private AlarmSeverity processAlarmSeverity(TbMsg msg) {
        AlarmSeverity severity = EnumUtils.getEnum(AlarmSeverity.class, TbNodeUtils.processPattern(this.config.getSeverity(), msg));
        if (severity == null) {
            throw new RuntimeException("Used incorrect pattern or Alarm Severity not included in message");
        }
        return severity;
    }

    /*
     * 本类总结：`TbCreateAlarmNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
