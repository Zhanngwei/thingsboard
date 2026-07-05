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
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.LATEST_TS;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;

/**
 * 中文说明：`TbAbstractGetAttributesNode` 是抽象获取属性节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`泛型或父类定义的配置对象`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
public abstract class TbAbstractGetAttributesNode<C extends TbGetAttributesNodeConfiguration, T extends EntityId> extends TbAbstractNodeWithFetchTo<C> {

    /**
     * 值常量，用于统一引用固定值。
     */
    private static final String VALUE = "value";
    /**
     * 时间戳常量，用于统一引用固定值。
     */
    private static final String TS = "ts";
    /**
     * 是否为失败信息。
     */
    private boolean isTellFailureIfAbsent;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private boolean getLatestValueWithTs;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx, configuration);
        getLatestValueWithTs = config.isGetLatestValueWithTs();
        isTellFailureIfAbsent = BooleanUtils.toBooleanDefaultIfNull(config.isTellFailureIfAbsent(), true);
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        var msgDataAsObjectNode = TbMsgSource.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        withCallback(
                findEntityIdAsync(ctx, msg),
                entityId -> safePutAttributes(ctx, msg, msgDataAsObjectNode, entityId),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<T> findEntityIdAsync(TbContext ctx, TbMsg msg);

    /**
     * 功能：执行 `upgradeRuleNodesWithOldPropertyToUseFetchTo` 对应的处理。
     * 参数：
     * - `oldConfiguration`：配置对象。
     * - `oldProperty`：`oldProperty` 参数。
     * - `ifTrue`：`ifTrue` 参数。
     * - `ifFalse`：`ifFalse` 参数。
     * 返回：处理结果。
     */
    protected TbPair<Boolean, JsonNode> upgradeRuleNodesWithOldPropertyToUseFetchTo(
            JsonNode oldConfiguration,
            String oldProperty,
            String ifTrue,
            String ifFalse
    ) throws TbNodeException {
        var newConfigObjectNode = (ObjectNode) oldConfiguration;
        if (!newConfigObjectNode.has(oldProperty)) {
            newConfigObjectNode.put(FETCH_TO_PROPERTY_NAME, TbMsgSource.METADATA.name());
            return new TbPair<>(true, newConfigObjectNode);
        }
        if (newConfigObjectNode.get(oldProperty).isNull()) {
            newConfigObjectNode.remove(oldProperty);
            newConfigObjectNode.put(FETCH_TO_PROPERTY_NAME, TbMsgSource.METADATA.name());
            return new TbPair<>(true, newConfigObjectNode);
        }
        return upgradeConfigurationToUseFetchTo(oldProperty, ifTrue, ifFalse, newConfigObjectNode);
    }

    /**
     * 功能：执行 `safePutAttributes` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `msgDataNode`：待处理消息。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    private void safePutAttributes(TbContext ctx, TbMsg msg, ObjectNode msgDataNode, T entityId) {
        Set<TbPair<String, List<String>>> failuresPairSet = ConcurrentHashMap.newKeySet();
        var getKvEntryPairFutures = Futures.allAsList(
                getAttrAsync(ctx, entityId, CLIENT_SCOPE, TbNodeUtils.processPatterns(config.getClientAttributeNames(), msg), failuresPairSet),
                getAttrAsync(ctx, entityId, SHARED_SCOPE, TbNodeUtils.processPatterns(config.getSharedAttributeNames(), msg), failuresPairSet),
                getAttrAsync(ctx, entityId, SERVER_SCOPE, TbNodeUtils.processPatterns(config.getServerAttributeNames(), msg), failuresPairSet),
                getLatestTelemetry(ctx, entityId, TbNodeUtils.processPatterns(config.getLatestTsKeyNames(), msg), failuresPairSet)
        );
        withCallback(getKvEntryPairFutures, futuresList -> {
            var msgMetaData = msg.getMetaData().copy();
            futuresList.stream().filter(Objects::nonNull).forEach(kvEntriesPair -> {
                var keyScope = kvEntriesPair.getFirst();
                var kvEntryList = kvEntriesPair.getSecond();
                var prefix = getPrefix(keyScope);
                kvEntryList.forEach(kvEntry -> {
                    String targetKey = prefix + kvEntry.getKey();
                    enrichMessage(msgDataNode, msgMetaData, kvEntry, targetKey);
                });
            });
            TbMsg outMsg = transformMessage(msg, msgDataNode, msgMetaData);
            if (failuresPairSet.isEmpty()) {
                ctx.tellSuccess(outMsg);
            } else {
                ctx.tellFailure(outMsg, reportFailures(failuresPairSet));
            }
        }, t -> ctx.tellFailure(msg, t), MoreExecutors.directExecutor());
    }

    /**
     * 功能：获取`Attr Async`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TbPair<String, List<AttributeKvEntry>>> getAttrAsync(
            TbContext ctx,
            EntityId entityId,
            String scope,
            List<String> keys,
            Set<TbPair<String, List<String>>> failuresPairSet
    ) {
        if (CollectionUtils.isEmpty(keys)) {
            return Futures.immediateFuture(null);
        }
        var attributeKvEntryListFuture = ctx.getAttributesService().find(ctx.getTenantId(), entityId, scope, keys);
        return Futures.transform(attributeKvEntryListFuture, attributeKvEntryList -> {
            if (isTellFailureIfAbsent && attributeKvEntryList.size() != keys.size()) {
                List<String> nonExistentKeys = getNonExistentKeys(attributeKvEntryList, keys);
                failuresPairSet.add(new TbPair<>(scope, nonExistentKeys));
            }
            return new TbPair<>(scope, attributeKvEntryList);
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：获取遥测。
     * 参数：
     * - `ctx`：处理上下文。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `failuresPairSet`：数据列表。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TbPair<String, List<TsKvEntry>>> getLatestTelemetry(TbContext ctx, EntityId entityId, List<String> keys, Set<TbPair<String, List<String>>> failuresPairSet) {
        if (CollectionUtils.isEmpty(keys)) {
            return Futures.immediateFuture(null);
        }
        ListenableFuture<List<TsKvEntry>> latestTelemetryFutures = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, keys);
        return Futures.transform(latestTelemetryFutures, tsKvEntries -> {
            var listTsKvEntry = new ArrayList<TsKvEntry>();
            var nonExistentKeys = new ArrayList<String>();
            tsKvEntries.forEach(tsKvEntry -> {
                if (tsKvEntry.getValue() == null) {
                    if (isTellFailureIfAbsent) {
                        nonExistentKeys.add(tsKvEntry.getKey());
                    }
                } else if (getLatestValueWithTs) {
                    listTsKvEntry.add(getValueWithTs(tsKvEntry));
                } else {
                    listTsKvEntry.add(new BasicTsKvEntry(tsKvEntry.getTs(), tsKvEntry));
                }
            });
            if (isTellFailureIfAbsent && !nonExistentKeys.isEmpty()) {
                failuresPairSet.add(new TbPair<>(LATEST_TS, nonExistentKeys));
            }
            return new TbPair<>(LATEST_TS, listTsKvEntry);
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * 返回：处理结果。
     */
    private TsKvEntry getValueWithTs(TsKvEntry tsKvEntry) {
        var mapper = TbMsgSource.DATA.equals(fetchTo) ? JacksonUtil.OBJECT_MAPPER : JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER;
        var value = JacksonUtil.newObjectNode(mapper);
        value.put(TS, tsKvEntry.getTs());
        JacksonUtil.addKvEntry(value, tsKvEntry, VALUE, mapper);
        return new BasicTsKvEntry(tsKvEntry.getTs(), new JsonDataEntry(tsKvEntry.getKey(), value.toString()));
    }

    /**
     * 功能：获取`Prefix`。
     * 参数：
     * - `scope`：`scope` 参数。
     * 返回：文本结果。
     */
    private String getPrefix(String scope) {
        var prefix = "";
        switch (scope) {
            case CLIENT_SCOPE:
                prefix = "cs_";
                break;
            case SHARED_SCOPE:
                prefix = "shared_";
                break;
            case SERVER_SCOPE:
                prefix = "ss_";
                break;
        }
        return prefix;
    }

    /**
     * 功能：获取`Non Existent Keys`。
     * 参数：
     * - `existingAttributesKvEntry`：数据列表。
     * - `allKeys`：键。
     * 返回：匹配的数据集合。
     */
    private List<String> getNonExistentKeys(List<AttributeKvEntry> existingAttributesKvEntry, List<String> allKeys) {
        List<String> existingKeys = existingAttributesKvEntry.stream().map(KvEntry::getKey).collect(Collectors.toList());
        return allKeys.stream().filter(key -> !existingKeys.contains(key)).collect(Collectors.toList());
    }

    /**
     * 功能：上报失败信息。
     * 参数：
     * - `failuresPairSet`：数据列表。
     * 返回：处理结果。
     */
    private RuntimeException reportFailures(Set<TbPair<String, List<String>>> failuresPairSet) {
        var errorMessage = new StringBuilder("The following attribute/telemetry keys is not present in the DB: ").append("\n");
        failuresPairSet.forEach(failurePair -> {
            String scope = failurePair.getFirst();
            List<String> nonExistentKeys = failurePair.getSecond();
            errorMessage.append("\t").append("[").append(scope).append("]:").append(nonExistentKeys.toString()).append("\n");
        });
        failuresPairSet.clear();
        return new RuntimeException(errorMessage.toString());
    }

    /*
     * 本类总结：`TbAbstractGetAttributesNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
