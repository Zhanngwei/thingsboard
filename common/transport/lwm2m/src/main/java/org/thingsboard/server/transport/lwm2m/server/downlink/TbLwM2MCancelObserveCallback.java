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
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;

/**
 * 中文说明：
 * 1. 类目的：`TbLwM2MCancelObserveCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class TbLwM2MCancelObserveCallback extends AbstractTbLwM2MRequestCallback<TbLwM2MCancelObserveRequest, Integer> {

    /**
     * `versionedId`ID，用于定位对应业务对象。
     */
    private final String versionedId;

    /**
     * 功能：创建 `TbLwM2MCancelObserveCallback` 实例，并初始化必要字段。
     * 参数：
     * - `logService`：服务对象。
     * - `client`：客户端对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：新创建的对象实例。
     */
    public TbLwM2MCancelObserveCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String versionedId) {
        super(logService, client);
        this.versionedId = versionedId;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `request`：请求对象。
     * - `canceledSubscriptionsCount`：`canceledSubscriptionsCount` 参数。
     * 返回：无。
     */
    @Override
    public void onSuccess(TbLwM2MCancelObserveRequest request, Integer canceledSubscriptionsCount) {
        log.trace("[{}] Cancel observation of [{}] successful: {}", client.getEndpoint(),  versionedId, canceledSubscriptionsCount);
        logService.log(client, String.format("[%s]: Cancel Observe for [%s] successful. Result: [%s]", LOG_LWM2M_INFO, versionedId, canceledSubscriptionsCount));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbLwM2MCancelObserveCallback` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
