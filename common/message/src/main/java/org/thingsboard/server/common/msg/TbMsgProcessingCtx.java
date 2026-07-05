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
package org.thingsboard.server.common.msg;

import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.gen.MsgProtos;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 13.01.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`TbMsgProcessingCtx` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public final class TbMsgProcessingCtx implements Serializable {

    /**
     * 规则节点对象，用于描述当前业务场景。
     */
    private final AtomicInteger ruleNodeExecCounter;
    private volatile LinkedList<TbMsgProcessingStackItem> stack;

    /**
     * 功能：创建 `TbMsgProcessingCtx` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TbMsgProcessingCtx() {
        this(0);
    }

    /**
     * 功能：创建 `TbMsgProcessingCtx` 实例，并初始化必要字段。
     * 参数：
     * - `ruleNodeExecCounter`：`ruleNodeExecCounter` 参数。
     * 返回：新创建的对象实例。
     */
    public TbMsgProcessingCtx(int ruleNodeExecCounter) {
        this(ruleNodeExecCounter, null);
    }

    /**
     * 功能：创建 `TbMsgProcessingCtx` 实例，并初始化必要字段。
     * 参数：
     * - `ruleNodeExecCounter`：`ruleNodeExecCounter` 参数。
     * - `stack`：数据列表。
     * 返回：新创建的对象实例。
     */
    protected TbMsgProcessingCtx(int ruleNodeExecCounter, LinkedList<TbMsgProcessingStackItem> stack) {
        this.ruleNodeExecCounter = new AtomicInteger(ruleNodeExecCounter);
        this.stack = stack;
    }

    /**
     * 功能：获取规则节点。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getAndIncrementRuleNodeCounter() {
        return ruleNodeExecCounter.getAndIncrement();
    }

    /**
     * 功能：执行 `copy` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public TbMsgProcessingCtx copy() {
        if (stack == null || stack.isEmpty()) {
            return new TbMsgProcessingCtx(ruleNodeExecCounter.get());
        } else {
            return new TbMsgProcessingCtx(ruleNodeExecCounter.get(), new LinkedList<>(stack));
        }
    }

    /**
     * 功能：执行 `push` 对应的处理。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `ruleNodeId`：规则节点ID。
     * 返回：无。
     */
    public void push(RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        if (stack == null) {
            stack = new LinkedList<>();
        }
        stack.add(new TbMsgProcessingStackItem(ruleChainId, ruleNodeId));
    }

    /**
     * 功能：执行 `pop` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public TbMsgProcessingStackItem pop() {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.removeLast();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：处理结果。
     */
    public static TbMsgProcessingCtx fromProto(MsgProtos.TbMsgProcessingCtxProto ctx) {
        int ruleNodeExecCounter = ctx.getRuleNodeExecCounter();
        if (ctx.getStackCount() > 0) {
            LinkedList<TbMsgProcessingStackItem> stack = new LinkedList<>();
            for (MsgProtos.TbMsgProcessingStackItemProto item : ctx.getStackList()) {
                stack.add(TbMsgProcessingStackItem.fromProto(item));
            }
            return new TbMsgProcessingCtx(ruleNodeExecCounter, stack);
        } else {
            return new TbMsgProcessingCtx(ruleNodeExecCounter);
        }
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public MsgProtos.TbMsgProcessingCtxProto toProto() {
        var ctxBuilder = MsgProtos.TbMsgProcessingCtxProto.newBuilder();
        ctxBuilder.setRuleNodeExecCounter(ruleNodeExecCounter.get());
        if (stack != null) {
            for (TbMsgProcessingStackItem item : stack) {
                ctxBuilder.addStack(item.toProto());
            }
        }
        return ctxBuilder.build();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbMsgProcessingCtx` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
