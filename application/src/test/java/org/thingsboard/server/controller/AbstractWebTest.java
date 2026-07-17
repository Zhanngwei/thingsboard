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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.actors.DefaultTbActorSystem;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbActorMailbox;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.device.DeviceActor;
import org.thingsboard.server.actors.device.DeviceActorMessageProcessor;
import org.thingsboard.server.actors.device.SessionInfo;
import org.thingsboard.server.actors.device.ToDeviceRpcRequestMetadata;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.config.ThingsboardSecurityConfiguration;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.entitiy.tenant.profile.TbTenantProfileService;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRequest;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 中文说明：
 * 1. `AbstractWebTest` 是 ThingsBoard Application 中验证 `AbstractWeb` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractInMemoryStorageTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public abstract class AbstractWebTest extends AbstractInMemoryStorageTest {

    /**
     * 超时时间常量，用于统一引用固定值。
     */
    public static final int TIMEOUT = 30;

    /**
     * 租户常量，用于统一引用固定值。
     */
    protected static final String TEST_TENANT_NAME = "TEST TENANT";
    protected static final String TEST_DIFFERENT_TENANT_NAME = "TEST DIFFERENT TENANT";

    /**
     * 邮箱常量，用于统一引用固定值。
     */
    protected static final String SYS_ADMIN_EMAIL = "sysadmin@thingsboard.org";
    private static final String SYS_ADMIN_PASSWORD = "sysadmin";

    /**
     * 租户常量，用于统一引用固定值。
     */
    protected static final String TENANT_ADMIN_EMAIL = "testtenant@thingsboard.org";
    protected static final String TENANT_ADMIN_PASSWORD = "tenant";

    /**
     * 租户常量，用于统一引用固定值。
     */
    protected static final String DIFFERENT_TENANT_ADMIN_EMAIL = "testdifftenant@thingsboard.org";
    private static final String DIFFERENT_TENANT_ADMIN_PASSWORD = "difftenant";

    /**
     * 客户常量，用于统一引用固定值。
     */
    protected static final String CUSTOMER_USER_EMAIL = "testcustomer@thingsboard.org";
    private static final String CUSTOMER_USER_PASSWORD = "customer";

    /**
     * 客户常量，用于统一引用固定值。
     */
    protected static final String DIFFERENT_CUSTOMER_USER_EMAIL = "testdifferentcustomer@thingsboard.org";

    /**
     * 租户常量，用于统一引用固定值。
     */
    protected static final String DIFFERENT_TENANT_CUSTOMER_USER_EMAIL = "testdifferenttenantcustomer@thingsboard.org";
    private static final String DIFFERENT_CUSTOMER_USER_PASSWORD = "diffcustomer";

    /**
     * See {@link org.springframework.test.web.servlet.DefaultMvcResult#getAsyncResult(long)}
     * and {@link org.springframework.mock.web.MockAsyncContext#getTimeout()}
     */
    /**
     * 超时时间常量，用于统一引用固定值。
     */
    private static final long DEFAULT_TIMEOUT = -1L;
    private static final int CLEANUP_TENANT_RETRIES_COUNT = 3;

    /**
     * 类型，用于区分不同处理分支。
     */
    protected MediaType contentType = MediaType.APPLICATION_JSON;

    /**
     * `mockMvc` 字段，保存当前对象的对应属性。
     */
    protected MockMvc mockMvc;

    /**
     * 令牌，用于认证或安全校验。
     */
    protected String currentActivateToken;
    protected String currentResetPasswordToken;

    /**
     * 令牌，用于认证或安全校验。
     */
    protected String token;
    protected String refreshToken;
    /**
     * 令牌，用于认证或安全校验。
     */
    protected String mobileToken;
    protected String username;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    protected TenantId tenantId;
    protected TenantProfileId tenantProfileId;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    protected UserId tenantAdminUserId;
    protected User tenantAdminUser;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    protected CustomerId tenantAdminCustomerId;
    protected CustomerId customerId;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    protected TenantId differentTenantId;
    protected CustomerId differentCustomerId;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    protected CustomerId differentTenantCustomerId;
    protected UserId customerUserId;
    /**
     * 客户ID，用于定位对应业务对象。
     */
    protected UserId differentCustomerUserId;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    protected UserId differentTenantCustomerUserId;

    /**
     * 消息，用于在不同数据结构之间转换。
     */
    @SuppressWarnings("rawtypes")
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    /**
     * 消息，用于在不同数据结构之间转换。
     */
    @SuppressWarnings("rawtypes")
    private HttpMessageConverter stringHttpMessageConverter;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    private WebApplicationContext webApplicationContext;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    private TenantProfileService tenantProfileService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    private TbTenantProfileService tbTenantProfileService;

    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Autowired
    public TimeseriesService tsService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected DefaultActorService actorService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @SpyBean
    protected MailService mailService;

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            log.info("Starting test: {}", description.getMethodName());
        }

        protected void finished(Description description) {
            log.info("Finished test: {}", description.getMethodName());
        }
    };

    /**
     * 功能：更新`Converters`。
     * 参数：
     * - `converters`：`converters` 参数。
     * 返回：无。
     */
    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.stream(converters)
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .get();

        this.stringHttpMessageConverter = Arrays.stream(converters)
                .filter(hmc -> hmc instanceof StringHttpMessageConverter)
                .findAny()
                .get();

        Assert.assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    /**
     * 功能：执行 `setupWebTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setupWebTest() throws Exception {
        log.debug("Executing web test setup");

        setupMailServiceMock();

        if (this.mockMvc == null) {
            this.mockMvc = webAppContextSetup(webApplicationContext)
                    .apply(springSecurity()).build();
        }
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle(TEST_TENANT_NAME);
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
        tenantProfileId = savedTenant.getTenantProfileId();

        tenantAdminUser = new User();
        tenantAdminUser.setAuthority(Authority.TENANT_ADMIN);
        tenantAdminUser.setTenantId(tenantId);
        tenantAdminUser.setEmail(TENANT_ADMIN_EMAIL);

        tenantAdminUser = createUserAndLogin(tenantAdminUser, TENANT_ADMIN_PASSWORD);
        tenantAdminUserId = tenantAdminUser.getId();
        tenantAdminCustomerId = tenantAdminUser.getCustomerId();

        Customer customer = new Customer();
        customer.setTitle("Customer");
        customer.setTenantId(tenantId);
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        customerId = savedCustomer.getId();

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(tenantId);
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail(CUSTOMER_USER_EMAIL);

        customerUser = createUserAndLogin(customerUser, CUSTOMER_USER_PASSWORD);
        customerUserId = customerUser.getId();

        resetTokens();

        log.debug("Executed web test setup");
    }

    /**
     * 功能：执行 `setupMailServiceMock` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void setupMailServiceMock() throws ThingsboardException {
        Mockito.doNothing().when(mailService).sendAccountActivatedEmail(anyString(), anyString());
        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String activationLink = (String) args[0];
                currentActivateToken = activationLink.split("=")[1];
                return null;
            }
        }).when(mailService).sendActivationEmail(anyString(), anyString());

        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String passwordResetLink = (String) args[0];
                currentResetPasswordToken = passwordResetLink.split("=")[1];
                return null;
            }
        }).when(mailService).sendResetPasswordEmailAsync(anyString(), anyString());
    }

    /**
     * 功能：执行 `teardownWebTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void teardownWebTest() throws Exception {
        log.debug("Executing web test teardown");

        loginSysAdmin();
        deleteTenant(tenantId);
        deleteDifferentTenant();
        verifyNoTenantsLeft();

        tenantProfileService.deleteTenantProfiles(TenantId.SYS_TENANT_ID);

        log.info("Executed web test teardown");
    }

    /**
     * 功能：校验`No Tenants Left`。
     * 参数：无。
     * 返回：无。
     */
    private void verifyNoTenantsLeft() throws Exception {
        List<Tenant> loadedTenants = getAllTenants();
        if (!loadedTenants.isEmpty()) {
            loadedTenants.forEach(tenant -> deleteTenant(tenant.getId()));
            loadedTenants = getAllTenants();
        }
        assertThat(loadedTenants).as("All tenants expected to be deleted, but some tenants left in the database").isEmpty();
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    private void deleteTenant(TenantId tenantId) {
        int status = 0;
        int retries = 0;
        while (status != HttpStatus.SC_OK && retries < CLEANUP_TENANT_RETRIES_COUNT) {
            retries++;
            try {
                status = doDelete("/api/tenant/" + tenantId.getId().toString())
                        .andReturn().getResponse().getStatus();
                if (status != HttpStatus.SC_OK) {
                    log.warn("Tenant deletion failed, tenantId: {}", tenantId.getId().toString());
                    Thread.sleep(1000L);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 功能：获取`All Tenants`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private List<Tenant> getAllTenants() throws Exception {
        List<Tenant> loadedTenants = new ArrayList<>();
        PageLink pageLink = new PageLink(10);
        PageData<Tenant> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<PageData<Tenant>>() {
            }, pageLink);
            loadedTenants.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        return loadedTenants;
    }

    /**
     * 功能：执行 `loginSysAdmin` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void loginSysAdmin() throws Exception {
        login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
    }

    /**
     * 功能：执行 `loginTenantAdmin` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void loginTenantAdmin() throws Exception {
        login(TENANT_ADMIN_EMAIL, TENANT_ADMIN_PASSWORD);
    }

    /**
     * 功能：执行 `loginCustomerUser` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void loginCustomerUser() throws Exception {
        login(CUSTOMER_USER_EMAIL, CUSTOMER_USER_PASSWORD);
    }

    /**
     * 功能：执行 `loginUser` 对应的处理。
     * 参数：
     * - `userName`：名称。
     * - `password`：`password` 参数。
     * 返回：无。
     */
    protected void loginUser(String userName, String password) throws Exception {
        login(userName, password);
    }

    /**
     * 租户对象，用于描述当前业务场景。
     */
    protected Tenant savedDifferentTenant;
    protected User savedDifferentTenantUser;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    private Customer savedDifferentCustomer;
    private Customer savedDifferentTenantCustomer;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    protected User differentCustomerUser;
    protected User differentTenantCustomerUser;

    /**
     * 功能：执行 `loginDifferentTenant` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void loginDifferentTenant() throws Exception {
        if (savedDifferentTenant != null) {
            login(DIFFERENT_TENANT_ADMIN_EMAIL, DIFFERENT_TENANT_ADMIN_PASSWORD);
        } else {
            createDifferentTenant();
        }
    }

    /**
     * 功能：保存或创建租户。
     * 参数：无。
     * 返回：无。
     */
    protected void createDifferentTenant() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle(TEST_DIFFERENT_TENANT_NAME);
        savedDifferentTenant = doPost("/api/tenant", tenant, Tenant.class);
        differentTenantId = savedDifferentTenant.getId();
        Assert.assertNotNull(savedDifferentTenant);
        User differentTenantAdmin = new User();
        differentTenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        differentTenantAdmin.setTenantId(savedDifferentTenant.getId());
        differentTenantAdmin.setEmail(DIFFERENT_TENANT_ADMIN_EMAIL);
        savedDifferentTenantUser = createUserAndLogin(differentTenantAdmin, DIFFERENT_TENANT_ADMIN_PASSWORD);
    }

    /**
     * 功能：执行 `loginDifferentCustomer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void loginDifferentCustomer() throws Exception {
        if (savedDifferentCustomer != null) {
            login(savedDifferentCustomer.getEmail(), CUSTOMER_USER_PASSWORD);
        } else {
            createDifferentCustomer();

            loginTenantAdmin();
            differentCustomerUser = new User();
            differentCustomerUser.setAuthority(Authority.CUSTOMER_USER);
            differentCustomerUser.setTenantId(tenantId);
            differentCustomerUser.setCustomerId(savedDifferentCustomer.getId());
            differentCustomerUser.setEmail(DIFFERENT_CUSTOMER_USER_EMAIL);

            differentCustomerUser = createUserAndLogin(differentCustomerUser, DIFFERENT_CUSTOMER_USER_PASSWORD);
            differentCustomerUserId = differentCustomerUser.getId();
        }
    }

    /**
     * 功能：执行 `loginDifferentTenantCustomer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void loginDifferentTenantCustomer() throws Exception {
        if (savedDifferentTenantCustomer != null) {
            login(savedDifferentTenantCustomer.getEmail(), CUSTOMER_USER_PASSWORD);
        } else {
            createDifferentTenantCustomer();

            loginDifferentTenant();
            differentTenantCustomerUser = new User();
            differentTenantCustomerUser.setAuthority(Authority.CUSTOMER_USER);
            differentTenantCustomerUser.setTenantId(savedDifferentTenantCustomer.getTenantId());
            differentTenantCustomerUser.setCustomerId(savedDifferentTenantCustomer.getId());
            differentTenantCustomerUser.setEmail(DIFFERENT_TENANT_CUSTOMER_USER_EMAIL);

            differentTenantCustomerUser = createUserAndLogin(differentTenantCustomerUser, DIFFERENT_CUSTOMER_USER_PASSWORD);
            differentTenantCustomerUserId = differentTenantCustomerUser.getId();
        }
    }

    /**
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：无。
     */
    protected void createDifferentCustomer() throws Exception {
        loginTenantAdmin();

        Customer customer = new Customer();
        customer.setTitle("Different customer");
        savedDifferentCustomer = doPost("/api/customer", customer, Customer.class);
        Assert.assertNotNull(savedDifferentCustomer);
        differentCustomerId = savedDifferentCustomer.getId();

        resetTokens();
    }

    /**
     * 功能：保存或创建租户。
     * 参数：无。
     * 返回：无。
     */
    protected void createDifferentTenantCustomer() throws Exception {
        loginDifferentTenant();

        Customer customer = new Customer();
        customer.setTitle("Different tenant customer");
        savedDifferentTenantCustomer = doPost("/api/customer", customer, Customer.class);
        Assert.assertNotNull(savedDifferentTenantCustomer);
        differentTenantCustomerId = savedDifferentTenantCustomer.getId();

        resetTokens();
    }

    /**
     * 功能：删除或清理租户。
     * 参数：无。
     * 返回：无。
     */
    protected void deleteDifferentTenant() throws Exception {
        if (savedDifferentTenant != null) {
            loginSysAdmin();
            deleteTenant(savedDifferentTenant.getId());
            savedDifferentTenant = null;
        }
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `user`：`user` 参数。
     * - `password`：`password` 参数。
     * 返回：处理结果。
     */
    protected User createUserAndLogin(User user, String password) throws Exception {
        User savedUser = doPost("/api/user", user, User.class);
        resetTokens();
        JsonNode activateRequest = getActivateRequest(password);
        JsonNode tokenInfo = readResponse(doPost("/api/noauth/activate", activateRequest).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, user.getEmail());
        return savedUser;
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `user`：`user` 参数。
     * - `password`：`password` 参数。
     * 返回：处理结果。
     */
    protected User createUser(User user, String password) throws Exception {
        User savedUser = doPost("/api/user", user, User.class);
        JsonNode activateRequest = getActivateRequest(password);
        ResultActions resultActions = doPost("/api/noauth/activate", activateRequest);
        resultActions.andExpect(status().isOk());
        return savedUser;
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `password`：`password` 参数。
     * 返回：处理结果。
     */
    private JsonNode getActivateRequest(String password) throws Exception {
        doGet("/api/noauth/activate?activateToken={activateToken}", this.currentActivateToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/createPassword?activateToken=" + this.currentActivateToken));
        return JacksonUtil.newObjectNode()
                .put("activateToken", this.currentActivateToken)
                .put("password", password);
    }

    /**
     * 功能：执行 `login` 对应的处理。
     * 参数：
     * - `username`：名称。
     * - `password`：`password` 参数。
     * 返回：无。
     */
    protected void login(String username, String password) throws Exception {
        resetTokens();
        JsonNode tokenInfo = readResponse(doPost("/api/auth/login", new LoginRequest(username, password)).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, username);
    }

    /**
     * 功能：更新令牌。
     * 参数：无。
     * 返回：无。
     */
    protected void refreshToken() throws Exception {
        this.token = null;
        JsonNode tokenInfo = readResponse(doPost("/api/auth/token", new RefreshTokenRequest(this.refreshToken)).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, this.username);
    }

    /**
     * 功能：校验令牌。
     * 参数：
     * - `tokenInfo`：`tokenInfo` 参数。
     * - `username`：名称。
     * 返回：无。
     */
    protected void validateAndSetJwtToken(JsonNode tokenInfo, String username) {
        Assert.assertNotNull(tokenInfo);
        Assert.assertTrue(tokenInfo.has("token"));
        Assert.assertTrue(tokenInfo.has("refreshToken"));
        String token = tokenInfo.get("token").asText();
        String refreshToken = tokenInfo.get("refreshToken").asText();
        validateJwtToken(token, username);
        validateJwtToken(refreshToken, username);
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
    }

    /**
     * 功能：校验令牌。
     * 参数：
     * - `token`：`token` 参数。
     * - `username`：名称。
     * 返回：无。
     */
    protected void validateJwtToken(String token, String username) {
        Assert.assertNotNull(token);
        Assert.assertFalse(token.isEmpty());
        int i = token.lastIndexOf('.');
        Assert.assertTrue(i > 0);
        String withoutSignature = token.substring(0, i + 1);
        Jwt<Header, Claims> jwsClaims = Jwts.parser().parseClaimsJwt(withoutSignature);
        Claims claims = jwsClaims.getBody();
        String subject = claims.getSubject();
        Assert.assertEquals(username, subject);
    }

    /**
     * 功能：执行 `resetTokens` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void resetTokens() throws Exception {
        this.token = null;
        this.refreshToken = null;
        this.username = null;
    }

    /**
     * 功能：执行 `logout` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void logout() throws Exception {
        doPost("/api/auth/logout").andExpect(status().isOk());
    }

    /**
     * 功能：更新令牌。
     * 参数：
     * - `request`：请求对象。
     * 返回：无。
     */
    protected void setJwtToken(MockHttpServletRequestBuilder request) {
        if (this.token != null) {
            request.header(ThingsboardSecurityConfiguration.JWT_TOKEN_HEADER_PARAM, "Bearer " + this.token);
        }
        if (this.mobileToken != null) {
            request.header(UserController.MOBILE_TOKEN_HEADER, this.mobileToken);
        }
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    protected DeviceProfile createDeviceProfile(String name) {
        return createDeviceProfile(name, null);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `name`：名称。
     * - `deviceProfileTransportConfiguration`：设备信息或设备标识。
     * 返回：处理结果。
     */
    protected DeviceProfile createDeviceProfile(String name, DeviceProfileTransportConfiguration deviceProfileTransportConfiguration) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(name);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setDescription(name + " Test");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        deviceProfileData.setConfiguration(configuration);
        if (deviceProfileTransportConfiguration != null) {
            deviceProfile.setTransportType(deviceProfileTransportConfiguration.getType());
            deviceProfileData.setTransportConfiguration(deviceProfileTransportConfiguration);
        } else {
            deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
            deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        }
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(null);
        return deviceProfile;
    }

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    protected AssetProfile createAssetProfile(String name) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(name);
        assetProfile.setDescription(name + " Test");
        assetProfile.setDefault(false);
        assetProfile.setDefaultRuleChainId(null);
        return assetProfile;
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `name`：名称。
     * - `accessToken`：`accessToken` 参数。
     * 返回：处理结果。
     */
    protected Device createDevice(String name, String accessToken) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        device.setDeviceData(deviceData);
        return doPost("/api/device?accessToken=" + accessToken, device, Device.class);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `transportPayloadTypeConfiguration`：配置对象。
     * - `sendAckOnValidationException`：`sendAckOnValidationException` 参数。
     * 返回：处理结果。
     */
    protected MqttDeviceProfileTransportConfiguration createMqttDeviceProfileTransportConfiguration(TransportPayloadTypeConfiguration transportPayloadTypeConfiguration, boolean sendAckOnValidationException) {
        MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = new MqttDeviceProfileTransportConfiguration();
        mqttDeviceProfileTransportConfiguration.setDeviceTelemetryTopic(MqttTopics.DEVICE_TELEMETRY_TOPIC);
        mqttDeviceProfileTransportConfiguration.setDeviceAttributesTopic(MqttTopics.DEVICE_ATTRIBUTES_TOPIC);
        mqttDeviceProfileTransportConfiguration.setDeviceAttributesSubscribeTopic(MqttTopics.DEVICE_ATTRIBUTES_TOPIC);
        mqttDeviceProfileTransportConfiguration.setSendAckOnValidationException(sendAckOnValidationException);
        mqttDeviceProfileTransportConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
        return mqttDeviceProfileTransportConfiguration;
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `transportPayloadTypeConfiguration`：配置对象。
     * - `sendAckOnValidationException`：`sendAckOnValidationException` 参数。
     * - `telemetryTopic`：主题名称或主题对象。
     * - `attributesPublishTopic`：主题名称或主题对象。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected MqttDeviceProfileTransportConfiguration createMqttDeviceProfileTransportConfiguration(TransportPayloadTypeConfiguration transportPayloadTypeConfiguration, boolean sendAckOnValidationException,
                                                                                                    String telemetryTopic, String attributesPublishTopic, String attributesSubscribeTopic) {
        MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = new MqttDeviceProfileTransportConfiguration();
        mqttDeviceProfileTransportConfiguration.setDeviceTelemetryTopic(telemetryTopic);
        mqttDeviceProfileTransportConfiguration.setDeviceAttributesTopic(attributesPublishTopic);
        mqttDeviceProfileTransportConfiguration.setDeviceAttributesSubscribeTopic(attributesSubscribeTopic);
        mqttDeviceProfileTransportConfiguration.setSendAckOnValidationException(sendAckOnValidationException);
        mqttDeviceProfileTransportConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
        return mqttDeviceProfileTransportConfiguration;
    }

    /**
     * 功能：保存或创建消息载荷。
     * 参数：
     * - `attributesProtoSchema`：`attributesProtoSchema` 参数。
     * - `telemetryProtoSchema`：`telemetryProtoSchema` 参数。
     * - `rpcRequestProtoSchema`：请求对象。
     * - `rpcResponseProtoSchema`：响应对象。
     * 返回：处理结果。
     */
    protected ProtoTransportPayloadConfiguration createProtoTransportPayloadConfiguration(String attributesProtoSchema, String telemetryProtoSchema, String rpcRequestProtoSchema, String rpcResponseProtoSchema) {
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = new ProtoTransportPayloadConfiguration();
        protoTransportPayloadConfiguration.setDeviceAttributesProtoSchema(attributesProtoSchema);
        protoTransportPayloadConfiguration.setDeviceTelemetryProtoSchema(telemetryProtoSchema);
        protoTransportPayloadConfiguration.setDeviceRpcRequestProtoSchema(rpcRequestProtoSchema);
        protoTransportPayloadConfiguration.setDeviceRpcResponseProtoSchema(rpcResponseProtoSchema);
        return protoTransportPayloadConfiguration;
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `type`：类型。
     * - `accessToken`：`accessToken` 参数。
     * 返回：处理结果。
     */
    protected Device createDevice(String deviceName, String type, String accessToken) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType(type);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        credentials.setCredentialsId(accessToken);

        SaveDeviceWithCredentialsRequest request = new SaveDeviceWithCredentialsRequest(device, credentials);
        return doPost("/api/device-with-credentials", request, Device.class);
    }

    /**
     * 功能：执行 `doGet` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected ResultActions doGet(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest = get(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(getRequest);
    }

    /**
     * 功能：执行 `doGet` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `httpHeaders`：`httpHeaders` 参数。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected ResultActions doGet(String urlTemplate, HttpHeaders httpHeaders, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest = get(urlTemplate, urlVariables);
        getRequest.headers(httpHeaders);
        setJwtToken(getRequest);
        return mockMvc.perform(getRequest);
    }

    /**
     * 功能：执行 `doGet` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `responseClass`：响应对象。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected <T> T doGet(String urlTemplate, Class<T> responseClass, Object... urlVariables) throws Exception {
        return readResponse(doGet(urlTemplate, urlVariables).andExpect(status().isOk()), responseClass);
    }

    /**
     * 功能：执行 `doGet` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `responseClass`：响应对象。
     * - `resultMatcher`：`resultMatcher` 参数。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected <T> T doGet(String urlTemplate, Class<T> responseClass, ResultMatcher resultMatcher, Object... urlVariables) throws Exception {
        return readResponse(doGet(urlTemplate, urlVariables).andExpect(resultMatcher), responseClass);
    }

    /**
     * 功能：执行 `doGetAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `responseClass`：响应对象。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected <T> T doGetAsync(String urlTemplate, Class<T> responseClass, Object... urlVariables) throws Exception {
        return readResponse(doGetAsync(urlTemplate, urlVariables).andExpect(status().isOk()), responseClass);
    }

    /**
     * 功能：执行 `doGetAsyncTyped` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `responseType`：响应对象。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected <T> T doGetAsyncTyped(String urlTemplate, TypeReference<T> responseType, Object... urlVariables) throws Exception {
        return readResponse(doGetAsync(urlTemplate, urlVariables).andExpect(status().isOk()), responseType);
    }

    /**
     * 功能：执行 `doGetAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected ResultActions doGetAsync(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest;
        getRequest = get(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(asyncDispatch(mockMvc.perform(getRequest).andExpect(request().asyncStarted()).andReturn()));
    }

    /**
     * 功能：执行 `doGetTyped` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `responseType`：响应对象。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected <T> T doGetTyped(String urlTemplate, TypeReference<T> responseType, Object... urlVariables) throws Exception {
        return readResponse(doGet(urlTemplate, urlVariables).andExpect(status().isOk()), responseType);
    }

    /**
     * 功能：执行 `doGetTypedWithPageLink` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `responseType`：响应对象。
     * - `pageLink`：`pageLink` 参数。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected <T> T doGetTypedWithPageLink(String urlTemplate, TypeReference<T> responseType,
                                           PageLink pageLink,
                                           Object... urlVariables) throws Exception {
        List<Object> pageLinkVariables = new ArrayList<>();
        urlTemplate += "pageSize={pageSize}&page={page}";
        pageLinkVariables.add(pageLink.getPageSize());
        pageLinkVariables.add(pageLink.getPage());
        if (StringUtils.isNotEmpty(pageLink.getTextSearch())) {
            urlTemplate += "&textSearch={textSearch}";
            pageLinkVariables.add(pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            urlTemplate += "&sortProperty={sortProperty}&sortOrder={sortOrder}";
            pageLinkVariables.add(pageLink.getSortOrder().getProperty());
            pageLinkVariables.add(pageLink.getSortOrder().getDirection().name());
        }

        Object[] vars = new Object[urlVariables.length + pageLinkVariables.size()];
        System.arraycopy(urlVariables, 0, vars, 0, urlVariables.length);
        System.arraycopy(pageLinkVariables.toArray(), 0, vars, urlVariables.length, pageLinkVariables.size());

        return readResponse(doGet(urlTemplate, vars).andExpect(status().isOk()), responseType);
    }

    /**
     * 功能：执行 `doGetTypedWithTimePageLink` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `responseType`：响应对象。
     * - `pageLink`：`pageLink` 参数。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected <T> T doGetTypedWithTimePageLink(String urlTemplate, TypeReference<T> responseType,
                                               TimePageLink pageLink,
                                               Object... urlVariables) throws Exception {
        List<Object> pageLinkVariables = new ArrayList<>();
        urlTemplate += "pageSize={pageSize}&page={page}";
        pageLinkVariables.add(pageLink.getPageSize());
        pageLinkVariables.add(pageLink.getPage());
        if (pageLink.getStartTime() != null) {
            urlTemplate += "&startTime={startTime}";
            pageLinkVariables.add(pageLink.getStartTime());
        }
        if (pageLink.getEndTime() != null) {
            urlTemplate += "&endTime={endTime}";
            pageLinkVariables.add(pageLink.getEndTime());
        }
        if (StringUtils.isNotEmpty(pageLink.getTextSearch())) {
            urlTemplate += "&textSearch={textSearch}";
            pageLinkVariables.add(pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            urlTemplate += "&sortProperty={sortProperty}&sortOrder={sortOrder}";
            pageLinkVariables.add(pageLink.getSortOrder().getProperty());
            pageLinkVariables.add(pageLink.getSortOrder().getDirection().name());
        }
        Object[] vars = new Object[urlVariables.length + pageLinkVariables.size()];
        System.arraycopy(urlVariables, 0, vars, 0, urlVariables.length);
        System.arraycopy(pageLinkVariables.toArray(), 0, vars, urlVariables.length, pageLinkVariables.size());

        return readResponse(doGet(urlTemplate, vars).andExpect(status().isOk()), responseType);
    }

    /**
     * 功能：执行 `doPost` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `responseClass`：响应对象。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected <T> T doPost(String urlTemplate, Class<T> responseClass, String... params) {
        try {
            return readResponse(doPost(urlTemplate, params).andExpect(status().isOk()), responseClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：执行 `doPost` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `responseClass`：响应对象。
     * - `resultMatcher`：`resultMatcher` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected <T> T doPost(String urlTemplate, T content, Class<T> responseClass, ResultMatcher resultMatcher, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, content, params).andExpect(resultMatcher), responseClass);
    }

    /**
     * 功能：执行 `doPost` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `responseClass`：响应对象。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected <T, R> R doPost(String urlTemplate, T content, Class<R> responseClass, String... params) {
        try {
            return readResponse(doPost(urlTemplate, content, params).andExpect(status().isOk()), responseClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：执行 `doPostWithResponse` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `responseClass`：响应对象。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected <T, R> R doPostWithResponse(String urlTemplate, T content, Class<R> responseClass, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, content, params).andExpect(status().isOk()), responseClass);
    }

    /**
     * 功能：执行 `doPostWithTypedResponse` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `responseType`：响应对象。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected <T, R> R doPostWithTypedResponse(String urlTemplate, T content, TypeReference<R> responseType, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, content, params).andExpect(status().isOk()), responseType);
    }

    /**
     * 功能：执行 `doPostWithTypedResponse` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `responseType`：响应对象。
     * - `resultMatcher`：`resultMatcher` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected <T, R> R doPostWithTypedResponse(String urlTemplate, T content, TypeReference<R> responseType, ResultMatcher resultMatcher, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, content, params).andExpect(resultMatcher), responseType);
    }

    /**
     * 功能：执行 `doPostAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `responseClass`：响应对象。
     * - `resultMatcher`：`resultMatcher` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected <T> T doPostAsync(String urlTemplate, T content, Class<T> responseClass, ResultMatcher resultMatcher, String... params) throws Exception {
        return readResponse(doPostAsync(urlTemplate, content, DEFAULT_TIMEOUT, params).andExpect(resultMatcher), responseClass);
    }

    /**
     * 功能：执行 `doPostAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `responseClass`：响应对象。
     * - `resultMatcher`：`resultMatcher` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected <T> T doPostAsync(String urlTemplate, T content, Class<T> responseClass, ResultMatcher resultMatcher, Long timeout, String... params) throws Exception {
        return readResponse(doPostAsync(urlTemplate, content, timeout, params).andExpect(resultMatcher), responseClass);
    }

    /**
     * 功能：执行 `doPostClaimAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `responseClass`：响应对象。
     * - `resultMatcher`：`resultMatcher` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected <T> T doPostClaimAsync(String urlTemplate, Object content, Class<T> responseClass, ResultMatcher resultMatcher, String... params) throws Exception {
        return readResponse(doPostAsync(urlTemplate, content, DEFAULT_TIMEOUT, params).andExpect(resultMatcher), responseClass);
    }

    /**
     * 功能：执行 `doPut` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `responseClass`：响应对象。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected <T> T doPut(String urlTemplate, Object content, Class<T> responseClass, String... params) {
        try {
            return readResponse(doPut(urlTemplate, content, params).andExpect(status().isOk()), responseClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：执行 `doPut` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected <T> ResultActions doPut(String urlTemplate, T content, String... params) throws Exception {
        MockHttpServletRequestBuilder postRequest = put(urlTemplate, params);
        setJwtToken(postRequest);
        String json = json(content);
        postRequest.contentType(contentType).content(json);
        return mockMvc.perform(postRequest);
    }

    /**
     * 功能：执行 `doDelete` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `responseClass`：响应对象。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected <T> T doDelete(String urlTemplate, Class<T> responseClass, String... params) throws Exception {
        return readResponse(doDelete(urlTemplate, params).andExpect(status().isOk()), responseClass);
    }

    /**
     * 功能：执行 `doDeleteAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `responseClass`：响应对象。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected <T> T doDeleteAsync(String urlTemplate, Class<T> responseClass, String... params) throws Exception {
        return readResponse(doDeleteAsync(urlTemplate, DEFAULT_TIMEOUT, params).andExpect(status().isOk()), responseClass);
    }

    /**
     * 功能：执行 `doPost` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected ResultActions doPost(String urlTemplate, String... params) throws Exception {
        MockHttpServletRequestBuilder postRequest = post(urlTemplate);
        setJwtToken(postRequest);
        populateParams(postRequest, params);
        return mockMvc.perform(postRequest);
    }

    /**
     * 功能：执行 `doPost` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected <T> ResultActions doPost(String urlTemplate, T content, String... params) throws Exception {
        MockHttpServletRequestBuilder postRequest = post(urlTemplate, params);
        setJwtToken(postRequest);
        String json = json(content);
        postRequest.contentType(contentType).content(json);
        return mockMvc.perform(postRequest);
    }

    /**
     * 功能：执行 `doPostAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `timeout`：`timeout` 参数。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected <T> ResultActions doPostAsync(String urlTemplate, T content, Long timeout, String... params) throws Exception {
        MockHttpServletRequestBuilder postRequest = post(urlTemplate, params);
        setJwtToken(postRequest);
        String json = json(content);
        postRequest.contentType(contentType).content(json);
        MvcResult result = mockMvc.perform(postRequest).andReturn();
        result.getAsyncResult(timeout);
        return mockMvc.perform(asyncDispatch(result));
    }

    /**
     * 功能：执行 `doDelete` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected ResultActions doDelete(String urlTemplate, String... params) throws Exception {
        MockHttpServletRequestBuilder deleteRequest = delete(urlTemplate);
        setJwtToken(deleteRequest);
        populateParams(deleteRequest, params);
        return mockMvc.perform(deleteRequest);
    }

    /**
     * 功能：执行 `doDeleteAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `timeout`：`timeout` 参数。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected ResultActions doDeleteAsync(String urlTemplate, Long timeout, String... params) throws Exception {
        MockHttpServletRequestBuilder deleteRequest = delete(urlTemplate, params);
        setJwtToken(deleteRequest);
//        populateParams(deleteRequest, params);
        MvcResult result = mockMvc.perform(deleteRequest).andReturn();
        result.getAsyncResult(timeout);
        return mockMvc.perform(asyncDispatch(result));
    }

    /**
     * 功能：执行 `populateParams` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `params`：`params` 参数。
     * 返回：无。
     */
    protected void populateParams(MockHttpServletRequestBuilder request, String... params) {
        if (params != null && params.length > 0) {
            Assert.assertEquals(0, params.length % 2);
            MultiValueMap<String, String> paramsMap = new LinkedMultiValueMap<>();
            for (int i = 0; i < params.length; i += 2) {
                paramsMap.add(params[i], params[i + 1]);
            }
            request.params(paramsMap);
        }
    }

    /**
     * 功能：执行 `json` 对应的处理。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：文本结果。
     */
    @SuppressWarnings("unchecked")
    protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();

        HttpMessageConverter converter = o instanceof String ? stringHttpMessageConverter : mappingJackson2HttpMessageConverter;
        converter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    /**
     * 功能：执行 `readResponse` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * - `responseClass`：响应对象。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    protected <T> T readResponse(ResultActions result, Class<T> responseClass) throws Exception {
        byte[] content = result.andReturn().getResponse().getContentAsByteArray();
        MockHttpInputMessage mockHttpInputMessage = new MockHttpInputMessage(content);
        HttpMessageConverter converter = responseClass.equals(String.class) ? stringHttpMessageConverter : mappingJackson2HttpMessageConverter;
        return (T) converter.read(responseClass, mockHttpInputMessage);
    }

    /**
     * 功能：执行 `readResponse` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    protected <T> T readResponse(ResultActions result, TypeReference<T> type) throws Exception {
        return readResponse(result.andReturn(), type);
    }

    /**
     * 功能：执行 `readResponse` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    protected <T> T readResponse(MvcResult result, TypeReference<T> type) throws Exception {
        byte[] content = result.getResponse().getContentAsByteArray();
        return JacksonUtil.OBJECT_MAPPER.readerFor(type).readValue(content);
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：文本结果。
     */
    protected String getErrorMessage(ResultActions result) throws Exception {
        return readResponse(result, JsonNode.class).get("message").asText();
    }

    /**
     * 中文说明：
     * 1. `IdComparator` 是 ThingsBoard Application 中处理 `Id Comparator` 的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `HasId`、`Comparator`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    public class IdComparator<D extends HasId> implements Comparator<D> {

        /**
         * 功能：执行 `compare` 对应的处理。
         * 参数：
         * - `o1`：`o1` 参数。
         * - `o2`：`o2` 参数。
         * 返回：数值结果。
         */
        @Override
        public int compare(D o1, D o2) {
            return o1.getId().getId().compareTo(o2.getId().getId());
        }

    }

    /**
     * 中文说明：
     * 1. `EntityIdComparator` 是 ThingsBoard Application 中处理实体的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `EntityId`、`Comparator`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    public class EntityIdComparator<D extends EntityId> implements Comparator<D> {

        /**
         * 功能：执行 `compare` 对应的处理。
         * 参数：
         * - `o1`：`o1` 参数。
         * - `o2`：`o2` 参数。
         * 返回：数值结果。
         */
        @Override
        public int compare(D o1, D o2) {
            return o1.getId().compareTo(o2.getId());
        }

    }

    /**
     * 功能：执行 `statusReason` 对应的处理。
     * 参数：
     * - `matcher`：`matcher` 参数。
     * 返回：处理结果。
     */
    protected static <T> ResultMatcher statusReason(Matcher<T> matcher) {
        return jsonPath("$.message", matcher);
    }

    /**
     * 功能：执行 `constructEdge` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `type`：类型。
     * 返回：处理结果。
     */
    protected Edge constructEdge(String name, String type) {
        return constructEdge(tenantId, name, type, StringUtils.randomAlphanumeric(20), StringUtils.randomAlphanumeric(20));
    }

    /**
     * 功能：执行 `constructEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * - `type`：类型。
     * - `routingKey`：键。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected Edge constructEdge(TenantId tenantId, String name, String type, String routingKey, String secret) {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName(name);
        edge.setType(type);
        edge.setRoutingKey(routingKey);
        edge.setSecret(secret);
        return edge;
    }

    /**
     * 功能：删除或清理`Entities Async`。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `entities`：数据列表。
     * - `executor`：数据列表。
     * 返回：匹配的数据集合。
     */
    protected <T extends HasId<? extends UUIDBased>> ListenableFuture<List<ResultActions>> deleteEntitiesAsync(String urlTemplate, List<T> entities, ListeningExecutorService executor) {
        List<ListenableFuture<ResultActions>> futures = new ArrayList<>(entities.size());
        for (T entity : entities) {
            futures.add(executor.submit(() ->
                    doDelete(urlTemplate + entity.getId().getId())
                            .andExpect(status().isOk())));
        }
        return Futures.allAsList(futures);
    }

    /**
     * 功能：验证实体相关场景。
     * 参数：
     * - `entityIdFrom`：实体对象。
     * - `entityTo`：实体对象。
     * - `urlDelete`：`urlDelete` 参数。
     * 返回：无。
     */
    protected void testEntityDaoWithRelationsOk(EntityId entityIdFrom, EntityId entityTo, String urlDelete) throws Exception {
        createEntityRelation(entityIdFrom, entityTo, "TEST_TYPE");
        assertThat(findRelationsByTo(entityTo)).hasSize(1);

        doDelete(urlDelete).andExpect(status().isOk());

        assertThat(findRelationsByTo(entityTo)).hasSize(0);
    }

    /**
     * 功能：验证实体相关场景。
     * 参数：
     * - `dao`：`dao` 参数。
     * - `entityIdFrom`：实体对象。
     * - `entityTo`：实体对象。
     * - `urlDelete`：`urlDelete` 参数。
     * 返回：无。
     */
    protected <T> void testEntityDaoWithRelationsTransactionalException(Dao<T> dao, EntityId entityIdFrom, EntityId entityTo,
                                                                        String urlDelete) throws Exception {
        Mockito.doThrow(new ConstraintViolationException("mock message", new SQLException(), "MOCK_CONSTRAINT")).when(dao).removeById(any(), any());
        try {
            createEntityRelation(entityIdFrom, entityTo, "TEST_TRANSACTIONAL_TYPE");
            assertThat(findRelationsByTo(entityTo)).hasSize(1);

            doDelete(urlDelete)
                    .andExpect(status().isInternalServerError());

            assertThat(findRelationsByTo(entityTo)).hasSize(1);
        } finally {
            Mockito.reset(dao);
        }
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `entityIdFrom`：实体对象。
     * - `entityIdTo`：实体对象。
     * - `typeRelation`：类型。
     * 返回：无。
     */
    protected void createEntityRelation(EntityId entityIdFrom, EntityId entityIdTo, String typeRelation) throws Exception {
        EntityRelation relation = new EntityRelation(entityIdFrom, entityIdTo, typeRelation);
        doPost("/api/relation", relation);
    }

    /**
     * 功能：获取`Relations By To`。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    protected List<EntityRelation> findRelationsByTo(EntityId entityId) throws Exception {
        String url = String.format("/api/relations?toId=%s&toType=%s", entityId.getId(), entityId.getEntityType().name());
        MvcResult mvcResult = doGet(url).andReturn();

        switch (mvcResult.getResponse().getStatus()) {
            case 200:
                return readResponse(mvcResult, new TypeReference<>() {
                });
            case 404:
                return Collections.emptyList();
        }
        throw new AssertionError("Unexpected status " + mvcResult.getResponse().getStatus());
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `target`：`target` 参数。
     * - `fieldName`：名称。
     * 返回：处理结果。
     */
    protected static <T> T getFieldValue(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `targetCls`：`targetCls` 参数。
     * - `fieldName`：名称。
     * - `value`：值。
     * 返回：无。
     */
    protected static void setStaticFieldValue(Class<?> targetCls, String fieldName, Object value) throws Exception {
        Field field = targetCls.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `targetCls`：`targetCls` 参数。
     * - `fieldName`：名称。
     * - `value`：值。
     * 返回：无。
     */
    protected static void setStaticFinalFieldValue(Class<?> targetCls, String fieldName, Object value) throws Exception {
        Field field = targetCls.getDeclaredField(fieldName);
        field.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, value);
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `featureType`：类型。
     * 返回：数值结果。
     */
    protected int getDeviceActorSubscriptionCount(DeviceId deviceId, FeatureType featureType) {
        DeviceActorMessageProcessor processor = getDeviceActorProcessor(deviceId);
        Map<UUID, SessionInfo> subscriptions = (Map<UUID, SessionInfo>) ReflectionTestUtils.getField(processor, getMapName(featureType));
        return subscriptions.size();
    }

    /**
     * 功能：执行 `awaitForDeviceActorToReceiveSubscription` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `featureType`：类型。
     * - `subscriptionCount`：`subscriptionCount` 参数。
     * 返回：无。
     */
    protected void awaitForDeviceActorToReceiveSubscription(DeviceId deviceId, FeatureType featureType, int subscriptionCount) {
        DeviceActorMessageProcessor processor = getDeviceActorProcessor(deviceId);
        Map<UUID, SessionInfo> subscriptions = (Map<UUID, SessionInfo>) ReflectionTestUtils.getField(processor, getMapName(featureType));
        Awaitility.await("Device actor received subscription command from the transport").atMost(5, TimeUnit.SECONDS).until(() -> {
            log.warn("device {}, subscriptions.size() == {}", deviceId, subscriptions.size());
            return subscriptions.size() == subscriptionCount;
        });
    }

    /**
     * 功能：执行 `awaitForDeviceActorToProcessAllRpcResponses` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    protected void awaitForDeviceActorToProcessAllRpcResponses(DeviceId deviceId) {
        DeviceActorMessageProcessor processor = getDeviceActorProcessor(deviceId);
        Map<Integer, ToDeviceRpcRequestMetadata> toDeviceRpcPendingMap = (Map<Integer, ToDeviceRpcRequestMetadata>) ReflectionTestUtils.getField(processor, "toDeviceRpcPendingMap");
        Awaitility.await("Device actor pending map is empty").atMost(5, TimeUnit.SECONDS).until(() -> {
            log.warn("device {}, toDeviceRpcPendingMap.size() == {}", deviceId, toDeviceRpcPendingMap.size());
            return toDeviceRpcPendingMap.isEmpty();
        });
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `featureType`：类型。
     * 返回：文本结果。
     */
    protected static String getMapName(FeatureType featureType) {
        switch (featureType) {
            case ATTRIBUTES:
                return "attributeSubscriptions";
            case RPC:
                return "rpcSubscriptions";
            default:
                throw new RuntimeException("Not supported feature " + featureType + "!");
        }
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    protected DeviceActorMessageProcessor getDeviceActorProcessor(DeviceId deviceId) {
        DefaultTbActorSystem actorSystem = (DefaultTbActorSystem) ReflectionTestUtils.getField(actorService, "system");
        ConcurrentMap<TbActorId, TbActorMailbox> actors = (ConcurrentMap<TbActorId, TbActorMailbox>) ReflectionTestUtils.getField(actorSystem, "actors");
        Awaitility.await("Device actor was created").atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> actors.containsKey(new TbEntityActorId(deviceId)));
        TbActorMailbox actorMailbox = actors.get(new TbEntityActorId(deviceId));
        DeviceActor actor = (DeviceActor) ReflectionTestUtils.getField(actorMailbox, "actor");
        return (DeviceActorMessageProcessor) ReflectionTestUtils.getField(actor, "processor");
    }

    /**
     * 功能：更新租户。
     * 参数：
     * - `updater`：`updater` 参数。
     * 返回：无。
     */
    protected void updateDefaultTenantProfileConfig(Consumer<DefaultTenantProfileConfiguration> updater) throws ThingsboardException {
        updateDefaultTenantProfile(tenantProfile -> {
            TenantProfileData profileData = tenantProfile.getProfileData();
            DefaultTenantProfileConfiguration profileConfiguration = (DefaultTenantProfileConfiguration) profileData.getConfiguration();
            updater.accept(profileConfiguration);
            tenantProfile.setProfileData(profileData);
        });
    }

    /**
     * 功能：更新租户。
     * 参数：
     * - `updater`：`updater` 参数。
     * 返回：无。
     */
    protected void updateDefaultTenantProfile(Consumer<TenantProfile> updater) throws ThingsboardException {
        TenantProfile oldTenantProfile = tenantProfileService.findDefaultTenantProfile(TenantId.SYS_TENANT_ID);
        TenantProfile tenantProfile = JacksonUtil.clone(oldTenantProfile);
        updater.accept(tenantProfile);
        tbTenantProfileService.save(TenantId.SYS_TENANT_ID, tenantProfile, oldTenantProfile);
    }

}
