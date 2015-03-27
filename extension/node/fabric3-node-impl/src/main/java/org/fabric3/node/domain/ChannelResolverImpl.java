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
 */
package org.fabric3.node.domain;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.api.host.HostNamespaces;
import org.fabric3.api.model.type.component.Component;
import org.fabric3.api.model.type.component.Consumer;
import org.fabric3.api.model.type.component.Producer;
import org.fabric3.api.model.type.contract.DataType;
import org.fabric3.api.model.type.java.InjectingComponentType;
import org.fabric3.api.node.NotFoundException;
import org.fabric3.node.nonmanaged.NonManagedConnectionSource;
import org.fabric3.node.nonmanaged.NonManagedConnectionTarget;
import org.fabric3.node.nonmanaged.NonManagedImplementation;
import org.fabric3.spi.container.builder.ChannelConnector;
import org.fabric3.spi.container.builder.channel.ChannelBuilderRegistry;
import org.fabric3.spi.container.channel.ChannelResolver;
import org.fabric3.spi.domain.LogicalComponentManager;
import org.fabric3.spi.domain.generator.channel.ChannelGenerator;
import org.fabric3.spi.domain.generator.channel.ConnectionGenerator;
import org.fabric3.spi.model.instance.LogicalChannel;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;
import org.fabric3.spi.model.instance.LogicalConsumer;
import org.fabric3.spi.model.instance.LogicalProducer;
import org.fabric3.spi.model.physical.DeliveryType;
import org.fabric3.spi.model.physical.PhysicalChannel;
import org.fabric3.spi.model.physical.PhysicalChannelConnection;
import org.fabric3.spi.model.type.java.JavaServiceContract;
import org.fabric3.spi.model.type.java.JavaType;
import org.oasisopen.sca.annotation.Reference;
import static org.fabric3.spi.domain.generator.channel.ChannelDirection.CONSUMER;
import static org.fabric3.spi.domain.generator.channel.ChannelDirection.PRODUCER;

/**
 *
 */
public class ChannelResolverImpl implements ChannelResolver {
    private static final QName SYNTHETIC_DEPLOYABLE = new QName(HostNamespaces.SYNTHESIZED, "SyntheticDeployable");
    private static final List<DataType> OBJECTS = Collections.singletonList(new JavaType(Object.class));

    private Introspector introspector;
    private LogicalComponentManager lcm;
    private ChannelGenerator channelGenerator;
    private ConnectionGenerator connectionGenerator;
    private ChannelBuilderRegistry channelBuilderRegistry;
    private ChannelConnector channelConnector;

    public ChannelResolverImpl(@Reference Introspector introspector,
                               @Reference(name = "lcm") LogicalComponentManager lcm,
                               @Reference ChannelGenerator channelGenerator,
                               @Reference ConnectionGenerator connectionGenerator,
                               @Reference ChannelBuilderRegistry channelBuilderRegistry,
                               @Reference ChannelConnector channelConnector) {
        this.introspector = introspector;
        this.lcm = lcm;
        this.channelGenerator = channelGenerator;
        this.connectionGenerator = connectionGenerator;
        this.channelBuilderRegistry = channelBuilderRegistry;
        this.channelConnector = channelConnector;
    }

    public <T> T getProducer(Class<T> interfaze, String name) throws Fabric3Exception {
        return getProducer(interfaze, name, null);
    }

    public <T> T getProducer(Class<T> interfaze, String name, String topic) {
        LogicalChannel logicalChannel = getChannel(name);
        LogicalProducer producer = createProducer(interfaze, logicalChannel.getUri());
        PhysicalChannel physicalChannel = channelGenerator.generate(logicalChannel, SYNTHETIC_DEPLOYABLE, PRODUCER);

        channelBuilderRegistry.build(physicalChannel);

        Map<LogicalChannel, DeliveryType> channels = Collections.singletonMap(logicalChannel, DeliveryType.DEFAULT);
        List<PhysicalChannelConnection> connections = connectionGenerator.generateProducer(producer, channels);
        PhysicalChannelConnection connection = connections.get(0);  // safe as there is only one connection
        connection.setTopic(topic);
        channelConnector.connect(connection);

        NonManagedConnectionSource source = (NonManagedConnectionSource) connection.getSource();
        return interfaze.cast(source.getProxy());
    }

