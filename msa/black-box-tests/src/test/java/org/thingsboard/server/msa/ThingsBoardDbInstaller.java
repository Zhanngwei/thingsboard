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
import org.testcontainers.utility.Base58;
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 中文说明：
 * 1. 类目的：`ThingsBoardDbInstaller` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
@Slf4j
public class ThingsBoardDbInstaller {

    final static boolean IS_REDIS_CLUSTER = Boolean.parseBoolean(System.getProperty("blackBoxTests.redisCluster"));
    final static boolean IS_REDIS_SENTINEL = Boolean.parseBoolean(System.getProperty("blackBoxTests.redisSentinel"));
    final static boolean IS_HYBRID_MODE = Boolean.parseBoolean(System.getProperty("blackBoxTests.hybridMode"));
    /**
     * 数据常量，用于统一引用固定值。
     */
    private final static String POSTGRES_DATA_VOLUME = "tb-postgres-test-data-volume";

    /**
     * 数据常量，用于统一引用固定值。
     */
    private final static String CASSANDRA_DATA_VOLUME = "tb-cassandra-test-data-volume";
    private final static String REDIS_DATA_VOLUME = "tb-redis-data-volume";
    /**
     * 数据常量，用于统一引用固定值。
     */
    private final static String REDIS_CLUSTER_DATA_VOLUME = "tb-redis-cluster-data-volume";
    private final static String REDIS_SENTINEL_DATA_VOLUME = "tb-redis-sentinel-data-volume";
    /**
     * `TB_LOG_VOLUME`常量，用于统一引用固定值。
     */
    private final static String TB_LOG_VOLUME = "tb-log-test-volume";
    private final static String TB_COAP_TRANSPORT_LOG_VOLUME = "tb-coap-transport-log-test-volume";
    /**
     * 传输层常量，用于统一引用固定值。
     */
    private final static String TB_LWM2M_TRANSPORT_LOG_VOLUME = "tb-lwm2m-transport-log-test-volume";
    private final static String TB_HTTP_TRANSPORT_LOG_VOLUME = "tb-http-transport-log-test-volume";
    /**
     * 传输层常量，用于统一引用固定值。
     */
    private final static String TB_MQTT_TRANSPORT_LOG_VOLUME = "tb-mqtt-transport-log-test-volume";
    private final static String TB_SNMP_TRANSPORT_LOG_VOLUME = "tb-snmp-transport-log-test-volume";
    /**
     * 执行器常量，用于统一引用固定值。
     */
    private final static String TB_VC_EXECUTOR_LOG_VOLUME = "tb-vc-executor-log-test-volume";
    private final static String JAVA_OPTS = "-Xmx512m";

    /**
     * `dockerCompose` 字段，保存当前对象的对应属性。
     */
    private final DockerComposeExecutor dockerCompose;

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private final String postgresDataVolume;
    private final String cassandraDataVolume;

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private final String redisDataVolume;
    private final String redisClusterDataVolume;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private final String redisSentinelDataVolume;
    private final String tbLogVolume;
    /**
     * 传输层，表示当前对象的对应属性。
     */
    private final String tbCoapTransportLogVolume;
    private final String tbLwm2mTransportLogVolume;
    /**
     * 传输层，表示当前对象的对应属性。
     */
    private final String tbHttpTransportLogVolume;
    private final String tbMqttTransportLogVolume;
    /**
     * 传输层，表示当前对象的对应属性。
     */
    private final String tbSnmpTransportLogVolume;
    private final String tbVcExecutorLogVolume;
    /**
     * `env`映射关系，用于按键查找对应值。
     */
    private final Map<String, String> env;

