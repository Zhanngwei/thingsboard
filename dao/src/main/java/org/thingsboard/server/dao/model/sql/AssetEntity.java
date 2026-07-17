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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Entity;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.ASSET_TABLE_NAME;

/**
 * 中文说明：
 * 1. `AssetEntity` 是 ThingsBoard DAO 中表示资产持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `AbstractAssetEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ASSET_TABLE_NAME)
public final class AssetEntity extends AbstractAssetEntity<Asset> {

    /**
     * 功能：创建 `AssetEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AssetEntity() {
        super();
    }

    /**
     * 功能：创建 `AssetEntity` 实例，并初始化必要字段。
     * 参数：
     * - `asset`：`asset` 参数。
     * 返回：新创建的对象实例。
     */
    public AssetEntity(Asset asset) {
        super(asset);
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset toData() {
        return super.toAsset();
    }

}
