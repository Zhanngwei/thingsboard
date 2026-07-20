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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;

/**
 * 中文说明：
 * 1. `OAuth2ConfigTemplateController` 是 ThingsBoard Application 中处理 `O Auth2 Template` 请求的 API 控制器。
 * 2. 它负责校验请求参数、解析当前用户上下文并调用对应服务完成操作。
 * 3. 方法返回面向客户端的数据对象或统一的异步响应。
 * 4. 直接依赖的类型边界包括 `BaseController`。
 * 5. 单独设置控制器可以把 HTTP 边界与业务实现分开，保持接口行为稳定。
 * 6. 阅读时重点关注路由、权限条件、参数校验以及服务调用结果的转换。
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/oauth2/config/template")
@Slf4j
public class OAuth2ConfigTemplateController extends BaseController {
    /**
     * 客户端常量，用于统一引用固定值。
     */
    private static final String CLIENT_REGISTRATION_TEMPLATE_ID = "clientRegistrationTemplateId";

    /**
     * 客户端常量，用于统一引用固定值。
     */
    private static final String OAUTH2_CLIENT_REGISTRATION_TEMPLATE_DEFINITION = "Client registration template is OAuth2 provider configuration template with default settings for registering new OAuth2 clients";

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `clientRegistrationTemplate`：客户端对象。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Create or update OAuth2 client registration template (saveClientRegistrationTemplate)" + SYSTEM_AUTHORITY_PARAGRAPH,
            notes = OAUTH2_CLIENT_REGISTRATION_TEMPLATE_DEFINITION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public OAuth2ClientRegistrationTemplate saveClientRegistrationTemplate(@RequestBody OAuth2ClientRegistrationTemplate clientRegistrationTemplate) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_TEMPLATE, Operation.WRITE);
        return oAuth2ConfigTemplateService.saveClientRegistrationTemplate(clientRegistrationTemplate);
    }

    /**
     * 功能：删除或清理客户端。
     * 参数：
     * - `delete`：`delete` 参数。
     * - `strClientRegistrationTemplateId`：客户端ID。
     * 返回：无。
     */
    @ApiOperation(value = "Delete OAuth2 client registration template by id (deleteClientRegistrationTemplate)" + SYSTEM_AUTHORITY_PARAGRAPH,
            notes = OAUTH2_CLIENT_REGISTRATION_TEMPLATE_DEFINITION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/{clientRegistrationTemplateId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteClientRegistrationTemplate(@ApiParam(value = "String representation of client registration template id to delete", example = "139b1f81-2f5d-11ec-9dbe-9b627e1a88f4")
                                                 @PathVariable(CLIENT_REGISTRATION_TEMPLATE_ID) String strClientRegistrationTemplateId) throws ThingsboardException {
        checkParameter(CLIENT_REGISTRATION_TEMPLATE_ID, strClientRegistrationTemplateId);
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_TEMPLATE, Operation.DELETE);
        OAuth2ClientRegistrationTemplateId clientRegistrationTemplateId = new OAuth2ClientRegistrationTemplateId(toUUID(strClientRegistrationTemplateId));
        oAuth2ConfigTemplateService.deleteClientRegistrationTemplateById(clientRegistrationTemplateId);
    }

    /**
     * 功能：获取客户端。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get the list of all OAuth2 client registration templates (getClientRegistrationTemplates)" + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            notes = OAUTH2_CLIENT_REGISTRATION_TEMPLATE_DEFINITION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<OAuth2ClientRegistrationTemplate> getClientRegistrationTemplates() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_TEMPLATE, Operation.READ);
        return oAuth2ConfigTemplateService.findAllClientRegistrationTemplates();
    }

}
