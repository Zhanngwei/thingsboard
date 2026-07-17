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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;
import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;

/**
 * 中文说明：
 * 1. `TbMsgAttributesNode` 是 ThingsBoard Rule Engine Components 中处理消息的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "save attributes",
        configClazz = TbMsgAttributesNodeConfiguration.class,
        version = 2,
        nodeDescription = "Saves attributes data",
        nodeDetails = "Saves entity attributes based on configurable scope parameter. Expects messages with 'POST_ATTRIBUTES_REQUEST' message type. " +
                      "If upsert(update/insert) operation is completed successfully rule node will send the incoming message via <b>Success</b> chain, otherwise, <b>Failure</b> chain is used. " +
                      "Additionally if checkbox <b>Send attributes updated notification</b> is set to true, rule node will put the \"Attributes Updated\" " +
                      "event for <b>SHARED_SCOPE</b> and <b>SERVER_SCOPE</b> attributes updates to the corresponding rule engine queue." +
                      "Performance checkbox 'Save attributes only if the value changes' will skip attributes overwrites for values with no changes (avoid concurrent writes because this check is not transactional; will not update 'Last updated time' for skipped attributes).",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAttributesConfig",
        icon = "file_upload"
)
public class TbMsgAttributesNode implements TbNode {

    /**
     * 设备常量，用于统一引用固定值。
     */
    static final String NOTIFY_DEVICE_KEY = "notifyDevice";
    /**
     * 键常量，用于统一引用固定值。
     */
    static final String SEND_ATTRIBUTES_UPDATED_NOTIFICATION_KEY = "sendAttributesUpdatedNotification";
    /**
     * 键常量，用于统一引用固定值。
     */
    static final String UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE_KEY = "updateAttributesOnlyOnValueChange";

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbMsgAttributesNodeConfiguration config;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgAttributesNodeConfiguration.class);
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!msg.isTypeOf(POST_ATTRIBUTES_REQUEST)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
            return;
        }
        String src = msg.getData();
        List<AttributeKvEntry> newAttributes = new ArrayList<>(JsonConverter.convertToAttributes(JsonParser.parseString(src)));
        if (newAttributes.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }
        String scope = getScope(msg.getMetaData().getValue(SCOPE));
        boolean sendAttributesUpdateNotification = checkSendNotification(scope);

        if (!config.isUpdateAttributesOnlyOnValueChange()) {
            saveAttr(newAttributes, ctx, msg, scope, sendAttributesUpdateNotification);
            return;
        }

        List<String> keys = newAttributes.stream().map(KvEntry::getKey).collect(Collectors.toList());
        ListenableFuture<List<AttributeKvEntry>> findFuture = ctx.getAttributesService().find(ctx.getTenantId(), msg.getOriginator(), scope, keys);

        DonAsynchron.withCallback(findFuture,
                currentAttributes -> {
                    List<AttributeKvEntry> attributesChanged = filterChangedAttr(currentAttributes, newAttributes);
                    saveAttr(attributesChanged, ctx, msg, scope, sendAttributesUpdateNotification);
                },
                throwable -> ctx.tellFailure(msg, throwable),
                MoreExecutors.directExecutor());
    }

    /**
     * 功能：保存或创建`Attr`。
     * 参数：
     * - `attributes`：数据列表。
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `scope`：`scope` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveAttr(List<AttributeKvEntry> attributes, TbContext ctx, TbMsg msg, String scope, boolean sendAttributesUpdateNotification) {
        if (attributes.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }
        ctx.getTelemetryService().saveAndNotify(
                ctx.getTenantId(),
                msg.getOriginator(),
                scope,
                attributes,
                config.isNotifyDevice() || checkNotifyDeviceMdValue(msg.getMetaData().getValue(NOTIFY_DEVICE_METADATA_KEY)),
                sendAttributesUpdateNotification ?
                        new AttributesUpdateNodeCallback(ctx, msg, scope, attributes) :
                        new TelemetryNodeCallback(ctx, msg)
        );
    }

    /**
     * 功能：执行 `filterChangedAttr` 对应的处理。
     * 参数：
     * - `currentAttributes`：数据列表。
     * - `newAttributes`：数据列表。
     * 返回：匹配的数据集合。
     */
    List<AttributeKvEntry> filterChangedAttr(List<AttributeKvEntry> currentAttributes, List<AttributeKvEntry> newAttributes) {
        if (currentAttributes == null || currentAttributes.isEmpty()) {
            return newAttributes;
        }

        Map<String, AttributeKvEntry> currentAttrMap = currentAttributes.stream()
                .collect(Collectors.toMap(AttributeKvEntry::getKey, Function.identity(), (existing, replacement) -> existing));

        return newAttributes.stream()
                .filter(item -> {
                    AttributeKvEntry cacheAttr = currentAttrMap.get(item.getKey());
                    return cacheAttr == null
                            || !Objects.equals(item.getValue(), cacheAttr.getValue()) //JSON and String can be equals by value, but different by type
                            || !Objects.equals(item.getDataType(), cacheAttr.getDataType());
                })
                .collect(Collectors.toList());
    }

    /**
     * 功能：校验通知。
     * 参数：
     * - `scope`：`scope` 参数。
     * 返回：判断结果。
     */
    private boolean checkSendNotification(String scope) {
        return config.isSendAttributesUpdatedNotification() && !CLIENT_SCOPE.equals(scope);
    }

    /**
     * 功能：校验设备。
     * 参数：
     * - `notifyDeviceMdValue`：设备信息或设备标识。
     * 返回：判断结果。
     */
    private boolean checkNotifyDeviceMdValue(String notifyDeviceMdValue) {
        // Check for empty string for backward-compatibility. A while ago node always notified devices.
        return StringUtils.isEmpty(notifyDeviceMdValue) || Boolean.parseBoolean(notifyDeviceMdValue);
    }

    /**
     * 功能：获取`Scope`。
     * 参数：
     * - `mdScopeValue`：值。
     * 返回：文本结果。
     */
    private String getScope(String mdScopeValue) {
        if (StringUtils.isNotEmpty(mdScopeValue)) {
            return mdScopeValue;
        }
        return config.getScope();
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
        switch (fromVersion) {
            case 0:
                if (!oldConfiguration.has(UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE_KEY)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put(UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE_KEY, false);
                }
            case 1:
                // update notifyDevice. set true if null or property doesn't exist for backward-compatibility.
                hasChanges = fixEscapedBooleanConfigParameter(oldConfiguration, NOTIFY_DEVICE_KEY, hasChanges, true);
                // update sendAttributesUpdatedNotification.
                hasChanges = fixEscapedBooleanConfigParameter(oldConfiguration, SEND_ATTRIBUTES_UPDATED_NOTIFICATION_KEY, hasChanges, false);
                // update updateAttributesOnlyOnValueChange.
                hasChanges = fixEscapedBooleanConfigParameter(oldConfiguration, UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE_KEY, hasChanges, true);
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

    /**
     * 功能：执行 `fixEscapedBooleanConfigParameter` 对应的处理。
     * 参数：
     * - `oldConfiguration`：配置对象。
     * - `boolKey`：键。
     * - `hasChanges`：`hasChanges` 参数。
     * - `valueIfNull`：值。
     * 返回：判断结果。
     */
    private boolean fixEscapedBooleanConfigParameter(JsonNode oldConfiguration, String boolKey, boolean hasChanges, boolean valueIfNull) {
        if (oldConfiguration.hasNonNull(boolKey)) {
            var value = oldConfiguration.get(boolKey);
            if (value.isTextual()) {
                hasChanges = true;
                ((ObjectNode) oldConfiguration)
                        .put(boolKey, value.asBoolean(valueIfNull));
            }
        } else {
            hasChanges = true;
            ((ObjectNode) oldConfiguration)
                    .put(boolKey, valueIfNull);
        }
        return hasChanges;
    }
}
