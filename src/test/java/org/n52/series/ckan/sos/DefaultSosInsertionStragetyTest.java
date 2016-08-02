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

import java.io.IOException;
import java.net.URISyntaxException;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceFieldCreator;
import org.n52.series.ckan.util.FileBasedCkanHarvestingService;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.values.QuantityValue;
import org.n52.sos.ogc.ows.OwsExceptionReport;

public class DefaultSosInsertionStragetyTest {

    private static final String TEST_DATE_TEMPLATE = "{ "
            + "\"field_id\": \"%s\","
            + "\"date_format\": \"%s\","
            + "\"field_type\": \"Date\""
            + " }";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private ResourceFieldCreator fieldCreator;

    @Before
    public void setUp() {
        this.fieldCreator = new ResourceFieldCreator();
    }

    @Test
    public void testAddZuluWhenDateFormatMissesOffsetInfo() {
        ResourceField field = fieldCreator.createFull(TEST_DATE_TEMPLATE, "my-test-id", "YYYYMMDDhh");
        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
        String format = strategy.parseDateFormat(field);
        MatcherAssert.assertThat(format, CoreMatchers.is("YYYYMMddHHZ"));
    }

    @Test
    public void testDateFormatWithOffsetInfo() {
        ResourceField field = fieldCreator.createFull(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-dd'T'HH:mm:ssz");
        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
        String format = strategy.parseDateFormat(field);
        MatcherAssert.assertThat(format, CoreMatchers.is("YYYY-MM-dd'T'HH:mm:ssz"));
    }

    @Test
    public void testParsingPhenomenonTime() {
        ResourceField field = fieldCreator.createFull(TEST_DATE_TEMPLATE, "my-test-id", "YYYYMMDDhh");
        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
        String format = strategy.parseDateFormat(field);

        // test daylight saving time 2015-03-29 @02:00:00 local
        TimeInstant instant = (TimeInstant)strategy.parseDateValue("2015032902", format);
        final DateTime dateTime = new DateTime("2015-03-29T02:00:00Z");
        Assert.assertTrue("expected: " + instant.getValue().toString()
                + ", actual: " + dateTime, instant.getValue().equals(dateTime));
    }

    @Test
    public void iso8601FormatValueHavingOffset() {
        ResourceField field = fieldCreator.createFull(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-DD'T'hh:mm:ss");
        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
        String format = strategy.parseDateFormat(field);

        TimeInstant instant1 = (TimeInstant)strategy.parseDateValue("2015-03-29T02:00:00+01:00", format);
        final DateTime dateTime1 = new DateTime("2015-03-29T01:00:00Z"); // UTC
        Assert.assertTrue("expected: " + instant1.getValue().toString()
                + ", actual: " + dateTime1, instant1.getValue().equals(dateTime1));
    }

    @Test
    public void iso8601FormatValueNotHavingOffset() {
        ResourceField field = fieldCreator.createFull(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-DD'T'hh:mm:ss");
        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
        String format = strategy.parseDateFormat(field);

        TimeInstant instant2 = (TimeInstant)strategy.parseDateValue("2015-03-29T02:00:00", format);
        final DateTime dateTime2 = new DateTime("2015-03-29T02:00:00Z"); // UTC
        Assert.assertTrue("expected: " + instant2.getValue().toString()
                + ", actual: " + dateTime2, instant2.getValue().equals(dateTime2));
    }

    @Test
    public void iso8601FormatMissingDateFormatDefinition() {
        ResourceField field = fieldCreator.createSimple("my-test-id");
        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
        String format = strategy.parseDateFormat(field);

        TimeInstant instant2 = (TimeInstant)strategy.parseDateValue("2015-03-29T02:00:00", format);
        final DateTime dateTime2 = new DateTime("2015-03-29T02:00:00Z", DateTimeZone.UTC); // UTC
        Assert.assertTrue("expected: " + instant2.getValue().toString()
                + ", actual: " + dateTime2, instant2.getValue().equals(dateTime2));
    }

    // @Test
    public void iso8601FormatWihtoutFractionValueWithFraction() {
        ResourceField field = fieldCreator.createFull(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-dd'T'HH:mm:ssz");
        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
        String format = strategy.parseDateFormat(field);
        Assert.assertNull(strategy.parseDateValue("2016-02-16T07:55:54.188+01:00", format));
    }

    @Test
    public void iso8601FormatFraction() {
        ResourceField field = fieldCreator.createFull(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
        String format = strategy.parseDateFormat(field);
        TimeInstant instant2 = (TimeInstant) strategy.parseDateValue("2016-02-16T08:03:54.609+01:00", format);
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

        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
        DateTime dt = new DateTime();
        TimeInstant instant = new TimeInstant(dt);
        TimeInstant instant2 = strategy.parseTimestamp(field, Long.toString(dt.getMillis()));
        Assert.assertTrue("expected: " + instant2.getValue().getMillis()
                + ", actual: " + instant.getValue().getMillis(), instant2.equals(instant));
    }

//    @Test
    public void parseDegreeCelsiusToUnit() throws Exception {
        final UnitFormat ucum = UnitFormat.getUCUMInstance();
        // using udunits
//        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
//        Unit uom = strategy.parseUom("degree Celsius");
//        Assert.assertTrue(SI.DEGREE_CELSIUS.equals(uom));
        // using uom-ri
        //final Object unit = UnitFormat.getInstance().parseObject("degree celsius");
        ucum.alias(SI.CELSIUS, "degree_celsius");
        SI.CELSIUS.toString();
        final Unit unit = (Unit) ucum.parseObject("degree_celsius");
        Assert.assertThat(unit.toString(), CoreMatchers.is("°C"));
//        System.out.println(ucum.format(ucum.parseObject("degree_celsius")));
    }

    @Test
    public void parseDecimalDegreeToUnit() throws Exception {
        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();

        // using udunits
//        Unit uom = strategy.parseUom("decimal degrees");
//        Assert.assertTrue(SI.DEGREE_CELSIUS.equals(uom));
//        final StandardUnitFormat format = StandardUnitFormat.instance();
//        final Unit unit = format.parse("arc degrees");
//
        // TODO

    }

    @Test
    public void parseStringTypeObservationValue() {
        DefaultSosInsertionStrategy strategy = new DefaultSosInsertionStrategySeam();
        ResourceField field = new ResourceFieldCreator()
                .createFull("{"
                        + " \"field_id\" : \"value\", "
                        + " \"short_name\" : \"value\", "
                        + " \"long_name\" : \"value\","
                        + " \"phenomenon\" : \"temperature\","
                        + " \"uom\" : \"°K\","
                        + " \"field_type\" : \"Float\" }");
        SingleObservationValue<Double> value = strategy.createQuantityObservationValue(field, "25.0");
        Assert.assertNotNull(value);
        Assert.assertThat(value.getValue(), CoreMatchers.instanceOf(QuantityValue.class));
        Assert.assertThat(value.getValue().getValue(), CoreMatchers.is(25.0));
        Assert.assertThat(value.getValue().getUnit(), CoreMatchers.is("°K"));
    }

    @Test
    public void when_insertingWindDWDDatasets_then_getObservationNotEmpty() throws OwsExceptionReport, IOException, URISyntaxException {
        FileBasedCkanHarvestingService service = new FileBasedCkanHarvestingService(testFolder.getRoot());
        SosH2Store sosStore = new SosH2Store(service.getCkanDataCache());
        sosStore.insertDatasetViaStrategy("12c3c8f1-bf44-47e1-b294-b6fc889873fc", new DefaultSosInsertionStrategy());
        sosStore.assertObservationsAvailable();
    }

    class DefaultSosInsertionStrategySeam extends DefaultSosInsertionStrategy {
        DefaultSosInsertionStrategySeam() {
            super(null, null, null, null);
        }
    }

}
