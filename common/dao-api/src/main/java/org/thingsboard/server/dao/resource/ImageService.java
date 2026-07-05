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
package org.thingsboard.server.dao.resource;

import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;

/**
 * 中文说明：
 * 1. 类目的：`ImageService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface ImageService {

    /**
     * 功能：保存或创建图片资源。
     * 参数：
     * - `image`：`image` 参数。
     * 返回：处理结果。
     */
    TbResourceInfo saveImage(TbResource image);

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `imageInfo`：`imageInfo` 参数。
     * 返回：处理结果。
     */
    TbResourceInfo saveImageInfo(TbResourceInfo imageInfo);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    TbResourceInfo getImageInfoByTenantIdAndKey(TenantId tenantId, String key);

    /**
     * 功能：获取公钥。
     * 参数：
     * - `publicResourceKey`：键。
     * 返回：处理结果。
     */
    TbResourceInfo getPublicImageInfoByKey(String publicResourceKey);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<TbResourceInfo> getImagesByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<TbResourceInfo> getAllImagesByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageId`：图片资源ID。
     * 返回：处理结果。
     */
    byte[] getImageData(TenantId tenantId, TbResourceId imageId);

    /**
     * 功能：获取图片资源。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageId`：图片资源ID。
     * 返回：处理结果。
     */
    byte[] getImagePreview(TenantId tenantId, TbResourceId imageId);

    /**
     * 功能：删除或清理图片资源。
     * 参数：
     * - `imageInfo`：`imageInfo` 参数。
     * - `force`：`force` 参数。
     * 返回：处理结果。
     */
    TbImageDeleteResult deleteImage(TbResourceInfo imageInfo, boolean force);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `etag`：`etag` 参数。
     * 返回：处理结果。
     */
    TbResourceInfo findSystemOrTenantImageByEtag(TenantId tenantId, String etag);

    /**
     * 功能：执行 `replaceBase64WithImageUrl` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `type`：类型。
     * 返回：判断结果。
     */
    boolean replaceBase64WithImageUrl(HasImage entity, String type);

    /**
     * 功能：执行 `replaceBase64WithImageUrl` 对应的处理。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * 返回：判断结果。
     */
    boolean replaceBase64WithImageUrl(Dashboard dashboard);

    /**
     * 功能：执行 `replaceBase64WithImageUrl` 对应的处理。
     * 参数：
     * - `widgetType`：类型。
     * 返回：判断结果。
     */
    boolean replaceBase64WithImageUrl(WidgetTypeDetails widgetType);

    /**
     * 功能：执行 `inlineImage` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * 返回：无。
     */
    void inlineImage(HasImage entity);

    /**
     * 功能：执行 `inlineImages` 对应的处理。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * 返回：无。
     */
    void inlineImages(Dashboard dashboard);

    /**
     * 功能：执行 `inlineImages` 对应的处理。
     * 参数：
     * - `widgetTypeDetails`：类型。
     * 返回：无。
     */
    void inlineImages(WidgetTypeDetails widgetTypeDetails);

    /**
     * 功能：执行 `inlineImageForEdge` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * 返回：无。
     */
    void inlineImageForEdge(HasImage entity);

    /**
     * 功能：执行 `inlineImagesForEdge` 对应的处理。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * 返回：无。
     */
    void inlineImagesForEdge(Dashboard dashboard);

    /**
     * 功能：执行 `inlineImagesForEdge` 对应的处理。
     * 参数：
     * - `widgetTypeDetails`：类型。
     * 返回：无。
     */
    void inlineImagesForEdge(WidgetTypeDetails widgetTypeDetails);
}

/*
 * 本类总结：
 * 1. 核心职责：`ImageService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
