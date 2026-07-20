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
package org.thingsboard.server.common.data.sync.vc;

import lombok.Data;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `RepositorySettings` 是 ThingsBoard Common Data 中描述 `Repository Settings` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
public class RepositorySettings implements Serializable {
    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -3211552851889198721L;

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private String repositoryUri;
    private RepositoryAuthMethod authMethod;
    /**
     * 用户名，用于认证或安全校验。
     */
    private String username;
    private String password;
    /**
     * 私钥，用于标识或展示当前对象。
     */
    private String privateKeyFileName;
    private String privateKey;
    /**
     * 密码，用于定位映射、配置或数据项。
     */
    private String privateKeyPassword;
    private String defaultBranch;
    /**
     * 是否满足`readOnly`条件。
     */
    private boolean readOnly;
    private boolean showMergeCommits;

    /**
     * 功能：创建 `RepositorySettings` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public RepositorySettings() {
    }

    /**
     * 功能：创建 `RepositorySettings` 实例，并初始化必要字段。
     * 参数：
     * - `settings`：配置对象。
     * 返回：新创建的对象实例。
     */
    public RepositorySettings(RepositorySettings settings) {
        this.repositoryUri = settings.getRepositoryUri();
        this.authMethod = settings.getAuthMethod();
        this.username = settings.getUsername();
        this.password = settings.getPassword();
        this.privateKeyFileName = settings.getPrivateKeyFileName();
        this.privateKey = settings.getPrivateKey();
        this.privateKeyPassword = settings.getPrivateKeyPassword();
        this.defaultBranch = settings.getDefaultBranch();
        this.readOnly = settings.isReadOnly();
        this.showMergeCommits = settings.isShowMergeCommits();
    }
}
