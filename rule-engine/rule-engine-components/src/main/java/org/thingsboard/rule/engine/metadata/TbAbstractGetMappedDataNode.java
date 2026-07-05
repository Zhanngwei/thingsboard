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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesFieldsAsyncLoader;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

/**
 * 中文说明：`TbAbstractGetMappedDataNode` 是抽象获取映射数据节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`泛型或父类定义的配置对象`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
public abstract class TbAbstractGetMappedDataNode<T extends EntityId, C extends TbGetMappedDataNodeConfiguration> extends TbAbstractNodeWithFetchTo<C> {

    /**
     * 功能：校验`If Mapping Is Not Empty Or Else Throw`。
     * 参数：
     * - `dataMapping`：待处理数据。
     * 返回：无。
     */
    protected void checkIfMappingIsNotEmptyOrElseThrow(Map<String, String> dataMapping) throws TbNodeException {
        if (dataMapping == null || dataMapping.isEmpty()) {
            throw new TbNodeException("At least one mapping entry should be specified!");
        }
    }

    /**
     * 功能：处理数据。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `entityId`：实体IDID。
     * - `msgDataAsJsonNode`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    protected void processFieldsData(TbContext ctx, TbMsg msg, T entityId, ObjectNode msgDataAsJsonNode, boolean ignoreNullStrings) {
        var mappingsMap = processFieldsMappingPatterns(msg);
        withCallback(getEntityFieldsAsync(ctx, entityId, mappingsMap, ignoreNullStrings),
                data -> putFieldsDataAndTell(ctx, msg, msgDataAsJsonNode, data),
                t -> ctx.tellFailure(msg, t),
                MoreExecutors.directExecutor());
    }

    /**
     * 功能：处理数据。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `entityId`：实体IDID。
     * - `msgDataAsJsonNode`：待处理消息。
     * 返回：无。
     */
    protected void processAttributesKvEntryData(TbContext ctx, TbMsg msg, T entityId, ObjectNode msgDataAsJsonNode) {
        var mappingsMap = processKvEntryMappingPatterns(msg);
        var sourceKeys = List.copyOf(mappingsMap.keySet());
        withCallback(getAttributesAsync(ctx, entityId, sourceKeys),
                data -> putKvEntryDataAndTell(ctx, msg, data, mappingsMap, msgDataAsJsonNode),
                t -> ctx.tellFailure(msg, t),
                MoreExecutors.directExecutor());
    }

    /**
     * 功能：处理时间戳。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `entityId`：实体IDID。
     * - `msgDataAsJsonNode`：待处理消息。
     * 返回：无。
     */
    protected void processTsKvEntryData(TbContext ctx, TbMsg msg, T entityId, ObjectNode msgDataAsJsonNode) {
        var mappingsMap = processKvEntryMappingPatterns(msg);
        var sourceKeys = List.copyOf(mappingsMap.keySet());
        withCallback(getLatestTelemetryAsync(ctx, entityId, sourceKeys),
                data -> putKvEntryDataAndTell(ctx, msg, data, mappingsMap, msgDataAsJsonNode),
                t -> ctx.tellFailure(msg, t),
                MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行 `putFieldsDataAndTell` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `msgDataAsJsonNode`：待处理消息。
     * - `targetKeysToSourceValuesMap`：键。
     * 返回：无。
     */
    private void putFieldsDataAndTell(TbContext ctx, TbMsg msg, ObjectNode msgDataAsJsonNode, Map<String, String> targetKeysToSourceValuesMap) {
        TbMsgMetaData msgMetaData = msg.getMetaData().copy();
        for (var entry : targetKeysToSourceValuesMap.entrySet()) {
            var targetKeyName = entry.getKey();
            var sourceFieldValue = entry.getValue();
            if (TbMsgSource.DATA.equals(fetchTo)) {
                msgDataAsJsonNode.put(targetKeyName, sourceFieldValue);
            } else if (TbMsgSource.METADATA.equals(fetchTo)) {
                msgMetaData.putValue(targetKeyName, sourceFieldValue);
            }
        }
        TbMsg outMsg = transformMessage(msg, msgDataAsJsonNode, msgMetaData);
        ctx.tellSuccess(outMsg);
    }

    /**
     * 功能：执行 `putKvEntryDataAndTell` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `data`：待处理数据。
     * - `map`：键值映射。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void putKvEntryDataAndTell(TbContext ctx, TbMsg msg, List<? extends KvEntry> data, Map<String, String> map, ObjectNode msgData) {
        var msgMetaData = msg.getMetaData().copy();
        for (KvEntry entry : data) {
            String targetKey = map.get(entry.getKey());
            enrichMessage(msgData, msgMetaData, entry, targetKey);
        }
        ctx.tellSuccess(transformMessage(msg, msgData, msgMetaData));
    }

    /**
     * 功能：处理`Fields Mapping Patterns`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private Map<String, String> processFieldsMappingPatterns(TbMsg msg) {
        var mappingsMap = new HashMap<String, String>();
        config.getDataMapping().forEach((sourceField, targetKey) -> {
            String patternProcessedTargetKey = TbNodeUtils.processPattern(targetKey, msg);
            mappingsMap.put(sourceField, patternProcessedTargetKey);
        });
        return mappingsMap;
    }

    /**
     * 功能：处理`Kv Entry Mapping Patterns`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private Map<String, String> processKvEntryMappingPatterns(TbMsg msg) {
        var mappingsMap = new HashMap<String, String>();
        config.getDataMapping().forEach((sourceKey, targetKey) -> {
            String patternProcessedSourceKey = TbNodeUtils.processPattern(sourceKey, msg);
            String patternProcessedTargetKey = TbNodeUtils.processPattern(targetKey, msg);
            mappingsMap.put(patternProcessedSourceKey, patternProcessedTargetKey);
        });
        return mappingsMap;
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityId`：实体IDID。
     * - `mappingsMap`：键值映射。
     * - `ignoreNullStrings`：`ignoreNullStrings` 参数。
     * 返回：处理结果。
     */
    private ListenableFuture<Map<String, String>> getEntityFieldsAsync(TbContext ctx, EntityId entityId, Map<String, String> mappingsMap, boolean ignoreNullStrings) {
        return Futures.transform(EntitiesFieldsAsyncLoader.findAsync(ctx, entityId),
                fieldsData -> {
                    var targetKeysToSourceValuesMap = new HashMap<String, String>();
                    for (var mappingEntry : mappingsMap.entrySet()) {
                        var sourceFieldName = mappingEntry.getKey();
                        var targetKeyName = mappingEntry.getValue();
                        var sourceFieldValue = fieldsData.getFieldValue(sourceFieldName, ignoreNullStrings);
                        if (sourceFieldValue != null) {
                            targetKeysToSourceValuesMap.put(targetKeyName, sourceFieldValue);
                        }
                    }
                    return targetKeysToSourceValuesMap;
                }, ctx.getDbCallbackExecutor()
        );
    }

    /**
     * 功能：获取`Attributes Async`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityId`：实体IDID。
     * - `attrKeys`：键。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<KvEntry>> getAttributesAsync(TbContext ctx, EntityId entityId, List<String> attrKeys) {
        var latest = ctx.getAttributesService().find(ctx.getTenantId(), entityId, SERVER_SCOPE, attrKeys);
        return Futures.transform(latest, l ->
                        l.stream()
                                .map(i -> (KvEntry) i)
                                .collect(Collectors.toList()),
                ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：获取遥测。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityId`：实体IDID。
     * - `timeseriesKeys`：键。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<KvEntry>> getLatestTelemetryAsync(TbContext ctx, EntityId entityId, List<String> timeseriesKeys) {
        var latest = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, timeseriesKeys);
        return Futures.transform(latest, l ->
                        l.stream()
                                .map(i -> (KvEntry) i)
                                .collect(Collectors.toList()),
                ctx.getDbCallbackExecutor());
    }

    /*
     * 本类总结：`TbAbstractGetMappedDataNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
