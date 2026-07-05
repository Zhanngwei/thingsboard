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
package org.thingsboard.server.service.edge.rpc.processor.edge;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`EdgeProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Slf4j
@Component
@TbCoreComponent
public class EdgeProcessor extends BaseEdgeProcessor {

    /**
     * 功能：转换边缘节点。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * 返回：处理结果。
     */
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent) {
        EdgeId edgeId = new EdgeId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Edge edge = edgeService.findEdgeById(edgeEvent.getTenantId(), edgeId);
                if (edge != null) {
                    EdgeConfiguration edgeConfigMsg =
                            edgeMsgConstructor.constructEdgeConfiguration(edge);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setEdgeConfiguration(edgeConfigMsg)
                            .build();
                }
                break;
        }
        return downlinkMsg;
    }

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeNotificationMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    public ListenableFuture<Void> processEdgeNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        try {
            EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
            EdgeId edgeId = new EdgeId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
            switch (actionType) {
                case ASSIGNED_TO_CUSTOMER:
                    CustomerId customerId = JacksonUtil.fromString(edgeNotificationMsg.getBody(), CustomerId.class);
                    Edge edge = edgeService.findEdgeById(tenantId, edgeId);
                    if (customerId != null && (edge == null || customerId.isNullUid())) {
                        return Futures.immediateFuture(null);
                    }
                    List<ListenableFuture<Void>> futures = new ArrayList<>();
                    futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, customerId, null));
                    futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.EDGE, EdgeEventActionType.ASSIGNED_TO_CUSTOMER, edgeId, null));
                    PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
                    PageData<User> pageData;
                    do {
                        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
                        if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                            log.trace("[{}][{}][{}] user(s) are going to be added to edge.", tenantId, edge.getId(), pageData.getData().size());
                            for (User user : pageData.getData()) {
                                futures.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, EdgeEventActionType.ADDED, user.getId(), null));
                            }
                            if (pageData.hasNext()) {
                                pageLink = pageLink.nextPageLink();
                            }
                        }
                    } while (pageData != null && pageData.hasNext());
                    return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
                case UNASSIGNED_FROM_CUSTOMER:
                    CustomerId customerIdToDelete = JacksonUtil.fromString(edgeNotificationMsg.getBody(), CustomerId.class);
                    edge = edgeService.findEdgeById(tenantId, edgeId);
                    if (customerIdToDelete != null && (edge == null || customerIdToDelete.isNullUid())) {
                        return Futures.immediateFuture(null);
                    }
                    return Futures.transformAsync(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.EDGE, EdgeEventActionType.UNASSIGNED_FROM_CUSTOMER, edgeId, null),
                            voids -> saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.DELETED, customerIdToDelete, null),
                            dbCallbackExecutorService);
                default:
                    return Futures.immediateFuture(null);
            }
        } catch (Exception e) {
            log.error("[{}] Exception during processing edge event", tenantId, e);
            return Futures.immediateFailedFuture(e);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
