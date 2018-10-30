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
package org.fabric3.api.host;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * A version identifier. Version identifiers have four components:
 *
 * - Major version. A non-negative integer.
 *
 * - Minor version. A non-negative integer.
 *
 * - Micro version. A non-negative integer.
 *
 * - Qualifier. A text string.
 *
 * This implementation is based on org.osgi.framework.Version from the OSGi Alliance issued under the Apache 2.0 License.
 */
public class Version implements Comparable, Serializable {
    private static final long serialVersionUID = -2755678770473603563L;
    private static final String SEPARATOR = ".";
    public static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    private final int major;
    private final int minor;
    private final int micro;
    private final String qualifier;
    private final boolean snapshot;

    /**
     * The empty version "0.0.0". Equivalent to calling <code>new Version(0,0,0)</code>.
     */
    public static final Version emptyVersion = new Version(0, 0, 0);

    /**
     * Creates a version identifier from the specified numerical components.
     *
     *
     * The qualifier is set to the empty string.
     *
     * @param major Major component of the version identifier.
     * @param minor Minor component of the version identifier.
     * @param micro Micro component of the version identifier.
     * @throws IllegalArgumentException If the numerical components are negative.
     */
    public Version(int major, int minor, int micro) {
        this(major, minor, micro, null, false);
    }


    /**
     * Creates a version identifier from the specified components.
     *
     * @param major Major component of the version identifier.
     * @param minor Minor component of the version identifier.
     * @param micro Micro component of the version identifier.
     * @param qualifier Qualifier component of the version identifier. If <code>null</code> is specified, then the qualifier will be set to the empty string.
     * @throws IllegalArgumentException If the numerical components are negative.
     */
    public Version(int major, int minor, int micro, String qualifier) {
        this(major, minor, micro, qualifier, false);
    }

    /**
     * Creates a version identifier from the specified components.
     *
     * @param major     Major component of the version identifier.
     * @param minor     Minor component of the version identifier.
     * @param micro     Micro component of the version identifier.
     * @param qualifier Qualifier component of the version identifier. If <code>null</code> is specified, then the qualifier will be set to the empty string.
     * @param snapshot  Indicates snapshot state of the version.
     * @throws IllegalArgumentException If the numerical components are negative or the qualifier string is invalid.
     */
    public Version(int major, int minor, int micro, String qualifier, boolean snapshot) {
        if (qualifier == null) {
            qualifier = "";
        }

        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier;
        this.snapshot = snapshot;
        validate();
    }

    /**
     * Created a version identifier from the specified string.
     *
     *
     * Here is the grammar for version strings.
     *
     * <pre>
     * version ::= major('.'minor('.'micro('.'qualifier)?)?)?
     * major ::= digit+
     * minor ::= digit+
     * micro ::= digit+
     * qualifier ::= (alpha|digit|'_'|'-')+
     * digit ::= [0..9]
     * alpha ::= [a..zA..Z]
     * </pre>
     *
     * There must be no whitespace in version.
     *
     * @param version String representation of the version identifier.
     * @throws IllegalArgumentException If <code>version</code> is improperly formatted.
     */
    public Version(String version) {
        int major;
        int minor = 0;
        int micro = 0;
        String qualifier = "";
        boolean snapshot = false;

        if (version.endsWith(SNAPSHOT_SUFFIX)) {
            snapshot = true;
            version = version.substring(0, version.length() - SNAPSHOT_SUFFIX.length());
        }

        try {
            StringTokenizer st = new StringTokenizer(version, SEPARATOR, true);
            major = Integer.parseInt(st.nextToken());

            if (st.hasMoreTokens()) {
                st.nextToken(); // consume delimiter
                minor = Integer.parseInt(st.nextToken());

                if (st.hasMoreTokens()) {
                    st.nextToken(); // consume delimiter
                    micro = Integer.parseInt(st.nextToken());

                    if (st.hasMoreTokens()) {
                        st.nextToken(); // consume delimiter
                        qualifier = st.nextToken();

                        if (st.hasMoreTokens()) {
                            throw new IllegalArgumentException("Invalid format: " + version);
                        }
                    }
                }
            }
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("invalid format: " + version);
        }

        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier;
        this.snapshot = snapshot;
        validate();
    }

