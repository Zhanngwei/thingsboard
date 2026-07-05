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
     * `ADD`常量，用于统一引用固定值。
     */
    ADD(2), SUB(2), MULT(2), DIV(2),
    /**
     * `ATAN2`常量，用于统一引用固定值。
     */
    SIN, SINH, COS, COSH, TAN, TANH, ACOS, ASIN, ATAN, ATAN2(2),
    /**
     * `GET_EXP`常量，用于统一引用固定值。
     */
    EXP, EXPM1, SQRT, CBRT, GET_EXP(1, 1, true), HYPOT(2), LOG, LOG10, LOG1P,
    /**
     * `CEIL`常量，用于统一引用固定值。
     */
    CEIL(1, 1, true), FLOOR(1, 1, true), FLOOR_DIV(2), FLOOR_MOD(2),
    /**
     * `MIN`常量，用于统一引用固定值。
     */
    ABS, MIN(2), MAX(2), POW(2), SIGNUM, RAD, DEG,

    /**
     * `CUSTOM`常量，用于统一引用固定值。
     */
    CUSTOM(0, 16, false); //Custom function based on exp4j

    /**
     * 参数，表示当前对象的对应属性。
     */
    @Getter
    private final int minArgs;
    /**
     * 参数，表示当前对象的对应属性。
     */
    @Getter
    private final int maxArgs;
    /**
     * 是否满足`integerResult`条件。
     */
    @Getter
    private final boolean integerResult;

    /**
     * 功能：创建 `TbRuleNodeMathFunctionType` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    TbRuleNodeMathFunctionType() {
        this(1, 1, false);
    }

    /**
     * 功能：创建 `TbRuleNodeMathFunctionType` 实例，并初始化必要字段。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：新创建的对象实例。
     */
    TbRuleNodeMathFunctionType(int args) {
        this(args, args, false);
    }

    /**
     * 功能：创建 `TbRuleNodeMathFunctionType` 实例，并初始化必要字段。
     * 参数：
     * - `minArgs`：`minArgs` 参数。
     * - `maxArgs`：`maxArgs` 参数。
     * - `integerResult`：`integerResult` 参数。
     * 返回：新创建的对象实例。
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
