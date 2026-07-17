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
 * 中文说明：
 * 1. `TbGpsGeofencingActionNode` 是 ThingsBoard Rule Engine Components 中处理 `Tb Gps Geofencing` 的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `AbstractGeofencingNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
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
}
