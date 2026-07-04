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
package org.thingsboard.server.transport.lwm2m.server.attributes;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteReplaceRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteResponseCallback;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper.getValueFromKvProto;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_TAG;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_TITLE;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_URL;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_VERSION;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_TAG;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_TITLE;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_URL;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_VERSION;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_WARN;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.compareAttNameKeyOta;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertMultiResourceValuesFromJson;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.valueEquals;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DefaultLwM2MAttributesService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class DefaultLwM2MAttributesService implements LwM2MAttributesService {

    //TODO: add timeout logic
    private final AtomicInteger reqIdSeq = new AtomicInteger();
    /**
     * 字段说明：
     * 1. 保存 `futures` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final Map<Integer, SettableFuture<List<TransportProtos.TsKvProto>>> futures;

    /**
     * 字段说明：
     * 1. 保存 `transportService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TransportService transportService;
    private final LwM2mTransportServerHelper helper;
    /**
     * 字段说明：
     * 1. 保存 `clientContext` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2mClientContext clientContext;
    private final LwM2MTransportServerConfig config;
    /**
     * 字段说明：
     * 1. 保存 `uplinkHandler` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2mUplinkMsgHandler uplinkHandler;
    private final LwM2mDownlinkMsgHandler downlinkHandler;
    /**
     * 字段说明：
     * 1. 保存 `logService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2MTelemetryLogService logService;
    private final LwM2MOtaUpdateService otaUpdateService;
    /**
     * 字段说明：
     * 1. 保存 `modelProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2mModelProvider modelProvider;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getSharedAttributes` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<List<TransportProtos.TsKvProto>> getSharedAttributes(LwM2mClient client, Collection<String> keys) {
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        SettableFuture<List<TransportProtos.TsKvProto>> future = SettableFuture.create();
        int requestId = reqIdSeq.incrementAndGet();
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        futures.put(requestId, future);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        transportService.process(client.getSession(), TransportProtos.GetAttributeRequestMsg.newBuilder().setRequestId(requestId).
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                addAllSharedAttributeNames(keys).build(), new TransportServiceCallback<Void>() {
            @Override
            public void onSuccess(Void msg) {

            }

            @Override
            public void onError(Throwable e) {
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                SettableFuture<List<TransportProtos.TsKvProto>> callback = futures.remove(requestId);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (callback != null) {
                    callback.setException(e);
                }
            }
        });
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        return future;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onGetAttributesResponse` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onGetAttributesResponse(GetAttributeResponseMsg getAttributesResponse, TransportProtos.SessionInfoProto sessionInfo) {
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        var callback = futures.remove(getAttributesResponse.getRequestId());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (callback != null) {
            callback.set(getAttributesResponse.getSharedAttributeListList());
        }
    }

    /**
     * Update - send request in change value resources in Client
     * 1. FirmwareUpdate:
     * - If msg.getSharedUpdatedList().forEach(tsKvProto -> {tsKvProto.getKv().getKey().indexOf(FIRMWARE_UPDATE_PREFIX, 0) == 0
     * 2. Shared Other AttributeUpdate
     * -- Path to resources from profile equal keyName or from ModelObject equal name
     * -- Only for resources:  isWritable && isPresent as attribute in profile -> LwM2MClientProfile (format: CamelCase)
     * 3. Delete - nothing
     *
     * @param msg -
     */
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onAttributesUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onAttributesUpdate(TransportProtos.AttributeUpdateNotificationMsg msg, TransportProtos.SessionInfoProto sessionInfo) {
        LwM2mClient lwM2MClient = clientContext.getClientBySessionInfo(sessionInfo);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (msg.getSharedUpdatedCount() > 0 && lwM2MClient != null) {
            String newFirmwareTitle = null;
            String newFirmwareVersion = null;
            String newFirmwareTag = null;
            String newFirmwareUrl = null;
            String newSoftwareTitle = null;
            String newSoftwareVersion = null;
            String newSoftwareTag = null;
            String newSoftwareUrl = null;
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            List<TransportProtos.TsKvProto> otherAttributes = new ArrayList<>();
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            for (TransportProtos.TsKvProto tsKvProto : msg.getSharedUpdatedList()) {
                String attrName = tsKvProto.getKv().getKey();
                if (compareAttNameKeyOta(attrName)) {
                    if (FIRMWARE_TITLE.equals(attrName)) {
                        newFirmwareTitle = getStrValue(tsKvProto);
                    } else if (FIRMWARE_VERSION.equals(attrName)) {
                        newFirmwareVersion = getStrValue(tsKvProto);
                    } else if (FIRMWARE_TAG.equals(attrName)) {
                        newFirmwareTag = getStrValue(tsKvProto);
                    } else if (FIRMWARE_URL.equals(attrName)) {
                        newFirmwareUrl = getStrValue(tsKvProto);
                    } else if (SOFTWARE_TITLE.equals(attrName)) {
                        newSoftwareTitle = getStrValue(tsKvProto);
                    } else if (SOFTWARE_VERSION.equals(attrName)) {
                        newSoftwareVersion = getStrValue(tsKvProto);
                    } else if (SOFTWARE_TAG.equals(attrName)) {
                        newSoftwareTag = getStrValue(tsKvProto);
                    } else if (SOFTWARE_URL.equals(attrName)) {
                        newSoftwareUrl = getStrValue(tsKvProto);
                    }
                } else {
                    otherAttributes.add(tsKvProto);
                }
            }
            if (newFirmwareTitle != null || newFirmwareVersion != null) {
                otaUpdateService.onTargetFirmwareUpdate(lwM2MClient, newFirmwareTitle, newFirmwareVersion, Optional.ofNullable(newFirmwareUrl), Optional.ofNullable(newFirmwareTag));
            }
            if (newSoftwareTitle != null || newSoftwareVersion != null) {
                otaUpdateService.onTargetSoftwareUpdate(lwM2MClient, newSoftwareTitle, newSoftwareVersion, Optional.ofNullable(newSoftwareUrl), Optional.ofNullable(newSoftwareTag));
            }
            if (!otherAttributes.isEmpty()) {
                onAttributesUpdate(lwM2MClient, otherAttributes, true);
            }
        } else if (lwM2MClient == null) {
            log.error("OnAttributeUpdate, lwM2MClient is null");
        }
    }

    /**
     * #1.1 If two names have equal path => last time attribute
     * #2.1 if there is a difference in values between the current resource values and the shared attribute values
     * => send to client Request Update of value (new value from shared attribute)
     * and LwM2MClient.delayedRequests.add(path)
     * #2.1 if there is not a difference in values between the current resource values and the shared attribute values
     */
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onAttributesUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onAttributesUpdate(LwM2mClient lwM2MClient, List<TransportProtos.TsKvProto> tsKvProtos, boolean logFailedUpdateOfNonChangedValue) {
        log.trace("[{}] onAttributesUpdate [{}]", lwM2MClient.getEndpoint(), tsKvProtos);
        Map<String, TransportProtos.TsKvProto> attributesUpdate = new ConcurrentHashMap<>();
        tsKvProtos.forEach(tsKvProto -> {
            try {
                String pathIdVer = clientContext.getObjectIdByKeyNameFromProfile(lwM2MClient, tsKvProto.getKv().getKey());
                if (pathIdVer != null) {
                    // #1.1
                    if (lwM2MClient.getSharedAttributes().containsKey(pathIdVer)) {
                        if (tsKvProto.getTs() > lwM2MClient.getSharedAttributes().get(pathIdVer).getTs()) {
                            attributesUpdate.put(pathIdVer, tsKvProto);
                        }
                    } else {
                        attributesUpdate.put(pathIdVer, tsKvProto);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Failed update resource [" + lwM2MClient.getEndpoint() + "] onAttributesUpdate:", e);
                String logMsg = String.format("%s: Failed update resource onAttributesUpdate %s.",
                        LOG_LWM2M_ERROR, e.getMessage());
                logService.log(lwM2MClient, logMsg);
            }
        });
        clientContext.update(lwM2MClient);
        // #2.1
        attributesUpdate.forEach((pathIdVer, tsKvProto) -> {
            ResourceModel resourceModel = lwM2MClient.getResourceModel(pathIdVer, modelProvider);
            Object newValProto = getValueFromKvProto(tsKvProto.getKv());
            Object oldResourceValue = this.getResourceValueFormatKv(lwM2MClient, pathIdVer);
            if (!resourceModel.multiple || !(newValProto instanceof JsonElement)) {
                this.pushUpdateToClientIfNeeded(lwM2MClient, oldResourceValue, newValProto, pathIdVer, tsKvProto, logFailedUpdateOfNonChangedValue);
            } else {
                try {
                    pushUpdateMultiToClientIfNeeded(lwM2MClient, resourceModel, (JsonElement) newValProto,
                            (Map<Integer, LwM2mResourceInstance>) oldResourceValue, pathIdVer, tsKvProto, logFailedUpdateOfNonChangedValue);
                } catch (Exception e) {
                    log.error("Failed update resource [" + lwM2MClient.getEndpoint() + "] onAttributesUpdate:", e);
                    String logMsg = String.format("%s: Failed update resource onAttributesUpdate %s.",
                            LOG_LWM2M_ERROR, e.getMessage());
                    logService.log(lwM2MClient, logMsg);
                }
            }
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushUpdateToClientIfNeeded` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushUpdateToClientIfNeeded(LwM2mClient lwM2MClient, Object oldValue, Object newValue,
                                            String versionedId, TransportProtos.TsKvProto tsKvProto, boolean logFailedUpdateOfNonChangedValue) {
        if (newValue == null) {
            String logMsg = String.format("%s: Failed update resource versionedId - %s value - %s. New value is  bad",
                    LOG_LWM2M_ERROR, versionedId, "null");
            logService.log(lwM2MClient, logMsg);
            log.error("Failed update resource [{}] [{}]", versionedId, "null");
        } else if ((oldValue == null) || !valueEquals(newValue, oldValue)) {
            TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId).value(newValue).timeout(clientContext.getRequestTimeout(lwM2MClient)).build();
            downlinkHandler.sendWriteReplaceRequest(lwM2MClient, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, lwM2MClient, versionedId) {
                @Override
                public void onSuccess(WriteRequest request, WriteResponse response) {
                    client.getSharedAttributes().put(versionedId, tsKvProto);
                    super.onSuccess(request, response);
                }
            });
        } else if (logFailedUpdateOfNonChangedValue) {
            String logMsg = String.format("%s: Didn't update the versionedId resource - %s value - %s. Value is not changed",
                    LOG_LWM2M_WARN, versionedId, newValue);
            logService.log(lwM2MClient, logMsg);
            log.warn("Didn't update resource [{}] [{}]. Value is not changed", versionedId, newValue);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushUpdateMultiToClientIfNeeded` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushUpdateMultiToClientIfNeeded(LwM2mClient client, ResourceModel resourceModel, JsonElement newValProto,
                                                 Map<Integer, LwM2mResourceInstance> valueOld, String versionedId,
                                                 TransportProtos.TsKvProto tsKvProto, boolean logFailedUpdateOfNonChangedValue) {
        Map<Integer, Object> newValues = convertMultiResourceValuesFromJson(newValProto, resourceModel.type, versionedId);
        if (newValues.size() > 0 && valueOld != null && valueOld.size() > 0) {
            valueOld.values().forEach((v) -> {
                if (newValues.containsKey(v.getId())) {
                    if (valueEquals(newValues.get(v.getId()), v.getValue())) {
                        newValues.remove(v.getId());
                    }
                }
            });
        }

        if (newValues.size() > 0) {
            TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId).value(newValues).timeout(this.config.getTimeout()).build();
            downlinkHandler.sendWriteReplaceRequest(client, request, new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId) {
                @Override
                public void onSuccess(WriteRequest request, WriteResponse response) {
                    client.getSharedAttributes().put(versionedId, tsKvProto);
                    super.onSuccess(request, response);
                }
            });
        } else if (logFailedUpdateOfNonChangedValue) {
            log.warn("Didn't update resource [{}] [{}]", versionedId, newValProto);
            String logMsg = String.format("%s: Didn't update resource versionedId - %s value - %s. Value is not changed",
                    LOG_LWM2M_WARN, versionedId, newValProto);
            logService.log(client, logMsg);
        }
    }

    /**
     * @param pathIdVer - path resource
     * @return - value of Resource into format KvProto or null
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `getResourceValueFormatKv` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Object getResourceValueFormatKv(LwM2mClient lwM2MClient, String pathIdVer) {
        LwM2mResource resourceValue = LwM2MTransportUtil.getResourceValueFromLwM2MClient(lwM2MClient, pathIdVer);
        if (resourceValue != null) {
            ResourceModel.Type currentType = resourceValue.getType();
            ResourceModel.Type expectedType = helper.getResourceModelTypeEqualsKvProtoValueType(currentType, pathIdVer);
            if (!resourceValue.isMultiInstances()) {
                return LwM2mValueConverterImpl.getInstance().convertValue(resourceValue.getValue(), currentType, expectedType,
                        new LwM2mPath(fromVersionedIdToObjectId(pathIdVer)));
            } else if (resourceValue.getInstances().size() > 0) {
                return resourceValue.getInstances();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getStrValue` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String getStrValue(TransportProtos.TsKvProto tsKvProto) {
        return tsKvProto.getKv().getStringV();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultLwM2MAttributesService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
