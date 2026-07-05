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
package org.thingsboard.server.service.queue;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbClusterServiceTest` 是ThingsBoard Application 测试模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTbClusterService.class)
public class DefaultTbClusterServiceTest {

    /**
     * `MONOLITH`常量，用于统一引用固定值。
     */
    public static final String MONOLITH = "monolith";

    /**
     * `CORE`常量，用于统一引用固定值。
     */
    public static final String CORE = "core";

    /**
     * 规则引擎常量，用于统一引用固定值。
     */
    public static final String RULE_ENGINE = "rule_engine";

    /**
     * 传输层常量，用于统一引用固定值。
     */
    public static final String TRANSPORT = "transport";

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected DataDecodingEncodingService encodingService;
    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @MockBean
    protected TbDeviceProfileCache deviceProfileCache;
    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @MockBean
    protected TbAssetProfileCache assetProfileCache;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected GatewayNotificationsService gatewayNotificationsService;
    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @MockBean
    protected EdgeService edgeService;
    /**
     * 分区，提供当前类调用的业务操作。
     */
    @MockBean
    protected PartitionService partitionService;
    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected TbQueueProducerProvider producerProvider;

    /**
     * 主题，提供当前类调用的业务操作。
     */
    @SpyBean
    protected TopicService topicService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @SpyBean
    protected TbClusterService clusterService;

    /**
     * 功能：验证队列相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOnQueueChangeSingleMonolith() {
        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(MONOLITH));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbQueueProducer);

        clusterService.onQueuesUpdate(List.of(createTestQueue()));

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH);
        verify(topicService, never()).getNotificationsTopic(eq(ServiceType.TB_CORE), any());
        verify(topicService, never()).getNotificationsTopic(eq(ServiceType.TB_TRANSPORT), any());

        verify(tbQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(producerProvider, never()).getTbCoreNotificationsMsgProducer();
        verify(producerProvider, never()).getTransportNotificationsMsgProducer();
    }

    /**
     * 功能：验证队列相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOnQueueChangeMultipleMonoliths() {
        String monolith1 = MONOLITH + 1;
        String monolith2 = MONOLITH + 2;
        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(monolith1, monolith2));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(monolith1, monolith2));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(monolith1, monolith2));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbQueueProducer);

        clusterService.onQueuesUpdate(List.of(createTestQueue()));

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2);
        verify(topicService, never()).getNotificationsTopic(eq(ServiceType.TB_CORE), any());
        verify(topicService, never()).getNotificationsTopic(eq(ServiceType.TB_TRANSPORT), any());

        verify(tbQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2)), any(TbProtoQueueMsg.class), isNull());

        verify(producerProvider, never()).getTbCoreNotificationsMsgProducer();
        verify(producerProvider, never()).getTransportNotificationsMsgProducer();
    }

    /**
     * 功能：验证队列相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOnQueueChangeSingleMonolithAndSingleRemoteTransport() {
        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(MONOLITH));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(MONOLITH, TRANSPORT));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbREQueueProducer = mock(TbQueueProducer.class);
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> tbTransportQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbREQueueProducer);
        when(producerProvider.getTransportNotificationsMsgProducer()).thenReturn(tbTransportQueueProducer);

        clusterService.onQueuesUpdate(List.of(createTestQueue()));

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_TRANSPORT, TRANSPORT);
        verify(topicService, never()).getNotificationsTopic(eq(ServiceType.TB_CORE), any());

        verify(tbREQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(tbTransportQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, TRANSPORT)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(tbTransportQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_CORE, MONOLITH)), any(TbProtoQueueMsg.class), isNull());

        verify(producerProvider, never()).getTbCoreNotificationsMsgProducer();
    }

    /**
     * 功能：验证队列相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOnQueueChangeMultipleMicroservices() {
        String monolith1 = MONOLITH + 1;
        String monolith2 = MONOLITH + 2;

        String core1 = CORE + 1;
        String core2 = CORE + 2;

        String ruleEngine1 = RULE_ENGINE + 1;
        String ruleEngine2 = RULE_ENGINE + 2;

        String transport1 = TRANSPORT + 1;
        String transport2 = TRANSPORT + 2;

        when(partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE)).thenReturn(Sets.newHashSet(monolith1, monolith2, ruleEngine1, ruleEngine2));
        when(partitionService.getAllServiceIds(ServiceType.TB_CORE)).thenReturn(Sets.newHashSet(monolith1, monolith2, core1, core2));
        when(partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT)).thenReturn(Sets.newHashSet(monolith1, monolith2, transport1, transport2));

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineNotificationMsg>> tbREQueueProducer = mock(TbQueueProducer.class);
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> tbCoreQueueProducer = mock(TbQueueProducer.class);
        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> tbTransportQueueProducer = mock(TbQueueProducer.class);

        when(producerProvider.getRuleEngineNotificationsMsgProducer()).thenReturn(tbREQueueProducer);
        when(producerProvider.getTbCoreNotificationsMsgProducer()).thenReturn(tbCoreQueueProducer);
        when(producerProvider.getTransportNotificationsMsgProducer()).thenReturn(tbTransportQueueProducer);

        clusterService.onQueuesUpdate(List.of(createTestQueue()));

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine1);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine2);

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_CORE, core1);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_CORE, core2);
        verify(topicService, never()).getNotificationsTopic(ServiceType.TB_CORE, monolith1);
        verify(topicService, never()).getNotificationsTopic(ServiceType.TB_CORE, monolith2);

        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_TRANSPORT, transport1);
        verify(topicService, times(1)).getNotificationsTopic(ServiceType.TB_TRANSPORT, transport2);
        verify(topicService, never()).getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith1);
        verify(topicService, never()).getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith2);

        verify(tbREQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbREQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, monolith2)), any(TbProtoQueueMsg.class), isNull());
        verify(tbREQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbREQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngine2)), any(TbProtoQueueMsg.class), isNull());

        verify(tbCoreQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_CORE, core1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbCoreQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_CORE, core2)), any(TbProtoQueueMsg.class), isNull());
        verify(tbCoreQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_CORE, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbCoreQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_CORE, monolith2)), any(TbProtoQueueMsg.class), isNull());

        verify(tbTransportQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transport1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, times(1))
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transport2)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith1)), any(TbProtoQueueMsg.class), isNull());
        verify(tbTransportQueueProducer, never())
                .send(eq(topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, monolith2)), any(TbProtoQueueMsg.class), isNull());
    }

    /**
     * 功能：保存或创建队列。
     * 参数：无。
     * 返回：处理结果。
     */
    protected Queue createTestQueue() {
        TenantId tenantId = TenantId.SYS_TENANT_ID;
        Queue queue = new Queue(new QueueId(UUID.randomUUID()));
        queue.setTenantId(tenantId);
        queue.setName(DataConstants.MAIN_QUEUE_NAME);
        queue.setTopic("main");
        queue.setPartitions(10);
        return queue;
    }

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbClusterServiceTest` 在 ThingsBoard Application 测试模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}