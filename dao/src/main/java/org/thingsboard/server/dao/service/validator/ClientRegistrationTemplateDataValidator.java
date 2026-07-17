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
package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

/**
 * 中文说明：
 * 1. `ClientRegistrationTemplateDataValidator` 是 ThingsBoard DAO 中负责 `Client Registration Template` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `DataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
public class ClientRegistrationTemplateDataValidator extends DataValidator<OAuth2ClientRegistrationTemplate> {

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `clientRegistrationTemplate`：客户端对象。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `clientRegistrationTemplate`：客户端对象。
     * 返回：判断结果。
     */
    @Override
    protected OAuth2ClientRegistrationTemplate validateUpdate(TenantId tenantId, OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        return null;
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `clientRegistrationTemplate`：客户端对象。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        if (StringUtils.isEmpty(clientRegistrationTemplate.getProviderId())) {
            throw new DataValidationException("Provider ID should be specified!");
        }
        if (clientRegistrationTemplate.getMapperConfig() == null) {
            throw new DataValidationException("Mapper config should be specified!");
        }
        if (clientRegistrationTemplate.getMapperConfig().getType() == null) {
            throw new DataValidationException("Mapper type should be specified!");
        }
        if (clientRegistrationTemplate.getMapperConfig().getBasic() == null) {
            throw new DataValidationException("Basic mapper config should be specified!");
        }
    }
}
