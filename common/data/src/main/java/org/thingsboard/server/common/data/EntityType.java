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
package org.thingsboard.server.common.data;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `EntityType` 是 ThingsBoard Common Data 中定义实体固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum EntityType {
    TENANT(1),
    CUSTOMER(2),
    USER(3),
    DASHBOARD(4),
    ASSET(5),
    DEVICE(6),
    ALARM (7),
    RULE_CHAIN (11),
    RULE_NODE (12),

    ENTITY_VIEW (15) {
        // backward compatibility for TbOriginatorTypeSwitchNode to return correct rule node connection.
        @Override
        public String getNormalName () {
            return "Entity View";
        }
    },
    WIDGETS_BUNDLE (16),
    WIDGET_TYPE (17),
    TENANT_PROFILE (20),
    DEVICE_PROFILE (21),
    ASSET_PROFILE (22),
    API_USAGE_STATE (23),
    TB_RESOURCE (24),
    OTA_PACKAGE (25),
    EDGE (26),
    RPC (27),
    QUEUE (28),
    NOTIFICATION_TARGET (29),
    NOTIFICATION_TEMPLATE (30),
    NOTIFICATION_REQUEST (31),
    NOTIFICATION (32),
    NOTIFICATION_RULE (33);

    /**
     * Protobuf，表示当前对象的对应属性。
     */
    @Getter
    private final int protoNumber; // Corresponds to EntityTypeProto

    /**
     * 功能：创建 `EntityType` 实例，并初始化必要字段。
     * 参数：
     * - `protoNumber`：`protoNumber` 参数。
     * 返回：新创建的对象实例。
     */
    private EntityType(int protoNumber) {
        this.protoNumber = protoNumber;
    }

    public static final List<String> NORMAL_NAMES = EnumSet.allOf(EntityType.class).stream()
            .map(EntityType::getNormalName).collect(Collectors.toUnmodifiableList());

    @Getter
    private final String normalName = StringUtils.capitalize(StringUtils.removeStart(name(), "TB_")
            .toLowerCase().replaceAll("_", " "));

}
