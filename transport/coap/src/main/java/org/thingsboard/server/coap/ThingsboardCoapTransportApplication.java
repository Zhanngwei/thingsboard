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
package org.thingsboard.server.coap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

/**
 * 中文说明：
 * 1. `ThingsboardCoapTransportApplication` 是 ThingsBoard CoAP Transport 的进程启动入口，用于启动和装配 CoAP 相关服务。
 * 2. 它负责创建应用上下文、加载组件并把启动参数交给实际运行模块。
 * 3. 类中通常只保留启动参数修正和框架启动调用，不承载协议或业务处理细节。
 * 4. 它直接协作于 Spring Boot 配置、组件扫描和当前模块的服务实现。
 * 5. 独立入口使该服务能够单独部署、配置和扩缩容。
 * 6. 阅读时重点关注组件扫描范围、配置文件名称和传入启动框架的参数。
 */
@SpringBootConfiguration
@EnableAsync
@EnableScheduling
@EnableAutoConfiguration
@ComponentScan({"org.thingsboard.server.coap", "org.thingsboard.server.common", "org.thingsboard.server.coapserver", "org.thingsboard.server.transport.coap", "org.thingsboard.server.queue", "org.thingsboard.server.cache"})
public class ThingsboardCoapTransportApplication {

    /**
     * 配置常量，用于统一引用固定值。
     */
    private static final String SPRING_CONFIG_NAME_KEY = "--spring.config.name";
    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "tb-coap-transport";

    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) {
        SpringApplication.run(ThingsboardCoapTransportApplication.class, updateArguments(args));
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
