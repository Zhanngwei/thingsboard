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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`WidgetTypeServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class WidgetTypeServiceTest extends AbstractServiceTest {

    /**
     * 部件包，提供当前类调用的业务操作。
     */
    @Autowired
    WidgetsBundleService widgetsBundleService;

    /**
     * 部件类型，提供当前类调用的业务操作。
     */
    @Autowired
    WidgetTypeService widgetTypeService;

    private IdComparator<WidgetType> idComparator = new IdComparator<>();

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetType() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = widgetTypeService.saveWidgetType(widgetType);

        Assert.assertNotNull(savedWidgetType);
        Assert.assertNotNull(savedWidgetType.getId());
        Assert.assertNotNull(savedWidgetType.getFqn());
        Assert.assertTrue(savedWidgetType.getCreatedTime() > 0);
        Assert.assertEquals(widgetType.getTenantId(), savedWidgetType.getTenantId());
        Assert.assertEquals(widgetType.getName(), savedWidgetType.getName());
        Assert.assertEquals(widgetType.getDescriptor(), savedWidgetType.getDescriptor());

        savedWidgetType.setName("New Widget Type");

        widgetTypeService.saveWidgetType(savedWidgetType);
        WidgetType foundWidgetType = widgetTypeService.findWidgetTypeById(tenantId, savedWidgetType.getId());
        Assert.assertEquals(foundWidgetType.getName(), savedWidgetType.getName());

        widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetTypeWithEmptyName() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        Assertions.assertThrows(DataValidationException.class, () -> {
            widgetTypeService.saveWidgetType(widgetType);
        });
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetTypeWithEmptyDescriptor() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{}", JsonNode.class));
        Assertions.assertThrows(DataValidationException.class, () -> {
            widgetTypeService.saveWidgetType(widgetType);
        });
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetTypeWithInvalidTenant() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        Assertions.assertThrows(DataValidationException.class, () -> {
            widgetTypeService.saveWidgetType(widgetType);
        });
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateWidgetTypeTenant() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        savedWidgetType.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                widgetTypeService.saveWidgetType(savedWidgetType);
            });
        } finally {
            widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
        }
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateWidgetTypeFqn() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        savedWidgetType.setFqn("some_fqn");
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                widgetTypeService.saveWidgetType(savedWidgetType);
            });
        } finally {
            widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
        }
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindWidgetTypeById() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        WidgetTypeDetails foundWidgetType = widgetTypeService.findWidgetTypeDetailsById(tenantId, savedWidgetType.getId());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(savedWidgetType, foundWidgetType);
        widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindWidgetTypeByTenantIdAndFqn() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = new WidgetType(widgetTypeService.saveWidgetType(widgetType));
        WidgetType foundWidgetType = widgetTypeService.findWidgetTypeByTenantIdAndFqn(tenantId, savedWidgetType.getFqn());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(savedWidgetType, foundWidgetType);
        widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteWidgetType() throws IOException {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(tenantId);
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetType savedWidgetType = widgetTypeService.saveWidgetType(widgetType);
        WidgetType foundWidgetType = widgetTypeService.findWidgetTypeById(tenantId, savedWidgetType.getId());
        Assert.assertNotNull(foundWidgetType);
        widgetTypeService.deleteWidgetType(tenantId, savedWidgetType.getId());
        foundWidgetType = widgetTypeService.findWidgetTypeById(tenantId, savedWidgetType.getId());
        Assert.assertNull(foundWidgetType);
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindWidgetTypesByTenantIdAndWidgetsBundleId() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        List<WidgetType> widgetTypes = new ArrayList<>();
        for (int i=0;i<121;i++) {
            WidgetTypeDetails widgetType = new WidgetTypeDetails();
            widgetType.setTenantId(tenantId);
            widgetType.setName("Widget Type " + i);
            widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
            widgetTypes.add(new WidgetType(widgetTypeService.saveWidgetType(widgetType)));
        }

        List<WidgetTypeId> widgetTypeIds = widgetTypes.stream().map(WidgetType::getId).collect(Collectors.toList());

        widgetTypeService.updateWidgetsBundleWidgetTypes(tenantId, savedWidgetsBundle.getId(), widgetTypeIds);

        List<WidgetType> loadedWidgetTypes = widgetTypeService.findWidgetTypesByWidgetsBundleId(tenantId, savedWidgetsBundle.getId());

        Collections.sort(widgetTypes, idComparator);
        Collections.sort(loadedWidgetTypes, idComparator);

        Assert.assertEquals(widgetTypes, loadedWidgetTypes);

        for (WidgetTypeId id : widgetTypeIds) {
            widgetTypeService.deleteWidgetType(tenantId, id);
        }

        loadedWidgetTypes = widgetTypeService.findWidgetTypesByWidgetsBundleId(tenantId, savedWidgetsBundle.getId());

        Assert.assertTrue(loadedWidgetTypes.isEmpty());

        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteAllTypesFromWidgetsBundle() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("Widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        List<WidgetType> widgetTypes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            WidgetTypeDetails widgetType = new WidgetTypeDetails();
            widgetType.setTenantId(tenantId);
            widgetType.setName("Widget Type " + i);
            widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
            widgetTypes.add(new WidgetType(widgetTypeService.saveWidgetType(widgetType)));
        }

        List<WidgetTypeId> widgetTypeIds = widgetTypes.stream().map(WidgetType::getId).collect(Collectors.toList());

        widgetTypeService.updateWidgetsBundleWidgetTypes(tenantId, savedWidgetsBundle.getId(), widgetTypeIds);

        List<WidgetType> loadedWidgetTypes = widgetTypeService.findWidgetTypesByWidgetsBundleId(tenantId, savedWidgetsBundle.getId());
        Assert.assertEquals(widgetTypes.size(), loadedWidgetTypes.size());

        widgetTypeService.updateWidgetsBundleWidgetTypes(tenantId, savedWidgetsBundle.getId(), Collections.emptyList());

        loadedWidgetTypes = widgetTypeService.findWidgetTypesByWidgetsBundleId(tenantId, savedWidgetsBundle.getId());
        Assert.assertEquals(0, loadedWidgetTypes.size());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`WidgetTypeServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
