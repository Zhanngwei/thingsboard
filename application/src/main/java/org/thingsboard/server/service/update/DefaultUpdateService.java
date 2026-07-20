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
package org.thingsboard.server.service.update;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EdgeUpgradeMessage;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.notification.rule.trigger.NewPlatformVersionTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.instructions.EdgeInstallInstructionsService;
import org.thingsboard.server.service.edge.instructions.EdgeUpgradeInstructionsService;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `DefaultUpdateService` 是 ThingsBoard Application 中负责 `Update` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `UpdateService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@Slf4j
public class DefaultUpdateService implements UpdateService {

    /**
     * 文件常量，用于统一引用固定值。
     */
    private static final String INSTANCE_ID_FILE = ".instance_id";
    private static final String UPDATE_SERVER_BASE_URL = "https://updates.thingsboard.io";

    /**
     * `PLATFORM_PARAM`常量，用于统一引用固定值。
     */
    private static final String PLATFORM_PARAM = "platform";
    private static final String VERSION_PARAM = "version";
    /**
     * `INSTANCE_ID_PARAM`常量，用于统一引用固定值。
     */
    private static final String INSTANCE_ID_PARAM = "instanceId";

    /**
     * 是否启用`updates`。
     */
    @Value("${updates.enabled}")
    private boolean updatesEnabled;

    /**
     * `buildProperties` 字段，保存当前对象的对应属性。
     */
    @Autowired(required = false)
    private BuildProperties buildProperties;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired
    private NotificationRuleProcessor notificationRuleProcessor;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    private EdgeInstallInstructionsService edgeInstallInstructionsService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    private EdgeUpgradeInstructionsService edgeUpgradeInstructionsService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, ThingsBoardThreadFactory.forName("tb-update-service"));

    /**
     * 异步结果，表示当前对象的对应属性。
     */
    private ScheduledFuture<?> checkUpdatesFuture = null;
    private final RestTemplate restClient = new RestTemplate();

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private UpdateMessage updateMessage;

    /**
     * `platform` 字段，保存当前对象的对应属性。
     */
    private String platform;
    private String version;
    /**
     * `instanceId`ID，用于定位对应业务对象。
     */
    private UUID instanceId = null;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() {
        version = buildProperties != null ? buildProperties.getVersion() : "unknown";
        updateMessage = new UpdateMessage(false, version, "", "",
                "https://thingsboard.io/docs/reference/releases",
                "https://thingsboard.io/docs/reference/releases");
        if (updatesEnabled) {
            try {
                platform = System.getProperty("platform", "unknown");
                instanceId = parseInstanceId();
                checkUpdatesFuture = scheduler.scheduleAtFixedRate(checkUpdatesRunnable, 0, 1, TimeUnit.HOURS);
            } catch (Exception e) {
                //Do nothing
            }
        }
    }

    /**
     * 功能：解析`Instance Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    private UUID parseInstanceId() throws IOException {
        UUID result = null;
        Path instanceIdPath = Paths.get(INSTANCE_ID_FILE);
        if (instanceIdPath.toFile().exists()) {
            byte[] data = Files.readAllBytes(instanceIdPath);
            if (data.length > 0) {
                try {
                    result = UUID.fromString(new String(data));
                } catch (IllegalArgumentException e) {
                    //Do nothing
                }
            }
        }
        if (result == null) {
            result = UUID.randomUUID();
            Files.write(instanceIdPath, result.toString().getBytes());
        }
        return result;
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        try {
            if (checkUpdatesFuture != null) {
                checkUpdatesFuture.cancel(true);
            }
            scheduler.shutdownNow();
        } catch (Exception e) {
            //Do nothing
        }
    }

    Runnable checkUpdatesRunnable = () -> {
        try {
            log.trace("Executing check update method for instanceId [{}], platform [{}] and version [{}]", instanceId, platform, version);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectNode request = JacksonUtil.newObjectNode();
            request.put(PLATFORM_PARAM, platform);
            request.put(VERSION_PARAM, version);
            request.put(INSTANCE_ID_PARAM, instanceId.toString());
            UpdateMessage prevUpdateMessage = updateMessage;
            updateMessage = restClient.postForObject(UPDATE_SERVER_BASE_URL + "/api/v2/thingsboard/updates", new HttpEntity<>(request.toString(), headers), UpdateMessage.class);
            if (updateMessage != null && updateMessage.isUpdateAvailable() && !updateMessage.equals(prevUpdateMessage)) {
                notificationRuleProcessor.process(NewPlatformVersionTrigger.builder()
                        .updateInfo(updateMessage)
                        .build());
            }
            ObjectNode edgeRequest = JacksonUtil.newObjectNode().put(VERSION_PARAM, version);
            String edgeInstallVersion = restClient.postForObject(UPDATE_SERVER_BASE_URL + "/api/v1/edge/installMapping", new HttpEntity<>(edgeRequest.toString(), headers), String.class);
            if (edgeInstallVersion != null) {
                edgeInstallInstructionsService.setAppVersion(edgeInstallVersion);
                edgeUpgradeInstructionsService.setAppVersion(edgeInstallVersion);
            }
            EdgeUpgradeMessage edgeUpgradeMessage = restClient.postForObject(UPDATE_SERVER_BASE_URL + "/api/v1/edge/upgradeMapping", new HttpEntity<>(edgeRequest.toString(), headers), EdgeUpgradeMessage.class);
            if (edgeUpgradeMessage != null) {
                edgeUpgradeInstructionsService.updateInstructionMap(edgeUpgradeMessage.getEdgeVersions());
            }
        } catch (Exception e) {
            log.trace(e.getMessage());
        }
    };

    /**
     * 功能：校验`Updates`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public UpdateMessage checkUpdates() {
        return updateMessage;
    }
}
