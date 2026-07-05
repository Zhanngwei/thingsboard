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
package org.thingsboard.server.install;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.thingsboard.server.dao.audit.AuditLogLevelFilter;
import org.thingsboard.server.dao.audit.AuditLogLevelProperties;

import java.util.HashMap;

/**
 * 中文说明：
 * 1. 类目的：`ThingsboardInstallConfiguration` 是ThingsBoard Application 模块中的应用服务支撑类型，用于承载服务端运行期的数据、依赖或流程控制。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 生命周期：由 Spring 容器、Actor System、Web 请求或队列消费流程管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO/Helper。
 */
@Configuration
@Profile("install")
public class ThingsboardInstallConfiguration {

    /**
     * 功能：执行 `emptyAuditLogLevelFilter` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    public AuditLogLevelFilter emptyAuditLogLevelFilter() {
        var props = new AuditLogLevelProperties();
        props.setMask(new HashMap<>());
        return new AuditLogLevelFilter(props);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ThingsboardInstallConfiguration` 在 ThingsBoard Application 模块 中承担应用服务支撑类型职责，核心目的是承载服务端运行期的数据、依赖或流程控制。
 * 2. 核心流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
