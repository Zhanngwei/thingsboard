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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

/**
 * 中文说明：
 * 1. `CalculateDeltaNode` 是 ThingsBoard Rule Engine Components 中处理 `Calculate Delta Node` 的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "calculate delta", relationTypes = {TbNodeConnectionType.SUCCESS, TbNodeConnectionType.FAILURE, TbNodeConnectionType.OTHER},
        configClazz = CalculateDeltaNodeConfiguration.class,
        nodeDescription = "Calculates delta and amount of time passed between previous timeseries key reading " +
                "and current value for this key from the incoming message",
        nodeDetails = "Useful for metering use cases, when you need to calculate consumption based on pulse counter reading.<br><br>" +
                "Output connections: <code>Success</code>, <code>Other</code> or <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeCalculateDeltaConfig")
public class CalculateDeltaNode implements TbNode {

    private Map<EntityId, ValueWithTs> cache;
    /**
     * 配置，保存当前对象的配置选项。
     */
    private CalculateDeltaNodeConfiguration config;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctx;
    /**
     * 时序数据，提供当前类调用的业务操作。
     */
    private TimeseriesService timeseriesService;
    /**
     * 是否使用`cache`。
     */
    private boolean useCache;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, CalculateDeltaNodeConfiguration.class);
        this.ctx = ctx;
        this.timeseriesService = ctx.getTimeseriesService();
        this.useCache = config.isUseCache();
        if (useCache) {
            cache = new ConcurrentHashMap<>();
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
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!msg.isTypeOf(TbMsgType.POST_TELEMETRY_REQUEST)) {
            ctx.tellNext(msg, TbNodeConnectionType.OTHER);
            return;
        }
        JsonNode json = JacksonUtil.toJsonNode(msg.getData());
        String inputKey = config.getInputValueKey();
        if (!json.has(inputKey)) {
            ctx.tellNext(msg, TbNodeConnectionType.OTHER);
            return;
        }
        withCallback(getLastValue(msg.getOriginator()),
                previousData -> {
                    double currentValue = json.get(inputKey).asDouble();
                    long currentTs = msg.getMetaDataTs();

                    if (useCache) {
                        cache.put(msg.getOriginator(), new ValueWithTs(currentTs, currentValue));
                    }

                    BigDecimal delta = BigDecimal.valueOf(previousData != null ? currentValue - previousData.value : 0.0);

                    if (config.isTellFailureIfDeltaIsNegative() && delta.doubleValue() < 0) {
                        ctx.tellFailure(msg, new IllegalArgumentException("Delta value is negative!"));
                        return;
                    }

                    if (config.getRound() != null) {
                        delta = delta.setScale(config.getRound(), RoundingMode.HALF_UP);
                    }

                    ObjectNode result = (ObjectNode) json;
                    if (delta.stripTrailingZeros().scale() > 0) {
                        result.put(config.getOutputValueKey(), delta.doubleValue());
                    } else {
                        result.put(config.getOutputValueKey(), delta.longValueExact());
                    }

                    if (config.isAddPeriodBetweenMsgs()) {
                        long period = previousData != null ? currentTs - previousData.ts : 0;
                        result.put(config.getPeriodValueKey(), period);
                    }
                    ctx.tellSuccess(TbMsg.transformMsgData(msg, JacksonUtil.toString(result)));
                },
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (useCache) {
            cache.clear();
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<ValueWithTs> fetchLatestValueAsync(EntityId entityId) {
        return Futures.transform(timeseriesService.findLatest(ctx.getTenantId(), entityId, Collections.singletonList(config.getInputValueKey())),
                list -> extractValue(list.get(0))
                , ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private ValueWithTs fetchLatestValue(EntityId entityId) {
        List<TsKvEntry> tsKvEntries = timeseriesService.findLatestSync(
                ctx.getTenantId(),
                entityId,
                Collections.singletonList(config.getInputValueKey()));
        return extractValue(tsKvEntries.get(0));
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<ValueWithTs> getLastValue(EntityId entityId) {
        if (useCache) {
            ValueWithTs latestValue;
            if ((latestValue = cache.get(entityId)) == null) {
                latestValue = fetchLatestValue(entityId);
            }
            return Futures.immediateFuture(latestValue);
        } else {
            return fetchLatestValueAsync(entityId);
        }
    }

    /**
     * 功能：执行 `extractValue` 对应的处理。
     * 参数：
     * - `kvEntry`：`kvEntry` 参数。
     * 返回：处理结果。
     */
    private ValueWithTs extractValue(TsKvEntry kvEntry) {
        if (kvEntry == null || kvEntry.getValue() == null) {
            return null;
        }
        double result = 0.0;
        long ts = kvEntry.getTs();
        switch (kvEntry.getDataType()) {
            case LONG:
                result = kvEntry.getLongValue().get();
                break;
            case DOUBLE:
                result = kvEntry.getDoubleValue().get();
                break;
            case STRING:
                try {
                    result = Double.parseDouble(kvEntry.getStrValue().get());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Calculation failed. Unable to parse value [" + kvEntry.getStrValue().get() + "]" +
                            " of telemetry [" + kvEntry.getKey() + "] to Double");
                }
                break;
            case BOOLEAN:
                throw new IllegalArgumentException("Calculation failed. Boolean values are not supported!");
            case JSON:
                throw new IllegalArgumentException("Calculation failed. JSON values are not supported!");
        }
        return new ValueWithTs(ts, result);
    }

    /**
     * 中文说明：
     * 1. `ValueWithTs` 是 ThingsBoard Rule Engine Components 中围绕 `Value With Ts` 提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    private static class ValueWithTs {
        /**
         * 时间戳，用于标识当前数据或事件发生的时间。
         */
        private final long ts;
        /**
         * 值，保存当前处理得到的具体内容。
         */
        private final double value;

        /**
         * 功能：创建 `CalculateDeltaNode` 实例，并初始化必要字段。
         * 参数：
         * - `ts`：时间戳。
         * - `value`：值。
         * 返回：新创建的对象实例。
         */
        private ValueWithTs(long ts, double value) {
            this.ts = ts;
            this.value = value;
        }
    }
}
