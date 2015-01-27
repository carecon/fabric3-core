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
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 */
package org.fabric3.fabric.domain.generator.resource;

import java.net.URI;

import junit.framework.TestCase;
import org.easymock.EasyMock;

import org.fabric3.fabric.container.command.DisposeResourcesCommand;
import org.fabric3.fabric.domain.generator.GeneratorRegistry;
import org.fabric3.api.model.type.component.ResourceDefinition;
import org.fabric3.spi.domain.generator.resource.ResourceGenerator;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;
import org.fabric3.spi.model.instance.LogicalResource;
import org.fabric3.spi.model.instance.LogicalState;
import org.fabric3.spi.model.physical.PhysicalResourceDefinition;

/**
 *
 */
public class DisposeResourceCommandGeneratorTestCase extends TestCase {

    @SuppressWarnings({"unchecked"})
    public void testBuild() throws Exception {
        ResourceGenerator<MockDefinition> resourceGenerator = EasyMock.createMock(ResourceGenerator.class);
        EasyMock.expect(resourceGenerator.generateResource(EasyMock.isA(LogicalResource.class))).andReturn(new MockPhysicalDefinition());
        GeneratorRegistry registry = EasyMock.createMock(GeneratorRegistry.class);
        EasyMock.expect(registry.getResourceGenerator(EasyMock.eq(MockDefinition.class))).andReturn(resourceGenerator);
        EasyMock.replay(registry, resourceGenerator);

        DisposeResourceCommandGenerator generator = new DisposeResourceCommandGenerator(registry);

        LogicalCompositeComponent composite = new LogicalCompositeComponent(URI.create("component"), null, null);
        composite.setState(LogicalState.MARKED);
        LogicalResource resource = new LogicalResource(new MockDefinition(), composite);
        composite.addResource(resource);

        DisposeResourcesCommand command = generator.generate(composite);
        assertFalse(command.getDefinitions().isEmpty());

        EasyMock.verify(registry, resourceGenerator);
    }

    @SuppressWarnings({"unchecked"})
    public void testNoBuild() throws Exception {
        GeneratorRegistry registry = EasyMock.createMock(GeneratorRegistry.class);
        EasyMock.replay(registry);

        DisposeResourceCommandGenerator generator = new DisposeResourceCommandGenerator(registry);

        LogicalCompositeComponent composite = new LogicalCompositeComponent(URI.create("component"), null, null);
        LogicalResource resource = new LogicalResource(new MockDefinition(), composite);
        composite.setState(LogicalState.PROVISIONED);
        composite.addResource(resource);

        assertNull(generator.generate(composite));

        EasyMock.verify(registry);
    }


    private class MockDefinition extends ResourceDefinition {
        private static final long serialVersionUID = -4013178193696275298L;
    }

    private class MockPhysicalDefinition extends PhysicalResourceDefinition {
        private static final long serialVersionUID = -4013178193696275298L;
    }
}
