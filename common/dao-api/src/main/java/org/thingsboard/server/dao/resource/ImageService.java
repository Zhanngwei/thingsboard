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
 * 1. `ImageService` 是 ThingsBoard Common 中定义图片资源能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
