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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.fail;

/**
 * 中文说明：
 * 1. 类目的：`ContainerTestSuite` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
@Slf4j
public class ContainerTestSuite {
    final static boolean IS_REDIS_CLUSTER = Boolean.parseBoolean(System.getProperty("blackBoxTests.redisCluster"));
    final static boolean IS_REDIS_SENTINEL = Boolean.parseBoolean(System.getProperty("blackBoxTests.redisSentinel"));
    final static boolean IS_HYBRID_MODE = Boolean.parseBoolean(System.getProperty("blackBoxTests.hybridMode"));
    final static String QUEUE_TYPE = System.getProperty("blackBoxTests.queue", "kafka");
    /**
     * `SOURCE_DIR`常量，用于统一引用固定值。
     */
    private static final String SOURCE_DIR = "./../../docker/";
    private static final String TB_CORE_LOG_REGEXP = ".*Starting polling for events.*";
    /**
     * `TRANSPORTS_LOG_REGEXP`常量，用于统一引用固定值。
     */
    private static final String TRANSPORTS_LOG_REGEXP = ".*Going to recalculate partitions.*";
    private static final String TB_VC_LOG_REGEXP = TRANSPORTS_LOG_REGEXP;
    /**
     * 执行器常量，用于统一引用固定值。
     */
    private static final String TB_JS_EXECUTOR_LOG_REGEXP = ".*template started.*";
    private static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofSeconds(400);

    /**
     * `testContainer` 字段，保存当前对象的对应属性。
     */
    private  DockerComposeContainer<?> testContainer;
    private  ThingsBoardDbInstaller installTb;
    /**
     * 当前对象是否处于激活状态。
     */
    private boolean isActive;

    /**
     * `containerTestSuite` 字段，保存当前对象的对应属性。
     */
    private static ContainerTestSuite containerTestSuite;

    /**
     * 功能：判断`Active`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * 功能：更新`Active`。
     * 参数：
     * - `active`：`active` 参数。
     * 返回：无。
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * 功能：创建 `ContainerTestSuite` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private ContainerTestSuite() {
    }

    /**
     * 功能：获取`Instance`。
     * 参数：无。
     * 返回：处理结果。
     */
    public static ContainerTestSuite getInstance() {
        if (containerTestSuite == null) {
            containerTestSuite = new ContainerTestSuite();
        }
        return containerTestSuite;
    }

    /**
     * 功能：执行 `start` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void start() {
        installTb = new ThingsBoardDbInstaller();
        installTb.createVolumes();
        log.info("System property of blackBoxTests.redisCluster is {}", IS_REDIS_CLUSTER);
        log.info("System property of blackBoxTests.redisSentinel is {}", IS_REDIS_SENTINEL);
        log.info("System property of blackBoxTests.hybridMode is {}", IS_HYBRID_MODE);
        boolean skipTailChildContainers = Boolean.parseBoolean(System.getProperty("blackBoxTests.skipTailChildContainers"));
        try {
            final String targetDir = FileUtils.getTempDirectoryPath() + "/" + "ContainerTestSuite-" + UUID.randomUUID() + "/";
            log.info("targetDir {}", targetDir);
            FileUtils.copyDirectory(new File(SOURCE_DIR), new File(targetDir));
            replaceInFile(targetDir + "docker-compose.yml", "    container_name: \"${LOAD_BALANCER_NAME}\"", "", "container_name");

            FileUtils.copyDirectory(new File("src/test/resources"), new File(targetDir));

            /**
             * 中文说明：
             * 1. 类目的：`DockerComposeContainerImpl` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
             * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
             * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
             * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
             * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
             * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
             * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
             * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
             */
            class DockerComposeContainerImpl<SELF extends DockerComposeContainer<SELF>> extends DockerComposeContainer<SELF> {
                /**
                 * 功能：创建 `ContainerTestSuite` 实例，并初始化必要字段。
                 * 参数：
                 * - `composeFiles`：数据列表。
                 * 返回：新创建的对象实例。
                 */
                public DockerComposeContainerImpl(List<File> composeFiles) {
                    super(composeFiles);
                }

                /**
                 * 功能：执行 `stop` 对应的处理。
                 * 参数：无。
                 * 返回：无。
                 */
                @Override
                public void stop() {
                    super.stop();
                    tryDeleteDir(targetDir);
                }
            }

            List<File> composeFiles = new ArrayList<>(Arrays.asList(
                    new File(targetDir + "docker-compose.yml"),
                    new File(targetDir + "docker-compose.volumes.yml"),
                    new File(targetDir + "docker-compose.mosquitto.yml"),
                    new File(targetDir + (IS_HYBRID_MODE ? "docker-compose.hybrid.yml" : "docker-compose.postgres.yml")),
                    new File(targetDir + (IS_HYBRID_MODE ? "docker-compose.hybrid-test-extras.yml" : "docker-compose.postgres-test-extras.yml")),
                    new File(targetDir + "docker-compose.postgres.volumes.yml"),
                    new File(targetDir + "docker-compose." + QUEUE_TYPE + ".yml"),
                    new File(targetDir + resolveRedisComposeFile()),
                    new File(targetDir + resolveRedisComposeVolumesFile()),
                    new File(targetDir + ("docker-selenium.yml"))
            ));

            Map<String, String> queueEnv = new HashMap<>();
            queueEnv.put("TB_QUEUE_TYPE", QUEUE_TYPE);
            switch (QUEUE_TYPE) {
                case "kafka":
                    composeFiles.add(new File(targetDir + "docker-compose.kafka.yml"));
                    break;
                case "aws-sqs":
                    replaceInFile(targetDir, "queue-aws-sqs.env",
                            Map.of("YOUR_KEY", getSysProp("blackBoxTests.awsKey"),
                                    "YOUR_SECRET", getSysProp("blackBoxTests.awsSecret"),
                                    "YOUR_REGION", getSysProp("blackBoxTests.awsRegion")));
                    break;
                case "rabbitmq":
                    composeFiles.add(new File(targetDir + "docker-compose.rabbitmq-server.yml"));
                    replaceInFile(targetDir, "queue-rabbitmq.env",
                            Map.of("localhost", "rabbitmq"));
                    break;
                case "service-bus":
                    replaceInFile(targetDir, "queue-service-bus.env",
                            Map.of("YOUR_NAMESPACE_NAME", getSysProp("blackBoxTests.serviceBusNamespace"),
                                    "YOUR_SAS_KEY_NAME", getSysProp("blackBoxTests.serviceBusSASPolicy")));
                    replaceInFile(targetDir, "queue-service-bus.env",
                            Map.of("YOUR_SAS_KEY", getSysProp("blackBoxTests.serviceBusPrimaryKey")));
                    break;
                case "pubsub":
                    replaceInFile(targetDir, "queue-pubsub.env",
                            Map.of("YOUR_PROJECT_ID", getSysProp("blackBoxTests.pubSubProjectId"),
                                    "YOUR_SERVICE_ACCOUNT", getSysProp("blackBoxTests.pubSubServiceAccount")));
                    break;
                default:
                    throw new RuntimeException("Unsupported queue type: " + QUEUE_TYPE);
            }

            if (IS_HYBRID_MODE) {
                composeFiles.add(new File(targetDir + "docker-compose.cassandra.volumes.yml"));
            }

            testContainer = new DockerComposeContainerImpl<>(composeFiles)
                    .withPull(false)
                    .withLocalCompose(true)
                    .withOptions("--compatibility")
                    .withTailChildContainers(!skipTailChildContainers)
                    .withEnv(installTb.getEnv())
                    .withEnv(queueEnv)
                    .withEnv("LOAD_BALANCER_NAME", "")
                    .withExposedService("haproxy", 80, Wait.forHttp("/swagger-ui.html").withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .withExposedService("broker", 1883)
                    .waitingFor("tb-core1", Wait.forLogMessage(TB_CORE_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-core2", Wait.forLogMessage(TB_CORE_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-http-transport1", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-http-transport2", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-mqtt-transport1", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-mqtt-transport2", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-vc-executor1", Wait.forLogMessage(TB_VC_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-vc-executor2", Wait.forLogMessage(TB_VC_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-js-executor", Wait.forLogMessage(TB_JS_EXECUTOR_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
            testContainer.start();
            setActive(true);
        } catch (Exception e) {
            log.error("Failed to create test container", e);
            fail("Failed to create test container");
        }
    }

    /**
     * 功能：执行 `resolveRedisComposeFile` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    private static String resolveRedisComposeFile() {
        if (IS_REDIS_CLUSTER) {
            return "docker-compose.redis-cluster.yml";
        }
        if (IS_REDIS_SENTINEL) {
            return "docker-compose.redis-sentinel.yml";
        }
        return "docker-compose.redis.yml";
    }

    /**
     * 功能：执行 `resolveRedisComposeVolumesFile` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    private static String resolveRedisComposeVolumesFile() {
        if (IS_REDIS_CLUSTER) {
            return "docker-compose.redis-cluster.volumes.yml";
        }
        if (IS_REDIS_SENTINEL) {
            return "docker-compose.redis-sentinel.volumes.yml";
        }
        return "docker-compose.redis.volumes.yml";
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void stop() {
        if (isActive) {
            testContainer.stop();
            installTb.savaLogsAndRemoveVolumes();
            setActive(false);
        }
    }

    /**
     * 功能：执行 `replaceInFile` 对应的处理。
     * 参数：
     * - `targetDir`：`targetDir` 参数。
     * - `fileName`：名称。
     * - `replacements`：键值映射。
     * 返回：无。
     */
    private static void replaceInFile(String targetDir, String fileName, Map<String, String> replacements) throws IOException {
        Path envFilePath = Path.of(targetDir, fileName);
        String data = Files.readString(envFilePath);
        for (var entry : replacements.entrySet()) {
            data = data.replace(entry.getKey(), entry.getValue());
        }
        Files.write(envFilePath, data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 功能：获取`Sys Prop`。
     * 参数：
     * - `propertyName`：名称。
     * 返回：文本结果。
     */
    private static String getSysProp(String propertyName) {
        var value = System.getProperty(propertyName);
        if (StringUtils.isEmpty(value)) {
            throw new RuntimeException("Please define system property: " + propertyName + "!");
        }
        return value;
    }

    /**
     * 功能：执行 `tryDeleteDir` 对应的处理。
     * 参数：
     * - `targetDir`：`targetDir` 参数。
     * 返回：无。
     */
    private static void tryDeleteDir(String targetDir) {
        try {
            log.info("Trying to delete temp dir {}", targetDir);
            FileUtils.deleteDirectory(new File(targetDir));
        } catch (IOException e) {
            log.error("Can't delete temp directory " + targetDir, e);
        }
    }

    /**
     * This workaround is actual until issue will be resolved:
     * Support container_name in docker-compose file #2472 https://github.com/testcontainers/testcontainers-java/issues/2472
     * docker-compose files which contain container_name are not supported and the creation of DockerComposeContainer fails due to IllegalStateException.
     * This has been introduced in #1151 as a quick fix for unintuitive feedback. https://github.com/testcontainers/testcontainers-java/issues/1151
     * Using the latest testcontainers and waiting for the fix...
     */
    /**
     * 功能：执行 `replaceInFile` 对应的处理。
     * 参数：
     * - `sourceFilename`：名称。
     * - `target`：`target` 参数。
     * - `replacement`：`replacement` 参数。
     * - `verifyPhrase`：`verifyPhrase` 参数。
     * 返回：无。
     */
    private static void replaceInFile(String sourceFilename, String target, String replacement, String verifyPhrase) {
        try {
            File file = new File(sourceFilename);
            String sourceContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

            String outputContent = sourceContent.replace(target, replacement);
            assertThat(outputContent, (not(containsString(target))));
            assertThat(outputContent, (not(containsString(verifyPhrase))));

            FileUtils.writeStringToFile(file, outputContent, StandardCharsets.UTF_8);
            assertThat(FileUtils.readFileToString(file, StandardCharsets.UTF_8), is(outputContent));
        } catch (IOException e) {
            log.error("failed to update file " + sourceFilename, e);
            fail("failed to update file");
        }
    }

    /**
     * 功能：获取`Test Container`。
     * 参数：无。
     * 返回：处理结果。
     */
    public DockerComposeContainer<?> getTestContainer() {
        return testContainer;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ContainerTestSuite` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
