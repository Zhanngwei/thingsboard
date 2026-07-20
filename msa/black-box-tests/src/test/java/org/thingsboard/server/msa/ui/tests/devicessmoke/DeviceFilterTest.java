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
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

/**
 * 中文说明：
 * 1. `DeviceFilterTest` 是 ThingsBoard Microservices 中验证 `DeviceFilter` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDeviceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Filter devices (By device profile and state)")
public class DeviceFilterTest extends AbstractDeviceTest {

    /**
     * 当前对象是否处于激活状态。
     */
    private String activeDeviceName;
    private String deviceWithProfileName;
    /**
     * 当前对象是否处于激活状态。
     */
    private String activeDeviceWithProfileName;

    /**
     * 功能：保存或创建`Test Entities`。
     * 参数：无。
     * 返回：无。
     */
    @BeforeClass
    public void createTestEntities() {
        DeviceProfile deviceProfile = testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(ENTITY_NAME + random()));
        Device deviceWithProfile = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random(), deviceProfile.getId()));
        Device activeDevice = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random()));
        Device activeDeviceWithProfile = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random(), deviceProfile.getId()));

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(activeDevice.getId());
        DeviceCredentials deviceCredentials1 = testRestClient.getDeviceCredentialsByDeviceId(activeDeviceWithProfile.getId());
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), JacksonUtil.toJsonNode(createPayload().toString()));
        testRestClient.postTelemetry(deviceCredentials1.getCredentialsId(), JacksonUtil.toJsonNode(createPayload().toString()));

        deviceProfileTitle = deviceProfile.getName();
        deviceWithProfileName = deviceWithProfile.getName();
        activeDeviceName = activeDevice.getName();
        activeDeviceWithProfileName = activeDeviceWithProfile.getName();
    }

    /**
     * 功能：删除或清理`Test Entities`。
     * 参数：无。
     * 返回：无。
     */
    @AfterClass
    public void deleteTestEntities() {
        deleteDevicesByName(List.of(deviceWithProfileName, activeDeviceName, activeDeviceWithProfileName));
        deleteDeviceProfileByTitle(deviceProfileTitle);
    }

    /**
     * 功能：执行 `filterDevicesByProfile` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Filter by device profile")
    public void filterDevicesByProfile() {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByDeviceProfile(deviceProfileTitle);

        devicePage.listOfDevicesProfile().forEach(d -> assertThat(d.getText())
                .as("There are only devices with the selected profile(%s) on the page", deviceProfileTitle)
                .isEqualTo(deviceProfileTitle));
    }

    /**
     * 功能：执行 `filterDevicesByState` 对应的处理。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：无。
     */
    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "filterData")
    @Description("Filter by state")
    public void filterDevicesByState(String state) {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByState(state);

        devicePage.listOfDevicesState().forEach(d -> assertThat(d.getText())
                .as("There are only devices with '%s' state on the page", state)
                .isEqualTo(state));
    }

    /**
     * 功能：执行 `filterDevicesByDeviceProfileAndState` 对应的处理。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：无。
     */
    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "filterData")
    @Description("Filter device by device profile and state")
    public void filterDevicesByDeviceProfileAndState(String state) {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByDeviceProfileAndState(deviceProfileTitle, state);

        devicePage.listOfDevicesProfile().forEach(d -> assertThat(d.getText())
                .as("There are only devices with the selected profile(%s) on the page", deviceProfileTitle)
                .isEqualTo(deviceProfileTitle));
        devicePage.listOfDevicesState().forEach(d -> assertThat(d.getText())
                .as("There are only devices with '%s' state on the page", state)
                .isEqualTo(state));
    }
}
