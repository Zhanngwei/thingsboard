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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "push to edge",
        configClazz = TbMsgPushToEdgeNodeConfiguration.class,
        nodeDescription = "Push messages from cloud to edge",
        nodeDetails = "Push messages from cloud to edge. " +
                "Message originator must be assigned to particular edge or message originator is <b>EDGE</b> entity itself. " +
                "This node used only on cloud instances to push messages from cloud to edge. " +
                "Once message arrived into this node it’s going to be converted into edge event and saved to the database. " +
                "Node doesn't push messages directly to edge, but stores event(s) in the edge queue. " +
                "Supports next message types:" +
                "<br><code>POST_TELEMETRY_REQUEST</code>" +
                "<br><code>POST_ATTRIBUTES_REQUEST</code>" +
                "<br><code>ATTRIBUTES_UPDATED</code>" +
                "<br><code>ATTRIBUTES_DELETED</code>" +
                "<br><code>ALARM</code><br><br>" +
                "Message will be routed via <b>Failure</b> route if node was not able to save edge event to database or unsupported message type arrived. " +
                "In case successful storage edge event to database message will be routed via <b>Success</b> route.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodePushToEdgeConfig",
        icon = "cloud_download",
        ruleChainTypes = RuleChainType.CORE
)
/**
 * Cloud 侧推送到 Edge 的节点，把消息转换为 EdgeEvent 并保存到 Edge 队列。
 * 本类不直接通过网络推送到 Edge；外部同步由 EdgeEventService 保存后触发的 Edge 通知链路完成。
 */
public class TbMsgPushToEdgeNode extends AbstractTbMsgPushNode<TbMsgPushToEdgeNodeConfiguration, EdgeEvent, EdgeEventType> {

    /**
     * 查询实体关联 Edge 时使用的默认分页大小。
     */
    static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * 构造 EdgeEvent 对象。
     * 本方法只填充本地事件字段，不直接保存数据库或触发远端同步。
     */
    @Override
    EdgeEvent buildEvent(TenantId tenantId, EdgeEventActionType eventAction, UUID entityId,
                         EdgeEventType eventType, JsonNode entityBody) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(eventAction);
        edgeEvent.setEntityId(entityId);
        edgeEvent.setType(eventType);
        edgeEvent.setBody(entityBody);
        return edgeEvent;
    }

    /**
     * 将实体类型映射为 EdgeEventType。
     * 本方法只调用工具类进行本地映射，不直接访问数据库或缓存。
     */
    @Override
    EdgeEventType getEventTypeByEntityType(EntityType entityType) {
        return EdgeUtils.getEdgeEventTypeByEntityType(entityType);
    }

    /**
     * 返回告警消息对应的 EdgeEventType。
     */
    @Override
    EdgeEventType getAlarmEventType() {
        return EdgeEventType.ALARM;
    }

    /**
     * 返回需要忽略的消息来源，避免处理来自 Edge 的回流消息。
     */
    @Override
    String getIgnoredMessageSource() {
        return DataConstants.EDGE_MSG_SOURCE;
    }

    /**
     * 返回 Push to Edge 节点配置类。
     * 本方法只用于配置转换，不直接访问数据库或缓存。
     */
    @Override
    protected Class<TbMsgPushToEdgeNodeConfiguration> getConfigClazz() {
        return TbMsgPushToEdgeNodeConfiguration.class;
    }

    /**
     * 将消息保存为一个或多个 EdgeEvent，并根据保存结果路由消息。
     * 如果 originator 本身是 EDGE，则保存到该 Edge；否则查询关联 Edge 并分别保存。
     * 本方法直接进入 EdgeEventService/EdgeService 调用链，数据库和缓存可能在这些服务内部涉及。
     */
    @Override
    protected void processMsg(TbContext ctx, TbMsg msg) {
        try {
            if (EntityType.EDGE.equals(msg.getOriginator().getEntityType())) {
                EdgeEvent edgeEvent = buildEvent(msg, ctx);
                EdgeId edgeId = new EdgeId(msg.getOriginator().getId());
                ListenableFuture<Void> future = notifyEdge(ctx, edgeEvent, edgeId);
                FutureCallback<Void> futureCallback = new FutureCallback<>() {
                    /**
                     * 单个 EdgeEvent 保存成功后路由 Success。
                     * 本回调运行在 dbCallbackExecutor 上。
                     */
                    @Override
                    public void onSuccess(@Nullable Void result) {
                        ctx.tellSuccess(msg);
                    }

                    /**
                     * 单个 EdgeEvent 保存失败后路由 Failure。
                     * 失败通常来自数据库持久化或 EdgeEventService 调用链。
                     */
                    @Override
                    public void onFailure(Throwable t) {
                        ctx.tellFailure(msg, t);
                    }
                };
                Futures.addCallback(future, futureCallback, ctx.getDbCallbackExecutor());
            } else {
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                PageDataIterableByTenantIdEntityId<EdgeId> edgeIds = new PageDataIterableByTenantIdEntityId<>(
                        ctx.getEdgeService()::findRelatedEdgeIdsByEntityId, ctx.getTenantId(), msg.getOriginator(), DEFAULT_PAGE_SIZE);
                for (EdgeId edgeId : edgeIds) {
                    // 对每个关联 Edge 单独构造并保存事件，全部成功后才走 Success。
                    EdgeEvent edgeEvent = buildEvent(msg, ctx);
                    futures.add(notifyEdge(ctx, edgeEvent, edgeId));
                }

                if (futures.isEmpty()) {
                    // ack in case no edges are related to provided entity
                    ctx.ack(msg);
                } else {
                    Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
                        /**
                         * 所有关联 EdgeEvent 保存成功后路由 Success。
                         */
                        @Override
                        public void onSuccess(@Nullable List<Void> voids) {
                            ctx.tellSuccess(msg);
                        }

                        /**
                         * 任一关联 EdgeEvent 保存失败后路由 Failure。
                         */
                        @Override
                        public void onFailure(Throwable t) {
                            ctx.tellFailure(msg, t);
                        }
                    }, ctx.getDbCallbackExecutor());
                }
            }
        } catch (Exception e) {
            log.error("Failed to build edge event", e);
            ctx.tellFailure(msg, e);
        }
    }

    /**
     * 保存 EdgeEvent 并触发 Edge 更新通知。
     * 本方法直接调用 EdgeEventService.saveAsync，属于数据库/持久化边界；转换回调运行在 dbCallbackExecutor。
     */
    private ListenableFuture<Void> notifyEdge(TbContext ctx, EdgeEvent edgeEvent, EdgeId edgeId) {
        edgeEvent.setEdgeId(edgeId);
        ListenableFuture<Void> future = ctx.getEdgeEventService().saveAsync(edgeEvent);
        return Futures.transform(future, result -> {
            ctx.onEdgeEventUpdate(ctx.getTenantId(), edgeId);
            return null;
        }, ctx.getDbCallbackExecutor());
    }
}

/*
 * 本类总结：
 * 本类把 Cloud 侧消息转换为 EdgeEvent，并保存到一个或多个 Edge 的事件队列。
 * 它不直接向 Edge 发起网络推送；EdgeEventService.saveAsync 和 onEdgeEventUpdate 触发后续同步，数据库/缓存可能在服务调用链中涉及。
 * 成功和失败路由由 dbCallbackExecutor 上的异步回调决定。
 */
