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
 * 1. 类目的：`AssetServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
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

/*
 * 本类总结：
 * 1. 核心职责：`AssetServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
