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
package org.thingsboard.server.dao;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;

import javax.script.ScriptException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AbstractNoSqlContainer` 是 ThingsBoard DAO 测试模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 测试上下文和 DAO 测试套件按用例创建、初始化和销毁。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
 */
public abstract class AbstractNoSqlContainer {

    public static final List<String> INIT_SCRIPTS = List.of(
            "cassandra/schema-keyspace.cql",
            "cassandra/schema-ts.cql",
            "cassandra/schema-ts-latest.cql"
    );

    @ClassRule(order = 0)
    public static final CassandraContainer cassandra = (CassandraContainer) new CassandraContainer("cassandra:4.1") {
        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            super.containerIsStarted(containerInfo);
            // Cassandra 访问通常是异步或分页的，需要在这里维护查询语句、结果转换和失败处理边界。
            DatabaseDelegate db = new CassandraDatabaseDelegate(this);
            INIT_SCRIPTS.forEach(script -> runInitScriptIfRequired(db, script));
        }

        private void runInitScriptIfRequired(DatabaseDelegate db, String initScriptPath) {
            logger().info("Init script [{}]", initScriptPath);
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (initScriptPath != null) {
                try {
                    URL resource = Thread.currentThread().getContextClassLoader().getResource(initScriptPath);
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (resource == null) {
                        logger().warn("Could not load classpath init script: {}", initScriptPath);
                        throw new ScriptUtils.ScriptLoadException("Could not load classpath init script: " + initScriptPath + ". Resource not found.");
                    }
                    String cql = IOUtils.toString(resource, StandardCharsets.UTF_8);
                    ScriptUtils.executeDatabaseScript(db, initScriptPath, cql);
                // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
                } catch (IOException e) {
                    logger().warn("Could not load classpath init script: {}", initScriptPath);
                    throw new ScriptUtils.ScriptLoadException("Could not load classpath init script: " + initScriptPath, e);
                // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
                } catch (ScriptException e) {
                    logger().error("Error while executing init script: {}", initScriptPath, e);
                    throw new ScriptUtils.UncategorizedScriptException("Error while executing init script: " + initScriptPath, e);
                }
            }
        }
    }
            .withEnv("HEAP_NEWSIZE", "64M")
            .withEnv("MAX_HEAP_SIZE", "512M")
            .withEnv("CASSANDRA_CLUSTER_NAME", "ThingsBoard Cluster");

    @ClassRule(order = 1)
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            // Cassandra 访问通常是异步或分页的，需要在这里维护查询语句、结果转换和失败处理边界。
            cassandra.start();
            // Cassandra 访问通常是异步或分页的，需要在这里维护查询语句、结果转换和失败处理边界。
            String cassandraUrl = String.format("%s:%s", cassandra.getHost(), cassandra.getMappedPort(9042));
            // Cassandra 访问通常是异步或分页的，需要在这里维护查询语句、结果转换和失败处理边界。
            log.debug("Cassandra url [{}]", cassandraUrl);
            // Cassandra 访问通常是异步或分页的，需要在这里维护查询语句、结果转换和失败处理边界。
            System.setProperty("cassandra.url", cassandraUrl);
        }

        @Override
        protected void after() {
            // Cassandra 访问通常是异步或分页的，需要在这里维护查询语句、结果转换和失败处理边界。
            cassandra.stop();
            List.of("cassandra.url")
                    .forEach(System.getProperties()::remove);
        }
    };

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractNoSqlContainer` 在 ThingsBoard DAO 测试模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
