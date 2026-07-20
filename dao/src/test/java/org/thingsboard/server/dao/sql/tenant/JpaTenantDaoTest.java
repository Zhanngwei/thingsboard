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
package org.thingsboard.server.dao.sql.tenant;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.TenantProfileServiceTest;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantProfileDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
/**
 * 中文说明：
 * 1. `JpaTenantDaoTest` 是 ThingsBoard DAO 中验证 `JpaTenantDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class JpaTenantDaoTest extends AbstractJpaDaoTest {

    /**
     * 租户，用于读取或保存对应领域对象。
     */
    @Autowired
    private TenantDao tenantDao;

    /**
     * 租户，用于读取或保存对应领域对象。
     */
    @Autowired
    private TenantProfileDao tenantProfileDao;

    List<Tenant> createdTenants = new ArrayList<>();
    /**
     * 租户对象，用于描述当前业务场景。
     */
    TenantProfile tenantProfile;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() throws Exception {
        tenantProfile = tenantProfileDao.save(TenantId.SYS_TENANT_ID, TenantProfileServiceTest.createTenantProfile("default tenant profile"));
        assertThat(tenantProfile).as("tenant profile").isNotNull();
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() throws Exception {
        createdTenants.forEach((tenant)-> tenantDao.removeById(TenantId.SYS_TENANT_ID, tenant.getUuidId()));
        tenantProfileDao.removeById(TenantId.SYS_TENANT_ID, tenantProfile.getUuidId());
    }

    @Test
    //@DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    /**
     * 功能：验证`Find Tenants`相关场景。
     * 参数：无。
     * 返回：无。
     */
    public void testFindTenants() {
        createTenants();
        assertEquals(30, tenantDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());

        PageLink pageLink = new PageLink(20, 0, "title");
        PageData<Tenant> tenants1 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID, pageLink);
        assertEquals(20, tenants1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Tenant> tenants2 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID,
                pageLink);
        assertEquals(10, tenants2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Tenant> tenants3 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID,
                pageLink);
        assertEquals(0, tenants3.getData().size());
    }

    /**
     * 功能：保存或创建`Tenants`。
     * 参数：无。
     * 返回：无。
     */
    private void createTenants() {
        for (int i = 0; i < 30; i++) {
            createTenant("TITLE", i);
        }
    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `title`：`title` 参数。
     * - `index`：`index` 参数。
     * 返回：无。
     */
    void createTenant(String title, int index) {
        Tenant tenant = new Tenant();
        tenant.setId(TenantId.fromUUID(Uuids.timeBased()));
        tenant.setTitle(title + "_" + index);
        tenant.setTenantProfileId(tenantProfile.getId());
        createdTenants.add(tenantDao.save(TenantId.SYS_TENANT_ID, tenant));
    }

    @Test
    //@DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    public void testIsExistsTenantById() {
        final UUID uuid = Uuids.timeBased();
        final TenantId tenantId = new TenantId(uuid);
        assertThat(tenantDao.existsById(tenantId, uuid)).as("Is tenant exists before save").isFalse();

        final Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTitle("Tenant " + uuid);
        tenant.setTenantProfileId(tenantProfile.getId());

        createdTenants.add(tenantDao.save(TenantId.SYS_TENANT_ID, tenant));

        assertThat(tenantDao.existsById(tenantId, uuid)).as("Is tenant exists after save").isTrue();

    }

}
