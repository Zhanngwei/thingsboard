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

@EqualsAndHashCode(callSuper = true)
@Data
/**
 * Push to Cloud 节点配置，继承基础 scope 配置。
 * 配置类本身不直接保存事件、不访问数据库/缓存，也不涉及异步回调。
 */
public class TbMsgPushToCloudNodeConfiguration extends BaseTbMsgPushNodeConfiguration {

    /**
     * 构造 Push to Cloud 节点默认配置。
     * 本方法只设置 SERVER_SCOPE，不直接推送或持久化消息。
     */
    @Override
    public TbMsgPushToCloudNodeConfiguration defaultConfiguration() {
        TbMsgPushToCloudNodeConfiguration configuration = new TbMsgPushToCloudNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类描述 push to cloud 节点的配置默认值，实际 Edge 侧事件保存和同步逻辑不在本配置类中。
 * 它不直接涉及外部调用、数据库、缓存或异步回调。
 */