    public <T> T getConsumer(Class<T> interfaze, String name) {
        return getConsumer(interfaze, name, null);
    }

    public <T> T getConsumer(Class<T> interfaze, String name, String topic) {
        LogicalChannel logicalChannel = getChannel(name);
        LogicalConsumer consumer = createConsumer(interfaze, logicalChannel.getUri());
        PhysicalChannel physicalChannel = channelGenerator.generate(logicalChannel, SYNTHETIC_DEPLOYABLE, CONSUMER);

        channelBuilderRegistry.build(physicalChannel);

        Map<LogicalChannel, DeliveryType> channels = Collections.singletonMap(logicalChannel, DeliveryType.DEFAULT);
        List<PhysicalChannelConnection> connections = connectionGenerator.generateConsumer(consumer, channels);

        PhysicalChannelConnection connection = connections.get(0);  // safe as there is only one connection
        connection.setTopic(topic);
        channelConnector.connect(connection);

        NonManagedConnectionTarget target = (NonManagedConnectionTarget) connection.getTarget();
        return interfaze.cast(target.getProxy());
    }

    private <T> LogicalConsumer createConsumer(Class<T> interfaze, URI channelUri) {
        JavaServiceContract contract = introspector.introspect(interfaze);
        LogicalCompositeComponent domain = lcm.getRootComponent();
        String root = domain.getUri().toString();

        LogicalComponent<NonManagedImplementation> logicalComponent = createComponent(interfaze, domain, root);

        Consumer consumer = new Consumer("consumer", OBJECTS, true);

        LogicalConsumer logicalConsumer = new LogicalConsumer(URI.create(root + "/F3Synthetic#consumer"), consumer, logicalComponent);
        logicalConsumer.setServiceContract(contract);
        logicalConsumer.addSource(channelUri);
        return logicalConsumer;
    }

    private LogicalChannel getChannel(String name) throws Fabric3Exception {
        LogicalCompositeComponent domainComponent = lcm.getRootComponent();
        String domainRoot = domainComponent.getUri().toString();
        URI channelUri = URI.create(domainRoot + "/" + name);
        LogicalChannel logicalChannel = domainComponent.getChannel(channelUri);
        if (logicalChannel == null) {
            throw new NotFoundException("Channel not found: " + name);
        }
        return logicalChannel;
    }

    private <T> LogicalProducer createProducer(Class<T> interfaze, URI channelUri) throws Fabric3Exception {
        JavaServiceContract contract = introspector.introspect(interfaze);

        LogicalCompositeComponent domain = lcm.getRootComponent();
        String root = domain.getUri().toString();

        LogicalComponent<NonManagedImplementation> logicalComponent = createComponent(interfaze, domain, root);

        Producer producer = new Producer("producer", contract);

        LogicalProducer logicalProducer = new LogicalProducer(URI.create(root + "/F3Synthetic#producer"), producer, logicalComponent);
        logicalProducer.setServiceContract(contract);
        logicalProducer.addTarget(channelUri);
        return logicalProducer;
    }

    private <T> LogicalComponent<NonManagedImplementation> createComponent(Class<T> interfaze, LogicalCompositeComponent domain, String root) {
        URI componentUri = URI.create(root + "/F3Synthetic");

        InjectingComponentType componentType = new InjectingComponentType();
        NonManagedImplementation implementation = new NonManagedImplementation();
        implementation.setComponentType(componentType);
        Component<NonManagedImplementation> component = new Component<>("F3Synthetic");
        component.setContributionUri(ContributionResolver.getContribution(interfaze));
        component.setImplementation(implementation);
        return new LogicalComponent<>(componentUri, component, domain);
    }

}
