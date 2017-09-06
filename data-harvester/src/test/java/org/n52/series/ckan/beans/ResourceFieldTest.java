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

package org.n52.series.ckan.beans;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.da.CkanMapping;

public class ResourceFieldTest {

    private static final String ID_VALUE = "IDENTIFIER_A";

    private static final String ID_VALUE_MAPPED = "IDENTIFIER_B";

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
    public void when_fieldWithFieldType_then_detectFieldType() {
        ResourceField testField = FieldBuilder.aField()
                                              .withFieldId("float_field")
                                              .withFieldType("Float")
                                              .create();
        Assert.assertTrue(testField.isOfType(CkanConstants.DataType.DOUBLE));
    }

    @Test
    public void when_checkingFieldType_then_fieldRecognizesMappings() {
        ResourceField testField = FieldBuilder.aField()
                                              .withFieldMappings("field_id", ID_VALUE, ID_VALUE_MAPPED)
                                              .createSimple(ID_VALUE);
        assertThat(testField.isField(ID_VALUE), is(true));
    }

    @Test
    public void testEqualityUsingId() {
        ResourceField first = FieldBuilder.aField()
                                          .createSimple("test42");
        ResourceField second = FieldBuilder.aField()
                                           .createSimple("test42");
        MatcherAssert.assertThat(first.equals(second), CoreMatchers.is(true));
    }

    @Test
    public void testIdEqualityIgnoringCase() {
        ResourceField expected = FieldBuilder.aField()
                                             .createSimple("Test42");
        ResourceField actual = FieldBuilder.aField()
                                           .createSimple("test42");
        MatcherAssert.assertThat(actual, is(expected));
    }

    @Test
    public void testEqualValues() {
        String json = ""
                + "{"
                + "    \"field_id\": \"Stations_id\","
                + "    \"short_name\": \"station ID\","
                + "    \"long_name\": \"Station identifier\","
                + "    \"description\": \"The Identifier for the station declared by the German weather service (DWD)\","
                + "    \"field_type\": \"Integer\""
                + "}";
        ResourceField intField = FieldBuilder.aField()
                                             .createViaTemplate(json);
        Assert.assertTrue(intField.equalsValues("100", "0100"));
    }

    @Test
    public void when_havingMappedAlternateIds_then_bothConsideredEqualFields() {
        ResourceField first = FieldBuilder.aField()
                                          .withFieldMappings(ID_VALUE, ID_VALUE_MAPPED)
                                          .createSimple(ID_VALUE);
        ResourceField second = FieldBuilder.aField()
                                           .withFieldMappings(ID_VALUE, ID_VALUE_MAPPED)
                                           .createSimple(ID_VALUE_MAPPED);
        assertThat("first field is not considered equal with second field having mapped id value", first, is(second));
        assertThat("second field is not considered equal with other first field having mapped id value",
                   second,
                   is(first));
    }

    @Test
    public void when_havingMappedAlternateIds_then_hashCodeRespectsAllMappings() {
        ResourceField first = FieldBuilder.aField()
                                          .withFieldMappings("field", ID_VALUE, ID_VALUE_MAPPED)
                                          .createSimple(ID_VALUE);
        ResourceField second = FieldBuilder.aField()
                                           .withFieldMappings("field", ID_VALUE, ID_VALUE_MAPPED)
                                           .createSimple(ID_VALUE_MAPPED);
        assertThat("hashCode is not equal of fields with mapped ids.", first.hashCode(), is(second.hashCode()));
    }

    @Test
    public void when_fieldsMatchOnlyViaMapping_then_equalsTrue() {
        String fieldMapping = "{"
                + "\"field\": {"
                + "  \"warncellid\" : ["
                + "      \"gc_warncellid\""
                + "    ]"
                + "  }"
                + "}";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fieldMapping.getBytes());
        CkanMapping ckanMapping = CkanMapping.loadCkanMapping(inputStream);

        ResourceField field1 = FieldBuilder.aField(ckanMapping)
                                           .createSimple("warncellid");
        ResourceField field2 = FieldBuilder.aField(ckanMapping)
                                           .createSimple("gc_warncellid");
        assertThat(field1, is(field2));
    }

    @Test
    public void when_fieldsMatchOnlyViaMapping_then_hashCodeIsSame() {
        String fieldMapping = "{"
                + "\"field\": {"
                +
                "    \"warncellid\" : ["
                +
                "        \"gc_warncellid\""
                +
                "      ],"
                + "  }"
                + "}";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fieldMapping.getBytes());
        CkanMapping ckanMapping = CkanMapping.loadCkanMapping(inputStream);

        ResourceField field1 = FieldBuilder.aField()
                                           .withCkanMapping(ckanMapping)
                                           .createSimple("warncellid");
        ResourceField field2 = FieldBuilder.aField()
                                           .withCkanMapping(ckanMapping)
                                           .createSimple("gc_warncellid");
        assertThat(field1.hashCode(), is(field2.hashCode()));
    }
}
