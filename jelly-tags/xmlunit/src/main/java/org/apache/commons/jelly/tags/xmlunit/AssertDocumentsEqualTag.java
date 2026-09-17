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

package org.apache.commons.jelly.tags.xmlunit;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.xml.secure.SecureDocumentBuilderFactory;
import org.apache.commons.xml.secure.SecureSAXParserFactory;

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.XMLOutput;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Compares two XML documents using XMLUnit (http://xmlunit.sourceforge.net/).
 * If they are different an exception will be thrown.
 */
public class AssertDocumentsEqualTag extends XMLUnitTagSupport {

    private Object actual;
    private Document actualDocument;

    private Object expected;
    private Document expectedDocument;

    /**
     * Controls whether whitespace differences are reported as differences.
     *
     * Defaults to {@code false}, so if {@code trim} is set to
     * {@code false} whitespace differences are detected.
     */
    private boolean ignoreWhitespace = false;

    @Override
    protected SAXReader createSAXReader() {
        return createSecureSAXReader();
    }

    @Override
    public void doTag(final XMLOutput output) throws JellyTagException {
        invokeBody(output);

        if (actual != null) {
            if (actualDocument != null) {
                fail("Cannot specify both actual attribute and element");
            }
            actualDocument = parse(actual);
        }

        if (expected != null) {
            if (expectedDocument != null) {
                fail("Cannot specify both expected attribute and element");
            }
            expectedDocument = parse(expected);
        }

        if ((expectedDocument == null
            || expectedDocument.getRootElement() == null)
            && (actualDocument == null
                || actualDocument.getRootElement() == null)) {
            return;
        }

        if (actualDocument != null) {
            XMLUnit.setIgnoreWhitespace(ignoreWhitespace);

            Diff delta = null;
            try {
                // Handed XML text, XMLUnit re-parses it with its own JAXP parser, which fetches external DTDs.
                // Build the DOM trees with Commons Secure XML instead and let XMLUnit compare those.
                final DocumentBuilderFactory factory = SecureDocumentBuilderFactory.newNSInstance();
                factory.setIgnoringElementContentWhitespace(ignoreWhitespace);
                final DocumentBuilder builder = factory.newDocumentBuilder();
                delta = new Diff(toDom(builder, expectedDocument), toDom(builder, actualDocument));
            }
            catch (final Throwable e) {
                throw new JellyTagException(e);
            }

            if (delta.identical()) {
                return;
            }
            fail(delta.toString());
        }
    }

    /**
     * Re-parses a dom4j document into a W3C DOM tree with the given builder.
     */
    private static org.w3c.dom.Document toDom(final DocumentBuilder builder, final Document document)
            throws SAXException, IOException {
        return builder.parse(new InputSource(new StringReader(document.asXML())));
    }

    /**
     * Sets the actual XML document which is either a Document, String (of an
     * URI), URI, Reader, or InputStream.
     */
    public void setActual(final Object actual) {
        this.actual = actual;
    }

    /**
     * Sets the expected XML document which is either a Document, String (of an
     * URI), URI, Reader, or InputStream.
     */
    public void setExpected(final Object expected) {
        this.expected = expected;
    }

    /**
     * Controls whether whitespace differences should be interpreted as
     * differences or not.  The default is {@code false}.  Note that the
     * use of the {@code trim} attribute is crucial here.
     */
    public void setIgnoreWhitespace(final boolean ignoreWhitespace) {
        this.ignoreWhitespace = ignoreWhitespace;
    }

}
