/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//jelly/src/java/org/apache/commons/jelly/impl/Attic/DynaTagScript.java,v 1.13 2002/09/30 17:38:16 jstrachan Exp $
 * $Revision: 1.13 $
 * $Date: 2002/09/30 17:38:16 $
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
 * $Id: DynaTagScript.java,v 1.13 2002/09/30 17:38:16 jstrachan Exp $
 */
package org.apache.commons.jelly.impl;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.beanutils.ConvertingWrapDynaBean;
import org.apache.commons.beanutils.DynaBean;

import org.apache.commons.jelly.CompilableTag;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.Tag;
import org.apache.commons.jelly.DynaTag;
import org.apache.commons.jelly.DynaBeanTagSupport;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.expression.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * <p><code>DynaTagScript</code> is a script evaluates a custom DynaTag.</p>
 *
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @version $Revision: 1.13 $
 */
public class DynaTagScript extends TagScript {

    /** The Log to which logging calls will be made. */
    private static final Log log = LogFactory.getLog(DynaTagScript.class);

    public DynaTagScript() {
    }

    public DynaTagScript(TagFactory tagFactory) {
        super(tagFactory);
    }

    // Script interface
    //-------------------------------------------------------------------------                

    /** Evaluates the body of a tag */
    public void run(JellyContext context, XMLOutput output) throws Exception {
        if ( ! context.isCacheTags() ) {
            clearTag();
        }
        try {
            Tag tag = getTag();
            if ( tag == null ) {
                return;
            }
            tag.setContext(context);
    
            if ( tag instanceof DynaTag ) {        
                DynaTag dynaTag = (DynaTag) tag;
        
                // ### probably compiling this to 2 arrays might be quicker and smaller
                for (Iterator iter = attributes.entrySet().iterator(); iter.hasNext();) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String name = (String) entry.getKey();
                    Expression expression = (Expression) entry.getValue();

                    Class type = dynaTag.getAttributeType(name);
                    Object value = null;        
                    if (type != null && type.isAssignableFrom(Expression.class) && !type.isAssignableFrom(Object.class)) {
                        value = expression;
                    }
                    else {
                        value = expression.evaluate(context);
                    }
                    dynaTag.setAttribute(name, value);
                }
            }
            else {
                // treat the tag as a bean
                DynaBean dynaBean = new ConvertingWrapDynaBean( tag );
                for (Iterator iter = attributes.entrySet().iterator(); iter.hasNext();) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String name = (String) entry.getKey();
                    Expression expression = (Expression) entry.getValue();
        
/*
 * @todo should implement this                    
                    Object value = null;        
                    if (type.isAssignableFrom(Expression.class) && !type.isAssignableFrom(Object.class)) {
                        value = expression;
                    }
                    else {
                        value = expression.evaluate(context);
                    }
*/                    
                    Object value = expression.evaluate(context);
                    dynaBean.set(name, value);
                }
            }
        
            tag.doTag(output);
        } 
        catch (JellyException e) {
            handleException(e);
        }
        catch (Exception e) {
            handleException(e);
        }
    }
}
