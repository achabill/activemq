/**
 *
 * Copyright 2005-2006 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.transport.multicast;

import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.transport.udp.CommandChannel;
import org.apache.activemq.transport.udp.CommandDatagramChannel;
import org.apache.activemq.transport.udp.CommandDatagramSocket;
import org.apache.activemq.transport.udp.DatagramHeaderMarshaller;
import org.apache.activemq.transport.udp.DefaultBufferPool;
import org.apache.activemq.transport.udp.UdpTransport;
import org.apache.activemq.util.ServiceStopper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;

/**
 * A multicast based transport.
 * 
 * @version $Revision$
 */
public class MulticastTransport extends UdpTransport {

    private static final Log log = LogFactory.getLog(MulticastTransport.class);

    private static final int DEFAULT_IDLE_TIME = 5000;

    private MulticastSocket socket;
    private InetAddress mcastAddress;
    private int mcastPort;
    private int timeToLive = 1;
    private boolean loopBackMode = false;
    private long keepAliveInterval = DEFAULT_IDLE_TIME;

    public MulticastTransport(OpenWireFormat wireFormat, URI remoteLocation) throws UnknownHostException, IOException {
        super(wireFormat, remoteLocation);
    }

    protected String getProtocolName() {
        return "Multicast";
    }

    protected String getProtocolUriScheme() {
        return "multicast://";
    }

    protected void bind(DatagramSocket socket, SocketAddress localAddress) throws SocketException {
    }

    protected void doStop(ServiceStopper stopper) throws Exception {
        super.doStop(stopper);
        if (socket != null) {
            try {
                socket.leaveGroup(mcastAddress);
            }
            catch (IOException e) {
                stopper.onException(this, e);
            }
            socket.close();
        }
    }

    protected CommandChannel createCommandChannel() throws IOException {
        socket = new MulticastSocket(mcastPort);
        socket.setLoopbackMode(loopBackMode);
        socket.setTimeToLive(timeToLive);

        log.debug("Joining multicast address: " + mcastAddress);
        socket.joinGroup(mcastAddress);
        socket.setSoTimeout((int) keepAliveInterval);

        return new CommandDatagramSocket(this, socket, getWireFormat(), getDatagramSize(), mcastAddress, mcastPort, createDatagramHeaderMarshaller());
    }

    protected InetSocketAddress createAddress(URI remoteLocation) throws UnknownHostException, IOException {
        this.mcastAddress = InetAddress.getByName(remoteLocation.getHost());
        this.mcastPort = remoteLocation.getPort();
        return new InetSocketAddress(mcastAddress, mcastPort);
    }

    protected DatagramHeaderMarshaller createDatagramHeaderMarshaller() {
        return new MulticastDatagramHeaderMarshaller("udp://dummyHostName:" + getPort());
    }

}
