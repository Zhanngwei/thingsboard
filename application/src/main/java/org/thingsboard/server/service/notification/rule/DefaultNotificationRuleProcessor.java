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
package org.thingsboard.server.service.notification.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.notification.NotificationDeduplicationService;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.notification.rule.cache.NotificationRulesCache;
import org.thingsboard.server.service.notification.rule.trigger.NotificationRuleTriggerProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
/**
 * 中文说明：
 * 1. 类目的：`DefaultNotificationRuleProcessor` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultNotificationRuleProcessor implements NotificationRuleProcessor {

    /**
     * 字段说明：
     * 1. 保存 `notificationRulesCache` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final NotificationRulesCache notificationRulesCache;
    private final NotificationRequestService notificationRequestService;
    /**
     * 字段说明：
     * 1. 保存 `deduplicationService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final NotificationDeduplicationService deduplicationService;
    private final PartitionService partitionService;
    /**
     * 字段说明：
     * 1. 保存 `rateLimitService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final RateLimitService rateLimitService;
    @Autowired @Lazy
    /**
     * 字段说明：
     * 1. 保存 `notificationCenter` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private NotificationCenter notificationCenter;
    private final NotificationExecutorService notificationExecutor;

    private final Map<NotificationRuleTriggerType, NotificationRuleTriggerProcessor> triggerProcessors = new EnumMap<>(NotificationRuleTriggerType.class);

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `process` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void process(NotificationRuleTrigger trigger) {
        NotificationRuleTriggerType triggerType = trigger.getType();
        TenantId tenantId = triggerType.isTenantLevel() ? trigger.getTenantId() : TenantId.SYS_TENANT_ID;
        notificationExecutor.submit(() -> {
            try {
                // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
                List<NotificationRule> enabledRules = notificationRulesCache.getEnabled(tenantId, triggerType);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (enabledRules.isEmpty()) {
                    return;
                }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (trigger.deduplicate()) {
                    enabledRules = new ArrayList<>(enabledRules);
                    enabledRules.removeIf(rule -> deduplicationService.alreadyProcessed(trigger, rule));
                }
                final List<NotificationRule> rules = enabledRules;
                // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                for (NotificationRule rule : rules) {
                    try {
                        processNotificationRule(rule, trigger);
                    // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                    } catch (Throwable e) {
                        log.error("Failed to process notification rule {} for trigger type {} with trigger object {}", rule.getId(), rule.getTriggerType(), trigger, e);
                    }
                }
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Throwable e) {
                log.error("Failed to process notification rules for trigger: {}", trigger, e);
            }
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processNotificationRule` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processNotificationRule(NotificationRule rule, NotificationRuleTrigger trigger) {
        NotificationRuleTriggerConfig triggerConfig = rule.getTriggerConfig();
        log.debug("Processing notification rule '{}' for trigger type {}", rule.getName(), rule.getTriggerType());

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (matchesClearRule(trigger, triggerConfig)) {
            List<NotificationRequest> notificationRequests = findAlreadySentNotificationRequests(rule, trigger);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (notificationRequests.isEmpty()) {
                return;
            }

            List<UUID> targets = notificationRequests.stream()
                    .filter(NotificationRequest::isSent)
                    .flatMap(notificationRequest -> notificationRequest.getTargets().stream())
                    .distinct().collect(Collectors.toList());
            NotificationInfo notificationInfo = constructNotificationInfo(trigger, triggerConfig);
            submitNotificationRequest(targets, rule, trigger.getOriginatorEntityId(), notificationInfo, 0);

            notificationRequests.forEach(notificationRequest -> {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (notificationRequest.isScheduled()) {
                    notificationCenter.deleteNotificationRequest(rule.getTenantId(), notificationRequest.getId());
                }
            });
            return;
        }

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (matchesFilter(trigger, triggerConfig)) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!rateLimitService.checkRateLimit(LimitedApi.NOTIFICATION_REQUESTS_PER_RULE, rule.getTenantId(), rule.getId())) {
                log.debug("[{}] Rate limit for notification requests per rule was exceeded (rule '{}')", rule.getTenantId(), rule.getName());
                return;
            }

            NotificationInfo notificationInfo = constructNotificationInfo(trigger, triggerConfig);
            rule.getRecipientsConfig().getTargetsTable().forEach((delay, targets) -> {
                submitNotificationRequest(targets, rule, trigger.getOriginatorEntityId(), notificationInfo, delay);
            });
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `findAlreadySentNotificationRequests` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<NotificationRequest> findAlreadySentNotificationRequests(NotificationRule rule, NotificationRuleTrigger trigger) {
        return notificationRequestService.findNotificationRequestsByRuleIdAndOriginatorEntityId(rule.getTenantId(), rule.getId(), trigger.getOriginatorEntityId());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `submitNotificationRequest` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void submitNotificationRequest(List<UUID> targets, NotificationRule rule,
                                           EntityId originatorEntityId, NotificationInfo notificationInfo, int delayInSec) {
        NotificationRequestConfig config = new NotificationRequestConfig();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (delayInSec > 0) {
            config.setSendingDelayInSec(delayInSec);
        }
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(rule.getTenantId())
                .targets(targets)
                .templateId(rule.getTemplateId())
                .additionalConfig(config)
                .info(notificationInfo)
                .ruleId(rule.getId())
                .originatorEntityId(originatorEntityId)
                .build();

        try {
            log.debug("Submitting notification request for rule '{}' with delay of {} sec to targets {}", rule.getName(), delayInSec, targets);
            notificationCenter.processNotificationRequest(rule.getTenantId(), notificationRequest, null);
        } catch (Exception e) {
            log.error("Failed to process notification request for tenant {} for rule {}", rule.getTenantId(), rule.getId(), e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `matchesFilter` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean matchesFilter(NotificationRuleTrigger trigger, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).matchesFilter(trigger, triggerConfig);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `matchesClearRule` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean matchesClearRule(NotificationRuleTrigger trigger, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).matchesClearRule(trigger, triggerConfig);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `constructNotificationInfo` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private NotificationInfo constructNotificationInfo(NotificationRuleTrigger trigger, NotificationRuleTriggerConfig triggerConfig) {
        return triggerProcessors.get(triggerConfig.getTriggerType()).constructNotificationInfo(trigger);
    }

    @EventListener(ComponentLifecycleMsg.class)
    /**
     * 方法说明：
     * 1. 职责：执行 `onNotificationRuleDeleted` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onNotificationRuleDeleted(ComponentLifecycleMsg componentLifecycleMsg) {
        if (componentLifecycleMsg.getEvent() != ComponentLifecycleEvent.DELETED ||
                componentLifecycleMsg.getEntityId().getEntityType() != EntityType.NOTIFICATION_RULE) {
            return;
        }

        TenantId tenantId = componentLifecycleMsg.getTenantId();
        NotificationRuleId notificationRuleId = (NotificationRuleId) componentLifecycleMsg.getEntityId();
        if (partitionService.isMyPartition(ServiceType.TB_CORE, tenantId, notificationRuleId)) {
            notificationExecutor.submit(() -> {
                List<NotificationRequestId> scheduledForRule = notificationRequestService.findNotificationRequestsIdsByStatusAndRuleId(tenantId, NotificationRequestStatus.SCHEDULED, notificationRuleId);
                for (NotificationRequestId notificationRequestId : scheduledForRule) {
                    notificationCenter.deleteNotificationRequest(tenantId, notificationRequestId);
                }
            });
        }
    }

    @Autowired
    /**
     * 方法说明：
     * 1. 职责：执行 `setTriggerProcessors` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setTriggerProcessors(Collection<NotificationRuleTriggerProcessor> processors) {
        processors.forEach(processor -> {
            triggerProcessors.put(processor.getTriggerType(), processor);
        });
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultNotificationRuleProcessor` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
