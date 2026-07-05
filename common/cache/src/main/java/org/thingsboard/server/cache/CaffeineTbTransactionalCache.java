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
 * 1. 类目的：`CaffeineTbTransactionalCache` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`CaffeineTbTransactionalCache` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
