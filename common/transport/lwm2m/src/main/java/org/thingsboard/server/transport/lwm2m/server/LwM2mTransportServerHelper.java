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
package org.thingsboard.server.transport.lwm2m.server;

import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.util.TbDDFFileParser;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.thingsboard.server.gen.transport.TransportProtos.KeyValueType.BOOLEAN_V;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mTransportServerHelper` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class LwM2mTransportServerHelper {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final LwM2mTransportContext context;
    private final static JsonParser JSON_PARSER = new JsonParser();

    /**
     * 功能：发送或提交属性。
     * 参数：
     * - `result`：数据列表。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    public void sendParametersOnThingsboardAttribute(List<TransportProtos.KeyValueProto> result, SessionInfoProto sessionInfo) {
        PostAttributeMsg.Builder request = PostAttributeMsg.newBuilder();
        request.addAllKv(result);
        PostAttributeMsg postAttributeMsg = request.build();
        context.getTransportService().process(sessionInfo, postAttributeMsg, TransportServiceCallback.EMPTY);
    }

    /**
     * 功能：发送或提交遥测。
     * 参数：
     * - `kvList`：数据列表。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    public void sendParametersOnThingsboardTelemetry(List<TransportProtos.KeyValueProto> kvList, SessionInfoProto sessionInfo) {
        sendParametersOnThingsboardTelemetry(kvList, sessionInfo, null);
    }

    /**
     * 功能：发送或提交遥测。
     * 参数：
     * - `kvList`：数据列表。
     * - `sessionInfo`：会话对象。
     * - `keyTsLatestMap`：键。
     * 返回：无。
     */
    public void sendParametersOnThingsboardTelemetry(List<TransportProtos.KeyValueProto> kvList, SessionInfoProto sessionInfo, @Nullable Map<String, AtomicLong> keyTsLatestMap) {
        TransportProtos.TsKvListProto tsKvList = toTsKvList(kvList, keyTsLatestMap);

        PostTelemetryMsg postTelemetryMsg = PostTelemetryMsg.newBuilder()
                .addTsKvList(tsKvList)
                .build();

        context.getTransportService().process(sessionInfo, postTelemetryMsg, TransportServiceCallback.EMPTY);
    }

    /**
     * 功能：执行 `toTsKvList` 对应的处理。
     * 参数：
     * - `kvList`：数据列表。
     * - `keyTsLatestMap`：键。
     * 返回：匹配的数据集合。
     */
    TransportProtos.TsKvListProto toTsKvList(List<TransportProtos.KeyValueProto> kvList, Map<String, AtomicLong> keyTsLatestMap) {
        return TransportProtos.TsKvListProto.newBuilder()
                .setTs(getTs(kvList, keyTsLatestMap))
                .addAllKv(kvList)
                .build();
    }

    /**
     * 功能：获取当前对象记录的时间戳。
     * 参数：
     * - `kvList`：数据列表。
     * - `keyTsLatestMap`：键。
     * 返回：数值结果。
     */
    long getTs(List<TransportProtos.KeyValueProto> kvList, Map<String, AtomicLong> keyTsLatestMap) {
        if (keyTsLatestMap == null || kvList == null || kvList.isEmpty()) {
            return getCurrentTimeMillis();
        }

        return getTsByKey(kvList.get(0).getKey(), keyTsLatestMap, getCurrentTimeMillis());
    }

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `key`：键。
     * - `keyTsLatestMap`：键。
     * - `tsNow`：`tsNow` 参数。
     * 返回：数值结果。
     */
    long getTsByKey(@Nonnull String key, @Nonnull Map<String, AtomicLong> keyTsLatestMap, final long tsNow) {
        AtomicLong tsLatestAtomic = keyTsLatestMap.putIfAbsent(key, new AtomicLong(tsNow));
        if (tsLatestAtomic == null) {
            return tsNow; // it is a first known timestamp for this key. return as the latest
        }

        return compareAndSwapOrIncrementTsAtomically(tsLatestAtomic, tsNow);
    }

    /**
     * This algorithm is sensitive to wall-clock time shift.
     * Once time have shifted *backward*, the latest ts never came back.
     * Ts latest will be incremented until current time overtake the latest ts.
     * In normal environment without race conditions method will return current ts (wall-clock)
     * */
    /**
     * 功能：执行 `compareAndSwapOrIncrementTsAtomically` 对应的处理。
     * 参数：
     * - `tsLatestAtomic`：`tsLatestAtomic` 参数。
     * - `tsNow`：`tsNow` 参数。
     * 返回：数值结果。
     */
    long compareAndSwapOrIncrementTsAtomically(AtomicLong tsLatestAtomic, final long tsNow) {
        long tsLatest;
        while ((tsLatest = tsLatestAtomic.get()) < tsNow) {
            if (tsLatestAtomic.compareAndSet(tsLatest, tsNow)) {
                return tsNow; //swap successful
            }
        }
        return tsLatestAtomic.incrementAndGet(); //return next ms
    }

    /**
     * For the test ability to mock system timer
     * */
    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：数值结果。
     */
    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * @return - sessionInfo after access connect client
     */
    /**
     * 功能：获取会话。
     * 参数：
     * - `msg`：待处理消息。
     * - `mostSignificantBits`：`mostSignificantBits` 参数。
     * - `leastSignificantBits`：`leastSignificantBits` 参数。
     * 返回：处理结果。
     */
    public SessionInfoProto getValidateSessionInfo(ValidateDeviceCredentialsResponse msg, long mostSignificantBits, long leastSignificantBits) {
        return SessionInfoProto.newBuilder()
                .setNodeId(context.getNodeId())
                .setSessionIdMSB(mostSignificantBits)
                .setSessionIdLSB(leastSignificantBits)
                .setDeviceIdMSB(msg.getDeviceInfo().getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceInfo().getDeviceId().getId().getLeastSignificantBits())
                .setTenantIdMSB(msg.getDeviceInfo().getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getDeviceInfo().getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(msg.getDeviceInfo().getCustomerId().getId().getMostSignificantBits())
                .setCustomerIdLSB(msg.getDeviceInfo().getCustomerId().getId().getLeastSignificantBits())
                .setDeviceName(msg.getDeviceInfo().getDeviceName())
                .setDeviceType(msg.getDeviceInfo().getDeviceType())
                .setDeviceProfileIdMSB(msg.getDeviceInfo().getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(msg.getDeviceInfo().getDeviceProfileId().getId().getLeastSignificantBits())
                .build();
    }

    /**
     * 功能：解析`From Xml To Object Model`。
     * 参数：
     * - `xmlByte`：`xmlByte` 参数。
     * - `streamName`：名称。
     * 返回：处理结果。
     */
    public ObjectModel parseFromXmlToObjectModel(byte[] xmlByte, String streamName) {
        try {
            TbDDFFileParser ddfFileParser = new TbDDFFileParser();
            return ddfFileParser.parse(new ByteArrayInputStream(xmlByte), streamName).get(0);
        } catch (IOException | InvalidDDFFileException e) {
            log.error("Could not parse the XML file [{}]", streamName, e);
            return null;
        }
    }

    /**
     * @param value - info about Logs
     * @return- KeyValueProto for telemetry (Logs)
     */
    /**
     * 功能：获取`Kv Stringto Thingsboard`。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：匹配的数据集合。
     */
    public List<TransportProtos.KeyValueProto> getKvStringtoThingsboard(String key, String value) {
        List<TransportProtos.KeyValueProto> result = new ArrayList<>();
        value = value.replaceAll("<", "").replaceAll(">", "");
        result.add(TransportProtos.KeyValueProto.newBuilder()
                .setKey(key)
                .setType(TransportProtos.KeyValueType.STRING_V)
                .setStringV(value).build());
        return result;
    }

    /**
     * @return - KeyValueProto for attribute/telemetry (change value)
     * @throws CodecException -
     */

    /**
     * 功能：获取遥测。
     * 参数：
     * - `resourceType`：类型。
     * - `resourceName`：名称。
     * - `value`：值。
     * - `isMultiInstances`：`isMultiInstances` 参数。
     * 返回：处理结果。
     */
    public TransportProtos.KeyValueProto getKvAttrTelemetryToThingsboard(ResourceModel.Type resourceType, String resourceName, Object value, boolean isMultiInstances) {
        TransportProtos.KeyValueProto.Builder kvProto = TransportProtos.KeyValueProto.newBuilder().setKey(resourceName);
        if (isMultiInstances) {
            kvProto.setType(TransportProtos.KeyValueType.JSON_V)
                    .setJsonV((String) value);
        } else {
            switch (resourceType) {
                case BOOLEAN:
                    kvProto.setType(BOOLEAN_V).setBoolV((Boolean) value).build();
                    break;
                case STRING:
                case TIME:
                case OPAQUE:
                case OBJLNK:
                    kvProto.setType(TransportProtos.KeyValueType.STRING_V).setStringV((String) value);
                    break;
                case INTEGER:
                    kvProto.setType(TransportProtos.KeyValueType.LONG_V).setLongV((Long) value);
                    break;
                case FLOAT:
                    kvProto.setType(TransportProtos.KeyValueType.DOUBLE_V).setDoubleV((Double) value);
            }
        }
        return kvProto.build();
    }

    /**
     * @param currentType  -
     * @param resourcePath -
     * @return
     */
    /**
     * 功能：获取值。
     * 参数：
     * - `currentType`：类型。
     * - `resourcePath`：文件或资源路径。
     * 返回：处理结果。
     */
    public static ResourceModel.Type getResourceModelTypeEqualsKvProtoValueType(ResourceModel.Type currentType, String resourcePath) {
        switch (currentType) {
            case BOOLEAN:
                return ResourceModel.Type.BOOLEAN;
            case STRING:
            case TIME:
            case OPAQUE:
            case OBJLNK:
                return ResourceModel.Type.STRING;
            case INTEGER:
                return ResourceModel.Type.INTEGER;
            case FLOAT:
                return ResourceModel.Type.FLOAT;
            default:
        }
        throw new CodecException("Invalid ResourceModel_Type for resource %s, got %s", resourcePath, currentType);
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `kv`：`kv` 参数。
     * 返回：处理结果。
     */
    public static Object getValueFromKvProto(TransportProtos.KeyValueProto kv) {
        switch (kv.getType()) {
            case BOOLEAN_V:
                return kv.getBoolV();
            case LONG_V:
                return kv.getLongV();
            case DOUBLE_V:
                return kv.getDoubleV();
            case STRING_V:
                return kv.getStringV();
            case JSON_V:
                try {
                    return JSON_PARSER.parse(kv.getJsonV());
                } catch (Exception e) {
                    return null;
                }
        }
        return null;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mTransportServerHelper` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
