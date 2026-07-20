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
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_DATA_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_DESCRIPTOR_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_ETAG_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_FILE_NAME_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_IS_PUBLIC_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_PREVIEW_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.PUBLIC_RESOURCE_KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TENANT_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TITLE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.RESOURCE_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

/**
 * 中文说明：
 * 1. `TbResourceEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
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
@Table(name = RESOURCE_TABLE_NAME)
public class TbResourceEntity extends BaseSqlEntity<TbResource> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = RESOURCE_TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @Column(name = RESOURCE_TITLE_COLUMN)
    private String title;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Column(name = RESOURCE_TYPE_COLUMN)
    private String resourceType;

    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Column(name = RESOURCE_KEY_COLUMN)
    private String resourceKey;

    /**
     * 搜索文本，表示当前对象的对应属性。
     */
    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = RESOURCE_FILE_NAME_COLUMN)
    private String fileName;

    /**
     * 数据列表，用于保存一组待处理对象。
     */
    @Column(name = RESOURCE_DATA_COLUMN)
    private byte[] data;

    /**
     * `etag` 字段，保存当前对象的对应属性。
     */
    @Column(name = RESOURCE_ETAG_COLUMN)
    private String etag;

    /**
     * `descriptor` 字段，保存当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = RESOURCE_DESCRIPTOR_COLUMN)
    private JsonNode descriptor;

    /**
     * `preview`列表，用于保存一组待处理对象。
     */
    @Column(name = RESOURCE_PREVIEW_COLUMN)
    private byte[] preview;

    /**
     * 当前对象是否公开可见。
     */
    @Column(name = RESOURCE_IS_PUBLIC_COLUMN)
    private Boolean isPublic;

    /**
     * 公钥，用于定位映射、配置或数据项。
     */
    @Column(name = PUBLIC_RESOURCE_KEY_COLUMN, unique = true, updatable = false)
    private String publicResourceKey;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `TbResourceEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TbResourceEntity() {
    }

    /**
     * 功能：创建 `TbResourceEntity` 实例，并初始化必要字段。
     * 参数：
     * - `resource`：`resource` 参数。
     * 返回：新创建的对象实例。
     */
    public TbResourceEntity(TbResource resource) {
        if (resource.getId() != null) {
            this.id = resource.getId().getId();
        }
        this.createdTime = resource.getCreatedTime();
        if (resource.getTenantId() != null) {
            this.tenantId = resource.getTenantId().getId();
        }
        this.title = resource.getTitle();
        this.resourceType = resource.getResourceType().name();
        this.resourceKey = resource.getResourceKey();
        this.searchText = resource.getSearchText();
        this.fileName = resource.getFileName();
        this.data = resource.getData();
        this.etag = resource.getEtag();
        this.descriptor = resource.getDescriptor();
        this.preview = resource.getPreview();
        this.isPublic = resource.isPublic();
        this.publicResourceKey = resource.getPublicResourceKey();
        this.externalId = getUuid(resource.getExternalId());
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbResource toData() {
        TbResource resource = new TbResource(new TbResourceId(id));
        resource.setCreatedTime(createdTime);
        resource.setTenantId(TenantId.fromUUID(tenantId));
        resource.setTitle(title);
        resource.setResourceType(ResourceType.valueOf(resourceType));
        resource.setResourceKey(resourceKey);
        resource.setSearchText(searchText);
        resource.setFileName(fileName);
        resource.setData(data);
        resource.setEtag(etag);
        resource.setDescriptor(descriptor);
        resource.setPreview(preview);
        resource.setPublic(isPublic == null || isPublic);
        resource.setPublicResourceKey(publicResourceKey);
        resource.setExternalId(getEntityId(externalId, TbResourceId::new));
        return resource;
    }

}
