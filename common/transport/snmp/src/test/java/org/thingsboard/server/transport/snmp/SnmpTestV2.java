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
 * 1. `SnmpTestV2` 是 ThingsBoard Common Transport 中负责 SNMP 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
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
