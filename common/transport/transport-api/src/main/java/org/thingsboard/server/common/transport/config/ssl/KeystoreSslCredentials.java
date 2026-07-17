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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * 中文说明：
 * 1. `KeystoreSslCredentials` 是 ThingsBoard Common Transport 中负责 `Keystore Ssl Credentials` 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `AbstractSslCredentials`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class KeystoreSslCredentials extends AbstractSslCredentials {

    /**
     * 类型，用于区分不同处理分支。
     */
    private String type;
    private String storeFile;
    /**
     * 密码，用于认证或安全校验。
     */
    private String storePassword;
    private String keyPassword;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private String keyAlias;

    /**
     * 功能：执行 `canUse` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    protected boolean canUse() {
        return ResourceUtils.resourceExists(this, this.storeFile);
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `trustsOnly`：`trustsOnly` 参数。
     * - `keyPasswordArray`：键。
     * 返回：处理结果。
     */
    @Override
    protected KeyStore loadKeyStore(boolean trustsOnly, char[] keyPasswordArray) throws IOException, GeneralSecurityException {
        String keyStoreType = StringUtils.isEmpty(this.type) ? KeyStore.getDefaultType() : this.type;
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (InputStream tsFileInputStream = ResourceUtils.getInputStream(this, this.storeFile)) {
            keyStore.load(tsFileInputStream, StringUtils.isEmpty(this.storePassword) ? new char[0] : this.storePassword.toCharArray());
        }
        return keyStore;
    }

    /**
     * 功能：更新键。
     * 参数：
     * - `keyAlias`：键。
     * 返回：无。
     */
    @Override
    protected void updateKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }
}
