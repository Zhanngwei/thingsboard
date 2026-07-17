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
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_DEVICE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

/**
 * 中文说明：
 * 1. `EditDeviceTest` 是 ThingsBoard Microservices 中验证 `EditDevice` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDeviceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Feature("Edit device")
public class EditDeviceTest extends AbstractDeviceTest {

    /**
     * 功能：执行 `changeName` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Change name by edit menu")
    public void changeName() {
        String newDeviceName = "Changed" + getRandomNumber();
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.setHeaderName();
        String nameBefore = devicePage.getHeaderName();
        devicePage.editPencilBtn().click();
        devicePage.changeNameEditMenu(newDeviceName);
        devicePage.doneBtnEditView().click();
        deviceName = newDeviceName;
        devicePage.setHeaderName();
        String nameAfter = devicePage.getHeaderName();

        assertThat(nameAfter).as("The name has changed").isNotEqualTo(nameBefore);
        assertThat(nameAfter).as("The name has changed correctly").isEqualTo(newDeviceName);
    }

    /**
     * 功能：删除或清理名称。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Delete name and save")
    public void deleteName() {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.changeNameEditMenu("");

        assertIsDisable(devicePage.doneBtnEditViewVisible());
    }

    /**
     * 功能：保存或创建`Only With Space`。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Save only with space")
    public void saveOnlyWithSpace() {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.changeNameEditMenu(" ");
        devicePage.doneBtnEditView().click();

        assertIsDisplayed(devicePage.warningMessage());
        assertThat(devicePage.warningMessage().getText()).as("Text of warning message").isEqualTo(EMPTY_DEVICE_MESSAGE);
    }

    /**
     * 功能：执行 `editDescription` 对应的处理。
     * 参数：
     * - `description`：`description` 参数。
     * - `newDescription`：`newDescription` 参数。
     * - `finalDescription`：`finalDescription` 参数。
     * 返回：无。
     */
    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME, description)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.descriptionEntityView().sendKeys(newDescription);
        devicePage.doneBtnEditView().click();
        devicePage.setDescription();

        assertThat(devicePage.getDescription()).as("The description changed correctly").isEqualTo(finalDescription);
    }

    /**
     * 功能：判断`Gateway`。
     * 参数：
     * - `isGateway`：`isGateway` 参数。
     * 返回：无。
     */
    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "enable")
    @Description("Enable gateway mode/Disable gateway")
    public void isGateway(boolean isGateway) {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME, isGateway)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.checkboxGatewayEdit().click();
        devicePage.doneBtnEditView().click();

        if (isGateway) {
            assertThat(devicePage.checkboxGatewayDetailsTab().getAttribute("class").contains("selected"))
                    .as("Gateway is disable").isFalse();
        } else {
            assertThat(devicePage.checkboxGatewayDetailsTab().getAttribute("class").contains("selected"))
                    .as("Gateway is enable").isTrue();
        }
    }

    /**
     * 功能：判断设备。
     * 参数：
     * - `isOverwriteActivityTimeForConnected`：`isOverwriteActivityTimeForConnected` 参数。
     * 返回：无。
     */
    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "enable")
    @Description("Enable overwrite activity time for connected/Disable overwrite activity time for connected")
    public void isOverwriteActivityTimeForConnectedDevice(boolean isOverwriteActivityTimeForConnected) {
        deviceName = testRestClient.postDevice("",
                EntityPrototypes.defaultDevicePrototype(ENTITY_NAME, true, isOverwriteActivityTimeForConnected)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.checkboxOverwriteActivityTimeEdit().click();
        devicePage.doneBtnEditView().click();

        if (isOverwriteActivityTimeForConnected) {
            assertThat(devicePage.checkboxOverwriteActivityTimeDetails().getAttribute("class").contains("selected"))
                    .as("Overwrite activity time for connected is disable").isFalse();
        } else {
            assertThat(devicePage.checkboxOverwriteActivityTimeDetails().getAttribute("class").contains("selected"))
                    .as("Overwrite activity time for connected is enable").isTrue();
        }
    }

    /**
     * 功能：执行 `changeDeviceProfile` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Change device profile")
    public void changeDeviceProfile() {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.changeDeviceProfile("DEFAULT");
        devicePage.doneBtnEditView().click();

        assertIsDisplayed(devicePage.deviceProfileRedirectedBtn());
        assertThat(devicePage.deviceProfileRedirectedBtn().getText()).as("Profile changed correctly").isEqualTo("DEFAULT");
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Test(groups = "smoke")
    @Description("Save without device profile")
    public void saveWithoutDeviceProfile() {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.clearProfileFieldBtn().click();

        assertIsDisable(devicePage.doneBtnEditViewVisible());
    }

    /**
     * 功能：执行 `editLabel` 对应的处理。
     * 参数：
     * - `label`：`label` 参数。
     * - `newLabel`：`newLabel` 参数。
     * - `finalLabel`：`finalLabel` 参数。
     * 返回：无。
     */
    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editDeviceLabel")
    @Description("Write the label and save the changes/Change the label and save the changes/Delete the label and save the changes")
    public void editLabel(String label, String newLabel, String finalLabel) {
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME, "", label)).getName();

        sideBarMenuView.goToDevicesPage();
        devicePage.entity(deviceName).click();
        devicePage.editPencilBtn().click();
        devicePage.deviceLabelEditField().sendKeys(newLabel);
        devicePage.doneBtnEditView().click();
        devicePage.setLabel();

        assertThat(devicePage.getLabel()).as("The label changed correctly").isEqualTo(finalLabel);
    }
}
