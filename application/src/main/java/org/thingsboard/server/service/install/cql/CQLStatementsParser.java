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
 * 1. `CQLStatementsParser` 是 ThingsBoard Application 中转换 `CQL Statements` 数据结构的适配组件。
 * 2. 它把输入对象、协议内容或持久化数据转换为目标模型。
 * 3. 转换过程负责字段映射、格式解析以及必要的默认值处理。
 * 4. 它直接协作于源模型、目标模型和相关编解码类型。
 * 5. 独立转换器可以避免不同模块重复编写并逐渐分叉的映射逻辑。
 * 6. 阅读时重点关注字段对应关系、空值处理和不兼容输入的处理方式。
 */
@Slf4j
public class CQLStatementsParser {

    /**
     * 中文说明：
     * 1. `State` 是 ThingsBoard Application 中定义状态固定取值的枚举类型。
     * 2. 它列出当前流程允许使用的有限状态、模式或类别。
     * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
     * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
     * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
     * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
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
