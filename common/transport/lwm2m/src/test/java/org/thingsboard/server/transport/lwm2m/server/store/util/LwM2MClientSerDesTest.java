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
package org.thingsboard.server.transport.lwm2m.server.store.util;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportResourceCache;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.client.ResourceValue;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`LwM2MClientSerDesTest` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class LwM2MClientSerDesTest {

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `serializeDeserialize` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void serializeDeserialize() throws Exception {
        LwM2mClient client = new LwM2mClient("nodeId", "testEndpoint");

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportDeviceInfo tdi = new TransportDeviceInfo();
        tdi.setPowerMode(PowerMode.PSM);
        tdi.setPsmActivityTimer(10000L);
        tdi.setPagingTransmissionWindow(2000L);
        tdi.setEdrxCycle(3000L);
        tdi.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        tdi.setCustomerId(new CustomerId(UUID.randomUUID()));
        tdi.setDeviceId(new DeviceId(UUID.randomUUID()));
        tdi.setDeviceProfileId(new DeviceProfileId(UUID.randomUUID()));
        tdi.setDeviceName("testDevice");
        tdi.setDeviceType("testType");
        ValidateDeviceCredentialsResponse credentialsResponse = ValidateDeviceCredentialsResponse.builder()
                .deviceInfo(tdi)
                .build();

        client.init(credentialsResponse, UUID.randomUUID());

        Registration registration =
                new Registration.Builder("test", "testEndpoint", Identity
                        .unsecure(new InetSocketAddress(1000)))
                        .supportedContentFormats()
                        .supportedObjects(Map.of(15, "1.0", 17, "1.0"))
                        .objectLinks(new Link[]{new Link("/")})
                        .build();

        client.setRegistration(registration);
        client.setState(LwM2MClientState.REGISTERED);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        client.getSharedAttributes().put("key1", TransportProtos.TsKvProto.newBuilder().setTs(0).setKv(TransportProtos.KeyValueProto.newBuilder().setStringV("test").build()).build());
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        client.getSharedAttributes().put("key2", TransportProtos.TsKvProto.newBuilder().setTs(1).setKv(TransportProtos.KeyValueProto.newBuilder().setDoubleV(1.02).build()).build());

        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        TransportResourceCache resourceCache = mock(TransportResourceCache.class);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        LwM2mTransportContext context = mock(LwM2mTransportContext.class);
        LwM2mClientContext clientContext = mock(LwM2mClientContext.class);

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        var provider = new LwM2mVersionedModelProvider(clientContext, new LwM2mTransportServerHelper(context), context);

        TbResource resource15 = new TbResource();
        resource15.setData(Files.readAllBytes(Path.of(this.getClass().getClassLoader().getResource("15.xml").toURI())));
        TbResource resource17 = new TbResource();
        resource17.setData(Files.readAllBytes(Path.of(this.getClass().getClassLoader().getResource("17.xml").toURI())));

        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        when(resourceCache.get(any(), any(), eq("15_1.0"))).thenReturn(Optional.of(resource15));
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        when(resourceCache.get(any(), any(), eq("17_1.0"))).thenReturn(Optional.of(resource17));
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        when(context.getTransportResourceCache()).thenReturn(resourceCache);
        when(clientContext.getClientByEndpoint(any())).thenReturn(client);

        LwM2mResource singleResource = LwM2mSingleResource.newStringResource(15, "testValue");
        LwM2mResource multipleResource = LwM2mMultipleResource.newStringResource(17, Map.of(0, "testValue", 1, "testValue"));
        client.saveResourceValue("/15_1.0/0/0", singleResource, provider, WriteRequest.Mode.UPDATE);
        client.saveResourceValue("/17_1.0/0/0", multipleResource, provider, WriteRequest.Mode.UPDATE);

        byte[] bytes = LwM2MClientSerDes.serialize(client);
        Assert.assertNotNull(bytes);

        LwM2mClient desClient = LwM2MClientSerDes.deserialize(bytes);

        assertEquals(client.getNodeId(), desClient.getNodeId());
        assertEquals(client.getEndpoint(), desClient.getEndpoint());
        assertEquals(client.getSharedAttributes(), desClient.getSharedAttributes());
        assertEquals(client.getKeyTsLatestMap(), desClient.getKeyTsLatestMap());
        assertEquals(client.getTenantId(), desClient.getTenantId());
        assertEquals(client.getProfileId(), desClient.getProfileId());
        assertEquals(client.getDeviceId(), desClient.getDeviceId());
        assertEquals(client.getState(), desClient.getState());
        assertEquals(client.getSession(), desClient.getSession());
        assertEquals(client.getPowerMode(), desClient.getPowerMode());
        assertEquals(client.getPsmActivityTimer(), desClient.getPsmActivityTimer());
        assertEquals(client.getPagingTransmissionWindow(), desClient.getPagingTransmissionWindow());
        assertEquals(client.getEdrxCycle(), desClient.getEdrxCycle());
        assertEquals(client.getRegistration(), desClient.getRegistration());
        assertEquals(client.isAsleep(), desClient.isAsleep());
        assertEquals(client.getLastUplinkTime(), desClient.getLastUplinkTime());
        assertEquals(client.getSleepTask(), desClient.getSleepTask());
        assertEquals(client.getClientSupportContentFormats(), desClient.getClientSupportContentFormats());
        assertEquals(client.getDefaultContentFormat(), desClient.getDefaultContentFormat());
        assertEquals(client.getRetryAttempts().get(), desClient.getRetryAttempts().get());
        assertEquals(client.getLastSentRpcId(), desClient.getLastSentRpcId());

        Map<String, ResourceValue> expectedResources = client.getResources();
        Map<String, ResourceValue> actualResources = desClient.getResources();
        assertNotNull(actualResources);
        assertEquals(expectedResources.size(), actualResources.size());
        expectedResources.forEach((key, value) -> assertEquals(value.toString(), actualResources.get(key).toString()));
    }


/*
 * 本类总结：
 * 1. 核心职责：`LwM2MClientSerDesTest` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}