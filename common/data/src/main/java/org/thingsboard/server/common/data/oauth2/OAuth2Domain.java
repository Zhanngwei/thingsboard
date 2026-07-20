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
package org.thingsboard.server.common.data.oauth2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.OAuth2DomainId;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;

/**
 * 中文说明：
 * 1. `OAuth2Domain` 是 ThingsBoard Common Data 中承载 `O Auth2 Domain` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
public class OAuth2Domain extends BaseData<OAuth2DomainId> {

    /**
     * `oauth2ParamsId`ID，用于定位对应业务对象。
     */
    private OAuth2ParamsId oauth2ParamsId;
    private String domainName;
    /**
     * `domainScheme` 字段，保存当前对象的对应属性。
     */
    private SchemeType domainScheme;

    /**
     * 功能：创建 `OAuth2Domain` 实例，并初始化必要字段。
     * 参数：
     * - `domain`：`domain` 参数。
     * 返回：新创建的对象实例。
     */
    public OAuth2Domain(OAuth2Domain domain) {
        super(domain);
        this.oauth2ParamsId = domain.oauth2ParamsId;
        this.domainName = domain.domainName;
        this.domainScheme = domain.domainScheme;
    }
}
