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
package org.thingsboard.server.common.data.limit;

import lombok.Getter;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.Optional;
import java.util.function.Function;

/**
 * 中文说明：
 * 1. `LimitedApi` 是 ThingsBoard Common Data 中定义 `Limited Api` 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum LimitedApi {

    ENTITY_EXPORT(DefaultTenantProfileConfiguration::getTenantEntityExportRateLimit, "entity version creation", true),
    ENTITY_IMPORT(DefaultTenantProfileConfiguration::getTenantEntityImportRateLimit, "entity version load", true),
    NOTIFICATION_REQUESTS(DefaultTenantProfileConfiguration::getTenantNotificationRequestsRateLimit, "notification requests", true),
    NOTIFICATION_REQUESTS_PER_RULE(DefaultTenantProfileConfiguration::getTenantNotificationRequestsPerRuleRateLimit, "notification requests per rule", false),
    REST_REQUESTS_PER_TENANT(DefaultTenantProfileConfiguration::getTenantServerRestLimitsConfiguration, "REST API requests", true),
    REST_REQUESTS_PER_CUSTOMER(DefaultTenantProfileConfiguration::getCustomerServerRestLimitsConfiguration, "REST API requests per customer", false),
    WS_UPDATES_PER_SESSION(DefaultTenantProfileConfiguration::getWsUpdatesPerSessionRateLimit, "WS updates per session", true),
    CASSANDRA_QUERIES(DefaultTenantProfileConfiguration::getCassandraQueryTenantRateLimitsConfiguration, "Cassandra queries", true),
    EDGE_EVENTS(DefaultTenantProfileConfiguration::getEdgeEventRateLimits, "Edge events", true),
    EDGE_EVENTS_PER_EDGE(DefaultTenantProfileConfiguration::getEdgeEventRateLimitsPerEdge, "Edge events per edge", false),
    EDGE_UPLINK_MESSAGES(DefaultTenantProfileConfiguration::getEdgeUplinkMessagesRateLimits, "Edge uplink messages", true),
    EDGE_UPLINK_MESSAGES_PER_EDGE(DefaultTenantProfileConfiguration::getEdgeUplinkMessagesRateLimitsPerEdge, "Edge uplink messages per edge", false),
    PASSWORD_RESET(false, true),
    TWO_FA_VERIFICATION_CODE_SEND(false, true),
    TWO_FA_VERIFICATION_CODE_CHECK(false, true),
    TRANSPORT_MESSAGES_PER_TENANT("transport messages", true),
    TRANSPORT_MESSAGES_PER_DEVICE("transport messages per device", false);

    /**
     * 配置，保存当前对象的配置选项。
     */
    private Function<DefaultTenantProfileConfiguration, String> configExtractor;
    /**
     * 是否满足租户条件。
     */
    @Getter
    private final boolean perTenant;
    /**
     * 是否满足数量限制条件。
     */
    @Getter
    private boolean refillRateLimitIntervally;
    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @Getter
    private String label;

    LimitedApi(Function<DefaultTenantProfileConfiguration, String> configExtractor, String label, boolean perTenant) {
        this.configExtractor = configExtractor;
        this.label = label;
        this.perTenant = perTenant;
    }

    LimitedApi(boolean perTenant, boolean refillRateLimitIntervally) {
        this.perTenant = perTenant;
        this.refillRateLimitIntervally = refillRateLimitIntervally;
    }

    LimitedApi(String label, boolean perTenant) {
        this.label = label;
        this.perTenant = perTenant;
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `profileConfiguration`：配置对象。
     * 返回：文本结果。
     */
    public String getLimitConfig(DefaultTenantProfileConfiguration profileConfiguration) {
        return Optional.ofNullable(configExtractor)
                .map(extractor -> extractor.apply(profileConfiguration))
                .orElse(null);
    }

}
