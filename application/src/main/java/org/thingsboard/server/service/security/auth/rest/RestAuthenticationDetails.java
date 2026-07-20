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
package org.thingsboard.server.service.security.auth.rest;

import lombok.Data;
import ua_parser.Client;
import ua_parser.Parser;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * 中文说明：
 * 1. `RestAuthenticationDetails` 是 ThingsBoard Application 中围绕 `Rest Authentication Details` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Data
public class RestAuthenticationDetails implements Serializable {

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private final String clientAddress;
    private final Client userAgent;

    /**
     * 功能：创建 `RestAuthenticationDetails` 实例，并初始化必要字段。
     * 参数：
     * - `request`：请求对象。
     * 返回：新创建的对象实例。
     */
    public RestAuthenticationDetails(HttpServletRequest request) {
        this.clientAddress = getClientIP(request);
        this.userAgent = getUserAgent(request);
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    private static String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    private static Client getUserAgent(HttpServletRequest request) {
        Parser uaParser = new Parser();
        return uaParser.parse(request.getHeader("User-Agent"));
    }
}
