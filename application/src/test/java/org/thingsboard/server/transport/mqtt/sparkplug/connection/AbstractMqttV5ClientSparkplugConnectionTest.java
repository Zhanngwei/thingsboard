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
package org.thingsboard.server.transport.mqtt.sparkplug.connection;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Assert;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.OFFLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.ONLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.STATE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.messageName;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.NAMESPACE;

/**
 * Created by nickAS21 on 12.01.23
 */
/**
 * 中文说明：
 * 1. 类目的：`AbstractMqttV5ClientSparkplugConnectionTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugConnectionTest extends AbstractMqttV5ClientSparkplugTest {

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectNodeAccessTokenWithNDEATH_Test() throws Exception {
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long value = bdSeq = 0;
        clientWithCorrectNodeAccessTokenWithNDEATH(ts, value);

        String keys = SparkplugMessageType.NDEATH.name() + " " + keysBdSeq;
        TsKvEntry expectedTsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(keys, value));
        AtomicReference<ListenableFuture<Optional<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + SparkplugMessageType.NDEATH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findLatest(tenantId, savedGateway.getId(), keys));
                    return finalFuture.get().get().isPresent();
                });
        TsKvEntry actualTsKvEntry = finalFuture.get().get().get();
        Assert.assertEquals(expectedTsKvEntry, actualTsKvEntry);
    }

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectNodeAccessTokenWithoutNDEATH_Test() throws Exception {
        this.client = new MqttV5TestClient();
        MqttException actualException = Assert.assertThrows(MqttException.class, () -> client.connectAndWait(gatewayAccessToken));
        String expectedMessage = "Server unavailable.";
        int expectedReasonCode = 136;
        Assert.assertEquals(expectedMessage, actualException.getMessage());
        Assert.assertEquals(expectedReasonCode, actualException.getReasonCode());
    }

    /**
     * 功能：处理名称。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectNodeAccessTokenNameSpaceInvalid_Test() throws Exception {
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long value = bdSeq = 0;
        MqttException actualException = Assert.assertThrows(MqttException.class, () -> clientConnectWithNDEATH(ts, value, "spBv1.2"));
        String expectedMessage = "Server unavailable.";
        int expectedReasonCode = 136;
        Assert.assertEquals(expectedMessage, actualException.getMessage());
        Assert.assertEquals(expectedReasonCode, actualException.getReasonCode());
    }

    /**
     * 功能：处理客户端。
     * 参数：
     * - `cntDevices`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenWithNDEATHCreatedDevices(int cntDevices) throws Exception {
        long ts = calendar.getTimeInMillis();
        connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(cntDevices, ts);
    }

    /**
     * 功能：处理状态。
     * 参数：
     * - `cntDevices`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_ALL(int cntDevices) throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(cntDevices, ts);

        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(messageName(STATE), ONLINE.name()));
        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();
        await(alias + messageName(STATE) + ", device: " + savedGateway.getName())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                    return finalFuture.get().get().contains(tsKvEntry);
                });

        for (Device device : devices) {
            await(alias + messageName(STATE) + ", device: " + device.getName())
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> {
                        finalFuture.set(tsService.findAllLatest(tenantId, device.getId()));
                        return finalFuture.get().get().contains(tsKvEntry);
                    });
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `cntDevices`：设备信息或设备标识。
     * - `indexDeviceDisconnect`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_All_Then_OneDeviceOFFLINE(int cntDevices, int indexDeviceDisconnect) throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(cntDevices, ts);

        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(messageName(STATE), OFFLINE.name()));
        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();

        SparkplugBProto.Payload.Builder payloadDeathDevice = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(ts)
                .setSeq(getSeqNum());
        if (client.isConnected()) {
            List<Device> devicesList = new ArrayList<>(devices);
            Device device =  devicesList.get(indexDeviceDisconnect);
            client.publish(NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.DDEATH.name() + "/" + edgeNode + "/" + device.getName(),
                    payloadDeathDevice.build().toByteArray(), 0, false);
            await(alias + messageName(STATE) + ", device: " + device.getName())
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> {
                        finalFuture.set(tsService.findAllLatest(tenantId, device.getId()));
                        return findEqualsKeyValueInKvEntrys(finalFuture.get().get(), tsKvEntry);
                    });
        }
    }

    /**
     * OFFLINE_All
     * @param cntDevices
     * @throws Exception
     */

    /**
     * 功能：处理状态。
     * 参数：
     * - `cntDevices`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_All_Then_OFFLINE_All(int cntDevices) throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(cntDevices, ts);

        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(messageName(STATE), OFFLINE.name()));
        AtomicReference<ListenableFuture<List<TsKvEntry>>> finalFuture = new AtomicReference<>();

        if (client.isConnected()) {
            client.disconnect();

            await(alias + messageName(STATE) + ", device: " + savedGateway.getName())
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> {
                        finalFuture.set(tsService.findAllLatest(tenantId, savedGateway.getId()));
                        return findEqualsKeyValueInKvEntrys(finalFuture.get().get(), tsKvEntry);
                    });

            List<Device> devicesList = new ArrayList<>(devices);
            for (Device device : devicesList) {
                await(alias + messageName(STATE) + ", device: " + device.getName())
                        .atMost(40, TimeUnit.SECONDS)
                        .until(() -> {
                            finalFuture.set(tsService.findAllLatest(tenantId, device.getId()));
                            return findEqualsKeyValueInKvEntrys(finalFuture.get().get(), tsKvEntry);
                        });
            }
        }
    }


    /**
     * 功能：获取键。
     * 参数：
     * - `finalFuture`：数据列表。
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * 返回：判断结果。
     */
    private boolean findEqualsKeyValueInKvEntrys(List<TsKvEntry> finalFuture, TsKvEntry tsKvEntry) {
        for (TsKvEntry kvEntry : finalFuture) {
            if (kvEntry.getKey().equals(tsKvEntry.getKey()) && kvEntry.getValue().equals(tsKvEntry.getValue())) {
                return true;
            }
        }
        return false;
    }
}


/*
 * 本类总结：
 * 1. 核心职责：`AbstractMqttV5ClientSparkplugConnectionTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
