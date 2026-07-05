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
@Slf4j
public final class AzureIotHubUtil {
    private static final String BASE_DIR_PATH = System.getProperty("user.dir");
    /**
     * `APP_DIR`常量，用于统一引用固定值。
     */
    private static final String APP_DIR = "application";
    private static final String SRC_DIR = "src";
    /**
     * `MAIN_DIR`常量，用于统一引用固定值。
     */
    private static final String MAIN_DIR = "main";
    private static final String DATA_DIR = "data";
    /**
     * `CERTS_DIR`常量，用于统一引用固定值。
     */
    private static final String CERTS_DIR = "certs";
    private static final String AZURE_DIR = "azure";
    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String FILE_NAME = "DigiCertGlobalRootG2.crt.pem";

    /**
     * 文件路径常量，用于统一引用固定值。
     */
    private static final Path FULL_FILE_PATH;

    static {
        if (BASE_DIR_PATH.endsWith("bin")) {
            FULL_FILE_PATH = Paths.get(BASE_DIR_PATH.replaceAll("bin$", ""), DATA_DIR, CERTS_DIR, AZURE_DIR, FILE_NAME);
        } else if (BASE_DIR_PATH.endsWith("conf")) {
            FULL_FILE_PATH = Paths.get(BASE_DIR_PATH.replaceAll("conf$", ""), DATA_DIR, CERTS_DIR, AZURE_DIR, FILE_NAME);
        } else {
            FULL_FILE_PATH = Paths.get(BASE_DIR_PATH, APP_DIR, SRC_DIR, MAIN_DIR, DATA_DIR, CERTS_DIR, AZURE_DIR, FILE_NAME);
        }
    }

    /**
     * 令牌常量，用于统一引用固定值。
     */
    private static final long SAS_TOKEN_VALID_SECS = 365 * 24 * 60 * 60;
    private static final long ONE_SECOND_IN_MILLISECONDS = 1000;

    /**
     * 令牌常量，用于统一引用固定值。
     */
    private static final String SAS_TOKEN_FORMAT = "SharedAccessSignature sr=%s&sig=%s&se=%s";

    /**
     * 用户名常量，用于统一引用固定值。
     */
    private static final String USERNAME_FORMAT = "%s/%s/?api-version=2018-06-30";

    /**
     * 功能：创建 `AzureIotHubUtil` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private AzureIotHubUtil() {
    }

    /**
     * 功能：构建用户名。
     * 参数：
     * - `host`：`host` 参数。
     * - `deviceId`：设备IDID。
     * 返回：文本结果。
     */
    public static String buildUsername(String host, String deviceId) {
        return String.format(USERNAME_FORMAT, host, deviceId);
    }

    /**
     * 功能：构建令牌。
     * 参数：
     * - `host`：`host` 参数。
     * - `sasKey`：键。
     * 返回：文本结果。
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to build SAS token!!!", e);
        }
    }

    /**
     * 功能：构建`Expires On`。
     * 参数：无。
     * 返回：数值结果。
     */
    private static long buildExpiresOn() {
        long expiresOnDate = System.currentTimeMillis();
        expiresOnDate += SAS_TOKEN_VALID_SECS * ONE_SECOND_IN_MILLISECONDS;
        return expiresOnDate / ONE_SECOND_IN_MILLISECONDS;
    }

    /**
     * 功能：获取`Default Ca Cert`。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String getDefaultCaCert() {
        byte[] fileBytes;
        if (Files.exists(FULL_FILE_PATH)) {
            try {
                fileBytes = Files.readAllBytes(FULL_FILE_PATH);
            } catch (IOException e) {
                log.error("Failed to load Default CaCert file!!! [{}]", FULL_FILE_PATH, e);
                throw new RuntimeException("Failed to load Default CaCert file!!!");
            }
        } else {
            Path azureDirectory = FULL_FILE_PATH.getParent();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(azureDirectory)) {
                Iterator<Path> iterator = stream.iterator();
                if (iterator.hasNext()) {
                    Path firstFile = iterator.next();
                    fileBytes = Files.readAllBytes(firstFile);
                } else {
                    log.error("Default CaCert file not found in the directory [{}]!!!", azureDirectory);
                    throw new RuntimeException("Default CaCert file not found in the directory!!!");
                }
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
