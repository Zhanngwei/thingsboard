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
package org.thingsboard.rest.client.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`RestJsonConverter` 是 ThingsBoard Rest Client 模块 中的REST JSON 到 KV 模型转换器，用于把 REST API 返回的属性和时序 JSON 转换为 ThingsBoard 内部 AttributeKvEntry、TsKvEntry 和 KvEntry 类型。
 * 2. 所属模块：位于 rest-client，服务于 ThingsBoard 的客户端访问、离线工具或独立协议接入边界。
 * 3. 协作模块：主要协作对象包括 Jackson JsonNode、ThingsBoard KV 数据模型、RestClient 属性/时序查询方法。
 * 4. 生命周期：作为无状态工具类被静态调用，方法执行期间临时创建转换结果，调用结束即可释放局部对象。
 * 5. 存在原因：独立转换器让 RestClient 保持 HTTP 门面职责，不把 JSON 类型判断和 KV 构造细节混入大量 REST 方法。
 * 6. 事务：不涉及事务，只在内存中转换 REST 响应数据。
 * 7. 缓存：不涉及缓存，每次调用都重新转换输入 JSON。
 * 8. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
 * 9. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
 * 10. 数据库：不访问数据库，数据来源是服务端 REST 响应。
 * 11. Rule Engine：不执行 Rule Engine，只为可能来自遥测/属性查询的数据构造客户端模型。
 * 12. 设计模式：主要体现 Adapter / Converter。
 */
public class RestJsonConverter {
    /**
     * 字段说明：
     * 1. 保存内容：`KEY` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `REST JSON 到 KV 模型转换器` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Rest Client 模块 的上层流程。
     */
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String LAST_UPDATE_TS = "lastUpdateTs";
    /**
     * 字段说明：
     * 1. 保存内容：`TS` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `REST JSON 到 KV 模型转换器` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Rest Client 模块 的上层流程。
     */
    private static final String TS = "ts";

    private static final String CAN_T_PARSE_VALUE = "Can't parse value: ";

