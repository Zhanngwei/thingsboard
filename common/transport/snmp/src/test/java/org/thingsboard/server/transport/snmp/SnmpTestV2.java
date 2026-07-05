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
package org.thingsboard.server.transport.snmp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.thingsboard.common.util.JacksonUtil;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`SnmpTestV2` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class SnmpTestV2 {

    private static final Scanner scanner = new Scanner(System.in);

    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) throws IOException {
        Map<String, String> mappings = new LinkedHashMap<>();
        for (int i = 1; i <= 50; i++) {
            String oid = String.format("1.3.6.1.2.1.%s.1.52", i);
            mappings.put(oid, "value_" + i);
        }

        SnmpDeviceSimulatorV2 device = new SnmpDeviceSimulatorV2(1610, "public", mappings);
        device.start();

        System.out.println("Hosting the following values:\n" + mappings.entrySet().stream()
                .map(entry -> entry.getKey() + " - " + entry.getValue())
                .collect(Collectors.joining("\n")));

        scanner.nextLine();
    }

    /**
     * 功能：执行 `inputTraps` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：无。
     */
    private static void inputTraps(SnmpDeviceSimulatorV2 client) throws IOException {
        while (true) {
            String data = scanner.nextLine();
            if (!data.isEmpty()) {
                client.sendTrap("127.0.0.1", 1620, Map.of(
                        "1.3.6.1.2.1.266.1.52", data + " (266)",
                        "1.3.6.1.2.1.267.1.52", data + " (267)"
                ));
            }
        }
    }

    /**
     * 功能：更新设备配置。
     * 参数：
     * - `file`：`file` 参数。
     * 返回：无。
     */
    private static void updateDeviceProfile(String file) throws Exception {
        File profileFile = new File(file);
        JsonNode deviceProfile = JacksonUtil.OBJECT_MAPPER.readTree(profileFile);
        ArrayNode mappingsJson = (ArrayNode) deviceProfile.at("/profileData/transportConfiguration/communicationConfigs/0/mappings");
        for (int i = 1; i <= 50; i++) {
            String oid = String.format(".1.3.6.1.2.1.%s.1.52", i);
            mappingsJson.add(JacksonUtil.newObjectNode()
                    .put("oid", oid)
                    .put("key", "key_" + i)
                    .put("dataType", "STRING"));
        }
        JacksonUtil.OBJECT_MAPPER.writeValue(profileFile, deviceProfile);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SnmpTestV2` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
