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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `UserCredentialsEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`、`BaseEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.USER_CREDENTIALS_TABLE_NAME)
public final class UserCredentialsEntity extends BaseSqlEntity<UserCredentials> implements BaseEntity<UserCredentials> {

    /**
     * 用户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.USER_CREDENTIALS_USER_ID_PROPERTY, unique = true)
    private UUID userId;

    /**
     * 是否启用`enabled`。
     */
    @Column(name = ModelConstants.USER_CREDENTIALS_ENABLED_PROPERTY)
    private boolean enabled;

    /**
     * 密码，用于认证或安全校验。
     */
    @Column(name = ModelConstants.USER_CREDENTIALS_PASSWORD_PROPERTY)
    private String password;

    /**
     * 令牌，用于认证或安全校验。
     */
    @Column(name = ModelConstants.USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY, unique = true)
    private String activateToken;

    /**
     * 令牌，用于认证或安全校验。
     */
    @Column(name = ModelConstants.USER_CREDENTIALS_RESET_TOKEN_PROPERTY, unique = true)
    private String resetToken;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.USER_CREDENTIALS_ADDITIONAL_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * 功能：创建 `UserCredentialsEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public UserCredentialsEntity() {
        super();
    }

    /**
     * 功能：创建 `UserCredentialsEntity` 实例，并初始化必要字段。
     * 参数：
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：新创建的对象实例。
     */
    public UserCredentialsEntity(UserCredentials userCredentials) {
        if (userCredentials.getId() != null) {
            this.setUuid(userCredentials.getId().getId());
        }
        this.setCreatedTime(userCredentials.getCreatedTime());
        if (userCredentials.getUserId() != null) {
            this.userId = userCredentials.getUserId().getId();
        }
        this.enabled = userCredentials.isEnabled();
        this.password = userCredentials.getPassword();
        this.activateToken = userCredentials.getActivateToken();
        this.resetToken = userCredentials.getResetToken();
        this.additionalInfo = userCredentials.getAdditionalInfo();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public UserCredentials toData() {
        UserCredentials userCredentials = new UserCredentials(new UserCredentialsId(this.getUuid()));
        userCredentials.setCreatedTime(createdTime);
        if (userId != null) {
            userCredentials.setUserId(new UserId(userId));
        }
        userCredentials.setEnabled(enabled);
        userCredentials.setPassword(password);
        userCredentials.setActivateToken(activateToken);
        userCredentials.setResetToken(resetToken);
        userCredentials.setAdditionalInfo(additionalInfo);
        return userCredentials;
    }

}
