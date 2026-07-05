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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntityContainer;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.user.UserService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.FAILURE;
import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.SUCCESS;

/**
 * 中文说明：`TbAbstractRelationActionNode` 是抽象关系Action节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbAbstractRelationActionNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
public abstract class TbAbstractRelationActionNode<C extends TbAbstractRelationActionNodeConfiguration> implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    protected C config;

    private LoadingCache<EntityKey, EntityContainer> entityIdCache;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadEntityNodeActionConfig(configuration);
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (this.config.getEntityCacheExpiration() > 0) {
            cacheBuilder.expireAfterWrite(this.config.getEntityCacheExpiration(), TimeUnit.SECONDS);
        }
        entityIdCache = cacheBuilder.build(new EntityCacheLoader(ctx, createEntityIfNotExists()));
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String relationType = processPattern(msg, config.getRelationType());
        withCallback(processEntityRelationAction(ctx, msg, relationType),
                filterResult -> ctx.tellNext(filterResult.getMsg(), filterResult.isResult() ? SUCCESS : FAILURE), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (entityIdCache != null) {
            entityIdCache.invalidateAll();
        }
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `relationType`：类型。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<RelationContainer> processEntityRelationAction(TbContext ctx, TbMsg msg, String relationType) {
        return Futures.transformAsync(getEntity(ctx, msg), entityContainer -> doProcessEntityRelationAction(ctx, msg, entityContainer, relationType), ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：保存或创建实体。
     * 参数：无。
     * 返回：判断结果。
     */
    protected abstract boolean createEntityIfNotExists();

    /**
     * 功能：执行 `doProcessEntityRelationAction` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `entityContainer`：实体对象。
     * - `relationType`：类型。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<RelationContainer> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entityContainer, String relationType);

    /**
     * 功能：获取实体。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    protected abstract C loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException;

    /**
     * 功能：获取实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<EntityContainer> getEntity(TbContext ctx, TbMsg msg) {
        String entityName = processPattern(msg, this.config.getEntityNamePattern());
        String type;
        if (this.config.getEntityTypePattern() != null) {
            type = processPattern(msg, this.config.getEntityTypePattern());
        } else {
            type = null;
        }
        EntityType entityType = EntityType.valueOf(this.config.getEntityType());
        EntityKey key = new EntityKey(entityName, type, entityType);
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            EntityContainer entityContainer = entityIdCache.get(key);
            if (entityContainer.getEntityId() == null) {
                throw new RuntimeException("No entity found with type '" + key.getEntityType() + "' and name '" + key.getEntityName() + "'.");
            }
            return entityContainer;
        });
    }

    /**
     * 功能：处理`Single Search Direction`。
     * 参数：
     * - `msg`：待处理消息。
     * - `entityContainer`：实体对象。
     * 返回：处理结果。
     */
    protected SearchDirectionIds processSingleSearchDirection(TbMsg msg, EntityContainer entityContainer) {
        SearchDirectionIds searchDirectionIds = new SearchDirectionIds();
        if (EntitySearchDirection.FROM.name().equals(this.config.getDirection())) {
            searchDirectionIds.setFromId(EntityIdFactory.getByTypeAndId(entityContainer.getEntityType().name(), entityContainer.getEntityId().toString()));
            searchDirectionIds.setToId(msg.getOriginator());
            searchDirectionIds.setOriginatorDirectionFrom(false);
        } else {
            searchDirectionIds.setToId(EntityIdFactory.getByTypeAndId(entityContainer.getEntityType().name(), entityContainer.getEntityId().toString()));
            searchDirectionIds.setFromId(msg.getOriginator());
            searchDirectionIds.setOriginatorDirectionFrom(true);
        }
        return searchDirectionIds;
    }

    /**
     * 功能：处理`List Search Direction`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<List<EntityRelation>> processListSearchDirection(TbContext ctx, TbMsg msg) {
        if (EntitySearchDirection.FROM.name().equals(this.config.getDirection())) {
            return ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), processPattern(msg, this.config.getRelationType()), RelationTypeGroup.COMMON);
        } else {
            return ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), processPattern(msg, this.config.getRelationType()), RelationTypeGroup.COMMON);
        }
    }

    /**
     * 功能：处理`Pattern`。
     * 参数：
     * - `msg`：待处理消息。
     * - `pattern`：`pattern` 参数。
     * 返回：文本结果。
     */
    protected String processPattern(TbMsg msg, String pattern) {
        return TbNodeUtils.processPattern(pattern, msg);
    }

    /**
     * 中文说明：`EntityKey` 是实体键辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    @Data
    @AllArgsConstructor
    private static class EntityKey {
        /**
         * 实体，用于标识或展示当前对象。
         */
        private String entityName;
        /**
         * 类型，用于区分不同处理分支。
         */
        private String type;
        /**
         * 实体，用于区分不同处理分支。
         */
        private EntityType entityType;
    }

    /**
     * 中文说明：`SearchDirectionIds` 是SearchDirectionIds辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    @Data
    protected static class SearchDirectionIds {
        /**
         * `fromId`ID，用于定位对应业务对象。
         */
        private EntityId fromId;
        /**
         * `toId`ID，用于定位对应业务对象。
         */
        private EntityId toId;
        /**
         * 是否满足`originatorDirectionFrom`条件。
         */
        private boolean originatorDirectionFrom;
    }

    /**
     * 中文说明：`EntityCacheLoader` 是实体CacheLoader辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    private static class EntityCacheLoader extends CacheLoader<EntityKey, EntityContainer> {

        /**
         * 上下文，汇总当前处理所需的上下文信息。
         */
        private final TbContext ctx;
        /**
         * 是否满足`createIfNotExists`条件。
         */
        private final boolean createIfNotExists;

        /**
         * 功能：创建 `TbAbstractRelationActionNode` 实例，并初始化必要字段。
         * 参数：
         * - `ctx`：处理上下文。
         * - `createIfNotExists`：`createIfNotExists` 参数。
         * 返回：新创建的对象实例。
         */
        private EntityCacheLoader(TbContext ctx, boolean createIfNotExists) {
            this.ctx = ctx;
            this.createIfNotExists = createIfNotExists;
        }

        /**
         * 功能：执行 `load` 对应的处理。
         * 参数：
         * - `key`：键。
         * 返回：处理结果。
         */
        @Override
        public EntityContainer load(EntityKey key) {
            return loadEntity(key);
        }

        /**
         * 功能：获取实体。
         * 参数：
         * - `entitykey`：实体对象。
         * 返回：处理结果。
         */
        private EntityContainer loadEntity(EntityKey entitykey) {
            EntityType type = entitykey.getEntityType();
            EntityContainer targetEntity = new EntityContainer();
            targetEntity.setEntityType(type);
            switch (type) {
                case DEVICE:
                    DeviceService deviceService = ctx.getDeviceService();
                    Device device = deviceService.findDeviceByTenantIdAndName(ctx.getTenantId(), entitykey.getEntityName());
                    if (device != null) {
                        targetEntity.setEntityId(device.getId());
                    } else if (createIfNotExists) {
                        Device newDevice = new Device();
                        newDevice.setName(entitykey.getEntityName());
                        newDevice.setType(entitykey.getType());
                        newDevice.setTenantId(ctx.getTenantId());
                        Device savedDevice = deviceService.saveDevice(newDevice);
                        ctx.getClusterService().onDeviceUpdated(savedDevice, null);
                        ctx.enqueue(ctx.deviceCreatedMsg(savedDevice, ctx.getSelfId()),
                                () -> log.trace("Pushed Device Created message: {}", savedDevice),
                                throwable -> log.warn("Failed to push Device Created message: {}", savedDevice, throwable));
                        targetEntity.setEntityId(savedDevice.getId());
                    }
                    break;
                case ASSET:
                    AssetService assetService = ctx.getAssetService();
                    Asset asset = assetService.findAssetByTenantIdAndName(ctx.getTenantId(), entitykey.getEntityName());
                    if (asset != null) {
                        targetEntity.setEntityId(asset.getId());
                    } else if (createIfNotExists) {
                        Asset newAsset = new Asset();
                        newAsset.setName(entitykey.getEntityName());
                        newAsset.setType(entitykey.getType());
                        newAsset.setTenantId(ctx.getTenantId());
                        Asset savedAsset = assetService.saveAsset(newAsset);
                        ctx.enqueue(ctx.assetCreatedMsg(savedAsset, ctx.getSelfId()),
                                () -> log.trace("Pushed Asset Created message: {}", savedAsset),
                                throwable -> log.warn("Failed to push Asset Created message: {}", savedAsset, throwable));
                        targetEntity.setEntityId(savedAsset.getId());
                    }
                    break;
                case CUSTOMER:
                    CustomerService customerService = ctx.getCustomerService();
                    Optional<Customer> customerOptional = customerService.findCustomerByTenantIdAndTitle(ctx.getTenantId(), entitykey.getEntityName());
                    if (customerOptional.isPresent()) {
                        targetEntity.setEntityId(customerOptional.get().getId());
                    } else if (createIfNotExists) {
                        Customer newCustomer = new Customer();
                        newCustomer.setTitle(entitykey.getEntityName());
                        newCustomer.setTenantId(ctx.getTenantId());
                        Customer savedCustomer = customerService.saveCustomer(newCustomer);
                        ctx.enqueue(ctx.customerCreatedMsg(savedCustomer, ctx.getSelfId()),
                                () -> log.trace("Pushed Customer Created message: {}", savedCustomer),
                                throwable -> log.warn("Failed to push Customer Created message: {}", savedCustomer, throwable));
                        targetEntity.setEntityId(savedCustomer.getId());
                    }
                    break;
                case TENANT:
                    targetEntity.setEntityId(ctx.getTenantId());
                    break;
                case ENTITY_VIEW:
                    EntityViewService entityViewService = ctx.getEntityViewService();
                    EntityView entityView = entityViewService.findEntityViewByTenantIdAndName(ctx.getTenantId(), entitykey.getEntityName());
                    if (entityView != null) {
                        targetEntity.setEntityId(entityView.getId());
                    }
                    break;
                case EDGE:
                    EdgeService edgeService = ctx.getEdgeService();
                    Edge edge = edgeService.findEdgeByTenantIdAndName(ctx.getTenantId(), entitykey.getEntityName());
                    if (edge != null) {
                        targetEntity.setEntityId(edge.getId());
                    }
                    break;
                case DASHBOARD:
                    DashboardService dashboardService = ctx.getDashboardService();
                    DashboardInfo dashboardInfo = dashboardService.findFirstDashboardInfoByTenantIdAndName(ctx.getTenantId(), entitykey.getEntityName());
                    if (dashboardInfo != null) {
                        targetEntity.setEntityId(dashboardInfo.getId());
                    }
                    break;
                case USER:
                    UserService userService = ctx.getUserService();
                    User user = userService.findUserByTenantIdAndEmail(ctx.getTenantId(), entitykey.getEntityName());
                    if (user != null) {
                        targetEntity.setEntityId(user.getId());
                    }
                    break;
                default:
                    return targetEntity;
            }
            return targetEntity;
        }
    }

    /**
     * 中文说明：`RelationContainer` 是关系Container辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    protected static class RelationContainer {

        /**
         * 消息，承载当前步骤需要处理的内容。
         */
        private TbMsg msg;
        /**
         * 是否满足`result`条件。
         */
        private boolean result;

    }


    /*
     * 本类总结：`TbAbstractRelationActionNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
