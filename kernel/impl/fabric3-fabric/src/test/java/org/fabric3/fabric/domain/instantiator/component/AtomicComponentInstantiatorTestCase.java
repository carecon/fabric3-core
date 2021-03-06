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
package org.fabric3.fabric.domain.instantiator.component;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.fabric3.api.model.type.component.Binding;
import org.fabric3.api.model.type.component.Component;
import org.fabric3.api.model.type.component.ComponentType;
import org.fabric3.api.model.type.component.Consumer;
import org.fabric3.api.model.type.component.Implementation;
import org.fabric3.api.model.type.component.Multiplicity;
import org.fabric3.api.model.type.component.Producer;
import org.fabric3.api.model.type.component.Property;
import org.fabric3.api.model.type.component.PropertyMany;
import org.fabric3.api.model.type.component.PropertyValue;
import org.fabric3.api.model.type.component.Reference;
import org.fabric3.api.model.type.component.ResourceReference;
import org.fabric3.api.model.type.component.Service;
import org.fabric3.api.model.type.component.Target;
import org.fabric3.fabric.domain.instantiator.InstantiationContext;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;
import org.fabric3.spi.model.instance.LogicalConsumer;
import org.fabric3.spi.model.instance.LogicalProducer;
import org.fabric3.spi.model.instance.LogicalProperty;
import org.fabric3.spi.model.instance.LogicalReference;
import org.fabric3.spi.model.instance.LogicalResourceReference;
import org.fabric3.spi.model.instance.LogicalService;
import org.fabric3.spi.model.type.component.CompositeImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 */
public class AtomicComponentInstantiatorTestCase extends TestCase {
    private static final URI PRODUCER_TARGET = URI.create("producerTarget");
    private static final URI CONSUMER_SOURCE = URI.create("consumerTarget");
    private static final String REFERENCE_TARGET = "referenceTarget";

    private AtomicComponentInstantiatorImpl instantiator;
    private LogicalCompositeComponent parent;
    private InstantiationContext context;
    private Component<MockImplementation> component;

    public void testInstantiate() throws Exception {
        LogicalComponent logicalComponent = instantiator.instantiate(component, parent, context);
        assertEquals("parent/component", logicalComponent.getUri().toString());

        LogicalService logicalService = logicalComponent.getService("service");
        assertEquals("parent/component#service", logicalService.getUri().toString());
        assertFalse(logicalService.getBindings().isEmpty());
        assertFalse(logicalService.getCallbackBindings().isEmpty());

        LogicalReference logicalReference = logicalComponent.getReference("reference");
        assertEquals("parent/component#reference", logicalReference.getUri().toString());
        assertFalse(logicalReference.getBindings().isEmpty());
        assertFalse(logicalReference.getCallbackBindings().isEmpty());

        LogicalConsumer logicalConsumer = logicalComponent.getConsumer("consumer");
        assertEquals("parent/component#consumer", logicalConsumer.getUri().toString());

        LogicalProducer logicalProducer = logicalComponent.getProducer("producer");
        assertEquals("parent/component#producer", logicalProducer.getUri().toString());

        LogicalResourceReference logicalResourceReference = logicalComponent.getResourceReference("resource");
        assertEquals("parent/component#resource", logicalResourceReference.getUri().toString());
    }

    public void testDuplicateComponent() throws Exception {
        parent.addComponent(new LogicalComponent<MockImplementation>(URI.create("parent/component"), null, parent));
        instantiator.instantiate(component, parent, context);
        assertTrue(context.hasErrors());
        assertTrue(context.getErrors().get(0) instanceof DuplicateComponent);
    }

    public void testPropertiesDefaultValue() {
        Property definition = new Property("property");
        Document document = EasyMock.createMock(Document.class);
        definition.setDefaultValue(document);
        component.getComponentType().add(definition);

        LogicalComponent logicalComponent = instantiator.instantiate(component, parent, context);
        LogicalProperty logicalProperty = logicalComponent.getProperties("property");
        assertNotNull(logicalProperty.getXmlValue());
    }

    public void testPropertyValueOnComponentDefinition() {
        Property definition = new Property("property");
        component.getComponentType().add(definition);

        Document document = EasyMock.createMock(Document.class);
        PropertyValue value = new PropertyValue("property", document, PropertyMany.SINGLE);
        component.add(value);
        LogicalComponent logicalComponent = instantiator.instantiate(component, parent, context);
        LogicalProperty logicalProperty = logicalComponent.getProperties("property");
        assertNotNull(logicalProperty.getXmlValue());
    }

    public void testPropertyValueXPathSource() throws Exception {
        Property definition = new Property("property");
        component.getComponentType().add(definition);

        PropertyValue value = new PropertyValue("property", "$theProp");
        NamespaceContext namespaceContext = EasyMock.createMock(NamespaceContext.class);
        value.setNamespaceContext(namespaceContext);
        component.add(value);

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("values");
        document.appendChild(root);
        Element valElement = document.createElement("value");
        valElement.setTextContent("test");
        root.appendChild(valElement);
        parent.setProperties(LogicalProperty.Builder.newBuilder("theProp", parent).xmlValue(document).many(false).build());

        LogicalComponent logicalComponent = instantiator.instantiate(component, parent, context);

        LogicalProperty logicalProperty = logicalComponent.getProperties("property");
        Element rootReturned = logicalProperty.getXmlValue().getDocumentElement();
        assertEquals("values", rootReturned.getNodeName());
        assertEquals("value", rootReturned.getChildNodes().item(0).getNodeName());
    }

