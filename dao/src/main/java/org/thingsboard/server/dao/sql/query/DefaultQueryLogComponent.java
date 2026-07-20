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
package org.thingsboard.server.dao.sql.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `DefaultQueryLogComponent` 是 ThingsBoard DAO 中负责 `Query Log Component` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `QueryLogComponent`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Component
@Slf4j
public class DefaultQueryLogComponent implements QueryLogComponent {

    /**
     * 是否满足SQL条件。
     */
    @Value("${sql.log_queries:false}")
    private boolean logSqlQueries;
    /**
     * 阈值，用于判断是否达到处理条件。
     */
    @Value("${sql.log_queries_threshold:5000}")
    private long logQueriesThreshold;

    /**
     * 功能：执行 `logQuery` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `query`：`query` 参数。
     * - `duration`：`duration` 参数。
     * 返回：无。
     */
    @Override
    public void logQuery(QueryContext ctx, String query, long duration) {
        if (logSqlQueries && duration > logQueriesThreshold) {

            String sqlToUse = substituteParametersInSqlString(query, ctx);
            log.warn("SLOW QUERY took {} ms: {}", duration, sqlToUse);

        }
    }

    /**
     * 功能：执行 `substituteParametersInSqlString` 对应的处理。
     * 参数：
     * - `sql`：`sql` 参数。
     * - `paramSource`：`paramSource` 参数。
     * 返回：文本结果。
     */
    String substituteParametersInSqlString(String sql, SqlParameterSource paramSource) {

        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
        List<SqlParameter> declaredParams = NamedParameterUtils.buildSqlParameterList(parsedSql, paramSource);

        if (declaredParams.isEmpty()) {
            return sql;
        }

        for (SqlParameter parSQL: declaredParams) {
            String paramName = parSQL.getName();
            if (!paramSource.hasValue(paramName)) {
                continue;
            }

            Object value = paramSource.getValue(paramName);
            if (value instanceof SqlParameterValue) {
                value = ((SqlParameterValue)value).getValue();
            }

            if (!(value instanceof Iterable)) {

                String ValueForSQLQuery = getValueForSQLQuery(value);
                sql = sql.replace(":" + paramName, ValueForSQLQuery);
                continue;
            }

            //Iterable
            int count = 0;
            String valueArrayStr = "";

            for (Object valueTemp: (Iterable)value) {

                if (count > 0) {
                    valueArrayStr+=", ";
                }

                String valueForSQLQuery = getValueForSQLQuery(valueTemp);
                valueArrayStr += valueForSQLQuery;
                ++count;
            }

            sql = sql.replace(":" + paramName, valueArrayStr);

        }

        return sql;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `valueParameter`：值。
     * 返回：文本结果。
     */
    String getValueForSQLQuery(Object valueParameter) {

        if (valueParameter instanceof String) {
            return "'" + ((String) valueParameter).replaceAll("'", "''") + "'";
        }

        if (valueParameter instanceof UUID) {
            return "'" + valueParameter + "'";
        }

        return String.valueOf(valueParameter);
    }
}
