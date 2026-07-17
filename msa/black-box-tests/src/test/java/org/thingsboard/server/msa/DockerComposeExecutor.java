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
 * 1. `DockerComposeExecutor` 是 ThingsBoard Microservices 中处理 `Docker Compose Executor` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 它直接协作于事件源、上下文对象和后续处理组件。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
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
