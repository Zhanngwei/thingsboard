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
package org.thingsboard.server.dao.sql.widget;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`JpaWidgetTypeDaoTest` 是 ThingsBoard DAO 测试模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public class JpaWidgetTypeDaoTest extends AbstractJpaDaoTest {

    // given search text should find a widget with tags, when searching by tags
    final Map<String, String[]> SHOULD_FIND_SEARCH_TO_TAGS_MAP = Map.of(
            "A",             new String[]{"a", "b", "c"},
            "test A test",   new String[]{"a", "b", "c"},
            "test x y test", new String[]{"x y", "b", "c"},
            "x y test",      new String[]{"x y", "b", "c"},
            "x y",           new String[]{"x y", "b", "c"},
            "test x y",      new String[]{"x y", "b", "c"}
    );

    // given search text should not find a widget with tags, when searching by tags
    final Map<String, String[]> SHOULDNT_FIND_SEARCH_TO_TAGS_MAP = Map.of(
            "testA test",   new String[]{"a", "b", "c"},
            "testx y test", new String[]{"x y", "b", "c"},
            "testx ytest",  new String[]{"x y", "b", "c"},
            "x ytest",      new String[]{"x y", "b", "c"},
            "testx y",      new String[]{"x y", "b", "c"},
            "x",            new String[]{"x y", "b", "c"}
    );

    /**
     * 字段说明：
     * 1. 保存 `BUNDLE_ALIAS` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    final String BUNDLE_ALIAS = "BUNDLE_ALIAS";
    final int WIDGET_TYPE_COUNT = 3;
    /**
     * 字段说明：
     * 1. 保存 `widgetTypeList` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    List<WidgetTypeDetails> widgetTypeList;
    WidgetsBundle widgetsBundle;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `widgetTypeDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private WidgetTypeDao widgetTypeDao;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `widgetsBundleDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private WidgetsBundleDao widgetsBundleDao;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `setUp` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void setUp() throws InterruptedException {
        widgetTypeList = new ArrayList<>();

        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setAlias(BUNDLE_ALIAS);
        widgetsBundle.setTitle(BUNDLE_ALIAS);
        widgetsBundle.setId(new WidgetsBundleId(Uuids.timeBased()));
        widgetsBundle.setTenantId(TenantId.SYS_TENANT_ID);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        this.widgetsBundle = widgetsBundleDao.save(TenantId.SYS_TENANT_ID, widgetsBundle);

        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (int i = 0; i < WIDGET_TYPE_COUNT; i++) {
            Thread.sleep(2);
            var widgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, i);
            widgetTypeList.add(widgetType);
            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(this.widgetsBundle.getId(), widgetType.getId(), i));
        }
        widgetTypeList.sort(Comparator.comparing(BaseWidgetType::getName));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createAndSaveWidgetType` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    WidgetTypeDetails createAndSaveWidgetType(TenantId tenantId, int number) {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("WIDGET_TYPE_" + number);
        widgetType.setDescription("WIDGET_TYPE_DESCRIPTION" + number);
        widgetType.setFqn("FQN_" + number);
        widgetType.setImage("/image/system/logo.png");
        var descriptor = JacksonUtil.newObjectNode();
        descriptor.put("type", number % 2 == 0 ? "latest" : "static");
        widgetType.setDescriptor(descriptor);
        String[] tags = new String[]{"Tag1_"+number, "Tag2_"+number, "TEST_"+number};
        widgetType.setTags(tags);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        return widgetTypeDao.save(TenantId.SYS_TENANT_ID, widgetType);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createAndSaveWidgetType` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    WidgetTypeDetails createAndSaveWidgetType(TenantId tenantId, int number, String[] tags) {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("WIDGET_TYPE_" + number);
        widgetType.setDescription("WIDGET_TYPE_DESCRIPTION" + number);
        widgetType.setFqn("FQN_" + number);
        widgetType.setImage("/image/tenant/logo.png");
        var descriptor = JacksonUtil.newObjectNode();
        descriptor.put("type", number % 2 == 0 ? "latest" : "static");
        widgetType.setDescriptor(descriptor);
        widgetType.setTags(tags);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        return widgetTypeDao.save(TenantId.SYS_TENANT_ID, widgetType);
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `tearDown` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void tearDown() {
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        widgetsBundleDao.removeById(TenantId.SYS_TENANT_ID, widgetsBundle.getUuidId());
        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (WidgetType widgetType : widgetTypeList) {
            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, widgetType.getUuidId());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByWidgetsBundleId` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByWidgetsBundleId() {
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByWidgetsBundleId(TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId());
        assertEquals(WIDGET_TYPE_COUNT, widgetTypes.size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindSystemWidgetTypes` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindSystemWidgetTypes() {
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findSystemWidgetTypes(TenantId.SYS_TENANT_ID, true, DeprecatedFilter.ALL, Collections.singletonList("static"),
                new PageLink(1024, 0, "TYPE_DESCRIPTION", new SortOrder("createdTime")));
        assertEquals(1, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(1)), widgetTypes.getData().get(0));

        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        widgetTypes = widgetTypeDao.findSystemWidgetTypes(TenantId.SYS_TENANT_ID, true, DeprecatedFilter.ALL, Collections.emptyList(),
                new PageLink(1024, 0, "hfgfd tag2_2 ghg", new SortOrder("createdTime")));
        assertEquals(1, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(2)), widgetTypes.getData().get(0));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTagsSearchInFindBySystemWidgetTypes` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testTagsSearchInFindBySystemWidgetTypes() {
        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (var entry : SHOULD_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findSystemWidgetTypes(
                    TenantId.SYS_TENANT_ID, true, DeprecatedFilter.ALL, null, new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(1);
            assertThat(widgetTypes.getData().get(0).getId()).isEqualTo(savedWidgetType.getId());

            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }

        for (var entry : SHOULDNT_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findSystemWidgetTypes(
                    TenantId.SYS_TENANT_ID, true, DeprecatedFilter.ALL, null, new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(0);

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantWidgetTypesByTenantId` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindTenantWidgetTypesByTenantId() {
        UUID tenantId = Uuids.timeBased();
        for (int i = 0; i < WIDGET_TYPE_COUNT; i++) {
            var widgetType = createAndSaveWidgetType(new TenantId(tenantId), i);
            widgetTypeList.add(widgetType);
        }
        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findTenantWidgetTypesByTenantId(tenantId, true, DeprecatedFilter.ALL, null,
                new PageLink(10, 0, "", new SortOrder("createdTime")));
        assertEquals(WIDGET_TYPE_COUNT, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(3)), widgetTypes.getData().get(0));
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(4)), widgetTypes.getData().get(1));
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(5)), widgetTypes.getData().get(2));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTagsSearchInFindTenantWidgetTypesByTenantId` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testTagsSearchInFindTenantWidgetTypesByTenantId() {
        for (var entry : SHOULD_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findTenantWidgetTypesByTenantId(
                    TenantId.SYS_TENANT_ID.getId(), true, DeprecatedFilter.ALL, null, new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(1);
            assertThat(widgetTypes.getData().get(0).getId()).isEqualTo(savedWidgetType.getId());

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }

        for (var entry : SHOULDNT_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findTenantWidgetTypesByTenantId(
                    TenantId.SYS_TENANT_ID.getId(), true, DeprecatedFilter.ALL, null, new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(0);

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTagsSearchInFindAllTenantWidgetTypesByTenantId` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testTagsSearchInFindAllTenantWidgetTypesByTenantId() {
        for (var entry : SHOULD_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findAllTenantWidgetTypesByTenantId(
                    TenantId.SYS_TENANT_ID.getId(), true, DeprecatedFilter.ALL, null, new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(1);
            assertThat(widgetTypes.getData().get(0).getId()).isEqualTo(savedWidgetType.getId());

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }

        for (var entry : SHOULDNT_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findAllTenantWidgetTypesByTenantId(
                    TenantId.SYS_TENANT_ID.getId(), true, DeprecatedFilter.ALL, null, new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(0);

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByWidgetTypeInfosByBundleId` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByWidgetTypeInfosByBundleId() {
        PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId(),true, DeprecatedFilter.ALL, Collections.singletonList("latest"),
                new PageLink(1024, 0, "TYPE_DESCRIPTION", new SortOrder("createdTime")));
        assertEquals(2, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(0)), widgetTypes.getData().get(0));
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(2)), widgetTypes.getData().get(1));

        widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId(), true, DeprecatedFilter.ALL, Collections.emptyList(),
                new PageLink(1024, 0, "hfgfd TEST_0 ghg", new SortOrder("createdTime")));
        assertEquals(1, widgetTypes.getData().size());
        assertEquals(new WidgetTypeInfo(widgetTypeList.get(0)), widgetTypes.getData().get(0));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTagsSearchInFindByWidgetTypeInfosByBundleId` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testTagsSearchInFindByWidgetTypeInfosByBundleId() {
        for (var entry : SHOULD_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);
            widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(this.widgetsBundle.getId(), savedWidgetType.getId(), WIDGET_TYPE_COUNT + 1));

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(
                    TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId(), true, DeprecatedFilter.ALL, null, new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(1);
            assertThat(widgetTypes.getData().get(0).getId()).isEqualTo(savedWidgetType.getId());

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }

        for (var entry : SHOULDNT_FIND_SEARCH_TO_TAGS_MAP.entrySet()) {
            String searchText = entry.getKey();
            String[] tags = entry.getValue();

            WidgetTypeDetails savedWidgetType = createAndSaveWidgetType(TenantId.SYS_TENANT_ID, WIDGET_TYPE_COUNT + 1, tags);
            widgetTypeDao.saveWidgetsBundleWidget(new WidgetsBundleWidget(this.widgetsBundle.getId(), savedWidgetType.getId(), WIDGET_TYPE_COUNT + 1));

            PageData<WidgetTypeInfo> widgetTypes = widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(
                    TenantId.SYS_TENANT_ID.getId(), widgetsBundle.getUuidId(), true, DeprecatedFilter.ALL, null, new PageLink(10, 0, searchText)
            );

            assertThat(widgetTypes.getData()).hasSize(0);

            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, savedWidgetType.getUuidId());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByTenantIdAndFqn` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByTenantIdAndFqn() {
        WidgetType result = widgetTypeList.get(0);
        assertNotNull(result);
        WidgetType widgetType = widgetTypeDao.findByTenantIdAndFqn(TenantId.SYS_TENANT_ID.getId(), "FQN_0");
        assertEquals(result.getId(), widgetType.getId());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByTenantAndImageLink` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByTenantAndImageLink() {
        var result = widgetTypeDao.findByImageLink("/image/system/logo.png", 5);
        assertEquals(3, result.size());
        result = widgetTypeDao.findByImageLink("/image/system/logo2.png", 5);
        assertEquals(0, result.size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindByImageLink` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testFindByImageLink() {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        WidgetTypeDetails details = createAndSaveWidgetType(tenantId, 0, new String[]{"a"});
        details.setDescriptor(JacksonUtil.newObjectNode().put("bg", "/image/tenant/widget.png"));
        widgetTypeDao.save(tenantId, details);
        var result = widgetTypeDao.findByTenantAndImageLink(tenantId, "/image/tenant/widget.png", 3);
        assertEquals(1, result.size());
        result = widgetTypeDao.findByTenantAndImageLink(tenantId, "/image/tenant/widget2.png", 3);
        assertEquals(0, result.size());
        widgetTypeDao.removeById(tenantId, details.getUuidId());
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JpaWidgetTypeDaoTest` 在 ThingsBoard DAO 测试模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
