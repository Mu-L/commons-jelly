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

import java.io.StringReader;

import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.XMLOutput;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.junit.Test;
import org.xml.sax.InputSource;

/**
 * Tests {@link AssertDocumentsEqualTag}.
 */
public class TestAssertDocumentsEqualTag {

    private static final String SCRIPT = "<j:jelly xmlns:j=\"jelly:core\" xmlns:xu=\"jelly:xmlunit\">"
            + "<xu:assertDocumentsEqual actual=\"${doc}\" expected=\"${doc}\" ignoreWhitespace=\"${ignoreWhitespace}\"/>"
            + "</j:jelly>";

    /**
     * The DOCTYPE of the compared documents points to a DTD that does not exist, so the comparison only succeeds if
     * XMLUnit is not left to re-parse the documents with a parser that fetches external DTDs.
     */
    private static void assertExternalDtdIsNotFetched(final boolean ignoreWhitespace) throws Exception {
        final Document document = DocumentHelper.createDocument();
        document.addDocType("a", null, "missing.dtd");
        document.addElement("a").addText("text");

        final JellyContext context = new JellyContext();
        context.setVariable("doc", document);
        context.setVariable("ignoreWhitespace", ignoreWhitespace);
        context.runScript(new InputSource(new StringReader(SCRIPT)), XMLOutput.createDummyXMLOutput());
    }

    @Test
    public void testExternalDtdIsNotFetched() throws Exception {
        assertExternalDtdIsNotFetched(false);
    }

    @Test
    public void testExternalDtdIsNotFetchedWhenIgnoringWhitespace() throws Exception {
        assertExternalDtdIsNotFetched(true);
    }
}
