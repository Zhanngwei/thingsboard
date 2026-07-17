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
 * 1. `AzureIotHubUtil` 是 ThingsBoard Common 中处理 `Azure Iot Hub` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
