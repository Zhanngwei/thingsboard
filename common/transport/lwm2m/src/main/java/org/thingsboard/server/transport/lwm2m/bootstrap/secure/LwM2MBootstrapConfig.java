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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.thingsboard.server.common.data.device.credentials.lwm2m.AbstractLwM2MBootstrapClientCredentialWithKeys;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;

import java.io.Serializable;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`LwM2MBootstrapConfig` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Data
public class LwM2MBootstrapConfig implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -4729088085817468640L;

    /**
     * 服务端列表，用于保存一组待处理对象。
     */
    List<LwM2MBootstrapServerCredential> serverConfiguration;

    /** -bootstrapServer, lwm2mServer
     * interface ServerSecurityConfig
     *   host?: string,
     *   port?: number,
     *   isBootstrapServer?: boolean,
     *   securityMode: string,
     *   clientPublicKeyOrId?: string,
     *   clientSecretKey?: string,
     *   serverPublicKey?: string;
     *   clientHoldOffTime?: number,
     *   serverId?: number,
     *   bootstrapServerAccountTimeout: number
     * */
    /**
     * 服务端，用于支撑当前网络或外部服务交互。
     */
    @Getter
    @Setter
    private LwM2MBootstrapClientCredential bootstrapServer;

    /**
     * 服务端，用于支撑当前网络或外部服务交互。
     */
    @Getter
    @Setter
    private LwM2MBootstrapClientCredential lwm2mServer;

    /**
     * 功能：创建 `LwM2MBootstrapConfig` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public LwM2MBootstrapConfig(){};

    /**
     * 功能：创建 `LwM2MBootstrapConfig` 实例，并初始化必要字段。
     * 参数：
     * - `serverConfiguration`：配置对象。
     * - `bootstrapClientServer`：客户端对象。
     * - `lwm2mClientServer`：客户端对象。
     * 返回：新创建的对象实例。
     */
    public LwM2MBootstrapConfig(List<LwM2MBootstrapServerCredential> serverConfiguration, LwM2MBootstrapClientCredential bootstrapClientServer, LwM2MBootstrapClientCredential lwm2mClientServer) {
        this.serverConfiguration = serverConfiguration;
        this.bootstrapServer = bootstrapClientServer;
        this.lwm2mServer = lwm2mClientServer;

    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public BootstrapConfig getLwM2MBootstrapConfig() {
        BootstrapConfig configBs = new BootstrapConfig();
        configBs.autoIdForSecurityObject = true;
        int id = 0;
        for (LwM2MBootstrapServerCredential serverCredential : serverConfiguration) {
            BootstrapConfig.ServerConfig serverConfig = setServerConfig((AbstractLwM2MBootstrapServerCredential) serverCredential);
            configBs.servers.put(id, serverConfig);
            BootstrapConfig.ServerSecurity serverSecurity = setServerSecurity((AbstractLwM2MBootstrapServerCredential) serverCredential, serverCredential.getSecurityMode());
            configBs.security.put(id, serverSecurity);
            id++;
        }
        /** in LwM2mDefaultBootstrapSessionManager -> initTasks
         * Delete all security/config objects if update bootstrap server and lwm2m server
         * if other: del or update only instances */

        return configBs;
    }

    /**
     * 功能：更新服务端。
     * 参数：
     * - `serverCredential`：`serverCredential` 参数。
     * - `securityMode`：`securityMode` 参数。
     * 返回：处理结果。
     */
    private BootstrapConfig.ServerSecurity setServerSecurity(AbstractLwM2MBootstrapServerCredential serverCredential, LwM2MSecurityMode securityMode) {
        BootstrapConfig.ServerSecurity serverSecurity = new BootstrapConfig.ServerSecurity();
        String serverUri = "coap://";
        byte[] publicKeyOrId = new byte[]{};
        byte[] secretKey = new byte[]{};
        byte[] serverPublicKey = new byte[]{};
        serverSecurity.serverId = serverCredential.getShortServerId();
        serverSecurity.securityMode = SecurityMode.valueOf(securityMode.name());
        serverSecurity.bootstrapServer = serverCredential.isBootstrapServerIs();
        if (!LwM2MSecurityMode.NO_SEC.equals(securityMode)) {
            AbstractLwM2MBootstrapClientCredentialWithKeys server;
            if (serverSecurity.bootstrapServer) {
                server = (AbstractLwM2MBootstrapClientCredentialWithKeys) this.bootstrapServer;

            } else {
                server = (AbstractLwM2MBootstrapClientCredentialWithKeys) this.lwm2mServer;
            }
            serverUri = "coaps://";
            if (LwM2MSecurityMode.PSK.equals(securityMode)) {
                publicKeyOrId = server.getClientPublicKeyOrId().getBytes();
                secretKey = Hex.decodeHex(server.getClientSecretKey().toCharArray());
            } else {
                publicKeyOrId = server.getDecodedClientPublicKeyOrId();
                secretKey = server.getDecodedClientSecretKey();
            }
            serverPublicKey = serverCredential.getDecodedCServerPublicKey();
        }
        serverUri += (((serverCredential.getHost().equals("0.0.0.0") ? "localhost" : serverCredential.getHost()) + ":" + serverCredential.getPort()));
        serverSecurity.uri = serverUri;
        serverSecurity.publicKeyOrId = publicKeyOrId;
        serverSecurity.secretKey = secretKey;
        serverSecurity.serverPublicKey = serverPublicKey;
        return serverSecurity;
    }

    /**
     * 功能：更新配置。
     * 参数：
     * - `serverCredential`：`serverCredential` 参数。
     * 返回：处理结果。
     */
    private BootstrapConfig.ServerConfig setServerConfig (AbstractLwM2MBootstrapServerCredential serverCredential) {
        BootstrapConfig.ServerConfig serverConfig = new BootstrapConfig.ServerConfig();
        serverConfig.shortId = serverCredential.getShortServerId();
        serverConfig.lifetime = serverCredential.getLifetime();
        serverConfig.defaultMinPeriod = serverCredential.getDefaultMinPeriod();
        serverConfig.notifIfDisabled = serverCredential.isNotifIfDisabled();
        serverConfig.binding = BindingMode.parse(serverCredential.getBinding());
        return serverConfig;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MBootstrapConfig` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
