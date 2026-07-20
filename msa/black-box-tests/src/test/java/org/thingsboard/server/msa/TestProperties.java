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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;

/**
 * 中文说明：
 * 1. `TestProperties` 是 ThingsBoard Microservices 中描述 `Test Properties` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Slf4j
public class TestProperties {

    /**
     * URL 地址常量，用于统一引用固定值。
     */
    private static final String HTTPS_URL = "https://localhost";

    /**
     * URL 地址常量，用于统一引用固定值。
     */
    private static final String WSS_URL = "wss://localhost";

    private static final ContainerTestSuite instance = ContainerTestSuite.getInstance();

    /**
     * 功能：获取基础访问地址。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String getBaseUrl() {
        if (instance.isActive()) {
            return HTTPS_URL;
        }
        return System.getProperty("tb.baseUrl", "http://localhost:8080");
    }

    /**
     * 功能：获取基础访问地址。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String getBaseUiUrl() {
        if (instance.isActive()) {
            //return "https://host.docker.internal"; // this alternative requires docker-selenium.yml extra_hosts: - "host.docker.internal:host-gateway"
            //return "https://" + DockerClientFactory.instance().dockerHostIpAddress(); //this alternative will get Docker IP from testcontainers
            return "https://haproxy"; //communicate inside current docker-compose network to the load balancer container
        }
        return System.getProperty("tb.baseUiUrl", "http://localhost:8080");
    }

    /**
     * 功能：获取URL 地址。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String getWebSocketUrl() {
        if (instance.isActive()) {
            return WSS_URL;
        }
        return System.getProperty("tb.wsUrl", "ws://localhost:8080");
    }

    /**
     * 功能：获取URL 地址。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String getMqttBrokerUrl() {
        if (instance.isActive()) {
            String host = instance.getTestContainer().getServiceHost("broker", 1883);
            Integer port = instance.getTestContainer().getServicePort("broker", 1883);
            return "tcp://" + host + ":" + port;
        }
        return System.getProperty("mqtt.broker", "tcp://localhost:1883");
    }
}
