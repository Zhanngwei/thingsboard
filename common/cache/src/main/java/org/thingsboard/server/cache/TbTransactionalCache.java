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
 * 1. 类目的：`TbTransactionalCache` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`TbTransactionalCache` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
