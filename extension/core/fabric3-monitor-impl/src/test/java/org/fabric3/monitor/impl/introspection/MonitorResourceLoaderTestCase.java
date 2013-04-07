/*
 * Fabric3
 * Copyright (c) 2009-2013 Metaform Systems
 *
 * Fabric3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version, with the
 * following exception:
 *
 * Linking this software statically or dynamically with other
 * modules is making a combined work based on this software.
 * Thus, the terms and conditions of the GNU General Public
 * License cover the whole combination.
 *
 * As a special exception, the copyright holders of this software
 * give you permission to link this software with independent
 * modules to produce an executable, regardless of the license
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, the
 * terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or
 * based on this software. If you modify this software, you may
 * extend this exception to your version of the software, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 *
 * Fabric3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the
 * GNU General Public License along with Fabric3.
 * If not, see <http://www.gnu.org/licenses/>.
*/
package org.fabric3.monitor.impl.introspection;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.fabric3.model.type.ModelObject;
import org.fabric3.monitor.impl.model.MonitorResourceDefinition;
import org.fabric3.monitor.spi.appender.AppenderDefinition;
import org.fabric3.spi.introspection.DefaultIntrospectionContext;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.xml.LoaderRegistry;

/**
 *
 */
public class MonitorResourceLoaderTestCase extends TestCase {
    private static final String XML = "<monitor name='test'><appenders><appender.console/></appenders></monitor>";
    private static final String XML_NO_NAME = "<monitor></monitor>";
    private static final String XML_MULTIPLE_TYPES = "<monitor name='test'><appenders><appender.console/><appender.console/></appenders></monitor>";

    private LoaderRegistry loaderRegistry;
    private MonitorResourceLoader loader;

    public void testLoad() throws Exception {
        XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(new ByteArrayInputStream(XML.getBytes()));
        IntrospectionContext context = new DefaultIntrospectionContext();

        loaderRegistry.load(reader, ModelObject.class, context);
        EasyMock.expectLastCall().andReturn(new AppenderDefinition("test"));

        EasyMock.replay(loaderRegistry);
        reader.nextTag();

        MonitorResourceDefinition definition = loader.load(reader, context);

        assertFalse(context.hasErrors());
        assertFalse(definition.getAppenderDefinitions().isEmpty());

        EasyMock.verify(loaderRegistry);
    }

    public void testNoNameLoad() throws Exception {
        XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(new ByteArrayInputStream(XML_NO_NAME.getBytes()));
        IntrospectionContext context = new DefaultIntrospectionContext();

        EasyMock.replay(loaderRegistry);
        reader.nextTag();

        loader.load(reader, context);

        assertTrue(context.hasErrors());

        EasyMock.verify(loaderRegistry);
    }

    public void testMultipleTypesErrorLoad() throws Exception {
        XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(new ByteArrayInputStream(XML_MULTIPLE_TYPES.getBytes()));
        IntrospectionContext context = new DefaultIntrospectionContext();

        loaderRegistry.load(reader, ModelObject.class, context);
        EasyMock.expectLastCall().andReturn(new AppenderDefinition("test")).times(2);

        EasyMock.replay(loaderRegistry);
        reader.nextTag();

        loader.load(reader, context);

        assertTrue(context.hasErrors());

        EasyMock.verify(loaderRegistry);
    }

    public void setUp() throws Exception {
        super.setUp();
        loaderRegistry = EasyMock.createMock(LoaderRegistry.class);

        loader = new MonitorResourceLoader(loaderRegistry);
    }

}
