/*
 * Copyright (C) 2015-2016 52°North Initiative for Geospatial Open Source
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
package org.n52.series.ckan.beans;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.n52.series.ckan.da.CkanMapping;

public class ResourceFieldTest {

    private ResourceFieldCreator fieldCreator;

    private CkanMapping ckanMapping;

    @Before
    public void setUp() {
        this.fieldCreator = new ResourceFieldCreator();
        this.ckanMapping = new CkanMapping();
        List<String> mappings = Arrays.asList(new String[]{"FIELD_IDENTIFIER", "ANOTHER_IDENTIFIER"});
        this.ckanMapping.addMapping("testField", new HashSet<>(mappings));
    }

    @Test
    public void when_simpleCreation_then_noExceptions() {
        new ResourceField();
    }

    @Test
    public void when_simpleCreation_then_noExceptionsOnSetter() {
        new ResourceField().setQualifier(new ResourceMember());
    }

    @Test
    public void when_simpleCreation_then_negativeIndex() {
        Assert.assertTrue(new ResourceField().getIndex() < 0);
    }

    @Test
    public void when_simpleCreation_then_falseOnIsFieldEmptyString() {
        Assert.assertFalse(new ResourceField().isField(""));
    }

    @Test
    public void when_simpleCreation_then_falseOnIsField() {
        Assert.assertFalse(new ResourceField().isField("field_id"));
    }

    @Test
    public void when_simpleCreation_then_falseOnIsProperty() {
        Assert.assertFalse(new ResourceField().hasProperty("field_type"));
    }

    @Test
    public void when_simpleCreation_then_falseOnIsOfType() {
        Assert.assertFalse(new ResourceField().isOfType("string"));
    }

    @Test
    public void when_simpleCreation_then_falseOnIsOfTypeEmptyString() {
        Assert.assertFalse(new ResourceField().isOfType(""));
    }

    @Test
    public void when_simpleCreation_then_falseOnIsOfTypeClass() {
        Assert.assertFalse(new ResourceField().isOfType(Double.class));
    }

    @Test
    public void when_checkingFieldType_then_fieldRecognizesMappings() {
        ResourceField testField = fieldCreator
                .withCkanMapping(ckanMapping)
                .createSimple("testField");
        assertThat(testField.isField("field_identifier"), is(true));
    }

    @Test
    public void testEqualityUsingId() {
        ResourceField first = fieldCreator.createSimple("test42");
        MatcherAssert.assertThat(first.equals(fieldCreator.createSimple("test42")), CoreMatchers.is(true));
    }

    @Test
    public void testIdEqualityIgnoringCase() {
        ResourceField first = fieldCreator.createSimple("test42");
        MatcherAssert.assertThat(first.equals(fieldCreator.createSimple("Test42")), CoreMatchers.is(true));
    }

    @Test
    public void testEqualValues() {
        String json = ""
                + "{" +
                "    \"field_id\": \"Stations_id\"," +
                "    \"short_name\": \"station ID\"," +
                "    \"long_name\": \"Station identifier\"," +
                "    \"description\": \"The Identifier for the station declared by the German weather service (DWD)\"," +
                "    \"field_type\": \"Integer\"" +
                "}";
        ResourceField intField = fieldCreator.createFull(json);
        Assert.assertTrue(intField.equalsValues("100", "0100"));
    }

}
