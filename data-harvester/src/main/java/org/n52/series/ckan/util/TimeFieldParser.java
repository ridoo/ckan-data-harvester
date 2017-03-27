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

import java.util.Date;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.sos.exception.ows.concrete.DateTimeParseException;
import org.n52.sos.ogc.gml.time.Time.TimeIndeterminateValue;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class TimeFieldParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeFieldParser.class);
    
    public TimeInstant parseTimestamp(String dateValue, ResourceField field) {
        return isLong(dateValue) && !hasDateFormat(field)
            ? new TimeInstant(new Date(Long.parseLong(dateValue)))
            : parseDateValue(dateValue, parseDateFormat(field));
    }
    
    public class ValidTimeBuilder extends ObservationFieldVisitor<TimePeriod> {

        private TimeInstant validStart;
        
        private TimeInstant validEnd;
        
        @Override
        public void visitObservationField(ResourceField field, String value) {
            if (field.isField(CkanConstants.KnownFieldIdValue.VALID_TIME_START)) {
                validStart = parseTimestamp(value, field);
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.VALID_TIME_END)) {
                validEnd = parseTimestamp(value, field);
            }
        }
    
        @Override
        public boolean hasResult() {
            return validStart == null && validEnd == null;
        }

        @Override
        public TimePeriod getResult() {
            if (validStart == null && validEnd == null) {
                return null;
            }
            TimePeriod validTime;
            if (validStart != null && validEnd == null) {
                validTime = new TimePeriod(validStart, new TimeInstant(TimeIndeterminateValue.unknown));
            }
            else if (validStart == null && validEnd != null) {
                validTime = new TimePeriod(new TimeInstant(TimeIndeterminateValue.unknown), validEnd);
            }
            else {
                validTime = new TimePeriod(validStart, validEnd);
            }
            return validTime;
        }
    }

    public class TimeBuilder extends ObservationFieldVisitor<TimeInstant> {

        private TimeInstant time;
        
        @Override
        public void visitObservationField(ResourceField field, String value) {
            if (field.isField(CkanConstants.KnownFieldIdValue.OBSERVATION_TIME)) {
                time = parseTimestamp(value, field);
            }
        }
    
        @Override
        public boolean hasResult() {
            return time != null;
        }

        @Override
        public TimeInstant getResult() {
            return time;
        }
    }

    private boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected String parseDateFormat(ResourceField field) {
        if (hasDateFormat(field)) {
            String format = field.getOther(CkanConstants.FieldPropertyName.DATE_FORMAT);
            format = ( !format.endsWith("Z") && !format.endsWith("z"))
                ? format + "Z"
                : format;
            return format.replace("DD", "dd").replace("hh", "HH"); // XXX hack to fix wrong format
        }
        return null;
    }

    private boolean hasDateFormat(ResourceField field) {
        return field != null && field.hasProperty(CkanConstants.FieldPropertyName.DATE_FORMAT);
    }

    protected TimeInstant parseDateValue(String dateValue, String dateFormat) {
        try {
            TimeInstant timeInstant = new TimeInstant();
            if ( !hasOffsetInfo(dateValue)) {
                dateValue += "Z";
            }
            DateTime dateTime = parseIsoString2DateTime(dateValue, dateFormat);
            timeInstant.setValue(dateTime);
            return timeInstant;
        }
        catch (Exception ex) {
            if (ex instanceof DateTimeParseException) {
                LOGGER.error("Cannot parse date string {} with format {}", dateValue, dateFormat);
            }
            else {
                LOGGER.error("Cannot parse date string {} with format {}", dateValue, dateFormat, ex);
            }

            return null;
        }

    }

    /**
     * Parses a time String to a Joda Time DateTime object
     *
     * @param timeString
     *        Time String
     * @param format
     *        Format of the time string
     * @return DateTime object
     * @throws DateTimeParseException
     *         If an error occurs.
     */
    protected DateTime parseIsoString2DateTime(final String timeString, String format) throws DateTimeParseException {
        if (Strings.isNullOrEmpty(timeString)) {
            return null;
        }
        try {
            if ( !Strings.isNullOrEmpty(format)) {
                return DateTime.parse(timeString, DateTimeFormat.forPattern(format));
            }
            else if (hasOffset(timeString)) {
                return ISODateTimeFormat.dateOptionalTimeParser().withOffsetParsed().parseDateTime(timeString);
            }
            else if (false /* TODO check, if time_zone field is set and parse to ISO */ ) {
                // add timezone
                return null;
            }
            else {
                return ISODateTimeFormat.dateOptionalTimeParser().withZone(DateTimeZone.UTC).parseDateTime(timeString);
            }
        }
        catch (final RuntimeException uoe) {
            throw new DateTimeParseException(timeString, uoe);
        }
    }

    protected boolean hasOffset(String timestring) {
        return Pattern.matches("^\\.*T\\.*[+-]\\.*$", timestring)
                || timestring.endsWith("z")
                || timestring.endsWith("Z");
    }

    private static boolean hasOffsetInfo(String dateValue) {
        return dateValue.endsWith("Z")
                || dateValue.contains("+")
                || Pattern.matches("-\\d", dateValue);
    }

}
