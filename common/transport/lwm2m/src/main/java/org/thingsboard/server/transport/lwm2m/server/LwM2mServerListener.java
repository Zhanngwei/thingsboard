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
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.send.SendListener;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.util.Collection;
import java.util.Map;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mServerListener` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class LwM2mServerListener {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final LwM2mUplinkMsgHandler service;

    /**
     * 功能：创建 `LwM2mServerListener` 实例，并初始化必要字段。
     * 参数：
     * - `service`：服务对象。
     * 返回：新创建的对象实例。
     */
    public LwM2mServerListener(LwM2mUplinkMsgHandler service) {
        this.service = service;
    }

    public final RegistrationListener registrationListener = new RegistrationListener() {
        /**
         * Register – query represented as POST /rd?…
         */
        @Override
        public void registered(Registration registration, Registration previousReg,
                               Collection<Observation> previousObservations) {
            log.debug("Client: registered: [{}]", registration.getEndpoint());
            service.onRegistered(registration, previousObservations);
        }

        /**
         * Update – query represented as CoAP POST request for the URL received in response to Register.
         */
        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                            Registration previousRegistration) {
            service.updatedReg(updatedRegistration);
        }

        /**
         * De-register (CoAP DELETE) – Sent by the client when a shutdown procedure is initiated.
         */
        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                                 Registration newReg) {
            service.unReg(registration, observations);
        }

    };

    public final PresenceListener presenceListener = new PresenceListener() {
        @Override
        public void onSleeping(Registration registration) {
            log.info("[{}] onSleeping", registration.getEndpoint());
            service.onSleepingDev(registration);
        }

        @Override
        public void onAwake(Registration registration) {
            log.info("[{}] onAwake", registration.getEndpoint());
            service.onAwakeDev(registration);
        }
    };

    public final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void cancelled(Observation observation) {
            //TODO: should be able to use CompositeObservation
            log.trace("Canceled Observation {}.", ((SingleObservation)observation).getPath());
        }

        @Override
        public void onResponse(SingleObservation observation, Registration registration, ObserveResponse response) {
            if (registration != null) {
                service.onUpdateValueAfterReadResponse(registration, convertObjectIdToVersionedId(observation.getPath().toString(), registration), response);
            }
        }

        @Override
        public void onResponse(CompositeObservation observation, Registration registration, ObserveCompositeResponse response) {
            throw new RuntimeException("Not implemented yet!");
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            if (error != null) {
                //TODO: should be able to use CompositeObservation
                log.debug("Unable to handle notification of [{}:{}] [{}]", observation.getRegistrationId(), ((SingleObservation)observation).getPath(), error.getMessage());
            }
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
            //TODO: should be able to use CompositeObservation
            log.trace("Successful start newObservation {}.", ((SingleObservation)observation).getPath());
        }
    };

    public final SendListener sendListener = new SendListener() {

        @Override
        public void dataReceived(Registration registration, Map<String, LwM2mNode> map, SendRequest sendRequest) {
            if (registration != null) {
                service.onUpdateValueWithSendRequest(registration, sendRequest);
            }
        }
    };
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mServerListener` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
