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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.objects.AttributesEntityView;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.adaptor.JsonConverter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.msg.TbMsgType.ACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_DELETED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_UPDATED;
import static org.thingsboard.server.common.data.msg.TbMsgType.INACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "copy to view",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Copy attributes from asset/device to entity view and changes message originator to related entity view",
        nodeDetails = "Copy attributes from asset/device to related entity view according to entity view configuration. \n " +
                "Copy will be done only for attributes that are between start and end dates and according to attribute keys configuration. \n" +
                "Changes message originator to related entity view and produces new messages according to count of updated entity views",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig",
        icon = "content_copy"
)
/**
 * 中文说明：`TbCopyAttributesToEntityViewNode` 是复制属性到实体View节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`EmptyNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbCopyAttributesToEntityViewNode implements TbNode {

    EmptyNodeConfiguration config;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `EntityViewService`, `TelemetryService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.isTypeOneOf(ATTRIBUTES_UPDATED, ATTRIBUTES_DELETED,
                ACTIVITY_EVENT, INACTIVITY_EVENT, POST_ATTRIBUTES_REQUEST)) {
            if (!msg.getMetaData().getData().isEmpty()) {
                long now = System.currentTimeMillis();
                String scope = msg.isTypeOf(POST_ATTRIBUTES_REQUEST) ?
                        DataConstants.CLIENT_SCOPE : msg.getMetaData().getValue(DataConstants.SCOPE);

                ListenableFuture<List<EntityView>> entityViewsFuture =
                        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
                        ctx.getEntityViewService().findEntityViewsByTenantIdAndEntityIdAsync(ctx.getTenantId(), msg.getOriginator());

                // 异步回调用于把服务或转换结果映射为规则链成功/失败关系。
                DonAsynchron.withCallback(entityViewsFuture,
                        entityViews -> {
                            for (EntityView entityView : entityViews) {
                                long startTime = entityView.getStartTimeMs();
                                long endTime = entityView.getEndTimeMs();
                                if ((endTime != 0 && endTime > now && startTime < now) || (endTime == 0 && startTime < now)) {
                                    if (msg.isTypeOf(ATTRIBUTES_DELETED)) {
                                        List<String> attributes = new ArrayList<>();
                                        for (JsonElement element : JsonParser.parseString(msg.getData()).getAsJsonObject().get("attributes").getAsJsonArray()) {
                                            if (element.isJsonPrimitive()) {
                                                JsonPrimitive value = element.getAsJsonPrimitive();
                                                if (value.isString()) {
                                                    attributes.add(value.getAsString());
                                                }
                                            }
                                        }
                                        List<String> filteredAttributes =
                                                attributes.stream().filter(attr -> attributeContainsInEntityView(scope, attr, entityView)).collect(Collectors.toList());
                                        if (!filteredAttributes.isEmpty()) {
                                            // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
                                            ctx.getTelemetryService().deleteAndNotify(ctx.getTenantId(), entityView.getId(), scope, filteredAttributes,
                                                    getFutureCallback(ctx, msg, entityView));
                                        }
                                    } else {
                                        Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(JsonParser.parseString(msg.getData()));
                                        List<AttributeKvEntry> filteredAttributes =
                                                attributes.stream().filter(attr -> attributeContainsInEntityView(scope, attr.getKey(), entityView)).collect(Collectors.toList());
                                        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
                                        ctx.getTelemetryService().saveAndNotify(ctx.getTenantId(), entityView.getId(), scope, filteredAttributes,
                                                getFutureCallback(ctx, msg, entityView));
                                    }
                                }
                            }
                            ctx.ack(msg);
                        },
                        t -> ctx.tellFailure(msg, t));
            } else {
                ctx.tellFailure(msg, new IllegalArgumentException("Message metadata is empty"));
            }
        } else {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type [" + msg.getType() + "]"));
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbCopyAttributesToEntityViewNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private FutureCallback<Void> getFutureCallback(TbContext ctx, TbMsg msg, EntityView entityView) {
        return new FutureCallback<Void>() {
            @Override
            /**
             * 方法说明：处理异步调用成功回调并继续规则链投递。
             * 调用边界：由异步 Future 或消息回调触发；本方法本身只衔接规则链结果，数据库、缓存、MQTT 或事务通常发生在触发该回调的上游调用链中。
             */
            public void onSuccess(@Nullable Void result) {
                transformAndTellNext(ctx, msg, entityView);
            }

            @Override
            /**
             * 方法说明：处理异步调用失败回调并转入失败关系。
             * 调用边界：由异步 Future 或消息回调触发；本方法本身只衔接规则链结果，数据库、缓存、MQTT 或事务通常发生在触发该回调的上游调用链中。
             */
            public void onFailure(Throwable t) {
                ctx.tellFailure(msg, t);
            }
        };
    }

    /**
     * 方法说明：转换规则链消息或转换异步处理结果，供 `TbCopyAttributesToEntityViewNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void transformAndTellNext(TbContext ctx, TbMsg msg, EntityView entityView) {
        // 通过规则引擎上下文安排后续消息投递或自身定时消息。
        ctx.enqueueForTellNext(ctx.newMsg(msg.getQueueName(), msg.getType(), entityView.getId(), msg.getCustomerId(), msg.getMetaData(), msg.getData()), SUCCESS);
    }

    /**
     * 方法说明：执行 `attributeContainsInEntityView` 对应的辅助逻辑，供 `TbCopyAttributesToEntityViewNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private boolean attributeContainsInEntityView(String scope, String attrKey, EntityView entityView) {
        AttributesEntityView attributesEntityView = entityView.getKeys().getAttributes();
        List<String> keys = null;
        switch (scope) {
            case DataConstants.CLIENT_SCOPE:
                keys = attributesEntityView.getCs();
                break;
            case DataConstants.SERVER_SCOPE:
                keys = attributesEntityView.getSs();
                break;
            case DataConstants.SHARED_SCOPE:
                keys = attributesEntityView.getSh();
                break;
        }
        return CollectionsUtil.contains(keys, attrKey);
    }

    /*
     * 本类总结：`TbCopyAttributesToEntityViewNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
