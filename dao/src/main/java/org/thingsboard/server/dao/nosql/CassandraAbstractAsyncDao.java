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
 * 1. `CassandraAbstractAsyncDao` 是 ThingsBoard DAO 中负责 `Cassandra Async` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `CassandraAbstractDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
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
