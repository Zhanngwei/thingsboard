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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.Collections;
import java.util.List;

/**
 * 中文说明：
 * 1. `DummyDiscoveryService` 是 ThingsBoard Common Queue 中负责 `Dummy Discovery` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `DiscoveryService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
@DependsOn("environmentLogService")
public class DummyDiscoveryService implements DiscoveryService {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;


    /**
     * 功能：创建 `DummyDiscoveryService` 实例，并初始化必要字段。
     * 参数：
     * - `serviceInfoProvider`：服务对象。
     * - `partitionService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public DummyDiscoveryService(TbServiceInfoProvider serviceInfoProvider, PartitionService partitionService) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionService = partitionService;
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.DISCOVERY_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        partitionService.recalculatePartitions(serviceInfoProvider.getServiceInfo(), Collections.emptyList());
    }

    /**
     * 功能：获取`Other Servers`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TransportProtos.ServiceInfo> getOtherServers() {
        return Collections.emptyList();
    }

    /**
     * 功能：判断`Monolith`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isMonolith() {
        return true;
    }
}
