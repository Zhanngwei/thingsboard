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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntityContainer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create relation",
        configClazz = TbCreateRelationNodeConfiguration.class,
        nodeDescription = "Finds target Entity by entity name pattern and (entity type pattern for Asset, Device) and then create a relation to Originator Entity by type and direction." +
                " If Selected entity type: Asset, Device or Customer will create new Entity if it doesn't exist and selected checkbox 'Create new entity if not exists'.<br>" +
                " In case that relation from the message originator to the selected entity not exist and  If selected checkbox 'Remove current relations'," +
                " before creating the new relation all existed relations to message originator by type and direction will be removed.<br>" +
                " If relation from the message originator to the selected entity created and If selected checkbox 'Change originator to related entity'," +
                " outbound message will be processed as a message from this entity.",
        nodeDetails = "If the relation already exists or successfully created -  Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateRelationConfig",
        icon = "add_circle"
)
/**
 * 中文说明：`TbCreateRelationNode` 是创建关系节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbCreateRelationNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbCreateRelationNode extends TbAbstractRelationActionNode<TbCreateRelationNodeConfiguration> {

    @Override
    /**
     * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected TbCreateRelationNodeConfiguration loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbCreateRelationNodeConfiguration.class);
    }

    @Override
    /**
     * 方法说明：创建实体、告警、关系或辅助对象，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected boolean createEntityIfNotExists() {
        return config.isCreateEntityIfNotExists();
    }

    @Override
    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected ListenableFuture<RelationContainer> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entity, String relationType) {
        ListenableFuture<Boolean> future = createRelationIfAbsent(ctx, msg, entity, relationType);
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
        return Futures.transform(future, result -> {
            if (result && config.isChangeOriginatorToRelatedEntity()) {
                TbMsg tbMsg = ctx.transformMsgOriginator(msg, entity.getEntityId());
                return new RelationContainer(tbMsg, result);
            }
            return new RelationContainer(msg, result);
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：创建实体、告警、关系或辅助对象，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> createRelationIfAbsent(TbContext ctx, TbMsg msg, EntityContainer entityContainer, String relationType) {
        SearchDirectionIds sdId = processSingleSearchDirection(msg, entityContainer);
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(deleteCurrentRelationsIfNeeded(ctx, msg, sdId, relationType), v ->
                        checkRelationAndCreateIfAbsent(ctx, entityContainer, relationType, sdId),
                ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：通过服务层删除属性、关系或状态数据，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Void> deleteCurrentRelationsIfNeeded(TbContext ctx, TbMsg msg, SearchDirectionIds sdId, String relationType) {
        if (config.isRemoveCurrentRelations()) {
            return deleteOriginatorRelations(ctx, findOriginatorRelations(ctx, msg, sdId, relationType));
        }
        return Futures.immediateFuture(null);
    }

    /**
     * 方法说明：按租户、实体或关系条件查询数据，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `RelationService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<List<EntityRelation>> findOriginatorRelations(TbContext ctx, TbMsg msg, SearchDirectionIds sdId, String relationType) {
        if (sdId.isOriginatorDirectionFrom()) {
            // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
            return ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), relationType, RelationTypeGroup.COMMON);
        } else {
            // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
            return ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), relationType, RelationTypeGroup.COMMON);
        }
    }

    /**
     * 方法说明：通过服务层删除属性、关系或状态数据，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `RelationService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Void> deleteOriginatorRelations(TbContext ctx, ListenableFuture<List<EntityRelation>> originatorRelationsFuture) {
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(originatorRelationsFuture, originatorRelations -> {
            List<ListenableFuture<Boolean>> list = new ArrayList<>();
            if (!CollectionUtils.isEmpty(originatorRelations)) {
                for (EntityRelation relation : originatorRelations) {
                    // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
                    list.add(ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), relation));
                }
            }
            // 异步聚合或转换服务返回值，完成后再继续规则链处理。
            return Futures.transform(Futures.allAsList(list), result -> null, ctx.getDbCallbackExecutor());
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：检查状态、关系、配置或数据合法性，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> checkRelationAndCreateIfAbsent(TbContext ctx, EntityContainer entityContainer, String relationType, SearchDirectionIds sdId) {
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(checkRelation(ctx, sdId, relationType), relationPresent -> {
            if (relationPresent) {
                return Futures.immediateFuture(true);
            }
            return processCreateRelation(ctx, entityContainer, sdId, relationType);
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：检查状态、关系、配置或数据合法性，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `RelationService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> checkRelation(TbContext ctx, SearchDirectionIds sdId, String relationType) {
        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
        return ctx.getRelationService().checkRelationAsync(ctx.getTenantId(), sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON);
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> processCreateRelation(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        switch (entityContainer.getEntityType()) {
            case ASSET:
                return processAsset(ctx, entityContainer, sdId, relationType);
            case DEVICE:
                return processDevice(ctx, entityContainer, sdId, relationType);
            case CUSTOMER:
                return processCustomer(ctx, entityContainer, sdId, relationType);
            case DASHBOARD:
                return processDashboard(ctx, entityContainer, sdId, relationType);
            case ENTITY_VIEW:
                return processView(ctx, entityContainer, sdId, relationType);
            case EDGE:
                return processEdge(ctx, entityContainer, sdId, relationType);
            case TENANT:
                return processTenant(ctx, entityContainer, sdId, relationType);
            case USER:
                return processUser(ctx, entityContainer, sdId, relationType);
        }
        return Futures.immediateFuture(true);
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `EntityViewService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> processView(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(entityContainer.getEntityId().getId())), entityView -> {
            if (entityView != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `EdgeService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> processEdge(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(entityContainer.getEntityId().getId())), edge -> {
            if (edge != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `DeviceService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> processDevice(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
        Device device = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), new DeviceId(entityContainer.getEntityId().getId()));
        if (device != null) {
            return processSave(ctx, sdId, relationType);
        } else {
            return Futures.immediateFuture(true);
        }
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AssetService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> processAsset(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(entityContainer.getEntityId().getId())), asset -> {
            if (asset != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `CustomerService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> processCustomer(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), new CustomerId(entityContainer.getEntityId().getId())), customer -> {
            if (customer != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `DashboardService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> processDashboard(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(ctx.getDashboardService().findDashboardByIdAsync(ctx.getTenantId(), new DashboardId(entityContainer.getEntityId().getId())), dashboard -> {
            if (dashboard != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `TenantService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> processTenant(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), TenantId.fromUUID(entityContainer.getEntityId().getId())), tenant -> {
            if (tenant != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `UserService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> processUser(TbContext ctx, EntityContainer entityContainer, SearchDirectionIds sdId, String relationType) {
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), new UserId(entityContainer.getEntityId().getId())), user -> {
            if (user != null) {
                return processSave(ctx, sdId, relationType);
            } else {
                return Futures.immediateFuture(true);
            }
        }, ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbCreateRelationNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `RelationService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Boolean> processSave(TbContext ctx, SearchDirectionIds sdId, String relationType) {
        // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
        return ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), new EntityRelation(sdId.getFromId(), sdId.getToId(), relationType, RelationTypeGroup.COMMON));
    }

    /*
     * 本类总结：`TbCreateRelationNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
