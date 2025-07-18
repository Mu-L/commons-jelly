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
package org.apache.commons.jelly.tags.bean;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.beanutils2.MethodUtils;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.Tag;
import org.apache.commons.jelly.TagLibrary;
import org.apache.commons.jelly.impl.TagFactory;
import org.apache.commons.jelly.impl.TagScript;
import org.xml.sax.Attributes;

/** Describes the Taglib. This class could be generated by XDoclet
  */
public class BeanTagLibrary extends TagLibrary {

    /** Synchronized map of tag names to bean classes */
    private final Map beanTypes = new Hashtable();

    /** Synchronized map of tag names to invoke methods */
    private final Map invokeMethods = new Hashtable();

    public BeanTagLibrary() {
        registerTagFactory(
            "beandef",
            (name, attributes) -> new BeandefTag(BeanTagLibrary.this)
        );
    }

    protected Tag createBeanTag(final String name, final Attributes attributes) throws JellyException {
        // is the name bound to a specific class
        final Class beanType = getBeanType(name, attributes);
        if (beanType != null) {
            final Method invokeMethod = (Method) invokeMethods.get(name);
            return new BeanTag(beanType, name, invokeMethod);
        }

        // its a property tag
        return new BeanPropertyTag(name);
    }

    /**
     * Factory method to create a TagFactory for a given tag attribute and attributes
     */
    protected TagFactory createTagFactory(final String name, final Attributes attributes) throws JellyException {

        return this::createBeanTag;
    }

    // TagLibrary interface
    //-------------------------------------------------------------------------
    @Override
    public TagScript createTagScript(
        final String name, final Attributes attributes
    ) throws JellyException {

        // check for standard tags first
        final TagScript answer = super.createTagScript(name, attributes);
        if (answer != null) {
            return answer;
        }

        // lets try a dynamic tag
        return new TagScript( createTagFactory(name, attributes) );
    }

    protected Class getBeanType(final String name, final Attributes attributes) {
        return (Class) beanTypes.get(name);
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    /**
     * Allows tags to register new bean types
     */
    public void registerBean(final String name, final Class type) {
        beanTypes.put(name, type);
    }

    /**
     * Allows tags to register new bean types with an associated method
     */
    public void registerBean(final String name, final Class type, final Method method) {
        registerBean(name, type);
        if (method != null) {
            invokeMethods.put(name, method);
        }
        else {
            invokeMethods.remove(name);
        }
    }

    /**
     * Allows tags to register new bean types with an associated method
     */
    public void registerBean(final String name, final Class type, final String methodName) {
        final Method method = MethodUtils.getAccessibleMethod(
            type, methodName, BeandefTag.EMPTY_ARGUMENT_TYPES
        );
        registerBean(name, type, method);
    }
}
