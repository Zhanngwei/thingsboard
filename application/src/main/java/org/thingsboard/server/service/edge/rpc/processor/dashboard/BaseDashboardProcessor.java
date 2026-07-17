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
package org.thingsboard.server.service.edge.rpc.processor.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.Set;

/**
 * 中文说明：
 * 1. `BaseDashboardProcessor` 是 ThingsBoard Application 中处理仪表盘的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `BaseEdgeProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class BaseDashboardProcessor extends BaseEdgeProcessor {

    /**
     * 功能：保存或创建仪表盘。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `dashboardUpdateMsg`：待处理消息。
     * - `customerId`：客户IDID。
     * 返回：判断结果。
     */
    protected boolean saveOrUpdateDashboard(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg, CustomerId customerId) {
        boolean created = false;
        Dashboard dashboard = constructDashboardFromUpdateMsg(tenantId, dashboardId, dashboardUpdateMsg);
        if (dashboard == null) {
            throw new RuntimeException("[{" + tenantId + "}] dashboardUpdateMsg {" + dashboardUpdateMsg + "} cannot be converted to dashboard");
        }
        Set<ShortCustomerInfo> assignedCustomers = null;
        Dashboard dashboardById = dashboardService.findDashboardById(tenantId, dashboardId);
        if (dashboardById == null) {
            created = true;
            dashboard.setId(null);
        } else {
            dashboard.setId(dashboardId);
            assignedCustomers = filterNonExistingCustomers(tenantId, dashboardById.getAssignedCustomers());
        }

        dashboardValidator.validate(dashboard, Dashboard::getTenantId);
        if (created) {
            dashboard.setId(dashboardId);
        }
        Set<ShortCustomerInfo> msgAssignedCustomers = filterNonExistingCustomers(tenantId, dashboard.getAssignedCustomers());
        if (msgAssignedCustomers != null) {
            if (assignedCustomers == null) {
                assignedCustomers = msgAssignedCustomers;
            } else {
                assignedCustomers.addAll(msgAssignedCustomers);
            }
        }
        dashboard.setAssignedCustomers(assignedCustomers);
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard, false);
        if (msgAssignedCustomers != null && !msgAssignedCustomers.isEmpty()) {
            for (ShortCustomerInfo assignedCustomer : msgAssignedCustomers) {
                if (assignedCustomer.getCustomerId().equals(customerId)) {
                    dashboardService.assignDashboardToCustomer(tenantId, savedDashboard.getId(), assignedCustomer.getCustomerId());
                }
            }
        } else {
            unassignCustomersFromDashboard(tenantId, savedDashboard, customerId);
        }
        return created;
    }

    /**
     * 功能：执行 `unassignCustomersFromDashboard` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboard`：`dashboard` 参数。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void unassignCustomersFromDashboard(TenantId tenantId, Dashboard dashboard, CustomerId customerId) {
        if (dashboard.getAssignedCustomers() != null && !dashboard.getAssignedCustomers().isEmpty()) {
            for (ShortCustomerInfo assignedCustomer : dashboard.getAssignedCustomers()) {
                if (assignedCustomer.getCustomerId().equals(customerId)) {
                    dashboardService.unassignDashboardFromCustomer(tenantId, dashboard.getId(), assignedCustomer.getCustomerId());
                }
            }
        }
    }

    /**
     * 功能：执行 `constructDashboardFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `dashboardUpdateMsg`：待处理消息。
     * 返回：处理结果。
     */
    protected abstract Dashboard constructDashboardFromUpdateMsg(TenantId tenantId, DashboardId dashboardId, DashboardUpdateMsg dashboardUpdateMsg);

    /**
     * 功能：执行 `filterNonExistingCustomers` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assignedCustomers`：`assignedCustomers` 参数。
     * 返回：匹配的数据集合。
     */
    protected abstract Set<ShortCustomerInfo> filterNonExistingCustomers(TenantId tenantId, Set<ShortCustomerInfo> assignedCustomers);
}
