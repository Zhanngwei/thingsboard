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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.OAuth2MobileId;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.oauth2.OAuth2Mobile;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`OAuth2MobileEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.OAUTH2_MOBILE_TABLE_NAME)
public class OAuth2MobileEntity extends BaseSqlEntity<OAuth2Mobile> {

    /**
     * `oauth2ParamsId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.OAUTH2_PARAMS_ID_PROPERTY)
    private UUID oauth2ParamsId;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.OAUTH2_PKG_NAME_PROPERTY)
    private String pkgName;

    /**
     * 密钥，用于认证或安全校验。
     */
    @Column(name = ModelConstants.OAUTH2_APP_SECRET_PROPERTY)
    private String appSecret;

    /**
     * 功能：创建 `OAuth2MobileEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public OAuth2MobileEntity() {
        super();
    }

    /**
     * 功能：创建 `OAuth2MobileEntity` 实例，并初始化必要字段。
     * 参数：
     * - `mobile`：`mobile` 参数。
     * 返回：新创建的对象实例。
     */
    public OAuth2MobileEntity(OAuth2Mobile mobile) {
        if (mobile.getId() != null) {
            this.setUuid(mobile.getId().getId());
        }
        this.setCreatedTime(mobile.getCreatedTime());
        if (mobile.getOauth2ParamsId() != null) {
            this.oauth2ParamsId = mobile.getOauth2ParamsId().getId();
        }
        this.pkgName = mobile.getPkgName();
        this.appSecret = mobile.getAppSecret();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public OAuth2Mobile toData() {
        OAuth2Mobile mobile = new OAuth2Mobile();
        mobile.setId(new OAuth2MobileId(id));
        mobile.setCreatedTime(createdTime);
        mobile.setOauth2ParamsId(new OAuth2ParamsId(oauth2ParamsId));
        mobile.setPkgName(pkgName);
        mobile.setAppSecret(appSecret);
        return mobile;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2MobileEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
