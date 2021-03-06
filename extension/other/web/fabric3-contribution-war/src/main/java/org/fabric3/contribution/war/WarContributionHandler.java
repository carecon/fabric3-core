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
package org.fabric3.contribution.war;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.api.host.stream.Source;
import org.fabric3.api.host.stream.UrlSource;
import org.fabric3.spi.contribution.ContentTypeResolver;
import org.fabric3.spi.contribution.Contribution;
import org.fabric3.spi.contribution.ContributionManifest;
import org.fabric3.spi.contribution.JavaArtifactIntrospector;
import org.fabric3.spi.contribution.Resource;
import org.fabric3.spi.contribution.archive.ArchiveContributionHandler;
import org.fabric3.spi.introspection.DefaultIntrospectionContext;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.xml.Loader;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Reference;

/**
 * Introspects a WAR contribution, delegating to ResourceProcessors for handling leaf-level children.
 */
@EagerInit
public class WarContributionHandler implements ArchiveContributionHandler {
    private static final int PREFIX = "WEB-INF/classes/".length();

    private Loader loader;
    private List<JavaArtifactIntrospector> artifactIntrospectors = Collections.emptyList();
    private ContentTypeResolver contentTypeResolver;

    public WarContributionHandler(@Reference Loader loader, @Reference ContentTypeResolver contentTypeResolver) {
        this.loader = loader;
        this.contentTypeResolver = contentTypeResolver;
    }

    @Reference
    public void setArtifactIntrospectors(List<JavaArtifactIntrospector> introspectors) {
        this.artifactIntrospectors = introspectors;
    }

    public boolean canProcess(Contribution contribution) {
        String sourceUrl = contribution.getLocation().toString();
        return sourceUrl.endsWith(".war");
    }

    public void processManifest(Contribution contribution, IntrospectionContext context) throws Fabric3Exception {
        ContributionManifest manifest;
        try {
            URL sourceUrl = contribution.getLocation();
            URL manifestUrl = new URL("jar:" + sourceUrl.toExternalForm() + "!/WEB-INF/sca-contribution.xml");
            ClassLoader cl = getClass().getClassLoader();
            URI uri = contribution.getUri();
            IntrospectionContext childContext = new DefaultIntrospectionContext(uri, cl);
            Source source = new UrlSource(manifestUrl);
            manifest = loader.load(source, ContributionManifest.class, childContext);
            if (childContext.hasErrors()) {
                context.addErrors(childContext.getErrors());
            }
            if (childContext.hasWarnings()) {
                context.addWarnings(childContext.getWarnings());
            }
            contribution.setManifest(manifest);
        } catch (Fabric3Exception e) {
            if (e.getCause() instanceof FileNotFoundException) {
                // ignore no manifest found
            } else {
                throw e;
            }
        } catch (MalformedURLException e) {
            // ignore no manifest found
        }
    }

    public void iterateArtifacts(Contribution contribution, Consumer<Resource> callback, IntrospectionContext context) throws Fabric3Exception {
        URL location = contribution.getLocation();
        ContributionManifest manifest = contribution.getManifest();
        ZipInputStream zipStream = null;
        try {
            zipStream = new ZipInputStream(location.openStream());
            while (true) {
                ZipEntry entry = zipStream.getNextEntry();
                if (entry == null) {
                    // EOF
                    break;
                }
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name.contains("WEB-INF/sca-contribution.xml")) {
                    // don't index the manifest
                    continue;
                }

                if (exclude(manifest, entry)) {
                    continue;
                }

                if (name.endsWith(".class") && name.startsWith("WEB-INF/classes/")) {
                    try {
                        URL entryUrl = new URL("jar:" + location.toExternalForm() + "!/" + name);
                        // note '/' must be used as archives always use '/' for a separator
                        name = name.substring(PREFIX, name.length() - 6).replace("/", ".");
                        Class<?> clazz = context.getClassLoader().loadClass(name);
                        Resource resource = null;
                        for (JavaArtifactIntrospector introspector : artifactIntrospectors) {
                            resource = introspector.inspect(clazz, entryUrl, contribution, context);
                            if (resource != null) {
                                break;
                            }
                        }

                        if (resource == null) {
                            continue;
                        }
                        contribution.addResource(resource);
                        callback.accept(resource);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // ignore since the class may reference another class not present in the contribution
                    }
                } else {

                    String contentType = contentTypeResolver.getContentType(name);
                    if (contentType == null) {
                        // skip entry if we don't recognize the content type
                        continue;
                    }
                    URL entryUrl = new URL("jar:" + location.toExternalForm() + "!/" + name);
                    UrlSource source = new UrlSource(entryUrl);
                    Resource resource = new Resource(contribution, source, contentType);
                    contribution.addResource(resource);

                    callback.accept(resource);
                }
            }
        } catch (IOException e) {
            throw new Fabric3Exception(e);
        } finally {
            try {
                if (zipStream != null) {
                    zipStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private boolean exclude(ContributionManifest manifest, ZipEntry entry) {
        for (Pattern pattern : manifest.getScanExcludes()) {
            if (pattern.matcher(entry.getName()).matches()) {
                return true;
            }
        }
        return false;
    }
}