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

/**
 * 中文说明：
 * 1. `AlarmState` 是 ThingsBoard Rule Engine Components 中承载告警信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@Slf4j
class AlarmState {

    /**
     * 消息常量，用于统一引用固定值。
     */
    public static final String ERROR_MSG = "Failed to process alarm rule for Device [%s]: %s";
    /**
     * 设备配置，保存当前对象的配置选项。
     */
    private final ProfileState deviceProfile;
    /**
     * `originator` 字段，保存当前对象的对应属性。
     */
    private final EntityId originator;
    /**
     * 告警对象，用于描述当前业务场景。
     */
    private DeviceProfileAlarm alarmDefinition;
    /**
     * `createRulesSortedBySeverityDesc`列表，用于保存一组待处理对象。
     */
    private volatile List<AlarmRuleState> createRulesSortedBySeverityDesc;
    /**
     * 状态，表示当前对象所处状态。
     */
    private volatile AlarmRuleState clearState;
    /**
     * 告警对象，用于描述当前业务场景。
     */
    private volatile Alarm currentAlarm;
    /**
     * 当前处理是否已经完成。
     */
    private volatile boolean initialFetchDone;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private volatile TbMsgMetaData lastMsgMetaData;
    /**
     * 队列名称，用于标识或展示当前对象。
     */
    private volatile String lastMsgQueueName;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private volatile DataSnapshot dataSnapshot;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private final DynamicPredicateValueCtx dynamicPredicateValueCtx;

    /**
     * 功能：创建 `AlarmState` 实例，并初始化必要字段。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `originator`：`originator` 参数。
     * - `alarmDefinition`：`alarmDefinition` 参数。
     * - `alarmState`：`alarmState` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    AlarmState(ProfileState deviceProfile, EntityId originator, DeviceProfileAlarm alarmDefinition, PersistedAlarmState alarmState, DynamicPredicateValueCtx dynamicPredicateValueCtx) {
        this.deviceProfile = deviceProfile;
        this.originator = originator;
        this.dynamicPredicateValueCtx = dynamicPredicateValueCtx;
        this.updateState(alarmDefinition, alarmState);
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `data`：待处理数据。
     * - `update`：`update` 参数。
     * 返回：判断结果。
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
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `ts`：时间戳。
     * 返回：判断结果。
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
     * 功能：保存或创建`Or Clear Alarms`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `data`：待处理数据。
     * - `update`：`update` 参数。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
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
     * 功能：删除或清理告警。
     * 参数：
     * - `stateUpdate`：`stateUpdate` 参数。
     * - `state`：`state` 参数。
     * 返回：判断结果。
     */
    public boolean clearAlarmState(boolean stateUpdate, AlarmRuleState state) {
        if (state != null) {
            state.clear();
            stateUpdate |= state.checkUpdate();
        }
        return stateUpdate;
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `update`：`update` 参数。
     * - `state`：`state` 参数。
     * 返回：判断结果。
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
     * 功能：初始化或启动告警。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    public void initCurrentAlarm(TbContext ctx) {
        if (!initialFetchDone) {
            Alarm alarm = ctx.getAlarmService().findLatestActiveByOriginatorAndType(ctx.getTenantId(), originator, alarmDefinition.getAlarmType());
            if (alarm != null && !alarm.getStatus().isCleared()) {
                currentAlarm = alarm;
            }
            initialFetchDone = true;
        }
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `alarmResult`：`alarmResult` 参数。
     * - `ruleState`：`ruleState` 参数。
     * 返回：无。
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
        ctx.enqueueForTellNext(newMsg, relationType);
    }

    /**
     * 功能：更新告警。
     * 参数：
     * - `ruleState`：`ruleState` 参数。
     * - `metaData`：待处理数据。
     * 返回：无。
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
     * 功能：更新状态。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `alarmState`：`alarmState` 参数。
     * 返回：无。
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
     * 功能：执行 `calculateAlarmResult` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `ruleState`：`ruleState` 参数。
     * 返回：处理结果。
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
     * 功能：保存或创建`Details`。
     * 参数：
     * - `ruleState`：`ruleState` 参数。
     * 返回：处理结果。
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
     * 功能：获取值。
     * 参数：
     * - `entityKeyValue`：实体对象。
     * 返回：文本结果。
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
     * 功能：处理告警。
     * 参数：
     * - `ctx`：处理上下文。
     * - `alarmNf`：`alarmNf` 参数。
     * 返回：判断结果。
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
     * 功能：处理告警。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：无。
     */
    public void processAckAlarm(Alarm alarm) {
        if (currentAlarm != null && currentAlarm.getId().equals(alarm.getId())) {
            currentAlarm.setAcknowledged(alarm.isAcknowledged());
            currentAlarm.setAckTs(alarm.getAckTs());
        }
    }
}
