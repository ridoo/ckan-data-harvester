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

import static org.n52.series.ckan.da.CkanConstants.DEFAULT_CHARSET;

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.table.ResourceKey;
import org.n52.series.ckan.util.AbstractRowVisitor;
import org.n52.series.ckan.util.GeometryBuilder;
import org.n52.series.ckan.util.ObservationFieldVisitor;
import org.n52.series.ckan.util.TimeFieldParser;
import org.n52.sos.ogc.gml.ReferenceType;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.om.NamedValue;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.values.QuantityValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

class ObservationBuilder extends AbstractRowVisitor<SosObservation> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationBuilder.class);

    private static final String DEFAULT_OBSERVATION_TYPE = OmConstants.OBS_TYPE_MEASUREMENT;

    private final SingleObservationValueBuilder observationValueBuilder = new SingleObservationValueBuilder();

    private final TimeFieldParser timeFieldParser = new TimeFieldParser();

    private final TimeFieldParser.ValidTimeBuilder validTimeBuilder = timeFieldParser.new ValidTimeBuilder();

    private final TimeFieldParser.TimeBuilder timeBuilder = timeFieldParser.new TimeBuilder();

    private final GeometryBuilder geometryBuilder = GeometryBuilder.create();

    private final OmObservation omObservation;

    private final Phenomenon phenomenon;

    private final ResourceKey qualifier;

    private String observationType = DEFAULT_OBSERVATION_TYPE;

    private UomParser uomParser = new UcumParser();

    private SensorBuilder sensorBuilder;

    public static ObservationBuilder create(Phenomenon phenomenon, ResourceKey qualifier) {
        return new ObservationBuilder(phenomenon, qualifier);
    }

    public ObservationBuilder withUomParser(UomParser uomParser) {
        this.uomParser = uomParser;
        return this;
    }

    private ObservationBuilder(Phenomenon phenomenon, ResourceKey qualifier) {
        this.phenomenon = phenomenon;
        this.qualifier = qualifier;

        this.omObservation = new OmObservation();
    }

    ObservationBuilder withSensorBuilder(SensorBuilder insertSensorRequestBuilder) {
        this.sensorBuilder = insertSensorRequestBuilder;
        return this;
    }

    @Override
    public void visit(ResourceField field, String value) {
        if ( !field.isObservationField()) {
            return;
        }
        field.accept(observationValueBuilder, value);
        field.accept(geometryBuilder, value);
        field.accept(validTimeBuilder, value);
        field.accept(timeBuilder, value);
    }

    @Override
    public boolean hasResult() {
        if (timeBuilder.hasResult()) {
            LOGGER.debug("ignore observation having no time.");
            return false;
        } else if ( !observationValueBuilder.hasResult()
                && !geometryBuilder.hasResult()) {
            LOGGER.debug("no value or geometry present to create obseration.");
            return false;
        }
        return true;
    }

    @Override
    public SosObservation getResult() {
        SingleObservationValue< ? > result = observationValueBuilder.hasResult()
                ? observationValueBuilder.getResult()
                : null;
        if ( !observationValueBuilder.hasResult() && geometryBuilder.hasResult()) {
            // pure geometry observation w/o any other value
            observationType = OmConstants.OBS_TYPE_GEOMETRY_OBSERVATION;
            SingleObservationValue<Geometry> obsValue = new SingleObservationValue<>();
            obsValue.setValue(geometryBuilder.getResult());
            result = obsValue;
        } if (geometryBuilder.hasResult()) {
            // geometry is meta info of an actual observation value
            final NamedValue<Geometry> namedValue = new NamedValue<>();
            namedValue.setName(new ReferenceType(OmConstants.PARAM_NAME_SAMPLING_GEOMETRY));
            namedValue.setValue(geometryBuilder.getResult());
            omObservation.addParameter(namedValue);
        }

        if (validTimeBuilder.hasResult()) {
            TimePeriod validTime = validTimeBuilder.getResult();
            omObservation.setValidTime(validTime);
        }

        Time time = timeBuilder.getResult();
        result.setPhenomenonTime(time);
        omObservation.setValue(result);

        if (sensorBuilder != null && isGeometryObservation()) {
            sensorBuilder.setInsitu(false);
        }

        omObservation.setDefaultElementEncoding(DEFAULT_CHARSET.toString());
        omObservation.setObservationConstellation(createConstellation(phenomenon));
        SosObservation o = new SosObservation(omObservation, observationType);
        LOGGER.trace("Observation: {}", o);
        return o;
    }

    private boolean isGeometryObservation() {
        return observationType.equals(OmConstants.OBS_TYPE_GEOMETRY_OBSERVATION);
    }

    private OmObservationConstellation createConstellation(Phenomenon phenomenon) {
        OmObservationConstellation constellation = new OmObservationConstellation();
        constellation.setObservableProperty(phenomenon.toObservableProperty());
        constellation.setObservationType(OmConstants.OBS_TYPE_MEASUREMENT);
        return constellation;
    }

    protected class SingleObservationValueBuilder extends ObservationFieldVisitor<SingleObservationValue< ? >> {

        private SingleObservationValue< ? > result;

        @Override
        public void visitObservationField(ResourceField field, String value) {
            if (field.matchesIndex(phenomenon.getFieldIdx())) {
                String phenomenonId = phenomenon.getId();
                String omIdentifier = qualifier.getKeyId() + "_" + phenomenonId;
                omObservation.setIdentifier(omIdentifier);
                // TODO support NO_DATA
                if (field.isOneOfType(CkanConstants.DataType.QUANTITY)) {
                    result = createQuantityValue(field, value);
                } else if (field.isOfType(CkanConstants.DataType.GEOMETRY)) {
                    observationType = OmConstants.OBS_TYPE_GEOMETRY_OBSERVATION;
                }
            }
        }

        protected SingleObservationValue<Double> createQuantityValue(ResourceField field, String value) {
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

        @Override
        public boolean hasResult() {
            return result != null;
        }

        @Override
        public SingleObservationValue<?> getResult() {
            return result;
        }
    }

}
