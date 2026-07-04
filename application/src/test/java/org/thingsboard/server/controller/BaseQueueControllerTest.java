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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.service.queue.TbRuleEngineConsumerStats;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.service.stats.DefaultRuleEngineStatisticsService;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.rule-engine.stats.max-error-message-length=100"
})
/**
 * 中文说明：
 * 1. 类目的：`BaseQueueControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class BaseQueueControllerTest extends AbstractControllerTest {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `ruleEngineStatisticsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private RuleEngineStatisticsService ruleEngineStatisticsService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `statsFactory` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private StatsFactory statsFactory;
    @SpyBean
    /**
     * 字段说明：
     * 1. 保存 `timeseriesDao` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TimeseriesDao timeseriesDao;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `assetService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AssetService assetService;

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testQueueWithServiceTypeRE` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testQueueWithServiceTypeRE() throws Exception {
        loginSysAdmin();

        // create queue
        Queue queue = new Queue();
        queue.setName("qwerty");
        queue.setTopic("tb_rule_engine.qwerty");
        queue.setPollInterval(25);
        queue.setPartitions(10);
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setConsumerPerPartition(false);
        queue.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.7);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue.setProcessingStrategy(processingStrategy);

        // create queue
        Queue queue2 = new Queue();
        queue2.setName("qwerty2");
        queue2.setTopic("tb_rule_engine.qwerty2");
        queue2.setPollInterval(25);
        queue2.setPartitions(10);
        queue2.setTenantId(TenantId.SYS_TENANT_ID);
        queue2.setConsumerPerPartition(false);
        queue2.setPackProcessingTimeout(2000);
        submitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue2.setSubmitStrategy(submitStrategy);
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.7);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue2.setProcessingStrategy(processingStrategy);

        Queue savedQueue = doPost("/api/queues?serviceType=" + "TB-RULE-ENGINE", queue, Queue.class);
        Queue savedQueue2 = doPost("/api/queues?serviceType=" + "TB_RULE_ENGINE", queue2, Queue.class);

        PageLink pageLink = new PageLink(10);
        PageData<Queue> pageData;
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB-RULE-ENGINE&", new TypeReference<>() {
        }, pageLink);
        Assert.assertFalse(pageData.getData().isEmpty());
        doDelete("/api/queues/" + savedQueue.getUuidId())
                .andExpect(status().isOk());

        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {
        }, pageLink);
        Assert.assertFalse(pageData.getData().isEmpty());
        doDelete("/api/queues/" + savedQueue2.getUuidId())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testQueueStatsTtl` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testQueueStatsTtl() throws ThingsboardException {
        Queue queue = new Queue();
        queue.setName("Test-1");
        queue.setTenantId(TenantId.SYS_TENANT_ID);

        TbRuleEngineProcessingResult testProcessingResult = Mockito.mock(TbRuleEngineProcessingResult.class);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg = new TbProtoQueueMsg<>(UUID.randomUUID(),
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                TransportProtos.ToRuleEngineMsg.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .build());
        when(testProcessingResult.getSuccessMap()).thenReturn(Stream.generate(() -> msg)
                .limit(5).collect(Collectors.toConcurrentMap(m -> UUID.randomUUID(), m -> m)));
        when(testProcessingResult.getFailedMap()).thenReturn(Stream.generate(() -> msg)
                .limit(5).collect(Collectors.toConcurrentMap(m -> UUID.randomUUID(), m -> m)));
        when(testProcessingResult.getPendingMap()).thenReturn(new ConcurrentHashMap<>());
        RuleEngineException ruleEngineException = new RuleEngineException("Test Exception");
        when(testProcessingResult.getExceptionsMap()).thenReturn(new ConcurrentHashMap<>(Map.of(
                tenantId, ruleEngineException
        )));

        TbRuleEngineConsumerStats testStats = new TbRuleEngineConsumerStats(new QueueKey(ServiceType.TB_RULE_ENGINE, queue), statsFactory);
        testStats.log(testProcessingResult, true);

        int queueStatsTtlDays = 14;
        int ruleEngineExceptionsTtlDays = 7;
        updateDefaultTenantProfileConfig(profileConfiguration -> {
            profileConfiguration.setQueueStatsTtlDays(queueStatsTtlDays);
            profileConfiguration.setRuleEngineExceptionsTtlDays(ruleEngineExceptionsTtlDays);
        });
        ruleEngineStatisticsService.reportQueueStats(System.currentTimeMillis(), testStats);

        Asset serviceAsset = assetService.findAssetsByTenantIdAndType(tenantId, TB_SERVICE_QUEUE, new PageLink(100)).getData()
                .stream().filter(asset -> asset.getName().startsWith(queue.getName()))
                .findFirst().get();

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(timeseriesDao).save(eq(tenantId), eq(serviceAsset.getId()), argThat(tsKvEntry -> {
            return tsKvEntry.getKey().equals(TbRuleEngineConsumerStats.SUCCESSFUL_MSGS) &&
                    tsKvEntry.getLongValue().get().equals(5L);
        }), ttlCaptor.capture());
        verify(timeseriesDao).save(eq(tenantId), eq(serviceAsset.getId()), argThat(tsKvEntry -> {
            return tsKvEntry.getKey().equals(TbRuleEngineConsumerStats.FAILED_MSGS) &&
                    tsKvEntry.getLongValue().get().equals(5L);
        }), ttlCaptor.capture());
        assertThat(ttlCaptor.getAllValues()).allSatisfy(usedTtl -> {
            assertThat(usedTtl).isEqualTo(TimeUnit.DAYS.toSeconds(queueStatsTtlDays));
        });

        verify(timeseriesDao).save(eq(tenantId), eq(serviceAsset.getId()), argThat(tsKvEntry -> {
            return tsKvEntry.getKey().equals(DefaultRuleEngineStatisticsService.RULE_ENGINE_EXCEPTION) &&
                    tsKvEntry.getJsonValue().get().equals(ruleEngineException.toJsonString(0));
        }), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(TimeUnit.DAYS.toSeconds(ruleEngineExceptionsTtlDays));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testRuleEngineExceptionTruncation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testRuleEngineExceptionTruncation() {
        Queue queue = new Queue();
        queue.setName("Test-2");
        queue.setTenantId(TenantId.SYS_TENANT_ID);

        TbRuleEngineProcessingResult testProcessingResult = Mockito.mock(TbRuleEngineProcessingResult.class);
        when(testProcessingResult.getSuccessMap()).thenReturn(new ConcurrentHashMap<>());
        when(testProcessingResult.getFailedMap()).thenReturn(new ConcurrentHashMap<>());
        when(testProcessingResult.getPendingMap()).thenReturn(new ConcurrentHashMap<>());

        String largeExceptionMessage = RandomStringUtils.randomAlphabetic(150);
        RuleEngineException ruleEngineException = new RuleEngineException(largeExceptionMessage);
        when(testProcessingResult.getExceptionsMap()).thenReturn(new ConcurrentHashMap<>(Map.of(
                tenantId, ruleEngineException
        )));

        TbRuleEngineConsumerStats testStats = new TbRuleEngineConsumerStats(new QueueKey(ServiceType.TB_RULE_ENGINE, queue), statsFactory);
        testStats.log(testProcessingResult, true);
        ruleEngineStatisticsService.reportQueueStats(System.currentTimeMillis(), testStats);

        AtomicReference<TsKvEntry> reExceptionTsKvEntryCaptor = new AtomicReference<>();
        verify(timeseriesDao).save(eq(tenantId), any(), argThat(tsKvEntry -> {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (tsKvEntry.getKey().equals(DefaultRuleEngineStatisticsService.RULE_ENGINE_EXCEPTION)) {
                reExceptionTsKvEntryCaptor.set(tsKvEntry);
                return true;
            }
            return false;
        }), anyLong());
        TsKvEntry reExceptionTsKvEntry = reExceptionTsKvEntryCaptor.get();

        String finalErrorMessage = JacksonUtil.toJsonNode(reExceptionTsKvEntry.getJsonValue().get()).get("message").asText();
        assertThat(finalErrorMessage).isEqualTo(largeExceptionMessage.substring(0, 100) + "...[truncated 50 symbols]");
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseQueueControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
