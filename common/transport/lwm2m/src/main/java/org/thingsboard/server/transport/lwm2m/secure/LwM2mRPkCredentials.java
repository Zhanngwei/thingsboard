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
 * 1. `LwM2mRPkCredentials` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
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
