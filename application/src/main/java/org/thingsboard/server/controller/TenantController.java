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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.tenant.TbTenantService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_INFO_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

/**
 * 中文说明：
 * 1. 类目的：`TenantController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
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
public class TenantController extends BaseController {

    /**
     * 租户常量，用于统一引用固定值。
     */
    private static final String TENANT_INFO_DESCRIPTION = "The Tenant Info object extends regular Tenant object and includes Tenant Profile name. ";

    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantService tenantService;
    private final TbTenantService tbTenantService;

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `strTenantId`：租户IDID。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Tenant (getTenantById)",
            notes = "Fetch the Tenant object based on the provided Tenant Id. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}", method = RequestMethod.GET)
    @ResponseBody
    public Tenant getTenantById(
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION)
            @PathVariable(TENANT_ID) String strTenantId) throws ThingsboardException {
        checkParameter(TENANT_ID, strTenantId);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
        Tenant tenant = checkTenantId(tenantId, Operation.READ);
        if (!tenant.getAdditionalInfo().isNull()) {
            processDashboardIdFromAdditionalInfo((ObjectNode) tenant.getAdditionalInfo(), HOME_DASHBOARD);
        }
        return tenant;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `strTenantId`：租户IDID。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Tenant Info (getTenantInfoById)",
            notes = "Fetch the Tenant Info object based on the provided Tenant Id. " +
                    TENANT_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/info/{tenantId}", method = RequestMethod.GET)
    @ResponseBody
    public TenantInfo getTenantInfoById(
            @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION)
            @PathVariable(TENANT_ID) String strTenantId) throws ThingsboardException {
        checkParameter(TENANT_ID, strTenantId);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
        return checkTenantInfoId(tenantId, Operation.READ);
    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenant`：租户信息或租户标识。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Create Or update Tenant (saveTenant)",
            notes = "Create or update the Tenant. When creating tenant, platform generates Tenant Id as " + UUID_WIKI_LINK +
                    "Default Rule Chain and Device profile are also generated for the new tenants automatically. " +
                    "The newly created Tenant Id will be present in the response. " +
                    "Specify existing Tenant Id id to update the Tenant. " +
                    "Referencing non-existing Tenant Id will cause 'Not Found' error." +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Tenant entity." +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant", method = RequestMethod.POST)
    @ResponseBody
    public Tenant saveTenant(@ApiParam(value = "A JSON value representing the tenant.")
                             @RequestBody Tenant tenant) throws Exception {
        checkEntity(tenant.getId(), tenant, Resource.TENANT);
        return tbTenantService.save(tenant);
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `strTenantId`：租户IDID。
     * 返回：无。
     */
    @ApiOperation(value = "Delete Tenant (deleteTenant)",
            notes = "Deletes the tenant, it's customers, rule chains, devices and all other related entities. Referencing non-existing tenant Id will cause an error." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteTenant(@ApiParam(value = TENANT_ID_PARAM_DESCRIPTION)
                             @PathVariable(TENANT_ID) String strTenantId) throws Exception {
        checkParameter(TENANT_ID, strTenantId);
        TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
        Tenant tenant = checkTenantId(tenantId, Operation.DELETE);
        tbTenantService.delete(tenant);
    }

    /**
     * 功能：获取`Tenants`。
     * 参数：
     * - `PAGE_SIZE_DESCRIPTION`：`PAGE_SIZE_DESCRIPTION` 参数。
     * - `pageSize`：`pageSize` 参数。
     * - `PAGE_NUMBER_DESCRIPTION`：`PAGE_NUMBER_DESCRIPTION` 参数。
     * - `page`：`page` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get Tenants (getTenants)", notes = "Returns a page of tenants registered in the platform. " + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenants", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Tenant> getTenants(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = TENANT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = TENANT_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tenantService.findTenants(pageLink));
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
    @ApiOperation(value = "Get Tenants Info (getTenants)", notes = "Returns a page of tenant info objects registered in the platform. "
            + TENANT_INFO_DESCRIPTION + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<TenantInfo> getTenantInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = TENANT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = TENANT_INFO_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(tenantService.findTenantInfos(pageLink));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TenantController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
