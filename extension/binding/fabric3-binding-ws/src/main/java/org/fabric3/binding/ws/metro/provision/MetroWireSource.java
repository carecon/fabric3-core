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
package org.fabric3.binding.ws.metro.provision;

import java.net.URI;
import java.util.List;

import org.fabric3.spi.model.physical.PhysicalBindingHandler;
import org.fabric3.spi.model.physical.PhysicalWireSource;

/**
 * Base class for service-side wire definitions.
 */
public abstract class MetroWireSource extends PhysicalWireSource {
    private URI serviceUri;
    private String wsdl;
    private ServiceEndpointDefinition endpointDefinition;
    private boolean bidirectional;
    private List<PhysicalBindingHandler> handlers;

    /**
     * Constructor.
     *
     * @param serviceUri         the structural service URI
     * @param endpointDefinition endpoint metadata
     * @param wsdl               the WSDL. May be null, in which case the WSDL will be introspected when the endpoint is provisioned.
     * @param bidirectional      true if the wire this definition is associated with is bidirectional, i.e. has a callback
     * @param handlers           optional binding handlers
     */
    public MetroWireSource(URI serviceUri,
                           ServiceEndpointDefinition endpointDefinition,
                           String wsdl,
                           boolean bidirectional,
                           List<PhysicalBindingHandler> handlers) {
        this.serviceUri = serviceUri;
        this.endpointDefinition = endpointDefinition;
        this.wsdl = wsdl;
        this.bidirectional = bidirectional;
        this.handlers = handlers;
    }

    /**
     * Returns the endpoint information.
     *
     * @return the endpoint information
     */
    public ServiceEndpointDefinition getEndpointDefinition() {
        return endpointDefinition;
    }

    public String getWsdl() {
        return wsdl;
    }

    /**
     * Returns the optional handlers to engage when processing an invocation.
     *
     * @return the optional handlers to engage when processing an invocation.
     */
    public List<PhysicalBindingHandler> getHandlers() {
        return handlers;
    }

    /**
     * The structural service URI.
     *
     * @return the structural service URI
     */
    public URI getServiceUri() {
        return serviceUri;
    }

    /**
     * True if this wire is bidirectional.
     *
     * @return true if this wire is bidirectional
     */
    public boolean isBidirectional() {
        return bidirectional;
    }
}
