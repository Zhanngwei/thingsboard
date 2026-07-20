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
package org.thingsboard.server.service.install.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 中文说明：
 * 1. `ImagesUpdater` 是 ThingsBoard Application 中处理 `Images Updater` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 它直接协作于事件源、上下文对象和后续处理组件。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImagesUpdater {
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final ImageService imageService;
    private final WidgetsBundleDao widgetsBundleDao;
    /**
     * 部件类型，用于读取或保存对应领域对象。
     */
    private final WidgetTypeDao widgetTypeDao;
    private final TenantDao tenantDao;
    /**
     * 仪表盘，用于读取或保存对应领域对象。
     */
    private final DashboardDao dashboardDao;
    private final DeviceProfileDao deviceProfileDao;
    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final AssetProfileDao assetProfileDao;

    /**
     * 功能：更新部件。
     * 参数：无。
     * 返回：无。
     */
    public void updateWidgetsBundlesImages() {
        log.info("Updating widgets bundles images...");
        var widgetsBundles = new PageDataIterable<>(widgetsBundleDao::findAllWidgetsBundles, 128);
        updateImages(widgetsBundles, "bundle", imageService::replaceBase64WithImageUrl, widgetsBundleDao);
    }

    /**
     * 功能：更新部件。
     * 参数：无。
     * 返回：无。
     */
    public void updateWidgetTypesImages() {
        log.info("Updating widget types images...");
        var widgetTypesIds = new PageDataIterable<>(widgetTypeDao::findAllWidgetTypesIds, 1024);
        updateImages(widgetTypesIds, "widget type", imageService::replaceBase64WithImageUrl, widgetTypeDao);
    }

    /**
     * 功能：更新`Dashboards Images`。
     * 参数：无。
     * 返回：无。
     */
    public void updateDashboardsImages() {
        log.info("Updating dashboards images...");
        updateImages("dashboard", dashboardDao::findIdsByTenantId, imageService::replaceBase64WithImageUrl, dashboardDao);
    }

    /**
     * 功能：保存或创建Actor 系统。
     * 参数：
     * - `defaultDashboard`：`defaultDashboard` 参数。
     * 返回：无。
     */
    public void createSystemImages(Dashboard defaultDashboard) {
        defaultDashboard.setTenantId(TenantId.SYS_TENANT_ID);
        boolean created = imageService.replaceBase64WithImageUrl(defaultDashboard);
        if (created) {
            log.debug("Created system images for default dashboard '{}'", defaultDashboard.getTitle());
        }
    }

    /**
     * 功能：更新设备。
     * 参数：无。
     * 返回：无。
     */
    public void updateDeviceProfilesImages() {
        log.info("Updating device profiles images...");
        var deviceProfiles = new PageDataIterable<>(deviceProfileDao::findAllWithImages, 256);
        updateImages(deviceProfiles, "device profile", imageService::replaceBase64WithImageUrl, deviceProfileDao);
    }

    /**
     * 功能：更新资产。
     * 参数：无。
     * 返回：无。
     */
    public void updateAssetProfilesImages() {
        log.info("Updating asset profiles images...");
        var assetProfiles = new PageDataIterable<>(assetProfileDao::findAllWithImages, 256);
        updateImages(assetProfiles, "asset profile", imageService::replaceBase64WithImageUrl, assetProfileDao);
    }

    /**
     * 功能：更新`Images`。
     * 参数：
     * - `entities`：`entities` 参数。
     * - `type`：类型。
     * - `updater`：`updater` 参数。
     * - `dao`：`dao` 参数。
     * 返回：无。
     */
    private <E extends HasImage> void updateImages(Iterable<E> entities, String type,
                                                   BiFunction<E, String, Boolean> updater, Dao<E> dao) {
        int updatedCount = 0;
        int totalCount = 0;
        for (E entity : entities) {
            totalCount++;
            try {
                boolean updated = updater.apply(entity, type);
                if (updated) {
                    dao.save(entity.getTenantId(), entity);
                    log.debug("[{}][{}] Updated {} images", entity.getTenantId(), entity.getName(), type);
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("[{}][{}] Failed to update {} images", entity.getTenantId(), entity.getName(), type, e);
            }
            if (totalCount % 100 == 0) {
                log.info("Processed {} {}s so far", totalCount, type);
            }
        }
        log.info("Updated {} {}s out of {}", updatedCount, type, totalCount);
    }

    /**
     * 功能：更新`Images`。
     * 参数：
     * - `entitiesIds`：`entitiesIds` 参数。
     * - `type`：类型。
     * - `updater`：`updater` 参数。
     * - `dao`：`dao` 参数。
     * 返回：无。
     */
    private <E extends HasImage> void updateImages(Iterable<? extends EntityId> entitiesIds, String type,
                                                   Function<E, Boolean> updater, Dao<E> dao) {
        int totalCount = 0;
        int updatedCount = 0;
        var counts = updateImages(entitiesIds, type, updater, dao, totalCount, updatedCount);
        totalCount = counts[0];
        updatedCount = counts[1];
        log.info("Updated {} {}s out of {}", updatedCount, type, totalCount);
    }

    /**
     * 功能：更新`Images`。
     * 参数：
     * - `type`：类型。
     * - `entityIdsByTenantId`：租户IDID。
     * - `updater`：`updater` 参数。
     * - `dao`：`dao` 参数。
     * 返回：无。
     */
    private <E extends HasImage> void updateImages(String type, BiFunction<TenantId, PageLink, PageData<? extends EntityId>> entityIdsByTenantId,
                                                   Function<E, Boolean> updater, Dao<E> dao) {
        int tenantCount = 0;
        int totalCount = 0;
        int updatedCount = 0;
        var tenantIds = new PageDataIterable<>(tenantDao::findTenantsIds, 128);
        for (var tenantId : tenantIds) {
            tenantCount++;
            var entitiesIds = new PageDataIterable<>(link -> entityIdsByTenantId.apply(tenantId, link), 128);
            var counts = updateImages(entitiesIds, type, updater, dao, totalCount, updatedCount);
            totalCount = counts[0];
            updatedCount = counts[1];
            if (tenantCount % 100 == 0) {
                log.info("Update {}s images: processed {} tenants so far", type, tenantCount);
            }
        }
        log.info("Updated {} {}s out of {}", updatedCount, type, totalCount);
    }

    /**
     * 功能：更新`Images`。
     * 参数：
     * - `entitiesIds`：`entitiesIds` 参数。
     * - `type`：类型。
     * - `updater`：`updater` 参数。
     * - `dao`：`dao` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private <E extends HasImage> int[] updateImages(Iterable<? extends EntityId> entitiesIds, String type,
                                                    Function<E, Boolean> updater, Dao<E> dao, int totalCount, int updatedCount) {
        for (EntityId id : entitiesIds) {
            totalCount++;
            E entity;
            try {
                entity = dao.findById(TenantId.SYS_TENANT_ID, id.getId());
            } catch (Exception e) {
                log.error("Failed to update {} images: error fetching {} by id [{}]: {}", type, type, id.getId(), StringUtils.abbreviate(e.toString(), 1000));
                continue;
            }
            try {
                boolean updated = updater.apply(entity);
                if (updated) {
                    dao.save(entity.getTenantId(), entity);
                    log.debug("[{}][{}] Updated {} images", entity.getTenantId(), entity.getName(), type);
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("[{}][{}] Failed to update {} images", entity.getTenantId(), entity.getName(), type, e);
            }
            if (totalCount % 100 == 0) {
                log.info("Processed {} {}s so far", totalCount, type);
            }
        }
        return new int[]{totalCount, updatedCount};
    }

}
