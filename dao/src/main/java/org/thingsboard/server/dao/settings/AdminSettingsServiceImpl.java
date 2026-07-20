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
 * 1. `AdminSettingsServiceImpl` 是 ThingsBoard DAO 中负责 `Admin` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AdminSettingsService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