    /**
     * Parses a version identifier from the specified string.
     *
     * See <code>Version(String)</code> for the format of the version string.
     *
     * @param version String representation of the version identifier. Leading and trailing whitespace will be ignored.
     * @return A <code>Version</code> object representing the version identifier. If <code>version</code> is <code>null</code> or the empty string then
     * <code>emptyVersion</code> will be returned.
     * @throws IllegalArgumentException If <code>version</code> is improperly formatted.
     */
    public static Version parseVersion(String version) {
        if (version == null) {
            return emptyVersion;
        }

        version = version.trim();
        if (version.length() == 0) {
            return emptyVersion;
        }

        return new Version(version);
    }

    /**
     * Returns the major component of this version identifier.
     *
     * @return The major component.
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor component of this version identifier.
     *
     * @return The minor component.
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Returns the micro component of this version identifier.
     *
     * @return The micro component.
     */
    public int getMicro() {
        return micro;
    }

    /**
     * Returns the qualifier component of this version identifier.
     *
     * @return The qualifier component.
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Returns true if this version is a snapshot.
     *
     * @return The snapshot component.
     */
    public boolean isSnapshot() {
        return snapshot;
    }

    /**
     * Returns the string representation of this version identifier.
     *
     *
     * The format of the version string will be <code>major.minor.micro</code> if qualifier is the empty string or <code>major.minor.micro.qualifier</code>
     * otherwise.
     *
     * @return The string representation of this version identifier.
     */
    public String toString() {
        String version = major + SEPARATOR + minor + SEPARATOR + micro;
        if (qualifier.length() > 0) {
            version = version + SEPARATOR + qualifier;
        }
        if (snapshot) {
            version = version + SNAPSHOT_SUFFIX;
        }
        return version;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return An integer which is a hash code value for this object.
     */
    public int hashCode() {
        return (((major << 24) + (minor << 16) + (micro << 8) + qualifier.hashCode()) << 1) + (snapshot ? 1 : 0);
    }

    /**
     * Compares this <code>Version</code> object to another object.
     *
     *
     * A version is considered to be <b>equal to </b> another version if the major, minor and micro components are equal and the qualifier component is equal
     * (using <code>String.equals</code>).
     *
     * @param object The <code>Version</code> object to be compared.
     * @return <code>true</code> if <code>object</code> is a <code>Version</code> and is equal to this object; <code>false</code> otherwise.
     */
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof Version)) {
            return false;
        }

        Version other = (Version) object;
        return (major == other.major) && (minor == other.minor) && (micro == other.micro) && qualifier.equals(other.qualifier) && snapshot == other.snapshot;
    }

    /**
     * Compares this <code>Version</code> object to another object.
     *
     *
     * A version is considered to be <b>less than </b> another version if its major component is less than the other version's major component, or the major
     * components are equal and its minor component is less than the other version's minor component, or the major and minor components are equal and its micro
     * component is less than the other version's micro component, or the major, minor and micro components are equal and it's qualifier component is less than
     * the other version's qualifier component (using <code>String.compareTo</code>).
     *
     * A version is considered to be <b>equal to</b> another version if the major, minor and micro components are equal and the qualifier component is equal
     * (using <code>String.compareTo</code>).
     *
     * @param object The <code>Version</code> object to be compared.
     * @return A negative integer, zero, or a positive integer if this object is less than, equal to, or greater than the specified <code>Version</code> object.
     * @throws ClassCastException If the specified object is not a <code>Version</code>.
     */
    public int compareTo(Object object) {
        if (object == this) {
            return 0;
        }

        Version other = (Version) object;

        int result = major - other.major;
        if (result != 0) {
            return result;
        }

        result = minor - other.minor;
        if (result != 0) {
            return result;
        }

        result = micro - other.micro;
        if (result != 0) {
            return result;
        }

        result = qualifier.compareTo(other.qualifier);
        if (result != 0) {
            return result;
        }

        return Boolean.compare(snapshot, other.snapshot);
    }

    /**
     * Called by the Version constructors to validate the version components.
     *
     * @throws IllegalArgumentException If the numerical components are negative or the qualifier string is invalid.
     */
    private void validate() {
        if (major < 0) {
            throw new IllegalArgumentException("negative major");
        }
        if (minor < 0) {
            throw new IllegalArgumentException("negative minor");
        }
        if (micro < 0) {
            throw new IllegalArgumentException("negative micro");
        }
        int length = qualifier.length();
        for (int i = 0; i < length; i++) {
            if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-".indexOf(qualifier.charAt(i)) == -1) {
                throw new IllegalArgumentException("invalid qualifier");
            }
        }
    }
}