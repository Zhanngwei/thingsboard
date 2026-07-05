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
package org.thingsboard.rule.engine.edge;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.DataConstants;

/**
 * `TbMsgPushToEdgeNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TbMsgPushToEdgeNodeConfiguration extends BaseTbMsgPushNodeConfiguration {

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbMsgPushToEdgeNodeConfiguration defaultConfiguration() {
        TbMsgPushToEdgeNodeConfiguration configuration = new TbMsgPushToEdgeNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类描述 push to edge 节点的配置默认值，实际 EdgeEvent 构造、保存、数据库回调和 Edge 通知由 TbMsgPushToEdgeNode 完成。
 * 它不直接涉及外部调用、数据库、缓存或异步回调。
 */
