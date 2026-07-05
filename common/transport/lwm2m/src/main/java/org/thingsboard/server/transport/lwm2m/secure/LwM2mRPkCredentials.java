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
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mRPkCredentials` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Data
public class LwM2mRPkCredentials {
    /**
     * 公钥，用于定位映射、配置或数据项。
     */
    private PublicKey serverPublicKey;
    private PrivateKey serverPrivateKey;
    /**
     * 证书，用于认证或安全校验。
     */
    private X509Certificate certificate;
    private List<Certificate> trustStore;

    /**
     * create All key RPK credentials
     * @param publX
     * @param publY
     * @param privS
     */
    /**
     * 功能：创建 `LwM2mRPkCredentials` 实例，并初始化必要字段。
     * 参数：
     * - `publX`：`publX` 参数。
     * - `publY`：`publY` 参数。
     * - `privS`：`privS` 参数。
     * 返回：新创建的对象实例。
     */
    public LwM2mRPkCredentials(String publX, String publY, String privS) {
        generatePublicKeyRPK(publX, publY, privS);
    }

    /**
     * 功能：执行 `generatePublicKeyRPK` 对应的处理。
     * 参数：
     * - `publX`：`publX` 参数。
     * - `publY`：`publY` 参数。
     * - `privS`：`privS` 参数。
     * 返回：无。
     */
    private void generatePublicKeyRPK(String publX, String publY, String privS) {
        try {
            /*Get Elliptic Curve Parameter spec for secp256r1 */
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
             if (publX != null && !publX.isEmpty() && publY != null && !publY.isEmpty()) {
                // Get point values
                byte[] publicX = Hex.decodeHex(publX.toCharArray());
                byte[] publicY = Hex.decodeHex(publY.toCharArray());
                 /* Create key specs */
                KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                        parameterSpec);
                 /* Get keys */
                this.serverPublicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            }
            if (privS != null && !privS.isEmpty()) {
                /* Get point values */
                byte[] privateS = Hex.decodeHex(privS.toCharArray());
                /* Create key specs */
                KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);
                /* Get keys */
                this.serverPrivateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("[{}] Failed generate Server KeyRPK", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mRPkCredentials` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
