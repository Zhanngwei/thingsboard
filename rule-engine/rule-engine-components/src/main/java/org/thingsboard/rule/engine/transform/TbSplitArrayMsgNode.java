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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.concurrent.ExecutionException;

/**
 * 中文说明：`TbSplitArrayMsgNode` 是拆分数组消息节点规则节点，用于转换消息体、元数据、发起实体或拆分/包装规则链消息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`EmptyNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "split array msg",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Split array message into several messages",
        nodeDetails = "Splits an array message into individual elements, with each element sent as a separate message. " +
                "All outbound messages will have the same type and metadata as the original array message.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        icon = "content_copy",
        configDirective = "tbNodeEmptyConfig"
)
public class TbSplitArrayMsgNode implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private EmptyNodeConfiguration config;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        JsonNode jsonNode = JacksonUtil.toJsonNode(msg.getData());
        if (jsonNode.isArray()) {
            ArrayNode data = (ArrayNode) jsonNode;
            if (data.isEmpty()) {
                ctx.ack(msg);
            } else if (data.size() == 1) {
                ctx.tellSuccess(TbMsg.transformMsgData(msg, JacksonUtil.toString(data.get(0))));
            } else {
                TbMsgCallbackWrapper wrapper = new MultipleTbMsgsCallbackWrapper(data.size(), new TbMsgCallback() {
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
                data.forEach(msgNode -> {
                    TbMsg outMsg = TbMsg.transformMsgData(msg, JacksonUtil.toString(msgNode));
                    ctx.enqueueForTellNext(outMsg, TbNodeConnectionType.SUCCESS, wrapper::onSuccess, wrapper::onFailure);
                });
            }
        } else {
            ctx.tellFailure(msg, new RuntimeException("Msg data is not a JSON Array!"));
        }
    }
    /*
     * 本类总结：`TbSplitArrayMsgNode` 负责转换消息体、元数据、发起实体或拆分/包装规则链消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
