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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides utility methods for handling Sparkplug MQTT message topics.
 */
/**
 * 中文说明：
 * 1. 类目的：`SparkplugTopicUtil` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class SparkplugTopicUtil {

    private static final Map<String, String[]> SPLIT_TOPIC_CACHE = new HashMap<String, String[]>();
    /**
     * 主题常量，用于统一引用固定值。
     */
    private static final String TOPIC_INVALID_NUMBER = "Invalid number of topic elements: ";
    public static final String NAMESPACE = "spBv1.0";

    /**
     * 功能：获取主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    public static String[] getSplitTopic(String topic) {
        String[] splitTopic = SPLIT_TOPIC_CACHE.get(topic);
        if (splitTopic == null) {
            splitTopic = topic.split("/");
            SPLIT_TOPIC_CACHE.put(topic, splitTopic);
        }

        return splitTopic;
    }

    /**
     * Serializes a {@link SparkplugTopic} instance in to a JSON string.
     *
     * @param topic a {@link SparkplugTopic} instance
     * @return a JSON string
     * @throws JsonProcessingException
     */
    /**
     * 功能：执行 `sparkplugTopicToString` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：文本结果。
     */
    public static String sparkplugTopicToString(SparkplugTopic topic) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(topic);
    }

    /**
     * Parses a Sparkplug MQTT message topic string and returns a {@link SparkplugTopic} instance.
     *
     * @param topic a topic string
     * @return a {@link SparkplugTopic} instance
     * @throws ThingsboardException if an error occurs while parsing
     */
    /**
     * 功能：解析主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    public static SparkplugTopic parseTopicSubscribe(String topic) throws ThingsboardException {
        // TODO "+", "$"
        topic = topic.indexOf("#") > 0 ? topic.substring(0, topic.indexOf("#")) : topic;
        return parseTopic(SparkplugTopicUtil.getSplitTopic(topic));
    }

    /**
     * 功能：解析主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    public static SparkplugTopic parseTopicPublish(String topic) throws ThingsboardException {
        if (topic.contains("#") || topic.contains("$") || topic.contains("+")) {
            throw new ThingsboardException("Invalid of topic elements for Publish", ThingsboardErrorCode.INVALID_ARGUMENTS);
        } else {
            String[] splitTopic = SparkplugTopicUtil.getSplitTopic(topic);
            if (splitTopic.length < 4 || splitTopic.length > 5) {
                throw new ThingsboardException(TOPIC_INVALID_NUMBER + splitTopic.length, ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            return parseTopic(splitTopic);
        }
    }

    /**
     * Parses a Sparkplug MQTT message topic string and returns a {@link SparkplugTopic} instance.
     *
     * @param splitTopic a topic split into tokens
     * @return a {@link SparkplugTopic} instance
     * @throws Exception if an error occurs while parsing
     */
    /**
     * 功能：解析主题。
     * 参数：
     * - `splitTopic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    @SuppressWarnings("incomplete-switch")
    public static SparkplugTopic parseTopic(String[] splitTopic) throws ThingsboardException {
        int length = splitTopic.length;
        if (length == 0) {
			throw new ThingsboardException(TOPIC_INVALID_NUMBER + length, ThingsboardErrorCode.INVALID_ARGUMENTS);
        } else {
            SparkplugMessageType type;
            String namespace, edgeNodeId, groupId, deviceId;
            namespace = validateNameSpace(splitTopic[0]);
            groupId = length > 1 ? splitTopic[1] : null;
            type = length > 2 ? SparkplugMessageType.parseMessageType(splitTopic[2]) : null;
            edgeNodeId = length > 3 ? splitTopic[3] : null;
			deviceId = length > 4 ? splitTopic[4] : null;
			return new SparkplugTopic(namespace, groupId, edgeNodeId, deviceId, type);
        }
    }

    /**
     * For the Sparkplug™ B version of the specification, the UTF-8 string constant for the namespace element will be: "spBv1.0"
     * @param nameSpace
     * @return
     */
    /**
     * 功能：校验名称。
     * 参数：
     * - `nameSpace`：名称。
     * 返回：判断结果。
     */
    private static String validateNameSpace(String nameSpace)  throws ThingsboardException {
        if (NAMESPACE.equals(nameSpace)) return nameSpace;
        throw new ThingsboardException("The namespace [" + nameSpace + "] is not valid and must be [" + NAMESPACE + "] for the Sparkplug™ B version.", ThingsboardErrorCode.INVALID_ARGUMENTS);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SparkplugTopicUtil` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
