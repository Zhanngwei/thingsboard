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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

/**
 * 中文说明：
 * 1. `OtaPackageControllerTest` 是 ThingsBoard Application 中验证 `OtaPackageController` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractControllerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class OtaPackageControllerTest extends AbstractControllerTest {

    private IdComparator<OtaPackageInfo> idComparator = new IdComparator<>();

    /**
     * `TITLE`常量，用于统一引用固定值。
     */
    public static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    /**
     * `CHECKSUM_ALGORITHM`常量，用于统一引用固定值。
     */
    private static final String CHECKSUM_ALGORITHM = "SHA256";
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{1});

    /**
     * 租户对象，用于描述当前业务场景。
     */
    private Tenant savedTenant;
    private User tenantAdmin;
    /**
     * 设备配置ID，用于定位对应业务对象。
     */
    private DeviceProfileId deviceProfileId;

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile");
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(savedDeviceProfile);
        deviceProfileId = savedDeviceProfile.getId();
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
     * 功能：验证`Save Firmware`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveFirmware() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        Mockito.reset(tbClusterService, auditLogService);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
        Assert.assertEquals(firmwareInfo.getVersion(), savedFirmwareInfo.getVersion());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedFirmwareInfo, savedFirmwareInfo.getId(), savedFirmwareInfo.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        save(new SaveOtaPackageInfoRequest(savedFirmwareInfo, false));

        OtaPackageInfo foundFirmwareInfo = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertEquals(foundFirmwareInfo.getTitle(), savedFirmwareInfo.getTitle());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundFirmwareInfo, foundFirmwareInfo.getId(), foundFirmwareInfo.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void saveOtaPackageInfoWithViolationOfLengthValidation() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(StringUtils.randomAlphabetic(300));
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);
        String msgError = msgErrorFieldLength("title");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/otaPackage", firmwareInfo)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        firmwareInfo.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(firmwareInfo,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));

        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("version");
        doPost("/api/otaPackage", firmwareInfo)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        firmwareInfo.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(firmwareInfo,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));

        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(true);
        msgError = msgErrorFieldLength("url");
        firmwareInfo.setUrl(StringUtils.randomAlphabetic(300));
        doPost("/api/otaPackage", firmwareInfo)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        firmwareInfo.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(firmwareInfo, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    /**
     * 功能：验证数据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveFirmwareData() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
        Assert.assertEquals(firmwareInfo.getVersion(), savedFirmwareInfo.getVersion());

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        save(new SaveOtaPackageInfoRequest(savedFirmwareInfo, false));

        OtaPackageInfo foundFirmwareInfo = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertEquals(foundFirmwareInfo.getTitle(), savedFirmwareInfo.getTitle());

        MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        Mockito.reset(tbClusterService, auditLogService);

        OtaPackage savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString()
                + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);

        Assert.assertEquals(FILE_NAME, savedFirmware.getFileName());
        Assert.assertEquals(CONTENT_TYPE, savedFirmware.getContentType());
        Assert.assertEquals(CHECKSUM_ALGORITHM, savedFirmware.getChecksumAlgorithm().name());
        Assert.assertEquals(CHECKSUM, savedFirmware.getChecksum());

        testNotifyEntityAllOneTime(savedFirmware, savedFirmware.getId(), savedFirmware.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateFirmwareFromDifferentTenant() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/otaPackage",
                new SaveOtaPackageInfoRequest(savedFirmwareInfo, false))
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedFirmwareInfo.getId(), savedFirmwareInfo);

        deleteDifferentTenant();
    }

    /**
     * 功能：验证信息对象相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindFirmwareInfoById() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        OtaPackageInfo foundFirmware = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmwareInfo, foundFirmware);
    }

    /**
     * 功能：验证`Find Firmware By Id`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindFirmwareById() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        OtaPackageInfo savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString()
                + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);

        OtaPackage foundFirmware = doGet("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString(), OtaPackage.class);
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, foundFirmware);
        Assert.assertEquals(DATA, foundFirmware.getData());
    }

    /**
     * 功能：验证`Delete Firmware`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteFirmware() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityAllOneTime(savedFirmwareInfo, savedFirmwareInfo.getId(), savedFirmwareInfo.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedFirmwareInfo.getId().getId().toString());

        doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTenantFirmwares() throws Exception {

        Mockito.reset(tbClusterService, auditLogService);

        List<OtaPackageInfo> otaPackages = new ArrayList<>();
        int cntEntity = 165;
        int startIndexSaveData = 101;
        for (int i = 0; i < cntEntity; i++) {
            SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
            firmwareInfo.setDeviceProfileId(deviceProfileId);
            firmwareInfo.setType(FIRMWARE);
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);
            firmwareInfo.setUsesUrl(false);

            OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

            if (i >= startIndexSaveData) {
                MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

                OtaPackage savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
                savedFirmwareInfo = new OtaPackageInfo(savedFirmware);
            }
            otaPackages.add(savedFirmwareInfo);
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new OtaPackageInfo(), new OtaPackageInfo(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, 0, (cntEntity*2 - startIndexSaveData));

        List<OtaPackageInfo> loadedFirmwares = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(otaPackages, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(otaPackages, loadedFirmwares);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTenantFirmwaresByHasData() throws Exception {
        List<OtaPackageInfo> otaPackagesWithData = new ArrayList<>();
        List<OtaPackageInfo> allOtaPackages = new ArrayList<>();

        for (int i = 0; i < 165; i++) {
            SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
            firmwareInfo.setDeviceProfileId(deviceProfileId);
            firmwareInfo.setType(FIRMWARE);
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);
            firmwareInfo.setUsesUrl(false);

            OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

            if (i > 100) {
                MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

                OtaPackage savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
                savedFirmwareInfo = new OtaPackageInfo(savedFirmware);
                otaPackagesWithData.add(savedFirmwareInfo);
            }

            allOtaPackages.add(savedFirmwareInfo);
        }

        List<OtaPackageInfo> loadedOtaPackagesWithData = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages/" + deviceProfileId.toString() + "/FIRMWARE?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedOtaPackagesWithData.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        List<OtaPackageInfo> allLoadedOtaPackages = new ArrayList<>();
        pageLink = new PageLink(24);
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages?",
                    new TypeReference<>() {
                    }, pageLink);
            allLoadedOtaPackages.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(otaPackagesWithData, idComparator);
        Collections.sort(allOtaPackages, idComparator);
        Collections.sort(loadedOtaPackagesWithData, idComparator);
        Collections.sort(allLoadedOtaPackages, idComparator);

        Assert.assertEquals(otaPackagesWithData, loadedOtaPackagesWithData);
        Assert.assertEquals(allOtaPackages, allLoadedOtaPackages);
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `firmwareInfo`：`firmwareInfo` 参数。
     * 返回：处理结果。
     */
    private OtaPackageInfo save(SaveOtaPackageInfoRequest firmwareInfo) throws Exception {
        return doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
    }

    /**
     * 功能：执行 `savaData` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected OtaPackage savaData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackage.class);
    }

}
