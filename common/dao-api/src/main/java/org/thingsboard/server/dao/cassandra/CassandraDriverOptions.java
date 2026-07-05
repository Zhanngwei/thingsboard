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

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.api.core.metrics.DefaultNodeMetric;
import com.datastax.oss.driver.api.core.metrics.DefaultSessionMetric;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.util.NoSqlAnyDao;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`CassandraDriverOptions` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@Configuration
@Data
@NoSqlAnyDao
public class CassandraDriverOptions {

    /**
     * `COMMA`常量，用于统一引用固定值。
     */
    private static final String COMMA = ",";

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Value("${cassandra.cluster_name}")
    private String clusterName;
    /**
     * URL 地址，用于定位外部资源或本地资源。
     */
    @Value("${cassandra.url}")
    private String url;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${cassandra.socket.connect_timeout}")
    private int connectTimeoutMillis;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${cassandra.socket.read_timeout}")
    private int readTimeoutMillis;
    /**
     * 是否满足`keepAlive`条件。
     */
    @Value("${cassandra.socket.keep_alive}")
    private Boolean keepAlive;
    /**
     * 是否满足`reuseAddress`条件。
     */
    @Value("${cassandra.socket.reuse_address}")
    private Boolean reuseAddress;
    /**
     * `soLinger` 字段，保存当前对象的对应属性。
     */
    @Value("${cassandra.socket.so_linger}")
    private Integer soLinger;
    /**
     * 是否满足延迟时间条件。
     */
    @Value("${cassandra.socket.tcp_no_delay}")
    private Boolean tcpNoDelay;
    /**
     * `receiveBufferSize` 字段，保存当前对象的对应属性。
     */
    @Value("${cassandra.socket.receive_buffer_size}")
    private Integer receiveBufferSize;
    /**
     * `sendBufferSize` 字段，保存当前对象的对应属性。
     */
    @Value("${cassandra.socket.send_buffer_size}")
    private Integer sendBufferSize;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${cassandra.max_requests_per_connection_local:32768}")
    private int max_requests_local;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${cassandra.max_requests_per_connection_remote:32768}")
    private int max_requests_remote;

    /**
     * `defaultFetchSize` 字段，保存当前对象的对应属性。
     */
    @Value("${cassandra.query.default_fetch_size}")
    private Integer defaultFetchSize;
    /**
     * `readConsistencyLevel` 字段，保存当前对象的对应属性。
     */
    @Value("${cassandra.query.read_consistency_level}")
    private String readConsistencyLevel;
    /**
     * `writeConsistencyLevel` 字段，保存当前对象的对应属性。
     */
    @Value("${cassandra.query.write_consistency_level}")
    private String writeConsistencyLevel;

    /**
     * `compression` 字段，保存当前对象的对应属性。
     */
    @Value("${cassandra.compression}")
    private String compression;
    
    /**
     * 是否启用 SSL。
     */
    @Value("${cassandra.ssl.enabled}")
    private Boolean ssl;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Value("${cassandra.ssl.key_store}")
    private String sslKeyStore;
    /**
     * 密码，用于定位映射、配置或数据项。
     */
    @Value("${cassandra.ssl.key_store_password}")
    private String sslKeyStorePassword;
    /**
     * SSL，表示当前对象的对应属性。
     */
    @Value("${cassandra.ssl.trust_store}")
    private String sslTrustStore;
    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${cassandra.ssl.trust_store_password}")
    private String sslTrustStorePassword;
    /**
     * 是否启用 SSL。
     */
    @Value("${cassandra.ssl.hostname_validation}")
    private Boolean sslHostnameValidation;
    /**
     * SSL列表，用于保存一组待处理对象。
     */
    @Value("${cassandra.ssl.cipher_suites}")
    private List<String> sslCipherSuites;
    
    /**
     * 是否满足`metrics`条件。
     */
    @Value("${cassandra.metrics}")
    private Boolean metrics;

    /**
     * 是否满足凭据条件。
     */
    @Value("${cassandra.credentials}")
    private Boolean credentials;
    /**
     * 用户名，用于认证或安全校验。
     */
    @Value("${cassandra.username}")
    private String username;
    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${cassandra.password}")
    private String password;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${cassandra.init_timeout_ms}")
    private long initTimeout;
    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    @Value("${cassandra.init_retry_interval_ms}")
    private long initRetryInterval;

