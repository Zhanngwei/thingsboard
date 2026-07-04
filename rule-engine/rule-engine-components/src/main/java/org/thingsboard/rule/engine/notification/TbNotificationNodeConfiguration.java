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

@Data
/**
 * 通知发送节点配置，保存通知目标和模板 ID。
 * 配置类本身不直接发送通知、不访问数据库/缓存，也不涉及异步回调。
 */
public class TbNotificationNodeConfiguration implements NodeConfiguration<TbNotificationNodeConfiguration> {

    /**
     * 通知目标 UUID 列表。
     */
    @NotEmpty
    private List<UUID> targets;
    /**
     * 通知模板 ID。
     */
    @NotNull
    private NotificationTemplateId templateId;

    /**
     * 构造通知节点默认配置。
     * 本方法只返回空配置对象，不直接读取模板或发送通知。
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
