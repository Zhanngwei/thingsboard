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
package org.thingsboard.server.queue.rabbitmq;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. 类目的：`TbRabbitMqQueueArguments` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
public class TbRabbitMqQueueArguments {
    /**
     * `coreProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.rabbitmq.queue-properties.core:}")
    private String coreProperties;
    /**
     * 规则引擎，表示当前对象的对应属性。
     */
    @Value("${queue.rabbitmq.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    /**
     * 传输层，表示当前对象的对应属性。
     */
    @Value("${queue.rabbitmq.queue-properties.transport-api:}")
    private String transportApiProperties;
    /**
     * `notificationsProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.rabbitmq.queue-properties.notifications:}")
    private String notificationsProperties;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Value("${queue.rabbitmq.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    /**
     * `vcProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.rabbitmq.queue-properties.version-control:}")
    private String vcProperties;

    /**
     * 参数映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> coreArgs;
    /**
     * 规则引擎映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> ruleEngineArgs;
    /**
     * 参数映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> transportApiArgs;
    /**
     * 参数映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> notificationsArgs;
    /**
     * 参数映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> jsExecutorArgs;
    /**
     * 参数映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> vcArgs;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        coreArgs = getArgs(coreProperties);
        ruleEngineArgs = getArgs(ruleEngineProperties);
        transportApiArgs = getArgs(transportApiProperties);
        notificationsArgs = getArgs(notificationsProperties);
        jsExecutorArgs = getArgs(jsExecutorProperties);
        vcArgs = getArgs(vcProperties);
    }

    /**
     * 功能：获取参数。
     * 参数：
     * - `properties`：`properties` 参数。
     * 返回：处理结果。
     */
    public static Map<String, Object> getArgs(String properties) {
        Map<String, Object> configs = new HashMap<>();
        if (StringUtils.isNotEmpty(properties)) {
            for (String property : properties.split(";")) {
                int delimiterPosition = property.indexOf(":");
                String key = property.substring(0, delimiterPosition);
                String strValue = property.substring(delimiterPosition + 1);
                configs.put(key, getObjectValue(strValue));
            }
        }
        return configs;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `str`：`str` 参数。
     * 返回：处理结果。
     */
    private static Object getObjectValue(String str) {
        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
            return Boolean.valueOf(str);
        } else if (isNumeric(str)) {
            return getNumericValue(str);
        }
        return str;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `str`：`str` 参数。
     * 返回：处理结果。
     */
    private static Object getNumericValue(String str) {
        if (str.contains(".")) {
            return Double.valueOf(str);
        } else {
            return Long.valueOf(str);
        }
    }

    private static final Pattern PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    /**
     * 功能：判断`Numeric`。
     * 参数：
     * - `strNum`：`strNum` 参数。
     * 返回：判断结果。
     */
    private static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        return PATTERN.matcher(strNum).matches();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbRabbitMqQueueArguments` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
