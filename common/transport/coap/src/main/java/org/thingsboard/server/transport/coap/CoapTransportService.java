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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.coapserver.TbCoapServerComponent;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.coap.efento.CoapEfentoTransportResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.UnknownHostException;

/**
 * 中文说明：
 * 1. 类目的：`CoapTransportService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Service("CoapTransportService")
@TbCoapServerComponent
@Slf4j
public class CoapTransportService implements TbTransportService {

    /**
     * `V1`常量，用于统一引用固定值。
     */
    private static final String V1 = "v1";
    private static final String API = "api";
    /**
     * `EFENTO`常量，用于统一引用固定值。
     */
    private static final String EFENTO = "efento";
    public static final String MEASUREMENTS = "m";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_INFO = "i";
    public static final String CONFIGURATION = "c";
    /**
     * 时间戳常量，用于统一引用固定值。
     */
    public static final String CURRENT_TIMESTAMP = "t";

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private CoapServerService coapServerService;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    private CoapTransportContext coapTransportContext;

    /**
     * 服务端，用于支撑当前网络或外部服务交互。
     */
    private CoapServer coapServer;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() throws UnknownHostException {
        log.info("Starting CoAP transport...");
        coapServer = coapServerService.getCoapServer();
        CoapResource api = new CoapResource(API);
        api.add(new CoapTransportResource(coapTransportContext, coapServerService, V1));

        CoapEfentoTransportResource efento = new CoapEfentoTransportResource(coapTransportContext, EFENTO);
        efento.add(new CoapResource(MEASUREMENTS));
        efento.add(new CoapResource(DEVICE_INFO));
        efento.add(new CoapResource(CONFIGURATION));
        efento.add(new CoapResource(CURRENT_TIMESTAMP));
        coapServer.add(api);
        coapServer.add(efento);
        coapServer.add(new OtaPackageTransportResource(coapTransportContext, OtaPackageType.FIRMWARE));
        coapServer.add(new OtaPackageTransportResource(coapTransportContext, OtaPackageType.SOFTWARE));
        log.info("CoAP transport started!");
    }

    /**
     * 功能：执行 `shutdown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void shutdown() {
        log.info("CoAP transport stopped!");
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getName() {
        return DataConstants.COAP_TRANSPORT_NAME;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CoapTransportService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
