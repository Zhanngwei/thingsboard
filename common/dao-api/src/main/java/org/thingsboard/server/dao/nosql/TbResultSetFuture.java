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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashvayka on 24.10.18.
 */
/**
 * 中文说明：
 * 1. `TbResultSetFuture` 是 ThingsBoard Common 中负责 `Tb Result Set` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `ListenableFuture`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
public class TbResultSetFuture implements ListenableFuture<TbResultSet> {

    /**
     * 异步结果集合，用于去重保存或快速判断对象是否存在。
     */
    private final SettableFuture<TbResultSet> mainFuture;

    /**
     * 功能：创建 `TbResultSetFuture` 实例，并初始化必要字段。
     * 参数：
     * - `mainFuture`：`mainFuture` 参数。
     * 返回：新创建的对象实例。
     */
    public TbResultSetFuture(SettableFuture<TbResultSet> mainFuture) {
        this.mainFuture = mainFuture;
    }

    /**
     * 功能：获取`Uninterruptibly`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public TbResultSet getUninterruptibly() {
        return getSafe();
    }

    /**
     * 功能：获取`Uninterruptibly`。
     * 参数：
     * - `timeout`：`timeout` 参数。
     * - `unit`：`unit` 参数。
     * 返回：匹配的数据集合。
     */
    public TbResultSet getUninterruptibly(long timeout, TimeUnit unit) throws TimeoutException {
        return getSafe(timeout, unit);
    }

    /**
     * 功能：执行 `cancel` 对应的处理。
     * 参数：
     * - `mayInterruptIfRunning`：`mayInterruptIfRunning` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return mainFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * 功能：判断`Cancelled`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isCancelled() {
        return mainFuture.isCancelled();
    }

    /**
     * 功能：判断`Done`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isDone() {
        return mainFuture.isDone();
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public TbResultSet get() throws InterruptedException, ExecutionException {
        return mainFuture.get();
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `timeout`：`timeout` 参数。
     * - `unit`：`unit` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public TbResultSet get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return mainFuture.get(timeout, unit);
    }

    /**
     * 功能：保存或创建监听器。
     * 参数：
     * - `listener`：`listener` 参数。
     * - `executor`：`executor` 参数。
     * 返回：无。
     */
    @Override
    public void addListener(Runnable listener, Executor executor) {
        mainFuture.addListener(listener, executor);
    }

    /**
     * 功能：获取`Safe`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private TbResultSet getSafe() {
        try {
            return mainFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 功能：获取`Safe`。
     * 参数：
     * - `timeout`：`timeout` 参数。
     * - `unit`：`unit` 参数。
     * 返回：匹配的数据集合。
     */
    private TbResultSet getSafe(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return mainFuture.get(timeout, unit);
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

}
