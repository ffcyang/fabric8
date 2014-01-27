/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.gateway.fabric.mq;

import io.fabric8.api.scr.support.ConfigInjection;
import io.fabric8.internal.Objects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.fusesource.common.util.Strings;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.fabric.FabricGateway;
import org.fusesource.gateway.fabric.FabricGatewaySupport;
import org.fusesource.gateway.fabric.GatewayListener;
import org.fusesource.gateway.handlers.Gateway;
import org.fusesource.gateway.handlers.tcp.TcpGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An MQ gateway which listens to a part of the ZooKeeper tree for messaging services and exposes those over protocol specific ports.
 */
@Component(name = "io.fabric8.gateway.mq", immediate = true, metatype = true,
        label = "Fabric8 MQ Gateway",
        description = "Provides a discovery and load balancing gateway between clients using various messaging protocols and the available message brokers in the fabric")
public class FabricMQGateway extends FabricGatewaySupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricMQGateway.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setGateway", unbind = "unsetGateway")
    private FabricGateway gateway;

    @Property(name = "zooKeeperPath", value = "/fabric/registry/clusters/fusemq",
    label="ZooKeeper path", description = "The path in ZooKeeper which is monitored to discover the available message brokers")
    private String zooKeeperPath;

    @Property(name = "host",
    label="Host name", description = "The host name used when listening on the various messaging ports")
    private String host;

    @Property(name = "openWireEnabled", boolValue = true,
    label = "OpenWire enabled", description = "Enable or disable the OpenWire transport protocol")
    private boolean openWireEnabled = true;
    @Property(name = "openWirePort", intValue = 61616,
    label = "OpenWire port", description =  "Port number to listen on for OpenWire")
    private int openWirePort = 61616;

    @Property(name = "stompEnabled", boolValue = true,
            label = "STOMP enabled", description = "Enable or disable the STOMP transport protocol")
    private boolean stompEnabled = true;
    @Property(name = "stompPort", intValue = 61613,
            label = "STOMP port", description =  "Port number to listen on for STOMP")
    private int stompPort = 61613;

    @Property(name = "amqpEnabled", boolValue = true,
            label = "AMQP enabled", description = "Enable or disable the AMQP transport protocol")
    private boolean amqpEnabled = true;
    @Property(name = "amqpPort", intValue = 5672,
            label = "AMQP port", description =  "Port number to listen on for AMQP")
    private int amqpPort = 5672;

    @Property(name = "mqttEnabled", boolValue = true,
            label = "MQTT enabled", description = "Enable or disable the MQTT transport protocol")
    private boolean mqttEnabled = true;
    @Property(name = "mqttPort", intValue = 5672,
    label = "MQTT port", description =  "Port number to listen on for MQTT")
    private int mqttPort = 5672;

    @Property(name = "websocketEnabled", boolValue = true,
            label = "WebSocket enabled", description = "Enable or disable the WebSocket transport protocol")
    private boolean websocketEnabled = true;
    @Property(name = "websocketPort", intValue = 61614,
            label = "WebSocket port", description =  "Port number to listen on for WebSocket")
    private int websocketPort = 61614;

    private GatewayListener gatewayListener;

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        ConfigInjection.applyConfiguration(configuration, this);
        Objects.notNull(getGateway(), "gateway");
        Objects.notNull(getZooKeeperPath(), "zooKeeperPath");
        activateComponent();

        gatewayListener = createListener();
        if (gatewayListener != null) {
            gatewayListener.init();
        }
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        if (gatewayListener != null) {
            gatewayListener.destroy();
        }
    }

    protected GatewayListener createListener() {
        String zkPath = getZooKeeperPath();

        // TODO we should discover the broker group configuration here using the same
        // mq-create / mq-client profiles so that we only listen to a subset of the available brokers here?

        ServiceMap serviceMap = new ServiceMap();

        FabricGateway gatewayService = getGateway();
        Vertx vertx = gatewayService.getVertx();
        CuratorFramework curator = gatewayService.getCurator();

        List<Gateway> gateways = new ArrayList<Gateway>();
        addGateway(gateways, vertx, serviceMap, "tcp", isOpenWireEnabled(), getOpenWirePort());
        addGateway(gateways, vertx, serviceMap, "stomp", isStompEnabled(), getStompPort());
        addGateway(gateways, vertx, serviceMap, "amqp", isAmqpEnabled(), getAmqpPort());
        addGateway(gateways, vertx, serviceMap, "mqtt", isMqttEnabled(), getMqttPort());
        addGateway(gateways, vertx, serviceMap, "ws", isWebsocketEnabled(), getWebsocketPort());

        if (gateways.isEmpty()) {
            return null;
        }
        return new GatewayListener(curator, zkPath, serviceMap, gateways);
    }

    protected Gateway addGateway(List<Gateway> gateways, Vertx vertx, ServiceMap serviceMap, String protocolName, boolean enabled, int listenPort) {
        if (enabled) {
            Gateway gateway = new TcpGateway(vertx, serviceMap, listenPort, protocolName);
            if (Strings.isNotBlank(host)) {
                gateway.setHost(host);
            }
            gateways.add(gateway);
            return gateway;
        } else {
            return null;
        }
    }

    // Properties
    //-------------------------------------------------------------------------

    public FabricGateway getGateway() {
        return gateway;
    }

    public void setGateway(FabricGateway gateway) {
        this.gateway = gateway;
    }

    public void unsetGateway(FabricGateway gateway) {
        this.gateway = null;
    }

    public String getZooKeeperPath() {
        return zooKeeperPath;
    }

    public void setZooKeeperPath(String zooKeeperPath) {
        this.zooKeeperPath = zooKeeperPath;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isOpenWireEnabled() {
        return openWireEnabled;
    }

    public void setOpenWireEnabled(boolean openWireEnabled) {
        this.openWireEnabled = openWireEnabled;
    }

    public int getOpenWirePort() {
        return openWirePort;
    }

    public void setOpenWirePort(int openWirePort) {
        this.openWirePort = openWirePort;
    }

    public boolean isStompEnabled() {
        return stompEnabled;
    }

    public void setStompEnabled(boolean stompEnabled) {
        this.stompEnabled = stompEnabled;
    }

    public int getStompPort() {
        return stompPort;
    }

    public void setStompPort(int stompPort) {
        this.stompPort = stompPort;
    }

    public boolean isAmqpEnabled() {
        return amqpEnabled;
    }

    public void setAmqpEnabled(boolean amqpEnabled) {
        this.amqpEnabled = amqpEnabled;
    }

    public int getAmqpPort() {
        return amqpPort;
    }

    public void setAmqpPort(int amqpPort) {
        this.amqpPort = amqpPort;
    }

    public boolean isMqttEnabled() {
        return mqttEnabled;
    }

    public void setMqttEnabled(boolean mqttEnabled) {
        this.mqttEnabled = mqttEnabled;
    }

    public int getMqttPort() {
        return mqttPort;
    }

    public void setMqttPort(int mqttPort) {
        this.mqttPort = mqttPort;
    }

    public boolean isWebsocketEnabled() {
        return websocketEnabled;
    }

    public void setWebsocketEnabled(boolean websocketEnabled) {
        this.websocketEnabled = websocketEnabled;
    }

    public int getWebsocketPort() {
        return websocketPort;
    }

    public void setWebsocketPort(int websocketPort) {
        this.websocketPort = websocketPort;
    }
}
