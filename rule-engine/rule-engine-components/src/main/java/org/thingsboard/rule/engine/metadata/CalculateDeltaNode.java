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
/**
 * 中文说明：`CalculateDeltaNode` 是计算增量节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`CalculateDeltaNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class CalculateDeltaNode implements TbNode {

    private Map<EntityId, ValueWithTs> cache;
    /**
     * 字段说明：保存从规则节点 JSON 转换得到的配置对象，供消息处理和生命周期方法复用。
     */
    private CalculateDeltaNodeConfiguration config;
    /**
     * 字段说明：保存 Rule Engine 上下文引用；本字段本身不直接代表数据库、MQTT 或事务资源。
     */
    private TbContext ctx;
    /**
     * 字段说明：保存 `timeseriesService` 服务层引用；具体服务实现可能访问数据库或缓存，本字段本身不管理事务。
     */
    private TimeseriesService timeseriesService;
    /**
     * 字段说明：保存 `useCache` 本地缓存、队列或并发状态，用于协调本类处理流程。
     */
    private boolean useCache;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `TimeseriesService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, CalculateDeltaNodeConfiguration.class);
        this.ctx = ctx;
        this.timeseriesService = ctx.getTimeseriesService();
        this.useCache = config.isUseCache();
        if (useCache) {
            cache = new ConcurrentHashMap<>();
        }
    }

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
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
        // 异步回调用于把服务或转换结果映射为规则链成功/失败关系。
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

    @Override
    /**
     * 方法说明：在节点生命周期销毁阶段释放缓存、脚本引擎、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void destroy() {
        if (useCache) {
            cache.clear();
        }
    }

    /**
     * 方法说明：从消息或服务层获取需要补充的数据，供 `CalculateDeltaNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<ValueWithTs> fetchLatestValueAsync(EntityId entityId) {
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
        return Futures.transform(timeseriesService.findLatest(ctx.getTenantId(), entityId, Collections.singletonList(config.getInputValueKey())),
                list -> extractValue(list.get(0))
                , ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：从消息或服务层获取需要补充的数据，供 `CalculateDeltaNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ValueWithTs fetchLatestValue(EntityId entityId) {
        List<TsKvEntry> tsKvEntries = timeseriesService.findLatestSync(
                ctx.getTenantId(),
                entityId,
                Collections.singletonList(config.getInputValueKey()));
        return extractValue(tsKvEntries.get(0));
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `CalculateDeltaNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：执行 `extractValue` 对应的辅助逻辑，供 `CalculateDeltaNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 中文说明：`ValueWithTs` 是值WithTs辅助类，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    private static class ValueWithTs {
        /**
         * 字段说明：保存 `ts`，表示时间戳，供本类方法在规则节点处理流程中使用。
         */
        private final long ts;
        /**
         * 字段说明：保存 `value`，表示计算值或最近值，供本类方法在规则节点处理流程中使用。
         */
        private final double value;

        /**
         * 方法说明：构造 `ValueWithTs` 实例并初始化必要字段。
         * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
         */
        private ValueWithTs(long ts, double value) {
            this.ts = ts;
            this.value = value;
        }
    }

    /*
     * 本类总结：`CalculateDeltaNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
