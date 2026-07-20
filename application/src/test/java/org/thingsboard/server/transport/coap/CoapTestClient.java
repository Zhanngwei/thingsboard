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

import lombok.Getter;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.thingsboard.server.common.msg.session.FeatureType;

import java.io.IOException;

/**
 * 中文说明：
 * 1. `CoapTestClient` 是 ThingsBoard Application 中访问 CoAP 的客户端封装。
 * 2. 它把连接建立、请求发送、认证信息和响应解析集中到统一入口。
 * 3. 公开方法以平台数据模型作为输入输出，隐藏底层通信细节。
 * 4. 它直接协作于网络客户端、认证模型和请求响应对象。
 * 5. 独立客户端可以保持调用 API 稳定，并避免使用方重复处理连接与序列化。
 * 6. 阅读时重点关注连接配置、认证状态、请求构造和资源释放。
 */
public class CoapTestClient {

    /**
     * 基础访问地址常量，用于统一引用固定值。
     */
    private static final String COAP_BASE_URL = "coap://localhost:5683/api/v1/";
    private static final long CLIENT_REQUEST_TIMEOUT = 60000L;

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private final CoapClient client;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Getter
    private CoAP.Type type = CoAP.Type.CON;

    /**
     * 功能：创建 `CoapTestClient` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public CoapTestClient() {
        this.client = createClient();
    }

    /**
     * 功能：创建 `CoapTestClient` 实例，并初始化必要字段。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * - `featureType`：类型。
     * 返回：新创建的对象实例。
     */
    public CoapTestClient(String accessToken, FeatureType featureType) {
        this.client = createClient(getFeatureTokenUrl(accessToken, featureType));
    }

    /**
     * 功能：创建 `CoapTestClient` 实例，并初始化必要字段。
     * 参数：
     * - `featureTokenUrl`：`featureTokenUrl` 参数。
     * 返回：新创建的对象实例。
     */
    public CoapTestClient(String featureTokenUrl) {
        this.client = createClient(featureTokenUrl);
    }

    /**
     * 功能：执行 `connectToCoap` 对应的处理。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * 返回：无。
     */
    public void connectToCoap(String accessToken) {
        setURI(accessToken, null);
    }

    /**
     * 功能：执行 `connectToCoap` 对应的处理。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * - `featureType`：类型。
     * 返回：无。
     */
    public void connectToCoap(String accessToken, FeatureType featureType) {
        setURI(accessToken, featureType);
    }

    /**
     * 功能：执行 `disconnect` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void disconnect() {
        if (client != null) {
            client.shutdown();
        }
    }

    /**
     * 功能：执行 `postMethod` 对应的处理。
     * 参数：
     * - `requestBody`：请求对象。
     * 返回：处理结果。
     */
    public CoapResponse postMethod(String requestBody) throws ConnectorException, IOException {
        return this.postMethod(requestBody.getBytes());
    }

    /**
     * 功能：执行 `postMethod` 对应的处理。
     * 参数：
     * - `requestBodyBytes`：请求对象。
     * 返回：处理结果。
     */
    public CoapResponse postMethod(byte[] requestBodyBytes) throws ConnectorException, IOException {
        return client.setTimeout(CLIENT_REQUEST_TIMEOUT).post(requestBodyBytes, MediaTypeRegistry.APPLICATION_JSON);
    }

    /**
     * 功能：执行 `postMethod` 对应的处理。
     * 参数：
     * - `handler`：处理器对象。
     * - `payload`：`payload` 参数。
     * - `format`：`format` 参数。
     * 返回：无。
     */
    public void postMethod(CoapHandler handler, String payload, int format) {
        client.post(handler, payload, format);
    }

    /**
     * 功能：执行 `postMethod` 对应的处理。
     * 参数：
     * - `handler`：处理器对象。
     * - `payload`：`payload` 参数。
     * - `format`：`format` 参数。
     * 返回：无。
     */
    public void postMethod(CoapHandler handler, byte[] payload, int format) {
        client.post(handler, payload, format);
    }

    /**
     * 功能：获取`Method`。
     * 参数：无。
     * 返回：处理结果。
     */
    public CoapResponse getMethod() throws ConnectorException, IOException {
        return client.setTimeout(CLIENT_REQUEST_TIMEOUT).get();
    }

    /**
     * 功能：获取关系。
     * 参数：
     * - `callback`：处理完成后的回调。
     * 返回：处理结果。
     */
    public CoapObserveRelation getObserveRelation(CoapTestCallback callback) {
        return getObserveRelation(callback, true);
    }

    /**
     * 功能：获取关系。
     * 参数：
     * - `callback`：处理完成后的回调。
     * - `confirmable`：`confirmable` 参数。
     * 返回：处理结果。
     */
    public CoapObserveRelation getObserveRelation(CoapTestCallback callback, boolean confirmable) {
        Request request = Request.newGet().setObserve();
        request.setType(confirmable ? CoAP.Type.CON : CoAP.Type.NON);
        return client.observe(request, callback);
    }

    /**
     * 功能：更新URI 地址。
     * 参数：
     * - `featureTokenUrl`：`featureTokenUrl` 参数。
     * 返回：无。
     */
    public void setURI(String featureTokenUrl) {
        if (client == null) {
            throw new RuntimeException("Failed to connect! CoapClient is not initialized!");
        }
        client.setURI(featureTokenUrl);
    }

    /**
     * 功能：更新URI 地址。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * - `featureType`：类型。
     * 返回：无。
     */
    public void setURI(String accessToken, FeatureType featureType) {
        if (featureType == null) {
            featureType = FeatureType.ATTRIBUTES;
        }
        setURI(getFeatureTokenUrl(accessToken, featureType));
    }

    /**
     * 功能：执行 `useCONs` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void useCONs() {
        if (client == null) {
            throw new RuntimeException("Failed to connect! CoapClient is not initialized!");
        }
        type = CoAP.Type.CON;
        client.useCONs();
    }

    /**
     * 功能：执行 `useNONs` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void useNONs() {
        if (client == null) {
            throw new RuntimeException("Failed to connect! CoapClient is not initialized!");
        }
        type = CoAP.Type.NON;
        client.useNONs();
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    private CoapClient createClient() {
        return new CoapClient();
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `featureTokenUrl`：`featureTokenUrl` 参数。
     * 返回：处理结果。
     */
    private CoapClient createClient(String featureTokenUrl) {
        return new CoapClient(featureTokenUrl);
    }

    /**
     * 功能：获取URL 地址。
     * 参数：
     * - `token`：`token` 参数。
     * - `featureType`：类型。
     * 返回：文本结果。
     */
    public static String getFeatureTokenUrl(String token, FeatureType featureType) {
        return COAP_BASE_URL + token + "/" + featureType.name().toLowerCase();
    }

    /**
     * 功能：获取URL 地址。
     * 参数：
     * - `token`：`token` 参数。
     * - `featureType`：类型。
     * - `requestId`：请求ID。
     * 返回：文本结果。
     */
    public static String getFeatureTokenUrl(String token, FeatureType featureType, int requestId) {
        return COAP_BASE_URL + token + "/" + featureType.name().toLowerCase() + "/" + requestId;
    }
}
