/*
 * $Header: /home/cvs/jakarta-commons-sandbox/jelly/src/java/org/apache/commons/jelly/impl/DynaTagScript.java,v 1.8 2002/06/25 17:10:07 jstrachan Exp $
 * $Revision: 1.8 $
 * $Date: 2002/06/25 17:10:07 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * $Id: DynaTagScript.java,v 1.8 2002/06/25 17:10:07 jstrachan Exp $
 */
package org.apache.commons.jelly.impl;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jelly.CompilableTag;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.Tag;
import org.apache.commons.jelly.DynaTag;
import org.apache.commons.jelly.DynaBeanTagSupport;
import org.apache.commons.jelly.TagLibrary;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.expression.Expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * <p><code>StaticTagScript</code> is a script that evaluates a StaticTag, a piece of static XML
 * though its attributes or element content may contain dynamic expressions.
 * The first time this tag evaluates, it may have become a dynamic tag, so it will check that
 * a new dynamic tag has not been generated.</p>
 *
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @version $Revision: 1.8 $
 */
public class StaticTagScript extends DynaTagScript {

    /** The Log to which logging calls will be made. */
    private static final Log log = LogFactory.getLog(StaticTagScript.class);

    public StaticTagScript() {
    }

    public StaticTagScript(TagFactory tagFactory) {
        super(tagFactory);
    }
    

    // Script interface
    //-------------------------------------------------------------------------                
    /** Evaluates the body of a tag */
    public void run(JellyContext context, XMLOutput output) throws Exception {

        startNamespacePrefixes(output);
            
        Tag tag = getTag();                
        
        // lets see if we have a dynamic tag
        if (tag instanceof StaticTag) {
            tag = findDynamicTag(context, (StaticTag) tag);
        }            
        
        setTag(tag);
        
        try {        
            if ( tag == null ) {
                return;
            }
            tag.setContext(context);
            
            DynaTag dynaTag = (DynaTag) tag;
    
            // ### probably compiling this to 2 arrays might be quicker and smaller
            for (Iterator iter = attributes.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                String name = (String) entry.getKey();
                Expression expression = (Expression) entry.getValue();
    
                Object value = expression.evaluate(context);
                dynaTag.setAttribute(name, value);
            }
        
            tag.doTag(output);
        } 
        catch (JellyException e) {
            handleException(e);
        }
        catch (Exception e) {
            handleException(e);
        }
        
        endNamespacePrefixes(output);
    }

    /**
     * Attempts to find a dynamically created tag that has been created since this
     * script was compiled
     */    
    protected Tag findDynamicTag(JellyContext context, StaticTag tag) throws Exception {
        // lets see if there's a tag library for this URI...
        TagLibrary taglib = context.getTagLibrary( tag.getUri() );
        if ( taglib instanceof DynamicTagLibrary ) {
            DynamicTagLibrary dynaLib = (DynamicTagLibrary) taglib;
            Tag newTag = dynaLib.createTag( tag.getLocalName() );
            if ( newTag != null ) {
                newTag.setParent( tag.getParent() );
                newTag.setBody( tag.getBody() );
                return newTag;
            }
        }
        return tag;
    }
}
