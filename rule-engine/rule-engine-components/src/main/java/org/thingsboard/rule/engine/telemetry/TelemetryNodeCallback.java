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

import com.google.common.util.concurrent.FutureCallback;
import lombok.Data;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.msg.TbMsg;

import javax.annotation.Nullable;

/**
 * Created by ashvayka on 02.04.18.
 */
/**
 * 中文说明：`TelemetryNodeCallback` 是遥测节点回调辅助类，用于保存、删除或通知属性与时间序列遥测数据。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
@Data
class TelemetryNodeCallback implements FutureCallback<Void> {
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final TbContext ctx;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private final TbMsg msg;

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    @Override
    public void onSuccess(@Nullable Void result) {
        ctx.tellSuccess(msg);
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：无。
     */
    @Override
    public void onFailure(Throwable t) {
        ctx.tellFailure(msg, t);
    }
    /*
     * 本类总结：`TelemetryNodeCallback` 负责保存、删除或通知属性与时间序列遥测数据；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
