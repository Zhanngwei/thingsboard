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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 中文说明：
 * 1. `TbTransactionalCache` 是 ThingsBoard Common Cache 中定义缓存能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbTransactionalCache<K extends Serializable, V extends Serializable> {

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    String getCacheName();

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    TbCacheValueWrapper<V> get(K key);

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    void put(K key, V value);

    /**
     * 功能：执行 `putIfAbsent` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    void putIfAbsent(K key, V value);

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：无。
     */
    void evict(K key);

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `keys`：键。
     * 返回：无。
     */
    void evict(Collection<K> keys);

    /**
     * 功能：删除或清理`Or Put`。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    void evictOrPut(K key, V value);

    /**
     * 功能：执行 `newTransactionForKey` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    TbCacheTransaction<K, V> newTransactionForKey(K key);

    /**
     * Note that all keys should be in the same cache slot for redis. You may control the cache slot using '{}' bracers.
     * See CLUSTER KEYSLOT command for more details.
     * @param keys - list of keys to use
     * @return transaction object
     */
    /**
     * 功能：执行 `newTransactionForKeys` 对应的处理。
     * 参数：
     * - `keys`：键。
     * 返回：处理结果。
     */
    TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys);

    /**
     * 功能：获取`Or Fetch From DB`。
     * 参数：
     * - `key`：键。
     * - `dbCall`：`dbCall` 参数。
     * - `cacheNullValue`：值。
     * - `putToCache`：`putToCache` 参数。
     * 返回：处理结果。
     */
    default V getOrFetchFromDB(K key, Supplier<V> dbCall, boolean cacheNullValue, boolean putToCache) {
        if (putToCache) {
            return getAndPutInTransaction(key, dbCall, cacheNullValue);
        } else {
            TbCacheValueWrapper<V> cacheValueWrapper = get(key);
            if (cacheValueWrapper != null) {
                return cacheValueWrapper.get();
            }
            return dbCall.get();
        }
    }

    /**
     * 功能：获取`And Put In Transaction`。
     * 参数：
     * - `key`：键。
     * - `dbCall`：`dbCall` 参数。
     * - `cacheNullValue`：值。
     * 返回：处理结果。
     */
    default V getAndPutInTransaction(K key, Supplier<V> dbCall, boolean cacheNullValue) {
        TbCacheValueWrapper<V> cacheValueWrapper = get(key);
        if (cacheValueWrapper != null) {
            return cacheValueWrapper.get();
        }
        var cacheTransaction = newTransactionForKey(key);
        try {
            V dbValue = dbCall.get();
            if (dbValue != null || cacheNullValue) {
                cacheTransaction.putIfAbsent(key, dbValue);
                cacheTransaction.commit();
                return dbValue;
            } else {
                cacheTransaction.rollback();
                return null;
            }
        } catch (Throwable e) {
            cacheTransaction.rollback();
            throw e;
        }
    }

    /**
     * 功能：获取`Or Fetch From DB`。
     * 参数：
     * - `key`：键。
     * - `dbCall`：`dbCall` 参数。
     * - `cacheValueToResult`：值。
     * - `dbValueToCacheValue`：值。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    default <R> R getOrFetchFromDB(K key, Supplier<R> dbCall, Function<V, R> cacheValueToResult, Function<R, V> dbValueToCacheValue, boolean cacheNullValue, boolean putToCache) {
        if (putToCache) {
            return getAndPutInTransaction(key, dbCall, cacheValueToResult, dbValueToCacheValue, cacheNullValue);
        } else {
            TbCacheValueWrapper<V> cacheValueWrapper = get(key);
            if (cacheValueWrapper != null) {
                var cacheValue = cacheValueWrapper.get();
                return cacheValue == null ? null : cacheValueToResult.apply(cacheValue);
            }
            return dbCall.get();
        }
    }

    /**
     * 功能：获取`And Put In Transaction`。
     * 参数：
     * - `key`：键。
     * - `dbCall`：`dbCall` 参数。
     * - `cacheValueToResult`：值。
     * - `dbValueToCacheValue`：值。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    default <R> R getAndPutInTransaction(K key, Supplier<R> dbCall, Function<V, R> cacheValueToResult, Function<R, V> dbValueToCacheValue, boolean cacheNullValue) {
        TbCacheValueWrapper<V> cacheValueWrapper = get(key);
        if (cacheValueWrapper != null) {
            var cacheValue = cacheValueWrapper.get();
            return cacheValue == null ? null : cacheValueToResult.apply(cacheValue);
        }
        var cacheTransaction = newTransactionForKey(key);
        try {
            R dbValue = dbCall.get();
            if (dbValue != null || cacheNullValue) {
                cacheTransaction.putIfAbsent(key, dbValueToCacheValue.apply(dbValue));
                cacheTransaction.commit();
                return dbValue;
            } else {
                cacheTransaction.rollback();
                return null;
            }
        } catch (Throwable e) {
            cacheTransaction.rollback();
            throw e;
        }
    }

}
