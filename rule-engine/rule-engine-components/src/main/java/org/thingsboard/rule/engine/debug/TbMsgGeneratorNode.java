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
package org.thingsboard.rule.engine.debug;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.TbStopWatch;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

/**
 * `TbMsgGeneratorNode` 类，封装当前模块中的一组相关职责。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "generator",
        configClazz = TbMsgGeneratorNodeConfiguration.class,
        version = 1,
        hasQueueName = true,
        nodeDescription = "Periodically generates messages",
        nodeDetails = "Generates messages with configurable period. Javascript function used for message generation.",
        inEnabled = false,
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeGeneratorConfig",
        icon = "repeat"
)

public class TbMsgGeneratorNode implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbMsgGeneratorNodeConfiguration config;
    /**
     * 脚本执行器，表示当前对象的对应属性。
     */
    private ScriptEngine scriptEngine;
    /**
     * 延迟时间，用于控制时间范围或等待时长。
     */
    private long delay;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private long lastScheduledTs;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private int currentMsgCount;
    /**
     * `originatorId`ID，用于定位对应业务对象。
     */
    private EntityId originatorId;
    /**
     * `nextTickId`ID，用于定位对应业务对象。
     */
    private UUID nextTickId;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private TbMsg prevMsg;
    /**
     * 是否满足`initialized`条件。
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    /**
     * 队列名称，用于标识或展示当前对象。
     */
    private String queueName;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgGeneratorNodeConfiguration.class);
        this.delay = TimeUnit.SECONDS.toMillis(config.getPeriodInSeconds());
        this.currentMsgCount = 0;
        this.queueName = ctx.getQueueName();
        if (!StringUtils.isEmpty(config.getOriginatorId())) {
            originatorId = EntityIdFactory.getByTypeAndUuid(config.getOriginatorType(), config.getOriginatorId());
            ctx.checkTenantEntity(originatorId);
        } else {
            originatorId = ctx.getSelfId();
        }
        log.debug("[{}] Initializing generator with config {}", originatorId, configuration);
        updateGeneratorState(ctx);
    }

    /**
     * 功能：处理分区。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        log.debug("[{}] Handling partition change msg: {}", originatorId, msg);
        updateGeneratorState(ctx);
    }

    /**
     * 功能：更新状态。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    private void updateGeneratorState(TbContext ctx) {
        log.trace("[{}] Updating generator state, config {}", originatorId, config);
        if (ctx.isLocalEntity(originatorId)) {
            if (initialized.compareAndSet(false, true)) {
                this.scriptEngine = ctx.createScriptEngine(config.getScriptLang(),
                        ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getTbelScript() : config.getJsScript(), "prevMsg", "prevMetadata", "prevMsgType");
                scheduleTickMsg(ctx, null);
            }
        } else if (initialized.compareAndSet(true, false)) {
            destroy();
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
        log.trace("[{}] onMsg. Expected msg id: {}, msg: {}, config: {}", originatorId, nextTickId, msg, config);
        if (initialized.get() && msg.isTypeOf(TbMsgType.GENERATOR_NODE_SELF_MSG) && msg.getId().equals(nextTickId)) {
            TbStopWatch sw = TbStopWatch.create();
            withCallback(generate(ctx, msg),
                    m -> {
                        log.trace("onMsg onSuccess callback, took {}ms, config {}, msg {}", sw.stopAndGetTotalTimeMillis(), config, msg);
                        if (initialized.get() && (config.getMsgCount() == TbMsgGeneratorNodeConfiguration.UNLIMITED_MSG_COUNT || currentMsgCount < config.getMsgCount())) {
                            ctx.enqueueForTellNext(m, TbNodeConnectionType.SUCCESS);
                            scheduleTickMsg(ctx, msg);
                            currentMsgCount++;
                        }
                    },
                    t -> {
                        log.trace("onMsg onFailure callback, took {}ms, config {}, msg {}", sw.stopAndGetTotalTimeMillis(), config, msg, t);
                        if (initialized.get() && (config.getMsgCount() == TbMsgGeneratorNodeConfiguration.UNLIMITED_MSG_COUNT || currentMsgCount < config.getMsgCount())) {
                            ctx.tellFailure(msg, t);
                            scheduleTickMsg(ctx, msg);
                            currentMsgCount++;
                        }
                    });
        }
    }

    /**
     * 功能：执行 `scheduleTickMsg` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void scheduleTickMsg(TbContext ctx, TbMsg msg) {
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs = lastScheduledTs + delay;
        long curDelay = Math.max(0L, (lastScheduledTs - curTs));
        TbMsg tickMsg = ctx.newMsg(queueName, TbMsgType.GENERATOR_NODE_SELF_MSG, ctx.getSelfId(),
                getCustomerIdFromMsg(msg), TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, curDelay);
        log.trace("[{}] Scheduled tick msg with delay {}, msg: {}, config: {}", originatorId, curDelay, tickMsg, config);
    }

    /**
     * 功能：执行 `generate` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TbMsg> generate(TbContext ctx, TbMsg msg) {
        log.trace("generate, config {}", config);
        if (prevMsg == null) {
            prevMsg = ctx.newMsg(queueName, TbMsg.EMPTY_STRING, originatorId, msg.getCustomerId(), TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        }
        if (initialized.get()) {
            ctx.logJsEvalRequest();
            return Futures.transformAsync(scriptEngine.executeGenerateAsync(prevMsg), generated -> {
                log.trace("generate process response, generated {}, config {}", generated, config);
                ctx.logJsEvalResponse();
                prevMsg = ctx.newMsg(queueName, generated.getType(), originatorId, msg.getCustomerId(), generated.getMetaData(), generated.getData());
                return Futures.immediateFuture(prevMsg);
            }, MoreExecutors.directExecutor()); //usually it runs on js-executor-remote-callback thread pool
        }
        return Futures.immediateFuture(prevMsg);

    }

    /**
     * 功能：获取客户ID。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private CustomerId getCustomerIdFromMsg(TbMsg msg) {
        return msg != null ? msg.getCustomerId() : null;
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        log.debug("[{}] Stopping generator", originatorId);
        initialized.set(false);
        prevMsg = null;
        nextTickId = null;
        lastScheduledTs = 0;
        if (scriptEngine != null) {
            scriptEngine.destroy();
            scriptEngine = null;
        }
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
}

/*
 * 本类总结：
 * 本类用自消息驱动周期生成，并通过 ScriptEngine 的异步 Future 回调把生成结果送入 Rule Engine 成功链路；它不直接读写数据库、缓存、MQTT 或 Actor，线程安全重点在 initialized、nextTickId、prevMsg 与分区归属切换的边界。
 */
