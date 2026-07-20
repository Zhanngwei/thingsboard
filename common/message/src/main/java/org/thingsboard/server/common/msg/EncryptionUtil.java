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
package org.thingsboard.server.common.msg;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

/**
 * @author Valerii Sosliuk
 */
/**
 * 中文说明：
 * 1. `EncryptionUtil` 是 ThingsBoard Common Message 中处理 `Encryption Util` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class EncryptionUtil {

    /**
     * 功能：创建 `EncryptionUtil` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private EncryptionUtil() {
    }

    /**
     * 功能：执行 `certTrimNewLines` 对应的处理。
     * 参数：
     * - `input`：`input` 参数。
     * 返回：文本结果。
     */
    public static String certTrimNewLines(String input) {
        return input.replaceAll("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("\n", "")
                .replaceAll("\r", "")
                .replaceAll("-----END CERTIFICATE-----", "");
    }

    /**
     * 功能：执行 `certTrimNewLinesForChainInDeviceProfile` 对应的处理。
     * 参数：
     * - `input`：`input` 参数。
     * 返回：文本结果。
     */
    public static String certTrimNewLinesForChainInDeviceProfile(String input) {
        return input.replaceAll("\n", "")
                .replaceAll("\r", "")
                .replaceAll("-----BEGIN CERTIFICATE-----", "-----BEGIN CERTIFICATE-----\n")
                .replaceAll("-----END CERTIFICATE-----", "\n-----END CERTIFICATE-----\n")
                .trim();
    }

    /**
     * 功能：执行 `pubkTrimNewLines` 对应的处理。
     * 参数：
     * - `input`：`input` 参数。
     * 返回：文本结果。
     */
    public static String pubkTrimNewLines(String input) {
        return input.replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("\n", "")
                .replaceAll("\r", "")
                .replaceAll("-----END PUBLIC KEY-----", "");
    }

    /**
     * 功能：执行 `prikTrimNewLines` 对应的处理。
     * 参数：
     * - `input`：`input` 参数。
     * 返回：文本结果。
     */
    public static String prikTrimNewLines(String input) {
        return input.replaceAll("-----BEGIN EC PRIVATE KEY-----", "")
                .replaceAll("\n", "")
                .replaceAll("\r", "")
                .replaceAll("-----END EC PRIVATE KEY-----", "");
    }


    /**
     * 功能：获取`Sha3 Hash`。
     * 参数：
     * - `data`：待处理数据。
     * 返回：文本结果。
     */
    public static String getSha3Hash(String data) {
        String trimmedData = certTrimNewLines(data);
        byte[] dataBytes = trimmedData.getBytes();
        SHA3Digest md = new SHA3Digest(256);
        md.reset();
        md.update(dataBytes, 0, dataBytes.length);
        byte[] hashedBytes = new byte[256 / 8];
        md.doFinal(hashedBytes, 0);
        String sha3Hash = ByteUtils.toHexString(hashedBytes);
        return sha3Hash;
    }

    /**
     * 功能：获取`Sha3 Hash`。
     * 参数：
     * - `delim`：`delim` 参数。
     * - `tokens`：`tokens` 参数。
     * 返回：文本结果。
     */
    public static String getSha3Hash(String delim, String... tokens) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String token : tokens) {
            if (token != null && !token.isEmpty()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(delim);
                }
                sb.append(token);
            }
        }
        return getSha3Hash(sb.toString());
    }
}
