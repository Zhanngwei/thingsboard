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
package org.thingsboard.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.internal.core.util.CountingIterator;
import com.datastax.oss.driver.internal.core.util.concurrent.BlockingOperation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 中文说明：
 * 1. `GuavaMultiPageResultSet` 是 ThingsBoard Common 中负责 `Guava Multi Page` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `ResultSet`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
public class GuavaMultiPageResultSet implements ResultSet {

    /**
     * `iterator` 字段，保存当前对象的对应属性。
     */
    private final RowIterator iterator;
    private final List<ExecutionInfo> executionInfos = new ArrayList<>();
    /**
     * `columnDefinitions` 字段，保存当前对象的对应属性。
     */
    private ColumnDefinitions columnDefinitions;

    /**
     * 功能：创建 `GuavaMultiPageResultSet` 实例，并初始化必要字段。
     * 参数：
     * - `session`：会话对象。
     * - `statement`：`statement` 参数。
     * - `firstPage`：`firstPage` 参数。
     * 返回：新创建的对象实例。
     */
    public GuavaMultiPageResultSet(@NonNull GuavaSession session, @NonNull Statement statement, @NonNull AsyncResultSet firstPage) {
        assert firstPage.hasMorePages();
        this.iterator = new RowIterator(session, statement, firstPage);
        this.executionInfos.add(firstPage.getExecutionInfo());
        this.columnDefinitions = firstPage.getColumnDefinitions();
    }

    /**
     * 功能：获取`Column Definitions`。
     * 参数：无。
     * 返回：处理结果。
     */
    @NonNull
    @Override
    public ColumnDefinitions getColumnDefinitions() {
        return columnDefinitions;
    }

    /**
     * 功能：获取`Execution Infos`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @NonNull
    @Override
    public List<ExecutionInfo> getExecutionInfos() {
        return executionInfos;
    }

    /**
     * 功能：判断`Fully Fetched`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isFullyFetched() {
        return iterator.isFullyFetched();
    }

    /**
     * 功能：获取`Available Without Fetching`。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getAvailableWithoutFetching() {
        return iterator.remaining();
    }

    /**
     * 功能：执行 `iterator` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @NonNull
    @Override
    public Iterator<Row> iterator() {
        return iterator;
    }

    /**
     * 功能：执行 `wasApplied` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean wasApplied() {
        return iterator.wasApplied();
    }

    /**
     * 中文说明：
     * 1. `RowIterator` 是 ThingsBoard Common 中负责 `Row Iterator` 存取的访问组件。
     * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
     * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
     * 4. 直接依赖的类型边界包括 `CountingIterator`。
     * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
     * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
     */
    private class RowIterator extends CountingIterator<Row> {
        /**
         * 会话，保存当前连接或交互过程的会话信息。
         */
        private GuavaSession session;
        private Statement statement;
        /**
         * `currentPage`集合，用于去重保存或快速判断对象是否存在。
         */
        private AsyncResultSet currentPage;
        private Iterator<Row> currentRows;

        /**
         * 功能：创建 `GuavaMultiPageResultSet` 实例，并初始化必要字段。
         * 参数：
         * - `session`：会话对象。
         * - `statement`：`statement` 参数。
         * - `firstPage`：`firstPage` 参数。
         * 返回：新创建的对象实例。
         */
        private RowIterator(GuavaSession session, Statement statement, AsyncResultSet firstPage) {
            super(firstPage.remaining());
            this.session = session;
            this.statement = statement;
            this.currentPage = firstPage;
            this.currentRows = firstPage.currentPage().iterator();
        }

        /**
         * 功能：执行 `computeNext` 对应的处理。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        protected Row computeNext() {
            maybeMoveToNextPage();
            return currentRows.hasNext() ? currentRows.next() : endOfData();
        }

        /**
         * 功能：执行 `maybeMoveToNextPage` 对应的处理。
         * 参数：无。
         * 返回：无。
         */
        private void maybeMoveToNextPage() {
            if (!currentRows.hasNext() && currentPage.hasMorePages()) {
                BlockingOperation.checkNotDriverThread();
                ByteBuffer nextPagingState = currentPage.getExecutionInfo().getPagingState();
                this.statement = this.statement.setPagingState(nextPagingState);
                AsyncResultSet nextPage = GuavaSession.getSafe(this.session.executeAsync(this.statement));
                currentPage = nextPage;
                remaining += nextPage.remaining();
                currentRows = nextPage.currentPage().iterator();
                executionInfos.add(nextPage.getExecutionInfo());
                // The definitions can change from page to page if this result set was built from a bound
                // 'SELECT *', and the schema was altered.
                columnDefinitions = nextPage.getColumnDefinitions();
            }
        }

        /**
         * 功能：判断`Fully Fetched`。
         * 参数：无。
         * 返回：判断结果。
         */
        private boolean isFullyFetched() {
            return !currentPage.hasMorePages();
        }

        /**
         * 功能：执行 `wasApplied` 对应的处理。
         * 参数：无。
         * 返回：判断结果。
         */
        private boolean wasApplied() {
            return currentPage.wasApplied();
        }
    }
}
