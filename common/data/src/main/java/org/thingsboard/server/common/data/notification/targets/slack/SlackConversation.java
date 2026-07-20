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
package org.thingsboard.server.common.data.notification.targets.slack;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * 中文说明：
 * 1. `SlackConversation` 是 ThingsBoard Common Data 中承载 `Slack Conversation` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `NotificationRecipient`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackConversation implements NotificationRecipient {

    /**
     * 类型，用于区分不同处理分支。
     */
    @NotNull
    private SlackConversationType type;
    /**
     * `id`ID，用于定位对应业务对象。
     */
    @NotEmpty
    private String id;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NotEmpty
    private String name;

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String wholeName;
    private String email;

    /**
     * 功能：获取`Title`。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getTitle() {
        if (type == SlackConversationType.DIRECT) {
            return StringUtils.defaultIfEmpty(wholeName, name);
        } else {
            return name;
        }
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonIgnore
    @Override
    public String getFirstName() {
        String firstName = StringUtils.contains(wholeName, " ") ? wholeName.split(" ")[0] : wholeName;
        if (isEmpty(firstName)) {
            firstName = name;
        }
        return firstName;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonIgnore
    @Override
    public String getLastName() {
        return StringUtils.contains(wholeName, " ") ? wholeName.split(" ")[1] : null;
    }

    /**
     * 功能：获取`Pointer`。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonIgnore
    public String getPointer() {
        return type == SlackConversationType.DIRECT ? "@" : "#";
    }

}
