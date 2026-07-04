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
package org.thingsboard.rule.engine.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.ENTERED;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.INSIDE;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.LEFT;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.OUTSIDE;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "gps geofencing events",
        version = 1,
        configClazz = TbGpsGeofencingActionNodeConfiguration.class,
        relationTypes = {"Success", "Entered", "Left", "Inside", "Outside"},
        nodeDescription = "Produces incoming messages using GPS based geofencing",
        nodeDetails = "Extracts latitude and longitude parameters from incoming message and returns different events based on configuration parameters. " +
                "<br><br>" +
                "If an object with coordinates extracted from incoming message enters the geofence, sends a message with the type <code>Entered</code>. " +
                "If an object leaves the geofence, sends a message with the type <code>Left</code>. " +
                "If the presence monitoring strategy <b>\"On first message\"</b> is selected, sends messages via rule node connection type <code>Inside</code> or <code>Outside</code> only the first time the geofencing and duration conditions are satisfied; otherwise sends messages via rule node connection type <code>Success</code>. " +
                "If the presence monitoring strategy <b>\"On each message\"</b> is selected, sends messages via rule node connection type <code>Inside</code> or <code>Outside</code> every time the geofencing condition is satisfied. " +
                "<br><br>" +
                "Output connections: <code>Entered</code>, <code>Left</code>, <code>Inside</code>, <code>Outside</code>, <code>Success</code>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeGpsGeofencingConfig"
)
/**
 * 中文说明：`TbGpsGeofencingActionNode` 是GPS地理围栏Action节点规则节点，用于执行 GPS 地理围栏、距离和多边形判断及状态跟踪。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbGpsGeofencingActionNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbGpsGeofencingActionNode extends AbstractGeofencingNode<TbGpsGeofencingActionNodeConfiguration> {

    /**
     * 常量字段：定义 `REPORT_PRESENCE_STATUS_ON_EACH_MESSAGE`，用于与本类处理流程相关的运行时值，本身不触发外部系统调用。
     */
    private static final String REPORT_PRESENCE_STATUS_ON_EACH_MESSAGE = "reportPresenceStatusOnEachMessage";
    private final Map<EntityId, EntityGeofencingState> entityStates = new HashMap<>();
    /**
     * 字段说明：保存 `gson`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final Gson gson = new Gson();
    /**
     * 字段说明：保存 `parser`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final JsonParser parser = new JsonParser();

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AttributesService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        boolean matches = checkMatches(msg);
        long ts = System.currentTimeMillis();

        EntityGeofencingState entityState = entityStates.computeIfAbsent(msg.getOriginator(), key -> {
            try {
                Optional<AttributeKvEntry> entry = ctx.getAttributesService()
                        .find(ctx.getTenantId(), msg.getOriginator(), DataConstants.SERVER_SCOPE, ctx.getServiceId())
                        .get(1, TimeUnit.MINUTES);
                if (entry.isPresent()) {
                    JsonObject element = parser.parse(entry.get().getValueAsString()).getAsJsonObject();
                    return new EntityGeofencingState(element.get("inside").getAsBoolean(), element.get("stateSwitchTime").getAsLong(), element.get("stayed").getAsBoolean());
                } else {
                    return new EntityGeofencingState(false, 0L, false);
                }
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        if (entityState.getStateSwitchTime() == 0L || entityState.isInside() != matches) {
            switchState(ctx, msg.getOriginator(), entityState, matches, ts);
            ctx.tellNext(msg, matches ? ENTERED : LEFT);
            return;
        }

        if (config.isReportPresenceStatusOnEachMessage()) {
            ctx.tellNext(msg, entityState.isInside() ? INSIDE : OUTSIDE);
            return;
        }

        if (entityState.isStayed()) {
            ctx.tellSuccess(msg);
            return;
        }

        long stayTime = ts - entityState.getStateSwitchTime();
        if (stayTime > (entityState.isInside() ?
                TimeUnit.valueOf(config.getMinInsideDurationTimeUnit()).toMillis(config.getMinInsideDuration()) :
                TimeUnit.valueOf(config.getMinOutsideDurationTimeUnit()).toMillis(config.getMinOutsideDuration()))) {
            setStaid(ctx, msg.getOriginator(), entityState);
            ctx.tellNext(msg, entityState.isInside() ? INSIDE : OUTSIDE);
            return;
        }

        ctx.tellSuccess(msg);
    }

    /**
     * 方法说明：执行 `switchState` 对应的辅助逻辑，供 `TbGpsGeofencingActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void switchState(TbContext ctx, EntityId entityId, EntityGeofencingState entityState, boolean matches, long ts) {
        entityState.setInside(matches);
        entityState.setStateSwitchTime(ts);
        entityState.setStayed(false);
        persist(ctx, entityId, entityState);
    }

    /**
     * 方法说明：写入本地对象字段或构造输出数据，供 `TbGpsGeofencingActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void setStaid(TbContext ctx, EntityId entityId, EntityGeofencingState entityState) {
        entityState.setStayed(true);
        persist(ctx, entityId, entityState);
    }

    /**
     * 方法说明：执行 `persist` 对应的辅助逻辑，供 `TbGpsGeofencingActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AttributesService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void persist(TbContext ctx, EntityId entityId, EntityGeofencingState entityState) {
        JsonObject object = new JsonObject();
        object.addProperty("inside", entityState.isInside());
        object.addProperty("stateSwitchTime", entityState.getStateSwitchTime());
        object.addProperty("stayed", entityState.isStayed());
        AttributeKvEntry entry = new BaseAttributeKvEntry(new StringDataEntry(ctx.getServiceId(), gson.toJson(object)), System.currentTimeMillis());
        List<AttributeKvEntry> attributeKvEntryList = Collections.singletonList(entry);
        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
        ctx.getAttributesService().save(ctx.getTenantId(), entityId, DataConstants.SERVER_SCOPE, attributeKvEntryList);
    }

    @Override
    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbGpsGeofencingActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected Class<TbGpsGeofencingActionNodeConfiguration> getConfigClazz() {
        return TbGpsGeofencingActionNodeConfiguration.class;
    }

    @Override
    /**
     * 方法说明：迁移旧版本规则节点 JSON 配置结构。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        if (fromVersion == 0) {
            if (!oldConfiguration.has(REPORT_PRESENCE_STATUS_ON_EACH_MESSAGE)) {
                hasChanges = true;
                ((ObjectNode) oldConfiguration).put(REPORT_PRESENCE_STATUS_ON_EACH_MESSAGE, false);
            }
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

    /*
     * 本类总结：`TbGpsGeofencingActionNode` 负责执行 GPS 地理围栏、距离和多边形判断及状态跟踪；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
