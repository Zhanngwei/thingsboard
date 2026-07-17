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
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

/**
 * 中文说明：
 * 1. `SslCredentialsConfig` 是 ThingsBoard Common Transport 中描述 `Ssl Credentials` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Slf4j
@Data
public class SslCredentialsConfig {

    /**
     * 是否启用`enabled`。
     */
    private boolean enabled = true;
    private SslCredentialsType type;
    /**
     * `pem` 字段，保存当前对象的对应属性。
     */
    private PemSslCredentials pem;
    private KeystoreSslCredentials keystore;

    /**
     * 凭据，用于认证或安全校验。
     */
    private SslCredentials credentials;

    /**
     * 名称，用于标识或展示当前对象。
     */
    private final String name;
    private final boolean trustsOnly;

    /**
     * 功能：创建 `SslCredentialsConfig` 实例，并初始化必要字段。
     * 参数：
     * - `name`：名称。
     * - `trustsOnly`：`trustsOnly` 参数。
     * 返回：新创建的对象实例。
     */
    public SslCredentialsConfig(String name, boolean trustsOnly) {
        this.name = name;
        this.trustsOnly = trustsOnly;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        if (this.enabled) {
            log.info("{}: Initializing SSL credentials.", name);
            if (SslCredentialsType.PEM.equals(type) && pem.canUse()) {
                this.credentials = this.pem;
            } else if (keystore.canUse()) {
                if (SslCredentialsType.PEM.equals(type)) {
                    log.warn("{}: Specified PEM configuration is not valid. Using SSL keystore configuration as fallback.", name);
                }
                this.credentials = this.keystore;
            } else {
                throw new RuntimeException(name + ": Invalid SSL credentials configuration. None of the PEM or KEYSTORE configurations can be used!");
            }
            try {
                this.credentials.init(this.trustsOnly);
            } catch (Exception e) {
                throw new RuntimeException(name + ": Failed to init SSL credentials configuration.", e);
            }
        } else {
            log.info("{}: Skipping initialization of disabled SSL credentials.", name);
        }
    }

}
