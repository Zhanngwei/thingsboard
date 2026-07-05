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
package org.thingsboard.server.dao.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.util.DeviceConnectivityUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.CHECK_DOCUMENTATION;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.COAP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.COAPS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.DOCKER;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTP;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.HTTPS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTT;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.MQTTS;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.getHost;
import static org.thingsboard.server.dao.util.DeviceConnectivityUtil.getPort;

/**
 * 中文说明：
 * 1. 类目的：`DeviceConnectivityServiceImpl` 是 ThingsBoard DAO 模块 中的设备持久化服务类型，用于维护设备、凭据、配置、声明、连接状态和设备画像相关的数据库访问流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DeviceService、DeviceCredentialsService、Transport、Rule Engine、缓存和审计服务。
 * 4. 生命周期：由设备创建、认证、更新、删除、声明或传输层认证流程触发，随事务完成后释放上下文。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Factory。
 */
@Service("DeviceConnectivityDaoService")
@Slf4j
@RequiredArgsConstructor
public class DeviceConnectivityServiceImpl implements DeviceConnectivityService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEFAULT_DEVICE_TELEMETRY_TOPIC = "v1/devices/me/telemetry";
    public static final String HTTP_DEFAULT_PORT = "80";
    /**
     * 端口号常量，用于统一引用固定值。
     */
    public static final String HTTPS_DEFAULT_PORT = "443";

    private final Map<String, Resource> certs = new ConcurrentHashMap<>();

    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    private final DeviceCredentialsService deviceCredentialsService;
    private final DeviceProfileService deviceProfileService;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final AdminSettingsService adminSettingsService;

    /**
     * 文件，用于定位本地文件或目录。
     */
    @Value("${device.connectivity.mqtts.pem_cert_file:}")
    private String mqttsPemCertFile;

    /**
     * 功能：获取设备。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    public JsonNode findDevicePublishTelemetryCommands(String baseUrl, Device device) throws URISyntaxException {
        DeviceId deviceId = device.getId();
        log.trace("Executing findDevicePublishTelemetryCommands [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);

        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), deviceId);
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
        DeviceTransportType transportType = deviceProfile.getTransportType();

        ObjectNode commands = JacksonUtil.newObjectNode();
        switch (transportType) {
            case DEFAULT:
                Optional.ofNullable(getHttpTransportPublishCommands(baseUrl, creds))
                        .ifPresent(v -> commands.set(HTTP, v));
                Optional.ofNullable(getMqttTransportPublishCommands(baseUrl, creds))
                        .ifPresent(v -> commands.set(MQTT, v));
                Optional.ofNullable(getCoapTransportPublishCommands(baseUrl, creds))
                        .ifPresent(v -> commands.set(COAP, v));
                break;
            case MQTT:
                MqttDeviceProfileTransportConfiguration transportConfiguration =
                        (MqttDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
                //TODO: add sparkplug command with emulator (check SSL)
                if (transportConfiguration.isSparkplug()) {
                    ObjectNode sparkplug = JacksonUtil.newObjectNode();
                    sparkplug.put("sparkplug", CHECK_DOCUMENTATION);
                    commands.set(MQTT, sparkplug);
                } else {
                    String topicName = transportConfiguration.getDeviceTelemetryTopic();

                    Optional.ofNullable(getMqttTransportPublishCommands(baseUrl, topicName, creds))
                            .ifPresent(v -> commands.set(MQTT, v));
                }
                break;
            case COAP:
                Optional.ofNullable(getCoapTransportPublishCommands(baseUrl, creds))
                        .ifPresent(v -> commands.set(COAP, v));
                break;
            default:
                commands.put(transportType.name(), CHECK_DOCUMENTATION);
        }
        return commands;
    }

    /**
     * 功能：获取文件。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * 返回：处理结果。
     */
    @Override
    public Resource getPemCertFile(String protocol) {
        return certs.computeIfAbsent(protocol, key -> {
            DeviceConnectivityInfo connectivity = getConnectivity(protocol);
            if (!MQTTS.equals(protocol) || connectivity == null) {
                log.warn("Unknown connectivity protocol: {}", protocol);
                return null;
            }

            if (StringUtils.isNotBlank(mqttsPemCertFile) && ResourceUtils.resourceExists(this, mqttsPemCertFile)) {
                try {
                    return getCert(mqttsPemCertFile);
                } catch (Exception e) {
                    String msg = String.format("Failed to read %s server certificate!", protocol);
                    log.warn(msg);
                    throw new RuntimeException(msg, e);
                }
            } else {
                return null;
            }
        });
    }

    /**
     * 功能：保存或创建文件。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    public Resource createGatewayDockerComposeFile(String baseUrl, Device device) throws URISyntaxException {
        String mqttType = isEnabled(MQTTS) ? MQTTS : MQTT;
        DeviceConnectivityInfo properties = getConnectivity(mqttType);
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId());
        return DeviceConnectivityUtil.getGatewayDockerComposeFile(baseUrl, properties, creds, mqttType);
    }

    /**
     * 功能：获取`Connectivity`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * 返回：处理结果。
     */
    private DeviceConnectivityInfo getConnectivity(String protocol) {
        AdminSettings connectivitySettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "connectivity");
        JsonNode connectivity;
        if (connectivitySettings != null && (connectivity = connectivitySettings.getJsonValue()) != null) {
            return JacksonUtil.convertValue(connectivity.get(protocol), DeviceConnectivityInfo.class);
        }
        return null;
    }

    /**
     * 功能：判断`Enabled`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * 返回：判断结果。
     */
    public boolean isEnabled(String protocol) {
        var info = getConnectivity(protocol);
        return info != null && info.isEnabled();
    }

    /**
     * 功能：获取`Cert`。
     * 参数：
     * - `path`：文件或资源路径。
     * 返回：处理结果。
     */
    private Resource getCert(String path) throws Exception {
        StringBuilder pemContentBuilder = new StringBuilder();

        try (InputStream inStream = ResourceUtils.getInputStream(this, path);
             PEMParser pemParser = new PEMParser(new InputStreamReader(inStream))) {

            Object object;

            while ((object = pemParser.readObject()) != null) {
                if (object instanceof X509CertificateHolder) {
                    var certHolder = (X509CertificateHolder) object;
                    String certBase64 = Base64.getEncoder().encodeToString(certHolder.getEncoded());

                    pemContentBuilder.append("-----BEGIN CERTIFICATE-----\n");
                    int index = 0;
                    while (index < certBase64.length()) {
                        pemContentBuilder.append(certBase64, index, Math.min(index + 64, certBase64.length()));
                        pemContentBuilder.append("\n");
                        index += 64;
                    }
                    pemContentBuilder.append("-----END CERTIFICATE-----\n");
                }
            }
        }

        return new ByteArrayResource(pemContentBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 功能：获取传输层。
     * 参数：
     * - `defaultHostname`：名称。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private JsonNode getHttpTransportPublishCommands(String defaultHostname, DeviceCredentials deviceCredentials) throws URISyntaxException {
        ObjectNode httpCommands = JacksonUtil.newObjectNode();
        Optional.ofNullable(getHttpPublishCommand(HTTP, defaultHostname, deviceCredentials))
                .ifPresent(v -> httpCommands.put(HTTP, v));
        Optional.ofNullable(getHttpPublishCommand(HTTPS, defaultHostname, deviceCredentials))
                .ifPresent(v -> httpCommands.put(HTTPS, v));
        return httpCommands.isEmpty() ? null : httpCommands;
    }

    /**
     * 功能：获取`Http Publish Command`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * - `baseUrl`：`baseUrl` 参数。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：文本结果。
     */
    private String getHttpPublishCommand(String protocol, String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(protocol);
        if (properties == null || !properties.isEnabled() ||
                deviceCredentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            return null;
        }
        String hostName = getHost(baseUrl, properties, protocol);
        String propertiesPort = getPort(properties);
        String port = (propertiesPort.isEmpty() || HTTP_DEFAULT_PORT.equals(propertiesPort) || HTTPS_DEFAULT_PORT.equals(propertiesPort))
                ? "" : ":" + propertiesPort;
        return DeviceConnectivityUtil.getHttpPublishCommand(protocol, hostName, port, deviceCredentials);
    }

    /**
     * 功能：获取传输层。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private JsonNode getMqttTransportPublishCommands(String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        return getMqttTransportPublishCommands(baseUrl, DEFAULT_DEVICE_TELEMETRY_TOPIC, deviceCredentials);
    }

    /**
     * 功能：获取传输层。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `topic`：主题名称或主题对象。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private JsonNode getMqttTransportPublishCommands(String baseUrl, String topic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        ObjectNode mqttCommands = JacksonUtil.newObjectNode();

        if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.X509_CERTIFICATE) {
            mqttCommands.put(MQTTS, CHECK_DOCUMENTATION);
            return mqttCommands;
        }

        ObjectNode dockerMqttCommands = JacksonUtil.newObjectNode();

        if (isEnabled(MQTT)) {
            Optional.ofNullable(getMqttPublishCommand(baseUrl, topic, deviceCredentials)).
                    ifPresent(v -> mqttCommands.put(MQTT, v));

            Optional.ofNullable(getDockerMqttPublishCommand(MQTT, baseUrl, topic, deviceCredentials))
                    .ifPresent(v -> dockerMqttCommands.put(MQTT, v));
        }

        if (isEnabled(MQTTS)) {
            List<String> mqttsPublishCommand = getMqttsPublishCommand(baseUrl, topic, deviceCredentials);
            if (mqttsPublishCommand != null) {
                ArrayNode arrayNode = mqttCommands.putArray(MQTTS);
                mqttsPublishCommand.forEach(arrayNode::add);
            }

            Optional.ofNullable(getDockerMqttPublishCommand(MQTTS, baseUrl, topic, deviceCredentials))
                    .ifPresent(v -> dockerMqttCommands.put(MQTTS, v));
        }

        if (!dockerMqttCommands.isEmpty()) {
            mqttCommands.set(DOCKER, dockerMqttCommands);
        }
        return mqttCommands.isEmpty() ? null : mqttCommands;
    }

    /**
     * 功能：获取`Mqtt Publish Command`。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `deviceTelemetryTopic`：设备信息或设备标识。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：文本结果。
     */
    private String getMqttPublishCommand(String baseUrl, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(MQTT);
        String mqttHost = getHost(baseUrl, properties, MQTT);
        String mqttPort = getPort(properties);
        return DeviceConnectivityUtil.getMqttPublishCommand(MQTT, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);
    }

    /**
     * 功能：获取`Mqtts Publish Command`。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `deviceTelemetryTopic`：设备信息或设备标识。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    private List<String> getMqttsPublishCommand(String baseUrl, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(MQTTS);
        String mqttHost = getHost(baseUrl, properties, MQTTS);
        String mqttPort = getPort(properties);
        String pubCommand = DeviceConnectivityUtil.getMqttPublishCommand(MQTTS, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);

        ArrayList<String> commands = new ArrayList<>();
        if (pubCommand != null) {
            commands.add(DeviceConnectivityUtil.getCurlPemCertCommand(baseUrl, MQTTS));
            commands.add(pubCommand);
            return commands;
        }
        return null;
    }

    /**
     * 功能：获取`Docker Mqtt Publish Command`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * - `baseUrl`：`baseUrl` 参数。
     * - `deviceTelemetryTopic`：设备信息或设备标识。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：文本结果。
     */
    private String getDockerMqttPublishCommand(String protocol, String baseUrl, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(protocol);
        String mqttHost = getHost(baseUrl, properties, protocol);
        String mqttPort = getPort(properties);
        return DeviceConnectivityUtil.getDockerMqttPublishCommand(protocol, baseUrl, mqttHost, mqttPort, deviceTelemetryTopic, deviceCredentials);
    }

    /**
     * 功能：获取传输层。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private JsonNode getCoapTransportPublishCommands(String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        ObjectNode coapCommands = JacksonUtil.newObjectNode();

        if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.X509_CERTIFICATE) {
            coapCommands.put(COAPS, CHECK_DOCUMENTATION);
            return coapCommands;
        }

        ObjectNode dockerCoapCommands = JacksonUtil.newObjectNode();

        if (isEnabled(COAP)) {
            Optional.ofNullable(getCoapPublishCommand(COAP, baseUrl, deviceCredentials))
                    .ifPresent(v -> coapCommands.put(COAP, v));

            Optional.ofNullable(getDockerCoapPublishCommand(COAP, baseUrl, deviceCredentials))
                    .ifPresent(v -> dockerCoapCommands.put(COAP, v));
        }

        if (isEnabled(COAPS)) {
            Optional.ofNullable(getCoapPublishCommand(COAPS, baseUrl, deviceCredentials))
                    .ifPresent(v -> coapCommands.put(COAPS, v));

            Optional.ofNullable(getDockerCoapPublishCommand(COAPS, baseUrl, deviceCredentials))
                    .ifPresent(v -> dockerCoapCommands.put(COAPS, v));
        }

        if (!dockerCoapCommands.isEmpty()) {
            coapCommands.set(DOCKER, dockerCoapCommands);
        }

        return coapCommands.isEmpty() ? null : coapCommands;
    }

    /**
     * 功能：获取`Coap Publish Command`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * - `baseUrl`：`baseUrl` 参数。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：文本结果。
     */
    private String getCoapPublishCommand(String protocol, String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(protocol);
        String hostName = getHost(baseUrl, properties, protocol);
        String port = StringUtils.isBlank(properties.getPort()) ? "" : ":" + properties.getPort();
        return DeviceConnectivityUtil.getCoapPublishCommand(protocol, hostName, port, deviceCredentials);
    }

    /**
     * 功能：获取`Docker Coap Publish Command`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * - `baseUrl`：`baseUrl` 参数。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：文本结果。
     */
    private String getDockerCoapPublishCommand(String protocol, String baseUrl, DeviceCredentials deviceCredentials) throws URISyntaxException {
        DeviceConnectivityInfo properties = getConnectivity(protocol);
        String host = getHost(baseUrl, properties, protocol);
        String port = StringUtils.isBlank(properties.getPort()) ? "" : ":" + properties.getPort();
        return DeviceConnectivityUtil.getDockerCoapPublishCommand(protocol, host, port, deviceCredentials);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceConnectivityServiceImpl` 在 ThingsBoard DAO 模块 中承担设备持久化服务类型职责，核心目的是维护设备、凭据、配置、声明、连接状态和设备画像相关的数据库访问流程。
 * 2. 核心流程：围绕租户和设备标识执行校验、DAO 调用、缓存失效、审计记录和下游通知。
 * 3. 关键依赖：主要依赖或协作对象包括DeviceService、DeviceCredentialsService、Transport、Rule Engine、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
