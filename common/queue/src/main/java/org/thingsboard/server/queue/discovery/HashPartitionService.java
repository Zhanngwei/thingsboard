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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.TenantNotFoundException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.event.ServiceListChangedEvent;
import org.thingsboard.server.queue.util.AfterStartUp;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.MAIN_QUEUE_NAME;

/**
 * 中文说明：
 * 1. 类目的：`HashPartitionService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Service
@Slf4j
public class HashPartitionService implements PartitionService {

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    @Value("${queue.core.topic}")
    private String coreTopic;
    /**
     * `corePartitions` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.core.partitions:100}")
    private Integer corePartitions;
    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    @Value("${queue.vc.topic:tb_version_control}")
    private String vcTopic;
    /**
     * `vcPartitions` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.vc.partitions:10}")
    private Integer vcPartitions;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @Value("${queue.partitions.hash_function_name:murmur3_128}")
    private String hashFunctionName;

    /**
     * 事件，表示当前对象的对应属性。
     */
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TbServiceInfoProvider serviceInfoProvider;
    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantRoutingInfoService tenantRoutingInfoService;
    private final QueueRoutingInfoService queueRoutingInfoService;
    /**
     * 主题，提供当前类调用的业务操作。
     */
    private final TopicService topicService;

    protected volatile ConcurrentMap<QueueKey, List<Integer>> myPartitions = new ConcurrentHashMap<>();

    private final ConcurrentMap<QueueKey, String> partitionTopicsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<QueueKey, Integer> partitionSizesMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<TenantId, TenantRoutingInfo> tenantRoutingInfoMap = new ConcurrentHashMap<>();

    /**
     * `currentOtherServices`列表，用于保存一组待处理对象。
     */
    private List<ServiceInfo> currentOtherServices;
    private final Map<String, List<ServiceInfo>> tbTransportServicesByType = new HashMap<>();
    private volatile Map<TenantProfileId, List<ServiceInfo>> responsibleServices = Collections.emptyMap();

    /**
     * `hashFunction` 字段，保存当前对象的对应属性。
     */
    private HashFunction hashFunction;

    /**
     * 功能：创建 `HashPartitionService` 实例，并初始化必要字段。
     * 参数：
     * - `serviceInfoProvider`：服务对象。
     * - `tenantRoutingInfoService`：租户信息或租户标识。
     * - `applicationEventPublisher`：`applicationEventPublisher` 参数。
     * - `queueRoutingInfoService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public HashPartitionService(TbServiceInfoProvider serviceInfoProvider,
                                TenantRoutingInfoService tenantRoutingInfoService,
                                ApplicationEventPublisher applicationEventPublisher,
                                QueueRoutingInfoService queueRoutingInfoService,
                                TopicService topicService) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.tenantRoutingInfoService = tenantRoutingInfoService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.queueRoutingInfoService = queueRoutingInfoService;
        this.topicService = topicService;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        this.hashFunction = forName(hashFunctionName);
        QueueKey coreKey = new QueueKey(ServiceType.TB_CORE);
        partitionSizesMap.put(coreKey, corePartitions);
        partitionTopicsMap.put(coreKey, coreTopic);

        QueueKey vcKey = new QueueKey(ServiceType.TB_VC_EXECUTOR);
        partitionSizesMap.put(vcKey, vcPartitions);
        partitionTopicsMap.put(vcKey, vcTopic);

        if (!isTransport(serviceInfoProvider.getServiceType())) {
            doInitRuleEnginePartitions();
        }
    }

    /**
     * 功能：执行 `partitionsInit` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.QUEUE_INFO_INITIALIZATION)
    public void partitionsInit() {
        if (isTransport(serviceInfoProvider.getServiceType())) {
            doInitRuleEnginePartitions();
        }
    }

    /**
     * 功能：获取`My Partitions`。
     * 参数：
     * - `queueKey`：队列名称或队列对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Integer> getMyPartitions(QueueKey queueKey) {
        return myPartitions.get(queueKey);
    }

    /**
     * 功能：执行 `doInitRuleEnginePartitions` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void doInitRuleEnginePartitions() {
        List<QueueRoutingInfo> queueRoutingInfoList = getQueueRoutingInfos();
        queueRoutingInfoList.forEach(queue -> {
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queue);
            partitionTopicsMap.put(queueKey, queue.getQueueTopic());
            partitionSizesMap.put(queueKey, queue.getPartitions());
        });
    }

    /**
     * 功能：获取队列。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private List<QueueRoutingInfo> getQueueRoutingInfos() {
        List<QueueRoutingInfo> queueRoutingInfoList;
        String serviceType = serviceInfoProvider.getServiceType();

        if (isTransport(serviceType)) {
            //If transport started earlier than tb-core
            int getQueuesRetries = 10;
            while (true) {
                if (getQueuesRetries > 0) {
                    log.info("Try to get queue routing info.");
                    try {
                        queueRoutingInfoList = queueRoutingInfoService.getAllQueuesRoutingInfo();
                        break;
                    } catch (Exception e) {
                        log.info("Failed to get queues routing info: {}!", e.getMessage());
                        getQueuesRetries--;
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        log.info("Failed to await queues routing info!", e);
                    }
                } else {
                    throw new RuntimeException("Failed to await queues routing info!");
                }
            }
        } else {
            queueRoutingInfoList = queueRoutingInfoService.getAllQueuesRoutingInfo();
        }
        return queueRoutingInfoList;
    }

    /**
     * 功能：判断传输层。
     * 参数：
     * - `serviceType`：服务对象。
     * 返回：判断结果。
     */
    private boolean isTransport(String serviceType) {
        return "tb-transport".equals(serviceType);
    }

    /**
     * 功能：更新`Queues`。
     * 参数：
     * - `queueUpdateMsgs`：队列名称或队列对象。
     * 返回：无。
     */
    @Override
    public void updateQueues(List<TransportProtos.QueueUpdateMsg> queueUpdateMsgs) {
        for (TransportProtos.QueueUpdateMsg queueUpdateMsg : queueUpdateMsgs) {
            TenantId tenantId = TenantId.fromUUID(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueUpdateMsg.getQueueName(), tenantId);
            partitionTopicsMap.put(queueKey, queueUpdateMsg.getQueueTopic());
            partitionSizesMap.put(queueKey, queueUpdateMsg.getPartitions());
            if (!tenantId.isSysTenantId()) {
                tenantRoutingInfoMap.remove(tenantId);
            }
        }
    }

    /**
     * 功能：删除或清理`Queues`。
     * 参数：
     * - `queueDeleteMsgs`：队列名称或队列对象。
     * 返回：无。
     */
    @Override
    public void removeQueues(List<TransportProtos.QueueDeleteMsg> queueDeleteMsgs) {
        List<QueueKey> queueKeys = queueDeleteMsgs.stream()
                .map(queueDeleteMsg -> {
                    TenantId tenantId = TenantId.fromUUID(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
                    return new QueueKey(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName(), tenantId);
                })
                .collect(Collectors.toList());
        queueKeys.forEach(queueKey -> {
            myPartitions.remove(queueKey);
            partitionTopicsMap.remove(queueKey);
            partitionSizesMap.remove(queueKey);
            evictTenantInfo(queueKey.getTenantId());
        });
        if (serviceInfoProvider.isService(ServiceType.TB_RULE_ENGINE)) {
            publishPartitionChangeEvent(ServiceType.TB_RULE_ENGINE, queueKeys.stream()
                    .collect(Collectors.toMap(k -> k, k -> Collections.emptySet())));
        }
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void removeTenant(TenantId tenantId) {
        List<QueueKey> queueKeys = partitionSizesMap.keySet().stream()
                .filter(queueKey -> tenantId.equals(queueKey.getTenantId()))
                .collect(Collectors.toList());
        queueKeys.forEach(queueKey -> {
            myPartitions.remove(queueKey);
            partitionTopicsMap.remove(queueKey);
            partitionSizesMap.remove(queueKey);
        });
        evictTenantInfo(tenantId);
    }

    /**
     * 功能：判断服务。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    @Override
    public boolean isManagedByCurrentService(TenantId tenantId) {
        if (serviceInfoProvider.isService(ServiceType.TB_CORE) || !serviceInfoProvider.isService(ServiceType.TB_RULE_ENGINE)) {
            return true;
        }

        boolean isManaged;
        Set<UUID> assignedTenantProfiles = serviceInfoProvider.getAssignedTenantProfiles();
        boolean isRegular = assignedTenantProfiles.isEmpty();
        if (tenantId.isSysTenantId()) {
            // All system queues are always processed on regular rule engines.
            return isRegular;
        }
        TenantRoutingInfo routingInfo = getRoutingInfo(tenantId);
        if (isRegular) {
            if (routingInfo.isIsolated()) {
                isManaged = hasDedicatedService(routingInfo.getProfileId());
            } else {
                isManaged = true;
            }
        } else {
            if (routingInfo.isIsolated()) {
                isManaged = assignedTenantProfiles.contains(routingInfo.getProfileId().getId());
            } else {
                isManaged = false;
            }
        }
        log.trace("[{}] Tenant {} managed by this service", tenantId, isManaged ? "is" : "is not");
        return isManaged;
    }

    /**
     * 功能：判断服务。
     * 参数：
     * - `profileId`：配置ID。
     * 返回：判断结果。
     */
    private boolean hasDedicatedService(TenantProfileId profileId) {
        return CollectionsUtil.isEmpty(responsibleServices.get(profileId));
    }

    /**
     * 功能：执行 `resolve` 对应的处理。
     * 参数：
     * - `serviceType`：服务对象。
     * - `queueName`：队列名称或队列对象。
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId) {
        TenantId isolatedOrSystemTenantId = getIsolatedOrSystemTenantId(serviceType, tenantId);
        if (queueName == null) {
            queueName = MAIN_QUEUE_NAME;
        }
        QueueKey queueKey = new QueueKey(serviceType, queueName, isolatedOrSystemTenantId);
        if (!partitionSizesMap.containsKey(queueKey)) {
            if (isolatedOrSystemTenantId.isSysTenantId()) {
                queueKey = new QueueKey(serviceType, TenantId.SYS_TENANT_ID);
            } else {
                queueKey = new QueueKey(serviceType, queueName, TenantId.SYS_TENANT_ID);
                if (!MAIN_QUEUE_NAME.equals(queueName) && !partitionSizesMap.containsKey(queueKey)) {
                    queueKey = new QueueKey(serviceType, TenantId.SYS_TENANT_ID);
                }
                log.warn("Using queue {} instead of isolated {} for tenant {}", queueKey, queueName, isolatedOrSystemTenantId);
            }
        }
        return resolve(queueKey, entityId);
    }

    /**
     * 功能：执行 `resolve` 对应的处理。
     * 参数：
     * - `serviceType`：服务对象。
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        return resolve(serviceType, null, tenantId, entityId);
    }

    /**
     * 功能：判断分区。
     * 参数：
     * - `serviceType`：服务对象。
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：判断结果。
     */
    @Override
    public boolean isMyPartition(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        try {
            return resolve(serviceType, tenantId, entityId).isMyPartition();
        } catch (TenantNotFoundException e) {
            log.warn("Tenant with id {} not found", tenantId, new RuntimeException("stacktrace"));
            return false;
        }
    }

    /**
     * 功能：执行 `resolve` 对应的处理。
     * 参数：
     * - `queueKey`：队列名称或队列对象。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private TopicPartitionInfo resolve(QueueKey queueKey, EntityId entityId) {
        Integer partitionSize = partitionSizesMap.get(queueKey);
        if (partitionSize == null) {
            throw new IllegalStateException("Partitions info for queue " + queueKey + " is missing");
        }

        int hash = hash(entityId.getId());
        int partition = Math.abs(hash % partitionSize);

        return buildTopicPartitionInfo(queueKey, partition);
    }

    /**
     * 功能：执行 `recalculatePartitions` 对应的处理。
     * 参数：
     * - `currentService`：服务对象。
     * - `otherServices`：服务对象。
     * 返回：无。
     */
    @Override
    public synchronized void recalculatePartitions(ServiceInfo currentService, List<ServiceInfo> otherServices) {
        log.info("Recalculating partitions");
        tbTransportServicesByType.clear();
        logServiceInfo(currentService);
        otherServices.forEach(this::logServiceInfo);

        Map<QueueKey, List<ServiceInfo>> queueServicesMap = new HashMap<>();
        Map<TenantProfileId, List<ServiceInfo>> responsibleServices = new HashMap<>();
        addNode(currentService, queueServicesMap, responsibleServices);
        for (ServiceInfo other : otherServices) {
            addNode(other, queueServicesMap, responsibleServices);
        }
        queueServicesMap.values().forEach(list -> list.sort(Comparator.comparing(ServiceInfo::getServiceId)));
        responsibleServices.values().forEach(list -> list.sort(Comparator.comparing(ServiceInfo::getServiceId)));

        final ConcurrentMap<QueueKey, List<Integer>> newPartitions = new ConcurrentHashMap<>();
        partitionSizesMap.forEach((queueKey, size) -> {
            for (int i = 0; i < size; i++) {
                try {
                    ServiceInfo serviceInfo = resolveByPartitionIdx(queueServicesMap.get(queueKey), queueKey, i, responsibleServices);
                    log.trace("Server responsible for {}[{}] - {}", queueKey, i, serviceInfo != null ? serviceInfo.getServiceId() : "none");
                    if (currentService.equals(serviceInfo)) {
                        newPartitions.computeIfAbsent(queueKey, key -> new ArrayList<>()).add(i);
                    }
                } catch (Exception e) {
                    log.warn("Failed to resolve server responsible for {}[{}]", queueKey, i, e);
                }
            }
        });
        this.responsibleServices = responsibleServices;

        final ConcurrentMap<QueueKey, List<Integer>> oldPartitions = myPartitions;
        myPartitions = newPartitions;

        Map<QueueKey, Set<TopicPartitionInfo>> changedPartitionsMap = new HashMap<>();

        Set<QueueKey> removed = new HashSet<>();
        oldPartitions.forEach((queueKey, partitions) -> {
            if (!newPartitions.containsKey(queueKey)) {
                removed.add(queueKey);
            }
        });
        if (serviceInfoProvider.isService(ServiceType.TB_RULE_ENGINE)) {
            partitionSizesMap.keySet().stream()
                    .filter(queueKey -> queueKey.getType() == ServiceType.TB_RULE_ENGINE &&
                            !queueKey.getTenantId().isSysTenantId() &&
                            !newPartitions.containsKey(queueKey))
                    .forEach(removed::add);
        }
        removed.forEach(queueKey -> {
            changedPartitionsMap.put(queueKey, Collections.emptySet());
        });

        myPartitions.forEach((queueKey, partitions) -> {
            if (!partitions.equals(oldPartitions.get(queueKey))) {
                Set<TopicPartitionInfo> tpiList = partitions.stream()
                        .map(partition -> buildTopicPartitionInfo(queueKey, partition))
                        .collect(Collectors.toSet());
                changedPartitionsMap.put(queueKey, tpiList);
            }
        });
        if (!changedPartitionsMap.isEmpty()) {
            Map<ServiceType, Map<QueueKey, Set<TopicPartitionInfo>>> partitionsByServiceType = new HashMap<>();
            changedPartitionsMap.forEach((queueKey, partitions) -> {
                partitionsByServiceType.computeIfAbsent(queueKey.getType(), serviceType -> new HashMap<>())
                        .put(queueKey, partitions);
            });
            partitionsByServiceType.forEach(this::publishPartitionChangeEvent);
        }

        if (currentOtherServices == null) {
            currentOtherServices = new ArrayList<>(otherServices);
        } else {
            Set<QueueKey> changes = new HashSet<>();
            Map<QueueKey, List<ServiceInfo>> currentMap = getServiceKeyListMap(currentOtherServices);
            Map<QueueKey, List<ServiceInfo>> newMap = getServiceKeyListMap(otherServices);
            currentOtherServices = otherServices;
            currentMap.forEach((key, list) -> {
                if (!list.equals(newMap.get(key))) {
                    changes.add(key);
                }
            });
            currentMap.keySet().forEach(newMap::remove);
            changes.addAll(newMap.keySet());
            if (!changes.isEmpty()) {
                applicationEventPublisher.publishEvent(new ClusterTopologyChangeEvent(this, changes));
                responsibleServices.forEach((profileId, serviceInfos) -> {
                    if (profileId != null) {
                        log.info("Servers responsible for tenant profile {}: {}", profileId, toServiceIds(serviceInfos));
                    } else {
                        log.info("Servers responsible for system queues: {}", toServiceIds(serviceInfos));
                    }
                });
            }
        }

        applicationEventPublisher.publishEvent(new ServiceListChangedEvent(otherServices, currentService));
    }

    /**
     * 功能：发送或提交分区。
     * 参数：
     * - `serviceType`：服务对象。
     * - `partitionsMap`：分区标识或分区信息。
     * 返回：无。
     */
    private void publishPartitionChangeEvent(ServiceType serviceType, Map<QueueKey, Set<TopicPartitionInfo>> partitionsMap) {
        log.info("Partitions changed: {}", System.lineSeparator() + partitionsMap.entrySet().stream()
                .map(entry -> "[" + entry.getKey() + "] - [" + entry.getValue().stream()
                        .map(tpi -> tpi.getPartition().orElse(-1).toString()).sorted()
                        .collect(Collectors.joining(", ")) + "]")
                .collect(Collectors.joining(System.lineSeparator())));
        PartitionChangeEvent event = new PartitionChangeEvent(this, serviceType, partitionsMap);
        try {
            applicationEventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish partition change event {}", event, e);
        }
    }

    /**
     * 功能：获取服务。
     * 参数：
     * - `serviceType`：服务对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<String> getAllServiceIds(ServiceType serviceType) {
        return getAllServices(serviceType).stream().map(ServiceInfo::getServiceId).collect(Collectors.toSet());
    }

    /**
     * 功能：获取`All Services`。
     * 参数：
     * - `serviceType`：服务对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<ServiceInfo> getAllServices(ServiceType serviceType) {
        Set<ServiceInfo> result = getOtherServices(serviceType);
        ServiceInfo current = serviceInfoProvider.getServiceInfo();
        if (current.getServiceTypesList().contains(serviceType.name())) {
            result.add(current);
        }
        return result;
    }

    /**
     * 功能：获取`Other Services`。
     * 参数：
     * - `serviceType`：服务对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<ServiceInfo> getOtherServices(ServiceType serviceType) {
        Set<ServiceInfo> result = new HashSet<>();
        if (currentOtherServices != null) {
            for (ServiceInfo serviceInfo : currentOtherServices) {
                if (serviceInfo.getServiceTypesList().contains(serviceType.name())) {
                    result.add(serviceInfo);
                }
            }
        }
        return result;
    }


    /**
     * 功能：执行 `resolvePartitionIndex` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `partitions`：分区标识或分区信息。
     * 返回：数值结果。
     */
    @Override
    public int resolvePartitionIndex(UUID entityId, int partitions) {
        int hash = hash(entityId);
        return Math.abs(hash % partitions);
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void evictTenantInfo(TenantId tenantId) {
        tenantRoutingInfoMap.remove(tenantId);
    }

    /**
     * 功能：统计类型数量。
     * 参数：
     * - `type`：类型。
     * 返回：数值结果。
     */
    @Override
    public int countTransportsByType(String type) {
        var list = tbTransportServicesByType.get(type);
        return list == null ? 0 : list.size();
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `services`：服务对象。
     * 返回：匹配的数据集合。
     */
    private Map<QueueKey, List<ServiceInfo>> getServiceKeyListMap(List<ServiceInfo> services) {
        final Map<QueueKey, List<ServiceInfo>> currentMap = new HashMap<>();
        services.forEach(serviceInfo -> {
            for (String serviceTypeStr : serviceInfo.getServiceTypesList()) {
                ServiceType serviceType = ServiceType.of(serviceTypeStr);
                if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
                    partitionTopicsMap.keySet().forEach(queueKey ->
                            currentMap.computeIfAbsent(queueKey, key -> new ArrayList<>()).add(serviceInfo));
                } else {
                    QueueKey queueKey = new QueueKey(serviceType);
                    currentMap.computeIfAbsent(queueKey, key -> new ArrayList<>()).add(serviceInfo);
                }
            }
        });
        return currentMap;
    }

    /**
     * 功能：构建主题。
     * 参数：
     * - `queueKey`：队列名称或队列对象。
     * - `partition`：分区标识或分区信息。
     * 返回：处理结果。
     */
    private TopicPartitionInfo buildTopicPartitionInfo(QueueKey queueKey, int partition) {
        TopicPartitionInfo.TopicPartitionInfoBuilder tpi = TopicPartitionInfo.builder();
        tpi.topic(topicService.buildTopicName(partitionTopicsMap.get(queueKey)));
        tpi.partition(partition);
        tpi.tenantId(queueKey.getTenantId());

        List<Integer> partitions = myPartitions.get(queueKey);
        if (partitions != null) {
            tpi.myPartition(partitions.contains(partition));
        } else {
            tpi.myPartition(false);
        }
        return tpi.build();
    }

    /**
     * 功能：判断`Isolated`。
     * 参数：
     * - `serviceType`：服务对象。
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    private boolean isIsolated(ServiceType serviceType, TenantId tenantId) {
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            return false;
        }
        TenantRoutingInfo routingInfo = getRoutingInfo(tenantId);
        if (routingInfo == null) {
            throw new TenantNotFoundException(tenantId);
        }
        switch (serviceType) {
            case TB_RULE_ENGINE:
                return routingInfo.isIsolated();
            default:
                return false;
        }
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    private TenantRoutingInfo getRoutingInfo(TenantId tenantId) {
        return tenantRoutingInfoMap.computeIfAbsent(tenantId, tenantRoutingInfoService::getRoutingInfo);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `serviceType`：服务对象。
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    private TenantId getIsolatedOrSystemTenantId(ServiceType serviceType, TenantId tenantId) {
        return isIsolated(serviceType, tenantId) ? tenantId : TenantId.SYS_TENANT_ID;
    }

    /**
     * 功能：执行 `logServiceInfo` 对应的处理。
     * 参数：
     * - `server`：`server` 参数。
     * 返回：无。
     */
    private void logServiceInfo(TransportProtos.ServiceInfo server) {
        log.info("[{}] Found common server: {}", server.getServiceId(), server.getServiceTypesList());
    }

    /**
     * 功能：保存或创建节点实例。
     * 参数：
     * - `instance`：`instance` 参数。
     * - `queueServiceList`：服务对象。
     * - `responsibleServices`：服务对象。
     * 返回：无。
     */
    private void addNode(ServiceInfo instance, Map<QueueKey, List<ServiceInfo>> queueServiceList, Map<TenantProfileId, List<ServiceInfo>> responsibleServices) {
        for (String serviceTypeStr : instance.getServiceTypesList()) {
            ServiceType serviceType = ServiceType.of(serviceTypeStr);
            if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
                partitionTopicsMap.keySet().forEach(key -> {
                    if (key.getType().equals(ServiceType.TB_RULE_ENGINE)) {
                        queueServiceList.computeIfAbsent(key, k -> new ArrayList<>()).add(instance);
                    }
                });

                if (instance.getAssignedTenantProfilesCount() > 0) {
                    for (String profileIdStr : instance.getAssignedTenantProfilesList()) {
                        TenantProfileId profileId;
                        try {
                            profileId = new TenantProfileId(UUID.fromString(profileIdStr));
                        } catch (IllegalArgumentException e) {
                            log.warn("Failed to parse '{}' as tenant profile id", profileIdStr);
                            continue;
                        }
                        responsibleServices.computeIfAbsent(profileId, k -> new ArrayList<>()).add(instance);
                    }
                }
            } else if (ServiceType.TB_CORE.equals(serviceType) || ServiceType.TB_VC_EXECUTOR.equals(serviceType)) {
                queueServiceList.computeIfAbsent(new QueueKey(serviceType), key -> new ArrayList<>()).add(instance);
            }
        }

        for (String transportType : instance.getTransportsList()) {
            tbTransportServicesByType.computeIfAbsent(transportType, t -> new ArrayList<>()).add(instance);
        }
    }

    /**
     * 功能：执行 `resolveByPartitionIdx` 对应的处理。
     * 参数：
     * - `servers`：数据列表。
     * - `queueKey`：队列名称或队列对象。
     * - `partition`：分区标识或分区信息。
     * - `responsibleServices`：服务对象。
     * 返回：处理结果。
     */
    protected ServiceInfo resolveByPartitionIdx(List<ServiceInfo> servers, QueueKey queueKey, int partition,
                                                Map<TenantProfileId, List<ServiceInfo>> responsibleServices) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        TenantId tenantId = queueKey.getTenantId();
        if (queueKey.getType() == ServiceType.TB_RULE_ENGINE) {
            if (!responsibleServices.isEmpty()) { // if there are any dedicated servers
                TenantProfileId profileId;
                if (tenantId != null && !tenantId.isSysTenantId()) {
                    TenantRoutingInfo routingInfo = tenantRoutingInfoService.getRoutingInfo(tenantId);
                    profileId = routingInfo.getProfileId();
                } else {
                    profileId = null;
                }

                List<ServiceInfo> responsible = responsibleServices.get(profileId);
                if (responsible == null) {
                    // if there are no dedicated servers for this tenant profile, or for system queues,
                    // using the servers that are not responsible for any profile
                    responsible = servers.stream()
                            .filter(serviceInfo -> serviceInfo.getAssignedTenantProfilesCount() == 0)
                            .sorted(Comparator.comparing(ServiceInfo::getServiceId))
                            .collect(Collectors.toList());
                    if (profileId != null) {
                        log.debug("Using servers {} for profile {}", toServiceIds(responsible), profileId);
                    }
                    responsibleServices.put(profileId, responsible);
                }
                if (responsible.isEmpty()) {
                    return null;
                }
                servers = responsible;
            }

            int hash = hash(tenantId.getId());
            return servers.get(Math.abs((hash + partition) % servers.size()));
        } else {
            return servers.get(partition % servers.size());
        }
    }

    /**
     * 功能：执行 `hash` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：数值结果。
     */
    private int hash(UUID key) {
        return hashFunction.newHasher()
                .putLong(key.getMostSignificantBits())
                .putLong(key.getLeastSignificantBits())
                .hash().asInt();
    }

    /**
     * 功能：执行 `forName` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static HashFunction forName(String name) {
        switch (name) {
            case "murmur3_32":
                return Hashing.murmur3_32();
            case "murmur3_128":
                return Hashing.murmur3_128();
            case "sha256":
                return Hashing.sha256();
            default:
                throw new IllegalArgumentException("Can't find hash function with name " + name);
        }
    }

    /**
     * 功能：执行 `toServiceIds` 对应的处理。
     * 参数：
     * - `serviceInfos`：服务对象。
     * 返回：匹配的数据集合。
     */
    private List<String> toServiceIds(Collection<ServiceInfo> serviceInfos) {
        return serviceInfos.stream().map(ServiceInfo::getServiceId).collect(Collectors.toList());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`HashPartitionService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
