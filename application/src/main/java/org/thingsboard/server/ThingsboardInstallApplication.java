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
package org.thingsboard.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.thingsboard.server.install.ThingsboardInstallService;

import java.util.Arrays;

/**
 * 中文说明：
 * 1. 类目的：`ThingsboardInstallApplication` 是ThingsBoard Application 模块中的应用服务支撑类型，用于承载服务端运行期的数据、依赖或流程控制。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 生命周期：由 Spring 容器、Actor System、Web 请求或队列消费流程管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO/Helper。
 */
@Slf4j
@SpringBootConfiguration
@ComponentScan({"org.thingsboard.server.install",
        "org.thingsboard.server.service.component",
        "org.thingsboard.server.service.install",
        "org.thingsboard.server.service.security.auth.jwt.settings",
        "org.thingsboard.server.dao",
        "org.thingsboard.server.common.stats",
        "org.thingsboard.server.common.transport.config.ssl",
        "org.thingsboard.server.cache",
        "org.thingsboard.server.springfox"
})
public class ThingsboardInstallApplication {

    /**
     * 配置常量，用于统一引用固定值。
     */
    private static final String SPRING_CONFIG_NAME_KEY = "--spring.config.name";
    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "thingsboard";

    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) {
        try {
            SpringApplication application = new SpringApplication(ThingsboardInstallApplication.class);
            application.setAdditionalProfiles("install");
            ConfigurableApplicationContext context = application.run(updateArguments(args));
            context.getBean(ThingsboardInstallService.class).performInstall();
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * 功能：补齐启动参数中的配置名称。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：处理结果。
     */
    private static String[] updateArguments(String[] args) {
        if (Arrays.stream(args).noneMatch(arg -> arg.startsWith(SPRING_CONFIG_NAME_KEY))) {
            String[] modifiedArgs = new String[args.length + 1];
            System.arraycopy(args, 0, modifiedArgs, 0, args.length);
            modifiedArgs[args.length] = DEFAULT_SPRING_CONFIG_PARAM;
            return modifiedArgs;
        }
        return args;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ThingsboardInstallApplication` 在 ThingsBoard Application 模块 中承担应用服务支撑类型职责，核心目的是承载服务端运行期的数据、依赖或流程控制。
 * 2. 核心流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
