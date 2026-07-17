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

import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.profile.state.PersistedAlarmState;
import org.thingsboard.rule.engine.profile.state.PersistedDeviceState;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.exception.ApiUsageLimitsExceededException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.msg.TbMsgType.ACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM_ACK;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM_CLEAR;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM_DELETE;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_DELETED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_UPDATED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_ASSIGNED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_UNASSIGNED;
import static org.thingsboard.server.common.data.msg.TbMsgType.INACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;

/**
 * 中文说明：
 * 1. `DeviceState` 是 ThingsBoard Rule Engine Components 中承载设备信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
class DeviceState {

    /**
     * 是否满足状态条件。
     */
    private final boolean persistState;
    /**
     * 设备ID，用于定位对应业务对象。
     */
    private final DeviceId deviceId;
    /**
     * 设备配置，保存当前对象的配置选项。
     */
    private final ProfileState deviceProfile;
    /**
     * 状态，表示当前对象所处状态。
     */
    private RuleNodeState state;
    /**
     * `pds` 字段，保存当前对象的对应属性。
     */
    private PersistedDeviceState pds;
    /**
     * `latestValues`，保存当前处理得到的具体内容。
     */
    private DataSnapshot latestValues;
    private final ConcurrentMap<String, AlarmState> alarmStates = new ConcurrentHashMap<>();
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private final DynamicPredicateValueCtx dynamicPredicateValueCtx;

    /**
     * 功能：创建 `DeviceState` 实例，并初始化必要字段。
     * 参数：
     * - `ctx`：处理上下文。
     * - `config`：配置对象。
     * - `deviceId`：设备IDID。
     * - `deviceProfile`：设备信息或设备标识。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    DeviceState(TbContext ctx, TbDeviceProfileNodeConfiguration config, DeviceId deviceId, ProfileState deviceProfile, RuleNodeState state) {
        this.persistState = config.isPersistAlarmRulesState();
        this.deviceId = deviceId;
        this.deviceProfile = deviceProfile;

        this.dynamicPredicateValueCtx = new DynamicPredicateValueCtxImpl(ctx.getTenantId(), deviceId, ctx);

        if (config.isPersistAlarmRulesState()) {
            if (state != null) {
                this.state = state;
            } else {
                this.state = ctx.findRuleNodeStateForEntity(deviceId);
            }
            if (this.state != null) {
                pds = JacksonUtil.fromString(this.state.getStateData(), PersistedDeviceState.class);
            } else {
                this.state = new RuleNodeState();
                this.state.setRuleNodeId(ctx.getSelfId());
                this.state.setEntityId(deviceId);
                pds = new PersistedDeviceState();
                pds.setAlarmStates(new HashMap<>());
            }
        }
        if (pds != null) {
            for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            }
        }
    }

    /**
     * 功能：更新配置。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    public void updateProfile(TbContext ctx, DeviceProfile deviceProfile) throws ExecutionException, InterruptedException {
        Set<AlarmConditionFilterKey> oldKeys = Set.copyOf(this.deviceProfile.getEntityKeys());
        this.deviceProfile.updateDeviceProfile(deviceProfile);
        if (latestValues != null) {
            Set<AlarmConditionFilterKey> keysToFetch = new HashSet<>(this.deviceProfile.getEntityKeys());
            keysToFetch.removeAll(oldKeys);
            if (!keysToFetch.isEmpty()) {
                addEntityKeysToSnapshot(ctx, deviceId, keysToFetch, latestValues);
            }
        }
        Set<String> newAlarmStateIds = this.deviceProfile.getAlarmSettings().stream().map(DeviceProfileAlarm::getId).collect(Collectors.toSet());
        alarmStates.keySet().removeIf(id -> !newAlarmStateIds.contains(id));
        for (DeviceProfileAlarm alarm : this.deviceProfile.getAlarmSettings()) {
            if (alarmStates.containsKey(alarm.getId())) {
                alarmStates.get(alarm.getId()).updateState(alarm, getOrInitPersistedAlarmState(alarm));
            } else {
                alarmStates.putIfAbsent(alarm.getId(), new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            }
        }
    }

    /**
     * 功能：执行 `harvestAlarms` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `ts`：时间戳。
     * 返回：无。
     */
    public void harvestAlarms(TbContext ctx, long ts) throws ExecutionException, InterruptedException {
        log.debug("[{}] Going to harvest alarms: {}", ctx.getSelfId(), ts);
        boolean stateChanged = false;
        for (AlarmState state : alarmStates.values()) {
            stateChanged |= state.process(ctx, ts);
        }
        if (persistState && stateChanged) {
            state.setStateData(JacksonUtil.toString(pds));
            state = ctx.saveRuleNodeState(state);
        }
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void process(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        if (latestValues == null) {
            latestValues = fetchLatestValues(ctx, deviceId);
        }
        boolean stateChanged = false;
        if (msg.isTypeOf(POST_TELEMETRY_REQUEST)) {
            stateChanged = processTelemetry(ctx, msg);
        } else if (msg.isTypeOf(POST_ATTRIBUTES_REQUEST)) {
            stateChanged = processAttributesUpdateRequest(ctx, msg);
        } else if (msg.isTypeOneOf(ACTIVITY_EVENT, INACTIVITY_EVENT)) {
            stateChanged = processDeviceActivityEvent(ctx, msg);
        } else if (msg.isTypeOf(ATTRIBUTES_UPDATED)) {
            stateChanged = processAttributesUpdateNotification(ctx, msg);
        } else if (msg.isTypeOf(ATTRIBUTES_DELETED)) {
            stateChanged = processAttributesDeleteNotification(ctx, msg);
        } else if (msg.isTypeOf(ALARM_CLEAR)) {
            stateChanged = processAlarmClearNotification(ctx, msg);
        } else if (msg.isTypeOf(ALARM_ACK)) {
            processAlarmAckNotification(ctx, msg);
        } else if (msg.isTypeOf(ALARM_DELETE)) {
            processAlarmDeleteNotification(ctx, msg);
        } else {
            if (msg.isTypeOneOf(ENTITY_ASSIGNED, ENTITY_UNASSIGNED)) {
                dynamicPredicateValueCtx.resetCustomer();
            }
            ctx.tellSuccess(msg);
        }
        if (persistState && stateChanged) {
            state.setStateData(JacksonUtil.toString(pds));
            state = ctx.saveRuleNodeState(state);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    private boolean processDeviceActivityEvent(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            return processTelemetry(ctx, msg);
        } else {
            return processAttributes(ctx, msg, scope);
        }
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    private boolean processAlarmClearNotification(TbContext ctx, TbMsg msg) {
        boolean stateChanged = false;
        Alarm alarmNf = JacksonUtil.fromString(msg.getData(), Alarm.class);
        for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
            AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                    a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            stateChanged |= alarmState.processAlarmClear(ctx, alarmNf);
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void processAlarmAckNotification(TbContext ctx, TbMsg msg) {
        Alarm alarmNf = JacksonUtil.fromString(msg.getData(), Alarm.class);
        for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
            AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                    a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            alarmState.processAckAlarm(alarmNf);
        }
        ctx.tellSuccess(msg);
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void processAlarmDeleteNotification(TbContext ctx, TbMsg msg) {
        Alarm alarm = JacksonUtil.fromString(msg.getData(), Alarm.class);
        alarmStates.values().removeIf(alarmState -> alarmState.getCurrentAlarm() != null
                && alarmState.getCurrentAlarm().getId().equals(alarm.getId()));
        ctx.tellSuccess(msg);
    }

    /**
     * 功能：处理通知。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    private boolean processAttributesUpdateNotification(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = DataConstants.CLIENT_SCOPE;
        }
        return processAttributes(ctx, msg, scope);
    }

    /**
     * 功能：处理通知。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    private boolean processAttributesDeleteNotification(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        List<String> keys = new ArrayList<>();
        JsonParser.parseString(msg.getData()).getAsJsonObject().get("attributes").getAsJsonArray().forEach(e -> keys.add(e.getAsString()));
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = DataConstants.CLIENT_SCOPE;
        }
        if (!keys.isEmpty()) {
            EntityKeyType keyType = getKeyTypeFromScope(scope);
            Set<AlarmConditionFilterKey> removedKeys = keys.stream().map(key -> new EntityKey(keyType, key))
                    .peek(latestValues::removeValue)
                    .map(DataSnapshot::toConditionKey).collect(Collectors.toSet());
            SnapshotUpdate update = new SnapshotUpdate(AlarmConditionKeyType.ATTRIBUTE, removedKeys);

            for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                stateChanged |= alarmState.process(ctx, msg, latestValues, update);
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    protected boolean processAttributesUpdateRequest(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        return processAttributes(ctx, msg, DataConstants.CLIENT_SCOPE);
    }

    /**
     * 功能：处理`Attributes`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `scope`：`scope` 参数。
     * 返回：判断结果。
     */
    private boolean processAttributes(TbContext ctx, TbMsg msg, String scope) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(JsonParser.parseString(msg.getData()));
        if (!attributes.isEmpty()) {
            SnapshotUpdate update = merge(latestValues, attributes, scope);
            for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                stateChanged |= alarmState.process(ctx, msg, latestValues, update);
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    /**
     * 功能：处理遥测。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    protected boolean processTelemetry(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToSortedTelemetry(JsonParser.parseString(msg.getData()), msg.getMetaDataTs());
        // iterate over data by ts (ASC order).
        for (Map.Entry<Long, List<KvEntry>> entry : tsKvMap.entrySet()) {
            Long ts = entry.getKey();
            List<KvEntry> data = entry.getValue();
            SnapshotUpdate update = merge(latestValues, ts, data);
            if (update.hasUpdate()) {
                for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                    AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                            a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                    try {
                        stateChanged |= alarmState.process(ctx, msg, latestValues, update);
                    } catch (ApiUsageLimitsExceededException e) {
                        alarmStates.remove(alarm.getId());
                        throw e;
                    }
                }
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    /**
     * 功能：执行 `merge` 对应的处理。
     * 参数：
     * - `latestValues`：值。
     * - `newTs`：时间戳。
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    private SnapshotUpdate merge(DataSnapshot latestValues, Long newTs, List<KvEntry> data) {
        Set<AlarmConditionFilterKey> keys = new HashSet<>();
        for (KvEntry entry : data) {
            AlarmConditionFilterKey entityKey = new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, entry.getKey());
            if (latestValues.putValue(entityKey, newTs, toEntityValue(entry))) {
                keys.add(entityKey);
            }
        }
        latestValues.setTs(newTs);
        return new SnapshotUpdate(AlarmConditionKeyType.TIME_SERIES, keys);
    }

    /**
     * 功能：执行 `merge` 对应的处理。
     * 参数：
     * - `latestValues`：值。
     * - `attributes`：`attributes` 参数。
     * - `scope`：`scope` 参数。
     * 返回：处理结果。
     */
    private SnapshotUpdate merge(DataSnapshot latestValues, Set<AttributeKvEntry> attributes, String scope) {
        long newTs = 0;
        Set<AlarmConditionFilterKey> keys = new HashSet<>();
        for (AttributeKvEntry entry : attributes) {
            newTs = Math.max(newTs, entry.getLastUpdateTs());
            AlarmConditionFilterKey entityKey = new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, entry.getKey());
            if (latestValues.putValue(entityKey, newTs, toEntityValue(entry))) {
                keys.add(entityKey);
            }
        }
        latestValues.setTs(newTs);
        return new SnapshotUpdate(AlarmConditionKeyType.ATTRIBUTE, keys);
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `scope`：`scope` 参数。
     * 返回：处理结果。
     */
    private static EntityKeyType getKeyTypeFromScope(String scope) {
        switch (scope) {
            case DataConstants.CLIENT_SCOPE:
                return EntityKeyType.CLIENT_ATTRIBUTE;
            case DataConstants.SHARED_SCOPE:
                return EntityKeyType.SHARED_ATTRIBUTE;
            case DataConstants.SERVER_SCOPE:
                return EntityKeyType.SERVER_ATTRIBUTE;
        }
        return EntityKeyType.ATTRIBUTE;
    }

    /**
     * 功能：获取`Latest Values`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `originator`：`originator` 参数。
     * 返回：处理结果。
     */
    private DataSnapshot fetchLatestValues(TbContext ctx, EntityId originator) throws ExecutionException, InterruptedException {
        Set<AlarmConditionFilterKey> entityKeysToFetch = deviceProfile.getEntityKeys();
        DataSnapshot result = new DataSnapshot(entityKeysToFetch);
        addEntityKeysToSnapshot(ctx, originator, entityKeysToFetch, result);
        return result;
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `originator`：`originator` 参数。
     * - `entityKeysToFetch`：实体对象。
     * - `result`：`result` 参数。
     * 返回：无。
     */
    private void addEntityKeysToSnapshot(TbContext ctx, EntityId originator, Set<AlarmConditionFilterKey> entityKeysToFetch, DataSnapshot result) throws InterruptedException, ExecutionException {
        Set<String> attributeKeys = new HashSet<>();
        Set<String> latestTsKeys = new HashSet<>();

        Device device = null;
        for (AlarmConditionFilterKey entityKey : entityKeysToFetch) {
            String key = entityKey.getKey();
            switch (entityKey.getType()) {
                case ATTRIBUTE:
                    attributeKeys.add(key);
                    break;
                case TIME_SERIES:
                    latestTsKeys.add(key);
                    break;
                case ENTITY_FIELD:
                    if (device == null) {
                        device = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), new DeviceId(originator.getId()));
                    }
                    if (device != null) {
                        switch (key) {
                            case EntityKeyMapping.NAME:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromString(device.getName()));
                                break;
                            case EntityKeyMapping.TYPE:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromString(device.getType()));
                                break;
                            case EntityKeyMapping.CREATED_TIME:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromLong(device.getCreatedTime()));
                                break;
                            case EntityKeyMapping.LABEL:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromString(device.getLabel()));
                                break;
                        }
                    }
                    break;
            }
        }

        if (!latestTsKeys.isEmpty()) {
            List<TsKvEntry> data = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), originator, latestTsKeys).get();
            for (TsKvEntry entry : data) {
                if (entry.getValue() != null) {
                    result.putValue(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, entry.getKey()), entry.getTs(), toEntityValue(entry));
                }
            }
        }
        if (!attributeKeys.isEmpty()) {
            addToSnapshot(result, ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.CLIENT_SCOPE, attributeKeys).get());
            addToSnapshot(result, ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.SHARED_SCOPE, attributeKeys).get());
            addToSnapshot(result, ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.SERVER_SCOPE, attributeKeys).get());
        }
    }

    /**
     * 功能：保存或创建`To Snapshot`。
     * 参数：
     * - `snapshot`：`snapshot` 参数。
     * - `data`：待处理数据。
     * 返回：无。
     */
    private void addToSnapshot(DataSnapshot snapshot, List<AttributeKvEntry> data) {
        for (AttributeKvEntry entry : data) {
            if (entry.getValue() != null) {
                EntityKeyValue value = toEntityValue(entry);
                snapshot.putValue(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, entry.getKey()), entry.getLastUpdateTs(), value);
            }
        }
    }

    /**
     * 功能：执行 `toEntityValue` 对应的处理。
     * 参数：
     * - `entry`：`entry` 参数。
     * 返回：处理结果。
     */
    public static EntityKeyValue toEntityValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case STRING:
                return EntityKeyValue.fromString(entry.getStrValue().get());
            case LONG:
                return EntityKeyValue.fromLong(entry.getLongValue().get());
            case DOUBLE:
                return EntityKeyValue.fromDouble(entry.getDoubleValue().get());
            case BOOLEAN:
                return EntityKeyValue.fromBool(entry.getBooleanValue().get());
            case JSON:
                return EntityKeyValue.fromJson(entry.getJsonValue().get());
            default:
                throw new RuntimeException("Can't parse entry: " + entry.getDataType());
        }
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public DeviceProfileId getProfileId() {
        return deviceProfile.getProfileId();
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：处理结果。
     */
    private PersistedAlarmState getOrInitPersistedAlarmState(DeviceProfileAlarm alarm) {
        if (pds != null) {
            PersistedAlarmState alarmState = pds.getAlarmStates().get(alarm.getId());
            if (alarmState == null) {
                alarmState = new PersistedAlarmState();
                alarmState.setCreateRuleStates(new HashMap<>());
                pds.getAlarmStates().put(alarm.getId(), alarmState);
            }
            return alarmState;
        } else {
            return null;
        }
    }
}
