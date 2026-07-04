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
package org.thingsboard.server.service.install.migrate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.model.sqlts.dictionary.TsKvDictionary;
import org.thingsboard.server.dao.model.sqlts.dictionary.TsKvDictionaryCompositeKey;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sqlts.dictionary.TsKvDictionaryRepository;
import org.thingsboard.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
import org.thingsboard.server.dao.util.NoSqlTsDao;
import org.thingsboard.server.dao.util.SqlTsLatestDao;
import org.thingsboard.server.service.install.InstallScripts;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.bigintColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.booleanColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.doubleColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.idColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.jsonColumn;
import static org.thingsboard.server.service.install.migrate.CassandraToSqlColumn.stringColumn;

@Service
@Profile("install")
@NoSqlTsDao
@SqlTsLatestDao
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`CassandraTsLatestToSqlMigrateService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class CassandraTsLatestToSqlMigrateService implements TsLatestMigrateService {

    /**
     * 字段说明：
     * 1. 保存 `MAX_KEY_LENGTH` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int MAX_KEY_LENGTH = 255;
    private static final int MAX_STR_V_LENGTH = 10000000;

    /**
     * 字段说明：
     * 1. 保存 `SQL_DIR` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String SQL_DIR = "sql";

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `insertLatestTsRepository` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private InsertLatestTsRepository insertLatestTsRepository;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `cluster` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected CassandraCluster cluster;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `dictionaryRepository` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected TsKvDictionaryRepository dictionaryRepository;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `installScripts` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private InstallScripts installScripts;

    @Value("${spring.datasource.url}")
    /**
     * 字段说明：
     * 1. 保存 `dbUrl` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected String dbUrl;

    @Value("${spring.datasource.username}")
    /**
     * 字段说明：
     * 1. 保存 `dbUserName` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected String dbUserName;

    @Value("${spring.datasource.password}")
    /**
     * 字段说明：
     * 1. 保存 `dbPassword` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected String dbPassword;

    private final ConcurrentMap<String, Integer> tsKvDictionaryMap = new ConcurrentHashMap<>();

    protected static final ReentrantLock tsCreationLock = new ReentrantLock();

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `migrate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void migrate() throws Exception {
        log.info("Performing migration of latest timeseries data from cassandra to SQL database ...");
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), SQL_DIR, "schema-ts-latest-psql.sql");
            loadSql(schemaUpdateFile, conn);
            conn.setAutoCommit(false);
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (CassandraToSqlTable table : tables) {
                table.migrateToSql(cluster.getSession(), conn);
            }
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.error("Unexpected error during ThingsBoard entities data migration!", e);
            throw e;
        }
    }

    private List<CassandraToSqlTable> tables = Arrays.asList(
            new CassandraToSqlTable("ts_kv_latest_cf", "ts_kv_latest",
                    idColumn("entity_id"),
                    stringColumn("key"),
                    bigintColumn("ts"),
                    booleanColumn("bool_v"),
                    stringColumn("str_v"),
                    bigintColumn("long_v"),
                    doubleColumn("dbl_v"),
                    jsonColumn("json_v")) {

                @Override
                protected void batchInsert(List<CassandraToSqlColumnData[]> batchData, Connection conn) {
                    insertLatestTsRepository
                            .saveOrUpdate(batchData.stream().map(data -> getTsKvLatestEntity(data)).collect(Collectors.toList()));
                }

                @Override
                protected CassandraToSqlColumnData[] validateColumnData(CassandraToSqlColumnData[] data) {
                    return data;
                }
            });

    /**
     * 方法说明：
     * 1. 职责：执行 `getTsKvLatestEntity` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TsKvLatestEntity getTsKvLatestEntity(CassandraToSqlColumnData[] data) {
        TsKvLatestEntity latestEntity = new TsKvLatestEntity();
        latestEntity.setEntityId(UUIDConverter.fromString(data[0].getValue()));
        latestEntity.setKey(getOrSaveKeyId(data[1].getValue()));
        latestEntity.setTs(Long.parseLong(data[2].getValue()));

        String strV = data[4].getValue();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (strV != null) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (strV.length() > MAX_STR_V_LENGTH) {
                log.warn("[ts_kv_latest] Value size [{}] exceeds maximum size [{}] of column [str_v] and will be truncated!",
                        strV.length(), MAX_STR_V_LENGTH);
                log.warn("Affected data:\n{}", strV);
                strV = strV.substring(0, MAX_STR_V_LENGTH);
            }
            latestEntity.setStrValue(strV);
        } else {
            Long longV = null;
            try {
                longV = Long.parseLong(data[5].getValue());
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (longV != null) {
                latestEntity.setLongValue(longV);
            } else {
                Double doubleV = null;
                try {
                    doubleV = Double.parseDouble(data[6].getValue());
                // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                } catch (Exception e) {
                }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (doubleV != null) {
                    latestEntity.setDoubleValue(doubleV);
                } else {

                    String jsonV = data[7].getValue();
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (StringUtils.isNoneEmpty(jsonV)) {
                        latestEntity.setJsonValue(jsonV);
                    } else {
                        Boolean boolV = null;
                        try {
                            boolV = Boolean.parseBoolean(data[3].getValue());
                        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                        } catch (Exception e) {
                        }
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (boolV != null) {
                            latestEntity.setBooleanValue(boolV);
                        } else {
                            log.warn("All values in key-value row are nullable ");
                        }
                    }
                }
            }
        }
        return latestEntity;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getOrSaveKeyId` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Integer getOrSaveKeyId(String strKey) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (strKey.length() > MAX_KEY_LENGTH) {
            log.warn("[ts_kv_latest] Value size [{}] exceeds maximum size [{}] of column [key] and will be truncated!",
                    strKey.length(), MAX_KEY_LENGTH);
            log.warn("Affected data:\n{}", strKey);
            strKey = strKey.substring(0, MAX_KEY_LENGTH);
        }

        Integer keyId = tsKvDictionaryMap.get(strKey);
        if (keyId == null) {
            Optional<TsKvDictionary> tsKvDictionaryOptional;
            tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
            if (!tsKvDictionaryOptional.isPresent()) {
                tsCreationLock.lock();
                try {
                    tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
                    if (!tsKvDictionaryOptional.isPresent()) {
                        TsKvDictionary tsKvDictionary = new TsKvDictionary();
                        tsKvDictionary.setKey(strKey);
                        try {
                            TsKvDictionary saved = dictionaryRepository.save(tsKvDictionary);
                            tsKvDictionaryMap.put(saved.getKey(), saved.getKeyId());
                            keyId = saved.getKeyId();
                        } catch (ConstraintViolationException e) {
                            tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
                            TsKvDictionary dictionary = tsKvDictionaryOptional.orElseThrow(() -> new RuntimeException("Failed to get TsKvDictionary entity from DB!"));
                            tsKvDictionaryMap.put(dictionary.getKey(), dictionary.getKeyId());
                            keyId = dictionary.getKeyId();
                        }
                    } else {
                        keyId = tsKvDictionaryOptional.get().getKeyId();
                    }
                } finally {
                    tsCreationLock.unlock();
                }
            } else {
                keyId = tsKvDictionaryOptional.get().getKeyId();
                tsKvDictionaryMap.put(strKey, keyId);
            }
        }
        return keyId;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `loadSql` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void loadSql(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), Charset.forName("UTF-8"));
        conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
        Thread.sleep(5000);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraTsLatestToSqlMigrateService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
