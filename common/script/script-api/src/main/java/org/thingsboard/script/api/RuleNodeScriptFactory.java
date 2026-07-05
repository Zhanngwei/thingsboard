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
 * 1. 类目的：`RuleNodeScriptFactory` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`RuleNodeScriptFactory` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
