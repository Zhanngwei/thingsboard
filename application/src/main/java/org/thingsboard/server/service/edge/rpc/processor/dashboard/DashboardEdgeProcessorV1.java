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
package org.thingsboard.server.service.edge.rpc.processor.dashboard;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`DashboardEdgeProcessorV1` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Component
@TbCoreComponent
public class DashboardEdgeProcessorV1 extends DashboardEdgeProcessor {

    /**
     * 功能：执行 `constructDashboardFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `dashboardUpdateMsg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    protected Dashboard constructDashboardFromUpdateMsg(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setCreatedTime(Uuids.unixTimestamp(dashboardId.getId()));
        dashboard.setTitle(dashboardUpdateMsg.getTitle());
        dashboard.setImage(dashboardUpdateMsg.hasImage() ? dashboardUpdateMsg.getImage() : null);
        dashboard.setConfiguration(JacksonUtil.toJsonNode(dashboardUpdateMsg.getConfiguration()));

        Set<ShortCustomerInfo> assignedCustomers;
        if (dashboardUpdateMsg.hasAssignedCustomers()) {
            assignedCustomers = JacksonUtil.fromString(dashboardUpdateMsg.getAssignedCustomers(), new TypeReference<>() {});
            assignedCustomers = filterNonExistingCustomers(tenantId, assignedCustomers);
            dashboard.setAssignedCustomers(assignedCustomers);
        }
        dashboard.setMobileOrder(dashboardUpdateMsg.hasMobileOrder() ? dashboardUpdateMsg.getMobileOrder() : null);
        dashboard.setMobileHide(dashboardUpdateMsg.getMobileHide());
        return dashboard;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DashboardEdgeProcessorV1` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
