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
package org.thingsboard.rule.engine.util;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

/**
 * 同时携带实体 ID 和实体类型的简单容器。
 * 本类不直接访问数据库、缓存或 Rule Engine 消息流，线程安全性取决于实例是否被调用方跨线程共享修改。
 */
@Data
public class EntityContainer {

    /**
     * 具体实体标识。
     */
    private EntityId entityId;
    /**
     * 实体类型，通常与 entityId 中的类型保持一致或用于配置展示。
     */
    private EntityType entityType;

}

/*
 * 本类总结：
 * 本类是 Lombok 数据容器，用于在规则节点或工具方法之间传递实体标识信息；持久化、缓存和消息处理均由调用方完成。
 */
