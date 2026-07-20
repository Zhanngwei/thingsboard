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
 * 1. `ResourceUtils` 是 ThingsBoard Common Data 中处理资源通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
