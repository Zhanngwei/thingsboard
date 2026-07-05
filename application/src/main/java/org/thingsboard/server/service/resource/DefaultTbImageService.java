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
package org.thingsboard.server.service.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.resource.ImageCacheKey;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbImageService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@Slf4j
@TbCoreComponent
public class DefaultTbImageService extends AbstractTbEntityService implements TbImageService {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbClusterService clusterService;
    private final ImageService imageService;
    /**
     * `etagCache` 字段，保存当前对象的对应属性。
     */
    private final Cache<ImageCacheKey, String> etagCache;

    /**
     * 功能：创建 `DefaultTbImageService` 实例，并初始化必要字段。
     * 参数：
     * - `clusterService`：服务对象。
     * - `imageService`：服务对象。
     * - `cacheTtl`：`cacheTtl` 参数。
     * - `cacheMaxSize`：`cacheMaxSize` 参数。
     * 返回：新创建的对象实例。
     */
    public DefaultTbImageService(TbClusterService clusterService, ImageService imageService,
                                 @Value("${cache.image.etag.timeToLiveInMinutes:44640}") int cacheTtl,
                                 @Value("${cache.image.etag.maxSize:10000}") int cacheMaxSize) {
        this.clusterService = clusterService;
        this.imageService = imageService;
        this.etagCache = Caffeine.newBuilder()
                .expireAfterAccess(cacheTtl, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
    }

    /**
     * 功能：获取`E Tag`。
     * 参数：
     * - `imageCacheKey`：键。
     * 返回：文本结果。
     */
    @Override
    public String getETag(ImageCacheKey imageCacheKey) {
        return etagCache.getIfPresent(imageCacheKey);
    }

    /**
     * 功能：执行 `putETag` 对应的处理。
     * 参数：
     * - `imageCacheKey`：键。
     * - `etag`：`etag` 参数。
     * 返回：无。
     */
    @Override
    public void putETag(ImageCacheKey imageCacheKey, String etag) {
        etagCache.put(imageCacheKey, etag);
    }

    /**
     * 功能：删除或清理`E Tags`。
     * 参数：
     * - `imageCacheKey`：键。
     * 返回：无。
     */
    @Override
    public void evictETags(ImageCacheKey imageCacheKey) {
        etagCache.invalidate(imageCacheKey);
        if (imageCacheKey.getPublicResourceKey() == null) {
            etagCache.invalidate(imageCacheKey.withPreview(true));
        }
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `image`：`image` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbResourceInfo save(TbResource image, User user) throws Exception {
        ActionType actionType = image.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = image.getTenantId();
        try {
            var oldEtag = getEtag(image);
            TbResourceInfo existingImage = null;
            if (image.getId() == null && StringUtils.isNotEmpty(image.getResourceKey())) {
                existingImage = imageService.getImageInfoByTenantIdAndKey(tenantId, image.getResourceKey());
                if (existingImage != null) {
                    image.setId(existingImage.getId());
                }
            }
            TbResourceInfo savedImage = imageService.saveImage(image);
            notificationEntityService.logEntityAction(tenantId, savedImage.getId(), savedImage, actionType, user);

            List<ImageCacheKey> toEvict = new ArrayList<>();
            if (oldEtag.isPresent()) {
                var newEtag = getEtag(savedImage);
                if (newEtag.isPresent() && !oldEtag.get().equals(newEtag.get())) {
                    toEvict.add(ImageCacheKey.forImage(tenantId, image.getResourceKey()));
                    if (image.isPublic()) {
                        toEvict.add(ImageCacheKey.forPublicImage(savedImage.getPublicResourceKey()));
                    }
                }
            }
            if (existingImage != null && image.isPublic() != existingImage.isPublic()) {
                toEvict.add(ImageCacheKey.forPublicImage(image.getPublicResourceKey()));
            }
            if (!toEvict.isEmpty()) {
                evictFromCache(tenantId, toEvict);
            }
            return savedImage;
        } catch (Exception e) {
            image.setData(null);
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.TB_RESOURCE), image, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：获取`Etag`。
     * 参数：
     * - `image`：`image` 参数。
     * 返回：可能存在的结果。
     */
    private Optional<String> getEtag(TbResourceInfo image) throws JsonProcessingException {
        var descriptor = image.getDescriptor(ImageDescriptor.class);
        return Optional.ofNullable(descriptor != null ? descriptor.getEtag() : null);
    }

    /**
     * 功能：获取`Preview Etag`。
     * 参数：
     * - `image`：`image` 参数。
     * 返回：可能存在的结果。
     */
    private Optional<String> getPreviewEtag(TbResourceInfo image) throws JsonProcessingException {
        var descriptor = image.getDescriptor(ImageDescriptor.class);
        descriptor = descriptor != null ? descriptor.getPreviewDescriptor() : null;
        return Optional.ofNullable(descriptor != null ? descriptor.getEtag() : null);
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `imageInfo`：`imageInfo` 参数。
     * - `oldImageInfo`：`oldImageInfo` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbResourceInfo save(TbResourceInfo imageInfo, TbResourceInfo oldImageInfo, User user) {
        TenantId tenantId = imageInfo.getTenantId();
        TbResourceId imageId = imageInfo.getId();
        try {
            imageInfo = imageService.saveImageInfo(imageInfo);
            notificationEntityService.logEntityAction(tenantId, imageId, imageInfo, ActionType.UPDATED, user);

            if (imageInfo.isPublic() != oldImageInfo.isPublic()) {
                evictFromCache(tenantId, List.of(ImageCacheKey.forPublicImage(imageInfo.getPublicResourceKey())));
            }
            return imageInfo;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, imageId, imageInfo, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `imageInfo`：`imageInfo` 参数。
     * - `user`：`user` 参数。
     * - `force`：`force` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbImageDeleteResult delete(TbResourceInfo imageInfo, User user, boolean force) {
        TenantId tenantId = imageInfo.getTenantId();
        TbResourceId imageId = imageInfo.getId();
        try {
            TbImageDeleteResult result = imageService.deleteImage(imageInfo, force);
            if (result.isSuccess()) {
                notificationEntityService.logEntityAction(tenantId, imageId, imageInfo, ActionType.DELETED, user, imageId.toString());

                List<ImageCacheKey> toEvict = new ArrayList<>();
                toEvict.add(ImageCacheKey.forImage(tenantId, imageInfo.getResourceKey()));
                if (imageInfo.isPublic()) {
                    toEvict.add(ImageCacheKey.forPublicImage(imageInfo.getPublicResourceKey()));
                }
                evictFromCache(tenantId, toEvict);
            }
            return result;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, imageId, ActionType.DELETED, user, e, imageId.toString());
            throw e;
        }
    }

    /**
     * 功能：删除或清理`From Cache`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `toEvict`：数据列表。
     * 返回：无。
     */
    private void evictFromCache(TenantId tenantId, List<ImageCacheKey> toEvict) {
        toEvict.forEach(this::evictETags);
        clusterService.broadcastToCore(TransportProtos.ToCoreNotificationMsg.newBuilder()
                .setResourceCacheInvalidateMsg(TransportProtos.ResourceCacheInvalidateMsg.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .addAllKeys(toEvict.stream().map(ImageCacheKey::toProto).collect(Collectors.toList()))
                        .build())
                .build());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbImageService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
