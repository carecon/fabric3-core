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
package org.fabric3.fabric.federation.addressing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import org.fabric3.api.annotation.monitor.Monitor;
import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.api.host.runtime.HostInfo;
import org.fabric3.api.model.type.RuntimeMode;
import org.fabric3.spi.federation.addressing.AddressAnnouncement;
import org.fabric3.spi.federation.addressing.AddressCache;
import org.fabric3.spi.federation.addressing.AddressEvent;
import org.fabric3.spi.federation.addressing.AddressListener;
import org.fabric3.spi.federation.addressing.AddressMonitor;
import org.fabric3.spi.federation.addressing.AddressRequest;
import org.fabric3.spi.federation.addressing.AddressUpdate;
import org.fabric3.spi.federation.addressing.SocketAddress;
import org.fabric3.spi.federation.topology.MessageReceiver;
import org.fabric3.spi.federation.topology.NodeTopologyService;
import org.fabric3.spi.federation.topology.TopologyListener;
import org.fabric3.spi.runtime.event.EventService;
import org.fabric3.spi.runtime.event.Fabric3EventListener;
import org.fabric3.spi.runtime.event.JoinDomainCompleted;
import org.oasisopen.sca.annotation.Destroy;
import org.oasisopen.sca.annotation.Init;
import org.oasisopen.sca.annotation.Reference;
import org.oasisopen.sca.annotation.Service;

/**
 *
 */
@Service(AddressCache.class)
public class AddressCacheImpl implements AddressCache, TopologyListener, MessageReceiver, Fabric3EventListener<JoinDomainCompleted> {
    private static final String ADDRESS_CHANNEL = "F3AddressChannel";

    private NodeTopologyService topologyService;

    private Executor executor;
    private EventService eventService;
    private HostInfo info;
    private AddressMonitor monitor;

    private String qualifiedChannelName;

    protected Map<String, List<SocketAddress>> addresses = new ConcurrentHashMap<>();
    protected Map<String, List<AddressListener>> listeners = new ConcurrentHashMap<>();

    public AddressCacheImpl(@Reference Executor executor, @Reference EventService eventService, @Reference HostInfo info, @Monitor AddressMonitor monitor) {
        this.executor = executor;
        this.eventService = eventService;
        this.info = info;
        this.monitor = monitor;
        this.qualifiedChannelName = ADDRESS_CHANNEL + "." + info.getDomain().getAuthority();
    }

