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
package org.thingsboard.monitoring.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.rest.client.RestClient;

import java.time.Duration;

/**
 * 中文说明：
 * 1. `TbClient` 是 ThingsBoard Monitoring 中访问 `Tb Client` 的客户端封装。
 * 2. 它把连接建立、请求发送、认证信息和响应解析集中到统一入口。
 * 3. 公开方法以平台数据模型作为输入输出，隐藏底层通信细节。
 * 4. 直接依赖的类型边界包括 `RestClient`。
 * 5. 独立客户端可以保持调用 API 稳定，并避免使用方重复处理连接与序列化。
 * 6. 阅读时重点关注连接配置、认证状态、请求构造和资源释放。
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TbClient extends RestClient {

    /**
     * 用户名，用于认证或安全校验。
     */
    @Value("${monitoring.rest.username}")
    private String username;
    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${monitoring.rest.password}")
    private String password;

    /**
     * 功能：创建 `TbClient` 实例，并初始化必要字段。
     * 参数：
     * - `baseUrl`：`baseUrl` 参数。
     * - `requestTimeoutMs`：请求对象。
     * 返回：新创建的对象实例。
     */
    public TbClient(@Value("${monitoring.rest.base_url}") String baseUrl,
                    @Value("${monitoring.rest.request_timeout_ms}") int requestTimeoutMs) {
        super(new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(requestTimeoutMs))
                .setReadTimeout(Duration.ofMillis(requestTimeoutMs))
                .build(), baseUrl);
    }

    /**
     * 功能：执行 `logIn` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public String logIn() {
        login(username, password);
        return getToken();
    }

}
