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
package org.thingsboard.rule.engine.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.UUID;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "push to cloud",
        configClazz = TbMsgPushToCloudNodeConfiguration.class,
        nodeDescription = "Pushes messages from edge to cloud",
        nodeDetails = "Push messages from edge to cloud. " +
                "This node used only on edge to push messages from edge to cloud. " +
                "Once message arrived into this node it’s going to be converted into cloud event and saved to the local database. " +
                "Node doesn't push messages directly to cloud, but stores event(s) in the cloud queue. " +
                "Supports next message types:" +
                "<br><code>POST_TELEMETRY_REQUEST</code>" +
                "<br><code>POST_ATTRIBUTES_REQUEST</code>" +
                "<br><code>ATTRIBUTES_UPDATED</code>" +
                "<br><code>ATTRIBUTES_DELETED</code>" +
                "<br><code>ALARM</code><br><br>" +
                "Message will be routed via <b>Failure</b> route if node was not able to save cloud event to database or unsupported originator type/message type arrived. " +
                "In case successful storage cloud event to database message will be routed via <b>Success</b> route.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodePushToCloudConfig",
        icon = "cloud_upload",
        ruleChainTypes = RuleChainType.EDGE
)
/**
 * Edge 侧推送到 Cloud 的占位节点，核心实现由 Edge 运行环境提供。
 * 当前类中的方法不直接保存数据库、不访问缓存，也不直接推送到 Cloud。
 */
public class TbMsgPushToCloudNode extends AbstractTbMsgPushNode<TbMsgPushToCloudNodeConfiguration, Object, Object> {

    // Implementation of this node is done on the Edge

    /**
     * Edge 运行环境中的实现应构造 Cloud 事件。
     * 当前占位实现不直接访问数据库、缓存或外部系统。
     */
    @Override
    Object buildEvent(TenantId tenantId, EdgeEventActionType eventAction, UUID entityId, Object eventType, JsonNode entityBody) {
        return null;
    }

    /**
     * Edge 运行环境中的实现应把实体类型映射为 Cloud 事件类型。
     * 当前占位实现不直接涉及外部调用。
     */
    @Override
    Object getEventTypeByEntityType(EntityType entityType) {
        return null;
    }

    /**
     * Edge 运行环境中的实现应返回告警事件类型。
     * 当前占位实现不直接涉及数据库或缓存。
     */
    @Override
    Object getAlarmEventType() {
        return null;
    }

    /**
     * Edge 运行环境中的实现应返回需要忽略的消息来源。
     * 当前占位实现不直接处理消息确认之外的外部边界。
     */
    @Override
    String getIgnoredMessageSource() {
        return null;
    }

    /**
     * 返回 push to cloud 节点配置类。
     * 本方法只用于配置转换，不直接访问数据库或缓存。
     */
    @Override
    protected Class<TbMsgPushToCloudNodeConfiguration> getConfigClazz() {
        return TbMsgPushToCloudNodeConfiguration.class;
    }

    /**
     * Edge 运行环境中的实现应保存 Cloud 事件并路由消息。
     * 当前占位实现为空，不直接涉及外部调用、数据库或缓存。
     */
    @Override
    void processMsg(TbContext ctx, TbMsg msg) {
    }

}

/*
 * 本类总结：
 * 本类是 push to cloud 的占位实现，实际逻辑由 Edge 侧运行环境提供。
 * 当前源码中的方法不直接执行数据库持久化、缓存访问、远端推送或异步回调。
 */
