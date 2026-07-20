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
package org.thingsboard.server.common.transport.service;

import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.kafka.TbKafkaDecoder;

import java.io.IOException;

/**
 * Created by ashvayka on 05.10.18.
 */
/**
 * 中文说明：
 * 1. `TransportApiResponseDecoder` 是 ThingsBoard Common Transport 中转换响应数据结构的适配组件。
 * 2. 它把输入对象、协议内容或持久化数据转换为目标模型。
 * 3. 转换过程负责字段映射、格式解析以及必要的默认值处理。
 * 4. 直接依赖的类型边界包括 `TbKafkaDecoder`。
 * 5. 独立转换器可以避免不同模块重复编写并逐渐分叉的映射逻辑。
 * 6. 阅读时重点关注字段对应关系、空值处理和不兼容输入的处理方式。
 */
public class TransportApiResponseDecoder implements TbKafkaDecoder<TransportApiResponseMsg> {

    /**
     * 功能：执行 `decode` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    public TransportApiResponseMsg decode(TbQueueMsg msg) throws IOException {
        return TransportApiResponseMsg.parseFrom(msg.getData());
    }
}
