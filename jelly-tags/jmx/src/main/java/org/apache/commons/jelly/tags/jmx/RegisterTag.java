/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.jelly.tags.jmx;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.TagSupport;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.impl.CollectionTag;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Registers a JavaBean or JMX MBean with a server.
 */
public class RegisterTag extends TagSupport implements CollectionTag {

    /** The Log to which logging calls will be made. */
    private static final Log log = LogFactory.getLog(RegisterTag.class);

    private ObjectName name;
    private MBeanServer server;

    public RegisterTag() {
    }

    // CollectionTag interface
    //-------------------------------------------------------------------------
    @Override
    public void addItem(final Object bean) throws JellyTagException {
        try {
            register(server, bean);
        }
        catch (final Exception e) {
            throw new JellyTagException("Failed to register bean: " + bean, e);
        }
    }

    // Tag interface
    //-------------------------------------------------------------------------
    @Override
    public void doTag(final XMLOutput output) throws MissingAttributeException, JellyTagException {
        if (name == null) {
            throw new MissingAttributeException("name");
        }
        if (server == null) {
            final ServerTag serverTag = (ServerTag) findAncestorWithClass(ServerTag.class);
            if (serverTag == null) {
                throw new JellyTagException("This class must be nested inside a <server> tag");
            }
            server = serverTag.getServer();
        }
        invokeBody(output);
    }

    // Properties
    //-------------------------------------------------------------------------

    /**
     * @return ObjectName
     */
    public ObjectName getName() {
        return name;
    }

    /**
     * @return MBeanServer
     */
    public MBeanServer getServer() {
        return server;
    }

    /**
     * Registers the given bean with the MBeanServer
     */
    protected void register(final MBeanServer server, final Object bean) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        server.registerMBean(bean, getName());
    }

    /**
     * Sets the name.
     * @param name The name to set
     */
    public void setName(final ObjectName name) {
        this.name = name;
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    /**
     * Sets the MBeanServer. If this attribute is not supplied then the parent &lt;server&gt; tag
     * is used to get the MBeanServer instance to use.
     *
     * @param server The MBeanServer to register the mbeans with.
     */
    public void setServer(final MBeanServer server) {
        this.server = server;
    }

}
