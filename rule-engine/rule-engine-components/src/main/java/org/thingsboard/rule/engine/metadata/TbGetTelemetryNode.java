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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by mshvayka on 04.09.18.
 */
@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "originator telemetry",
        configClazz = TbGetTelemetryNodeConfiguration.class,
        nodeDescription = "Adds message originator telemetry for selected time range into message metadata",
        nodeDetails = "Useful when you need to get telemetry data set from the message originator for a specific time range " +
                "instead of fetching just the latest telemetry or if you need to get the closest telemetry to the fetch interval start or end. " +
                "Also, this node can be used for telemetry aggregation within configured fetch interval.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeGetTelemetryFromDatabase")
/**
 * 中文说明：`TbGetTelemetryNode` 是获取遥测节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbGetTelemetryNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbGetTelemetryNode implements TbNode {

    /**
     * 常量字段：定义 `DESC_ORDER`，用于与本类处理流程相关的运行时值，本身不触发外部系统调用。
     */
    private static final String DESC_ORDER = "DESC";
    /**
     * 常量字段：定义 `ASC_ORDER`，用于与本类处理流程相关的运行时值，本身不触发外部系统调用。
     */
    private static final String ASC_ORDER = "ASC";

    /**
     * 字段说明：保存从规则节点 JSON 转换得到的配置对象，供消息处理和生命周期方法复用。
     */
    private TbGetTelemetryNodeConfiguration config;
    /**
     * 字段说明：保存 `tsKeyNames`，表示消息体、元数据、属性或遥测中的键名，供本类方法在规则节点处理流程中使用。
     */
    private List<String> tsKeyNames;
    /**
     * 字段说明：保存 `limit`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private int limit;
    /**
     * 字段说明：保存 `fetchMode`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private String fetchMode;
    /**
     * 字段说明：保存 `orderByFetchAll`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private String orderByFetchAll;
    /**
     * 字段说明：保存 `aggregation`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private Aggregation aggregation;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetTelemetryNodeConfiguration.class);
        tsKeyNames = config.getLatestTsKeyNames();
        limit = config.getFetchMode().equals(TbGetTelemetryNodeConfiguration.FETCH_MODE_ALL) ? validateLimit(config.getLimit()) : 1;
        fetchMode = config.getFetchMode();
        orderByFetchAll = config.getOrderBy();
        if (StringUtils.isEmpty(orderByFetchAll)) {
            orderByFetchAll = ASC_ORDER;
        }
        aggregation = parseAggregationConfig(config.getAggregation());
    }

    /**
     * 方法说明：执行 `parseAggregationConfig` 对应的辅助逻辑，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    Aggregation parseAggregationConfig(String aggName) {
        if (StringUtils.isEmpty(aggName) || !fetchMode.equals(TbGetTelemetryNodeConfiguration.FETCH_MODE_ALL)) {
            return Aggregation.NONE;
        }
        return Aggregation.valueOf(aggName);
    }

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `TimeseriesService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (tsKeyNames.isEmpty()) {
            ctx.tellFailure(msg, new IllegalStateException("Telemetry is not selected!"));
        } else {
            try {
                Interval interval = getInterval(msg);
                List<String> keys = TbNodeUtils.processPatterns(tsKeyNames, msg);
                // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
                ListenableFuture<List<TsKvEntry>> list = ctx.getTimeseriesService().findAll(ctx.getTenantId(), msg.getOriginator(), buildQueries(interval, keys));
                // 异步回调用于把服务或转换结果映射为规则链成功/失败关系。
                DonAsynchron.withCallback(list, data -> {
                    var metaData = updateMetadata(data, msg, keys);
                    ctx.tellSuccess(TbMsg.transformMsgMetadata(msg, metaData));
                }, error -> ctx.tellFailure(msg, error), ctx.getDbCallbackExecutor());
            } catch (Exception e) {
                ctx.tellFailure(msg, e);
            }
        }
    }

    /**
     * 方法说明：构造告警详情、SSL 上下文或输出对象，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private List<ReadTsKvQuery> buildQueries(Interval interval, List<String> keys) {
        final long aggIntervalStep = Aggregation.NONE.equals(aggregation) ? 1 :
                // exact how it validates on BaseTimeseriesService.validate()
                // see CassandraBaseTimeseriesDao.findAllAsync()
                interval.getEndTs() - interval.getStartTs();

        return keys.stream()
                .map(key -> new BaseReadTsKvQuery(key, interval.getStartTs(), interval.getEndTs(), aggIntervalStep, limit, aggregation, getOrderBy()))
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private String getOrderBy() {
        switch (fetchMode) {
            case TbGetTelemetryNodeConfiguration.FETCH_MODE_ALL:
                return orderByFetchAll;
            case TbGetTelemetryNodeConfiguration.FETCH_MODE_FIRST:
                return ASC_ORDER;
            default:
                return DESC_ORDER;
        }
    }

    /**
     * 方法说明：执行 `updateMetadata` 对应的辅助逻辑，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private TbMsgMetaData updateMetadata(List<TsKvEntry> entries, TbMsg msg, List<String> keys) {
        ObjectNode resultNode = JacksonUtil.newObjectNode(JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        if (TbGetTelemetryNodeConfiguration.FETCH_MODE_ALL.equals(fetchMode)) {
            entries.forEach(entry -> processArray(resultNode, entry));
        } else {
            entries.forEach(entry -> processSingle(resultNode, entry));
        }
        var copy = msg.getMetaData().copy();
        for (String key : keys) {
            if (resultNode.has(key)) {
                copy.putValue(key, resultNode.get(key).toString());
            }
        }
        return copy;
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void processSingle(ObjectNode node, TsKvEntry entry) {
        node.put(entry.getKey(), entry.getValueAsString());
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void processArray(ObjectNode node, TsKvEntry entry) {
        if (node.has(entry.getKey())) {
            ArrayNode arrayNode = (ArrayNode) node.get(entry.getKey());
            arrayNode.add(buildNode(entry));
        } else {
            ArrayNode arrayNode = JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER.createArrayNode();
            arrayNode.add(buildNode(entry));
            node.set(entry.getKey(), arrayNode);
        }
    }

    /**
     * 方法说明：构造告警详情、SSL 上下文或输出对象，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ObjectNode buildNode(TsKvEntry entry) {
        ObjectNode obj = JacksonUtil.newObjectNode(JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        obj.put("ts", entry.getTs());
        JacksonUtil.addKvEntry(obj, entry, "value", JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        return obj;
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private Interval getInterval(TbMsg msg) {
        if (config.isUseMetadataIntervalPatterns()) {
            return getIntervalFromPatterns(msg);
        } else {
            Interval interval = new Interval();
            long ts = System.currentTimeMillis();
            interval.setStartTs(ts - TimeUnit.valueOf(config.getStartIntervalTimeUnit()).toMillis(config.getStartInterval()));
            interval.setEndTs(ts - TimeUnit.valueOf(config.getEndIntervalTimeUnit()).toMillis(config.getEndInterval()));
            return interval;
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private Interval getIntervalFromPatterns(TbMsg msg) {
        Interval interval = new Interval();
        interval.setStartTs(checkPattern(msg, config.getStartIntervalPattern()));
        interval.setEndTs(checkPattern(msg, config.getEndIntervalPattern()));
        return interval;
    }

    /**
     * 方法说明：检查状态、关系、配置或数据合法性，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private long checkPattern(TbMsg msg, String pattern) {
        String value = getValuePattern(msg, pattern);
        if (value == null) {
            throw new IllegalArgumentException("Message value: '" +
                    replaceRegex(pattern) + "' is undefined");
        }
        boolean parsable = NumberUtils.isParsable(value);
        if (!parsable) {
            throw new IllegalArgumentException("Message value: '" +
                    replaceRegex(pattern) + "' has invalid format");
        }
        return Long.parseLong(value);
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private String getValuePattern(TbMsg msg, String pattern) {
        String value = TbNodeUtils.processPattern(pattern, msg);
        return value.equals(pattern) ? null : value;
    }

    /**
     * 方法说明：执行 `replaceRegex` 对应的辅助逻辑，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private String replaceRegex(String pattern) {
        return pattern.replaceAll("[$\\[{}\\]]", "");
    }

    /**
     * 方法说明：校验配置或数据是否满足节点要求，供 `TbGetTelemetryNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private int validateLimit(int limit) {
        if (limit != 0) {
            return limit;
        } else {
            return TbGetTelemetryNodeConfiguration.MAX_FETCH_SIZE;
        }
    }

    @Data
    @NoArgsConstructor
    /**
     * 中文说明：`Interval` 是Interval辅助类，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    private static class Interval {
        /**
         * 字段说明：保存 `startTs`，表示时间戳，供本类方法在规则节点处理流程中使用。
         */
        private Long startTs;
        /**
         * 字段说明：保存 `endTs`，表示时间戳，供本类方法在规则节点处理流程中使用。
         */
        private Long endTs;
    }

    /*
     * 本类总结：`TbGetTelemetryNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
