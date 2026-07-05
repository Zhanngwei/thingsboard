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
package org.thingsboard.rule.engine.telemetry;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;

import java.util.Collections;
import java.util.List;

/**
 * 中文说明：`TbMsgDeleteAttributesNodeConfiguration` 是消息删除属性节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
 */
@Data
public class TbMsgDeleteAttributesNodeConfiguration implements NodeConfiguration<TbMsgDeleteAttributesNodeConfiguration> {

    /**
     * `scope` 字段，保存当前对象的对应属性。
     */
    private String scope;
    /**
     * `keys`列表，用于保存一组待处理对象。
     */
    private List<String> keys;
    /**
     * 当前对象是否已经删除。
     */
    private boolean sendAttributesDeletedNotification;
    /**
     * 是否满足设备条件。
     */
    private boolean notifyDevice;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbMsgDeleteAttributesNodeConfiguration defaultConfiguration() {
        TbMsgDeleteAttributesNodeConfiguration configuration = new TbMsgDeleteAttributesNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        configuration.setKeys(Collections.emptyList());
        configuration.setSendAttributesDeletedNotification(false);
        configuration.setNotifyDevice(false);
        return configuration;
    }
    /*
     * 本类总结：`TbMsgDeleteAttributesNodeConfiguration` 负责保存、删除或通知属性与时间序列遥测数据；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
