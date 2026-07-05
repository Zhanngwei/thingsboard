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
package org.thingsboard.server.msa.ui.base;

import lombok.SneakyThrows;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.fail;

/**
 * 中文说明：
 * 1. 类目的：`AbstractBasePage` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
abstract public class AbstractBasePage {
    public static final long WAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    /**
     * `driver` 字段，保存当前对象的对应属性。
     */
    protected WebDriver driver;
    protected WebDriverWait wait;
    /**
     * `actions` 字段，保存当前对象的对应属性。
     */
    protected Actions actions;
    protected JavascriptExecutor js;

    /**
     * 功能：创建 `AbstractBasePage` 实例，并初始化必要字段。
     * 参数：
     * - `driver`：`driver` 参数。
     * 返回：新创建的对象实例。
     */
    public AbstractBasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofMillis(WAIT_TIMEOUT));
        this.actions = new Actions(driver);
        this.js = (JavascriptExecutor) driver;
    }

    /**
     * 功能：执行 `sleep` 对应的处理。
     * 参数：
     * - `second`：`second` 参数。
     * 返回：无。
     */
    @SneakyThrows
    protected static void sleep(double second) {
        Thread.sleep((long) (second * 1000L));
    }

    /**
     * 功能：执行 `waitUntilVisibilityOfElementLocated` 对应的处理。
     * 参数：
     * - `locator`：`locator` 参数。
     * 返回：处理结果。
     */
    protected WebElement waitUntilVisibilityOfElementLocated(String locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator)));
        } catch (WebDriverException e) {
            return fail("No visibility element: " + locator);
        }
    }

    /**
     * 功能：执行 `waitUntilPresenceOfElementLocated` 对应的处理。
     * 参数：
     * - `locator`：`locator` 参数。
     * 返回：处理结果。
     */
    protected WebElement waitUntilPresenceOfElementLocated(String locator) {
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(locator)));
        } catch (WebDriverException e) {
            return fail("No presence element: " + locator);
        }
    }

    /**
     * 功能：执行 `waitUntilElementToBeClickable` 对应的处理。
     * 参数：
     * - `locator`：`locator` 参数。
     * 返回：处理结果。
     */
    protected WebElement waitUntilElementToBeClickable(String locator) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(locator)));
        } catch (WebDriverException e) {
            return fail("No clickable element: " + locator);
        }
    }

    /**
     * 功能：执行 `waitUntilVisibilityOfElementsLocated` 对应的处理。
     * 参数：
     * - `locator`：`locator` 参数。
     * 返回：匹配的数据集合。
     */
    protected List<WebElement> waitUntilVisibilityOfElementsLocated(String locator) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator)));
            return driver.findElements(By.xpath(locator));
        } catch (WebDriverException e) {
            return fail("No visibility elements: " + locator);
        }
    }

    /**
     * 功能：执行 `waitUntilElementsToBeClickable` 对应的处理。
     * 参数：
     * - `locator`：`locator` 参数。
     * 返回：匹配的数据集合。
     */
    protected List<WebElement> waitUntilElementsToBeClickable(String locator) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath(locator)));
            return driver.findElements(By.xpath(locator));
        } catch (WebDriverException e) {
            return fail("No clickable elements: " + locator);
        }
    }

    /**
     * 功能：执行 `waitUntilUrlContainsText` 对应的处理。
     * 参数：
     * - `urlPath`：文件或资源路径。
     * 返回：无。
     */
    public void waitUntilUrlContainsText(String urlPath) {
        try {
            wait.until(ExpectedConditions.urlContains(urlPath));
        } catch (WebDriverException e) {
            fail("This URL path is missing");
        }
    }

    /**
     * 功能：执行 `moveCursor` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * 返回：无。
     */
    protected void moveCursor(WebElement element) {
        actions.moveToElement(element).perform();
    }

    /**
     * 功能：执行 `doubleClick` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * 返回：无。
     */
    protected void doubleClick(WebElement element) {
        actions.doubleClick(element).build().perform();
    }

    /**
     * 功能：执行 `elementIsNotPresent` 对应的处理。
     * 参数：
     * - `locator`：`locator` 参数。
     * 返回：判断结果。
     */
    public boolean elementIsNotPresent(String locator) {
        try {
            return wait.until(ExpectedConditions.not(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator))));
        } catch (WebDriverException e) {
            return fail("Element is present: " + locator);
        }
    }

    /**
     * 功能：执行 `elementsIsNotPresent` 对应的处理。
     * 参数：
     * - `locator`：`locator` 参数。
     * 返回：判断结果。
     */
    public boolean elementsIsNotPresent(String locator) {
        try {
            return wait.until(ExpectedConditions.not(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(locator))));
        } catch (WebDriverException e) {
            return fail("Elements is present: " + locator);
        }
    }

    /**
     * 功能：执行 `waitUntilNumberOfTabToBe` 对应的处理。
     * 参数：
     * - `tabNumber`：`tabNumber` 参数。
     * 返回：无。
     */
    public void waitUntilNumberOfTabToBe(int tabNumber) {
        try {
            wait.until(ExpectedConditions.numberOfWindowsToBe(tabNumber));
        } catch (WebDriverException e) {
            fail("No tabs with this number: " + tabNumber);
        }
    }

    /**
     * 功能：执行 `jsClick` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * 返回：无。
     */
    public void jsClick(WebElement element) {
        js.executeScript("arguments[0].click();", element);
    }

    /**
     * 功能：执行 `enterText` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * - `keysToEnter`：键。
     * 返回：无。
     */
    public void enterText(WebElement element, CharSequence keysToEnter) {
        element.click();
        element.sendKeys(keysToEnter);
        if (element.getAttribute("value").isEmpty()) {
            element.sendKeys(keysToEnter);
        }
    }

    /**
     * 功能：执行 `scrollToElement` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * 返回：无。
     */
    public void scrollToElement(WebElement element) {
        js.executeScript("arguments[0].scrollIntoView(true);", element);
    }

    /**
     * 功能：执行 `waitUntilAttributeContains` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * - `attribute`：`attribute` 参数。
     * - `value`：值。
     * 返回：无。
     */
    public void waitUntilAttributeContains(WebElement element, String attribute, String value) {
        try {
            wait.until(ExpectedConditions.attributeContains(element, attribute, value));
        } catch (WebDriverException e) {
            fail("Failed to wait until attribute '" + attribute + "' of element '" + element + "' contains value '" + value + "'");
        }
    }

    /**
     * 功能：执行 `goToNextTab` 对应的处理。
     * 参数：
     * - `tabNumber`：`tabNumber` 参数。
     * 返回：无。
     */
    public void goToNextTab(int tabNumber) {
        waitUntilNumberOfTabToBe(tabNumber);
        ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(tabs.get(tabNumber - 1));
    }

    /**
     * 功能：获取`Random Number`。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String getRandomNumber() {
        StringBuilder random = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            random.append(ThreadLocalRandom.current().nextInt(0, 100));
        }
        return random.toString();
    }

    /**
     * 功能：执行 `randomUUID` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String randomUUID() {
        UUID randomUUID = UUID.randomUUID();
        return randomUUID.toString().replaceAll("_", "");
    }

    /**
     * 功能：执行 `random` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    public static String random() {
        return getRandomNumber() + randomUUID().substring(0, 6);
    }

    /**
     * 功能：获取`Random Symbol`。
     * 参数：无。
     * 返回：处理结果。
     */
    public static char getRandomSymbol() {
        Random rand = new Random();
        String s = "~`!@#$^&*()_+=-";
        return s.charAt(rand.nextInt(s.length()));
    }

    /**
     * 功能：执行 `pull` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * - `xOffset`：偏移量。
     * - `yOffset`：偏移量。
     * 返回：无。
     */
    public void pull(WebElement element, int xOffset, int yOffset) {
        actions.clickAndHold(element).moveByOffset(xOffset, yOffset).release().perform();
    }

    /**
     * 功能：执行 `waitUntilAttributeToBe` 对应的处理。
     * 参数：
     * - `locator`：`locator` 参数。
     * - `attribute`：`attribute` 参数。
     * - `value`：值。
     * 返回：无。
     */
    public void waitUntilAttributeToBe(String locator, String attribute, String value) {
        try {
            wait.until(ExpectedConditions.attributeToBe(By.xpath(locator), attribute, value));
        } catch (WebDriverException e) {
            fail("Failed to wait until attribute '" + attribute + "' of element located by '" + locator + "' is '" + value + "'");
        }
    }

    /**
     * 功能：删除或清理字段名。
     * 参数：
     * - `element`：`element` 参数。
     * 返回：无。
     */
    public void clearInputField(WebElement element) {
        element.click();
        element.sendKeys(Keys.CONTROL + "A" + Keys.BACK_SPACE);
    }

    /**
     * 功能：执行 `waitUntilAttributeToBeNotEmpty` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * - `attribute`：`attribute` 参数。
     * 返回：无。
     */
    public void waitUntilAttributeToBeNotEmpty(WebElement element, String attribute) {
        try {
            wait.until(ExpectedConditions.attributeToBeNotEmpty(element, attribute));
        } catch (WebDriverException e) {
            fail("Failed to wait until attribute '" + attribute + "' of element '" + element + "' is not empty");
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractBasePage` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
