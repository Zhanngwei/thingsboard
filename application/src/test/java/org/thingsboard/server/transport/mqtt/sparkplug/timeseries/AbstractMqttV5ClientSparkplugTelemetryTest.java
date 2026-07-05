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
package org.thingsboard.server.transport.mqtt.sparkplug.timeseries;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.NAMESPACE;

/**
 * Created by nickAS21 on 12.01.23
 */
/**
 * 中文说明：
 * 1. 类目的：`AbstractMqttV5ClientSparkplugTelemetryTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugTelemetryTest extends AbstractMqttV5ClientSparkplugTest {

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenPublishNBIRTH() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        List<String> listKeys = connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findLatest(tenantId, savedGateway.getId(), listKeys));
                    return !finalFuture.get().get().isEmpty();
                });
        Assert.assertEquals(listKeys.size(), finalFuture.get().get().size());
    }

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenPushNodeMetricBuildPrimitiveSimple() throws Exception {
        List<String> listKeys = new ArrayList<>();
        clientWithCorrectNodeAccessTokenWithNDEATH();

        String messageTypeName = SparkplugMessageType.NDATA.name();

        List<TsKvEntry> listTsKvEntry = new ArrayList<>();

        SparkplugBProto.Payload.Builder ndataPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis())
                .setSeq(getSeqNum());
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;

        createdAddMetricValuePrimitiveTsKv(listTsKvEntry, listKeys, ndataPayload, ts);

        if (client.isConnected()) {
            client.publish(NAMESPACE + "/" + groupId + "/" + messageTypeName + "/" + edgeNode,
                    ndataPayload.build().toByteArray(), 0, false);
        }

        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NDATA.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                    return finalFuture.get().get().size() == (listTsKvEntry.size() + 1);
                });
        Assert.assertTrue("Actual tsKvEntrys is not containsAll Expected tsKvEntrys", finalFuture.get().get().containsAll(listTsKvEntry));
    }

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenPushNodeMetricBuildArraysPrimitiveSimple() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();

        String messageTypeName = SparkplugMessageType.NDATA.name();
        List<String> listKeys = new ArrayList<>();
        List<TsKvEntry> listTsKvEntry = new ArrayList<>();

        SparkplugBProto.Payload.Builder ndataPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis())
                .setSeq(getSeqNum());
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;

        createdAddMetricValueArraysPrimitiveTsKv(listTsKvEntry, listKeys, ndataPayload, ts);

        if (client.isConnected()) {
            client.publish(NAMESPACE + "/" + groupId + "/" + messageTypeName + "/" + edgeNode,
                    ndataPayload.build().toByteArray(), 0, false);
        }

        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NDATA.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                    return finalFuture.get().get().size() == (listTsKvEntry.size() + 1);
                });
        Assert.assertTrue("Actual tsKvEntrys is not containsAll Expected tsKvEntrys", finalFuture.get().get().containsAll(listTsKvEntry));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractMqttV5ClientSparkplugTelemetryTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
