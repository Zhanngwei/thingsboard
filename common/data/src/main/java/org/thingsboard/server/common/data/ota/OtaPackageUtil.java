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
package org.thingsboard.server.common.data.ota;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.HasOtaPackage;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 中文说明：
 * 1. 类目的：`OtaPackageUtil` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Slf4j
public class OtaPackageUtil {

    /**
     * 属性常量，用于统一引用固定值。
     */
    public static final List<String> ALL_FW_ATTRIBUTE_KEYS;

    /**
     * 属性常量，用于统一引用固定值。
     */
    public static final List<String> ALL_SW_ATTRIBUTE_KEYS;

    static {
        ALL_FW_ATTRIBUTE_KEYS = new ArrayList<>();
        for (OtaPackageKey key : OtaPackageKey.values()) {
            ALL_FW_ATTRIBUTE_KEYS.add(getAttributeKey(OtaPackageType.FIRMWARE, key));

        }

        ALL_SW_ATTRIBUTE_KEYS = new ArrayList<>();
        for (OtaPackageKey key : OtaPackageKey.values()) {
            ALL_SW_ATTRIBUTE_KEYS.add(getAttributeKey(OtaPackageType.SOFTWARE, key));

        }
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `firmwareType`：类型。
     * 返回：匹配的数据集合。
     */
    public static List<String> getAttributeKeys(OtaPackageType firmwareType) {
        switch (firmwareType) {
            case FIRMWARE:
                return ALL_FW_ATTRIBUTE_KEYS;
            case SOFTWARE:
                return ALL_SW_ATTRIBUTE_KEYS;
        }
        return Collections.emptyList();
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * 返回：文本结果。
     */
    public static String getAttributeKey(OtaPackageType type, OtaPackageKey key) {
        return type.getKeyPrefix() + "_" + key.getValue();
    }

    /**
     * 功能：获取遥测。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * 返回：文本结果。
     */
    public static String getTargetTelemetryKey(OtaPackageType type, OtaPackageKey key) {
        return getTelemetryKey("target_", type, key);
    }

    /**
     * 功能：获取遥测。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * 返回：文本结果。
     */
    public static String getCurrentTelemetryKey(OtaPackageType type, OtaPackageKey key) {
        return getTelemetryKey("current_", type, key);
    }

    /**
     * 功能：获取遥测。
     * 参数：
     * - `prefix`：`prefix` 参数。
     * - `type`：类型。
     * - `key`：键。
     * 返回：文本结果。
     */
    private static String getTelemetryKey(String prefix, OtaPackageType type, OtaPackageKey key) {
        return prefix + type.getKeyPrefix() + "_" + key.getValue();
    }

    /**
     * 功能：获取遥测。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * 返回：文本结果。
     */
    public static String getTelemetryKey(OtaPackageType type, OtaPackageKey key) {
        return type.getKeyPrefix() + "_" + key.getValue();
    }

    /**
     * 功能：获取`Ota Package Id`。
     * 参数：
     * - `entity`：实体对象。
     * - `type`：类型。
     * 返回：处理结果。
     */
    public static OtaPackageId getOtaPackageId(HasOtaPackage entity, OtaPackageType type) {
        switch (type) {
            case FIRMWARE:
                return entity.getFirmwareId();
            case SOFTWARE:
                return entity.getSoftwareId();
            default:
                log.warn("Unsupported ota package type: [{}]", type);
                return null;
        }
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `firmwareSupplier`：`firmwareSupplier` 参数。
     * - `softwareSupplier`：`softwareSupplier` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    public static <T> T getByOtaPackageType(Supplier<T> firmwareSupplier, Supplier<T> softwareSupplier, OtaPackageType type) {
        switch (type) {
            case FIRMWARE:
                return firmwareSupplier.get();
            case SOFTWARE:
                return softwareSupplier.get();
            default:
                throw new RuntimeException("Unsupported OtaPackage type: " + type);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`OtaPackageUtil` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
