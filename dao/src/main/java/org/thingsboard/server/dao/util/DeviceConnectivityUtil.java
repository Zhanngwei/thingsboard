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
 * 1. 类目的：`DeviceConnectivityUtil` 是 ThingsBoard DAO 模块 中的DAO 工具和配置类型，用于提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 生命周期：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Utility / Factory / Template。
 */
public class DeviceConnectivityUtil {

    /**
     * 字段说明：
     * 1. 保存 `HTTP` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    /**
     * 字段说明：
     * 1. 保存 `MQTT` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String MQTT = "mqtt";
    public static final String DOCKER = "docker";
    /**
     * 字段说明：
     * 1. 保存 `MQTTS` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String MQTTS = "mqtts";
    public static final String COAP = "coap";
    /**
     * 字段说明：
     * 1. 保存 `COAPS` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String COAPS = "coaps";
    public static final String CA_ROOT_CERT_PEM = "ca-root.pem";
    /**
     * 字段说明：
     * 1. 保存 `DOCKER_COMPOSE_YML` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String DOCKER_COMPOSE_YML = "docker-compose.yml";
    public static final String CHECK_DOCUMENTATION = "Check documentation";
    /**
     * 字段说明：
     * 1. 保存 `JSON_EXAMPLE_PAYLOAD` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String JSON_EXAMPLE_PAYLOAD = "\"{temperature:25}\"";
    public static final String DOCKER_RUN = "docker run --rm -it ";
    /**
     * 字段说明：
     * 1. 保存 `HOST_DOCKER_INTERNAL` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String HOST_DOCKER_INTERNAL = "host.docker.internal";
    public static final String ADD_DOCKER_INTERNAL_HOST = "--add-host=" + HOST_DOCKER_INTERNAL + ":host-gateway ";
    /**
     * 字段说明：
     * 1. 保存 `MQTT_IMAGE` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String MQTT_IMAGE = "thingsboard/mosquitto-clients ";
    public static final String COAP_IMAGE = "thingsboard/coap-clients ";
    private final static Pattern VALID_URL_PATTERN = Pattern.compile("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    /**
     * 方法说明：
     * 1. 职责：执行 `getHttpPublishCommand` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static String getHttpPublishCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        return String.format("curl -v -X POST %s://%s%s/api/v1/%s/telemetry --header Content-Type:application/json --data " + JSON_EXAMPLE_PAYLOAD,
                protocol, host, port, deviceCredentials.getCredentialsId());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getMqttPublishCommand` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static String getMqttPublishCommand(String protocol, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        StringBuilder command = new StringBuilder("mosquitto_pub -d -q 1");
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (MQTTS.equals(protocol)) {
            command.append(" --cafile ").append(CA_ROOT_CERT_PEM);
        }
        command.append(" -h ").append(host).append(StringUtils.isBlank(port) ? "" : " -p " + port);
        command.append(" -t ").append(deviceTelemetryTopic);

        // 根据实体类型、查询类型或数据库方言分支，保持不同持久化路径的语义隔离。
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                command.append(" -u \"").append(deviceCredentials.getCredentialsId()).append("\"");
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                if (credentials != null) {
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (credentials.getClientId() != null) {
                        command.append(" -i \"").append(credentials.getClientId()).append("\"");
                    }
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (credentials.getUserName() != null) {
                        command.append(" -u \"").append(credentials.getUserName()).append("\"");
                    }
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
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
     * 方法说明：
     * 1. 职责：执行 `getGatewayDockerComposeFile` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
        // 根据实体类型、查询类型或数据库方言分支，保持不同持久化路径的语义隔离。
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                dockerComposeBuilder.append("      - accessToken=").append(deviceCredentials.getCredentialsId()).append("\n");
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                if (credentials != null) {
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (credentials.getClientId() != null) {
                        dockerComposeBuilder.append("      - clientId=").append(credentials.getClientId()).append("\n");
                    }
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (credentials.getUserName() != null) {
                        dockerComposeBuilder.append("      - username=").append(credentials.getUserName()).append("\n");
                    }
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
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
     * 方法说明：
     * 1. 职责：执行 `getDockerMqttPublishCommand` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static String getDockerMqttPublishCommand(String protocol, String baseUrl, String host, String port, String deviceTelemetryTopic, DeviceCredentials deviceCredentials) {
        String mqttCommand = getMqttPublishCommand(protocol, host, port, deviceTelemetryTopic, deviceCredentials);

        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (mqttCommand == null) {
            return null;
        }

        StringBuilder mqttDockerCommand = new StringBuilder();
        mqttDockerCommand.append(DOCKER_RUN).append(isLocalhost(host) ? ADD_DOCKER_INTERNAL_HOST : "").append(MQTT_IMAGE);

        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (isLocalhost(host)) {
            mqttCommand = mqttCommand.replace(host, HOST_DOCKER_INTERNAL);
        }

        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
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
     * 方法说明：
     * 1. 职责：执行 `getCurlPemCertCommand` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static String getCurlPemCertCommand(String baseUrl, String protocol) {
        return getCurlPemCertCommand(baseUrl, protocol, CA_ROOT_CERT_PEM);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCurlPemCertCommand` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static String getCurlPemCertCommand(String baseUrl, String protocol, String caCertFilePath) {
        return String.format("curl -f -S -o %s %s/api/device-connectivity/%s/certificate/download", caCertFilePath, baseUrl, protocol);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCoapPublishCommand` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
     * 方法说明：
     * 1. 职责：执行 `getDockerCoapPublishCommand` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static String getDockerCoapPublishCommand(String protocol, String host, String port, DeviceCredentials deviceCredentials) {
        String coapCommand = getCoapPublishCommand(protocol, host, port, deviceCredentials);
        if (coapCommand != null && isLocalhost(host)) {
            coapCommand = coapCommand.replace(host, HOST_DOCKER_INTERNAL);
        }
        return coapCommand != null ? String.format("%s%s%s", DOCKER_RUN + (isLocalhost(host) ? ADD_DOCKER_INTERNAL_HOST : ""), COAP_IMAGE, coapCommand) : null;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getHost` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
     * 方法说明：
     * 1. 职责：执行 `getPort` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static String getPort(DeviceConnectivityInfo properties) {
        return StringUtils.isBlank(properties.getPort()) ? "" : properties.getPort();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isLocalhost` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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

/*
 * 本类总结：
 * 1. 核心职责：`DeviceConnectivityUtil` 在 ThingsBoard DAO 模块 中承担DAO 工具和配置类型职责，核心目的是提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 核心流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
 * 3. 关键依赖：主要依赖或协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
