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
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.List;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. `TbAbstractTransformNode` 是 ThingsBoard Rule Engine Components 中处理 `Tb Transform Node` 的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
public abstract class TbAbstractTransformNode<C> implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    protected C config;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = loadNodeConfiguration(ctx, configuration);
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
        withCallback(transform(ctx, msg),
                m -> transformSuccess(ctx, msg, m),
                t -> transformFailure(ctx, msg, t),
                MoreExecutors.directExecutor());
    }

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    protected abstract C loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException;

    /**
     * 功能：转换失败信息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `t`：`t` 参数。
     * 返回：无。
     */
    protected void transformFailure(TbContext ctx, TbMsg msg, Throwable t) {
        ctx.tellFailure(msg, t);
    }

    /**
     * 功能：转换`Success`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `msgs`：待处理消息。
     * 返回：无。
     */
    protected void transformSuccess(TbContext ctx, TbMsg msg, List<TbMsg> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            ctx.tellFailure(msg, new RuntimeException("Message or messages list are empty!"));
        } else if (msgs.size() == 1) {
            ctx.tellSuccess(msgs.get(0));
        } else {
            TbMsgCallbackWrapper wrapper = new MultipleTbMsgsCallbackWrapper(msgs.size(), new TbMsgCallback() {
                /**
                 * 功能：处理`on Success`。
                 * 参数：无。
                 * 返回：无。
                 */
                @Override
                public void onSuccess() {
                    ctx.ack(msg);
                }

                /**
                 * 功能：处理失败信息。
                 * 参数：
                 * - `e`：`e` 参数。
                 * 返回：无。
                 */
                @Override
                public void onFailure(RuleEngineException e) {
                    ctx.tellFailure(msg, e);
                }
            });
            msgs.forEach(newMsg -> ctx.enqueueForTellNext(newMsg, TbNodeConnectionType.SUCCESS, wrapper::onSuccess, wrapper::onFailure));
        }
    }

    /**
     * 功能：执行 `transform` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg);
}
