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
package org.thingsboard.server.transport.http;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;

/**
 * Created by ashvayka on 04.10.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`HttpTransportContext` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.http.enabled}'=='true')")
@Component
public class HttpTransportContext extends TransportContext {

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${transport.http.request_timeout}")
    private long defaultTimeout;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Getter
    @Value("${transport.http.max_request_timeout}")
    private long maxRequestTimeout;

    /**
     * 功能：执行 `tomcatAsyncTimeoutConnectorCustomizer` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    public TomcatConnectorCustomizer tomcatAsyncTimeoutConnectorCustomizer() {
        return connector -> {
            ProtocolHandler handler = connector.getProtocolHandler();
            if (handler instanceof Http11NioProtocol) {
                log.trace("Setting async max request timeout {}", maxRequestTimeout);
                connector.setAsyncTimeout(maxRequestTimeout);
            }
        };
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`HttpTransportContext` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
