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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

/**
 * 中文说明：
 * 1. `DeleteDeviceTest` 是 ThingsBoard Microservices 中验证 `DeleteDevice` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDeviceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Delete device")
public class DeleteDeviceTest extends AbstractDeviceTest {

    /**
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void createDevice() {
        Device device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();
    }

    /**
     * 功能：删除或清理设备。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Remove the device by clicking on the trash icon in the right side of device")
    public void deleteDeviceByRightSideBtn() {
        sideBarMenuView.goToDevicesPage();
        devicePage.deleteDeviceByRightSideBtn(deviceName);
        devicePage.refreshBtn().click();

        devicePage.assertEntityIsNotPresent(deviceName);
    }

    /**
     * 功能：删除或清理设备。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Remove device by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void deleteSelectedDevice() {
        sideBarMenuView.goToDevicesPage();
        devicePage.deleteSelected(deviceName);
        devicePage.refreshBtn().click();

        devicePage.assertEntityIsNotPresent(deviceName);
    }

    /**
     * 功能：删除或清理设备。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Remove the device by clicking on the 'Delete device' btn in the entity view")
    public void deleteDeviceFromDetailsTab() {
        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.deleteDeviceFromDetailsTab();
        devicePage.refreshBtn();

        devicePage.assertEntityIsNotPresent(deviceName);
    }

    /**
     * 功能：删除或清理设备。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Remove the device by clicking on the trash icon in the right side of device without refresh")
    public void deleteDeviceWithoutRefresh() {
        sideBarMenuView.goToDevicesPage();
        devicePage.deleteDeviceByRightSideBtn(deviceName);

        devicePage.assertEntityIsNotPresent(deviceName);
    }
}
