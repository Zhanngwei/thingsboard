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
package org.thingsboard.server.common.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;

/**
 * Created by ashvayka on 15.10.18.
 */
/**
 * 中文说明：
 * 1. `TransportContext` 是 ThingsBoard Common Transport 中承载传输层信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
@Data
public abstract class TransportContext {

    protected final ObjectMapper mapper = new ObjectMapper();

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TransportService transportService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    /**
     * 调度器，用于安排延迟任务或周期任务。
     */
    @Autowired
    private SchedulerComponent scheduler;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Getter
    private ExecutorService executor;

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @Getter
    @Autowired
    private OtaPackageDataCache otaPackageDataCache;

    /**
     * 传输层，表示当前对象的对应属性。
     */
    @Autowired
    private TransportResourceCache transportResourceCache;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TransportRateLimitService rateLimitService;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        executor = ThingsBoardExecutors.newWorkStealingPool(50, getClass());
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：获取节点实例。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getNodeId() {
        return serviceInfoProvider.getServiceId();
    }



}
