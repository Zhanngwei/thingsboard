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
 * 1. 类目的：`SnmpTransportBalancingService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`SnmpTransportBalancingService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
