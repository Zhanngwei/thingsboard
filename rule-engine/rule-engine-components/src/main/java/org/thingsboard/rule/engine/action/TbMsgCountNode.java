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
package org.thingsboard.rule.engine.action;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
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

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：`TbMsgCountNode` 是消息计数节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbMsgCountNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "message count",
        configClazz = TbMsgCountNodeConfiguration.class,
        nodeDescription = "Count incoming messages",
        nodeDetails = "Count incoming messages for specified interval and produces POST_TELEMETRY_REQUEST msg with messages count",
        icon = "functions",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMsgCountConfig"
)
public class TbMsgCountNode implements TbNode {

    /**
     * `messagesProcessed` 字段，保存当前对象的对应属性。
     */
    private AtomicLong messagesProcessed = new AtomicLong(0);
    /**
     * `gson` 字段，保存当前对象的对应属性。
     */
    private final Gson gson = new Gson();
    /**
     * `nextTickId`ID，用于定位对应业务对象。
     */
    private UUID nextTickId;
    /**
     * 延迟时间，用于控制时间范围或等待时长。
     */
    private long delay;
    /**
     * 遥测，表示当前对象的对应属性。
     */
    private String telemetryPrefix;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private long lastScheduledTs;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbMsgCountNodeConfiguration config = TbNodeUtils.convert(configuration, TbMsgCountNodeConfiguration.class);
        this.delay = TimeUnit.SECONDS.toMillis(config.getInterval());
        this.telemetryPrefix = config.getTelemetryPrefix();
        scheduleTickMsg(ctx, null);

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
        if (msg.isTypeOf(TbMsgType.MSG_COUNT_SELF_MSG) && msg.getId().equals(nextTickId)) {
            JsonObject telemetryJson = new JsonObject();
            telemetryJson.addProperty(this.telemetryPrefix + "_" + ctx.getServiceId(), messagesProcessed.longValue());

            messagesProcessed = new AtomicLong(0);

            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("delta", Long.toString(System.currentTimeMillis() - lastScheduledTs + delay));

            TbMsg tbMsg = TbMsg.newMsg(msg.getQueueName(), TbMsgType.POST_TELEMETRY_REQUEST, ctx.getTenantId(), msg.getCustomerId(), metaData, gson.toJson(telemetryJson));
            ctx.enqueueForTellNext(tbMsg, TbNodeConnectionType.SUCCESS);
            scheduleTickMsg(ctx, tbMsg);
        } else {
            messagesProcessed.incrementAndGet();
            ctx.ack(msg);
        }
    }

    /**
     * 功能：执行 `scheduleTickMsg` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void scheduleTickMsg(TbContext ctx, TbMsg msg) {
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs = lastScheduledTs + delay;
        long curDelay = Math.max(0L, (lastScheduledTs - curTs));
        TbMsg tickMsg = ctx.newMsg(null, TbMsgType.MSG_COUNT_SELF_MSG, ctx.getSelfId(), msg != null ? msg.getCustomerId() : null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, curDelay);
    }

    /*
     * 本类总结：`TbMsgCountNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
