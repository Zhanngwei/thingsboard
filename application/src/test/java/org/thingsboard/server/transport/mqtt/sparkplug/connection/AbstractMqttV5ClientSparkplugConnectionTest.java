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
 * 1. `AbstractMqttV5ClientSparkplugConnectionTest` 是 ThingsBoard Application 中验证 `AbstractMqttV5ClientSparkplugConnection` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttV5ClientSparkplugTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
