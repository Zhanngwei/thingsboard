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
package org.thingsboard.server.service.sms.smpp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.smpp.Session;
import org.smpp.pdu.SubmitSMResp;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.sms.config.SmppSmsProviderConfiguration;

import java.lang.reflect.Constructor;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `SmppSmsSenderTest` 是 ThingsBoard Application 中验证 `SmppSmsSender` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@RunWith(MockitoJUnitRunner.class)
public class SmppSmsSenderTest {

    /**
     * `smppSmsSender` 字段，保存当前对象的对应属性。
     */
    SmppSmsSender smppSmsSender;
    SmppSmsProviderConfiguration smppConfig;
    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    Session smppSession;

    /**
     * 功能：执行 `beforeEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeEach() throws Exception {
        Constructor<SmppSmsSender> constructor = SmppSmsSender.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        smppSmsSender = spy(constructor.newInstance());

        smppSession = mock(Session.class);
        smppSmsSender.smppSession = smppSession;

        smppConfig = new SmppSmsProviderConfiguration();
        smppSmsSender.config = smppConfig;
    }

    /**
     * 功能：验证`Send Sms`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSendSms() throws Exception {
        when(smppSession.isOpened()).thenReturn(true);
        when(smppSession.submit(any())).thenReturn(new SubmitSMResp());
        setDefaultSmppConfig();

        String number = "123545";
        String message = "message";
        smppSmsSender.sendSms(number, message);

        verify(smppSmsSender, never()).initSmppSession();
        verify(smppSession).submit(argThat(submitRequest -> {
            try {
                return submitRequest.getShortMessage().equals(message) &&
                        submitRequest.getDestAddr().getAddress().equals(number) &&
                        submitRequest.getServiceType().equals(smppConfig.getServiceType()) &&
                        (StringUtils.isEmpty(smppConfig.getSourceAddress()) ? submitRequest.getSourceAddr().getAddress().equals("")
                                : submitRequest.getSourceAddr().getAddress().equals(smppConfig.getSourceAddress()) &&
                                submitRequest.getSourceAddr().getTon() == smppConfig.getSourceTon() &&
                                submitRequest.getSourceAddr().getNpi() == smppConfig.getSourceNpi()) &&
                        submitRequest.getDestAddr().getTon() == smppConfig.getDestinationTon() &&
                        submitRequest.getDestAddr().getNpi() == smppConfig.getDestinationNpi() &&
                        submitRequest.getDataCoding() == smppConfig.getCodingScheme() &&
                        submitRequest.getReplaceIfPresentFlag() == 0 &&
                        submitRequest.getEsmClass() == 0 &&
                        submitRequest.getProtocolId() == 0 &&
                        submitRequest.getPriorityFlag() == 0 &&
                        submitRequest.getRegisteredDelivery() == 0 &&
                        submitRequest.getSmDefaultMsgId() == 0;
            } catch (Exception e) {
                fail(e.getMessage());
                return false;
            }
        }));
    }

    /**
     * 功能：更新配置。
     * 参数：无。
     * 返回：无。
     */
    private void setDefaultSmppConfig() {
        smppConfig.setProtocolVersion("3.3");
        smppConfig.setHost("smpphost");
        smppConfig.setPort(5687);
        smppConfig.setSystemId("213131");
        smppConfig.setPassword("35125q");

        smppConfig.setSystemType("");
        smppConfig.setBindType(SmppSmsProviderConfiguration.SmppBindType.TX);
        smppConfig.setServiceType("");

        smppConfig.setSourceAddress("");
        smppConfig.setSourceTon((byte) 5);
        smppConfig.setSourceNpi((byte) 0);

        smppConfig.setDestinationTon((byte) 5);
        smppConfig.setDestinationNpi((byte) 0);

        smppConfig.setAddressRange("");
        smppConfig.setCodingScheme((byte) 0);
    }

}
