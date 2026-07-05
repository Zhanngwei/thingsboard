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
package org.thingsboard.server.transport.lwm2m.server.store;

import lombok.RequiredArgsConstructor;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.registration.InMemoryRegistrationStore;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;

import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`TbLwM2mStoreFactory` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class TbLwM2mStoreFactory {

    /**
     * `redisConfiguration`，保存当前对象的配置选项。
     */
    private final Optional<TBRedisCacheConfiguration> redisConfiguration;
    private final LwM2MTransportServerConfig config;
    /**
     * 校验器，封装可复用的处理规则。
     */
    private final LwM2mCredentialsSecurityInfoValidator validator;

    /**
     * 功能：执行 `registrationStore` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    private CaliforniumRegistrationStore registrationStore() {
        return redisConfiguration.isPresent() ?
                new TbLwM2mRedisRegistrationStore(getConnectionFactory()) : new InMemoryRegistrationStore(config.getCleanPeriodInSec());
    }

    /**
     * 功能：执行 `securityStore` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    private TbMainSecurityStore securityStore() {
        return new TbLwM2mSecurityStore(redisConfiguration.isPresent() ?
                new TbLwM2mRedisSecurityStore(getConnectionFactory()) : new TbInMemorySecurityStore(), validator);
    }

    /**
     * 功能：执行 `clientStore` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    private TbLwM2MClientStore clientStore() {
        return redisConfiguration.isPresent() ? new TbRedisLwM2MClientStore(getConnectionFactory()) : new TbDummyLwM2MClientStore();
    }

    /**
     * 功能：执行 `modelConfigStore` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    private TbLwM2MModelConfigStore modelConfigStore() {
        return redisConfiguration.isPresent() ? new TbRedisLwM2MModelConfigStore(getConnectionFactory()) : new TbDummyLwM2MModelConfigStore();
    }

    /**
     * 功能：执行 `otaStore` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    private TbLwM2MClientOtaInfoStore otaStore() {
        return redisConfiguration.isPresent() ? new TbLwM2mRedisClientOtaInfoStore(getConnectionFactory()) : new TbDummyLwM2MClientOtaInfoStore();
    }

    /**
     * 功能：执行 `sessionStore` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    private TbLwM2MDtlsSessionStore sessionStore() {
        return redisConfiguration.isPresent() ? new TbLwM2MDtlsSessionRedisStore(getConnectionFactory()) : new TbL2M2MDtlsSessionInMemoryStore();
    }

    /**
     * 功能：获取工厂。
     * 参数：无。
     * 返回：处理结果。
     */
    private RedisConnectionFactory getConnectionFactory() {
        return redisConfiguration.get().redisConnectionFactory();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbLwM2mStoreFactory` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
