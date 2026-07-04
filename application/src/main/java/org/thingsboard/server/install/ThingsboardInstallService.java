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
package org.thingsboard.server.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.install.DatabaseEntitiesUpgradeService;
import org.thingsboard.server.service.install.EntityDatabaseSchemaService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.NoSqlKeyspaceService;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.TsDatabaseSchemaService;
import org.thingsboard.server.service.install.TsLatestDatabaseSchemaService;
import org.thingsboard.server.service.install.migrate.TsLatestMigrateService;
import org.thingsboard.server.service.install.update.CacheCleanupService;
import org.thingsboard.server.service.install.update.DataUpdateService;

import static org.thingsboard.server.service.install.update.DefaultDataUpdateService.getEnv;

@Service
@Profile("install")
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`ThingsboardInstallService` 是ThingsBoard Application 模块中的应用服务支撑类型，用于承载服务端运行期的数据、依赖或流程控制。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 生命周期：由 Spring 容器、Actor System、Web 请求或队列消费流程管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO/Helper。
 */
public class ThingsboardInstallService {

    @Value("${install.upgrade:false}")
    /**
     * 字段说明：
     * 1. 保存 `isUpgrade` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Boolean isUpgrade;

    @Value("${install.upgrade.from_version:1.2.3}")
    /**
     * 字段说明：
     * 1. 保存 `upgradeFromVersion` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String upgradeFromVersion;

    @Value("${install.load_demo:false}")
    /**
     * 字段说明：
     * 1. 保存 `loadDemo` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Boolean loadDemo;

    @Value("${state.persistToTelemetry:false}")
    /**
     * 字段说明：
     * 1. 保存 `persistToTelemetry` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean persistToTelemetry;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `entityDatabaseSchemaService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private EntityDatabaseSchemaService entityDatabaseSchemaService;

    @Autowired(required = false)
    /**
     * 字段说明：
     * 1. 保存 `noSqlKeyspaceService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private NoSqlKeyspaceService noSqlKeyspaceService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tsDatabaseSchemaService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TsDatabaseSchemaService tsDatabaseSchemaService;

    @Autowired(required = false)
    /**
     * 字段说明：
     * 1. 保存 `tsLatestDatabaseSchemaService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TsLatestDatabaseSchemaService tsLatestDatabaseSchemaService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `databaseEntitiesUpgradeService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DatabaseEntitiesUpgradeService databaseEntitiesUpgradeService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `componentDiscoveryService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `context` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ApplicationContext context;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `systemDataLoaderService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `dataUpdateService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DataUpdateService dataUpdateService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `cacheCleanupService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private CacheCleanupService cacheCleanupService;

    @Autowired(required = false)
    /**
     * 字段说明：
     * 1. 保存 `latestMigrateService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TsLatestMigrateService latestMigrateService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `installScripts` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private InstallScripts installScripts;

    /**
     * 方法说明：
     * 1. 职责：执行 `performInstall` 对应的应用服务支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器、Actor System、Web 请求或队列消费流程管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void performInstall() {
        try {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (isUpgrade) {
                log.info("Starting ThingsBoard Upgrade from version {} ...", upgradeFromVersion);

                // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
                cacheCleanupService.clearCache(upgradeFromVersion);

                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if ("cassandra-latest-to-postgres".equals(upgradeFromVersion)) {
                    log.info("Migrating ThingsBoard latest timeseries data from cassandra to SQL database ...");
                    latestMigrateService.migrate();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (upgradeFromVersion.equals("3.6.2-images")) {
                    installScripts.updateImages();
                } else {
                    // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
                    switch (upgradeFromVersion) {
                        case "3.5.0":
                            log.info("Upgrading ThingsBoard from version 3.5.0 to 3.5.1 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.5.0");
                        case "3.5.1":
                            log.info("Upgrading ThingsBoard from version 3.5.1 to 3.6.0 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.5.1");
                            dataUpdateService.updateData("3.5.1");
                            systemDataLoaderService.updateDefaultNotificationConfigs();
                        case "3.6.0":
                            log.info("Upgrading ThingsBoard from version 3.6.0 to 3.6.1 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.6.0");
                            dataUpdateService.updateData("3.6.0");
                        case "3.6.1":
                            log.info("Upgrading ThingsBoard from version 3.6.1 to 3.6.2 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.6.1");
                            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                            if (!getEnv("SKIP_IMAGES_MIGRATION", false)) {
                                installScripts.setUpdateImages(true);
                            } else {
                                log.info("Skipping images migration. Run the upgrade with fromVersion as '3.6.2-images' to migrate");
                            }
                        case "3.6.2":
                            log.info("Upgrading ThingsBoard from version 3.6.2 to 3.6.3 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.6.2");
                            systemDataLoaderService.updateDefaultNotificationConfigs();
                        case "3.6.3":
                            log.info("Upgrading ThingsBoard from version 3.6.3 to 3.6.4 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.6.3");
                            //TODO DON'T FORGET to update switch statement in the CacheCleanupService if you need to clear the cache
                            break;
                        default:
                            throw new RuntimeException("Unable to upgrade ThingsBoard, unsupported fromVersion: " + upgradeFromVersion);
                    }
                    entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
                    entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);
                    log.info("Updating system data...");
                    dataUpdateService.upgradeRuleNodes();
                    systemDataLoaderService.loadSystemWidgets();
                    installScripts.loadSystemLwm2mResources();
                    installScripts.loadSystemImages();
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (installScripts.isUpdateImages()) {
                        installScripts.updateImages();
                    }
                }
                log.info("Upgrade finished successfully!");

            } else {

                log.info("Starting ThingsBoard Installation...");

                log.info("Installing DataBase schema for entities...");

                entityDatabaseSchemaService.createDatabaseSchema();

                entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
                entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);

                log.info("Installing DataBase schema for timeseries...");

                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (noSqlKeyspaceService != null) {
                    noSqlKeyspaceService.createDatabaseSchema();
                }

                tsDatabaseSchemaService.createDatabaseSchema();

                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (tsLatestDatabaseSchemaService != null) {
                    tsLatestDatabaseSchemaService.createDatabaseSchema();
                }

                log.info("Loading system data...");

                componentDiscoveryService.discoverComponents();

                systemDataLoaderService.createSysAdmin();
                systemDataLoaderService.createDefaultTenantProfiles();
                systemDataLoaderService.createAdminSettings();
                systemDataLoaderService.createRandomJwtSettings();
                systemDataLoaderService.loadSystemWidgets();
                systemDataLoaderService.createOAuth2Templates();
                systemDataLoaderService.createQueues();
                systemDataLoaderService.createDefaultNotificationConfigs();

//                systemDataLoaderService.loadSystemPlugins();
//                systemDataLoaderService.loadSystemRules();
                installScripts.loadSystemLwm2mResources();
                installScripts.loadSystemImages();

                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (loadDemo) {
                    log.info("Loading demo data...");
                    systemDataLoaderService.loadDemoData();
                }
                log.info("Installation finished successfully!");
            }


        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.error("Unexpected error during ThingsBoard installation!", e);
            throw new ThingsboardInstallException("Unexpected error during ThingsBoard installation!", e);
        } finally {
            SpringApplication.exit(context);
        }
    }

}


/*
 * 本类总结：
 * 1. 核心职责：`ThingsboardInstallService` 在 ThingsBoard Application 模块 中承担应用服务支撑类型职责，核心目的是承载服务端运行期的数据、依赖或流程控制。
 * 2. 核心流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
