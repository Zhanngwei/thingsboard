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
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

import java.util.UUID;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

/**
 * 中文说明：
 * 1. `WidgetTypeDataValidatorTest` 是 ThingsBoard DAO 中验证 `WidgetTypeDataValidator` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@SpringBootTest(classes = WidgetTypeDataValidator.class)
class WidgetTypeDataValidatorTest {
    /**
     * 部件类型，用于读取或保存对应领域对象。
     */
    @MockBean
    WidgetTypeDao widgetTypeDao;
    /**
     * 部件包，用于读取或保存对应领域对象。
     */
    @MockBean
    WidgetsBundleDao widgetsBundleDao;
    /**
     * 租户，提供当前类调用的业务操作。
     */
    @MockBean
    TenantService tenantService;
    /**
     * 校验器，封装可复用的处理规则。
     */
    @SpyBean
    WidgetTypeDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    /**
     * 功能：验证名称相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testValidateNameInvocation() {
        WidgetTypeDetails widgetTypeDetails = new WidgetTypeDetails();
        widgetTypeDetails.setName("widget type gas");
        widgetTypeDetails.setDescriptor(JacksonUtil.toJsonNode("{\"content\":\"empty\"}"));
        widgetTypeDetails.setTenantId(tenantId);

        validator.validateDataImpl(tenantId, widgetTypeDetails);
        verify(validator).validateString("Widgets type name", widgetTypeDetails.getName());
    }

}
