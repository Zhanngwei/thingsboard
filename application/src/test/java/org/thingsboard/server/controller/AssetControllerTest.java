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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.stats.DefaultRuleEngineStatisticsService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@ContextConfiguration(classes = {AssetControllerTest.Config.class})
@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`AssetControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class AssetControllerTest extends AbstractControllerTest {

    private IdComparator<Asset> idComparator = new IdComparator<>();

    /**
     * 字段说明：
     * 1. 保存 `savedTenant` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `assetDao` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AssetDao assetDao;

    /**
     * 中文说明：
     * 1. 类目的：`Config` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
     * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 MVC Controller / Facade。
     */
    static class Config {
        @Bean
        @Primary
        /**
         * 方法说明：
         * 1. 职责：执行 `assetDao` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public AssetDao assetDao(AssetDao assetDao) {
            // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
            return Mockito.mock(AssetDao.class, AdditionalAnswers.delegatesTo(assetDao));
        }
    }

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `beforeTest` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `afterTest` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAsset` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAsset() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED);

        Assert.assertNotNull(savedAsset);
        Assert.assertNotNull(savedAsset.getId());
        Assert.assertTrue(savedAsset.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedAsset.getTenantId());
        Assert.assertNotNull(savedAsset.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedAsset.getCustomerId().getId());
        Assert.assertEquals(asset.getName(), savedAsset.getName());

        Mockito.reset(tbClusterService, auditLogService);

        savedAsset.setName("My new asset");
        doPost("/api/asset", savedAsset, Asset.class);

        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED);

        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(foundAsset.getName(), savedAsset.getName());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAssetWithViolationOfLengthValidation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAssetWithViolationOfLengthValidation() throws Exception {
        Asset asset = new Asset();
        asset.setName(StringUtils.randomAlphabetic(300));
        asset.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("name");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        asset.setName("Normal name");
        asset.setType(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("type");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        asset.setType("default");
        asset.setLabel(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("label");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateAssetFromDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUpdateAssetFromDifferentTenant() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/asset", savedAsset)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedAsset.getId(), savedAsset);

        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedAsset.getId(), savedAsset);

        deleteDifferentTenant();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAssetWithProfileFromDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAssetWithProfileFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        AssetProfile differentProfile = createAssetProfile("Different profile");
        differentProfile = doPost("/api/assetProfile", differentProfile, AssetProfile.class);

        loginTenantAdmin();
        Asset asset = new Asset();
        asset.setName("My device");
        asset.setAssetProfileId(differentProfile.getId());
        doPost("/api/asset", asset).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Asset can`t be referencing to asset profile from different tenant!")));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAssetById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindAssetById() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertNotNull(foundAsset);
        Assert.assertEquals(savedAsset, foundAsset);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAssetTypesByTenantId` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindAssetTypesByTenantId() throws Exception {
        AssetProfile assetProfile = createAssetProfile("typeB");
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        Mockito.reset(tbClusterService, auditLogService);

        int cntTime = 3;
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < cntTime; i++) {
            Asset asset = new Asset();
            asset.setName("My asset B" + i);
            asset.setType("typeB");
            asset.setAssetProfileId(assetProfile.getId());
            doPost("/api/asset", asset, Asset.class);
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Asset(), new Asset(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntTime, cntTime, cntTime);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 7; i++) {
            Asset asset = new Asset();
            asset.setName("My asset C" + i);
            asset.setType("typeC");
            doPost("/api/asset", asset, Asset.class);
        }
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 9; i++) {
            Asset asset = new Asset();
            asset.setName("My asset A" + i);
            asset.setType("typeA");
            doPost("/api/asset", asset, Asset.class);
        }
        List<EntitySubtype> assetTypes = doGetTyped("/api/asset/types",
                new TypeReference<List<EntitySubtype>>() {
                });

        Assert.assertNotNull(assetTypes);
        Assert.assertEquals(3, assetTypes.size());
        Assert.assertEquals("typeA", assetTypes.get(0).getType());
        Assert.assertEquals("typeB", assetTypes.get(1).getType());
        Assert.assertEquals("typeC", assetTypes.get(2).getType());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAsset` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAsset() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedAsset.getId().getId().toString());

        String assetIdStr = savedAsset.getId().getId().toString();
        doGet("/api/asset/" + assetIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Asset", assetIdStr))));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAssetWithAlarmsAndAlarmTypes` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAssetWithAlarmsAndAlarmTypes() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(savedAsset.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type("test_type")
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);

        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());

        String assetIdStr = savedAsset.getId().getId().toString();
        doGet("/api/asset/" + assetIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Asset", assetIdStr))));

        doGet("/api/alarm/info/" + alarm.getId())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Alarm", alarm.getId().getId().toString()))));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAssetWithPropagatedAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAssetWithPropagatedAlarm() throws Exception {
        Device device = new Device();
        device.setTenantId(savedTenant.getTenantId());
        device.setName("Test device");
        device.setLabel("Label");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset = doPost("/api/asset", asset, Asset.class);

        EntityRelation entityRelation = new EntityRelation(asset.getId(), device.getId(), "CONTAINS");
        doPost("/api/relation", entityRelation);

        //create alarm
        Alarm alarm = Alarm.builder()
                .tenantId(savedTenant.getTenantId())
                .originator(device.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type("test_type")
                .propagate(true)
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);

        PageData<AlarmInfo> deviceAlarms = doGetTyped("/api/alarm/DEVICE/" + device.getUuidId() + "?page=0&pageSize=10", new TypeReference<>() {
        });
        assertThat(deviceAlarms.getData()).hasSize(1);

        PageData<AlarmInfo> assetAlarms = doGetTyped("/api/alarm/ASSET/" + asset.getUuidId() + "?page=0&pageSize=10", new TypeReference<>() {
        });
        assertThat(assetAlarms.getData()).hasSize(1);

        //delete asset
        doDelete("/api/asset/" + asset.getId().getId().toString())
                .andExpect(status().isOk());

        //check device alarms
        PageData<AlarmInfo> deviceAlarmsAfterAssetDeletion = doGetTyped("/api/alarm/DEVICE/" + device.getUuidId() + "?page=0&pageSize=10", new TypeReference<PageData<AlarmInfo>>() {
        });
        assertThat(deviceAlarmsAfterAssetDeletion.getData()).hasSize(1);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAssetAssignedToEntityView` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAssetAssignedToEntityView() throws Exception {
        Asset asset1 = new Asset();
        asset1.setName("My asset 1");
        asset1.setType("default");
        Asset savedAsset1 = doPost("/api/asset", asset1, Asset.class);

        Asset asset2 = new Asset();
        asset2.setName("My asset 2");
        asset2.setType("default");
        Asset savedAsset2 = doPost("/api/asset", asset2, Asset.class);

        EntityView view = new EntityView();
        view.setEntityId(savedAsset1.getId());
        view.setTenantId(savedTenant.getId());
        view.setName("My entity view");
        view.setType("default");
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Can't delete asset that has entity views";
        doDelete("/api/asset/" + savedAsset1.getId().getId().toString())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeEdgeServiceNeverError(savedAsset1, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, new DataValidationException(msgError), savedAsset1.getId().getId().toString());

        savedView.setEntityId(savedAsset2.getId());

        doPost("/api/entityView", savedView, EntityView.class);

        doDelete("/api/asset/" + savedAsset1.getId().getId().toString())
                .andExpect(status().isOk());

        String assetIdStr = savedAsset1.getId().getId().toString();
        doGet("/api/asset/" + assetIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Asset", assetIdStr))));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAssetWithEmptyType` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAssetWithEmptyType() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");

        Mockito.reset(tbClusterService, auditLogService);

        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        Assert.assertEquals("default", savedAsset.getType());

        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAssetWithEmptyName` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAssetWithEmptyName() throws Exception {
        Asset asset = new Asset();
        asset.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Asset name " + msgErrorShouldBeSpecified;
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignUnassignAssetToCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignUnassignAssetToCustomer() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Mockito.reset(tbClusterService, auditLogService);

        Asset assignedAsset = doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(savedCustomer.getId(), assignedAsset.getCustomerId());

        testNotifyAssignUnassignEntityAllOneTime(assignedAsset, assignedAsset.getId(), assignedAsset.getId(),
                savedTenant.getId(), savedCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ASSIGNED_TO_CUSTOMER, ActionType.UPDATED, assignedAsset.getId().toString(), savedCustomer.getId().toString(), savedCustomer.getTitle());

        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(savedCustomer.getId(), foundAsset.getCustomerId());

        Mockito.reset(tbClusterService, auditLogService);

        Asset unassignedAsset =
                doDelete("/api/customer/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedAsset.getCustomerId().getId());

        testNotifyAssignUnassignEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), savedCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_CUSTOMER, ActionType.UPDATED, savedAsset.getId().toString(), savedCustomer.getId().toString(), savedCustomer.getTitle());

        foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundAsset.getCustomerId().getId());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignUnassignAssetToPublicCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignUnassignAssetToPublicCustomer() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        Asset assignedAsset = doPost("/api/customer/public/asset/" + savedAsset.getId().getId().toString(), Asset.class);

        Customer publicCustomer = doGet("/api/customer/" + assignedAsset.getCustomerId(), Customer.class);
        Assert.assertTrue(publicCustomer.isPublic());

        testNotifyAssignUnassignEntityAllOneTime(assignedAsset, assignedAsset.getId(), assignedAsset.getId(),
                savedTenant.getId(), publicCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ASSIGNED_TO_CUSTOMER, ActionType.UPDATED, assignedAsset.getId().toString(),
                publicCustomer.getId().toString(), publicCustomer.getTitle());

        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(publicCustomer.getId(), foundAsset.getCustomerId());

        Mockito.reset(tbClusterService, auditLogService);

        Asset unassignedAsset =
                doDelete("/api/customer/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedAsset.getCustomerId().getId());

        testNotifyAssignUnassignEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), publicCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_CUSTOMER, ActionType.UPDATED, savedAsset.getId().toString(),
                publicCustomer.getId().toString(), publicCustomer.getTitle());

        foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundAsset.getCustomerId().getId());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignAssetToNonExistentCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignAssetToNonExistentCustomer() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        String customerIdStr = Uuids.timeBased().toString();
        doPost("/api/customer/" + customerIdStr
                + "/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Customer", customerIdStr))));

        testNotifyEntityNever(asset.getId(), asset);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignAssetToCustomerFromDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignAssetToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");

        createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        login(tenantAdmin.getEmail(), "testPassword1");

        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedAsset.getId(), savedAsset);

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant2.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantAssets` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantAssets() throws Exception {
        List<Asset> assets = new ArrayList<>();
        int cntEntity = 178;

        Mockito.reset(tbClusterService, auditLogService);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < cntEntity; i++) {
            Asset asset = new Asset();
            asset.setName("Asset" + i);
            asset.setType("default");
            assets.add(doPost("/api/asset", asset, Asset.class));
        }
        List<Asset> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Asset> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssets.addAll(pageData.getData());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Asset(), new Asset(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, cntEntity, cntEntity);

        loadedAssets.removeIf(asset -> asset.getType().equals(DefaultRuleEngineStatisticsService.TB_SERVICE_QUEUE));

        assets.sort(idComparator);
        loadedAssets.sort(idComparator);

        Assert.assertEquals(assets, loadedAssets);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantAssetsByName` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantAssetsByName() throws Exception {
        String title1 = "Asset title 1";
        List<Asset> assetsTitle1 = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 143; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle1.add(doPost("/api/asset", asset, Asset.class));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 75; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle2.add(doPost("/api/asset", asset, Asset.class));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsTitle1.sort(idComparator);
        loadedAssetsTitle1.sort(idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<Asset> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsTitle2.sort(idComparator);
        loadedAssetsTitle2.sort(idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Asset asset : loadedAssetsTitle1) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                new TypeReference<PageData<Asset>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Asset asset : loadedAssetsTitle2) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                new TypeReference<PageData<Asset>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantAssetsByType` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantAssetsByType() throws Exception {
        String title1 = "Asset title 1";
        String type1 = "typeA";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            assetsType1.add(doPost("/api/asset", asset, Asset.class));
        }
        String title2 = "Asset title 2";
        String type2 = "typeB";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            assetsType2.add(doPost("/api/asset", asset, Asset.class));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink, type1);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsType1.sort(idComparator);
        loadedAssetsType1.sort(idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink, type2);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsType2.sort(idComparator);
        loadedAssetsType2.sort(idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                new TypeReference<PageData<Asset>>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                new TypeReference<PageData<Asset>>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindCustomerAssets` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindCustomerAssets() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            Asset asset = new Asset();
            asset.setName("Asset" + i);
            asset.setType("default");
            asset = doPost("/api/asset", asset, Asset.class);
            assets.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }

        List<Asset> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assets.sort(idComparator);
        loadedAssets.sort(idComparator);

        Assert.assertEquals(assets, loadedAssets);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindCustomerAssetsByName` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindCustomerAssetsByName() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        String title1 = "Asset title 1";
        List<Asset> assetsTitle1 = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = doPost("/api/asset", asset, Asset.class);
            assetsTitle1.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = doPost("/api/asset", asset, Asset.class);
            assetsTitle2.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsTitle1.sort(idComparator);
        loadedAssetsTitle1.sort(idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<Asset> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsTitle2.sort(idComparator);
        loadedAssetsTitle2.sort(idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            doDelete("/api/customer/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            doDelete("/api/customer/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindCustomerAssetsByType` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindCustomerAssetsByType() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        String title1 = "Asset title 1";
        String type1 = "typeC";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            asset = doPost("/api/asset", asset, Asset.class);
            assetsType1.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }
        String title2 = "Asset title 2";
        String type2 = "typeD";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            asset = doPost("/api/asset", asset, Asset.class);
            assetsType2.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink, type1);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsType1.sort(idComparator);
        loadedAssetsType1.sort(idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink, type2);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsType2.sort(idComparator);
        loadedAssetsType2.sort(idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            doDelete("/api/customer/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                new TypeReference<PageData<Asset>>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            doDelete("/api/customer/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                new TypeReference<PageData<Asset>>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignAssetToEdge` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignAssetToEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/edge/" + savedEdge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);

        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ASSIGNED_TO_EDGE,
                savedAsset.getId().getId().toString(), savedEdge.getId().getId().toString(), edge.getName());


        PageData<Asset> pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId().toString() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, new PageLink(100));

        Assert.assertEquals(1, pageData.getData().size());

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);


        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_EDGE, savedAsset.getId().getId().toString(), savedEdge.getId().getId().toString(), savedEdge.getName());

        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId().toString() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, new PageLink(100));

        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAssetWithDeleteRelationsOk` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAssetWithDeleteRelationsOk() throws Exception {
        AssetId assetId = createAsset("Asset for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), assetId, "/api/asset/" + assetId);
    }

    @Ignore
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAssetExceptionWithRelationsTransactional` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAssetExceptionWithRelationsTransactional() throws Exception {
        AssetId assetId = createAsset("Asset for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(assetDao, savedTenant.getId(), assetId, "/api/asset/" + assetId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createAsset` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Asset createAsset(String name) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType("default");
        return doPost("/api/asset", asset, Asset.class);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AssetControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
