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
package org.thingsboard.server.dao.sql.dashboard;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
/**
 * 中文说明：
 * 1. `JpaDashboardInfoDaoTest` 是 ThingsBoard DAO 中验证 `JpaDashboardInfoDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class JpaDashboardInfoDaoTest extends AbstractJpaDaoTest {

    /**
     * 仪表盘，用于读取或保存对应领域对象。
     */
    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDashboardsByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            createDashboard(tenantId1, i);
            createDashboard(tenantId2, i * 2);
        }

        PageLink pageLink = new PageLink(15, 0, "DASHBOARD");
        PageData<DashboardInfo> dashboardInfos1 = dashboardInfoDao.findDashboardsByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, dashboardInfos1.getData().size());

        PageData<DashboardInfo> dashboardInfos2 = dashboardInfoDao.findDashboardsByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, dashboardInfos2.getData().size());
    }

    /**
     * 功能：保存或创建仪表盘。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `index`：`index` 参数。
     * 返回：无。
     */
    private void createDashboard(UUID tenantId, int index) {
        DashboardInfo dashboardInfo = new DashboardInfo();
        dashboardInfo.setId(new DashboardId(Uuids.timeBased()));
        dashboardInfo.setTenantId(TenantId.fromUUID(tenantId));
        dashboardInfo.setTitle("DASHBOARD_" + index);
        dashboardInfoDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, dashboardInfo);
    }
}
