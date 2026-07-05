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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.ota.TbOtaPackageService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.OTA_PACKAGE_CHECKSUM_ALGORITHM_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.OTA_PACKAGE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.OTA_PACKAGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.OTA_PACKAGE_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.OTA_PACKAGE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.OTA_PACKAGE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

/**
 * 中文说明：
 * 1. 类目的：`OtaPackageController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
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
public class OtaPackageController extends BaseController {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbOtaPackageService tbOtaPackageService;

    /**
     * `OTA_PACKAGE_ID`常量，用于统一引用固定值。
     */
    public static final String OTA_PACKAGE_ID = "otaPackageId";
    public static final String CHECKSUM_ALGORITHM = "checksumAlgorithm";

    /**
     * 功能：执行 `downloadOtaPackage` 对应的处理。
     * 参数：
     * - `strOtaPackageId`：`strOtaPackageId`ID。
     * 返回：响应结果。
     */
    @ApiOperation(value = "Download OTA Package (downloadOtaPackage)", notes = "Download OTA Package based on the provided OTA Package Id." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority( 'TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> downloadOtaPackage(@ApiParam(value = OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                                                                   @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
        OtaPackage otaPackage = checkOtaPackageId(otaPackageId, Operation.READ);

        if (otaPackage.hasUrl()) {
            return ResponseEntity.badRequest().build();
        }

        ByteArrayResource resource = new ByteArrayResource(otaPackage.getData().array());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + otaPackage.getFileName())
                .header("x-filename", otaPackage.getFileName())
                .contentLength(resource.contentLength())
                .contentType(parseMediaType(otaPackage.getContentType()))
                .body(resource);
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `strOtaPackageId`：`strOtaPackageId`ID。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get OTA Package Info (getOtaPackageInfoById)",
            notes = "Fetch the OTA Package Info object based on the provided OTA Package Id. " +
                    OTA_PACKAGE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackage/info/{otaPackageId}", method = RequestMethod.GET)
    @ResponseBody
    public OtaPackageInfo getOtaPackageInfoById(@ApiParam(value = OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                                @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
        return checkNotNull(otaPackageService.findOtaPackageInfoById(getTenantId(), otaPackageId));
    }

    /**
     * 功能：获取`Ota Package By Id`。
     * 参数：
     * - `strOtaPackageId`：`strOtaPackageId`ID。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get OTA Package (getOtaPackageById)",
            notes = "Fetch the OTA Package object based on the provided OTA Package Id. " +
                    "The server checks that the OTA Package is owned by the same tenant. " + OTA_PACKAGE_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.GET)
    @ResponseBody
    public OtaPackage getOtaPackageById(@ApiParam(value = OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                        @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
        return checkOtaPackageId(otaPackageId, Operation.READ);
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Create Or Update OTA Package Info (saveOtaPackageInfo)",
            notes = "Create or update the OTA Package Info. When creating OTA Package Info, platform generates OTA Package id as " + UUID_WIKI_LINK +
                    "The newly created OTA Package id will be present in the response. " +
                    "Specify existing OTA Package id to update the OTA Package Info. " +
                    "Referencing non-existing OTA Package Id will cause 'Not Found' error. " +
                    "\n\nOTA Package combination of the title with the version is unique in the scope of tenant. " + TENANT_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage", method = RequestMethod.POST)
    @ResponseBody
    public OtaPackageInfo saveOtaPackageInfo(@ApiParam(value = "A JSON value representing the OTA Package.")
                                             @RequestBody SaveOtaPackageInfoRequest otaPackageInfo) throws ThingsboardException {
        otaPackageInfo.setTenantId(getTenantId());
        checkEntity(otaPackageInfo.getId(), otaPackageInfo, Resource.OTA_PACKAGE);

        return tbOtaPackageService.save(otaPackageInfo, getCurrentUser());
    }

    /**
     * 功能：保存或创建数据。
     * 参数：
     * - `strOtaPackageId`：`strOtaPackageId`ID。
     * - `example`：`example` 参数。
     * - `checksum`：`checksum` 参数。
     * - `algorithm`：`algorithm` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Save OTA Package data (saveOtaPackageData)",
            notes = "Update the OTA Package. Adds the date to the existing OTA Package Info" + TENANT_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE,
            consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.POST, consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public OtaPackageInfo saveOtaPackageData(@ApiParam(value = OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                             @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId,
                                             @ApiParam(value = "OTA Package checksum. For example, '0xd87f7e0c'")
                                             @RequestParam(required = false) String checksum,
                                             @ApiParam(value = "OTA Package checksum algorithm.", allowableValues = OTA_PACKAGE_CHECKSUM_ALGORITHM_ALLOWABLE_VALUES)
                                             @RequestParam(CHECKSUM_ALGORITHM) String checksumAlgorithmStr,
                                             @ApiParam(value = "OTA Package data.")
                                             @RequestPart MultipartFile file) throws ThingsboardException, IOException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        checkParameter(CHECKSUM_ALGORITHM, checksumAlgorithmStr);
        OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
        OtaPackageInfo otaPackageInfo = checkOtaPackageInfoId(otaPackageId, Operation.READ);
        ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.valueOf(checksumAlgorithmStr.toUpperCase());
        byte[] data = file.getBytes();
        return tbOtaPackageService.saveOtaPackageData(otaPackageInfo, checksum, checksumAlgorithm,
                data, file.getOriginalFilename(), file.getContentType(), getCurrentUser());
    }

    /**
     * 功能：获取`Ota Packages`。
     * 参数：
     * - `PAGE_SIZE_DESCRIPTION`：`PAGE_SIZE_DESCRIPTION` 参数。
     * - `pageSize`：`pageSize` 参数。
     * - `PAGE_NUMBER_DESCRIPTION`：`PAGE_NUMBER_DESCRIPTION` 参数。
     * - `page`：`page` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get OTA Package Infos (getOtaPackages)",
            notes = "Returns a page of OTA Package Info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + OTA_PACKAGE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getOtaPackages(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                   @RequestParam int pageSize,
                                                   @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                   @RequestParam int page,
                                                   @ApiParam(value = OTA_PACKAGE_TEXT_SEARCH_DESCRIPTION)
                                                   @RequestParam(required = false) String textSearch,
                                                   @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = OTA_PACKAGE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortProperty,
                                                   @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(otaPackageService.findTenantOtaPackagesByTenantId(getTenantId(), pageLink));
    }

    /**
     * 功能：获取`Ota Packages`。
     * 参数：
     * - `strDeviceProfileId`：设备配置ID。
     * - `type`：类型。
     * - `FIRMWARE`：`FIRMWARE` 参数。
     * - `strType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get OTA Package Infos (getOtaPackages)",
            notes = "Returns a page of OTA Package Info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + OTA_PACKAGE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages/{deviceProfileId}/{type}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getOtaPackages(@ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
                                                   @PathVariable("deviceProfileId") String strDeviceProfileId,
                                                   @ApiParam(value = "OTA Package type.", allowableValues = "FIRMWARE, SOFTWARE")
                                                   @PathVariable("type") String strType,
                                                   @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                   @RequestParam int pageSize,
                                                   @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                   @RequestParam int page,
                                                   @ApiParam(value = OTA_PACKAGE_TEXT_SEARCH_DESCRIPTION)
                                                   @RequestParam(required = false) String textSearch,
                                                   @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = OTA_PACKAGE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortProperty,
                                                   @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("deviceProfileId", strDeviceProfileId);
        checkParameter("type", strType);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(otaPackageService.findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(getTenantId(),
                new DeviceProfileId(toUUID(strDeviceProfileId)), OtaPackageType.valueOf(strType), pageLink));
    }

    /**
     * 功能：删除或清理`Ota Package`。
     * 参数：
     * - `strOtaPackageId`：`strOtaPackageId`ID。
     * 返回：无。
     */
    @ApiOperation(value = "Delete OTA Package (deleteOtaPackage)",
            notes = "Deletes the OTA Package. Referencing non-existing OTA Package Id will cause an error. " +
                    "Can't delete the OTA Package if it is referenced by existing devices or device profile." + TENANT_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteOtaPackage(@ApiParam(value = OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                 @PathVariable("otaPackageId") String strOtaPackageId) throws ThingsboardException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
        OtaPackageInfo otaPackageInfo = checkOtaPackageInfoId(otaPackageId, Operation.DELETE);
        tbOtaPackageService.delete(otaPackageInfo, getCurrentUser());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`OtaPackageController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
