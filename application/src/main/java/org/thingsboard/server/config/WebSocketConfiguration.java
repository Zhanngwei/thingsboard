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
package org.thingsboard.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.thingsboard.server.controller.plugin.TbWebSocketHandler;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `WebSocketConfiguration` 是 ThingsBoard Application 中描述 `Web Socket` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `WebSocketConfigurer`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Configuration
@TbCoreComponent
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfiguration implements WebSocketConfigurer {

    /**
     * `WS_API_ENDPOINT`常量，用于统一引用固定值。
     */
    public static final String WS_API_ENDPOINT = "/api/ws";
    public static final String WS_PLUGINS_ENDPOINT = "/api/ws/plugins/";
    /**
     * `WS_API_MAPPING`常量，用于统一引用固定值。
     */
    private static final String WS_API_MAPPING = "/api/ws/**";

    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final WebSocketHandler wsHandler;

    /**
     * 功能：保存或创建`Web Socket Container`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(32768);
        container.setMaxBinaryMessageBufferSize(32768);
        return container;
    }

    /**
     * 功能：保存或创建`Web Socket Handlers`。
     * 参数：
     * - `registry`：`registry` 参数。
     * 返回：无。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (!(wsHandler instanceof TbWebSocketHandler)) {
            log.error("TbWebSocketHandler expected but [{}] provided", wsHandler);
            throw new RuntimeException("TbWebSocketHandler expected but " + wsHandler + " provided");
        }
        registry.addHandler(wsHandler, WS_API_MAPPING).setAllowedOriginPatterns("*");
    }

}
