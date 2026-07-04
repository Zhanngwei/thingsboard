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
package org.thingsboard.client.tools;

/**
 * @author Valerii Sosliuk
 * This class is intended for manual MQTT SSL Testing
 */

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.thingsboard.server.common.data.ResourceUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`MqttSslClient` 是 ThingsBoard Tools 模块 中的MQTT SSL 手工验证客户端，用于加载 JKS 证书并通过 Eclipse Paho 建立 MQTT over TLS 连接，向 ThingsBoard 设备遥测主题发布测试数据。
 * 2. 所属模块：位于 tools，服务于 ThingsBoard 的客户端访问、离线工具或独立协议接入边界。
 * 3. 协作模块：主要协作对象包括 Paho MqttAsyncClient、SSLContext、KeyStore、ResourceUtils、ThingsBoard MQTT transport。
 * 4. 生命周期：由 main 方法启动，建立连接、发布一次遥测、断开连接后退出进程。
 * 5. 存在原因：独立手测工具便于快速验证证书、TLS 握手和 MQTT transport 配置，不需要启动完整测试框架。
 * 6. 事务：不参与在线事务；迁移工具生成离线 SSTable 文件，由 Cassandra 导入流程承担最终写入。
 * 7. 缓存：使用内存 Map/Set 缓存 dump 中的字典、实体类型和分区键，生命周期限定在单次迁移命令内。
 * 8. MQTT：直接涉及 MQTT，负责 CONNECT、TLS socket、PUBLISH 和 DISCONNECT 的客户端侧验证。
 * 9. Actor 通信：消息进入 ThingsBoard MQTT transport 后才可能转换为 Actor 消息，本工具只负责协议入口。
 * 10. 数据库：迁移工具面向 PostgreSQL dump 和 Cassandra SSTable 文件，属于离线数据库迁移辅助逻辑。
 * 11. Rule Engine：发布的遥测进入服务端后可能触发 Rule Engine，本工具不执行规则链。
 * 12. 设计模式：主要体现 Command / Client Adapter。
 */
public class MqttSslClient {


    /**
     * 字段说明：
     * 1. 保存内容：`MQTT_URL` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `MQTT SSL 手工验证客户端` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
     */
    private static final String MQTT_URL = "ssl://localhost:1883";

    private static final String CLIENT_ID = "MQTT_SSL_JAVA_CLIENT";
    /**
     * 字段说明：
     * 1. 保存内容：`KEY_STORE_FILE` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `MQTT SSL 手工验证客户端` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
     */
    private static final String KEY_STORE_FILE = "mqttclient.jks";
    private static final String JKS="JKS";
    private static final String TLS="TLS";

    /**
     * 方法说明：
     * 1. 职责：`main` 执行 MQTT SSL 手工验证客户端 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由 main 方法启动，建立连接、发布一次遥测、断开连接后退出进程；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：加载 trust/key store，初始化 SSLContext，配置 MQTT socket factory，连接 broker，发布遥测并断开。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：不参与在线事务；迁移工具生成离线 SSTable 文件，由 Cassandra 导入流程承担最终写入。
     * 9. 缓存：使用内存 Map/Set 缓存 dump 中的字典、实体类型和分区键，生命周期限定在单次迁移命令内。
     * 10. MQTT：直接涉及 MQTT，负责 CONNECT、TLS socket、PUBLISH 和 DISCONNECT 的客户端侧验证。
     * 11. Actor 通信：消息进入 ThingsBoard MQTT transport 后才可能转换为 Actor 消息，本工具只负责协议入口。
     * 12. 数据库：迁移工具面向 PostgreSQL dump 和 Cassandra SSTable 文件，属于离线数据库迁移辅助逻辑。
     * 13. Rule Engine：发布的遥测进入服务端后可能触发 Rule Engine，本工具不执行规则链。
     */
    public static void main(String[] args) {

        try {
            // TLS 上下文同时配置客户端证书和信任链，用来验证 MQTT SSL 双向认证配置是否正确。
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            KeyStore trustStore = KeyStore.getInstance(JKS);
            char[] ksPwd = new char[]{0x63, 0x6C, 0x69, 0x65, 0x6E, 0x74, 0x5F, 0x6B, 0x73, 0x5F, 0x70, 0x61, 0x73, 0x73, 0x77, 0x6F, 0x72, 0x64};
            trustStore.load(ResourceUtils.getInputStream(MqttSslClient.class.getClassLoader(), KEY_STORE_FILE), ksPwd);
            tmf.init(trustStore);
            KeyStore ks = KeyStore.getInstance(JKS);

            ks.load(ResourceUtils.getInputStream(MqttSslClient.class.getClassLoader(), KEY_STORE_FILE), ksPwd);
            // TLS 上下文同时配置客户端证书和信任链，用来验证 MQTT SSL 双向认证配置是否正确。
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            char[] clientPwd = new char[]{0x63, 0x6C, 0x69, 0x65, 0x6E, 0x74, 0x5F, 0x6B, 0x65, 0x79, 0x5F, 0x70, 0x61, 0x73, 0x73, 0x77, 0x6F, 0x72, 0x64};
            kmf.init(ks, clientPwd);

            KeyManager[] km = kmf.getKeyManagers();
            TrustManager[] tm = tmf.getTrustManagers();
            // TLS 上下文同时配置客户端证书和信任链，用来验证 MQTT SSL 双向认证配置是否正确。
            SSLContext sslContext = SSLContext.getInstance(TLS);
            // TLS 上下文同时配置客户端证书和信任链，用来验证 MQTT SSL 双向认证配置是否正确。
            sslContext.init(km, tm, null);

            MqttConnectOptions options = new MqttConnectOptions();
            // TLS 上下文同时配置客户端证书和信任链，用来验证 MQTT SSL 双向认证配置是否正确。
            options.setSocketFactory(sslContext.getSocketFactory());
            // MQTT 操作只用于手工验证连接、发布和断开链路，真实设备会话由 transport 服务端管理。
            MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, CLIENT_ID, new MemoryPersistence());
            // MQTT 操作只用于手工验证连接、发布和断开链路，真实设备会话由 transport 服务端管理。
            client.connect(options);
            Thread.sleep(3000);
            MqttMessage message = new MqttMessage();
            message.setPayload("{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4}".getBytes());
            // MQTT 操作只用于手工验证连接、发布和断开链路，真实设备会话由 transport 服务端管理。
            client.publish("v1/devices/me/telemetry", message);
            // MQTT 操作只用于手工验证连接、发布和断开链路，真实设备会话由 transport 服务端管理。
            client.disconnect();
            log.info("Disconnected");
            System.exit(0);
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (Exception e) {
            log.error("Unexpected exception occurred in MqttSslClient", e);
        }
    }
    /**
     * 本类总结：
     * 1. 核心职责：`MqttSslClient` 负责加载 JKS 证书并通过 Eclipse Paho 建立 MQTT over TLS 连接，向 ThingsBoard 设备遥测主题发布测试数据。
     * 2. 核心流程：加载 trust/key store，初始化 SSLContext，配置 MQTT socket factory，连接 broker，发布遥测并断开。
     * 3. 关键依赖：Paho MqttAsyncClient、SSLContext、KeyStore、ResourceUtils、ThingsBoard MQTT transport。
     * 4. 设计重点：通过 Command / Client Adapter 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}