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
package org.thingsboard.rule.engine.notification;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.id.NotificationTemplateId;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * `TbNotificationNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbNotificationNodeConfiguration implements NodeConfiguration<TbNotificationNodeConfiguration> {

    /**
     * `targets`列表，用于保存一组待处理对象。
     */
    @NotEmpty
    private List<UUID> targets;
    /**
     * `templateId`ID，用于定位对应业务对象。
     */
    @NotNull
    private NotificationTemplateId templateId;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbNotificationNodeConfiguration defaultConfiguration() {
        return new TbNotificationNodeConfiguration();
    }

}

/*
 * 本类总结：
 * 本类描述通知发送节点的目标和模板配置，实际通知请求处理由 TbNotificationNode 与 NotificationCenter 完成。
 * 配置类本身不直接涉及外部调用、数据库、缓存或异步回调。
 */
