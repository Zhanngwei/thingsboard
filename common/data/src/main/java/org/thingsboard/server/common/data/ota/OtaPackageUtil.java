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
 * 1. `OtaPackageUtil` 是 ThingsBoard Common Data 中处理 OTA 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
