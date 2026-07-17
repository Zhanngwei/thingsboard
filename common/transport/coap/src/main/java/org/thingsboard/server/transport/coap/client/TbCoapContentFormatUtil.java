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
package org.thingsboard.server.transport.coap.client;

import org.eclipse.californium.core.coap.MediaTypeRegistry;

/**
 * 中文说明：
 * 1. `TbCoapContentFormatUtil` 是 ThingsBoard Common Transport 中处理 CoAP 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class TbCoapContentFormatUtil {

    /**
     * 功能：获取内容格式。
     * 参数：
     * - `requestFormat`：请求对象。
     * - `adaptorFormat`：`adaptorFormat` 参数。
     * 返回：数值结果。
     */
    public static int getContentFormat(int requestFormat, int adaptorFormat) {
        if (isStrict(adaptorFormat)) {
            return adaptorFormat;
        } else {
            return requestFormat != MediaTypeRegistry.UNDEFINED ? requestFormat : adaptorFormat;
        }
    }

    /**
     * 功能：判断`Strict`。
     * 参数：
     * - `contentFormat`：`contentFormat` 参数。
     * 返回：判断结果。
     */
    public static boolean isStrict(int contentFormat) {
        return contentFormat == MediaTypeRegistry.APPLICATION_OCTET_STREAM;
    }
}