    /**
     * 功能：创建 `ThingsBoardDbInstaller` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public ThingsBoardDbInstaller() {
        log.info("System property of blackBoxTests.redisCluster is {}", IS_REDIS_CLUSTER);
        log.info("System property of blackBoxTests.redisCluster is {}", IS_REDIS_SENTINEL);
        log.info("System property of blackBoxTests.hybridMode is {}", IS_HYBRID_MODE);
        List<File> composeFiles = new ArrayList<>(Arrays.asList(
                new File("./../../docker/docker-compose.yml"),
                new File("./../../docker/docker-compose.volumes.yml"),
                IS_HYBRID_MODE
                        ? new File("./../../docker/docker-compose.hybrid.yml")
                        : new File("./../../docker/docker-compose.postgres.yml"),
                new File("./../../docker/docker-compose.postgres.volumes.yml"),
                resolveRedisComposeFile(),
                resolveRedisComposeVolumesFile()
        ));
        if (IS_HYBRID_MODE) {
            composeFiles.add(new File("./../../docker/docker-compose.cassandra.volumes.yml"));
            composeFiles.add(new File("src/test/resources/docker-compose.hybrid-test-extras.yml"));
        } else {
            composeFiles.add(new File("src/test/resources/docker-compose.postgres-test-extras.yml"));
        }

        String identifier = Base58.randomString(6).toLowerCase();
        String project = identifier + Base58.randomString(6).toLowerCase();

        postgresDataVolume = project + "_" + POSTGRES_DATA_VOLUME;
        cassandraDataVolume = project + "_" + CASSANDRA_DATA_VOLUME;
        redisDataVolume = project + "_" + REDIS_DATA_VOLUME;
        redisClusterDataVolume = project + "_" + REDIS_CLUSTER_DATA_VOLUME;
        redisSentinelDataVolume = project + "_" + REDIS_SENTINEL_DATA_VOLUME;
        tbLogVolume = project + "_" + TB_LOG_VOLUME;
        tbCoapTransportLogVolume = project + "_" + TB_COAP_TRANSPORT_LOG_VOLUME;
        tbLwm2mTransportLogVolume = project + "_" + TB_LWM2M_TRANSPORT_LOG_VOLUME;
        tbHttpTransportLogVolume = project + "_" + TB_HTTP_TRANSPORT_LOG_VOLUME;
        tbMqttTransportLogVolume = project + "_" + TB_MQTT_TRANSPORT_LOG_VOLUME;
        tbSnmpTransportLogVolume = project + "_" + TB_SNMP_TRANSPORT_LOG_VOLUME;
        tbVcExecutorLogVolume = project + "_" + TB_VC_EXECUTOR_LOG_VOLUME;

        dockerCompose = new DockerComposeExecutor(composeFiles, project);

        env = new HashMap<>();
        env.put("JAVA_OPTS", JAVA_OPTS);
        env.put("POSTGRES_DATA_VOLUME", postgresDataVolume);
        if (IS_HYBRID_MODE) {
            env.put("CASSANDRA_DATA_VOLUME", cassandraDataVolume);
        }
        env.put("TB_LOG_VOLUME", tbLogVolume);
        env.put("TB_COAP_TRANSPORT_LOG_VOLUME", tbCoapTransportLogVolume);
        env.put("TB_LWM2M_TRANSPORT_LOG_VOLUME", tbLwm2mTransportLogVolume);
        env.put("TB_HTTP_TRANSPORT_LOG_VOLUME", tbHttpTransportLogVolume);
        env.put("TB_MQTT_TRANSPORT_LOG_VOLUME", tbMqttTransportLogVolume);
        env.put("TB_SNMP_TRANSPORT_LOG_VOLUME", tbSnmpTransportLogVolume);
        env.put("TB_VC_EXECUTOR_LOG_VOLUME", tbVcExecutorLogVolume);
        if (IS_REDIS_CLUSTER) {
            for (int i = 0; i < 6; i++) {
                env.put("REDIS_CLUSTER_DATA_VOLUME_" + i, redisClusterDataVolume + '-' + i);
            }
        } else if (IS_REDIS_SENTINEL) {
            env.put("REDIS_SENTINEL_DATA_VOLUME_MASTER", redisSentinelDataVolume + "-" + "master");
            env.put("REDIS_SENTINEL_DATA_VOLUME_SLAVE", redisSentinelDataVolume + "-" + "slave");
            env.put("REDIS_SENTINEL_DATA_VOLUME_SENTINEL", redisSentinelDataVolume + "-" + "sentinel");
        } else {
            env.put("REDIS_DATA_VOLUME", redisDataVolume);
        }
        dockerCompose.withEnv(env);
    }

    /**
     * 功能：执行 `resolveRedisComposeVolumesFile` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static File resolveRedisComposeVolumesFile() {
        if (IS_REDIS_CLUSTER) {
            return new File("./../../docker/docker-compose.redis-cluster.volumes.yml");
        }
        if (IS_REDIS_SENTINEL) {
            return new File("./../../docker/docker-compose.redis-sentinel.volumes.yml");
        }
        return new File("./../../docker/docker-compose.redis.volumes.yml");
    }

    /**
     * 功能：执行 `resolveRedisComposeFile` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static File resolveRedisComposeFile() {
        if (IS_REDIS_CLUSTER) {
            return new File("./../../docker/docker-compose.redis-cluster.yml");
        }
        if (IS_REDIS_SENTINEL) {
            return new File("./../../docker/docker-compose.redis-sentinel.yml");
        }
        return new File("./../../docker/docker-compose.redis.yml");
    }

    /**
     * 功能：获取`Env`。
     * 参数：无。
     * 返回：处理结果。
     */
    public Map<String, String> getEnv() {
        return env;
    }