    /**
     * `loader` 字段，保存当前对象的对应属性。
     */
    private DriverConfigLoader loader;

    /**
     * `defaultReadConsistencyLevel` 字段，保存当前对象的对应属性。
     */
    private ConsistencyLevel defaultReadConsistencyLevel;
    private ConsistencyLevel defaultWriteConsistencyLevel;

    /**
     * 功能：初始化或启动`Loader`。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void initLoader() {
        ProgrammaticDriverConfigLoaderBuilder driverConfigBuilder =
                DriverConfigLoader.programmaticBuilder();

        driverConfigBuilder
                .withStringList(DefaultDriverOption.CONTACT_POINTS, getContactPoints(url))
                .withString(DefaultDriverOption.SESSION_NAME, clusterName);

        this.initSocketOptions(driverConfigBuilder);
        this.initPoolingOptions(driverConfigBuilder);
        this.initQueryOptions(driverConfigBuilder);

        driverConfigBuilder.withString(DefaultDriverOption.PROTOCOL_COMPRESSION,
                StringUtils.isEmpty(this.compression) ? "none" : this.compression.toLowerCase());

        if (this.ssl) {
            driverConfigBuilder.withString(DefaultDriverOption.SSL_ENGINE_FACTORY_CLASS,
                    "DefaultSslEngineFactory")
                .withBoolean(DefaultDriverOption.SSL_HOSTNAME_VALIDATION, this.sslHostnameValidation);
            if(!this.sslTrustStore.isEmpty()) {
                driverConfigBuilder.withString(DefaultDriverOption.SSL_TRUSTSTORE_PATH, this.sslTrustStore)
                    .withString(DefaultDriverOption.SSL_TRUSTSTORE_PASSWORD, this.sslTrustStorePassword);
            }
            if(!this.sslKeyStore.isEmpty()) {
                driverConfigBuilder.withString(DefaultDriverOption.SSL_KEYSTORE_PATH, this.sslKeyStore)
                    .withString(DefaultDriverOption.SSL_KEYSTORE_PASSWORD, this.sslKeyStorePassword);
            }
            if(!this.sslCipherSuites.isEmpty()) {
                driverConfigBuilder.withStringList(DefaultDriverOption.SSL_CIPHER_SUITES, this.sslCipherSuites);
            }
        }

        if (this.metrics) {
            driverConfigBuilder.withStringList(DefaultDriverOption.METRICS_SESSION_ENABLED,
                    Arrays.asList(DefaultSessionMetric.CONNECTED_NODES.getPath(),
                            DefaultSessionMetric.CQL_REQUESTS.getPath()));
            driverConfigBuilder.withStringList(DefaultDriverOption.METRICS_NODE_ENABLED,
                    Arrays.asList(DefaultNodeMetric.OPEN_CONNECTIONS.getPath(),
                            DefaultNodeMetric.IN_FLIGHT.getPath()));
        }

        if (this.credentials) {
            driverConfigBuilder.withString(DefaultDriverOption.AUTH_PROVIDER_CLASS,
                    "PlainTextAuthProvider");
            driverConfigBuilder.withString(DefaultDriverOption.AUTH_PROVIDER_USER_NAME,
                    this.username);
            driverConfigBuilder.withString(DefaultDriverOption.AUTH_PROVIDER_PASSWORD,
                    this.password);
        }

        driverConfigBuilder.withBoolean(DefaultDriverOption.RECONNECT_ON_INIT,
                    true);
        driverConfigBuilder.withString(DefaultDriverOption.RECONNECTION_POLICY_CLASS,
                "ExponentialReconnectionPolicy");
        driverConfigBuilder.withDuration(DefaultDriverOption.RECONNECTION_BASE_DELAY,
                Duration.ofMillis(this.initRetryInterval));
        driverConfigBuilder.withDuration(DefaultDriverOption.RECONNECTION_MAX_DELAY,
                Duration.ofMillis(this.initTimeout));

        this.loader = driverConfigBuilder.build();
    }

    /**
     * 功能：获取`Default Read Consistency Level`。
     * 参数：无。
     * 返回：处理结果。
     */
    protected ConsistencyLevel getDefaultReadConsistencyLevel() {
        if (defaultReadConsistencyLevel == null) {
            if (readConsistencyLevel != null) {
                defaultReadConsistencyLevel = DefaultConsistencyLevel.valueOf(readConsistencyLevel.toUpperCase());
            } else {
                defaultReadConsistencyLevel = DefaultConsistencyLevel.ONE;
            }
        }
        return defaultReadConsistencyLevel;
    }

