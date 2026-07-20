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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `UiSettingsController` 是 ThingsBoard Application 中处理 `Ui` 请求的 API 控制器。
 * 2. 它负责校验请求参数、解析当前用户上下文并调用对应服务完成操作。
 * 3. 方法返回面向客户端的数据对象或统一的异步响应。
 * 4. 直接依赖的类型边界包括 `BaseController`。
 * 5. 单独设置控制器可以把 HTTP 边界与业务实现分开，保持接口行为稳定。
 * 6. 阅读时重点关注路由、权限条件、参数校验以及服务调用结果的转换。
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class UiSettingsController extends BaseController {

    /**
     * 基础访问地址，用于定位外部资源或本地资源。
     */
    @Value("${ui.help.base-url}")
    private String helpBaseUrl;

    /**
     * 功能：获取基础访问地址。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiOperation(value = "Get UI help base url (getHelpBaseUrl)",
            notes = "Get UI help base url used to fetch help assets. " +
                    "The actual value of the base url is configurable in the system configuration file.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/uiSettings/helpBaseUrl", method = RequestMethod.GET)
    @ResponseBody
    public String getHelpBaseUrl() throws ThingsboardException {
        return helpBaseUrl;
    }

}
