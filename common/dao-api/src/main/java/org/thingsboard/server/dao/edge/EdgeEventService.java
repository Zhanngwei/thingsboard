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
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

/**
 * 中文说明：
 * 1. `EdgeEventService` 是 ThingsBoard Common 中定义事件能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface EdgeEventService {

    /**
     * 功能：保存或创建`Async`。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent);

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `seqIdStart`：`seqIdStart` 参数。
     * - `seqIdEnd`：`seqIdEnd` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    PageData<EdgeEvent> findEdgeEvents(TenantId tenantId, EdgeId edgeId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink);

    /**
     * Executes stored procedure to cleanup old edge events.
     * @param ttl the ttl for edge events in seconds
     */
    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `ttl`：`ttl` 参数。
     * 返回：无。
     */
    void cleanupEvents(long ttl);
}
