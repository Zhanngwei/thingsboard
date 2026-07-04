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
package org.thingsboard.rule.engine.math;

import lombok.Getter;

/**
 * 中文说明：`TbRuleNodeMathFunctionType` 是规则节点数学函数类型枚举，用于限定解析数学参数、计算结果并可写回消息、属性或时间序列时可选择的固定值。
 * 调用边界：本枚举本身不直接涉及数据库、缓存、Rule Engine、Actor、MQTT 或事务，只作为配置或流程判断的类型值。
 */
public enum TbRuleNodeMathFunctionType {

    /**
     * 枚举项说明：本行枚举常量定义 `TbRuleNodeMathFunctionType` 支持的取值，用于配置或处理流程中的分支判断。
     */
    ADD(2), SUB(2), MULT(2), DIV(2),
    /**
     * 枚举项说明：本行枚举常量定义 `TbRuleNodeMathFunctionType` 支持的取值，用于配置或处理流程中的分支判断。
     */
    SIN, SINH, COS, COSH, TAN, TANH, ACOS, ASIN, ATAN, ATAN2(2),
    /**
     * 枚举项说明：本行枚举常量定义 `TbRuleNodeMathFunctionType` 支持的取值，用于配置或处理流程中的分支判断。
     */
    EXP, EXPM1, SQRT, CBRT, GET_EXP(1, 1, true), HYPOT(2), LOG, LOG10, LOG1P,
    /**
     * 枚举项说明：本行枚举常量定义 `TbRuleNodeMathFunctionType` 支持的取值，用于配置或处理流程中的分支判断。
     */
    CEIL(1, 1, true), FLOOR(1, 1, true), FLOOR_DIV(2), FLOOR_MOD(2),
    /**
     * 枚举项说明：本行枚举常量定义 `TbRuleNodeMathFunctionType` 支持的取值，用于配置或处理流程中的分支判断。
     */
    ABS, MIN(2), MAX(2), POW(2), SIGNUM, RAD, DEG,

    /**
     * 枚举项说明：本行枚举常量定义 `TbRuleNodeMathFunctionType` 支持的取值，用于配置或处理流程中的分支判断。
     */
    CUSTOM(0, 16, false); //Custom function based on exp4j

    @Getter
    /**
     * 字段说明：保存 `minArgs`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final int minArgs;
    @Getter
    /**
     * 字段说明：保存 `maxArgs`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final int maxArgs;
    @Getter
    /**
     * 字段说明：保存 `integerResult`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final boolean integerResult;

    /**
     * 方法说明：构造 `TbRuleNodeMathFunctionType` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    TbRuleNodeMathFunctionType() {
        this(1, 1, false);
    }

    /**
     * 方法说明：构造 `TbRuleNodeMathFunctionType` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    TbRuleNodeMathFunctionType(int args) {
        this(args, args, false);
    }

    /**
     * 方法说明：构造 `TbRuleNodeMathFunctionType` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    TbRuleNodeMathFunctionType(int minArgs, int maxArgs, boolean integerResult) {
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.integerResult = integerResult;
    }

    /*
     * 本类总结：`TbRuleNodeMathFunctionType` 负责解析数学参数、计算结果并可写回消息、属性或时间序列；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
