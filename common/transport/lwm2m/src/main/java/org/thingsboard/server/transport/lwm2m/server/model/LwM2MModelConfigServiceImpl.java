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
package org.thingsboard.server.transport.lwm2m.server.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesRequest;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MModelConfigStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `LwM2MModelConfigServiceImpl` 是 ThingsBoard Common Transport 中负责 LwM2M 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `LwM2MModelConfigService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Service
@TbLwM2mTransportComponent
public class LwM2MModelConfigServiceImpl implements LwM2MModelConfigService {

    /**
     * 存储组件，表示当前对象的对应属性。
     */
    @Autowired
    TbLwM2MModelConfigStore modelStore;

    /**
     * 消息，负责处理对应任务或消息。
     */
    @Autowired
    @Lazy
    private LwM2mDownlinkMsgHandler downlinkMsgHandler;
    /**
     * 消息，负责处理对应任务或消息。
     */
    @Autowired
    @Lazy
    private LwM2mUplinkMsgHandler uplinkMsgHandler;
    /**
     * 上下文，用于发起外部调用或协议交互。
     */
    @Autowired
    @Lazy
    private LwM2mClientContext clientContext;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private LwM2MTelemetryLogService logService;

    /**
     * `currentModelConfigs`映射关系，用于按键查找对应值。
     */
    ConcurrentMap<String, LwM2MModelConfig> currentModelConfigs;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.BEFORE_TRANSPORT_SERVICE)
    public void init() {
        List<LwM2MModelConfig> models = modelStore.getAll();
        log.debug("Fetched model configs: {}", models);
        currentModelConfigs = models.stream()
                .collect(Collectors.toConcurrentMap(LwM2MModelConfig::getEndpoint, m -> m, (existing, replacement) -> existing));
    }

    /**
     * 功能：发送或提交`Updates`。
     * 参数：
     * - `lwM2mClient`：客户端对象。
     * 返回：无。
     */
    @Override
    public void sendUpdates(LwM2mClient lwM2mClient) {
        LwM2MModelConfig modelConfig = currentModelConfigs.get(lwM2mClient.getEndpoint());
        if (modelConfig == null || modelConfig.isEmpty()) {
            return;
        }

        doSend(lwM2mClient, modelConfig);
    }

    /**
     * 功能：发送或提交`Updates`。
     * 参数：
     * - `lwM2mClient`：客户端对象。
     * - `newModelConfig`：配置对象。
     * 返回：无。
     */
    public void sendUpdates(LwM2mClient lwM2mClient, LwM2MModelConfig newModelConfig) {
        String endpoint = lwM2mClient.getEndpoint();
        LwM2MModelConfig modelConfig = currentModelConfigs.get(endpoint);
        if (modelConfig == null || modelConfig.isEmpty()) {
            modelConfig = newModelConfig;
            currentModelConfigs.put(endpoint, modelConfig);
        } else {
            modelConfig.merge(newModelConfig);
        }

        if (lwM2mClient.isAsleep()) {
            modelStore.put(modelConfig);
        } else {
            doSend(lwM2mClient, modelConfig);
        }
    }

    /**
     * 功能：执行 `doSend` 对应的处理。
     * 参数：
     * - `lwM2mClient`：客户端对象。
     * - `modelConfig`：配置对象。
     * 返回：无。
     */
    private void doSend(LwM2mClient lwM2mClient, LwM2MModelConfig modelConfig) {
        log.trace("Send LwM2M Model updates: [{}]", modelConfig);

        String endpoint = lwM2mClient.getEndpoint();

        Map<String, ObjectAttributes> attrToAdd = modelConfig.getAttributesToAdd();
        attrToAdd.forEach((id, attributes) -> {
            TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(id)
                    .attributes(attributes)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendWriteAttributesRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        attrToAdd.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MWriteAttributesCallback(logService, lwM2mClient, id))
            );
        });

        Set<String> attrToRemove = modelConfig.getAttributesToRemove();
        attrToRemove.forEach((id) -> {
            TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(id)
                    .attributes(new ObjectAttributes())
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendWriteAttributesRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        attrToRemove.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MWriteAttributesCallback(logService, lwM2mClient, id))
            );
        });

        Set<String> toRead = modelConfig.getToRead();
        toRead.forEach(id -> {
            TbLwM2MReadRequest request = TbLwM2MReadRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendReadRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toRead.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MReadCallback(uplinkMsgHandler, logService, lwM2mClient, id))
            );
        });

        Set<String> toObserve = modelConfig.getToObserve();
        toObserve.forEach(id -> {
            TbLwM2MObserveRequest request = TbLwM2MObserveRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendObserveRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toObserve.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MObserveCallback(uplinkMsgHandler, logService, lwM2mClient, id))
            );
        });

        Set<String> toCancelObserve = modelConfig.getToCancelObserve();
        toCancelObserve.forEach(id -> {
            TbLwM2MCancelObserveRequest request = TbLwM2MCancelObserveRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendCancelObserveRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toCancelObserve.remove(id);
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MCancelObserveCallback(logService, lwM2mClient, id))
            );
        });
    }

    /**
     * 功能：保存或创建回调。
     * 参数：
     * - `processRemove`：`processRemove` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：处理结果。
     */
    private <R, T> DownlinkRequestCallback<R, T> createDownlinkProxyCallback(Runnable processRemove, DownlinkRequestCallback<R, T> callback) {
        return new DownlinkRequestCallback<>() {
            @Override
            public void onSuccess(R request, T response) {
                processRemove.run();
                callback.onSuccess(request, response);
            }

            @Override
            public void onValidationError(String params, String msg) {
                processRemove.run();
                callback.onValidationError(params, msg);
            }

            @Override
            public void onError(String params, Exception e) {
                try {
                    if (e instanceof TimeoutException) {
                        return;
                    }
                    processRemove.run();
                } finally {
                    callback.onError(params, e);
                }
            }

        };
    }

    /**
     * 功能：执行 `persistUpdates` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：无。
     */
    @Override
    public void persistUpdates(String endpoint) {
        LwM2MModelConfig modelConfig = currentModelConfigs.get(endpoint);
        if (modelConfig != null && !modelConfig.isEmpty()) {
            modelStore.put(modelConfig);
        }
    }

    /**
     * 功能：删除或清理`Updates`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：无。
     */
    @Override
    public void removeUpdates(String endpoint) {
        currentModelConfigs.remove(endpoint);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        currentModelConfigs.values().forEach(model -> {
            if (model != null && !model.isEmpty()) {
                modelStore.put(model);
            }
        });
    }
}
