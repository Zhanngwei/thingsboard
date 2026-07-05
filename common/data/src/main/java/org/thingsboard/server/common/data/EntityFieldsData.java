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
 * 1. 类目的：`EntityFieldsData` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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
     * 1. 类目的：`EntityIdFieldSerializer` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
     * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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


/*
 * 本类总结：
 * 1. 核心职责：`EntityFieldsData` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
