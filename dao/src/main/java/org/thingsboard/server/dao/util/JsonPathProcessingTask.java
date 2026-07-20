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
import org.thingsboard.server.dao.dashboard.DashboardServiceImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. `JsonPathProcessingTask` 是 ThingsBoard DAO 中负责 `Json Path Processing` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Data
public class JsonPathProcessingTask {
    /**
     * `tokens`列表，用于保存一组待处理对象。
     */
    private final String[] tokens;
    private final Map<String, String> variables;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private final JsonNode node;

    /**
     * 功能：创建 `JsonPathProcessingTask` 实例，并初始化必要字段。
     * 参数：
     * - `tokens`：`tokens` 参数。
     * - `variables`：键值映射。
     * - `node`：`node` 参数。
     * 返回：新创建的对象实例。
     */
    public JsonPathProcessingTask(String[] tokens, Map<String, String> variables, JsonNode node) {
        this.tokens = tokens;
        this.variables = variables;
        this.node = node;
    }

    /**
     * 功能：判断`Last`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isLast() {
        return tokens.length == 1;
    }

    /**
     * 功能：执行 `currentToken` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String currentToken() {
        return tokens[0];
    }

    /**
     * 功能：执行 `next` 对应的处理。
     * 参数：
     * - `next`：`next` 参数。
     * 返回：处理结果。
     */
    public JsonPathProcessingTask next(JsonNode next) {
        return new JsonPathProcessingTask(
                Arrays.copyOfRange(tokens, 1, tokens.length),
                variables,
                next);
    }

    /**
     * 功能：执行 `next` 对应的处理。
     * 参数：
     * - `next`：`next` 参数。
     * - `key`：键。
     * - `value`：值。
     * 返回：处理结果。
     */
    public JsonPathProcessingTask next(JsonNode next, String key, String value) {
        Map<String, String> variables = new HashMap<>(this.variables);
        variables.put(key, value);
        return new JsonPathProcessingTask(
                Arrays.copyOfRange(tokens, 1, tokens.length),
                variables,
                next);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        return "JsonPathProcessingTask{" +
                "tokens=" + Arrays.toString(tokens) +
                ", variables=" + variables +
                ", node=" + node.toString().substring(0, 20) +
                '}';
    }
}
