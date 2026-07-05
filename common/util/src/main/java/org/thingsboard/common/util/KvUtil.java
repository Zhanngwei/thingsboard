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
package org.thingsboard.common.util;

import org.thingsboard.server.common.data.kv.KvEntry;

/**
 * 中文说明：
 * 1. 类目的：`KvUtil` 是ThingsBoard Common 模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
 */
public class KvUtil {

    /**
     * 功能：获取值。
     * 参数：
     * - `entry`：`entry` 参数。
     * 返回：文本结果。
     */
    public static String getStringValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(String::valueOf).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().map(String::valueOf).orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().map(String::valueOf).orElse(null);
            case STRING:
                return entry.getStrValue().orElse("");
            case JSON:
                return entry.getJsonValue().orElse("");
            default:
                return null;
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `entry`：`entry` 参数。
     * 返回：数值结果。
     */
    public static Double getDoubleValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(Long::doubleValue).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().map(e -> e ? 1.0 : 0).orElse(null);
            case STRING:
                try {
                    return Double.parseDouble(entry.getStrValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Double.parseDouble(entry.getJsonValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `entry`：`entry` 参数。
     * 返回：判断结果。
     */
    public static Boolean getBoolValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case LONG:
                return entry.getLongValue().map(e -> e != 0).orElse(null);
            case DOUBLE:
                return entry.getDoubleValue().map(e -> e != 0).orElse(null);
            case BOOLEAN:
                return entry.getBooleanValue().orElse(null);
            case STRING:
                try {
                    return Boolean.parseBoolean(entry.getStrValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Boolean.parseBoolean(entry.getJsonValue().orElse(""));
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`KvUtil` 在 ThingsBoard Common 模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
