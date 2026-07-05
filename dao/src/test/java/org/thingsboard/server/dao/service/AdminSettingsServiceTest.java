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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 中文说明：
 * 1. 类目的：`AdminSettingsServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class AdminSettingsServiceTest extends AbstractServiceTest {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    AdminSettingsService adminSettingsService;

    /**
     * 功能：验证配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAdminSettingsByKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        Assert.assertNotNull(adminSettings);
        adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
        Assert.assertNotNull(adminSettings);
        adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "unknown");
        Assert.assertNull(adminSettings);
    }

    /**
     * 功能：验证配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAdminSettingsById() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        AdminSettings foundAdminSettings = adminSettingsService.findAdminSettingsById(SYSTEM_TENANT_ID, adminSettings.getId());
        Assert.assertNotNull(foundAdminSettings);
        Assert.assertEquals(adminSettings, foundAdminSettings);
    }

    /**
     * 功能：验证配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAdminSettings() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        JsonNode json = adminSettings.getJsonValue();
        ((ObjectNode) json).put("baseUrl", "http://myhost.org");
        adminSettings.setJsonValue(json);
        adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, adminSettings);
        AdminSettings savedAdminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "general");
        Assert.assertNotNull(savedAdminSettings);
        Assert.assertEquals(adminSettings.getJsonValue(), savedAdminSettings.getJsonValue());
    }

    /**
     * 功能：验证配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAdminSettingsWithEmptyKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
        adminSettings.setKey(null);
        Assertions.assertThrows(DataValidationException.class, () -> {
            adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, adminSettings);
        });
    }

    /**
     * 功能：验证配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testChangeAdminSettingsKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(SYSTEM_TENANT_ID, "mail");
        adminSettings.setKey("newKey");
        Assertions.assertThrows(DataValidationException.class, () -> {
            adminSettingsService.saveAdminSettings(SYSTEM_TENANT_ID, adminSettings);
        });
    }

    /**
     * 功能：验证 `whenSavingAdminSettingsWithAlreadyExistingKey_thenReturnError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void whenSavingAdminSettingsWithAlreadyExistingKey_thenReturnError() {
        String key = RandomStringUtils.randomAlphanumeric(15);
        ObjectNode value = JacksonUtil.newObjectNode().put("test", "test");

        AdminSettings systemSettings = new AdminSettings();
        systemSettings.setTenantId(TenantId.SYS_TENANT_ID);
        systemSettings.setKey(key);
        systemSettings.setJsonValue(value);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, systemSettings);

        assertThatThrownBy(() -> {
            adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, systemSettings);
        }).hasMessageContaining("already exists");

        AdminSettings tenantSettings = new AdminSettings();
        tenantSettings.setTenantId(tenantId);
        tenantSettings.setKey(key);
        tenantSettings.setJsonValue(value);
        assertDoesNotThrow(() -> {
            adminSettingsService.saveAdminSettings(tenantId, tenantSettings);
        });

        assertThatThrownBy(() -> {
            adminSettingsService.saveAdminSettings(tenantId, tenantSettings);
        }).hasMessageContaining("already exists");
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AdminSettingsServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
