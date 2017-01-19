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

import java.util.Map;
import java.util.Map.Entry;

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.table.ResourceKey;
import org.n52.series.ckan.util.GeometryBuilder;
import org.n52.series.ckan.util.TimeFieldParser;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.gml.time.Time.TimeIndeterminateValue;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.values.QuantityValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

class ObservationBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationBuilder.class);

    private final Entry<ResourceKey, Map<ResourceField, String>> rowEntry;

    private final TimeFieldParser timeFieldParser;

    private SensorBuilder sensorBuilder;

    private UomParser uomParser;

    ObservationBuilder(Entry<ResourceKey, Map<ResourceField, String>> rowEntry) {
        this(rowEntry, new UcumParser());
    }

    ObservationBuilder(Entry<ResourceKey, Map<ResourceField, String>> rowEntry, UomParser uomParser) {
        this.timeFieldParser = new TimeFieldParser();
        this.rowEntry = rowEntry;
        this.uomParser = uomParser;
    }


    ObservationBuilder withSensorBuilder(SensorBuilder insertSensorRequestBuilder) {
        this.sensorBuilder = insertSensorRequestBuilder;
        return this;
    }

    SosObservation createObservation(DataInsertion dataInsertion, Phenomenon phenomenon) {
        if (rowEntry == null) {
            return new SosObservation();
        }

        OmObservationConstellation constellation = dataInsertion.createConstellation(phenomenon);

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
            if ( !field.isObservationField()) {
                continue;
            }

            String normalizedValue = field.normalizeValue(cells.getValue());
            if (field.getIndex() == phenomenon.getFieldIdx()) {
                String phenomenonId = constellation.getObservableProperty().getIdentifier();
                omObservation.setIdentifier(rowEntry.getKey().getKeyId() + "_" + phenomenonId);
                // TODO support NO_DATA
                if (field.isOneOfType(CkanConstants.DataType.QUANTITY)) {
                    value = createQuantityObservationValue(field, normalizedValue);
                } else if (field.isOfType(CkanConstants.DataType.GEOMETRY)) {
                    observationType = OmConstants.OBS_TYPE_GEOMETRY_OBSERVATION;
                    parseGeometryField(geometryBuilder, cells);
                }
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.OBSERVATION_TIME)) {
                time = timeFieldParser.parseTimestamp(normalizedValue, field);
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.LOCATION)) {
                parseGeometryField(geometryBuilder, cells);
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.CRS)) {
                geometryBuilder.withCrs(normalizedValue);
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.LATITUDE)) {
                geometryBuilder.setLatitude(normalizedValue);
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.LONGITUDE)) {
                geometryBuilder.setLongitude(normalizedValue);
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.ALTITUDE)) {
                geometryBuilder.setAltitude(normalizedValue);
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.VALID_TIME_START)) {
                validStart = timeFieldParser.parseTimestamp(normalizedValue, field);
            }
            else if (field.isField(CkanConstants.KnownFieldIdValue.VALID_TIME_END)) {
                validEnd = timeFieldParser.parseTimestamp(normalizedValue, field);
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
            // observation value is present
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
        if (sensorBuilder != null && observationType.equals(OmConstants.OBS_TYPE_GEOMETRY_OBSERVATION)) {
            sensorBuilder.setInsitu(false);
        }
        SosObservation o = new SosObservation(omObservation, observationType);
        LOGGER.trace("Observation: {}", o);
        return o;
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

    private void parseGeometryField(final GeometryBuilder geometryBuilder, Entry<ResourceField, String> cells) {
        ResourceField field = cells.getKey();
        if (field.isOfType("JsonObject")) {
            geometryBuilder.withGeoJson(cells.getValue());
        } else {
            geometryBuilder.withWKT(cells.getValue());
        }
    }


}
