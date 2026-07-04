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
package org.thingsboard.rule.engine.delay;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 已废弃的消息延迟节点，通过保存原消息并调度自消息来延后继续投递。
 * 本节点不直接读写数据库或缓存，也不直接开启事务；它会 ack 原队列消息并把待延迟消息保存在节点内存中，因此重启或失败时不保证恢复。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delay (deprecated)",
        configClazz = TbMsgDelayNodeConfiguration.class,
        nodeDescription = "Delays incoming message (deprecated)",
        nodeDetails = "Delays messages for a configurable period. " +
                "Please note, this node acknowledges the message from the current queue (message will be removed from queue). " +
                "Deprecated because the acknowledged message still stays in memory (to be delayed) and this " +
                "does not guarantee that message will be processed even if the \"retry failures and timeouts\" processing strategy will be chosen.",
        icon = "pause",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMsgDelayConfig"
)
public class TbMsgDelayNode implements TbNode {

    /**
     * 延迟节点配置，包含固定延迟、最大挂起数量和元数据模板开关。
     */
    private TbMsgDelayNodeConfiguration config;
    /**
     * 按原消息 ID 保存等待延迟完成的消息；只保存在本节点内存中，不持久化到数据库。
     */
    private Map<UUID, TbMsg> pendingMsgs;

    /**
     * 初始化延迟节点配置和内存挂起表。
     * 本方法不访问数据库或缓存；pendingMsgs 的线程安全边界依赖 Rule Engine 对单节点消息处理的调度约束。
     *
     * @param ctx 规则节点上下文
     * @param configuration 节点配置
     * @throws TbNodeException 配置转换异常
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDelayNodeConfiguration.class);
        this.pendingMsgs = new HashMap<>();
    }

    /**
     * 处理普通消息或延迟超时自消息。
     * 普通消息会被保存到内存并 ack，然后调度一个自消息；超时自消息到达时再把原消息复制后发送到成功链路。
     *
     * @param ctx 规则节点上下文
     * @param msg 当前消息，可能是业务消息或 DELAY_TIMEOUT_SELF_MSG 自消息
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.isTypeOf(TbMsgType.DELAY_TIMEOUT_SELF_MSG)) {
            // 自消息的数据保存原消息 ID，用于从内存挂起表取回待继续处理的消息。
            TbMsg pendingMsg = pendingMsgs.remove(UUID.fromString(msg.getData()));
            if (pendingMsg != null) {
                ctx.enqueueForTellNext(
                        TbMsg.newMsg(
                                pendingMsg.getQueueName(),
                                pendingMsg.getType(),
                                pendingMsg.getOriginator(),
                                pendingMsg.getCustomerId(),
                                pendingMsg.getMetaData(),
                                pendingMsg.getData()
                        ),
                        TbNodeConnectionType.SUCCESS
                );
            }
        } else {
            if (pendingMsgs.size() < config.getMaxPendingMsgs()) {
                // 普通消息先进入内存挂起表；这里没有数据库持久化，节点销毁后挂起消息会丢失。
                pendingMsgs.put(msg.getId(), msg);
                TbMsg tickMsg = ctx.newMsg(null, TbMsgType.DELAY_TIMEOUT_SELF_MSG, ctx.getSelfId(), msg.getCustomerId(), TbMsgMetaData.EMPTY, msg.getId().toString());
                ctx.tellSelf(tickMsg, getDelay(msg));
                // 原队列消息在调度自消息后被确认，后续延迟期间只依赖内存中的 pendingMsgs。
                ctx.ack(msg);
            } else {
                ctx.tellFailure(msg, new RuntimeException("Max limit of pending messages reached!"));
            }
        }
    }

    /**
     * 计算当前消息应延迟的毫秒数。
     * 延迟值可以来自固定配置，也可以由消息元数据模板解析；本方法不读取数据库或缓存。
     *
     * @param msg 用于模板解析的消息
     * @return 延迟毫秒数
     */
    private long getDelay(TbMsg msg) {
        int periodInSeconds;
        if (config.isUseMetadataPeriodInSecondsPatterns()) {
            if (isParsable(msg, config.getPeriodInSecondsPattern())) {
                // 模板会从消息内容或元数据中展开，展开结果必须是可解析数字。
                periodInSeconds = Integer.parseInt(TbNodeUtils.processPattern(config.getPeriodInSecondsPattern(), msg));
            } else {
                throw new RuntimeException("Can't parse period in seconds from metadata using pattern: " + config.getPeriodInSecondsPattern());
            }
        } else {
            periodInSeconds = config.getPeriodInSeconds();
        }
        return TimeUnit.SECONDS.toMillis(periodInSeconds);
    }

    /**
     * 判断模板展开结果是否可解析为数字。
     * 本方法只执行字符串模板处理和数字格式检查，不触发消息投递、数据库访问或缓存访问。
     *
     * @param msg 用于模板展开的消息
     * @param pattern 延迟秒数模板
     * @return true 表示模板结果可解析
     */
    private boolean isParsable(TbMsg msg, String pattern) {
        return NumberUtils.isParsable(TbNodeUtils.processPattern(pattern, msg));
    }

    /**
     * 清理节点内存中挂起的消息。
     * 本方法不补发或持久化未完成消息，只释放内存状态。
     */
    @Override
    public void destroy() {
        pendingMsgs.clear();
    }
}

/*
 * 本类总结：
 * 本类通过 Rule Engine 自消息实现内存级延迟，涉及 ack 和成功链路投递；它不直接涉及数据库事务、缓存或 MQTT，节点停止后 pendingMsgs 中的消息不会被本类恢复。
 */
