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
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.util.mapping.JsonBinaryType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `UserSettingsEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `ToData`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Entity
@Table(name = ModelConstants.USER_SETTINGS_TABLE_NAME)
@IdClass(UserSettingsCompositeKey.class)
public class UserSettingsEntity implements ToData<UserSettings> {

    /**
     * 用户ID，用于定位对应业务对象。
     */
    @Id
    @Column(name = ModelConstants.USER_SETTINGS_USER_ID_PROPERTY)
    private UUID userId;
    /**
     * 类型，用于区分不同处理分支。
     */
    @Id
    @Column(name = ModelConstants.USER_SETTINGS_TYPE_PROPERTY)
    private String type;
    /**
     * 配置，保存当前对象的配置选项。
     */
    @Type(type = "jsonb")
    @Column(name = ModelConstants.USER_SETTINGS_SETTINGS, columnDefinition = "jsonb")
    private JsonNode settings;

    /**
     * 功能：创建 `UserSettingsEntity` 实例，并初始化必要字段。
     * 参数：
     * - `userSettings`：配置对象。
     * 返回：新创建的对象实例。
     */
    public UserSettingsEntity(UserSettings userSettings) {
        this.userId = userSettings.getUserId().getId();
        this.type = userSettings.getType().name();
        if (userSettings.getSettings() != null) {
            this.settings = userSettings.getSettings();
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public UserSettings toData() {
        UserSettings userSettings = new UserSettings();
        userSettings.setUserId(new UserId(userId));
        userSettings.setType(UserSettingsType.valueOf(type));
        if (settings != null) {
            userSettings.setSettings(settings);
        }
        return userSettings;
    }

}
