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
 * 1. `PartitionService` 是 ThingsBoard Common Queue 中定义分区能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
