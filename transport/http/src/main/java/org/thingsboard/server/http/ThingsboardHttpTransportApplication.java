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
package org.thingsboard.server.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ComponentScan({"org.thingsboard.server.http", "org.thingsboard.server.common", "org.thingsboard.server.transport.http", "org.thingsboard.server.queue", "org.thingsboard.server.cache"})
/**
 * 中文说明：
 * 1. 类目的：`ThingsboardHttpTransportApplication` 是 ThingsBoard HTTP Transport 启动模块 中的HTTP 独立协议服务启动入口，用于启动独立的 ThingsBoard HTTP transport Spring Boot 进程，并加载协议包、common、queue 和 cache 相关组件。
 * 2. 所属模块：位于 transport/http，服务于 ThingsBoard 的客户端访问、离线工具或独立协议接入边界。
 * 3. 协作模块：主要协作对象包括 HTTP transport 实现、ThingsBoard common transport、queue、cache、Spring Boot 自动配置和部署脚本。
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
public class ThingsboardHttpTransportApplication {

    /**
     * 字段说明：
     * 1. 保存内容：`SPRING_CONFIG_NAME_KEY` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `HTTP 独立协议服务启动入口` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard HTTP Transport 启动模块 的上层流程。
     */
    private static final String SPRING_CONFIG_NAME_KEY = "--spring.config.name";
    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "tb-http-transport";

    /**
     * 方法说明：
     * 1. 职责：`main` 执行 HTTP 独立协议服务启动入口 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由操作系统服务、Docker 容器或命令行启动，SpringApplication 创建上下文后持续运行直到进程关闭；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：main 方法接收命令行参数，补齐默认 spring.config.name 后启动 Spring Boot，上下文再加载协议 handler、队列生产者、缓存和调度任务。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：启动类不参与事务，设备会话、遥测入站和数据库事务由被扫描到的 transport/common/DAO 服务控制。
     * 9. 缓存：启动类只启用 cache 包扫描，不直接读写缓存，缓存生命周期由 Spring Bean 管理。
     * 10. MQTT：仅 MQTT transport 启动入口直接承载 MQTT 服务；其它协议启动入口分别承载 HTTP、CoAP、LwM2M 或 SNMP 入站流程。
     * 11. Actor 通信：入站消息通常经 queue/common transport 转换后进入服务端 Actor 系统，启动类只负责装配入口进程。
     * 12. 数据库：启动类不访问数据库，协议消息后续持久化由服务端 DAO 和事务层完成。
     * 13. Rule Engine：入站遥测/属性/RPC 消息后续可能进入 Rule Engine，启动类只决定对应 transport 进程是否启动。
     */
    public static void main(String[] args) {
        // 这里启动独立 transport Spring 上下文，使该协议可以单独部署、扩缩容和读取专属配置文件。
        SpringApplication.run(ThingsboardHttpTransportApplication.class, updateArguments(args));
    }

    /**
     * 方法说明：
     * 1. 职责：`updateArguments` 执行 HTTP 独立协议服务启动入口 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由操作系统服务、Docker 容器或命令行启动，SpringApplication 创建上下文后持续运行直到进程关闭；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：main 方法接收命令行参数，补齐默认 spring.config.name 后启动 Spring Boot，上下文再加载协议 handler、队列生产者、缓存和调度任务。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：启动类不参与事务，设备会话、遥测入站和数据库事务由被扫描到的 transport/common/DAO 服务控制。
     * 9. 缓存：启动类只启用 cache 包扫描，不直接读写缓存，缓存生命周期由 Spring Bean 管理。
     * 10. MQTT：仅 MQTT transport 启动入口直接承载 MQTT 服务；其它协议启动入口分别承载 HTTP、CoAP、LwM2M 或 SNMP 入站流程。
     * 11. Actor 通信：入站消息通常经 queue/common transport 转换后进入服务端 Actor 系统，启动类只负责装配入口进程。
     * 12. 数据库：启动类不访问数据库，协议消息后续持久化由服务端 DAO 和事务层完成。
     * 13. Rule Engine：入站遥测/属性/RPC 消息后续可能进入 Rule Engine，启动类只决定对应 transport 进程是否启动。
     */
    private static String[] updateArguments(String[] args) {
        // 仅在调用方未显式指定 spring.config.name 时补默认值，保留运维脚本覆盖配置文件名的能力。
        if (Arrays.stream(args).noneMatch(arg -> arg.startsWith(SPRING_CONFIG_NAME_KEY))) {
            String[] modifiedArgs = new String[args.length + 1];
            // 复制原始参数后追加默认配置项，避免修改调用方传入数组并保持参数顺序稳定。
            System.arraycopy(args, 0, modifiedArgs, 0, args.length);
            modifiedArgs[args.length] = DEFAULT_SPRING_CONFIG_PARAM;
            return modifiedArgs;
        }
        return args;
    }
    /**
     * 本类总结：
     * 1. 核心职责：`ThingsboardHttpTransportApplication` 负责启动独立的 ThingsBoard HTTP transport Spring Boot 进程，并加载协议包、common、queue 和 cache 相关组件。
     * 2. 核心流程：main 方法接收命令行参数，补齐默认 spring.config.name 后启动 Spring Boot，上下文再加载协议 handler、队列生产者、缓存和调度任务。
     * 3. 关键依赖：HTTP transport 实现、ThingsBoard common transport、queue、cache、Spring Boot 自动配置和部署脚本。
     * 4. 设计重点：通过 Bootstrap / Adapter 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}
