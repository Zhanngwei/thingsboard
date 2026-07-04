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
 * 周期性生成消息的调试/动作节点，通过自消息驱动定时触发，并异步执行脚本生成下一条消息。
 * 本节点不直接读取业务数据库或缓存；初始化中的实体校验、脚本执行和消息投递分别由 TbContext、ScriptEngine 与 Rule Engine 调用链完成。
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
     * 节点运行配置，包含生成周期、消息数量、来源实体和脚本内容。
     */
    private TbMsgGeneratorNodeConfiguration config;
    /**
     * 用于异步执行 TBEL 或 JavaScript 生成逻辑的脚本引擎。
     */
    private ScriptEngine scriptEngine;
    /**
     * 两次生成之间的延迟毫秒数，由配置中的秒数换算而来。
     */
    private long delay;
    /**
     * 上一次计划触发时间戳，用于尽量按固定节奏调度下一条自消息。
     */
    private long lastScheduledTs;
    /**
     * 当前已经成功或失败处理过的生成次数。
     */
    private int currentMsgCount;
    /**
     * 生成消息使用的来源实体；未配置时使用规则节点自身 ID。
     */
    private EntityId originatorId;
    /**
     * 下一条定时自消息的 ID，用于忽略过期或不匹配的自消息。
     */
    private UUID nextTickId;
    /**
     * 上一次生成出的消息，会作为下一次脚本执行的 prevMsg 输入。
     */
    private TbMsg prevMsg;
    /**
     * 标记本节点是否在当前分区本地负责该 originator，使用原子变量保护初始化和销毁切换。
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    /**
     * 生成消息投递到的队列名称。
     */
    private String queueName;

    /**
     * 初始化消息生成器节点。
     * 本方法解析配置、确定来源实体并调度首次自消息；checkTenantEntity 的数据库或缓存读取由 TbContext 调用链决定，本方法本身不直接操作存储。
     *
     * @param ctx 规则节点上下文
     * @param configuration 节点配置
     * @throws TbNodeException 配置解析或实体校验异常
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
     * 处理分区变更通知。
     * 分区变更可能改变当前节点是否负责 originator，本方法据此初始化或销毁脚本引擎；不直接访问数据库或缓存。
     *
     * @param ctx 规则节点上下文
     * @param msg 分区变更消息
     */
    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        log.debug("[{}] Handling partition change msg: {}", originatorId, msg);
        updateGeneratorState(ctx);
    }

    /**
     * 根据 originator 是否属于本地分区更新生成器状态。
     * 初始化和销毁通过 AtomicBoolean 控制，避免重复创建脚本引擎；Rule Engine 分区调度决定本方法何时被调用。
     *
     * @param ctx 规则节点上下文
     */
    private void updateGeneratorState(TbContext ctx) {
        log.trace("[{}] Updating generator state, config {}", originatorId, config);
        if (ctx.isLocalEntity(originatorId)) {
            if (initialized.compareAndSet(false, true)) {
                // 仅本地负责该 originator 时创建脚本引擎，并由自消息启动周期生成。
                this.scriptEngine = ctx.createScriptEngine(config.getScriptLang(),
                        ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getTbelScript() : config.getJsScript(), "prevMsg", "prevMetadata", "prevMsgType");
                scheduleTickMsg(ctx, null);
            }
        } else if (initialized.compareAndSet(true, false)) {
            // 分区迁出后释放脚本引擎和调度状态，避免非本地节点继续生成消息。
            destroy();
        }
    }

    /**
     * 处理生成器自消息。
     * 只有匹配 nextTickId 的 GENERATOR_NODE_SELF_MSG 才会触发异步脚本生成；成功或失败回调中会继续投递消息并安排下一次自消息。
     *
     * @param ctx 规则节点上下文
     * @param msg 当前自消息
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        log.trace("[{}] onMsg. Expected msg id: {}, msg: {}, config: {}", originatorId, nextTickId, msg, config);
        if (initialized.get() && msg.isTypeOf(TbMsgType.GENERATOR_NODE_SELF_MSG) && msg.getId().equals(nextTickId)) {
            TbStopWatch sw = TbStopWatch.create();
            withCallback(generate(ctx, msg),
                    m -> {
                        // 脚本异步成功回调中继续 Rule Engine 消息流，并在计数未达上限时调度下一次 tick。
                        log.trace("onMsg onSuccess callback, took {}ms, config {}, msg {}", sw.stopAndGetTotalTimeMillis(), config, msg);
                        if (initialized.get() && (config.getMsgCount() == TbMsgGeneratorNodeConfiguration.UNLIMITED_MSG_COUNT || currentMsgCount < config.getMsgCount())) {
                            ctx.enqueueForTellNext(m, TbNodeConnectionType.SUCCESS);
                            scheduleTickMsg(ctx, msg);
                            currentMsgCount++;
                        }
                    },
                    t -> {
                        // 脚本异步失败回调中通知失败链路，同时按相同计数规则决定是否继续调度。
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
     * 创建并调度下一条生成器自消息。
     * 本方法通过 Rule Engine 的 tellSelf 安排后续处理，不直接使用线程睡眠、数据库事务或缓存。
     *
     * @param ctx 规则节点上下文
     * @param msg 当前消息；为空时不设置 customerId
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
     * 异步执行脚本并生成下一条 Rule Engine 消息。
     * 脚本执行返回 ListenableFuture，回调使用直接执行器接续在脚本回调线程上完成；prevMsg 会在回调中更新，线程安全边界依赖 initialized 检查和 Rule Engine/脚本执行器的调用约束。
     *
     * @param ctx 规则节点上下文
     * @param msg 触发生成的自消息
     * @return 生成消息的异步结果
     */
    private ListenableFuture<TbMsg> generate(TbContext ctx, TbMsg msg) {
        log.trace("generate, config {}", config);
        if (prevMsg == null) {
            prevMsg = ctx.newMsg(queueName, TbMsg.EMPTY_STRING, originatorId, msg.getCustomerId(), TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        }
        if (initialized.get()) {
            ctx.logJsEvalRequest();
            return Futures.transformAsync(scriptEngine.executeGenerateAsync(prevMsg), generated -> {
                // 脚本回调只转换生成结果并更新 prevMsg，不读取数据库或缓存。
                log.trace("generate process response, generated {}, config {}", generated, config);
                ctx.logJsEvalResponse();
                prevMsg = ctx.newMsg(queueName, generated.getType(), originatorId, msg.getCustomerId(), generated.getMetaData(), generated.getData());
                return Futures.immediateFuture(prevMsg);
            }, MoreExecutors.directExecutor()); //usually it runs on js-executor-remote-callback thread pool
        }
        return Futures.immediateFuture(prevMsg);

    }

    /**
     * 从当前消息中提取客户 ID。
     * 本方法只读取消息对象字段，不访问数据库、缓存或外部服务。
     *
     * @param msg 当前消息，可为空
     * @return 消息客户 ID 或 null
     */
    private CustomerId getCustomerIdFromMsg(TbMsg msg) {
        return msg != null ? msg.getCustomerId() : null;
    }

    /**
     * 停止生成器并释放脚本引擎。
     * 本方法清理内存状态，不回滚已经投递的 Rule Engine 消息，也不直接处理数据库事务。
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
     * 升级旧版本节点配置。
     * 当前只移除历史 queueName 字段；本方法处理 JSON 配置对象，不访问数据库、缓存或消息队列。
     *
     * @param fromVersion 旧配置版本
     * @param oldConfiguration 待升级配置
     * @return 是否变更以及升级后的配置
     * @throws TbNodeException 升级异常，当前实现不会主动抛出
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
