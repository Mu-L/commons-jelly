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

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.XMLOutput;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

public class ActualTag extends XMLUnitTagSupport {

    @Override
    protected SAXReader createSAXReader() {
        return new SAXReader();
    }

    @Override
    public void doTag(final XMLOutput output) throws JellyTagException {
        final Document actualDocument = parseBody();

        final AssertDocumentsEqualTag assertTag =
            (AssertDocumentsEqualTag) findAncestorWithClass(AssertDocumentsEqualTag
                .class);
        assertTag.setActual(actualDocument);
    }
}
