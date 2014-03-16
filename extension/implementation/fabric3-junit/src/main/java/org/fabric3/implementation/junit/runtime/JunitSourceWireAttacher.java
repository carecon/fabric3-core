/*
* Fabric3
* Copyright (c) 2009-2013 Metaform Systems
*
* Fabric3 is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as
* published by the Free Software Foundation, either version 3 of
* the License, or (at your option) any later version, with the
* following exception:
*
* Linking this software statically or dynamically with other
* modules is making a combined work based on this software.
* Thus, the terms and conditions of the GNU General Public
* License cover the whole combination.
*
* As a special exception, the copyright holders of this software
* give you permission to link this software with independent
* modules to produce an executable, regardless of the license
* terms of these independent modules, and to copy and distribute
* the resulting executable under terms of your choice, provided
* that you also meet, for each linked independent module, the
* terms and conditions of the license of that module. An
* independent module is a module which is not derived from or
* based on this software. If you modify this software, you may
* extend this exception to your version of the software, but
* you are not obligated to do so. If you do not wish to do so,
* delete this exception statement from your version.
*
* Fabric3 is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty
* of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the
* GNU General Public License along with Fabric3.
* If not, see <http://www.gnu.org/licenses/>.
*/
package org.fabric3.implementation.junit.runtime;

import org.fabric3.spi.container.ContainerException;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Reference;

import org.fabric3.implementation.junit.common.ContextConfiguration;
import org.fabric3.implementation.junit.provision.JUnitWireSourceDefinition;
import org.fabric3.implementation.pojo.builder.PojoSourceWireAttacher;
import org.fabric3.spi.container.builder.component.SourceWireAttacher;
import org.fabric3.spi.classloader.ClassLoaderRegistry;
import org.fabric3.spi.model.physical.PhysicalWireTargetDefinition;
import org.fabric3.spi.container.objectfactory.ObjectFactory;
import org.fabric3.spi.security.AuthenticationService;
import org.fabric3.spi.transform.TransformerRegistry;
import org.fabric3.spi.container.wire.Interceptor;
import org.fabric3.spi.container.wire.InvocationChain;
import org.fabric3.spi.container.wire.Wire;
import org.fabric3.test.spi.TestWireHolder;

/**
 *
 */
@EagerInit
public class JunitSourceWireAttacher extends PojoSourceWireAttacher implements SourceWireAttacher<JUnitWireSourceDefinition> {
    private TestWireHolder holder;
    private AuthenticationService authenticationService;

    public JunitSourceWireAttacher(@Reference ClassLoaderRegistry classLoaderRegistry,
                                   @Reference TransformerRegistry transformerRegistry,
                                   @Reference TestWireHolder holder) {
        super(transformerRegistry, classLoaderRegistry);
        this.holder = holder;
    }

    @Reference(required = false)
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void attach(JUnitWireSourceDefinition source, PhysicalWireTargetDefinition target, Wire wire) throws ContainerException {
        String testName = source.getTestName();
        ContextConfiguration configuration = source.getConfiguration();
        if (configuration != null) {
            if (authenticationService == null) {
                throw new ContainerException("Security information set for the test but a security extension has not been installed in the runtime");
            }
            // configuration an authentication interceptor to set the subject on the work context
            for (InvocationChain chain : wire.getInvocationChains()) {
                Interceptor next = chain.getHeadInterceptor();
                String username = configuration.getUsername();
                String password = configuration.getPassword();
                AuthenticatingInterceptor interceptor = new AuthenticatingInterceptor(username, password, authenticationService, next);
                chain.addInterceptor(0, interceptor);
            }
        }
        holder.add(testName, wire);
    }

    public void attachObjectFactory(JUnitWireSourceDefinition source, ObjectFactory<?> objectFactory, PhysicalWireTargetDefinition target)
            throws ContainerException {
        throw new UnsupportedOperationException();
    }

    public void detach(JUnitWireSourceDefinition source, PhysicalWireTargetDefinition target) throws ContainerException {
    }

    public void detachObjectFactory(JUnitWireSourceDefinition source, PhysicalWireTargetDefinition target) throws ContainerException {
    }


}
