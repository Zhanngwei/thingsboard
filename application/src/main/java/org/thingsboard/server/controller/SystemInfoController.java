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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.SystemParams;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`SystemInfoController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@ApiIgnore
@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class SystemInfoController extends BaseController {

    /**
     * 是否启用用户。
     */
    @Value("${security.user_token_access_enabled}")
    private boolean userTokenAccessEnabled;

    /**
     * 是否启用`tbel`。
     */
    @Value("${tbel.enabled:true}")
    private boolean tbelEnabled;

    /**
     * 是否满足遥测条件。
     */
    @Value("${state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    /**
     * 数量限制，用于控制数量、位置或分页范围。
     */
    @Value("${ui.dashboard.max_datapoints_limit}")
    private long maxDatapointsLimit;

    /**
     * `buildProperties` 字段，保存当前对象的对应属性。
     */
    @Autowired(required = false)
    private BuildProperties buildProperties;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private EntitiesVersionControlService versionControlService;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        JsonNode info = buildInfoObject();
        log.info("System build info: {}", info);
    }

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/system/info", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getSystemVersionInfo() {
        return buildInfoObject();
    }

    /**
     * 功能：获取Actor 系统。
     * 参数：无。
     * 返回：处理结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/system/params", method = RequestMethod.GET)
    @ResponseBody
    public SystemParams getSystemParams() throws ThingsboardException {
        SystemParams systemParams = new SystemParams();
        SecurityUser currentUser = getCurrentUser();
        TenantId tenantId = currentUser.getTenantId();
        CustomerId customerId = currentUser.getCustomerId();
        if (currentUser.isSystemAdmin() || currentUser.isTenantAdmin()) {
            systemParams.setUserTokenAccessEnabled(userTokenAccessEnabled);
        } else {
            systemParams.setUserTokenAccessEnabled(false);
        }
        boolean forceFullscreen = isForceFullscreen(currentUser);
        if (forceFullscreen && (currentUser.isTenantAdmin() || currentUser.isCustomerUser())) {
            PageLink pageLink = new PageLink(100);
            List<DashboardInfo> dashboards;
            if (currentUser.isTenantAdmin()) {
                dashboards = dashboardService.findDashboardsByTenantId(tenantId, pageLink).getData();
            } else {
                dashboards = dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink).getData();
            }
            systemParams.setAllowedDashboardIds(dashboards.stream().map(d -> d.getId().getId().toString()).collect(Collectors.toList()));
        } else {
            systemParams.setAllowedDashboardIds(Collections.emptyList());
        }
        systemParams.setEdgesSupportEnabled(edgesEnabled);
        if (currentUser.isTenantAdmin()) {
            systemParams.setHasRepository(versionControlService.getVersionControlSettings(tenantId) != null);
            systemParams.setTbelEnabled(tbelEnabled);
        } else {
            systemParams.setHasRepository(false);
            systemParams.setTbelEnabled(false);
        }
        if (currentUser.isTenantAdmin() || currentUser.isCustomerUser()) {
            systemParams.setPersistDeviceStateToTelemetry(persistToTelemetry);
        } else {
            systemParams.setPersistDeviceStateToTelemetry(false);
        }
        UserSettings userSettings = userSettingsService.findUserSettings(currentUser.getTenantId(), currentUser.getId(), UserSettingsType.GENERAL);
        ObjectNode userSettingsNode = userSettings == null ? JacksonUtil.newObjectNode() : (ObjectNode) userSettings.getSettings();
        if (!userSettingsNode.has("openedMenuSections")) {
            userSettingsNode.set("openedMenuSections", JacksonUtil.newArrayNode());
        }
        systemParams.setUserSettings(userSettingsNode);
        systemParams.setMaxDatapointsLimit(maxDatapointsLimit);
        if (!currentUser.isSystemAdmin()) {
            DefaultTenantProfileConfiguration tenantProfileConfiguration = tenantProfileCache.get(tenantId).getDefaultProfileConfiguration();
            systemParams.setMaxResourceSize(tenantProfileConfiguration.getMaxResourceSize());
        }
        return systemParams;
    }

    /**
     * 功能：判断`Force Fullscreen`。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * 返回：判断结果。
     */
    private boolean isForceFullscreen(SecurityUser currentUser) {
        return UserPrincipal.Type.PUBLIC_ID.equals(currentUser.getUserPrincipal().getType()) ||
                (currentUser.getAdditionalInfo() != null &&
                        currentUser.getAdditionalInfo().has("defaultDashboardFullscreen") &&
                        currentUser.getAdditionalInfo().get("defaultDashboardFullscreen").booleanValue());
    }

    /**
     * 功能：构建信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    private JsonNode buildInfoObject() {
        ObjectNode infoObject = JacksonUtil.newObjectNode();
        if (buildProperties != null) {
            infoObject.put("version", buildProperties.getVersion());
            infoObject.put("artifact", buildProperties.getArtifact());
            infoObject.put("name", buildProperties.getName());
        } else {
            infoObject.put("version", "unknown");
        }
        return infoObject;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`SystemInfoController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
