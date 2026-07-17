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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.edge.EdgeEventService;

/**
 * 中文说明：
 * 1. `GeneralEdgeEventFetcher` 是 ThingsBoard Application 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `EdgeEventFetcher`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@AllArgsConstructor
@Slf4j
public class GeneralEdgeEventFetcher implements EdgeEventFetcher {

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private final Long queueStartTs;
    private Long seqIdStart;
    /**
     * 序号，用于控制处理规模或位置。
     */
    @Getter
    private Long seqIdEnd;
    /**
     * 当前处理是否已经启动。
     */
    @Getter
    private boolean seqIdNewCycleStarted;
    private Long maxReadRecordsCount;
    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    private final EdgeEventService edgeEventService;

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageLink getPageLink(int pageSize) {
        return new TimePageLink(
                pageSize,
                0,
                null,
                null,
                queueStartTs,
                System.currentTimeMillis());
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        try {
            PageData<EdgeEvent> edgeEvents = edgeEventService.findEdgeEvents(tenantId, edge.getId(), seqIdStart, seqIdEnd, (TimePageLink) pageLink);
            if (edgeEvents.getData().isEmpty()) {
                this.seqIdEnd = Math.max(this.maxReadRecordsCount, seqIdStart - this.maxReadRecordsCount);
                edgeEvents = edgeEventService.findEdgeEvents(tenantId, edge.getId(), 0L, seqIdEnd, (TimePageLink) pageLink);
                if (edgeEvents.getData().stream().anyMatch(ee -> ee.getSeqId() < seqIdStart)) {
                    log.info("[{}] seqId column of edge_event table started new cycle [{}]", tenantId, edge.getId());
                    this.seqIdNewCycleStarted = true;
                    this.seqIdStart = 0L;
                } else {
                    edgeEvents = new PageData<>();
                    log.warn("[{}] unexpected edge notification message received. " +
                            "no new events found and seqId column of edge_event table doesn't started new cycle [{}]", tenantId, edge.getId());
                }
            }
            return edgeEvents;
        } catch (Exception e) {
            log.error("[{}] failed to find edge events [{}]", tenantId, edge.getId());
        }
        return new PageData<>();
    }
}
