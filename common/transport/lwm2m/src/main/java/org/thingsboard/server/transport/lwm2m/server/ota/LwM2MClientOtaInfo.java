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
package org.thingsboard.server.transport.lwm2m.server.ota;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `LwM2MClientOtaInfo` 是 ThingsBoard Common Transport 中承载 LwM2M 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@NoArgsConstructor
public abstract class LwM2MClientOtaInfo<Strategy, State, Result> {

    /**
     * `endpoint` 字段，保存当前对象的对应属性。
     */
    private String endpoint;
    private String baseUrl;

    /**
     * 名称，用于标识或展示当前对象。
     */
    protected String targetName;
    protected String targetVersion;
    /**
     * 目标对象，表示当前对象的对应属性。
     */
    protected String targetTag;
    protected String targetUrl;

    //TODO: use value from device if applicable;
    /**
     * 策略对象，封装可复用的处理规则。
     */
    protected Strategy strategy;
    protected State updateState;
    /**
     * `result` 字段，保存当前对象的对应属性。
     */
    protected Result result;
    protected OtaPackageUpdateStatus status;

    /**
     * `failedPackageId`ID，用于定位对应业务对象。
     */
    protected String failedPackageId;
    protected int retryAttempts;

    /**
     * 名称，用于标识或展示当前对象。
     */
    protected String currentName;
    protected String currentVersion3;
    /**
     * 版本号，表示当前对象的对应属性。
     */
    protected String currentVersion;

    /**
     * 功能：创建 `LwM2MClientOtaInfo` 实例，并初始化必要字段。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * - `baseUrl`：`baseUrl` 参数。
     * - `strategy`：`strategy` 参数。
     * 返回：新创建的对象实例。
     */
    public LwM2MClientOtaInfo(String endpoint, String baseUrl, Strategy strategy) {
        this.endpoint = endpoint;
        this.baseUrl = baseUrl;
        this.strategy = strategy;
    }

    /**
     * 功能：更新目标对象。
     * 参数：
     * - `targetName`：名称。
     * - `targetVersion`：`targetVersion` 参数。
     * - `newTargetUrl`：`newTargetUrl` 参数。
     * - `newTargetTag`：`newTargetTag` 参数。
     * 返回：无。
     */
    public void updateTarget(String targetName, String targetVersion, Optional<String> newTargetUrl, Optional<String> newTargetTag) {
        this.targetName = targetName;
        this.targetVersion = targetVersion;
        this.targetUrl = newTargetUrl.orElse(null);
        this.targetTag = newTargetTag.orElse(null);
    }

    /**
     * 功能：判断`Update Required`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isUpdateRequired() {
        if (StringUtils.isEmpty(targetName) || StringUtils.isEmpty(targetVersion) || !isSupported()) {
            return false;
        } else {
            String targetPackageId = getPackageId(targetName, targetVersion);
            String currentPackageId = getPackageId(currentName, currentVersion);
            if (StringUtils.isNotEmpty(failedPackageId) && failedPackageId.equals(targetPackageId)) {
                return false;
            } else {
                if (targetPackageId.equals(currentPackageId)) {
                    return false;
                } else if (StringUtils.isNotEmpty(targetTag) && targetTag.equals(currentPackageId)) {
                    return false;
                } else if (StringUtils.isNotEmpty(currentVersion3)) {
                    if (StringUtils.isNotEmpty(targetTag) && currentVersion3.contains(targetTag)) {
                        return false;
                    }
                    return !currentVersion3.contains(targetPackageId);
                } else {
                    return true;
                }
            }
        }
    }

    /**
     * 功能：判断`Supported`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isSupported() {
        return StringUtils.isNotEmpty(currentName) || StringUtils.isNotEmpty(currentVersion) || StringUtils.isNotEmpty(currentVersion3);
    }

    /**
     * 功能：判断`Assigned`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isAssigned() {
        return StringUtils.isNotEmpty(targetName) && StringUtils.isNotEmpty(targetVersion);
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    public abstract void update(Result result);

    /**
     * 功能：获取`Package Id`。
     * 参数：
     * - `name`：名称。
     * - `version`：`version` 参数。
     * 返回：文本结果。
     */
    protected static String getPackageId(String name, String version) {
        return (StringUtils.isNotEmpty(name) ? name : "") + (StringUtils.isNotEmpty(version) ? version : "");
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    public abstract OtaPackageType getType();

    /**
     * 功能：获取目标对象。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonIgnore
    public String getTargetPackageId() {
        return getPackageId(targetName, targetVersion);
    }
}
