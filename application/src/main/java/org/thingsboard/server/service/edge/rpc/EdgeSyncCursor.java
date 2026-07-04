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
package org.thingsboard.server.service.edge.rpc;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.AdminSettingsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AssetProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AssetsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerUsersEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DashboardsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DefaultProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DeviceProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DevicesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EntityViewsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.OtaPackagesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.QueuesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.RuleChainsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetTypesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetsBundlesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantAdminUsersEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantResourcesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetTypesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetsBundlesEdgeEventFetcher;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 中文说明：
 * 1. 类目的：`EdgeSyncCursor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public class EdgeSyncCursor {

    private final List<EdgeEventFetcher> fetchers = new LinkedList<>();

    /**
     * 字段说明：
     * 1. 保存 `currentIdx` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int currentIdx = 0;

    /**
     * 方法说明：
     * 1. 职责：执行 `EdgeSyncCursor` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EdgeSyncCursor(EdgeContextComponent ctx, Edge edge, boolean fullSync) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (fullSync) {
            fetchers.add(new TenantEdgeEventFetcher(ctx.getTenantService()));
            fetchers.add(new QueuesEdgeEventFetcher(ctx.getQueueService()));
            fetchers.add(new RuleChainsEdgeEventFetcher(ctx.getRuleChainService()));
            fetchers.add(new AdminSettingsEdgeEventFetcher(ctx.getAdminSettingsService()));
            fetchers.add(new TenantAdminUsersEdgeEventFetcher(ctx.getUserService()));
            Customer publicCustomer = ctx.getCustomerService().findOrCreatePublicCustomer(edge.getTenantId());
            fetchers.add(new CustomerEdgeEventFetcher(publicCustomer.getId()));
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
                fetchers.add(new CustomerEdgeEventFetcher(edge.getCustomerId()));
                fetchers.add(new CustomerUsersEdgeEventFetcher(ctx.getUserService(), edge.getCustomerId()));
            }
        }
        fetchers.add(new DashboardsEdgeEventFetcher(ctx.getDashboardService()));
        fetchers.add(new DefaultProfilesEdgeEventFetcher(ctx.getDeviceProfileService(), ctx.getAssetProfileService()));
        fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
        fetchers.add(new AssetProfilesEdgeEventFetcher(ctx.getAssetProfileService()));
        fetchers.add(new DevicesEdgeEventFetcher(ctx.getDeviceService()));
        fetchers.add(new AssetsEdgeEventFetcher(ctx.getAssetService()));
        fetchers.add(new EntityViewsEdgeEventFetcher(ctx.getEntityViewService()));
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (fullSync) {
            fetchers.add(new SystemWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new TenantWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new SystemWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new TenantWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new OtaPackagesEdgeEventFetcher(ctx.getOtaPackageService()));
            fetchers.add(new TenantResourcesEdgeEventFetcher(ctx.getResourceService()));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `hasNext` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean hasNext() {
        return fetchers.size() > currentIdx;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getNext` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EdgeEventFetcher getNext() {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        EdgeEventFetcher edgeEventFetcher = fetchers.get(currentIdx);
        currentIdx++;
        return edgeEventFetcher;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCurrentIdx` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public int getCurrentIdx() {
        return currentIdx;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeSyncCursor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
