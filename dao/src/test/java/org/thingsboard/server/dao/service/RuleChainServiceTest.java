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
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Created by igor on 3/13/18.
 */
/**
 * 中文说明：
 * 1. 类目的：`RuleChainServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class RuleChainServiceTest extends AbstractServiceTest {

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    EdgeService edgeService;
    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Autowired
    RuleChainService ruleChainService;

    private IdComparator<RuleChain> idComparator = new IdComparator<>();
    private IdComparator<RuleNode> ruleNodeIdComparator = new IdComparator<>();

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveRuleChain() throws IOException {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("My RuleChain");

        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);
        Assert.assertNotNull(savedRuleChain);
        Assert.assertNotNull(savedRuleChain.getId());
        Assert.assertTrue(savedRuleChain.getCreatedTime() > 0);
        Assert.assertEquals(ruleChain.getTenantId(), savedRuleChain.getTenantId());
        Assert.assertEquals(ruleChain.getName(), savedRuleChain.getName());

        savedRuleChain.setName("My new RuleChain");

        ruleChainService.saveRuleChain(savedRuleChain);
        RuleChain foundRuleChain = ruleChainService.findRuleChainById(tenantId, savedRuleChain.getId());
        Assert.assertEquals(foundRuleChain.getName(), savedRuleChain.getName());

        ruleChainService.deleteRuleChainById(tenantId, savedRuleChain.getId());
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveRuleChainWithEmptyName() {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            ruleChainService.saveRuleChain(ruleChain);
        });
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveRuleChainWithInvalidTenant() {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("My RuleChain");
        ruleChain.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            ruleChainService.saveRuleChain(ruleChain);
        });
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRuleChainById() {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("My RuleChain");
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);
        RuleChain foundRuleChain = ruleChainService.findRuleChainById(tenantId, savedRuleChain.getId());
        Assert.assertNotNull(foundRuleChain);
        Assert.assertEquals(savedRuleChain, foundRuleChain);
        ruleChainService.deleteRuleChainById(tenantId, savedRuleChain.getId());
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteRuleChain() {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("My RuleChain");
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);
        RuleChain foundRuleChain = ruleChainService.findRuleChainById(tenantId, savedRuleChain.getId());
        Assert.assertNotNull(foundRuleChain);
        ruleChainService.deleteRuleChainById(tenantId, savedRuleChain.getId());
        foundRuleChain = ruleChainService.findRuleChainById(tenantId, savedRuleChain.getId());
        Assert.assertNull(foundRuleChain);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRuleChainsByTenantId() {
        List<RuleChain> ruleChains = new ArrayList<>();
        for (int i = 0; i < 165; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setTenantId(tenantId);
            ruleChain.setName("RuleChain" + i);
            ruleChains.add(ruleChainService.saveRuleChain(ruleChain));
        }

        List<RuleChain> loadedRuleChains = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        PageData<RuleChain> pageData = null;
        do {
            pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
            loadedRuleChains.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(ruleChains, idComparator);
        Collections.sort(loadedRuleChains, idComparator);

        Assert.assertEquals(ruleChains, loadedRuleChains);

        ruleChainService.deleteRuleChainsByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRuleChainsByTenantIdAndName() {
        String name1 = "RuleChain name 1";
        List<RuleChain> ruleChainsName1 = new ArrayList<>();
        for (int i = 0; i < 123; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (Math.random() * 17));
            String name = name1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            ruleChain.setName(name);
            ruleChainsName1.add(ruleChainService.saveRuleChain(ruleChain));
        }
        String name2 = "RuleChain name 2";
        List<RuleChain> ruleChainsName2 = new ArrayList<>();
        for (int i = 0; i < 193; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (Math.random() * 15));
            String name = name2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            ruleChain.setName(name);
            ruleChainsName2.add(ruleChainService.saveRuleChain(ruleChain));
        }

        List<RuleChain> loadedRuleChainsName1 = new ArrayList<>();
        PageLink pageLink = new PageLink(19, 0, name1);
        PageData<RuleChain> pageData = null;
        do {
            pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
            loadedRuleChainsName1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(ruleChainsName1, idComparator);
        Collections.sort(loadedRuleChainsName1, idComparator);

        Assert.assertEquals(ruleChainsName1, loadedRuleChainsName1);

        List<RuleChain> loadedRuleChainsName2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, name2);
        do {
            pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
            loadedRuleChainsName2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(ruleChainsName2, idComparator);
        Collections.sort(loadedRuleChainsName2, idComparator);

        Assert.assertEquals(ruleChainsName2, loadedRuleChainsName2);

        for (RuleChain ruleChain : loadedRuleChainsName1) {
            ruleChainService.deleteRuleChainById(tenantId, ruleChain.getId());
        }

        pageLink = new PageLink(4, 0, name1);
        pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (RuleChain ruleChain : loadedRuleChainsName2) {
            ruleChainService.deleteRuleChainById(tenantId, ruleChain.getId());
        }

        pageLink = new PageLink(4, 0, name2);
        pageData = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveRuleChainMetaData() throws Exception {

        RuleChainMetaData savedRuleChainMetaData = createRuleChainMetadata();

        Assert.assertEquals(3, savedRuleChainMetaData.getNodes().size());
        Assert.assertEquals(3, savedRuleChainMetaData.getConnections().size());

        for (RuleNode ruleNode : savedRuleChainMetaData.getNodes()) {
            Assert.assertNotNull(ruleNode.getId());
            List<EntityRelation> relations = ruleChainService.getRuleNodeRelations(tenantId, ruleNode.getId());
            if ("name1".equals(ruleNode.getName())) {
                Assert.assertEquals(2, relations.size());
            } else if ("name2".equals(ruleNode.getName())) {
                Assert.assertEquals(1, relations.size());
            } else if ("name3".equals(ruleNode.getName())) {
                Assert.assertEquals(0, relations.size());
            }
        }

        List<RuleNode> loadedRuleNodes = ruleChainService.getRuleChainNodes(tenantId, savedRuleChainMetaData.getRuleChainId());

        Collections.sort(savedRuleChainMetaData.getNodes(), ruleNodeIdComparator);
        Collections.sort(loadedRuleNodes, ruleNodeIdComparator);

        Assert.assertEquals(savedRuleChainMetaData.getNodes(), loadedRuleNodes);

        ruleChainService.deleteRuleChainById(tenantId, savedRuleChainMetaData.getRuleChainId());
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateRuleChainMetaData() throws Exception {
        RuleChainMetaData savedRuleChainMetaData = createRuleChainMetadata();

        List<RuleNode> ruleNodes = savedRuleChainMetaData.getNodes();
        int name3Index = -1;
        for (int i=0;i<ruleNodes.size();i++) {
            if ("name3".equals(ruleNodes.get(i).getName())) {
                name3Index = i;
                break;
            }
        }

        RuleNode ruleNode4 = new RuleNode();
        ruleNode4.setName("name4");
        ruleNode4.setType("type4");
        ruleNode4.setConfiguration(JacksonUtil.toJsonNode("\"key4\": \"val4\""));

        ruleNodes.set(name3Index, ruleNode4);

        Assert.assertTrue(ruleChainService.saveRuleChainMetaData(tenantId, savedRuleChainMetaData, Function.identity()).isSuccess());
        RuleChainMetaData updatedRuleChainMetaData = ruleChainService.loadRuleChainMetaData(tenantId, savedRuleChainMetaData.getRuleChainId());

        Assert.assertEquals(3, updatedRuleChainMetaData.getNodes().size());
        Assert.assertEquals(3, updatedRuleChainMetaData.getConnections().size());

        for (RuleNode ruleNode : updatedRuleChainMetaData.getNodes()) {
            Assert.assertNotNull(ruleNode.getId());
            List<EntityRelation> relations = ruleChainService.getRuleNodeRelations(tenantId, ruleNode.getId());
            if ("name1".equals(ruleNode.getName())) {
                Assert.assertEquals(2, relations.size());
            } else if ("name2".equals(ruleNode.getName())) {
                Assert.assertEquals(1, relations.size());
            } else if ("name4".equals(ruleNode.getName())) {
                Assert.assertEquals(0, relations.size());
            }
        }

        List<RuleNode> loadedRuleNodes = ruleChainService.getRuleChainNodes(tenantId, savedRuleChainMetaData.getRuleChainId());

        Collections.sort(updatedRuleChainMetaData.getNodes(), ruleNodeIdComparator);
        Collections.sort(loadedRuleNodes, ruleNodeIdComparator);

        Assert.assertEquals(updatedRuleChainMetaData.getNodes(), loadedRuleNodes);

        ruleChainService.deleteRuleChainById(tenantId, savedRuleChainMetaData.getRuleChainId());
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateRuleChainMetaDataWithCirclingRelation() {
        Assertions.assertThrows(DataValidationException.class, () -> {
            ruleChainService.saveRuleChainMetaData(tenantId, createRuleChainMetadataWithCirclingRelation(), Function.identity());
        });
    }

    /**
     * 功能：验证规则链相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateRuleChainMetaDataWithCirclingRelation2() {
        Assertions.assertThrows(DataValidationException.class, () -> {
            ruleChainService.saveRuleChainMetaData(tenantId, createRuleChainMetadataWithCirclingRelation2(), Function.identity());
        });
    }

    /**
     * 功能：验证边缘节点相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGetDefaultEdgeRuleChains() throws Exception {
        RuleChainId ruleChainId = saveRuleChainAndSetAutoAssignToEdge("Default Edge Rule Chain 1");
        saveRuleChainAndSetAutoAssignToEdge("Default Edge Rule Chain 2");
        PageData<RuleChain> result = ruleChainService.findAutoAssignToEdgeRuleChainsByTenantId(tenantId, new PageLink(100));
        Assert.assertEquals(2, result.getData().size());

        ruleChainService.unsetAutoAssignToEdgeRuleChain(tenantId, ruleChainId);

        result = ruleChainService.findAutoAssignToEdgeRuleChainsByTenantId(tenantId, new PageLink(100));
        Assert.assertEquals(1, result.getData().size());
    }

    /**
     * 功能：更新规则链。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void setEdgeTemplateRootRuleChain() throws Exception {
        RuleChainId ruleChainId1 = saveRuleChainAndSetAutoAssignToEdge("Default Edge Rule Chain 1");
        RuleChainId ruleChainId2 = saveRuleChainAndSetAutoAssignToEdge("Default Edge Rule Chain 2");

        ruleChainService.setEdgeTemplateRootRuleChain(tenantId, ruleChainId1);
        ruleChainService.setEdgeTemplateRootRuleChain(tenantId, ruleChainId2);

        RuleChain ruleChainById = ruleChainService.findRuleChainById(tenantId, ruleChainId1);
        Assert.assertFalse(ruleChainById.isRoot());

        ruleChainById = ruleChainService.findRuleChainById(tenantId, ruleChainId2);
        Assert.assertTrue(ruleChainById.isRoot());
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    private RuleChainId saveRuleChainAndSetAutoAssignToEdge(String name) {
        RuleChain edgeRuleChain = new RuleChain();
        edgeRuleChain.setTenantId(tenantId);
        edgeRuleChain.setType(RuleChainType.EDGE);
        edgeRuleChain.setName(name);
        RuleChain savedEdgeRuleChain = ruleChainService.saveRuleChain(edgeRuleChain);
        ruleChainService.setAutoAssignToEdgeRuleChain(tenantId, savedEdgeRuleChain.getId());
        return savedEdgeRuleChain.getId();
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    private RuleChainMetaData createRuleChainMetadata() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("My RuleChain");
        ruleChain.setTenantId(tenantId);
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(savedRuleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType("type1");
        ruleNode1.setConfiguration(JacksonUtil.toJsonNode("\"key1\": \"val1\""));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType("type2");
        ruleNode2.setConfiguration(JacksonUtil.toJsonNode("\"key2\": \"val2\""));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType("type3");
        ruleNode3.setConfiguration(JacksonUtil.toJsonNode("\"key3\": \"val3\""));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0,1,"success");
        ruleChainMetaData.addConnectionInfo(0,2,"fail");
        ruleChainMetaData.addConnectionInfo(1,2,"success");

        Assert.assertTrue(ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetaData, Function.identity()).isSuccess());
        return ruleChainService.loadRuleChainMetaData(tenantId, ruleChainMetaData.getRuleChainId());
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    private RuleChainMetaData createRuleChainMetadataWithCirclingRelation() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("My RuleChain");
        ruleChain.setTenantId(tenantId);
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(savedRuleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType("type1");
        ruleNode1.setConfiguration(JacksonUtil.toJsonNode("\"key1\": \"val1\""));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType("type2");
        ruleNode2.setConfiguration(JacksonUtil.toJsonNode("\"key2\": \"val2\""));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType("type3");
        ruleNode3.setConfiguration(JacksonUtil.toJsonNode("\"key3\": \"val3\""));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0,1,"success");
        ruleChainMetaData.addConnectionInfo(0,2,"fail");
        ruleChainMetaData.addConnectionInfo(1,2,"success");
        ruleChainMetaData.addConnectionInfo(2,2,"success");

        return ruleChainMetaData;
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    private RuleChainMetaData createRuleChainMetadataWithCirclingRelation2() throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("My RuleChain");
        ruleChain.setTenantId(tenantId);
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);

        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(savedRuleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType("type1");
        ruleNode1.setConfiguration(JacksonUtil.toJsonNode("\"key1\": \"val1\""));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType("type2");
        ruleNode2.setConfiguration(JacksonUtil.toJsonNode("\"key2\": \"val2\""));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType("type3");
        ruleNode3.setConfiguration(JacksonUtil.toJsonNode("\"key3\": \"val3\""));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0,1,"success");
        ruleChainMetaData.addConnectionInfo(0,2,"fail");
        ruleChainMetaData.addConnectionInfo(1,2,"success");
        ruleChainMetaData.addConnectionInfo(2,0,"success");

        return ruleChainMetaData;
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindEdgeRuleChainsByTenantIdAndName() {
        Edge edge = constructEdge(tenantId, "My edge", "default");
        Edge savedEdge = edgeService.saveEdge(edge);

        String name1 = "Edge RuleChain name 1";
        List<RuleChain> ruleChainsName1 = new ArrayList<>();
        for (int i = 0; i < 123; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (Math.random() * 17));
            String name = name1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            ruleChain.setName(name);
            ruleChain.setType(RuleChainType.EDGE);
            ruleChainsName1.add(ruleChainService.saveRuleChain(ruleChain));
        }
        ruleChainsName1.forEach(ruleChain -> ruleChainService.assignRuleChainToEdge(tenantId, ruleChain.getId(), savedEdge.getId()));

        String name2 = "Edge RuleChain name 2";
        List<RuleChain> ruleChainsName2 = new ArrayList<>();
        for (int i = 0; i < 193; i++) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (Math.random() * 15));
            String name = name2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            ruleChain.setName(name);
            ruleChain.setType(RuleChainType.EDGE);
            ruleChainsName2.add(ruleChainService.saveRuleChain(ruleChain));
        }
        ruleChainsName2.forEach(ruleChain -> ruleChainService.assignRuleChainToEdge(tenantId, ruleChain.getId(), savedEdge.getId()));

        List<RuleChain> loadedRuleChainsName1 = new ArrayList<>();
        PageLink pageLink = new PageLink(19, 0, name1);
        PageData<RuleChain> pageData = null;
        do {
            pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
            loadedRuleChainsName1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(ruleChainsName1, idComparator);
        Collections.sort(loadedRuleChainsName1, idComparator);

        Assert.assertEquals(ruleChainsName1, loadedRuleChainsName1);

        List<RuleChain> loadedRuleChainsName2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, name2);
        do {
            pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
            loadedRuleChainsName2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(ruleChainsName2, idComparator);
        Collections.sort(loadedRuleChainsName2, idComparator);

        Assert.assertEquals(ruleChainsName2, loadedRuleChainsName2);

        for (RuleChain ruleChain : loadedRuleChainsName1) {
            ruleChainService.deleteRuleChainById(tenantId, ruleChain.getId());
        }

        pageLink = new PageLink(4, 0, name1);
        pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (RuleChain ruleChain : loadedRuleChainsName2) {
            ruleChainService.deleteRuleChainById(tenantId, ruleChain.getId());
        }

        pageLink = new PageLink(4, 0, name2);
        pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, savedEdge.getId(), pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
