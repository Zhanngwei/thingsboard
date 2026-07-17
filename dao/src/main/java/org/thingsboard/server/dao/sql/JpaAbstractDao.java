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
package org.thingsboard.server.dao.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Valerii Sosliuk
 */
/**
 * 中文说明：
 * 1. `JpaAbstractDao` 是 ThingsBoard DAO 中负责 `Jpa` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `BaseEntity`、`Dao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@SqlDao
public abstract class JpaAbstractDao<E extends BaseEntity<D>, D>
        extends JpaAbstractDaoListeningExecutorService
        implements Dao<D> {

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract Class<E> getEntityClass();

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract JpaRepository<E, UUID> getRepository();

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `domain`：`domain` 参数。
     * 返回：处理结果。
     */
    @Override
    @Transactional
    public D save(TenantId tenantId, D domain) {
        E entity;
        try {
            entity = getEntityClass().getConstructor(domain.getClass()).newInstance(domain);
        } catch (Exception e) {
            log.error("Can't create entity for domain object {}", domain, e);
            throw new IllegalArgumentException("Can't create entity for domain object {" + domain + "}", e);
        }
        log.debug("Saving entity {}", entity);
        boolean isNew = entity.getUuid() == null;
        if (isNew) {
            UUID uuid = Uuids.timeBased();
            entity.setUuid(uuid);
            entity.setCreatedTime(Uuids.unixTimestamp(uuid));
        }
        entity = doSave(entity, isNew);
        return DaoUtil.getData(entity);
    }

    /**
     * 功能：执行 `doSave` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `isNew`：`isNew` 参数。
     * 返回：处理结果。
     */
    protected E doSave(E entity, boolean isNew) {
        return getRepository().save(entity);
    }

    /**
     * 功能：保存或创建`And Flush`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `domain`：`domain` 参数。
     * 返回：处理结果。
     */
    @Override
    @Transactional
    public D saveAndFlush(TenantId tenantId, D domain) {
        D d = save(tenantId, domain);
        getRepository().flush();
        return d;
    }

    /**
     * 功能：获取`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public D findById(TenantId tenantId, UUID key) {
        log.debug("Get entity by key {}", key);
        Optional<E> entity = getRepository().findById(key);
        return DaoUtil.getData(entity);
    }

    /**
     * 功能：获取`By Id Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<D> findByIdAsync(TenantId tenantId, UUID key) {
        log.debug("Get entity by key async {}", key);
        return service.submit(() -> DaoUtil.getData(getRepository().findById(key)));
    }

    /**
     * 功能：判断`By Id`是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：判断结果。
     */
    @Override
    public boolean existsById(TenantId tenantId, UUID key) {
        log.debug("Exists by key {}", key);
        return getRepository().existsById(key);
    }

    /**
     * 功能：判断`By Id Async`是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：判断结果。
     */
    @Override
    public ListenableFuture<Boolean> existsByIdAsync(TenantId tenantId, UUID key) {
        log.debug("Exists by key async {}", key);
        return service.submit(() -> getRepository().existsById(key));
    }

    /**
     * 功能：删除或清理`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：判断结果。
     */
    @Override
    @Transactional
    public boolean removeById(TenantId tenantId, UUID id) {
        getRepository().deleteById(id);
        log.debug("Remove request: {}", id);
        return !getRepository().existsById(id);
    }

    /**
     * 功能：删除或清理`All By Ids`。
     * 参数：
     * - `ids`：数据列表。
     * 返回：无。
     */
    @Transactional
    public void removeAllByIds(Collection<UUID> ids) {
        JpaRepository<E, UUID> repository = getRepository();
        ids.forEach(repository::deleteById);
    }

    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<D> find(TenantId tenantId) {
        List<E> entities = Lists.newArrayList(getRepository().findAll());
        return DaoUtil.convertDataList(entities);
    }

}
