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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. `AbstractControllerTest` 是 ThingsBoard Application 中验证 `AbstractController` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractNotifyEntityTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AbstractControllerTest.class, loader = SpringBootContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan({"org.thingsboard.server"})
@EnableWebSocket
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public abstract class AbstractControllerTest extends AbstractNotifyEntityTest {

    /**
     * URL 地址常量，用于统一引用固定值。
     */
    public static final String WS_URL = "ws://localhost:";

    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @LocalServerPort
    protected int wsPort;

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    protected volatile TbTestWebSocketClient wsClient; // lazy
    protected volatile TbTestWebSocketClient anotherWsClient; // lazy

    /**
     * 功能：获取客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    public TbTestWebSocketClient getWsClient() {
        if (wsClient == null) {
            synchronized (this) {
                try {
                    if (wsClient == null) {
                        wsClient = buildAndConnectWebSocketClient();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return wsClient;
    }

    /**
     * 功能：获取客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    public TbTestWebSocketClient getAnotherWsClient() {
        if (anotherWsClient == null) {
            synchronized (this) {
                try {
                    if (anotherWsClient == null) {
                        anotherWsClient = buildAndConnectWebSocketClient();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return anotherWsClient;
    }

    /**
     * 功能：执行 `beforeWsTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeWsTest() throws Exception {
        // placeholder
    }

    /**
     * 功能：执行 `afterWsTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterWsTest() throws Exception {
        if (wsClient != null) {
            wsClient.close();
        }
        if (anotherWsClient != null) {
            anotherWsClient.close();
        }
    }

    /**
     * 功能：构建客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    protected TbTestWebSocketClient buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        return buildAndConnectWebSocketClient("/api/ws");
    }

    /**
     * 功能：构建客户端。
     * 参数：
     * - `path`：文件或资源路径。
     * 返回：处理结果。
     */
    protected TbTestWebSocketClient buildAndConnectWebSocketClient(String path) throws URISyntaxException, InterruptedException {
        TbTestWebSocketClient wsClient = new TbTestWebSocketClient(new URI(WS_URL + wsPort + path));
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        if (!path.contains("token=")) {
            wsClient.authenticate(token);
        }
        return wsClient;
    }

}
