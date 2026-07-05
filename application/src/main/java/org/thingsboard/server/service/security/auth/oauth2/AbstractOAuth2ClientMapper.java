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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.entitiy.tenant.TbTenantService;
import org.thingsboard.server.service.entitiy.user.TbUserService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. 类目的：`AbstractOAuth2ClientMapper` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Slf4j
public abstract class AbstractOAuth2ClientMapper {
    /**
     * 请求常量，用于统一引用固定值。
     */
    private static final int DASHBOARDS_REQUEST_LIMIT = 10;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    private UserService userService;

    /**
     * 密码，用于认证或安全校验。
     */
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    private TenantService tenantService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    private TbTenantService tbTenantService;

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    private CustomerService customerService;

    /**
     * 仪表盘，提供当前类调用的业务操作。
     */
    @Autowired
    private DashboardService dashboardService;

    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private InstallScripts installScripts;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    private TbUserService tbUserService;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Autowired
    protected TbTenantProfileCache tenantProfileCache;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbClusterService tbClusterService;

    /**
     * 是否启用`edges`。
     */
    @Value("${edges.enabled}")
    @Getter
    private boolean edgesEnabled;

    private final Lock userCreationLock = new ReentrantLock();

    /**
     * 功能：获取用户。
     * 参数：
     * - `oauth2User`：`oauth2User` 参数。
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    protected SecurityUser getOrCreateSecurityUserFromOAuth2User(OAuth2User oauth2User, OAuth2Registration registration) {

        OAuth2MapperConfig config = registration.getMapperConfig();

        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, oauth2User.getEmail());

        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, oauth2User.getEmail());

        if (user == null && !config.isAllowUserCreation()) {
            throw new UsernameNotFoundException("User not found: " + oauth2User.getEmail());
        }

        if (user == null) {
            userCreationLock.lock();
            try {
                user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, oauth2User.getEmail());
                if (user == null) {
                    user = new User();
                    if (oauth2User.getCustomerId() == null && StringUtils.isEmpty(oauth2User.getCustomerName())) {
                        user.setAuthority(Authority.TENANT_ADMIN);
                    } else {
                        user.setAuthority(Authority.CUSTOMER_USER);
                    }
                    TenantId tenantId = oauth2User.getTenantId() != null ?
                            oauth2User.getTenantId() : getTenantId(oauth2User.getTenantName());
                    user.setTenantId(tenantId);
                    CustomerId customerId = oauth2User.getCustomerId() != null ?
                            oauth2User.getCustomerId() : getCustomerId(user.getTenantId(), oauth2User.getCustomerName());
                    user.setCustomerId(customerId);
                    user.setEmail(oauth2User.getEmail());
                    user.setFirstName(oauth2User.getFirstName());
                    user.setLastName(oauth2User.getLastName());

                    ObjectNode additionalInfo = JacksonUtil.newObjectNode();

                    if (!StringUtils.isEmpty(oauth2User.getDefaultDashboardName())) {
                        Optional<DashboardId> dashboardIdOpt =
                                user.getAuthority() == Authority.TENANT_ADMIN ?
                                        getDashboardId(tenantId, oauth2User.getDefaultDashboardName())
                                        : getDashboardId(tenantId, customerId, oauth2User.getDefaultDashboardName());
                        if (dashboardIdOpt.isPresent()) {
                            additionalInfo.put("defaultDashboardFullscreen", oauth2User.isAlwaysFullScreen());
                            additionalInfo.put("defaultDashboardId", dashboardIdOpt.get().getId().toString());
                        }
                    }

                    if (registration.getAdditionalInfo() != null &&
                            registration.getAdditionalInfo().has("providerName")) {
                        additionalInfo.put("authProviderName", registration.getAdditionalInfo().get("providerName").asText());
                    }

                    user.setAdditionalInfo(additionalInfo);

                    user = tbUserService.save(tenantId, customerId, user, false, null, null);
                    if (config.isActivateUser()) {
                        UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
                        userService.activateUserCredentials(user.getTenantId(), userCredentials.getActivateToken(), passwordEncoder.encode(""));
                    }
                }
            } catch (Exception e) {
                log.error("Can't get or create security user from oauth2 user", e);
                throw new RuntimeException("Can't get or create security user from oauth2 user", e);
            } finally {
                userCreationLock.unlock();
            }
        }

        try {
            SecurityUser securityUser = new SecurityUser(user, true, principal);
            return (SecurityUser) new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()).getPrincipal();
        } catch (Exception e) {
            log.error("Can't get or create security user from oauth2 user", e);
            throw new RuntimeException("Can't get or create security user from oauth2 user", e);
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantName`：租户信息或租户标识。
     * 返回：处理结果。
     */
    private TenantId getTenantId(String tenantName) throws Exception {
        List<Tenant> tenants = tenantService.findTenants(new PageLink(1, 0, tenantName)).getData();
        Tenant tenant;
        if (tenants == null || tenants.isEmpty()) {
            tenant = new Tenant();
            tenant.setTitle(tenantName);
            tenant = tbTenantService.save(tenant);
        } else {
            tenant = tenants.get(0);
        }
        return tenant.getTenantId();
    }

    /**
     * 功能：获取客户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerName`：名称。
     * 返回：处理结果。
     */
    private CustomerId getCustomerId(TenantId tenantId, String customerName) {
        if (StringUtils.isEmpty(customerName)) {
            return null;
        }
        Optional<Customer> customerOpt = customerService.findCustomerByTenantIdAndTitle(tenantId, customerName);
        if (customerOpt.isPresent()) {
            return customerOpt.get().getId();
        } else {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setTitle(customerName);
            return customerService.saveCustomer(customer).getId();
        }
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardName`：名称。
     * 返回：可能存在的结果。
     */
    private Optional<DashboardId> getDashboardId(TenantId tenantId, String dashboardName) {
        return Optional.ofNullable(dashboardService.findFirstDashboardInfoByTenantIdAndName(tenantId, dashboardName)).map(IdBased::getId);
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `dashboardName`：名称。
     * 返回：可能存在的结果。
     */
    private Optional<DashboardId> getDashboardId(TenantId tenantId, CustomerId customerId, String dashboardName) {
        PageData<DashboardInfo> dashboardsPage;
        PageLink pageLink = null;
        do {
            pageLink = pageLink == null ? new PageLink(DASHBOARDS_REQUEST_LIMIT) : pageLink.nextPageLink();
            dashboardsPage = dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            Optional<DashboardInfo> dashboardInfoOpt = dashboardsPage.getData().stream()
                    .filter(dashboardInfo -> dashboardName.equals(dashboardInfo.getName()))
                    .findAny();
            if (dashboardInfoOpt.isPresent()) {
                return dashboardInfoOpt.map(DashboardInfo::getId);
            }
        } while (dashboardsPage.hasNext());
        return Optional.empty();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractOAuth2ClientMapper` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
