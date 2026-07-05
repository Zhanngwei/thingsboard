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
package org.thingsboard.server.service.install.cql;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`CQLStatementsParser` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
public class CQLStatementsParser {

    /**
     * 中文说明：
     * 1. 类目的：`State` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    enum State {
        DEFAULT,
        INSINGLELINECOMMENT,
        INMULTILINECOMMENT,
        INQUOTESTRING,
        INSQUOTESTRING,

    }

    /**
     * `text` 字段，保存当前对象的对应属性。
     */
    private String text;
    private State state;
    /**
     * `pos` 字段，保存当前对象的对应属性。
     */
    private int pos;
    private List<String> statements;

    /**
     * 功能：创建 `CQLStatementsParser` 实例，并初始化必要字段。
     * 参数：
     * - `cql`：`cql` 参数。
     * 返回：新创建的对象实例。
     */
    public CQLStatementsParser(Path cql) throws IOException {
        try {
            List<String> lines = Files.readAllLines(cql);
            StringBuilder t = new StringBuilder();
            for (String l : lines) {
                t.append(l.trim());
                t.append('\n');
            }

            text = t.toString();
            pos = 0;
            state = State.DEFAULT;
            parseStatements();
        }
        catch (IOException e) {
            log.error("Unable to parse CQL file [{}]!", cql);
            log.error("Exception", e);
            throw e;
        }
    }

    /**
     * 功能：获取`Statements`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<String> getStatements() {
        return this.statements;
    }

    /**
     * 功能：解析`Statements`。
     * 参数：无。
     * 返回：无。
     */
    private void parseStatements() {
        this.statements = new ArrayList<>();
        StringBuilder statementUnderConstruction = new StringBuilder();

        char c;
        while ((c = getChar()) != 0) {
            switch (state) {
                case DEFAULT:
                    processDefaultState(c, statementUnderConstruction);
                    break;
                case INSINGLELINECOMMENT:
                    if (c == '\n') {
                        state = State.DEFAULT;
                    }
                    break;

                case INMULTILINECOMMENT:
                    if (c == '*' && peekAhead() == '/') {
                        state = State.DEFAULT;
                        advance();
                    }
                    break;

                case INQUOTESTRING:
                    processInQuoteStringState(c, statementUnderConstruction);
                    break;
                case INSQUOTESTRING:
                    processInSQuoteStringState(c, statementUnderConstruction);
                    break;
            }

        }
        String tmp = statementUnderConstruction.toString().trim();
        if (tmp.length() > 0) {
            this.statements.add(tmp);
        }
    }

    /**
     * 功能：处理状态。
     * 参数：
     * - `c`：`c` 参数。
     * - `statementUnderConstruction`：`statementUnderConstruction` 参数。
     * 返回：无。
     */
    private void processDefaultState(char c, StringBuilder statementUnderConstruction) {
        if ((c == '/' && peekAhead() == '/') || (c == '-' && peekAhead() == '-')) {
            state = State.INSINGLELINECOMMENT;
            advance();
        } else if (c == '/' && peekAhead() == '*') {
            state = State.INMULTILINECOMMENT;
            advance();
        } else if (c == '\n') {
            statementUnderConstruction.append(' ');
        } else {
            statementUnderConstruction.append(c);
            if (c == '\"') {
                state = State.INQUOTESTRING;
            } else if (c == '\'') {
                state = State.INSQUOTESTRING;
            } else if (c == ';') {
                statements.add(statementUnderConstruction.toString().trim());
                statementUnderConstruction.setLength(0);
            }
        }
    }

    /**
     * 功能：处理状态。
     * 参数：
     * - `c`：`c` 参数。
     * - `statementUnderConstruction`：`statementUnderConstruction` 参数。
     * 返回：无。
     */
    private void processInQuoteStringState(char c, StringBuilder statementUnderConstruction) {
        statementUnderConstruction.append(c);
        if (c == '"') {
            if (peekAhead() == '"') {
                statementUnderConstruction.append(getChar());
            } else {
                state = State.DEFAULT;
            }
        }
    }

    /**
     * 功能：处理状态。
     * 参数：
     * - `c`：`c` 参数。
     * - `statementUnderConstruction`：`statementUnderConstruction` 参数。
     * 返回：无。
     */
    private void processInSQuoteStringState(char c, StringBuilder statementUnderConstruction) {
        statementUnderConstruction.append(c);
        if (c == '\'') {
            if (peekAhead() == '\'') {
                statementUnderConstruction.append(getChar());
            } else {
                state = State.DEFAULT;
            }
        }
    }

    /**
     * 功能：获取`Char`。
     * 参数：无。
     * 返回：处理结果。
     */
    private char getChar() {
        if (pos < text.length())
            return text.charAt(pos++);
        else
            return 0;
    }

    /**
     * 功能：执行 `peekAhead` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private char peekAhead() {
        if (pos < text.length())
            return text.charAt(pos);  // don't advance
        else
            return 0;
    }

    /**
     * 功能：执行 `advance` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void advance() {
        pos++;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CQLStatementsParser` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
