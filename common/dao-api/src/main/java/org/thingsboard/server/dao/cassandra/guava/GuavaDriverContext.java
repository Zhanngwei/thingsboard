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

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.PrepareRequest;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.session.ProgrammaticArguments;
import com.datastax.oss.driver.internal.core.context.DefaultDriverContext;
import com.datastax.oss.driver.internal.core.cql.CqlPrepareAsyncProcessor;
import com.datastax.oss.driver.internal.core.cql.CqlPrepareSyncProcessor;
import com.datastax.oss.driver.internal.core.cql.CqlRequestAsyncProcessor;
import com.datastax.oss.driver.internal.core.cql.CqlRequestSyncProcessor;
import com.datastax.oss.driver.internal.core.session.RequestProcessorRegistry;

/**
 * A Custom {@link DefaultDriverContext} that overrides {@link #getRequestProcessorRegistry()} to
 * return a {@link RequestProcessorRegistry} that includes processors for returning guava futures.
 */
/**
 * 中文说明：
 * 1. `GuavaDriverContext` 是 ThingsBoard Common 中承载 `Guava Driver Context` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `DefaultDriverContext`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class GuavaDriverContext extends DefaultDriverContext {

    /**
     * 功能：创建 `GuavaDriverContext` 实例，并初始化必要字段。
     * 参数：
     * - `configLoader`：配置对象。
     * - `programmaticArguments`：`programmaticArguments` 参数。
     * 返回：新创建的对象实例。
     */
    public GuavaDriverContext(DriverConfigLoader configLoader, ProgrammaticArguments programmaticArguments) {
        super(configLoader, programmaticArguments);
    }

    /**
     * 功能：构建请求。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RequestProcessorRegistry buildRequestProcessorRegistry() {
        // Register the typical request processors, except instead of the normal async processors,
        // use GuavaRequestAsyncProcessor to return ListenableFutures in async methods.

        CqlRequestAsyncProcessor cqlRequestAsyncProcessor = new CqlRequestAsyncProcessor();
        CqlPrepareAsyncProcessor cqlPrepareAsyncProcessor = new CqlPrepareAsyncProcessor();
        CqlRequestSyncProcessor cqlRequestSyncProcessor =
                new CqlRequestSyncProcessor(cqlRequestAsyncProcessor);

        return new RequestProcessorRegistry(
                getSessionName(),
                cqlRequestSyncProcessor,
                new CqlPrepareSyncProcessor(cqlPrepareAsyncProcessor),
                new GuavaRequestAsyncProcessor<>(
                        cqlRequestAsyncProcessor, Statement.class, GuavaSession.ASYNC),
                new GuavaRequestAsyncProcessor<>(
                        cqlPrepareAsyncProcessor, PrepareRequest.class, GuavaSession.ASYNC_PREPARED));
    }
}
