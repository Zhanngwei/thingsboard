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
 * 1. 类目的：`ThingsboardCoapTransportApplication` 是 ThingsBoard COAP Transport 启动模块 中的COAP 独立协议服务启动入口，用于启动独立的 ThingsBoard COAP transport Spring Boot 进程，并加载协议包、common、queue 和 cache 相关组件。
 * 2. 所属模块：位于 transport/coap，服务于 ThingsBoard 的客户端访问、离线工具或独立协议接入边界。
 * 3. 协作模块：主要协作对象包括 COAP transport 实现、ThingsBoard common transport、queue、cache、Spring Boot 自动配置和部署脚本。
 * 4. 生命周期：由操作系统服务、Docker 容器或命令行启动，SpringApplication 创建上下文后持续运行直到进程关闭。
 * 5. 存在原因：每种协议以独立启动类和独立配置名运行，可以单独扩缩容、隔离端口/线程/资源配置，并降低主服务进程负载。
 * 6. 事务：启动类不参与事务，设备会话、遥测入站和数据库事务由被扫描到的 transport/common/DAO 服务控制。
 * 7. 缓存：启动类只启用 cache 包扫描，不直接读写缓存，缓存生命周期由 Spring Bean 管理。
 * 8. MQTT：仅 MQTT transport 启动入口直接承载 MQTT 服务；其它协议启动入口分别承载 HTTP、CoAP、LwM2M 或 SNMP 入站流程。
 * 9. Actor 通信：入站消息通常经 queue/common transport 转换后进入服务端 Actor 系统，启动类只负责装配入口进程。
 * 10. 数据库：启动类不访问数据库，协议消息后续持久化由服务端 DAO 和事务层完成。
 * 11. Rule Engine：入站遥测/属性/RPC 消息后续可能进入 Rule Engine，启动类只决定对应 transport 进程是否启动。
 * 12. 设计模式：主要体现 Bootstrap / Adapter。
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
    /**
     * 本类总结：
     * 1. 核心职责：`ThingsboardCoapTransportApplication` 负责启动独立的 ThingsBoard COAP transport Spring Boot 进程，并加载协议包、common、queue 和 cache 相关组件。
     * 2. 核心流程：main 方法接收命令行参数，补齐默认 spring.config.name 后启动 Spring Boot，上下文再加载协议 handler、队列生产者、缓存和调度任务。
     * 3. 关键依赖：COAP transport 实现、ThingsBoard common transport、queue、cache、Spring Boot 自动配置和部署脚本。
     * 4. 设计重点：通过 Bootstrap / Adapter 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}
