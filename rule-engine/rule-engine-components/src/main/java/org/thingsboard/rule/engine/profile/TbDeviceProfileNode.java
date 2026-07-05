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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：`TbDeviceProfileNode` 是设备配置节点规则节点，用于维护设备配置、告警规则、快照和设备运行状态。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbDeviceProfileNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "device profile",
        customRelations = true,
        relationTypes = {"Alarm Created", "Alarm Updated", "Alarm Severity Updated", "Alarm Cleared", "Success", "Failure"},
        configClazz = TbDeviceProfileNodeConfiguration.class,
        nodeDescription = "Process device messages based on device profile settings",
        nodeDetails = "Create and clear alarms based on alarm rules defined in device profile. The output relation type is either " +
                "'Alarm Created', 'Alarm Updated', 'Alarm Severity Updated' and 'Alarm Cleared' or simply 'Success' if no alarms were affected.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbDeviceProfileConfig"
)
public class TbDeviceProfileNode implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbDeviceProfileNodeConfiguration config;
    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    private RuleEngineDeviceProfileCache cache;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctx;
    private final Map<DeviceId, DeviceState> deviceStates = new ConcurrentHashMap<>();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDeviceProfileNodeConfiguration.class);
        this.cache = ctx.getDeviceProfileCache();
        this.ctx = ctx;
        scheduleAlarmHarvesting(ctx, null);
        ctx.addDeviceProfileListeners(this::onProfileUpdate, this::onDeviceUpdate);
        initAlarmRuleState(false);
    }

    /**
     * 功能：初始化或启动告警。
     * 参数：
     * - `printNewlyAddedDeviceStates`：设备信息或设备标识。
     * 返回：无。
     */
    private void initAlarmRuleState(boolean printNewlyAddedDeviceStates) {
        if (config.isFetchAlarmRulesStateOnStart()) {
            log.info("[{}] Fetching alarm rule state", ctx.getSelfId());
            int fetchCount = 0;
            PageLink pageLink = new PageLink(1024);
            while (true) {
                PageData<RuleNodeState> states = ctx.findRuleNodeStates(pageLink);
                if (!states.getData().isEmpty()) {
                    for (RuleNodeState rns : states.getData()) {
                        fetchCount++;
                        if (rns.getEntityId().getEntityType().equals(EntityType.DEVICE) && ctx.isLocalEntity(rns.getEntityId())) {
                            getOrCreateDeviceState(ctx, new DeviceId(rns.getEntityId().getId()), rns, printNewlyAddedDeviceStates);
                        }
                    }
                }
                if (!states.hasNext()) {
                    break;
                } else {
                    pageLink = pageLink.nextPageLink();
                }
            }
            log.info("[{}] Fetched alarm rule state for {} entities", ctx.getSelfId(), fetchCount);
        }
        if (!config.isPersistAlarmRulesState() && ctx.isLocalEntity(ctx.getSelfId())) {
            log.debug("[{}] Going to cleanup rule node states", ctx.getSelfId());
            ctx.clearRuleNodeStates();
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        EntityType originatorType = msg.getOriginator().getEntityType();
        if (msg.isTypeOf(TbMsgType.DEVICE_PROFILE_PERIODIC_SELF_MSG)) {
            scheduleAlarmHarvesting(ctx, msg);
            harvestAlarms(ctx, System.currentTimeMillis());
        } else if (msg.isTypeOf(TbMsgType.DEVICE_PROFILE_UPDATE_SELF_MSG)) {
            updateProfile(ctx, new DeviceProfileId(UUID.fromString(msg.getData())));
        } else if (msg.isTypeOf(TbMsgType.DEVICE_UPDATE_SELF_MSG)) {
            JsonNode data = JacksonUtil.toJsonNode(msg.getData());
            DeviceId deviceId = new DeviceId(UUID.fromString(data.get("deviceId").asText()));
            if (data.has("profileId")) {
                invalidateDeviceProfileCache(deviceId, new DeviceProfileId(UUID.fromString(data.get("deviceProfileId").asText())));
            } else {
                removeDeviceState(deviceId);
            }
        } else {
            if (EntityType.DEVICE.equals(originatorType)) {
                DeviceId deviceId = new DeviceId(msg.getOriginator().getId());
                if (msg.isTypeOf(TbMsgType.ENTITY_UPDATED)) {
                    invalidateDeviceProfileCache(deviceId, msg.getData());
                    ctx.tellSuccess(msg);
                } else if (msg.isTypeOf(TbMsgType.ENTITY_DELETED)) {
                    removeDeviceState(deviceId);
                    ctx.tellSuccess(msg);
                } else {
                    DeviceState deviceState = getOrCreateDeviceState(ctx, deviceId, null, false);
                    if (deviceState != null) {
                        deviceState.process(ctx, msg);
                    } else {
                        log.info("Device was not found! Most probably device [" + deviceId + "] has been removed from the database. Acknowledging msg.");
                        ctx.ack(msg);
                    }
                }
            } else {
                ctx.tellSuccess(msg);
            }
        }
    }

    /**
     * 功能：处理分区。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        // Cleanup the cache for all entities that are no longer assigned to current server partitions
        deviceStates.entrySet().removeIf(entry -> !ctx.isLocalEntity(entry.getKey()));
        initAlarmRuleState(true);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        ctx.removeListeners();
        deviceStates.clear();
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceId`：设备IDID。
     * - `rns`：`rns` 参数。
     * - `printNewlyAddedDeviceStates`：设备信息或设备标识。
     * 返回：处理结果。
     */
    protected DeviceState getOrCreateDeviceState(TbContext ctx, DeviceId deviceId, RuleNodeState rns, boolean printNewlyAddedDeviceStates) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState == null) {
            DeviceProfile deviceProfile = cache.get(ctx.getTenantId(), deviceId);
            if (deviceProfile != null) {
                deviceState = new DeviceState(ctx, config, deviceId, new ProfileState(deviceProfile), rns);
                deviceStates.put(deviceId, deviceState);
                if (printNewlyAddedDeviceStates) {
                    log.info("[{}][{}] Device [{}] was added during PartitionChangeMsg", ctx.getTenantId(), ctx.getSelfId(), deviceId);
                }
            }
        }
        return deviceState;
    }

    /**
     * 功能：执行 `scheduleAlarmHarvesting` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    protected void scheduleAlarmHarvesting(TbContext ctx, TbMsg msg) {
        TbMsg periodicCheck = TbMsg.newMsg(TbMsgType.DEVICE_PROFILE_PERIODIC_SELF_MSG, ctx.getTenantId(), msg != null ? msg.getCustomerId() : null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        ctx.tellSelf(periodicCheck, TimeUnit.MINUTES.toMillis(1));
    }

    /**
     * 功能：执行 `harvestAlarms` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `ts`：时间戳。
     * 返回：无。
     */
    protected void harvestAlarms(TbContext ctx, long ts) throws ExecutionException, InterruptedException {
        for (DeviceState state : deviceStates.values()) {
            state.harvestAlarms(ctx, ts);
        }
    }

    /**
     * 功能：更新配置。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceProfileId`：设备配置ID。
     * 返回：无。
     */
    protected void updateProfile(TbContext ctx, DeviceProfileId deviceProfileId) throws ExecutionException, InterruptedException {
        DeviceProfile deviceProfile = cache.get(ctx.getTenantId(), deviceProfileId);
        if (deviceProfile != null) {
            log.debug("[{}] Received device profile update notification: {}", ctx.getSelfId(), deviceProfile);
            for (DeviceState state : deviceStates.values()) {
                if (deviceProfile.getId().equals(state.getProfileId())) {
                    state.updateProfile(ctx, deviceProfile);
                }
            }
        } else {
            log.debug("[{}] Received stale profile update notification: [{}]", ctx.getSelfId(), deviceProfileId);
        }
    }

    /**
     * 功能：处理配置。
     * 参数：
     * - `profile`：`profile` 参数。
     * 返回：无。
     */
    protected void onProfileUpdate(DeviceProfile profile) {
        ctx.tellSelf(TbMsg.newMsg(TbMsgType.DEVICE_PROFILE_UPDATE_SELF_MSG, ctx.getTenantId(), TbMsgMetaData.EMPTY, profile.getId().getId().toString()), 0L);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    private void onDeviceUpdate(DeviceId deviceId, DeviceProfile deviceProfile) {
        ObjectNode msgData = JacksonUtil.newObjectNode();
        msgData.put("deviceId", deviceId.getId().toString());
        if (deviceProfile != null) {
            msgData.put("deviceProfileId", deviceProfile.getId().getId().toString());
        }
        ctx.tellSelf(TbMsg.newMsg(TbMsgType.DEVICE_UPDATE_SELF_MSG, ctx.getTenantId(), TbMsgMetaData.EMPTY, JacksonUtil.toString(msgData)), 0L);
    }

    /**
     * 功能：执行 `invalidateDeviceProfileCache` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `deviceJson`：设备信息或设备标识。
     * 返回：无。
     */
    protected void invalidateDeviceProfileCache(DeviceId deviceId, String deviceJson) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState != null) {
            DeviceProfileId currentProfileId = deviceState.getProfileId();
            try {
                Device device = JacksonUtil.fromString(deviceJson, Device.class);
                if (!currentProfileId.equals(device.getDeviceProfileId())) {
                    removeDeviceState(deviceId);
                }
            } catch (IllegalArgumentException e) {
                log.debug("[{}] Received device update notification with non-device msg body: [{}]", ctx.getSelfId(), deviceId, e);
            }
        }
    }

    /**
     * 功能：执行 `invalidateDeviceProfileCache` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：无。
     */
    protected void invalidateDeviceProfileCache(DeviceId deviceId, DeviceProfileId deviceProfileId) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState != null) {
            if (!deviceState.getProfileId().equals(deviceProfileId)) {
                removeDeviceState(deviceId);
            }
        }
    }

    /**
     * 功能：删除或清理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    private void removeDeviceState(DeviceId deviceId) {
        DeviceState state = deviceStates.remove(deviceId);
        if (config.isPersistAlarmRulesState() && (state != null || !config.isFetchAlarmRulesStateOnStart())) {
            ctx.removeRuleNodeStateForEntity(deviceId);
        }
    }
    /*
     * 本类总结：`TbDeviceProfileNode` 负责维护设备配置、告警规则、快照和设备运行状态；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
