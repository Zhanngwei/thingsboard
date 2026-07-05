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
package org.thingsboard.server.queue.discovery;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Once application is ready or cluster topology changes, this Service will produce {@link PartitionChangeEvent}
 */
/**
 * 中文说明：
 * 1. 类目的：`PartitionService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface PartitionService {

    /**
     * 功能：执行 `resolve` 对应的处理。
     * 参数：
     * - `serviceType`：服务对象。
     * - `queueName`：队列名称或队列对象。
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId);

    /**
     * 功能：执行 `resolve` 对应的处理。
     * 参数：
     * - `serviceType`：服务对象。
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId);

    /**
     * 功能：判断分区。
     * 参数：
     * - `serviceType`：服务对象。
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：判断结果。
     */
    boolean isMyPartition(ServiceType serviceType, TenantId tenantId, EntityId entityId);

    /**
     * 功能：获取`My Partitions`。
     * 参数：
     * - `queueKey`：队列名称或队列对象。
     * 返回：匹配的数据集合。
     */
    List<Integer> getMyPartitions(QueueKey queueKey);

    /**
     * Received from the Discovery service when network topology is changed.
     * @param currentService - current service information {@link org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo}
     * @param otherServices - all other discovered services {@link org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo}
     */
    /**
     * 功能：执行 `recalculatePartitions` 对应的处理。
     * 参数：
     * - `currentService`：服务对象。
     * - `otherServices`：服务对象。
     * 返回：无。
     */
    void recalculatePartitions(TransportProtos.ServiceInfo currentService, List<TransportProtos.ServiceInfo> otherServices);

    /**
     * Get all active service ids by service type
     * @param serviceType to filter the list of services
     * @return list of all active services
     */
    /**
     * 功能：获取服务。
     * 参数：
     * - `serviceType`：服务对象。
     * 返回：匹配的数据集合。
     */
    Set<String> getAllServiceIds(ServiceType serviceType);

    /**
     * 功能：获取`All Services`。
     * 参数：
     * - `serviceType`：服务对象。
     * 返回：匹配的数据集合。
     */
    Set<TransportProtos.ServiceInfo> getAllServices(ServiceType serviceType);

    /**
     * 功能：获取`Other Services`。
     * 参数：
     * - `serviceType`：服务对象。
     * 返回：匹配的数据集合。
     */
    Set<TransportProtos.ServiceInfo> getOtherServices(ServiceType serviceType);

    /**
     * 功能：执行 `resolvePartitionIndex` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `partitions`：分区标识或分区信息。
     * 返回：数值结果。
     */
    int resolvePartitionIndex(UUID entityId, int partitions);

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void evictTenantInfo(TenantId tenantId);

    /**
     * 功能：统计类型数量。
     * 参数：
     * - `type`：类型。
     * 返回：数值结果。
     */
    int countTransportsByType(String type);

    /**
     * 功能：更新`Queues`。
     * 参数：
     * - `queueUpdateMsgs`：队列名称或队列对象。
     * 返回：无。
     */
    void updateQueues(List<TransportProtos.QueueUpdateMsg> queueUpdateMsgs);

    /**
     * 功能：删除或清理`Queues`。
     * 参数：
     * - `queueDeleteMsgs`：队列名称或队列对象。
     * 返回：无。
     */
    void removeQueues(List<TransportProtos.QueueDeleteMsg> queueDeleteMsgs);

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void removeTenant(TenantId tenantId);

    /**
     * 功能：判断服务。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    boolean isManagedByCurrentService(TenantId tenantId);

}

/*
 * 本类总结：
 * 1. 核心职责：`PartitionService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
