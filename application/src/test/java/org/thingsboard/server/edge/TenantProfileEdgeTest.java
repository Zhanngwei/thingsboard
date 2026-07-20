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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. `TenantProfileEdgeTest` 是 ThingsBoard Application 中验证 `TenantProfileEdge` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractEdgeTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class TenantProfileEdgeTest extends AbstractEdgeTest {

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testTenantProfiles() throws Exception {
        loginSysAdmin();

        // save current values into tmp to revert after test
        TenantProfile edgeTenantProfile = doGet("/api/tenantProfile/" + tenantProfileId.getId(), TenantProfile.class);

        // updated edge tenant profile
        edgeTenantProfile.setName("Tenant Profile Edge Test");
        edgeTenantProfile.setDescription("Updated tenant profile Edge Test");
        edgeImitator.expectMessageAmount(1);
        edgeTenantProfile = doPost("/api/tenantProfile", edgeTenantProfile, TenantProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof TenantProfileUpdateMsg);
        TenantProfileUpdateMsg tenantProfileUpdateMsg = (TenantProfileUpdateMsg) latestMessage;
        TenantProfile tenantProfileMsg = JacksonUtil.fromString(tenantProfileUpdateMsg.getEntity(), TenantProfile.class, true);
        Assert.assertNotNull(tenantProfileMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantProfileUpdateMsg.getMsgType());
        Assert.assertEquals(edgeTenantProfile, tenantProfileMsg);
        Assert.assertEquals("Updated tenant profile Edge Test", tenantProfileMsg.getDescription());
        Assert.assertEquals("Tenant Profile Edge Test", tenantProfileMsg.getName());

        loginTenantAdmin();
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testIsolatedTenantProfile() throws Exception {
        loginSysAdmin();

        TenantProfile edgeTenantProfile = doGet("/api/tenantProfile/" + tenantProfileId.getId(), TenantProfile.class);

        // set tenant profile isolated and add 2 queues - main and isolated
        edgeTenantProfile.setIsolatedTbRuleEngine(true);
        TenantProfileQueueConfiguration mainQueueConfiguration = createQueueConfig(DataConstants.MAIN_QUEUE_NAME, DataConstants.MAIN_QUEUE_TOPIC);
        TenantProfileQueueConfiguration isolatedQueueConfiguration = createQueueConfig("IsolatedHighPriority", "tb_rule_engine.isolated_hp");
        edgeTenantProfile.getProfileData().setQueueConfiguration(List.of(mainQueueConfiguration, isolatedQueueConfiguration));
        edgeImitator.expectMessageAmount(3);
        edgeTenantProfile = doPost("/api/tenantProfile", edgeTenantProfile, TenantProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<TenantProfileUpdateMsg> tenantProfileUpdateMsgOpt  = edgeImitator.findMessageByType(TenantProfileUpdateMsg.class);
        Assert.assertTrue(tenantProfileUpdateMsgOpt.isPresent());
        TenantProfileUpdateMsg tenantProfileUpdateMsg = tenantProfileUpdateMsgOpt.get();
        TenantProfile tenantProfile = JacksonUtil.fromString(tenantProfileUpdateMsg.getEntity(), TenantProfile.class, true);
        Assert.assertNotNull(tenantProfile);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantProfileUpdateMsg.getMsgType());
        Assert.assertEquals(edgeTenantProfile.getId(), tenantProfile.getId());
        Assert.assertEquals(edgeTenantProfile.getDescription(), tenantProfile.getDescription());

        List<QueueUpdateMsg> queueUpdateMsgs = edgeImitator.findAllMessagesByType(QueueUpdateMsg.class);
        Assert.assertEquals(2, queueUpdateMsgs.size());

        loginTenantAdmin();

        edgeImitator.expectMessageAmount(21);
        doPost("/api/edge/sync/" + edge.getId());
        assertThat(edgeImitator.waitForMessages()).as("await for messages after edge sync rest api call").isTrue();

        Assert.assertTrue(edgeImitator.getDownlinkMsgs().get(0) instanceof TenantUpdateMsg);
        Assert.assertTrue(edgeImitator.getDownlinkMsgs().get(1) instanceof TenantProfileUpdateMsg);

        queueUpdateMsgs = edgeImitator.findAllMessagesByType(QueueUpdateMsg.class);
        Assert.assertEquals(2, queueUpdateMsgs.size());
        for (QueueUpdateMsg queueUpdateMsg : queueUpdateMsgs) {
            Queue queue = JacksonUtil.fromString(queueUpdateMsg.getEntity(), Queue.class, true);
            Assert.assertNotNull(queue);
            Assert.assertEquals(tenantId, queue.getTenantId());
        }
    }

    /**
     * 功能：保存或创建队列。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `queueTopic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    private TenantProfileQueueConfiguration createQueueConfig(String queueName, String queueTopic) {
        TenantProfileQueueConfiguration queueConfiguration = new TenantProfileQueueConfiguration();
        queueConfiguration.setName(queueName);
        queueConfiguration.setTopic(queueTopic);
        queueConfiguration.setPollInterval(25);
        queueConfiguration.setPartitions(10);
        queueConfiguration.setConsumerPerPartition(true);
        queueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        queueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        queueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);
        return queueConfiguration;
    }
}
