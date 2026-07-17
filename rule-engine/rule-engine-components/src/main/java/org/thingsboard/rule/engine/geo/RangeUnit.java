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
 * 中文说明：
 * 1. `RangeUnit` 是 ThingsBoard Rule Engine Components 中定义 `Range Unit` 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
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
}
