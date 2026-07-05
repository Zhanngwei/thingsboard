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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceObserver;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_UPDATE_COAP_RESOURCE;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_UPDATE_COAP_RESOURCE;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mTransportCoapResource` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class LwM2mTransportCoapResource extends AbstractLwM2mTransportResource {
    private final ConcurrentMap<String, ObserveRelation> tokenToObserveRelationMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicInteger> tokenToObserveNotificationSeqMap = new ConcurrentHashMap<>();
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private final OtaPackageDataCache otaPackageDataCache;

    /**
     * 功能：创建 `LwM2mTransportCoapResource` 实例，并初始化必要字段。
     * 参数：
     * - `otaPackageDataCache`：待处理数据。
     * - `name`：名称。
     * 返回：新创建的对象实例。
     */
    public LwM2mTransportCoapResource(OtaPackageDataCache otaPackageDataCache, String name) {
        super(name);
        this.otaPackageDataCache = otaPackageDataCache;
        this.setObservable(true); // enable observing
        this.addObserver(new CoapResourceObserver());
    }


    /**
     * 功能：校验关系。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void checkObserveRelation(Exchange exchange, Response response) {
        String token = getTokenFromRequest(exchange.getRequest());
        final ObserveRelation relation = exchange.getRelation();
        if (relation == null || relation.isCanceled()) {
            return; // because request did not try to establish a relation
        }
        if (response.getCode().isSuccess()) {

            if (!relation.isEstablished()) {
                relation.setEstablished();
                addObserveRelation(relation);
            }
            AtomicInteger notificationCounter = tokenToObserveNotificationSeqMap.computeIfAbsent(token, s -> new AtomicInteger(0));
            response.getOptions().setObserve(notificationCounter.getAndIncrement());
        } // ObserveLayer takes care of the else case
    }


    /**
     * 功能：处理`Handle Get`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    @Override
    protected void processHandleGet(CoapExchange exchange) {
        log.debug("processHandleGet [{}]", exchange);
        List<String> uriPath = exchange.getRequestOptions().getUriPath();
        if (uriPath.size() >= 2 &&
                (FIRMWARE_UPDATE_COAP_RESOURCE.equals(uriPath.get(uriPath.size() - 2)) ||
                        SOFTWARE_UPDATE_COAP_RESOURCE.equals(uriPath.get(uriPath.size() - 2)))) {
            this.sendOtaData(exchange);
        }
    }

    /**
     * 功能：处理`Handle Post`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    @Override
    protected void processHandlePost(CoapExchange exchange) {
        log.debug("processHandlePost [{}]", exchange);
    }

    /**
     * Override the default behavior so that requests to sub resources (typically /{name}/{token}) are handled by
     * /name resource.
     */
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
     * 功能：获取请求。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    private String getTokenFromRequest(Request request) {
        return (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getAddress().getHostAddress() : "null")
                + ":" + (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getPort() : -1) + ":" + request.getTokenString();
    }

    /**
     * 中文说明：
     * 1. 类目的：`CoapResourceObserver` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    public class CoapResourceObserver implements ResourceObserver {

        /**
         * 功能：执行 `changedName` 对应的处理。
         * 参数：
         * - `old`：`old` 参数。
         * 返回：无。
         */
        @Override
        public void changedName(String old) {

        }

        /**
         * 功能：执行 `changedPath` 对应的处理。
         * 参数：
         * - `old`：`old` 参数。
         * 返回：无。
         */
        @Override
        public void changedPath(String old) {

        }

        /**
         * 功能：执行 `addedChild` 对应的处理。
         * 参数：
         * - `child`：`child` 参数。
         * 返回：无。
         */
        @Override
        public void addedChild(Resource child) {

        }

        /**
         * 功能：执行 `removedChild` 对应的处理。
         * 参数：
         * - `child`：`child` 参数。
         * 返回：无。
         */
        @Override
        public void removedChild(Resource child) {

        }

        /**
         * 功能：执行 `addedObserveRelation` 对应的处理。
         * 参数：
         * - `relation`：`relation` 参数。
         * 返回：无。
         */
        @Override
        public void addedObserveRelation(ObserveRelation relation) {

        }

        /**
         * 功能：执行 `removedObserveRelation` 对应的处理。
         * 参数：
         * - `relation`：`relation` 参数。
         * 返回：无。
         */
        @Override
        public void removedObserveRelation(ObserveRelation relation) {

        }
    }

    /**
     * 功能：发送或提交数据。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    private void sendOtaData(CoapExchange exchange) {
        String idStr = exchange.getRequestOptions().getUriPath().get(exchange.getRequestOptions().getUriPath().size() - 1
        );
        UUID currentId = UUID.fromString(idStr);
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        byte[] otaData = this.getOtaData(currentId);
        if (otaData != null && otaData.length > 0) {
            log.debug("Read ota data (length): [{}]", otaData.length);
            response.setPayload(otaData);
            if (exchange.getRequestOptions().getBlock2() != null) {
                int chunkSize = exchange.getRequestOptions().getBlock2().getSzx();
                boolean lastFlag = otaData.length <= chunkSize;
                response.getOptions().setBlock2(chunkSize, lastFlag, 0);
                log.trace("With block2 Send currentId: [{}], length: [{}], chunkSize [{}], moreFlag [{}]", currentId.toString(), otaData.length, chunkSize, lastFlag);
            } else {
                log.trace("With block1 Send currentId: [{}], length: [{}], ", currentId.toString(), otaData.length);
            }
            exchange.respond(response);
        } else {
            log.trace("Ota packaged currentId: [{}] is not found.", currentId.toString());
        }
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `currentId`：`currentId`ID。
     * 返回：处理结果。
     */
    private byte[] getOtaData(UUID currentId) {
        return otaPackageDataCache.get(currentId.toString());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mTransportCoapResource` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
