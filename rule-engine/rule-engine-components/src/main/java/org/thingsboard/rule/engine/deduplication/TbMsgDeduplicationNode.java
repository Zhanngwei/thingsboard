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
package org.thingsboard.rule.engine.deduplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "deduplication",
        configClazz = TbMsgDeduplicationNodeConfiguration.class,
        version = 1,
        hasQueueName = true,
        nodeDescription = "Deduplicate messages within the same originator entity for a configurable period " +
                "based on a specified deduplication strategy.",
        nodeDetails = "Deduplication strategies: <ul><li><strong>FIRST</strong> - return first message that arrived during deduplication period.</li>" +
                "<li><strong>LAST</strong> - return last message that arrived during deduplication period.</li>" +
                "<li><strong>ALL</strong> - return all messages as a single JSON array message. " +
                "Where each element represents object with <strong><i>msg</i></strong> and <strong><i>metadata</i></strong> inner properties.</li></ul>",
        icon = "content_copy",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMsgDeduplicationConfig"
)
@Slf4j
/**
 * 中文说明：`TbMsgDeduplicationNode` 是消息去重节点规则节点，用于按配置聚合、去重、延迟和输出消息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbMsgDeduplicationNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbMsgDeduplicationNode implements TbNode {

    /**
     * 常量字段：定义 `TB_MSG_DEDUPLICATION_RETRY_DELAY`，用于重试次数或重试间隔，本身不触发外部系统调用。
     */
    public static final int TB_MSG_DEDUPLICATION_RETRY_DELAY = 10;

    /**
     * 字段说明：保存从规则节点 JSON 转换得到的配置对象，供消息处理和生命周期方法复用。
     */
    private TbMsgDeduplicationNodeConfiguration config;

    private final Map<EntityId, DeduplicationData> deduplicationMap;
    /**
     * 字段说明：保存 `deduplicationInterval`，表示时间间隔，供本类方法在规则节点处理流程中使用。
     */
    private long deduplicationInterval;
    /**
     * 字段说明：保存 `queueName` 本地缓存、队列或并发状态，用于协调本类处理流程。
     */
    private String queueName;

    /**
     * 方法说明：构造 `TbMsgDeduplicationNode` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    public TbMsgDeduplicationNode() {
        this.deduplicationMap = new HashMap<>();
    }

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeduplicationNodeConfiguration.class);
        this.deduplicationInterval = TimeUnit.SECONDS.toMillis(config.getInterval());
        this.queueName = ctx.getQueueName();
    }

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (msg.isTypeOf(TbMsgType.DEDUPLICATION_TIMEOUT_SELF_MSG)) {
            processDeduplication(ctx, msg.getOriginator());
        } else {
            processOnRegularMsg(ctx, msg);
        }
    }

    @Override
    /**
     * 方法说明：在节点生命周期销毁阶段释放缓存、脚本引擎、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void destroy() {
        deduplicationMap.clear();
    }

    @Override
    /**
     * 方法说明：迁移旧版本规则节点 JSON 配置结构。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (oldConfiguration.has(QUEUE_NAME)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).remove(QUEUE_NAME);
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbMsgDeduplicationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void processOnRegularMsg(TbContext ctx, TbMsg msg) {
        EntityId id = msg.getOriginator();
        DeduplicationData deduplicationMsgs = deduplicationMap.computeIfAbsent(id, k -> new DeduplicationData());
        if (deduplicationMsgs.size() < config.getMaxPendingMsgs()) {
            log.trace("[{}][{}] Adding msg: [{}][{}] to the pending msgs map ...", ctx.getSelfId(), id, msg.getId(), msg.getMetaDataTs());
            deduplicationMsgs.add(msg);
            ctx.ack(msg);
            scheduleTickMsg(ctx, id, deduplicationMsgs);
        } else {
            log.trace("[{}] Max limit of pending messages reached for deduplication id: [{}]", ctx.getSelfId(), id);
            ctx.tellFailure(msg, new RuntimeException("[" + ctx.getSelfId() + "] Max limit of pending messages reached for deduplication id: [" + id + "]"));
        }
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbMsgDeduplicationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void processDeduplication(TbContext ctx, EntityId deduplicationId) {
        DeduplicationData data = deduplicationMap.get(deduplicationId);
        if (data == null) {
            return;
        }
        data.setTickScheduled(false);
        if (data.isEmpty()) {
            return;
        }
        long deduplicationTimeoutMs = System.currentTimeMillis();
        try {
            List<TbMsg> deduplicationResults = new ArrayList<>();
            List<TbMsg> msgList = data.getMsgList();
            Optional<TbPair<Long, Long>> packBoundsOpt = findValidPack(msgList, deduplicationTimeoutMs);
            while (packBoundsOpt.isPresent()) {
                TbPair<Long, Long> packBounds = packBoundsOpt.get();
                if (DeduplicationStrategy.ALL.equals(config.getStrategy())) {
                    List<TbMsg> pack = new ArrayList<>();
                    for (Iterator<TbMsg> iterator = msgList.iterator(); iterator.hasNext(); ) {
                        TbMsg msg = iterator.next();
                        long msgTs = msg.getMetaDataTs();
                        if (msgTs >= packBounds.getFirst() && msgTs < packBounds.getSecond()) {
                            pack.add(msg);
                            iterator.remove();
                        }
                    }
                    deduplicationResults.add(TbMsg.newMsg(
                            queueName,
                            config.getOutMsgType(),
                            deduplicationId,
                            getMetadata(),
                            getMergedData(pack)));
                } else {
                    TbMsg resultMsg = null;
                    boolean searchMin = DeduplicationStrategy.FIRST.equals(config.getStrategy());
                    for (Iterator<TbMsg> iterator = msgList.iterator(); iterator.hasNext(); ) {
                        TbMsg msg = iterator.next();
                        long msgTs = msg.getMetaDataTs();
                        if (msgTs >= packBounds.getFirst() && msgTs < packBounds.getSecond()) {
                            iterator.remove();
                            if (resultMsg == null
                                    || (searchMin && msg.getMetaDataTs() < resultMsg.getMetaDataTs())
                                    || (!searchMin && msg.getMetaDataTs() > resultMsg.getMetaDataTs())) {
                                resultMsg = msg;
                            }
                        }
                    }
                    if (resultMsg != null) {
                        deduplicationResults.add(TbMsg.newMsg(
                                queueName != null ? queueName : resultMsg.getQueueName(),
                                resultMsg.getType(),
                                resultMsg.getOriginator(),
                                resultMsg.getCustomerId(),
                                resultMsg.getMetaData(),
                                resultMsg.getData()));
                    }
                }
                packBoundsOpt = findValidPack(msgList, deduplicationTimeoutMs);
            }
            deduplicationResults.forEach(outMsg -> enqueueForTellNextWithRetry(ctx, outMsg, 0));
        } finally {
            if (!data.isEmpty()) {
                scheduleTickMsg(ctx, deduplicationId, data);
            }
        }
    }

    /**
     * 方法说明：执行 `scheduleTickMsg` 对应的辅助逻辑，供 `TbMsgDeduplicationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId, DeduplicationData data) {
        if (!data.isTickScheduled()) {
            scheduleTickMsg(ctx, deduplicationId);
            data.setTickScheduled(true);
        }
    }

    /**
     * 方法说明：按租户、实体或关系条件查询数据，供 `TbMsgDeduplicationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private Optional<TbPair<Long, Long>> findValidPack(List<TbMsg> msgs, long deduplicationTimeoutMs) {
        Optional<TbMsg> min = msgs.stream().min(Comparator.comparing(TbMsg::getMetaDataTs));
        return min.map(minTsMsg -> {
            long packStartTs = minTsMsg.getMetaDataTs();
            long packEndTs = packStartTs + deduplicationInterval;
            if (packEndTs <= deduplicationTimeoutMs) {
                return new TbPair<>(packStartTs, packEndTs);
            }
            return null;
        });
    }

    /**
     * 方法说明：把处理结果发送到规则链后续关系，供 `TbMsgDeduplicationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void enqueueForTellNextWithRetry(TbContext ctx, TbMsg msg, int retryAttempt) {
        if (config.getMaxRetries() > retryAttempt) {
            // 通过规则引擎上下文安排后续消息投递或自身定时消息。
            ctx.enqueueForTellNext(msg, TbNodeConnectionType.SUCCESS,
                    () -> {
                        log.trace("[{}][{}][{}] Successfully enqueue deduplication result message!", ctx.getSelfId(), msg.getOriginator(), retryAttempt);
                    },
                    throwable -> {
                        log.trace("[{}][{}][{}] Failed to enqueue deduplication output message due to: ", ctx.getSelfId(), msg.getOriginator(), retryAttempt, throwable);
                        // 通过规则引擎上下文安排后续消息投递或自身定时消息。
                        ctx.schedule(() -> {
                            enqueueForTellNextWithRetry(ctx, msg, retryAttempt + 1);
                        }, TB_MSG_DEDUPLICATION_RETRY_DELAY, TimeUnit.SECONDS);
                    });
        }
    }

    /**
     * 方法说明：执行 `scheduleTickMsg` 对应的辅助逻辑，供 `TbMsgDeduplicationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId) {
        // 通过规则引擎上下文安排后续消息投递或自身定时消息。
        ctx.tellSelf(ctx.newMsg(null, TbMsgType.DEDUPLICATION_TIMEOUT_SELF_MSG, deduplicationId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING), deduplicationInterval + 1);
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbMsgDeduplicationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private String getMergedData(List<TbMsg> msgs) {
        ArrayNode mergedData = JacksonUtil.newArrayNode();
        msgs.forEach(msg -> {
            ObjectNode msgNode = JacksonUtil.newObjectNode();
            msgNode.set("msg", JacksonUtil.toJsonNode(msg.getData()));
            msgNode.set("metadata", JacksonUtil.valueToTree(msg.getMetaData().getData()));
            mergedData.add(msgNode);
        });
        return JacksonUtil.toString(mergedData);
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbMsgDeduplicationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private TbMsgMetaData getMetadata() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", String.valueOf(System.currentTimeMillis()));
        return metaData;
    }

    /*
     * 本类总结：`TbMsgDeduplicationNode` 负责按配置聚合、去重、延迟和输出消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
