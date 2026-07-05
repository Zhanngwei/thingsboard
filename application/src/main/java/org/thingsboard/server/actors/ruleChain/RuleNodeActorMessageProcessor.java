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
package org.thingsboard.server.actors.ruleChain;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbRuleNodeUpdateException;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.RuleNodeException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.gen.transport.TransportProtos;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`RuleNodeActorMessageProcessor` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
@Slf4j
public class RuleNodeActorMessageProcessor extends ComponentMsgProcessor<RuleNodeId> {

    /**
     * 规则链，用于标识或展示当前对象。
     */
    private final String ruleChainName;
    private final TbApiUsageReportClient apiUsageClient;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final DefaultTbContext defaultCtx;
    private RuleNode ruleNode;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbNode tbNode;
    private RuleNodeInfo info;

    RuleNodeActorMessageProcessor(TenantId tenantId, String ruleChainName, RuleNodeId ruleNodeId, ActorSystemContext systemContext
            , TbActorRef parent, TbActorRef self) {
        super(systemContext, tenantId, ruleNodeId);
        this.apiUsageClient = systemContext.getApiUsageClient();
        this.ruleChainName = ruleChainName;
        this.ruleNode = systemContext.getRuleChainService().findRuleNodeById(tenantId, entityId);
        this.defaultCtx = new DefaultTbContext(systemContext, ruleChainName, new RuleNodeCtx(tenantId, parent, self, ruleNode));
        this.info = new RuleNodeInfo(ruleNodeId, ruleChainName, ruleNode != null ? ruleNode.getName() : "Unknown");
    }

    /**
     * 功能：执行 `start` 对应的处理。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    @Override
    public void start(TbActorCtx context) throws Exception {
        if (isMyNodePartition()) {
            log.debug("[{}][{}] Starting", tenantId, entityId);
            tbNode = initComponent(ruleNode);
            if (tbNode != null) {
                state = ComponentLifecycleState.ACTIVE;
            }
        }
    }

    /**
     * 功能：处理`on Update`。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    @Override
    public void onUpdate(TbActorCtx context) throws Exception {
        RuleNode newRuleNode = systemContext.getRuleChainService().findRuleNodeById(tenantId, entityId);
        if (isMyNodePartition(newRuleNode)) {
            this.info = new RuleNodeInfo(entityId, ruleChainName, newRuleNode != null ? newRuleNode.getName() : "Unknown");
            boolean restartRequired = state != ComponentLifecycleState.ACTIVE ||
                    !(ruleNode.getType().equals(newRuleNode.getType()) && ruleNode.getConfiguration().equals(newRuleNode.getConfiguration()));
            this.ruleNode = newRuleNode;
            this.defaultCtx.updateSelf(newRuleNode);
            if (restartRequired) {
                if (tbNode != null) {
                    tbNode.destroy();
                }
                try {
                    start(context);
                } catch (Exception e) {
                    throw new TbRuleNodeUpdateException("Failed to update rule node", e);
                }
            }
        } else if (tbNode != null) {
            stop(null);
            tbNode = null;
        }
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：
     * - `context`：处理上下文。
     * 返回：无。
     */
    @Override
    public void stop(TbActorCtx context) {
        log.debug("[{}][{}] Stopping", tenantId, entityId);
        if (tbNode != null) {
            tbNode.destroy();
            state = ComponentLifecycleState.SUSPENDED;
        }
    }

