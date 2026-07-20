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

import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.util.AsyncTask;

import java.util.function.Function;

/**
 * Created by ashvayka on 24.10.18.
 */
/**
 * 中文说明：
 * 1. `CassandraStatementTask` 是 ThingsBoard Common 中负责 `Cassandra Statement Task` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `AsyncTask`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Data
public class CassandraStatementTask implements AsyncTask {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final GuavaSession session;
    /**
     * 语句，表示当前对象所处状态。
     */
    private final Statement statement;

    /**
     * 功能：执行`Async`。
     * 参数：
     * - `executeAsyncFunction`：`executeAsyncFunction` 参数。
     * 返回：匹配的数据集合。
     */
    public ListenableFuture<TbResultSet> executeAsync(Function<Statement, TbResultSetFuture> executeAsyncFunction) {
        return Futures.transform(session.executeAsync(statement),
                result -> new TbResultSet(statement, result, executeAsyncFunction),
                MoreExecutors.directExecutor()
        );
    }

}
