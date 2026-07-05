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
@Service
@Profile("install")
@Slf4j
public class ThingsboardInstallService {

    /**
     * 是否为`upgrade`。
     */
    @Value("${install.upgrade:false}")
    private Boolean isUpgrade;

    /**
     * 版本号，表示当前对象的对应属性。
     */
    @Value("${install.upgrade.from_version:1.2.3}")
    private String upgradeFromVersion;

    /**
     * 是否加载对应数据。
     */
    @Value("${install.load_demo:false}")
    private Boolean loadDemo;

    /**
     * 是否满足遥测条件。
     */
    @Value("${state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Autowired
    private EntityDatabaseSchemaService entityDatabaseSchemaService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    private NoSqlKeyspaceService noSqlKeyspaceService;

    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Autowired
    private TsDatabaseSchemaService tsDatabaseSchemaService;

    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    private TsLatestDatabaseSchemaService tsLatestDatabaseSchemaService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private DatabaseEntitiesUpgradeService databaseEntitiesUpgradeService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    private ApplicationContext context;

    /**
     * 数据，提供当前类调用的业务操作。
     */
    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    /**
     * 数据，提供当前类调用的业务操作。
     */
    @Autowired
    private DataUpdateService dataUpdateService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private CacheCleanupService cacheCleanupService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    private TsLatestMigrateService latestMigrateService;

    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private InstallScripts installScripts;

    /**
     * 功能：执行 `performInstall` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void performInstall() {
        try {
            if (isUpgrade) {
                log.info("Starting ThingsBoard Upgrade from version {} ...", upgradeFromVersion);

                cacheCleanupService.clearCache(upgradeFromVersion);

                if ("cassandra-latest-to-postgres".equals(upgradeFromVersion)) {
                    log.info("Migrating ThingsBoard latest timeseries data from cassandra to SQL database ...");
                    latestMigrateService.migrate();
                } else if (upgradeFromVersion.equals("3.6.2-images")) {
                    installScripts.updateImages();
                } else {
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

                if (noSqlKeyspaceService != null) {
                    noSqlKeyspaceService.createDatabaseSchema();
                }

                tsDatabaseSchemaService.createDatabaseSchema();

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

                if (loadDemo) {
                    log.info("Loading demo data...");
                    systemDataLoaderService.loadDemoData();
                }
                log.info("Installation finished successfully!");
            }


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
