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
package org.fabric3.execution.runtime;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.execution.provision.ExecutorServiceWireTargetDefinition;
import org.fabric3.spi.container.builder.component.TargetWireAttacher;
import org.oasisopen.sca.annotation.Reference;

/**
 */
public class ExecutorServiceWireAttacher implements TargetWireAttacher<ExecutorServiceWireTargetDefinition> {
    private Supplier<ExecutorService> factory;

    public ExecutorServiceWireAttacher(@Reference(name = "executorService") ExecutorService executorService) {
        ExecutorServiceProxy proxy = new ExecutorServiceProxy(executorService);
        this.factory = () -> proxy;
    }

    public Supplier<ExecutorService> createSupplier(ExecutorServiceWireTargetDefinition target) throws Fabric3Exception {
        return factory;
    }

}