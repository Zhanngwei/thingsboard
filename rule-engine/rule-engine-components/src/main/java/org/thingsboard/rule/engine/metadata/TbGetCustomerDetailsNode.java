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

/**
 * 中文说明：
 * 1. `TbGetCustomerDetailsNode` 是 ThingsBoard Rule Engine Components 中处理客户的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractGetEntityDetailsNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
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
public class TbGetCustomerDetailsNode extends TbAbstractGetEntityDetailsNode<TbGetCustomerDetailsNodeConfiguration, CustomerId> {

    /**
     * 客户常量，用于统一引用固定值。
     */
    private static final String CUSTOMER_PREFIX = "customer_";

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    protected TbGetCustomerDetailsNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetCustomerDetailsNodeConfiguration.class);
        checkIfDetailsListIsNotEmptyOrElseThrow(config.getDetailsList());
        return config;
    }

    /**
     * 功能：获取`Prefix`。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getPrefix() {
        return CUSTOMER_PREFIX;
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<Customer> getContactBasedFuture(TbContext ctx, TbMsg msg) {
        switch (msg.getOriginator().getEntityType()) {
            case DEVICE:
                return Futures.transformAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId())),
                        device -> getCustomerFuture(ctx, device, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case ASSET:
                return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(msg.getOriginator().getId())),
                        asset -> getCustomerFuture(ctx, asset, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case ENTITY_VIEW:
                return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId())),
                        entityView -> getCustomerFuture(ctx, entityView, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case USER:
                return Futures.transformAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), new UserId(msg.getOriginator().getId())),
                        user -> getCustomerFuture(ctx, user, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case EDGE:
                return Futures.transformAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(msg.getOriginator().getId())),
                        edge -> getCustomerFuture(ctx, edge, msg.getOriginator()), ctx.getDbCallbackExecutor());
            default:
                return Futures.immediateFailedFuture(new NoSuchElementException("Entity with entityType '" + msg.getOriginator().getEntityType() + "' is not supported."));
        }
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `ctx`：处理上下文。
     * - `hasCustomerId`：客户IDID。
     * - `originator`：`originator` 参数。
     * 返回：匹配的数据集合。
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
                return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), hasCustomerId.getCustomerId());
            }
        }
    }

    /**
     * 功能：执行 `upgrade` 对应的处理。
     * 参数：
     * - `fromVersion`：`fromVersion` 参数。
     * - `oldConfiguration`：配置对象。
     * 返回：处理结果。
     */
    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ?
                upgradeRuleNodesWithOldPropertyToUseFetchTo(
                        oldConfiguration,
                        "addToMetadata",
                        TbMsgSource.METADATA.name(),
                        TbMsgSource.DATA.name()) :
                new TbPair<>(false, oldConfiguration);
    }
}
