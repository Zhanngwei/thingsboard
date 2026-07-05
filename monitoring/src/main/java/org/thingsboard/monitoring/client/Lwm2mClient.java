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
package org.thingsboard.monitoring.client;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.response.ReadResponse;
import org.thingsboard.monitoring.util.ResourceUtils;

import javax.security.auth.Destroyable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.leshan.client.object.Security.noSec;
import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;

/**
 * 中文说明：
 * 1. 类目的：`Lwm2mClient` 是 ThingsBoard Monitoring 模块 中的监控客户端适配类型，用于封装 REST、WebSocket 或 LwM2M 客户端连接、认证、订阅和消息收发边界。
 * 2. 所属模块：位于 monitoring 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 生命周期：由 Monitoring Spring Boot 应用启动后创建，随周期性探测、失败恢复和应用关闭而运行或释放。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块通常不直接访问数据库；健康检查通过服务端 API 或协议入口间接验证后端数据库、缓存和规则链状态。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Adapter / Client。
 */
@Slf4j
public class Lwm2mClient extends BaseInstanceEnabler implements Destroyable {

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @Getter
    @Setter
    private LeshanClient leshanClient;

    private static final List<Integer> supportedResources = Collections.singletonList(0);

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private String data = "";

    /**
     * 服务端 URI，用于支撑当前网络或外部服务交互。
     */
    private String serverUri;
    private String endpoint;

    /**
     * 功能：创建 `Lwm2mClient` 实例，并初始化必要字段。
     * 参数：
     * - `serverUri`：`serverUri` 参数。
     * - `endpoint`：`endpoint` 参数。
     * 返回：新创建的对象实例。
     */
    public Lwm2mClient(String serverUri, String endpoint) {
        this.serverUri = serverUri;
        this.endpoint = endpoint;
    }

    /**
     * 功能：创建 `Lwm2mClient` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Lwm2mClient() {
    }

    /**
     * 功能：初始化或启动客户端。
     * 参数：无。
     * 返回：无。
     */
    public void initClient() throws InvalidDDFFileException, IOException {
        String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "test-model.xml"};
        List<ObjectModel> models = new ArrayList<>();
        for (String resourceName : resources) {
            models.addAll(ObjectLoader.loadDdfFile(ResourceUtils.getResourceAsStream("lwm2m/models/" + resourceName), resourceName));
        }

        Security security = noSec(serverUri, 123);
        NetworkConfig coapConfig = new NetworkConfig().setString(NetworkConfig.Keys.COAP_PORT, StringUtils.substringAfterLast(serverUri, ":"));

        LeshanClient leshanClient;

        LwM2mModel model = new StaticModel(models);
        ObjectsInitializer initializer = new ObjectsInitializer(model);
        initializer.setInstancesForObject(SECURITY, security);
        initializer.setInstancesForObject(SERVER, new Server(123, 300));
        initializer.setInstancesForObject(DEVICE, this);
        initializer.setClassForObject(ACCESS_CONTROL, DummyInstanceEnabler.class);
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(true);
        dtlsConfig.setClientOnly();

        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        engineFactory.setReconnectOnUpdate(false);
        engineFactory.setResumeOnConnect(true);

        EndpointFactory endpointFactory = new EndpointFactory() {

            @Override
            public CoapEndpoint createUnsecuredEndpoint(InetSocketAddress address, NetworkConfig coapConfig,
                                                        ObservationStore store) {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                builder.setInetSocketAddress(address);
                builder.setNetworkConfig(coapConfig);
                return builder.build();
            }

            @Override
            public CoapEndpoint createSecuredEndpoint(DtlsConnectorConfig dtlsConfig, NetworkConfig coapConfig,
                                                      ObservationStore store) {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                DtlsConnectorConfig.Builder dtlsConfigBuilder = new DtlsConnectorConfig.Builder(dtlsConfig);
                builder.setConnector(new DTLSConnector(dtlsConfigBuilder.build()));
                builder.setNetworkConfig(coapConfig);
                return builder.build();
            }
        };

        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setObjects(initializer.createAll());
        builder.setCoapConfig(coapConfig);
        builder.setDtlsConfig(dtlsConfig);
        builder.setRegistrationEngineFactory(engineFactory);
        builder.setEndpointFactory(endpointFactory);
        builder.setDecoder(new DefaultLwM2mDecoder(false));
        builder.setEncoder(new DefaultLwM2mEncoder(false));
        leshanClient = builder.build();

        setLeshanClient(leshanClient);

        leshanClient.start();
    }

    /**
     * 功能：获取`Available Resource Ids`。
     * 参数：
     * - `model`：`model` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    /**
     * 功能：执行 `read` 对应的处理。
     * 参数：
     * - `identity`：实体对象。
     * - `resourceId`：`resourceId`ID。
     * 返回：处理结果。
     */
    @Override
    public ReadResponse read(ServerIdentity identity, int resourceId) {
        if (supportedResources.contains(resourceId)) {
            return ReadResponse.success(resourceId, data);
        }
        return super.read(identity, resourceId);
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `data`：待处理数据。
     * - `resource`：`resource` 参数。
     * 返回：无。
     */
    @SneakyThrows
    public void send(String data, int resource) {
        this.data = data;
        fireResourcesChange(resource);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (leshanClient != null) {
            leshanClient.destroy(true);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`Lwm2mClient` 在 ThingsBoard Monitoring 模块 中承担监控客户端适配类型职责，核心目的是封装 REST、WebSocket 或 LwM2M 客户端连接、认证、订阅和消息收发边界。
 * 2. 核心流程：加载目标和传输配置，按协议执行健康检查，记录延迟与失败状态，并在阈值或状态变化时发送通知。
 * 3. 关键依赖：主要依赖或协作对象包括Monitoring 配置、REST 客户端、WebSocket 客户端、MQTT/HTTP/CoAP/LwM2M 探测器、Slack 通知和目标 ThingsBoard 服务。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
