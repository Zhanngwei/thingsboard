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
package org.thingsboard.server.dao.nosql;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.common.util.ThingsBoardExecutors;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;

/**
 * Created by ashvayka on 21.02.17.
 */
/**
 * 中文说明：
 * 1. 类目的：`CassandraAbstractAsyncDao` 是 ThingsBoard DAO 模块 中的NoSQL/Cassandra 持久化类型，用于封装 Cassandra 异步查询、分页读取、语句构造和 NoSQL 时序/事件访问边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Cassandra Session、Guava Future、NoSQL DAO API、TimeseriesService 和测试容器。
 * 4. 生命周期：由 Spring 容器创建并在 Cassandra 查询生命周期内处理异步结果、分页和异常转换。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Async Callback / Template。
 */
public abstract class CassandraAbstractAsyncDao extends CassandraAbstractDao {

    /**
     * 执行器，负责处理对应任务或消息。
     */
    protected ExecutorService readResultsProcessingExecutor;

    /**
     * 线程池大小，用于控制处理规模或位置。
     */
    @Value("${cassandra.query.result_processing_threads:50}")
    private int threadPoolSize;

    /**
     * 功能：初始化或启动执行器。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void startExecutor() {
        readResultsProcessingExecutor = ThingsBoardExecutors.newWorkStealingPool(threadPoolSize, "cassandra-callback");
    }

    /**
     * 功能：停止或关闭执行器。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `future`：`future` 参数。
     * - `transformer`：`transformer` 参数。
     * 返回：匹配的数据集合。
     */
    protected <T> ListenableFuture<T> getFuture(TbResultSetFuture future, java.util.function.Function<TbResultSet, T> transformer) {
        return Futures.transform(future, new Function<TbResultSet, T>() {
            @Nullable
            @Override
            public T apply(@Nullable TbResultSet input) {
                return transformer.apply(input);
            }
        }, readResultsProcessingExecutor);
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `future`：`future` 参数。
     * - `transformer`：`transformer` 参数。
     * 返回：匹配的数据集合。
     */
    protected <T> ListenableFuture<T> getFutureAsync(TbResultSetFuture future, com.google.common.util.concurrent.AsyncFunction<TbResultSet, T> transformer) {
        return Futures.transformAsync(future, new AsyncFunction<TbResultSet, T>() {
            @Nullable
            @Override
            public ListenableFuture<T> apply(@Nullable TbResultSet input) {
                try {
                    return transformer.apply(input);
                } catch (Exception e) {
                    return Futures.immediateFailedFuture(e);
                }
            }
        }, readResultsProcessingExecutor);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraAbstractAsyncDao` 在 ThingsBoard DAO 模块 中承担NoSQL/Cassandra 持久化类型职责，核心目的是封装 Cassandra 异步查询、分页读取、语句构造和 NoSQL 时序/事件访问边界。
 * 2. 核心流程：构造 Cassandra 语句并异步执行，随后把 ResultSet 转换为 DAO API 需要的领域结果。
 * 3. 关键依赖：主要依赖或协作对象包括Cassandra Session、Guava Future、NoSQL DAO API、TimeseriesService 和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
