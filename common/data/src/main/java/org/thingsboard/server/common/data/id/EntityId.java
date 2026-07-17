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
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */

/**
 * 中文说明：
 * 1. `EntityId` 是 ThingsBoard Common Data 中定义实体能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `HasUUID`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
@JsonDeserialize(using = EntityIdDeserializer.class)
@JsonSerialize(using = EntityIdSerializer.class)
@ApiModel
public interface EntityId extends HasUUID, Serializable { //NOSONAR, the constant is closely related to EntityId

    UUID NULL_UUID = UUID.fromString("13814000-1dd2-11b2-8080-808080808080");

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, required = true, value = "ID of the entity, time-based UUID v1", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    UUID getId();

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 2, required = true, example = "DEVICE")
    EntityType getEntityType();

    /**
     * 功能：判断`Null Uid`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    default boolean isNullUid() {
        return NULL_UUID.equals(getId());
    }

}
