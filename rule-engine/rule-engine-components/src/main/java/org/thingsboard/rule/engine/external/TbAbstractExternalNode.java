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
 * 外部系统类规则节点的基础封装，统一处理强制确认模式下的消息确认和后续链路投递。
 * 本类不直接访问数据库、缓存、MQTT 或 Actor；具体外部调用和异步回调由子类实现。
 */
public abstract class TbAbstractExternalNode implements TbNode {

    /**
     * 是否在外部节点中先确认原消息，再使用复制后的新上下文继续 Rule Engine 消息流。
     */
    private boolean forceAck;

    /**
     * 初始化外部节点基础状态。
     * 本方法只从 TbContext 读取运行配置，不直接发起数据库读取、缓存读取或外部系统调用；字段在初始化后由规则节点处理线程读取。
     *
     * @param ctx 规则节点运行上下文
     */
    public void init(TbContext ctx) {
        this.forceAck = ctx.isExternalNodeForceAck();
    }

    /**
     * 将消息发送到成功链路。
     * forceAck 开启时会使用复制的新上下文入队，避免已经 ack 的原消息上下文继续参与 Rule Engine 流转；本方法本身不处理异步回调。
     *
     * @param ctx 规则节点运行上下文
     * @param tbMsg 待转发消息
     */
    protected void tellSuccess(TbContext ctx, TbMsg tbMsg) {
        if (forceAck) {
            // 强制 ack 场景下通过入队复制消息延续规则链，原消息确认由调用方或 ackIfNeeded 完成。
            ctx.enqueueForTellNext(tbMsg.copyWithNewCtx(), TbNodeConnectionType.SUCCESS);
        } else {
            ctx.tellSuccess(tbMsg);
        }
    }

    /**
     * 将消息发送到失败链路或失败处理。
     * forceAck 开启时用复制后的消息上下文入队，避免后续失败回调影响已确认的原消息；数据库、缓存和外部系统异常来源由子类调用链决定。
     *
     * @param ctx 规则节点运行上下文
     * @param tbMsg 待转发消息
     * @param t 失败原因；为空时只走失败关系链路
     */
    protected void tellFailure(TbContext ctx, TbMsg tbMsg, Throwable t) {
        if (forceAck) {
            if (t == null) {
                // 无异常对象时只表达 FAILURE 关系，仍使用复制消息保持强制 ack 的线程安全边界。
                ctx.enqueueForTellNext(tbMsg.copyWithNewCtx(), TbNodeConnectionType.FAILURE);
            } else {
                // 有异常对象时交给 Rule Engine 的失败入队路径处理。
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
     * 按外部节点强制确认策略确认消息，并返回可继续传递的消息实例。
     * 本方法本身不访问数据库或缓存；forceAck 开启时会立即 ack 原消息并返回复制上下文，后续 Rule Engine 消息流应使用返回值。
     *
     * @param ctx 规则节点运行上下文
     * @param msg 原始消息
     * @return 可供后续处理或异步回调继续使用的消息
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
