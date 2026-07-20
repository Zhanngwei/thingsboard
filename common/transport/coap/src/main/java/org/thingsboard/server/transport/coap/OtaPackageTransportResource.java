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
 * 1. `OtaPackageTransportResource` 是 ThingsBoard Common Transport 中负责传输层接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `AbstractCoapTransportResource`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
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
     * 1. `OtaPackageCallback` 是 ThingsBoard Common Transport 中处理 OTA 的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `TransportServiceCallback`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
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
