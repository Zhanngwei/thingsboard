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
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AttributeCache")
public class AttributeRedisCache extends RedisTbTransactionalCache<AttributeCacheKey, AttributeKvEntry> {

    /**
     * 功能：创建 `AttributeRedisCache` 实例，并初始化必要字段。
     * 参数：
     * - `configuration`：配置对象。
     * - `cacheSpecsMap`：键值映射。
     * - `connectionFactory`：`connectionFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public AttributeRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.ATTRIBUTES_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbRedisSerializer<>() {
            @Override
            public byte[] serialize(AttributeKvEntry attributeKvEntry) throws SerializationException {
                AttributeValueProto.Builder builder = AttributeValueProto.newBuilder()
                        .setLastUpdateTs(attributeKvEntry.getLastUpdateTs());
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
            public AttributeKvEntry deserialize(AttributeCacheKey key, byte[] bytes) throws SerializationException {
                try {
                    AttributeValueProto proto = AttributeValueProto.parseFrom(bytes);
                    boolean hasValue = proto.getHasV();
                    KvEntry entry;
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
