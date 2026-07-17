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

/**
 * `TbNotificationNode` 类，封装当前模块中的一组相关职责。
 */
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
public class TbNotificationNode extends TbAbstractExternalNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbNotificationNodeConfiguration config;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbNotificationNodeConfiguration.class);
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
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
             * 功能：处理`on Success`。
             * 参数：
             * - `stats`：`stats` 参数。
             * 返回：无。
             */
            @Override
            public void onSuccess(NotificationRequestStats stats) {
                TbMsgMetaData metaData = tbMsg.getMetaData().copy();
                metaData.putValue("notificationRequestResult", JacksonUtil.toString(stats));
                tellSuccess(ctx, TbMsg.transformMsgMetadata(tbMsg, metaData));
            }

            /**
             * 功能：处理失败信息。
             * 参数：
             * - `e`：`e` 参数。
             * 返回：无。
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
