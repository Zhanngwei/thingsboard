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
     * 枚举项说明：本行枚举常量定义 `RangeUnit` 支持的取值，用于配置或处理流程中的分支判断。
     */
    METER(1000.0), KILOMETER(1.0), FOOT(3280.84), MILE(0.62137), NAUTICAL_MILE(0.539957);

    /**
     * 字段说明：保存 `fromKm`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final double fromKm;

    /**
     * 方法说明：构造 `RangeUnit` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    RangeUnit(double fromKm) {
        this.fromKm = fromKm;
    }

    /**
     * 方法说明：执行 `fromKm` 对应的辅助逻辑，供 `RangeUnit` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public double fromKm(double v) {
        return v * fromKm;
    }
    /*
     * 本类总结：`RangeUnit` 负责执行 GPS 地理围栏、距离和多边形判断及状态跟踪；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
