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

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.n52.series.ckan.beans.FieldBuilder;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.da.CkanConstants;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanOrganization;

public class SimpleFeatureBuilderTest {

    private static final String ORGA_NAME = "test_orga";

    private SimpleFeatureBuilder createFeatureBuilder() {
        CkanDataset dataset = new CkanDataset("test_dataset");
        dataset.setOrganization(new CkanOrganization(ORGA_NAME));
        return new SimpleFeatureBuilder(dataset);
    }

    @Test
    public void when_noGeometry_then_featureWithUnknownType() {
        Map<ResourceField, String> row = singletonMap(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.PLATFORM_ID)
                .create(), "foobar_station");
        String actual = createFeatureBuilder()
                .visit(row)
                .getResult()
                .getFeatureType();
        MatcherAssert.assertThat(actual, is("http://www.opengis.net/def/nil/OGC/0/unknown"));
    }

    @Test
    public void when_rowEntryWithStationId_then_featureHasAppropriateFoiId() {
        ResourceField field = FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.PLATFORM_ID)
                .create();
        field.setQualifier(new ResourceMember("foo", CkanConstants.ResourceType.OBSERVATIONS));
        Map<ResourceField, String> row = singletonMap(field, "foobar_station");
        String actual = createFeatureBuilder()
                .visit(row)
                .getResult()
                .getIdentifier();
        MatcherAssert.assertThat(actual, is(ORGA_NAME + "-foobar_station"));
    }

    @Test
    public void when_rowEntryWithStationName_then_featureHasAppropriateName() {
        Map<ResourceField, String> row = singletonMap(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.PLATFORM_NAME)
                .create(), "foobar_station");
        String actual = createFeatureBuilder()
                .visit(row)
                .getResult()
                .getFirstName().getValue();
        MatcherAssert.assertThat(actual, is("foobar_station"));
    }

    @Test
    public void when_rowEntriesWithLonLatAlt_then_featureHasAppropriateGeometry() {
        Map<ResourceField, String> row = new HashMap<>();
        row.put(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.LATITUDE)
                .withFieldType(CkanConstants.DataType.DOUBLE)
                .withUom("decimal degrees")
                .create(), "52.7");
        row.put(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.LONGITUDE)
                .withFieldType(CkanConstants.DataType.DOUBLE)
                .withUom("decimal degrees")
                .create(), "7.5");
        row.put(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.ALTITUDE)
                .withFieldType(CkanConstants.DataType.DOUBLE)
                .withUom("m")
                .create(), "10");

        Geometry geometry = createFeatureBuilder()
                .visit(row)
                .getResult()
                .getGeometry();

//        final String actual = geometry.toText(); // toText() does not respect altitude
        final String actual = new WKTWriter(3).write(geometry);
        MatcherAssert.assertThat(actual, is("POINT (52.7 7.5 10)"));
    }

    @Test
    public void when_rowEntriesWithGeoJson_then_featureHasAppropriateGeometry() {
        Map<ResourceField, String> row = singletonMap(FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.LOCATION)
                .withFieldType("JsonObject")
                .withUom("geojson")
                .create(), "{'coordinates':[51.05, 13.74],'type':'Point'}");
        Geometry geometry = createFeatureBuilder()
                .visit(row)
                .getResult()
                .getGeometry();
        MatcherAssert.assertThat(geometry.toText(), is("POINT (51.05 13.74)"));
    }

    @Test
    public void when_rowEntriesWithGeoJsonAsPhenomenon_then_featureHasNoGeometry() {
        ResourceField field = FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.LOCATION)
                .withResourceType(CkanConstants.ResourceType.OBSERVATIONS_WITH_GEOMETRIES)
                .withFieldType("JsonObject")
                .create();
        Map<ResourceField, String> row = singletonMap(field, "{'coordinates':[51.05, 13.74],'type':'Point'}");
        Geometry geometry = createFeatureBuilder()
                .visit(row)
                .getResult()
                .getGeometry();
        MatcherAssert.assertThat(geometry, is(CoreMatchers.nullValue(Geometry.class)));
    }

    @Test
    public void when_rowEntriesWithWkt_then_featureHasNoGeometry() {
        ResourceField field = FieldBuilder.aField()
                .withFieldId(CkanConstants.KnownFieldIdValue.LOCATION)
                .withResourceType(CkanConstants.ResourceType.OBSERVATIONS_WITH_GEOMETRIES)
                .create();
        Map<ResourceField, String> row = singletonMap(field, "POINT(51.05 13.74)");
        Geometry geometry = createFeatureBuilder()
                .visit(row)
                .getResult()
                .getGeometry();
        MatcherAssert.assertThat(geometry, is(CoreMatchers.nullValue(Geometry.class)));
    }

    //@Test
    public void when_locationPhenomenonAsJsonObject_then_WHAT_FEATURE_TYPE_SHALL_WE_EXPECT_HERE () {
        // TODO
    }
}
