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
package org.thingsboard.server.service.install.update;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

/**
 * 中文说明：
 * 1. `PaginatedUpdater` 是 ThingsBoard Application 中处理 `Paginated Updater` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 它直接协作于事件源、上下文对象和后续处理组件。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class PaginatedUpdater<I, D> {

    /**
     * 数量限制常量，用于统一引用固定值。
     */
    private static final int DEFAULT_LIMIT = 100;
    private int updated = 0;

    /**
     * 功能：更新`Entities`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    public void updateEntities(I id) {
        updated = 0;
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        boolean hasNext = true;
        while (hasNext) {
            PageData<D> entities = findEntities(id, pageLink);
            for (D entity : entities.getData()) {
                updateEntity(entity);
            }
            updated += entities.getData().size();
            hasNext = entities.hasNext();
            if (hasNext) {
                log.info("{}: {} entities updated so far...", getName(), updated);
                pageLink = pageLink.nextPageLink();
            } else {
                if (updated > DEFAULT_LIMIT || forceReportTotal()) {
                    log.info("{}: {} total entities updated.", getName(), updated);
                }
            }
        }
    }

    /**
     * 功能：更新`Entities`。
     * 参数：无。
     * 返回：无。
     */
    public void updateEntities() {
        updateEntities(null);
    }

    /**
     * 功能：执行 `forceReportTotal` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    protected boolean forceReportTotal() {
        return false;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    protected abstract String getName();

    /**
     * 功能：获取`Entities`。
     * 参数：
     * - `id`：`id`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    protected abstract PageData<D> findEntities(I id, PageLink pageLink);

    /**
     * 功能：更新实体。
     * 参数：
     * - `entity`：实体对象。
     * 返回：无。
     */
    protected abstract void updateEntity(D entity);

}
