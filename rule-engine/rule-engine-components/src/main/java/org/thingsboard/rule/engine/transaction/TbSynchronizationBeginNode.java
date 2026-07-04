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
package org.thingsboard.rule.engine.transaction;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * 已废弃的同步开始节点，保留用于兼容旧规则链配置。
 * 本节点不会直接开启、提交或回滚数据库事务，只是在规则链中表达历史上的按 originator 同步处理边界；当前实现直接放行消息。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "synchronization start",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "This Node is now deprecated. Use \"Checkpoint\" instead.",
        nodeDetails = "This node should be used together with \"synchronization end\" node. \n This node will put messages into queue based on message originator id. \n" +
                "Subsequent messages will not be processed until the previous message processing is completed or timeout event occurs.\n" +
                "Size of the queue per originator and timeout values are configurable on a system level",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
@Deprecated
public class TbSynchronizationBeginNode implements TbNode {

    /**
     * 初始化同步开始节点。
     * 当前节点无配置和运行态资源，不读取数据库或缓存，也不建立事务上下文。
     *
     * @param ctx 规则节点上下文
     * @param configuration 空配置
     * @throws TbNodeException 初始化异常，当前实现不会主动抛出
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
    }

    /**
     * 处理进入同步开始节点的消息。
     * 当前实现只记录废弃告警并将消息发送到成功链路；本方法不直接开启数据库事务，具体顺序保证应由队列提交策略等调用链能力提供。
     *
     * @param ctx 规则节点上下文
     * @param msg 待处理消息
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        log.warn("Synchronization Start/End nodes are deprecated since TB 2.5. Use queue with submit strategy SEQUENTIAL_BY_ORIGINATOR instead.");
        ctx.tellSuccess(msg);
    }

}

/*
 * 本类总结：
 * 本类是 deprecated 同步开始节点的兼容实现；它只在规则链语义上表示同步边界，不直接管理数据库事务、缓存、MQTT 或 Actor 调度。
 */
