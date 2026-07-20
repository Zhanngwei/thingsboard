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
package org.thingsboard.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.TenantProfileNotFoundException;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 中文说明：
 * 1. `RateLimitProcessingFilter` 是 ThingsBoard Application 中处理 `Rate Limit Processing` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `OncePerRequestFilter`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitProcessingFilter extends OncePerRequestFilter {

    /**
     * 响应，负责处理对应任务或消息。
     */
    private final ThingsboardErrorResponseHandler errorResponseHandler;
    private final RateLimitService rateLimitService;

    /**
     * 功能：执行 `doFilterInternal` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * - `chain`：`chain` 参数。
     * 返回：无。
     */
    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        SecurityUser user = getCurrentUser();
        if (user != null && !user.isSystemAdmin()) {
            try {
                if (!rateLimitService.checkRateLimit(LimitedApi.REST_REQUESTS_PER_TENANT, user.getTenantId())) {
                    rateLimitExceeded(EntityType.TENANT, response);
                    return;
                }
            } catch (TenantProfileNotFoundException e) {
                log.debug("[{}] Failed to lookup tenant profile", user.getTenantId());
                errorResponseHandler.handle(new BadCredentialsException("Failed to lookup tenant profile"), response);
                return;
            }

            if (user.isCustomerUser()) {
                if (!rateLimitService.checkRateLimit(LimitedApi.REST_REQUESTS_PER_CUSTOMER, user.getTenantId(), user.getCustomerId())) {
                    rateLimitExceeded(EntityType.CUSTOMER, response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 功能：执行 `shouldNotFilterAsyncDispatch` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * 功能：执行 `shouldNotFilterErrorDispatch` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    /**
     * 功能：执行 `rateLimitExceeded` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `response`：响应对象。
     * 返回：无。
     */
    private void rateLimitExceeded(EntityType type, HttpServletResponse response) {
        errorResponseHandler.handle(new TbRateLimitsException(type), response);
    }

    /**
     * 功能：获取用户。
     * 参数：无。
     * 返回：处理结果。
     */
    protected SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            return null;
        }
    }

}
