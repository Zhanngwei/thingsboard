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
package org.thingsboard.server.common.data.relation;

import io.swagger.annotations.ApiModelProperty;

/**
 * 中文说明：
 * 1. `EntityRelationInfo` 是 ThingsBoard Common Data 中承载实体关系信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `EntityRelation`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class EntityRelationInfo extends EntityRelation {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 2807343097519543363L;

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String fromName;
    private String toName;

    /**
     * 功能：创建 `EntityRelationInfo` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public EntityRelationInfo() {
        super();
    }

    /**
     * 功能：创建 `EntityRelationInfo` 实例，并初始化必要字段。
     * 参数：
     * - `entityRelation`：实体对象。
     * 返回：新创建的对象实例。
     */
    public EntityRelationInfo(EntityRelation entityRelation) {
        super(entityRelation);
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 6, value = "Name of the entity for [from] direction.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "A4B72CCDFF33")
    public String getFromName() {
        return fromName;
    }

    /**
     * 功能：更新名称。
     * 参数：
     * - `fromName`：名称。
     * 返回：无。
     */
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 7, value = "Name of the entity for [to] direction.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "A4B72CCDFF35")
    public String getToName() {
        return toName;
    }

    /**
     * 功能：更新名称。
     * 参数：
     * - `toName`：名称。
     * 返回：无。
     */
    public void setToName(String toName) {
        this.toName = toName;
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EntityRelationInfo that = (EntityRelationInfo) o;

        return toName != null ? toName.equals(that.toName) : that.toName == null;

    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (toName != null ? toName.hashCode() : 0);
        return result;
    }
}
