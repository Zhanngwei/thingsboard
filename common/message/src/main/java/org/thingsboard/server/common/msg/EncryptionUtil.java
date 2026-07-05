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
 * 1. 类目的：`EncryptionUtil` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`EncryptionUtil` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
