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
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

/**
 * 中文说明：
 * 1. `TBRedisClusterConfiguration` 是 ThingsBoard Common Cache 中描述 `TB Redis Cluster` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `TBRedisCacheConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Configuration
@ConditionalOnMissingBean(TbCaffeineCacheConfiguration.class)
@ConditionalOnProperty(prefix = "redis.connection", value = "type", havingValue = "cluster")
public class TBRedisClusterConfiguration extends TBRedisCacheConfiguration {

    /**
     * 集群，用于支撑当前网络或外部服务交互。
     */
    @Value("${redis.cluster.nodes:}")
    private String clusterNodes;

    /**
     * `maxRedirects` 字段，保存当前对象的对应属性。
     */
    @Value("${redis.cluster.max-redirects:12}")
    private Integer maxRedirects;

    /**
     * 当前对象是否为默认项。
     */
    @Value("${redis.cluster.useDefaultPoolConfig:true}")
    private boolean useDefaultPoolConfig;

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
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        clusterConfiguration.setClusterNodes(getNodes(clusterNodes));
        clusterConfiguration.setMaxRedirects(maxRedirects);
        clusterConfiguration.setPassword(password);
        if (useDefaultPoolConfig) {
            return new JedisConnectionFactory(clusterConfiguration);
        } else {
            return new JedisConnectionFactory(clusterConfiguration, buildPoolConfig());
        }
    }
}
