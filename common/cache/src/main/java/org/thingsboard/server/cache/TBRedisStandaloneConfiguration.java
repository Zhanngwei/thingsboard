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
 * 1. `TBRedisStandaloneConfiguration` 是 ThingsBoard Common Cache 中描述 `TB Redis Standalone` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `TBRedisCacheConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
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
}