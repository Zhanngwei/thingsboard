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
package org.thingsboard.script.api;

/**
 * 中文说明：
 * 1. `RuleNodeScriptFactory` 是 ThingsBoard Common 中创建或提供规则节点对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 它直接协作于目标接口、具体实现和创建所需配置。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
public class RuleNodeScriptFactory {

    /**
     * 消息常量，用于统一引用固定值。
     */
    public static final String MSG = "msg";
    public static final String METADATA = "metadata";
    /**
     * 消息常量，用于统一引用固定值。
     */
    public static final String MSG_TYPE = "msgType";
    public static final String RULE_NODE_FUNCTION_NAME = "ruleNodeFunc";

    private static final String JS_WRAPPER_PREFIX_TEMPLATE = "function %s(msgStr, metadataStr, msgType) { " +
            "    var msg = JSON.parse(msgStr); " +
            "    var metadata = JSON.parse(metadataStr); " +
            "    return JSON.stringify(%s(msg, metadata, msgType));" +
            "    function %s(%s, %s, %s) {";
    private static final String JS_WRAPPER_SUFFIX = "\n}" +
            "\n}";


    /**
     * 功能：执行 `generateRuleNodeScript` 对应的处理。
     * 参数：
     * - `functionName`：名称。
     * - `scriptBody`：`scriptBody` 参数。
     * - `argNames`：名称。
     * 返回：文本结果。
     */
    public static String generateRuleNodeScript(String functionName, String scriptBody, String... argNames) {
        String msgArg;
        String metadataArg;
        String msgTypeArg;
        if (argNames != null && argNames.length == 3) {
            msgArg = argNames[0];
            metadataArg = argNames[1];
            msgTypeArg = argNames[2];
        } else {
            msgArg = MSG;
            metadataArg = METADATA;
            msgTypeArg = MSG_TYPE;
        }
        String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, functionName,
                RULE_NODE_FUNCTION_NAME, RULE_NODE_FUNCTION_NAME, msgArg, metadataArg, msgTypeArg);
        return jsWrapperPrefix + scriptBody + JS_WRAPPER_SUFFIX;
    }

}
