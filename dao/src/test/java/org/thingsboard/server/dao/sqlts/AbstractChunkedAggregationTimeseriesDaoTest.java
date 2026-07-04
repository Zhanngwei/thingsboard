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
package org.thingsboard.server.dao.sqlts;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.data.id.TenantId.SYS_TENANT_ID;
import static org.thingsboard.server.common.data.kv.Aggregation.COUNT;

/**
 * 中文说明：
 * 1. 类目的：`AbstractChunkedAggregationTimeseriesDaoTest` 是 ThingsBoard DAO 测试模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
public class AbstractChunkedAggregationTimeseriesDaoTest {

    /**
     * 字段说明：
     * 1. 保存 `LIMIT` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    final int LIMIT = 1;
    final String TEMP = "temp";
    /**
     * 字段说明：
     * 1. 保存 `DESC` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    final String DESC = "DESC";
    private AbstractChunkedAggregationTimeseriesDao tsDao;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `setUp` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void setUp() throws Exception {
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        tsDao = spy(AbstractChunkedAggregationTimeseriesDao.class);
        // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
        Optional<TsKvEntry> optionalListenableFuture = Optional.of(mock(TsKvEntry.class));
        // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
        willReturn(Futures.immediateFuture(optionalListenableFuture)).given(tsDao).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
        willReturn(Futures.immediateFuture(mock(ReadTsKvQueryResult.class))).given(tsDao).getReadTsKvQueryResultFuture(any(), any());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenLastIntervalShorterThanOthersAndEqualsEndTs` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenLastIntervalShorterThanOthersAndEqualsEndTs() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 2000, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 2001, 1001, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQuerySecond = new BaseReadTsKvQuery(TEMP, 2001, 3000, 2501, LIMIT, COUNT, DESC);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(2)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 2001, getTsForReadTsKvQuery(1, 2001), COUNT);
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQuerySecond.getKey(), 2001, 3000, getTsForReadTsKvQuery(2001, 3000), COUNT);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriod` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriod() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3000, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query)).isNotNull();
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodMinusOne` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodMinusOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 2999, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3000, 1500, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);

    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodPlusOne` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodPlusOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3001, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsZero` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsZero() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 0, 0, 1, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 0, 1, 0, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 0, 1, getTsForReadTsKvQuery(0, 1), COUNT);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsOne` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 1, 1, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQuery = new BaseReadTsKvQuery(TEMP, 1, 2, 1, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQuery.getKey(), 1, 2, getTsForReadTsKvQuery(1, 2), COUNT);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsIntegerMax` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsIntegerMax() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Integer.MAX_VALUE, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), Integer.MAX_VALUE, 1L + Integer.MAX_VALUE, getTsForReadTsKvQuery(Integer.MAX_VALUE, 1L + Integer.MAX_VALUE), COUNT);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsBigNumber` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsBigNumber() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, Integer.MAX_VALUE, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenCountIntervalEqualsPeriodSize` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenCountIntervalEqualsPeriodSize() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1000)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (long i = 1; i <= 3000; i += 3) {
            verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, TEMP, i, Math.min(i + 3, 3000), getTsForReadTsKvQuery(i, i + 3), COUNT);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getTsForReadTsKvQuery` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    long getTsForReadTsKvQuery(long startTs, long endTs) {
        return startTs + (endTs - startTs) / 2L;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractChunkedAggregationTimeseriesDaoTest` 在 ThingsBoard DAO 测试模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
