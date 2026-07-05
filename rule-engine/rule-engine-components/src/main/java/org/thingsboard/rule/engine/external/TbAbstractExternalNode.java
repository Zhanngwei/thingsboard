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
package org.thingsboard.rule.engine.external;

import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * `TbAbstractExternalNode` 类，封装当前模块中的一组相关职责。
 */
public abstract class TbAbstractExternalNode implements TbNode {

    /**
     * 是否满足`forceAck`条件。
     */
    private boolean forceAck;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    public void init(TbContext ctx) {
        this.forceAck = ctx.isExternalNodeForceAck();
    }

    /**
     * 功能：执行 `tellSuccess` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `tbMsg`：待处理消息。
     * 返回：无。
     */
    protected void tellSuccess(TbContext ctx, TbMsg tbMsg) {
        if (forceAck) {
            ctx.enqueueForTellNext(tbMsg.copyWithNewCtx(), TbNodeConnectionType.SUCCESS);
        } else {
            ctx.tellSuccess(tbMsg);
        }
    }

    /**
     * 功能：执行 `tellFailure` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `tbMsg`：待处理消息。
     * - `t`：`t` 参数。
     * 返回：无。
     */
    protected void tellFailure(TbContext ctx, TbMsg tbMsg, Throwable t) {
        if (forceAck) {
            if (t == null) {
                ctx.enqueueForTellNext(tbMsg.copyWithNewCtx(), TbNodeConnectionType.FAILURE);
            } else {
                ctx.enqueueForTellFailure(tbMsg.copyWithNewCtx(), t);
            }
        } else {
            if (t == null) {
                ctx.tellNext(tbMsg, TbNodeConnectionType.FAILURE);
            } else {
                ctx.tellFailure(tbMsg, t);
            }
        }
    }

    /**
     * 功能：执行 `ackIfNeeded` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    protected TbMsg ackIfNeeded(TbContext ctx, TbMsg msg) {
        if (forceAck) {
            ctx.ack(msg);
            return msg.copyWithNewCtx();
        } else {
            return msg;
        }
    }

}

/*
 * 本类总结：
 * 本类为外部节点提供 ack 与成功/失败链路转发的公共逻辑；它不直接实现外部 I/O、数据库事务、缓存读取或 MQTT 交互，线程安全边界主要由初始化后的 forceAck 配置和调用方的消息上下文使用方式决定。
 */
