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
package org.thingsboard.server.dao.attributes;

import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AttributeCache")
/**
 * 中文说明：
 * 1. 类目的：`AttributeRedisCache` 是 ThingsBoard DAO 模块 中的DAO 缓存和失效事件类型，用于保存实体、配置、类型或会话相关数据的缓存键、缓存值和跨节点失效事件。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Caffeine、Redis、Spring Cache、DAO Service、Application 服务和集群事件总线。
 * 4. 生命周期：缓存对象随 Spring 缓存 Bean 存在，失效事件随单次保存、删除或配置更新流程传播。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Cache-Aside / Observer / Value Object。
 */
public class AttributeRedisCache extends RedisTbTransactionalCache<AttributeCacheKey, AttributeKvEntry> {

    /**
     * 方法说明：
     * 1. 职责：执行 `AttributeRedisCache` 对应的DAO 缓存和失效事件类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：缓存对象随 Spring 缓存 Bean 存在，失效事件随单次保存、删除或配置更新流程传播时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据租户、实体或配置键读取缓存，数据库变更后发布失效事件以维持多节点一致性。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public AttributeRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
        super(CacheConstants.ATTRIBUTES_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbRedisSerializer<>() {
            @Override
            public byte[] serialize(AttributeKvEntry attributeKvEntry) throws SerializationException {
                AttributeValueProto.Builder builder = AttributeValueProto.newBuilder()
                        .setLastUpdateTs(attributeKvEntry.getLastUpdateTs());
                // 根据实体类型、查询类型或数据库方言分支，保持不同持久化路径的语义隔离。
                switch (attributeKvEntry.getDataType()) {
                    case BOOLEAN:
                        attributeKvEntry.getBooleanValue().ifPresent(builder::setBoolV);
                        builder.setHasV(attributeKvEntry.getBooleanValue().isPresent());
                        builder.setType(KeyValueType.BOOLEAN_V);
                        break;
                    case STRING:
                        attributeKvEntry.getStrValue().ifPresent(builder::setStringV);
                        builder.setHasV(attributeKvEntry.getStrValue().isPresent());
                        builder.setType(KeyValueType.STRING_V);
                        break;
                    case DOUBLE:
                        attributeKvEntry.getDoubleValue().ifPresent(builder::setDoubleV);
                        builder.setHasV(attributeKvEntry.getDoubleValue().isPresent());
                        builder.setType(KeyValueType.DOUBLE_V);
                        break;
                    case LONG:
                        attributeKvEntry.getLongValue().ifPresent(builder::setLongV);
                        builder.setHasV(attributeKvEntry.getLongValue().isPresent());
                        builder.setType(KeyValueType.LONG_V);
                        break;
                    case JSON:
                        attributeKvEntry.getJsonValue().ifPresent(builder::setJsonV);
                        builder.setHasV(attributeKvEntry.getJsonValue().isPresent());
                        builder.setType(KeyValueType.JSON_V);
                        break;

                }
                return builder.build().toByteArray();
            }

            @Override
            // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
            public AttributeKvEntry deserialize(AttributeCacheKey key, byte[] bytes) throws SerializationException {
                try {
                    AttributeValueProto proto = AttributeValueProto.parseFrom(bytes);
                    boolean hasValue = proto.getHasV();
                    KvEntry entry;
                    // 根据实体类型、查询类型或数据库方言分支，保持不同持久化路径的语义隔离。
                    switch (proto.getType()) {
                        case BOOLEAN_V:
                            entry = new BooleanDataEntry(key.getKey(), hasValue ? proto.getBoolV() : null);
                            break;
                        case LONG_V:
                            entry = new LongDataEntry(key.getKey(), hasValue ? proto.getLongV() : null);
                            break;
                        case DOUBLE_V:
                            entry = new DoubleDataEntry(key.getKey(), hasValue ? proto.getDoubleV() : null);
                            break;
                        case STRING_V:
                            entry = new StringDataEntry(key.getKey(), hasValue ? proto.getStringV() : null);
                            break;
                        case JSON_V:
                            entry = new JsonDataEntry(key.getKey(), hasValue ? proto.getJsonV() : null);
                            break;
                        default:
                            throw new InvalidProtocolBufferException("Unrecognized type: " + proto.getType() + " !");
                    }
                    return new BaseAttributeKvEntry(proto.getLastUpdateTs(), entry);
                // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
                } catch (InvalidProtocolBufferException e) {
                    throw new SerializationException(e.getMessage());
                }
            }
        });
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AttributeRedisCache` 在 ThingsBoard DAO 模块 中承担DAO 缓存和失效事件类型职责，核心目的是保存实体、配置、类型或会话相关数据的缓存键、缓存值和跨节点失效事件。
 * 2. 核心流程：根据租户、实体或配置键读取缓存，数据库变更后发布失效事件以维持多节点一致性。
 * 3. 关键依赖：主要依赖或协作对象包括Caffeine、Redis、Spring Cache、DAO Service、Application 服务和集群事件总线。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
