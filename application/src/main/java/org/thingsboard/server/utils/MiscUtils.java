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
package org.thingsboard.server.utils;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;


/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`MiscUtils` 是ThingsBoard Application 模块中的应用服务支撑类型，用于承载服务端运行期的数据、依赖或流程控制。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 生命周期：由 Spring 容器、Actor System、Web 请求或队列消费流程管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO/Helper。
 */
public class MiscUtils {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * 功能：执行 `missingProperty` 对应的处理。
     * 参数：
     * - `propertyName`：名称。
     * 返回：文本结果。
     */
    public static String missingProperty(String propertyName) {
        return "The " + propertyName + " property need to be set!";
    }

    /**
     * 功能：执行 `forName` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    @SuppressWarnings("deprecation")
    public static HashFunction forName(String name) {
        switch (name) {
            case "murmur3_32":
                return Hashing.murmur3_32();
            case "murmur3_128":
                return Hashing.murmur3_128();
            case "crc32":
                return Hashing.crc32();
            case "md5":
                return Hashing.md5();
            default:
                throw new IllegalArgumentException("Can't find hash function with name " + name);
        }
    }

    /**
     * 功能：执行 `constructBaseUrl` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    public static String constructBaseUrl(HttpServletRequest request) {
        return String.format("%s://%s:%d",
                getScheme(request),
                getDomainName(request),
                getPort(request));
    }

    /**
     * 功能：获取`Scheme`。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    public static String getScheme(HttpServletRequest request){
        String scheme = request.getScheme();
        String forwardedProto = request.getHeader("x-forwarded-proto");
        if (forwardedProto != null) {
            scheme = forwardedProto;
        }
        return scheme;
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    public static String getDomainName(HttpServletRequest request){
        return request.getServerName();
    }

    /**
     * 功能：获取端口号。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    public static String getDomainNameAndPort(HttpServletRequest request){
        String domainName = getDomainName(request);
        String scheme = getScheme(request);
        int port = MiscUtils.getPort(request);
        if (needsPort(scheme, port)) {
            domainName += ":" + port;
        }
        return domainName;
    }

    /**
     * 功能：执行 `needsPort` 对应的处理。
     * 参数：
     * - `scheme`：`scheme` 参数。
     * - `port`：`port` 参数。
     * 返回：判断结果。
     */
    private static boolean needsPort(String scheme, int port) {
        boolean isHttpDefault = "http".equals(scheme.toLowerCase()) && port == 80;
        boolean isHttpsDefault = "https".equals(scheme.toLowerCase()) && port == 443;
        return !isHttpDefault && !isHttpsDefault;
    }

    /**
     * 功能：获取端口号。
     * 参数：
     * - `request`：请求对象。
     * 返回：数值结果。
     */
    public static int getPort(HttpServletRequest request){
        String forwardedProto = request.getHeader("x-forwarded-proto");

        int serverPort = request.getServerPort();
        if (request.getHeader("x-forwarded-port") != null) {
            try {
                serverPort = request.getIntHeader("x-forwarded-port");
            } catch (NumberFormatException e) {
            }
        } else if (forwardedProto != null) {
            switch (forwardedProto) {
                case "http":
                    serverPort = 80;
                    break;
                case "https":
                    serverPort = 443;
                    break;
            }
        }
        return serverPort;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MiscUtils` 在 ThingsBoard Application 模块 中承担应用服务支撑类型职责，核心目的是承载服务端运行期的数据、依赖或流程控制。
 * 2. 核心流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
