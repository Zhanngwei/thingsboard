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
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;

/**
 * `BaseTbMsgPushNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class BaseTbMsgPushNodeConfiguration implements NodeConfiguration<BaseTbMsgPushNodeConfiguration> {

    /**
     * `scope` 字段，保存当前对象的对应属性。
     */
    private String scope;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public BaseTbMsgPushNodeConfiguration defaultConfiguration() {
        BaseTbMsgPushNodeConfiguration configuration = new BaseTbMsgPushNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类只保存 Edge/Cloud 推送节点共用的 scope 配置。
 * 实际消息处理、事件保存、数据库回调和远端同步由具体节点类完成。
 */
