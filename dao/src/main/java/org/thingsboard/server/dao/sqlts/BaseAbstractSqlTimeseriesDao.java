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
package org.thingsboard.server.dao.sqlts;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.model.sqlts.dictionary.TsKvDictionary;
import org.thingsboard.server.dao.model.sqlts.dictionary.TsKvDictionaryCompositeKey;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.sqlts.dictionary.TsKvDictionaryRepository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`BaseAbstractSqlTimeseriesDao` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
@Slf4j
public abstract class BaseAbstractSqlTimeseriesDao extends JpaAbstractDaoListeningExecutorService {

    private final ConcurrentMap<String, Integer> tsKvDictionaryMap = new ConcurrentHashMap<>();
    protected static final ReentrantLock tsCreationLock = new ReentrantLock();
    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    protected TsKvDictionaryRepository dictionaryRepository;

    /**
     * 功能：获取键。
     * 参数：
     * - `strKey`：键。
     * 返回：数值结果。
     */
    protected Integer getOrSaveKeyId(String strKey) {
        Integer keyId = tsKvDictionaryMap.get(strKey);
        if (keyId == null) {
            Optional<TsKvDictionary> tsKvDictionaryOptional;
            tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
            if (tsKvDictionaryOptional.isEmpty()) {
                tsCreationLock.lock();
                try {
                    keyId = tsKvDictionaryMap.get(strKey);
                    if (keyId != null) {
                        return keyId;
                    }
                    tsKvDictionaryOptional = dictionaryRepository.findById(new TsKvDictionaryCompositeKey(strKey));
                    if (tsKvDictionaryOptional.isEmpty()) {
                        TsKvDictionary tsKvDictionary = new TsKvDictionary();
                        tsKvDictionary.setKey(strKey);
                        try {
                            TsKvDictionary saved = dictionaryRepository.save(tsKvDictionary);
                            tsKvDictionaryMap.put(saved.getKey(), saved.getKeyId());
                            keyId = saved.getKeyId();
                        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
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
     * 功能：获取时间戳。
     * 参数：
     * - `query`：`query` 参数。
     * - `future`：数据列表。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<ReadTsKvQueryResult> getReadTsKvQueryResultFuture(ReadTsKvQuery query, ListenableFuture<List<Optional<? extends AbstractTsKvEntity>>> future) {
        return Futures.transform(future, new Function<>() {
            @Nullable
            @Override
            public ReadTsKvQueryResult apply(@Nullable List<Optional<? extends AbstractTsKvEntity>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                List<? extends AbstractTsKvEntity> data = results.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
                var lastTs = data.stream().map(AbstractTsKvEntity::getAggValuesLastTs).filter(Objects::nonNull).max(Long::compare);
                if (lastTs.isEmpty()) {
                    lastTs = data.stream().map(AbstractTsKvEntity::getTs).filter(Objects::nonNull).max(Long::compare);
                }
                return new ReadTsKvQueryResult(query.getId(), DaoUtil.convertDataList(data), lastTs.orElse(query.getStartTs()));
            }
        }, service);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseAbstractSqlTimeseriesDao` 在 ThingsBoard DAO 模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
