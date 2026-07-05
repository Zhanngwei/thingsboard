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
package org.thingsboard.server.dao.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;

/**
 * 中文说明：
 * 1. 类目的：`AdminSettingsServiceImpl` 是 ThingsBoard DAO 模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 容器创建为 DAO、Repository、Service 或配置 Bean，并随应用生命周期参与请求处理。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
 */
@Service
@Slf4j
public class AdminSettingsServiceImpl implements AdminSettingsService {
    
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AdminSettingsDao adminSettingsDao;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private DataValidator<AdminSettings> adminSettingsValidator;

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `adminSettingsId`：配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AdminSettings findAdminSettingsById(TenantId tenantId, AdminSettingsId adminSettingsId) {
        log.trace("Executing findAdminSettingsById [{}]", adminSettingsId);
        Validator.validateId(adminSettingsId, "Incorrect adminSettingsId " + adminSettingsId);
        return  adminSettingsDao.findById(tenantId, adminSettingsId.getId());
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public AdminSettings findAdminSettingsByKey(TenantId tenantId, String key) {
        log.trace("Executing findAdminSettingsByKey [{}]", key);
        Validator.validateString(key, "Incorrect key " + key);
        return findAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, key);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public AdminSettings findAdminSettingsByTenantIdAndKey(TenantId tenantId, String key) {
        return adminSettingsDao.findByTenantIdAndKey(tenantId.getId(), key);
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `adminSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public AdminSettings saveAdminSettings(TenantId tenantId, AdminSettings adminSettings) {
        log.trace("Executing saveAdminSettings [{}]", adminSettings);
        adminSettingsValidator.validate(adminSettings, data -> tenantId);
        if (adminSettings.getKey().equals("mail")){
            AdminSettings mailSettings = findAdminSettingsByKey(tenantId, "mail");
            if (mailSettings != null) {
                JsonNode newJsonValue = adminSettings.getJsonValue();
                JsonNode oldJsonValue = mailSettings.getJsonValue();
                if (!newJsonValue.has("password") && oldJsonValue.has("password")){
                     ((ObjectNode) newJsonValue).put("password", oldJsonValue.get("password").asText());
                }
                if (!newJsonValue.has("refreshToken") && oldJsonValue.has("refreshToken")){
                    ((ObjectNode) newJsonValue).put("refreshToken", oldJsonValue.get("refreshToken").asText());
                }
                dropTokenIfProviderInfoChanged(newJsonValue, oldJsonValue);
            }
        }
        if (adminSettings.getTenantId() == null) {
            adminSettings.setTenantId(TenantId.SYS_TENANT_ID);
        }
        return adminSettingsDao.save(tenantId, adminSettings);
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：判断结果。
     */
    @Override
    public boolean deleteAdminSettingsByTenantIdAndKey(TenantId tenantId, String key) {
        log.trace("Executing deleteAdminSettings, tenantId [{}], key [{}]", tenantId, key);
        Validator.validateString(key, "Incorrect key " + key);
        return adminSettingsDao.removeByTenantIdAndKey(tenantId.getId(), key);
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteAdminSettingsByTenantId(TenantId tenantId) {
        adminSettingsDao.removeByTenantId(tenantId.getId());
    }

    /**
     * 功能：执行 `dropTokenIfProviderInfoChanged` 对应的处理。
     * 参数：
     * - `newJsonValue`：值。
     * - `oldJsonValue`：值。
     * 返回：无。
     */
    private void dropTokenIfProviderInfoChanged(JsonNode newJsonValue, JsonNode oldJsonValue) {
        if (newJsonValue.has("enableOauth2") && newJsonValue.get("enableOauth2").asBoolean()){
            if (!newJsonValue.get("providerId").equals(oldJsonValue.get("providerId")) ||
                    !newJsonValue.get("clientId").equals(oldJsonValue.get("clientId")) ||
                    !newJsonValue.get("clientSecret").equals(oldJsonValue.get("clientSecret")) ||
                    !newJsonValue.get("redirectUri").equals(oldJsonValue.get("redirectUri")) ||
                    (newJsonValue.has("providerTenantId") && !newJsonValue.get("providerTenantId").equals(oldJsonValue.get("providerTenantId")))){
                ((ObjectNode) newJsonValue).put("tokenGenerated", false);
                ((ObjectNode) newJsonValue).remove("refreshToken");
                ((ObjectNode) newJsonValue).remove("refreshTokenExpires");
            }
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AdminSettingsServiceImpl` 在 ThingsBoard DAO 模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
