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
package org.thingsboard.server.dao.dashboard;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`DashboardService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface DashboardService extends EntityDaoService {

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：处理结果。
     */
    Dashboard findDashboardById(TenantId tenantId, DashboardId dashboardId);

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Dashboard> findDashboardByIdAsync(TenantId tenantId, DashboardId dashboardId);

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：处理结果。
     */
    DashboardInfo findDashboardInfoById(TenantId tenantId, DashboardId dashboardId);

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：文本结果。
     */
    String findDashboardTitleById(TenantId tenantId, DashboardId dashboardId);

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(TenantId tenantId, DashboardId dashboardId);

    /**
     * 功能：保存或创建仪表盘。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * - `doValidate`：`doValidate` 参数。
     * 返回：处理结果。
     */
    Dashboard saveDashboard(Dashboard dashboard, boolean doValidate);

    /**
     * 功能：保存或创建仪表盘。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * 返回：处理结果。
     */
    Dashboard saveDashboard(Dashboard dashboard);

    /**
     * 功能：执行 `assignDashboardToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    Dashboard assignDashboardToCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId);

    /**
     * 功能：执行 `unassignDashboardFromCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    Dashboard unassignDashboardFromCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId);

    /**
     * 功能：删除或清理仪表盘。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：无。
     */
    void deleteDashboard(TenantId tenantId, DashboardId dashboardId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<DashboardInfo> findMobileDashboardsByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteDashboardsByTenantId(TenantId tenantId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * 功能：执行 `unassignCustomerDashboards` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    void unassignCustomerDashboards(TenantId tenantId, CustomerId customerId);

    /**
     * 功能：更新客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    void updateCustomerDashboards(TenantId tenantId, CustomerId customerId);

    /**
     * 功能：执行 `assignDashboardToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    Dashboard assignDashboardToEdge(TenantId tenantId, DashboardId dashboardId, EdgeId edgeId);

    /**
     * 功能：执行 `unassignDashboardFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    Dashboard unassignDashboardFromEdge(TenantId tenantId, DashboardId dashboardId, EdgeId edgeId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<DashboardInfo> findDashboardsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    DashboardInfo findFirstDashboardInfoByTenantIdAndName(TenantId tenantId, String name);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `title`：`title` 参数。
     * 返回：匹配的数据集合。
     */
    List<Dashboard> findTenantDashboardsByTitle(TenantId tenantId, String title);

}

/*
 * 本类总结：
 * 1. 核心职责：`DashboardService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