    public void testPropertyValueFileSource() throws Exception {
        createFileSourceProperty("property.xml");

        parent.setProperties(LogicalProperty.Builder.newBuilder("theProp", parent).many(false).build());
        LogicalComponent logicalComponent = instantiator.instantiate(component, parent, context);
        LogicalProperty logicalProperty = logicalComponent.getProperties("property");
        Element rootReturned = logicalProperty.getXmlValue().getDocumentElement();
        assertEquals("values", rootReturned.getNodeName());
        assertEquals("value", rootReturned.getChildNodes().item(0).getNodeName());
    }

    public void testPropertyValueNoValuesElementFileSource() throws Exception {
        createFileSourceProperty("propertyNoValues.xml");

        parent.setProperties(LogicalProperty.Builder.newBuilder("theProp", parent).many(false).build());
        LogicalComponent logicalComponent = instantiator.instantiate(component, parent, context);
        LogicalProperty logicalProperty = logicalComponent.getProperties("property");
        Element rootReturned = logicalProperty.getXmlValue().getDocumentElement();
        assertEquals("values", rootReturned.getNodeName());
        assertEquals("value", rootReturned.getChildNodes().item(0).getNodeName());
    }

    public void testPropertyValueNoRootElementFileSource() throws Exception {
        createFileSourceProperty("propertyNoRoot.xml");

        parent.setProperties(LogicalProperty.Builder.newBuilder("theProp", parent).many(false).build());
        LogicalComponent logicalComponent = instantiator.instantiate(component, parent, context);
        LogicalProperty logicalProperty = logicalComponent.getProperties("property");
        Element rootReturned = logicalProperty.getXmlValue().getDocumentElement();
        assertEquals("values", rootReturned.getNodeName());
        assertEquals("value", rootReturned.getChildNodes().item(0).getNodeName());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ComponentType type = new ComponentType();
        MockImplementation implementation = new MockImplementation();
        implementation.setComponentType(type);
        component = new Component<>("component");
        component.setImplementation(implementation);

        createService(component);
        createReference(component);
        createProducer(component);
        createConsumer(component);
        createResource(component);

        URI parentUri = URI.create("parent");
        Component<CompositeImplementation> definition = new Component<>("parent");
        parent = new LogicalCompositeComponent(parentUri, definition, null);

        context = new InstantiationContext();
        instantiator = new AtomicComponentInstantiatorImpl();

    }

    private void createConsumer(Component component) {
        Consumer<ComponentType> definition = new Consumer<>("consumer");
        component.getComponentType().add(definition);

        Consumer<Component> consumer = new Consumer<>("consumer");
        consumer.setSources(Collections.singletonList(CONSUMER_SOURCE));
        component.add(consumer);
    }

    private void createProducer(Component component) {
        Producer<ComponentType> definition = new Producer<>("producer");
        component.getComponentType().add(definition);

        Producer<Component> producer = new Producer<>("producer");
        producer.setTargets( Collections.singletonList(PRODUCER_TARGET));
        component.add(producer);
    }

    private void createReference(Component component) {
        Reference<ComponentType> definition = new Reference<>("reference", Multiplicity.ONE_ONE);
        component.getComponentType().add(definition);

        Reference<Component> reference = new Reference<>("reference", Multiplicity.ONE_ONE);
        Target target = new Target(REFERENCE_TARGET);
        reference.addTarget(target);
        reference.addBinding(new MockBinding());
        reference.addCallbackBinding(new MockBinding());
        component.add(reference);
    }

    private void createService(Component component) {
        Service<ComponentType> definition = new Service<>("service");
        component.getComponentType().add(definition);

        Service<Component> service = new Service<>("service");
        service.addBinding(new MockBinding());
        service.addCallbackBinding(new MockBinding());
        component.add(service);
    }

    private void createResource(Component component) {
        ResourceReference definition = new ResourceReference("resource", null, false);
        component.getComponentType().add(definition);
    }

    private void createFileSourceProperty(String fileName) throws URISyntaxException {
        Property definition = new Property("property");
        component.getComponentType().add(definition);
        String name = getClass().getPackage().getName().replace(".", "/") + "/" + fileName;
        URI fileSource = getClass().getClassLoader().getResource(name).toURI();
        PropertyValue value = new PropertyValue("property", fileSource);
        NamespaceContext namespaceContext = EasyMock.createMock(NamespaceContext.class);
        value.setNamespaceContext(namespaceContext);
        component.add(value);
    }


    private class MockBinding extends Binding {
        public MockBinding() {
            super(null, null);
        }
    }

    private class MockImplementation extends Implementation<ComponentType> {
        public String getType() {
            return null;
        }
    }

}
