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
package org.thingsboard.server.service.sync.vc;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文说明：
 * 1. `PendingCommit` 是 ThingsBoard Common 中围绕 `Pending Commit` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Data
public class PendingCommit {

    /**
     * `txId`ID，用于定位对应业务对象。
     */
    private final UUID txId;
    private final String nodeId;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final String workingBranch;
    /**
     * 分支名称，用于展示或标识当前对象。
     */
    private String branch;
    private String versionName;

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String authorName;
    private String authorEmail;

    /**
     * `chunkedMsgs`列表，用于保存一组待处理对象。
     */
    private Map<String, String[]> chunkedMsgs;

    /**
     * 功能：创建 `PendingCommit` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `nodeId`：节点实例ID。
     * - `txId`：`txId`ID。
     * - `branch`：`branch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public PendingCommit(TenantId tenantId, String nodeId, UUID txId, String branch, String versionName, String authorName, String authorEmail) {
        this.tenantId = tenantId;
        this.nodeId = nodeId;
        this.txId = txId;
        this.branch = branch;
        this.versionName = versionName;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.workingBranch = txId.toString();
    }

    /**
     * 功能：获取`Chunked Msgs`。
     * 参数：无。
     * 返回：处理结果。
     */
    public Map<String, String[]> getChunkedMsgs() {
        if (chunkedMsgs == null) {
            chunkedMsgs = new ConcurrentHashMap<>();
        }
        return chunkedMsgs;
    }

}
