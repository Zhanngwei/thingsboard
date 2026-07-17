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
package org.thingsboard.server.service.edge.rpc;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.AdminSettingsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AssetProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.AssetsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.CustomerUsersEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DashboardsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DefaultProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DeviceProfilesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.DevicesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.EntityViewsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.OtaPackagesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.QueuesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.RuleChainsEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetTypesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.SystemWidgetsBundlesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantAdminUsersEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantResourcesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetTypesEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.TenantWidgetsBundlesEdgeEventFetcher;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 中文说明：
 * 1. `EdgeSyncCursor` 是 ThingsBoard Application 中承载边缘节点信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class EdgeSyncCursor {

    private final List<EdgeEventFetcher> fetchers = new LinkedList<>();

    /**
     * `currentIdx` 字段，保存当前对象的对应属性。
     */
    private int currentIdx = 0;

    /**
     * 功能：创建 `EdgeSyncCursor` 实例，并初始化必要字段。
     * 参数：
     * - `ctx`：处理上下文。
     * - `edge`：`edge` 参数。
     * - `fullSync`：`fullSync` 参数。
     * 返回：新创建的对象实例。
     */
    public EdgeSyncCursor(EdgeContextComponent ctx, Edge edge, boolean fullSync) {
        if (fullSync) {
            fetchers.add(new TenantEdgeEventFetcher(ctx.getTenantService()));
            fetchers.add(new QueuesEdgeEventFetcher(ctx.getQueueService()));
            fetchers.add(new RuleChainsEdgeEventFetcher(ctx.getRuleChainService()));
            fetchers.add(new AdminSettingsEdgeEventFetcher(ctx.getAdminSettingsService()));
            fetchers.add(new TenantAdminUsersEdgeEventFetcher(ctx.getUserService()));
            Customer publicCustomer = ctx.getCustomerService().findOrCreatePublicCustomer(edge.getTenantId());
            fetchers.add(new CustomerEdgeEventFetcher(publicCustomer.getId()));
            if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
                fetchers.add(new CustomerEdgeEventFetcher(edge.getCustomerId()));
                fetchers.add(new CustomerUsersEdgeEventFetcher(ctx.getUserService(), edge.getCustomerId()));
            }
        }
        fetchers.add(new DashboardsEdgeEventFetcher(ctx.getDashboardService()));
        fetchers.add(new DefaultProfilesEdgeEventFetcher(ctx.getDeviceProfileService(), ctx.getAssetProfileService()));
        fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
        fetchers.add(new AssetProfilesEdgeEventFetcher(ctx.getAssetProfileService()));
        fetchers.add(new DevicesEdgeEventFetcher(ctx.getDeviceService()));
        fetchers.add(new AssetsEdgeEventFetcher(ctx.getAssetService()));
        fetchers.add(new EntityViewsEdgeEventFetcher(ctx.getEntityViewService()));
        if (fullSync) {
            fetchers.add(new SystemWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new TenantWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new SystemWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new TenantWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new OtaPackagesEdgeEventFetcher(ctx.getOtaPackageService()));
            fetchers.add(new TenantResourcesEdgeEventFetcher(ctx.getResourceService()));
        }
    }

    /**
     * 功能：判断`Next`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean hasNext() {
        return fetchers.size() > currentIdx;
    }

    /**
     * 功能：获取`Next`。
     * 参数：无。
     * 返回：处理结果。
     */
    public EdgeEventFetcher getNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        EdgeEventFetcher edgeEventFetcher = fetchers.get(currentIdx);
        currentIdx++;
        return edgeEventFetcher;
    }

    /**
     * 功能：获取`Current Idx`。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getCurrentIdx() {
        return currentIdx;
    }
}
