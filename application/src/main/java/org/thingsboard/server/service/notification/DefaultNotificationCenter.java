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
package org.thingsboard.server.service.notification;

import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.AlreadySentException;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings;
import org.thingsboard.server.common.data.notification.targets.MicrosoftTeamsNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.targets.slack.SlackNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.notification.channels.NotificationChannel;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;
import org.thingsboard.server.service.telemetry.AbstractSubscriptionService;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.WEB;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes"})
/**
 * 中文说明：
 * 1. 类目的：`DefaultNotificationCenter` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultNotificationCenter extends AbstractSubscriptionService implements NotificationCenter, NotificationChannel<User, WebDeliveryMethodNotificationTemplate> {

    /**
     * 字段说明：
     * 1. 保存 `notificationTargetService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final NotificationTargetService notificationTargetService;
    private final NotificationRequestService notificationRequestService;
    /**
     * 字段说明：
     * 1. 保存 `notificationService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final NotificationService notificationService;
    private final NotificationTemplateService notificationTemplateService;
    /**
     * 字段说明：
     * 1. 保存 `notificationSettingsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final NotificationSettingsService notificationSettingsService;
    private final NotificationExecutorService notificationExecutor;
    /**
     * 字段说明：
     * 1. 保存 `topicService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TopicService topicService;
    private final TbQueueProducerProvider producerProvider;
    /**
     * 字段说明：
     * 1. 保存 `rateLimitService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final RateLimitService rateLimitService;

    /**
     * 字段说明：
     * 1. 保存 `channels` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Map<NotificationDeliveryMethod, NotificationChannel> channels;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processNotificationRequest` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public NotificationRequest processNotificationRequest(TenantId tenantId, NotificationRequest request, FutureCallback<NotificationRequestStats> callback) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (request.getRuleId() == null) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!rateLimitService.checkRateLimit(LimitedApi.NOTIFICATION_REQUESTS, tenantId)) {
                throw new TbRateLimitsException(EntityType.TENANT);
            }
        }

        NotificationTemplate notificationTemplate;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (request.getTemplateId() != null) {
            notificationTemplate = notificationTemplateService.findNotificationTemplateById(tenantId, request.getTemplateId());
        } else {
            notificationTemplate = request.getTemplate();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (notificationTemplate == null) throw new IllegalArgumentException("Template is missing");

        Set<NotificationDeliveryMethod> deliveryMethods = new HashSet<>();
        List<NotificationTarget> targets = request.getTargets().stream().map(NotificationTargetId::new)
                .map(id -> notificationTargetService.findNotificationTargetById(tenantId, id))
                .collect(Collectors.toList());

        NotificationRuleId ruleId = request.getRuleId();
        notificationTemplate.getConfiguration().getDeliveryMethodsTemplates().forEach((deliveryMethod, template) -> {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!template.isEnabled()) return;
            try {
                channels.get(deliveryMethod).check(tenantId);
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (ruleId == null) {
                    throw new IllegalArgumentException(e.getMessage());
                } else {
                    return; // if originated by rule - just ignore delivery method
                }
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (ruleId == null) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (targets.stream().noneMatch(target -> target.getConfiguration().getType().getSupportedDeliveryMethods().contains(deliveryMethod))) {
                    throw new IllegalArgumentException("Recipients for " + deliveryMethod.getName() + " delivery method not chosen");
                }
            }
            deliveryMethods.add(deliveryMethod);
        });
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (deliveryMethods.isEmpty()) {
            throw new IllegalArgumentException("No delivery methods to send notification with");
        }

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (request.getAdditionalConfig() != null) {
            NotificationRequestConfig config = request.getAdditionalConfig();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (config.getSendingDelayInSec() > 0 && request.getId() == null) {
                request.setStatus(NotificationRequestStatus.SCHEDULED);
                request = notificationRequestService.saveNotificationRequest(tenantId, request);
                forwardToNotificationSchedulerService(tenantId, request.getId());
                return request;
            }
        }
        NotificationSettings settings = notificationSettingsService.findNotificationSettings(tenantId);
        NotificationSettings systemSettings = tenantId.isSysTenantId() ? settings : notificationSettingsService.findNotificationSettings(TenantId.SYS_TENANT_ID);

        log.debug("Processing notification request (tenantId: {}, targets: {})", tenantId, request.getTargets());
        request.setStatus(NotificationRequestStatus.PROCESSING);
        request = notificationRequestService.saveNotificationRequest(tenantId, request);

        NotificationProcessingContext ctx = NotificationProcessingContext.builder()
                .tenantId(tenantId)
                .request(request)
                .deliveryMethods(deliveryMethods)
                .template(notificationTemplate)
                .settings(settings)
                .systemSettings(systemSettings)
                .build();

        processNotificationRequestAsync(ctx, targets, callback);
        return request;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `sendGeneralWebNotification` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendGeneralWebNotification(TenantId tenantId, UsersFilter recipients, NotificationTemplate template) {
        NotificationTarget target = new NotificationTarget();
        target.setTenantId(tenantId);
        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(recipients);
        target.setConfiguration(targetConfig);

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(tenantId)
                .template(template)
                .targets(List.of(EntityId.NULL_UUID)) // this is temporary and will be removed when 'create from scratch' functionality is implemented for recipients
                .status(NotificationRequestStatus.PROCESSING)
                .build();
        try {
            notificationRequest = notificationRequestService.saveNotificationRequest(tenantId, notificationRequest);
            NotificationProcessingContext ctx = NotificationProcessingContext.builder()
                    .tenantId(tenantId)
                    .request(notificationRequest)
                    .deliveryMethods(Set.of(WEB))
                    .template(template)
                    .build();

            processNotificationRequestAsync(ctx, List.of(target), null);
        } catch (Exception e) {
            log.error("Failed to process notification request for recipients {} for template '{}'", recipients, template.getName(), e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processNotificationRequestAsync` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processNotificationRequestAsync(NotificationProcessingContext ctx, List<NotificationTarget> targets, FutureCallback<NotificationRequestStats> callback) {
        notificationExecutor.submit(() -> {
            long startTs = System.currentTimeMillis();
            NotificationRequestId requestId = ctx.getRequest().getId();
            for (NotificationTarget target : targets) {
                try {
                    processForTarget(target, ctx);
                } catch (Exception e) {
                    log.error("[{}] Failed to process notification request for target {}", requestId, target.getId(), e);
                    ctx.getStats().setError(e.getMessage());
                    updateRequestStats(ctx, requestId, ctx.getStats());

                    if (callback != null) {
                        callback.onFailure(e);
                    }
                    return;
                }
            }

            NotificationRequestStats stats = ctx.getStats();
            long time = System.currentTimeMillis() - startTs;
            int sent = stats.getTotalSent().get();
            int errors = stats.getTotalErrors().get();
            if (errors > 0) {
                log.info("[{}][{}] Notification request processing finished in {} ms (sent: {}, errors: {})", ctx.getTenantId(), requestId, time, sent, errors);
            } else {
                log.info("[{}][{}] Notification request processing finished in {} ms (sent: {})", ctx.getTenantId(), requestId, time, sent);
            }
            updateRequestStats(ctx, requestId, stats);
            if (callback != null) {
                callback.onSuccess(stats);
            }
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `updateRequestStats` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void updateRequestStats(NotificationProcessingContext ctx, NotificationRequestId requestId, NotificationRequestStats stats) {
        try {
            notificationRequestService.updateNotificationRequest(ctx.getTenantId(), requestId, NotificationRequestStatus.SENT, stats);
        } catch (Exception e) {
            log.error("[{}] Failed to update stats for notification request", requestId, e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processForTarget` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processForTarget(NotificationTarget target, NotificationProcessingContext ctx) {
        Iterable<? extends NotificationRecipient> recipients;
        switch (target.getConfiguration().getType()) {
            case PLATFORM_USERS: {
                PlatformUsersNotificationTargetConfig targetConfig = (PlatformUsersNotificationTargetConfig) target.getConfiguration();
                if (targetConfig.getUsersFilter().getType().isForRules() && ctx.getRequest().getInfo() instanceof RuleOriginatedNotificationInfo) {
                    recipients = new PageDataIterable<>(pageLink -> {
                        return notificationTargetService.findRecipientsForRuleNotificationTargetConfig(ctx.getTenantId(), targetConfig, (RuleOriginatedNotificationInfo) ctx.getRequest().getInfo(), pageLink);
                    }, 256);
                } else {
                    recipients = new PageDataIterable<>(pageLink -> {
                        return notificationTargetService.findRecipientsForNotificationTargetConfig(ctx.getTenantId(), targetConfig, pageLink);
                    }, 256);
                }
                break;
            }
            case SLACK: {
                SlackNotificationTargetConfig targetConfig = (SlackNotificationTargetConfig) target.getConfiguration();
                recipients = List.of(targetConfig.getConversation());
                break;
            }
            case MICROSOFT_TEAMS: {
                MicrosoftTeamsNotificationTargetConfig targetConfig = (MicrosoftTeamsNotificationTargetConfig) target.getConfiguration();
                recipients = List.of(targetConfig);
                break;
            }
            default: {
                recipients = Collections.emptyList();
            }
        }

        Set<NotificationDeliveryMethod> deliveryMethods = new HashSet<>(ctx.getDeliveryMethods());
        deliveryMethods.removeIf(deliveryMethod -> !target.getConfiguration().getType().getSupportedDeliveryMethods().contains(deliveryMethod));
        log.debug("[{}] Processing notification request for {} target ({}) for delivery methods {}", ctx.getRequest().getId(), target.getConfiguration().getType(), target.getId(), deliveryMethods);
        if (deliveryMethods.isEmpty()) {
            return;
        }

        for (NotificationRecipient recipient : recipients) {
            for (NotificationDeliveryMethod deliveryMethod : deliveryMethods) {
                try {
                    processForRecipient(deliveryMethod, recipient, ctx);
                    ctx.getStats().reportSent(deliveryMethod, recipient);
                } catch (Exception error) {
                    ctx.getStats().reportError(deliveryMethod, error, recipient);
                }
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processForRecipient` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processForRecipient(NotificationDeliveryMethod deliveryMethod, NotificationRecipient recipient, NotificationProcessingContext ctx) throws Exception {
        if (ctx.getStats().contains(deliveryMethod, recipient.getId())) {
            throw new AlreadySentException();
        } else {
            ctx.getStats().reportProcessed(deliveryMethod, recipient.getId());
        }

        if (recipient instanceof User) {
            UserNotificationSettings settings = notificationSettingsService.getUserNotificationSettings(ctx.getTenantId(), ((User) recipient).getId(), false);
            if (!settings.isEnabled(ctx.getNotificationType(), deliveryMethod)) {
                throw new RuntimeException("User disabled " + deliveryMethod.getName() + " notifications of this type");
            }
        }

        NotificationChannel notificationChannel = channels.get(deliveryMethod);
        DeliveryMethodNotificationTemplate processedTemplate = ctx.getProcessedTemplate(deliveryMethod, recipient);

        log.trace("[{}] Sending {} notification for recipient {}", ctx.getRequest().getId(), deliveryMethod, recipient);
        notificationChannel.sendNotification(recipient, processedTemplate, ctx);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `sendNotification` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendNotification(User recipient, WebDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        NotificationRequest request = ctx.getRequest();
        Notification notification = Notification.builder()
                .requestId(request.getId())
                .recipientId(recipient.getId())
                .type(ctx.getNotificationType())
                .deliveryMethod(WEB)
                .subject(processedTemplate.getSubject())
                .text(processedTemplate.getBody())
                .additionalConfig(processedTemplate.getAdditionalConfig())
                .info(request.getInfo())
                .status(NotificationStatus.SENT)
                .build();
        try {
            notification = notificationService.saveNotification(recipient.getTenantId(), notification);
        } catch (Exception e) {
            log.error("Failed to create notification for recipient {}", recipient.getId(), e);
            throw e;
        }

        NotificationUpdate update = NotificationUpdate.builder()
                .created(true)
                .notification(notification)
                .build();
        onNotificationUpdate(recipient.getTenantId(), recipient.getId(), update);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `markNotificationAsRead` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        boolean updated = notificationService.markNotificationAsRead(tenantId, recipientId, notificationId);
        if (updated) {
            log.trace("Marked notification {} as read (recipient id: {}, tenant id: {})", notificationId, recipientId, tenantId);
            Notification notification = notificationService.findNotificationById(tenantId, notificationId);
            if (notification.getDeliveryMethod() == WEB) {
                NotificationUpdate update = NotificationUpdate.builder()
                        .updated(true)
                        .notificationId(notificationId.getId())
                        .newStatus(NotificationStatus.READ)
                        .build();
                onNotificationUpdate(tenantId, recipientId, update);
            }
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `markAllNotificationsAsRead` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void markAllNotificationsAsRead(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId) {
        int updatedCount = notificationService.markAllNotificationsAsRead(tenantId, deliveryMethod, recipientId);
        if (updatedCount > 0 && deliveryMethod == WEB) {
            log.trace("Marked all notifications as read (recipient id: {}, tenant id: {})", recipientId, tenantId);
            NotificationUpdate update = NotificationUpdate.builder()
                    .updated(true)
                    .allNotifications(true)
                    .newStatus(NotificationStatus.READ)
                    .build();
            onNotificationUpdate(tenantId, recipientId, update);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteNotification` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteNotification(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        Notification notification = notificationService.findNotificationById(tenantId, notificationId);
        boolean deleted = notificationService.deleteNotification(tenantId, recipientId, notificationId);
        if (deleted && notification.getDeliveryMethod() == WEB) {
            NotificationUpdate update = NotificationUpdate.builder()
                    .deleted(true)
                    .notification(notification)
                    .build();
            onNotificationUpdate(tenantId, recipientId, update);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getAvailableDeliveryMethods` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Set<NotificationDeliveryMethod> getAvailableDeliveryMethods(TenantId tenantId) {
        return channels.values().stream()
                .filter(channel -> {
                    try {
                        channel.check(tenantId);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(NotificationChannel::getDeliveryMethod)
                .collect(Collectors.toSet());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `check` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void check(TenantId tenantId) throws Exception {
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteNotificationRequest` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId) {
        log.debug("Deleting notification request {}", notificationRequestId);
        NotificationRequest notificationRequest = notificationRequestService.findNotificationRequestById(tenantId, notificationRequestId);
        notificationRequestService.deleteNotificationRequest(tenantId, notificationRequestId);

        if (notificationRequest.isSent()) {
            // TODO: no need to send request update for other than PLATFORM_USERS target type
            onNotificationRequestUpdate(tenantId, NotificationRequestUpdate.builder()
                    .notificationRequestId(notificationRequestId)
                    .deleted(true)
                    .build());
        } else if (notificationRequest.isScheduled()) {
            // TODO: just forward to scheduler service
            clusterService.broadcastEntityStateChangeEvent(tenantId, notificationRequestId, ComponentLifecycleEvent.DELETED);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `forwardToNotificationSchedulerService` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void forwardToNotificationSchedulerService(TenantId tenantId, NotificationRequestId notificationRequestId) {
        TransportProtos.NotificationSchedulerServiceMsg.Builder msg = TransportProtos.NotificationSchedulerServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequestIdMSB(notificationRequestId.getId().getMostSignificantBits())
                .setRequestIdLSB(notificationRequestId.getId().getLeastSignificantBits())
                .setTs(System.currentTimeMillis());
        TransportProtos.ToCoreMsg toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setNotificationSchedulerServiceMsg(msg)
                .build();
        clusterService.pushMsgToCore(tenantId, notificationRequestId, toCoreMsg, null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onNotificationUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onNotificationUpdate(TenantId tenantId, UserId recipientId, NotificationUpdate update) {
        log.trace("Submitting notification update for recipient {}: {}", recipientId, update);
        forwardToSubscriptionManagerService(tenantId, recipientId, subscriptionManagerService -> {
            subscriptionManagerService.onNotificationUpdate(tenantId, recipientId, update, TbCallback.EMPTY);
        }, () -> TbSubscriptionUtils.notificationUpdateToProto(tenantId, recipientId, update));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onNotificationRequestUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onNotificationRequestUpdate(TenantId tenantId, NotificationRequestUpdate update) {
        log.trace("Submitting notification request update: {}", update);
        wsCallBackExecutor.submit(() -> {
            TransportProtos.ToCoreNotificationMsg notificationRequestUpdateProto = TbSubscriptionUtils.notificationRequestUpdateToProto(tenantId, update);
            Set<String> coreServices = new HashSet<>(partitionService.getAllServiceIds(ServiceType.TB_CORE));
            for (String serviceId : coreServices) {
                TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
                producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), notificationRequestUpdateProto), null);
            }
        });
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getDeliveryMethod` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public NotificationDeliveryMethod getDeliveryMethod() {
        return WEB;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getExecutorPrefix` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected String getExecutorPrefix() {
        return "notification";
    }

    @Autowired
    /**
     * 方法说明：
     * 1. 职责：执行 `setChannels` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setChannels(List<NotificationChannel> channels, NotificationCenter webNotificationChannel) {
        this.channels = channels.stream().collect(Collectors.toMap(NotificationChannel::getDeliveryMethod, c -> c));
        this.channels.put(WEB, (NotificationChannel) webNotificationChannel);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultNotificationCenter` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
