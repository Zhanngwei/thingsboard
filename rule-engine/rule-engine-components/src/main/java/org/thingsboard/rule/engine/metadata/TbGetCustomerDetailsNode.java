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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.NoSuchElementException;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "customer details",
        configClazz = TbGetCustomerDetailsNodeConfiguration.class,
        version = 1,
        nodeDescription = "Adds message originator customer details into message or message metadata",
        nodeDetails = "Useful in multi-customer solutions where we need dynamically use customer contact information " +
                "such as email, phone, address, etc., for notifications via email, SMS, and other notification providers.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeEntityDetailsConfig")
/**
 * 中文说明：`TbGetCustomerDetailsNode` 是获取客户详情节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbGetCustomerDetailsNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbGetCustomerDetailsNode extends TbAbstractGetEntityDetailsNode<TbGetCustomerDetailsNodeConfiguration, CustomerId> {

    /**
     * 常量字段：定义 `CUSTOMER_PREFIX`，用于客户名称、客户标识或客户缓存，本身不触发外部系统调用。
     */
    private static final String CUSTOMER_PREFIX = "customer_";

    @Override
    /**
     * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `TbGetCustomerDetailsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected TbGetCustomerDetailsNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetCustomerDetailsNodeConfiguration.class);
        checkIfDetailsListIsNotEmptyOrElseThrow(config.getDetailsList());
        return config;
    }

    @Override
    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbGetCustomerDetailsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected String getPrefix() {
        return CUSTOMER_PREFIX;
    }

    @Override
    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbGetCustomerDetailsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AssetService`, `DeviceService`, `EdgeService`, `EntityViewService`, `UserService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected ListenableFuture<Customer> getContactBasedFuture(TbContext ctx, TbMsg msg) {
        switch (msg.getOriginator().getEntityType()) {
            case DEVICE:
                // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
                return Futures.transformAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId())),
                        device -> getCustomerFuture(ctx, device, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case ASSET:
                // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
                return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(msg.getOriginator().getId())),
                        asset -> getCustomerFuture(ctx, asset, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case ENTITY_VIEW:
                // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
                return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId())),
                        entityView -> getCustomerFuture(ctx, entityView, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case USER:
                // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
                return Futures.transformAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), new UserId(msg.getOriginator().getId())),
                        user -> getCustomerFuture(ctx, user, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case EDGE:
                // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
                return Futures.transformAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(msg.getOriginator().getId())),
                        edge -> getCustomerFuture(ctx, edge, msg.getOriginator()), ctx.getDbCallbackExecutor());
            default:
                return Futures.immediateFailedFuture(new NoSuchElementException("Entity with entityType '" + msg.getOriginator().getEntityType() + "' is not supported."));
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbGetCustomerDetailsNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `CustomerService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Customer> getCustomerFuture(TbContext ctx, HasCustomerId hasCustomerId, EntityId originator) {
        if (hasCustomerId == null) {
            return Futures.immediateFuture(null);
        } else {
            if (hasCustomerId.getCustomerId() == null || hasCustomerId.getCustomerId().isNullUid()) {
                if (hasCustomerId instanceof HasName) {
                    var hasName = (HasName) hasCustomerId;
                    throw new RuntimeException(originator.getEntityType().getNormalName() + " with name '" + hasName.getName() + "' is not assigned to Customer!");
                }
                throw new RuntimeException(originator.getEntityType().getNormalName() + " with id '" + originator + "' is not assigned to Customer!");
            } else {
                // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
                return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), hasCustomerId.getCustomerId());
            }
        }
    }

    @Override
    /**
     * 方法说明：迁移旧版本规则节点 JSON 配置结构。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ?
                upgradeRuleNodesWithOldPropertyToUseFetchTo(
                        oldConfiguration,
                        "addToMetadata",
                        TbMsgSource.METADATA.name(),
                        TbMsgSource.DATA.name()) :
                new TbPair<>(false, oldConfiguration);
    }

    /*
     * 本类总结：`TbGetCustomerDetailsNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
