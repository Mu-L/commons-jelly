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

package org.apache.commons.jelly.tags.ant;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.beanutils2.BeanUtils;
import org.apache.commons.beanutils2.MethodUtils;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MapTagSupport;
import org.apache.commons.jelly.Tag;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.expression.Expression;
import org.apache.commons.jelly.impl.BeanSource;
import org.apache.commons.jelly.impl.StaticTag;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.types.DataType;

/**
 * Tag supporting ant's Tasks as well as
 * dynamic runtime behavior for 'unknown' tags.
 */
public class AntTag extends MapTagSupport implements TaskSource {

    /** The Log to which logging calls will be made. */
    private static final Log log = LogFactory.getLog(AntTag.class);

    private static final Class[] addTaskParamTypes = { String.class };

    /** Store the name of the manifest tag for special handling */
    private static final String ANT_MANIFEST_TAG = "manifest";

    /** The name of this tag. */
    protected String tagName;

    /** The general object underlying this tag. */
    protected Object object;

    /** Task, if this tag represents a task. */
    protected Task task;

    /** Constructs  with a project and tag name.
     *
     *  @param tagName The name on the tag.
     */
    public AntTag(final String tagName) {
        this.tagName = tagName;
    }

    /**
     * @return an object create with the given constructor and args.
     * @param ctor a constructor to use creating the object
     * @param args the arguments to pass to the constructor
     * @param name the name of the data type being created
     * @param argDescription a human readable description of the args passed
     */
    private Object createDataType(final Constructor ctor, final Object[] args, final String name, final String argDescription) {
        try {
            final Object datatype = ctor.newInstance(args);
            return datatype;
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException ite) {
            log.error("datatype '" + name + "' couldn't be created with " + argDescription, ite);
        }
        return null;
    }

    // TaskSource interface
    //-------------------------------------------------------------------------

    public Object createDataType(final String name) {

        Object dataType = null;

        final Class type = (Class) getAntProject().getDataTypeDefinitions().get(name);

        if ( type != null ) {

            Constructor ctor = null;
            boolean noArg = false;

            // DataType can have a "no arg" constructor or take a single
            // Project argument.
            try {
                ctor = type.getConstructor(new Class[0]);
                noArg = true;
            }
            catch (final NoSuchMethodException nse) {
                try {
                    ctor = type.getConstructor(new Class[] { Project.class });
                    noArg = false;
                } catch (final NoSuchMethodException nsme) {
                    log.info("datatype '" + name
                        + "' didn't have a constructor with an Ant Project", nsme);
                }
            }

            if (noArg) {
                dataType = createDataType(ctor, new Object[0], name, "no-arg constructor");
            }
            else {
                dataType = createDataType(ctor, new Object[] { getAntProject() }, name, "an Ant project");
            }
            if (dataType != null && dataType instanceof DataType) {
                ((DataType)dataType).setProject( getAntProject() );
            }
        }

        return dataType;
    }

    /**
     * Creates a nested object of the given object with the specified name
     */
    public Object createNestedObject(final Object object, final String name) {
        Object dataType = null;
        if ( object != null ) {
            final IntrospectionHelper ih = IntrospectionHelper.getHelper( object.getClass() );

            if ( ih != null && ! (object instanceof AntTag)) {
                try {
                    dataType = ih.createElement( getAntProject(), object, name.toLowerCase() );
                } catch (final BuildException be) {
                    if (object instanceof Tag)
                    {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed attempt to create an ant datatype for a jelly tag", be);
                        }
                    } else {
                        log.error(be);
                    }
                }
            }
        }

        if ( dataType == null ) {
            dataType = createDataType( name );
        }

