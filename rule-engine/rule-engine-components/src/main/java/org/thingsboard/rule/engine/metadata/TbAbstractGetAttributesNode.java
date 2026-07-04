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

@Slf4j
/**
 * 中文说明：`TbAbstractGetAttributesNode` 是抽象获取属性节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`泛型或父类定义的配置对象`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public abstract class TbAbstractGetAttributesNode<C extends TbGetAttributesNodeConfiguration, T extends EntityId> extends TbAbstractNodeWithFetchTo<C> {

    /**
     * 常量字段：定义 `VALUE`，用于计算值或最近值，本身不触发外部系统调用。
     */
    private static final String VALUE = "value";
    /**
     * 常量字段：定义 `TS`，用于时间戳，本身不触发外部系统调用。
     */
    private static final String TS = "ts";
    /**
     * 字段说明：保存 `isTellFailureIfAbsent`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private boolean isTellFailureIfAbsent;
    /**
     * 字段说明：保存 `getLatestValueWithTs`，表示计算值或最近值，供本类方法在规则节点处理流程中使用。
     */
    private boolean getLatestValueWithTs;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx, configuration);
        getLatestValueWithTs = config.isGetLatestValueWithTs();
        isTellFailureIfAbsent = BooleanUtils.toBooleanDefaultIfNull(config.isTellFailureIfAbsent(), true);
    }

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        var msgDataAsObjectNode = TbMsgSource.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        // 异步回调用于把服务或转换结果映射为规则链成功/失败关系。
        withCallback(
                findEntityIdAsync(ctx, msg),
                entityId -> safePutAttributes(ctx, msg, msgDataAsObjectNode, entityId),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：按租户、实体或关系条件查询数据，供 `TbAbstractGetAttributesNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract ListenableFuture<T> findEntityIdAsync(TbContext ctx, TbMsg msg);

    /**
     * 方法说明：迁移旧版本规则节点 JSON 配置结构，供 `TbAbstractGetAttributesNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：执行 `safePutAttributes` 对应的辅助逻辑，供 `TbAbstractGetAttributesNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void safePutAttributes(TbContext ctx, TbMsg msg, ObjectNode msgDataNode, T entityId) {
        Set<TbPair<String, List<String>>> failuresPairSet = ConcurrentHashMap.newKeySet();
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
        var getKvEntryPairFutures = Futures.allAsList(
                getAttrAsync(ctx, entityId, CLIENT_SCOPE, TbNodeUtils.processPatterns(config.getClientAttributeNames(), msg), failuresPairSet),
                getAttrAsync(ctx, entityId, SHARED_SCOPE, TbNodeUtils.processPatterns(config.getSharedAttributeNames(), msg), failuresPairSet),
                getAttrAsync(ctx, entityId, SERVER_SCOPE, TbNodeUtils.processPatterns(config.getServerAttributeNames(), msg), failuresPairSet),
                getLatestTelemetry(ctx, entityId, TbNodeUtils.processPatterns(config.getLatestTsKeyNames(), msg), failuresPairSet)
        );
        // 异步回调用于把服务或转换结果映射为规则链成功/失败关系。
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
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractGetAttributesNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AttributesService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
        var attributeKvEntryListFuture = ctx.getAttributesService().find(ctx.getTenantId(), entityId, scope, keys);
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
        return Futures.transform(attributeKvEntryListFuture, attributeKvEntryList -> {
            if (isTellFailureIfAbsent && attributeKvEntryList.size() != keys.size()) {
                List<String> nonExistentKeys = getNonExistentKeys(attributeKvEntryList, keys);
                failuresPairSet.add(new TbPair<>(scope, nonExistentKeys));
            }
            return new TbPair<>(scope, attributeKvEntryList);
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractGetAttributesNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `TimeseriesService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<TbPair<String, List<TsKvEntry>>> getLatestTelemetry(TbContext ctx, EntityId entityId, List<String> keys, Set<TbPair<String, List<String>>> failuresPairSet) {
        if (CollectionUtils.isEmpty(keys)) {
            return Futures.immediateFuture(null);
        }
        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
        ListenableFuture<List<TsKvEntry>> latestTelemetryFutures = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), entityId, keys);
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
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
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractGetAttributesNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private TsKvEntry getValueWithTs(TsKvEntry tsKvEntry) {
        var mapper = TbMsgSource.DATA.equals(fetchTo) ? JacksonUtil.OBJECT_MAPPER : JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER;
        var value = JacksonUtil.newObjectNode(mapper);
        value.put(TS, tsKvEntry.getTs());
        JacksonUtil.addKvEntry(value, tsKvEntry, VALUE, mapper);
        return new BasicTsKvEntry(tsKvEntry.getTs(), new JsonDataEntry(tsKvEntry.getKey(), value.toString()));
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractGetAttributesNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractGetAttributesNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private List<String> getNonExistentKeys(List<AttributeKvEntry> existingAttributesKvEntry, List<String> allKeys) {
        List<String> existingKeys = existingAttributesKvEntry.stream().map(KvEntry::getKey).collect(Collectors.toList());
        return allKeys.stream().filter(key -> !existingKeys.contains(key)).collect(Collectors.toList());
    }

    /**
     * 方法说明：执行 `reportFailures` 对应的辅助逻辑，供 `TbAbstractGetAttributesNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
