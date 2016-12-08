package org.n52.series.ckan.util;

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

public class TimeFieldParserTest {

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

//    @Test
//    public void timeStrippedOfWhenApplyRegexPattern() {
//        Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}).*$");
//        Matcher matcher = pattern.matcher("2015-03-29T02:00:00+01:00");
//        MatcherAssert.assertThat(matcher.matches(), is(true));
//        matcher.toMatchResult().group(1);
//        MatcherAssert.assertThat(matcher.group(1), is("2015-03-29"));
//    }

    @Test
    public void testAddZuluWhenDateFormatMissesOffsetInfo() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYYMMDDhh");
        TimeFieldParser parser = new TimeFieldParser();
        String format = parser.parseDateFormat(field);
        MatcherAssert.assertThat(format, CoreMatchers.is("YYYYMMddHHZ"));
    }

    @Test
    public void testDateFormatWithOffsetInfo() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-dd'T'HH:mm:ssz");
        TimeFieldParser parser = new TimeFieldParser();
        String format = parser.parseDateFormat(field);
        MatcherAssert.assertThat(format, CoreMatchers.is("YYYY-MM-dd'T'HH:mm:ssz"));
    }

    @Test
    public void testParsingPhenomenonTime() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYYMMDDhh");
        TimeFieldParser parser = new TimeFieldParser();
        String format = parser.parseDateFormat(field);

        // test daylight saving time 2015-03-29 @02:00:00 local
        TimeInstant instant = (TimeInstant)parser.parseDateValue("2015032902", format);
        final DateTime dateTime = new DateTime("2015-03-29T02:00:00Z");
        Assert.assertTrue("expected: " + instant.getValue().toString()
                + ", actual: " + dateTime, instant.getValue().equals(dateTime));
    }

    @Test
    public void iso8601FormatValueHavingOffset() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-DD'T'hh:mm:ss");
        TimeFieldParser parser = new TimeFieldParser();
        String format = parser.parseDateFormat(field);

        TimeInstant instant1 = (TimeInstant)parser.parseDateValue("2015-03-29T02:00:00+01:00", format);
        final DateTime dateTime1 = new DateTime("2015-03-29T01:00:00Z"); // UTC
        Assert.assertTrue("expected: " + instant1.getValue().toString()
                + ", actual: " + dateTime1, instant1.getValue().equals(dateTime1));
    }

    @Test
    public void iso8601FormatValueNotHavingOffset() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-DD'T'hh:mm:ss");
        TimeFieldParser parser = new TimeFieldParser();
        String format = parser.parseDateFormat(field);

        TimeInstant instant2 = (TimeInstant)parser.parseDateValue("2015-03-29T02:00:00", format);
        final DateTime dateTime2 = new DateTime("2015-03-29T02:00:00Z"); // UTC
        Assert.assertTrue("expected: " + instant2.getValue().toString()
                + ", actual: " + dateTime2, instant2.getValue().equals(dateTime2));
    }

    @Test
    public void when_missingDateFormatAndMissingOffset_then_parseToUTC() {
        ResourceField field = fieldCreator.createSimple("my-test-id");
        TimeFieldParser parser = new TimeFieldParser();
        String format = parser.parseDateFormat(field);

        TimeInstant instant2 = (TimeInstant)parser.parseDateValue("2015-03-29T02:00:00", format);
        final DateTime dateTime2 = new DateTime("2015-03-29T02:00:00Z", DateTimeZone.UTC); // UTC
        Assert.assertTrue("expected: " + instant2.getValue().toString()  + ", actual: " + dateTime2,
                instant2.getValue().equals(dateTime2));
    }

    @Test
    public void when_timeStringEndsWithZulu_then_detectOffset() {
        TimeFieldParser parser = new TimeFieldParser();
        Assert.assertTrue(parser.hasOffset("2015-03-29T02:00:00Z"));
    }

    // @Test
    public void iso8601FormatWihtoutFractionValueWithFraction() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-dd'T'HH:mm:ssz");
        TimeFieldParser parser = new TimeFieldParser();
        String format = parser.parseDateFormat(field);
        Assert.assertNull(parser.parseDateValue("2016-02-16T07:55:54.188+01:00", format));
    }

    @Test
    public void iso8601FormatFraction() {
        ResourceField field = fieldCreator.createViaTemplate(TEST_DATE_TEMPLATE, "my-test-id", "YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        TimeFieldParser parser = new TimeFieldParser();
        String format = parser.parseDateFormat(field);
        TimeInstant instant2 = (TimeInstant) parser.parseDateValue("2016-02-16T08:03:54.609+01:00", format);
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

        TimeFieldParser parser = new TimeFieldParser();
        DateTime dt = new DateTime();
        TimeInstant instant = new TimeInstant(dt);
        TimeInstant instant2 = parser.parseTimestamp(Long.toString(dt.getMillis()), field);
        Assert.assertTrue("expected: " + instant2.getValue().getMillis()
                + ", actual: " + instant.getValue().getMillis(), instant2.equals(instant));
    }

}
