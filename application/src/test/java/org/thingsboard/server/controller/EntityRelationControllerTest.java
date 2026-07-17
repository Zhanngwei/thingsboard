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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. `EntityRelationControllerTest` 是 ThingsBoard Application 中验证 `EntityRelationController` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractControllerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@DaoSqlTest
public class EntityRelationControllerTest extends AbstractControllerTest {

    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String BASE_DEVICE_NAME = "Test dummy device";

    /**
     * 关系，提供当前类调用的业务操作。
     */
    @Autowired
    RelationService relationService;

    /**
     * `idComparator` 字段，保存当前对象的对应属性。
     */
    private IdComparator<EntityView> idComparator;
    private Tenant savedTenant;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    private User tenantAdmin;
    private Device mainDevice;

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();
        idComparator = new IdComparator<>();

        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");

        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        Device device = new Device();
        device.setName("Main test device");
        device.setType("default");
        mainDevice = doPost("/api/device", device, Device.class);
    }

    /**
     * 功能：执行 `afterTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAndFindRelation() throws Exception {
        Device device = buildSimpleDevice("Test device 1");
        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);

        testNotifyEntityAllOneTimeRelation(foundRelation,
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATION_ADD_OR_UPDATE, foundRelation);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWithDeviceFromNotCreated() throws Exception {
        Device device = new Device();
        device.setName("Test device 2");
        device.setType("default");
        EntityRelation relation = createFromRelation(device, mainDevice, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Parameter entityId can't be empty!")));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWithDeviceToNotCreated() throws Exception {
        Device device = new Device();
        device.setName("Test device 2");
        device.setType("default");
        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Parameter entityId can't be empty!")));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWithDeviceToMissing() throws Exception {
        Device device = new Device();
        device.setName("Test device 2");
        device.setType("default");
        device.setId(new DeviceId(UUID.randomUUID()));
        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", device.getId().getId().toString()))));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    /**
     * 功能：验证`Save And Find Relations By From`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAndFindRelationsByFrom() throws Exception {
        final int numOfDevices = 30;

        Mockito.reset(tbClusterService, auditLogService);

        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        EntityRelation relationTest = createFromRelation(mainDevice, mainDevice, "TEST_NOTIFY_ENTITY");
        testNotifyEntityAllManyRelation(relationTest, savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATION_ADD_OR_UPDATE, numOfDevices);

        String url = String.format("/api/relations?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(url, numOfDevices);
    }

    /**
     * 功能：验证`Save And Find Relations By To`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAndFindRelationsByTo() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(url, numOfDevices);
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAndFindRelationsByFromWithRelationType() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(mainDevice, device, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());
        String url = String.format("/api/relations?fromId=%s&fromType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationType
        );

        assertFoundList(url, 1);
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAndFindRelationsByFromWithRelationTypeOther() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(mainDevice, device, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());

        String relationTypeOther = "TEST_OTHER";
        String url = String.format("/api/relations?fromId=%s&fromType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationTypeOther
        );

        assertFoundList(url, 0);
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAndFindRelationsByToWithRelationType() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(device, mainDevice, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());
        String url = String.format("/api/relations?toId=%s&toType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationType
        );

        assertFoundList(url, 1);
    }


    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAndFindRelationsByToWithRelationTypeOther() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(device, mainDevice, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());

        String relationTypeOther = "TEST_OTHER";
        String url = String.format("/api/relations?toId=%s&toType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationTypeOther
        );

        assertFoundList(url, 0);
    }

    /**
     * 功能：验证信息对象相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRelationsInfoByFrom() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations/info?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        List<EntityRelationInfo> relationsInfos =
                JacksonUtil.convertValue(doGet(url, JsonNode.class), new TypeReference<>() {
                });

        Assert.assertNotNull("Relations is not found!", relationsInfos);
        Assert.assertEquals("List of found relationsInfos is not equal to number of created relations!",
                numOfDevices, relationsInfos.size());

        assertRelationsInfosByFrom(relationsInfos);
    }

    /**
     * 功能：验证信息对象相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRelationsInfoByTo() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations/info?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        List<EntityRelationInfo> relationsInfos =
                JacksonUtil.convertValue(doGet(url, JsonNode.class), new TypeReference<>() {
                });

        Assert.assertNotNull("Relations is not found!", relationsInfos);
        Assert.assertEquals("List of found relationsInfos is not equal to number of created relations!",
                numOfDevices, relationsInfos.size());

        assertRelationsInfosByTo(relationsInfos);
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteRelation() throws Exception {
        Device device = buildSimpleDevice("Test device 1");

        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url).andExpect(status().isOk());

        testNotifyEntityAllOneTimeRelation(foundRelation,
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATION_DELETED, foundRelation);

        doGet(url).andExpect(status().is4xxClientError());
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteRelationWithOtherFromDeviceError() throws Exception {
        Device device = buildSimpleDevice("Test device 1");

        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        Device device2 = buildSimpleDevice("Test device 2");
        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                device2.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteRelationWithOtherToDeviceError() throws Exception {
        Device device = buildSimpleDevice("Test device 1");

        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        Device device2 = buildSimpleDevice("Test device 2");
        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device2.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    /**
     * 功能：验证`Delete Relations`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteRelations() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME + " from");
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME + " to");

        String urlTo = String.format("/api/relations?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );
        String urlFrom = String.format("/api/relations?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(urlTo, numOfDevices);
        assertFoundList(urlFrom, numOfDevices);

        String url = String.format("/api/relations?entityId=%s&entityType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url).andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(null, mainDevice.getId(), mainDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATIONS_DELETED);

        Assert.assertTrue(
                "Performed deletion of all relations but some relations were found!",
                doGet(urlTo, List.class).isEmpty()
        );
        Assert.assertTrue(
                "Performed deletion of all relations but some relations were found!",
                doGet(urlFrom, List.class).isEmpty()
        );
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRelationsByFromQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.FROM,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelation> relations = readResponse(
                doPost("/api/relations", query).andExpect(status().isOk()),
                new TypeReference<List<EntityRelation>>() {
                }
        );

        assertFoundRelations(relations, numOfDevices);
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRelationsByToQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.TO,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelation> relations = readResponse(
                doPost("/api/relations", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertFoundRelations(relations, numOfDevices);
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRelationsInfoByFromQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.FROM,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelationInfo> relationsInfo = readResponse(
                doPost("/api/relations/info", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertRelationsInfosByFrom(relationsInfo);
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRelationsInfoByToQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.TO,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelationInfo> relationsInfo = readResponse(
                doPost("/api/relations/info", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertRelationsInfosByTo(relationsInfo);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateRelationFromTenantToDevice() throws Exception {
        EntityRelation relation = new EntityRelation(tenantAdmin.getTenantId(), mainDevice.getId(), "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                tenantAdmin.getTenantId(), EntityType.TENANT,
                "CONTAINS", mainDevice.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateRelationFromDeviceToTenant() throws Exception {
        EntityRelation relation = new EntityRelation(mainDevice.getId(), tenantAdmin.getTenantId(), "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", tenantAdmin.getTenantId(), EntityType.TENANT
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveAndFindRelationDifferentTenant() throws Exception {
        Device device = buildSimpleDevice("Test device 1");
        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        loginDifferentTenant();

        doGet(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", relation.getFrom().getId().toString()))));

        deleteDifferentTenant();
    }

    /**
     * 功能：构建设备。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    private Device buildSimpleDevice(String name) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        device = doPost("/api/device", device, Device.class);
        return device;
    }

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `mainDevice`：设备信息或设备标识。
     * - `device`：设备信息或设备标识。
     * - `relationType`：类型。
     * 返回：处理结果。
     */
    private EntityRelation createFromRelation(Device mainDevice, Device device, String relationType) {
        return new EntityRelation(mainDevice.getId(), device.getId(), relationType);
    }

    /**
     * 功能：保存或创建`Devices By From`。
     * 参数：
     * - `numOfDevices`：设备信息或设备标识。
     * - `baseName`：名称。
     * 返回：无。
     */
    private void createDevicesByFrom(int numOfDevices, String baseName) throws Exception {
        for (int i = 0; i < numOfDevices; i++) {
            Device device = buildSimpleDevice(baseName + i);

            EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
            doPost("/api/relation", relation).andExpect(status().isOk());
        }
    }

    /**
     * 功能：保存或创建`Devices By To`。
     * 参数：
     * - `numOfDevices`：设备信息或设备标识。
     * - `baseName`：名称。
     * 返回：无。
     */
    private void createDevicesByTo(int numOfDevices, String baseName) throws Exception {
        for (int i = 0; i < numOfDevices; i++) {
            Device device = buildSimpleDevice(baseName + i);
            EntityRelation relation = createFromRelation(device, mainDevice, "CONTAINS");
            doPost("/api/relation", relation).andExpect(status().isOk());
        }
    }

    /**
     * 功能：执行 `assertFoundRelations` 对应的处理。
     * 参数：
     * - `relations`：数据列表。
     * - `numOfDevices`：设备信息或设备标识。
     * 返回：无。
     */
    private void assertFoundRelations(List<EntityRelation> relations, int numOfDevices) {
        Assert.assertNotNull("Relations is not found!", relations);
        Assert.assertEquals("List of found relations is not equal to number of created relations!",
                numOfDevices, relations.size());
    }

    /**
     * 功能：执行 `assertFoundList` 对应的处理。
     * 参数：
     * - `url`：`url` 参数。
     * - `numOfDevices`：设备信息或设备标识。
     * 返回：无。
     */
    private void assertFoundList(String url, int numOfDevices) throws Exception {
        @SuppressWarnings("unchecked")
        List<EntityRelation> relations = doGet(url, List.class);
        assertFoundRelations(relations, numOfDevices);
    }

    /**
     * 功能：执行 `assertRelationsInfosByFrom` 对应的处理。
     * 参数：
     * - `relationsInfos`：数据列表。
     * 返回：无。
     */
    private void assertRelationsInfosByFrom(List<EntityRelationInfo> relationsInfos) {
        for (EntityRelationInfo info : relationsInfos) {
            Assert.assertEquals("Wrong FROM entityId!", mainDevice.getId(), info.getFrom());
            Assert.assertTrue("Wrong FROM name!", info.getToName().contains(BASE_DEVICE_NAME));
            Assert.assertEquals("Wrong relationType!", "CONTAINS", info.getType());
        }
    }

    /**
     * 功能：执行 `assertRelationsInfosByTo` 对应的处理。
     * 参数：
     * - `relationsInfos`：数据列表。
     * 返回：无。
     */
    private void assertRelationsInfosByTo(List<EntityRelationInfo> relationsInfos) {
        for (EntityRelationInfo info : relationsInfos) {
            Assert.assertEquals("Wrong TO entityId!", mainDevice.getId(), info.getTo());
            Assert.assertTrue("Wrong TO name!", info.getFromName().contains(BASE_DEVICE_NAME));
            Assert.assertEquals("Wrong relationType!", "CONTAINS", info.getType());
        }
    }
}
