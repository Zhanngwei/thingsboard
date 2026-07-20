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
package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.PlatformType;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `OAuth2Service` 是 ThingsBoard Common 中定义 `O Auth2` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface OAuth2Service {
    /**
     * 功能：获取`O Auth2 Clients`。
     * 参数：
     * - `domainScheme`：`domainScheme` 参数。
     * - `domainName`：名称。
     * - `pkgName`：名称。
     * - `platformType`：类型。
     * 返回：匹配的数据集合。
     */
    List<OAuth2ClientInfo> getOAuth2Clients(String domainScheme, String domainName, String pkgName, PlatformType platformType);

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `oauth2Info`：`oauth2Info` 参数。
     * 返回：无。
     */
    void saveOAuth2Info(OAuth2Info oauth2Info);

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    OAuth2Info findOAuth2Info();

    /**
     * 功能：获取`Registration`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    OAuth2Registration findRegistration(UUID id);

    /**
     * 功能：获取`All Registrations`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    List<OAuth2Registration> findAllRegistrations();

    /**
     * 功能：获取密钥。
     * 参数：
     * - `registrationId`：`registrationId`ID。
     * - `pkgName`：名称。
     * 返回：文本结果。
     */
    String findAppSecret(UUID registrationId, String pkgName);
}
