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
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DashboardImportService` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
 */
public class DashboardImportService extends BaseEntityImportService<DashboardId, Dashboard, EntityExportData<Dashboard>> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.DASHBOARD, EntityType.DEVICE, EntityType.ASSET));
    public static final Pattern WIDGET_CONFIG_PROCESSED_FIELDS_PATTERN = Pattern.compile(".*Id.*");

    /**
     * 字段说明：
     * 1. 保存 `dashboardService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final DashboardService dashboardService;


    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `setOwner` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void setOwner(TenantId tenantId, Dashboard dashboard, IdProvider idProvider) {
        dashboard.setTenantId(tenantId);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findExistingEntity` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Dashboard findExistingEntity(EntitiesImportCtx ctx, Dashboard dashboard, IdProvider idProvider) {
        Dashboard existingDashboard = super.findExistingEntity(ctx, dashboard, idProvider);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (existingDashboard == null && ctx.isFindExistingByName()) {
            existingDashboard = dashboardService.findTenantDashboardsByTitle(ctx.getTenantId(), dashboard.getName()).stream().findFirst().orElse(null);
        }
        return existingDashboard;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `prepare` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Dashboard prepare(EntitiesImportCtx ctx, Dashboard dashboard, Dashboard old, EntityExportData<Dashboard> exportData, IdProvider idProvider) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (JsonNode entityAlias : dashboard.getEntityAliasesConfig()) {
            replaceIdsRecursively(ctx, idProvider, entityAlias, Set.of("id"), null, HINTS);
        }
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (JsonNode widgetConfig : dashboard.getWidgetsConfig()) {
            replaceIdsRecursively(ctx, idProvider, JacksonUtil.getSafely(widgetConfig, "config", "actions"), Collections.emptySet(), WIDGET_CONFIG_PROCESSED_FIELDS_PATTERN, HINTS);
        }
        return dashboard;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `saveOrUpdate` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Dashboard saveOrUpdate(EntitiesImportCtx ctx, Dashboard dashboard, EntityExportData<Dashboard> exportData, IdProvider idProvider) {
        var tenantId = ctx.getTenantId();

        Set<ShortCustomerInfo> assignedCustomers = Optional.ofNullable(dashboard.getAssignedCustomers()).orElse(Collections.emptySet()).stream()
                .peek(customerInfo -> customerInfo.setCustomerId(idProvider.getInternalId(customerInfo.getCustomerId())))
                .collect(Collectors.toSet());

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (dashboard.getId() == null) {
            dashboard.setAssignedCustomers(assignedCustomers);
            dashboard = dashboardService.saveDashboard(dashboard);
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (ShortCustomerInfo customerInfo : assignedCustomers) {
                dashboard = dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerInfo.getCustomerId());
            }
        } else {
            Set<CustomerId> existingAssignedCustomers = Optional.ofNullable(dashboardService.findDashboardById(tenantId, dashboard.getId()).getAssignedCustomers())
                    .orElse(Collections.emptySet()).stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());
            Set<CustomerId> newAssignedCustomers = assignedCustomers.stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toSet());

            Set<CustomerId> toUnassign = new HashSet<>(existingAssignedCustomers);
            toUnassign.removeAll(newAssignedCustomers);
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (CustomerId customerId : toUnassign) {
                assignedCustomers = dashboardService.unassignDashboardFromCustomer(tenantId, dashboard.getId(), customerId).getAssignedCustomers();
            }

            Set<CustomerId> toAssign = new HashSet<>(newAssignedCustomers);
            toAssign.removeAll(existingAssignedCustomers);
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (CustomerId customerId : toAssign) {
                assignedCustomers = dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId).getAssignedCustomers();
            }
            dashboard.setAssignedCustomers(assignedCustomers);
            dashboard = dashboardService.saveDashboard(dashboard);
        }
        return dashboard;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deepCopy` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Dashboard deepCopy(Dashboard dashboard) {
        return new Dashboard(dashboard);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `compare` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean compare(EntitiesImportCtx ctx, EntityExportData<Dashboard> exportData, Dashboard prepared, Dashboard existing) {
        return super.compare(ctx, exportData, prepared, existing) || !prepared.getConfiguration().equals(existing.getConfiguration());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getEntityType` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DashboardImportService` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
