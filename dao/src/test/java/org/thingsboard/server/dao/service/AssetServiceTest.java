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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * 中文说明：
 * 1. `AssetServiceTest` 是 ThingsBoard DAO 中验证 `AssetService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractServiceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class AssetServiceTest extends AbstractServiceTest {

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    AssetService assetService;
    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    AssetDao assetDao;
    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    CustomerService customerService;
    /**
     * 关系，提供当前类调用的业务操作。
     */
    @Autowired
    RelationService relationService;
    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetProfileService assetProfileService;
    /**
     * 管理器，负责处理对应任务或消息。
     */
    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    private IdComparator<Asset> idComparator = new IdComparator<>();

    /**
     * 功能：验证资产相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAsset() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);

        Assert.assertNotNull(savedAsset);
        Assert.assertNotNull(savedAsset.getId());
        Assert.assertTrue(savedAsset.getCreatedTime() > 0);
        Assert.assertEquals(asset.getTenantId(), savedAsset.getTenantId());
        Assert.assertNotNull(savedAsset.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedAsset.getCustomerId().getId());
        Assert.assertEquals(asset.getName(), savedAsset.getName());

        savedAsset.setName("My new asset");

        assetService.saveAsset(savedAsset);
        Asset foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertEquals(foundAsset.getName(), savedAsset.getName());

        assetService.deleteAsset(tenantId, savedAsset.getId());
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testShouldNotPutInCacheRolledbackAssetProfile() {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(StringUtils.randomAlphabetic(10));
        assetProfile.setTenantId(tenantId);

        Asset asset = new Asset();
        asset.setName("My asset" + StringUtils.randomAlphabetic(15));
        asset.setType(assetProfile.getName());
        asset.setTenantId(tenantId);

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = platformTransactionManager.getTransaction(def);
        try {
            assetProfileService.saveAssetProfile(assetProfile);
            assetService.saveAsset(asset);
        } finally {
            platformTransactionManager.rollback(status);
        }
        AssetProfile assetProfileByName = assetProfileService.findAssetProfileByName(tenantId, assetProfile.getName());
        Assert.assertNull(assetProfileByName);
    }

    /**
     * 功能：验证资产相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAssetWithEmptyName() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setType("default");
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetService.saveAsset(asset);
        });
    }

    /**
     * 功能：验证校验异常相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceWithNameContains0x00_thenDataValidationException() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setType("default");
        asset.setName("F0929906\000\000\000\000\000\000\000\000\000");
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetService.saveAsset(asset);
        });
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAssetWithEmptyTenant() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetService.saveAsset(asset);
        });
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAssetWithInvalidTenant() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetService.saveAsset(asset);
        });
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAssignAssetToNonExistentCustomer() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(tenantId);
        Asset savedAsset = assetService.saveAsset(asset);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                assetService.assignAssetToCustomer(tenantId, savedAsset.getId(), new CustomerId(Uuids.timeBased()));
            });
        } finally {
            assetService.deleteAsset(tenantId, savedAsset.getId());
        }
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAssignAssetToCustomerFromDifferentTenant() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(tenantId);
        Asset savedAsset = assetService.saveAsset(asset);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        Customer savedCustomer = customerService.saveCustomer(customer);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                assetService.assignAssetToCustomer(tenantId, savedAsset.getId(), savedCustomer.getId());
            });
        } finally {
            assetService.deleteAsset(tenantId, savedAsset.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }

    /**
     * 功能：验证资产ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetById() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);
        Asset foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNotNull(foundAsset);
        Assert.assertEquals(savedAsset, foundAsset);
        assetService.deleteAsset(tenantId, savedAsset.getId());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetTypesByTenantId() throws Exception {
        List<Asset> assets = new ArrayList<>();
        try {
            for (int i=0;i<3;i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset B"+i);
                asset.setType("typeB");
                assets.add(assetService.saveAsset(asset));
            }
            for (int i=0;i<7;i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset C"+i);
                asset.setType("typeC");
                assets.add(assetService.saveAsset(asset));
            }
            for (int i=0;i<9;i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset A"+i);
                asset.setType("typeA");
                assets.add(assetService.saveAsset(asset));
            }
            List<EntitySubtype> assetTypes = assetService.findAssetTypesByTenantId(tenantId).get();
            Assert.assertNotNull(assetTypes);
            Assert.assertEquals(3, assetTypes.size());
            Assert.assertEquals("typeA", assetTypes.get(0).getType());
            Assert.assertEquals("typeB", assetTypes.get(1).getType());
            Assert.assertEquals("typeC", assetTypes.get(2).getType());
        } finally {
            assets.forEach((asset) -> { assetService.deleteAsset(tenantId, asset.getId()); });
        }
    }

    /**
     * 功能：验证资产相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteAsset() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);
        EntityRelation relation = new EntityRelation(tenantId, savedAsset.getId(), EntityRelation.CONTAINS_TYPE);
        relationService.saveRelation(tenantId, relation);

        Asset foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNotNull(foundAsset);
        assetService.deleteAsset(tenantId, savedAsset.getId());
        foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNull(foundAsset);
        Assert.assertTrue(relationService.findByTo(tenantId, savedAsset.getId(), RelationTypeGroup.COMMON).isEmpty());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantId() {
        List<Asset> assets = new ArrayList<>();
        for (int i=0;i<13;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset"+i);
            asset.setType("default");
            assets.add(assetService.saveAsset(asset));
        }

        List<Asset> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);

        assetService.deleteAssetsByTenantId(tenantId);

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdAndName() {
        String title1 = "Asset title 1";
        List<AssetInfo> assetsTitle1 = new ArrayList<>();
        for (int i=0;i<13;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle1.add(new AssetInfo(assetService.saveAsset(asset), null, false, "default"));
        }
        String title2 = "Asset title 2";
        List<AssetInfo> assetsTitle2 = new ArrayList<>();
        for (int i=0;i<17;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle2.add(new AssetInfo(assetService.saveAsset(asset), null, false, "default"));
        }

        List<AssetInfo> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3, 0, title1);
        PageData<AssetInfo> pageData = null;
        do {
            pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle1, idComparator);
        Collections.sort(loadedAssetsTitle1, idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<AssetInfo> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdAndType() {
        String title1 = "Asset title 1";
        String type1 = "typeA";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i=0;i<13;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            assetsType1.add(assetService.saveAsset(asset));
        }
        String title2 = "Asset title 2";
        String type2 = "typeB";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i=0;i<17;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            assetsType2.add(assetService.saveAsset(asset));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndType(tenantId, type1, pageLink);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType1, idComparator);
        Collections.sort(loadedAssetsType1, idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = assetService.findAssetsByTenantIdAndType(tenantId, type2, pageLink);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdAndCustomerId() {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        List<AssetInfo> assets = new ArrayList<>();
        for (int i=0;i<13;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset"+i);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assets.add(new AssetInfo(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId), customer.getTitle(), customer.isPublic(), "default"));
        }

        List<AssetInfo> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<AssetInfo> pageData = null;
        do {
            pageData = assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);

        assetService.unassignCustomerAssets(tenantId, customerId);

        pageLink = new PageLink(4);
        pageData = assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdCustomerIdAndName() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        String title1 = "Asset title 1";
        List<Asset> assetsTitle1 = new ArrayList<>();
        for (int i=0;i<17;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assetsTitle1.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i=0;i<13;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assetsTitle2.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3, 0, title1);
        PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle1, idComparator);
        Collections.sort(loadedAssetsTitle1, idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<Asset> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdCustomerIdAndType() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        String title1 = "Asset title 1";
        String type1 = "typeC";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i=0;i<17;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            asset = assetService.saveAsset(asset);
            assetsType1.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }
        String title2 = "Asset title 2";
        String type2 = "typeD";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i=0;i<13;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            asset = assetService.saveAsset(asset);
            assetsType2.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType1, idComparator);
        Collections.sort(loadedAssetsType1, idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    /**
     * 功能：验证资产相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCleanCacheIfAssetRenamed() {
        String assetNameBeforeRename = StringUtils.randomAlphanumeric(15);
        String assetNameAfterRename = StringUtils.randomAlphanumeric(15);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(assetNameBeforeRename);
        asset.setType("default");
        assetService.saveAsset(asset);

        Asset savedAsset = assetService.findAssetByTenantIdAndName(tenantId, assetNameBeforeRename);

        savedAsset.setName(assetNameAfterRename);
        assetService.saveAsset(savedAsset);

        Asset renamedAsset = assetService.findAssetByTenantIdAndName(tenantId, assetNameBeforeRename);

        Assert.assertNull("Can't find asset by name in cache if it was renamed", renamedAsset);
        assetService.deleteAsset(tenantId, savedAsset.getId());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetInfoByTenantId() {
        Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("default");
        asset.setType("default");
        asset.setLabel("label");
        asset.setCustomerId(savedCustomer.getId());

        Asset savedAsset = assetService.saveAsset(asset);

        PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<AssetInfo> assetInfosWithLabel = assetService
                .findAssetInfosByTenantId(tenantId, pageLinkWithLabel).getData();

        Assert.assertFalse(assetInfosWithLabel.isEmpty());
        Assert.assertTrue(
                assetInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getLabel().equals(savedAsset.getLabel())
                        )
        );

        PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getTitle());
        List<AssetInfo> assetInfosWithCustomer = assetService
                .findAssetInfosByTenantId(tenantId, pageLinkWithCustomer).getData();

        Assert.assertFalse(assetInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                assetInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );

        PageLink pageLinkWithType = new PageLink(100, 0, asset.getType());
        List<AssetInfo> assetInfosWithType = assetService
                .findAssetInfosByTenantId(tenantId, pageLinkWithType).getData();

        Assert.assertFalse(assetInfosWithType.isEmpty());
        Assert.assertTrue(
                assetInfosWithType.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getType().equals(asset.getType())
                        )
        );
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetInfoByTenantIdAndType() {
        Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("default");
        asset.setType("default");
        asset.setLabel("label");
        asset.setCustomerId(savedCustomer.getId());
        Asset savedAsset = assetService.saveAsset(asset);

        PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<AssetInfo> assetInfosWithLabel = assetService
                .findAssetInfosByTenantIdAndType(tenantId, asset.getType(), pageLinkWithLabel).getData();

        Assert.assertFalse(assetInfosWithLabel.isEmpty());
        Assert.assertTrue(
                assetInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileName().equals(savedAsset.getType())
                                        && d.getLabel().equals(savedAsset.getLabel())
                        )
        );

        PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getTitle());
        List<AssetInfo> assetInfosWithCustomer = assetService
                .findAssetInfosByTenantIdAndType(tenantId, asset.getType(), pageLinkWithCustomer).getData();

        Assert.assertFalse(assetInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                assetInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileName().equals(savedAsset.getType())
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetInfoByTenantIdAndAssetProfileId() {
        Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("default");
        asset.setLabel("label");
        asset.setCustomerId(savedCustomer.getId());
        Asset savedAsset = assetService.saveAsset(asset);

        PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<AssetInfo> assetInfosWithLabel = assetService
                .findAssetInfosByTenantIdAndAssetProfileId(tenantId, savedAsset.getAssetProfileId(), pageLinkWithLabel).getData();

        Assert.assertFalse(assetInfosWithLabel.isEmpty());
        Assert.assertTrue(
                assetInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileId().equals(savedAsset.getAssetProfileId())
                                        && d.getLabel().equals(savedAsset.getLabel())
                        )
        );

        PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getTitle());
        List<AssetInfo> assetInfosWithCustomer = assetService
                .findAssetInfosByTenantIdAndAssetProfileId(tenantId, savedAsset.getAssetProfileId(), pageLinkWithCustomer).getData();

        Assert.assertFalse(assetInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                assetInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileId().equals(savedAsset.getAssetProfileId())
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );
    }

}