    /**
     * 功能：保存或创建`Volumes`。
     * 参数：无。
     * 返回：无。
     */
    public void createVolumes() {
        try {

            dockerCompose.withCommand("volume create " + postgresDataVolume);
            dockerCompose.invokeDocker();

            if (IS_HYBRID_MODE) {
                dockerCompose.withCommand("volume create " + cassandraDataVolume);
                dockerCompose.invokeDocker();
            }

            dockerCompose.withCommand("volume create " + tbLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbCoapTransportLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbLwm2mTransportLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbHttpTransportLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbMqttTransportLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbSnmpTransportLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbVcExecutorLogVolume);
            dockerCompose.invokeDocker();

            StringBuilder additionalServices = new StringBuilder();
            if (IS_HYBRID_MODE) {
                additionalServices.append(" cassandra");
            }
            if (IS_REDIS_CLUSTER) {
                for (int i = 0; i < 6; i++) {
                    additionalServices.append(" redis-node-").append(i);
                    dockerCompose.withCommand("volume create " + redisClusterDataVolume + '-' + i);
                    dockerCompose.invokeDocker();
                }
            } else if (IS_REDIS_SENTINEL) {
                additionalServices.append(" redis-master");
                dockerCompose.withCommand("volume create " + redisSentinelDataVolume + "-" + "master");
                dockerCompose.invokeDocker();

                additionalServices.append(" redis-slave");
                dockerCompose.withCommand("volume create " + redisSentinelDataVolume + '-' + "slave");
                dockerCompose.invokeDocker();

                additionalServices.append(" redis-sentinel");
                dockerCompose.withCommand("volume create " + redisSentinelDataVolume + '-' + "sentinel");
                dockerCompose.invokeDocker();
            } else {
                additionalServices.append(" redis");
                dockerCompose.withCommand("volume create " + redisDataVolume);
                dockerCompose.invokeDocker();
            }

            dockerCompose.withCommand("up -d postgres" + additionalServices);
            dockerCompose.invokeCompose();

            dockerCompose.withCommand("run --no-deps --rm -e INSTALL_TB=true -e LOAD_DEMO=true tb-core1");
            dockerCompose.invokeCompose();

        } finally {
            try {
                dockerCompose.withCommand("down -v");
                dockerCompose.invokeCompose();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 功能：执行 `savaLogsAndRemoveVolumes` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void savaLogsAndRemoveVolumes() {
        copyLogs(tbLogVolume, "./target/tb-logs/");
        copyLogs(tbCoapTransportLogVolume, "./target/tb-coap-transport-logs/");
        copyLogs(tbLwm2mTransportLogVolume, "./target/tb-lwm2m-transport-logs/");
        copyLogs(tbHttpTransportLogVolume, "./target/tb-http-transport-logs/");
        copyLogs(tbMqttTransportLogVolume, "./target/tb-mqtt-transport-logs/");
        copyLogs(tbSnmpTransportLogVolume, "./target/tb-snmp-transport-logs/");
        copyLogs(tbVcExecutorLogVolume, "./target/tb-vc-executor-logs/");

        StringJoiner rmVolumesCommand = new StringJoiner(" ")
                .add("volume rm -f")
                .add(postgresDataVolume)
                .add(tbLogVolume)
                .add(tbCoapTransportLogVolume)
                .add(tbLwm2mTransportLogVolume)
                .add(tbHttpTransportLogVolume)
                .add(tbMqttTransportLogVolume)
                .add(tbSnmpTransportLogVolume)
                .add(tbVcExecutorLogVolume)
                .add(resolveRedisComposeVolumeLog());

        if (IS_HYBRID_MODE) {
            rmVolumesCommand.add(cassandraDataVolume);
        }

        dockerCompose.withCommand(rmVolumesCommand.toString());
    }

    /**
     * 功能：执行 `resolveRedisComposeVolumeLog` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    private String resolveRedisComposeVolumeLog() {
        if (IS_REDIS_CLUSTER) {
            return IntStream.range(0, 6).mapToObj(i -> " " + redisClusterDataVolume + "-" + i).collect(Collectors.joining());
        }
        if (IS_REDIS_SENTINEL) {
            return redisSentinelDataVolume + "-" + "master " + " " +
                    redisSentinelDataVolume + "-" + "slave" + " " +
                    redisSentinelDataVolume + " " + "sentinel";
        }
        return redisDataVolume;
    }

    /**
     * 功能：执行 `copyLogs` 对应的处理。
     * 参数：
     * - `volumeName`：名称。
     * - `targetDir`：`targetDir` 参数。
     * 返回：无。
     */
    private void copyLogs(String volumeName, String targetDir) {
        File tbLogsDir = new File(targetDir);
        tbLogsDir.mkdirs();

        String logsContainerName = "tb-logs-container-" + StringUtils.randomAlphanumeric(10);

        dockerCompose.withCommand("run -d --rm --name " + logsContainerName + " -v " + volumeName + ":/root alpine tail -f /dev/null");
        dockerCompose.invokeDocker();

        dockerCompose.withCommand("cp " + logsContainerName + ":/root/. " + tbLogsDir.getAbsolutePath());
        dockerCompose.invokeDocker();

        dockerCompose.withCommand("rm -f " + logsContainerName);
        dockerCompose.invokeDocker();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ThingsBoardDbInstaller` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
