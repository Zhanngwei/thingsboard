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
package org.thingsboard.server.transport.lwm2m.ota.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.transport.lwm2m.ota.AbstractOtaLwM2MIntegrationTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.rest.client.utils.RestJsonConverter.toTimeseries;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.FAILED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.INITIATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.QUEUED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.UPDATING;
import static org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus.VERIFIED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

/**
 * 中文说明：
 * 1. 类目的：`OtaLwM2MIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public class OtaLwM2MIntegrationTest extends AbstractOtaLwM2MIntegrationTest {

    protected final String OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA =

            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/5_1.0/0/3\": \"state\",\n" +
                    "      \"/5_1.0/0/5\": \"updateResult\",\n" +
                    "      \"/5_1.0/0/6\": \"pkgname\",\n" +
                    "      \"/5_1.0/0/7\": \"pkgversion\",\n" +
                    "      \"/5_1.0/0/9\": \"firmwareUpdateDeliveryMethod\",\n" +
                    "      \"/9_1.0/0/0\": \"pkgname\",\n" +
                    "      \"/9_1.0/0/1\": \"pkgversion\",\n" +
                    "      \"/9_1.0/0/7\": \"updateState\",\n" +
                    "      \"/9_1.0/0/9\": \"updateResult\"\n" +
                    "    },\n" +
                    "    \"observe\": [\n" +
                    "      \"/5_1.0/0/3\",\n" +
                    "      \"/5_1.0/0/5\",\n" +
                    "      \"/5_1.0/0/6\",\n" +
                    "      \"/5_1.0/0/7\",\n" +
                    "      \"/5_1.0/0/9\",\n" +
                    "      \"/9_1.0/0/0\",\n" +
                    "      \"/9_1.0/0/1\",\n" +
                    "      \"/9_1.0/0/7\",\n" +
                    "      \"/9_1.0/0/9\"\n" +
                    "    ],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/5_1.0/0/3\",\n" +
                    "      \"/5_1.0/0/5\",\n" +
                    "      \"/5_1.0/0/6\",\n" +
                    "      \"/5_1.0/0/7\",\n" +
                    "      \"/5_1.0/0/9\",\n" +
                    "      \"/9_1.0/0/0\",\n" +
                    "      \"/9_1.0/0/1\",\n" +
                    "      \"/9_1.0/0/7\",\n" +
                    "      \"/9_1.0/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    /**
     * `expectedStatuses`列表，用于保存一组待处理对象。
     */
    private List<OtaPackageUpdateStatus> expectedStatuses;

    /**
     * 功能：验证配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFirmwareUpdateWithClientWithoutFirmwareOtaInfoFromProfile() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(this.CLIENT_ENDPOINT_WITHOUT_FW_INFO));
        final Device device = createDevice(deviceCredentials, this.CLIENT_ENDPOINT_WITHOUT_FW_INFO);
        createNewClient(SECURITY_NO_SEC, COAP_CONFIG, false, this.CLIENT_ENDPOINT_WITHOUT_FW_INFO, false, null);
        awaitObserveReadAll(0, false, device.getId().getId().toString());

        device.setFirmwareId(createFirmware().getId());
        final Device savedDevice = doPost("/api/device", device, Device.class);

        Thread.sleep(1000);

        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        List<TsKvEntry> ts = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" +
                savedDevice.getId().getId() + "/values/timeseries?keys=fw_state", new TypeReference<>() {}));
        List<OtaPackageUpdateStatus> statuses = ts.stream().map(KvEntry::getValueAsString).map(OtaPackageUpdateStatus::valueOf).collect(Collectors.toList());
        List<OtaPackageUpdateStatus> expectedStatuses = Collections.singletonList(FAILED);

        Assert.assertEquals(expectedStatuses, statuses);
    }

    /**
     * 功能：验证`Firmware Update By Object5`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFirmwareUpdateByObject5() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(this.CLIENT_ENDPOINT_OTA5));
        final Device device = createDevice(deviceCredentials, this.CLIENT_ENDPOINT_OTA5);
        createNewClient(SECURITY_NO_SEC, COAP_CONFIG, false, this.CLIENT_ENDPOINT_OTA5, false, null);
        awaitObserveReadAll(9, false, device.getId().getId().toString());

        device.setFirmwareId(createFirmware().getId());
        final Device savedDevice = doPost("/api/device", device, Device.class);

        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        expectedStatuses = Arrays.asList(QUEUED, INITIATED, DOWNLOADING, DOWNLOADED, UPDATING, UPDATED);
        List<TsKvEntry> ts = await("await on timeseries")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" +
                        savedDevice.getId().getId() + "/values/timeseries?orderBy=ASC&keys=fw_state&startTs=0&endTs=" +
                        System.currentTimeMillis(), new TypeReference<>() {
                })), this::predicateForStatuses);
        log.warn("Object5: Got the ts: {}", ts);
    }

    /**
     * This is the example how to use the AWAITILITY instead Thread.sleep()
     * Test will finish as fast as possible, but will await until TIMEOUT if a build machine is busy or slow
     * Check the detailed log output to learn how Awaitility polling the API and when exactly expected result appears
     * */
    /**
     * 功能：验证`Software Update By Object9`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSoftwareUpdateByObject9() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(this.CLIENT_ENDPOINT_OTA9));
        final Device device = createDevice(deviceCredentials, this.CLIENT_ENDPOINT_OTA9);
        createNewClient(SECURITY_NO_SEC, COAP_CONFIG, false, this.CLIENT_ENDPOINT_OTA9, false, null);
        awaitObserveReadAll(9, false, device.getId().getId().toString());

        device.setSoftwareId(createSoftware().getId());
        final Device savedDevice = doPost("/api/device", device, Device.class); //sync call

        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        expectedStatuses = List.of(
                QUEUED, INITIATED, DOWNLOADING, DOWNLOADING, DOWNLOADING, DOWNLOADED, VERIFIED, UPDATED);

        List<TsKvEntry> ts = await("await on timeseries")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getSwStateTelemetryFromAPI(device.getId().getId()), this::predicateForStatuses);
        log.warn("Object9: Got the ts: {}", ts);
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    private Device getDeviceFromAPI(UUID deviceId) throws Exception {
        final Device device = doGet("/api/device/" + deviceId, Device.class);
        log.trace("Fetched device by API for deviceId {}, device is {}", deviceId, device);
        return device;
    }

    /**
     * 功能：获取遥测。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：匹配的数据集合。
     */
    private List<TsKvEntry> getSwStateTelemetryFromAPI(UUID deviceId) throws Exception {
        final List<TsKvEntry> tsKvEntries = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?orderBy=ASC&keys=sw_state&startTs=0&endTs=" + System.currentTimeMillis(), new TypeReference<>() {
        }));
        log.warn("Fetched telemetry by API for deviceId {}, list size {}, tsKvEntries {}", deviceId, tsKvEntries.size(), tsKvEntries);
        return tsKvEntries;
    }

    /**
     * 功能：执行 `predicateForStatuses` 对应的处理。
     * 参数：
     * - `ts`：时间戳。
     * 返回：判断结果。
     */
    private boolean predicateForStatuses (List<TsKvEntry> ts) {
        List<OtaPackageUpdateStatus> statuses = ts.stream().sorted(Comparator
                .comparingLong(TsKvEntry::getTs)).map(KvEntry::getValueAsString)
                .map(OtaPackageUpdateStatus::valueOf)
                .collect(Collectors.toList());
        log.warn("{}", statuses);
        return statuses.containsAll(expectedStatuses);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`OtaLwM2MIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
