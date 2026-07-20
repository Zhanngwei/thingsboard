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
package org.thingsboard.monitoring.service.transport;

import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.monitoring.config.transport.TransportInfo;
import org.thingsboard.monitoring.config.transport.TransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.BaseHealthChecker;
import org.thingsboard.monitoring.util.ResourceUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

/**
 * 中文说明：
 * 1. `TransportHealthChecker` 是 ThingsBoard Monitoring 中负责传输层接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `TransportMonitoringConfig`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
public abstract class TransportHealthChecker<C extends TransportMonitoringConfig> extends BaseHealthChecker<C, TransportMonitoringTarget> {

    /**
     * 功能：创建 `TransportHealthChecker` 实例，并初始化必要字段。
     * 参数：
     * - `config`：配置对象。
     * - `target`：`target` 参数。
     * 返回：新创建的对象实例。
     */
    public TransportHealthChecker(C config, TransportMonitoringTarget target) {
        super(config, target);
    }

    /**
     * 功能：执行 `initialize` 对应的处理。
     * 参数：
     * - `tbClient`：客户端对象。
     * 返回：无。
     */
    @Override
    protected void initialize(TbClient tbClient) {
        Device device = getOrCreateDevice(tbClient);
        DeviceCredentials credentials = tbClient.getDeviceCredentialsByDeviceId(device.getId())
                .orElseThrow(() -> new IllegalArgumentException("No credentials found for device " + device.getId()));

        DeviceConfig deviceConfig = new DeviceConfig();
        deviceConfig.setId(device.getId().toString());
        deviceConfig.setName(device.getName());
        deviceConfig.setCredentials(credentials);
        target.setDevice(deviceConfig);
    }

    /**
     * 功能：保存或创建消息载荷。
     * 参数：
     * - `testValue`：值。
     * 返回：文本结果。
     */
    @Override
    protected String createTestPayload(String testValue) {
        return JacksonUtil.newObjectNode().set(TEST_TELEMETRY_KEY, new TextNode(testValue)).toString();
    }

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Object getInfo() {
        return new TransportInfo(getTransportType(), target.getBaseUrl(), target.getQueue());
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getKey() {
        return getTransportType().name().toLowerCase() + (target.getQueue().equals("Main") ? "" : target.getQueue()) + "Transport";
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract TransportType getTransportType();


    /**
     * 功能：获取设备。
     * 参数：
     * - `tbClient`：客户端对象。
     * 返回：处理结果。
     */
    private Device getOrCreateDevice(TbClient tbClient) {
        TransportType transportType = config.getTransportType();
        String deviceName = String.format("%s (%s) - %s", transportType.getName(), target.getQueue(), target.getBaseUrl());
        Device device = tbClient.getTenantDevice(deviceName).orElse(null);
        if (device != null) {
            return device;
        }

        log.info("Creating new device '{}'", deviceName);
        device = new Device();
        device.setName(deviceName);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId(RandomStringUtils.randomAlphabetic(20));
        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());

        DeviceProfile deviceProfile = getOrCreateDeviceProfile(tbClient);
        device.setType(deviceProfile.getName());
        device.setDeviceProfileId(deviceProfile.getId());

        if (transportType != TransportType.LWM2M) {
            deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
            credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        } else {
            deviceData.setTransportConfiguration(new Lwm2mDeviceTransportConfiguration());
            credentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
            LwM2MDeviceCredentials lwm2mCreds = new LwM2MDeviceCredentials();
            NoSecClientCredential client = new NoSecClientCredential();
            client.setEndpoint(credentials.getCredentialsId());
            lwm2mCreds.setClient(client);
            LwM2MBootstrapClientCredentials bootstrap = new LwM2MBootstrapClientCredentials();
            bootstrap.setBootstrapServer(new NoSecBootstrapClientCredential());
            bootstrap.setLwm2mServer(new NoSecBootstrapClientCredential());
            lwm2mCreds.setBootstrap(bootstrap);
            credentials.setCredentialsValue(JacksonUtil.toString(lwm2mCreds));
        }

        return tbClient.saveDeviceWithCredentials(device, credentials).get();
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tbClient`：客户端对象。
     * 返回：处理结果。
     */
    private DeviceProfile getOrCreateDeviceProfile(TbClient tbClient) {
        TransportType transportType = config.getTransportType();
        String profileName = String.format("%s (%s)", transportType.getName(), target.getQueue());
        DeviceProfile deviceProfile = tbClient.getDeviceProfiles(new PageLink(1, 0, profileName)).getData()
                .stream().findFirst().orElse(null);
        if (deviceProfile != null) {
            return deviceProfile;
        }

        log.info("Creating new device profile '{}'", profileName);
        if (transportType != TransportType.LWM2M) {
            deviceProfile = new DeviceProfile();
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
            DeviceProfileData profileData = new DeviceProfileData();
            profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
            profileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
            deviceProfile.setProfileData(profileData);
        } else {
            tbClient.getResources(new PageLink(1, 0, "lwm2m monitoring")).getData()
                    .stream().findFirst()
                    .orElseGet(() -> {
                        TbResource newResource = ResourceUtils.getResource("lwm2m/resource.json", TbResource.class);
                        log.info("Creating LwM2M resource");
                        return tbClient.saveResource(newResource);
                    });
            deviceProfile = ResourceUtils.getResource("lwm2m/device_profile.json", DeviceProfile.class);
        }

        deviceProfile.setName(profileName);
        deviceProfile.setDefaultQueueName(target.getQueue());
        return tbClient.saveDeviceProfile(deviceProfile);
    }

}
