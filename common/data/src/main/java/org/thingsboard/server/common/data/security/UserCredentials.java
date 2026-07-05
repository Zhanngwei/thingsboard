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
 * 1. 类目的：`UserCredentials` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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

/*
 * 本类总结：
 * 1. 核心职责：`UserCredentials` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
