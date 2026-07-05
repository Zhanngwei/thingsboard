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
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`WidgetsBundleServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class WidgetsBundleServiceTest extends AbstractServiceTest {

    /**
     * 部件包，提供当前类调用的业务操作。
     */
    @Autowired
    WidgetsBundleService widgetsBundleService;

    private IdComparator<WidgetsBundle> idComparator = new IdComparator<>();

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetsBundle() throws IOException {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My first widgets bundle");

        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);

        Assert.assertNotNull(savedWidgetsBundle);
        Assert.assertNotNull(savedWidgetsBundle.getId());
        Assert.assertNotNull(savedWidgetsBundle.getAlias());
        Assert.assertTrue(savedWidgetsBundle.getCreatedTime() > 0);
        Assert.assertEquals(widgetsBundle.getTenantId(), savedWidgetsBundle.getTenantId());
        Assert.assertEquals(widgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        savedWidgetsBundle.setTitle("My new widgets bundle");

        widgetsBundleService.saveWidgetsBundle(savedWidgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertEquals(foundWidgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetsBundleWithEmptyTitle() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        });
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetsBundleWithInvalidTenant() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        });
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateWidgetsBundleTenant() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle.setTenantId(tenantId);
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        savedWidgetsBundle.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                widgetsBundleService.saveWidgetsBundle(savedWidgetsBundle);
            });
        } finally {
            widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
        }
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateWidgetsBundleAlias() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle.setTenantId(tenantId);
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        savedWidgetsBundle.setAlias("new_alias");
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                widgetsBundleService.saveWidgetsBundle(savedWidgetsBundle);
            });
        } finally {
            widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
        }
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindWidgetsBundleById() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertNotNull(foundWidgetsBundle);
        Assert.assertEquals(savedWidgetsBundle, foundWidgetsBundle);
        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindWidgetsBundleByTenantIdAndAlias() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(tenantId, savedWidgetsBundle.getAlias());
        Assert.assertNotNull(foundWidgetsBundle);
        Assert.assertEquals(savedWidgetsBundle, foundWidgetsBundle);
        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteWidgetsBundle() {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenantId);
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        WidgetsBundle foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertNotNull(foundWidgetsBundle);
        widgetsBundleService.deleteWidgetsBundle(tenantId, savedWidgetsBundle.getId());
        foundWidgetsBundle = widgetsBundleService.findWidgetsBundleById(tenantId, savedWidgetsBundle.getId());
        Assert.assertNull(foundWidgetsBundle);
    }

    /**
     * 功能：验证分页查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindSystemWidgetsBundlesByPageLink() {

        TenantId tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);
        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        for (int i=0;i<235;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(tenantId);
            widgetsBundle.setTitle("Widgets bundle "+i);
            createdWidgetsBundles.add(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(19);
        PageData<WidgetsBundle> pageData = null;
        do {
            pageData = widgetsBundleService.findSystemWidgetsBundlesByPageLink(tenantId, false, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);
    }

    /**
     * 功能：验证部件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindSystemWidgetsBundles() {
        TenantId tenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        for (int i=0;i<135;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(tenantId);
            widgetsBundle.setTitle("Widgets bundle "+i);
            createdWidgetsBundles.add(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTenantWidgetsBundlesByTenantId() {

        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        for (int i=0;i<127;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(tenantId);
            widgetsBundle.setTitle("Widgets bundle "+i);
            widgetsBundles.add(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        }

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(11);
        PageData<WidgetsBundle> pageData = null;
        do {
            pageData = widgetsBundleService.findTenantWidgetsBundlesByTenantId(tenantId, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);

        pageLink = new PageLink(15);
        pageData = widgetsBundleService.findTenantWidgetsBundlesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    /**
     * 功能：验证分页查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAllWidgetsBundlesByTenantIdAndPageLink() {

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        TenantId systemTenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        List<WidgetsBundle> createdSystemWidgetsBundles = new ArrayList<>();
        for (int i=0;i<177;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(i % 2 == 0 ? tenantId : systemTenantId);
            widgetsBundle.setTitle((i % 2 == 0 ? "Widgets bundle " : "System widget bundle ") + i);
            WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
            createdWidgetsBundles.add(savedWidgetsBundle);
            if (i % 2 == 1) {
                createdSystemWidgetsBundles.add(savedWidgetsBundle);
            }
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<WidgetsBundle> pageData = null;
        do {
            pageData = widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, false, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);

        loadedWidgetsBundles.clear();
        pageLink = new PageLink(14);
        do {
            pageData = widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, false, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        List<WidgetsBundle> allSystemWidgetsBundles = new ArrayList<>(systemWidgetsBundles);
        allSystemWidgetsBundles.addAll(createdSystemWidgetsBundles);

        Collections.sort(allSystemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(allSystemWidgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdSystemWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles.clear();
        pageLink = new PageLink(18);
        do {
            pageData = widgetsBundleService.findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, false, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAllWidgetsBundlesByTenantId() {

        List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(tenantId);

        TenantId systemTenantId = TenantId.fromUUID(ModelConstants.NULL_UUID);

        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        List<WidgetsBundle> createdSystemWidgetsBundles = new ArrayList<>();
        for (int i=0;i<277;i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTenantId(i % 2 == 0 ? tenantId : systemTenantId);
            widgetsBundle.setTitle((i % 2 == 0 ? "Widgets bundle " : "System widget bundle ") + i);
            WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
            createdWidgetsBundles.add(savedWidgetsBundle);
            if (i % 2 == 1) {
                createdSystemWidgetsBundles.add(savedWidgetsBundle);
            }
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(systemWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId);

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);

        loadedWidgetsBundles = widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId);

        List<WidgetsBundle> allSystemWidgetsBundles = new ArrayList<>(systemWidgetsBundles);
        allSystemWidgetsBundles.addAll(createdSystemWidgetsBundles);

        Collections.sort(allSystemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(allSystemWidgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdSystemWidgetsBundles) {
            widgetsBundleService.deleteWidgetsBundle(tenantId, widgetsBundle.getId());
        }

        loadedWidgetsBundles = widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId);

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`WidgetsBundleServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
