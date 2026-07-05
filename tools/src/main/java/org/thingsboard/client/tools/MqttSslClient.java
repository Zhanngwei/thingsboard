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
@Slf4j
public class MqttSslClient {


    /**
     * URL 地址常量，用于统一引用固定值。
     */
    private static final String MQTT_URL = "ssl://localhost:1883";

    private static final String CLIENT_ID = "MQTT_SSL_JAVA_CLIENT";
    /**
     * 键常量，用于统一引用固定值。
     */
    private static final String KEY_STORE_FILE = "mqttclient.jks";
    private static final String JKS="JKS";
    private static final String TLS="TLS";

    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) {

        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            KeyStore trustStore = KeyStore.getInstance(JKS);
            char[] ksPwd = new char[]{0x63, 0x6C, 0x69, 0x65, 0x6E, 0x74, 0x5F, 0x6B, 0x73, 0x5F, 0x70, 0x61, 0x73, 0x73, 0x77, 0x6F, 0x72, 0x64};
            trustStore.load(ResourceUtils.getInputStream(MqttSslClient.class.getClassLoader(), KEY_STORE_FILE), ksPwd);
            tmf.init(trustStore);
            KeyStore ks = KeyStore.getInstance(JKS);

            ks.load(ResourceUtils.getInputStream(MqttSslClient.class.getClassLoader(), KEY_STORE_FILE), ksPwd);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            char[] clientPwd = new char[]{0x63, 0x6C, 0x69, 0x65, 0x6E, 0x74, 0x5F, 0x6B, 0x65, 0x79, 0x5F, 0x70, 0x61, 0x73, 0x73, 0x77, 0x6F, 0x72, 0x64};
            kmf.init(ks, clientPwd);

            KeyManager[] km = kmf.getKeyManagers();
            TrustManager[] tm = tmf.getTrustManagers();
            SSLContext sslContext = SSLContext.getInstance(TLS);
            sslContext.init(km, tm, null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(sslContext.getSocketFactory());
            MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, CLIENT_ID, new MemoryPersistence());
            client.connect(options);
            Thread.sleep(3000);
            MqttMessage message = new MqttMessage();
            message.setPayload("{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4}".getBytes());
            client.publish("v1/devices/me/telemetry", message);
            client.disconnect();
            log.info("Disconnected");
            System.exit(0);
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