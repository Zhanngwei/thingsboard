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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.oauth2.OAuth2Configuration;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.utils.MiscUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;

/**
 * 中文说明：
 * 1. `OAuth2Controller` 是 ThingsBoard Application 中处理 `O Auth2` 请求的 API 控制器。
 * 2. 它负责校验请求参数、解析当前用户上下文并调用对应服务完成操作。
 * 3. 方法返回面向客户端的数据对象或统一的异步响应。
 * 4. 直接依赖的类型边界包括 `BaseController`。
 * 5. 单独设置控制器可以把 HTTP 边界与业务实现分开，保持接口行为稳定。
 * 6. 阅读时重点关注路由、权限条件、参数校验以及服务调用结果的转换。
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class OAuth2Controller extends BaseController {

    /**
     * `oAuth2Configuration`，保存当前对象的配置选项。
     */
    @Autowired
    private OAuth2Configuration oAuth2Configuration;


    /**
     * 功能：获取`O Auth2 Clients`。
     * 参数：
     * - `request`：请求对象。
     * - `name`：名称。
     * - `pkgName`：名称。
     * - `ignored`：`ignored` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get OAuth2 clients (getOAuth2Clients)", notes = "Get the list of OAuth2 clients " +
            "to log in with, available for such domain scheme (HTTP or HTTPS) (if x-forwarded-proto request header is present - " +
            "the scheme is known from it) and domain name and port (port may be known from x-forwarded-port header)")
    @RequestMapping(value = "/noauth/oauth2Clients", method = RequestMethod.POST)
    @ResponseBody
    public List<OAuth2ClientInfo> getOAuth2Clients(HttpServletRequest request,
                                                   @ApiParam(value = "Mobile application package name, to find OAuth2 clients " +
                                                           "where there is configured mobile application with such package name")
                                                   @RequestParam(required = false) String pkgName,
                                                   @ApiParam(value = "Platform type to search OAuth2 clients for which " +
                                                           "the usage with this platform type is allowed in the settings. " +
                                                           "If platform type is not one of allowable values - it will just be ignored",
                                                           allowableValues = "WEB, ANDROID, IOS")
                                                   @RequestParam(required = false) String platform) throws ThingsboardException {
        if (log.isDebugEnabled()) {
            log.debug("Executing getOAuth2Clients: [{}][{}][{}]", request.getScheme(), request.getServerName(), request.getServerPort());
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String header = headerNames.nextElement();
                log.debug("Header: {} {}", header, request.getHeader(header));
            }
        }
        PlatformType platformType = null;
        if (StringUtils.isNotEmpty(platform)) {
            try {
                platformType = PlatformType.valueOf(platform);
            } catch (Exception e) {}
        }
        return oAuth2Service.getOAuth2Clients(MiscUtils.getScheme(request), MiscUtils.getDomainNameAndPort(request), pkgName, platformType);
    }

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Get current OAuth2 settings (getCurrentOAuth2Info)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public OAuth2Info getCurrentOAuth2Info() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_INFO, Operation.READ);
        return oAuth2Service.findOAuth2Info();
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `oauth2Info`：`oauth2Info` 参数。
     * 返回：处理结果。
     */
    @ApiOperation(value = "Save OAuth2 settings (saveOAuth2Info)", notes = SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/config", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public OAuth2Info saveOAuth2Info(@RequestBody OAuth2Info oauth2Info) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_INFO, Operation.WRITE);
        oAuth2Service.saveOAuth2Info(oauth2Info);
        return oAuth2Service.findOAuth2Info();
    }

    /**
     * 功能：获取URL 地址。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiOperation(value = "Get OAuth2 log in processing URL (getLoginProcessingUrl)", notes = "Returns the URL enclosed in " +
            "double quotes. After successful authentication with OAuth2 provider, it makes a redirect to this path so that the platform can do " +
            "further log in processing. This URL may be configured as 'security.oauth2.loginProcessingUrl' property in yml configuration file, or " +
            "as 'SECURITY_OAUTH2_LOGIN_PROCESSING_URL' env variable. By default it is '/login/oauth2/code/'" + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/oauth2/loginProcessingUrl", method = RequestMethod.GET)
    @ResponseBody
    public String getLoginProcessingUrl() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.OAUTH2_CONFIGURATION_INFO, Operation.READ);
        return "\"" + oAuth2Configuration.getLoginProcessingUrl() + "\"";
    }

}
