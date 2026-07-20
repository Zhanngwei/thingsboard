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
package org.thingsboard.server.service.security.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

/**
 * 中文说明：
 * 1. `AbstractJwtAuthenticationToken` 是 ThingsBoard Application 中承载 `Jwt Authentication Token` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `AbstractAuthenticationToken`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public abstract class AbstractJwtAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -6212297506742428406L;

    /**
     * 令牌，用于认证或安全校验。
     */
    private RawAccessJwtToken rawAccessToken;
    private SecurityUser securityUser;

    /**
     * 功能：创建 `AbstractJwtAuthenticationToken` 实例，并初始化必要字段。
     * 参数：
     * - `unsafeToken`：`unsafeToken` 参数。
     * 返回：新创建的对象实例。
     */
    public AbstractJwtAuthenticationToken(RawAccessJwtToken unsafeToken) {
        super(null);
        this.rawAccessToken = unsafeToken;
        this.setAuthenticated(false);
    }

    /**
     * 功能：创建 `AbstractJwtAuthenticationToken` 实例，并初始化必要字段。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * 返回：新创建的对象实例。
     */
    public AbstractJwtAuthenticationToken(SecurityUser securityUser) {
        super(securityUser.getAuthorities());
        this.eraseCredentials();
        this.securityUser = securityUser;
        super.setAuthenticated(true);
    }

    /**
     * 功能：更新`Authenticated`。
     * 参数：
     * - `authenticated`：`authenticated` 参数。
     * 返回：无。
     */
    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Object getCredentials() {
        return rawAccessToken;
    }

    /**
     * 功能：获取`Principal`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Object getPrincipal() {
        return this.securityUser;
    }

    /**
     * 功能：执行 `eraseCredentials` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.rawAccessToken = null;
    }
}
