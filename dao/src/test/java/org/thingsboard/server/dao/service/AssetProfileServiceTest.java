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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. 类目的：`AssetProfileServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class AssetProfileServiceTest extends AbstractServiceTest {

    private IdComparator<AssetProfile> idComparator = new IdComparator<>();
    private IdComparator<AssetProfileInfo> assetProfileInfoIdComparator = new IdComparator<>();

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    AssetProfileService assetProfileService;
    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    AssetService assetService;

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAssetProfile() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        Assert.assertNotNull(savedAssetProfile);
        Assert.assertNotNull(savedAssetProfile.getId());
        Assert.assertTrue(savedAssetProfile.getCreatedTime() > 0);
        Assert.assertEquals(assetProfile.getName(), savedAssetProfile.getName());
        Assert.assertEquals(assetProfile.getDescription(), savedAssetProfile.getDescription());
        Assert.assertEquals(assetProfile.isDefault(), savedAssetProfile.isDefault());
        Assert.assertEquals(assetProfile.getDefaultRuleChainId(), savedAssetProfile.getDefaultRuleChainId());
        savedAssetProfile.setName("New asset profile");
        assetProfileService.saveAssetProfile(savedAssetProfile);
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfile.getName());
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetProfileById() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertNotNull(foundAssetProfile);
        Assert.assertEquals(savedAssetProfile, foundAssetProfile);
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetProfileInfoById() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        AssetProfileInfo foundAssetProfileInfo = assetProfileService.findAssetProfileInfoById(tenantId, savedAssetProfile.getId());
        Assert.assertNotNull(foundAssetProfileInfo);
        Assert.assertEquals(savedAssetProfile.getId(), foundAssetProfileInfo.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfileInfo.getName());
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDefaultAssetProfile() {
        AssetProfile foundDefaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(foundDefaultAssetProfile);
        Assert.assertNotNull(foundDefaultAssetProfile.getId());
        Assert.assertNotNull(foundDefaultAssetProfile.getName());
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDefaultAssetProfileInfo() {
        AssetProfileInfo foundDefaultAssetProfileInfo = assetProfileService.findDefaultAssetProfileInfo(tenantId);
        Assert.assertNotNull(foundDefaultAssetProfileInfo);
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getId());
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getName());
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindOrCreateAssetProfile() throws ExecutionException, InterruptedException {
        ListeningExecutorService testExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(100, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-test-scope")));
        try {
            List<ListenableFuture<AssetProfile>> futures = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                futures.add(testExecutor.submit(() -> assetProfileService.findOrCreateAssetProfile(tenantId, "Asset Profile 1")));
                futures.add(testExecutor.submit(() -> assetProfileService.findOrCreateAssetProfile(tenantId, "Asset Profile 2")));
            }

            List<AssetProfile> assetProfiles = Futures.allAsList(futures).get();
            assetProfiles.forEach(Assert::assertNotNull);
        } finally {
            testExecutor.shutdownNow();
        }
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSetDefaultAssetProfile() {
        AssetProfile assetProfile1 = this.createAssetProfile(tenantId, "Asset Profile 1");
        AssetProfile assetProfile2 = this.createAssetProfile(tenantId, "Asset Profile 2");

        AssetProfile savedAssetProfile1 = assetProfileService.saveAssetProfile(assetProfile1);
        AssetProfile savedAssetProfile2 = assetProfileService.saveAssetProfile(assetProfile2);

        boolean result = assetProfileService.setDefaultAssetProfile(tenantId, savedAssetProfile1.getId());
        Assert.assertTrue(result);
        AssetProfile defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(defaultAssetProfile);
        Assert.assertEquals(savedAssetProfile1.getId(), defaultAssetProfile.getId());
        result = assetProfileService.setDefaultAssetProfile(tenantId, savedAssetProfile2.getId());
        Assert.assertTrue(result);
        defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(defaultAssetProfile);
        Assert.assertEquals(savedAssetProfile2.getId(), defaultAssetProfile.getId());
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAssetProfileWithEmptyName() {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetProfileService.saveAssetProfile(assetProfile);
        });
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAssetProfileWithSameName() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        assetProfileService.saveAssetProfile(assetProfile);
        AssetProfile assetProfile2 = this.createAssetProfile(tenantId, "Asset Profile");
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetProfileService.saveAssetProfile(assetProfile2);
        });
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteAssetProfileWithExistingAsset() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("Test asset");
        asset.setAssetProfileId(savedAssetProfile.getId());
        assetService.saveAsset(asset);
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetProfileService.deleteAssetProfile(tenantId, savedAssetProfile.getId());
        });
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteAssetProfile() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        assetProfileService.deleteAssetProfile(tenantId, savedAssetProfile.getId());
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertNull(foundAssetProfile);
    }

    /**
     * 功能：验证资产相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetProfiles() {

        List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        assetProfiles.addAll(pageData.getData());

        for (int i = 0; i < 28; i++) {
            AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile" + i);
            assetProfiles.add(assetProfileService.saveAssetProfile(assetProfile));
        }

        List<AssetProfile> loadedAssetProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
            loadedAssetProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfiles, idComparator);

        Assert.assertEquals(assetProfiles, loadedAssetProfiles);

        for (AssetProfile assetProfile : loadedAssetProfiles) {
            if (!assetProfile.isDefault()) {
                assetProfileService.deleteAssetProfile(tenantId, assetProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    /**
     * 功能：验证资产配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAssetProfileInfos() {

        List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> assetProfilePageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(assetProfilePageData.hasNext());
        Assert.assertEquals(1, assetProfilePageData.getTotalElements());
        assetProfiles.addAll(assetProfilePageData.getData());

        for (int i = 0; i < 28; i++) {
            AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile" + i);
            assetProfiles.add(assetProfileService.saveAssetProfile(assetProfile));
        }

        List<AssetProfileInfo> loadedAssetProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<AssetProfileInfo> pageData;
        do {
            pageData = assetProfileService.findAssetProfileInfos(tenantId, pageLink);
            loadedAssetProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());


        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfileInfos, assetProfileInfoIdComparator);

        List<AssetProfileInfo> assetProfileInfos = assetProfiles.stream()
                .map(assetProfile -> new AssetProfileInfo(assetProfile.getId(), assetProfile.getTenantId(),
                        assetProfile.getName(), assetProfile.getImage(), assetProfile.getDefaultDashboardId())).collect(Collectors.toList());

        Assert.assertEquals(assetProfileInfos, loadedAssetProfileInfos);

        for (AssetProfile assetProfile : assetProfiles) {
            if (!assetProfile.isDefault()) {
                assetProfileService.deleteAssetProfile(tenantId, assetProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = assetProfileService.findAssetProfileInfos(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAllassetProfilesByTenantId() {
        int assetProfilesCount = 4; // 3 created + default
        var assetProfiles = new ArrayList<AssetProfile>(4);

        var profileC = assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, "profile C"));
        assetProfiles.add(assetProfileService.saveAssetProfile(profileC));


        var profileA = assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, "profile A"));
        assetProfiles.add(assetProfileService.saveAssetProfile(profileA));


        var profileB = assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, "profile B"));
        assetProfiles.add(assetProfileService.saveAssetProfile(profileB));


        assetProfiles.add(assetProfileService.findDefaultAssetProfile(tenantId));

        List<EntityInfo> sortedProfileInfos = assetProfiles.stream()
                .map(profile -> new EntityInfo(profile.getId(), profile.getName()))
                .sorted(Comparator.comparing(EntityInfo::getName))
                .collect(Collectors.toList());

        var assetProfileInfos = assetProfileService
                .findAssetProfileNamesByTenantId(tenantId, false);

        assertThat(assetProfileInfos).isNotNull();
        assertThat(assetProfileInfos).hasSize(assetProfilesCount);
        assertThat(assetProfileInfos).isEqualTo(sortedProfileInfos);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindActiveOnlyassetProfilesByTenantId() {

        String profileCName = "profile C";
        assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, profileCName));

        String profileAName = "profile A";
        assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, profileAName));

        String profileBName = "profile B";
        assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, profileBName));


        var assetProfileInfos = assetProfileService
                .findAssetProfileNamesByTenantId(tenantId, true);

        assertThat(assetProfileInfos).isNotNull();
        assertThat(assetProfileInfos).isEmpty();

        var assetC = new Asset();
        assetC.setName("Test asset C");
        assetC.setType(profileCName);
        assetC.setTenantId(tenantId);

        assetC = assetService.saveAsset(assetC);

        var assetA = new Asset();
        assetA.setName("Test asset A");
        assetA.setType(profileAName);
        assetA.setTenantId(tenantId);

        assetA = assetService.saveAsset(assetA);

        var assetB = new Asset();
        assetB.setName("Test asset B");
        assetB.setType(profileBName);
        assetB.setTenantId(tenantId);

        assetB = assetService.saveAsset(assetB);

        assetProfileInfos = assetProfileService
                .findAssetProfileNamesByTenantId(tenantId, true);

        var expected = List.of(
                new EntityInfo(assetA.getAssetProfileId(), profileAName),
                new EntityInfo(assetB.getAssetProfileId(), profileBName),
                new EntityInfo(assetC.getAssetProfileId(), profileCName)
        );

        assertThat(assetProfileInfos).isNotEmpty();
        assertThat(assetProfileInfos).hasSize(3);
        assertThat(assetProfileInfos).isEqualTo(expected);
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`AssetProfileServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
