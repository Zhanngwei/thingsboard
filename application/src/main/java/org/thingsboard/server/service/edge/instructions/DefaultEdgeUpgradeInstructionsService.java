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
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUpgradeInfo;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`DefaultEdgeUpgradeInstructionsService` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
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
public class DefaultEdgeUpgradeInstructionsService implements EdgeUpgradeInstructionsService {

    private static final Map<String, EdgeUpgradeInfo> upgradeVersionHashMap = new HashMap<>();

    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    private static final String EDGE_DIR = "edge";
    private static final String INSTRUCTIONS_DIR = "instructions";
    /**
     * `UPGRADE_DIR`常量，用于统一引用固定值。
     */
    private static final String UPGRADE_DIR = "upgrade";

    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    private final InstallScripts installScripts;
    private final AttributesService attributesService;

    /**
     * 版本号，表示当前对象的对应属性。
     */
    @Value("${app.version:unknown}")
    @Setter
    private String appVersion;

    /**
     * 功能：获取`Upgrade Instructions`。
     * 参数：
     * - `edgeVersion`：`edgeVersion` 参数。
     * - `upgradeMethod`：`upgradeMethod` 参数。
     * 返回：处理结果。
     */
    @Override
    public EdgeInstructions getUpgradeInstructions(String edgeVersion, String upgradeMethod) {
        String tbVersion = appVersion.replace("-SNAPSHOT", "");
        String currentEdgeVersion = convertEdgeVersionToDocsFormat(edgeVersion);
        switch (upgradeMethod.toLowerCase()) {
            case "docker":
                return getDockerUpgradeInstructions(tbVersion, currentEdgeVersion);
            case "ubuntu":
            case "centos":
                return getLinuxUpgradeInstructions(tbVersion, currentEdgeVersion, upgradeMethod.toLowerCase());
            default:
                throw new IllegalArgumentException("Unsupported upgrade method for Edge: " + upgradeMethod);
        }
    }

    /**
     * 功能：更新`Instruction Map`。
     * 参数：
     * - `map`：键值映射。
     * 返回：无。
     */
    @Override
    public void updateInstructionMap(Map<String, EdgeUpgradeInfo> map) {
        for (String key : map.keySet()) {
            upgradeVersionHashMap.put(key, map.get(key));
        }
    }

    /**
     * 功能：判断`Upgrade Available`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：判断结果。
     */
    @Override
    public boolean isUpgradeAvailable(TenantId tenantId, EdgeId edgeId) throws Exception {
        Optional<AttributeKvEntry> attributeKvEntryOpt = attributesService.find(tenantId, edgeId, DataConstants.SERVER_SCOPE, DataConstants.EDGE_VERSION_ATTR_KEY).get();
        if (attributeKvEntryOpt.isPresent()) {
            String edgeVersionFormatted = convertEdgeVersionToDocsFormat(attributeKvEntryOpt.get().getValueAsString());
            return isVersionGreaterOrEqualsThan(edgeVersionFormatted, "3.6.0") && !isVersionGreaterOrEqualsThan(edgeVersionFormatted, appVersion);
        }
        return false;
    }

    /**
     * 功能：判断版本号。
     * 参数：
     * - `version1`：`version1` 参数。
     * - `version2`：`version2` 参数。
     * 返回：判断结果。
     */
    private boolean isVersionGreaterOrEqualsThan(String version1, String version2) {
        String[] v1 = version1.split("\\.");
        String[] v2 = version2.split("\\.");

        int length = Math.max(v1.length, v2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < v1.length ? Integer.parseInt(v1[i]) : 0;
            int num2 = i < v2.length ? Integer.parseInt(v2[i]) : 0;

            if (num1 < num2) {
                return false;
            } else if (num1 > num2) {
                return true;
            }
        }
        return true;
    }

