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
package org.thingsboard.server.service.device.provision;


import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.device.credentials.ProvisionDeviceCredentialsData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.device.DeviceProvisionServiceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `DeviceProvisionServiceTest` 是 ThingsBoard Application 中验证 `DeviceProvisionService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DeviceProvisionServiceImpl.class)
public class DeviceProvisionServiceTest {

    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    @MockBean
    protected TbQueueProducerProvider producerProvider;
    /**
     * 规则引擎，承载当前步骤需要处理的内容。
     */
    @MockBean
    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> ruleEngineMsgProducer;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected TbClusterService clusterService;
    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceProfileService deviceProfileService;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceService deviceService;
    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceCredentialsService deviceCredentialsService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected AttributesService attributesService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected AuditLogService auditLogService;
    /**
     * 分区，提供当前类调用的业务操作。
     */
    @MockBean
    protected PartitionService partitionService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @SpyBean
    DeviceProvisionServiceImpl service;

    /**
     * `chain`列表，用于保存一组待处理对象。
     */
    private String[] chain;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        String filePath = "src/test/resources/provision/x509ChainProvisionTest.pem";
        try {
            String certificateChain = Files.readString(Paths.get(filePath));
            certificateChain = certTrimNewLinesForChainInDeviceProfile(certificateChain);
            chain = fetchLeafCertificateFromChain(certificateChain);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 功能：执行 `provisionDeviceViaX509Certificate` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void provisionDeviceViaX509Certificate() {
        var tenant = createTenant();
        var deviceProfile = createDeviceProfile(tenant.getId(), chain[1], true);

        var device = createDevice(tenant.getId(), deviceProfile.getId());
        when(deviceService.findDeviceByTenantIdAndName(any(), any())).thenReturn(device);

        var deviceCredentials = createDeviceCredentials(chain[0], device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(any(), any())).thenReturn(deviceCredentials);
        when(deviceCredentialsService.updateDeviceCredentials(any(), any())).thenReturn(deviceCredentials);

        ProvisionResponse response = service.provisionDeviceViaX509Chain(deviceProfile, createProvisionRequest(chain[0]));

        verify(deviceService, times(1)).findDeviceByTenantIdAndName(any(), any());
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByDeviceId(any(), any());
        verify(deviceCredentialsService, times(1)).updateDeviceCredentials(any(), any());

        Assertions.assertThat(response.getResponseStatus()).isEqualTo(ProvisionResponseStatus.SUCCESS);
        Assertions.assertThat(response.getDeviceCredentials()).isEqualTo(deviceCredentials);
    }

    /**
     * 功能：执行 `provisionDeviceWithIncorrectConfiguration` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void provisionDeviceWithIncorrectConfiguration() {
        var tenant = createTenant();
        var deviceProfile = createDeviceProfile(tenant.getId(), chain[1], false);

        Assertions.assertThatThrownBy(() ->
                        service.provisionDeviceViaX509Chain(deviceProfile, createProvisionRequest(chain[0])))
                .isInstanceOf(ProvisionFailedException.class);

        verify(deviceService, times(1)).findDeviceByTenantIdAndName(any(), any());
    }

    /**
     * 功能：执行 `matchDeviceNameFromX509CNCertificateByRegex` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void matchDeviceNameFromX509CNCertificateByRegex() {
        var tenant = createTenant();
        var deviceProfile = createDeviceProfile(tenant.getId(), chain[1], true);
        X509CertificateChainProvisionConfiguration configuration = (X509CertificateChainProvisionConfiguration) deviceProfile.getProfileData().getProvisionConfiguration();
        String CN = getCNFromX509Certificate(chain[0]);
        String deviceName = service.extractDeviceNameFromCNByRegEx(deviceProfile, CN, configuration.getCertificateRegExPattern());

        Assertions.assertThat(deviceName).isNotBlank();
        Assertions.assertThat(deviceName).isEqualTo("deviceCertificate");
    }

    /**
     * 功能：执行 `matchDeviceNameFromCNByRegex` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void matchDeviceNameFromCNByRegex() {
        var CN = "DeviceA.company.com";
        var regex = "(.*)\\.company.com";
        var result = service.extractDeviceNameFromCNByRegEx(null, CN, regex);
        Assertions.assertThat(result).isNotBlank();
        Assertions.assertThat(result).isEqualTo("DeviceA");

        CN = "DeviceA@company.com";
        regex = "(.*)@company.com";
        result = service.extractDeviceNameFromCNByRegEx(null, CN, regex);
        Assertions.assertThat(result).isNotBlank();
        Assertions.assertThat(result).isEqualTo("DeviceA");

        CN = "prefixDeviceAsuffix@company.com";
        regex = "prefix(.*)suffix@company.com";
        result = service.extractDeviceNameFromCNByRegEx(null, CN, regex);
        Assertions.assertThat(result).isNotBlank();
        Assertions.assertThat(result).isEqualTo("DeviceA");

        CN = "prefixDeviceAsufix@company.com";
        regex = "prefix(.*)sufix@company.com";
        result = service.extractDeviceNameFromCNByRegEx(null, CN, regex);
        Assertions.assertThat(result).isNotBlank();
        Assertions.assertThat(result).isEqualTo("DeviceA");

        CN = "region.DeviceA.220423@company.com";
        regex = "\\D+\\.(.*)\\.\\d+@company.com";
        result = service.extractDeviceNameFromCNByRegEx(null, CN, regex);
        Assertions.assertThat(result).isNotBlank();
        Assertions.assertThat(result).isEqualTo("DeviceA");
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `certificateValue`：值。
     * - `isAllowToCreateNewDevices`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private DeviceProfile createDeviceProfile(TenantId tenantId, String certificateValue, boolean isAllowToCreateNewDevices) {
        X509CertificateChainProvisionConfiguration provision = new X509CertificateChainProvisionConfiguration();
        provision.setProvisionDeviceSecret(certificateValue);
        provision.setCertificateRegExPattern("([^@]+)");
        provision.setAllowCreateNewDevicesByX509Certificate(isAllowToCreateNewDevices);

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setProvisionConfiguration(provision);

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(new DeviceProfileId(UUID.randomUUID()));
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setProvisionDeviceKey(EncryptionUtil.getSha3Hash(certificateValue));
        deviceProfile.setProvisionType(DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN);
        deviceProfile.setTenantId(tenantId);
        return deviceProfile;
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    private Device createDevice(TenantId tenantId, DeviceProfileId deviceProfileId) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setId(new DeviceId(UUID.randomUUID()));
        device.setDeviceProfileId(deviceProfileId);
        device.setCustomerId(new CustomerId(UUID.randomUUID()));
        return device;
    }

    /**
     * 功能：保存或创建租户。
     * 参数：无。
     * 返回：处理结果。
     */
    private Tenant createTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(new TenantId(UUID.randomUUID()));
        return tenant;
    }

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `certificateValue`：值。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    private DeviceCredentials createDeviceCredentials(String certificateValue, DeviceId deviceId) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(deviceId);
        deviceCredentials.setCredentialsValue(certificateValue);
        deviceCredentials.setCredentialsId(EncryptionUtil.getSha3Hash(certificateValue));
        deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        return deviceCredentials;
    }

    /**
     * 功能：保存或创建请求。
     * 参数：
     * - `certificateValue`：值。
     * 返回：处理结果。
     */
    private ProvisionRequest createProvisionRequest(String certificateValue) {
        return new ProvisionRequest(null, DeviceCredentialsType.X509_CERTIFICATE,
                new ProvisionDeviceCredentialsData(null, null, null, null, certificateValue),
                null);
    }

    /**
     * 功能：执行 `certTrimNewLinesForChainInDeviceProfile` 对应的处理。
     * 参数：
     * - `input`：`input` 参数。
     * 返回：文本结果。
     */
    public static String certTrimNewLinesForChainInDeviceProfile(String input) {
        return input.replaceAll("\n", "")
                .replaceAll("\r", "")
                .replaceAll("-----BEGIN CERTIFICATE-----", "-----BEGIN CERTIFICATE-----\n")
                .replaceAll("-----END CERTIFICATE-----", "\n-----END CERTIFICATE-----\n")
                .trim();
    }

    /**
     * 功能：获取证书。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    private String[] fetchLeafCertificateFromChain(String value) {
        List<String> chain = new ArrayList<>();
        String regex = "-----BEGIN CERTIFICATE-----\\s*.*?\\s*-----END CERTIFICATE-----";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            chain.add(matcher.group(0));
        }
        return chain.toArray(new String[0]);
    }

    /**
     * 功能：获取证书。
     * 参数：
     * - `x509Value`：值。
     * 返回：文本结果。
     */
    private String getCNFromX509Certificate(String x509Value) {
        try {
            return SslUtil.parseCommonName(SslUtil.readCertFile(x509Value));
        } catch (Exception e) {
            return null;
        }
    }
}
