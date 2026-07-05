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
 * 1. 类目的：`SnmpTestV3` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`SnmpTestV3` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
