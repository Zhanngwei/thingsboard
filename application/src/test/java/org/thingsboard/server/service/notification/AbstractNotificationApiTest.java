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

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.HasSubject;
import org.thingsboard.server.common.data.notification.template.MobileAppDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.SmsDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`AbstractNotificationApiTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public abstract class AbstractNotificationApiTest extends AbstractControllerTest {

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    protected NotificationApiWsClient wsClient;
    protected NotificationApiWsClient otherWsClient;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected SlackService slackService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected MailService mailService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected NotificationRuleService notificationRuleService;
    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected NotificationTemplateService notificationTemplateService;
    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected NotificationTargetService notificationTargetService;
    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected NotificationRequestService notificationRequestService;
    /**
     * 通知服务集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected NotificationSettingsService notificationSettingsService;
    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    protected SqlPartitioningRepository partitioningRepository;
    /**
     * `defaultNotifications` 字段，保存当前对象的对应属性。
     */
    @Autowired
    protected DefaultNotifications defaultNotifications;

    /**
     * 通知常量，用于统一引用固定值。
     */
    public static final String DEFAULT_NOTIFICATION_SUBJECT = "Just a test";
    public static final NotificationType DEFAULT_NOTIFICATION_TYPE = NotificationType.GENERAL;

    /**
     * 功能：执行 `afterEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterEach() {
        notificationRequestService.deleteNotificationRequestsByTenantId(TenantId.SYS_TENANT_ID);
        notificationRuleService.deleteNotificationRulesByTenantId(TenantId.SYS_TENANT_ID);
        notificationTemplateService.deleteNotificationTemplatesByTenantId(TenantId.SYS_TENANT_ID);
        notificationTargetService.deleteNotificationTargetsByTenantId(TenantId.SYS_TENANT_ID);
        partitioningRepository.dropPartitionsBefore("notification", Long.MAX_VALUE, 1);
        notificationSettingsService.deleteNotificationSettings(TenantId.SYS_TENANT_ID);
    }

    /**
     * 功能：保存或创建目标对象。
     * 参数：
     * - `usersIds`：`usersIds` 参数。
     * 返回：处理结果。
     */
    protected NotificationTarget createNotificationTarget(UserId... usersIds) {
        UserListFilter filter = new UserListFilter();
        filter.setUsersIds(DaoUtil.toUUIDs(List.of(usersIds)));
        return createNotificationTarget(filter);
    }

    /**
     * 功能：保存或创建目标对象。
     * 参数：
     * - `usersFilter`：`usersFilter` 参数。
     * 返回：处理结果。
     */
    protected NotificationTarget createNotificationTarget(UsersFilter usersFilter) {
        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setName(usersFilter.toString());
        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(usersFilter);
        notificationTarget.setConfiguration(targetConfig);
        return saveNotificationTarget(notificationTarget);
    }

    /**
     * 功能：保存或创建目标对象。
     * 参数：
     * - `notificationTarget`：`notificationTarget` 参数。
     * 返回：处理结果。
     */
    protected NotificationTarget saveNotificationTarget(NotificationTarget notificationTarget) {
        return doPost("/api/notification/target", notificationTarget, NotificationTarget.class);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `targetId`：目标对象ID。
     * - `text`：`text` 参数。
     * - `deliveryMethods`：`deliveryMethods` 参数。
     * 返回：处理结果。
     */
    protected NotificationRequest submitNotificationRequest(NotificationTargetId targetId, String text, NotificationDeliveryMethod... deliveryMethods) {
        return submitNotificationRequest(targetId, text, 0, deliveryMethods);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `targetId`：目标对象ID。
     * - `text`：`text` 参数。
     * - `delayInSec`：`delayInSec` 参数。
     * - `deliveryMethods`：`deliveryMethods` 参数。
     * 返回：处理结果。
     */
    protected NotificationRequest submitNotificationRequest(NotificationTargetId targetId, String text, int delayInSec, NotificationDeliveryMethod... deliveryMethods) {
        return submitNotificationRequest(List.of(targetId), text, delayInSec, deliveryMethods);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `targets`：数据列表。
     * - `text`：`text` 参数。
     * - `delayInSec`：`delayInSec` 参数。
     * - `deliveryMethods`：`deliveryMethods` 参数。
     * 返回：处理结果。
     */
    protected NotificationRequest submitNotificationRequest(List<NotificationTargetId> targets, String text, int delayInSec, NotificationDeliveryMethod... deliveryMethods) {
        if (deliveryMethods.length == 0) {
            deliveryMethods = new NotificationDeliveryMethod[]{NotificationDeliveryMethod.WEB, NotificationDeliveryMethod.MOBILE_APP};
        }
        NotificationTemplate notificationTemplate = createNotificationTemplate(DEFAULT_NOTIFICATION_TYPE, DEFAULT_NOTIFICATION_SUBJECT, text, deliveryMethods);
        return submitNotificationRequest(targets, notificationTemplate.getId(), delayInSec);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `targets`：数据列表。
     * - `notificationTemplateId`：通知ID。
     * - `delayInSec`：`delayInSec` 参数。
     * 返回：处理结果。
     */
    protected NotificationRequest submitNotificationRequest(List<NotificationTargetId> targets, NotificationTemplateId notificationTemplateId, int delayInSec) {
        NotificationRequestConfig config = new NotificationRequestConfig();
        config.setSendingDelayInSec(delayInSec);
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .targets(targets.stream().map(UUIDBased::getId).collect(Collectors.toList()))
                .templateId(notificationTemplateId)
                .additionalConfig(config)
                .build();
        return doPost("/api/notification/request", notificationRequest, NotificationRequest.class);
    }

    /**
     * 功能：获取`Stats`。
     * 参数：
     * - `notificationRequestId`：请求ID。
     * 返回：处理结果。
     */
    protected NotificationRequestStats getStats(NotificationRequestId notificationRequestId) throws Exception {
        return findNotificationRequest(notificationRequestId).getStats();
    }

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `notificationType`：类型。
     * - `subject`：`subject` 参数。
     * - `text`：`text` 参数。
     * - `deliveryMethods`：`deliveryMethods` 参数。
     * 返回：处理结果。
     */
    protected NotificationTemplate createNotificationTemplate(NotificationType notificationType, String subject,
                                                              String text, NotificationDeliveryMethod... deliveryMethods) {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setTenantId(tenantId);
        notificationTemplate.setName("Notification template: " + text);
        notificationTemplate.setNotificationType(notificationType);
        NotificationTemplateConfig config = new NotificationTemplateConfig();
        config.setDeliveryMethodsTemplates(new HashMap<>());
        for (NotificationDeliveryMethod deliveryMethod : deliveryMethods) {
            DeliveryMethodNotificationTemplate deliveryMethodNotificationTemplate;
            switch (deliveryMethod) {
                case WEB: {
                    deliveryMethodNotificationTemplate = new WebDeliveryMethodNotificationTemplate();
                    break;
                }
                case EMAIL: {
                    deliveryMethodNotificationTemplate = new EmailDeliveryMethodNotificationTemplate();
                    break;
                }
                case SMS: {
                    deliveryMethodNotificationTemplate = new SmsDeliveryMethodNotificationTemplate();
                    break;
                }
                case MOBILE_APP:
                    deliveryMethodNotificationTemplate = new MobileAppDeliveryMethodNotificationTemplate();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported delivery method " + deliveryMethod);
            }
            deliveryMethodNotificationTemplate.setEnabled(true);
            deliveryMethodNotificationTemplate.setBody(text);
            if (deliveryMethodNotificationTemplate instanceof HasSubject) {
                ((HasSubject) deliveryMethodNotificationTemplate).setSubject(subject);
            }
            config.getDeliveryMethodsTemplates().put(deliveryMethod, deliveryMethodNotificationTemplate);
        }
        notificationTemplate.setConfiguration(config);
        return saveNotificationTemplate(notificationTemplate);
    }

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `notificationTemplate`：`notificationTemplate` 参数。
     * 返回：处理结果。
     */
    protected NotificationTemplate saveNotificationTemplate(NotificationTemplate notificationTemplate) {
        return doPost("/api/notification/template", notificationTemplate, NotificationTemplate.class);
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `notificationSettings`：配置对象。
     * 返回：无。
     */
    protected void saveNotificationSettings(NotificationSettings notificationSettings) throws Exception {
        doPost("/api/notification/settings", notificationSettings).andExpect(status().isOk());
    }

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `configs`：配置对象。
     * 返回：无。
     */
    protected void saveNotificationSettings(NotificationDeliveryMethodConfig... configs) throws Exception {
        NotificationSettings settings = new NotificationSettings();
        settings.setDeliveryMethodsConfigs(Arrays.stream(configs)
                .collect(Collectors.toMap(
                        NotificationDeliveryMethodConfig::getMethod, config -> config
                )));
        saveNotificationSettings(settings);
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `authority`：`authority` 参数。
     * 返回：处理结果。
     */
    protected Pair<User, NotificationApiWsClient> createUserAndConnectWsClient(Authority authority) throws Exception {
        User user = new User();
        user.setTenantId(tenantId);
        user.setAuthority(authority);
        user.setEmail(RandomStringUtils.randomAlphabetic(20) + "@thingsboard.com");
        user = createUserAndLogin(user, "12345678");
        NotificationApiWsClient wsClient = buildAndConnectWebSocketClient();
        return Pair.of(user, wsClient);
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    protected NotificationRequestInfo findNotificationRequest(NotificationRequestId id) throws Exception {
        return doGet("/api/notification/request/" + id, NotificationRequestInfo.class);
    }

    /**
     * 功能：获取通知。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    protected PageData<NotificationRequestInfo> findNotificationRequests() throws Exception {
        PageLink pageLink = new PageLink(10);
        return doGetTypedWithPageLink("/api/notification/requests?", new TypeReference<PageData<NotificationRequestInfo>>() {}, pageLink);
    }

    /**
     * 功能：删除或清理请求。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    protected void deleteNotificationRequest(NotificationRequestId id) throws Exception {
        doDelete("/api/notification/request/" + id);
    }

    /**
     * 功能：获取`My Notifications`。
     * 参数：
     * - `unreadOnly`：`unreadOnly` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    protected List<Notification> getMyNotifications(boolean unreadOnly, int limit) throws Exception {
        return getMyNotifications(NotificationDeliveryMethod.WEB, unreadOnly, limit);
    }

    /**
     * 功能：获取`My Notifications`。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `unreadOnly`：`unreadOnly` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    protected List<Notification> getMyNotifications(NotificationDeliveryMethod deliveryMethod, boolean unreadOnly, int limit) throws Exception {
        return doGetTypedWithPageLink("/api/notifications?unreadOnly={unreadOnly}&deliveryMethod={deliveryMethod}&", new TypeReference<PageData<Notification>>() {},
                new PageLink(limit, 0), unreadOnly, deliveryMethod).getData();
    }

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `triggerConfig`：配置对象。
     * - `subject`：`subject` 参数。
     * - `text`：`text` 参数。
     * - `targets`：`targets` 参数。
     * 返回：处理结果。
     */
    protected NotificationRule createNotificationRule(NotificationRuleTriggerConfig triggerConfig, String subject, String text, NotificationTargetId... targets) {
        return createNotificationRule(triggerConfig, subject, text, List.of(targets), NotificationDeliveryMethod.WEB);
    }

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `triggerConfig`：配置对象。
     * - `subject`：`subject` 参数。
     * - `text`：`text` 参数。
     * - `targets`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected NotificationRule createNotificationRule(NotificationRuleTriggerConfig triggerConfig, String subject, String text, List<NotificationTargetId> targets, NotificationDeliveryMethod... deliveryMethods) {
        NotificationTemplate template = createNotificationTemplate(NotificationType.valueOf(triggerConfig.getTriggerType().toString()), subject, text, deliveryMethods);

        NotificationRule rule = new NotificationRule();
        rule.setName(triggerConfig.getTriggerType() + " " + targets);
        rule.setEnabled(true);
        rule.setTemplateId(template.getId());
        rule.setTriggerType(triggerConfig.getTriggerType());
        rule.setTriggerConfig(triggerConfig);

        DefaultNotificationRuleRecipientsConfig recipientsConfig = new DefaultNotificationRuleRecipientsConfig();
        recipientsConfig.setTriggerType(triggerConfig.getTriggerType());
        recipientsConfig.setTargets(DaoUtil.toUUIDs(targets));
        rule.setRecipientsConfig(recipientsConfig);

        return saveNotificationRule(rule);
    }

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `notificationRule`：`notificationRule` 参数。
     * 返回：处理结果。
     */
    protected NotificationRule saveNotificationRule(NotificationRule notificationRule) {
        return doPost("/api/notification/rule", notificationRule, NotificationRule.class);
    }

    /**
     * 功能：获取通知。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    protected PageData<NotificationRuleInfo> findNotificationRules() throws Exception {
        PageLink pageLink = new PageLink(10);
        return doGetTypedWithPageLink("/api/notification/rules?", new TypeReference<PageData<NotificationRuleInfo>>() {}, pageLink);
    }

    /**
     * 功能：构建客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected NotificationApiWsClient buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        NotificationApiWsClient wsClient = new NotificationApiWsClient(WS_URL + wsPort);
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        wsClient.authenticate(token);
        return wsClient;
    }

    /**
     * 功能：获取客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationApiWsClient getWsClient() {
        return (NotificationApiWsClient) super.getWsClient();
    }

    /**
     * 功能：获取客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationApiWsClient getAnotherWsClient() {
        return (NotificationApiWsClient) super.getAnotherWsClient();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractNotificationApiTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
