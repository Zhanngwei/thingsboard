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
 * 1. `TbMsgProcessingCtx` 是 ThingsBoard Common Message 中承载消息信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
