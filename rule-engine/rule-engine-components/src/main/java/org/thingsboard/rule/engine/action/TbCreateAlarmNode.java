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
/**
 * 中文说明：`TbCreateAlarmNode` 是创建告警节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbCreateAlarmNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbCreateAlarmNode extends TbAbstractAlarmNode<TbCreateAlarmNodeConfiguration> {

    /**
     * 字段说明：保存 `relationTypes`，表示关系方向、类型或关系查询条件，供本类方法在规则节点处理流程中使用。
     */
    private List<String> relationTypes;
    /**
     * 字段说明：保存 `notDynamicAlarmSeverity`，表示告警类型、严重级别或详情，供本类方法在规则节点处理流程中使用。
     */
    private AlarmSeverity notDynamicAlarmSeverity;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx, configuration);
        if (!this.config.isDynamicSeverity()) {
            this.notDynamicAlarmSeverity = EnumUtils.getEnum(AlarmSeverity.class, this.config.getSeverity());
            if (this.notDynamicAlarmSeverity == null) {
                throw new TbNodeException("Incorrect Alarm Severity value: " + this.config.getSeverity(), true);
            }
        }
    }


    @Override
    /**
     * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `TbCreateAlarmNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected TbCreateAlarmNodeConfiguration loadAlarmNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        TbCreateAlarmNodeConfiguration nodeConfiguration = TbNodeUtils.convert(configuration, TbCreateAlarmNodeConfiguration.class);
        relationTypes = nodeConfiguration.getRelationTypes();
        return nodeConfiguration;
    }

    @Override
    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateAlarmNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AlarmService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
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

        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
        Alarm existingAlarm = ctx.getAlarmService().findLatestActiveByOriginatorAndType(ctx.getTenantId(), msg.getOriginator(), alarmType);
        if (existingAlarm == null || existingAlarm.getStatus().isCleared()) {
            return createNewAlarm(ctx, msg, msgAlarm);
        } else {
            return updateAlarm(ctx, msg, existingAlarm, msgAlarm);
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbCreateAlarmNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：创建实体、告警、关系或辅助对象，供 `TbCreateAlarmNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AlarmService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
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
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
        ListenableFuture<AlarmApiCallResult> asyncCreated = Futures.transform(asyncAlarm,
                alarm -> ctx.getAlarmService().createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(alarm)), ctx.getDbCallbackExecutor());
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
        return Futures.transform(asyncCreated, TbAlarmResult::fromAlarmResult, MoreExecutors.directExecutor());
    }

    /**
     * 方法说明：执行 `updateAlarm` 对应的辅助逻辑，供 `TbCreateAlarmNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AlarmService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
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
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
        return Futures.transform(asyncUpdated, TbAlarmResult::fromAlarmResult, MoreExecutors.directExecutor());
    }

    /**
     * 方法说明：构造告警详情、SSL 上下文或输出对象，供 `TbCreateAlarmNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：执行本类核心处理流程，供 `TbCreateAlarmNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
