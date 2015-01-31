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
package org.fabric3.implementation.java.runtime;

import java.net.URI;

import org.fabric3.api.host.ContainerException;
import org.fabric3.api.model.type.java.Injectable;
import org.fabric3.implementation.java.provision.JavaConnectionSourceDefinition;
import org.fabric3.implementation.pojo.spi.proxy.ChannelProxyService;
import org.fabric3.spi.classloader.ClassLoaderRegistry;
import org.fabric3.spi.container.builder.component.SourceConnectionAttacher;
import org.fabric3.spi.container.channel.ChannelConnection;
import org.fabric3.spi.container.component.ComponentManager;
import org.fabric3.spi.container.objectfactory.ObjectFactory;
import org.fabric3.spi.model.physical.PhysicalConnectionTargetDefinition;
import org.fabric3.spi.util.UriHelper;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Reference;

/**
 * Attaches and detaches a {@link ChannelConnection} from a Java component producer.
 */
@EagerInit
public class JavaSourceConnectionAttacher implements SourceConnectionAttacher<JavaConnectionSourceDefinition> {
    private ComponentManager manager;
    private ChannelProxyService proxyService;
    private ClassLoaderRegistry classLoaderRegistry;

    public JavaSourceConnectionAttacher(@Reference ComponentManager manager,
                                        @Reference ChannelProxyService proxyService,
                                        @Reference ClassLoaderRegistry classLoaderRegistry) {
        this.manager = manager;
        this.proxyService = proxyService;
        this.classLoaderRegistry = classLoaderRegistry;
    }

    public void attach(JavaConnectionSourceDefinition source, PhysicalConnectionTargetDefinition target, ChannelConnection connection)
            throws ContainerException {
        URI sourceUri = source.getUri();
        URI sourceName = UriHelper.getDefragmentedName(sourceUri);
        JavaComponent component = (JavaComponent) manager.getComponent(sourceName);
        if (component == null) {
            throw new ContainerException("Source component not found: " + sourceName);
        }
        Injectable injectable = source.getInjectable();
        Class<?> type;
        try {
            type = classLoaderRegistry.loadClass(source.getClassLoaderId(), source.getInterfaceName());
        } catch (ClassNotFoundException e) {
            String name = source.getInterfaceName();
            throw new ContainerException("Unable to load interface class: " + name, e);
        }
        ObjectFactory<?> factory = proxyService.createObjectFactory(type, connection);
        component.setObjectFactory(injectable, factory);
    }

    public void detach(JavaConnectionSourceDefinition source, PhysicalConnectionTargetDefinition target) throws ContainerException {
        URI sourceName = UriHelper.getDefragmentedName(source.getUri());
        JavaComponent component = (JavaComponent) manager.getComponent(sourceName);
        Injectable injectable = source.getInjectable();
        component.removeObjectFactory(injectable);
    }

}