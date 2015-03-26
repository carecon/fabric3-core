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
package org.fabric3.monitor.impl.generator;

import java.util.List;
import java.util.Map;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.monitor.impl.model.physical.PhysicalDefaultMonitorDestination;
import org.fabric3.monitor.impl.model.type.DefaultMonitorDestinationDefinition;
import org.fabric3.monitor.spi.appender.AppenderGenerator;
import org.fabric3.monitor.spi.destination.MonitorDestinationGenerator;
import org.fabric3.monitor.spi.model.physical.PhysicalAppender;
import org.fabric3.monitor.spi.model.type.AppenderDefinition;
import org.oasisopen.sca.annotation.Reference;

/**
 * Generates {@link PhysicalDefaultMonitorDestination}s.
 */
public class DefaultMonitorDestinationGenerator implements MonitorDestinationGenerator<DefaultMonitorDestinationDefinition> {
    private Map<Class<?>, AppenderGenerator<?>> appenderGenerators;

    @Reference
    public void setAppenderGenerators(Map<Class<?>, AppenderGenerator<?>> appenderGenerators) {
        this.appenderGenerators = appenderGenerators;
    }

    @SuppressWarnings("unchecked")
    public PhysicalDefaultMonitorDestination generateResource(DefaultMonitorDestinationDefinition physicalDestination) throws Fabric3Exception {
        String name = physicalDestination.getParent().getName();
        PhysicalDefaultMonitorDestination physicalDefinition = new PhysicalDefaultMonitorDestination(name);
        List<AppenderDefinition> appenderDefinitions = physicalDestination.getAppenderDefinitions();

        for (AppenderDefinition appenderDefinition : appenderDefinitions) {
            AppenderGenerator generator = getAppenderGenerator(appenderDefinition);
            PhysicalAppender physicalAppender = generator.generateResource(appenderDefinition);
            physicalDefinition.add(physicalAppender);
        }
        return physicalDefinition;
    }

    private AppenderGenerator getAppenderGenerator(AppenderDefinition definition) throws Fabric3Exception {
        AppenderGenerator generator = appenderGenerators.get(definition.getClass());
        if (generator == null) {
            throw new Fabric3Exception("Unknown appender type: " + definition.getClass());
        }
        return generator;
    }

}
