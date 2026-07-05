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

import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 中文说明：`AttributesUpdateNodeCallback` 是属性更新节点回调辅助类，用于保存、删除或通知属性与时间序列遥测数据。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
public class AttributesUpdateNodeCallback extends TelemetryNodeCallback {

    /**
     * `scope` 字段，保存当前对象的对应属性。
     */
    private final String scope;
    /**
     * `attributes`列表，用于保存一组待处理对象。
     */
    private final List<AttributeKvEntry> attributes;

    /**
     * 功能：创建 `AttributesUpdateNodeCallback` 实例，并初始化必要字段。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * 返回：新创建的对象实例。
     */
    public AttributesUpdateNodeCallback(TbContext ctx, TbMsg msg, String scope, List<AttributeKvEntry> attributes) {
        super(ctx, msg);
        this.scope = scope;
        this.attributes = attributes;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    @Override
    public void onSuccess(@Nullable Void result) {
        TbContext ctx = this.getCtx();
        TbMsg tbMsg = this.getMsg();
        ctx.enqueue(ctx.attributesUpdatedActionMsg(tbMsg.getOriginator(), ctx.getSelfId(), scope, attributes),
                () -> ctx.tellSuccess(tbMsg),
                throwable -> ctx.tellFailure(tbMsg, throwable));
    }
    /*
     * 本类总结：`AttributesUpdateNodeCallback` 负责保存、删除或通知属性与时间序列遥测数据；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
