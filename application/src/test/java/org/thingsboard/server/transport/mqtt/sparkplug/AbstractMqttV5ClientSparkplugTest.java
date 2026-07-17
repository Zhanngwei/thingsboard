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
package org.thingsboard.server.transport.mqtt.sparkplug;

import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_CONNACK;
import static org.thingsboard.common.util.JacksonUtil.newArrayNode;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Bytes;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int16;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int32;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int64;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int8;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt16;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt32;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt64;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt8;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.createMetric;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.NAMESPACE;

/**
 * Created by nickAS21 on 12.01.23
 */
/**
 * 中文说明：
 * 1. `AbstractMqttV5ClientSparkplugTest` 是 ThingsBoard Application 中验证 `AbstractMqttV5ClientSparkplug` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugTest extends AbstractMqttIntegrationTest {

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    protected MqttV5TestClient client;
    protected SparkplugMqttCallback mqttCallback;
    protected Calendar calendar = Calendar.getInstance();
    protected ThreadLocalRandom random = ThreadLocalRandom.current();

    /**
     * `groupId`常量，用于统一引用固定值。
     */
    protected static final String groupId = "SparkplugBGroupId";
    protected static final String edgeNode = "SparkpluBNode";
    /**
     * 序号常量，用于统一引用固定值。
     */
    protected static final String keysBdSeq = "bdSeq";
    protected static final String alias = "Failed Telemetry/Attribute proto sparkplug payload. SparkplugMessageType ";
    /**
     * 设备ID，用于定位对应业务对象。
     */
    protected String deviceId = "Test Sparkplug B Device";
    protected int bdSeq = 0;
    /**
     * 序号，用于控制处理规模或位置。
     */
    protected int seq = 0;
    protected static final long PUBLISH_TS_DELTA_MS = 86400000;// Publish start TS <-> 24h

    // NBIRTH
    /**
     * 键常量，用于统一引用固定值。
     */
    protected static final String keyNodeRebirth = "Node Control/Rebirth";

    //*BIRTH
    /**
     * 数据常量，用于统一引用固定值。
     */
    protected static final MetricDataType metricBirthDataType_Int32 = Int32;
    protected static final String metricBirthName_Int32 = "Device Metric int32";
    /**
     * Sparkplug集合，用于去重保存或快速判断对象是否存在。
     */
    protected Set<String> sparkplugAttributesMetricNames;

    /**
     * 功能：执行 `beforeSparkplugTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void beforeSparkplugTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .gatewayName("Test Connect Sparkplug client node")
                .isSparkplug(true)
                .sparkplugAttributesMetricNames(sparkplugAttributesMetricNames)
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    /**
     * 功能：执行 `clientWithCorrectNodeAccessTokenWithNDEATH` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void clientWithCorrectNodeAccessTokenWithNDEATH() throws Exception {
        long ts = calendar.getTimeInMillis();
        long value = bdSeq = 0;
        clientWithCorrectNodeAccessTokenWithNDEATH(ts, value);
    }

    /**
     * 功能：执行 `clientWithCorrectNodeAccessTokenWithNDEATH` 对应的处理。
     * 参数：
     * - `ts`：时间戳。
     * - `value`：值。
     * 返回：无。
     */
    public void clientWithCorrectNodeAccessTokenWithNDEATH(long ts, long value) throws Exception {
        IMqttToken connectionResult = clientConnectWithNDEATH(ts, value);
        MqttWireMessage response = connectionResult.getResponse();
        Assert.assertEquals(MESSAGE_TYPE_CONNACK, response.getType());
        MqttConnAck connAckMsg = (MqttConnAck) response;
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, connAckMsg.getReturnCode());
    }

    /**
     * 功能：执行 `clientConnectWithNDEATH` 对应的处理。
     * 参数：
     * - `ts`：时间戳。
     * - `value`：值。
     * - `nameSpaceBad`：名称。
     * 返回：处理结果。
     */
    public IMqttToken clientConnectWithNDEATH(long ts, long value, String... nameSpaceBad) throws Exception {
        String key = keysBdSeq;
        MetricDataType metricDataType = Int64;
        SparkplugBProto.Payload.Builder deathPayload = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis());
        deathPayload.addMetrics(createMetric(value, ts, key, metricDataType));
        byte[] deathBytes = deathPayload.build().toByteArray();
        this.client = new MqttV5TestClient();
        this.mqttCallback = new SparkplugMqttCallback();
        this.client.setCallback(this.mqttCallback);
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setUserName(gatewayAccessToken);
        String nameSpace = nameSpaceBad.length == 0 ? NAMESPACE : nameSpaceBad[0];
        String topic = nameSpace + "/" + groupId + "/" + SparkplugMessageType.NDEATH.name() + "/" + edgeNode;
        MqttMessage msg = new MqttMessage();
        msg.setId(0);
        msg.setPayload(deathBytes);
        options.setWill(topic, msg);
        return client.connect(options);
    }

    /**
     * 功能：执行 `connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices` 对应的处理。
     * 参数：
     * - `cntDevices`：设备信息或设备标识。
     * - `ts`：时间戳。
     * 返回：匹配的数据集合。
     */
    protected List<Device> connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(int cntDevices, long ts) throws Exception {
        List<Device> devices = new ArrayList<>();
        clientWithCorrectNodeAccessTokenWithNDEATH();
        MetricDataType metricDataType = Int32;
        String key = "Node Metric int32";
        int valueDeviceInt32 = 1024;
        SparkplugBProto.Payload.Metric metric = createMetric(valueDeviceInt32, ts, key, metricDataType);
        SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(ts)
                .setSeq(getBdSeqNum());
        payloadBirthNode.addMetrics(metric);
        payloadBirthNode.setTimestamp(ts);
        if (client.isConnected()) {
            client.publish(NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.NBIRTH.name() + "/" + edgeNode,
                    payloadBirthNode.build().toByteArray(), 0, false);
        }

        valueDeviceInt32 = 4024;
        metric = createMetric(valueDeviceInt32, ts, metricBirthName_Int32,  metricBirthDataType_Int32);
        for (int i = 0; i < cntDevices; i++) {
            SparkplugBProto.Payload.Builder payloadBirthDevice = SparkplugBProto.Payload.newBuilder()
                    .setTimestamp(ts)
                    .setSeq(getSeqNum());
            String deviceName = deviceId + "_" + i;

            payloadBirthDevice.addMetrics(metric);
            if (client.isConnected()) {
                client.publish(NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.DBIRTH.name() + "/" + edgeNode + "/" + deviceName,
                        payloadBirthDevice.build().toByteArray(), 0, false);
                AtomicReference<Device> device = new AtomicReference<>();
                await(alias + "find device [" + deviceName + "] after created")
                        .atMost(200, TimeUnit.SECONDS)
                        .until(() -> {
                            device.set(doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class));
                            return device.get() != null;
                        });
                devices.add(device.get());
            }

        }

        Assert.assertEquals(cntDevices, devices.size());
        return devices;
    }

    /**
     * 功能：获取序号。
     * 参数：无。
     * 返回：数值结果。
     */
    protected long getBdSeqNum() throws Exception {
        if (bdSeq == 256) {
            bdSeq = 0;
        }
        return bdSeq++;
    }

    /**
     * 功能：获取序号。
     * 参数：无。
     * 返回：数值结果。
     */
    protected long getSeqNum() throws Exception {
        if (seq == 256) {
            seq = 0;
        }
        return seq++;
    }

    /**
     * 功能：执行 `connectionWithNBirth` 对应的处理。
     * 参数：
     * - `metricDataType`：待处理数据。
     * - `metricKey`：键。
     * - `metricValue`：值。
     * 返回：匹配的数据集合。
     */
    protected List<String> connectionWithNBirth(MetricDataType metricDataType, String metricKey, Object metricValue) throws Exception {
        List<String> listKeys = new ArrayList<>();
        SparkplugBProto.Payload.Builder payloadBirthNode = SparkplugBProto.Payload.newBuilder()
                .setTimestamp(calendar.getTimeInMillis());
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long valueBdSec = getBdSeqNum();
        payloadBirthNode.addMetrics(createMetric(valueBdSec, ts, keysBdSeq, Int64));
        listKeys.add(SparkplugMessageType.NBIRTH.name() + " " + keysBdSeq);
        payloadBirthNode.addMetrics(createMetric(false, ts, keyNodeRebirth, MetricDataType.Boolean));
        listKeys.add(keyNodeRebirth);

        payloadBirthNode.addMetrics(createMetric(metricValue, ts, metricKey, metricDataType));
        listKeys.add(metricKey);

        if (client.isConnected()) {
            client.publish(NAMESPACE + "/" + groupId + "/" + SparkplugMessageType.NBIRTH.name() + "/" + edgeNode,
                    payloadBirthNode.build().toByteArray(), 0, false);
        }
        return listKeys;
    }

    /**
     * 功能：执行 `createdAddMetricValuePrimitiveTsKv` 对应的处理。
     * 参数：
     * - `listTsKvEntry`：数据列表。
     * - `listKeys`：键。
     * - `dataPayload`：待处理数据。
     * - `ts`：时间戳。
     * 返回：无。
     */
    protected void createdAddMetricValuePrimitiveTsKv(List<TsKvEntry> listTsKvEntry, List<String> listKeys,
                                                      SparkplugBProto.Payload.Builder dataPayload, long ts) throws ThingsboardException {

        String keys = "MyInt8";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextInt8(), ts, Int8));
        listKeys.add(keys);

        keys = "MyInt16";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextInt16(), ts, Int16));
        listKeys.add(keys);

        keys = "MyInt32";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextInt32(), ts, Int32));
        listKeys.add(keys);

        keys = "MyInt64";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextInt64(), ts, Int64));
        listKeys.add(keys);

        keys = "MyUInt8";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt8(), ts, UInt8));
        listKeys.add(keys);

        keys = "MyUInt16";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt16(), ts, UInt16));
        listKeys.add(keys);

        keys = "MyUInt32";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt32(), ts, UInt32));
        listKeys.add(keys);

        keys = "MyUInt64";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextUInt64(), ts, UInt64));
        listKeys.add(keys);

        keys = "MyFloat";
        listTsKvEntry.add(createdAddMetricTsKvFloat(dataPayload, keys, nextFloat(0, 100), ts, MetricDataType.Float));
        listKeys.add(keys);

        keys = "MyDateTime";
        listTsKvEntry.add(createdAddMetricTsKvLong(dataPayload, keys, nextDateTime(), ts, MetricDataType.DateTime));
        listKeys.add(keys);

        keys = "MyDouble";
        listTsKvEntry.add(createdAddMetricTsKvDouble(dataPayload, keys, nextDouble(), ts, MetricDataType.Double));
        listKeys.add(keys);

        keys = "MyBoolean";
        listTsKvEntry.add(createdAddMetricTsKvBoolean(dataPayload, keys, nextBoolean(), ts, MetricDataType.Boolean));
        listKeys.add(keys);

        keys = "MyString";
        listTsKvEntry.add(createdAddMetricTsKvString(dataPayload, keys, nextString(), ts, MetricDataType.String));
        listKeys.add(keys);

        keys = "MyText";
        listTsKvEntry.add(createdAddMetricTsKvString(dataPayload, keys, nextString(), ts, MetricDataType.Text));
        listKeys.add(keys);

        keys = "MyUUID";
        listTsKvEntry.add(createdAddMetricTsKvString(dataPayload, keys, nextString(), ts, MetricDataType.UUID));
        listKeys.add(keys);

    }

    /**
     * 功能：执行 `createdAddMetricValueArraysPrimitiveTsKv` 对应的处理。
     * 参数：
     * - `listTsKvEntry`：数据列表。
     * - `listKeys`：键。
     * - `dataPayload`：待处理数据。
     * - `ts`：时间戳。
     * 返回：无。
     */
    protected void createdAddMetricValueArraysPrimitiveTsKv(List<TsKvEntry> listTsKvEntry, List<String> listKeys,
                                                            SparkplugBProto.Payload.Builder dataPayload, long ts) throws ThingsboardException {
        String keys = "MyBytesArray";
        byte[] bytes = {nextInt8(), nextInt8(), nextInt8()};
        createdAddMetricTsKvJson(dataPayload, keys, bytes, ts, Bytes, listTsKvEntry, listKeys);
    }

    /**
     * 功能：执行 `createdAddMetricTsKvLong` 对应的处理。
     * 参数：
     * - `dataPayload`：待处理数据。
     * - `key`：键。
     * - `value`：值。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private TsKvEntry createdAddMetricTsKvLong(SparkplugBProto.Payload.Builder dataPayload, String key, Object value,
                                               long ts, MetricDataType metricDataType) throws ThingsboardException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(key, Long.valueOf(String.valueOf(value))));
        dataPayload.addMetrics(createMetric(value, ts, key, metricDataType));
        return tsKvEntry;
    }

    /**
     * 功能：执行 `createdAddMetricTsKvFloat` 对应的处理。
     * 参数：
     * - `dataPayload`：待处理数据。
     * - `key`：键。
     * - `value`：值。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private TsKvEntry createdAddMetricTsKvFloat(SparkplugBProto.Payload.Builder dataPayload, String key, float value,
                                                long ts, MetricDataType metricDataType) throws ThingsboardException {
        Double dd = Double.parseDouble(Float.toString(value));
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new DoubleDataEntry(key, dd));
        dataPayload.addMetrics(createMetric(value, ts, key, metricDataType));
        return tsKvEntry;
    }

    /**
     * 功能：执行 `createdAddMetricTsKvDouble` 对应的处理。
     * 参数：
     * - `dataPayload`：待处理数据。
     * - `key`：键。
     * - `value`：值。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private TsKvEntry createdAddMetricTsKvDouble(SparkplugBProto.Payload.Builder dataPayload, String key, double value,
                                                 long ts, MetricDataType metricDataType) throws ThingsboardException {
        Long l = Double.valueOf(value).longValue();
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new LongDataEntry(key, l));
        dataPayload.addMetrics(createMetric(value, ts, key, metricDataType));
        return tsKvEntry;
    }

    /**
     * 功能：执行 `createdAddMetricTsKvBoolean` 对应的处理。
     * 参数：
     * - `dataPayload`：待处理数据。
     * - `key`：键。
     * - `value`：值。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private TsKvEntry createdAddMetricTsKvBoolean(SparkplugBProto.Payload.Builder dataPayload, String key, boolean value,
                                                  long ts, MetricDataType metricDataType) throws ThingsboardException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new BooleanDataEntry(key, value));
        dataPayload.addMetrics(createMetric(value, ts, key, metricDataType));
        return tsKvEntry;
    }

    /**
     * 功能：执行 `createdAddMetricTsKvString` 对应的处理。
     * 参数：
     * - `dataPayload`：待处理数据。
     * - `key`：键。
     * - `value`：值。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private TsKvEntry createdAddMetricTsKvString(SparkplugBProto.Payload.Builder dataPayload, String key, String value,
                                                 long ts, MetricDataType metricDataType) throws ThingsboardException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(ts, new StringDataEntry(key, value));
        dataPayload.addMetrics(createMetric(value, ts, key, metricDataType));
        return tsKvEntry;
    }

    /**
     * 功能：执行 `createdAddMetricTsKvJson` 对应的处理。
     * 参数：
     * - `dataPayload`：待处理数据。
     * - `key`：键。
     * - `values`：值。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void createdAddMetricTsKvJson(SparkplugBProto.Payload.Builder dataPayload, String key,
                                          Object values, long ts, MetricDataType metricDataType,
                                          List<TsKvEntry> listTsKvEntry,
                                          List<String> listKeys) throws ThingsboardException {
        ArrayNode nodeArray = newArrayNode();
        switch (metricDataType) {
            case Bytes:
                for (byte b : (byte[]) values) {
                    nodeArray.add(b);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + metricDataType);
        }
        if (nodeArray.size() > 0) {
            Optional<TsKvEntry> tsKvEntryOptional = Optional.of(new BasicTsKvEntry(ts, new JsonDataEntry(key, nodeArray.toString())));
            if (tsKvEntryOptional.isPresent()) {
                dataPayload.addMetrics(createMetric(values, ts, key, metricDataType));
                listTsKvEntry.add(tsKvEntryOptional.get());
                listKeys.add(key);
            }
        }
    }

    /**
     * 功能：执行 `nextInt8` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private byte nextInt8() {
        return (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    /**
     * 功能：执行 `nextUInt8` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private short nextUInt8() {
        return (short) random.nextInt(0, Byte.MAX_VALUE * 2 + 1);
    }

    /**
     * 功能：执行 `nextInt16` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private short nextInt16() {
        return (short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
    }

    /**
     * 功能：执行 `nextUInt16` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    private int nextUInt16() {
        return random.nextInt(0, Short.MAX_VALUE * 2 + 1);
    }

    /**
     * 功能：执行 `nextInt32` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    protected int nextInt32() {
        return random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * 功能：执行 `nextUInt32` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    protected long nextUInt32() {
        long l = Integer.MAX_VALUE;
        return random.nextLong(0, l * 2 + 1);
    }

    /**
     * 功能：执行 `nextInt64` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    private long nextInt64() {
        return random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * 功能：执行 `nextUInt64` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    private long nextUInt64() {
        double d = Long.MAX_VALUE;
        return random.nextLong(0, (long) (d * 2 + 1));
    }

    /**
     * 功能：执行 `nextDouble` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    protected double nextDouble() {
        return random.nextDouble(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * 功能：执行 `nextDateTime` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    private long nextDateTime() {
        long min = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long max = calendar.getTimeInMillis();
        return random.nextLong(min, max);
    }

    /**
     * 功能：执行 `nextFloat` 对应的处理。
     * 参数：
     * - `min`：`min` 参数。
     * - `max`：`max` 参数。
     * 返回：数值结果。
     */
    protected float nextFloat(float min, float max) {
        if (min >= max)
            throw new IllegalArgumentException("max must be greater than min");
        float result = ThreadLocalRandom.current().nextFloat() * (max - min) + min;
        if (result >= max) // correct for rounding
            result = Float.intBitsToFloat(Float.floatToIntBits(max) - 1);
        return result;
    }

    /**
     * 功能：执行 `nextBoolean` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    protected boolean nextBoolean() {
        return random.nextBoolean();
    }

    /**
     * 功能：执行 `nextString` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    protected String nextString() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * 中文说明：
     * 1. `SparkplugMqttCallback` 是 ThingsBoard Application 中处理 MQTT 的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `MqttCallback`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    public class SparkplugMqttCallback  implements MqttCallback {
        private final List<SparkplugBProto.Payload.Metric> messageArrivedMetrics = new ArrayList<>();

        /**
         * 功能：执行 `disconnected` 对应的处理。
         * 参数：
         * - `mqttDisconnectResponse`：响应对象。
         * 返回：无。
         */
        @Override
        public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

        }

        /**
         * 功能：执行 `mqttErrorOccurred` 对应的处理。
         * 参数：
         * - `e`：`e` 参数。
         * 返回：无。
         */
        @Override
        public void mqttErrorOccurred(MqttException e) {

        }

        /**
         * 功能：执行 `messageArrived` 对应的处理。
         * 参数：
         * - `topic`：主题名称或主题对象。
         * - `mqttMsg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void messageArrived(String topic, MqttMessage mqttMsg) throws Exception {
            SparkplugBProto.Payload sparkplugBProtoNode = SparkplugBProto.Payload.parseFrom(mqttMsg.getPayload());
            messageArrivedMetrics.addAll(sparkplugBProtoNode.getMetricsList());
        }

        /**
         * 功能：执行 `deliveryComplete` 对应的处理。
         * 参数：
         * - `iMqttToken`：`iMqttToken` 参数。
         * 返回：无。
         */
        @Override
        public void deliveryComplete(IMqttToken iMqttToken) {

        }

        /**
         * 功能：执行 `connectComplete` 对应的处理。
         * 参数：
         * - `b`：`b` 参数。
         * - `s`：`s` 参数。
         * 返回：无。
         */
        @Override
        public void connectComplete(boolean b, String s) {

        }

        /**
         * 功能：执行 `authPacketArrived` 对应的处理。
         * 参数：
         * - `i`：`i` 参数。
         * - `mqttProperties`：`mqttProperties` 参数。
         * 返回：无。
         */
        @Override
        public void authPacketArrived(int i, MqttProperties mqttProperties) {

        }

        /**
         * 功能：获取消息。
         * 参数：无。
         * 返回：匹配的数据集合。
         */
        public List<SparkplugBProto.Payload.Metric> getMessageArrivedMetrics() {
            return messageArrivedMetrics;
        }

        /**
         * 功能：删除或清理消息。
         * 参数：
         * - `id`：`id`ID。
         * 返回：无。
         */
        public void deleteMessageArrivedMetrics(int id) {
            messageArrivedMetrics.remove(id);
        }
    }

}
