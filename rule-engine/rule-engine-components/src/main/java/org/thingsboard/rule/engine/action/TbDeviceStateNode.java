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

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.tools.TbRateLimits;

import java.util.EnumSet;
import java.util.Set;

/**
 * 中文说明：`TbDeviceStateNode` 是设备状态节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbDeviceStateNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "device state",
        nodeDescription = "Triggers device connectivity events",
        nodeDetails = "If incoming message originator is a device, registers configured event for that device in the Device State Service, which sends appropriate message to the Rule Engine." +
                " If metadata <code>ts</code> property is present, it will be used as event timestamp. Otherwise, the message timestamp will be used." +
                " If originator entity type is not <code>DEVICE</code> or unexpected error happened during processing, then incoming message is forwarded using <code>Failure</code> chain." +
                " If rate of connectivity events for a given originator is too high, then incoming message is forwarded using <code>Rate limited</code> chain. " +
                "<br>" +
                "Supported device connectivity events are:" +
                "<ul>" +
                "<li>Connect event</li>" +
                "<li>Disconnect event</li>" +
                "<li>Activity event</li>" +
                "<li>Inactivity event</li>" +
                "</ul>" +
                "This node is particularly useful when device isn't using transports to receive data, such as when fetching data from external API or computing new data within the rule chain.",
        configClazz = TbDeviceStateNodeConfiguration.class,
        relationTypes = {TbNodeConnectionType.SUCCESS, TbNodeConnectionType.FAILURE, "Rate limited"},
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeDeviceStateConfig"
)
public class TbDeviceStateNode implements TbNode {

    /**
     * `SUPPORTED_EVENTS`常量，用于统一引用固定值。
     */
    private static final Set<TbMsgType> SUPPORTED_EVENTS = EnumSet.of(
            TbMsgType.CONNECT_EVENT, TbMsgType.ACTIVITY_EVENT, TbMsgType.DISCONNECT_EVENT, TbMsgType.INACTIVITY_EVENT
    );
    /**
     * 配置常量，用于统一引用固定值。
     */
    private static final String DEFAULT_RATE_LIMIT_CONFIG = "1:1,30:60,60:3600";
    private ConcurrentReferenceHashMap<DeviceId, TbRateLimits> rateLimits;
    /**
     * 配置，保存当前对象的配置选项。
     */
    private String rateLimitConfig;
    /**
     * 事件，表示当前对象的对应属性。
     */
    private TbMsgType event;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbMsgType event = TbNodeUtils.convert(configuration, TbDeviceStateNodeConfiguration.class).getEvent();
        if (event == null) {
            throw new TbNodeException("Event cannot be null!", true);
        }
        if (!SUPPORTED_EVENTS.contains(event)) {
            throw new TbNodeException("Unsupported event: " + event, true);
        }
        this.event = event;
        rateLimits = new ConcurrentReferenceHashMap<>();
        String deviceStateNodeRateLimitConfig = ctx.getDeviceStateNodeRateLimitConfig();
        try {
            rateLimitConfig = new TbRateLimits(deviceStateNodeRateLimitConfig).getConfiguration();
        } catch (Exception e) {
            log.error("[{}][{}] Invalid rate limit configuration provided: [{}]. Will use default value [{}].",
                    ctx.getTenantId().getId(), ctx.getSelfId().getId(), deviceStateNodeRateLimitConfig, DEFAULT_RATE_LIMIT_CONFIG, e);
            rateLimitConfig = DEFAULT_RATE_LIMIT_CONFIG;
        }
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
        EntityType originatorEntityType = msg.getOriginator().getEntityType();
        if (!EntityType.DEVICE.equals(originatorEntityType)) {
            ctx.tellFailure(msg, new IllegalArgumentException(
                    "Unsupported originator entity type: [" + originatorEntityType + "]. Only DEVICE entity type is supported."
            ));
            return;
        }
        DeviceId originator = new DeviceId(msg.getOriginator().getId());
        rateLimits.compute(originator, (__, rateLimit) -> {
            if (rateLimit == null) {
                rateLimit = new TbRateLimits(rateLimitConfig);
            }
            boolean isNotRateLimited = rateLimit.tryConsume();
            if (isNotRateLimited) {
                sendEventAndTell(ctx, originator, msg);
            } else {
                ctx.tellNext(msg, "Rate limited");
            }
            return rateLimit;
        });
    }

    /**
     * 功能：发送或提交事件。
     * 参数：
     * - `ctx`：处理上下文。
     * - `originator`：`originator` 参数。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void sendEventAndTell(TbContext ctx, DeviceId originator, TbMsg msg) {
        TenantId tenantId = ctx.getTenantId();
        long eventTs = msg.getMetaDataTs();

        RuleEngineDeviceStateManager deviceStateManager = ctx.getDeviceStateManager();
        TbCallback callback = getMsgEnqueuedCallback(ctx, msg);

        switch (event) {
            case CONNECT_EVENT:
                deviceStateManager.onDeviceConnect(tenantId, originator, eventTs, callback);
                break;
            case ACTIVITY_EVENT:
                deviceStateManager.onDeviceActivity(tenantId, originator, eventTs, callback);
                break;
            case DISCONNECT_EVENT:
                deviceStateManager.onDeviceDisconnect(tenantId, originator, eventTs, callback);
                break;
            case INACTIVITY_EVENT:
                deviceStateManager.onDeviceInactivity(tenantId, originator, eventTs, callback);
                break;
            default:
                ctx.tellFailure(msg, new IllegalStateException("Configured event [" + event + "] is not supported!"));
        }
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private TbCallback getMsgEnqueuedCallback(TbContext ctx, TbMsg msg) {
        return new TbCallback() {
            /**
             * 功能：处理`on Success`。
             * 参数：无。
             * 返回：无。
             */
            @Override
            public void onSuccess() {
                ctx.tellSuccess(msg);
            }

            /**
             * 功能：处理失败信息。
             * 参数：
             * - `t`：`t` 参数。
             * 返回：无。
             */
            @Override
            public void onFailure(Throwable t) {
                ctx.tellFailure(msg, t);
            }
        };
    }

    /**
     * 功能：处理分区。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        rateLimits.entrySet().removeIf(entry -> !ctx.isLocalEntity(entry.getKey()));
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (rateLimits != null) {
            rateLimits.clear();
            rateLimits = null;
        }
        rateLimitConfig = null;
        event = null;
    }

    /*
     * 本类总结：`TbDeviceStateNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
