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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * 中文说明：
 * 1. `OAuth2ConfigTemplateServiceImpl` 是 ThingsBoard DAO 中负责 `O Auth2 Template` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractEntityService`、`OAuth2ConfigTemplateService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Service
@AllArgsConstructor
public class OAuth2ConfigTemplateServiceImpl extends AbstractEntityService implements OAuth2ConfigTemplateService {

    /**
     * 客户端常量，用于统一引用固定值。
     */
    private static final String INCORRECT_CLIENT_REGISTRATION_TEMPLATE_ID = "Incorrect clientRegistrationTemplateId ";
    private static final String INCORRECT_CLIENT_REGISTRATION_PROVIDER_ID = "Incorrect clientRegistrationProviderId ";

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private final OAuth2ClientRegistrationTemplateDao clientRegistrationTemplateDao;
    private final DataValidator<OAuth2ClientRegistrationTemplate> clientRegistrationTemplateValidator;

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `clientRegistrationTemplate`：客户端对象。
     * 返回：处理结果。
     */
    @Override
    public OAuth2ClientRegistrationTemplate saveClientRegistrationTemplate(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        log.trace("Executing saveClientRegistrationTemplate [{}]", clientRegistrationTemplate);
        clientRegistrationTemplateValidator.validate(clientRegistrationTemplate, o -> TenantId.SYS_TENANT_ID);
        OAuth2ClientRegistrationTemplate savedClientRegistrationTemplate;
        try {
            savedClientRegistrationTemplate = clientRegistrationTemplateDao.save(TenantId.SYS_TENANT_ID, clientRegistrationTemplate);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("oauth2_template_provider_id_unq_key")) {
                throw new DataValidationException("Client registration template with such providerId already exists!");
            } else {
                throw t;
            }
        }
        return savedClientRegistrationTemplate;
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `providerId`：提供者ID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<OAuth2ClientRegistrationTemplate> findClientRegistrationTemplateByProviderId(String providerId) {
        log.trace("Executing findClientRegistrationTemplateByProviderId [{}]", providerId);
        validateString(providerId, INCORRECT_CLIENT_REGISTRATION_PROVIDER_ID + providerId);
        return clientRegistrationTemplateDao.findByProviderId(providerId);
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `templateId`：`templateId`ID。
     * 返回：处理结果。
     */
    @Override
    public OAuth2ClientRegistrationTemplate findClientRegistrationTemplateById(OAuth2ClientRegistrationTemplateId templateId) {
        log.trace("Executing findClientRegistrationTemplateById [{}]", templateId);
        validateId(templateId, INCORRECT_CLIENT_REGISTRATION_TEMPLATE_ID + templateId);
        return clientRegistrationTemplateDao.findById(TenantId.SYS_TENANT_ID, templateId.getId());
    }

    /**
     * 功能：获取客户端。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<OAuth2ClientRegistrationTemplate> findAllClientRegistrationTemplates() {
        log.trace("Executing findAllClientRegistrationTemplates");
        return clientRegistrationTemplateDao.findAll();
    }

    /**
     * 功能：删除或清理客户端。
     * 参数：
     * - `templateId`：`templateId`ID。
     * 返回：无。
     */
    @Override
    public void deleteClientRegistrationTemplateById(OAuth2ClientRegistrationTemplateId templateId) {
        log.trace("Executing deleteClientRegistrationTemplateById [{}]", templateId);
        validateId(templateId, INCORRECT_CLIENT_REGISTRATION_TEMPLATE_ID + templateId);
        clientRegistrationTemplateDao.removeById(TenantId.SYS_TENANT_ID, templateId.getId());
    }
}
