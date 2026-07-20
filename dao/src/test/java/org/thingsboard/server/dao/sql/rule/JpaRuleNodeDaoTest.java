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
package org.thingsboard.server.dao.sql.rule;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.rule.RuleChainDao;
import org.thingsboard.server.dao.rule.RuleNodeDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

/**
 * 中文说明：
 * 1. `JpaRuleNodeDaoTest` 是 ThingsBoard DAO 中验证 `JpaRuleNodeDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class JpaRuleNodeDaoTest extends AbstractJpaDaoTest {

    /**
     * 数量常量，用于统一引用固定值。
     */
    public static final int COUNT = 40;
    public static final String PREFIX_FOR_RULE_NODE_NAME = "SEARCH_TEXT_";
    /**
     * 规则节点列表，用于保存一组待处理对象。
     */
    List<UUID> ruleNodeIds;
    TenantId tenantId1;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    TenantId tenantId2;
    RuleChainId ruleChainId1;
    /**
     * 规则链对象，用于描述当前业务场景。
     */
    RuleChainId ruleChainId2;

    /**
     * 规则链，用于读取或保存对应领域对象。
     */
    @Autowired
    private RuleChainDao ruleChainDao;

    /**
     * 规则节点，用于读取或保存对应领域对象。
     */
    @Autowired
    private RuleNodeDao ruleNodeDao;

    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    ListeningExecutorService executor;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        tenantId1 = TenantId.fromUUID(Uuids.timeBased());
        ruleChainId1 = new RuleChainId(UUID.randomUUID());
        tenantId2 = TenantId.fromUUID(Uuids.timeBased());
        ruleChainId2 = new RuleChainId(UUID.randomUUID());

        ruleNodeIds = createRuleNodes(tenantId1, tenantId2, ruleChainId1, ruleChainId2, COUNT);
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() throws Exception {
        ruleNodeDao.removeAllByIds(ruleNodeIds);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：验证`Save Rule Name0x00 then Some Database Exception`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveRuleName0x00_thenSomeDatabaseException() {
        RuleNode ruleNode = getRuleNode(ruleChainId1, "T", "\u0000");
        assertThatThrownBy(() -> ruleNodeIds.add(ruleNodeDao.save(tenantId1, ruleNode).getUuidId()));
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRuleNodesByTenantIdAndType() {
        List<RuleNode> ruleNodes1 = ruleNodeDao.findRuleNodesByTenantIdAndType(tenantId1, "A", PREFIX_FOR_RULE_NODE_NAME);
        assertEquals(20, ruleNodes1.size());

        List<RuleNode> ruleNodes2 = ruleNodeDao.findRuleNodesByTenantIdAndType(tenantId2, "B", PREFIX_FOR_RULE_NODE_NAME);
        assertEquals(20, ruleNodes2.size());

        ruleNodes1 = ruleNodeDao.findRuleNodesByTenantIdAndType(tenantId1, "A", null);
        assertEquals(20, ruleNodes1.size());

        ruleNodes2 = ruleNodeDao.findRuleNodesByTenantIdAndType(tenantId2, "B", null);
        assertEquals(20, ruleNodes2.size());
    }

    /**
     * 功能：验证类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRuleNodesByType() {
        PageData<RuleNode> ruleNodes = ruleNodeDao.findAllRuleNodesByType( "A", new PageLink(10, 0, PREFIX_FOR_RULE_NODE_NAME));
        assertEquals(20, ruleNodes.getTotalElements());
        assertEquals(2, ruleNodes.getTotalPages());
        assertEquals(10, ruleNodes.getData().size());

        ruleNodes = ruleNodeDao.findAllRuleNodesByType( "A", new PageLink(10, 0));
        assertEquals(20, ruleNodes.getTotalElements());
        assertEquals(2, ruleNodes.getTotalPages());
        assertEquals(10, ruleNodes.getData().size());
    }

    /**
     * 功能：验证类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRuleNodesByTypeAndVersionLessThan() {
        PageData<RuleNode> ruleNodes = ruleNodeDao.findAllRuleNodesByTypeAndVersionLessThan( "A", 1, new PageLink(10, 0, PREFIX_FOR_RULE_NODE_NAME));
        assertEquals(20, ruleNodes.getTotalElements());
        assertEquals(2, ruleNodes.getTotalPages());
        assertEquals(10, ruleNodes.getData().size());

        ruleNodes = ruleNodeDao.findAllRuleNodesByTypeAndVersionLessThan( "A", 1, new PageLink(10, 0));
        assertEquals(20, ruleNodes.getTotalElements());
        assertEquals(2, ruleNodes.getTotalPages());
        assertEquals(10, ruleNodes.getData().size());
    }

    /**
     * 功能：验证规则节点相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRuleNodeIdsByTypeAndVersionLessThan() {
        PageData<RuleNodeId> ruleNodeIds = ruleNodeDao.findAllRuleNodeIdsByTypeAndVersionLessThan( "A", 1, new PageLink(10, 0, PREFIX_FOR_RULE_NODE_NAME));
        assertEquals(20, ruleNodeIds.getTotalElements());
        assertEquals(2, ruleNodeIds.getTotalPages());
        assertEquals(10, ruleNodeIds.getData().size());

        ruleNodeIds = ruleNodeDao.findAllRuleNodeIdsByTypeAndVersionLessThan( "A", 1, new PageLink(10, 0));
        assertEquals(20, ruleNodeIds.getTotalElements());
        assertEquals(2, ruleNodeIds.getTotalPages());
        assertEquals(10, ruleNodeIds.getData().size());

        // test - search text ignored
        ruleNodeIds = ruleNodeDao.findAllRuleNodeIdsByTypeAndVersionLessThan( "A", 1, new PageLink(10, 0, StringUtils.randomAlphabetic(5)));
        assertEquals(20, ruleNodeIds.getTotalElements());
        assertEquals(2, ruleNodeIds.getTotalPages());
        assertEquals(10, ruleNodeIds.getData().size());
    }

    /**
     * 功能：验证规则节点相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAllRuleNodeByIds() {
        var fromUUIDs = ruleNodeIds.stream().map(RuleNodeId::new).collect(Collectors.toList());
        var ruleNodes = ruleNodeDao.findAllRuleNodeByIds(fromUUIDs);
        assertEquals(40, ruleNodes.size());
    }

    /**
     * 功能：保存或创建`Rule Nodes`。
     * 参数：
     * - `tenantId1`：租户信息或租户标识。
     * - `tenantId2`：租户信息或租户标识。
     * - `ruleChainId1`：`ruleChainId1` 参数。
     * - `ruleChainId2`：`ruleChainId2` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private List<UUID> createRuleNodes(TenantId tenantId1, TenantId tenantId2, RuleChainId ruleChainId1, RuleChainId ruleChainId2, int count) {
        return createRuleNodes(tenantId1, tenantId2, ruleChainId1, ruleChainId2, "A", "B", count);
    }

    /**
     * 功能：保存或创建`Rule Nodes`。
     * 参数：
     * - `tenantId1`：租户信息或租户标识。
     * - `tenantId2`：租户信息或租户标识。
     * - `ruleChainId1`：`ruleChainId1` 参数。
     * - `ruleChainId2`：`ruleChainId2` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private List<UUID> createRuleNodes(TenantId tenantId1, TenantId tenantId2,
                                       RuleChainId ruleChainId1, RuleChainId ruleChainId2,
                                       String typeA, String typeB, int count) {
        var chain1 = new RuleChain(ruleChainId1);
        chain1.setTenantId(tenantId1);
        chain1.setName(ruleChainId1.toString());
        ruleChainDao.save(tenantId1, chain1);
        var chain2 = new RuleChain(ruleChainId2);
        chain2.setTenantId(tenantId2);
        chain2.setName(ruleChainId2.toString());
        ruleChainDao.save(tenantId2, chain2);
        List<UUID> savedRuleNodeIds = new ArrayList<>();
        for (int i = 0; i < count / 2; i++) {
            savedRuleNodeIds.add(ruleNodeDao.save(tenantId1, getRuleNode(ruleChainId1, typeA, Integer.toString(i))).getUuidId());
            savedRuleNodeIds.add(ruleNodeDao.save(tenantId2, getRuleNode(ruleChainId2, typeB, Integer.toString(i + count / 2))).getUuidId());
        }
        return savedRuleNodeIds;
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `type`：类型。
     * - `nameSuffix`：名称。
     * 返回：处理结果。
     */
    private RuleNode getRuleNode(RuleChainId ruleChainId, String type, String nameSuffix) {
        return getRuleNode(ruleChainId, Uuids.timeBased(), type, nameSuffix);
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `ruleNodeId`：规则节点ID。
     * - `type`：类型。
     * - `nameSuffix`：名称。
     * 返回：处理结果。
     */
    private RuleNode getRuleNode(RuleChainId ruleChainId, UUID ruleNodeId, String type, String nameSuffix) {
        RuleNode ruleNode = new RuleNode();
        ruleNode.setId(new RuleNodeId(ruleNodeId));
        ruleNode.setRuleChainId(ruleChainId);
        ruleNode.setName(nameSuffix);
        ruleNode.setType(type);
        ruleNode.setConfiguration(JacksonUtil.newObjectNode().put("searchHint", PREFIX_FOR_RULE_NODE_NAME + nameSuffix));
        ruleNode.setConfigurationVersion(0);
        return ruleNode;
    }
}
