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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.IOException;

/**
 * Created by ashvayka on 01.06.18.
 */
/**
 * 中文说明：
 * 1. `EntityFieldsData` 是 ThingsBoard Common Data 中承载实体信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@AllArgsConstructor
public class EntityFieldsData {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        SimpleModule entityFieldsModule = new SimpleModule("EntityFieldsModule", new Version(1, 0, 0, null, null, null));
        entityFieldsModule.addSerializer(EntityId.class, new EntityIdFieldSerializer());
        mapper.disable(MapperFeature.USE_ANNOTATIONS);
        mapper.registerModule(entityFieldsModule);
    }

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private ObjectNode fieldsData;

    /**
     * 功能：创建 `EntityFieldsData` 实例，并初始化必要字段。
     * 参数：
     * - `data`：待处理数据。
     * 返回：新创建的对象实例。
     */
    public EntityFieldsData(BaseData data) {
        fieldsData = mapper.valueToTree(data);
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `field`：`field` 参数。
     * 返回：文本结果。
     */
    public String getFieldValue(String field) {
        return getFieldValue(field, false);
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `field`：`field` 参数。
     * - `ignoreNullStrings`：`ignoreNullStrings` 参数。
     * 返回：文本结果。
     */
    public String getFieldValue(String field, boolean ignoreNullStrings) {
        String[] fieldsTree = field.split("\\.");
        JsonNode current = fieldsData;
        for (String key : fieldsTree) {
            if (current.has(key)) {
                current = current.get(key);
            } else {
                current = null;
                break;
            }
        }
        if (current == null) {
            return null;
        }
        if (current.isNull() && ignoreNullStrings) {
            return null;
        }
        if (current.isValueNode()) {
            String textValue = current.asText();
            if (StringUtils.isEmpty(textValue) && ignoreNullStrings) {
                return null;
            }
            return textValue;
        }
        try {
            return mapper.writeValueAsString(current);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 中文说明：
     * 1. `EntityIdFieldSerializer` 是 ThingsBoard Common Data 中承载实体信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 直接依赖的类型边界包括 `JsonSerializer`。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    private static class EntityIdFieldSerializer extends JsonSerializer<EntityId> {

        /**
         * 功能：执行 `serialize` 对应的处理。
         * 参数：
         * - `value`：值。
         * - `gen`：`gen` 参数。
         * - `serializers`：`serializers` 参数。
         * 返回：无。
         */
        @Override
        public void serialize(EntityId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeObject(value.getId());
        }
    }

}
