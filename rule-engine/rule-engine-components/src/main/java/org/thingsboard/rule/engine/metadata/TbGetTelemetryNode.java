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
/**
 * 中文说明：`TbGetTelemetryNode` 是获取遥测节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbGetTelemetryNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
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
public class TbGetTelemetryNode implements TbNode {

    /**
     * `DESC_ORDER`常量，用于统一引用固定值。
     */
    private static final String DESC_ORDER = "DESC";
    /**
     * `ASC_ORDER`常量，用于统一引用固定值。
     */
    private static final String ASC_ORDER = "ASC";

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbGetTelemetryNodeConfiguration config;
    /**
     * 时间戳列表，用于保存一组待处理对象。
     */
    private List<String> tsKeyNames;
    /**
     * 数量限制，用于控制数量、位置或分页范围。
     */
    private int limit;
    /**
     * `fetchMode` 字段，保存当前对象的对应属性。
     */
    private String fetchMode;
    /**
     * `orderByFetchAll` 字段，保存当前对象的对应属性。
     */
    private String orderByFetchAll;
    /**
     * `aggregation` 字段，保存当前对象的对应属性。
     */
    private Aggregation aggregation;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
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
     * 功能：解析配置。
     * 参数：
     * - `aggName`：名称。
     * 返回：处理结果。
     */
    Aggregation parseAggregationConfig(String aggName) {
        if (StringUtils.isEmpty(aggName) || !fetchMode.equals(TbGetTelemetryNodeConfiguration.FETCH_MODE_ALL)) {
            return Aggregation.NONE;
        }
        return Aggregation.valueOf(aggName);
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (tsKeyNames.isEmpty()) {
            ctx.tellFailure(msg, new IllegalStateException("Telemetry is not selected!"));
        } else {
            try {
                Interval interval = getInterval(msg);
                List<String> keys = TbNodeUtils.processPatterns(tsKeyNames, msg);
                ListenableFuture<List<TsKvEntry>> list = ctx.getTimeseriesService().findAll(ctx.getTenantId(), msg.getOriginator(), buildQueries(interval, keys));
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
     * 功能：构建`Queries`。
     * 参数：
     * - `interval`：`interval` 参数。
     * - `keys`：键。
     * 返回：匹配的数据集合。
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
     * 功能：获取`Order By`。
     * 参数：无。
     * 返回：文本结果。
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
     * 功能：更新`Metadata`。
     * 参数：
     * - `entries`：数据列表。
     * - `msg`：待处理消息。
     * - `keys`：键。
     * 返回：处理结果。
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
     * 功能：处理`Single`。
     * 参数：
     * - `node`：`node` 参数。
     * - `entry`：`entry` 参数。
     * 返回：无。
     */
    private void processSingle(ObjectNode node, TsKvEntry entry) {
        node.put(entry.getKey(), entry.getValueAsString());
    }

    /**
     * 功能：处理`Array`。
     * 参数：
     * - `node`：`node` 参数。
     * - `entry`：`entry` 参数。
     * 返回：无。
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
     * 功能：构建节点实例。
     * 参数：
     * - `entry`：`entry` 参数。
     * 返回：处理结果。
     */
    private ObjectNode buildNode(TsKvEntry entry) {
        ObjectNode obj = JacksonUtil.newObjectNode(JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        obj.put("ts", entry.getTs());
        JacksonUtil.addKvEntry(obj, entry, "value", JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        return obj;
    }

    /**
     * 功能：获取时间间隔。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
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
     * 功能：获取时间间隔。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private Interval getIntervalFromPatterns(TbMsg msg) {
        Interval interval = new Interval();
        interval.setStartTs(checkPattern(msg, config.getStartIntervalPattern()));
        interval.setEndTs(checkPattern(msg, config.getEndIntervalPattern()));
        return interval;
    }

    /**
     * 功能：校验`Pattern`。
     * 参数：
     * - `msg`：待处理消息。
     * - `pattern`：`pattern` 参数。
     * 返回：判断结果。
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
     * 功能：获取值。
     * 参数：
     * - `msg`：待处理消息。
     * - `pattern`：`pattern` 参数。
     * 返回：文本结果。
     */
    private String getValuePattern(TbMsg msg, String pattern) {
        String value = TbNodeUtils.processPattern(pattern, msg);
        return value.equals(pattern) ? null : value;
    }

    /**
     * 功能：执行 `replaceRegex` 对应的处理。
     * 参数：
     * - `pattern`：`pattern` 参数。
     * 返回：文本结果。
     */
    private String replaceRegex(String pattern) {
        return pattern.replaceAll("[$\\[{}\\]]", "");
    }

    /**
     * 功能：校验数量限制。
     * 参数：
     * - `limit`：数量限制。
     * 返回：判断结果。
     */
    private int validateLimit(int limit) {
        if (limit != 0) {
            return limit;
        } else {
            return TbGetTelemetryNodeConfiguration.MAX_FETCH_SIZE;
        }
    }

    /**
     * 中文说明：`Interval` 是Interval辅助类，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    @Data
    @NoArgsConstructor
    private static class Interval {
        /**
         * 时间戳，用于标识当前数据或事件发生的时间。
         */
        private Long startTs;
        /**
         * 时间戳，用于标识当前数据或事件发生的时间。
         */
        private Long endTs;
    }

    /*
     * 本类总结：`TbGetTelemetryNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
