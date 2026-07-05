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
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.CompositeDownlinkRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.SimpleDownlinkRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mVersionedModelProvider;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.common.LwM2MExecutorAwareService;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MReadCompositeRequest;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.rpc.composite.RpcWriteCompositeRequest;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.eclipse.leshan.core.attributes.Attribute.GREATER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.LESSER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.MAXIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.MINIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.STEP;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OBJLNK;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertMultiResourceValuesFromRpcBody;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.createModelsDefault;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.getVerFromPathIdVerOrId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.validateVersionedId;

/**
 * 中文说明：
 * 1. 类目的：`DefaultLwM2mDownlinkMsgHandler` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Service("lwM2mDownlinkMsgHandler")
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mDownlinkMsgHandler extends LwM2MExecutorAwareService implements LwM2mDownlinkMsgHandler {

    /**
     * 转换器，用于在不同数据结构之间转换。
     */
    public LwM2mValueConverterImpl converter;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final LwM2MTelemetryLogService logService;
    private final LwM2mClientContext clientContext;
    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    private final LwM2mVersionedModelProvider modelProvider;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        super.init();
        this.converter = LwM2mValueConverterImpl.getInstance();
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        log.trace("Destroying {}", getClass().getSimpleName());
        super.destroy();
    }

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    protected int getExecutorSize() {
        return config.getDownlinkPoolSize();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getExecutorName() {
        return "LwM2M Downlink";
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendReadRequest(LwM2mClient client, TbLwM2MReadRequest request, DownlinkRequestCallback<ReadRequest, ReadResponse> callback) {
        validateVersionedId(client, request);
        ReadRequest downlink = new ReadRequest(getRequestContentFormat(client, request.getVersionedId(), modelProvider), request.getObjectId());
        sendSimpleRequest(client, downlink, request.getTimeout(), callback);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * - `compositeContentFormat`：`compositeContentFormat` 参数。
     * 返回：无。
     */
    @Override
    public void sendReadCompositeRequest(LwM2mClient client, TbLwM2MReadCompositeRequest request,
                                         DownlinkRequestCallback<ReadCompositeRequest, ReadCompositeResponse> callback, ContentFormat compositeContentFormat) {
        try {
            ReadCompositeRequest downlink = new ReadCompositeRequest(compositeContentFormat, compositeContentFormat, request.getObjectIds());
            sendCompositeRequest(client, downlink, this.config.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendObserveRequest(LwM2mClient client, TbLwM2MObserveRequest request, DownlinkRequestCallback<ObserveRequest, ObserveResponse> callback) {
        try {
            validateVersionedId(client, request);
            LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
            Set<Observation> observations = context.getServer().getObservationService().getObservations(client.getRegistration());
            //TODO: should be able to use CompositeObservation
            if (observations.stream().noneMatch(observation -> ((SingleObservation)observation).getPath().equals(resultIds))) {
                ObserveRequest downlink;
                ContentFormat contentFormat = getReadRequestContentFormat(client, request, modelProvider);
                if (resultIds.isResource()) {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId());
                } else if (resultIds.isObjectInstance()) {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId());
                } else {
                    downlink = new ObserveRequest(contentFormat, resultIds.getObjectId());
                }
                log.info("[{}] Send observation: {}.", client.getEndpoint(), request.getVersionedId());
                sendSimpleRequest(client, downlink, request.getTimeout(), callback);
            } else {
                callback.onValidationError(resultIds.toString(), "Observation is already registered!");
            }
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendObserveAllRequest(LwM2mClient client, TbLwM2MObserveAllRequest request, DownlinkRequestCallback<TbLwM2MObserveAllRequest, Set<String>> callback) {
        Set<Observation> observations = context.getServer().getObservationService().getObservations(client.getRegistration());
        //TODO: should be able to use CompositeObservation
        Set<String> paths = observations.stream().map(observation -> ((SingleObservation)observation).getPath().toString()).collect(Collectors.toUnmodifiableSet());
        callback.onSuccess(request, paths);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendDiscoverAllRequest(LwM2mClient client, TbLwM2MDiscoverAllRequest request, DownlinkRequestCallback<TbLwM2MDiscoverAllRequest, List<Link>> callback) {
        callback.onSuccess(request, Arrays.asList(client.getRegistration().getSortedObjectLinks()));
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendExecuteRequest(LwM2mClient client, TbLwM2MExecuteRequest request, DownlinkRequestCallback<ExecuteRequest, ExecuteResponse> callback) {
        try {
            validateVersionedId(client, request);
            LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(request.getVersionedId()));
            ResourceModel resourceModelExecute = client.getResourceModel(request.getVersionedId(), modelProvider);
            if (resourceModelExecute == null) {
                LwM2mModel model = createModelsDefault();
                if (pathIds.isResource()) {
                    resourceModelExecute = model.getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
                }
            }
            if (resourceModelExecute == null) {
                callback.onValidationError(request.toString(), "ResourceModel with " + request.getVersionedId() +
                        " is absent in system. Need ddd Lwm2m Model with id=" + pathIds.getObjectId() + " ver=" +
                        getVerFromPathIdVerOrId(request.getVersionedId()) + " to profile.");
            } else if (resourceModelExecute.operations.isExecutable()) {
                ExecuteRequest downlink;
                if (request.getParams() != null && !resourceModelExecute.multiple) {
                    downlink = new ExecuteRequest(request.getObjectId(), (String) this.converter.convertValue(request.getParams(),
                            resourceModelExecute.type, ResourceModel.Type.STRING, new LwM2mPath(request.getObjectId())));
                } else {
                    downlink = new ExecuteRequest(request.getObjectId());
                }
                sendSimpleRequest(client, downlink, request.getTimeout(), callback);
            } else {
                callback.onValidationError(request.toString(), "Resource with " + request.getVersionedId() + " is not executable.");
            }
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendDeleteRequest(LwM2mClient client, TbLwM2MDeleteRequest request, DownlinkRequestCallback<DeleteRequest, DeleteResponse> callback) {
        try {
            validateVersionedId(client, request);
            sendSimpleRequest(client, new DeleteRequest(request.getObjectId()), request.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendCancelObserveRequest(LwM2mClient client, TbLwM2MCancelObserveRequest request, DownlinkRequestCallback<TbLwM2MCancelObserveRequest, Integer> callback) {
        validateVersionedId(client, request);
        int observeCancelCnt = context.getServer().getObservationService().cancelObservations(client.getRegistration(), request.getObjectId());
        callback.onSuccess(request, observeCancelCnt);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendCancelAllRequest(LwM2mClient client, TbLwM2MCancelAllRequest request, DownlinkRequestCallback<TbLwM2MCancelAllRequest, Integer> callback) {
        int observeCancelCnt = context.getServer().getObservationService().cancelObservations(client.getRegistration());
        callback.onSuccess(request, observeCancelCnt);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendDiscoverRequest(LwM2mClient client, TbLwM2MDiscoverRequest request, DownlinkRequestCallback<DiscoverRequest, DiscoverResponse> callback) {
        validateVersionedId(client, request);
        sendSimpleRequest(client, new DiscoverRequest(request.getObjectId()), request.getTimeout(), callback);
    }

    /**
     * Example # 1:
     * AttributeSet attributes = new AttributeSet(new Attribute(Attribute.MINIMUM_PERIOD, 10L),
     * new Attribute(Attribute.MAXIMUM_PERIOD, 100L));
     * WriteAttributesRequest requestTest = new WriteAttributesRequest(3, 0, 14, attributes);
     * sendSimpleRequest(client, requestTest, request.getTimeout(), callback);
     * <p>
     * Example # 2
     * Dimension and Object version are read only attributes.
     * addAttribute(attributes, DIMENSION, params.getDim(), dim -> dim >= 0 && dim <= 255);
     * addAttribute(attributes, OBJECT_VERSION, params.getVer(), StringUtils::isNotEmpty, Function.identity());
     */
    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendWriteAttributesRequest(LwM2mClient client, TbLwM2MWriteAttributesRequest request, DownlinkRequestCallback<WriteAttributesRequest, WriteAttributesResponse> callback) {
        try {
            validateVersionedId(client, request);
            if (request.getAttributes() == null) {
                throw new IllegalArgumentException("Attributes to write are not specified!");
            }
            ObjectAttributes params = request.getAttributes();
            List<Attribute> attributes = new LinkedList<>();
            addAttribute(attributes, MAXIMUM_PERIOD, params.getPmax());
            addAttribute(attributes, MINIMUM_PERIOD, params.getPmin());
            addAttribute(attributes, GREATER_THAN, params.getGt());
            addAttribute(attributes, LESSER_THAN, params.getLt());
            addAttribute(attributes, STEP, params.getSt());
            AttributeSet attributeSet = new AttributeSet(attributes);
            sendSimpleRequest(client, new WriteAttributesRequest(request.getObjectId(), attributeSet), request.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(request.toString(), e.getMessage());
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendWriteReplaceRequest(LwM2mClient client, TbLwM2MWriteReplaceRequest request, DownlinkRequestCallback<WriteRequest, WriteResponse> callback) {
        LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
        if (resultIds.isResource() || resultIds.isResourceInstance()) {
            validateVersionedId(client, request);
            ResourceModel resourceModelWrite = client.getResourceModel(request.getVersionedId(), modelProvider);
            if (resourceModelWrite != null) {
                ContentFormat contentFormat = getWriteRequestContentFormat(client, request, modelProvider);
                try {
                    WriteRequest downlink = null;
                    String msgError = "";
                    if (resourceModelWrite.multiple) {
                        try {
                            Map<Integer, Object> value = convertMultiResourceValuesFromRpcBody(request.getValue(), resourceModelWrite.type, request.getObjectId());
                            downlink = new WriteRequest(contentFormat, resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId(),
                                    value, resourceModelWrite.type);
                        } catch (Exception e) {
                        }
                    }
                    if (downlink == null) {
                        try {
                            downlink = this.getWriteRequestSingleResource(resourceModelWrite.type, contentFormat,
                                    resultIds.getObjectId(), resultIds.getObjectInstanceId(), resultIds.getResourceId(), request.getValue());
                        } catch (Exception e) {
                            msgError = "Resource id=" + resultIds.toString() + ", value = " + request.getValue() +
                                    ", class = " + request.getValue().getClass().getSimpleName() + ". Format value is bad. Value for this Single Resource must be " + resourceModelWrite.type + "!";
                        }
                    }
                    if (downlink != null) {
                        sendSimpleRequest(client, downlink, request.getTimeout(), callback);
                    } else {
                        callback.onValidationError(toString(request), msgError);
                    }
                } catch (Exception e) {
                    callback.onError(toString(request), e);
                }
            } else {
                callback.onValidationError(toString(request), "Resource " + request.getVersionedId() + " is not configured in the device profile!");
            }
        } else {
            callback.onValidationError(toString(request), "Resource " + request.getVersionedId() + ". This operation can only be used for Resource or ResourceInstance!");
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `rpcWriteCompositeRequest`：请求对象。
     * - `callback`：处理完成后的回调。
     * - `contentFormatComposite`：`contentFormatComposite` 参数。
     * 返回：无。
     */
    @Override
    public void sendWriteCompositeRequest(LwM2mClient client, RpcWriteCompositeRequest rpcWriteCompositeRequest,
                                          DownlinkRequestCallback<WriteCompositeRequest, WriteCompositeResponse> callback, ContentFormat contentFormatComposite) {
        try {
            WriteCompositeRequest downlink = new WriteCompositeRequest(contentFormatComposite, rpcWriteCompositeRequest.getNodes());
            //TODO: replace config.getTimeout();
            sendWriteCompositeRequest(client, downlink, this.config.getTimeout(), callback);
        } catch (InvalidRequestException e) {
            callback.onValidationError(rpcWriteCompositeRequest.toString(), e.getMessage());
        } catch (Exception e) {
            callback.onError(toString(rpcWriteCompositeRequest), e);
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void sendWriteUpdateRequest(LwM2mClient client, TbLwM2MWriteUpdateRequest request, DownlinkRequestCallback<WriteRequest, WriteResponse> callback) {
        try {
            LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
            if (resultIds.isObjectInstance() || resultIds.isResource()) {
                validateVersionedId(client, request);
                WriteRequest downlink = null;
                ContentFormat contentFormat = getWriteRequestContentFormat(client, request, modelProvider);
                String msgError = "";
                if (resultIds.isObjectInstance()) {
                    /*
                     *  params = "{\"id\":0,\"value\":[{\"id\":14,\"value\":\"+5\"},{\"id\":15,\"value\":\"+9\"}]}"
                     *  int rscId = resultIds.getObjectInstanceId();
                     *  contentFormat – Format of the payload (TLV or JSON).
                     */
                    Collection<LwM2mResource> resources = client.getNewResourcesForInstance(request.getVersionedId(),
                            request.getValue(), modelProvider, this.converter);
                    if (resources.size() > 0) {
                        downlink = new WriteRequest(WriteRequest.Mode.UPDATE, contentFormat, resultIds.getObjectId(),
                                resultIds.getObjectInstanceId(), resources);
                    } else {
                        msgError = " No resources to update!";
                    }
                } else if (resultIds.isResource()) {
                    ResourceModel resourceModelWrite = client.getResourceModel(request.getVersionedId(), modelProvider);
                    if (resourceModelWrite != null) {
                        if (resourceModelWrite.multiple) {
                            try {
                                Map<Integer, Object> value = convertMultiResourceValuesFromRpcBody(request.getValue(), resourceModelWrite.type, request.getObjectId());
                                downlink = new WriteRequest(WriteRequest.Mode.UPDATE, contentFormat, resultIds.getObjectId(),
                                        resultIds.getObjectInstanceId(), resultIds.getResourceId(),
                                        value, resourceModelWrite.type);
                            } catch (Exception e1) {
                                msgError = " Resource id=" + resultIds.toString() +
                                        ", class = " + request.getValue().getClass().getSimpleName() +
                                        ", value = " + request.getValue() + " is bad. " +
                                        "Value of Multi-Instance Resource must be in Json format!";
                            }
                        }
                    } else {
                        msgError = " Resource " + request.getVersionedId() + " is not configured in the device profile!";
                    }
                }
                if (downlink != null) {
                    sendSimpleRequest(client, downlink, request.getTimeout(), callback);
                } else {
                    callback.onValidationError(toString(request), "Resource " + request.getVersionedId() +
                            ". This operation can only be used for ObjectInstance or Multi-Instance Resource !" + msgError);
                }
            } else {
                callback.onValidationError(toString(request), "Resource " + request.getVersionedId() +
                        ". This operation can only be used for ObjectInstance or Resource (multiple)");
            }
        } catch (Exception e) {
            callback.onValidationError(toString(request), e.getMessage());
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    public void sendCreateRequest(LwM2mClient client, TbLwM2MCreateRequest request, DownlinkRequestCallback<CreateRequest, CreateResponse> callback) {
        validateVersionedId(client, request);
        CreateRequest downlink = null;
        LwM2mPath resultIds = new LwM2mPath(request.getObjectId());
        ObjectModel objectModel = client.getObjectModel(request.getVersionedId(), modelProvider);
        // POST /{Object ID}/{Object Instance ID} && Resources is Mandatory
        if (objectModel != null) {
            if (objectModel.multiple) {

                // LwM2M CBOR, SenML CBOR, SenML JSON, or TLV (see [LwM2M-CORE])
                ContentFormat contentFormat = getWriteRequestContentFormat(client, request, modelProvider);
                if (resultIds.isObject() || resultIds.isObjectInstance()) {
                    Collection<LwM2mResource> resources;
                    if (resultIds.isObject()) {
                        if (request.getValue() != null) {
                            resources = client.getNewResourcesForInstance(request.getVersionedId(), request.getValue(), modelProvider, this.converter);
                            downlink = new CreateRequest(contentFormat, resultIds.getObjectId(), resources);
                        } else if (request.getNodes() != null && request.getNodes().size() > 0) {
                            Set<LwM2mObjectInstance> instances = ConcurrentHashMap.newKeySet();
                            request.getNodes().forEach((key, value) -> {
                                Collection<LwM2mResource> resourcesForInstance = client.getNewResourcesForInstance(request.getVersionedId(), value, modelProvider, this.converter);
                                LwM2mObjectInstance instance = new LwM2mObjectInstance(Integer.parseInt(key), resourcesForInstance);
                                instances.add(instance);
                            });
                            LwM2mObjectInstance[] instanceArrays = instances.toArray(new LwM2mObjectInstance[instances.size()]);
                            downlink = new CreateRequest(contentFormat, resultIds.getObjectId(), instanceArrays);
                        }

                    } else {
                        resources = client.getNewResourcesForInstance(request.getVersionedId(), request.getValue(), modelProvider, this.converter);
                        LwM2mObjectInstance instance = new LwM2mObjectInstance(resultIds.getObjectInstanceId(), resources);
                        downlink = new CreateRequest(contentFormat, resultIds.getObjectId(), instance);
                    }
                }
                if (downlink != null) {
                    sendSimpleRequest(client, downlink, request.getTimeout(), callback);
                } else {
                    callback.onValidationError(toString(request), "Path " + request.getVersionedId() +
                            ". Object must be Multiple !");
                }
            } else {
                throw new IllegalArgumentException("Path " + request.getVersionedId() + ". Object must be Multiple !");
            }
        } else {
            callback.onValidationError(toString(request), "Resource " + request.getVersionedId() +
                    " is not configured in the device profile!");
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `timeoutInMs`：`timeoutInMs` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private <R extends SimpleDownlinkRequest<T>, T extends LwM2mResponse> void sendSimpleRequest(LwM2mClient client, R request, long timeoutInMs, DownlinkRequestCallback<R, T> callback) {
        sendRequest(client, request, timeoutInMs, callback, r -> request.getPath().toString());
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `timeoutInMs`：`timeoutInMs` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private <R extends CompositeDownlinkRequest<T>, T extends LwM2mResponse> void sendCompositeRequest(LwM2mClient client, R request, long timeoutInMs, DownlinkRequestCallback<R, T> callback) {
        sendRequest(client, request, timeoutInMs, callback, r -> request.getPaths().toString());
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `timeoutInMs`：`timeoutInMs` 参数。
     * - `callback`：处理完成后的回调。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private <R extends DownlinkRequest<T>, T extends LwM2mResponse> void sendRequest(LwM2mClient client, R request, long timeoutInMs, DownlinkRequestCallback<R, T> callback, Function<R, String> pathToStringFunction) {
        if (!clientContext.isDownlinkAllowed(client)) {
            log.trace("[{}] ignore downlink request cause client is sleeping.", client.getEndpoint());
            return;
        }
        Registration registration = client.getRegistration();
        try {
            logService.log(client, String.format("[%s][%s] Sending request: %s to %s", registration.getId(), registration.getSocketAddress(), request.getClass().getSimpleName(), pathToStringFunction.apply(request)));
            if (!callback.onSent(request)) {
                return;
            }

            context.getServer().send(registration, request, timeoutInMs, response -> {
                executor.submit(() -> {
                    try {
                        callback.onSuccess(request, response);
                    } catch (Exception e) {
                        log.error("[{}] failed to process successful response [{}] ", registration.getEndpoint(), response, e);
                    } finally {
                        clientContext.awake(client);
                    }
                });
            }, e -> handleDownlinkError(client, request, callback, e));
        } catch (Exception e) {
            handleDownlinkError(client, request, callback, e);
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `timeoutInMs`：`timeoutInMs` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private <R extends SimpleDownlinkRequest<T>, T extends LwM2mResponse> void sendWriteCompositeRequest(LwM2mClient client, WriteCompositeRequest request, long timeoutInMs, DownlinkRequestCallback<WriteCompositeRequest, WriteCompositeResponse> callback) {
        if (!clientContext.isDownlinkAllowed(client)) {
            log.trace("[{}] ignore downlink request cause client is sleeping.", client.getEndpoint());
            return;
        }
        Registration registration = client.getRegistration();
        try {
            logService.log(client, String.format("[%s][%s] Sending request: %s to %s", registration.getId(), registration.getSocketAddress(), request.getClass().getSimpleName(), request.getPaths()));
            context.getServer().send(registration, request, timeoutInMs, response -> {
                executor.submit(() -> {
                    try {
                        if (response.isSuccess()) {
                            callback.onSuccess(request, response);
                        } else {
                            callback.onValidationError(request.getNodes().values().toString(), response.getErrorMessage());
                        }
                    } catch (Exception e) {
                        log.error("[{}] failed to process successful response [{}] ", registration.getEndpoint(), response, e);
                    } finally {
                        clientContext.awake(client);
                    }
                });
            }, e -> handleDownlinkError(client, request, callback, e));
        } catch (Exception e) {
            handleDownlinkError(client, request, callback, e);
        }
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    private <R extends DownlinkRequest<T>, T extends LwM2mResponse> void handleDownlinkError(LwM2mClient client, R request, DownlinkRequestCallback<R, T> callback, Exception e) {
        log.trace("[{}] Received downlink error: {}.", client.getEndpoint(), e);
        try {
            client.updateLastUplinkTime();
            executor.submit(() -> {
                if (e instanceof TimeoutException || e instanceof ClientSleepingException) {
                    log.trace("[{}] Received {}, client is probably sleeping", client.getEndpoint(), e.getClass().getSimpleName());
                    clientContext.asleep(client);
                } else {
                    log.trace("[{}] Received {}", client.getEndpoint(), e.getClass().getSimpleName());
                }
                callback.onError(toString(request), e);
            });
        } catch (RejectedExecutionException ree) {
            log.warn("[{}] Can not handle downlink error. Executor already down", client.getEndpoint(), ree);
        } catch (Exception exception) {
            log.warn("[{}] Can not handle downlink error", client.getEndpoint(), exception);
        }
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `type`：类型。
     * - `contentFormat`：`contentFormat` 参数。
     * - `objectId`：`objectId`ID。
     * - `instanceId`：`instanceId`ID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private WriteRequest getWriteRequestSingleResource(ResourceModel.Type type, ContentFormat contentFormat, int objectId, int instanceId, int resourceId, Object value) {
        switch (type) {
            case STRING:    // String
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, value.toString());
            case INTEGER:   // Long
                final long valueInt = Integer.toUnsignedLong(Integer.parseInt(value.toString()));
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, valueInt);
            case OBJLNK:    // ObjectLink
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, ObjectLink.fromPath(value.toString()));
            case BOOLEAN:   // Boolean
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, Boolean.parseBoolean(value.toString()));
            case FLOAT:     // Double
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, Double.parseDouble(value.toString()));
            case TIME:      // Date
                Date date = new Date(Long.decode(value.toString()));
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, date);
            case OPAQUE:    // byte[] value, base64
                byte[] valueRequest;
                if (value instanceof byte[]) {
                    valueRequest = (byte[]) value;
                } else {
                    valueRequest = Hex.decodeHex(value.toString().toCharArray());
                }
                return new WriteRequest(contentFormat, objectId, instanceId, resourceId, valueRequest);
            default:
                throw new IllegalArgumentException("Not supported type:" + type.name());
        }
    }

    /**
     * 功能：保存或创建属性。
     * 参数：
     * - `attributes`：数据列表。
     * - `attributeName`：名称。
     * - `value`：值。
     * 返回：无。
     */
    private static <T> void addAttribute(List<Attribute> attributes, String attributeName, T value) {
        addAttribute(attributes, attributeName, value, null, null);
    }

    /**
     * 功能：保存或创建属性。
     * 参数：
     * - `attributes`：数据列表。
     * - `attributeName`：名称。
     * - `value`：值。
     * - `converter`：`converter` 参数。
     * 返回：无。
     */
    private static <T> void addAttribute(List<Attribute> attributes, String attributeName, T value, Function<T, ?> converter) {
        addAttribute(attributes, attributeName, value, null, converter);
    }

    /**
     * 功能：保存或创建属性。
     * 参数：
     * - `attributes`：数据列表。
     * - `attributeName`：名称。
     * - `value`：值。
     * - `filter`：`filter` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private static <T> void addAttribute(List<Attribute> attributes, String attributeName, T value, Predicate<T> filter, Function<T, ?> converter) {
        if (value != null && ((filter == null) || filter.test(value))) {
            attributes.add(new Attribute(attributeName, converter != null ? converter.apply(value) : value));
        }
    }

    /**
     * 功能：获取内容格式。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：处理结果。
     */
    private static <T extends HasContentFormat & HasVersionedId> ContentFormat getReadRequestContentFormat(LwM2mClient client, T request, LwM2mModelProvider modelProvider) {
        if (request.getRequestContentFormat().isPresent()) {
            return request.getRequestContentFormat().get();
        } else {
            return getRequestContentFormat(client, request.getVersionedId(), modelProvider);
        }
    }

    /**
     * 功能：获取内容格式。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：处理结果。
     */
    private static ContentFormat getWriteRequestContentFormat(LwM2mClient client, TbLwM2MDownlinkRequest request, LwM2mModelProvider modelProvider) {
        if (request instanceof TbLwM2MWriteReplaceRequest && ((TbLwM2MWriteReplaceRequest) request).getContentFormat() != null) {
            return ((TbLwM2MWriteReplaceRequest) request).getContentFormat();
        } else if (request instanceof TbLwM2MWriteUpdateRequest && ((TbLwM2MWriteUpdateRequest) request).getObjectContentFormat() != null) {
            return ((TbLwM2MWriteUpdateRequest) request).getObjectContentFormat();
        } else {
            String versionedId = null;
            if (request instanceof TbLwM2MWriteReplaceRequest) {
                versionedId = ((TbLwM2MWriteReplaceRequest) request).getVersionedId();
            } else if (request instanceof TbLwM2MWriteUpdateRequest) {
                versionedId = ((TbLwM2MWriteUpdateRequest) request).getVersionedId();
            } else if (request instanceof TbLwM2MCreateRequest) {
                versionedId = ((TbLwM2MCreateRequest) request).getVersionedId();
            }
            return getRequestContentFormat(client, versionedId, modelProvider);
        }
    }

    /**
     * 功能：获取内容格式。
     * 参数：
     * - `client`：客户端对象。
     * - `versionedId`：`versionedId`ID。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：处理结果。
     */
    private static ContentFormat getRequestContentFormat(LwM2mClient client, String versionedId, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(versionedId));
        if (pathIds.isResource() || pathIds.isResourceInstance()) {
            ResourceModel resourceModel = client.getResourceModel(versionedId, modelProvider);
            if (resourceModel != null && (pathIds.isResourceInstance() || (pathIds.isResource() && !resourceModel.multiple))) {
                if (OBJLNK.equals(resourceModel.type)) {
                    return ContentFormat.LINK;
                } else if (OPAQUE.equals(resourceModel.type)) {
                    return ContentFormat.OPAQUE;
                } else {
                    return findFirst(client.getClientSupportContentFormats(), client.getDefaultContentFormat(), ContentFormat.CBOR, ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON);
                }
            } else {
                return getContentFormatForComplex(client);
            }
        } else {
            return getContentFormatForComplex(client);
        }
    }

    /**
     * 功能：获取内容格式。
     * 参数：
     * - `client`：客户端对象。
     * 返回：处理结果。
     */
    private static ContentFormat getContentFormatForComplex(LwM2mClient client) {
        if (LwM2m.LwM2mVersion.V1_0.equals(client.getRegistration().getLwM2mVersion())) {
            return ContentFormat.TLV;
        } else if (LwM2m.LwM2mVersion.V1_1.equals(client.getRegistration().getLwM2mVersion())) {
            ContentFormat result = findFirst(client.getClientSupportContentFormats(), null, ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON, ContentFormat.TLV, ContentFormat.JSON);
            if (result != null) {
                return result;
            } else {
                throw new RuntimeException("The client does not support any of SenML CBOR, SenML JSON, TLV or JSON formats. Can't send complex requests. Try using singe-instance requests.");
            }
        } else {
            throw new RuntimeException("The version " + client.getRegistration().getLwM2mVersion() + " is not supported!");
        }
    }

    /**
     * 功能：获取`First`。
     * 参数：
     * - `supported`：`supported` 参数。
     * - `defaultValue`：值。
     * - `desiredFormats`：`desiredFormats` 参数。
     * 返回：处理结果。
     */
    private static ContentFormat findFirst(Set<ContentFormat> supported, ContentFormat defaultValue, ContentFormat... desiredFormats) {
        for (ContentFormat contentFormat : desiredFormats) {
            if (supported.contains(contentFormat)) {
                return contentFormat;
            }
        }
        return defaultValue;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    private <R> String toString(R request) {
        try {
            return request != null ? request.toString() : "";
        } catch (Exception e) {
            log.debug("Failed to convert request to string", e);
            return request.getClass().getSimpleName();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultLwM2mDownlinkMsgHandler` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
