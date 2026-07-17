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
 * 1. `BaseAbstractSqlTimeseriesDao` 是 ThingsBoard DAO 中负责时序数据存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDaoListeningExecutorService`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
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
