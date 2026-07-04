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
package org.thingsboard.rule.engine.notification;

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.info.RuleEngineOriginatedNotificationInfo;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.ExecutionException;

@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send notification",
        configClazz = TbNotificationNodeConfiguration.class,
        nodeDescription = "Sends notification to targets using the template",
        nodeDetails = "Will send notification to the specified targets using the template",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeNotificationConfig",
        icon = "notifications"
)
/**
 * ThingsBoard 通知发送外部节点，构造通知请求并委托 NotificationCenter 处理。
 * 本类不直接访问数据库或缓存；通知中心服务的具体实现/调用链可能间接涉及模板、目标或发送状态存储。
 */
public class TbNotificationNode extends TbAbstractExternalNode {

    /**
     * 通知节点配置，包含目标列表和通知模板 ID。
     */
    private TbNotificationNodeConfiguration config;

    /**
     * 初始化通知节点配置。
     * 本方法本身不直接调用外部通知渠道，也不直接访问数据库或缓存。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbNotificationNodeConfiguration.class);
    }

    /**
     * 构造通知请求并异步交给 NotificationCenter。
     * 消息先经 ackIfNeeded 处理确认关系；NotificationCenter 的 callback 决定 Success/Failure。
     * 本方法本身不直接调用邮件、短信、Slack 等外部渠道，具体外部调用边界在通知中心服务内部。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        RuleEngineOriginatedNotificationInfo notificationInfo = RuleEngineOriginatedNotificationInfo.builder()
                .msgOriginator(msg.getOriginator())
                .msgCustomerId(msg.getOriginator().getEntityType() == EntityType.CUSTOMER
                        && msg.getOriginator().equals(msg.getCustomerId()) ? null : msg.getCustomerId())
                .msgMetadata(msg.getMetaData().getData())
                .msgData(JacksonUtil.toFlatMap(JacksonUtil.toJsonNode(msg.getData())))
                .msgType(msg.getType())
                .build();

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(ctx.getTenantId())
                .targets(config.getTargets())
                .templateId(config.getTemplateId())
                .info(notificationInfo)
                .additionalConfig(new NotificationRequestConfig())
                .originatorEntityId(ctx.getSelf().getRuleChainId())
                .build();

        var tbMsg = ackIfNeeded(ctx, msg);

        var callback = new FutureCallback<NotificationRequestStats>() {
            /**
             * NotificationCenter 成功处理后的回调，将统计结果写入元数据并走 Success。
             * 回调线程由通知执行器或服务实现决定，本方法不直接访问数据库或缓存。
             */
            @Override
            public void onSuccess(NotificationRequestStats stats) {
                TbMsgMetaData metaData = tbMsg.getMetaData().copy();
                metaData.putValue("notificationRequestResult", JacksonUtil.toString(stats));
                tellSuccess(ctx, TbMsg.transformMsgMetadata(tbMsg, metaData));
            }

            /**
             * NotificationCenter 处理失败后的回调，直接走 Failure。
             * 失败原因可能来自模板、目标、渠道或服务内部调用链。
             */
            @Override
            public void onFailure(Throwable e) {
                tellFailure(ctx, tbMsg, e);
            }
        };

        var future = ctx.getNotificationExecutor().executeAsync(() ->
                ctx.getNotificationCenter().processNotificationRequest(ctx.getTenantId(), notificationRequest, callback));
        DonAsynchron.withCallback(future, r -> {}, callback::onFailure);
    }

}

/*
 * 本类总结：
 * 本类把 Rule Engine 消息转换为 NotificationRequest，并通过通知执行器委托 NotificationCenter 处理。
 * 节点本身只负责请求构造、消息确认和回调路由；数据库、缓存和具体外部渠道发送只可能在 NotificationCenter 调用链中间接涉及。
 */
