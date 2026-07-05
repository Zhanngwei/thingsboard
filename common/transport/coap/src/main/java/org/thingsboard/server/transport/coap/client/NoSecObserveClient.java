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
package org.thingsboard.server.transport.coap.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. 类目的：`NoSecObserveClient` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class NoSecObserveClient {

    /**
     * `INFINIT_EXCHANGE_LIFETIME`常量，用于统一引用固定值。
     */
    private static final long INFINIT_EXCHANGE_LIFETIME = 0L;

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private CoapClient coapClient;
    private CoapObserveRelation observeRelation;
    private ExecutorService executor = Executors.newFixedThreadPool(1, ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
    /**
     * 等待器，用于在测试或异步流程中等待结果。
     */
    private CountDownLatch latch;

    /**
     * 功能：创建 `NoSecObserveClient` 实例，并初始化必要字段。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `accessToken`：`accessToken` 参数。
     * 返回：新创建的对象实例。
     */
    public NoSecObserveClient(String host, int port, String accessToken) throws URISyntaxException {
        URI uri = new URI(getFutureUrl(host, port, accessToken));
        this.coapClient = new CoapClient(uri);
        coapClient.setTimeout(INFINIT_EXCHANGE_LIFETIME);
        this.latch = new CountDownLatch(5);
    }

    /**
     * 功能：执行 `start` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void start() {
        executor.submit(() -> {
            try {
                Request request = Request.newGet();
                request.setObserve();
                observeRelation = coapClient.observe(request, new CoapHandler() {
                    @Override
                    public void onLoad(CoapResponse response) {
                        String responseText = response.getResponseText();
                        CoAP.ResponseCode code = response.getCode();
                        Integer observe = response.getOptions().getObserve();
                        log.info("CoAP Response received! " +
                                        "responseText: {}, " +
                                        "code: {}, " +
                                        "observe seq number: {}",
                                responseText,
                                code,
                                observe);
                        latch.countDown();
                    }

                    @Override
                    public void onError() {
                        log.error("Ack error!");
                        latch.countDown();
                    }
                });
            } catch (Exception e) {
                log.error("Error occurred while sending COAP requests: ");
            }
        });
        try {
            latch.await();
            observeRelation.proactiveCancel();
        } catch (InterruptedException e) {
            log.error("Error occurred: ", e);
        }
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `accessToken`：`accessToken` 参数。
     * 返回：文本结果。
     */
    private String getFutureUrl(String host, Integer port, String accessToken) {
        return "coap://" + host + ":" + port + "/api/v1/" + accessToken + "/attributes";
    }

    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) throws URISyntaxException {
        log.info("Usage: java -cp ... org.thingsboard.server.transport.coap.client.NoSecObserveClient " +
                "host port accessToken");

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String accessToken = args[2];

        final NoSecObserveClient client = new NoSecObserveClient(host, port, accessToken);
        client.start();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`NoSecObserveClient` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
