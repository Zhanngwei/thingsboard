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
package org.thingsboard.rule.engine.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.profile.state.PersistedAlarmRuleState;
import org.thingsboard.rule.engine.profile.state.PersistedAlarmState;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpecType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

@Data
@Slf4j
/**
 * 中文说明：`AlarmState` 是告警状态辅助类，用于维护设备配置、告警规则、快照和设备运行状态。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
class AlarmState {

    /**
     * 常量字段：定义 `ERROR_MSG`，用于当前规则链消息，本身不触发外部系统调用。
     */
    public static final String ERROR_MSG = "Failed to process alarm rule for Device [%s]: %s";
    /**
     * 字段说明：保存 `deviceProfile`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final ProfileState deviceProfile;
    /**
     * 字段说明：保存 `originator`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final EntityId originator;
    /**
     * 字段说明：保存 `alarmDefinition`，表示告警类型、严重级别或详情，供本类方法在规则节点处理流程中使用。
     */
    private DeviceProfileAlarm alarmDefinition;
    /**
     * 字段说明：保存 `createRulesSortedBySeverityDesc`，表示告警严重级别，供本类方法在规则节点处理流程中使用。
     */
    private volatile List<AlarmRuleState> createRulesSortedBySeverityDesc;
    /**
     * 字段说明：保存 `clearState`，表示运行状态，供本类方法在规则节点处理流程中使用。
     */
    private volatile AlarmRuleState clearState;
    /**
     * 字段说明：保存 `currentAlarm`，表示告警类型、严重级别或详情，供本类方法在规则节点处理流程中使用。
     */
    private volatile Alarm currentAlarm;
    /**
     * 字段说明：保存 `initialFetchDone`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private volatile boolean initialFetchDone;
    /**
     * 字段说明：保存 `lastMsgMetaData`，表示消息元数据，供本类方法在规则节点处理流程中使用。
     */
    private volatile TbMsgMetaData lastMsgMetaData;
    /**
     * 字段说明：保存 `lastMsgQueueName` 本地缓存、队列或并发状态，用于协调本类处理流程。
     */
    private volatile String lastMsgQueueName;
    /**
     * 字段说明：保存 `dataSnapshot`，表示消息体数据，供本类方法在规则节点处理流程中使用。
     */
    private volatile DataSnapshot dataSnapshot;
    /**
     * 字段说明：保存 `dynamicPredicateValueCtx`，表示规则引擎上下文，供本类方法在规则节点处理流程中使用。
     */
    private final DynamicPredicateValueCtx dynamicPredicateValueCtx;

    /**
     * 方法说明：构造 `AlarmState` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    AlarmState(ProfileState deviceProfile, EntityId originator, DeviceProfileAlarm alarmDefinition, PersistedAlarmState alarmState, DynamicPredicateValueCtx dynamicPredicateValueCtx) {
        this.deviceProfile = deviceProfile;
        this.originator = originator;
        this.dynamicPredicateValueCtx = dynamicPredicateValueCtx;
        this.updateState(alarmDefinition, alarmState);
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public boolean process(TbContext ctx, TbMsg msg, DataSnapshot data, SnapshotUpdate update) throws ExecutionException, InterruptedException {
        initCurrentAlarm(ctx);
        lastMsgMetaData = msg.getMetaData();
        lastMsgQueueName = msg.getQueueName();
        this.dataSnapshot = data;
        try {
            return createOrClearAlarms(ctx, msg, data, update, AlarmRuleState::eval);
        } catch (NumericParseException e) {
            throw new RuntimeException(String.format(ERROR_MSG, originator.getId().toString(), e.getMessage()));
        }
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public boolean process(TbContext ctx, long ts) throws ExecutionException, InterruptedException {
        initCurrentAlarm(ctx);
        try {
            return createOrClearAlarms(ctx, null, ts, null, (alarmState, tsParam) -> alarmState.eval(tsParam, dataSnapshot));
        } catch (NumericParseException e) {
            throw new RuntimeException(String.format(ERROR_MSG, originator.getId().toString(), e.getMessage()));
        }
    }

    /**
     * 方法说明：创建实体、告警、关系或辅助对象，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AlarmService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public <T> boolean createOrClearAlarms(TbContext ctx, TbMsg msg, T data, SnapshotUpdate update, BiFunction<AlarmRuleState, T, AlarmEvalResult> evalFunction) {
        boolean stateUpdate = false;
        AlarmRuleState resultState = null;
        log.debug("[{}] processing update: {}", alarmDefinition.getId(), data);
        for (AlarmRuleState state : createRulesSortedBySeverityDesc) {
            if (!validateUpdate(update, state)) {
                log.debug("[{}][{}] Update is not valid for current rule state", alarmDefinition.getId(), state.getSeverity());
                continue;
            }
            AlarmEvalResult evalResult = evalFunction.apply(state, data);
            stateUpdate |= state.checkUpdate();
            if (AlarmEvalResult.TRUE.equals(evalResult)) {
                resultState = state;
                break;
            } else if (AlarmEvalResult.FALSE.equals(evalResult)) {
                stateUpdate = clearAlarmState(stateUpdate, state);
            }
        }
        if (resultState != null) {
            TbAlarmResult result = calculateAlarmResult(ctx, resultState);
            if (result != null) {
                pushMsg(ctx, msg, result, resultState);
            }
            stateUpdate = clearAlarmState(stateUpdate, clearState);
        } else if (currentAlarm != null && clearState != null) {
            if (!validateUpdate(update, clearState)) {
                log.debug("[{}] Update is not valid for current clear state", alarmDefinition.getId());
                return stateUpdate;
            }
            AlarmEvalResult evalResult = evalFunction.apply(clearState, data);
            if (AlarmEvalResult.TRUE.equals(evalResult)) {
                stateUpdate = clearAlarmState(stateUpdate, clearState);
                for (AlarmRuleState state : createRulesSortedBySeverityDesc) {
                    stateUpdate = clearAlarmState(stateUpdate, state);
                }
                AlarmApiCallResult result = ctx.getAlarmService().clearAlarm(
                        ctx.getTenantId(), currentAlarm.getId(), System.currentTimeMillis(), createDetails(clearState)
                );
                if (result.isCleared()) {
                    pushMsg(ctx, msg, new TbAlarmResult(false, false, true, result.getAlarm()), clearState);
                }
                currentAlarm = null;
            } else if (AlarmEvalResult.FALSE.equals(evalResult)) {
                stateUpdate = clearAlarmState(stateUpdate, clearState);
            }
        }
        return stateUpdate;
    }

    /**
     * 方法说明：清除告警或本地状态，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public boolean clearAlarmState(boolean stateUpdate, AlarmRuleState state) {
        if (state != null) {
            state.clear();
            stateUpdate |= state.checkUpdate();
        }
        return stateUpdate;
    }

    /**
     * 方法说明：校验配置或数据是否满足节点要求，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public boolean validateUpdate(SnapshotUpdate update, AlarmRuleState state) {
        if (update != null) {
            //Check that the update type and that keys match.
            if (update.getType().equals(AlarmConditionKeyType.TIME_SERIES)) {
                return state.validateTsUpdate(update.getKeys());
            } else if (update.getType().equals(AlarmConditionKeyType.ATTRIBUTE)) {
                return state.validateAttrUpdate(update.getKeys());
            }
        }
        return true;
    }

    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AlarmService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void initCurrentAlarm(TbContext ctx) {
        if (!initialFetchDone) {
            // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
            Alarm alarm = ctx.getAlarmService().findLatestActiveByOriginatorAndType(ctx.getTenantId(), originator, alarmDefinition.getAlarmType());
            if (alarm != null && !alarm.getStatus().isCleared()) {
                currentAlarm = alarm;
            }
            initialFetchDone = true;
        }
    }

    /**
     * 方法说明：执行 `pushMsg` 对应的辅助逻辑，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void pushMsg(TbContext ctx, TbMsg msg, TbAlarmResult alarmResult, AlarmRuleState ruleState) {
        JsonNode jsonNodes = JacksonUtil.valueToTree(alarmResult.getAlarm());
        String data = jsonNodes.toString();
        TbMsgMetaData metaData = lastMsgMetaData != null ? lastMsgMetaData.copy() : new TbMsgMetaData();
        String relationType;
        if (alarmResult.isCreated()) {
            relationType = "Alarm Created";
            metaData.putValue(DataConstants.IS_NEW_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isUpdated()) {
            relationType = "Alarm Updated";
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isSeverityUpdated()) {
            relationType = "Alarm Severity Updated";
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
            metaData.putValue(DataConstants.IS_SEVERITY_UPDATED_ALARM, Boolean.TRUE.toString());
        } else {
            relationType = "Alarm Cleared";
            metaData.putValue(DataConstants.IS_CLEARED_ALARM, Boolean.TRUE.toString());
        }
        setAlarmConditionMetadata(ruleState, metaData);
        TbMsg newMsg = ctx.newMsg(lastMsgQueueName != null ? lastMsgQueueName : null, TbMsgType.ALARM,
                originator, msg != null ? msg.getCustomerId() : null, metaData, data);
        // 通过规则引擎上下文安排后续消息投递或自身定时消息。
        ctx.enqueueForTellNext(newMsg, relationType);
    }

    /**
     * 方法说明：写入本地对象字段或构造输出数据，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected void setAlarmConditionMetadata(AlarmRuleState ruleState, TbMsgMetaData metaData) {
        if (ruleState.getSpec().getType() == AlarmConditionSpecType.REPEATING) {
            metaData.putValue(DataConstants.ALARM_CONDITION_REPEATS, String.valueOf(ruleState.getState().getEventCount()));
        }
        if (ruleState.getSpec().getType() == AlarmConditionSpecType.DURATION) {
            metaData.putValue(DataConstants.ALARM_CONDITION_DURATION, String.valueOf(ruleState.getState().getDuration()));
        }
    }

    /**
     * 方法说明：执行 `updateState` 对应的辅助逻辑，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void updateState(DeviceProfileAlarm alarm, PersistedAlarmState alarmState) {
        this.alarmDefinition = alarm;
        this.createRulesSortedBySeverityDesc = new ArrayList<>();
        alarmDefinition.getCreateRules().forEach((severity, rule) -> {
            PersistedAlarmRuleState ruleState = null;
            if (alarmState != null) {
                ruleState = alarmState.getCreateRuleStates().get(severity);
                if (ruleState == null) {
                    ruleState = new PersistedAlarmRuleState();
                    alarmState.getCreateRuleStates().put(severity, ruleState);
                }
            }
            createRulesSortedBySeverityDesc.add(new AlarmRuleState(severity, rule,
                    deviceProfile.getCreateAlarmKeys(alarm.getId(), severity), ruleState, dynamicPredicateValueCtx));
        });
        createRulesSortedBySeverityDesc.sort(Comparator.comparingInt(state -> state.getSeverity().ordinal()));
        PersistedAlarmRuleState ruleState = alarmState == null ? null : alarmState.getClearRuleState();
        if (alarmDefinition.getClearRule() != null) {
            clearState = new AlarmRuleState(null, alarmDefinition.getClearRule(), deviceProfile.getClearAlarmKeys(alarm.getId()), ruleState, dynamicPredicateValueCtx);
        }
    }

    /**
     * 方法说明：执行数学或业务结果计算，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AlarmService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private TbAlarmResult calculateAlarmResult(TbContext ctx, AlarmRuleState ruleState) {
        AlarmSeverity severity = ruleState.getSeverity();
        if (currentAlarm != null) {
            // TODO: In some extremely rare cases, we might miss the event of alarm clear (If one use in-mem queue and restarted the server) or (if one manipulated the rule chain).
            // Maybe we should fetch alarm every time?
            currentAlarm.setEndTs(System.currentTimeMillis());
            AlarmSeverity oldSeverity = currentAlarm.getSeverity();
            // Skip update if severity is decreased.
            if (severity.ordinal() <= oldSeverity.ordinal()) {
                currentAlarm.setDetails(createDetails(ruleState));
                currentAlarm.setSeverity(severity);
                AlarmApiCallResult result = ctx.getAlarmService().updateAlarm(AlarmUpdateRequest.fromAlarm(currentAlarm));
                currentAlarm = result.getAlarm();
                return TbAlarmResult.fromAlarmResult(result);
            } else {
                return null;
            }
        } else {
            currentAlarm = new Alarm();
            currentAlarm.setType(alarmDefinition.getAlarmType());
            currentAlarm.setAcknowledged(false);
            currentAlarm.setCleared(false);
            currentAlarm.setSeverity(severity);
            long startTs = dataSnapshot.getTs();
            if (startTs == 0L) {
                startTs = System.currentTimeMillis();
            }
            currentAlarm.setStartTs(startTs);
            currentAlarm.setEndTs(currentAlarm.getStartTs());
            currentAlarm.setDetails(createDetails(ruleState));
            currentAlarm.setOriginator(originator);
            currentAlarm.setTenantId(ctx.getTenantId());
            currentAlarm.setPropagate(alarmDefinition.isPropagate());
            currentAlarm.setPropagateToOwner(alarmDefinition.isPropagateToOwner());
            currentAlarm.setPropagateToTenant(alarmDefinition.isPropagateToTenant());
            if (alarmDefinition.getPropagateRelationTypes() != null) {
                currentAlarm.setPropagateRelationTypes(alarmDefinition.getPropagateRelationTypes());
            }
            AlarmApiCallResult result = ctx.getAlarmService().createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(currentAlarm));
            currentAlarm = result.getAlarm();
            return TbAlarmResult.fromAlarmResult(result);
        }
    }

    /**
     * 方法说明：创建实体、告警、关系或辅助对象，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private JsonNode createDetails(AlarmRuleState ruleState) {
        JsonNode alarmDetails;
        String alarmDetailsStr = ruleState.getAlarmRule().getAlarmDetails();
        DashboardId dashboardId = ruleState.getAlarmRule().getDashboardId();

        if (StringUtils.isNotEmpty(alarmDetailsStr) || dashboardId != null) {
            ObjectNode newDetails = JacksonUtil.newObjectNode();
            if (StringUtils.isNotEmpty(alarmDetailsStr)) {
                for (var keyFilter : ruleState.getAlarmRule().getCondition().getCondition()) {
                    EntityKeyValue entityKeyValue = dataSnapshot.getValue(keyFilter.getKey());
                    if (entityKeyValue != null) {
                        alarmDetailsStr = alarmDetailsStr.replaceAll(String.format("\\$\\{%s}", keyFilter.getKey().getKey()), getValueAsString(entityKeyValue));
                    }
                }
                newDetails.put("data", alarmDetailsStr);
            }
            if (dashboardId != null) {
                newDetails.put("dashboardId", dashboardId.getId().toString());
            }
            alarmDetails = newDetails;
        } else if (currentAlarm != null) {
            alarmDetails = currentAlarm.getDetails();
        } else {
            alarmDetails = JacksonUtil.newObjectNode();
        }

        return alarmDetails;
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static String getValueAsString(EntityKeyValue entityKeyValue) {
        Object result = null;
        switch (entityKeyValue.getDataType()) {
            case STRING:
                result = entityKeyValue.getStrValue();
                break;
            case JSON:
                result = entityKeyValue.getJsonValue();
                break;
            case LONG:
                result = entityKeyValue.getLngValue();
                break;
            case DOUBLE:
                result = entityKeyValue.getDblValue();
                break;
            case BOOLEAN:
                result = entityKeyValue.getBoolValue();
                break;
        }
        return String.valueOf(result);
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public boolean processAlarmClear(TbContext ctx, Alarm alarmNf) {
        boolean updated = false;
        if (currentAlarm != null && currentAlarm.getId().equals(alarmNf.getId())) {
            currentAlarm = null;
            for (AlarmRuleState state : createRulesSortedBySeverityDesc) {
                updated = clearAlarmState(updated, state);
            }
        }
        return updated;
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `AlarmState` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void processAckAlarm(Alarm alarm) {
        if (currentAlarm != null && currentAlarm.getId().equals(alarm.getId())) {
            currentAlarm.setAcknowledged(alarm.isAcknowledged());
            currentAlarm.setAckTs(alarm.getAckTs());
        }
    }
    /*
     * 本类总结：`AlarmState` 负责维护设备配置、告警规则、快照和设备运行状态；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
