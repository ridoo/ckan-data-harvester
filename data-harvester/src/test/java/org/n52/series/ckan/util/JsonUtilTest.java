/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.series.ckan.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtilTest {

    private static final String TEST_TEMPLATE = ""
            + "{" +
            "    \"property\": \"value\"," +
            "    \"list\": [\"item1\", \"item2\"]," +
            "    \"uppercased\": \"UPPERCASED_VALUE\"," +
            "    \"key\": \"value\"," +
            "    \"wkt_geometry\": \"POINT(7.2 51)\"" +
            "}";
    private ObjectMapper om;

    @Before
    public void setUp() {
        this.om = new ObjectMapper();
    }

    @Test
    public void when_parseMissingTextNode_then_returnEmptyString() throws IOException {
        JsonNode node = om.readTree(TEST_TEMPLATE);
        Set<String> names = Collections.singleton("missing");
        assertThat(JsonUtil.parse(node, names), is(""));
    }

    @Test
    public void when_parseAvailableTextNode_then_returnValue() throws IOException {
        JsonNode node = om.readTree(TEST_TEMPLATE);
        Set<String> names = Collections.singleton("property");
        assertThat(JsonUtil.parse(node, names), is("value"));
    }

    @Test
    public void when_parseMissingArrayNode_then_returnEmptyList() throws IOException {
        JsonNode node = om.readTree(TEST_TEMPLATE);
        Set<String> names = Collections.singleton("missingList");
        assertThat(JsonUtil.parseMissingToEmptyArray(node, names), is(empty()));
    }

    @Test
    public void when_parseAvailableArrayNode_then_returnList() throws IOException {
        JsonNode node = om.readTree(TEST_TEMPLATE);
        Set<String> names = Collections.singleton("list");
        assertThat(JsonUtil.parseMissingToEmptyArray(node, names).size(), is(2));
    }

    @Test
    public void when_parseLowerCasedNodeWithUpperCasedName_then_returnValue() throws IOException {
        JsonNode node = om.readTree(TEST_TEMPLATE);
        Set<String> names = Collections.singleton("uppercased");
        assertThat(JsonUtil.parse(node, names), is("UPPERCASED_VALUE"));
    }

    @Test
    public void when_nodeContainsUpperCasedValue_then_findAlsoViaLowerCased() throws IOException {
        JsonNode node = om.readTree(TEST_TEMPLATE);
        Set<String> names = Collections.singleton("KEY");
        assertThat(JsonUtil.parse(node, names), is("value"));
    }


    @Test
    public void when_parseWithAlternateProperties_then_findValue() throws IOException {
        JsonNode node = om.readTree(TEST_TEMPLATE);
        Set<String> names = Collections.singleton("wkt_geometry");
        assertThat(JsonUtil.parse(node, names), is("POINT(7.2 51)"));
    }
}
