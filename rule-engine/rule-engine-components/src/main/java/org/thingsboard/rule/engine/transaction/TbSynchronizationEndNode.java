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
 * 已废弃的同步结束节点，保留用于兼容旧规则链配置。
 * 本节点不会直接结束数据库事务，也不会提交或回滚数据；当前实现仅把消息继续发送到成功链路。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "synchronization end",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "This Node is now deprecated. Use \"Checkpoint\" instead.",
        nodeDetails = "",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = ("tbNodeEmptyConfig")
)
@Deprecated
public class TbSynchronizationEndNode implements TbNode {

    /**
     * 初始化同步结束节点。
     * 当前节点没有配置状态，不访问数据库或缓存，也不创建事务资源。
     *
     * @param ctx 规则节点上下文
     * @param configuration 空配置
     * @throws TbNodeException 初始化异常，当前实现不会主动抛出
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
    }

    /**
     * 处理进入同步结束节点的消息。
     * 当前实现只记录废弃告警并向成功链路转发；它只表达旧规则链同步边界，不直接关闭数据库事务。
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
 * 本类是 deprecated 同步结束节点的兼容实现；它不直接涉及数据库事务生命周期，只在 Rule Engine 消息流中继续传递消息。
 */
