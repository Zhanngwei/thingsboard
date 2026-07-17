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
package org.thingsboard.server.transport.mqtt;

import io.netty.handler.ssl.SslHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.ProtoMqttAdaptor;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 04.10.18.
 */
/**
 * 中文说明：
 * 1. `MqttTransportContext` 是 ThingsBoard Common Transport 中承载 MQTT 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TransportContext`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
@Component
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.mqtt.enabled}'=='true')")
public class MqttTransportContext extends TransportContext {

    /**
     * 处理器，用于按场景创建或提供目标对象。
     */
    @Getter
    @Autowired(required = false)
    private MqttSslHandlerProvider sslHandlerProvider;

    /**
     * JSON，表示当前对象的对应属性。
     */
    @Getter
    @Autowired
    private JsonMqttAdaptor jsonMqttAdaptor;

    /**
     * Protobuf，表示当前对象的对应属性。
     */
    @Getter
    @Autowired
    private ProtoMqttAdaptor protoMqttAdaptor;

    /**
     * 消息载荷，保存消息正文内容。
     */
    @Getter
    @Value("${transport.mqtt.netty.max_payload_size}")
    private Integer maxPayloadSize;

    /**
     * 是否满足客户端条件。
     */
    @Getter
    @Value("${transport.mqtt.ssl.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Getter
    @Setter
    private SslHandler sslHandler;

    /**
     * 设备，承载当前步骤需要处理的内容。
     */
    @Getter
    @Value("${transport.mqtt.msg_queue_size_per_device_limit:100}")
    private int messageQueueSizePerDeviceLimit;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${transport.mqtt.timeout:10000}")
    private long timeout;

    /**
     * 是否启用`proxy`。
     */
    @Getter
    @Value("${transport.mqtt.proxy_enabled:false}")
    private boolean proxyEnabled;

    private final AtomicInteger connectionsCounter = new AtomicInteger();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        super.init();
        transportService.createGaugeStats("openConnections", connectionsCounter);
    }

    /**
     * 功能：执行 `channelRegistered` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void channelRegistered() {
        connectionsCounter.incrementAndGet();
    }

    /**
     * 功能：执行 `channelUnregistered` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void channelUnregistered() {
        connectionsCounter.decrementAndGet();
    }

    /**
     * 功能：校验`Address`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：判断结果。
     */
    public boolean checkAddress(InetSocketAddress address) {
        return rateLimitService.checkAddress(address);
    }

    /**
     * 功能：处理`on Auth Success`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    public void onAuthSuccess(InetSocketAddress address) {
        rateLimitService.onAuthSuccess(address);
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    public void onAuthFailure(InetSocketAddress address) {
        rateLimitService.onAuthFailure(address);
    }

}
