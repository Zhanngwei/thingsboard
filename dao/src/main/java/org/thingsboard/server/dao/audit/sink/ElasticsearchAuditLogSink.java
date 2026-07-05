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
package org.thingsboard.server.dao.audit.sink;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.TenantId;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. 类目的：`ElasticsearchAuditLogSink` 是 ThingsBoard DAO 模块 中的审计与事件持久化类型，用于记录用户操作、系统事件、实体事件和事件溯源数据，支持查询、清理和异步下沉。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括AuditLogService、EventService、HouseKeeper、DAO、队列和外部审计 Sink。
 * 4. 生命周期：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Observer / Event Sourcing / Repository。
 */
@Component
@ConditionalOnProperty(prefix = "audit-log.sink", value = "type", havingValue = "elasticsearch")
@Slf4j
public class ElasticsearchAuditLogSink implements AuditLogSink {

    /**
     * 租户常量，用于统一引用固定值。
     */
    private static final String TENANT_PLACEHOLDER = "@{TENANT}";
    private static final String DATE_PLACEHOLDER = "@{DATE}";
    /**
     * 类型常量，用于统一引用固定值。
     */
    private static final String INDEX_TYPE = "audit_log";

    /**
     * 索引，用于控制数量、位置或分页范围。
     */
    @Value("${audit-log.sink.index_pattern}")
    private String indexPattern;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @Value("${audit-log.sink.scheme_name}")
    private String schemeName;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Value("${audit-log.sink.host}")
    private String host;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Value("${audit-log.sink.port}")
    private int port;
    /**
     * 用户，用于标识或展示当前对象。
     */
    @Value("${audit-log.sink.user_name}")
    private String userName;
    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${audit-log.sink.password}")
    private String password;
    /**
     * `dateFormat` 字段，保存当前对象的对应属性。
     */
    @Value("${audit-log.sink.date_format}")
    private String dateFormat;

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private RestClient restClient;
    private ExecutorService executor;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        try {
            log.trace("Adding elastic rest endpoint... host [{}], port [{}], scheme name [{}]",
                    host, port, schemeName);
            RestClientBuilder builder = RestClient.builder(
                    new HttpHost(host, port, schemeName));

            if (StringUtils.isNotEmpty(userName) &&
                    StringUtils.isNotEmpty(password)) {
                log.trace("...using username [{}] and password ***", userName);
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(userName, password));
                builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            }

            this.restClient = builder.build();
            this.executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("elasticsearch-audit-log"));
        } catch (Exception e) {
            log.error("Sink init failed!", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：执行 `logAction` 对应的处理。
     * 参数：
     * - `auditLogEntry`：`auditLogEntry` 参数。
     * 返回：无。
     */
    @Override
    public void logAction(AuditLog auditLogEntry) {
        executor.execute(() -> {
            try {
                doLogAction(auditLogEntry);
            } catch (Exception e) {
                log.error("Failed to log action", e);
            }
        });
    }

    /**
     * 功能：执行 `doLogAction` 对应的处理。
     * 参数：
     * - `auditLogEntry`：`auditLogEntry` 参数。
     * 返回：无。
     */
    private void doLogAction(AuditLog auditLogEntry) {
        String jsonContent = createElasticJsonRecord(auditLogEntry);

        HttpEntity entity = new NStringEntity(
                jsonContent,
                ContentType.APPLICATION_JSON);

        restClient.performRequestAsync(
                HttpMethod.POST.name(),
                String.format("/%s/%s", getIndexName(auditLogEntry.getTenantId()), INDEX_TYPE),
                Collections.emptyMap(),
                entity,
                responseListener);
    }

    /**
     * 功能：保存或创建JSON。
     * 参数：
     * - `auditLog`：`auditLog` 参数。
     * 返回：文本结果。
     */
    private String createElasticJsonRecord(AuditLog auditLog) {
        ObjectNode auditLogNode = JacksonUtil.newObjectNode();
        auditLogNode.put("postDate", LocalDateTime.now().toString());
        auditLogNode.put("id", auditLog.getId().getId().toString());
        auditLogNode.put("entityName", auditLog.getEntityName());
        auditLogNode.put("tenantId", auditLog.getTenantId().getId().toString());
        if (auditLog.getCustomerId() != null) {
            auditLogNode.put("customerId", auditLog.getCustomerId().getId().toString());
        }
        auditLogNode.put("entityId", auditLog.getEntityId().getId().toString());
        auditLogNode.put("entityType", auditLog.getEntityId().getEntityType().name());
        auditLogNode.put("userId", auditLog.getUserId().getId().toString());
        auditLogNode.put("userName", auditLog.getUserName());
        auditLogNode.put("actionType", auditLog.getActionType().name());
        if (auditLog.getActionData() != null) {
            auditLogNode.put("actionData", auditLog.getActionData().toString());
        }
        auditLogNode.put("actionStatus", auditLog.getActionStatus().name());
        auditLogNode.put("actionFailureDetails", auditLog.getActionFailureDetails());
        return auditLogNode.toString();
    }

    private ResponseListener responseListener = new ResponseListener() {
        @Override
        public void onSuccess(Response response) {
            log.trace("Elasticsearch sink log action method succeeded. Response result [{}]!", response);
        }

        @Override
        public void onFailure(Exception exception) {
            log.warn("Elasticsearch sink log action method failed!", exception);
        }
    };

    /**
     * 功能：获取名称。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：文本结果。
     */
    private String getIndexName(TenantId tenantId) {
        String indexName = indexPattern;
        if (indexName.contains(TENANT_PLACEHOLDER) && tenantId != null) {
            indexName = indexName.replace(TENANT_PLACEHOLDER, tenantId.getId().toString());
        }
        if (indexName.contains(DATE_PLACEHOLDER)) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
            indexName = indexName.replace(DATE_PLACEHOLDER, now.format(formatter));
        }
        return indexName.toLowerCase();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ElasticsearchAuditLogSink` 在 ThingsBoard DAO 模块 中承担审计与事件持久化类型职责，核心目的是记录用户操作、系统事件、实体事件和事件溯源数据，支持查询、清理和异步下沉。
 * 2. 核心流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
 * 3. 关键依赖：主要依赖或协作对象包括AuditLogService、EventService、HouseKeeper、DAO、队列和外部审计 Sink。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
