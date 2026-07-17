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

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * 中文说明：
 * 1. `DefaultTokenOutdatingService` 是 ThingsBoard Application 中负责 `Token Outdating` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TokenOutdatingService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
public class DefaultTokenOutdatingService implements TokenOutdatingService {

    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    private final TbTransactionalCache<String, Long> cache;
    private final JwtTokenFactory tokenFactory;

    /**
     * 功能：创建 `DefaultTokenOutdatingService` 实例，并初始化必要字段。
     * 参数：
     * - `cache`：`cache` 参数。
     * - `tokenFactory`：`tokenFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public DefaultTokenOutdatingService(@Qualifier("UsersSessionInvalidation") TbTransactionalCache<String, Long> cache, JwtTokenFactory tokenFactory) {
        this.cache = cache;
        this.tokenFactory = tokenFactory;
    }

    /**
     * 功能：处理用户。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @EventListener(classes = UserAuthDataChangedEvent.class)
    public void onUserAuthDataChanged(UserAuthDataChangedEvent event) {
        if (StringUtils.hasText(event.getId())) {
            cache.put(event.getId(), event.getTs());
        }
    }

    /**
     * 功能：判断`Outdated`。
     * 参数：
     * - `token`：`token` 参数。
     * - `userId`：用户ID。
     * 返回：判断结果。
     */
    @Override
    public boolean isOutdated(String token, UserId userId) {
        Claims claims = tokenFactory.parseTokenClaims(token).getBody();
        long issueTime = claims.getIssuedAt().getTime();
        String sessionId = claims.get("sessionId", String.class);
        if (isTokenOutdated(issueTime, userId.toString())){
             return true;
        } else {
             return sessionId != null && isTokenOutdated(issueTime, sessionId);
        }
    }

    /**
     * 功能：判断令牌。
     * 参数：
     * - `issueTime`：`issueTime` 参数。
     * - `sessionId`：会话ID。
     * 返回：判断结果。
     */
    private Boolean isTokenOutdated(long issueTime, String sessionId) {
        return Optional.ofNullable(cache.get(sessionId)).map(outdatageTime -> isTokenOutdated(issueTime, outdatageTime.get())).orElse(false);
    }

    /**
     * 功能：判断令牌。
     * 参数：
     * - `issueTime`：`issueTime` 参数。
     * - `outdatageTime`：待处理数据。
     * 返回：判断结果。
     */
    private boolean isTokenOutdated(long issueTime, Long outdatageTime) {
        return MILLISECONDS.toSeconds(issueTime) < MILLISECONDS.toSeconds(outdatageTime);
    }
}
