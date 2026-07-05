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
package org.thingsboard.server.system;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.controller.AbstractControllerTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Illia Barkov
 */

/**
 * 中文说明：
 * 1. 类目的：`BaseRestApiLimitsTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
@Slf4j
public abstract class BaseRestApiLimitsTest extends AbstractControllerTest {

    /**
     * 数量限制常量，用于统一引用固定值。
     */
    private static int MESSAGES_LIMIT = 10;
    private static int TIME_FOR_LIMIT = 5;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    TenantProfile tenantProfile;

    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() throws Exception {
        loginSysAdmin();
        tenantProfile = getDefaultTenantProfile();
        resetTokens();
    }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() throws Exception {
        resetTokens();
        loginSysAdmin();
        saveTenantProfileWitConfiguration(tenantProfile, new DefaultTenantProfileConfiguration());
        resetTokens();
        service.shutdown();
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCustomerRestApiLimits() throws Exception {
        loginSysAdmin();

        String customerRestLimit = MESSAGES_LIMIT + ":" + TIME_FOR_LIMIT;

        DefaultTenantProfileConfiguration configurationWithCustomerRestLimits = createTenantProfileConfigurationWithRestLimits(null, customerRestLimit);

        saveTenantProfileWitConfiguration(tenantProfile, configurationWithCustomerRestLimits);

        resetTokens();

        loginCustomerUser();

        for (int i = 0; i < MESSAGES_LIMIT; i++) {
            doGet("/api/device/types").andExpect(status().isOk());
        }
        doGet("/api/device/types").andExpect(status().is4xxClientError());
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testTenantRestApiLimits() throws Exception {
        loginSysAdmin();

        String tenantRestLimit = MESSAGES_LIMIT + ":" + TIME_FOR_LIMIT;

        DefaultTenantProfileConfiguration configurationWithTenantRestLimits = createTenantProfileConfigurationWithRestLimits(tenantRestLimit, null);

        saveTenantProfileWitConfiguration(tenantProfile, configurationWithTenantRestLimits);

        resetTokens();

        loginCustomerUser();

        for (int i = 0; i < MESSAGES_LIMIT; i++) {
            doGet("/api/device/types").andExpect(status().isOk());
        }
        doGet("/api/device/types").andExpect(status().is4xxClientError());
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCustomerRestApiLimitsWithAsyncMethod() throws Exception {
        loginSysAdmin();

        String tenantRestLimit = MESSAGES_LIMIT + ":" + TIME_FOR_LIMIT;

        DefaultTenantProfileConfiguration configurationWithTenantRestLimits = createTenantProfileConfigurationWithRestLimits(tenantRestLimit, null);

        saveTenantProfileWitConfiguration(tenantProfile, configurationWithTenantRestLimits);

        resetTokens();

        loginTenantAdmin();

        List<ListenableFuture<ResultActions>> attributesRequests = new ArrayList<>();

        doGet("/api/plugins/telemetry/" + tenantId.getEntityType() + "/" + tenantId.getId().toString() + "/values/attributes").andExpect(status().isOk());
        Thread.sleep(TimeUnit.SECONDS.toMillis(TIME_FOR_LIMIT)); // Wait to initialization for bucket4j

        for (int i = 0; i < MESSAGES_LIMIT; i++) {
            attributesRequests.add(service.submit(() -> doGet("/api/plugins/telemetry/" + tenantId.getEntityType() + "/" + tenantId.getId().toString() + "/values/attributes")));
        }

        List<ResultActions> lists = blockForResponses(attributesRequests);

        for (ResultActions resultActions : lists) {
            resultActions.andExpect(status().isOk());
        }

        doGet("/api/plugins/telemetry/" + tenantId.getEntityType() + "/" + tenantId.getId().toString() + "/values/attributes").andExpect(status().is4xxClientError());
    }

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    private TenantProfile getDefaultTenantProfile() throws Exception {

        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        List<TenantProfile> tenantProfiles = new ArrayList<>(pageData.getData());

        Optional<TenantProfile> optionalDefaultProfile = tenantProfiles.stream().filter(TenantProfile::isDefault).reduce((a, b) -> null);
        Assert.assertTrue(optionalDefaultProfile.isPresent());

        return optionalDefaultProfile.get();
    }

    /**
     * 功能：执行 `blockForResponses` 对应的处理。
     * 参数：
     * - `futures`：数据列表。
     * 返回：匹配的数据集合。
     */
    List<ResultActions> blockForResponses(List<ListenableFuture<ResultActions>> futures) throws ExecutionException {
        ListenableFuture<List<ResultActions>> futureOfList = Futures.allAsList(futures);
        List<ResultActions> responses;
        try {
            responses = futureOfList.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            responses = new ArrayList<>();
            for (ListenableFuture<ResultActions> future : futures) {
                if (future.isDone()) {
                    responses.add(Uninterruptibles.getUninterruptibly(future));
                }
            }
        }
        return responses;
    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenantLimits`：租户信息或租户标识。
     * - `customerLimits`：数量限制。
     * 返回：处理结果。
     */
    private DefaultTenantProfileConfiguration createTenantProfileConfigurationWithRestLimits(String tenantLimits, String customerLimits) {
        DefaultTenantProfileConfiguration.DefaultTenantProfileConfigurationBuilder builder = DefaultTenantProfileConfiguration.builder();
        builder.tenantServerRestLimitsConfiguration(tenantLimits);
        builder.customerServerRestLimitsConfiguration(customerLimits);
        return builder.build();

    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * - `tenantProfileConfiguration`：租户信息或租户标识。
     * 返回：无。
     */
    private void saveTenantProfileWitConfiguration(TenantProfile tenantProfile, TenantProfileConfiguration tenantProfileConfiguration) {
        TenantProfileData tenantProfileData = tenantProfile.getProfileData();
        tenantProfileData.setConfiguration(tenantProfileConfiguration);
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseRestApiLimitsTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
