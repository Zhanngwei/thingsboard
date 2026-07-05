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
 * 1. 类目的：`OAuth2ConfigTemplateController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
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

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2ConfigTemplateController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
