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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.n52.series.ckan.beans.FieldBuilder;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.values.QuantityValue;

public class ObservationBuilderTest {

    private static final String TEST_DATE_TEMPLATE = "{ "
            + "\"field_id\": \"%s\","
            + "\"date_format\": \"%s\","
            + "\"field_type\": \"Date\""
            + " }";

    private FieldBuilder fieldCreator;

    @Before
    public void setUp() {
        this.fieldCreator = new FieldBuilder();
    }

    @Test
    public void testAddZuluWhenDateFormatMissesOffsetInfo() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYYMMDDhh");
        ObservationBuilder builder = new ObservationBuilder(null);
        String format = builder.parseDateFormat(field);
        MatcherAssert.assertThat(format, CoreMatchers.is("YYYYMMddHHZ"));
    }

    @Test
    public void testDateFormatWithOffsetInfo() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-dd'T'HH:mm:ssz");
        ObservationBuilder builder = new ObservationBuilder(null);
        String format = builder.parseDateFormat(field);
        MatcherAssert.assertThat(format, CoreMatchers.is("YYYY-MM-dd'T'HH:mm:ssz"));
    }

    @Test
    public void testParsingPhenomenonTime() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYYMMDDhh");
        ObservationBuilder builder = new ObservationBuilder(null);
        String format = builder.parseDateFormat(field);

        // test daylight saving time 2015-03-29 @02:00:00 local
        TimeInstant instant = (TimeInstant)builder.parseDateValue("2015032902", format);
        final DateTime dateTime = new DateTime("2015-03-29T02:00:00Z");
        Assert.assertTrue("expected: " + instant.getValue().toString()
                + ", actual: " + dateTime, instant.getValue().equals(dateTime));
    }

    @Test
    public void iso8601FormatValueHavingOffset() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-DD'T'hh:mm:ss");
        ObservationBuilder builder = new ObservationBuilder(null);
        String format = builder.parseDateFormat(field);

        TimeInstant instant1 = (TimeInstant)builder.parseDateValue("2015-03-29T02:00:00+01:00", format);
        final DateTime dateTime1 = new DateTime("2015-03-29T01:00:00Z"); // UTC
        Assert.assertTrue("expected: " + instant1.getValue().toString()
                + ", actual: " + dateTime1, instant1.getValue().equals(dateTime1));
    }

    @Test
    public void iso8601FormatValueNotHavingOffset() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-DD'T'hh:mm:ss");
        ObservationBuilder builder = new ObservationBuilder(null);
        String format = builder.parseDateFormat(field);

        TimeInstant instant2 = (TimeInstant)builder.parseDateValue("2015-03-29T02:00:00", format);
        final DateTime dateTime2 = new DateTime("2015-03-29T02:00:00Z"); // UTC
        Assert.assertTrue("expected: " + instant2.getValue().toString()
                + ", actual: " + dateTime2, instant2.getValue().equals(dateTime2));
    }

    @Test
    public void when_missingDateFormatAndMissingOffset_then_parseToUTC() {
        ResourceField field = fieldCreator.createSimple("my-test-id");
        ObservationBuilder builder = new ObservationBuilder(null);
        String format = builder.parseDateFormat(field);

        TimeInstant instant2 = (TimeInstant)builder.parseDateValue("2015-03-29T02:00:00", format);
        final DateTime dateTime2 = new DateTime("2015-03-29T02:00:00Z", DateTimeZone.UTC); // UTC
        Assert.assertTrue("expected: " + instant2.getValue().toString()  + ", actual: " + dateTime2,
                instant2.getValue().equals(dateTime2));
    }

    @Test
    public void when_timeStringEndsWithZulu_then_detectOffset() {
        ObservationBuilder builder = new ObservationBuilder(null);
        Assert.assertTrue(builder.hasOffset("2015-03-29T02:00:00Z"));
    }

    // @Test
    public void iso8601FormatWihtoutFractionValueWithFraction() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-dd'T'HH:mm:ssz");
        ObservationBuilder builder = new ObservationBuilder(null);
        String format = builder.parseDateFormat(field);
        Assert.assertNull(builder.parseDateValue("2016-02-16T07:55:54.188+01:00", format));
    }

    @Test
    public void iso8601FormatFraction() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        ObservationBuilder builder = new ObservationBuilder(null);
        String format = builder.parseDateFormat(field);
        TimeInstant instant2 = (TimeInstant) builder.parseDateValue("2016-02-16T08:03:54.609+01:00", format);
        final DateTime dateTime2 = new DateTime("2016-02-16T08:03:54.609+01:00"); // UTC
        Assert.assertTrue("expected: " + instant2.getValue().toString()
                + ", actual: " + dateTime2, instant2.getValue().equals(dateTime2));
    }

    @Test
    public void dateInMillis() {
//        ResourceField field = new ResourceFieldTypeSeam("my-test-id", "Date");
        ResourceField field = fieldCreator.createSimple("my-test-id");

        // XXX parseTime only relevant for valid_time_start/end ?! but not available in
        // current schema descriptors at all! Clear, how to handle millis in resource
        // fields .. perhaps considering a 'millis' keyword in date_format

        ObservationBuilder builder = new ObservationBuilder(null);
        DateTime dt = new DateTime();
        TimeInstant instant = new TimeInstant(dt);
        TimeInstant instant2 = builder.parseTimestamp(field, Long.toString(dt.getMillis()));
        Assert.assertTrue("expected: " + instant2.getValue().getMillis()
                + ", actual: " + instant.getValue().getMillis(), instant2.equals(instant));
    }

    @Test
    public void when_floatTypedValue_then_createQuantityObservation() {
        ObservationBuilder builder = new ObservationBuilder(null);
        ResourceField field = new FieldBuilder()
                .createViaTemplate("{"
                        + " \"field_id\" : \"value\", "
                        + " \"short_name\" : \"value\", "
                        + " \"long_name\" : \"value\","
                        + " \"phenomenon\" : \"temperature\","
                        + " \"uom\" : \"°K\","
                        + " \"field_type\" : \"Float\" }");
        SingleObservationValue<Double> value = builder.createQuantityObservationValue(field, "25.0");
        Assert.assertNotNull(value);
        Assert.assertThat(value.getValue(), CoreMatchers.instanceOf(QuantityValue.class));
        Assert.assertThat(value.getValue().getValue(), CoreMatchers.is(25.0));
        Assert.assertThat(value.getValue().getUnit(), CoreMatchers.is("°K"));
    }

}
