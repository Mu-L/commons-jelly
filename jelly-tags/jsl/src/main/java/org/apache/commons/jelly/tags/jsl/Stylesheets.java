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
package org.apache.commons.jelly.tags.jsl;

import java.util.List;

import org.dom4j.Node;
import org.dom4j.rule.Stylesheet;
import org.jaxen.XPath;

/**
 * Runs a {@link Stylesheet} on an untyped XPath context, which is either a {@link Node} or a {@link List} of nodes.
 * <p>
 * dom4j 1.x offered {@code Stylesheet.run(Object)} and {@code Stylesheet.applyTemplates(Object, ...)} overloads
 * that dispatched on the runtime type of their input; dom4j 2.x only keeps the typed overloads.
 * </p>
 */
final class Stylesheets {

    /**
     * Applies the templates of the given mode to the children of each node in the input.
     */
    static void applyTemplates(final Stylesheet stylesheet, final Object input, final String mode) throws Exception {
        if (input instanceof Node) {
            stylesheet.applyTemplates((Node) input, mode);
        } else if (input instanceof List) {
            for (final Object item : (List<?>) input) {
                if (item instanceof Node) {
                    stylesheet.applyTemplates((Node) item, mode);
                }
            }
        }
    }

    /**
     * Applies the templates of the given mode to the nodes selected by the XPath expression on the input.
     */
    static void applyTemplates(final Stylesheet stylesheet, final Object input, final XPath xpath, final String mode)
            throws Exception {
        for (final Object item : xpath.selectNodes(input)) {
            if (item instanceof Node) {
                stylesheet.run((Node) item, mode);
            }
        }
    }

    /**
     * Runs the stylesheet in its current mode on each node in the input.
     */
    static void run(final Stylesheet stylesheet, final Object input) throws Exception {
        if (input instanceof Node) {
            stylesheet.run((Node) input);
        } else if (input instanceof List) {
            for (final Object item : (List<?>) input) {
                if (item instanceof Node) {
                    stylesheet.run((Node) item);
                }
            }
        }
    }

    private Stylesheets() {
    }
}
