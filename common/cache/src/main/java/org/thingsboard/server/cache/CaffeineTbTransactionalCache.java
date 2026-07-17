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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. `CaffeineTbTransactionalCache` 是 ThingsBoard Common Cache 中管理缓存缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `Serializable`、`TbTransactionalCache`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@RequiredArgsConstructor
public abstract class CaffeineTbTransactionalCache<K extends Serializable, V extends Serializable> implements TbTransactionalCache<K, V> {

    /**
     * 管理器，负责处理对应任务或消息。
     */
    private final CacheManager cacheManager;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @Getter
    private final String cacheName;

    private final Lock lock = new ReentrantLock();
    private final Map<K, Set<UUID>> objectTransactions = new HashMap<>();
    private final Map<UUID, CaffeineTbCacheTransaction<K, V>> transactions = new HashMap<>();

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public TbCacheValueWrapper<V> get(K key) {
        return SimpleTbCacheValueWrapper.wrap(cacheManager.getCache(cacheName).get(key));
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void put(K key, V value) {
        lock.lock();
        try {
            failAllTransactionsByKey(key);
            cacheManager.getCache(cacheName).put(key, value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 功能：执行 `putIfAbsent` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void putIfAbsent(K key, V value) {
        lock.lock();
        try {
            failAllTransactionsByKey(key);
            doPutIfAbsent(key, value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：无。
     */
    @Override
    public void evict(K key) {
        lock.lock();
        try {
            failAllTransactionsByKey(key);
            doEvict(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `keys`：键。
     * 返回：无。
     */
    @Override
    public void evict(Collection<K> keys) {
        lock.lock();
        try {
            keys.forEach(key -> {
                failAllTransactionsByKey(key);
                doEvict(key);
            });
        } finally {
            lock.unlock();
        }
    }

    /**
     * 功能：删除或清理`Or Put`。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void evictOrPut(K key, V value) {
        //No need to put the value in case of Caffeine, because evict will cancel concurrent transaction used to "get" the missing value from cache.
        evict(key);
    }

    /**
     * 功能：执行 `newTransactionForKey` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public TbCacheTransaction<K, V> newTransactionForKey(K key) {
        return newTransaction(Collections.singletonList(key));
    }

    /**
     * 功能：执行 `newTransactionForKeys` 对应的处理。
     * 参数：
     * - `keys`：键。
     * 返回：处理结果。
     */
    @Override
    public TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys) {
        return newTransaction(keys);
    }

    /**
     * 功能：执行 `doPutIfAbsent` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    void doPutIfAbsent(Object key, Object value) {
        cacheManager.getCache(cacheName).putIfAbsent(key, value);
    }

    /**
     * 功能：执行 `doEvict` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：无。
     */
    void doEvict(K key) {
        cacheManager.getCache(cacheName).evict(key);
    }

    /**
     * 功能：执行 `newTransaction` 对应的处理。
     * 参数：
     * - `keys`：键。
     * 返回：处理结果。
     */
    TbCacheTransaction<K, V> newTransaction(List<K> keys) {
        lock.lock();
        try {
            var transaction = new CaffeineTbCacheTransaction<>(this, keys);
            var transactionId = transaction.getId();
            for (K key : keys) {
                objectTransactions.computeIfAbsent(key, k -> new HashSet<>()).add(transactionId);
            }
            transactions.put(transactionId, transaction);
            return transaction;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 功能：执行 `commit` 对应的处理。
     * 参数：
     * - `trId`：`trId`ID。
     * - `pendingPuts`：键值映射。
     * 返回：判断结果。
     */
    public boolean commit(UUID trId, Map<Object, Object> pendingPuts) {
        lock.lock();
        try {
            var tr = transactions.get(trId);
            var success = !tr.isFailed();
            if (success) {
                for (K key : tr.getKeys()) {
                    Set<UUID> otherTransactions = objectTransactions.get(key);
                    if (otherTransactions != null) {
                        for (UUID otherTrId : otherTransactions) {
                            if (trId == null || !trId.equals(otherTrId)) {
                                transactions.get(otherTrId).setFailed(true);
                            }
                        }
                    }
                }
                pendingPuts.forEach(this::doPutIfAbsent);
            }
            removeTransaction(trId);
            return success;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 功能：执行 `rollback` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    void rollback(UUID id) {
        lock.lock();
        try {
            removeTransaction(id);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 功能：删除或清理`Transaction`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    private void removeTransaction(UUID id) {
        CaffeineTbCacheTransaction<K, V> transaction = transactions.remove(id);
        if (transaction != null) {
            for (var key : transaction.getKeys()) {
                Set<UUID> transactions = objectTransactions.get(key);
                if (transactions != null) {
                    transactions.remove(id);
                    if (transactions.isEmpty()) {
                        objectTransactions.remove(key);
                    }
                }
            }
        }
    }

    /**
     * 功能：执行 `failAllTransactionsByKey` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：无。
     */
    private void failAllTransactionsByKey(K key) {
        Set<UUID> transactionsIds = objectTransactions.get(key);
        if (transactionsIds != null) {
            for (UUID otherTrId : transactionsIds) {
                transactions.get(otherTrId).setFailed(true);
            }
        }
    }

}
