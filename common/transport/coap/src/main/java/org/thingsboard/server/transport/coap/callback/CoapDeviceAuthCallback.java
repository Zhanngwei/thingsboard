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
package org.thingsboard.server.transport.coap.callback;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;

import java.util.function.BiConsumer;

/**
 * 中文说明：
 * 1. 类目的：`CoapDeviceAuthCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class CoapDeviceAuthCallback implements TransportServiceCallback<ValidateDeviceCredentialsResponse> {
    /**
     * `exchange` 字段，保存当前对象的对应属性。
     */
    private final CoapExchange exchange;
    private final BiConsumer<ValidateDeviceCredentialsResponse, DeviceProfile> onSuccess;

    /**
     * 功能：创建 `CoapDeviceAuthCallback` 实例，并初始化必要字段。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * - `onSuccess`：`onSuccess` 参数。
     * 返回：新创建的对象实例。
     */
    public CoapDeviceAuthCallback(CoapExchange exchange, BiConsumer<ValidateDeviceCredentialsResponse, DeviceProfile> onSuccess) {
        this.exchange = exchange;
        this.onSuccess = onSuccess;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
        DeviceProfile deviceProfile = msg.getDeviceProfile();
        if (msg.hasDeviceInfo() && deviceProfile != null) {
            onSuccess.accept(msg, deviceProfile);
        } else {
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
        }
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    public void onError(Throwable e) {
        log.warn("Failed to process request", e);
        exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CoapDeviceAuthCallback` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
