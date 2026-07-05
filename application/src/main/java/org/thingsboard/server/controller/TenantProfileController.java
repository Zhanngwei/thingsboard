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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.tenant.profile.TbTenantProfileService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_PROFILE_INFO_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_PROFILE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

/**
 * 中文说明：
 * 1. 类目的：`TenantProfileController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class TenantProfileController extends BaseController {

    /**
     * 租户常量，用于统一引用固定值。
     */
    private static final String TENANT_PROFILE_INFO_DESCRIPTION = "Tenant Profile Info is a lightweight object that contains only id and name of the profile. ";

    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TbTenantProfileService tbTenantProfileService;

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `strTenantProfileId`：租户IDID。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Tenant Profile (getTenantProfileById)",
            notes = "Fetch the Tenant Profile object based on the provided Tenant Profile Id. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public TenantProfile getTenantProfileById(
            @ApiParam(value = TENANT_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
        return checkTenantProfileId(tenantProfileId, Operation.READ);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `strTenantProfileId`：租户IDID。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Tenant Profile Info (getTenantProfileInfoById)",
            notes = "Fetch the Tenant Profile Info object based on the provided Tenant Profile Id. " + TENANT_PROFILE_INFO_DESCRIPTION + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfo/{tenantProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityInfo getTenantProfileInfoById(
            @ApiParam(value = TENANT_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
        return checkNotNull(tenantProfileService.findTenantProfileInfoById(getTenantId(), tenantProfileId));
    }

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get default Tenant Profile Info (getDefaultTenantProfileInfo)",
            notes = "Fetch the default Tenant Profile Info object based. " + TENANT_PROFILE_INFO_DESCRIPTION + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfo/default", method = RequestMethod.GET)
    @ResponseBody
    public EntityInfo getDefaultTenantProfileInfo() throws ThingsboardException {
        return checkNotNull(tenantProfileService.findDefaultTenantProfileInfo(getTenantId()));
    }

    @ApiOperation(value = "Create Or update Tenant Profile (saveTenantProfile)",
            notes = "Create or update the Tenant Profile. When creating tenant profile, platform generates Tenant Profile Id as " + UUID_WIKI_LINK +
                    "The newly created Tenant Profile Id will be present in the response. " +
                    "Specify existing Tenant Profile Id id to update the Tenant Profile. " +
                    "Referencing non-existing Tenant Profile Id will cause 'Not Found' error. " +
                    "\n\nUpdate of the tenant profile configuration will cause immediate recalculation of API limits for all affected Tenants. " +
                    "\n\nThe **'profileData'** object is the part of Tenant Profile that defines API limits and Rate limits. " +
                    "\n\nYou have an ability to define maximum number of devices ('maxDevice'), assets ('maxAssets') and other entities. " +
                    "You may also define maximum number of messages to be processed per month ('maxTransportMessages', 'maxREExecutions', etc). " +
                    "The '*RateLimit' defines the rate limits using simple syntax. For example, '1000:1,20000:60' means up to 1000 events per second but no more than 20000 event per minute. " +
                    "Let's review the example of tenant profile data below: " +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"name\": \"Default\",\n" +
                    "  \"description\": \"Default tenant profile\",\n" +
                    "  \"isolatedTbRuleEngine\": false,\n" +
                    "  \"profileData\": {\n" +
                    "    \"configuration\": {\n" +
                    "      \"type\": \"DEFAULT\",\n" +
                    "      \"maxDevices\": 0,\n" +
                    "      \"maxAssets\": 0,\n" +
                    "      \"maxCustomers\": 0,\n" +
                    "      \"maxUsers\": 0,\n" +
                    "      \"maxDashboards\": 0,\n" +
                    "      \"maxRuleChains\": 0,\n" +
                    "      \"maxResourcesInBytes\": 0,\n" +
                    "      \"maxOtaPackagesInBytes\": 0,\n" +
                    "      \"maxResourceSize\": 0,\n" +
                    "      \"transportTenantMsgRateLimit\": \"1000:1,20000:60\",\n" +
                    "      \"transportTenantTelemetryMsgRateLimit\": \"1000:1,20000:60\",\n" +
                    "      \"transportTenantTelemetryDataPointsRateLimit\": \"1000:1,20000:60\",\n" +
                    "      \"transportDeviceMsgRateLimit\": \"20:1,600:60\",\n" +
                    "      \"transportDeviceTelemetryMsgRateLimit\": \"20:1,600:60\",\n" +
                    "      \"transportDeviceTelemetryDataPointsRateLimit\": \"20:1,600:60\",\n" +
                    "      \"maxTransportMessages\": 10000000,\n" +
                    "      \"maxTransportDataPoints\": 10000000,\n" +
                    "      \"maxREExecutions\": 4000000,\n" +
                    "      \"maxJSExecutions\": 5000000,\n" +
                    "      \"maxDPStorageDays\": 0,\n" +
                    "      \"maxRuleNodeExecutionsPerMessage\": 50,\n" +
                    "      \"maxEmails\": 0,\n" +
                    "      \"maxSms\": 0,\n" +
                    "      \"maxCreatedAlarms\": 1000,\n" +
                    "      \"defaultStorageTtlDays\": 0,\n" +
                    "      \"alarmsTtlDays\": 0,\n" +
                    "      \"rpcTtlDays\": 0,\n" +
                    "      \"queueStatsTtlDays\": 0,\n" +
                    "      \"ruleEngineExceptionsTtlDays\": 0,\n" +
                    "      \"warnThreshold\": 0\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"default\": true\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "Remove 'id', from the request body example (below) to create new Tenant Profile entity." +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * 返回：处理结果。
     */
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile", method = RequestMethod.POST)
    @ResponseBody
    public TenantProfile saveTenantProfile(@ApiParam(value = "A JSON value representing the tenant profile.")
                                           @RequestBody TenantProfile tenantProfile) throws ThingsboardException {
        TenantProfile oldProfile;
        if (tenantProfile.getId() == null) {
            accessControlService.checkPermission(getCurrentUser(), Resource.TENANT_PROFILE, Operation.CREATE);
            oldProfile = null;
        } else {
            oldProfile = checkTenantProfileId(tenantProfile.getId(), Operation.WRITE);
        }

        return tbTenantProfileService.save(getTenantId(), tenantProfile, oldProfile);
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `strTenantProfileId`：租户IDID。
     * 返回：无。
     */
    @ApiOperation(value = "Delete Tenant Profile (deleteTenantProfile)",
            notes = "Deletes the tenant profile. Referencing non-existing tenant profile Id will cause an error. Referencing profile that is used by the tenants will cause an error. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteTenantProfile(@ApiParam(value = TENANT_PROFILE_ID_PARAM_DESCRIPTION)
                                    @PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
        TenantProfile profile = checkTenantProfileId(tenantProfileId, Operation.DELETE);
        tbTenantProfileService.delete(getTenantId(), profile);
    }

    /**
     * 功能：更新租户。
     * 参数：
     * - `strTenantProfileId`：租户IDID。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Make tenant profile default (setDefaultTenantProfile)",
            notes = "Makes specified tenant profile to be default. Referencing non-existing tenant profile Id will cause an error. " + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfile/{tenantProfileId}/default", method = RequestMethod.POST)
    @ResponseBody
    public TenantProfile setDefaultTenantProfile(
            @ApiParam(value = TENANT_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable("tenantProfileId") String strTenantProfileId) throws ThingsboardException {
        checkParameter("tenantProfileId", strTenantProfileId);
        TenantProfileId tenantProfileId = new TenantProfileId(toUUID(strTenantProfileId));
        TenantProfile tenantProfile = checkTenantProfileId(tenantProfileId, Operation.WRITE);
        tenantProfileService.setDefaultTenantProfile(getTenantId(), tenantProfileId);
        return tenantProfile;
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `PAGE_SIZE_DESCRIPTION`：`PAGE_SIZE_DESCRIPTION` 参数。
     * - `pageSize`：`pageSize` 参数。
     * - `PAGE_NUMBER_DESCRIPTION`：`PAGE_NUMBER_DESCRIPTION` 参数。
     * - `page`：`page` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get Tenant Profiles (getTenantProfiles)", notes = "Returns a page of tenant profiles registered in the platform. " + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<TenantProfile> getTenantProfiles(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = TENANT_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = TENANT_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tenantProfileService.findTenantProfiles(getTenantId(), pageLink));
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `PAGE_SIZE_DESCRIPTION`：`PAGE_SIZE_DESCRIPTION` 参数。
     * - `pageSize`：`pageSize` 参数。
     * - `PAGE_NUMBER_DESCRIPTION`：`PAGE_NUMBER_DESCRIPTION` 参数。
     * - `page`：`page` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get Tenant Profiles Info (getTenantProfileInfos)", notes = "Returns a page of tenant profile info objects registered in the platform. "
            + TENANT_PROFILE_INFO_DESCRIPTION + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantProfileInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityInfo> getTenantProfileInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = TENANT_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = TENANT_PROFILE_INFO_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tenantProfileService.findTenantProfileInfos(getTenantId(), pageLink));
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `ids`：`ids` 参数。
     * 返回：匹配的数据集合。
     */
    @GetMapping(value = "/tenantProfiles", params = {"ids"})
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public List<TenantProfile> getTenantProfilesByIds(@RequestParam("ids") UUID[] ids) {
        return tenantProfileService.findTenantProfilesByIds(TenantId.SYS_TENANT_ID, ids);
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`TenantProfileController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
