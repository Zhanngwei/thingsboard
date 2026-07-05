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
package org.thingsboard.server.vc.service;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.discovery.TenantRoutingInfo;
import org.thingsboard.server.queue.discovery.TenantRoutingInfoService;

/**
 * 中文说明：
 * 1. 类目的：`VersionControlTenantRoutingInfoService` 是 ThingsBoard MSA 模块 中的版本控制执行器服务类型，用于为独立 VC executor 微服务提供启动入口、租户路由和队列路由信息。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Version Control API、Queue、Spring Boot、gRPC/REST 和 ThingsBoard 集群路由配置。
 * 4. 生命周期：由 Spring Boot 启动为独立进程，随服务注册、队列路由读取和进程关闭而存在。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Service / Routing Strategy。
 */
@Service
public class VersionControlTenantRoutingInfoService implements TenantRoutingInfoService {
    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public TenantRoutingInfo getRoutingInfo(TenantId tenantId) {
        //This dummy implementation is ok since Version Control service does not produce any rule engine messages.
        return new TenantRoutingInfo(tenantId, null, false);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`VersionControlTenantRoutingInfoService` 在 ThingsBoard MSA 模块 中承担版本控制执行器服务类型职责，核心目的是为独立 VC executor 微服务提供启动入口、租户路由和队列路由信息。
 * 2. 核心流程：启动 VC executor 后读取租户和队列路由配置，为版本控制请求选择正确的执行队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control API、Queue、Spring Boot、gRPC/REST 和 ThingsBoard 集群路由配置。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
