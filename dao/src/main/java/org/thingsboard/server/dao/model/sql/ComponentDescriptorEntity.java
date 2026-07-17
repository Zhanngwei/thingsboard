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
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * 中文说明：
 * 1. `ComponentDescriptorEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.COMPONENT_DESCRIPTOR_TABLE_NAME)
public class ComponentDescriptorEntity extends BaseSqlEntity<ComponentDescriptor> {

    /**
     * 类型，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY)
    private ComponentType type;

    /**
     * `scope` 字段，保存当前对象的对应属性。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_SCOPE_PROPERTY)
    private ComponentScope scope;

    /**
     * `clusteringMode` 字段，保存当前对象的对应属性。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CLUSTERING_MODE_PROPERTY)
    private ComponentClusteringMode clusteringMode;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_NAME_PROPERTY)
    private String name;

    /**
     * `clazz` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY, unique = true)
    private String clazz;

    /**
     * `configurationDescriptor`，保存当前对象的配置选项。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY)
    private JsonNode configurationDescriptor;

    /**
     * 版本号，保存当前对象的配置选项。
     */
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CONFIGURATION_VERSION_PROPERTY)
    private int configurationVersion;

    /**
     * `actions` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY)
    private String actions;

    /**
     * 是否包含队列名称。
     */
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_HAS_QUEUE_NAME_PROPERTY)
    private boolean hasQueueName;

    /**
     * 功能：创建 `ComponentDescriptorEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public ComponentDescriptorEntity() {
    }

    /**
     * 功能：创建 `ComponentDescriptorEntity` 实例，并初始化必要字段。
     * 参数：
     * - `component`：`component` 参数。
     * 返回：新创建的对象实例。
     */
    public ComponentDescriptorEntity(ComponentDescriptor component) {
        if (component.getId() != null) {
            this.setUuid(component.getId().getId());
        }
        this.setCreatedTime(component.getCreatedTime());
        this.actions = component.getActions();
        this.type = component.getType();
        this.scope = component.getScope();
        this.clusteringMode = component.getClusteringMode();
        this.name = component.getName();
        this.clazz = component.getClazz();
        this.configurationDescriptor = component.getConfigurationDescriptor();
        this.configurationVersion = component.getConfigurationVersion();
        this.hasQueueName = component.isHasQueueName();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public ComponentDescriptor toData() {
        ComponentDescriptor data = new ComponentDescriptor(new ComponentDescriptorId(this.getUuid()));
        data.setCreatedTime(createdTime);
        data.setType(type);
        data.setScope(scope);
        data.setClusteringMode(clusteringMode);
        data.setName(this.getName());
        data.setClazz(this.getClazz());
        data.setActions(this.getActions());
        data.setConfigurationDescriptor(configurationDescriptor);
        data.setConfigurationVersion(configurationVersion);
        data.setHasQueueName(hasQueueName);
        return data;
    }
}
