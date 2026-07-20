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
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.cql.SyncCqlSession;
import com.datastax.oss.driver.api.core.session.Session;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.datastax.oss.driver.internal.core.cql.DefaultPrepareRequest;
import com.datastax.oss.driver.internal.core.cql.SinglePageResultSet;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.concurrent.ExecutionException;

/**
 * 中文说明：
 * 1. `GuavaSession` 是 ThingsBoard Common 中定义会话能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Session`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface GuavaSession extends Session, SyncCqlSession {

    GenericType<ListenableFuture<AsyncResultSet>> ASYNC =
            new GenericType<ListenableFuture<AsyncResultSet>>() {};

    GenericType<ListenableFuture<PreparedStatement>> ASYNC_PREPARED =
            new GenericType<ListenableFuture<PreparedStatement>>() {};

    /**
     * 功能：执行 `execute` 对应的处理。
     * 参数：
     * - `statement`：`statement` 参数。
     * 返回：匹配的数据集合。
     */
    @NonNull
    default ResultSet execute(@NonNull Statement<?> statement) {
        AsyncResultSet firstPage = getSafe(this.executeAsync(statement));
        if (firstPage.hasMorePages()) {
            return new GuavaMultiPageResultSet(this, statement, firstPage);
        } else {
            return new SinglePageResultSet(firstPage);
        }
    }

    /**
     * 功能：执行`Async`。
     * 参数：
     * - `statement`：`statement` 参数。
     * 返回：匹配的数据集合。
     */
    default ListenableFuture<AsyncResultSet> executeAsync(Statement<?> statement) {
        return this.execute(statement, ASYNC);
    }

    /**
     * 功能：执行`Async`。
     * 参数：
     * - `statement`：`statement` 参数。
     * 返回：匹配的数据集合。
     */
    default ListenableFuture<AsyncResultSet> executeAsync(String statement) {
        return this.executeAsync(SimpleStatement.newInstance(statement));
    }

    /**
     * 功能：执行 `prepareAsync` 对应的处理。
     * 参数：
     * - `statement`：`statement` 参数。
     * 返回：匹配的数据集合。
     */
    default ListenableFuture<PreparedStatement> prepareAsync(SimpleStatement statement) {
        return this.execute(new DefaultPrepareRequest(statement), ASYNC_PREPARED);
    }

    /**
     * 功能：执行 `prepareAsync` 对应的处理。
     * 参数：
     * - `statement`：`statement` 参数。
     * 返回：匹配的数据集合。
     */
    default ListenableFuture<PreparedStatement> prepareAsync(String statement) {
        return this.prepareAsync(SimpleStatement.newInstance(statement));
    }

    /**
     * 功能：获取`Safe`。
     * 参数：
     * - `future`：数据列表。
     * 返回：匹配的数据集合。
     */
    static AsyncResultSet getSafe(ListenableFuture<AsyncResultSet> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }
}
