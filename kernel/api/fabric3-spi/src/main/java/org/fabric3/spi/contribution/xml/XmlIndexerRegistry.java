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
package org.fabric3.spi.contribution.xml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.fabric3.api.host.ContainerException;
import org.fabric3.spi.contribution.Resource;
import org.fabric3.spi.introspection.IntrospectionContext;

/**
 * A registry of XmlIndexers
 */
public interface XmlIndexerRegistry {
    /**
     * Register a XmlIndexer using the processor's QName type as the key
     *
     * @param indexer the indexer to register
     */
    void register(XmlIndexer indexer);

    /**
     * Unregister an XmlIndexer for a QName
     *
     * @param name the QName
     */
    void unregister(QName name);

    /**
     * Dispatch to an XMLIndexer based on the element type of the resource document tag.
     *
     * @param resource the resource being indexed
     * @param reader   the reader positioned on the start element of the first tag
     * @param context  the context to which validation errors and warnings are reported
     * @throws ContainerException if an error occurs during indexing
     */
    void index(Resource resource, XMLStreamReader reader, IntrospectionContext context) throws ContainerException;

}