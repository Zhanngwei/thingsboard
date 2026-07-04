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
package org.thingsboard.rule.engine.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

@Slf4j
@RuleNode(
        type = ComponentType.FLOW,
        name = "checkpoint",
        configClazz = EmptyNodeConfiguration.class,
        version = 1,
        hasQueueName = true,
        nodeDescription = "transfers the message to another queue",
        nodeDetails = "After successful transfer incoming message is automatically acknowledged. Queue name is configurable.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig"
)
/**
 * 中文说明：`TbCheckpointNode` 是检查点节点规则节点，用于控制规则链输入输出、确认和检查点流转。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`EmptyNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbCheckpointNode implements TbNode {

    /**
     * 字段说明：保存 `queueName` 本地缓存、队列或并发状态，用于协调本类处理流程。
     */
    private String queueName;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.queueName = ctx.getQueueName();
    }

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) {
        // 通过规则引擎上下文安排后续消息投递或自身定时消息。
        ctx.enqueueForTellNext(msg, queueName, TbNodeConnectionType.SUCCESS, () -> ctx.ack(msg), error -> ctx.tellFailure(msg, error));
    }

    @Override
    /**
     * 方法说明：迁移旧版本规则节点 JSON 配置结构。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
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

    /*
     * 本类总结：`TbCheckpointNode` 负责控制规则链输入输出、确认和检查点流转；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
