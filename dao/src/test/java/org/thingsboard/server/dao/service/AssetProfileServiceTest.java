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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`AssetProfileServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
public class AssetProfileServiceTest extends AbstractServiceTest {

    private IdComparator<AssetProfile> idComparator = new IdComparator<>();
    private IdComparator<AssetProfileInfo> assetProfileInfoIdComparator = new IdComparator<>();

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `assetProfileService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    AssetProfileService assetProfileService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `assetService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    AssetService assetService;

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAssetProfile` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testSaveAssetProfile() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        Assert.assertNotNull(savedAssetProfile);
        Assert.assertNotNull(savedAssetProfile.getId());
        Assert.assertTrue(savedAssetProfile.getCreatedTime() > 0);
        Assert.assertEquals(assetProfile.getName(), savedAssetProfile.getName());
        Assert.assertEquals(assetProfile.getDescription(), savedAssetProfile.getDescription());
        Assert.assertEquals(assetProfile.isDefault(), savedAssetProfile.isDefault());
        Assert.assertEquals(assetProfile.getDefaultRuleChainId(), savedAssetProfile.getDefaultRuleChainId());
        savedAssetProfile.setName("New asset profile");
        assetProfileService.saveAssetProfile(savedAssetProfile);
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfile.getName());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAssetProfileById` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindAssetProfileById() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertNotNull(foundAssetProfile);
        Assert.assertEquals(savedAssetProfile, foundAssetProfile);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAssetProfileInfoById` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindAssetProfileInfoById() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        AssetProfileInfo foundAssetProfileInfo = assetProfileService.findAssetProfileInfoById(tenantId, savedAssetProfile.getId());
        Assert.assertNotNull(foundAssetProfileInfo);
        Assert.assertEquals(savedAssetProfile.getId(), foundAssetProfileInfo.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfileInfo.getName());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindDefaultAssetProfile` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindDefaultAssetProfile() {
        AssetProfile foundDefaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(foundDefaultAssetProfile);
        Assert.assertNotNull(foundDefaultAssetProfile.getId());
        Assert.assertNotNull(foundDefaultAssetProfile.getName());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindDefaultAssetProfileInfo` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindDefaultAssetProfileInfo() {
        AssetProfileInfo foundDefaultAssetProfileInfo = assetProfileService.findDefaultAssetProfileInfo(tenantId);
        Assert.assertNotNull(foundDefaultAssetProfileInfo);
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getId());
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getName());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindOrCreateAssetProfile` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindOrCreateAssetProfile() throws ExecutionException, InterruptedException {
        ListeningExecutorService testExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(100, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-test-scope")));
        try {
            // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
            List<ListenableFuture<AssetProfile>> futures = new ArrayList<>();
            // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
            for (int i = 0; i < 50; i++) {
                // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
                futures.add(testExecutor.submit(() -> assetProfileService.findOrCreateAssetProfile(tenantId, "Asset Profile 1")));
                // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
                futures.add(testExecutor.submit(() -> assetProfileService.findOrCreateAssetProfile(tenantId, "Asset Profile 2")));
            }

            // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
            List<AssetProfile> assetProfiles = Futures.allAsList(futures).get();
            assetProfiles.forEach(Assert::assertNotNull);
        } finally {
            testExecutor.shutdownNow();
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSetDefaultAssetProfile` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testSetDefaultAssetProfile() {
        AssetProfile assetProfile1 = this.createAssetProfile(tenantId, "Asset Profile 1");
        AssetProfile assetProfile2 = this.createAssetProfile(tenantId, "Asset Profile 2");

        AssetProfile savedAssetProfile1 = assetProfileService.saveAssetProfile(assetProfile1);
        AssetProfile savedAssetProfile2 = assetProfileService.saveAssetProfile(assetProfile2);

        boolean result = assetProfileService.setDefaultAssetProfile(tenantId, savedAssetProfile1.getId());
        Assert.assertTrue(result);
        AssetProfile defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(defaultAssetProfile);
        Assert.assertEquals(savedAssetProfile1.getId(), defaultAssetProfile.getId());
        result = assetProfileService.setDefaultAssetProfile(tenantId, savedAssetProfile2.getId());
        Assert.assertTrue(result);
        defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
        Assert.assertNotNull(defaultAssetProfile);
        Assert.assertEquals(savedAssetProfile2.getId(), defaultAssetProfile.getId());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAssetProfileWithEmptyName` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testSaveAssetProfileWithEmptyName() {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetProfileService.saveAssetProfile(assetProfile);
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAssetProfileWithSameName` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testSaveAssetProfileWithSameName() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        assetProfileService.saveAssetProfile(assetProfile);
        AssetProfile assetProfile2 = this.createAssetProfile(tenantId, "Asset Profile");
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetProfileService.saveAssetProfile(assetProfile2);
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAssetProfileWithExistingAsset` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testDeleteAssetProfileWithExistingAsset() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("Test asset");
        asset.setAssetProfileId(savedAssetProfile.getId());
        assetService.saveAsset(asset);
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetProfileService.deleteAssetProfile(tenantId, savedAssetProfile.getId());
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAssetProfile` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testDeleteAssetProfile() {
        AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile");
        AssetProfile savedAssetProfile = assetProfileService.saveAssetProfile(assetProfile);
        assetProfileService.deleteAssetProfile(tenantId, savedAssetProfile.getId());
        AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, savedAssetProfile.getId());
        Assert.assertNull(foundAssetProfile);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAssetProfiles` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindAssetProfiles() {

        List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        assetProfiles.addAll(pageData.getData());

        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (int i = 0; i < 28; i++) {
            AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile" + i);
            assetProfiles.add(assetProfileService.saveAssetProfile(assetProfile));
        }

        List<AssetProfile> loadedAssetProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
            loadedAssetProfiles.addAll(pageData.getData());
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfiles, idComparator);

        Assert.assertEquals(assetProfiles, loadedAssetProfiles);

        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (AssetProfile assetProfile : loadedAssetProfiles) {
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (!assetProfile.isDefault()) {
                assetProfileService.deleteAssetProfile(tenantId, assetProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAssetProfileInfos` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindAssetProfileInfos() {

        List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> assetProfilePageData = assetProfileService.findAssetProfiles(tenantId, pageLink);
        Assert.assertFalse(assetProfilePageData.hasNext());
        Assert.assertEquals(1, assetProfilePageData.getTotalElements());
        assetProfiles.addAll(assetProfilePageData.getData());

        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (int i = 0; i < 28; i++) {
            AssetProfile assetProfile = this.createAssetProfile(tenantId, "Asset Profile" + i);
            assetProfiles.add(assetProfileService.saveAssetProfile(assetProfile));
        }

        List<AssetProfileInfo> loadedAssetProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<AssetProfileInfo> pageData;
        do {
            pageData = assetProfileService.findAssetProfileInfos(tenantId, pageLink);
            loadedAssetProfileInfos.addAll(pageData.getData());
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());


        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfileInfos, assetProfileInfoIdComparator);

        List<AssetProfileInfo> assetProfileInfos = assetProfiles.stream()
                .map(assetProfile -> new AssetProfileInfo(assetProfile.getId(), assetProfile.getTenantId(),
                        assetProfile.getName(), assetProfile.getImage(), assetProfile.getDefaultDashboardId())).collect(Collectors.toList());

        Assert.assertEquals(assetProfileInfos, loadedAssetProfileInfos);

        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (AssetProfile assetProfile : assetProfiles) {
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (!assetProfile.isDefault()) {
                assetProfileService.deleteAssetProfile(tenantId, assetProfile.getId());
            }
        }

        pageLink = new PageLink(17);
        pageData = assetProfileService.findAssetProfileInfos(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAllassetProfilesByTenantId` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindAllassetProfilesByTenantId() {
        int assetProfilesCount = 4; // 3 created + default
        var assetProfiles = new ArrayList<AssetProfile>(4);

        var profileC = assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, "profile C"));
        assetProfiles.add(assetProfileService.saveAssetProfile(profileC));


        var profileA = assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, "profile A"));
        assetProfiles.add(assetProfileService.saveAssetProfile(profileA));


        var profileB = assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, "profile B"));
        assetProfiles.add(assetProfileService.saveAssetProfile(profileB));


        assetProfiles.add(assetProfileService.findDefaultAssetProfile(tenantId));

        List<EntityInfo> sortedProfileInfos = assetProfiles.stream()
                .map(profile -> new EntityInfo(profile.getId(), profile.getName()))
                .sorted(Comparator.comparing(EntityInfo::getName))
                .collect(Collectors.toList());

        var assetProfileInfos = assetProfileService
                .findAssetProfileNamesByTenantId(tenantId, false);

        assertThat(assetProfileInfos).isNotNull();
        assertThat(assetProfileInfos).hasSize(assetProfilesCount);
        assertThat(assetProfileInfos).isEqualTo(sortedProfileInfos);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindActiveOnlyassetProfilesByTenantId` 对应的DAO 服务测试或服务支撑类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindActiveOnlyassetProfilesByTenantId() {

        String profileCName = "profile C";
        assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, profileCName));

        String profileAName = "profile A";
        assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, profileAName));

        String profileBName = "profile B";
        assetProfileService.saveAssetProfile(
                createAssetProfile(tenantId, profileBName));


        var assetProfileInfos = assetProfileService
                .findAssetProfileNamesByTenantId(tenantId, true);

        assertThat(assetProfileInfos).isNotNull();
        assertThat(assetProfileInfos).isEmpty();

        var assetC = new Asset();
        assetC.setName("Test asset C");
        assetC.setType(profileCName);
        assetC.setTenantId(tenantId);

        assetC = assetService.saveAsset(assetC);

        var assetA = new Asset();
        assetA.setName("Test asset A");
        assetA.setType(profileAName);
        assetA.setTenantId(tenantId);

        assetA = assetService.saveAsset(assetA);

        var assetB = new Asset();
        assetB.setName("Test asset B");
        assetB.setType(profileBName);
        assetB.setTenantId(tenantId);

        assetB = assetService.saveAsset(assetB);

        assetProfileInfos = assetProfileService
                .findAssetProfileNamesByTenantId(tenantId, true);

        var expected = List.of(
                new EntityInfo(assetA.getAssetProfileId(), profileAName),
                new EntityInfo(assetB.getAssetProfileId(), profileBName),
                new EntityInfo(assetC.getAssetProfileId(), profileCName)
        );

        assertThat(assetProfileInfos).isNotEmpty();
        assertThat(assetProfileInfos).hasSize(3);
        assertThat(assetProfileInfos).isEqualTo(expected);
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`AssetProfileServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
