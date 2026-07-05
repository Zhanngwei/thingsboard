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
package org.thingsboard.server.dao.sql.asset;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.AssetProfileDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`JpaAssetDaoTest` 是 ThingsBoard DAO 测试模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public class JpaAssetDaoTest extends AbstractJpaDaoTest {

    /**
     * 租户对象，用于描述当前业务场景。
     */
    UUID tenantId1;
    UUID tenantId2;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    UUID customerId1;
    UUID customerId2;
    List<Asset> assets = new ArrayList<>();
    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetDao assetDao;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetProfileDao assetProfileDao;

    private Map<String, AssetProfileId> savedAssetProfiles = new HashMap<>();

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        tenantId1 = Uuids.timeBased();
        tenantId2 = Uuids.timeBased();
        customerId1 = Uuids.timeBased();
        customerId2 = Uuids.timeBased();
        for (int i = 0; i < 60; i++) {
            UUID assetId = Uuids.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            UUID customerId = i % 2 == 0 ? customerId1 : customerId2;
            assets.add(saveAsset(assetId, tenantId, customerId, "ASSET_" + i));
        }
        assertEquals(assets.size(), assetDao.find(TenantId.fromUUID(tenantId1)).size());
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() {
        for (Asset asset : assets) {
            assetDao.removeById(asset.getTenantId(), asset.getUuidId());
        }
        assets.clear();
        for (AssetProfileId assetProfileId : savedAssetProfiles.values()) {
            assetProfileDao.removeById(TenantId.SYS_TENANT_ID, assetProfileId.getId());
        }
        savedAssetProfiles.clear();
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceName0x00_thenSomeDatabaseException() {
        assertThatThrownBy(() -> assets.add(
                saveAsset(UUID.randomUUID(), tenantId2, customerId2, "F0929906\000\000\000\000\000\000\000\000\000")));
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantId() {
        PageLink pageLink = new PageLink(20, 0, "ASSET_");
        PageData<Asset> assets1 = assetDao.findAssetsByTenantId(tenantId1, pageLink);
        assertEquals(20, assets1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Asset> assets2 = assetDao.findAssetsByTenantId(tenantId1, pageLink);
        assertEquals(10, assets2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Asset> assets3 = assetDao.findAssetsByTenantId(tenantId1, pageLink);
        assertEquals(0, assets3.getData().size());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdAndCustomerId() {
        PageLink pageLink = new PageLink(20, 0, "ASSET_");
        PageData<Asset> assets1 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(20, assets1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Asset> assets2 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(10, assets2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Asset> assets3 = assetDao.findAssetsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(0, assets3.getData().size());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdAndIdsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        List<UUID> searchIds = getAssetsUuids(tenantId1);

        ListenableFuture<List<Asset>> assetsFuture = assetDao
                .findAssetsByTenantIdAndIdsAsync(tenantId1, searchIds);
        List<Asset> assets = assetsFuture.get(30, TimeUnit.SECONDS);
        assertNotNull(assets);
        assertEquals(searchIds.size(), assets.size());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdCustomerIdAndIdsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        List<UUID> searchIds = getAssetsUuids(tenantId1);

        ListenableFuture<List<Asset>> assetsFuture = assetDao
                .findAssetsByTenantIdAndCustomerIdAndIdsAsync(tenantId1, customerId1, searchIds);
        List<Asset> assets = assetsFuture.get(30, TimeUnit.SECONDS);
        assertNotNull(assets);
        assertEquals(searchIds.size(), assets.size());
    }

    /**
     * 功能：获取`Assets Uuids`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    private List<UUID> getAssetsUuids(UUID tenantId) {
        List<UUID> result = new ArrayList<>();
        for (Asset asset : assets) {
            if (asset.getTenantId().getId().equals(tenantId)) {
                result.add(asset.getUuidId());
            }
        }
        return result;
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdAndName() {
        UUID assetId = Uuids.timeBased();
        String name = "TEST_ASSET";
        assets.add(saveAsset(assetId, tenantId2, customerId2, name));

        Optional<Asset> assetOpt1 = assetDao.findAssetsByTenantIdAndName(tenantId2, name);
        assertTrue("Optional expected to be non-empty", assetOpt1.isPresent());
        assertEquals(assetId, assetOpt1.get().getId().getId());

        Optional<Asset> assetOpt2 = assetDao.findAssetsByTenantIdAndName(tenantId2, "NON_EXISTENT_NAME");
        assertFalse("Optional expected to be empty", assetOpt2.isPresent());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdAndType() {
        String type = "TYPE_2";
        assets.add(saveAsset(Uuids.timeBased(), tenantId2, customerId2, "TEST_ASSET", type));

        List<Asset> foundedAssetsByType = assetDao
                .findAssetsByTenantIdAndType(tenantId2, type, new PageLink(3)).getData();
        compareFoundedAssetByType(foundedAssetsByType, type);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetsByTenantIdAndCustomerIdAndType() {
        String type = "TYPE_2";
        assets.add(saveAsset(Uuids.timeBased(), tenantId2, customerId2, "TEST_ASSET", type));

        List<Asset> foundedAssetsByType = assetDao
                .findAssetsByTenantIdAndCustomerIdAndType(tenantId2, customerId2, type, new PageLink(3)).getData();
        compareFoundedAssetByType(foundedAssetsByType, type);
    }

    /**
     * 功能：执行 `compareFoundedAssetByType` 对应的处理。
     * 参数：
     * - `foundedAssetsByType`：类型。
     * - `type`：类型。
     * 返回：无。
     */
    private void compareFoundedAssetByType(List<Asset> foundedAssetsByType, String type) {
        assertNotNull(foundedAssetsByType);
        assertEquals(1, foundedAssetsByType.size());
        assertEquals(type, foundedAssetsByType.get(0).getType());
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTenantAssetTypesAsync() throws ExecutionException, InterruptedException, TimeoutException {
        // Assets with type "TYPE_1" added in setUp method
        assets.add(saveAsset(Uuids.timeBased(), tenantId1, customerId1, "TEST_ASSET_3", "TYPE_2"));
        assets.add(saveAsset(Uuids.timeBased(), tenantId1, customerId1, "TEST_ASSET_4", "TYPE_3"));
        assets.add(saveAsset(Uuids.timeBased(), tenantId1, customerId1, "TEST_ASSET_5", "TYPE_3"));
        assets.add(saveAsset(Uuids.timeBased(), tenantId1, customerId1, "TEST_ASSET_6", "TYPE_3"));

        assets.add(saveAsset(Uuids.timeBased(), tenantId2, customerId2, "TEST_ASSET_7", "TYPE_4"));

        List<EntitySubtype> tenant1Types = assetDao.findTenantAssetTypesAsync(tenantId1).get(30, TimeUnit.SECONDS);
        assertNotNull(tenant1Types);
        List<EntitySubtype> tenant2Types = assetDao.findTenantAssetTypesAsync(tenantId2).get(30, TimeUnit.SECONDS);
        assertNotNull(tenant2Types);

        List<String> types = List.of("default", "TYPE_1", "TYPE_2", "TYPE_3", "TYPE_4");
        assertEquals(getDifferentTypesCount(types, tenant1Types), tenant1Types.size());
        assertEquals(getDifferentTypesCount(types, tenant2Types), tenant2Types.size());
    }

    /**
     * 功能：获取数量。
     * 参数：
     * - `types`：类型。
     * - `foundedAssetsTypes`：类型。
     * 返回：数值结果。
     */
    private long getDifferentTypesCount(List<String> types, List<EntitySubtype> foundedAssetsTypes) {
        return foundedAssetsTypes.stream().filter(type -> types.contains(type.getType())).count();
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `id`：`id`ID。
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    private Asset saveAsset(UUID id, UUID tenantId, UUID customerId, String name) {
        return saveAsset(id, tenantId, customerId, name, null);
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `id`：`id`ID。
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `name`：名称。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private Asset saveAsset(UUID id, UUID tenantId, UUID customerId, String name, String type) {
        if (type == null) {
            type = "default";
        }
        Asset asset = new Asset();
        asset.setId(new AssetId(id));
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setCustomerId(new CustomerId(customerId));
        asset.setName(name);
        asset.setType(type);
        asset.setAssetProfileId(assetProfileId(type));
        return assetDao.save(TenantId.fromUUID(tenantId), asset);
    }

    /**
     * 功能：执行 `assetProfileId` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：匹配的数据集合。
     */
    private AssetProfileId assetProfileId(String type) {
        AssetProfileId assetProfileId = savedAssetProfiles.get(type);
        if (assetProfileId == null) {
            AssetProfile assetProfile = new AssetProfile();
            assetProfile.setName(type);
            assetProfile.setTenantId(TenantId.SYS_TENANT_ID);
            assetProfile.setDescription("Test");
            AssetProfile savedAssetProfile = assetProfileDao.save(TenantId.SYS_TENANT_ID, assetProfile);
            assetProfileId = savedAssetProfile.getId();
            savedAssetProfiles.put(type, assetProfileId);
        }
        return assetProfileId;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JpaAssetDaoTest` 在 ThingsBoard DAO 测试模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
