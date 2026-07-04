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
package org.thingsboard.server.dao.service;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2DomainInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2MobileInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ParamsInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.oauth2.OAuth2Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`OAuth2ServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
public class OAuth2ServiceTest extends AbstractServiceTest {
    private static final OAuth2Info EMPTY_PARAMS = new OAuth2Info(false, Collections.emptyList());

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `oAuth2Service` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    protected OAuth2Service oAuth2Service;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `beforeRun` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void beforeRun() {
        Assert.assertTrue(oAuth2Service.findAllRegistrations().isEmpty());
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `after` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void after() {
        oAuth2Service.saveOAuth2Info(EMPTY_PARAMS);
        Assert.assertTrue(oAuth2Service.findAllRegistrations().isEmpty());
        Assert.assertTrue(oAuth2Service.findOAuth2Info().getOauth2ParamsInfos().isEmpty());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveHttpAndMixedDomainsTogether` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testSaveHttpAndMixedDomainsTogether() {
        OAuth2Info oAuth2Info = new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build()
        ));
        Assertions.assertThrows(DataValidationException.class, () -> {
            oAuth2Service.saveOAuth2Info(oAuth2Info);
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveHttpsAndMixedDomainsTogether` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testSaveHttpsAndMixedDomainsTogether() {
        OAuth2Info oAuth2Info = new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTPS).build(),
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build()
        ));
        Assertions.assertThrows(DataValidationException.class, () -> {
            oAuth2Service.saveOAuth2Info(oAuth2Info);
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCreateAndFindParams` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testCreateAndFindParams() {
        OAuth2Info oAuth2Info = createDefaultOAuth2Info();
        oAuth2Service.saveOAuth2Info(oAuth2Info);
        OAuth2Info foundOAuth2Info = oAuth2Service.findOAuth2Info();
        Assert.assertNotNull(foundOAuth2Info);
        // TODO ask if it's safe to check equality on AdditionalProperties
        Assert.assertEquals(oAuth2Info, foundOAuth2Info);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDisableParams` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testDisableParams() {
        OAuth2Info oAuth2Info = createDefaultOAuth2Info();
        oAuth2Info.setEnabled(true);
        oAuth2Service.saveOAuth2Info(oAuth2Info);
        OAuth2Info foundOAuth2Info = oAuth2Service.findOAuth2Info();
        Assert.assertNotNull(foundOAuth2Info);
        Assert.assertEquals(oAuth2Info, foundOAuth2Info);

        oAuth2Info.setEnabled(false);
        oAuth2Service.saveOAuth2Info(oAuth2Info);
        OAuth2Info foundDisabledOAuth2Info = oAuth2Service.findOAuth2Info();
        Assert.assertEquals(oAuth2Info, foundDisabledOAuth2Info);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testClearDomainParams` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testClearDomainParams() {
        OAuth2Info oAuth2Info = createDefaultOAuth2Info();
        oAuth2Service.saveOAuth2Info(oAuth2Info);
        OAuth2Info foundOAuth2Info = oAuth2Service.findOAuth2Info();
        Assert.assertNotNull(foundOAuth2Info);
        Assert.assertEquals(oAuth2Info, foundOAuth2Info);

        oAuth2Service.saveOAuth2Info(EMPTY_PARAMS);
        OAuth2Info foundAfterClearClientsParams = oAuth2Service.findOAuth2Info();
        Assert.assertNotNull(foundAfterClearClientsParams);
        Assert.assertEquals(EMPTY_PARAMS, foundAfterClearClientsParams);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateClientsParams` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testUpdateClientsParams() {
        OAuth2Info oAuth2Info = createDefaultOAuth2Info();
        oAuth2Service.saveOAuth2Info(oAuth2Info);
        OAuth2Info foundOAuth2Info = oAuth2Service.findOAuth2Info();
        Assert.assertNotNull(foundOAuth2Info);
        Assert.assertEquals(oAuth2Info, foundOAuth2Info);

        OAuth2Info newOAuth2Info = new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("another-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo()
                        ))
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("test-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo()
                        ))
                        .build()
        ));
        oAuth2Service.saveOAuth2Info(newOAuth2Info);
        OAuth2Info foundAfterUpdateOAuth2Info = oAuth2Service.findOAuth2Info();
        Assert.assertNotNull(foundAfterUpdateOAuth2Info);
        Assert.assertEquals(newOAuth2Info, foundAfterUpdateOAuth2Info);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetOAuth2Clients` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testGetOAuth2Clients() {
        List<OAuth2RegistrationInfo> firstGroup = Lists.newArrayList(
                validRegistrationInfo(),
                validRegistrationInfo(),
                validRegistrationInfo(),
                validRegistrationInfo()
        );
        List<OAuth2RegistrationInfo> secondGroup = Lists.newArrayList(
                validRegistrationInfo(),
                validRegistrationInfo()
        );
        List<OAuth2RegistrationInfo> thirdGroup = Lists.newArrayList(
                validRegistrationInfo()
        );
        OAuth2Info oAuth2Info = new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(firstGroup)
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(secondGroup)
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTPS).build(),
                                OAuth2DomainInfo.builder().name("fifth-domain").scheme(SchemeType.HTTP).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(thirdGroup)
                        .build()
        ));

        oAuth2Service.saveOAuth2Info(oAuth2Info);
        OAuth2Info foundOAuth2Info = oAuth2Service.findOAuth2Info();
        Assert.assertNotNull(foundOAuth2Info);
        Assert.assertEquals(oAuth2Info, foundOAuth2Info);

        List<OAuth2ClientInfo> firstGroupClientInfos = firstGroup.stream()
                .map(registrationInfo -> new OAuth2ClientInfo(
                        registrationInfo.getLoginButtonLabel(), registrationInfo.getLoginButtonIcon(), null))
                .collect(Collectors.toList());
        List<OAuth2ClientInfo> secondGroupClientInfos = secondGroup.stream()
                .map(registrationInfo -> new OAuth2ClientInfo(
                        registrationInfo.getLoginButtonLabel(), registrationInfo.getLoginButtonIcon(), null))
                .collect(Collectors.toList());
        List<OAuth2ClientInfo> thirdGroupClientInfos = thirdGroup.stream()
                .map(registrationInfo -> new OAuth2ClientInfo(
                        registrationInfo.getLoginButtonLabel(), registrationInfo.getLoginButtonIcon(), null))
                .collect(Collectors.toList());

        List<OAuth2ClientInfo> nonExistentDomainClients = oAuth2Service.getOAuth2Clients("http", "non-existent-domain", null, null);
        Assert.assertTrue(nonExistentDomainClients.isEmpty());

        List<OAuth2ClientInfo> firstDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "first-domain", null, null);
        Assert.assertEquals(firstGroupClientInfos.size(), firstDomainHttpClients.size());
        firstGroupClientInfos.forEach(firstGroupClientInfo -> {
            Assert.assertTrue(
                    firstDomainHttpClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(firstGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(firstGroupClientInfo.getName()))
            );
        });

        List<OAuth2ClientInfo> firstDomainHttpsClients = oAuth2Service.getOAuth2Clients("https", "first-domain", null, null);
        Assert.assertTrue(firstDomainHttpsClients.isEmpty());

        List<OAuth2ClientInfo> fourthDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "fourth-domain", null, null);
        Assert.assertEquals(secondGroupClientInfos.size(), fourthDomainHttpClients.size());
        secondGroupClientInfos.forEach(secondGroupClientInfo -> {
            Assert.assertTrue(
                    fourthDomainHttpClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(secondGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(secondGroupClientInfo.getName()))
            );
        });
        List<OAuth2ClientInfo> fourthDomainHttpsClients = oAuth2Service.getOAuth2Clients("https", "fourth-domain", null, null);
        Assert.assertEquals(secondGroupClientInfos.size(), fourthDomainHttpsClients.size());
        secondGroupClientInfos.forEach(secondGroupClientInfo -> {
            Assert.assertTrue(
                    fourthDomainHttpsClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(secondGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(secondGroupClientInfo.getName()))
            );
        });

        List<OAuth2ClientInfo> secondDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "second-domain", null, null);
        Assert.assertEquals(firstGroupClientInfos.size() + secondGroupClientInfos.size(), secondDomainHttpClients.size());
        firstGroupClientInfos.forEach(firstGroupClientInfo -> {
            Assert.assertTrue(
                    secondDomainHttpClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(firstGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(firstGroupClientInfo.getName()))
            );
        });
        secondGroupClientInfos.forEach(secondGroupClientInfo -> {
            Assert.assertTrue(
                    secondDomainHttpClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(secondGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(secondGroupClientInfo.getName()))
            );
        });

        List<OAuth2ClientInfo> secondDomainHttpsClients = oAuth2Service.getOAuth2Clients("https", "second-domain", null, null);
        Assert.assertEquals(firstGroupClientInfos.size() + thirdGroupClientInfos.size(), secondDomainHttpsClients.size());
        firstGroupClientInfos.forEach(firstGroupClientInfo -> {
            Assert.assertTrue(
                    secondDomainHttpsClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(firstGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(firstGroupClientInfo.getName()))
            );
        });
        thirdGroupClientInfos.forEach(thirdGroupClientInfo -> {
            Assert.assertTrue(
                    secondDomainHttpsClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(thirdGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(thirdGroupClientInfo.getName()))
            );
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetOAuth2ClientsForHttpAndHttps` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testGetOAuth2ClientsForHttpAndHttps() {
        List<OAuth2RegistrationInfo> firstGroup = Lists.newArrayList(
                validRegistrationInfo(),
                validRegistrationInfo(),
                validRegistrationInfo(),
                validRegistrationInfo()
        );
        OAuth2Info oAuth2Info = new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(firstGroup)
                        .build()
        ));

        oAuth2Service.saveOAuth2Info(oAuth2Info);
        OAuth2Info foundOAuth2Info = oAuth2Service.findOAuth2Info();
        Assert.assertNotNull(foundOAuth2Info);
        Assert.assertEquals(oAuth2Info, foundOAuth2Info);

        List<OAuth2ClientInfo> firstGroupClientInfos = firstGroup.stream()
                .map(registrationInfo -> new OAuth2ClientInfo(
                        registrationInfo.getLoginButtonLabel(), registrationInfo.getLoginButtonIcon(), null))
                .collect(Collectors.toList());

        List<OAuth2ClientInfo> firstDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "first-domain", null, null);
        Assert.assertEquals(firstGroupClientInfos.size(), firstDomainHttpClients.size());
        firstGroupClientInfos.forEach(firstGroupClientInfo -> {
            Assert.assertTrue(
                    firstDomainHttpClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(firstGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(firstGroupClientInfo.getName()))
            );
        });

        List<OAuth2ClientInfo> firstDomainHttpsClients = oAuth2Service.getOAuth2Clients("https", "first-domain", null, null);
        Assert.assertEquals(firstGroupClientInfos.size(), firstDomainHttpsClients.size());
        firstGroupClientInfos.forEach(firstGroupClientInfo -> {
            Assert.assertTrue(
                    firstDomainHttpsClients.stream().anyMatch(clientInfo ->
                            clientInfo.getIcon().equals(firstGroupClientInfo.getIcon())
                                    && clientInfo.getName().equals(firstGroupClientInfo.getName()))
            );
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetDisabledOAuth2Clients` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testGetDisabledOAuth2Clients() {
        OAuth2Info oAuth2Info = new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build()
        ));

        oAuth2Service.saveOAuth2Info(oAuth2Info);

        List<OAuth2ClientInfo> secondDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "second-domain", null, null);
        Assert.assertEquals(5, secondDomainHttpClients.size());

        oAuth2Info.setEnabled(false);
        oAuth2Service.saveOAuth2Info(oAuth2Info);

        List<OAuth2ClientInfo> secondDomainHttpDisabledClients = oAuth2Service.getOAuth2Clients("http", "second-domain", null, null);
        Assert.assertEquals(0, secondDomainHttpDisabledClients.size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAllRegistrations` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindAllRegistrations() {
        OAuth2Info oAuth2Info = new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTPS).build(),
                                OAuth2DomainInfo.builder().name("fifth-domain").scheme(SchemeType.HTTP).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo()
                        ))
                        .build()
        ));

        oAuth2Service.saveOAuth2Info(oAuth2Info);
        List<OAuth2Registration> foundRegistrations = oAuth2Service.findAllRegistrations();
        Assert.assertEquals(6, foundRegistrations.size());
        oAuth2Info.getOauth2ParamsInfos().stream()
                .flatMap(paramsInfo -> paramsInfo.getClientRegistrations().stream())
                .forEach(registrationInfo ->
                        Assert.assertTrue(
                                foundRegistrations.stream()
                                        .anyMatch(registration -> registration.getClientId().equals(registrationInfo.getClientId()))
                        )
                );
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindRegistrationById` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindRegistrationById() {
        OAuth2Info oAuth2Info = new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTPS).build(),
                                OAuth2DomainInfo.builder().name("fifth-domain").scheme(SchemeType.HTTP).build()
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo()
                        ))
                        .build()
        ));

        oAuth2Service.saveOAuth2Info(oAuth2Info);
        List<OAuth2Registration> foundRegistrations = oAuth2Service.findAllRegistrations();
        foundRegistrations.forEach(registration -> {
            OAuth2Registration foundRegistration = oAuth2Service.findRegistration(registration.getUuidId());
            Assert.assertEquals(registration, foundRegistration);
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAppSecret` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindAppSecret() {
        OAuth2Info oAuth2Info = new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .mobileInfos(Lists.newArrayList(
                                validMobileInfo("com.test.pkg1", "testPkg1AppSecret"),
                                validMobileInfo("com.test.pkg2", "testPkg2AppSecret")
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build()
        ));
        oAuth2Service.saveOAuth2Info(oAuth2Info);

        OAuth2Info foundOAuth2Info = oAuth2Service.findOAuth2Info();
        Assert.assertEquals(oAuth2Info, foundOAuth2Info);

        List<OAuth2ClientInfo> firstDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "first-domain", "com.test.pkg1", null);
        Assert.assertEquals(3, firstDomainHttpClients.size());
        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (OAuth2ClientInfo clientInfo : firstDomainHttpClients) {
            String[] segments = clientInfo.getUrl().split("/");
            String registrationId = segments[segments.length-1];
            String appSecret = oAuth2Service.findAppSecret(UUID.fromString(registrationId), "com.test.pkg1");
            Assert.assertNotNull(appSecret);
            Assert.assertEquals("testPkg1AppSecret", appSecret);
            appSecret = oAuth2Service.findAppSecret(UUID.fromString(registrationId), "com.test.pkg2");
            Assert.assertNotNull(appSecret);
            Assert.assertEquals("testPkg2AppSecret", appSecret);
            appSecret = oAuth2Service.findAppSecret(UUID.fromString(registrationId), "com.test.pkg3");
            Assert.assertNull(appSecret);
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindClientsByPackageAndPlatform` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindClientsByPackageAndPlatform() {
        OAuth2Info oAuth2Info = new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .mobileInfos(Lists.newArrayList(
                                validMobileInfo("com.test.pkg1", "testPkg1Callback"),
                                validMobileInfo("com.test.pkg2", "testPkg2Callback")
                        ))
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo("Google", Arrays.asList(PlatformType.WEB, PlatformType.ANDROID)),
                                validRegistrationInfo("Facebook", Arrays.asList(PlatformType.IOS)),
                                validRegistrationInfo("GitHub", Collections.emptyList())
                        ))
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build()
        ));
        oAuth2Service.saveOAuth2Info(oAuth2Info);

        OAuth2Info foundOAuth2Info = oAuth2Service.findOAuth2Info();
        Assert.assertEquals(oAuth2Info, foundOAuth2Info);

        List<OAuth2ClientInfo> firstDomainHttpClients = oAuth2Service.getOAuth2Clients("http", "first-domain", null, null);
        Assert.assertEquals(3, firstDomainHttpClients.size());
        List<OAuth2ClientInfo> pkg1Clients = oAuth2Service.getOAuth2Clients("http", "first-domain", "com.test.pkg1", null);
        Assert.assertEquals(3, pkg1Clients.size());
        List<OAuth2ClientInfo> pkg1AndroidClients = oAuth2Service.getOAuth2Clients("http", "first-domain", "com.test.pkg1", PlatformType.ANDROID);
        Assert.assertEquals(2, pkg1AndroidClients.size());
        Assert.assertTrue(pkg1AndroidClients.stream().anyMatch(client -> client.getName().equals("Google")));
        Assert.assertTrue(pkg1AndroidClients.stream().anyMatch(client -> client.getName().equals("GitHub")));
        List<OAuth2ClientInfo> pkg1IOSClients = oAuth2Service.getOAuth2Clients("http", "first-domain", "com.test.pkg1", PlatformType.IOS);
        Assert.assertEquals(2, pkg1IOSClients.size());
        Assert.assertTrue(pkg1IOSClients.stream().anyMatch(client -> client.getName().equals("Facebook")));
        Assert.assertTrue(pkg1IOSClients.stream().anyMatch(client -> client.getName().equals("GitHub")));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDefaultOAuth2Info` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private OAuth2Info createDefaultOAuth2Info() {
        return new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("first-domain").scheme(SchemeType.HTTP).build(),
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("third-domain").scheme(SchemeType.HTTPS).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo(),
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build(),
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("second-domain").scheme(SchemeType.MIXED).build(),
                                OAuth2DomainInfo.builder().name("fourth-domain").scheme(SchemeType.MIXED).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo(),
                                validRegistrationInfo()
                        ))
                        .build()
        ));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validRegistrationInfo` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private OAuth2RegistrationInfo validRegistrationInfo() {
        return validRegistrationInfo(null, Collections.emptyList());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validRegistrationInfo` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private OAuth2RegistrationInfo validRegistrationInfo(String label, List<PlatformType> platforms) {
        return OAuth2RegistrationInfo.builder()
                .clientId(UUID.randomUUID().toString())
                .clientSecret(UUID.randomUUID().toString())
                .authorizationUri(UUID.randomUUID().toString())
                .accessTokenUri(UUID.randomUUID().toString())
                .scope(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .platforms(platforms == null ? Collections.emptyList() : platforms)
                .userInfoUri(UUID.randomUUID().toString())
                .userNameAttributeName(UUID.randomUUID().toString())
                .jwkSetUri(UUID.randomUUID().toString())
                .clientAuthenticationMethod(UUID.randomUUID().toString())
                .loginButtonLabel(label != null ? label : UUID.randomUUID().toString())
                .loginButtonIcon(UUID.randomUUID().toString())
                .additionalInfo(JacksonUtil.newObjectNode().put(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .mapperConfig(
                        OAuth2MapperConfig.builder()
                                .allowUserCreation(true)
                                .activateUser(true)
                                .type(MapperType.CUSTOM)
                                .custom(
                                        OAuth2CustomMapperConfig.builder()
                                                .url(UUID.randomUUID().toString())
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validMobileInfo` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private OAuth2MobileInfo validMobileInfo(String pkgName, String appSecret) {
        return OAuth2MobileInfo.builder().pkgName(pkgName)
                .appSecret(appSecret != null ? appSecret : StringUtils.randomAlphanumeric(24))
                .build();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2ServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
