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

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.transport.TransportProtos.ImageCacheKeyProto;

/**
 * 中文说明：
 * 1. `ImageCacheKey` 是 ThingsBoard DAO 中管理缓存缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 它直接协作于缓存实现、领域标识符和调用该缓存的服务。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageCacheKey {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final String resourceKey;
    /**
     * 是否满足`preview`条件。
     */
    @With
    private final boolean preview;

    /**
     * 公钥，用于定位映射、配置或数据项。
     */
    private final String publicResourceKey;

    /**
     * 功能：执行 `forImage` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * - `preview`：`preview` 参数。
     * 返回：处理结果。
     */
    public static ImageCacheKey forImage(TenantId tenantId, String key, boolean preview) {
        return new ImageCacheKey(tenantId, key, preview, null);
    }

    /**
     * 功能：执行 `forImage` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    public static ImageCacheKey forImage(TenantId tenantId, String key) {
        return forImage(tenantId, key, false);
    }

    /**
     * 功能：执行 `forPublicImage` 对应的处理。
     * 参数：
     * - `publicKey`：键。
     * 返回：处理结果。
     */
    public static ImageCacheKey forPublicImage(String publicKey) {
        return new ImageCacheKey(null, null, false, publicKey);
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public ImageCacheKeyProto toProto() {
        var msg = ImageCacheKeyProto.newBuilder();
        if (resourceKey != null) {
            msg.setResourceKey(resourceKey);
        } else {
            msg.setPublicResourceKey(publicResourceKey);
        }
        return msg.build();
    }

    /**
     * 功能：判断`Public`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isPublic() {
        return this.publicResourceKey != null;
    }

}
