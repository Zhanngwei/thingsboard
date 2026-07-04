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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.customer.CustomerService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
/**
 * 中文说明：`TbAbstractCustomerActionNode` 是抽象客户Action节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbAbstractCustomerActionNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public abstract class TbAbstractCustomerActionNode<C extends TbAbstractCustomerActionNodeConfiguration> implements TbNode {

    /**
     * 字段说明：保存从规则节点 JSON 转换得到的配置对象，供消息处理和生命周期方法复用。
     */
    protected C config;

    private LoadingCache<CustomerKey, Optional<CustomerId>> customerIdCache;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadCustomerNodeActionConfig(configuration);
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (this.config.getCustomerCacheExpiration() > 0) {
            cacheBuilder.expireAfterWrite(this.config.getCustomerCacheExpiration(), TimeUnit.SECONDS);
        }
        customerIdCache = cacheBuilder
                .build(new CustomerCacheLoader(ctx, createCustomerIfNotExists()));
    }

    /**
     * 方法说明：创建实体、告警、关系或辅助对象，供 `TbAbstractCustomerActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract boolean createCustomerIfNotExists();

    /**
     * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `TbAbstractCustomerActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract C loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) {
        // 异步回调用于把服务或转换结果映射为规则链成功/失败关系。
        withCallback(processCustomerAction(ctx, msg),
                m -> ctx.tellSuccess(msg),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbAbstractCustomerActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Void> processCustomerAction(TbContext ctx, TbMsg msg) {
        ListenableFuture<CustomerId> customerIdFeature = getCustomer(ctx, msg);
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
        return Futures.transform(customerIdFeature, customerId -> {
                    doProcessCustomerAction(ctx, msg, customerId);
                    return null;
                }, ctx.getDbCallbackExecutor()
        );
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbAbstractCustomerActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract void doProcessCustomerAction(TbContext ctx, TbMsg msg, CustomerId customerId);

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractCustomerActionNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected ListenableFuture<CustomerId> getCustomer(TbContext ctx, TbMsg msg) {
        String customerTitle = TbNodeUtils.processPattern(this.config.getCustomerNamePattern(), msg);
        CustomerKey key = new CustomerKey(customerTitle);
        // 脚本执行交给规则节点脚本引擎，异常会通过回调进入失败关系。
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            Optional<CustomerId> customerId = customerIdCache.get(key);
            if (!customerId.isPresent()) {
                throw new RuntimeException("No customer found with name '" + key.getCustomerTitle() + "'.");
            }
            return customerId.get();
        });
    }

    @Override
    /**
     * 方法说明：在节点生命周期销毁阶段释放缓存、脚本引擎、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void destroy() {
        if (customerIdCache != null) {
            customerIdCache.invalidateAll();
        }
    }

    @Data
    @AllArgsConstructor
    /**
     * 中文说明：`CustomerKey` 是客户键辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    private static class CustomerKey {
        /**
         * 字段说明：保存 `customerTitle`，表示客户名称、客户标识或客户缓存，供本类方法在规则节点处理流程中使用。
         */
        private String customerTitle;
    }

    /**
     * 中文说明：`CustomerCacheLoader` 是客户CacheLoader辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    private static class CustomerCacheLoader extends CacheLoader<CustomerKey, Optional<CustomerId>> {

        /**
         * 字段说明：保存 Rule Engine 上下文引用；本字段本身不直接代表数据库、MQTT 或事务资源。
         */
        private final TbContext ctx;
        /**
         * 字段说明：保存 `createIfNotExists`，表示时间戳，供本类方法在规则节点处理流程中使用。
         */
        private final boolean createIfNotExists;

        /**
         * 方法说明：构造 `CustomerCacheLoader` 实例并初始化必要字段。
         * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
         */
        private CustomerCacheLoader(TbContext ctx, boolean createIfNotExists) {
            this.ctx = ctx;
            this.createIfNotExists = createIfNotExists;
        }

        @Override
        /**
         * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `CustomerCacheLoader` 的规则节点处理或辅助流程调用。
         * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，涉及 `CustomerService`，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
         */
        public Optional<CustomerId> load(CustomerKey key) {
            CustomerService service = ctx.getCustomerService();
            Optional<Customer> customerOptional =
                    service.findCustomerByTenantIdAndTitle(ctx.getTenantId(), key.getCustomerTitle());
            if (customerOptional.isPresent()) {
                return Optional.of(customerOptional.get().getId());
            } else if (createIfNotExists) {
                Customer newCustomer = new Customer();
                newCustomer.setTitle(key.getCustomerTitle());
                newCustomer.setTenantId(ctx.getTenantId());
                Customer savedCustomer = service.saveCustomer(newCustomer);
                ctx.enqueue(ctx.customerCreatedMsg(savedCustomer, ctx.getSelfId()),
                        () -> log.trace("Pushed Customer Created message: {}", savedCustomer),
                        throwable -> log.warn("Failed to push Customer Created message: {}", savedCustomer, throwable));
                return Optional.of(savedCustomer.getId());
            }
            return Optional.empty();
        }

    }

    /*
     * 本类总结：`TbAbstractCustomerActionNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
