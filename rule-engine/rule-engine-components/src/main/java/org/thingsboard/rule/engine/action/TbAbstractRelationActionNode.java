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

@Slf4j
/**
 * 中文说明：`TbAbstractRelationActionNode` 是抽象关系Action节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbAbstractRelationActionNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public abstract class TbAbstractRelationActionNode<C extends TbAbstractRelationActionNodeConfiguration> implements TbNode {

    /**
     * 字段说明：保存从规则节点 JSON 转换得到的配置对象，供消息处理和生命周期方法复用。
     */
    protected C config;

    private LoadingCache<EntityKey, EntityContainer> entityIdCache;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadEntityNodeActionConfig(configuration);
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (this.config.getEntityCacheExpiration() > 0) {
            cacheBuilder.expireAfterWrite(this.config.getEntityCacheExpiration(), TimeUnit.SECONDS);
        }
        entityIdCache = cacheBuilder.build(new EntityCacheLoader(ctx, createEntityIfNotExists()));
    }

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) {
        String relationType = processPattern(msg, config.getRelationType());
        // 异步回调用于把服务或转换结果映射为规则链成功/失败关系。
        withCallback(processEntityRelationAction(ctx, msg, relationType),
                filterResult -> ctx.tellNext(filterResult.getMsg(), filterResult.isResult() ? SUCCESS : FAILURE), t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    @Override
    /**
     * 方法说明：在节点生命周期销毁阶段释放缓存、脚本引擎、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void destroy() {
        if (entityIdCache != null) {
            entityIdCache.invalidateAll();
        }
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbAbstractRelationActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected ListenableFuture<RelationContainer> processEntityRelationAction(TbContext ctx, TbMsg msg, String relationType) {
        // 异步串联后续服务调用，避免阻塞当前规则节点处理线程。
        return Futures.transformAsync(getEntity(ctx, msg), entityContainer -> doProcessEntityRelationAction(ctx, msg, entityContainer, relationType), ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：创建实体、告警、关系或辅助对象，供 `TbAbstractRelationActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract boolean createEntityIfNotExists();

    /**
     * 方法说明：执行本类核心处理流程，供 `TbAbstractRelationActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract ListenableFuture<RelationContainer> doProcessEntityRelationAction(TbContext ctx, TbMsg msg, EntityContainer entityContainer, String relationType);

    /**
     * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `TbAbstractRelationActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract C loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException;

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractRelationActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
        // 脚本执行交给规则节点脚本引擎，异常会通过回调进入失败关系。
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            EntityContainer entityContainer = entityIdCache.get(key);
            if (entityContainer.getEntityId() == null) {
                throw new RuntimeException("No entity found with type '" + key.getEntityType() + "' and name '" + key.getEntityName() + "'.");
            }
            return entityContainer;
        });
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbAbstractRelationActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：执行本类核心处理流程，供 `TbAbstractRelationActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `RelationService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected ListenableFuture<List<EntityRelation>> processListSearchDirection(TbContext ctx, TbMsg msg) {
        if (EntitySearchDirection.FROM.name().equals(this.config.getDirection())) {
            // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
            return ctx.getRelationService().findByToAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), processPattern(msg, this.config.getRelationType()), RelationTypeGroup.COMMON);
        } else {
            // 通过 `TbContext` 暴露的服务层访问数据，具体持久化和缓存由服务实现负责。
            return ctx.getRelationService().findByFromAndTypeAsync(ctx.getTenantId(), msg.getOriginator(), processPattern(msg, this.config.getRelationType()), RelationTypeGroup.COMMON);
        }
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbAbstractRelationActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected String processPattern(TbMsg msg, String pattern) {
        return TbNodeUtils.processPattern(pattern, msg);
    }

    @Data
    @AllArgsConstructor
    /**
     * 中文说明：`EntityKey` 是实体键辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    private static class EntityKey {
        /**
         * 字段说明：保存 `entityName`，表示目标实体类型、名称或标识，供本类方法在规则节点处理流程中使用。
         */
        private String entityName;
        /**
         * 字段说明：保存 `type`，表示类型匹配条件，供本类方法在规则节点处理流程中使用。
         */
        private String type;
        /**
         * 字段说明：保存 `entityType`，表示目标实体类型、名称或标识，供本类方法在规则节点处理流程中使用。
         */
        private EntityType entityType;
    }

    @Data
    /**
     * 中文说明：`SearchDirectionIds` 是SearchDirectionIds辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    protected static class SearchDirectionIds {
        /**
         * 字段说明：保存 `fromId`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
         */
        private EntityId fromId;
        /**
         * 字段说明：保存 `toId`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
         */
        private EntityId toId;
        /**
         * 字段说明：保存 `originatorDirectionFrom`，表示关系方向，供本类方法在规则节点处理流程中使用。
         */
        private boolean originatorDirectionFrom;
    }

    /**
     * 中文说明：`EntityCacheLoader` 是实体CacheLoader辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    private static class EntityCacheLoader extends CacheLoader<EntityKey, EntityContainer> {

        /**
         * 字段说明：保存 Rule Engine 上下文引用；本字段本身不直接代表数据库、MQTT 或事务资源。
         */
        private final TbContext ctx;
        /**
         * 字段说明：保存 `createIfNotExists`，表示时间戳，供本类方法在规则节点处理流程中使用。
         */
        private final boolean createIfNotExists;

        /**
         * 方法说明：构造 `EntityCacheLoader` 实例并初始化必要字段。
         * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
         */
        private EntityCacheLoader(TbContext ctx, boolean createIfNotExists) {
            this.ctx = ctx;
            this.createIfNotExists = createIfNotExists;
        }

        @Override
        /**
         * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `EntityCacheLoader` 的规则节点处理或辅助流程调用。
         * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
         */
        public EntityContainer load(EntityKey key) {
            return loadEntity(key);
        }

        /**
         * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `EntityCacheLoader` 的规则节点处理或辅助流程调用。
         * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `AssetService`, `ClusterService`, `CustomerService`, `DashboardService`, `DeviceService`, `EdgeService` 等服务，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    /**
     * 中文说明：`RelationContainer` 是关系Container辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    protected static class RelationContainer {

        /**
         * 字段说明：保存 `msg`，表示当前规则链消息，供本类方法在规则节点处理流程中使用。
         */
        private TbMsg msg;
        /**
         * 字段说明：保存 `result`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
         */
        private boolean result;

    }


    /*
     * 本类总结：`TbAbstractRelationActionNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
