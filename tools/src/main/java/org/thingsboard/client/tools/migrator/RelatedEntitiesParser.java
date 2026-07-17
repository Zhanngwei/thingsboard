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
package org.thingsboard.client.tools.migrator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. `RelatedEntitiesParser` 是 ThingsBoard Tools 中转换 `Related Entities` 数据结构的适配组件。
 * 2. 它把输入对象、协议内容或持久化数据转换为目标模型。
 * 3. 转换过程负责字段映射、格式解析以及必要的默认值处理。
 * 4. 它直接协作于源模型、目标模型和相关编解码类型。
 * 5. 独立转换器可以避免不同模块重复编写并逐渐分叉的映射逻辑。
 * 6. 阅读时重点关注字段对应关系、空值处理和不兼容输入的处理方式。
 */
public class RelatedEntitiesParser {
    private final Map<String, String> allEntityIdsAndTypes = new HashMap<>();
    
    private final Map<String, EntityType> tableNameAndEntityType = Map.ofEntries(
            Map.entry("COPY public.alarm ", EntityType.ALARM),
            Map.entry("COPY public.asset ", EntityType.ASSET),
            Map.entry("COPY public.customer ", EntityType.CUSTOMER),
            Map.entry("COPY public.dashboard ", EntityType.DASHBOARD),
            Map.entry("COPY public.device ", EntityType.DEVICE),
            Map.entry("COPY public.rule_chain ", EntityType.RULE_CHAIN),
            Map.entry("COPY public.rule_node ", EntityType.RULE_NODE),
            Map.entry("COPY public.tenant ", EntityType.TENANT),
            Map.entry("COPY public.tb_user ", EntityType.USER),
            Map.entry("COPY public.entity_view ", EntityType.ENTITY_VIEW),
            Map.entry("COPY public.widgets_bundle ", EntityType.WIDGETS_BUNDLE),
            Map.entry("COPY public.widget_type ", EntityType.WIDGET_TYPE),
            Map.entry("COPY public.tenant_profile ", EntityType.TENANT_PROFILE),
            Map.entry("COPY public.device_profile ", EntityType.DEVICE_PROFILE),
            Map.entry("COPY public.asset_profile ", EntityType.ASSET_PROFILE),
            Map.entry("COPY public.api_usage_state ", EntityType.API_USAGE_STATE)
    );

    /**
     * 功能：创建 `RelatedEntitiesParser` 实例，并初始化必要字段。
     * 参数：
     * - `source`：`source` 参数。
     * 返回：新创建的对象实例。
     */
    public RelatedEntitiesParser(File source) throws IOException {
        processAllTables(FileUtils.lineIterator(source));
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `uuid`：`uuid`ID。
     * 返回：文本结果。
     */
    public String getEntityType(String uuid) {
        return this.allEntityIdsAndTypes.get(uuid);
    }

    /**
     * 功能：判断`Block Finished`。
     * 参数：
     * - `line`：`line` 参数。
     * 返回：判断结果。
     */
    private boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

    /**
     * 功能：处理`All Tables`。
     * 参数：
     * - `lineIterator`：`lineIterator` 参数。
     * 返回：无。
     */
    private void processAllTables(LineIterator lineIterator) throws IOException {
        String currentLine;
        try {
            while (lineIterator.hasNext()) {
                currentLine = lineIterator.nextLine();
                for(Map.Entry<String, EntityType> entry : tableNameAndEntityType.entrySet()) {
                    if(currentLine.startsWith(entry.getKey())) {
                        processBlock(lineIterator, entry.getValue());
                    }
                }
            }
        } finally {
            lineIterator.close();
        }
    }

    /**
     * 功能：处理`Block`。
     * 参数：
     * - `lineIterator`：`lineIterator` 参数。
     * - `entityType`：实体对象。
     * 返回：无。
     */
    private void processBlock(LineIterator lineIterator, EntityType entityType) {
        String currentLine;
        while(lineIterator.hasNext()) {
            currentLine = lineIterator.nextLine();
            if(isBlockFinished(currentLine)) {
                return;
            }
            allEntityIdsAndTypes.put(currentLine.split("\t")[0], entityType.name());
        }
    }
}
