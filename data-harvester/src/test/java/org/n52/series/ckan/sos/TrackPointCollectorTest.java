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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.n52.series.ckan.beans.FieldBuilder;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanMapping;
import org.n52.series.ckan.sos.TrackPointCollector.TrackPoint;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class TrackPointCollectorTest {

    @Test
    public void when_simpleCreate_then_noException() {
        new TrackPointCollector();
    }

    @Test
    public void when_simpleCreate_then_timestampIsDefaultTrackId() throws ParseException {
        ResourceField timeField = createField("timestamp", "Date");
        TrackPointCollector collector = new TrackPointCollector();
        TrackPoint trackPoint = collector.newTrackPoint()
                .withGeometry(new WKTReader().read("POINT (52 7)"))
                .withProperty(timeField, "2012-07-21T12:00:00Z");

        String trackId = collector.addToTrack(trackPoint);
        MatcherAssert.assertThat(trackId, is(collector.getTrackId(trackPoint)));
    }

    @Test
    public void when_trackPointWithoutTrackId_then_featureIdTimestamp() throws ParseException {
        ResourceField timeField = createField("timestamp", "Date");
        TrackPointCollector collector = new TrackPointCollector();
        TrackPoint trackPoint = collector.newTrackPoint()
                .withProperty(timeField, "2012-07-21T12:00:00Z");
        SamplingFeature actual = collector.createFeature(trackPoint);
        MatcherAssert.assertThat(actual, notNullValue());
        MatcherAssert.assertThat(DateTime.parse(actual.getIdentifier()),
                is(DateTime.parse("2012-07-21T12:00:00Z")));
    }

    @Test
    public void when_trackPointWithoutTrackId_then_featureHasTimestampAsId() throws ParseException {
        TrackPointCollector collector = new TrackPointCollector();
        TrackPoint trackPoint = collector.newTrackPoint();
        MatcherAssert.assertThat(trackPoint.getTimestamp(), notNullValue());
        MatcherAssert.assertThat(trackPoint.getTimestamp().getValue()
                , is(new DateTime(0, DateTimeZone.UTC)));
    }

    @Test
    public void when_createWithTrackConfigSingleColumnAndPattern_then_extractTrackId() throws IOException, ParseException {
        ResourceField timeField = createField("timestamp", "Date");
        CkanMapping ckanMapping = getCkanMapping("{"
                + "  \"strategy\": {"
                + "    \"mobile\": {"
                + "      \"track_discriminator\": [{"
                + "        \"column\": \"timestamp\","
                + "        \"pattern\": \"(\\\\d{4}-\\\\d{2}-\\\\d{2}).*$\""
                + "      }]"
                + "    }"
                + "  }"
                + "}");
        TrackPointCollector collector = new TrackPointCollector(ckanMapping);
        TrackPoint trackPoint = collector.newTrackPoint()
                .withProperty(timeField, "2012-07-21T12:00:00Z");

        String trackId = collector.addToTrack(trackPoint);
        MatcherAssert.assertThat(trackId, is("2012-07-21"));
    }

    @Test
    public void when_createWithTrackConfigMultipleColumnsAndPattern_then_extractTrackId() throws IOException, ParseException {
        ResourceField timeField = createField("timestamp", "Date");
        ResourceField foobarField = createField("foobar", "String");
        CkanMapping ckanMapping = getCkanMapping("{"
                + "  \"strategy\": {"
                + "    \"mobile\": {"
                + "      \"track_discriminator\": [{"
                + "        \"column\": \"timestamp\","
                + "        \"pattern\": \"(\\\\d{4}-\\\\d{2}-\\\\d{2}).*$\""
                + "      },"
                + "      {"
                + "        \"column\": \"foobar\","
                + "        \"separator\": \"_\""
                + "      }]"
                + "    }"
                + "  }"
                + "}");
        TrackPointCollector collector = new TrackPointCollector(ckanMapping);
        TrackPoint trackPoint = collector.newTrackPoint()
                .withProperty(foobarField, "fooValue")
                .withProperty(timeField, "2012-07-21T12:00:00Z");

        String trackId = collector.addToTrack(trackPoint);
        MatcherAssert.assertThat(trackId, is("2012-07-21_fooValue"));
    }

    private CkanMapping getCkanMapping(String json) throws IOException {
        return new CkanMapping(new ObjectMapper().readTree(json));
    }

    private ResourceField createField(String id, String type) {
        return FieldBuilder.aField()
                .withFieldId(id)
                .withFieldType(type)
                .create();
    }

}
