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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `Oauth2AuthenticationSuccessHandlerTest` 是 ThingsBoard Application 中验证 `Oauth2AuthenticationSuccessHandler` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractControllerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class Oauth2AuthenticationSuccessHandlerTest extends AbstractControllerTest {

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired
    private Oauth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    @Mock
    private JwtTokenFactory jwtTokenFactory;

    /**
     * 用户对象，用于描述当前业务场景。
     */
    private SecurityUser securityUser;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() {
        UserId userId = new UserId(UUID.randomUUID());
        securityUser = new SecurityUser(userId);
        when(jwtTokenFactory.createTokenPair(eq(securityUser))).thenReturn(new JwtPair("testAccessToken", "testRefreshToken"));
    }

    /**
     * 功能：验证URL 地址相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGetRedirectUrl() {
        JwtPair jwtPair = jwtTokenFactory.createTokenPair(securityUser);

        String urlWithoutParams = "http://localhost:8080/dashboardGroups/3fa13530-6597-11ed-bd76-8bd591f0ec3e";
        String urlWithParams = "http://localhost:8080/dashboardGroups/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState&page=1";

        String redirectUrl = oauth2AuthenticationSuccessHandler.getRedirectUrl(urlWithoutParams, jwtPair);
        String expectedUrl = urlWithoutParams + "/?accessToken=" + jwtPair.getToken() + "&refreshToken=" + jwtPair.getRefreshToken();
        assertEquals(expectedUrl, redirectUrl);

        redirectUrl = oauth2AuthenticationSuccessHandler.getRedirectUrl(urlWithParams, jwtPair);
        expectedUrl = urlWithParams + "&accessToken=" + jwtPair.getToken() + "&refreshToken=" + jwtPair.getRefreshToken();
        assertEquals(expectedUrl, redirectUrl);
    }
}