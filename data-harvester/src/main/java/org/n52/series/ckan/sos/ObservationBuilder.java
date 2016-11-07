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

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.table.ResourceKey;
import org.n52.series.ckan.util.GeometryBuilder;
import org.n52.sos.exception.ows.concrete.DateTimeParseException;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.gml.time.Time.TimeIndeterminateValue;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.values.QuantityValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.vividsolutions.jts.geom.Geometry;

class ObservationBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationBuilder.class);

    private final Entry<ResourceKey, Map<ResourceField, String>> rowEntry;

    private UomParser uomParser;


    ObservationBuilder(Entry<ResourceKey, Map<ResourceField, String>> rowEntry) {
        this(rowEntry, new UcumParser());
    }

    ObservationBuilder(Entry<ResourceKey, Map<ResourceField, String>> rowEntry, UomParser uomParser) {
        this.rowEntry = rowEntry;
        this.uomParser = uomParser;
    }

    SosObservation createObservation(OmObservationConstellation constellation, Phenomenon phenomenon) {
        if (rowEntry == null) {
            return new SosObservation();
        }

        SingleObservationValue< ? > value = null;
        Time time = null;
        TimeInstant validStart = null;
        TimeInstant validEnd = null;

        OmObservation omObservation = new OmObservation();
        omObservation.setObservationConstellation(constellation);
        omObservation.setDefaultElementEncoding(CkanConstants.DEFAULT_CHARSET.toString());
        String observationType = OmConstants.OBS_TYPE_MEASUREMENT; // the default

        final GeometryBuilder geometryBuilder = GeometryBuilder.create();
        for (Map.Entry<ResourceField, String> cells : rowEntry.getValue().entrySet()) {

            ResourceField field = cells.getKey();
            String resourceType = field.getQualifier().getResourceType();
            if ( !resourceType.equalsIgnoreCase(CkanConstants.ResourceType.OBSERVATIONS)) {
                continue;
            }

            if (field.getIndex() == phenomenon.getFieldIdx()) {
                String phenomenonId = constellation.getObservableProperty().getIdentifier();
                omObservation.setIdentifier(rowEntry.getKey().getKeyId() + "_" + phenomenonId);
                // TODO support NO_DATA
                if (field.isOfType(CkanConstants.DataType.DOUBLE)) {
                    value = createQuantityObservationValue(field, cells.getValue());
                } else if (field.isOfType(CkanConstants.DataType.GEOMETRY)){
                    if (field.isOfType("JsonObject")) {
                        geometryBuilder.withGeoJson(cells.getValue());
                    }
                }
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.RESULT_TIME)) {
                time = parseTimestamp(field, cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.LOCATION)) {
                if (field.isOfType(Geometry.class)) {
                    observationType = OmConstants.OBS_TYPE_GEOMETRY_OBSERVATION;
                    geometryBuilder.withGeoJson(cells.getValue());
                }
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.CRS)) {
                geometryBuilder.withCrs(cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.LATITUDE)) {
                geometryBuilder.setLatitude(cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.LONGITUDE)) {
                geometryBuilder.setLongitude(cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.ALTITUDE)) {
                geometryBuilder.setAltitude(cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.VALID_TIME_START)) {
                validStart = parseTimestamp(field, cells.getValue());
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.VALID_TIME_END)) {
                validEnd = parseTimestamp(field, cells.getValue());
            }
        }

        if (validStart != null || validEnd != null) {
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
            omObservation.setValidTime(validTime);
        }

        // TODO support NO_DATA

        if (time == null) {
            LOGGER.debug("ignore observation having no time.");
            return null;
        }

        if (value == null && geometryBuilder.canBuildGeometry()) {
            // construct observation at this stage to allow single lat/lon/alt
            // values to server as fallback geometry observation when no other
            // observation value are present
            SingleObservationValue<Geometry> obsValue = new SingleObservationValue<>();
            obsValue.setValue(geometryBuilder.createGeometryValue());
            observationType = OmConstants.OBS_TYPE_GEOMETRY_OBSERVATION;
            value = obsValue;
        } else {
            if (geometryBuilder.canBuildGeometry()) {
                omObservation.addParameter(geometryBuilder.createNamedValue());
            }
        }

        if (value == null) {
            return null;
        }

        value.setPhenomenonTime(time);
        omObservation.setValue(value);
        return new SosObservation(omObservation, observationType);
    }

    protected TimeInstant parseTimestamp(ResourceField field, String dateValue) {
        return !hasDateFormat(field)
            ? new TimeInstant(new Date(Long.parseLong(dateValue)))
            : parseDateValue(dateValue, parseDateFormat(field));
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
        return field.hasProperty(CkanConstants.FieldPropertyName.DATE_FORMAT);
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
            else if (timeString.contains("+") || Pattern.matches("-\\d", timeString) || timeString.contains("Z")) {
                return ISODateTimeFormat.dateOptionalTimeParser().withOffsetParsed().parseDateTime(timeString);
            }
            else {
                return ISODateTimeFormat.dateOptionalTimeParser().withZone(DateTimeZone.UTC).parseDateTime(timeString);
            }
        }
        catch (final RuntimeException uoe) {
            throw new DateTimeParseException(timeString, uoe);
        }
    }

    private static boolean hasOffsetInfo(String dateValue) {
        return dateValue.endsWith("Z")
                || dateValue.contains("+")
                || Pattern.matches("-\\d", dateValue);
    }

    protected SingleObservationValue<Double> createQuantityObservationValue(ResourceField field, String value) {
        try {
            SingleObservationValue<Double> obsValue = new SingleObservationValue<>();
            if (field.isOfType(Integer.class)
                    || field.isOfType(Float.class)
                    || field.isOfType(Double.class)
                    || field.isOfType(String.class)) {
                QuantityValue quantityValue = new QuantityValue(Double.parseDouble(value));
                quantityValue.setUnit(uomParser.parse(field));
                obsValue.setValue(quantityValue);
                return obsValue;
            }
        }
        catch (Exception e) {
            LOGGER.error("could not parse value {}", value, e);
        }
        return null;
    }

}
