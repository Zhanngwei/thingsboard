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
package org.thingsboard.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AzureIotHubUtil` 是ThingsBoard Common 模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
 */
public final class AzureIotHubUtil {
    private static final String BASE_DIR_PATH = System.getProperty("user.dir");
    /**
     * 字段说明：
     * 1. 保存 `APP_DIR` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String APP_DIR = "application";
    private static final String SRC_DIR = "src";
    /**
     * 字段说明：
     * 1. 保存 `MAIN_DIR` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String MAIN_DIR = "main";
    private static final String DATA_DIR = "data";
    /**
     * 字段说明：
     * 1. 保存 `CERTS_DIR` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String CERTS_DIR = "certs";
    private static final String AZURE_DIR = "azure";
    /**
     * 字段说明：
     * 1. 保存 `FILE_NAME` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String FILE_NAME = "DigiCertGlobalRootG2.crt.pem";

    /**
     * 字段说明：
     * 1. 保存 `FULL_FILE_PATH` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final Path FULL_FILE_PATH;

    static {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (BASE_DIR_PATH.endsWith("bin")) {
            FULL_FILE_PATH = Paths.get(BASE_DIR_PATH.replaceAll("bin$", ""), DATA_DIR, CERTS_DIR, AZURE_DIR, FILE_NAME);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (BASE_DIR_PATH.endsWith("conf")) {
            FULL_FILE_PATH = Paths.get(BASE_DIR_PATH.replaceAll("conf$", ""), DATA_DIR, CERTS_DIR, AZURE_DIR, FILE_NAME);
        } else {
            FULL_FILE_PATH = Paths.get(BASE_DIR_PATH, APP_DIR, SRC_DIR, MAIN_DIR, DATA_DIR, CERTS_DIR, AZURE_DIR, FILE_NAME);
        }
    }

    /**
     * 字段说明：
     * 1. 保存 `SAS_TOKEN_VALID_SECS` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final long SAS_TOKEN_VALID_SECS = 365 * 24 * 60 * 60;
    private static final long ONE_SECOND_IN_MILLISECONDS = 1000;

    /**
     * 字段说明：
     * 1. 保存 `SAS_TOKEN_FORMAT` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String SAS_TOKEN_FORMAT = "SharedAccessSignature sr=%s&sig=%s&se=%s";

    /**
     * 字段说明：
     * 1. 保存 `USERNAME_FORMAT` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String USERNAME_FORMAT = "%s/%s/?api-version=2018-06-30";

    /**
     * 方法说明：
     * 1. 职责：执行 `AzureIotHubUtil` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private AzureIotHubUtil() {
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `buildUsername` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static String buildUsername(String host, String deviceId) {
        return String.format(USERNAME_FORMAT, host, deviceId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `buildSasToken` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static String buildSasToken(String host, String sasKey) {
        try {
            final String targetUri = URLEncoder.encode(host.toLowerCase(), "UTF-8");
            final long expiryTime = buildExpiresOn();
            String toSign = targetUri + "\n" + expiryTime;
            byte[] keyBytes = Base64.getDecoder().decode(sasKey.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
            String signature = URLEncoder.encode(Base64.getEncoder().encodeToString(rawHmac), "UTF-8");
            return String.format(SAS_TOKEN_FORMAT, targetUri, signature, expiryTime);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            throw new RuntimeException("Failed to build SAS token!!!", e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `buildExpiresOn` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static long buildExpiresOn() {
        long expiresOnDate = System.currentTimeMillis();
        expiresOnDate += SAS_TOKEN_VALID_SECS * ONE_SECOND_IN_MILLISECONDS;
        return expiresOnDate / ONE_SECOND_IN_MILLISECONDS;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getDefaultCaCert` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static String getDefaultCaCert() {
        byte[] fileBytes;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (Files.exists(FULL_FILE_PATH)) {
            try {
                fileBytes = Files.readAllBytes(FULL_FILE_PATH);
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (IOException e) {
                log.error("Failed to load Default CaCert file!!! [{}]", FULL_FILE_PATH, e);
                throw new RuntimeException("Failed to load Default CaCert file!!!");
            }
        } else {
            Path azureDirectory = FULL_FILE_PATH.getParent();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(azureDirectory)) {
                Iterator<Path> iterator = stream.iterator();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (iterator.hasNext()) {
                    Path firstFile = iterator.next();
                    fileBytes = Files.readAllBytes(firstFile);
                } else {
                    log.error("Default CaCert file not found in the directory [{}]!!!", azureDirectory);
                    throw new RuntimeException("Default CaCert file not found in the directory!!!");
                }
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (IOException e) {
                log.error("Failed to load Default CaCert file from the directory [{}]!!!", azureDirectory, e);
                throw new RuntimeException("Failed to load Default CaCert file from the directory!!!");
            }
        }
        return new String(fileBytes);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AzureIotHubUtil` 在 ThingsBoard Common 模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
