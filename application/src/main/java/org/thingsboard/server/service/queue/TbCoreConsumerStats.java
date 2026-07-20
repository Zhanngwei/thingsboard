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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文说明：
 * 1. `TbCoreConsumerStats` 是 ThingsBoard Application 中围绕统计数据提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbCoreConsumerStats {
    /**
     * `TOTAL_MSGS`常量，用于统一引用固定值。
     */
    public static final String TOTAL_MSGS = "totalMsgs";
    public static final String SESSION_EVENTS = "sessionEvents";
    /**
     * 属性常量，用于统一引用固定值。
     */
    public static final String GET_ATTRIBUTE = "getAttr";
    public static final String ATTRIBUTE_SUBSCRIBES = "subToAttr";
    /**
     * RPC常量，用于统一引用固定值。
     */
    public static final String RPC_SUBSCRIBES = "subToRpc";
    public static final String TO_DEVICE_RPC_CALL_RESPONSES = "toDevRpc";
    /**
     * 订阅常量，用于统一引用固定值。
     */
    public static final String SUBSCRIPTION_INFO = "subInfo";
    public static final String DEVICE_CLAIMS = "claimDevice";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_STATES = "deviceState";
    public static final String SUBSCRIPTION_MSGS = "subMsgs";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_NOTIFICATIONS = "edgeNfs";
    public static final String DEVICE_CONNECTS = "deviceConnect";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_ACTIVITIES = "deviceActivity";
    public static final String DEVICE_DISCONNECTS = "deviceDisconnect";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_INACTIVITIES = "deviceInactivity";

    /**
     * `TO_CORE_NF_OTHER`常量，用于统一引用固定值。
     */
    public static final String TO_CORE_NF_OTHER = "coreNfOther"; // normally, there is no messages when codebase is fine
    public static final String TO_CORE_NF_COMPONENT_LIFECYCLE = "coreNfCompLfcl";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String TO_CORE_NF_DEVICE_RPC_RESPONSE = "coreNfDevRpcRsp";
    public static final String TO_CORE_NF_EDGE_EVENT_UPDATE = "coreNfEdgeUpd";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String TO_CORE_NF_EDGE_SYNC_REQUEST = "coreNfEdgeSyncReq";
    public static final String TO_CORE_NF_EDGE_SYNC_RESPONSE = "coreNfEdgeSyncResp";
    /**
     * 处理器常量，用于统一引用固定值。
     */
    public static final String TO_CORE_NF_NOTIFICATION_RULE_PROCESSOR = "coreNfNfRlProc";
    public static final String TO_CORE_NF_QUEUE_UPDATE = "coreNfQueueUpd";
    /**
     * 队列常量，用于统一引用固定值。
     */
    public static final String TO_CORE_NF_QUEUE_DELETE = "coreNfQueueDel";
    public static final String TO_CORE_NF_SUBSCRIPTION_SERVICE = "coreNfSubSvc";
    /**
     * 订阅常量，用于统一引用固定值。
     */
    public static final String TO_CORE_NF_SUBSCRIPTION_MANAGER = "coreNfSubMgr";
    public static final String TO_CORE_NF_VC_RESPONSE = "coreNfVCRsp";

    /**
     * 计数器，用于控制处理规模或位置。
     */
    private final StatsCounter totalCounter;
    private final StatsCounter sessionEventCounter;
    /**
     * 计数器，用于控制处理规模或位置。
     */
    private final StatsCounter getAttributesCounter;
    private final StatsCounter subscribeToAttributesCounter;
    /**
     * RPC，表示当前对象的对应属性。
     */
    private final StatsCounter subscribeToRPCCounter;
    private final StatsCounter toDeviceRPCCallResponseCounter;
    /**
     * 订阅，表示当前对象的对应属性。
     */
    private final StatsCounter subscriptionInfoCounter;
    private final StatsCounter claimDeviceCounter;
    /**
     * 设备，表示当前对象所处状态。
     */
    private final StatsCounter deviceStateCounter;
    private final StatsCounter subscriptionMsgCounter;
    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    private final StatsCounter edgeNotificationsCounter;
    private final StatsCounter deviceConnectsCounter;
    /**
     * 设备对象，用于描述当前业务场景。
     */
    private final StatsCounter deviceActivitiesCounter;
    private final StatsCounter deviceDisconnectsCounter;
    /**
     * 设备对象，用于描述当前业务场景。
     */
    private final StatsCounter deviceInactivitiesCounter;

    /**
     * 计数器，用于控制处理规模或位置。
     */
    private final StatsCounter toCoreNfOtherCounter;
    private final StatsCounter toCoreNfComponentLifecycleCounter;
    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    private final StatsCounter toCoreNfDeviceRpcResponseCounter;
    private final StatsCounter toCoreNfEdgeEventUpdateCounter;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private final StatsCounter toCoreNfEdgeSyncRequestCounter;
    private final StatsCounter toCoreNfEdgeSyncResponseCounter;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final StatsCounter toCoreNfNotificationRuleProcessorCounter;
    private final StatsCounter toCoreNfQueueUpdateCounter;
    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    private final StatsCounter toCoreNfQueueDeleteCounter;
    private final StatsCounter toCoreNfSubscriptionServiceCounter;
    /**
     * 订阅，负责处理对应任务或消息。
     */
    private final StatsCounter toCoreNfSubscriptionManagerCounter;
    private final StatsCounter toCoreNfVersionControlResponseCounter;

    private final List<StatsCounter> counters = new ArrayList<>(24);

    /**
     * 功能：创建 `TbCoreConsumerStats` 实例，并初始化必要字段。
     * 参数：
     * - `statsFactory`：`statsFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public TbCoreConsumerStats(StatsFactory statsFactory) {
        String statsKey = StatsType.CORE.getName();

        this.totalCounter = register(statsFactory.createStatsCounter(statsKey, TOTAL_MSGS));
        this.sessionEventCounter = register(statsFactory.createStatsCounter(statsKey, SESSION_EVENTS));
        this.getAttributesCounter = register(statsFactory.createStatsCounter(statsKey, GET_ATTRIBUTE));
        this.subscribeToAttributesCounter = register(statsFactory.createStatsCounter(statsKey, ATTRIBUTE_SUBSCRIBES));
        this.subscribeToRPCCounter = register(statsFactory.createStatsCounter(statsKey, RPC_SUBSCRIBES));
        this.toDeviceRPCCallResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_DEVICE_RPC_CALL_RESPONSES));
        this.subscriptionInfoCounter = register(statsFactory.createStatsCounter(statsKey, SUBSCRIPTION_INFO));
        this.claimDeviceCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_CLAIMS));
        this.deviceStateCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_STATES));
        this.subscriptionMsgCounter = register(statsFactory.createStatsCounter(statsKey, SUBSCRIPTION_MSGS));
        this.edgeNotificationsCounter = register(statsFactory.createStatsCounter(statsKey, EDGE_NOTIFICATIONS));
        this.deviceConnectsCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_CONNECTS));
        this.deviceActivitiesCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_ACTIVITIES));
        this.deviceDisconnectsCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_DISCONNECTS));
        this.deviceInactivitiesCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_INACTIVITIES));

        // Core notification counters
        this.toCoreNfOtherCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_OTHER));
        this.toCoreNfComponentLifecycleCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_COMPONENT_LIFECYCLE));
        this.toCoreNfDeviceRpcResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_DEVICE_RPC_RESPONSE));
        this.toCoreNfEdgeEventUpdateCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_EVENT_UPDATE));
        this.toCoreNfEdgeSyncRequestCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_SYNC_REQUEST));
        this.toCoreNfEdgeSyncResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_SYNC_RESPONSE));
        this.toCoreNfNotificationRuleProcessorCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_NOTIFICATION_RULE_PROCESSOR));
        this.toCoreNfQueueUpdateCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_QUEUE_UPDATE));
        this.toCoreNfQueueDeleteCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_QUEUE_DELETE));
        this.toCoreNfSubscriptionServiceCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_SUBSCRIPTION_SERVICE));
        this.toCoreNfSubscriptionManagerCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_SUBSCRIPTION_MANAGER));
        this.toCoreNfVersionControlResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_VC_RESPONSE));

    }

    /**
     * 功能：执行 `register` 对应的处理。
     * 参数：
     * - `counter`：`counter` 参数。
     * 返回：处理结果。
     */
    private StatsCounter register(StatsCounter counter){
        counters.add(counter);
        return counter;
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void log(TransportProtos.TransportToDeviceActorMsg msg) {
        totalCounter.increment();
        if (msg.hasSessionEvent()) {
            sessionEventCounter.increment();
        }
        if (msg.hasGetAttributes()) {
            getAttributesCounter.increment();
        }
        if (msg.hasSubscribeToAttributes()) {
            subscribeToAttributesCounter.increment();
        }
        if (msg.hasSubscribeToRPC()) {
            subscribeToRPCCounter.increment();
        }
        if (msg.hasToDeviceRPCCallResponse()) {
            toDeviceRPCCallResponseCounter.increment();
        }
        if (msg.hasSubscriptionInfo()) {
            subscriptionInfoCounter.increment();
        }
        if (msg.hasClaimDevice()) {
            claimDeviceCounter.increment();
        }
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void log(TransportProtos.DeviceStateServiceMsgProto msg) {
        totalCounter.increment();
        deviceStateCounter.increment();
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void log(TransportProtos.EdgeNotificationMsgProto msg) {
        totalCounter.increment();
        edgeNotificationsCounter.increment();
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void log(TransportProtos.DeviceConnectProto msg) {
        totalCounter.increment();
        deviceConnectsCounter.increment();
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void log(TransportProtos.DeviceActivityProto msg) {
        totalCounter.increment();
        deviceActivitiesCounter.increment();
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void log(TransportProtos.DeviceDisconnectProto msg) {
        totalCounter.increment();
        deviceDisconnectsCounter.increment();
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void log(TransportProtos.DeviceInactivityProto msg) {
        totalCounter.increment();
        deviceInactivitiesCounter.increment();
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void log(TransportProtos.SubscriptionMgrMsgProto msg) {
        totalCounter.increment();
        subscriptionMsgCounter.increment();
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void log(TransportProtos.ToCoreNotificationMsg msg) {
        totalCounter.increment();
        if (msg.hasToLocalSubscriptionServiceMsg()) {
            toCoreNfSubscriptionServiceCounter.increment();
        } else if (msg.hasFromDeviceRpcResponse()) {
            toCoreNfDeviceRpcResponseCounter.increment();
        } else if (msg.hasComponentLifecycle()) {
            toCoreNfComponentLifecycleCounter.increment();
        } else if (!msg.getComponentLifecycleMsg().isEmpty()) {
            toCoreNfComponentLifecycleCounter.increment();
        } else if (msg.hasEdgeEventUpdate()) {
            toCoreNfEdgeEventUpdateCounter.increment();
        } else if (!msg.getEdgeEventUpdateMsg().isEmpty()) {
            toCoreNfEdgeEventUpdateCounter.increment();
        } else if (msg.hasToEdgeSyncRequest()) {
            toCoreNfEdgeSyncRequestCounter.increment();
        } else if (!msg.getToEdgeSyncRequestMsg().isEmpty()) {
            toCoreNfEdgeSyncRequestCounter.increment();
        } else if (msg.hasFromEdgeSyncResponse()) {
            toCoreNfEdgeSyncResponseCounter.increment();
        } else if (!msg.getFromEdgeSyncResponseMsg().isEmpty()) {
            toCoreNfEdgeSyncResponseCounter.increment();
        } else if (msg.getQueueUpdateMsgsCount() > 0) {
            toCoreNfQueueUpdateCounter.increment();
        } else if (msg.getQueueDeleteMsgsCount() > 0) {
            toCoreNfQueueDeleteCounter.increment();
        } else if (msg.hasVcResponseMsg()) {
            toCoreNfVersionControlResponseCounter.increment();
        } else if (msg.hasToSubscriptionMgrMsg()) {
            toCoreNfSubscriptionManagerCounter.increment();
        } else if (msg.hasNotificationRuleProcessorMsg()) {
            toCoreNfNotificationRuleProcessorCounter.increment();
        } else {
            toCoreNfOtherCounter.increment();
        }
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void printStats() {
        int total = totalCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach(counter -> {
                stats.append(counter.getName()).append(" = [").append(counter.get()).append("] ");
            });
            log.info("Core Stats: {}", stats);
        }
    }

    /**
     * 功能：执行 `reset` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void reset() {
        counters.forEach(StatsCounter::clear);
    }
}
