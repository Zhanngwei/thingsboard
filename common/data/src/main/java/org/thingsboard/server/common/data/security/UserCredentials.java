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
package org.thingsboard.server.common.data.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.NoXss;

import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.getJson;
import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.setJson;

/**
 * 中文说明：
 * 1. `UserCredentials` 是 ThingsBoard Common Data 中承载用户信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@EqualsAndHashCode(callSuper = true)
public class UserCredentials extends BaseData<UserCredentialsId> {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -2108436378880529163L;

    /**
     * 用户ID，用于定位对应业务对象。
     */
    private UserId userId;
    private boolean enabled;
    /**
     * 密码，用于认证或安全校验。
     */
    private String password;
    private String activateToken;
    /**
     * 令牌，用于认证或安全校验。
     */
    private String resetToken;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @NoXss
    private transient JsonNode additionalInfo;

    /**
     * 扩展信息列表，用于保存一组待处理对象。
     */
    @JsonIgnore
    private byte[] additionalInfoBytes;

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    public JsonNode getAdditionalInfo() {
        return getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }

    /**
     * 功能：更新扩展信息。
     * 参数：
     * - `settings`：配置对象。
     * 返回：无。
     */
    public void setAdditionalInfo(JsonNode settings) {
        setJson(settings, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
    }
    
    /**
     * 功能：创建 `UserCredentials` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public UserCredentials() {
        super();
    }

    /**
     * 功能：创建 `UserCredentials` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public UserCredentials(UserCredentialsId id) {
        super(id);
    }

    /**
     * 功能：创建 `UserCredentials` 实例，并初始化必要字段。
     * 参数：
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：新创建的对象实例。
     */
    public UserCredentials(UserCredentials userCredentials) {
        super(userCredentials);
        this.userId = userCredentials.getUserId();
        this.password = userCredentials.getPassword();
        this.enabled = userCredentials.isEnabled();
        this.activateToken = userCredentials.getActivateToken();
        this.resetToken = userCredentials.getResetToken();
        setAdditionalInfo(userCredentials.getAdditionalInfo());
    }

    /**
     * 功能：获取用户。
     * 参数：无。
     * 返回：处理结果。
     */
    public UserId getUserId() {
        return userId;
    }

    /**
     * 功能：更新用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：无。
     */
    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    /**
     * 功能：判断`Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 功能：更新`Enabled`。
     * 参数：
     * - `enabled`：`enabled` 参数。
     * 返回：无。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 功能：获取密码。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getPassword() {
        return password;
    }

    /**
     * 功能：更新密码。
     * 参数：
     * - `password`：`password` 参数。
     * 返回：无。
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 功能：获取令牌。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getActivateToken() {
        return activateToken;
    }

    /**
     * 功能：更新令牌。
     * 参数：
     * - `activateToken`：`activateToken` 参数。
     * 返回：无。
     */
    public void setActivateToken(String activateToken) {
        this.activateToken = activateToken;
    }
    
    /**
     * 功能：获取令牌。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getResetToken() {
        return resetToken;
    }

    /**
     * 功能：更新令牌。
     * 参数：
     * - `resetToken`：`resetToken` 参数。
     * 返回：无。
     */
    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserCredentials [userId=");
        builder.append(userId);
        builder.append(", enabled=");
        builder.append(enabled);
        builder.append(", password=");
        builder.append(password);
        builder.append(", activateToken=");
        builder.append(activateToken);
        builder.append(", resetToken=");
        builder.append(resetToken);
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}
