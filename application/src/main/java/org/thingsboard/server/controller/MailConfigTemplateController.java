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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.mail.TbMailConfigTemplateService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.io.IOException;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;

/**
 * 中文说明：
 * 1. `MailConfigTemplateController` 是 ThingsBoard Application 中处理 `Mail Template` 请求的 API 控制器。
 * 2. 它负责校验请求参数、解析当前用户上下文并调用对应服务完成操作。
 * 3. 方法返回面向客户端的数据对象或统一的异步响应。
 * 4. 直接依赖的类型边界包括 `BaseController`。
 * 5. 单独设置控制器可以把 HTTP 边界与业务实现分开，保持接口行为稳定。
 * 6. 阅读时重点关注路由、权限条件、参数校验以及服务调用结果的转换。
 */
@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/mail/config/template")
@Slf4j
public class MailConfigTemplateController extends BaseController {
    /**
     * 配置常量，用于统一引用固定值。
     */
    private static final String MAIL_CONFIG_TEMPLATE_DEFINITION = "Mail configuration template is set of default smtp settings for mail server that specific provider supports";
    private final TbMailConfigTemplateService mailConfigTemplateService;

    /**
     * 功能：获取客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get the list of all OAuth2 client registration templates (getClientRegistrationTemplates)" + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            notes = MAIL_CONFIG_TEMPLATE_DEFINITION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public JsonNode getClientRegistrationTemplates() throws ThingsboardException, IOException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
        return mailConfigTemplateService.findAllMailConfigTemplates();
    }

}
