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
package org.thingsboard.server.transport.lwm2m.server.ota;

import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`LwM2MOtaUpdateService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface LwM2MOtaUpdateService {

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：无。
     */
    void init(LwM2mClient client);

    /**
     * 功能：执行 `forceFirmwareUpdate` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：无。
     */
    void forceFirmwareUpdate(LwM2mClient client);

    /**
     * 功能：处理目标对象。
     * 参数：
     * - `client`：客户端对象。
     * - `newFwTitle`：`newFwTitle` 参数。
     * - `newFwVersion`：`newFwVersion` 参数。
     * - `newFwUrl`：`newFwUrl` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void onTargetFirmwareUpdate(LwM2mClient client, String newFwTitle, String newFwVersion, Optional<String> newFwUrl, Optional<String> newFwTag);

    /**
     * 功能：处理目标对象。
     * 参数：
     * - `client`：客户端对象。
     * - `newSwTitle`：`newSwTitle` 参数。
     * - `newSwVersion`：`newSwVersion` 参数。
     * - `newSwUrl`：`newSwUrl` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void onTargetSoftwareUpdate(LwM2mClient client, String newSwTitle, String newSwVersion, Optional<String> newSwUrl, Optional<String> newSwTag);

    /**
     * 功能：处理名称。
     * 参数：
     * - `client`：客户端对象。
     * - `name`：名称。
     * 返回：无。
     */
    void onCurrentFirmwareNameUpdate(LwM2mClient client, String name);

    /**
     * 功能：处理策略对象。
     * 参数：
     * - `client`：客户端对象。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    void onFirmwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration);

    /**
     * 功能：处理策略对象。
     * 参数：
     * - `client`：客户端对象。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    void onCurrentSoftwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration);

    /**
     * 功能：处理`on Current Firmware Version3 Update`。
     * 参数：
     * - `client`：客户端对象。
     * - `version`：`version` 参数。
     * 返回：无。
     */
    void onCurrentFirmwareVersion3Update(LwM2mClient client, String version);

    /**
     * 功能：处理版本号。
     * 参数：
     * - `client`：客户端对象。
     * - `version`：`version` 参数。
     * 返回：无。
     */
    void onCurrentFirmwareVersionUpdate(LwM2mClient client, String version);

    /**
     * 功能：处理状态。
     * 参数：
     * - `client`：客户端对象。
     * - `state`：`state` 参数。
     * 返回：无。
     */
    void onCurrentFirmwareStateUpdate(LwM2mClient client, Long state);

    /**
     * 功能：处理`on Current Firmware Result Update`。
     * 参数：
     * - `client`：客户端对象。
     * - `result`：`result` 参数。
     * 返回：无。
     */
    void onCurrentFirmwareResultUpdate(LwM2mClient client, Long result);

    /**
     * 功能：处理`on Current Firmware Delivery Method Update`。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `value`：值。
     * 返回：无。
     */
    void onCurrentFirmwareDeliveryMethodUpdate(LwM2mClient lwM2MClient, Long value);

    /**
     * 功能：处理名称。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `name`：名称。
     * 返回：无。
     */
    void onCurrentSoftwareNameUpdate(LwM2mClient lwM2MClient, String name);

    /**
     * 功能：处理`on Current Software Version3 Update`。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `version`：`version` 参数。
     * 返回：无。
     */
    void onCurrentSoftwareVersion3Update(LwM2mClient lwM2MClient, String version);

    /**
     * 功能：处理版本号。
     * 参数：
     * - `client`：客户端对象。
     * - `version`：`version` 参数。
     * 返回：无。
     */
    void onCurrentSoftwareVersionUpdate(LwM2mClient client, String version);

    /**
     * 功能：处理状态。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `value`：值。
     * 返回：无。
     */
    void onCurrentSoftwareStateUpdate(LwM2mClient lwM2MClient, Long value);

    /**
     * 功能：处理`on Current Software Result Update`。
     * 参数：
     * - `client`：客户端对象。
     * - `result`：`result` 参数。
     * 返回：无。
     */
    void onCurrentSoftwareResultUpdate(LwM2mClient client, Long result);

    /**
     * 功能：判断`Ota Downloading`。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    boolean isOtaDownloading(LwM2mClient client);
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MOtaUpdateService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
