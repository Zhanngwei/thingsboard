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
 * 1. `WidgetsBundleServiceTest` 是 ThingsBoard DAO 中验证 `WidgetsBundleService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractServiceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
