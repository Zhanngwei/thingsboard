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
package org.thingsboard.server.transport.mqtt;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.ResourceLeakDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `MqttTransportService` 是 ThingsBoard Common Transport 中负责 MQTT 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TbTransportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service("MqttTransportService")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.mqtt.enabled}'=='true')")
@Slf4j
public class MqttTransportService implements TbTransportService {

    public static AttributeKey<InetSocketAddress> ADDRESS = AttributeKey.newInstance("SRC_ADDRESS");

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Value("${transport.mqtt.bind_address}")
    private String host;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Value("${transport.mqtt.bind_port}")
    private Integer port;

    /**
     * 是否启用 SSL。
     */
    @Value("${transport.mqtt.ssl.enabled}")
    private boolean sslEnabled;

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Value("${transport.mqtt.ssl.bind_address}")
    private String sslHost;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Value("${transport.mqtt.ssl.bind_port}")
    private Integer sslPort;

    /**
     * `leakDetectorLevel` 字段，保存当前对象的对应属性。
     */
    @Value("${transport.mqtt.netty.leak_detector_level}")
    private String leakDetectorLevel;
    /**
     * 数量，用于控制数量、位置或分页范围。
     */
    @Value("${transport.mqtt.netty.boss_group_thread_count}")
    private Integer bossGroupThreadCount;
    /**
     * 工作线程组，用于控制数量、位置或分页范围。
     */
    @Value("${transport.mqtt.netty.worker_group_thread_count}")
    private Integer workerGroupThreadCount;
    /**
     * 是否满足`keepAlive`条件。
     */
    @Value("${transport.mqtt.netty.so_keep_alive}")
    private boolean keepAlive;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    private MqttTransportContext context;

    /**
     * 服务端 Channel，表示当前网络连接使用的通道。
     */
    private Channel serverChannel;
    private Channel sslServerChannel;
    /**
     * `bossGroup` 字段，保存当前对象的对应属性。
     */
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() throws Exception {
        log.info("Setting resource leak detector level to {}", leakDetectorLevel);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.valueOf(leakDetectorLevel.toUpperCase()));

        log.info("Starting MQTT transport...");
        bossGroup = new NioEventLoopGroup(bossGroupThreadCount);
        workerGroup = new NioEventLoopGroup(workerGroupThreadCount);
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new MqttTransportServerInitializer(context, false))
                .childOption(ChannelOption.SO_KEEPALIVE, keepAlive);

        serverChannel = b.bind(host, port).sync().channel();
        if (sslEnabled) {
            b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new MqttTransportServerInitializer(context, true))
                    .childOption(ChannelOption.SO_KEEPALIVE, keepAlive);
            sslServerChannel = b.bind(sslHost, sslPort).sync().channel();
        }
        log.info("Mqtt transport started!");
    }

    /**
     * 功能：执行 `shutdown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void shutdown() throws InterruptedException {
        log.info("Stopping MQTT transport!");
        try {
            serverChannel.close().sync();
            if (sslEnabled) {
                sslServerChannel.close().sync();
            }
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
        log.info("MQTT transport stopped!");
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getName() {
        return DataConstants.MQTT_TRANSPORT_NAME;
    }
}