    @Reference(required = false)
    public void setTopologyService(NodeTopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @Init
    public void init() {
        eventService.subscribe(JoinDomainCompleted.class, this);
        if (isNode()) {
            topologyService.register(this);
        }
    }

    @Destroy
    public void destroy() throws Fabric3Exception {
        if (isNode()) {
            topologyService.closeChannel(qualifiedChannelName);
            topologyService.deregister(this);
        }
    }

    public List<SocketAddress> getActiveAddresses(String endpointId) {
        List<SocketAddress> list = addresses.get(endpointId);
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    public void publish(AddressEvent event) {
        publish(event, true);
    }

    public void subscribe(String endpointId, AddressListener listener) {
        List<AddressListener> list = listeners.get(endpointId);
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            this.listeners.put(endpointId, list);
        }
        list.add(listener);
    }

    public void unsubscribe(String endpointId, String listenerId) {
        List<AddressListener> list = listeners.get(endpointId);
        if (list == null) {
            return;
        }
        List<AddressListener> deleted = new ArrayList<>();
        for (AddressListener listener : list) {
            if (listenerId.equals(listener.getId())) {
                deleted.add(listener);
                if (list.isEmpty()) {
                    listeners.remove(endpointId);
                }
                break;
            }
        }
        for (AddressListener listener : deleted) {
            list.remove(listener);
        }
    }

    protected void notifyChange(String endpointId) {
        List<SocketAddress> addresses = this.addresses.get(endpointId);
        if (addresses == null) {
            addresses = Collections.emptyList();
        }
        List<AddressListener> list = listeners.get(endpointId);
        if (list == null) {
            return;
        }
        for (AddressListener listener : list) {
            listener.onUpdate(addresses);
        }
    }

    public void onMessage(Object object) {
        if (object instanceof AddressAnnouncement) {
            AddressAnnouncement announcement = (AddressAnnouncement) object;
            publish(announcement, false);
        } else if (object instanceof AddressUpdate) {
            AddressUpdate update = (AddressUpdate) object;
            for (AddressAnnouncement announcement : update.getAnnouncements()) {
                publish(announcement, false);
            }
        } else if (object instanceof AddressRequest) {
            handleAddressRequest((AddressRequest) object);
        }
    }

    public void onLeave(String name) {
        for (Map.Entry<String, List<SocketAddress>> entry : addresses.entrySet()) {
            List<SocketAddress> toDelete = new ArrayList<>();
            List<SocketAddress> list = entry.getValue();
            for (SocketAddress address : list) {
                if (name.equals(address.getRuntimeName())) {
                    toDelete.add(address);
                }
            }
            for (SocketAddress address : toDelete) {
                monitor.removed(name, address.toString());
                list.remove(address);
            }
            if (list.isEmpty()) {
                addresses.remove(entry.getKey());
            }
            notifyChange(entry.getKey());
        }
    }

    /**
     * Broadcasts address requests after the runtime has joined the domain to synchronize the cache.
     *
     * @param event the event the event signalling the runtime has joined the domain
     */
    public void onEvent(JoinDomainCompleted event) {
        try {
            if (isNode()) {
                topologyService.openChannel(qualifiedChannelName, null, this);
                AddressRequest request = new AddressRequest(info.getRuntimeName());
                topologyService.sendAsynchronous(qualifiedChannelName, request);
            }
        } catch (Fabric3Exception e) {
            monitor.error(e);
        }
    }

    private void publish(AddressEvent event, boolean propagate) {
        if (event instanceof AddressAnnouncement) {
            AddressAnnouncement announcement = (AddressAnnouncement) event;
            String endpointId = announcement.getEndpointId();
            List<SocketAddress> addresses = this.addresses.get(endpointId);
            SocketAddress address = announcement.getAddress();
            if (AddressAnnouncement.Type.ACTIVATED == announcement.getType()) {
                // add the new address
                if (addresses == null) {
                    addresses = new CopyOnWriteArrayList<>();
                    this.addresses.put(endpointId, addresses);
                }
                monitor.added(endpointId, address.toString());
                addresses.add(address);
            } else {
                // remove the address
                if (addresses != null) {
                    monitor.removed(endpointId, address.toString());
                    addresses.remove(address);
                    if (addresses.isEmpty()) {
                        this.addresses.remove(endpointId);
                    }
                }
            }

            if (propagate && isNode() && event instanceof AddressAnnouncement) {
                try {
                    topologyService.sendAsynchronous(qualifiedChannelName, event);
                } catch (Fabric3Exception e) {
                    monitor.error(e);
                }
            }
            // ignore on controller

            // notify listeners of a change
            notifyChange(endpointId);
        }
    }

    private void handleAddressRequest(final AddressRequest request) {
        monitor.receivedRequest(request.getRuntimeName());
        final AddressUpdate update = new AddressUpdate();
        for (Map.Entry<String, List<SocketAddress>> entry : addresses.entrySet()) {
            for (SocketAddress address : entry.getValue()) {
                if (info.getRuntimeName().equals(address.getRuntimeName())) {
                    AddressAnnouncement announcement = new AddressAnnouncement(entry.getKey(), AddressAnnouncement.Type.ACTIVATED, address);
                    update.addAnnouncement(announcement);
                }
            }
        }
        if (!update.getAnnouncements().isEmpty()) {
            // send response from a separate thread to avoid blocking on the federation callback
            executor.execute(() -> {
                try {
                    if (isNode()) {
                        topologyService.sendAsynchronous(request.getRuntimeName(), qualifiedChannelName, update);
                    }
                    // ignore on controller
                } catch (Fabric3Exception e) {
                    monitor.error(e);
                }
            });
        }
    }

    private boolean isNode() {
        return RuntimeMode.NODE == info.getRuntimeMode() && topologyService != null;
    }

}
