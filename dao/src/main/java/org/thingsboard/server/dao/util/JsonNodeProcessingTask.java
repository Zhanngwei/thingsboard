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
package org.thingsboard.server.dao.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 中文说明：
 * 1. `JsonNodeProcessingTask` 是 ThingsBoard DAO 中负责 `Json Node Processing` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Data
public class JsonNodeProcessingTask {
    /**
     * 路径，用于定位本地文件或目录。
     */
    private final String path;
    private final JsonNode node;

    /**
     * 功能：创建 `JsonNodeProcessingTask` 实例，并初始化必要字段。
     * 参数：
     * - `path`：文件或资源路径。
     * - `node`：`node` 参数。
     * 返回：新创建的对象实例。
     */
    public JsonNodeProcessingTask(String path, JsonNode node) {
        this.path = path;
        this.node = node;
    }
}