        return dataType;
    }

    /**
     * @param taskName
     * @return
     * @throws JellyTagException
     */
    public Task createTask(final String taskName) throws JellyTagException {
        return createTask( taskName,
                           (Class) getAntProject().getTaskDefinitions().get( taskName ) );
    }

    public Task createTask(final String taskName,
                           final Class taskType) throws JellyTagException {

        if (taskType == null) {
            return null;
        }

        Object o = null;
        try {
            o = taskType.getConstructor().newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new JellyTagException(e);
        }

        Task task = null;
        if ( o instanceof Task ) {
            task = (Task) o;
        }
        else {
            final TaskAdapter taskA=new TaskAdapter();
            taskA.setProxy( o );
            task=taskA;
        }

        task.setProject(getAntProject());
        task.setTaskName(taskName);

        return task;
    }

    // Tag interface
    //-------------------------------------------------------------------------
    @Override
    public void doTag(final XMLOutput output) throws JellyTagException {

        final Project project = getAntProject();
        final String tagName = getTagName();
        final Object parentObject = findBeanAncestor();
        final Object parentTask = findParentTaskObject();

        // lets assume that Task instances are not nested inside other Task instances
        // for example <manifest> inside a <jar> should be a nested object, where as
        // if the parent is not a Task the <manifest> should create a ManifestTask
        //
        // also its possible to have a root Ant tag which isn't a task, such as when
        // defining <fileset id="...">...</fileset>

        Object nested = null;
        if (parentObject != null && !( parentTask instanceof TaskContainer) ) {
            nested = createNestedObject( parentObject, tagName );
        }

        if (nested == null) {
            task = createTask( tagName );

            if (task != null) {

                if ( log.isDebugEnabled() ) {
                    log.debug( "Creating an ant Task for name: " + tagName );
                }

                // the following algorithm follows the lifetime of a tag
                // http://jakarta.apache.org/ant/manual/develop.html#writingowntask
                // kindly recommended by Stefan Bodewig

                // create and set its project reference
                if ( task instanceof TaskAdapter ) {
                    setObject( ((TaskAdapter)task).getProxy() );
                }
                else {
                    setObject( task );
                }

                // set the task ID if one is given
                final Object id = getAttributes().remove( "id" );
                if ( id != null ) {
                    project.addReference( (String) id, task );
                }

                // ### we might want to spoof a Target setting here

                // now lets initialize
                task.init();

                // now lets invoke the body to call all the createXXX() or addXXX() methods
                final String body = getBodyText();

                // now lets set any attributes of this tag...
                setBeanProperties();

                // now lets set the addText() of the body content, if its applicable
                final Method method = MethodUtils.getAccessibleMethod( task.getClass(),
                                                                 "addText",
                                                                 addTaskParamTypes );
                if (method != null) {
                    final Object[] args = { body };
                    try {
                        method.invoke(this.task, args);
                    }
                    catch (final IllegalAccessException | InvocationTargetException e) {
                        throw new JellyTagException(e);
                    }
                }

                // now lets set all the attributes of the child elements
                // XXXX: to do!

                // now we're ready to invoke the task
                // XXX: should we call execute() or perform()?
                // according to org.apache.tools.ant.Main, redirect stdout and stderr
                final PrintStream initialOut = System.out;
                final PrintStream initialErr = System.err;
                final PrintStream newOut = new PrintStream(new DemuxOutputStream(project, false));
                final PrintStream newErr = new PrintStream(new DemuxOutputStream(project, true));
                try {
                    System.setOut(newOut);
                    System.setErr(newErr);
                    task.perform();
                } finally {
                    System.setOut(initialOut);
                    System.setErr(initialErr);
                }
            }
        }

        if (task == null) {

            if (nested == null) {

                if ( log.isDebugEnabled() ) {
                    log.debug( "Trying to create a data type for tag: " + tagName );
                }
                nested = createDataType( tagName );
            } else if ( log.isDebugEnabled() ) {
                log.debug( "Created nested property tag: " + tagName );
            }

            if ( nested != null ) {
                setObject( nested );

                // set the task ID if one is given
                final Object id = getAttributes().remove( "id" );
                if ( id != null ) {
                    project.addReference( (String) id, nested );
                }

                // TODO: work out why we always set the name attribute.
                // See JELLY-105.
//                try{
//                    PropertyUtils.setProperty( nested, "name", tagName );
//                }
//                catch (Exception e) {
//                    log.warn( "Caught exception setting nested name: " + tagName, e );
//                }

                // now lets invoke the body
                final String body = getBodyText();

                // now lets set any attributes of this tag...
                setBeanProperties();

                // now lets add it to its parent
                if ( parentObject != null ) {
                    final IntrospectionHelper ih = IntrospectionHelper.getHelper( parentObject.getClass() );
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("About to set the: " + tagName
                                + " property on: " + safeToString(parentObject) + " to value: "
                                + nested + " with type: " + nested.getClass()
                            );
                        }

                        ih.storeElement( project, parentObject, nested, tagName.toLowerCase() );
                    }
                    catch (final Exception e) {
                        log.warn( "Caught exception setting nested: " + tagName, e );
                    }

                    // now try to set the property for good measure
                    // as the storeElement() method does not
                    // seem to call any setter methods of non-String types
                    try {
                        BeanUtils.setProperty( parentObject, tagName, nested );
                    }
                    catch (final Exception e) {
                        log.debug("Caught exception trying to set property: " + tagName + " on: " + safeToString(parentObject));
                    }
                }
            }
            else {
                log.warn("Could not convert tag: " + tagName + " into an Ant task, data type or property");

                // lets treat this tag as static XML...
                final StaticTag tag = new StaticTag("", tagName, tagName);
                tag.setParent( getParent() );
                tag.setBody( getBody() );

                tag.setContext(context);

                for (final Iterator iter = getAttributes().entrySet().iterator(); iter.hasNext();) {
                    final Map.Entry entry = (Map.Entry) iter.next();
                    final String name = (String) entry.getKey();
                    final Object value = entry.getValue();

                    tag.setAttribute(name, value);
                }

                tag.doTag(output);
            }
        }
    }

    /**
     * Attempts to look up in the parent hierarchy for a tag that implements the
     * TaskSource interface, which returns an Ant Task object or that implements
     * BeanSource interface which creates a bean,
     * or will return the parent tag, which is also a bean.
     */
    protected Object findBeanAncestor() throws JellyTagException {
        Tag tag = getParent();
        while (tag != null) {
            if (tag instanceof BeanSource) {
                final BeanSource beanSource = (BeanSource) tag;
                return beanSource.getBean();
            }
            if (tag instanceof TaskSource) {
                final TaskSource taskSource = (TaskSource) tag;
                return taskSource.getTaskObject();
            }
            tag = tag.getParent();
        }
        return getParent();
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    /**
     * Walks the hierarchy until it finds a parent TaskSource and returns its source or returns null
     */
    protected Object findParentTaskObject() throws JellyTagException {
        Tag tag = getParent();
        while (tag != null) {
            if (tag instanceof TaskSource) {
                final TaskSource source = (TaskSource) tag;
                return source.getTaskObject();
            }
            tag = tag.getParent();
        }
        return null;
    }

    public Project getAntProject() {
        return Objects.requireNonNull(AntTagLibrary.getProject(context), "No Ant Project object is available");
    }

    // Properties
    //-------------------------------------------------------------------------
    public String getTagName() {
        return this.tagName;
    }

    /** Retrieve the general object underlying this tag.
     *
     *  @return The object underlying this tag.
     */
    @Override
    public Object getTaskObject() {
        return this.object;
    }

    private String safeToString(final Object o) {
        if (o==null) {
            return "null";
        }
        String r = null;
        try {
            r = o.toString();
        } catch (final Exception ex) {}
        if (r == null) {
            r = "(object of class " + o.getClass() + ")";
        }
        return r;
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        if ( value == null ) {
            // should we send in null?
            super.setAttribute( name, "" );
        } else if ( value instanceof Expression )
        {
            super.setAttribute( name, ((Expression) value).evaluateRecurse(context) );
        }
        else
        {
            super.setAttribute( name, value.toString() );
        }
    }

    /**
     * Sets the properties on the Ant task
     */
    public void setBeanProperties() throws JellyTagException {
        final Object object = getTaskObject();
        if ( object != null ) {
            final Map map = getAttributes();
            for ( final Iterator iter = map.entrySet().iterator(); iter.hasNext(); ) {
                final Map.Entry entry = (Map.Entry) iter.next();
                final String name = (String) entry.getKey();
                final Object value = entry.getValue();
                setBeanProperty( object, name, value );
            }
        }
    }

    public void setBeanProperty(final Object object, final String name, final Object value) throws JellyTagException {
        if ( log.isDebugEnabled() ) {
            log.debug( "Setting bean property on: "+  safeToString(object )+ " name: " + name + " value: " + safeToString(value));
        }

        final IntrospectionHelper ih = IntrospectionHelper.getHelper( object.getClass() );

        if ( value instanceof String ) {
            try {
                ih.setAttribute( getAntProject(), object, name.toLowerCase(), (String) value );
                return;
            }
            catch (final Exception e) {
                // ignore: not a valid property
            }
        }

        try {

            ih.storeElement( getAntProject(), object, value, name );
        }
        catch (final Exception e) {

            try {
                // let any exceptions bubble up from here
                BeanUtils.setProperty( object, name, value );
            }
            catch (final IllegalAccessException | InvocationTargetException ex) {
                throw new JellyTagException(ex);
            }
        }
    }

    /** Sets the object underlying this tag.
     *
     *  @param object The object.
     */
    public void setObject(final Object object) {
        this.object = object;
    }

    /**
     * Allows nested tags to set a property on the task object of this tag
     */
    @Override
    public void setTaskProperty(final String name, final Object value) throws JellyTagException {
        final Object object = getTaskObject();
        if ( object != null ) {
            setBeanProperty( object, name, value );
        }
    }

    @Override
    public String toString() {
        return "[AntTag: name=" + getTagName() + "]";
    }

}
