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
package org.thingsboard.server.common.data;

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * 中文说明：
 * 1. 类目的：`ResourceUtils` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Slf4j
public class ResourceUtils {

    /**
     * URL 地址常量，用于统一引用固定值。
     */
    public static final String CLASSPATH_URL_PREFIX = "classpath:";

    /**
     * 功能：执行 `resourceExists` 对应的处理。
     * 参数：
     * - `classLoaderSource`：`classLoaderSource` 参数。
     * - `filePath`：文件或资源路径。
     * 返回：判断结果。
     */
    public static boolean resourceExists(Object classLoaderSource, String filePath) {
        return resourceExists(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    /**
     * 功能：执行 `resourceExists` 对应的处理。
     * 参数：
     * - `classLoader`：`classLoader` 参数。
     * - `filePath`：文件或资源路径。
     * 返回：判断结果。
     */
    public static boolean resourceExists(ClassLoader classLoader, String filePath) {
        boolean classPathResource = false;
        String path = filePath;
        if (path.startsWith(CLASSPATH_URL_PREFIX)) {
            path = path.substring(CLASSPATH_URL_PREFIX.length());
            classPathResource = true;
        }
        if (!classPathResource) {
            File resourceFile = new File(path);
            if (resourceFile.exists()) {
                return true;
            }
        }
        InputStream classPathStream = classLoader.getResourceAsStream(path);
        if (classPathStream != null) {
            return true;
        } else {
            try {
                URL url = Resources.getResource(path);
                if (url != null) {
                    return true;
                }
            } catch (IllegalArgumentException e) {}
        }
        return false;
    }

    /**
     * 功能：获取`Input Stream`。
     * 参数：
     * - `classLoaderSource`：`classLoaderSource` 参数。
     * - `filePath`：文件或资源路径。
     * 返回：处理结果。
     */
    public static InputStream getInputStream(Object classLoaderSource, String filePath) {
        return getInputStream(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    /**
     * 功能：获取`Input Stream`。
     * 参数：
     * - `classLoader`：`classLoader` 参数。
     * - `filePath`：文件或资源路径。
     * 返回：处理结果。
     */
    public static InputStream getInputStream(ClassLoader classLoader, String filePath) {
        boolean classPathResource = false;
        String path = filePath;
        if (path.startsWith(CLASSPATH_URL_PREFIX)) {
            path = path.substring(CLASSPATH_URL_PREFIX.length());
            classPathResource = true;
        }
        try {
            if (!classPathResource) {
                File resourceFile = new File(path);
                if (resourceFile.exists()) {
                    log.info("Reading resource data from file {}", filePath);
                    return new FileInputStream(resourceFile);
                }
            }
            InputStream classPathStream = classLoader.getResourceAsStream(path);
            if (classPathStream != null) {
                log.info("Reading resource data from class path {}", filePath);
                return classPathStream;
            } else {
                URL url = Resources.getResource(path);
                if (url != null) {
                    URI uri = url.toURI();
                    log.info("Reading resource data from URI {}", filePath);
                    return new FileInputStream(new File(uri));
                }
            }
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                log.warn("Unable to find resource: " + filePath);
            } else {
                log.warn("Unable to find resource: " + filePath, e);
            }
        }
        throw new RuntimeException("Unable to find resource: " + filePath);
    }

    /**
     * 功能：获取URI 地址。
     * 参数：
     * - `classLoaderSource`：`classLoaderSource` 参数。
     * - `filePath`：文件或资源路径。
     * 返回：文本结果。
     */
    public static String getUri(Object classLoaderSource, String filePath) {
        return getUri(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    /**
     * 功能：获取URI 地址。
     * 参数：
     * - `classLoader`：`classLoader` 参数。
     * - `filePath`：文件或资源路径。
     * 返回：文本结果。
     */
    public static String getUri(ClassLoader classLoader, String filePath) {
        try {
            File resourceFile = new File(filePath);
            if (resourceFile.exists()) {
                log.info("Reading resource data from file {}", filePath);
                return resourceFile.getAbsolutePath();
            } else {
                URL url = classLoader.getResource(filePath);
                return url.toURI().toString();
            }
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                log.warn("Unable to find resource: " + filePath);
            } else {
                log.warn("Unable to find resource: " + filePath, e);
            }
            throw new RuntimeException("Unable to find resource: " + filePath);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ResourceUtils` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
