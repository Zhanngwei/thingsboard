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

/**
 * 中文说明：
 * 1. `CassandraTsLatestToSqlMigrateService` 是 ThingsBoard Application 中负责 `Cassandra Ts Latest` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TsLatestMigrateService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Profile("install")
@NoSqlTsDao
@SqlTsLatestDao
@Slf4j
public class CassandraTsLatestToSqlMigrateService implements TsLatestMigrateService {

    /**
     * 键常量，用于统一引用固定值。
     */
    private static final int MAX_KEY_LENGTH = 255;
    private static final int MAX_STR_V_LENGTH = 10000000;

    /**
     * SQL常量，用于统一引用固定值。
     */
    private static final String SQL_DIR = "sql";

    /**
     * 时间戳，用于读取或保存对应领域对象。
     */
    @Autowired
    private InsertLatestTsRepository insertLatestTsRepository;

    /**
     * 集群，用于支撑当前网络或外部服务交互。
     */
    @Autowired
    protected CassandraCluster cluster;

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    protected TsKvDictionaryRepository dictionaryRepository;

    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private InstallScripts installScripts;

    /**
     * URL 地址，用于定位外部资源或本地资源。
     */
    @Value("${spring.datasource.url}")
    protected String dbUrl;

    /**
     * 用户，用于标识或展示当前对象。
     */
    @Value("${spring.datasource.username}")
    protected String dbUserName;

    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${spring.datasource.password}")
    protected String dbPassword;

    private final ConcurrentMap<String, Integer> tsKvDictionaryMap = new ConcurrentHashMap<>();

    protected static final ReentrantLock tsCreationLock = new ReentrantLock();

    /**
     * 功能：执行 `migrate` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void migrate() throws Exception {
        log.info("Performing migration of latest timeseries data from cassandra to SQL database ...");
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), SQL_DIR, "schema-ts-latest-psql.sql");
            loadSql(schemaUpdateFile, conn);
            conn.setAutoCommit(false);
            for (CassandraToSqlTable table : tables) {
                table.migrateToSql(cluster.getSession(), conn);
            }
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
     * 功能：获取实体。
     * 参数：
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    private TsKvLatestEntity getTsKvLatestEntity(CassandraToSqlColumnData[] data) {
        TsKvLatestEntity latestEntity = new TsKvLatestEntity();
        latestEntity.setEntityId(UUIDConverter.fromString(data[0].getValue()));
        latestEntity.setKey(getOrSaveKeyId(data[1].getValue()));
        latestEntity.setTs(Long.parseLong(data[2].getValue()));

        String strV = data[4].getValue();
        if (strV != null) {
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
            } catch (Exception e) {
            }
            if (longV != null) {
                latestEntity.setLongValue(longV);
            } else {
                Double doubleV = null;
                try {
                    doubleV = Double.parseDouble(data[6].getValue());
                } catch (Exception e) {
                }
                if (doubleV != null) {
                    latestEntity.setDoubleValue(doubleV);
                } else {

                    String jsonV = data[7].getValue();
                    if (StringUtils.isNoneEmpty(jsonV)) {
                        latestEntity.setJsonValue(jsonV);
                    } else {
                        Boolean boolV = null;
                        try {
                            boolV = Boolean.parseBoolean(data[3].getValue());
                        } catch (Exception e) {
                        }
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
     * 功能：获取键。
     * 参数：
     * - `strKey`：键。
     * 返回：数值结果。
     */
    protected Integer getOrSaveKeyId(String strKey) {
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
     * 功能：获取SQL。
     * 参数：
     * - `sqlFile`：`sqlFile` 参数。
     * - `conn`：`conn` 参数。
     * 返回：无。
     */
    private void loadSql(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), Charset.forName("UTF-8"));
        conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
        Thread.sleep(5000);
    }
}
