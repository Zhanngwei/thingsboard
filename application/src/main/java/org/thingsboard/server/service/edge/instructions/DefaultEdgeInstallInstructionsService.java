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
package org.thingsboard.server.service.edge.instructions;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.dao.util.DeviceConnectivityUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 中文说明：
 * 1. 类目的：`DefaultEdgeInstallInstructionsService` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class DefaultEdgeInstallInstructionsService implements EdgeInstallInstructionsService {

    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    private static final String EDGE_DIR = "edge";
    private static final String INSTRUCTIONS_DIR = "instructions";
    /**
     * `INSTALL_DIR`常量，用于统一引用固定值。
     */
    private static final String INSTALL_DIR = "install";

    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    private final InstallScripts installScripts;

    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Value("${edges.rpc.port}")
    private int rpcPort;

    /**
     * 是否启用 SSL。
     */
    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;

    /**
     * 版本号，表示当前对象的对应属性。
     */
    @Value("${app.version:unknown}")
    @Setter
    private String appVersion;

    /**
     * 功能：获取`Install Instructions`。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `installationMethod`：`installationMethod` 参数。
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    @Override
    public EdgeInstructions getInstallInstructions(Edge edge, String installationMethod, HttpServletRequest request) {
        switch (installationMethod.toLowerCase()) {
            case "docker":
                return getDockerInstallInstructions(edge, request);
            case "ubuntu":
                return getUbuntuInstallInstructions(edge, request);
            case "centos":
                return getCentosInstallInstructions(edge, request);
            default:
                throw new IllegalArgumentException("Unsupported installation method for Edge: " + installationMethod);
        }
    }

    /**
     * 功能：获取`Docker Install Instructions`。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    private EdgeInstructions getDockerInstallInstructions(Edge edge, HttpServletRequest request) {
        String dockerInstallInstructions = readFile(resolveFile("docker", "instructions.md"));
        String baseUrl = request.getServerName();

        if (DeviceConnectivityUtil.isLocalhost(baseUrl)) {
            dockerInstallInstructions = dockerInstallInstructions.replace("${EXTRA_HOSTS}", "extra_hosts:\n      - \"host.docker.internal:host-gateway\"\n");
            dockerInstallInstructions = dockerInstallInstructions.replace("${BASE_URL}", "host.docker.internal");
        } else {
            dockerInstallInstructions = dockerInstallInstructions.replace("${EXTRA_HOSTS}", "");
            dockerInstallInstructions = dockerInstallInstructions.replace("${BASE_URL}", baseUrl);
        }
        String edgeVersion = appVersion + "EDGE";
        edgeVersion = edgeVersion.replace("-SNAPSHOT", "");
        dockerInstallInstructions = dockerInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        dockerInstallInstructions = replacePlaceholders(dockerInstallInstructions, edge);
        return new EdgeInstructions(dockerInstallInstructions);
    }

    /**
     * 功能：获取`Ubuntu Install Instructions`。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    private EdgeInstructions getUbuntuInstallInstructions(Edge edge, HttpServletRequest request) {
        String ubuntuInstallInstructions = readFile(resolveFile("ubuntu", "instructions.md"));
        ubuntuInstallInstructions = replacePlaceholders(ubuntuInstallInstructions, edge);
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${BASE_URL}", request.getServerName());
        String edgeVersion = appVersion.replace("-SNAPSHOT", "");
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        return new EdgeInstructions(ubuntuInstallInstructions);
    }


    /**
     * 功能：获取`Centos Install Instructions`。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    private EdgeInstructions getCentosInstallInstructions(Edge edge, HttpServletRequest request) {
        String centosInstallInstructions = readFile(resolveFile("centos", "instructions.md"));
        centosInstallInstructions = replacePlaceholders(centosInstallInstructions, edge);
        centosInstallInstructions = centosInstallInstructions.replace("${BASE_URL}", request.getServerName());
        String edgeVersion = appVersion.replace("-SNAPSHOT", "");
        centosInstallInstructions = centosInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        return new EdgeInstructions(centosInstallInstructions);
    }

    /**
     * 功能：执行 `replacePlaceholders` 对应的处理。
     * 参数：
     * - `instructions`：`instructions` 参数。
     * - `edge`：`edge` 参数。
     * 返回：文本结果。
     */
    private String replacePlaceholders(String instructions, Edge edge) {
        instructions = instructions.replace("${CLOUD_ROUTING_KEY}", edge.getRoutingKey());
        instructions = instructions.replace("${CLOUD_ROUTING_SECRET}", edge.getSecret());
        instructions = instructions.replace("${CLOUD_RPC_PORT}", Integer.toString(rpcPort));
        instructions = instructions.replace("${CLOUD_RPC_SSL_ENABLED}", Boolean.toString(sslEnabled));
        return instructions;
    }

    /**
     * 功能：执行 `readFile` 对应的处理。
     * 参数：
     * - `file`：`file` 参数。
     * 返回：文本结果。
     */
    private String readFile(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            log.warn("Failed to read file: {}", file, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：执行 `resolveFile` 对应的处理。
     * 参数：
     * - `subDir`：`subDir` 参数。
     * - `subDirs`：`subDirs` 参数。
     * 返回：处理结果。
     */
    private Path resolveFile(String subDir, String... subDirs) {
        return getEdgeInstallInstructionsDir().resolve(Paths.get(subDir, subDirs));
    }

    /**
     * 功能：获取边缘节点。
     * 参数：无。
     * 返回：处理结果。
     */
    private Path getEdgeInstallInstructionsDir() {
        return Paths.get(installScripts.getDataDir(), InstallScripts.JSON_DIR, EDGE_DIR, INSTRUCTIONS_DIR, INSTALL_DIR);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultEdgeInstallInstructionsService` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
