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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.widgets.type.TbWidgetTypeService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.thingsboard.server.controller.ControllerConstants.INLINE_IMAGES;
import static org.thingsboard.server.controller.ControllerConstants.INLINE_IMAGES_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.ControllerConstants.WIDGET_TYPE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.WIDGET_TYPE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.WIDGET_TYPE_TEXT_SEARCH_DESCRIPTION;

/**
 * 中文说明：
 * 1. 类目的：`WidgetTypeController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
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
@RequiredArgsConstructor
public class WidgetTypeController extends AutoCommitController {

    /**
     * 部件类型，提供当前类调用的业务操作。
     */
    private final TbWidgetTypeService tbWidgetTypeService;
    private final ImageService imageService;

    /**
     * `and` 类，封装当前模块中的一组相关职责。
     */
    private static final String WIDGET_TYPE_DESCRIPTION = "Widget Type represents the template for widget creation. Widget Type and Widget are similar to class and object in OOP theory.";
    private static final String WIDGET_TYPE_DETAILS_DESCRIPTION = "Widget Type Details extend Widget Type and add image and description properties. " +
            "Those properties are useful to edit the Widget Type but they are not required for Dashboard rendering. ";
    /**
     * 部件类型常量，用于统一引用固定值。
     */
    private static final String WIDGET_TYPE_INFO_DESCRIPTION = "Widget Type Info is a lightweight object that represents Widget Type but does not contain the heavyweight widget descriptor JSON";
    private static final String TENANT_ONLY_PARAM_DESCRIPTION = "Optional boolean parameter indicating whether only tenant widget types should be returned";
    /**
     * 描述信息常量，用于统一引用固定值。
     */
    private static final String FULL_SEARCH_PARAM_DESCRIPTION = "Optional boolean parameter indicating whether search widgets by description not only by name";
    private static final String DEPRECATED_FILTER_ALLOWABLE_VALUES = "ALL, ACTUAL, DEPRECATED";
    /**
     * 描述信息常量，用于统一引用固定值。
     */
    private static final String DEPRECATED_FILTER_PARAM_DESCRIPTION = "Optional string parameter indicating whether to include deprecated widgets";
    private static final String UPDATE_EXISTING_BY_FQN_PARAM_DESCRIPTION = "Optional boolean parameter indicating whether to update existing widget type by FQN if present instead of creating new one";
    /**
     * 部件类型常量，用于统一引用固定值。
     */
    private static final String WIDGET_TYPE_ARRAY_DESCRIPTION = "A list of string values separated by comma ',' representing one of the widget type value";
    private static final String WIDGET_TYPE_ALLOWABLE_VALUES = "timeseries, latest, control, alarm, static";

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `WIDGET_TYPE_ID_PARAM_DESCRIPTION`：类型。
     * - `strWidgetTypeId`：部件类型ID。
     * - `INLINE_IMAGES`：`INLINE_IMAGES` 参数。
     * - `inlineImages`：`inlineImages` 参数。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Widget Type Details (getWidgetTypeById)",
            notes = "Get the Widget Type Details based on the provided Widget Type Id. " + WIDGET_TYPE_DETAILS_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.GET)
    @ResponseBody
    public WidgetTypeDetails getWidgetTypeById(
            @ApiParam(value = WIDGET_TYPE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetTypeId") String strWidgetTypeId,
            @ApiParam(value = INLINE_IMAGES_DESCRIPTION)
            @RequestParam(value = INLINE_IMAGES, required = false) boolean inlineImages) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
        var result = checkWidgetTypeId(widgetTypeId, Operation.READ);
        if (inlineImages) {
            imageService.inlineImages(result);
        }
        return result;
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `WIDGET_TYPE_ID_PARAM_DESCRIPTION`：类型。
     * - `strWidgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Widget Type Info (getWidgetTypeInfoById)",
            notes = "Get the Widget Type Info based on the provided Widget Type Id. " + WIDGET_TYPE_DETAILS_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypeInfo/{widgetTypeId}", method = RequestMethod.GET)
    @ResponseBody
    public WidgetTypeInfo getWidgetTypeInfoById(
            @ApiParam(value = WIDGET_TYPE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetTypeId") String strWidgetTypeId) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
        return new WidgetTypeInfo(checkWidgetTypeId(widgetTypeId, Operation.READ));
    }

    /**
     * 功能：保存或创建部件类型。
     * 参数：
     * - `Details`：`Details` 参数。
     * - `widgetTypeDetails`：类型。
     * - `updateExistingByFqn`：`updateExistingByFqn` 参数。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Create Or Update Widget Type (saveWidgetType)",
            notes = "Create or update the Widget Type. " + WIDGET_TYPE_DESCRIPTION + " " +
                    "When creating the Widget Type, platform generates Widget Type Id as " + UUID_WIKI_LINK +
                    "The newly created Widget Type Id will be present in the response. " +
                    "Specify existing Widget Type id to update the Widget Type. " +
                    "Referencing non-existing Widget Type Id will cause 'Not Found' error." +
                    "\n\nWidget Type fqn is unique in the scope of System or Tenant. " +
                    "Special Tenant Id '13814000-1dd2-11b2-8080-808080808080' is automatically used if the create request is sent by user with 'SYS_ADMIN' authority." +
                    "Remove 'id', 'tenantId' rom the request body example (below) to create new Widget Type entity." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType", method = RequestMethod.POST)
    @ResponseBody
    public WidgetTypeDetails saveWidgetType(
            @ApiParam(value = "A JSON value representing the Widget Type Details.", required = true)
            @RequestBody WidgetTypeDetails widgetTypeDetails,
            @ApiParam(value = UPDATE_EXISTING_BY_FQN_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean updateExistingByFqn) throws Exception {
        var currentUser = getCurrentUser();
        if (Authority.SYS_ADMIN.equals(currentUser.getAuthority())) {
            widgetTypeDetails.setTenantId(TenantId.SYS_TENANT_ID);
        } else {
            widgetTypeDetails.setTenantId(currentUser.getTenantId());
        }

        checkEntity(widgetTypeDetails.getId(), widgetTypeDetails, Resource.WIDGET_TYPE);
        return tbWidgetTypeService.save(widgetTypeDetails, updateExistingByFqn != null && updateExistingByFqn, currentUser);
    }

    /**
     * 功能：删除或清理部件类型。
     * 参数：
     * - `WIDGET_TYPE_ID_PARAM_DESCRIPTION`：类型。
     * - `strWidgetTypeId`：部件类型ID。
     * 返回：无。
     */
    @ApiOperation(value = "Delete widget type (deleteWidgetType)",
            notes = "Deletes the  Widget Type. Referencing non-existing Widget Type Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteWidgetType(
            @ApiParam(value = WIDGET_TYPE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("widgetTypeId") String strWidgetTypeId) throws Exception {
        checkParameter("widgetTypeId", strWidgetTypeId);
        WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
        WidgetTypeDetails wtd = checkWidgetTypeId(widgetTypeId, Operation.DELETE);
        tbWidgetTypeService.delete(wtd, getCurrentUser());
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `PAGE_SIZE_DESCRIPTION`：`PAGE_SIZE_DESCRIPTION` 参数。
     * - `pageSize`：`pageSize` 参数。
     * - `PAGE_NUMBER_DESCRIPTION`：`PAGE_NUMBER_DESCRIPTION` 参数。
     * - `page`：`page` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get Widget Types (getWidgetTypes)",
            notes = "Returns a page of Widget Type objects available for current user. " + WIDGET_TYPE_DESCRIPTION + " " +
                    PAGE_DATA_PARAMETERS + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetTypes", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<WidgetTypeInfo> getWidgetTypes(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = WIDGET_TYPE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = WIDGET_TYPE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = TENANT_ONLY_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean tenantOnly,
            @ApiParam(value = FULL_SEARCH_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean fullSearch,
            @ApiParam(value = DEPRECATED_FILTER_PARAM_DESCRIPTION, allowableValues = DEPRECATED_FILTER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String deprecatedFilter,
            @ApiParam(value = WIDGET_TYPE_ARRAY_DESCRIPTION, allowableValues = WIDGET_TYPE_ALLOWABLE_VALUES)
            @RequestParam(required = false) String[] widgetTypeList) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        List<String> widgetTypes = widgetTypeList != null ? Arrays.asList(widgetTypeList) : Collections.emptyList();
        boolean fullSearchBool = fullSearch != null && fullSearch;
        DeprecatedFilter widgetTypeDeprecatedFilter = StringUtils.isNotEmpty(deprecatedFilter) ? DeprecatedFilter.valueOf(deprecatedFilter) : DeprecatedFilter.ALL;
        if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
            return checkNotNull(widgetTypeService.findSystemWidgetTypesByPageLink(getTenantId(), fullSearchBool, widgetTypeDeprecatedFilter, widgetTypes, pageLink));
        } else {
            if (tenantOnly != null && tenantOnly) {
                return checkNotNull(widgetTypeService.findTenantWidgetTypesByTenantIdAndPageLink(getTenantId(), fullSearchBool, widgetTypeDeprecatedFilter, widgetTypes, pageLink));
            } else {
                return checkNotNull(widgetTypeService.findAllTenantWidgetTypesByTenantIdAndPageLink(getTenantId(), fullSearchBool, widgetTypeDeprecatedFilter, widgetTypes, pageLink));
            }
        }
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `Tenant`：租户信息或租户标识。
     * - `isSystem`：`isSystem` 参数。
     * - `alias`：`alias` 参数。
     * - `bundleAlias`：`bundleAlias` 参数。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get all Widget types for specified Bundle (getBundleWidgetTypesByBundleAlias) (Deprecated)",
            notes = "Returns an array of Widget Type objects that belong to specified Widget Bundle." + WIDGET_TYPE_DESCRIPTION + " " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypes", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    @Deprecated
    public List<WidgetType> getBundleWidgetTypesByBundleAlias(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias) throws ThingsboardException {
        TenantId tenantId;
        if (isSystem) {
            tenantId = TenantId.SYS_TENANT_ID;
        } else {
            tenantId = getCurrentUser().getTenantId();
        }
        WidgetsBundle widgetsBundle = checkNotNull(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(tenantId, bundleAlias));
        return checkNotNull(widgetTypeService.findWidgetTypesByWidgetsBundleId(getTenantId(), widgetsBundle.getId()));
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `Id`：`Id`ID。
     * - `strWidgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get all Widget types for specified Bundle (getBundleWidgetTypes)",
            notes = "Returns an array of Widget Type objects that belong to specified Widget Bundle." + WIDGET_TYPE_DESCRIPTION + " " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetTypes", params = {"widgetsBundleId"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetType> getBundleWidgetTypes(
            @ApiParam(value = "Widget Bundle Id", required = true)
            @RequestParam("widgetsBundleId") String strWidgetsBundleId) throws ThingsboardException {
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(toUUID(strWidgetsBundleId));
        return checkNotNull(widgetTypeService.findWidgetTypesByWidgetsBundleId(getTenantId(), widgetsBundleId));
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `Tenant`：租户信息或租户标识。
     * - `isSystem`：`isSystem` 参数。
     * - `alias`：`alias` 参数。
     * - `bundleAlias`：`bundleAlias` 参数。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get all Widget types details for specified Bundle (getBundleWidgetTypesDetailsByBundleAlias) (Deprecated)",
            notes = "Returns an array of Widget Type Details objects that belong to specified Widget Bundle." + WIDGET_TYPE_DETAILS_DESCRIPTION + " " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypesDetails", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    @Deprecated
    public List<WidgetTypeDetails> getBundleWidgetTypesDetailsByBundleAlias(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias) throws ThingsboardException {
        TenantId tenantId;
        if (isSystem) {
            tenantId = TenantId.SYS_TENANT_ID;
        } else {
            tenantId = getCurrentUser().getTenantId();
        }
        WidgetsBundle widgetsBundle = checkNotNull(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(tenantId, bundleAlias));
        return checkNotNull(widgetTypeService.findWidgetTypesDetailsByWidgetsBundleId(getTenantId(), widgetsBundle.getId()));
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `Id`：`Id`ID。
     * - `strWidgetsBundleId`：部件包ID。
     * - `INLINE_IMAGES`：`INLINE_IMAGES` 参数。
     * - `inlineImages`：`inlineImages` 参数。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get all Widget types details for specified Bundle (getBundleWidgetTypesDetails)",
            notes = "Returns an array of Widget Type Details objects that belong to specified Widget Bundle." + WIDGET_TYPE_DETAILS_DESCRIPTION + " " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetTypesDetails", params = {"widgetsBundleId"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetTypeDetails> getBundleWidgetTypesDetails(
            @ApiParam(value = "Widget Bundle Id", required = true)
            @RequestParam("widgetsBundleId") String strWidgetsBundleId,
            @ApiParam(value = INLINE_IMAGES_DESCRIPTION)
            @RequestParam(value = INLINE_IMAGES, required = false) boolean inlineImages
    ) throws ThingsboardException {
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(toUUID(strWidgetsBundleId));
        var result = checkNotNull(widgetTypeService.findWidgetTypesDetailsByWidgetsBundleId(getTenantId(), widgetsBundleId));
        if (inlineImages) {
            result.forEach(imageService::inlineImages);
        }
        return result;
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `Id`：`Id`ID。
     * - `strWidgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get all Widget type fqns for specified Bundle (getBundleWidgetTypeFqns)",
            notes = "Returns an array of Widget Type fqns that belong to specified Widget Bundle." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypeFqns", params = {"widgetsBundleId"}, method = RequestMethod.GET)
    @ResponseBody
    public List<String> getBundleWidgetTypeFqns(
            @ApiParam(value = "Widget Bundle Id", required = true)
            @RequestParam("widgetsBundleId") String strWidgetsBundleId) throws ThingsboardException {
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(toUUID(strWidgetsBundleId));
        return checkNotNull(widgetTypeService.findWidgetFqnsByWidgetsBundleId(getTenantId(), widgetsBundleId));
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `Tenant`：租户信息或租户标识。
     * - `isSystem`：`isSystem` 参数。
     * - `alias`：`alias` 参数。
     * - `bundleAlias`：`bundleAlias` 参数。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get Widget Type Info objects (getBundleWidgetTypesInfosByBundleAlias) (Deprecated)",
            notes = "Get the Widget Type Info objects based on the provided parameters. " + WIDGET_TYPE_INFO_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetTypesInfos", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    @Deprecated
    public List<WidgetTypeInfo> getBundleWidgetTypesInfosByBundleAlias(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias) throws ThingsboardException {
        TenantId tenantId;
        if (isSystem) {
            tenantId = TenantId.SYS_TENANT_ID;
        } else {
            tenantId = getCurrentUser().getTenantId();
        }
        WidgetsBundle widgetsBundle = checkNotNull(widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(tenantId, bundleAlias));
        return checkNotNull(widgetTypeService.findWidgetTypesInfosByWidgetsBundleId(getTenantId(), widgetsBundle.getId(), false, DeprecatedFilter.ALL,
                null, new PageLink(1024))).getData();
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `Id`：`Id`ID。
     * - `strWidgetsBundleId`：部件包ID。
     * - `PAGE_SIZE_DESCRIPTION`：`PAGE_SIZE_DESCRIPTION` 参数。
     * - `pageSize`：`pageSize` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get Widget Type Info objects (getBundleWidgetTypesInfos)",
            notes = "Get the Widget Type Info objects based on the provided parameters. " + WIDGET_TYPE_INFO_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetTypesInfos", params = {"widgetsBundleId", "pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<WidgetTypeInfo> getBundleWidgetTypesInfos(
            @ApiParam(value = "Widget Bundle Id", required = true)
            @RequestParam("widgetsBundleId") String strWidgetsBundleId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = WIDGET_TYPE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = WIDGET_TYPE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = FULL_SEARCH_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean fullSearch,
            @ApiParam(value = DEPRECATED_FILTER_PARAM_DESCRIPTION, allowableValues = DEPRECATED_FILTER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String deprecatedFilter,
            @ApiParam(value = WIDGET_TYPE_ARRAY_DESCRIPTION, allowableValues = WIDGET_TYPE_ALLOWABLE_VALUES)
            @RequestParam(required = false) String[] widgetTypeList) throws ThingsboardException {
        WidgetsBundleId widgetsBundleId = new WidgetsBundleId(toUUID(strWidgetsBundleId));
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        List<String> widgetTypes = widgetTypeList != null ? Arrays.asList(widgetTypeList) : Collections.emptyList();
        DeprecatedFilter widgetTypeDeprecatedFilter = StringUtils.isNotEmpty(deprecatedFilter) ? DeprecatedFilter.valueOf(deprecatedFilter) : DeprecatedFilter.ALL;
        return checkNotNull(widgetTypeService.findWidgetTypesInfosByWidgetsBundleId(getTenantId(), widgetsBundleId, fullSearch != null && fullSearch,
                widgetTypeDeprecatedFilter, widgetTypes, pageLink));
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `Tenant`：租户信息或租户标识。
     * - `isSystem`：`isSystem` 参数。
     * - `alias`：`alias` 参数。
     * - `bundleAlias`：`bundleAlias` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Widget Type (getWidgetTypeByBundleAliasAndTypeAlias) (Deprecated)",
            notes = "Get the Widget Type based on the provided parameters. " + WIDGET_TYPE_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetType", params = {"isSystem", "bundleAlias", "alias"}, method = RequestMethod.GET)
    @ResponseBody
    @Deprecated
    public WidgetType getWidgetTypeByBundleAliasAndTypeAlias(
            @ApiParam(value = "System or Tenant", required = true)
            @RequestParam boolean isSystem,
            @ApiParam(value = "Widget Bundle alias", required = true)
            @RequestParam String bundleAlias,
            @ApiParam(value = "Widget Type alias", required = true)
            @RequestParam String alias) throws ThingsboardException {
        TenantId tenantId;
        if (isSystem) {
            tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);
        } else {
            tenantId = getCurrentUser().getTenantId();
        }
        WidgetType widgetType = widgetTypeService.findWidgetTypeByTenantIdAndFqn(tenantId, bundleAlias + "." + alias);
        checkNotNull(widgetType);
        accessControlService.checkPermission(getCurrentUser(), Resource.WIDGET_TYPE, Operation.READ, widgetType.getId(), widgetType);
        return widgetType;
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `fqn`：`fqn` 参数。
     * - `fqn`：`fqn` 参数。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Widget Type (getWidgetType)",
            notes = "Get the Widget Type by FQN. " + WIDGET_TYPE_DESCRIPTION + AVAILABLE_FOR_ANY_AUTHORIZED_USER, hidden = true)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetType", params = {"fqn"}, method = RequestMethod.GET)
    @ResponseBody
    public WidgetType getWidgetType(
            @ApiParam(value = "Widget Type fqn", required = true)
            @RequestParam String fqn) throws ThingsboardException {
        String[] parts = fqn.split("\\.");
        String scopeQualifier = parts.length > 0 ? parts[0] : null;
        if (parts.length < 2 || (!scopeQualifier.equals("system") && !scopeQualifier.equals("tenant"))) {
            throw new ThingsboardException("Invalid fqn!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        TenantId tenantId;
        if ("system".equals(scopeQualifier)) {
            tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);
        } else {
            tenantId = getCurrentUser().getTenantId();
        }
        String typeFqn = fqn.substring(scopeQualifier.length() + 1);
        WidgetType widgetType = widgetTypeService.findWidgetTypeByTenantIdAndFqn(tenantId, typeFqn);
        checkNotNull(widgetType);

        accessControlService.checkPermission(getCurrentUser(), Resource.WIDGET_TYPE, Operation.READ, widgetType.getId(), widgetType);
        return widgetType;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`WidgetTypeController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
