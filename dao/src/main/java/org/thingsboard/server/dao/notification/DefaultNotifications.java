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
package org.thingsboard.server.dao.notification;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.EscalatedNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmAssignmentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmCommentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig.AlarmAction;
import org.thingsboard.server.common.data.notification.rule.trigger.config.ApiUsageLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig.DeviceEvent;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeCommunicationFailureNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeConnectionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeConnectionNotificationRuleTriggerConfig.EdgeConnectivityEvent;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntitiesLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NewPlatformVersionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.RateLimitsNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static org.thingsboard.common.util.JacksonUtil.newObjectNode;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;

@Service
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DefaultNotifications` 是 ThingsBoard DAO 模块 中的通知持久化服务类型，用于管理通知模板、规则、目标、请求、设置和用户通知状态的持久化访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括NotificationService、UserService、TenantService、Scheduler、缓存和审计服务。
 * 4. 生命周期：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Scheduler Command。
 */
public class DefaultNotifications {

    /**
     * 字段说明：
     * 1. 保存 `YELLOW_COLOR` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private static final String YELLOW_COLOR = "#F9D916";
    private static final String RED_COLOR = "#e91a1a";

    public static final DefaultNotification maintenanceWork = DefaultNotification.builder()
            .name("Maintenance work notification")
            .subject("Infrastructure maintenance")
            .text("Maintenance work is scheduled for tomorrow (7:00 a.m. - 9:00 a.m. UTC)")
            .build();

    public static final DefaultNotification entitiesLimitForSysadmin = DefaultNotification.builder()
            .name("Entities count limit notification for sysadmin")
            .type(NotificationType.ENTITIES_LIMIT)
            .subject("${entityType}s limit will be reached soon for tenant ${tenantName}")
            .text("${entityType}s usage: ${currentCount}/${limit} (${percents}%)")
            .icon("warning").color(YELLOW_COLOR)
            .rule(DefaultRule.builder()
                    .name("Entities count limit (sysadmin)")
                    .triggerConfig(EntitiesLimitNotificationRuleTriggerConfig.builder()
                            .entityTypes(null).threshold(0.8f)
                            .build())
                    .description("Send notification to system admins when count of entities of some type reached 80% threshold of the limit for a tenant")
                    .build())
            .build();
    public static final DefaultNotification entitiesLimitForTenant = entitiesLimitForSysadmin.toBuilder()
            .name("Entities count limit notification for tenant")
            .subject("WARNING: ${entityType}s limit will be reached soon")
            .rule(entitiesLimitForSysadmin.getRule().toBuilder()
                    .name("Entities count limit")
                    .description("Send notification to tenant admins when count of entities of some type reached 80% threshold of the limit")
                    .build())
            .build();

    public static final DefaultNotification apiFeatureWarningForSysadmin = DefaultNotification.builder()
            .name("API feature warning notification for sysadmin")
            .type(NotificationType.API_USAGE_LIMIT)
            .subject("${feature} feature will be disabled soon for tenant ${tenantName}")
            .text("Usage: ${currentValue} out of ${limit} ${unitLabel}s")
            .icon("warning").color(YELLOW_COLOR)
            .rule(DefaultRule.builder()
                    .name("API feature warning (sysadmin)")
                    .triggerConfig(ApiUsageLimitNotificationRuleTriggerConfig.builder()
                            .apiFeatures(null)
                            .notifyOn(Set.of(ApiUsageStateValue.WARNING))
                            .build())
                    .description("Send notification to system admins on API feature usage WARNING state for a tenant")
                    .build())
            .build();
    public static final DefaultNotification apiFeatureWarningForTenant = apiFeatureWarningForSysadmin.toBuilder()
            .name("API feature warning notification for tenant")
            .subject("WARNING: ${feature} feature will be disabled soon")
            .rule(apiFeatureWarningForSysadmin.getRule().toBuilder()
                    .name("API feature warning")
                    .description("Send notification to tenant admins on API feature usage WARNING state")
                    .build())
            .build();
    public static final DefaultNotification apiFeatureDisabledForSysadmin = DefaultNotification.builder()
            .name("API feature disabled notification for sysadmin")
            .type(NotificationType.API_USAGE_LIMIT)
            .subject("${feature} feature was disabled for tenant ${tenantName}")
            .text("Used ${currentValue} out of ${limit} ${unitLabel}s")
            .icon("block").color(RED_COLOR)
            .rule(DefaultRule.builder()
                    .name("API feature disabled (sysadmin)")
                    .triggerConfig(ApiUsageLimitNotificationRuleTriggerConfig.builder()
                            .apiFeatures(null)
                            .notifyOn(Set.of(ApiUsageStateValue.DISABLED))
                            .build())
                    .description("Send notification to system admins when API feature is disabled for a tenant")
                    .build())
            .build();
    public static final DefaultNotification apiFeatureDisabledForTenant = apiFeatureDisabledForSysadmin.toBuilder()
            .name("API feature disabled notification for tenant")
            .subject("${feature} feature was disabled")
            .rule(apiFeatureDisabledForSysadmin.getRule().toBuilder()
                    .name("API feature disabled")
                    .description("Send notification to tenant admins when API feature is disabled")
                    .build())
            .build();

    public static final DefaultNotification exceededRateLimits = DefaultNotification.builder()
            .name("Exceeded per-tenant rate limits notification for tenant")
            .type(NotificationType.RATE_LIMITS)
            .subject("Rate limits exceeded")
            .text("Rate limits for ${api} exceeded")
            .icon("block").color(RED_COLOR)
            .rule(DefaultRule.builder()
                    .name("Per-tenant rate limits exceeded")
                    .triggerConfig(RateLimitsNotificationRuleTriggerConfig.builder()
                            .apis(Arrays.stream(LimitedApi.values())
                                    .filter(LimitedApi::isPerTenant)
                                    .filter(api -> api.getLabel() != null)
                                    .collect(Collectors.toSet()))
                            .build())
                    .description("Send notification to tenant admins when some per-tenant rate limit is exceeded")
                    .build())
            .build();
    public static final DefaultNotification exceededPerEntityRateLimits = DefaultNotification.builder()
            .name("Exceeded per-entity rate limits notification for tenant")
            .type(NotificationType.RATE_LIMITS)
            .subject("Rate limits exceeded")
            .text("Rate limits for ${api} exceeded for '${limitLevelEntityName}'")
            .icon("block").color(RED_COLOR)
            .rule(DefaultRule.builder()
                    .name("Per-entity rate limits exceeded")
                    .triggerConfig(RateLimitsNotificationRuleTriggerConfig.builder()
                            .apis(Arrays.stream(LimitedApi.values())
                                    .filter(not(LimitedApi::isPerTenant))
                                    .filter(api -> api.getLabel() != null)
                                    .collect(Collectors.toSet()))
                            .build())
                    .description("Send notification to tenant admins when some per-entity rate limit is exceeded for an entity")
                    .build())
            .build();
    public static final DefaultNotification exceededRateLimitsForSysadmin = exceededRateLimits.toBuilder()
            .name("Exceeded per-tenant rate limits notification for sysadmin")
            .subject("Rate limits exceeded for tenant ${tenantName}")
            .button("Go to tenant").link("/tenants/${tenantId}")
            .rule(exceededRateLimits.getRule().toBuilder()
                    .name("Per-tenant rate limits exceeded (sysadmin)")
                    .description("Send notification to system admins when a tenant exceeds some per-tenant rate limit")
                    .build())
            .build();

    public static final DefaultNotification newPlatformVersion = DefaultNotification.builder()
            .name("New platform version notification")
            .type(NotificationType.NEW_PLATFORM_VERSION)
            .subject("New version <b>${latestVersion}</b> is available")
            .text("Current platform version is ${currentVersion}")
            .button("Open release notes").link("${latestVersionReleaseNotesUrl}")
            .rule(DefaultRule.builder()
                    .name("New platform version")
                    .triggerConfig(new NewPlatformVersionNotificationRuleTriggerConfig())
                    .description("Send notification to system admins when new platform version is available")
                    .build())
            .build();

    public static final DefaultNotification newAlarm = DefaultNotification.builder()
            .name("New alarm notification")
            .type(NotificationType.ALARM)
            .subject("New alarm '${alarmType}'")
            .text("Severity: ${alarmSeverity}, originator: ${alarmOriginatorEntityType} '${alarmOriginatorName}'")
            .icon("notifications").color(null)
            .rule(DefaultRule.builder()
                    .name("New alarm")
                    .triggerConfig(AlarmNotificationRuleTriggerConfig.builder()
                            .alarmTypes(null)
                            .alarmSeverities(null)
                            .notifyOn(Set.of(AlarmAction.CREATED))
                            .build())
                    .description("Send notification to tenant admins when an alarm is created")
                    .build())
            .build();
    public static final DefaultNotification alarmUpdate = DefaultNotification.builder()
            .name("Alarm update notification")
            .type(NotificationType.ALARM)
            .subject("Alarm '${alarmType}' - ${action}")
            .text("Severity: ${alarmSeverity}, originator: ${alarmOriginatorEntityType} '${alarmOriginatorName}'")
            .icon("notifications").color(null)
            .rule(DefaultRule.builder()
                    .name("Alarm update")
                    .triggerConfig(AlarmNotificationRuleTriggerConfig.builder()
                            .alarmTypes(null)
                            .alarmSeverities(null)
                            .notifyOn(Set.of(AlarmAction.SEVERITY_CHANGED, AlarmAction.ACKNOWLEDGED, AlarmAction.CLEARED))
                            .build())
                    .description("Send notification to tenant admins when any alarm is updated or cleared")
                    .build())
            .build();
    public static final DefaultNotification entityAction = DefaultNotification.builder()
            .name("Entity action notification")
            .type(NotificationType.ENTITY_ACTION)
            .subject("${entityType} was ${actionType}")
            .text("${entityType} '${entityName}' was ${actionType} by user ${userEmail}")
            .icon("info").color(null)
            .button("Go to ${entityType:lowerCase}").link("/${entityType:lowerCase}s/${entityId}")
            .rule(DefaultRule.builder()
                    .name("Device created")
                    .triggerConfig(EntityActionNotificationRuleTriggerConfig.builder()
                            .entityTypes(Set.of(EntityType.DEVICE))
                            .created(true)
                            .updated(false)
                            .deleted(false)
                            .build())
                    .description("Send notification to tenant admins when device is created")
                    .build())
            .build();
    public static final DefaultNotification deviceActivity = DefaultNotification.builder()
            .name("Device activity notification")
            .type(NotificationType.DEVICE_ACTIVITY)
            .subject("Device '${deviceName}' became ${eventType}")
            .text("Device '${deviceName}' of type '${deviceType}' is now ${eventType}")
            .icon("info").color(null)
            .button("Go to device").link("/devices/${deviceId}")
            .rule(DefaultRule.builder()
                    .name("Device activity status change")
                    .enabled(false)
                    .triggerConfig(DeviceActivityNotificationRuleTriggerConfig.builder()
                            .devices(null)
                            .deviceProfiles(null)
                            .notifyOn(Set.of(DeviceEvent.ACTIVE, DeviceEvent.INACTIVE))
                            .build())
                    .description("Send notification to tenant admins when any device changes its activity state")
                    .build())
            .build();
    public static final DefaultNotification alarmComment = DefaultNotification.builder()
            .name("Alarm comment notification")
            .type(NotificationType.ALARM_COMMENT)
            .subject("Comment on '${alarmType}' alarm")
            .text("${userEmail} ${action} comment: ${comment}")
            .icon("people").color(null)
            .rule(DefaultRule.builder()
                    .name("Comment on active alarm")
                    .triggerConfig(AlarmCommentNotificationRuleTriggerConfig.builder()
                            .alarmTypes(null)
                            .alarmSeverities(null)
                            .alarmStatuses(Set.of(AlarmSearchStatus.ACTIVE))
                            .onlyUserComments(true)
                            .notifyOnCommentUpdate(false)
                            .build())
                    .description("Send notification to tenant admins when comment is added by user on active alarm")
                    .build())
            .build();
    public static final DefaultNotification alarmAssignment = DefaultNotification.builder()
            .name("Alarm assigned notification")
            .type(NotificationType.ALARM_ASSIGNMENT)
            .subject("Alarm '${alarmType}' (${alarmSeverity}) was assigned to user")
            .text("${userEmail} assigned alarm on ${alarmOriginatorEntityType} '${alarmOriginatorName}' to ${assigneeEmail}")
            .icon("person").color(null)
            .rule(DefaultRule.builder()
                    .name("Alarm assignment")
                    .triggerConfig(AlarmAssignmentNotificationRuleTriggerConfig.builder()
                            .alarmTypes(null)
                            .alarmSeverities(null)
                            .alarmStatuses(null)
                            .notifyOn(Set.of(AlarmAssignmentNotificationRuleTriggerConfig.Action.ASSIGNED))
                            .build())
                    .description("Send notification to user when any alarm was assigned to him")
                    .build())
            .build();
    public static final DefaultNotification ruleEngineComponentLifecycleFailure = DefaultNotification.builder()
            .name("Rule chain/node lifecycle failure notification")
            .type(NotificationType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT)
            .subject("${action:capitalize} failure in Rule chain '${ruleChainName}'")
            .text("${componentType} '${componentName}' failed to ${action}")
            .icon("warning").color(null)
            .button("Go to rule chain").link("/ruleChains/${ruleChainId}")
            .rule(DefaultRule.builder()
                    .name("Rule node initialization failure")
                    .triggerConfig(RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig.builder()
                            .ruleChains(null)
                            .ruleChainEvents(Set.of(ComponentLifecycleEvent.STARTED, ComponentLifecycleEvent.UPDATED, ComponentLifecycleEvent.STOPPED))
                            .onlyRuleChainLifecycleFailures(true)
                            .trackRuleNodeEvents(true)
                            .ruleNodeEvents(Set.of(ComponentLifecycleEvent.STARTED, ComponentLifecycleEvent.UPDATED, ComponentLifecycleEvent.STOPPED))
                            .onlyRuleNodeLifecycleFailures(true)
                            .build())
                    .description("Send notification to tenant admins when any Rule chain or Rule node failed to start, update or stop")
                    .build())
            .build();
    public static final DefaultNotification edgeConnection = DefaultNotification.builder()
            .name("Edge connection notification")
            .type(NotificationType.EDGE_CONNECTION)
            .subject("Edge connection status change")
            .text("Edge '${edgeName}' is now ${eventType}")
            .icon("info").color(null)
            .button("Go to Edge").link("/edgeManagement/instances/${edgeId}")
            .rule(DefaultRule.builder()
                    .name("Edge connection status change")
                    .triggerConfig(EdgeConnectionNotificationRuleTriggerConfig.builder()
                            .edges(null)
                            .notifyOn(Set.of(EdgeConnectivityEvent.CONNECTED, EdgeConnectivityEvent.DISCONNECTED))
                            .build())
                    .description("Send notification to tenant admins when the connection status between TB and Edge changes")
                    .build())
            .build();
    public static final DefaultNotification edgeCommunicationFailures = DefaultNotification.builder()
            .name("Edge communication failure notification")
            .type(NotificationType.EDGE_COMMUNICATION_FAILURE)
            .subject("Edge '${edgeName}' communication failure occurred")
            .text("Failure message: '${failureMsg}'")
            .icon("error").color(RED_COLOR)
            .button("Go to Edge").link("/edgeManagement/instances/${edgeId}")
            .rule(DefaultRule.builder()
                    .name("Edge communication failure")
                    .triggerConfig(EdgeCommunicationFailureNotificationRuleTriggerConfig.builder().edges(null).build())
                    .description("Send notification to tenant admins when communication failures occur")
                    .build())
            .build();

    public static final DefaultNotification jwtSigningKeyIssue = DefaultNotification.builder()
            .name("JWT Signing Key issue notification")
            .type(NotificationType.GENERAL)
            .subject("WARNING: security issue")
            .text("The platform is configured to use default JWT Signing Key. Please change it on the security settings page")
            .icon("warning").color(YELLOW_COLOR)
            .button("Go to settings").link("/security-settings/general")
            .build();

    /**
     * 字段说明：
     * 1. 保存 `templateService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private final NotificationTemplateService templateService;
    private final NotificationRuleService ruleService;

    /**
     * 方法说明：
     * 1. 职责：执行 `create` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public final void create(TenantId tenantId, DefaultNotification defaultNotification, NotificationTargetId... targets) {
        NotificationTemplate template = defaultNotification.toTemplate();
        template.setTenantId(tenantId);
        template = templateService.saveNotificationTemplate(tenantId, template);

        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (defaultNotification.getRule() != null && targets.length > 0) {
            NotificationRule rule = defaultNotification.toRule(template.getId(), targets);
            rule.setTenantId(tenantId);
            ruleService.saveNotificationRule(tenantId, rule);
        }
    }

    @Data
    @Builder(toBuilder = true)
    /**
     * 中文说明：
     * 1. 类目的：`DefaultNotification` 是 ThingsBoard DAO 模块 中的通知持久化服务类型，用于管理通知模板、规则、目标、请求、设置和用户通知状态的持久化访问。
     * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
     * 3. 协作对象：主要协作对象包括NotificationService、UserService、TenantService、Scheduler、缓存和审计服务。
     * 4. 生命周期：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化。
     * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
     * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
     * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
     * 8. 设计模式：主要体现 Service / Repository / Scheduler Command。
     */
    public static class DefaultNotification {

        /**
         * 字段说明：
         * 1. 保存 `name` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private final String name;
        private final NotificationType type;
        /**
         * 字段说明：
         * 1. 保存 `subject` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private final String subject;
        private final String text;
        /**
         * 字段说明：
         * 1. 保存 `icon` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private final String icon;
        private final String color;
        /**
         * 字段说明：
         * 1. 保存 `button` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private final String button;
        private final String link;

        /**
         * 字段说明：
         * 1. 保存 `rule` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private final DefaultRule rule;

        /**
         * 方法说明：
         * 1. 职责：执行 `toTemplate` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
         * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
         * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
         * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
         * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
         * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
         * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
         * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
         * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
         */
        public NotificationTemplate toTemplate() {
            NotificationTemplate template = new NotificationTemplate();
            template.setName(name);
            template.setNotificationType(type != null ? type : NotificationType.GENERAL);

            NotificationTemplateConfig templateConfig = new NotificationTemplateConfig();
            WebDeliveryMethodNotificationTemplate webTemplate = new WebDeliveryMethodNotificationTemplate();
            webTemplate.setSubject(subject);
            webTemplate.setBody(text);
            ObjectNode additionalConfig = newObjectNode();
            ObjectNode iconConfig = newObjectNode();
            additionalConfig.set("icon", iconConfig);
            ObjectNode buttonConfig = newObjectNode();
            additionalConfig.set("actionButtonConfig", buttonConfig);
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (icon != null) {
                iconConfig.put("enabled", true)
                        .put("icon", icon)
                        .put("color", color != null ? color : "#757575");
            } else {
                iconConfig.put("enabled", false);
            }
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (button != null) {
                buttonConfig.put("enabled", true)
                        .put("text", button)
                        .put("linkType", "LINK")
                        .put("link", link);
            } else {
                buttonConfig.put("enabled", false);
            }
            webTemplate.setAdditionalConfig(additionalConfig);
            webTemplate.setEnabled(true);
            templateConfig.setDeliveryMethodsTemplates(Map.of(
                    NotificationDeliveryMethod.WEB, webTemplate
            ));
            template.setConfiguration(templateConfig);
            return template;
        }

        /**
         * 方法说明：
         * 1. 职责：执行 `toRule` 对应的通知持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
         * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
         * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
         * 4. 调用时机：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
         * 5. 使用流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
         * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
         * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
         * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
         * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
         */
        public NotificationRule toRule(NotificationTemplateId templateId, NotificationTargetId... targets) {
            DefaultRule defaultRule = this.rule;
            NotificationRule rule = new NotificationRule();
            rule.setName(defaultRule.getName());
            rule.setEnabled(defaultRule.getEnabled() == null || defaultRule.getEnabled());
            rule.setTemplateId(templateId);
            rule.setTriggerType(defaultRule.getTriggerConfig().getTriggerType());
            rule.setTriggerConfig(defaultRule.getTriggerConfig());
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (rule.getTriggerType() == NotificationRuleTriggerType.ALARM) {
                EscalatedNotificationRuleRecipientsConfig recipientsConfig = new EscalatedNotificationRuleRecipientsConfig();
                recipientsConfig.setTriggerType(rule.getTriggerType());
                recipientsConfig.setEscalationTable(Map.of(0, toUUIDs(List.of(targets))));
                rule.setRecipientsConfig(recipientsConfig);
            } else {
                DefaultNotificationRuleRecipientsConfig recipientsConfig = new DefaultNotificationRuleRecipientsConfig();
                recipientsConfig.setTriggerType(rule.getTriggerType());
                recipientsConfig.setTargets(toUUIDs(List.of(targets)));
                rule.setRecipientsConfig(recipientsConfig);
            }
            NotificationRuleConfig additionalConfig = new NotificationRuleConfig();
            additionalConfig.setDescription(defaultRule.getDescription());
            rule.setAdditionalConfig(additionalConfig);
            return rule;
        }

    }

    @Data
    @Builder(toBuilder = true)
    /**
     * 中文说明：
     * 1. 类目的：`DefaultRule` 是 ThingsBoard DAO 模块 中的通知持久化服务类型，用于管理通知模板、规则、目标、请求、设置和用户通知状态的持久化访问。
     * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
     * 3. 协作对象：主要协作对象包括NotificationService、UserService、TenantService、Scheduler、缓存和审计服务。
     * 4. 生命周期：由通知创建、发送、确认、查询或规则更新流程调用，随数据库事务和缓存状态变化。
     * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
     * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
     * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
     * 8. 设计模式：主要体现 Service / Repository / Scheduler Command。
     */
    public static class DefaultRule {
        /**
         * 字段说明：
         * 1. 保存 `name` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private final String name;
        private final Boolean enabled;
        /**
         * 字段说明：
         * 1. 保存 `triggerConfig` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
         * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
         * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
         */
        private final NotificationRuleTriggerConfig triggerConfig;
        private final String description;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultNotifications` 在 ThingsBoard DAO 模块 中承担通知持久化服务类型职责，核心目的是管理通知模板、规则、目标、请求、设置和用户通知状态的持久化访问。
 * 2. 核心流程：根据租户、接收方和通知规则读写数据库，并把状态返回给通知发送或查询流程。
 * 3. 关键依赖：主要依赖或协作对象包括NotificationService、UserService、TenantService、Scheduler、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
