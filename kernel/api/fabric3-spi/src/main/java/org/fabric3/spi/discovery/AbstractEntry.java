package org.fabric3.spi.discovery;

/**
 * Base discovery entry.
 */
public abstract class AbstractEntry {
    protected String name;
    protected String transport;

    protected String address;
    protected int port;

    private boolean frozen;

    /**
     * Returns the entry name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the entry name.
     *
     * @param name the name
     */
    public void setName(String name) {
        check();
        this.name = name;
    }

    /**
     * Returns the binding type.
     *
     * @return the binding type
     */
    public String getTransport() {
        return transport;
    }

    /**
     * Sets the binding type.
     *
     * @param transport the binding type
     */
    public void setTransport(String transport) {
        check();
        this.transport = transport;
    }

    /**
     * Returns the host address.
     *
     * @return the host address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the host address
     *
     * @param address the host address
     */
    public void setAddress(String address) {
        check();
        this.address = address;
    }

    /**
     * Returns the port number.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port number.
     *
     * @param port the port number
     */
    public void setPort(int port) {
        check();
        this.port = port;
    }

    public void freeze() {
        frozen = true;
    }

    protected void check() {
        if (frozen) {
            throw new IllegalStateException("Entry cannot be modified");
        }
    }
}