    /**
     * 功能：处理分区。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onPartitionChangeMsg(PartitionChangeMsg msg) throws Exception {
        log.debug("[{}][{}] onPartitionChangeMsg: [{}]", tenantId, entityId, msg);
        if (tbNode != null) {
            if (!isMyNodePartition()) {
                stop(null);
                tbNode = null;
            } else {
                tbNode.onPartitionChangeMsg(defaultCtx, msg);
            }
        } else if (isMyNodePartition()) {
            start(null);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void onRuleToSelfMsg(RuleNodeToSelfMsg msg) throws Exception {
        checkComponentStateActive(msg.getMsg());
        TbMsg tbMsg = msg.getMsg();
        int ruleNodeCount = tbMsg.getAndIncrementRuleNodeCounter();
        int maxRuleNodeExecutionsPerMessage = getTenantProfileConfiguration().getMaxRuleNodeExecsPerMessage();
        if (maxRuleNodeExecutionsPerMessage == 0 || ruleNodeCount < maxRuleNodeExecutionsPerMessage) {
            apiUsageClient.report(tenantId, tbMsg.getCustomerId(), ApiUsageRecordKey.RE_EXEC_COUNT);
            if (ruleNode.isDebugMode()) {
                systemContext.persistDebugInput(tenantId, entityId, msg.getMsg(), "Self");
            }
            try {
                tbNode.onMsg(defaultCtx, msg.getMsg());
            } catch (Exception e) {
                defaultCtx.tellFailure(msg.getMsg(), e);
            }
        } else {
            tbMsg.getCallback().onFailure(new RuleNodeException("Message is processed by more then " + maxRuleNodeExecutionsPerMessage + " rule nodes!", ruleChainName, ruleNode));
        }
    }

    /**
     * 功能：处理规则链。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void onRuleChainToRuleNodeMsg(RuleChainToRuleNodeMsg msg) throws Exception {
        if (!isMyNodePartition()) {
            putToNodePartition(msg.getMsg());
        } else {
            msg.getMsg().getCallback().onProcessingStart(info);
            checkComponentStateActive(msg.getMsg());
            TbMsg tbMsg = msg.getMsg();
            int ruleNodeCount = tbMsg.getAndIncrementRuleNodeCounter();
            int maxRuleNodeExecutionsPerMessage = getTenantProfileConfiguration().getMaxRuleNodeExecsPerMessage();
            if (maxRuleNodeExecutionsPerMessage == 0 || ruleNodeCount < maxRuleNodeExecutionsPerMessage) {
                apiUsageClient.report(tenantId, tbMsg.getCustomerId(), ApiUsageRecordKey.RE_EXEC_COUNT);
                if (ruleNode.isDebugMode()) {
                    systemContext.persistDebugInput(tenantId, entityId, msg.getMsg(), msg.getFromRelationType());
                }
                try {
                    tbNode.onMsg(msg.getCtx(), msg.getMsg());
                } catch (Exception e) {
                    msg.getCtx().tellFailure(msg.getMsg(), e);
                }
            } else {
                tbMsg.getCallback().onFailure(new RuleNodeException("Message is processed by more then " + maxRuleNodeExecutionsPerMessage + " rule nodes!", ruleChainName, ruleNode));
            }
        }
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getComponentName() {
        return ruleNode.getName();
    }

    /**
     * 功能：初始化或启动`Component`。
     * 参数：
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：处理结果。
     */
    private TbNode initComponent(RuleNode ruleNode) throws Exception {
        TbNode tbNode = null;
        if (ruleNode != null) {
            Class<?> componentClazz = Class.forName(ruleNode.getType());
            tbNode = (TbNode) (componentClazz.getDeclaredConstructor().newInstance());
            tbNode.init(defaultCtx, new TbNodeConfiguration(ruleNode.getConfiguration()));
        }
        return tbNode;
    }

    /**
     * 功能：获取`Inactive Exception`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected RuleNodeException getInactiveException() {
        return new RuleNodeException("Rule Node is not active! Failed to initialize.", ruleChainName, ruleNode);
    }

    /**
     * 功能：判断分区。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean isMyNodePartition() {
        return isMyNodePartition(this.ruleNode);
    }

    /**
     * 功能：判断分区。
     * 参数：
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：判断结果。
     */
    private boolean isMyNodePartition(RuleNode ruleNode) {
        boolean result = ruleNode == null || !ruleNode.isSingletonMode()
                || systemContext.getDiscoveryService().isMonolith()
                || defaultCtx.isLocalEntity(ruleNode.getId());
        if (!result) {
            log.trace("[{}][{}] Is not my node partition", tenantId, entityId);
        }
        return result;
    }

    //Message will return after processing. See RuleChainActorMessageProcessor.pushToTarget.
    /**
     * 功能：执行 `putToNodePartition` 对应的处理。
     * 参数：
     * - `source`：`source` 参数。
     * 返回：无。
     */
    private void putToNodePartition(TbMsg source) {
        TbMsg tbMsg = TbMsg.newMsg(source, source.getQueueName(), source.getRuleChainId(), entityId);
        TopicPartitionInfo tpi = systemContext.resolve(ServiceType.TB_RULE_ENGINE, tbMsg.getQueueName(), tenantId, ruleNode.getId());
        TransportProtos.ToRuleEngineMsg toQueueMsg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg))
                .build();
        systemContext.getClusterService().pushMsgToRuleEngine(tpi, tbMsg.getId(), toQueueMsg, null);
        defaultCtx.ack(source);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RuleNodeActorMessageProcessor` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
