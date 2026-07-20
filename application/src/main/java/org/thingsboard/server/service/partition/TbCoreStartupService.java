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
package org.thingsboard.server.service.partition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `TbCoreStartupService` 是 ThingsBoard Application 中负责 `Tb Core Startup` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@TbCoreComponent
@Service
@RequiredArgsConstructor
public class TbCoreStartupService {

    /**
     * 分区，提供当前类调用的业务操作。
     */
    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbClusterService clusterService;

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.STARTUP_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var myPartitions = partitionService.getMyPartitions(new QueueKey(ServiceType.TB_CORE));
        if (myPartitions != null && !myPartitions.isEmpty()) {
            TransportProtos.ToCoreNotificationMsg toCoreMsg = TransportProtos.ToCoreNotificationMsg.newBuilder()
                    .setCoreStartupMsg(TransportProtos.CoreStartupMsg.newBuilder()
                            .setServiceId(serviceInfoProvider.getServiceId()).addAllPartitions(myPartitions).build()).build();
            clusterService.broadcastToCore(toCoreMsg);
        }
    }

}
