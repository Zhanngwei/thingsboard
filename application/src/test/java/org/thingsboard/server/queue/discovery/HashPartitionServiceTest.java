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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
/**
 * 中文说明：
 * 1. 类目的：`HashPartitionServiceTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
public class HashPartitionServiceTest {

    /**
     * 字段说明：
     * 1. 保存 `ITERATIONS` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final int ITERATIONS = 1000000;
    public static final int SERVER_COUNT = 3;
    /**
     * 字段说明：
     * 1. 保存 `partitionService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private HashPartitionService partitionService;

    /**
     * 字段说明：
     * 1. 保存 `serviceInfoProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbServiceInfoProvider serviceInfoProvider;
    private TenantRoutingInfoService routingInfoService;
    /**
     * 字段说明：
     * 1. 保存 `applicationEventPublisher` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ApplicationEventPublisher applicationEventPublisher;
    private QueueRoutingInfoService queueRoutingInfoService;
    /**
     * 字段说明：
     * 1. 保存 `topicService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TopicService topicService;

    /**
     * 字段说明：
     * 1. 保存 `hashFunctionName` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String hashFunctionName = "murmur3_128";

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `setup` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setup() throws Exception {
        serviceInfoProvider = mock(TbServiceInfoProvider.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        routingInfoService = mock(TenantRoutingInfoService.class);
        queueRoutingInfoService = mock(QueueRoutingInfoService.class);
        topicService = mock(TopicService.class);
        when(topicService.buildTopicName(Mockito.any())).thenAnswer(i -> i.getArguments()[0]);
        partitionService = createPartitionService();
        ServiceInfo currentServer = ServiceInfo.newBuilder()
                .setServiceId("tb-core-0")
                .addAllServiceTypes(Collections.singletonList(ServiceType.TB_CORE.name()))
                .build();
        List<ServiceInfo> otherServers = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 1; i < SERVER_COUNT; i++) {
            otherServers.add(ServiceInfo.newBuilder()
                    .setServiceId("tb-rule-" + i)
                    .addAllServiceTypes(Collections.singletonList(ServiceType.TB_CORE.name()))
                    .build());
        }

        partitionService.recalculatePartitions(currentServer, otherServers);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDispersionOnMillionDevices` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDispersionOnMillionDevices() {
        List<DeviceId> devices = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < ITERATIONS; i++) {
            devices.add(new DeviceId(Uuids.timeBased()));
        }
        testDevicesDispersion(devices);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `testDevicesDispersion` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testDevicesDispersion(List<DeviceId> devices) {
        long start = System.currentTimeMillis();
        Map<Integer, Integer> map = new HashMap<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (DeviceId deviceId : devices) {
            TopicPartitionInfo address = partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, deviceId);
            Integer partition = address.getPartition().get();
            map.put(partition, map.getOrDefault(partition, 0) + 1);
        }

        checkDispersion(start, map, ITERATIONS, 5.0);
    }

    @SneakyThrows
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDispersionOnResolveByPartitionIdx` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDispersionOnResolveByPartitionIdx() {
        int serverCount = 5;
        int tenantCount = 1000;
        int queueCount = 3;
        int partitionCount = 3;

        List<ServiceInfo> services = new ArrayList<>();

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < serverCount; i++) {
            services.add(ServiceInfo.newBuilder().setServiceId("RE-" + i).build());
        }

        long start = System.currentTimeMillis();
        Map<String, Integer> map = new HashMap<>();
        services.forEach(s -> map.put(s.getServiceId(), 0));

        Random random = new Random();
        long ts = new SimpleDateFormat("dd-MM-yyyy").parse("06-12-2016").getTime() - TimeUnit.DAYS.toMillis(tenantCount);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int tenantIndex = 0; tenantIndex < tenantCount; tenantIndex++) {
            TenantId tenantId = new TenantId(Uuids.startOf(ts));
            ts += TimeUnit.DAYS.toMillis(1) + random.nextInt(1000);
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (int queueIndex = 0; queueIndex < queueCount; queueIndex++) {
                QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, "queue" + queueIndex, tenantId);
                // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                for (int partition = 0; partition < partitionCount; partition++) {
                    ServiceInfo serviceInfo = partitionService.resolveByPartitionIdx(services, queueKey, partition, Collections.emptyMap());
                    String serviceId = serviceInfo.getServiceId();
                    map.put(serviceId, map.get(serviceId) + 1);
                }
            }
        }

        checkDispersion(start, map, tenantCount * queueCount * partitionCount, 10.0);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkDispersion` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <T> void checkDispersion(long start, Map<T, Integer> map, int iterations, double maxDiffPercent) {
        List<Map.Entry<T, Integer>> data = map.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
        long end = System.currentTimeMillis();
        double ideal = ((double) iterations) / map.size();
        double diff = Math.max(data.get(data.size() - 1).getValue() - ideal, ideal - data.get(0).getValue());
        double diffPercent = (diff / ideal) * 100.0;
        System.out.println("Time: " + (end - start) + " Diff: " + diff + "(" + String.format("%f", diffPercent) + "%)");
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Map.Entry<T, Integer> entry : data) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        Assert.assertTrue(diffPercent < maxDiffPercent);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testPartitionsAssignmentWithDedicatedServers` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testPartitionsAssignmentWithDedicatedServers() {
        int isolatedProfilesCount = 5;
        int tenantsCountPerProfile = 100;
        int dedicatedServerSetsCount = 3;
        int serversCountPerSet = 3;
        int profilesPerSet = (int) Math.ceil((double) isolatedProfilesCount / dedicatedServerSetsCount);

        List<TenantProfileId> isolatedTenantProfiles = Stream.generate(() -> new TenantProfileId(UUID.randomUUID()))
                .limit(isolatedProfilesCount).collect(Collectors.toList());
        Map<TenantId, TenantProfileId> tenants = new HashMap<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (TenantProfileId tenantProfileId : isolatedTenantProfiles) {
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (int i = 0; i < tenantsCountPerProfile; i++) {
                tenants.put(new TenantId(UUID.randomUUID()), tenantProfileId);
            }
        }

        List<Queue> queues = new ArrayList<>();
        queues.add(createQueue(TenantId.SYS_TENANT_ID, 10));
        tenants.forEach((tenantId, profileId) -> {
            queues.add(createQueue(tenantId, 2));
            mockRoutingInfo(tenantId, profileId, true);
        });
        mockQueues(queues);

        List<ServiceInfo> ruleEngines = new ArrayList<>();
        Map<TenantProfileId, List<ServiceInfo>> dedicatedServers = new HashMap<>();
        int serviceId = 0;
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < serversCountPerSet; i++) {
            ServiceInfo commonServer = ServiceInfo.newBuilder()
                    .setServiceId("tb-rule-engine-" + serviceId)
                    .addAllServiceTypes(List.of(ServiceType.TB_RULE_ENGINE.name()))
                    .build();
            ruleEngines.add(commonServer);
            serviceId++;
        }
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < dedicatedServerSetsCount; i++) {
            List<TenantProfileId> assignedProfiles = ListUtils.partition(isolatedTenantProfiles, profilesPerSet).get(i);
            for (int j = 0; j < serversCountPerSet; j++) {
                ServiceInfo dedicatedServer = ServiceInfo.newBuilder()
                        .setServiceId("tb-rule-engine-" + serviceId)
                        .addAllServiceTypes(List.of(ServiceType.TB_RULE_ENGINE.name()))
                        .addAllAssignedTenantProfiles(assignedProfiles.stream().map(UUIDBased::toString).collect(Collectors.toList()))
                        .build();
                ruleEngines.add(dedicatedServer);
                serviceId++;

                for (TenantProfileId assignedProfileId : assignedProfiles) {
                    dedicatedServers.computeIfAbsent(assignedProfileId, p -> new ArrayList<>()).add(dedicatedServer);
                }
            }
        }

        Map<QueueKey, Map<ServiceInfo, List<Integer>>> serversPartitions = new HashMap<>();
        when(serviceInfoProvider.isService(eq(ServiceType.TB_RULE_ENGINE))).thenReturn(true);
        partitionService.init();
        for (ServiceInfo ruleEngine : ruleEngines) {
            List<ServiceInfo> other = new ArrayList<>(ruleEngines);
            other.removeIf(serviceInfo -> serviceInfo.getServiceId().equals(ruleEngine.getServiceId()));

            partitionService.recalculatePartitions(ruleEngine, other);
            partitionService.myPartitions.forEach((queueKey, partitions) -> {
                serversPartitions.computeIfAbsent(queueKey, k -> new HashMap<>()).put(ruleEngine, partitions);
            });

            Set<UUID> assignedTenantProfiles = ruleEngine.getAssignedTenantProfilesList().stream().map(UUID::fromString).collect(Collectors.toSet());
            when(serviceInfoProvider.getAssignedTenantProfiles()).thenReturn(assignedTenantProfiles);
            if (assignedTenantProfiles.isEmpty()) {
                assertThat(partitionService.isManagedByCurrentService(TenantId.SYS_TENANT_ID)).isTrue();
                tenants.forEach((tenantId, tenantProfileId) -> {
                    assertThat(partitionService.isManagedByCurrentService(tenantId)).isFalse();
                });
            } else {
                assertThat(partitionService.isManagedByCurrentService(TenantId.SYS_TENANT_ID)).isFalse();
                tenants.forEach((tenantId, tenantProfileId) -> {
                    if (assignedTenantProfiles.contains(tenantProfileId.getId())) {
                        assertThat(partitionService.isManagedByCurrentService(tenantId)).isTrue();
                    } else {
                        assertThat(partitionService.isManagedByCurrentService(tenantId)).isFalse();
                    }
                });
            }
        }
        assertThat(serversPartitions.keySet()).containsAll(queues.stream().map(queue -> new QueueKey(ServiceType.TB_RULE_ENGINE, queue)).collect(Collectors.toList()));

        serversPartitions.forEach((queueKey, partitionsPerServer) -> {
            if (queueKey.getTenantId().isSysTenantId()) {
                partitionsPerServer.forEach((server, partitions) -> {
                    assertThat(server.getAssignedTenantProfilesCount()).as("system queues are not assigned to dedicated servers").isZero();
                });
            } else {
                List<ServiceInfo> responsibleServers = dedicatedServers.get(tenants.get(queueKey.getTenantId()));
                partitionsPerServer.forEach((server, partitions) -> {
                    assertThat(server.getAssignedTenantProfilesCount()).as("isolated queues are only assigned to dedicated servers").isPositive();
                    assertThat(responsibleServers).contains(server);
                });
            }

            List<Integer> allPartitions = partitionsPerServer.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            assertThat(allPartitions).doesNotHaveDuplicates();
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testPartitionChangeEvents_isolatedProfile_oneCommonServer_oneDedicated` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testPartitionChangeEvents_isolatedProfile_oneCommonServer_oneDedicated() {
        ServiceInfo commonRuleEngine = ServiceInfo.newBuilder()
                .setServiceId("tb-rule-engine-1")
                .addAllServiceTypes(List.of(ServiceType.TB_RULE_ENGINE.name()))
                .build();
        TenantProfileId tenantProfileId = new TenantProfileId(UUID.randomUUID());
        ServiceInfo dedicatedRuleEngine = ServiceInfo.newBuilder()
                .setServiceId("tb-rule-engine-isolated-1")
                .addAllServiceTypes(List.of(ServiceType.TB_RULE_ENGINE.name()))
                .addAssignedTenantProfiles(tenantProfileId.toString())
                .build();

        List<Queue> queues = new ArrayList<>();
        Queue systemQueue = createQueue(TenantId.SYS_TENANT_ID, 10);
        queues.add(systemQueue);

        TenantId tenantId = new TenantId(UUID.randomUUID());
        mockRoutingInfo(tenantId, tenantProfileId, false); // not isolated yet
        mockQueues(queues);

        when(serviceInfoProvider.isService(eq(ServiceType.TB_RULE_ENGINE))).thenReturn(true);
        Mockito.reset(applicationEventPublisher);
        HashPartitionService partitionService_common = createPartitionService();
        partitionService_common.recalculatePartitions(commonRuleEngine, List.of(dedicatedRuleEngine));
        verifyPartitionChangeEvent(event -> {
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, TenantId.SYS_TENANT_ID);
            return event.getPartitionsMap().get(queueKey).size() == systemQueue.getPartitions();
        });

        Mockito.reset(applicationEventPublisher);
        HashPartitionService partitionService_dedicated = createPartitionService();
        partitionService_dedicated.recalculatePartitions(dedicatedRuleEngine, List.of(commonRuleEngine));
        verify(applicationEventPublisher, never()).publishEvent(any(PartitionChangeEvent.class));


        Queue isolatedQueue = createQueue(tenantId, 3);
        queues.add(isolatedQueue);
        mockQueues(queues);
        mockRoutingInfo(tenantId, tenantProfileId, true); // making isolated
        TransportProtos.QueueUpdateMsg queueUpdateMsg = TransportProtos.QueueUpdateMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setQueueIdMSB(isolatedQueue.getUuidId().getMostSignificantBits())
                .setQueueIdLSB(isolatedQueue.getUuidId().getLeastSignificantBits())
                .setQueueName(isolatedQueue.getName())
                .setQueueTopic(isolatedQueue.getTopic())
                .setPartitions(isolatedQueue.getPartitions())
                .build();

        partitionService_common.updateQueues(List.of(queueUpdateMsg));
        partitionService_common.recalculatePartitions(commonRuleEngine, List.of(dedicatedRuleEngine));
        // expecting event about no partitions for isolated queue key
        verifyPartitionChangeEvent(event -> {
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, tenantId);
            return event.getPartitionsMap().get(queueKey).isEmpty();
        });

        partitionService_dedicated.updateQueues(List.of(queueUpdateMsg));
        partitionService_dedicated.recalculatePartitions(dedicatedRuleEngine, List.of(commonRuleEngine));
        verifyPartitionChangeEvent(event -> {
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, tenantId);
            return event.getPartitionsMap().get(queueKey).size() == isolatedQueue.getPartitions();
        });


        queues = List.of(systemQueue);
        mockQueues(queues);
        mockRoutingInfo(tenantId, tenantProfileId, false); // turning off isolation
        Mockito.reset(applicationEventPublisher);
        TransportProtos.QueueDeleteMsg queueDeleteMsg = TransportProtos.QueueDeleteMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setQueueIdMSB(isolatedQueue.getUuidId().getMostSignificantBits())
                .setQueueIdLSB(isolatedQueue.getUuidId().getLeastSignificantBits())
                .setQueueName(isolatedQueue.getName())
                .build();
        partitionService_dedicated.removeQueues(List.of(queueDeleteMsg));
        verifyPartitionChangeEvent(event -> {
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, tenantId);
            return event.getPartitionsMap().get(queueKey).isEmpty();
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testPartitionsDistribution_sameTenantDifferentQueues` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testPartitionsDistribution_sameTenantDifferentQueues() {
        List<ServiceInfo> ruleEngines = new ArrayList<>();
        int serviceId = 0;
        for (int i = 0; i < 5; i++) {
            ServiceInfo commonServer = ServiceInfo.newBuilder()
                    .setServiceId("tb-rule-engine-" + serviceId)
                    .addAllServiceTypes(List.of(ServiceType.TB_RULE_ENGINE.name()))
                    .build();
            ruleEngines.add(commonServer);
            serviceId++;
        }

        Stream.concat(Stream.of(TenantId.SYS_TENANT_ID), Stream.generate(UUID::randomUUID).map(TenantId::new).limit(10)).forEach(tenantId -> {
            List<QueueKey> queues = Stream.generate(() -> RandomStringUtils.randomAlphabetic(10))
                    .map(queueName -> new QueueKey(ServiceType.TB_RULE_ENGINE, queueName, tenantId))
                    .limit(100).collect(Collectors.toList());

            for (int partition = 0; partition < 10; partition++) {
                ServiceInfo expectedAssignedRuleEngine = partitionService.resolveByPartitionIdx(ruleEngines, new QueueKey(ServiceType.TB_RULE_ENGINE, tenantId), partition, Collections.emptyMap());
                for (QueueKey queueKey : queues) {
                    ServiceInfo assignedRuleEngine = partitionService.resolveByPartitionIdx(ruleEngines, queueKey, partition, Collections.emptyMap());
                    assertThat(assignedRuleEngine).as(queueKey + "[" + partition + "] should be assigned to " + expectedAssignedRuleEngine.getServiceId())
                            .isEqualTo(expectedAssignedRuleEngine);
                }
            }
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `verifyPartitionChangeEvent` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void verifyPartitionChangeEvent(Predicate<PartitionChangeEvent> predicate) {
        verify(applicationEventPublisher).publishEvent(argThat(event -> event instanceof PartitionChangeEvent && predicate.test((PartitionChangeEvent) event)));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `mockRoutingInfo` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void mockRoutingInfo(TenantId tenantId, TenantProfileId tenantProfileId, boolean isolated) {
        when(routingInfoService.getRoutingInfo(eq(tenantId)))
                .thenReturn(new TenantRoutingInfo(tenantId, tenantProfileId, isolated));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `mockQueues` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void mockQueues(List<Queue> queues) {
        when(queueRoutingInfoService.getAllQueuesRoutingInfo()).thenReturn(queues.stream()
                .map(QueueRoutingInfo::new).collect(Collectors.toList()));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createQueue` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Queue createQueue(TenantId tenantId, int partitions) {
        Queue systemQueue = new Queue();
        systemQueue.setTenantId(tenantId);
        systemQueue.setName("Main");
        systemQueue.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        systemQueue.setPartitions(partitions);
        systemQueue.setId(new QueueId(UUID.randomUUID()));
        return systemQueue;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createPartitionService` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private HashPartitionService createPartitionService() {
        HashPartitionService partitionService = new HashPartitionService(serviceInfoProvider,
                routingInfoService,
                applicationEventPublisher,
                queueRoutingInfoService,
                topicService);
        ReflectionTestUtils.setField(partitionService, "coreTopic", "tb.core");
        ReflectionTestUtils.setField(partitionService, "corePartitions", 10);
        ReflectionTestUtils.setField(partitionService, "vcTopic", "tb.vc");
        ReflectionTestUtils.setField(partitionService, "vcPartitions", 10);
        ReflectionTestUtils.setField(partitionService, "hashFunctionName", hashFunctionName);
        partitionService.init();
        partitionService.partitionsInit();
        return partitionService;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`HashPartitionServiceTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
