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
@Data
/**
 * 中文说明：`TelemetryNodeCallback` 是遥测节点回调辅助类，用于保存、删除或通知属性与时间序列遥测数据。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
class TelemetryNodeCallback implements FutureCallback<Void> {
    /**
     * 字段说明：保存 Rule Engine 上下文引用；本字段本身不直接代表数据库、MQTT 或事务资源。
     */
    private final TbContext ctx;
    /**
     * 字段说明：保存 `msg`，表示当前规则链消息，供本类方法在规则节点处理流程中使用。
     */
    private final TbMsg msg;

    @Override
    /**
     * 方法说明：处理异步调用成功回调并继续规则链投递。
     * 调用边界：由异步 Future 或消息回调触发；本方法本身只衔接规则链结果，数据库、缓存、MQTT 或事务通常发生在触发该回调的上游调用链中。
     */
    public void onSuccess(@Nullable Void result) {
        ctx.tellSuccess(msg);
    }

    @Override
    /**
     * 方法说明：处理异步调用失败回调并转入失败关系。
     * 调用边界：由异步 Future 或消息回调触发；本方法本身只衔接规则链结果，数据库、缓存、MQTT 或事务通常发生在触发该回调的上游调用链中。
     */
    public void onFailure(Throwable t) {
        ctx.tellFailure(msg, t);
    }
    /*
     * 本类总结：`TelemetryNodeCallback` 负责保存、删除或通知属性与时间序列遥测数据；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
