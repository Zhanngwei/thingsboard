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

import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.ImageExportData;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.util.ThrowingSupplier;
import org.thingsboard.server.dao.resource.ImageCacheKey;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.validator.ResourceDataValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.resource.TbImageService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_INCLUDE_SYSTEM_IMAGES_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * 中文说明：
 * 1. `ImageController` 是 ThingsBoard Application 中处理图片资源请求的 API 控制器。
 * 2. 它负责校验请求参数、解析当前用户上下文并调用对应服务完成操作。
 * 3. 方法返回面向客户端的数据对象或统一的异步响应。
 * 4. 直接依赖的类型边界包括 `BaseController`。
 * 5. 单独设置控制器可以把 HTTP 边界与业务实现分开，保持接口行为稳定。
 * 6. 阅读时重点关注路由、权限条件、参数校验以及服务调用结果的转换。
 */
@Slf4j
@RestController
@TbCoreComponent
@RequiredArgsConstructor
public class ImageController extends BaseController {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final ImageService imageService;
    private final TbImageService tbImageService;
    /**
     * 校验器，封装可复用的处理规则。
     */
    private final ResourceDataValidator resourceValidator;

    /**
     * Actor 系统，表示当前对象的对应属性。
     */
    @Value("${cache.image.systemImagesBrowserTtlInMinutes:0}")
    private int systemImagesBrowserTtlInMinutes;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Value("${cache.image.tenantImagesBrowserTtlInMinutes:0}")
    private int tenantImagesBrowserTtlInMinutes;

    /**
     * URL 地址常量，用于统一引用固定值。
     */
    private static final String IMAGE_URL = "/api/images/{type}/{key}";
    private static final String SYSTEM_IMAGE = "system";
    /**
     * 租户常量，用于统一引用固定值。
     */
    private static final String TENANT_IMAGE = "tenant";

    /**
     * 类型常量，用于统一引用固定值。
     */
    private static final String IMAGE_TYPE_PARAM_DESCRIPTION = "Type of the image: tenant or system";
    private static final String IMAGE_TYPE_PARAM_ALLOWABLE_VALUES = "tenant, system";
    /**
     * 键常量，用于统一引用固定值。
     */
    private static final String IMAGE_KEY_PARAM_DESCRIPTION = "Image resource key, for example thermostats_dashboard_background.jpeg";

