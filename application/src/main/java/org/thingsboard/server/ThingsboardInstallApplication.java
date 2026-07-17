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
 * 1. `ThingsboardInstallApplication` 是 ThingsBoard Application 的进程启动入口，用于启动和装配 `Thingsboard Install Application` 相关服务。
 * 2. 它负责创建应用上下文、加载组件并把启动参数交给实际运行模块。
 * 3. 类中通常只保留启动参数修正和框架启动调用，不承载协议或业务处理细节。
 * 4. 它直接协作于 Spring Boot 配置、组件扫描和当前模块的服务实现。
 * 5. 独立入口使该服务能够单独部署、配置和扩缩容。
 * 6. 阅读时重点关注组件扫描范围、配置文件名称和传入启动框架的参数。
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
