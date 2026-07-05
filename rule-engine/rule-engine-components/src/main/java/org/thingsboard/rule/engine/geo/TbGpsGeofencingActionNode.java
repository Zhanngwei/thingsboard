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
/**
 * 中文说明：`TbGpsGeofencingActionNode` 是GPS地理围栏Action节点规则节点，用于执行 GPS 地理围栏、距离和多边形判断及状态跟踪。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbGpsGeofencingActionNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
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
public class TbGpsGeofencingActionNode extends AbstractGeofencingNode<TbGpsGeofencingActionNodeConfiguration> {

    /**
     * 消息常量，用于统一引用固定值。
     */
    private static final String REPORT_PRESENCE_STATUS_ON_EACH_MESSAGE = "reportPresenceStatusOnEachMessage";
    private final Map<EntityId, EntityGeofencingState> entityStates = new HashMap<>();
    /**
     * `gson` 字段，保存当前对象的对应属性。
     */
    private final Gson gson = new Gson();
    /**
     * 解析器，封装可复用的处理规则。
     */
    private final JsonParser parser = new JsonParser();

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
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
     * 功能：执行 `switchState` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityId`：实体IDID。
     * - `entityState`：实体对象。
     * - `matches`：`matches` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void switchState(TbContext ctx, EntityId entityId, EntityGeofencingState entityState, boolean matches, long ts) {
        entityState.setInside(matches);
        entityState.setStateSwitchTime(ts);
        entityState.setStayed(false);
        persist(ctx, entityId, entityState);
    }

    /**
     * 功能：更新`Staid`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityId`：实体IDID。
     * - `entityState`：实体对象。
     * 返回：无。
     */
    private void setStaid(TbContext ctx, EntityId entityId, EntityGeofencingState entityState) {
        entityState.setStayed(true);
        persist(ctx, entityId, entityState);
    }

    /**
     * 功能：执行 `persist` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityId`：实体IDID。
     * - `entityState`：实体对象。
     * 返回：无。
     */
    private void persist(TbContext ctx, EntityId entityId, EntityGeofencingState entityState) {
        JsonObject object = new JsonObject();
        object.addProperty("inside", entityState.isInside());
        object.addProperty("stateSwitchTime", entityState.getStateSwitchTime());
        object.addProperty("stayed", entityState.isStayed());
        AttributeKvEntry entry = new BaseAttributeKvEntry(new StringDataEntry(ctx.getServiceId(), gson.toJson(object)), System.currentTimeMillis());
        List<AttributeKvEntry> attributeKvEntryList = Collections.singletonList(entry);
        ctx.getAttributesService().save(ctx.getTenantId(), entityId, DataConstants.SERVER_SCOPE, attributeKvEntryList);
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<TbGpsGeofencingActionNodeConfiguration> getConfigClazz() {
        return TbGpsGeofencingActionNodeConfiguration.class;
    }

    /**
     * 功能：执行 `upgrade` 对应的处理。
     * 参数：
     * - `fromVersion`：`fromVersion` 参数。
     * - `oldConfiguration`：配置对象。
     * 返回：处理结果。
     */
    @Override
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
