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
package org.thingsboard.server.coapserver;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.eclipse.californium.core.config.CoapConfig.DEFAULT_BLOCKWISE_STATUS_LIFETIME_IN_SECONDS;

/**
 * 中文说明：
 * 1. `DefaultCoapServerService` 是 ThingsBoard Common 中负责 CoAP 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `CoapServerService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Component
@TbCoapServerComponent
public class DefaultCoapServerService implements CoapServerService {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    private CoapServerContext coapServerContext;

    /**
     * 服务端，用于支撑当前网络或外部服务交互。
     */
    private CoapServer server;

    /**
     * 证书，用于认证或安全校验。
     */
    private TbCoapDtlsCertificateVerifier tbDtlsCertificateVerifier;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    private ScheduledExecutorService dtlsSessionsExecutor;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() throws UnknownHostException {
        createCoapServer();
    }

    /**
     * 功能：执行 `shutdown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void shutdown() {
        if (dtlsSessionsExecutor != null) {
            dtlsSessionsExecutor.shutdownNow();
        }
        log.info("Stopping CoAP server!");
        server.destroy();
        log.info("CoAP server stopped!");
    }

    /**
     * 功能：获取服务端。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CoapServer getCoapServer() throws UnknownHostException {
        if (server != null) {
            return server;
        } else {
            return createCoapServer();
        }
    }

    /**
     * 功能：获取`Dtls Sessions Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public ConcurrentMap<InetSocketAddress, TbCoapDtlsSessionInfo> getDtlsSessionsMap() {
        return tbDtlsCertificateVerifier != null ? tbDtlsCertificateVerifier.getTbCoapDtlsSessionsMap() : null;
    }

    /**
     * 功能：获取超时时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public long getTimeout() {
        return coapServerContext.getTimeout();
    }

    /**
     * 功能：获取超时时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public long getPiggybackTimeout() {
        return coapServerContext.getPiggybackTimeout();
    }

    /**
     * 功能：保存或创建服务端。
     * 参数：无。
     * 返回：处理结果。
     */
    private CoapServer createCoapServer() throws UnknownHostException {
        Configuration networkConfig = new Configuration();
        networkConfig.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, true);
        networkConfig.set(CoapConfig.BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER, true);
        networkConfig.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, DEFAULT_BLOCKWISE_STATUS_LIFETIME_IN_SECONDS, TimeUnit.SECONDS);
        networkConfig.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 256 * 1024 * 1024);
        networkConfig.set(CoapConfig.RESPONSE_MATCHING, CoapConfig.MatcherMode.RELAXED);
        networkConfig.set(CoapConfig.PREFERRED_BLOCK_SIZE, 1024);
        networkConfig.set(CoapConfig.MAX_MESSAGE_SIZE, 1024);
        networkConfig.set(CoapConfig.MAX_RETRANSMIT, 4);
        networkConfig.set(CoapConfig.COAP_PORT, coapServerContext.getPort());
        server = new CoapServer(networkConfig);

        CoapEndpoint.Builder noSecCoapEndpointBuilder = new CoapEndpoint.Builder();
        InetAddress addr = InetAddress.getByName(coapServerContext.getHost());
        InetSocketAddress sockAddr = new InetSocketAddress(addr, coapServerContext.getPort());
        noSecCoapEndpointBuilder.setInetSocketAddress(sockAddr);

        noSecCoapEndpointBuilder.setConfiguration(networkConfig);
        CoapEndpoint noSecCoapEndpoint = noSecCoapEndpointBuilder.build();
        server.addEndpoint(noSecCoapEndpoint);
        if (isDtlsEnabled()) {
            CoapEndpoint.Builder dtlsCoapEndpointBuilder = new CoapEndpoint.Builder();
            TbCoapDtlsSettings dtlsSettings = coapServerContext.getDtlsSettings();
            DtlsConnectorConfig dtlsConnectorConfig = dtlsSettings.dtlsConnectorConfig(networkConfig);
            networkConfig.set(CoapConfig.COAP_SECURE_PORT, dtlsConnectorConfig.getAddress().getPort());
            dtlsCoapEndpointBuilder.setConfiguration(networkConfig);
            DTLSConnector connector = new DTLSConnector(dtlsConnectorConfig);
            dtlsCoapEndpointBuilder.setConnector(connector);
            CoapEndpoint dtlsCoapEndpoint = dtlsCoapEndpointBuilder.build();
            server.addEndpoint(dtlsCoapEndpoint);
            tbDtlsCertificateVerifier = (TbCoapDtlsCertificateVerifier) dtlsConnectorConfig.getAdvancedCertificateVerifier();
            dtlsSessionsExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
            dtlsSessionsExecutor.scheduleAtFixedRate(this::evictTimeoutSessions, new Random().nextInt((int) getDtlsSessionReportTimeout()), getDtlsSessionReportTimeout(), TimeUnit.MILLISECONDS);
        }
        Resource root = server.getRoot();
        TbCoapServerMessageDeliverer messageDeliverer = new TbCoapServerMessageDeliverer(root);
        server.setMessageDeliverer(messageDeliverer);

        server.start();
        return server;
    }

    /**
     * 功能：判断`Dtls Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean isDtlsEnabled() {
        return coapServerContext.getDtlsSettings() != null;
    }

    /**
     * 功能：删除或清理超时时间。
     * 参数：无。
     * 返回：无。
     */
    private void evictTimeoutSessions() {
        tbDtlsCertificateVerifier.evictTimeoutSessions();
    }

    /**
     * 功能：获取会话。
     * 参数：无。
     * 返回：数值结果。
     */
    private long getDtlsSessionReportTimeout() {
        return tbDtlsCertificateVerifier.getDtlsSessionReportTimeout();
    }

}
