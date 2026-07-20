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
package org.thingsboard.server.transport.snmp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.ServiceListChangedEvent;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.event.SnmpTransportListChangedEvent;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 中文说明：
 * 1. `SnmpTransportBalancingService` 是 ThingsBoard Common Transport 中负责 SNMP 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@TbSnmpTransportComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class SnmpTransportBalancingService {
    /**
     * 分区，提供当前类调用的业务操作。
     */
    private final PartitionService partitionService;
    private final ApplicationEventPublisher eventPublisher;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final SnmpTransportService snmpTransportService;

    /**
     * 数量，用于控制数量、位置或分页范围。
     */
    private int snmpTransportsCount = 1;
    private Integer currentTransportPartitionIndex = 0;

    /**
     * 功能：处理服务。
     * 参数：
     * - `event`：数据列表。
     * 返回：无。
     */
    public void onServiceListChanged(ServiceListChangedEvent event) {
        log.trace("Got service list changed event: {}", event);
        recalculatePartitions(event.getOtherServices(), event.getCurrentService());
    }

    /**
     * 功能：判断传输层。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：判断结果。
     */
    public boolean isManagedByCurrentTransport(UUID entityId) {
        boolean isManaged = resolvePartitionIndexForEntity(entityId) == currentTransportPartitionIndex;
        if (!isManaged) {
            log.trace("Entity {} is not managed by current SNMP transport node", entityId);
        }
        return isManaged;
    }

    /**
     * 功能：执行 `resolvePartitionIndexForEntity` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：数值结果。
     */
    private int resolvePartitionIndexForEntity(UUID entityId) {
        return partitionService.resolvePartitionIndex(entityId, snmpTransportsCount);
    }

    /**
     * 功能：执行 `recalculatePartitions` 对应的处理。
     * 参数：
     * - `otherServices`：服务对象。
     * - `currentService`：服务对象。
     * 返回：无。
     */
    private void recalculatePartitions(List<ServiceInfo> otherServices, ServiceInfo currentService) {
        log.info("Recalculating partitions for SNMP transports");
        List<ServiceInfo> snmpTransports = Stream.concat(otherServices.stream(), Stream.of(currentService))
                .filter(service -> service.getTransportsList().contains(snmpTransportService.getName()))
                .sorted(Comparator.comparing(ServiceInfo::getServiceId))
                .collect(Collectors.toList());
        log.trace("Found SNMP transports: {}", snmpTransports);

        int previousCurrentTransportPartitionIndex = currentTransportPartitionIndex;
        int previousSnmpTransportsCount = snmpTransportsCount;

        if (!snmpTransports.isEmpty()) {
            for (int i = 0; i < snmpTransports.size(); i++) {
                if (snmpTransports.get(i).equals(currentService)) {
                    currentTransportPartitionIndex = i;
                    break;
                }
            }
            snmpTransportsCount = snmpTransports.size();
        }

        if (snmpTransportsCount != previousSnmpTransportsCount || currentTransportPartitionIndex != previousCurrentTransportPartitionIndex) {
            log.info("SNMP transports partitions have changed: transports count = {}, current transport partition index = {}", snmpTransportsCount, currentTransportPartitionIndex);
            eventPublisher.publishEvent(new SnmpTransportListChangedEvent());
        } else {
            log.info("SNMP transports partitions have not changed");
        }
    }

}
