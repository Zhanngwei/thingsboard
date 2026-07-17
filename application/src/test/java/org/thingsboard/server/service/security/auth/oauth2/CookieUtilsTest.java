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

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.service.security.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME;

/**
 * 中文说明：
 * 1. `CookieUtilsTest` 是 ThingsBoard Application 中验证 `CookieUtils` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class CookieUtilsTest {

    /**
     * 功能：执行 `serializeDeserializeOAuth2AuthorizationRequestTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void serializeDeserializeOAuth2AuthorizationRequestTest() {
        HttpCookieOAuth2AuthorizationRequestRepository cookieRequestRepo = new HttpCookieOAuth2AuthorizationRequestRepository();
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);

        Map<String, Object> additionalParameters = new LinkedHashMap<>();
        additionalParameters.put("param1", "value1");
        additionalParameters.put("param2", "value2");
        var request = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("testUri").clientId("testId")
                .scope("read", "write")
                .additionalParameters(additionalParameters).build();


        Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, CookieUtils.serialize(request));
        Mockito.when(servletRequest.getCookies()).thenReturn(new Cookie[]{cookie});

        OAuth2AuthorizationRequest deserializedRequest = cookieRequestRepo.loadAuthorizationRequest(servletRequest);

        assertNotNull(deserializedRequest);
        assertEquals(request.getGrantType(), deserializedRequest.getGrantType());
        assertEquals(request.getAuthorizationUri(), deserializedRequest.getAuthorizationUri());
        assertEquals(request.getClientId(), deserializedRequest.getClientId());
    }
}