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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.callback.CoapDeviceAuthCallback;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`OtaPackageTransportResource` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class OtaPackageTransportResource extends AbstractCoapTransportResource {
    /**
     * 令牌常量，用于统一引用固定值。
     */
    private static final int ACCESS_TOKEN_POSITION = 2;

    /**
     * 类型，用于区分不同处理分支。
     */
    private final OtaPackageType otaPackageType;

    /**
     * 功能：创建 `OtaPackageTransportResource` 实例，并初始化必要字段。
     * 参数：
     * - `ctx`：处理上下文。
     * - `otaPackageType`：类型。
     * 返回：新创建的对象实例。
     */
    public OtaPackageTransportResource(CoapTransportContext ctx, OtaPackageType otaPackageType) {
        super(ctx, otaPackageType.getKeyPrefix());
        this.otaPackageType = otaPackageType;

        this.setObservable(true);
    }

    /**
     * 功能：处理`Handle Get`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    @Override
    protected void processHandleGet(CoapExchange exchange) {
        log.trace("Processing {}", exchange.advanced().getRequest());
        exchange.accept();
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();
        processAccessTokenRequest(exchange, request);
    }

    /**
     * 功能：处理`Handle Post`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    @Override
    protected void processHandlePost(CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void processAccessTokenRequest(CoapExchange exchange, Request request) {
        Optional<DeviceTokenCredentials> credentials = decodeCredentials(request);
        if (credentials.isEmpty()) {
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
            return;
        }
        transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(credentials.get().getCredentialsId()).build(),
                new CoapDeviceAuthCallback(exchange, (msg, deviceProfile) -> {
                    getOtaPackageCallback(msg, exchange, otaPackageType);
                }));
    }

    /**
     * 功能：获取回调。
     * 参数：
     * - `msg`：待处理消息。
     * - `exchange`：`exchange` 参数。
     * - `firmwareType`：类型。
     * 返回：无。
     */
    private void getOtaPackageCallback(ValidateDeviceCredentialsResponse msg, CoapExchange exchange, OtaPackageType firmwareType) {
        TenantId tenantId = msg.getDeviceInfo().getTenantId();
        DeviceId deviceId = msg.getDeviceInfo().getDeviceId();
        TransportProtos.GetOtaPackageRequestMsg requestMsg = TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setType(firmwareType.name()).build();
        transportContext.getTransportService().process(SessionInfoCreator.create(msg, transportContext, UUID.randomUUID()), requestMsg, new OtaPackageCallback(exchange));
    }

    /**
     * 功能：解析凭据。
     * 参数：
     * - `request`：请求对象。
     * 返回：可能存在的结果。
     */
    private Optional<DeviceTokenCredentials> decodeCredentials(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        if (uriPath.size() == ACCESS_TOKEN_POSITION) {
            return Optional.of(new DeviceTokenCredentials(uriPath.get(ACCESS_TOKEN_POSITION - 1)));
        } else {
            return Optional.empty();
        }
    }

    /**
     * 功能：获取`Child`。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public Resource getChild(String name) {
        return this;
    }

    /**
     * 中文说明：
     * 1. 类目的：`OtaPackageCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private class OtaPackageCallback implements TransportServiceCallback<TransportProtos.GetOtaPackageResponseMsg> {
        /**
         * `exchange` 字段，保存当前对象的对应属性。
         */
        private final CoapExchange exchange;

        OtaPackageCallback(CoapExchange exchange) {
            this.exchange = exchange;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onSuccess(TransportProtos.GetOtaPackageResponseMsg msg) {
            String title = exchange.getQueryParameter("title");
            String version = exchange.getQueryParameter("version");
            if (msg.getResponseStatus().equals(TransportProtos.ResponseStatus.SUCCESS)) {
                String firmwareId = new UUID(msg.getOtaPackageIdMSB(), msg.getOtaPackageIdLSB()).toString();
                if ((title == null || msg.getTitle().equals(title)) && (version == null || msg.getVersion().equals(version))) {
                    String strChunkSize = exchange.getQueryParameter("size");
                    String strChunk = exchange.getQueryParameter("chunk");
                    int chunkSize = StringUtils.isEmpty(strChunkSize) ? 0 : Integer.parseInt(strChunkSize);
                    int chunk = StringUtils.isEmpty(strChunk) ? 0 : Integer.parseInt(strChunk);
                    respondOtaPackage(exchange, transportContext.getOtaPackageDataCache().get(firmwareId, chunkSize, chunk));
                } else {
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                }
            } else {
                exchange.respond(CoAP.ResponseCode.NOT_FOUND);
            }
        }

        /**
         * 功能：处理错误信息。
         * 参数：
         * - `e`：`e` 参数。
         * 返回：无。
         */
        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 功能：执行 `respondOtaPackage` 对应的处理。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * - `data`：待处理数据。
     * 返回：无。
     */
    private void respondOtaPackage(CoapExchange exchange, byte[] data) {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        if (data != null && data.length > 0) {
            response.setPayload(data);
            if (exchange.getRequestOptions().getBlock2() != null) {
                int chunkSize = exchange.getRequestOptions().getBlock2().getSzx();
                boolean lastFlag = data.length <= chunkSize;
                response.getOptions().setBlock2(chunkSize, lastFlag, 0);
            }
            transportContext.getExecutor().submit(() -> exchange.respond(response));
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`OtaPackageTransportResource` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
