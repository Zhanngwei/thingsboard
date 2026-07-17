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
package org.thingsboard.server.transport.coap.adaptors;

import org.eclipse.californium.core.coap.Request;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 中文说明：
 * 1. `CoapAdaptorUtils` 是 ThingsBoard Common Transport 中处理 CoAP 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class CoapAdaptorUtils {

    /**
     * 功能：执行 `toGetAttributeRequestMsg` 对应的处理。
     * 参数：
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    public static TransportProtos.GetAttributeRequestMsg toGetAttributeRequestMsg(Request inbound) throws AdaptorException {
        List<String> queryElements = inbound.getOptions().getUriQuery();
        TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
        if (queryElements != null && queryElements.size() > 0) {
            Set<String> clientKeys = toKeys(queryElements, "clientKeys");
            Set<String> sharedKeys = toKeys(queryElements, "sharedKeys");
            if (clientKeys != null) {
                result.addAllClientAttributeNames(clientKeys);
            }
            if (sharedKeys != null) {
                result.addAllSharedAttributeNames(sharedKeys);
            }
        }
        result.setOnlyShared(false);
        return result.build();
    }

    /**
     * 功能：执行 `toKeys` 对应的处理。
     * 参数：
     * - `queryElements`：数据列表。
     * - `attributeName`：名称。
     * 返回：匹配的数据集合。
     */
    private static Set<String> toKeys(List<String> queryElements, String attributeName) throws AdaptorException {
        String keys = null;
        for (String queryElement : queryElements) {
            String[] queryItem = queryElement.split("=");
            if (queryItem.length == 2 && queryItem[0].equals(attributeName)) {
                keys = queryItem[1];
            }
        }
        if (keys != null && !StringUtils.isEmpty(keys)) {
            return new HashSet<>(Arrays.asList(keys.split(",")));
        } else {
            return null;
        }
    }
}
