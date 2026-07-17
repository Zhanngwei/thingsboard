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

/**
 * 中文说明：
 * 1. `TbMsgDeduplicationNode` 是 ThingsBoard Rule Engine Components 中处理消息的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
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
public class TbMsgDeduplicationNode implements TbNode {

    /**
     * 消息常量，用于统一引用固定值。
     */
    public static final int TB_MSG_DEDUPLICATION_RETRY_DELAY = 10;

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbMsgDeduplicationNodeConfiguration config;

    private final Map<EntityId, DeduplicationData> deduplicationMap;
    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    private long deduplicationInterval;
    /**
     * 队列名称，用于标识或展示当前对象。
     */
    private String queueName;

    /**
     * 功能：创建 `TbMsgDeduplicationNode` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TbMsgDeduplicationNode() {
        this.deduplicationMap = new HashMap<>();
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeduplicationNodeConfiguration.class);
        this.deduplicationInterval = TimeUnit.SECONDS.toMillis(config.getInterval());
        this.queueName = ctx.getQueueName();
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
        if (msg.isTypeOf(TbMsgType.DEDUPLICATION_TIMEOUT_SELF_MSG)) {
            processDeduplication(ctx, msg.getOriginator());
        } else {
            processOnRegularMsg(ctx, msg);
        }
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        deduplicationMap.clear();
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
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
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
     * 功能：处理`Deduplication`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deduplicationId`：`deduplicationId`ID。
     * 返回：无。
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
     * 功能：执行 `scheduleTickMsg` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deduplicationId`：`deduplicationId`ID。
     * - `data`：待处理数据。
     * 返回：无。
     */
    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId, DeduplicationData data) {
        if (!data.isTickScheduled()) {
            scheduleTickMsg(ctx, deduplicationId);
            data.setTickScheduled(true);
        }
    }

    /**
     * 功能：获取`Valid Pack`。
     * 参数：
     * - `msgs`：待处理消息。
     * - `deduplicationTimeoutMs`：`deduplicationTimeoutMs` 参数。
     * 返回：处理结果。
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
     * 功能：执行 `enqueueForTellNextWithRetry` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `retryAttempt`：`retryAttempt` 参数。
     * 返回：无。
     */
    private void enqueueForTellNextWithRetry(TbContext ctx, TbMsg msg, int retryAttempt) {
        if (config.getMaxRetries() > retryAttempt) {
            ctx.enqueueForTellNext(msg, TbNodeConnectionType.SUCCESS,
                    () -> {
                        log.trace("[{}][{}][{}] Successfully enqueue deduplication result message!", ctx.getSelfId(), msg.getOriginator(), retryAttempt);
                    },
                    throwable -> {
                        log.trace("[{}][{}][{}] Failed to enqueue deduplication output message due to: ", ctx.getSelfId(), msg.getOriginator(), retryAttempt, throwable);
                        ctx.schedule(() -> {
                            enqueueForTellNextWithRetry(ctx, msg, retryAttempt + 1);
                        }, TB_MSG_DEDUPLICATION_RETRY_DELAY, TimeUnit.SECONDS);
                    });
        }
    }

    /**
     * 功能：执行 `scheduleTickMsg` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deduplicationId`：`deduplicationId`ID。
     * 返回：无。
     */
    private void scheduleTickMsg(TbContext ctx, EntityId deduplicationId) {
        ctx.tellSelf(ctx.newMsg(null, TbMsgType.DEDUPLICATION_TIMEOUT_SELF_MSG, deduplicationId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING), deduplicationInterval + 1);
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `msgs`：待处理消息。
     * 返回：文本结果。
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
     * 功能：获取`Metadata`。
     * 参数：无。
     * 返回：处理结果。
     */
    private TbMsgMetaData getMetadata() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", String.valueOf(System.currentTimeMillis()));
        return metaData;
    }
}