    /**
     * 功能：执行 `uploadImage` 对应的处理。
     * 参数：
     * - `file`：`file` 参数。
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping("/api/image")
    public TbResourceInfo uploadImage(@RequestPart MultipartFile file,
                                      @RequestPart(required = false) String title) throws Exception {
        SecurityUser user = getCurrentUser();
        TbResource image = new TbResource();
        image.setTenantId(user.getTenantId());
        accessControlService.checkPermission(user, Resource.TB_RESOURCE, Operation.CREATE, null, image);
        resourceValidator.validateResourceSize(user.getTenantId(), null, file.getSize());

        image.setFileName(file.getOriginalFilename());
        if (StringUtils.isNotEmpty(title)) {
            image.setTitle(title);
        } else {
            image.setTitle(file.getOriginalFilename());
        }
        image.setResourceType(ResourceType.IMAGE);
        ImageDescriptor descriptor = new ImageDescriptor();
        descriptor.setMediaType(file.getContentType());
        image.setDescriptorValue(descriptor);
        image.setData(file.getBytes());
        image.setPublic(true);
        return tbImageService.save(image, user);
    }

    /**
     * 功能：更新图片资源。
     * 参数：
     * - `IMAGE_TYPE_PARAM_DESCRIPTION`：类型。
     * - `IMAGE_TYPE_PARAM_ALLOWABLE_VALUES`：类型。
     * - `type`：类型。
     * - `IMAGE_KEY_PARAM_DESCRIPTION`：键。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(IMAGE_URL)
    public TbResourceInfo updateImage(@ApiParam(value = IMAGE_TYPE_PARAM_DESCRIPTION, allowableValues = IMAGE_TYPE_PARAM_ALLOWABLE_VALUES, required = true)
                                      @PathVariable String type,
                                      @ApiParam(value = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                      @PathVariable String key,
                                      @RequestPart MultipartFile file) throws Exception {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        resourceValidator.validateResourceSize(getTenantId(), imageInfo.getId(), file.getSize());

        TbResource image = new TbResource(imageInfo);
        image.setData(file.getBytes());
        image.setFileName(file.getOriginalFilename());
        image.updateDescriptor(ImageDescriptor.class, descriptor -> {
            descriptor.setMediaType(file.getContentType());
            return descriptor;
        });
        return tbImageService.save(image, getCurrentUser());
    }

    /**
     * 功能：更新信息对象。
     * 参数：
     * - `IMAGE_TYPE_PARAM_DESCRIPTION`：类型。
     * - `IMAGE_TYPE_PARAM_ALLOWABLE_VALUES`：类型。
     * - `type`：类型。
     * - `IMAGE_KEY_PARAM_DESCRIPTION`：键。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(IMAGE_URL + "/info")
    public TbResourceInfo updateImageInfo(@ApiParam(value = IMAGE_TYPE_PARAM_DESCRIPTION, allowableValues = IMAGE_TYPE_PARAM_ALLOWABLE_VALUES, required = true)
                                          @PathVariable String type,
                                          @ApiParam(value = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                          @PathVariable String key,
                                          @RequestBody TbResourceInfo request) throws ThingsboardException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        TbResourceInfo newImageInfo = new TbResourceInfo(imageInfo);
        newImageInfo.setTitle(request.getTitle());
        return tbImageService.save(newImageInfo, imageInfo, getCurrentUser());
    }

    /**
     * 功能：更新状态。
     * 参数：
     * - `IMAGE_TYPE_PARAM_DESCRIPTION`：类型。
     * - `IMAGE_TYPE_PARAM_ALLOWABLE_VALUES`：类型。
     * - `type`：类型。
     * - `IMAGE_KEY_PARAM_DESCRIPTION`：键。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(IMAGE_URL + "/public/{isPublic}")
    public TbResourceInfo updateImagePublicStatus(@ApiParam(value = IMAGE_TYPE_PARAM_DESCRIPTION, allowableValues = IMAGE_TYPE_PARAM_ALLOWABLE_VALUES, required = true)
                                                  @PathVariable String type,
                                                  @ApiParam(value = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                  @PathVariable String key,
                                                  @PathVariable boolean isPublic) throws ThingsboardException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        TbResourceInfo newImageInfo = new TbResourceInfo(imageInfo);
        newImageInfo.setPublic(isPublic);
        return tbImageService.save(newImageInfo, imageInfo, getCurrentUser());
    }

    /**
     * 功能：执行 `downloadImage` 对应的处理。
     * 参数：
     * - `IMAGE_TYPE_PARAM_DESCRIPTION`：类型。
     * - `IMAGE_TYPE_PARAM_ALLOWABLE_VALUES`：类型。
     * - `type`：类型。
     * - `IMAGE_KEY_PARAM_DESCRIPTION`：键。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = IMAGE_URL, produces = "image/*")
    public ResponseEntity<ByteArrayResource> downloadImage(@ApiParam(value = IMAGE_TYPE_PARAM_DESCRIPTION, allowableValues = IMAGE_TYPE_PARAM_ALLOWABLE_VALUES, required = true)
                                                           @PathVariable String type,
                                                           @ApiParam(value = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                           @PathVariable String key,
                                                           @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws Exception {
        return downloadIfChanged(type, key, etag, false);
    }

    /**
     * 功能：执行 `downloadPublicImage` 对应的处理。
     * 参数：
     * - `publicResourceKey`：键。
     * - `HttpHeadersIF_NONE_MATCH`：`HttpHeadersIF_NONE_MATCH` 参数。
     * - `etag`：`etag` 参数。
     * 返回：响应结果。
     */
    @GetMapping(value = "/api/images/public/{publicResourceKey}", produces = "image/*")
    public ResponseEntity<ByteArrayResource> downloadPublicImage(@PathVariable String publicResourceKey,
                                                                 @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws Exception {
        ImageCacheKey cacheKey = ImageCacheKey.forPublicImage(publicResourceKey);
        return downloadIfChanged(cacheKey, etag, () -> imageService.getPublicImageInfoByKey(publicResourceKey));
    }

    /**
     * 功能：执行 `exportImage` 对应的处理。
     * 参数：
     * - `IMAGE_TYPE_PARAM_DESCRIPTION`：类型。
     * - `IMAGE_TYPE_PARAM_ALLOWABLE_VALUES`：类型。
     * - `type`：类型。
     * - `IMAGE_KEY_PARAM_DESCRIPTION`：键。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = IMAGE_URL + "/export")
    public ImageExportData exportImage(@ApiParam(value = IMAGE_TYPE_PARAM_DESCRIPTION, allowableValues = IMAGE_TYPE_PARAM_ALLOWABLE_VALUES, required = true)
                                       @PathVariable String type,
                                       @ApiParam(value = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                       @PathVariable String key) throws Exception {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.READ);
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        byte[] data = imageService.getImageData(imageInfo.getTenantId(), imageInfo.getId());
        return ImageExportData.builder()
                .mediaType(descriptor.getMediaType())
                .fileName(imageInfo.getFileName())
                .title(imageInfo.getTitle())
                .resourceKey(imageInfo.getResourceKey())
                .isPublic(imageInfo.isPublic())
                .publicResourceKey(imageInfo.getPublicResourceKey())
                .data(Base64Utils.encodeToString(data))
                .build();
    }

    /**
     * 功能：执行 `importImage` 对应的处理。
     * 参数：
     * - `imageData`：待处理数据。
     * 返回：处理结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping("/api/image/import")
    public TbResourceInfo importImage(@RequestBody ImageExportData imageData) throws Exception {
        SecurityUser user = getCurrentUser();
        TbResource image = new TbResource();
        image.setTenantId(user.getTenantId());
        accessControlService.checkPermission(user, Resource.TB_RESOURCE, Operation.CREATE, null, image);

        image.setFileName(imageData.getFileName());
        if (StringUtils.isNotEmpty(imageData.getTitle())) {
            image.setTitle(imageData.getTitle());
        } else {
            image.setTitle(imageData.getFileName());
        }
        image.setResourceType(ResourceType.IMAGE);
        image.setResourceKey(imageData.getResourceKey());
        image.setPublic(imageData.isPublic());
        image.setPublicResourceKey(imageData.getPublicResourceKey());
        ImageDescriptor descriptor = new ImageDescriptor();
        descriptor.setMediaType(imageData.getMediaType());
        image.setDescriptorValue(descriptor);
        image.setData(Base64Utils.decodeFromString(imageData.getData()));
        return tbImageService.save(image, user);
    }

    /**
     * 功能：执行 `downloadImagePreview` 对应的处理。
     * 参数：
     * - `IMAGE_TYPE_PARAM_DESCRIPTION`：类型。
     * - `IMAGE_TYPE_PARAM_ALLOWABLE_VALUES`：类型。
     * - `type`：类型。
     * - `IMAGE_KEY_PARAM_DESCRIPTION`：键。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = IMAGE_URL + "/preview", produces = "image/png")
    public ResponseEntity<ByteArrayResource> downloadImagePreview(@ApiParam(value = IMAGE_TYPE_PARAM_DESCRIPTION, allowableValues = IMAGE_TYPE_PARAM_ALLOWABLE_VALUES, required = true)
                                                                  @PathVariable String type,
                                                                  @ApiParam(value = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                                  @PathVariable String key,
                                                                  @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws Exception {
        return downloadIfChanged(type, key, etag, true);
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `IMAGE_TYPE_PARAM_DESCRIPTION`：类型。
     * - `IMAGE_TYPE_PARAM_ALLOWABLE_VALUES`：类型。
     * - `type`：类型。
     * - `IMAGE_KEY_PARAM_DESCRIPTION`：键。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(IMAGE_URL + "/info")
    public TbResourceInfo getImageInfo(@ApiParam(value = IMAGE_TYPE_PARAM_DESCRIPTION, allowableValues = IMAGE_TYPE_PARAM_ALLOWABLE_VALUES, required = true)
                                       @PathVariable String type,
                                       @ApiParam(value = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                       @PathVariable String key) throws ThingsboardException {
        return checkImageInfo(type, key, Operation.READ);
    }

    /**
     * 功能：获取`Images`。
     * 参数：
     * - `PAGE_SIZE_DESCRIPTION`：`PAGE_SIZE_DESCRIPTION` 参数。
     * - `pageSize`：`pageSize` 参数。
     * - `PAGE_NUMBER_DESCRIPTION`：`PAGE_NUMBER_DESCRIPTION` 参数。
     * - `page`：`page` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/api/images")
    public PageData<TbResourceInfo> getImages(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                              @RequestParam int pageSize,
                                              @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                              @RequestParam int page,
                                              @ApiParam(value = RESOURCE_INCLUDE_SYSTEM_IMAGES_DESCRIPTION)
                                              @RequestParam(required = false) boolean includeSystemImages,
                                              @ApiParam(value = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                              @RequestParam(required = false) String textSearch,
                                              @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = RESOURCE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                              @RequestParam(required = false) String sortProperty,
                                              @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                              @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        // PE: generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getTenantId();
        if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN || !includeSystemImages) {
            return checkNotNull(imageService.getImagesByTenantId(tenantId, pageLink));
        } else {
            return checkNotNull(imageService.getAllImagesByTenantId(tenantId, pageLink));
        }
    }

    /**
     * 功能：删除或清理图片资源。
     * 参数：
     * - `IMAGE_TYPE_PARAM_DESCRIPTION`：类型。
     * - `IMAGE_TYPE_PARAM_ALLOWABLE_VALUES`：类型。
     * - `type`：类型。
     * - `IMAGE_KEY_PARAM_DESCRIPTION`：键。
     * - 其余参数：补充处理条件。
     * 返回：响应结果。
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(IMAGE_URL)
    public ResponseEntity<TbImageDeleteResult> deleteImage(@ApiParam(value = IMAGE_TYPE_PARAM_DESCRIPTION, allowableValues = IMAGE_TYPE_PARAM_ALLOWABLE_VALUES, required = true)
                                                           @PathVariable String type,
                                                           @ApiParam(value = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                           @PathVariable String key,
                                                           @RequestParam(name = "force", required = false) boolean force) throws ThingsboardException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.DELETE);
        TbImageDeleteResult result = tbImageService.delete(imageInfo, getCurrentUser(), force);
        return (result.isSuccess() ? ResponseEntity.ok() : ResponseEntity.badRequest()).body(result);
    }

    /**
     * 功能：执行 `downloadIfChanged` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * - `etag`：`etag` 参数。
     * - `preview`：`preview` 参数。
     * 返回：响应结果。
     */
    private ResponseEntity<ByteArrayResource> downloadIfChanged(String type, String key, String etag, boolean preview) throws Exception {
        ImageCacheKey cacheKey = ImageCacheKey.forImage(getTenantId(type), key, preview);
        return downloadIfChanged(cacheKey, etag, () -> checkImageInfo(type, key, Operation.READ));
    }

    /**
     * 功能：执行 `downloadIfChanged` 对应的处理。
     * 参数：
     * - `cacheKey`：键。
     * - `etag`：`etag` 参数。
     * - `imageInfoSupplier`：`imageInfoSupplier` 参数。
     * 返回：响应结果。
     */
    private ResponseEntity<ByteArrayResource> downloadIfChanged(ImageCacheKey cacheKey, String etag, ThrowingSupplier<TbResourceInfo> imageInfoSupplier) throws Exception {
        if (StringUtils.isNotEmpty(etag)) {
            etag = StringUtils.remove(etag, '\"'); // etag is wrapped in double quotes due to HTTP specification
            if (etag.equals(tbImageService.getETag(cacheKey))) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
        }

        TbResourceInfo imageInfo = checkNotNull(imageInfoSupplier.get());
        String fileName = imageInfo.getFileName();
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        byte[] data;
        if (cacheKey.isPreview()) {
            descriptor = descriptor.getPreviewDescriptor();
            data = imageService.getImagePreview(imageInfo.getTenantId(), imageInfo.getId());
        } else {
            data = imageService.getImageData(imageInfo.getTenantId(), imageInfo.getId());
        }
        tbImageService.putETag(cacheKey, descriptor.getEtag());
        var result = ResponseEntity.ok()
                .header("Content-Type", descriptor.getMediaType())
                .contentLength(data.length)
                .eTag(descriptor.getEtag());
        if (!cacheKey.isPublic()) {
            result
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                    .header("x-filename", fileName);
        }
        if (systemImagesBrowserTtlInMinutes > 0 && imageInfo.getTenantId().isSysTenantId()) {
            result.cacheControl(CacheControl.maxAge(systemImagesBrowserTtlInMinutes, TimeUnit.MINUTES));
        } else if (tenantImagesBrowserTtlInMinutes > 0 && !imageInfo.getTenantId().isSysTenantId()) {
            result.cacheControl(CacheControl.maxAge(tenantImagesBrowserTtlInMinutes, TimeUnit.MINUTES));
        } else {
            result.cacheControl(CacheControl.noCache());
        }
        return result.body(new ByteArrayResource(data));
    }

    /**
     * 功能：校验信息对象。
     * 参数：
     * - `imageType`：类型。
     * - `key`：键。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    private TbResourceInfo checkImageInfo(String imageType, String key, Operation operation) throws ThingsboardException {
        TenantId tenantId = getTenantId(imageType);
        TbResourceInfo imageInfo = imageService.getImageInfoByTenantIdAndKey(tenantId, key);
        checkEntity(getCurrentUser(), checkNotNull(imageInfo), operation);
        return imageInfo;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `imageType`：类型。
     * 返回：处理结果。
     */
    private TenantId getTenantId(String imageType) throws ThingsboardException {
        TenantId tenantId;
        if (imageType.equals(TENANT_IMAGE)) {
            tenantId = getTenantId();
        } else if (imageType.equals(SYSTEM_IMAGE)) {
            tenantId = TenantId.SYS_TENANT_ID;
        } else {
            throw new IllegalArgumentException("Invalid image URL");
        }
        return tenantId;
    }
}