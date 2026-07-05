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
package org.thingsboard.server.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.time.Duration;

/**
 * 中文说明：
 * 1. 类目的：`TBRedisStandaloneConfiguration` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Configuration
@ConditionalOnMissingBean(TbCaffeineCacheConfiguration.class)
@ConditionalOnProperty(prefix = "redis.connection", value = "type", havingValue = "standalone")
public class TBRedisStandaloneConfiguration extends TBRedisCacheConfiguration {

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Value("${redis.standalone.host:localhost}")
    private String host;

    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Value("${redis.standalone.port:6379}")
    private Integer port;

    /**
     * 名称，用于发起外部调用或协议交互。
     */
    @Value("${redis.standalone.clientName:standalone}")
    private String clientName;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${redis.standalone.connectTimeout:30000}")
    private Long connectTimeout;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${redis.standalone.readTimeout:60000}")
    private Long readTimeout;

    /**
     * 当前对象是否为默认项。
     */
    @Value("${redis.standalone.useDefaultClientConfig:true}")
    private boolean useDefaultClientConfig;

    /**
     * 是否使用配置。
     */
    @Value("${redis.standalone.usePoolConfig:false}")
    private boolean usePoolConfig;

    /**
     * `db` 字段，保存当前对象的对应属性。
     */
    @Value("${redis.db:0}")
    private Integer db;

    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${redis.password:}")
    private String password;

    /**
     * 功能：获取工厂。
     * 参数：无。
     * 返回：处理结果。
     */
    public JedisConnectionFactory loadFactory() {
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        standaloneConfiguration.setHostName(host);
        standaloneConfiguration.setPort(port);
        standaloneConfiguration.setDatabase(db);
        standaloneConfiguration.setPassword(password);
        if (useDefaultClientConfig) {
            return new JedisConnectionFactory(standaloneConfiguration);
        } else {
            return new JedisConnectionFactory(standaloneConfiguration, buildClientConfig());
        }
    }

    /**
     * 功能：构建配置。
     * 参数：无。
     * 返回：处理结果。
     */
    private JedisClientConfiguration buildClientConfig() {
        if (usePoolConfig) {
            return JedisClientConfiguration.builder()
                    .clientName(clientName)
                    .connectTimeout(Duration.ofMillis(connectTimeout))
                    .readTimeout(Duration.ofMillis(readTimeout))
                    .usePooling().poolConfig(buildPoolConfig())
                    .build();
        } else {
            return JedisClientConfiguration.builder()
                    .clientName(clientName)
                    .connectTimeout(Duration.ofMillis(connectTimeout))
                    .readTimeout(Duration.ofMillis(readTimeout)).build();
        }
    }

/*
 * 本类总结：
 * 1. 核心职责：`TBRedisStandaloneConfiguration` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}