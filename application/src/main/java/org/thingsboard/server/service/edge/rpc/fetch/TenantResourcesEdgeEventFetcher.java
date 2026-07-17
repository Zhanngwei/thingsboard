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
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.resource.ResourceService;

/**
 * 中文说明：
 * 1. `TenantResourcesEdgeEventFetcher` 是 ThingsBoard Application 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BasePageableEdgeEventFetcher`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@AllArgsConstructor
@Slf4j
public class TenantResourcesEdgeEventFetcher extends BasePageableEdgeEventFetcher<TbResource> {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final ResourceService resourceService;

    /**
     * 功能：获取数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    PageData<TbResource> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return resourceService.findAllTenantResources(tenantId, pageLink);
    }

    /**
     * 功能：执行 `constructEdgeEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `tbResource`：`tbResource` 参数。
     * 返回：处理结果。
     */
    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, TbResource tbResource) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.TB_RESOURCE,
                EdgeEventActionType.ADDED, tbResource.getId(), null);
    }
}
