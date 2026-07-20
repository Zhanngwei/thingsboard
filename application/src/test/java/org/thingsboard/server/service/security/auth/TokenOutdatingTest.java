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
package org.thingsboard.server.service.security.auth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.security.event.UserSessionInvalidationEvent;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenAuthenticationProvider;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `TokenOutdatingTest` 是 ThingsBoard Application 中验证 `TokenOutdating` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TokenOutdatingTest.class, loader = SpringBootContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ComponentScan({"org.thingsboard.server"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DaoSqlTest
@TestPropertySource(properties = {
        "security.jwt.tokenIssuer=test.io",
        "security.jwt.tokenSigningKey=secret",
        "security.jwt.tokenExpirationTime=600",
        "security.jwt.refreshTokenExpTime=15",
        // explicitly set the wrong value to check that it is NOT used.
        "cache.specs.userSessionsInvalidation.timeToLiveInMinutes=2"
})
public class TokenOutdatingTest {
    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    private JwtAuthenticationProvider accessTokenAuthenticationProvider;
    private RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private TokenOutdatingService tokenOutdatingService;
    /**
     * 事件，表示当前对象的对应属性。
     */
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    @Autowired
    private JwtTokenFactory tokenFactory;
    private SecurityUser securityUser;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        UserId userId = new UserId(UUID.randomUUID());
        securityUser = createMockSecurityUser(userId);

        UserService userService = mock(UserService.class);

        User user = new User();
        user.setId(userId);
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("email");
        when(userService.findUserById(any(), eq(userId))).thenReturn(user);

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);
        when(userService.findUserCredentialsByUserId(any(), eq(userId))).thenReturn(userCredentials);

        accessTokenAuthenticationProvider = new JwtAuthenticationProvider(tokenFactory, tokenOutdatingService);
        refreshTokenAuthenticationProvider = new RefreshTokenAuthenticationProvider(tokenFactory, userService, mock(CustomerService.class), tokenOutdatingService);
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOutdateOldUserTokens() throws Exception {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        // Token outdatage time is rounded to 1 sec. Need to wait before outdating so that outdatage time is strictly after token issue time
        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
        assertTrue(tokenOutdatingService.isOutdated(jwtToken.getToken(), securityUser.getId()));

        SECONDS.sleep(1);

        JwtToken newJwtToken = tokenFactory.createAccessJwtToken(securityUser);
        assertFalse(tokenOutdatingService.isOutdated(newJwtToken.getToken(), securityUser.getId()));
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAuthenticateWithOutdatedAccessToken() throws InterruptedException {
        RawAccessJwtToken accessJwtToken = getRawJwtToken(tokenFactory.createAccessJwtToken(securityUser));

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessJwtToken));
        });

        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(accessJwtToken));
        });
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAuthenticateWithOutdatedRefreshToken() throws InterruptedException {
        RawAccessJwtToken refreshJwtToken = getRawJwtToken(tokenFactory.createRefreshToken(securityUser));

        assertDoesNotThrow(() -> {
            refreshTokenAuthenticationProvider.authenticate(new RefreshAuthenticationToken(refreshJwtToken));
        });

        SECONDS.sleep(1);
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(CredentialsExpiredException.class, () -> {
            refreshTokenAuthenticationProvider.authenticate(new RefreshAuthenticationToken(refreshJwtToken));
        });
    }

    // This test takes too long to run and is basically testing the cache logic
//    @Test
//    public void testTokensOutdatageTimeRemovalFromCache() throws Exception {
//        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);
//
//        SECONDS.sleep(1);
//        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
//
//        SECONDS.sleep(1);
//
//        assertTrue(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//
//        SECONDS.sleep(30); // refreshTokenExpTime/2
//
//        assertTrue(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//
//        SECONDS.sleep(30 + 1); // refreshTokenExpTime/2 + 1
//
//        assertFalse(tokenOutdatingService.isOutdated(jwtToken, securityUser.getId()));
//    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOnlyOneTokenExpired() throws InterruptedException {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        SecurityUser anotherSecurityUser = new SecurityUser(securityUser, securityUser.isEnabled(), securityUser.getUserPrincipal());
        JwtToken anotherJwtToken = tokenFactory.createAccessJwtToken(anotherSecurityUser);

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        SECONDS.sleep(1);

        eventPublisher.publishEvent(new UserSessionInvalidationEvent(securityUser.getSessionId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });
    }

    /**
     * 功能：验证`Reset All Sessions`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testResetAllSessions() throws InterruptedException {
        JwtToken jwtToken = tokenFactory.createAccessJwtToken(securityUser);

        SecurityUser anotherSecurityUser = new SecurityUser(securityUser, securityUser.isEnabled(), securityUser.getUserPrincipal());
        JwtToken anotherJwtToken = tokenFactory.createAccessJwtToken(anotherSecurityUser);

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertDoesNotThrow(() -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });

        SECONDS.sleep(1);

        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(jwtToken)));
        });

        assertThrows(JwtExpiredTokenException.class, () -> {
            accessTokenAuthenticationProvider.authenticate(new JwtAuthenticationToken(getRawJwtToken(anotherJwtToken)));
        });
    }


    /**
     * 功能：获取令牌。
     * 参数：
     * - `token`：`token` 参数。
     * 返回：处理结果。
     */
    private RawAccessJwtToken getRawJwtToken(JwtToken token) {
        return new RawAccessJwtToken(token.getToken());
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    private SecurityUser createMockSecurityUser(UserId userId) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setEmail("email");
        securityUser.setUserPrincipal(new UserPrincipal(UserPrincipal.Type.USER_NAME, securityUser.getEmail()));
        securityUser.setAuthority(Authority.CUSTOMER_USER);
        securityUser.setId(userId);
        securityUser.setSessionId(UUID.randomUUID().toString());
        return securityUser;
    }
}
