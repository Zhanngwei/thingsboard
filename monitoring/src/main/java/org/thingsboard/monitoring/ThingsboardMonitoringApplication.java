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
package org.thingsboard.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.monitoring.service.BaseMonitoringService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `ThingsboardMonitoringApplication` 是 ThingsBoard Monitoring 的进程启动入口，用于启动和装配 `Thingsboard Monitoring Application` 相关服务。
 * 2. 它负责创建应用上下文、加载组件并把启动参数交给实际运行模块。
 * 3. 类中通常只保留启动参数修正和框架启动调用，不承载协议或业务处理细节。
 * 4. 它直接协作于 Spring Boot 配置、组件扫描和当前模块的服务实现。
 * 5. 独立入口使该服务能够单独部署、配置和扩缩容。
 * 6. 阅读时重点关注组件扫描范围、配置文件名称和传入启动框架的参数。
 */
@SpringBootApplication
@EnableScheduling
@Slf4j
public class ThingsboardMonitoringApplication {

    /**
     * `monitoringServices`列表，用于保存一组待处理对象。
     */
    @Autowired
    private List<BaseMonitoringService<?, ?>> monitoringServices;

    /**
     * 监控执行间隔，用于控制时间范围或等待时长。
     */
    @Value("${monitoring.monitoring_rate_ms}")
    private int monitoringRateMs;

    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) {
        new SpringApplicationBuilder(ThingsboardMonitoringApplication.class)
                .properties(Map.of("spring.config.name", "tb-monitoring"))
                .run(args);
    }

    /**
     * 功能：初始化或启动`Monitoring`。
     * 参数：无。
     * 返回：无。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startMonitoring() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("monitoring-executor"));
        scheduler.scheduleWithFixedDelay(() -> {
            monitoringServices.forEach(monitoringService -> {
                monitoringService.runChecks();
            });
        }, 0, monitoringRateMs, TimeUnit.MILLISECONDS);
    }

}
