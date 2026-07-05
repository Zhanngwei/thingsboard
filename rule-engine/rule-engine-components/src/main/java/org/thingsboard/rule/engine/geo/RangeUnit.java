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
package org.thingsboard.rule.engine.geo;

/**
 * 中文说明：`RangeUnit` 是距离单位枚举，用于限定执行 GPS 地理围栏、距离和多边形判断及状态跟踪时可选择的固定值。
 * 调用边界：本枚举本身不直接涉及数据库、缓存、Rule Engine、Actor、MQTT 或事务，只作为配置或流程判断的类型值。
 */
public enum RangeUnit {
    /**
     * `METER`常量，用于统一引用固定值。
     */
    METER(1000.0), KILOMETER(1.0), FOOT(3280.84), MILE(0.62137), NAUTICAL_MILE(0.539957);

    /**
     * `fromKm` 字段，保存当前对象的对应属性。
     */
    private final double fromKm;

    /**
     * 功能：创建 `RangeUnit` 实例，并初始化必要字段。
     * 参数：
     * - `fromKm`：`fromKm` 参数。
     * 返回：新创建的对象实例。
     */
    RangeUnit(double fromKm) {
        this.fromKm = fromKm;
    }

    /**
     * 功能：执行 `fromKm` 对应的处理。
     * 参数：
     * - `v`：`v` 参数。
     * 返回：数值结果。
     */
    public double fromKm(double v) {
        return v * fromKm;
    }
    /*
     * 本类总结：`RangeUnit` 负责执行 GPS 地理围栏、距离和多边形判断及状态跟踪；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
