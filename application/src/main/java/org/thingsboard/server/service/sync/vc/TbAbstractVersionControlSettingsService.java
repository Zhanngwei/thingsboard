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
package org.thingsboard.server.service.sync.vc;

import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `TbAbstractVersionControlSettingsService` 是 ThingsBoard Application 中负责版本控制的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
public abstract class TbAbstractVersionControlSettingsService<T extends Serializable> {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private final String settingsKey;
    private final AdminSettingsService adminSettingsService;
    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    private final TbTransactionalCache<TenantId, T> cache;
    private final Class<T> clazz;

    /**
     * 功能：创建 `TbAbstractVersionControlSettingsService` 实例，并初始化必要字段。
     * 参数：
     * - `adminSettingsService`：服务对象。
     * - `cache`：`cache` 参数。
     * - `clazz`：`clazz` 参数。
     * - `settingsKey`：配置对象。
     * 返回：新创建的对象实例。
     */
    public TbAbstractVersionControlSettingsService(AdminSettingsService adminSettingsService, TbTransactionalCache<TenantId, T> cache, Class<T> clazz, String settingsKey) {
        this.adminSettingsService = adminSettingsService;
        this.cache = cache;
        this.clazz = clazz;
        this.settingsKey = settingsKey;
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    public T get(TenantId tenantId) {
        return cache.getAndPutInTransaction(tenantId, () -> {
            AdminSettings adminSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, settingsKey);
            if (adminSettings != null) {
                try {
                    return JacksonUtil.convertValue(adminSettings.getJsonValue(), clazz);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load " + settingsKey + " settings!", e);
                }
            }
            return null;
        }, true);
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：处理结果。
     */
    public T save(TenantId tenantId, T settings) {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, settingsKey);
        if (adminSettings == null) {
            adminSettings = new AdminSettings();
            adminSettings.setKey(settingsKey);
            adminSettings.setTenantId(tenantId);
        }
        adminSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        AdminSettings savedAdminSettings = adminSettingsService.saveAdminSettings(tenantId, adminSettings);
        T savedSettings;
        try {
            savedSettings = JacksonUtil.convertValue(savedAdminSettings.getJsonValue(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load auto commit settings!", e);
        }
        //API calls to adminSettingsService are not in transaction, so we can simply evict the cache.
        cache.evict(tenantId);
        return savedSettings;
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    public boolean delete(TenantId tenantId) {
        boolean result = adminSettingsService.deleteAdminSettingsByTenantIdAndKey(tenantId, settingsKey);
        cache.evict(tenantId);
        return result;
    }

}
