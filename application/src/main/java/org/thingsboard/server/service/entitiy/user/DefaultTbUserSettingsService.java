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
package org.thingsboard.server.service.entitiy.user;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.HasTitle;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.settings.AbstractUserDashboardInfo;
import org.thingsboard.server.common.data.settings.LastVisitedDashboardInfo;
import org.thingsboard.server.common.data.settings.StarredDashboardInfo;
import org.thingsboard.server.common.data.settings.UserDashboardAction;
import org.thingsboard.server.common.data.settings.UserDashboardsInfo;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.user.UserSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `DefaultTbUserSettingsService` 是 ThingsBoard Application 中负责用户的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TbUserSettingsService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbUserSettingsService implements TbUserSettingsService {

    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    private static final int MAX_DASHBOARD_INFO_LIST_SIZE = 10;
    private static final Predicate<HasTitle> EMPTY_TITLE = i -> StringUtils.isEmpty(i.getTitle());

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final UserSettingsService settingsService;
    private final DashboardService dashboardService;

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public UserSettings saveUserSettings(TenantId tenantId, UserSettings userSettings) {
        return settingsService.saveUserSettings(tenantId, userSettings);
    }

    /**
     * 功能：更新用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `type`：类型。
     * - `settings`：配置对象。
     * 返回：无。
     */
    @Override
    public void updateUserSettings(TenantId tenantId, UserId userId, UserSettingsType type, JsonNode settings) {
        settingsService.updateUserSettings(tenantId, userId, type, settings);
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `type`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public UserSettings findUserSettings(TenantId tenantId, UserId userId, UserSettingsType type) {
        return settingsService.findUserSettings(tenantId, userId, type);
    }

    /**
     * 功能：删除或清理用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `type`：类型。
     * - `jsonPaths`：文件或资源路径。
     * 返回：无。
     */
    @Override
    public void deleteUserSettings(TenantId tenantId, UserId userId, UserSettingsType type, List<String> jsonPaths) {
        settingsService.deleteUserSettings(tenantId, userId, type, jsonPaths);
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public UserDashboardsInfo findUserDashboardsInfo(TenantId tenantId, UserId id) {
        UserSettings us = findUserSettings(tenantId, id, UserSettingsType.VISITED_DASHBOARDS);
        if (us == null) {
            return UserDashboardsInfo.EMPTY;
        }
        UserDashboardsInfo stored = JacksonUtil.convertValue(us.getSettings(), UserDashboardsInfo.class);
        return refreshDashboardTitles(tenantId, stored);
    }

    /**
     * 功能：上报用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `dashboardId`：仪表盘IDID。
     * - `action`：`action` 参数。
     * 返回：处理结果。
     */
    @Override
    public UserDashboardsInfo reportUserDashboardAction(TenantId tenantId, UserId id, DashboardId dashboardId, UserDashboardAction action) {
        UserSettings us = findUserSettings(tenantId, id, UserSettingsType.VISITED_DASHBOARDS);
        UserDashboardsInfo stored = null;
        if (us != null) {
            stored = JacksonUtil.convertValue(us.getSettings(), UserDashboardsInfo.class);
        }
        if (stored == null) {
            stored = new UserDashboardsInfo();
        }

        switch (action) {
            case STAR:
                addToStarred(stored, dashboardId);
                break;
            case UNSTAR:
                removeFromStarred(stored, dashboardId);
                break;
            case VISIT:
                addToVisited(stored, dashboardId);
                break;
        }

        stored = refreshDashboardTitles(tenantId, stored);

        us = new UserSettings();
        us.setUserId(id);
        us.setType(UserSettingsType.VISITED_DASHBOARDS);
        us.setSettings(JacksonUtil.valueToTree(stored));
        saveUserSettings(tenantId, us);
        return stored;
    }

    /**
     * 功能：保存或创建`To Visited`。
     * 参数：
     * - `stored`：`stored` 参数。
     * - `dashboardId`：仪表盘IDID。
     * 返回：无。
     */
    private void addToVisited(UserDashboardsInfo stored, DashboardId dashboardId) {
        UUID id = dashboardId.getId();
        long ts = System.currentTimeMillis();
        var opt = stored.getLast().stream().filter(filterById(id)).findFirst();
        if (opt.isPresent()) {
            opt.get().setLastVisited(ts);
        } else {
            var newInfo = new LastVisitedDashboardInfo();
            newInfo.setId(id);
            newInfo.setStarred(stored.getStarred().stream().anyMatch(filterById(id)));
            newInfo.setLastVisited(System.currentTimeMillis());
            stored.getLast().add(newInfo);
        }
        stored.getLast().sort(Comparator.comparing(LastVisitedDashboardInfo::getLastVisited).reversed());
        if (stored.getLast().size() > MAX_DASHBOARD_INFO_LIST_SIZE) {
            stored.setLast(stored.getLast().stream().limit(MAX_DASHBOARD_INFO_LIST_SIZE).collect(Collectors.toList()));
        }
    }

    /**
     * 功能：删除或清理`From Starred`。
     * 参数：
     * - `stored`：`stored` 参数。
     * - `dashboardId`：仪表盘IDID。
     * 返回：无。
     */
    private void removeFromStarred(UserDashboardsInfo stored, DashboardId dashboardId) {
        UUID id = dashboardId.getId();
        stored.getStarred().removeIf(filterById(id));
        stored.getLast().stream().filter(d -> id.equals(d.getId())).findFirst().ifPresent(d -> d.setStarred(false));
    }

    /**
     * 功能：保存或创建`To Starred`。
     * 参数：
     * - `stored`：`stored` 参数。
     * - `dashboardId`：仪表盘IDID。
     * 返回：无。
     */
    private void addToStarred(UserDashboardsInfo stored, DashboardId dashboardId) {
        UUID id = dashboardId.getId();
        long ts = System.currentTimeMillis();
        var opt = stored.getStarred().stream().filter(filterById(id)).findFirst();
        if (opt.isPresent()) {
            opt.get().setStarredAt(ts);
        } else {
            var newInfo = new StarredDashboardInfo();
            newInfo.setId(id);
            newInfo.setStarredAt(System.currentTimeMillis());
            stored.getStarred().add(newInfo);
        }
        stored.getStarred().sort(Comparator.comparing(StarredDashboardInfo::getStarredAt).reversed());
        if (stored.getStarred().size() > MAX_DASHBOARD_INFO_LIST_SIZE) {
            stored.setStarred(stored.getStarred().stream().limit(MAX_DASHBOARD_INFO_LIST_SIZE).collect(Collectors.toList()));
        }
        Set<UUID> starredMap =
                stored.getStarred().stream().map(AbstractUserDashboardInfo::getId).collect(Collectors.toSet());
        stored.getLast().forEach(d -> d.setStarred(starredMap.contains(d.getId())));
    }

    /**
     * 功能：执行 `filterById` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    private Predicate<AbstractUserDashboardInfo> filterById(UUID id) {
        return d -> id.equals(d.getId());
    }

    /**
     * 功能：更新仪表盘。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `stored`：`stored` 参数。
     * 返回：处理结果。
     */
    private UserDashboardsInfo refreshDashboardTitles(TenantId tenantId, UserDashboardsInfo stored) {
        if (stored == null) {
            return UserDashboardsInfo.EMPTY;
        }
        stored.getLast().forEach(i -> i.setTitle(null));
        stored.getStarred().forEach(i -> i.setTitle(null));

        Set<UUID> uniqueIds = new HashSet<>();
        stored.getLast().stream().map(AbstractUserDashboardInfo::getId).forEach(uniqueIds::add);
        stored.getStarred().stream().map(AbstractUserDashboardInfo::getId).forEach(uniqueIds::add);

        Map<UUID, String> dashboardTitles = new HashMap<>();
        uniqueIds.forEach(id -> {
                    var title = dashboardService.findDashboardTitleById(tenantId, new DashboardId(id));
                    if (StringUtils.isNotEmpty(title)) {
                        dashboardTitles.put(id, title);
                    }
                }
        );

        stored.getLast().forEach(i -> i.setTitle(dashboardTitles.get(i.getId())));
        stored.getLast().removeIf(EMPTY_TITLE);
        stored.getStarred().forEach(i -> i.setTitle(dashboardTitles.get(i.getId())));
        stored.getStarred().removeIf(EMPTY_TITLE);
        return stored;
    }

}
