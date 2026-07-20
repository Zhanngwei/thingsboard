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
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;

/**
 * 中文说明：
 * 1. `TimescaleTsDatabaseUpgradeService` 是 ThingsBoard Application 中负责 `Timescale Ts Database` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractSqlTsDatabaseUpgradeService`、`DatabaseTsUpgradeService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Profile("install")
@Slf4j
@TimescaleDBTsDao
public class TimescaleTsDatabaseUpgradeService extends AbstractSqlTsDatabaseUpgradeService implements DatabaseTsUpgradeService {

    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private InstallScripts installScripts;

    /**
     * 功能：执行 `upgradeDatabase` 对应的处理。
     * 参数：
     * - `fromVersion`：`fromVersion` 参数。
     * 返回：无。
     */
    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        switch (fromVersion) {
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }

    /**
     * 功能：获取SQL。
     * 参数：
     * - `conn`：`conn` 参数。
     * - `fileName`：名称。
     * - `version`：`version` 参数。
     * 返回：无。
     */
    @Override
    protected void loadSql(Connection conn, String fileName, String version) {
        Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", version, fileName);
        try {
            loadFunctions(schemaUpdateFile, conn);
            log.info("Functions successfully loaded!");
        } catch (Exception e) {
            log.info("Failed to load PostgreSQL upgrade functions due to: {}", e.getMessage());
        }
    }
}
