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
package org.thingsboard.server.transport.mqtt.mqttv5.client.unsubscribe;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttUnsubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_UNSUBACK;

/**
 * 中文说明：
 * 1. `AbstractMqttV5ClientUnsubscribeTest` 是 ThingsBoard Application 中验证 `AbstractMqttV5ClientUnsubscribe` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public abstract class AbstractMqttV5ClientUnsubscribeTest extends AbstractMqttIntegrationTest {

    /**
     * 功能：处理主题。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientUnsubscribeFromCorrectTopicTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        client.subscribeAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedDevice.getId(), FeatureType.ATTRIBUTES, 1);
        IMqttToken unsubscribeResult = client.unsubscribeAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC);
        MqttWireMessage response = unsubscribeResult.getResponse();
        Assert.assertEquals(MESSAGE_TYPE_UNSUBACK, response.getType());
        MqttUnsubAck unsubAckMsg = (MqttUnsubAck) response;
        Assert.assertEquals(1, unsubAckMsg.getReturnCodes().length);
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, unsubAckMsg.getReturnCodes()[0]);

        client.disconnect();
    }

    /**
     * 功能：处理主题。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientUnsubscribeWithoutSubscribeTopicTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        IMqttToken iMqttToken = client.unsubscribeAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC);
        Assert.assertEquals(MESSAGE_TYPE_UNSUBACK, iMqttToken.getResponse().getType());
        MqttUnsubAck unsubAck = (MqttUnsubAck) iMqttToken.getResponse();
        Assert.assertEquals(1, unsubAck.getReturnCodes().length);
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_NO_SUBSCRIPTION_EXISTED, unsubAck.getReturnCodes()[0]);

        client.disconnect();
    }

}
