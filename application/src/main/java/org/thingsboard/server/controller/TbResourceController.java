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
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.resource.TbResourceService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.thingsboard.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.thingsboard.server.controller.ControllerConstants.LWM2M_OBJECT_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.LWM2M_OBJECT_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_TYPE_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

/**
 * 中文说明：
 * 1. 类目的：`TbResourceController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class TbResourceController extends BaseController {

    /**
     * `DOWNLOAD_RESOURCE_IF_NOT_CHANGED`常量，用于统一引用固定值。
     */
    private static final String DOWNLOAD_RESOURCE_IF_NOT_CHANGED = "Download Resource based on the provided Resource Id or return 304 status code if resource was not changed.";
    private final TbResourceService tbResourceService;

    /**
     * `RESOURCE_ID`常量，用于统一引用固定值。
     */
    public static final String RESOURCE_ID = "resourceId";

    /**
     * 功能：执行 `downloadResource` 对应的处理。
     * 参数：
     * - `strResourceId`：`strResourceId`ID。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Download Resource (downloadResource)", notes = "Download Resource based on the provided Resource Id." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/{resourceId}/download")
    public ResponseEntity<ByteArrayResource> downloadResource(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                                              @PathVariable(RESOURCE_ID) String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        TbResource tbResource = checkResourceId(resourceId, Operation.READ);

        ByteArrayResource resource = new ByteArrayResource(tbResource.getData());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + tbResource.getFileName())
                .header("x-filename", tbResource.getFileName())
                .contentLength(resource.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * 功能：执行 `downloadLwm2mResourceIfChanged` 对应的处理。
     * 参数：
     * - `strResourceId`：`strResourceId`ID。
     * - `HttpHeadersIF_NONE_MATCH`：`HttpHeadersIF_NONE_MATCH` 参数。
     * - `etag`：`etag` 参数。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Download LWM2M Resource (downloadLwm2mResourceIfChanged)", notes = DOWNLOAD_RESOURCE_IF_NOT_CHANGED + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/lwm2m/{resourceId}/download", produces = "application/xml")
    public ResponseEntity<ByteArrayResource> downloadLwm2mResourceIfChanged(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                            @PathVariable(RESOURCE_ID) String strResourceId,
                                                                            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws ThingsboardException {
        return downloadResourceIfChanged(ResourceType.LWM2M_MODEL, strResourceId, etag);
    }

    /**
     * 功能：执行 `downloadPkcs12ResourceIfChanged` 对应的处理。
     * 参数：
     * - `strResourceId`：`strResourceId`ID。
     * - `HttpHeadersIF_NONE_MATCH`：`HttpHeadersIF_NONE_MATCH` 参数。
     * - `etag`：`etag` 参数。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Download PKCS_12 Resource (downloadPkcs12ResourceIfChanged)", notes = DOWNLOAD_RESOURCE_IF_NOT_CHANGED + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/pkcs12/{resourceId}/download", method = RequestMethod.GET, produces = "application/x-pkcs12")
    public ResponseEntity<ByteArrayResource> downloadPkcs12ResourceIfChanged(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                             @PathVariable(RESOURCE_ID) String strResourceId,
                                                                             @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws ThingsboardException {
        return downloadResourceIfChanged(ResourceType.PKCS_12, strResourceId, etag);
    }

    /**
     * 功能：执行 `downloadJksResourceIfChanged` 对应的处理。
     * 参数：
     * - `strResourceId`：`strResourceId`ID。
     * - `HttpHeadersIF_NONE_MATCH`：`HttpHeadersIF_NONE_MATCH` 参数。
     * - `etag`：`etag` 参数。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Download JKS Resource (downloadJksResourceIfChanged)",
            notes = DOWNLOAD_RESOURCE_IF_NOT_CHANGED + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/jks/{resourceId}/download", produces = "application/x-java-keystore")
    public ResponseEntity<ByteArrayResource> downloadJksResourceIfChanged(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                          @PathVariable(RESOURCE_ID) String strResourceId,
                                                                          @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws ThingsboardException {
        return downloadResourceIfChanged(ResourceType.JKS, strResourceId, etag);
    }

    /**
     * 功能：执行 `downloadJsResourceIfChanged` 对应的处理。
     * 参数：
     * - `strResourceId`：`strResourceId`ID。
     * - `HttpHeadersIF_NONE_MATCH`：`HttpHeadersIF_NONE_MATCH` 参数。
     * - `etag`：`etag` 参数。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Download JS Resource (downloadJsResourceIfChanged)", notes = DOWNLOAD_RESOURCE_IF_NOT_CHANGED + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/resource/js/{resourceId}/download", produces = "application/javascript")
    public ResponseEntity<ByteArrayResource> downloadJsResourceIfChanged(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                         @PathVariable(RESOURCE_ID) String strResourceId,
                                                                         @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws ThingsboardException {
        return downloadResourceIfChanged(ResourceType.JS_MODULE, strResourceId, etag);
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `strResourceId`：`strResourceId`ID。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Resource Info (getResourceInfoById)",
            notes = "Fetch the Resource Info object based on the provided Resource Id. " +
                    RESOURCE_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/info/{resourceId}")
    public TbResourceInfo getResourceInfoById(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                              @PathVariable(RESOURCE_ID) String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        return checkResourceInfoId(resourceId, Operation.READ);
    }

    /**
     * 功能：获取`Resource By Id`。
     * 参数：
     * - `strResourceId`：`strResourceId`ID。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get Resource (getResourceById)",
            notes = "Fetch the Resource object based on the provided Resource Id. " +
                    RESOURCE_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json", hidden = true)
    @Deprecated  // resource's data should be fetched with a download request
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource/{resourceId}")
    public TbResource getResourceById(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                      @PathVariable(RESOURCE_ID) String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        return checkResourceId(resourceId, Operation.READ);
    }

    /**
     * 功能：保存或创建`Resource`。
     * 参数：
     * - `resource`：`resource` 参数。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Create Or Update Resource (saveResource)",
            notes = "Create or update the Resource. When creating the Resource, platform generates Resource id as " + UUID_WIKI_LINK +
                    "The newly created Resource id will be present in the response. " +
                    "Specify existing Resource id to update the Resource. " +
                    "Referencing non-existing Resource Id will cause 'Not Found' error. " +
                    "\n\nResource combination of the title with the key is unique in the scope of tenant. " +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Resource entity." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json",
            consumes = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/resource")
    public TbResourceInfo saveResource(@ApiParam(value = "A JSON value representing the Resource.")
                                       @RequestBody TbResource resource) throws Exception {
        resource.setTenantId(getTenantId());
        checkEntity(resource.getId(), resource, Resource.TB_RESOURCE);
        return new TbResourceInfo(tbResourceService.save(resource, getCurrentUser()));
    }

    /**
     * 功能：获取`Resources`。
     * 参数：
     * - `PAGE_SIZE_DESCRIPTION`：`PAGE_SIZE_DESCRIPTION` 参数。
     * - `pageSize`：`pageSize` 参数。
     * - `PAGE_NUMBER_DESCRIPTION`：`PAGE_NUMBER_DESCRIPTION` 参数。
     * - `page`：`page` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get Resource Infos (getResources)",
            notes = "Returns a page of Resource Info objects owned by tenant or sysadmin. " +
                    PAGE_DATA_PARAMETERS + RESOURCE_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = "/resource")
    public PageData<TbResourceInfo> getResources(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                 @RequestParam int pageSize,
                                                 @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                 @RequestParam int page,
                                                 @ApiParam(value = RESOURCE_TYPE, allowableValues = RESOURCE_TYPE_PROPERTY_ALLOWABLE_VALUES)
                                                 @RequestParam(required = false) String resourceType,
                                                 @ApiParam(value = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                                 @RequestParam(required = false) String textSearch,
                                                 @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = RESOURCE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                 @RequestParam(required = false) String sortProperty,
                                                 @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                 @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TbResourceInfoFilter.TbResourceInfoFilterBuilder filter = TbResourceInfoFilter.builder();
        filter.tenantId(getTenantId());
        Set<ResourceType> resourceTypes = new HashSet<>();
        if (StringUtils.isNotEmpty(resourceType)) {
            resourceTypes.add(ResourceType.valueOf(resourceType));
        } else {
            Collections.addAll(resourceTypes, ResourceType.values());
            resourceTypes.remove(ResourceType.IMAGE);
        }
        filter.resourceTypes(resourceTypes);
        if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
            return checkNotNull(resourceService.findTenantResourcesByTenantId(filter.build(), pageLink));
        } else {
            return checkNotNull(resourceService.findAllTenantResourcesByTenantId(filter.build(), pageLink));
        }
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
    @ApiOperation(value = "Get All Resource Infos (getAllResources)",
            notes = "Returns a page of Resource Info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + RESOURCE_INFO_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/resource/tenant")
    public PageData<TbResourceInfo> getTenantResources(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                       @RequestParam int pageSize,
                                                       @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                       @RequestParam int page,
                                                       @ApiParam(value = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                                       @RequestParam(required = false) String textSearch,
                                                       @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = RESOURCE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                       @RequestParam(required = false) String sortProperty,
                                                       @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                       @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(getTenantId())
                .resourceTypes(EnumSet.allOf(ResourceType.class))
                .build();
        return checkNotNull(resourceService.findTenantResourcesByTenantId(filter, pageLink));
    }

    /**
     * 功能：获取`Lwm2m List Objects Page`。
     * 参数：
     * - `PAGE_SIZE_DESCRIPTION`：`PAGE_SIZE_DESCRIPTION` 参数。
     * - `pageSize`：`pageSize` 参数。
     * - `PAGE_NUMBER_DESCRIPTION`：`PAGE_NUMBER_DESCRIPTION` 参数。
     * - `page`：`page` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get LwM2M Objects (getLwm2mListObjectsPage)",
            notes = "Returns a page of LwM2M objects parsed from Resources with type 'LWM2M_MODEL' owned by tenant or sysadmin. " +
                    PAGE_DATA_PARAMETERS + LWM2M_OBJECT_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/resource/lwm2m/page")
    public List<LwM2mObject> getLwm2mListObjectsPage(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                     @RequestParam int pageSize,
                                                     @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                     @RequestParam int page,
                                                     @ApiParam(value = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                                     @RequestParam(required = false) String textSearch,
                                                     @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = LWM2M_OBJECT_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                     @RequestParam(required = false) String sortProperty,
                                                     @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                     @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = new PageLink(pageSize, page, textSearch);
        return checkNotNull(tbResourceService.findLwM2mObjectPage(getTenantId(), sortProperty, sortOrder, pageLink));
    }

    /**
     * 功能：获取`Lwm2m List Objects`。
     * 参数：
     * - `SORT_ORDER_DESCRIPTION`：`SORT_ORDER_DESCRIPTION` 参数。
     * - `SORT_ORDER_ALLOWABLE_VALUES`：值。
     * - `sortOrder`：`sortOrder` 参数。
     * - `SORT_PROPERTY_DESCRIPTION`：`SORT_PROPERTY_DESCRIPTION` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get LwM2M Objects (getLwm2mListObjects)",
            notes = "Returns a page of LwM2M objects parsed from Resources with type 'LWM2M_MODEL' owned by tenant or sysadmin. " +
                    "You can specify parameters to filter the results. " + LWM2M_OBJECT_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/resource/lwm2m")
    public List<LwM2mObject> getLwm2mListObjects(@ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES, required = true)
                                                 @RequestParam String sortOrder,
                                                 @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = LWM2M_OBJECT_SORT_PROPERTY_ALLOWABLE_VALUES, required = true)
                                                 @RequestParam String sortProperty,
                                                 @ApiParam(value = "LwM2M Object ids.", required = true)
                                                 @RequestParam(required = false) String[] objectIds) throws ThingsboardException {
        return checkNotNull(tbResourceService.findLwM2mObject(getTenantId(), sortOrder, sortProperty, objectIds));
    }

    /**
     * 功能：删除或清理`Resource`。
     * 参数：
     * - `strResourceId`：`strResourceId`ID。
     * 返回：无。
     */
    @ApiOperation(value = "Delete Resource (deleteResource)",
            notes = "Deletes the Resource. Referencing non-existing Resource Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(value = "/resource/{resourceId}")
    public void deleteResource(@ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                               @PathVariable("resourceId") String strResourceId) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        TbResource tbResource = checkResourceId(resourceId, Operation.DELETE);
        tbResourceService.delete(tbResource, getCurrentUser());
    }

    /**
     * 功能：执行 `downloadResourceIfChanged` 对应的处理。
     * 参数：
     * - `resourceType`：类型。
     * - `strResourceId`：`strResourceId`ID。
     * - `etag`：`etag` 参数。
     * 返回：响应结果。
     */
    private ResponseEntity<ByteArrayResource> downloadResourceIfChanged(ResourceType resourceType, String strResourceId, String etag) throws ThingsboardException {
        checkParameter(RESOURCE_ID, strResourceId);
        TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        if (etag != null) {
            TbResourceInfo tbResourceInfo = checkResourceInfoId(resourceId, Operation.READ);
            etag = StringUtils.remove(etag, '\"'); // etag is wrapped in double quotes due to HTTP specification
            if (etag.equals(tbResourceInfo.getEtag())) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(tbResourceInfo.getEtag())
                        .build();
            }
        }

        TbResource tbResource = checkResourceId(resourceId, Operation.READ);

        ByteArrayResource resource = new ByteArrayResource(tbResource.getData());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + tbResource.getFileName())
                .header("x-filename", tbResource.getFileName())
                .contentLength(resource.contentLength())
                .header("Content-Type", resourceType.getMediaType())
                .cacheControl(CacheControl.noCache())
                .eTag(tbResource.getEtag())
                .body(resource);
    }


/*
 * 本类总结：
 * 1. 核心职责：`TbResourceController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}