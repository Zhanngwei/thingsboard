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
package org.thingsboard.server.queue.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_ADDED;
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `ZkDiscoveryServiceTest` 是 ThingsBoard Common Queue 中验证 `ZkDiscoveryService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@RunWith(MockitoJUnitRunner.class)
public class ZkDiscoveryServiceTest {

    /**
     * 事件，表示当前对象的对应属性。
     */
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    private TbServiceInfoProvider serviceInfoProvider;

    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Mock
    private PartitionService partitionService;

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @Mock
    private CuratorFramework client;

    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    @Mock
    private PathChildrenCache cache;

    /**
     * `curatorFramework` 字段，保存当前对象的对应属性。
     */
    @Mock
    private CuratorFramework curatorFramework;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private ZkDiscoveryService zkDiscoveryService;

    /**
     * 延迟时间常量，用于统一引用固定值。
     */
    private static final long RECALCULATE_DELAY = 100L;

    final TransportProtos.ServiceInfo currentInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("tb-rule-engine-0").build();
    final ChildData currentData = new ChildData("/thingsboard/nodes/0000000010", null, currentInfo.toByteArray());
    final TransportProtos.ServiceInfo childInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("tb-rule-engine-1").build();
    final ChildData childData = new ChildData("/thingsboard/nodes/0000000020", null, childInfo.toByteArray());

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setup() {
        zkDiscoveryService = Mockito.spy(new ZkDiscoveryService(applicationEventPublisher, serviceInfoProvider, partitionService));
        ScheduledExecutorService zkExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("zk-discovery"));
        when(client.getState()).thenReturn(CuratorFrameworkState.STARTED);
        ReflectionTestUtils.setField(zkDiscoveryService, "stopped", false);
        ReflectionTestUtils.setField(zkDiscoveryService, "client", client);
        ReflectionTestUtils.setField(zkDiscoveryService, "cache", cache);
        ReflectionTestUtils.setField(zkDiscoveryService, "nodePath", "/thingsboard/nodes/0000000010");
        ReflectionTestUtils.setField(zkDiscoveryService, "zkExecutorService", zkExecutorService);
        ReflectionTestUtils.setField(zkDiscoveryService, "recalculateDelay", RECALCULATE_DELAY);
        ReflectionTestUtils.setField(zkDiscoveryService, "zkDir", "/thingsboard");

        when(serviceInfoProvider.getServiceInfo()).thenReturn(currentInfo);

        List<ChildData> dataList = new ArrayList<>();
        dataList.add(currentData);
        when(cache.getCurrentData()).thenReturn(dataList);
    }

    /**
     * 功能：执行 `restartNodeInTimeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void restartNodeInTimeTest() throws Exception {
        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        verify(partitionService, never()).recalculatePartitions(any(), any());

        startNode(childData);

        verify(partitionService, never()).recalculatePartitions(any(), any());

        Thread.sleep(RECALCULATE_DELAY * 2);

        verify(partitionService, never()).recalculatePartitions(any(), any());

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());
    }

    /**
     * 功能：执行 `restartNodeNotInTimeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void restartNodeNotInTimeTest() throws Exception {
        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        Thread.sleep(RECALCULATE_DELAY * 2);

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(Collections.emptyList()));

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);
    }

    /**
     * 功能：初始化或启动节点实例。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void startAnotherNodeDuringRestartTest() throws Exception {
        var anotherInfo = TransportProtos.ServiceInfo.newBuilder().setServiceId("tb-transport").build();
        var anotherData = new ChildData("/thingsboard/nodes/0000000030", null, anotherInfo.toByteArray());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(childInfo)));

        reset(partitionService);

        stopNode(childData);

        assertEquals(1, zkDiscoveryService.delayedTasks.size());

        startNode(anotherData);

        assertTrue(zkDiscoveryService.delayedTasks.isEmpty());

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(anotherInfo)));
        reset(partitionService);

        Thread.sleep(RECALCULATE_DELAY * 2);

        verify(partitionService, never()).recalculatePartitions(any(), any());

        startNode(childData);

        verify(partitionService, times(1)).recalculatePartitions(eq(currentInfo), eq(List.of(anotherInfo, childInfo)));
    }

    /**
     * 功能：初始化或启动节点实例。
     * 参数：
     * - `data`：待处理数据。
     * 返回：无。
     */
    private void startNode(ChildData data) throws Exception {
        cache.getCurrentData().add(data);
        zkDiscoveryService.childEvent(curatorFramework, new PathChildrenCacheEvent(CHILD_ADDED, data));
    }

    /**
     * 功能：停止或关闭节点实例。
     * 参数：
     * - `data`：待处理数据。
     * 返回：无。
     */
    private void stopNode(ChildData data) throws Exception {
        cache.getCurrentData().remove(data);
        zkDiscoveryService.childEvent(curatorFramework, new PathChildrenCacheEvent(CHILD_REMOVED, data));
    }

}
