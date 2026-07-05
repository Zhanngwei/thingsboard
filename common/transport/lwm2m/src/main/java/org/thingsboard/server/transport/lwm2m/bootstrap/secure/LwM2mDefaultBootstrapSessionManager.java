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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapTaskProvider;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSession;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.model.LwM2mBootstrapModelProvider;
import org.eclipse.leshan.server.model.StandardBootstrapModelProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapConfigStoreTaskProvider;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapTaskProvider;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mDefaultBootstrapSessionManager` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class LwM2mDefaultBootstrapSessionManager extends DefaultBootstrapSessionManager {

    /**
     * 存储组件，表示当前对象的对应属性。
     */
    private final BootstrapSecurityStore bsSecurityStore;
    private final SecurityChecker securityChecker;
    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    private final LwM2MBootstrapTaskProvider tasksProvider;
    private final LwM2mBootstrapModelProvider modelProvider;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private TransportService transportService;

    /**
     * Create a {@link DefaultBootstrapSessionManager} using a default {@link SecurityChecker} to accept or refuse new
     * {@link BootstrapSession}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by default {@link SecurityChecker}.
     */
    /**
     * 功能：创建 `LwM2mDefaultBootstrapSessionManager` 实例，并初始化必要字段。
     * 参数：
     * - `bsSecurityStore`：`bsSecurityStore` 参数。
     * - `configStore`：配置对象。
     * - `transportService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, BootstrapConfigStore configStore, TransportService transportService) {
        this(bsSecurityStore, new SecurityChecker(), new LwM2MBootstrapConfigStoreTaskProvider(configStore),
                new StandardBootstrapModelProvider());
        this.transportService = transportService;
    }

    /**
     * Create a {@link DefaultBootstrapSessionManager}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by {@link SecurityChecker}.
     * @param securityChecker used to accept or refuse new {@link BootstrapSession}.
     */
    /**
     * 功能：创建 `LwM2mDefaultBootstrapSessionManager` 实例，并初始化必要字段。
     * 参数：
     * - `bsSecurityStore`：`bsSecurityStore` 参数。
     * - `securityChecker`：`securityChecker` 参数。
     * - `tasksProvider`：`tasksProvider` 参数。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：新创建的对象实例。
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker,
                                               LwM2MBootstrapTaskProvider tasksProvider, LwM2mBootstrapModelProvider modelProvider) {
        super(bsSecurityStore, securityChecker, tasksProvider, modelProvider);
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
        this.tasksProvider = tasksProvider;
        this.modelProvider = modelProvider;
    }

    /**
     * 功能：执行 `begin` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `clientIdentity`：客户端对象。
     * 返回：处理结果。
     */
    @Override
    public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity) {
        boolean authorized = true;
        Iterator<SecurityInfo> securityInfos = null;
        try {
            if (bsSecurityStore != null && securityChecker != null) {
                if (clientIdentity.isPSK()) {
                    SecurityInfo securityInfo = bsSecurityStore.getByIdentity(clientIdentity.getPskIdentity());
                    securityInfos = Collections.singletonList(securityInfo).iterator();
                } else if (!clientIdentity.isX509()) {
                    securityInfos = bsSecurityStore.getAllByEndpoint(request.getEndpointName());
                }
                authorized = this.checkSecurityInfo(request.getEndpointName(), clientIdentity, securityInfos);
            }
        } catch (LwM2MAuthException e) {
            authorized = false;
        }
        DefaultBootstrapSession session = new DefaultBootstrapSession(request, clientIdentity, authorized);
        if (authorized) {
            try {
                this.tasksProvider.put(session.getEndpoint());
            } catch (InvalidConfigurationException e){
                log.error("Failed put to lwM2MBootstrapSessionClients by endpoint [{}]", request.getEndpointName(), e);
            }
            this.sendLogs(request.getEndpointName(),
                    String.format("%s: Bootstrap session started...", LOG_LWM2M_INFO, request.getEndpointName()));
        }
        return session;
    }

    /**
     * 功能：判断配置。
     * 参数：
     * - `session`：会话对象。
     * 返回：判断结果。
     */
    @Override
    public boolean hasConfigFor(BootstrapSession session) {
        BootstrapTaskProvider.Tasks firstTasks = this.tasksProvider.getTasks(session, null);
        if (firstTasks == null) {
            return false;
        }
        initTasks(session, firstTasks);
        return true;
    }

    /**
     * 功能：初始化或启动`Tasks`。
     * 参数：
     * - `bssession`：会话对象。
     * - `tasks`：`tasks` 参数。
     * 返回：无。
     */
    protected void initTasks(BootstrapSession bssession, BootstrapTaskProvider.Tasks tasks) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bssession;
        // set models
        if (tasks.supportedObjects != null)
            session.setModel(modelProvider.getObjectModel(session, tasks.supportedObjects));

        // set Requests to Send
        log.info("tasks.requestsToSend = [{}]", tasks.requestsToSend);
        session.setRequests(tasks.requestsToSend);

        // prepare list where we will store Responses
        session.setResponses(new ArrayList<LwM2mResponse>(tasks.requestsToSend.size()));

        // is last Tasks ?
        session.setMoreTasks(!tasks.last);
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `bsSession`：会话对象。
     * 返回：处理结果。
     */
    @Override
    public BootstrapDownlinkRequest<? extends LwM2mResponse> getFirstRequest(BootstrapSession bsSession) {
        return nextRequest(bsSession);
    }

    /**
     * 功能：执行 `nextRequest` 对应的处理。
     * 参数：
     * - `bsSession`：会话对象。
     * 返回：处理结果。
     */
    protected BootstrapDownlinkRequest<? extends LwM2mResponse> nextRequest(BootstrapSession bsSession) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsToSend = session.getRequests();

        if (!requestsToSend.isEmpty()) {
            // get next requests
            return requestsToSend.remove(0);
        } else {
            if (session.hasMoreTasks()) {
                BootstrapTaskProvider.Tasks nextTasks = this.tasksProvider.getTasks(session, session.getResponses());
                if (nextTasks == null) {
                    session.setMoreTasks(false);
                    return new BootstrapFinishRequest();
                }

                initTasks(session, nextTasks);
                return nextRequest(bsSession);
            } else {
                return new BootstrapFinishRequest();
            }
        }
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `bsSession`：会话对象。
     * - `request`：请求对象。
     * - `response`：响应对象。
     * 返回：处理结果。
     */
    @Override
    public BootstrapPolicy onResponseSuccess(BootstrapSession bsSession,
                                             BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response) {
        if (!(request instanceof BootstrapFinishRequest)) {
            // store response
            DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
            session.getResponses().add(response);
            String msg = String.format("%s: receives success response for:  %s  %s %s", LOG_LWM2M_INFO,
                    request.getClass().getSimpleName(), request.getPath().toString(), response.toString());
            this.sendLogs(bsSession.getEndpoint(), msg);

            // on success for NOT bootstrap finish request we send next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on success for bootstrap finish request we stop the session
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: receives success response for bootstrap finish.", LOG_LWM2M_INFO));
            this.tasksProvider.remove(bsSession.getEndpoint());
            return BootstrapPolicy.finished();
        }
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `bsSession`：会话对象。
     * - `request`：请求对象。
     * - `response`：响应对象。
     * 返回：处理结果。
     */
    @Override
    public BootstrapPolicy onResponseError(BootstrapSession bsSession,
                                           BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response) {
        if (!(request instanceof BootstrapFinishRequest)) {
            // store response
            DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
            session.getResponses().add(response);
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: %s %s receives error response %s ", LOG_LWM2M_INFO,
                            request.getClass().getSimpleName(),
                            request.getPath().toString(), response.toString()));
            // on response error for NOT bootstrap finish request we continue any sending next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on response error for bootstrap finish request we stop the session
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: error response for request bootstrap finish. Stop the session: %s", LOG_LWM2M_ERROR, bsSession.toString()));
            this.tasksProvider.remove(bsSession.getEndpoint());
            return BootstrapPolicy.failed();
        }
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `bsSession`：会话对象。
     * - `request`：请求对象。
     * - `cause`：`cause` 参数。
     * 返回：处理结果。
     */
    @Override
    public BootstrapPolicy onRequestFailure(BootstrapSession bsSession,
                                            BootstrapDownlinkRequest<? extends LwM2mResponse> request, Throwable cause) {
        this.sendLogs(bsSession.getEndpoint(),
                String.format("%s: %s %s failed because of %s", LOG_LWM2M_ERROR, request.getClass().getSimpleName(),
                        request.getPath().toString(), cause.toString()));
        return BootstrapPolicy.failed();
    }

    /**
     * 功能：执行 `end` 对应的处理。
     * 参数：
     * - `bsSession`：会话对象。
     * 返回：无。
     */
    @Override
    public void end(BootstrapSession bsSession) {
        this.sendLogs(bsSession.getEndpoint(), String.format("%s: Bootstrap session finished.", LOG_LWM2M_INFO));
        this.tasksProvider.remove(bsSession.getEndpoint());
    }

    /**
     * 功能：执行 `failed` 对应的处理。
     * 参数：
     * - `bsSession`：会话对象。
     * - `cause`：`cause` 参数。
     * 返回：无。
     */
    @Override
    public void failed(BootstrapSession bsSession, BootstrapFailureCause cause) {
        this.sendLogs(bsSession.getEndpoint(), String.format("%s: Bootstrap session failed because of %s", LOG_LWM2M_ERROR,
                cause.toString()));
        this.tasksProvider.remove(bsSession.getEndpoint());
    }

    /**
     * 功能：发送或提交`Logs`。
     * 参数：
     * - `endpointName`：名称。
     * - `logMsg`：待处理消息。
     * 返回：无。
     */
    private void sendLogs(String endpointName, String logMsg) {
        log.info("Endpoint: [{}] [{}]", endpointName, logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(endpointName), logMsg);
    }

    /**
     * 功能：校验信息对象。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * - `clientIdentity`：客户端对象。
     * - `securityInfos`：`securityInfos` 参数。
     * 返回：判断结果。
     */
    private boolean checkSecurityInfo(String endpoint, Identity clientIdentity, Iterator<SecurityInfo> securityInfos) {
        if (clientIdentity.isX509()) {
            return clientIdentity.getX509CommonName().equals(endpoint)
                    & ((LwM2MBootstrapSecurityStore) bsSecurityStore).getBootstrapConfigByEndpoint(endpoint) != null;
        } else {
            return securityChecker.checkSecurityInfos(endpoint, clientIdentity, securityInfos);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mDefaultBootstrapSessionManager` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
