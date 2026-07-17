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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

/**
 * An enumeration of Sparkplug MQTT message types.  The type provides an indication as to what the MQTT Payload of 
 * message will contain.
 */
/**
 * 中文说明：
 * 1. `SparkplugMessageType` 是 ThingsBoard Common Transport 中定义消息固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum SparkplugMessageType {
	
	/**
	 * Birth certificate for MQTT Edge of Network (EoN) Nodes.
	 */
	NBIRTH,
	
	/**
	 * Death certificate for MQTT Edge of Network (EoN) Nodes.
	 */
	NDEATH,
	
	/**
	 * Birth certificate for MQTT Devices.
	 */
	DBIRTH,
	
	/**
	 * Death certificate for MQTT Devices.
	 */
	DDEATH,
	
	/**
	 * Edge of Network (EoN) Node data message.
	 */
	NDATA,
	
	/**
	 * Device data message.
	 */
	DDATA,
	
	/**
	 * Edge of Network (EoN) Node command message.
	 */
	NCMD,
	
	/**
	 * Device command message.
	 */
	DCMD,
	
	/**
	 * Critical application state message.
	 */
	STATE,
	
	/**
	 * Device record message.
	 */
	DRECORD,
	
	/**
	 * Edge of Network (EoN) Node record message.
	 */
	NRECORD;
	
	/**
	 * 功能：解析消息。
	 * 参数：
	 * - `type`：类型。
	 * 返回：处理结果。
	 */
	public static SparkplugMessageType parseMessageType(String type) throws ThingsboardException {
		for (SparkplugMessageType messageType : SparkplugMessageType.values()) {
			if (messageType.name().equals(type)) {
				return messageType;
			}
		}
		throw new ThingsboardException("Invalid message type: " + type, ThingsboardErrorCode.INVALID_ARGUMENTS);
	}
	/**
	 * 功能：执行 `messageName` 对应的处理。
	 * 参数：
	 * - `type`：类型。
	 * 返回：文本结果。
	 */
	public static String messageName(SparkplugMessageType type) {
		return STATE.equals(type) ? "sparkplugConnectionState" : type.name();
	}
	
	/**
	 * 功能：判断`Death`。
	 * 参数：无。
	 * 返回：判断结果。
	 */
	public boolean isDeath() {
		return this.equals(DDEATH) || this.equals(NDEATH);
	}
	
	/**
	 * 功能：判断`Command`。
	 * 参数：无。
	 * 返回：判断结果。
	 */
	public boolean isCommand() {
		return this.equals(DCMD) || this.equals(NCMD);
	}
	
	/**
	 * 功能：判断数据。
	 * 参数：无。
	 * 返回：判断结果。
	 */
	public boolean isData() {
		return this.equals(DDATA) || this.equals(NDATA);
	}
	
	/**
	 * 功能：判断`Birth`。
	 * 参数：无。
	 * 返回：判断结果。
	 */
	public boolean isBirth() {
		return this.equals(DBIRTH) || this.equals(NBIRTH);
	}
	
	/**
	 * 功能：判断`Record`。
	 * 参数：无。
	 * 返回：判断结果。
	 */
	public boolean isRecord() {
		return this.equals(DRECORD) || this.equals(NRECORD);
	}
}
