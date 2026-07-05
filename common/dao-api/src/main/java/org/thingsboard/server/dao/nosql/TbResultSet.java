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

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * 中文说明：
 * 1. 类目的：`TbResultSet` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class TbResultSet implements AsyncResultSet {

    /**
     * 语句，表示当前对象所处状态。
     */
    private final Statement originalStatement;
    private final AsyncResultSet delegate;
    /**
     * `executeAsyncFunction`集合，用于去重保存或快速判断对象是否存在。
     */
    private final Function<Statement, TbResultSetFuture> executeAsyncFunction;

    /**
     * 功能：创建 `TbResultSet` 实例，并初始化必要字段。
     * 参数：
     * - `originalStatement`：`originalStatement` 参数。
     * - `delegate`：`delegate` 参数。
     * - `executeAsyncFunction`：`executeAsyncFunction` 参数。
     * 返回：新创建的对象实例。
     */
    public TbResultSet(Statement originalStatement, AsyncResultSet delegate,
                       Function<Statement, TbResultSetFuture> executeAsyncFunction) {
        this.originalStatement = originalStatement;
        this.delegate = delegate;
        this.executeAsyncFunction = executeAsyncFunction;
    }

    /**
     * 功能：获取`Column Definitions`。
     * 参数：无。
     * 返回：处理结果。
     */
    @NonNull
    @Override
    public ColumnDefinitions getColumnDefinitions() {
        return delegate.getColumnDefinitions();
    }

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    @NonNull
    @Override
    public ExecutionInfo getExecutionInfo() {
        return delegate.getExecutionInfo();
    }

    /**
     * 功能：执行 `remaining` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int remaining() {
        return delegate.remaining();
    }

    /**
     * 功能：执行 `currentPage` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @NonNull
    @Override
    public Iterable<Row> currentPage() {
        return delegate.currentPage();
    }

    /**
     * 功能：判断`More Pages`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean hasMorePages() {
        return delegate.hasMorePages();
    }

    /**
     * 功能：获取`Next Page`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @NonNull
    @Override
    public CompletionStage<AsyncResultSet> fetchNextPage() throws IllegalStateException {
        return delegate.fetchNextPage();
    }

    /**
     * 功能：执行 `wasApplied` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean wasApplied() {
        return delegate.wasApplied();
    }

    /**
     * 功能：执行 `allRows` 对应的处理。
     * 参数：
     * - `executor`：`executor` 参数。
     * 返回：匹配的数据集合。
     */
    public ListenableFuture<List<Row>> allRows(Executor executor) {
        List<Row> allRows = new ArrayList<>();
        SettableFuture<List<Row>> resultFuture = SettableFuture.create();
        this.processRows(originalStatement, delegate, allRows, resultFuture, executor);
        return resultFuture;
    }

    /**
     * 功能：处理`Rows`。
     * 参数：
     * - `statement`：`statement` 参数。
     * - `resultSet`：`resultSet` 参数。
     * - `allRows`：数据列表。
     * - `resultFuture`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void processRows(Statement statement,
                             AsyncResultSet resultSet,
                             List<Row> allRows,
                             SettableFuture<List<Row>> resultFuture,
                             Executor executor) {
        allRows.addAll(loadRows(resultSet));
        if (resultSet.hasMorePages()) {
            ByteBuffer nextPagingState = resultSet.getExecutionInfo().getPagingState();
            Statement<?> nextStatement = statement.setPagingState(nextPagingState);
            TbResultSetFuture resultSetFuture = executeAsyncFunction.apply(nextStatement);
            Futures.addCallback(resultSetFuture,
                    new FutureCallback<TbResultSet>() {
                        @Override
                        public void onSuccess(@Nullable TbResultSet result) {
                            processRows(nextStatement, result,
                                    allRows, resultFuture, executor);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            resultFuture.setException(t);
                        }
                    }, executor != null ? executor : MoreExecutors.directExecutor()
            );
        } else {
            resultFuture.set(allRows);
        }
    }

    /**
     * 功能：获取`Rows`。
     * 参数：
     * - `resultSet`：`resultSet` 参数。
     * 返回：匹配的数据集合。
     */
    List<Row> loadRows(AsyncResultSet resultSet) {
        return Lists.newArrayList(resultSet.currentPage());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbResultSet` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
