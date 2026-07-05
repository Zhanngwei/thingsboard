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
package org.thingsboard.server.msa;


import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.utility.CommandLine;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;

/**
 * 中文说明：
 * 1. 类目的：`DockerComposeExecutor` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
@Slf4j
public class DockerComposeExecutor {

    /**
     * 名称常量，用于统一引用固定值。
     */
    String ENV_PROJECT_NAME = "COMPOSE_PROJECT_NAME";
    String ENV_COMPOSE_FILE = "COMPOSE_FILE";

    /**
     * `COMPOSE_EXECUTABLE`常量，用于统一引用固定值。
     */
    private static final String COMPOSE_EXECUTABLE = SystemUtils.IS_OS_WINDOWS ? "docker-compose.exe" : "docker-compose";
    private static final String DOCKER_EXECUTABLE = SystemUtils.IS_OS_WINDOWS ? "docker.exe" : "docker";

    /**
     * `composeFiles`列表，用于保存一组待处理对象。
     */
    private final List<File> composeFiles;
    private final String identifier;
    /**
     * `cmd` 字段，保存当前对象的对应属性。
     */
    private String cmd = "";
    private Map<String, String> env = new HashMap<>();

    /**
     * 功能：创建 `DockerComposeExecutor` 实例，并初始化必要字段。
     * 参数：
     * - `composeFiles`：数据列表。
     * - `identifier`：`identifier` 参数。
     * 返回：新创建的对象实例。
     */
    public DockerComposeExecutor(List<File> composeFiles, String identifier) {
        validateFileList(composeFiles);
        this.composeFiles = composeFiles;
        this.identifier = identifier;
    }

    /**
     * 功能：执行 `withCommand` 对应的处理。
     * 参数：
     * - `cmd`：`cmd` 参数。
     * 返回：处理结果。
     */
    public DockerComposeExecutor withCommand(String cmd) {
        this.cmd = cmd;
        return this;
    }

    /**
     * 功能：执行 `withEnv` 对应的处理。
     * 参数：
     * - `env`：键值映射。
     * 返回：处理结果。
     */
    public DockerComposeExecutor withEnv(Map<String, String> env) {
        this.env = env;
        return this;
    }

    /**
     * 功能：执行 `invokeCompose` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void invokeCompose() {
        // bail out early
        if (!CommandLine.executableExists(COMPOSE_EXECUTABLE)) {
            throw new ContainerLaunchException("Local Docker Compose not found. Is " + COMPOSE_EXECUTABLE + " on the PATH?");
        }
        final Map<String, String> environment = Maps.newHashMap(env);
        environment.put(ENV_PROJECT_NAME, identifier);
        final Stream<String> absoluteDockerComposeFilePaths = composeFiles.stream().map(File::getAbsolutePath).map(Objects::toString);
        final String composeFileEnvVariableValue = absoluteDockerComposeFilePaths.collect(joining(File.pathSeparator + ""));
        log.debug("Set env COMPOSE_FILE={}", composeFileEnvVariableValue);
        final File pwd = composeFiles.get(0).getAbsoluteFile().getParentFile().getAbsoluteFile();
        environment.put(ENV_COMPOSE_FILE, composeFileEnvVariableValue);
        log.info("Local Docker Compose is running command: {}", cmd);
        final List<String> command = Splitter.onPattern(" ").omitEmptyStrings().splitToList(COMPOSE_EXECUTABLE + " " + cmd);
        try {
            new ProcessExecutor().command(command).redirectOutput(Slf4jStream.of(log).asInfo()).redirectError(Slf4jStream.of(log).asError()).environment(environment).directory(pwd).exitValueNormal().executeNoTimeout();
            log.info("Docker Compose has finished running");
        } catch (InvalidExitValueException e) {
            throw new ContainerLaunchException("Local Docker Compose exited abnormally with code " + e.getExitValue() + " whilst running command: " + cmd);
        } catch (Exception e) {
            throw new ContainerLaunchException("Error running local Docker Compose command: " + cmd, e);
        }
    }

    /**
     * 功能：执行 `invokeDocker` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void invokeDocker() {
        // bail out early
        if (!CommandLine.executableExists(DOCKER_EXECUTABLE)) {
            throw new ContainerLaunchException("Local Docker not found. Is " + DOCKER_EXECUTABLE + " on the PATH?");
        }
        final File pwd = composeFiles.get(0).getAbsoluteFile().getParentFile().getAbsoluteFile();
        log.info("Local Docker is running command: {}", cmd);
        final List<String> command = Splitter.onPattern(" ").omitEmptyStrings().splitToList(DOCKER_EXECUTABLE + " " + cmd);
        try {
            new ProcessExecutor().command(command).redirectOutput(Slf4jStream.of(log).asInfo()).redirectError(Slf4jStream.of(log).asError()).directory(pwd).exitValueNormal().executeNoTimeout();
            log.info("Docker has finished running");
        } catch (InvalidExitValueException e) {
            throw new ContainerLaunchException("Local Docker exited abnormally with code " + e.getExitValue() + " whilst running command: " + cmd);
        } catch (Exception e) {
            throw new ContainerLaunchException("Error running local Docker command: " + cmd, e);
        }
    }

    /**
     * 功能：校验文件。
     * 参数：
     * - `composeFiles`：数据列表。
     * 返回：无。
     */
    void validateFileList(List<File> composeFiles) {
        checkNotNull(composeFiles);
        checkArgument(!composeFiles.isEmpty(), "No docker compose file have been provided");
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`DockerComposeExecutor` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
