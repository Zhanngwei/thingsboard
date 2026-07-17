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
package org.thingsboard.server.dao.cassandra;


import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.cassandra.guava.GuavaSessionBuilder;
import org.thingsboard.server.dao.cassandra.guava.GuavaSessionUtils;

import javax.annotation.PreDestroy;
import java.nio.file.Paths;

/**
 * 中文说明：
 * 1. `AbstractCassandraCluster` 是 ThingsBoard Common 中负责 `Cassandra Cluster` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
public abstract class AbstractCassandraCluster {

    /**
     * 是否满足`jmx`条件。
     */
    @Value("${cassandra.jmx}")
    private Boolean jmx;
    /**
     * 是否满足`metrics`条件。
     */
    @Value("${cassandra.metrics}")
    private Boolean metrics;
    /**
     * `localDatacenter` 字段，保存当前对象的对应属性。
     */
    @Value("${cassandra.local_datacenter:datacenter1}")
    private String localDatacenter;

    /**
     * 包，用于定位本地文件或目录。
     */
    @Value("${cassandra.cloud.secure_connect_bundle_path:}")
    private String cloudSecureConnectBundlePath;
    /**
     * 客户端ID，用于定位对应业务对象。
     */
    @Value("${cassandra.cloud.client_id:}")
    private String cloudClientId;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @Value("${cassandra.cloud.client_secret:}")
    private String cloudClientSecret;

    /**
     * `driverOptions` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private CassandraDriverOptions driverOptions;

    /**
     * 环境配置，保存当前对象的配置选项。
     */
    @Autowired
    private Environment environment;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    private GuavaSessionBuilder sessionBuilder;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    private GuavaSession session;

    /**
     * `reporter` 字段，保存当前对象的对应属性。
     */
    private JmxReporter reporter;

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String keyspaceName;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `keyspaceName`：名称。
     * 返回：无。
     */
    protected void init(String keyspaceName) {
        this.keyspaceName = keyspaceName;
        this.sessionBuilder = GuavaSessionUtils.builder().withConfigLoader(this.driverOptions.getLoader());
        if (!isInstall()) {
            initSession();
        }
    }

    /**
     * 功能：获取会话。
     * 参数：无。
     * 返回：处理结果。
     */
    public GuavaSession getSession() {
        if (!isInstall()) {
            return session;
        } else {
            if (session == null) {
                initSession();
            }
            return session;
        }
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getKeyspaceName() {
        return keyspaceName;
    }

    /**
     * 功能：判断`Install`。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    /**
     * 功能：初始化或启动会话。
     * 参数：无。
     * 返回：无。
     */
    private void initSession() {
        if (this.keyspaceName != null) {
            this.sessionBuilder.withKeyspace(this.keyspaceName);
        }
        this.sessionBuilder.withLocalDatacenter(localDatacenter);

        if (StringUtils.isNotBlank(cloudSecureConnectBundlePath)) {
            this.sessionBuilder.withCloudSecureConnectBundle(Paths.get(cloudSecureConnectBundlePath));
            this.sessionBuilder.withAuthCredentials(cloudClientId, cloudClientSecret);
        }

        session = sessionBuilder.build();

        if (this.metrics && this.jmx) {
            MetricRegistry registry =
                    session.getMetrics().orElseThrow(
                            () -> new IllegalStateException("Metrics are disabled"))
                    .getRegistry();
            this.reporter =
                    JmxReporter.forRegistry(registry)
                            .inDomain("com.datastax.oss.driver")
                            .build();
            this.reporter.start();
        }
    }

    /**
     * 功能：执行 `close` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void close() {
        if (reporter != null) {
            reporter.stop();
        }
        if (session != null) {
            session.close();
        }
    }

    /**
     * 功能：获取`Default Read Consistency Level`。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConsistencyLevel getDefaultReadConsistencyLevel() {
        return driverOptions.getDefaultReadConsistencyLevel();
    }

    /**
     * 功能：获取`Default Write Consistency Level`。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConsistencyLevel getDefaultWriteConsistencyLevel() {
        return driverOptions.getDefaultWriteConsistencyLevel();
    }

}
