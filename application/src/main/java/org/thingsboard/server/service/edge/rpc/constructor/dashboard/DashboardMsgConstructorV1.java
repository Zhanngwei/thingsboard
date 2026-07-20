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
package org.thingsboard.server.service.edge.rpc.constructor.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `DashboardMsgConstructorV1` 是 ThingsBoard Application 中创建或提供仪表盘对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `BaseDashboardMsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public class DashboardMsgConstructorV1 extends BaseDashboardMsgConstructor {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ImageService imageService;

    /**
     * 功能：执行 `constructDashboardUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `dashboard`：`dashboard` 参数。
     * 返回：处理结果。
     */
    @Override
    public DashboardUpdateMsg constructDashboardUpdatedMsg(UpdateMsgType msgType, Dashboard dashboard) {
        dashboard = JacksonUtil.clone(dashboard);
        imageService.inlineImagesForEdge(dashboard);
        DashboardUpdateMsg.Builder builder = DashboardUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(dashboard.getId().getId().getMostSignificantBits())
                .setIdLSB(dashboard.getId().getId().getLeastSignificantBits())
                .setTitle(dashboard.getTitle())
                .setConfiguration(JacksonUtil.toString(dashboard.getConfiguration()))
                .setMobileHide(dashboard.isMobileHide());
        if (dashboard.getAssignedCustomers() != null) {
            builder.setAssignedCustomers(JacksonUtil.toString(dashboard.getAssignedCustomers()));
        }
        if (dashboard.getImage() != null) {
            builder.setImage(dashboard.getImage());
        }
        if (dashboard.getMobileOrder() != null) {
            builder.setMobileOrder(dashboard.getMobileOrder());
        }
        return builder.build();
    }

}
