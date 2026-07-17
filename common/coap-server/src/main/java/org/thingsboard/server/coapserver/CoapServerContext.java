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
package org.thingsboard.server.coapserver;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 中文说明：
 * 1. `CoapServerContext` 是 ThingsBoard Common 中承载 CoAP 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
@TbCoapServerComponent
@Component
public class CoapServerContext {

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Getter
    @Value("${transport.coap.bind_address}")
    private String host;

    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Getter
    @Value("${transport.coap.bind_port}")
    private Integer port;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${transport.coap.timeout}")
    private Long timeout;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${transport.coap.piggyback_timeout}")
    private Long piggybackTimeout;

    /**
     * 定时器，用于安排延迟任务或周期任务。
     */
    @Getter
    @Value("${transport.coap.psm_activity_timer:10000}")
    private long psmActivityTimer;

    /**
     * `pagingTransmissionWindow` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Value("${transport.coap.paging_transmission_window:10000}")
    private long pagingTransmissionWindow;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    @Autowired(required = false)
    private TbCoapDtlsSettings dtlsSettings;

}
