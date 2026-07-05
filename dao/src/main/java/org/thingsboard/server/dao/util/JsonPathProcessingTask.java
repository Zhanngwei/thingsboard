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
 * 1. 类目的：`JsonPathProcessingTask` 是 ThingsBoard DAO 模块 中的DAO 工具和配置类型，用于提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 生命周期：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Utility / Factory / Template。
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

/*
 * 本类总结：
 * 1. 核心职责：`JsonPathProcessingTask` 在 ThingsBoard DAO 模块 中承担DAO 工具和配置类型职责，核心目的是提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 核心流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
 * 3. 关键依赖：主要依赖或协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