    /**
     * 方法说明：
     * 1. 职责：`toAttributes` 执行 REST JSON 到 KV 模型转换器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：作为无状态工具类被静态调用，方法执行期间临时创建转换结果，调用结束即可释放局部对象；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：读取 JSON 中的 key、value、ts 或 lastUpdateTs 字段，按布尔、数字、文本、JSON 容器类型构造对应 KV entry。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：不涉及事务，只在内存中转换 REST 响应数据。
     * 9. 缓存：不涉及缓存，每次调用都重新转换输入 JSON。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：不访问数据库，数据来源是服务端 REST 响应。
     * 13. Rule Engine：不执行 Rule Engine，只为可能来自遥测/属性查询的数据构造客户端模型。
     */
    public static List<AttributeKvEntry> toAttributes(List<JsonNode> attributes) {
        // 空集合直接返回空结果，避免调用方处理 null，同时表达“服务端无数据”而不是转换失败。
        if (!CollectionUtils.isEmpty(attributes)) {
            return attributes.stream().map(attr -> {
                        KvEntry entry = parseValue(attr.get(KEY).asText(), attr.get(VALUE));
                        return new BaseAttributeKvEntry(entry, attr.get(LAST_UPDATE_TS).asLong());
                    }
            ).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`toTimeseries` 执行 REST JSON 到 KV 模型转换器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：作为无状态工具类被静态调用，方法执行期间临时创建转换结果，调用结束即可释放局部对象；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：读取 JSON 中的 key、value、ts 或 lastUpdateTs 字段，按布尔、数字、文本、JSON 容器类型构造对应 KV entry。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：不涉及事务，只在内存中转换 REST 响应数据。
     * 9. 缓存：不涉及缓存，每次调用都重新转换输入 JSON。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：不访问数据库，数据来源是服务端 REST 响应。
     * 13. Rule Engine：不执行 Rule Engine，只为可能来自遥测/属性查询的数据构造客户端模型。
     */
    public static List<TsKvEntry> toTimeseries(Map<String, List<JsonNode>> timeseries) {
        // 空集合直接返回空结果，避免调用方处理 null，同时表达“服务端无数据”而不是转换失败。
        if (!CollectionUtils.isEmpty(timeseries)) {
            List<TsKvEntry> result = new ArrayList<>();
            timeseries.forEach((key, values) ->
                    result.addAll(values.stream().map(ts -> {
                                KvEntry entry = parseValue(key, ts.get(VALUE));
                                return new BasicTsKvEntry(ts.get(TS).asLong(), entry);
                            }
                    ).collect(Collectors.toList()))
            );
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`parseValue` 执行 REST JSON 到 KV 模型转换器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：作为无状态工具类被静态调用，方法执行期间临时创建转换结果，调用结束即可释放局部对象；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：读取 JSON 中的 key、value、ts 或 lastUpdateTs 字段，按布尔、数字、文本、JSON 容器类型构造对应 KV entry。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：不涉及事务，只在内存中转换 REST 响应数据。
     * 9. 缓存：不涉及缓存，每次调用都重新转换输入 JSON。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：不访问数据库，数据来源是服务端 REST 响应。
     * 13. Rule Engine：不执行 Rule Engine，只为可能来自遥测/属性查询的数据构造客户端模型。
     */
    private static KvEntry parseValue(String key, JsonNode value) {
        // 按 JsonNode 实际类型选择 KV 子类，保持客户端模型与服务端遥测/属性存储类型一致。
        if (!value.isContainerNode()) {
            // 按 JsonNode 实际类型选择 KV 子类，保持客户端模型与服务端遥测/属性存储类型一致。
            if (value.isBoolean()) {
                return new BooleanDataEntry(key, value.asBoolean());
            // 按 JsonNode 实际类型选择 KV 子类，保持客户端模型与服务端遥测/属性存储类型一致。
            } else if (value.isNumber()) {
                return parseNumericValue(key, value);
            // 按 JsonNode 实际类型选择 KV 子类，保持客户端模型与服务端遥测/属性存储类型一致。
            } else if (value.isTextual()) {
                return new StringDataEntry(key, value.asText());
            } else {
                throw new RuntimeException(CAN_T_PARSE_VALUE + value);
            }
        } else {
            return new JsonDataEntry(key, value.toString());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`parseNumericValue` 执行 REST JSON 到 KV 模型转换器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：作为无状态工具类被静态调用，方法执行期间临时创建转换结果，调用结束即可释放局部对象；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：读取 JSON 中的 key、value、ts 或 lastUpdateTs 字段，按布尔、数字、文本、JSON 容器类型构造对应 KV entry。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：不涉及事务，只在内存中转换 REST 响应数据。
     * 9. 缓存：不涉及缓存，每次调用都重新转换输入 JSON。
     * 10. MQTT：REST 客户端不直接使用 MQTT，但设备凭据、遥测、规则链等 API 可能影响后续 MQTT transport 入站行为。
     * 11. Actor 通信：REST 请求到达服务端后可能触发 Actor 消息，例如设备、规则链或遥测相关操作；本类只负责 HTTP 边界。
     * 12. 数据库：不访问数据库，数据来源是服务端 REST 响应。
     * 13. Rule Engine：不执行 Rule Engine，只为可能来自遥测/属性查询的数据构造客户端模型。
     */
    private static KvEntry parseNumericValue(String key, JsonNode value) {
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (value.isFloatingPointNumber()) {
            return new DoubleDataEntry(key, value.asDouble());
        } else {
            try {
                long longValue = Long.parseLong(value.toString());
                return new LongDataEntry(key, longValue);
            // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Big integer values are not supported!");
            }
        }
    }
    /**
     * 本类总结：
     * 1. 核心职责：`RestJsonConverter` 负责把 REST API 返回的属性和时序 JSON 转换为 ThingsBoard 内部 AttributeKvEntry、TsKvEntry 和 KvEntry 类型。
     * 2. 核心流程：读取 JSON 中的 key、value、ts 或 lastUpdateTs 字段，按布尔、数字、文本、JSON 容器类型构造对应 KV entry。
     * 3. 关键依赖：Jackson JsonNode、ThingsBoard KV 数据模型、RestClient 属性/时序查询方法。
     * 4. 设计重点：通过 Adapter / Converter 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}
