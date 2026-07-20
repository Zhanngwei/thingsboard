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
package org.thingsboard.server.service.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `OAuth2ClientMapperProvider` 是 ThingsBoard Application 中创建或提供 `O Auth2 Client` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 它直接协作于目标接口、具体实现和创建所需配置。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@Slf4j
@TbCoreComponent
public class OAuth2ClientMapperProvider {

    /**
     * 客户端映射关系，用于按键查找对应值。
     */
    @Autowired
    @Qualifier("basicOAuth2ClientMapper")
    private OAuth2ClientMapper basicOAuth2ClientMapper;

    /**
     * 客户端映射关系，用于按键查找对应值。
     */
    @Autowired
    @Qualifier("customOAuth2ClientMapper")
    private OAuth2ClientMapper customOAuth2ClientMapper;

    /**
     * 客户端映射关系，用于按键查找对应值。
     */
    @Autowired
    @Qualifier("githubOAuth2ClientMapper")
    private OAuth2ClientMapper githubOAuth2ClientMapper;

    /**
     * 客户端映射关系，用于按键查找对应值。
     */
    @Autowired
    @Qualifier("appleOAuth2ClientMapper")
    private OAuth2ClientMapper appleOAuth2ClientMapper;

    /**
     * 功能：获取类型。
     * 参数：
     * - `oauth2MapperType`：类型。
     * 返回：键值映射结果。
     */
    public OAuth2ClientMapper getOAuth2ClientMapperByType(MapperType oauth2MapperType) {
        switch (oauth2MapperType) {
            case CUSTOM:
                return customOAuth2ClientMapper;
            case BASIC:
                return basicOAuth2ClientMapper;
            case GITHUB:
                return githubOAuth2ClientMapper;
            case APPLE:
                return appleOAuth2ClientMapper;
            default:
                throw new RuntimeException("OAuth2ClientRegistrationMapper with type " + oauth2MapperType + " is not supported!");
        }
    }
}
