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
package org.fabric3.cache.model;

import java.util.ArrayList;
import java.util.List;

import org.fabric3.api.model.type.component.Resource;
import org.fabric3.cache.spi.CacheResource;

/**
 * A set of cache configurations defined in a composite.
 */
public class CacheSetResource extends Resource {
    private List<CacheResource> configurations = new ArrayList<>();

    public void addDefinition(CacheResource configuration) {
        configurations.add(configuration);
    }

    public List<CacheResource> getDefinitions() {
        return configurations;
    }

}
