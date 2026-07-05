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

/**
 * 中文说明：`TbAbstractCustomerActionNode` 是抽象客户Action节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbAbstractCustomerActionNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
public abstract class TbAbstractCustomerActionNode<C extends TbAbstractCustomerActionNodeConfiguration> implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    protected C config;

    private LoadingCache<CustomerKey, Optional<CustomerId>> customerIdCache;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
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
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：判断结果。
     */
    protected abstract boolean createCustomerIfNotExists();

    /**
     * 功能：获取客户。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    protected abstract C loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException;

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(processCustomerAction(ctx, msg),
                m -> ctx.tellSuccess(msg),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：处理客户。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> processCustomerAction(TbContext ctx, TbMsg msg) {
        ListenableFuture<CustomerId> customerIdFeature = getCustomer(ctx, msg);
        return Futures.transform(customerIdFeature, customerId -> {
                    doProcessCustomerAction(ctx, msg, customerId);
                    return null;
                }, ctx.getDbCallbackExecutor()
        );
    }

    /**
     * 功能：执行 `doProcessCustomerAction` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    protected abstract void doProcessCustomerAction(TbContext ctx, TbMsg msg, CustomerId customerId);

    /**
     * 功能：获取客户。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<CustomerId> getCustomer(TbContext ctx, TbMsg msg) {
        String customerTitle = TbNodeUtils.processPattern(this.config.getCustomerNamePattern(), msg);
        CustomerKey key = new CustomerKey(customerTitle);
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            Optional<CustomerId> customerId = customerIdCache.get(key);
            if (!customerId.isPresent()) {
                throw new RuntimeException("No customer found with name '" + key.getCustomerTitle() + "'.");
            }
            return customerId.get();
        });
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (customerIdCache != null) {
            customerIdCache.invalidateAll();
        }
    }

    /**
     * 中文说明：`CustomerKey` 是客户键辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    @Data
    @AllArgsConstructor
    private static class CustomerKey {
        /**
         * 客户对象，用于描述当前业务场景。
         */
        private String customerTitle;
    }

    /**
     * 中文说明：`CustomerCacheLoader` 是客户CacheLoader辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
     * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
     */
    private static class CustomerCacheLoader extends CacheLoader<CustomerKey, Optional<CustomerId>> {

        /**
         * 上下文，汇总当前处理所需的上下文信息。
         */
        private final TbContext ctx;
        /**
         * 是否满足`createIfNotExists`条件。
         */
        private final boolean createIfNotExists;

        /**
         * 功能：创建 `TbAbstractCustomerActionNode` 实例，并初始化必要字段。
         * 参数：
         * - `ctx`：处理上下文。
         * - `createIfNotExists`：`createIfNotExists` 参数。
         * 返回：新创建的对象实例。
         */
        private CustomerCacheLoader(TbContext ctx, boolean createIfNotExists) {
            this.ctx = ctx;
            this.createIfNotExists = createIfNotExists;
        }

        /**
         * 功能：执行 `load` 对应的处理。
         * 参数：
         * - `key`：键。
         * 返回：可能存在的结果。
         */
        @Override
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
