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
package org.thingsboard.server.service.transport;


import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceProvisionService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `DefaultTransportApiServiceTest` 是 ThingsBoard Application 中验证 `DefaultTransportApiService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTransportApiService.class)
public class DefaultTransportApiServiceTest {

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @MockBean
    protected TbDeviceProfileCache deviceProfileCache;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @MockBean
    protected TbTenantProfileCache tenantProfileCache;
    /**
     * 状态，提供当前类调用的业务操作。
     */
    @MockBean
    protected TbApiUsageStateService apiUsageStateService;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceService deviceService;
    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceProfileService deviceProfileService;
    /**
     * 关系，提供当前类调用的业务操作。
     */
    @MockBean
    protected RelationService relationService;
    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceCredentialsService deviceCredentialsService;
    /**
     * 回调，提供当前类调用的业务操作。
     */
    @MockBean
    protected DbCallbackExecutorService dbCallbackExecutorService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected TbClusterService tbClusterService;
    /**
     * 数据，提供当前类调用的业务操作。
     */
    @MockBean
    protected DataDecodingEncodingService dataDecodingEncodingService;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @MockBean
    protected DeviceProvisionService deviceProvisionService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected ResourceService resourceService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    protected OtaPackageService otaPackageService;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @MockBean
    protected OtaPackageDataCache otaPackageDataCache;
    /**
     * 队列，提供当前类调用的业务操作。
     */
    @MockBean
    protected QueueService queueService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @SpyBean
    DefaultTransportApiService service;

    /**
     * 证书，用于认证或安全校验。
     */
    private String certificateChain;
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
            certificateChain = Files.readString(Paths.get(filePath));
            certificateChain = certTrimNewLinesForChainInDeviceProfile(certificateChain);
            chain = fetchLeafCertificateFromChain(certificateChain);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：校验证书。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void validateExistingDeviceByX509CertificateStrategy() {
        var device = createDevice();

        var deviceCredentials = createDeviceCredentials(chain[0], device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByCredentialsId(any())).thenReturn(deviceCredentials);

        TransportProtos.TransportApiResponseMsg response = mock(TransportProtos.TransportApiResponseMsg.class);
        willReturn(response).given(service).getDeviceInfo(deviceCredentials);

        service.validateOrCreateDeviceX509Certificate(certificateChain);
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByCredentialsId(any());
    }

    /**
     * 功能：执行 `provisionDeviceX509Certificate` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void provisionDeviceX509Certificate() {
        var deviceProfile = createDeviceProfile(chain[1]);
        when(deviceProfileService.findDeviceProfileByProvisionDeviceKey(any())).thenReturn(deviceProfile);

        var device = createDevice();
        when(deviceService.findDeviceByTenantIdAndName(any(), any())).thenReturn(device);

        var deviceCredentials = createDeviceCredentials(chain[0], device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByCredentialsId(any())).thenReturn(null);
        when(deviceCredentialsService.updateDeviceCredentials(any(), any())).thenReturn(deviceCredentials);

        var provisionResponse = createProvisionResponse(deviceCredentials);
        when(deviceProvisionService.provisionDeviceViaX509Chain(any(), any())).thenReturn(provisionResponse);

        TransportProtos.TransportApiResponseMsg response = mock(TransportProtos.TransportApiResponseMsg.class);
        willReturn(response).given(service).getDeviceInfo(deviceCredentials);

        service.validateOrCreateDeviceX509Certificate(certificateChain);
        verify(deviceProfileService, times(1)).findDeviceProfileByProvisionDeviceKey(any());
        verify(service, times(1)).getDeviceInfo(any());
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByCredentialsId(any());
        verify(deviceProvisionService, times(1)).provisionDeviceViaX509Chain(any(), any());
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `certificateValue`：值。
     * 返回：处理结果。
     */
    private DeviceProfile createDeviceProfile(String certificateValue) {
        X509CertificateChainProvisionConfiguration provision = new X509CertificateChainProvisionConfiguration();
        provision.setProvisionDeviceSecret(certificateValue);
        provision.setCertificateRegExPattern("([^@]+)");
        provision.setAllowCreateNewDevicesByX509Certificate(true);

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setProvisionConfiguration(provision);

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setProvisionDeviceKey(EncryptionUtil.getSha3Hash(certificateValue));
        deviceProfile.setProvisionType(DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN);
        return deviceProfile;
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
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：处理结果。
     */
    private Device createDevice() {
        Device device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        return device;
    }

    /**
     * 功能：保存或创建响应。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private ProvisionResponse createProvisionResponse(DeviceCredentials deviceCredentials) {
        return new ProvisionResponse(deviceCredentials, ProvisionResponseStatus.SUCCESS);
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
}
