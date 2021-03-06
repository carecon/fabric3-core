/*
 * Fabric3
 * Copyright (c) 2009-2015 Metaform Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 */
package org.fabric3.implementation.bytecode.proxy.channel;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.fabric3.spi.container.channel.ChannelConnection;
import org.fabric3.spi.container.channel.EventStream;
import org.fabric3.spi.container.channel.EventStreamHandler;

/**
 *
 */
public class ChannelProxySupplierTestCase extends TestCase {

    public void testDispatch() throws Exception {
        EventStreamHandler handler = EasyMock.createMock(EventStreamHandler.class);
        handler.handle(EasyMock.isA(String.class), EasyMock.anyBoolean());

        EventStream stream = EasyMock.createMock(EventStream.class);
        EasyMock.expect(stream.getHeadHandler()).andReturn(handler);

        ChannelConnection connection = EasyMock.createMock(ChannelConnection.class);
        EasyMock.expect(connection.getEventStream()).andReturn(stream);
        EasyMock.expect(connection.getCloseable()).andReturn(null);

        EasyMock.replay(connection, stream, handler);

        ChannelProxyDispatcher dispatcher = new ChannelProxyDispatcher();
        dispatcher.init(connection);

        dispatcher._f3_invoke(0, "test");

        EasyMock.verify(stream, handler);
    }

}
