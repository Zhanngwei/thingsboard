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
package org.thingsboard.server.transport.lwm2m.server;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MAuthorizer;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;

/**
 * 中文说明：
 * 1. `DefaultLwM2mTransportServiceTest` 是 ThingsBoard Common Transport 中验证 `DefaultLwM2mTransportService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@ExtendWith(MockitoExtension.class)
public class DefaultLwM2mTransportServiceTest {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private LwM2mTransportContext context;

    /**
     * 配置，保存当前对象的配置选项。
     */
    @Mock
    private LwM2MTransportServerConfig config;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @Mock
    private OtaPackageDataCache otaPackageDataCache;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Mock
    private LwM2mUplinkMsgHandler handler;
    /**
     * 存储组件，表示当前对象的对应属性。
     */
    @Mock
    private CaliforniumRegistrationStore registrationStore;
    /**
     * 存储组件，表示当前对象的对应属性。
     */
    @Mock
    private TbSecurityStore securityStore;
    /**
     * 证书，用于认证或安全校验。
     */
    @Mock
    private TbLwM2MDtlsCertificateVerifier certificateVerifier;
    /**
     * `authorizer` 字段，保存当前对象的对应属性。
     */
    @Mock
    private TbLwM2MAuthorizer authorizer;
    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    @Mock
    private LwM2mVersionedModelProvider modelProvider;


    /**
     * 功能：验证 `getLHServer_creates_ConnectionIdGenerator_when_connection_id_length_not_null` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void getLHServer_creates_ConnectionIdGenerator_when_connection_id_length_not_null(){
        final Integer CONNECTION_ID_LENGTH = 6;
        when(config.getDtlsConnectionIdLength()).thenReturn(CONNECTION_ID_LENGTH);
        var lwm2mService = createLwM2MService();

        LeshanServer server = ReflectionTestUtils.invokeMethod(lwm2mService, "getLhServer");

        assertThat(server).isNotNull();
        var securedEndpoint = (CoapEndpoint) ReflectionTestUtils.getField(server, "securedEndpoint");
        assertThat(securedEndpoint).isNotNull();

        var config = (DtlsConnectorConfig) ReflectionTestUtils.getField(securedEndpoint.getConnector(), "config");
        assertThat(config).isNotNull();
        assertThat(config.getConnectionIdGenerator()).isNotNull();
        assertThat((Integer) ReflectionTestUtils.getField(config.getConnectionIdGenerator(), "connectionIdLength"))
                .isEqualTo(CONNECTION_ID_LENGTH);
    }

    /**
     * 功能：验证 `getLHServer_creates_no_ConnectionIdGenerator_when_connection_id_length_is_null` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void getLHServer_creates_no_ConnectionIdGenerator_when_connection_id_length_is_null(){
        when(config.getDtlsConnectionIdLength()).thenReturn(null);
        var lwm2mService = createLwM2MService();

        LeshanServer server = ReflectionTestUtils.invokeMethod(lwm2mService, "getLhServer");

        assertThat(server).isNotNull();
        var securedEndpoint = (CoapEndpoint) ReflectionTestUtils.getField(server, "securedEndpoint");
        assertThat(securedEndpoint).isNotNull();
        var config = (DtlsConnectorConfig) ReflectionTestUtils.getField(securedEndpoint.getConnector(), "config");
        assertThat(config).isNotNull();
        assertThat(config.getConnectionIdGenerator()).isNull();
    }

    /**
     * 功能：保存或创建服务。
     * 参数：无。
     * 返回：处理结果。
     */
    private DefaultLwM2mTransportService createLwM2MService() {
        setDefaultConfigVariables();
        return new DefaultLwM2mTransportService(context, config, otaPackageDataCache, handler, registrationStore,
                securityStore, certificateVerifier, authorizer, modelProvider);
    }

    /**
     * 功能：更新配置。
     * 参数：无。
     * 返回：无。
     */
    private void setDefaultConfigVariables(){
        when(config.getPort()).thenReturn(5683);
        when(config.getSecurePort()).thenReturn(5684);
        when(config.isRecommendedCiphers()).thenReturn(false);
        when(config.getDtlsRetransmissionTimeout()).thenReturn(9000);
    }
}