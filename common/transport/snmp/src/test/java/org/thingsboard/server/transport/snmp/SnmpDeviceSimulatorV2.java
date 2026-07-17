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
package org.thingsboard.server.transport.snmp;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.TransportMappings;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `SnmpDeviceSimulatorV2` 是 ThingsBoard Common Transport 中负责设备接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `BaseAgent`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@SuppressWarnings("deprecation")
public class SnmpDeviceSimulatorV2 extends BaseAgent {

    /**
     * 目标对象，表示当前对象的对应属性。
     */
    private final Target target;
    private final Address address;
    /**
     * `mappings`映射关系，用于按键查找对应值。
     */
    private final Map<String, String> mappings;
    private Snmp snmp;

    /**
     * 功能：创建 `SnmpDeviceSimulatorV2` 实例，并初始化必要字段。
     * 参数：
     * - `port`：`port` 参数。
     * - `password`：`password` 参数。
     * - `mappings`：键值映射。
     * 返回：新创建的对象实例。
     */
    public SnmpDeviceSimulatorV2(int port, String password, Map<String, String> mappings) throws IOException {
        super(new File("conf.agent"), new File("bootCounter.agent"), new CommandProcessor(new OctetString("12312")));
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(password));
        this.address = GenericAddress.parse("udp:0.0.0.0/" + port);
        target.setAddress(address);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        this.target = target;
        this.mappings = mappings;
    }

    /**
     * 功能：执行 `start` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void start() throws IOException {
        init();
        addShutdownHook();
        getServer().addContext(new OctetString("public"));
        finishInit();
        run();
        sendColdStartNotification();
        snmp = new Snmp(transportMappings[0]);
    }

    /**
     * 功能：发送或提交`Trap`。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `values`：值。
     * 返回：无。
     */
    public void sendTrap(String host, int port, Map<String, String> values) throws IOException {
        PDU pdu = new PDU();
        pdu.addAll(values.entrySet().stream()
                .map(entry -> new VariableBinding(new OID(entry.getKey()), new OctetString(entry.getValue())))
                .collect(Collectors.toList()));
        pdu.setType(PDU.TRAP);

        CommunityTarget remoteTarget = (CommunityTarget) getTarget().clone();
        remoteTarget.setAddress(new UdpAddress(host + "/" + port));

        snmp.send(pdu, remoteTarget);
    }

    /**
     * 功能：保存或创建`Managed Objects`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void registerManagedObjects() {
        unregisterManagedObject(getSnmpv2MIB());
        mappings.forEach((oid, response) -> {
            registerManagedObject(new MOScalar<>(new OID(oid), MOAccessImpl.ACCESS_READ_WRITE, new OctetString(response)));
        });
    }

    /**
     * 功能：保存或创建`Managed Object`。
     * 参数：
     * - `mo`：`mo` 参数。
     * 返回：无。
     */
    protected void registerManagedObject(ManagedObject mo) {
        try {
            server.register(mo, null);
        } catch (DuplicateRegistrationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 功能：执行 `unregisterManagedObject` 对应的处理。
     * 参数：
     * - `moGroup`：`moGroup` 参数。
     * 返回：无。
     */
    protected void unregisterManagedObject(MOGroup moGroup) {
        moGroup.unregisterMOs(server, getContext(moGroup));
    }

    /**
     * 功能：保存或创建通知。
     * 参数：
     * - `targetMIB`：`targetMIB` 参数。
     * - `notificationMIB`：`notificationMIB` 参数。
     * 返回：无。
     */
    @Override
    protected void addNotificationTargets(SnmpTargetMIB targetMIB,
                                          SnmpNotificationMIB notificationMIB) {
    }

    /**
     * 功能：保存或创建`Views`。
     * 参数：
     * - `vacm`：`vacm` 参数。
     * 返回：无。
     */
    @Override
    protected void addViews(VacmMIB vacm) {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(
                        "cpublic"), new OctetString("v1v2group"),
                StorageType.nonVolatile);

        vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
                SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
                new OctetString("fullWriteView"), new OctetString(
                        "fullNotifyView"), StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `usm`：`usm` 参数。
     * 返回：无。
     */
    protected void addUsmUser(USM usm) {
    }

    /**
     * 功能：初始化或启动传输层。
     * 参数：无。
     * 返回：无。
     */
    @SuppressWarnings({"unchecked"})
    protected void initTransportMappings() {
        transportMappings = new TransportMapping[]{TransportMappings.getInstance().createTransportMapping(address)};
    }

    /**
     * 功能：执行 `unregisterManagedObjects` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void unregisterManagedObjects() {
        unregisterManagedObject(getSnmpv2MIB());
    }

    /**
     * 功能：保存或创建`Communities`。
     * 参数：
     * - `communityMIB`：`communityMIB` 参数。
     * 返回：无。
     */
    protected void addCommunities(SnmpCommunityMIB communityMIB) {
        Variable[] com2sec = new Variable[]{
                new OctetString("public"),
                new OctetString("cpublic"),
                getAgent().getContextEngineID(),
                new OctetString("public"),
                new OctetString(),
                new Integer32(StorageType.nonVolatile),
                new Integer32(RowStatus.active)
        };
        SnmpCommunityMIB.SnmpCommunityEntryRow row = communityMIB.getSnmpCommunityEntry().createRow(
                new OctetString("public2public").toSubIndex(true), com2sec);
        communityMIB.getSnmpCommunityEntry().addRow(row);
    }

    /**
     * 功能：获取目标对象。
     * 参数：无。
     * 返回：处理结果。
     */
    public Target getTarget() {
        return target;
    }

}
