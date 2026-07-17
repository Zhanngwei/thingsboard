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

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.mp.MPv3;
import org.snmp4j.smi.OctetString;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

/**
 * 中文说明：
 * 1. `SnmpTestV3` 是 ThingsBoard Common Transport 中负责 SNMP 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
public class SnmpTestV3 {
    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) throws IOException {
        SnmpDeviceSimulatorV3 device = new SnmpDeviceSimulatorV3(new CommandProcessor(new OctetString(MPv3.createLocalEngineID())) {
            @Override
            public void processPdu(CommandResponderEvent event) {
                System.out.println("event: " + event);
            }
        });
        device.start("0.0.0.0", "1610");

        device.setUpMappings(Map.of(
                ".1.3.6.1.2.1.1.1.50", "12",
                ".1.3.6.1.2.1.2.1.52", "56",
                ".1.3.6.1.2.1.3.1.54", "yes",
                ".1.3.6.1.2.1.7.1.58", ""
        ));

        new Scanner(System.in).nextLine();
    }
}
