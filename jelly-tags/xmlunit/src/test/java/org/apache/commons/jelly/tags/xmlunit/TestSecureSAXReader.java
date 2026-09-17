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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.dom4j.Document;
import org.junit.Test;

/**
 * Tests the SAXReader created by {@link XMLUnitTagSupport#createSecureSAXReader()}.
 */
public class TestSecureSAXReader {

    /**
     * The DOCTYPE of the document points to a DTD that does not exist, so parsing only succeeds if dom4j leaves the
     * external DTD to the ignore-all resolver of Commons Secure XML instead of installing its own resolver.
     */
    @Test
    public void testExternalDtdIsNotFetched() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("externalDtd.xml")) {
            assertNotNull("externalDtd.xml", in);
            final Document document = XMLUnitTagSupport.createSecureSAXReader().read(in, "externalDtd.xml");
            assertEquals("a", document.getRootElement().getName());
            assertEquals("text", document.getRootElement().getText());
        }
    }
}
