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

/**
 * `TbMsgPushToCloudNode` 类，封装当前模块中的一组相关职责。
 */
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
public class TbMsgPushToCloudNode extends AbstractTbMsgPushNode<TbMsgPushToCloudNodeConfiguration, Object, Object> {

    // Implementation of this node is done on the Edge

    /**
     * 功能：构建事件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `eventAction`：`eventAction` 参数。
     * - `entityId`：实体IDID。
     * - `eventType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    Object buildEvent(TenantId tenantId, EdgeEventActionType eventAction, UUID entityId, Object eventType, JsonNode entityBody) {
        return null;
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    @Override
    Object getEventTypeByEntityType(EntityType entityType) {
        return null;
    }

    /**
     * 功能：获取告警。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    Object getAlarmEventType() {
        return null;
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    String getIgnoredMessageSource() {
        return null;
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<TbMsgPushToCloudNodeConfiguration> getConfigClazz() {
        return TbMsgPushToCloudNodeConfiguration.class;
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
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
