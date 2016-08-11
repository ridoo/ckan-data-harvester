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

package org.n52.series.ckan.sos;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.n52.series.ckan.beans.FieldBuilder;
import org.n52.series.ckan.beans.ResourceField;

public class PhenomenonParserTest {

    private PhenomenonParser parser;

    @Before
    public void setUp() {
        this.parser = new PhenomenonParser();
    }

    @Test
    public void when_passingEmptyList_then_returnEmptyList() {
        final List<ResourceField> emptyList = Collections.emptyList();
        assertThat(parser.parse(emptyList), is(emptyCollectionOf(Phenomenon.class)));
    }

    @Test
    public void when_withUomProperty_then_returnPhenomenon() {
        List<ResourceField> fields = singletonList(FieldBuilder.aField()
                .withFieldId("observationValue")
                .withUom("°C")
                .create());
        List<Phenomenon> actual = parser.parse(fields);
        assertThat(actual, hasSize(1));
        Phenomenon phenomenon = actual.get(0);
        assertThat(phenomenon.getId(), is("observationValue"));
    }

    @Test
    public void when_singlePhenomenon_then_singletonList() {
        List<ResourceField> fields = singletonList(FieldBuilder.aField()
                .withFieldId("observationValue")
                .withPhenomenon("temperature")
                .withUom("°C")
                .create());
        assertThat(parser.parse(fields), hasSize(1));
    }

    @Test
    public void when_multiplePhenomenona_then_nonSingletonList() {
        List<ResourceField> fields = new ArrayList<>();
        fields.add(FieldBuilder.aField()
                   .withFieldId("observationValue")
                   .withPhenomenon("temperature")
                   .withUom("°C")
                   .create());
        fields.add(FieldBuilder.aField()
                   .withFieldId("someValue")
                   .withPhenomenon("Kelvin")
                   .withUom("K")
                   .create());
        assertThat(parser.parse(fields), hasSize(2));
    }


}
