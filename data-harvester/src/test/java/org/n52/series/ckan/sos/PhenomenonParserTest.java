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
package org.n52.series.ckan.sos;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.n52.series.ckan.beans.FieldBuilder;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.table.ResourceTestHelper;

public class PhenomenonParserTest {

    private PhenomenonParser parser;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private ResourceTestHelper testHelper;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        testHelper = new ResourceTestHelper(testFolder);
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
                .withPhenomenon("Temperature")
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

    @Test
    public void when_loadingTemperatureDwdData_then_parseUOMFromSchemaDescription() {
        String dataset = "eab53bfe-fce7-4fd8-8325-a0fe5cdb23c8";
        String observationResource = "a29d8acc-f8b6-402a-b91b-d2962fb1ca10";
        String type = CkanConstants.ResourceType.OBSERVATIONS;
        List<String> phenomenonIds = parsePhenomenonIdsOfResource(dataset, observationResource, type);
        String[] expected = {
                "LUFTTEMPERATUR",
                "REL_FEUCHTE"
        };
        assertThat(phenomenonIds, containsInAnyOrder(expected));
        assertThat(phenomenonIds, not(containsInAnyOrder(new String[] { "STRUKTUR_VERSION", "QUALITAETS_NIVEAU", "MESS_DATUM", "STATIONS_ID"})));
    }

    @Test
    public void when_loadingSunDwdData_then_parseUOMFromSchemaDescription() {
        String dataset = "582ca1ba-bdc0-48de-a685-3184339d29f0";
        String observationResource = "e4e8a0f7-dc71-4bcc-9011-5a9cdebf7f23";
        String type = CkanConstants.ResourceType.OBSERVATIONS;
        List<String> phenomenonIds = parsePhenomenonIdsOfResource(dataset, observationResource, type);
        String[] expected = {
                "STUNDENSUMME_SONNENSCHEIN"
        };
        assertThat(phenomenonIds, containsInAnyOrder(expected));
        assertThat(phenomenonIds, not(containsInAnyOrder(new String[] { "STRUKTUR_VERSION", "QUALITAETS_NIVEAU", "MESS_DATUM", "STATIONS_ID"})));
    }

    @Test
    public void when_loadingOpenWeatherMapTempData_then_parseUOMFromSchemaDescription() {
        String dataset = "a5442a6a-0a84-4326-a5b5-e6288e8fa457";
        String observationResource = "c9077aee-e82f-4b1d-a771-22b310f218bc";
        String type = CkanConstants.ResourceType.OBSERVATIONS;
        List<String> phenomenonIds = parsePhenomenonIdsOfResource(dataset, observationResource, type);
        String[] expected = {
                "temperature"
        };
        assertThat(phenomenonIds, containsInAnyOrder(expected));
        assertThat(phenomenonIds, not(containsInAnyOrder(new String[] { "datatime", "location", "timestamp", "station_id"})));
    }

    @Test
    public void when_loadingHeavyMetalData_then_parseUOMFromSchemaDescription() {
        String dataset = "3eb54ee2-6ec5-4ad9-af96-264159008aa7";
        String observationResource = "c8b2d332-2019-4311-a600-eefe94eb6b54";
        String type = CkanConstants.ResourceType.OBSERVATIONS_WITH_GEOMETRIES;
        List<String> phenomenonIds = parsePhenomenonIdsOfResource(dataset, observationResource, type);
        String[] expected = {
                "Zn(1000 - 400) [micro_g/g]",
                "Zn(400 - 100) [micro_g/g]",
                "Zn(100 - 63) [micro_g/g]",
                "Zn(63 - 0.45) [micro_g/g]",
                "Zn(SUMM) [micro_g/g]",
                "Cu(1000 - 400) [micro_g/g]",
                "Cu(400 - 100) [micro_g/g]",
                "Cu(100 - 63) [micro_g/g]",
                "Cu(63 - 0.45) [micro_g/g]",
                "Cu(SUMM) [micro_g/g]",
                "Cd(1000 - 400) [micro_g/g]",
                "Cd(400 - 100) [micro_g/g]",
                "Cd(100 - 63) [micro_g/g]",
                "Cd(63 - 0.45) [micro_g/g]",
                "Cd(SUMM) [micro_g/g]"
        };
        assertThat(phenomenonIds, containsInAnyOrder(expected));
        assertThat(phenomenonIds, not(containsInAnyOrder(new String[] { "X", "Y", "Timestamp", ""})));
    }

    private List<String> parsePhenomenonIdsOfResource(String dataset, String observationResource, String type) {
        ResourceMember member = new ResourceMember(observationResource, type);
        ResourceMember metadata = testHelper.getResourceMember(dataset, member);
        List<Phenomenon> phenomena = parser.parse(metadata.getResourceFields());
        return toIds(phenomena);
    }

    private List<String> toIds(List<Phenomenon> phenomena) {
        List<String> ids = new ArrayList<>();
        for (Phenomenon phenomenon : phenomena) {
            ids.add(phenomenon.getId());
        }
        return ids;
    }

}
