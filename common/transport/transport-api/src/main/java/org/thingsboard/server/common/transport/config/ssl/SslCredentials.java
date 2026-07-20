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
package org.thingsboard.server.common.transport.config.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

/**
 * 中文说明：
 * 1. `SslCredentials` 是 ThingsBoard Common Transport 中定义 `Ssl Credentials` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface SslCredentials {

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `trustsOnly`：`trustsOnly` 参数。
     * 返回：无。
     */
    void init(boolean trustsOnly) throws IOException, GeneralSecurityException;

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：处理结果。
     */
    KeyStore getKeyStore();

    /**
     * 功能：获取密码。
     * 参数：无。
     * 返回：文本结果。
     */
    String getKeyPassword();

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    String getKeyAlias();

    /**
     * 功能：获取私钥。
     * 参数：无。
     * 返回：处理结果。
     */
    PrivateKey getPrivateKey();

    /**
     * 功能：获取公钥。
     * 参数：无。
     * 返回：处理结果。
     */
    PublicKey getPublicKey();

    /**
     * 功能：获取证书。
     * 参数：无。
     * 返回：处理结果。
     */
    X509Certificate[] getCertificateChain();

    /**
     * 功能：获取`Trusted Certificates`。
     * 参数：无。
     * 返回：处理结果。
     */
    X509Certificate[] getTrustedCertificates();

    /**
     * 功能：保存或创建工厂。
     * 参数：无。
     * 返回：处理结果。
     */
    TrustManagerFactory createTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException;

    /**
     * 功能：保存或创建键。
     * 参数：无。
     * 返回：处理结果。
     */
    KeyManagerFactory createKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException;

    /**
     * 功能：获取键。
     * 参数：
     * - `subjectName`：名称。
     * - `key`：键。
     * 返回：文本结果。
     */
    String getValueFromSubjectNameByKey(String subjectName, String key);
}
