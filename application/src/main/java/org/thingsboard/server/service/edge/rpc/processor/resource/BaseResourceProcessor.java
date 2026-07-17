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
package org.thingsboard.server.service.edge.rpc.processor.resource;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

/**
 * 中文说明：
 * 1. `BaseResourceProcessor` 是 ThingsBoard Application 中处理资源的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `BaseEdgeProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class BaseResourceProcessor extends BaseEdgeProcessor {

    /**
     * 功能：保存或创建`Or Update Tb Resource`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tbResourceId`：`tbResourceId`ID。
     * - `resourceUpdateMsg`：待处理消息。
     * 返回：判断结果。
     */
    protected boolean saveOrUpdateTbResource(TenantId tenantId, TbResourceId tbResourceId, ResourceUpdateMsg resourceUpdateMsg) {
        boolean resourceKeyUpdated = false;
        try {
            TbResource resource = constructResourceFromUpdateMsg(tenantId, tbResourceId, resourceUpdateMsg);
            if (resource == null) {
                throw new RuntimeException("[{" + tenantId + "}] resourceUpdateMsg {" + resourceUpdateMsg + " } cannot be converted to resource");
            }
            boolean created = false;
            TbResource resourceById = resourceService.findResourceById(tenantId, tbResourceId);
            if (resourceById == null) {
                resource.setCreatedTime(Uuids.unixTimestamp(tbResourceId.getId()));
                created = true;
                resource.setId(null);
            } else {
                resource.setId(tbResourceId);
            }
            String resourceKey = resource.getResourceKey();
            ResourceType resourceType = resource.getResourceType();
            PageDataIterable<TbResource> resourcesIterable = new PageDataIterable<>(
                    link -> resourceService.findTenantResourcesByResourceTypeAndPageLink(tenantId, resourceType, link), 1024);
            for (TbResource tbResource : resourcesIterable) {
                if (tbResource.getResourceKey().equals(resourceKey) && !tbResourceId.equals(tbResource.getId())) {
                    resourceKey = StringUtils.randomAlphabetic(15) + "_" + resourceKey;
                    log.warn("[{}] Resource with resource type {} and key {} already exists. Renaming resource key to {}",
                            tenantId, resourceType, resource.getResourceKey(), resourceKey);
                    resourceKeyUpdated = true;
                }
            }
            resource.setResourceKey(resourceKey);
            resourceValidator.validate(resource, TbResourceInfo::getTenantId);
            if (created) {
                resource.setId(tbResourceId);
            }
            resourceService.saveResource(resource, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process resource update msg [{}]", tenantId, resourceUpdateMsg, e);
            throw e;
        }
        return resourceKeyUpdated;
    }

    /**
     * 功能：执行 `constructResourceFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tbResourceId`：`tbResourceId`ID。
     * - `resourceUpdateMsg`：待处理消息。
     * 返回：处理结果。
     */
    protected abstract TbResource constructResourceFromUpdateMsg(TenantId tenantId, TbResourceId tbResourceId, ResourceUpdateMsg resourceUpdateMsg);
}