    /**
     * 功能：获取`Docker Upgrade Instructions`。
     * 参数：
     * - `tbVersion`：`tbVersion` 参数。
     * - `currentEdgeVersion`：`currentEdgeVersion` 参数。
     * 返回：处理结果。
     */
    private EdgeInstructions getDockerUpgradeInstructions(String tbVersion, String currentEdgeVersion) {
        EdgeUpgradeInfo edgeUpgradeInfo = upgradeVersionHashMap.get(currentEdgeVersion);
        if (edgeUpgradeInfo == null || edgeUpgradeInfo.getNextEdgeVersion() == null || tbVersion.equals(currentEdgeVersion)) {
            return new EdgeInstructions("Edge upgrade instruction for " + currentEdgeVersion + "EDGE is not available.");
        }
        StringBuilder result = new StringBuilder(readFile(resolveFile("docker", "upgrade_preparing.md")));
        while (edgeUpgradeInfo.getNextEdgeVersion() != null && !tbVersion.equals(currentEdgeVersion)) {
            String edgeVersion = edgeUpgradeInfo.getNextEdgeVersion();
            String dockerUpgradeInstructions = readFile(resolveFile("docker", "instructions.md"));
            if (edgeUpgradeInfo.isRequiresUpdateDb()) {
                String upgradeDb = readFile(resolveFile("docker", "upgrade_db.md"));
                dockerUpgradeInstructions = dockerUpgradeInstructions.replace("${UPGRADE_DB}", upgradeDb);
            } else {
                dockerUpgradeInstructions = dockerUpgradeInstructions.replace("${UPGRADE_DB}", "");
            }
            dockerUpgradeInstructions = dockerUpgradeInstructions.replace("${TB_EDGE_VERSION}", edgeVersion + "EDGE");
            dockerUpgradeInstructions = dockerUpgradeInstructions.replace("${FROM_TB_EDGE_VERSION}", currentEdgeVersion + "EDGE");
            currentEdgeVersion = edgeVersion;
            edgeUpgradeInfo = upgradeVersionHashMap.get(edgeUpgradeInfo.getNextEdgeVersion());
            result.append(dockerUpgradeInstructions);
        }
        String startService = readFile(resolveFile("docker", "start_service.md"));
        startService = startService.replace("${TB_EDGE_VERSION}", currentEdgeVersion + "EDGE");
        result.append(startService);
        return new EdgeInstructions(result.toString());
    }

    /**
     * 功能：获取`Linux Upgrade Instructions`。
     * 参数：
     * - `tbVersion`：`tbVersion` 参数。
     * - `currentEdgeVersion`：`currentEdgeVersion` 参数。
     * - `os`：`os` 参数。
     * 返回：处理结果。
     */
    private EdgeInstructions getLinuxUpgradeInstructions(String tbVersion, String currentEdgeVersion, String os) {
        EdgeUpgradeInfo edgeUpgradeInfo = upgradeVersionHashMap.get(currentEdgeVersion);
        if (edgeUpgradeInfo == null || edgeUpgradeInfo.getNextEdgeVersion() == null || tbVersion.equals(currentEdgeVersion)) {
            return new EdgeInstructions("Edge upgrade instruction for " + currentEdgeVersion + "EDGE is not available.");
        }
        String upgrade_preparing = readFile(resolveFile("upgrade_preparing.md"));
        upgrade_preparing = upgrade_preparing.replace("${OS}", os.equals("centos") ? "RHEL/CentOS 7/8" : "Ubuntu");
        StringBuilder result = new StringBuilder(upgrade_preparing);
        while (edgeUpgradeInfo.getNextEdgeVersion() != null && !tbVersion.equals(currentEdgeVersion)) {
            String edgeVersion = edgeUpgradeInfo.getNextEdgeVersion();
            String linuxUpgradeInstructions = readFile(resolveFile(os, "instructions.md"));
            if (edgeUpgradeInfo.isRequiresUpdateDb()) {
                String upgradeDb = readFile(resolveFile("upgrade_db.md"));
                linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${UPGRADE_DB}", upgradeDb);
            } else {
                linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${UPGRADE_DB}", "");
            }
            linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${TB_EDGE_TAG}", getTagVersion(edgeVersion));
            linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${FROM_TB_EDGE_TAG}", getTagVersion(currentEdgeVersion));
            linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
            linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${FROM_TB_EDGE_VERSION}", currentEdgeVersion);
            currentEdgeVersion = edgeVersion;
            edgeUpgradeInfo = upgradeVersionHashMap.get(edgeUpgradeInfo.getNextEdgeVersion());
            result.append(linuxUpgradeInstructions);
        }
        String startService = readFile(resolveFile("start_service.md"));
        result.append(startService);
        return new EdgeInstructions(result.toString());
    }

    /**
     * 功能：获取版本号。
     * 参数：
     * - `version`：`version` 参数。
     * 返回：文本结果。
     */
    private String getTagVersion(String version) {
        return version.endsWith(".0") ? version.substring(0, version.length() - 2) : version;
    }

    /**
     * 功能：转换边缘节点。
     * 参数：
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：文本结果。
     */
    private String convertEdgeVersionToDocsFormat(String edgeVersion) {
        return edgeVersion.replace("_", ".").substring(2);
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
        return Paths.get(installScripts.getDataDir(), InstallScripts.JSON_DIR, EDGE_DIR, INSTRUCTIONS_DIR, UPGRADE_DIR);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultEdgeUpgradeInstructionsService` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
