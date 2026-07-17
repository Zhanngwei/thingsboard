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
package org.thingsboard.server.dao.util;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceConnectivityInfo;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. `DeviceConnectivityUtil` 是 ThingsBoard DAO 中处理设备通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class DeviceConnectivityUtil {

    /**
     * `HTTP`常量，用于统一引用固定值。
     */
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    /**
     * `MQTT`常量，用于统一引用固定值。
     */
    public static final String MQTT = "mqtt";
    public static final String DOCKER = "docker";
    /**
     * `MQTTS`常量，用于统一引用固定值。
     */
    public static final String MQTTS = "mqtts";
    public static final String COAP = "coap";
    /**
     * `COAPS`常量，用于统一引用固定值。
     */
    public static final String COAPS = "coaps";
    public static final String CA_ROOT_CERT_PEM = "ca-root.pem";
    /**
     * `DOCKER_COMPOSE_YML`常量，用于统一引用固定值。
     */
    public static final String DOCKER_COMPOSE_YML = "docker-compose.yml";
    public static final String CHECK_DOCUMENTATION = "Check documentation";
    /**
     * 消息载荷常量，用于统一引用固定值。
     */
    public static final String JSON_EXAMPLE_PAYLOAD = "\"{temperature:25}\"";
    public static final String DOCKER_RUN = "docker run --rm -it ";
    /**
     * 主机地址常量，用于统一引用固定值。
     */
    public static final String HOST_DOCKER_INTERNAL = "host.docker.internal";
    public static final String ADD_DOCKER_INTERNAL_HOST = "--add-host=" + HOST_DOCKER_INTERNAL + ":host-gateway ";
    /**
     * 图片资源常量，用于统一引用固定值。
     */
    public static final String MQTT_IMAGE = "thingsboard/mosquitto-clients ";
    public static final String COAP_IMAGE = "thingsboard/coap-clients ";
    private final static Pattern VALID_URL_PATTERN = Pattern.compile("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    /**
     * 功能：获取`Http Publish Command`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：文本结果。
     */
    public static String getHttpPublishCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        return String.format("curl -v -X POST %s://%s%s/api/v1/%s/telemetry --header Content-Type:application/json --data " + JSON_EXAMPLE_PAYLOAD,
                protocol, host, port, deviceCredentials.getCredentialsId());
    }

    /**
     * 功能：获取`Mqtt Publish Command`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `deviceTelemetryTopic`：设备信息或设备标识。
     * - 其余参数：补充处理条件。
     * 返回：文本结果。
     */
    public static String getMqttPublishCommand(String protocol, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        StringBuilder command = new StringBuilder("mosquitto_pub -d -q 1");
        if (MQTTS.equals(protocol)) {
            command.append(" --cafile ").append(CA_ROOT_CERT_PEM);
        }
        command.append(" -h ").append(host).append(StringUtils.isBlank(port) ? "" : " -p " + port);
        command.append(" -t ").append(deviceTelemetryTopic);

        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                command.append(" -u \"").append(deviceCredentials.getCredentialsId()).append("\"");
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (credentials.getClientId() != null) {
                        command.append(" -i \"").append(credentials.getClientId()).append("\"");
                    }
                    if (credentials.getUserName() != null) {
                        command.append(" -u \"").append(credentials.getUserName()).append("\"");
                    }
                    if (credentials.getPassword() != null) {
                        command.append(" -P \"").append(credentials.getPassword()).append("\"");
                    }
                } else {
                    return null;
                }
                break;
            default:
                return null;
        }
        command.append(" -m " + JSON_EXAMPLE_PAYLOAD);
        return command.toString();
    }

    /**
     * 功能：获取文件。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `properties`：`properties` 参数。
     * - `deviceCredentials`：设备信息或设备标识。
     * - `mqttType`：类型。
     * 返回：处理结果。
     */
    public static Resource getGatewayDockerComposeFile(String baseUrl, DeviceConnectivityInfo properties, DeviceCredentials deviceCredentials, String mqttType) throws URISyntaxException {
        String host = getHost(baseUrl, properties, mqttType);

        StringBuilder dockerComposeBuilder = new StringBuilder();
        dockerComposeBuilder.append("version: '3.4'\n");
        dockerComposeBuilder.append("services:\n");
        dockerComposeBuilder.append("  # ThingsBoard IoT Gateway Service Configuration\n");
        dockerComposeBuilder.append("  tb-gateway:\n");
        dockerComposeBuilder.append("    image: thingsboard/tb-gateway\n");
        dockerComposeBuilder.append("    container_name: tb-gateway\n");
        dockerComposeBuilder.append("    restart: always\n");
        dockerComposeBuilder.append("\n");
        dockerComposeBuilder.append("    # Ports bindings - required by some connectors\n");
        dockerComposeBuilder.append("    ports:\n");
        dockerComposeBuilder.append("        - \"5000:5000\" # Comment if you don't use REST connector and change if you use another port\n");
        dockerComposeBuilder.append("        # Uncomment and modify the following ports based on connector usage:\n");
        dockerComposeBuilder.append("#        - \"1052:1052\" # BACnet connector\n");
        dockerComposeBuilder.append("#        - \"5026:5026\" # Modbus TCP connector (Modbus Slave)\n");
        dockerComposeBuilder.append("#        - \"50000:50000/tcp\" # Socket connector with type TCP\n");
        dockerComposeBuilder.append("#        - \"50000:50000/udp\" # Socket connector with type UDP\n");
        dockerComposeBuilder.append("\n");
        dockerComposeBuilder.append("    # Necessary mapping for Linux\n");
        dockerComposeBuilder.append("    extra_hosts:\n");
        dockerComposeBuilder.append("      - \"host.docker.internal:host-gateway\"\n");
        dockerComposeBuilder.append("\n");
        dockerComposeBuilder.append("    # Environment variables\n");
        dockerComposeBuilder.append("    environment:\n");
        dockerComposeBuilder.append("      - host=").append(isLocalhost(host) ? HOST_DOCKER_INTERNAL : host).append("\n");
        dockerComposeBuilder.append("      - port=1883\n");
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                dockerComposeBuilder.append("      - accessToken=").append(deviceCredentials.getCredentialsId()).append("\n");
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (credentials.getClientId() != null) {
                        dockerComposeBuilder.append("      - clientId=").append(credentials.getClientId()).append("\n");
                    }
                    if (credentials.getUserName() != null) {
                        dockerComposeBuilder.append("      - username=").append(credentials.getUserName()).append("\n");
                    }
                    if (credentials.getPassword() != null) {
                        dockerComposeBuilder.append("      - password=").append(credentials.getPassword()).append("\n");
                    }
                }
                break;
        }
        dockerComposeBuilder.append("\n");
        dockerComposeBuilder.append("    # Volumes bind\n");
        dockerComposeBuilder.append("    volumes:\n");
        dockerComposeBuilder.append("      - tb-gw-config:/thingsboard_gateway/config\n");
        dockerComposeBuilder.append("      - tb-gw-logs:/thingsboard_gateway/logs\n");
        dockerComposeBuilder.append("      - tb-gw-extensions:/thingsboard_gateway/extensions\n");
        dockerComposeBuilder.append("\n");
        dockerComposeBuilder.append("# Volumes declaration for configurations, extensions and configuration\n");
        dockerComposeBuilder.append("volumes:\n");
        dockerComposeBuilder.append("  tb-gw-config:\n");
        dockerComposeBuilder.append("    name: tb-gw-config\n");
        dockerComposeBuilder.append("  tb-gw-logs:\n");
        dockerComposeBuilder.append("    name: tb-gw-logs\n");
        dockerComposeBuilder.append("  tb-gw-extensions:\n");
        dockerComposeBuilder.append("    name: tb-gw-extensions\n");

        return new ByteArrayResource(dockerComposeBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 功能：获取`Docker Mqtt Publish Command`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * - `baseUrl`：`baseUrl` 参数。
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - 其余参数：补充处理条件。
     * 返回：文本结果。
     */
    public static String getDockerMqttPublishCommand(String protocol, String baseUrl, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        String mqttCommand = getMqttPublishCommand(protocol, host, port, deviceTelemetryTopic, deviceCredentials);

        if (mqttCommand == null) {
            return null;
        }

        StringBuilder mqttDockerCommand = new StringBuilder();
        mqttDockerCommand.append(DOCKER_RUN).append(isLocalhost(host) ? ADD_DOCKER_INTERNAL_HOST : "").append(MQTT_IMAGE);

        if (isLocalhost(host)) {
            mqttCommand = mqttCommand.replace(host, HOST_DOCKER_INTERNAL);
        }

        if (MQTTS.equals(protocol)) {
            mqttDockerCommand.append("/bin/sh -c \"")
                    .append(getCurlPemCertCommand(baseUrl, protocol))
                    .append(" && ")
                    .append(mqttCommand)
                    .append("\"");
        } else {
            mqttDockerCommand.append(mqttCommand);
        }

        return mqttDockerCommand.toString();
    }

    /**
     * 功能：获取`Curl Pem Cert Command`。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `protocol`：`protocol` 参数。
     * 返回：文本结果。
     */
    public static String getCurlPemCertCommand(String baseUrl, String protocol) {
        return getCurlPemCertCommand(baseUrl, protocol, CA_ROOT_CERT_PEM);
    }

    /**
     * 功能：获取`Curl Pem Cert Command`。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `protocol`：`protocol` 参数。
     * - `caCertFilePath`：文件或资源路径。
     * 返回：文本结果。
     */
    public static String getCurlPemCertCommand(String baseUrl, String protocol, String caCertFilePath) {
        return String.format("curl -f -S -o %s %s/api/device-connectivity/%s/certificate/download", caCertFilePath, baseUrl, protocol);
    }

    /**
     * 功能：获取`Coap Publish Command`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：文本结果。
     */
    public static String getCoapPublishCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                String client = COAPS.equals(protocol) ? "coap-client-openssl" : "coap-client";
                return String.format("%s -v 6 -m POST %s://%s%s/api/v1/%s/telemetry -t json -e %s",
                        client, protocol, host, port, deviceCredentials.getCredentialsId(), JSON_EXAMPLE_PAYLOAD);
            default:
                return null;
        }
    }

    /**
     * 功能：获取`Docker Coap Publish Command`。
     * 参数：
     * - `protocol`：`protocol` 参数。
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：文本结果。
     */
    public static String getDockerCoapPublishCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        String coapCommand = getCoapPublishCommand(protocol, host, port, deviceCredentials);
        if (coapCommand != null && isLocalhost(host)) {
            coapCommand = coapCommand.replace(host, HOST_DOCKER_INTERNAL);
        }
        return coapCommand != null ? String.format("%s%s%s", DOCKER_RUN + (isLocalhost(host) ? ADD_DOCKER_INTERNAL_HOST : ""), COAP_IMAGE, coapCommand) : null;
    }

    /**
     * 功能：获取主机地址。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `properties`：`properties` 参数。
     * - `protocol`：`protocol` 参数。
     * 返回：文本结果。
     */
    public static String getHost(String baseUrl, DeviceConnectivityInfo properties, String protocol) throws URISyntaxException {
        String initialHost = StringUtils.isBlank(properties.getHost()) ? baseUrl : properties.getHost();
        InetAddress inetAddress;
        String host = null;
        if (VALID_URL_PATTERN.matcher(initialHost).matches()) {
            host = new URI(initialHost).getHost();
        }
        if (host == null) {
            host = initialHost;
        }
        try {
            host = host.replaceAll("^https?://", "");
            inetAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return host;
        }
        if (inetAddress instanceof Inet6Address) {
            host = host.replaceAll("[\\[\\]]", "");
            if (!MQTT.equals(protocol) && !MQTTS.equals(protocol)) {
                host = "[" + host + "]";
            }
        }
        return host;
    }

    /**
     * 功能：获取端口号。
     * 参数：
     * - `properties`：`properties` 参数。
     * 返回：文本结果。
     */
    public static String getPort(DeviceConnectivityInfo properties) {
        return StringUtils.isBlank(properties.getPort()) ? "" : properties.getPort();
    }

    /**
     * 功能：判断`Localhost`。
     * 参数：
     * - `host`：`host` 参数。
     * 返回：判断结果。
     */
    public static boolean isLocalhost(String host) {
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            return inetAddress.isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