    /**
     * 功能：获取`Default Write Consistency Level`。
     * 参数：无。
     * 返回：处理结果。
     */
    protected ConsistencyLevel getDefaultWriteConsistencyLevel() {
        if (defaultWriteConsistencyLevel == null) {
            if (writeConsistencyLevel != null) {
                defaultWriteConsistencyLevel = DefaultConsistencyLevel.valueOf(writeConsistencyLevel.toUpperCase());
            } else {
                defaultWriteConsistencyLevel = DefaultConsistencyLevel.ONE;
            }
        }
        return defaultWriteConsistencyLevel;
    }

    /**
     * 功能：初始化或启动`Socket Options`。
     * 参数：
     * - `driverConfigBuilder`：配置对象。
     * 返回：无。
     */
    private void initSocketOptions(ProgrammaticDriverConfigLoaderBuilder driverConfigBuilder) {
        driverConfigBuilder.withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT,
                Duration.ofMillis(this.connectTimeoutMillis));
        driverConfigBuilder.withDuration(DefaultDriverOption.REQUEST_TIMEOUT,
                Duration.ofMillis(this.readTimeoutMillis));
        if (this.keepAlive != null) {
            driverConfigBuilder.withBoolean(DefaultDriverOption.SOCKET_KEEP_ALIVE,
                    this.keepAlive);
        }
        if (this.reuseAddress != null) {
            driverConfigBuilder.withBoolean(DefaultDriverOption.SOCKET_REUSE_ADDRESS,
                    this.reuseAddress);
        }
        if (this.soLinger != null) {
            driverConfigBuilder.withInt(DefaultDriverOption.SOCKET_LINGER_INTERVAL,
                    this.soLinger);
        }
        if (this.tcpNoDelay != null) {
            driverConfigBuilder.withBoolean(DefaultDriverOption.SOCKET_TCP_NODELAY,
                    this.tcpNoDelay);
        }
        if (this.receiveBufferSize != null) {
            driverConfigBuilder.withInt(DefaultDriverOption.SOCKET_RECEIVE_BUFFER_SIZE,
                    this.receiveBufferSize);
        }
        if (this.sendBufferSize != null) {
            driverConfigBuilder.withInt(DefaultDriverOption.SOCKET_SEND_BUFFER_SIZE,
                    this.sendBufferSize);
        }
    }

    /**
     * 功能：初始化或启动`Pooling Options`。
     * 参数：
     * - `driverConfigBuilder`：配置对象。
     * 返回：无。
     */
    private void initPoolingOptions(ProgrammaticDriverConfigLoaderBuilder driverConfigBuilder) {
        driverConfigBuilder.withInt(DefaultDriverOption.CONNECTION_MAX_REQUESTS,
                this.max_requests_local);
    }

    /**
     * 功能：初始化或启动查询条件。
     * 参数：
     * - `driverConfigBuilder`：配置对象。
     * 返回：无。
     */
    private void initQueryOptions(ProgrammaticDriverConfigLoaderBuilder driverConfigBuilder) {
        driverConfigBuilder.withInt(DefaultDriverOption.REQUEST_PAGE_SIZE,
                this.defaultFetchSize);
    }

    /**
     * 功能：获取`Contact Points`。
     * 参数：
     * - `url`：`url` 参数。
     * 返回：匹配的数据集合。
     */
    private List<String> getContactPoints(String url) {
        List<String> result;
        if (StringUtils.isBlank(url)) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<>();
            for (String hostPort : url.split(COMMA)) {
                result.add(hostPort);
            }
        }
        return result;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraDriverOptions` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
