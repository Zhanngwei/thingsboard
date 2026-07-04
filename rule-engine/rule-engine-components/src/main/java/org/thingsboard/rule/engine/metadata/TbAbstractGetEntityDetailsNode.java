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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.ContactBasedEntityDetails;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
/**
 * 中文说明：`TbAbstractGetEntityDetailsNode` 是抽象获取实体详情节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbAbstractGetEntityDetailsNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public abstract class TbAbstractGetEntityDetailsNode<C extends TbAbstractGetEntityDetailsNodeConfiguration, I extends UUIDBased> extends TbAbstractNodeWithFetchTo<C> {

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) {
        var msgDataAsObjectNode = TbMsgSource.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        // 异步回调用于把服务或转换结果映射为规则链成功/失败关系。
        withCallback(getDetails(ctx, msg, msgDataAsObjectNode),
                ctx::tellSuccess,
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractGetEntityDetailsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract String getPrefix();

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractGetEntityDetailsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract ListenableFuture<? extends ContactBased<I>> getContactBasedFuture(TbContext ctx, TbMsg msg);

    /**
     * 方法说明：检查状态、关系、配置或数据合法性，供 `TbAbstractGetEntityDetailsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected void checkIfDetailsListIsNotEmptyOrElseThrow(List<ContactBasedEntityDetails> detailsList) throws TbNodeException {
        if (detailsList == null || detailsList.isEmpty()) {
            throw new TbNodeException("At least one entity detail should be selected!");
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractGetEntityDetailsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<TbMsg> getDetails(TbContext ctx, TbMsg msg, ObjectNode messageData) {
        ListenableFuture<? extends ContactBased<I>> contactBasedFuture = getContactBasedFuture(ctx, msg);
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(contactBasedFuture, contactBased -> {
            if (contactBased == null) {
                return Futures.immediateFuture(msg);
            }
            var msgMetaData = msg.getMetaData().copy();
            fetchEntityDetailsToMsg(contactBased, messageData, msgMetaData);
            return Futures.immediateFuture(transformMessage(msg, messageData, msgMetaData));
        }, MoreExecutors.directExecutor());
    }

    /**
     * 方法说明：从消息或服务层获取需要补充的数据，供 `TbAbstractGetEntityDetailsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void fetchEntityDetailsToMsg(ContactBased<I> contactBased, ObjectNode messageData, TbMsgMetaData msgMetaData) {
        String value = null;
        for (var entityDetail : config.getDetailsList()) {
            switch (entityDetail) {
                case ID:
                    value = contactBased.getId().getId().toString();
                    break;
                case TITLE:
                    value = contactBased.getName();
                    break;
                case ADDRESS:
                    value = contactBased.getAddress();
                    break;
                case ADDRESS2:
                    value = contactBased.getAddress2();
                    break;
                case CITY:
                    value = contactBased.getCity();
                    break;
                case COUNTRY:
                    value = contactBased.getCountry();
                    break;
                case STATE:
                    value = contactBased.getState();
                    break;
                case EMAIL:
                    value = contactBased.getEmail();
                    break;
                case PHONE:
                    value = contactBased.getPhone();
                    break;
                case ZIP:
                    value = contactBased.getZip();
                    break;
                case ADDITIONAL_INFO:
                    if (contactBased.getAdditionalInfo().hasNonNull("description")) {
                        value = contactBased.getAdditionalInfo().get("description").asText();
                    }
                    break;
            }
            if (value == null) {
                continue;
            }
            setDetail(entityDetail.getRuleEngineName(), value, messageData, msgMetaData);
        }
    }

    /**
     * 方法说明：写入本地对象字段或构造输出数据，供 `TbAbstractGetEntityDetailsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void setDetail(String property, String value, ObjectNode messageData, TbMsgMetaData msgMetaData) {
        String fieldName = getPrefix() + property;
        if (TbMsgSource.METADATA.equals(fetchTo)) {
            msgMetaData.putValue(fieldName, value);
        } else if (TbMsgSource.DATA.equals(fetchTo)) {
            messageData.put(fieldName, value);
        }
    }

    /*
     * 本类总结：`TbAbstractGetEntityDetailsNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
