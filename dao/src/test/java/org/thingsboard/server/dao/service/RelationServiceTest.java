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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 中文说明：
 * 1. `RelationServiceTest` 是 ThingsBoard DAO 中验证 `RelationService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractServiceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class RelationServiceTest extends AbstractServiceTest {

    /**
     * 关系，提供当前类调用的业务操作。
     */
    @Autowired
    RelationService relationService;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() {
    }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() {
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveRelation() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(saveRelation(relation));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, "NOT_EXISTING_TYPE", RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, parentId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, parentId, "NOT_EXISTING_TYPE", RelationTypeGroup.COMMON));
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteRelation() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());
        AssetId subChildId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);

        Assert.assertTrue(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, relationA).get());

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteRelationConcurrently() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);

        List<ListenableFuture<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, relationA));
        }
        List<Boolean> results = Futures.allAsList(futures).get();
        Assert.assertTrue(results.contains(true));
    }

    /**
     * 功能：验证实体相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteEntityRelations() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());
        AssetId subChildId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);

        relationService.deleteEntityRelations(SYSTEM_TENANT_ID, childId);

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));
    }

    /**
     * 功能：验证实体相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteEntityCommonRelations() {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());
        AssetId subChildId = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(parentId, childId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE);
        EntityRelation relationD = new EntityRelation(childId, subChildId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);

        relationService.deleteEntityCommonRelations(SYSTEM_TENANT_ID, childId);

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));
        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE));
        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.MANAGES_TYPE, RelationTypeGroup.EDGE));
    }

    /**
     * 功能：验证`Find From`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindFrom() throws ExecutionException, InterruptedException {
        AssetId parentA = new AssetId(Uuids.timeBased());
        AssetId parentB = new AssetId(Uuids.timeBased());
        AssetId childA = new AssetId(Uuids.timeBased());
        AssetId childB = new AssetId(Uuids.timeBased());

        EntityRelation relationA1 = new EntityRelation(parentA, childA, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationA2 = new EntityRelation(parentA, childB, EntityRelation.CONTAINS_TYPE);

        EntityRelation relationB1 = new EntityRelation(parentB, childA, EntityRelation.MANAGES_TYPE);
        EntityRelation relationB2 = new EntityRelation(parentB, childB, EntityRelation.MANAGES_TYPE);

        saveRelation(relationA1);
        saveRelation(relationA2);

        saveRelation(relationB1);
        saveRelation(relationB2);

        List<EntityRelation> relations = relationService.findByFrom(SYSTEM_TENANT_ID, parentA, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(EntityRelation.CONTAINS_TYPE, relation.getType());
            Assert.assertEquals(parentA, relation.getFrom());
            Assert.assertTrue(childA.equals(relation.getTo()) || childB.equals(relation.getTo()));
        }

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByFrom(SYSTEM_TENANT_ID, parentB, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(EntityRelation.MANAGES_TYPE, relation.getType());
            Assert.assertEquals(parentB, relation.getFrom());
            Assert.assertTrue(childA.equals(relation.getTo()) || childB.equals(relation.getTo()));
        }

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());
    }

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `relationA1`：`relationA1` 参数。
     * 返回：判断结果。
     */
    private Boolean saveRelation(EntityRelation relationA1) {
        return relationService.saveRelation(SYSTEM_TENANT_ID, relationA1);
    }

    /**
     * 功能：验证`Find To`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTo() throws ExecutionException, InterruptedException {
        AssetId parentA = new AssetId(Uuids.timeBased());
        AssetId parentB = new AssetId(Uuids.timeBased());
        AssetId childA = new AssetId(Uuids.timeBased());
        AssetId childB = new AssetId(Uuids.timeBased());

        EntityRelation relationA1 = new EntityRelation(parentA, childA, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationA2 = new EntityRelation(parentA, childB, EntityRelation.CONTAINS_TYPE);

        EntityRelation relationB1 = new EntityRelation(parentB, childA, EntityRelation.MANAGES_TYPE);
        EntityRelation relationB2 = new EntityRelation(parentB, childB, EntityRelation.MANAGES_TYPE);

        saveRelation(relationA1);
        saveRelation(relationA2);

        saveRelation(relationB1);
        saveRelation(relationB2);

        List<EntityRelation> relations = relationService.findByTo(SYSTEM_TENANT_ID, childA, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(childA, relation.getTo());
            Assert.assertTrue(parentA.equals(relation.getFrom()) || parentB.equals(relation.getFrom()));
        }

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, childA, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(1, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, childB, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(1, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByTo(SYSTEM_TENANT_ID, childB, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (EntityRelation relation : relations) {
            Assert.assertEquals(childB, relation.getTo());
            Assert.assertTrue(parentA.equals(relation.getFrom()) || parentB.equals(relation.getFrom()));
        }
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCyclicRecursiveRelation() throws ExecutionException, InterruptedException {
        // A -> B -> C -> A
        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetA, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRecursiveRelation() throws ExecutionException, InterruptedException {
        // A -> B -> [C,D]
        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        DeviceId deviceD = new DeviceId(Uuids.timeBased());

        EntityRelation relationAB = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBC = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBD = new EntityRelation(assetB, deviceD, EntityRelation.CONTAINS_TYPE);


        saveRelation(relationAB);
        saveRelation(relationBC);
        saveRelation(relationBD);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(2, relations.size());
        Assert.assertTrue(relations.contains(relationAB));
        Assert.assertTrue(relations.contains(relationBC));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(2, relations.size());
        Assert.assertTrue(relations.contains(relationAB));
        Assert.assertTrue(relations.contains(relationBC));
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRecursiveRelationDepth() throws ExecutionException, InterruptedException {
        int maxLevel = 1000;
        AssetId root = new AssetId(Uuids.timeBased());
        AssetId left = new AssetId(Uuids.timeBased());
        AssetId right = new AssetId(Uuids.timeBased());

        List<EntityRelation> expected = new ArrayList<>();

        EntityRelation relationAB = new EntityRelation(root, left, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationBC = new EntityRelation(root, right, EntityRelation.CONTAINS_TYPE);
        saveRelation(relationAB);
        expected.add(relationAB);

        saveRelation(relationBC);
        expected.add(relationBC);

        for (int i = 0; i < maxLevel; i++) {
            var newLeft = new AssetId(Uuids.timeBased());
            var newRight = new AssetId(Uuids.timeBased());
            EntityRelation relationLeft = new EntityRelation(left, newLeft, EntityRelation.CONTAINS_TYPE);
            EntityRelation relationRight = new EntityRelation(right, newRight, EntityRelation.CONTAINS_TYPE);
            saveRelation(relationLeft);
            expected.add(relationLeft);
            saveRelation(relationRight);
            expected.add(relationRight);
            left = newLeft;
            right = newRight;
        }


        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(root, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expected.size(), relations.size());
        for(EntityRelation r : expected){
            Assert.assertTrue(relations.contains(r));
        }

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expected.size(), relations.size());
        for(EntityRelation r : expected){
            Assert.assertTrue(relations.contains(r));
        }
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveRelationWithEmptyFrom() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setTo(new AssetId(Uuids.timeBased()));
        relation.setType(EntityRelation.CONTAINS_TYPE);
        Assertions.assertThrows(DataValidationException.class, () -> {
            Assert.assertTrue(saveRelation(relation));
        });
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveRelationWithEmptyTo() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(Uuids.timeBased()));
        relation.setType(EntityRelation.CONTAINS_TYPE);
        Assertions.assertThrows(DataValidationException.class, () -> {
            Assert.assertTrue(saveRelation(relation));
        });
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveRelationWithEmptyType() throws ExecutionException, InterruptedException {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(Uuids.timeBased()));
        relation.setTo(new AssetId(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            Assert.assertTrue(saveRelation(relation));
        });
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByQueryFetchLastOnlyTreeLike() throws Exception {
        // A -> B
        // A -> C
        // C -> D
        // C -> E

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByQueryFetchLastOnlySingleLinked() throws Exception {
        // A -> B -> C -> D

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(1, relations.size());
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertFalse(relations.contains(relationA));
        Assert.assertFalse(relations.contains(relationB));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertFalse(relations.contains(relationA));
        Assert.assertFalse(relations.contains(relationB));
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByQueryFetchLastOnlyTreeLikeWithMaxLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());
        AssetId assetF = new AssetId(Uuids.timeBased());
        AssetId assetG = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);
        saveRelation(relationE);
        saveRelation(relationF);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, 2, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByQueryTreeLikeWithMaxLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());
        AssetId assetF = new AssetId(Uuids.timeBased());
        AssetId assetG = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);
        saveRelation(relationE);
        saveRelation(relationF);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, 2, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(4, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByQueryTreeLikeWithUnlimLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        AssetId assetA = new AssetId(Uuids.timeBased());
        AssetId assetB = new AssetId(Uuids.timeBased());
        AssetId assetC = new AssetId(Uuids.timeBased());
        AssetId assetD = new AssetId(Uuids.timeBased());
        AssetId assetE = new AssetId(Uuids.timeBased());
        AssetId assetF = new AssetId(Uuids.timeBased());
        AssetId assetG = new AssetId(Uuids.timeBased());

        EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);
        saveRelation(relationE);
        saveRelation(relationF);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(6, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertTrue(relations.contains(relationE));
        Assert.assertTrue(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertTrue(relations.contains(relationE));
        Assert.assertTrue(relations.contains(relationF));
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByQueryLargeHierarchyFetchAllWithUnlimLvl() throws Exception {
        AssetId rootAsset = new AssetId(Uuids.timeBased());
        final int hierarchyLvl = 10;
        List<EntityRelation> expectedRelations = new LinkedList<>();

        createAssetRelationsRecursively(rootAsset, hierarchyLvl, expectedRelations, false);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(rootAsset, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expectedRelations.size(), relations.size());
        Assert.assertTrue(relations.containsAll(expectedRelations));
    }

    /**
     * 功能：验证查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByQueryLargeHierarchyFetchLastOnlyWithUnlimLvl() throws Exception {
        AssetId rootAsset = new AssetId(Uuids.timeBased());
        final int hierarchyLvl = 10;
        List<EntityRelation> expectedRelations = new LinkedList<>();

        createAssetRelationsRecursively(rootAsset, hierarchyLvl, expectedRelations, true);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(rootAsset, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expectedRelations.size(), relations.size());
        Assert.assertTrue(relations.containsAll(expectedRelations));
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `rootAsset`：`rootAsset` 参数。
     * - `lvl`：`lvl` 参数。
     * - `entityRelations`：实体对象。
     * - `lastLvlOnly`：`lastLvlOnly` 参数。
     * 返回：无。
     */
    private void createAssetRelationsRecursively(AssetId rootAsset, int lvl, List<EntityRelation> entityRelations, boolean lastLvlOnly) throws Exception {
        if (lvl == 0) return;

        AssetId firstAsset = new AssetId(Uuids.timeBased());
        AssetId secondAsset = new AssetId(Uuids.timeBased());

        EntityRelation firstRelation = new EntityRelation(rootAsset, firstAsset, EntityRelation.CONTAINS_TYPE);
        EntityRelation secondRelation = new EntityRelation(rootAsset, secondAsset, EntityRelation.CONTAINS_TYPE);

        saveRelation(firstRelation);
        saveRelation(secondRelation);

        if (!lastLvlOnly || lvl == 1) {
            entityRelations.add(firstRelation);
            entityRelations.add(secondRelation);
        }

        createAssetRelationsRecursively(firstAsset, lvl - 1, entityRelations, lastLvlOnly);
        createAssetRelationsRecursively(secondAsset, lvl - 1, entityRelations, lastLvlOnly);
    }
}
