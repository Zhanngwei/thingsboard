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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.DelivererException;
import org.eclipse.californium.core.server.ServerMessageDeliverer;
import org.eclipse.californium.core.server.resources.Resource;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 中文说明：
 * 1. `TbCoapServerMessageDeliverer` 是 ThingsBoard Common 中承载消息信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `ServerMessageDeliverer`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
public class TbCoapServerMessageDeliverer extends ServerMessageDeliverer {

    /**
     * 功能：创建 `TbCoapServerMessageDeliverer` 实例，并初始化必要字段。
     * 参数：
     * - `root`：`root` 参数。
     * 返回：新创建的对象实例。
     */
    public TbCoapServerMessageDeliverer(Resource root) {
        super(root);
    }

    /**
     * 功能：获取`Resource`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：处理结果。
     */
    @Override
    protected Resource findResource(Exchange exchange) throws DelivererException {
        validateUriPath(exchange);
        return findResource(exchange.getRequest().getOptions().getUriPath());
    }

    /**
     * 功能：校验路径。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    private void validateUriPath(Exchange exchange) {
        OptionSet options = exchange.getRequest().getOptions();
        List<String> uriPathList = options.getUriPath();
        String path = toPath(uriPathList);
        if (path != null) {
            options.setUriPath(path);
            exchange.getRequest().setOptions(options);
        }
    }

    /**
     * 功能：执行 `toPath` 对应的处理。
     * 参数：
     * - `list`：数据列表。
     * 返回：文本结果。
     */
    private String toPath(List<String> list) {
        if (!CollectionUtils.isEmpty(list) && list.size() == 1) {
            final String slash = "/";
            String path = list.get(0);
            if (path.startsWith(slash)) {
                path = path.substring(slash.length());
            }
            return path;
        }
        return null;
    }
}