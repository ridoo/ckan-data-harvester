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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.n52.sos.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.SosOffering;
import org.n52.sos.request.InsertObservationRequest;
import org.n52.sos.request.InsertSensorRequest;

class DataInsertion {

    private final List<OmObservation> observations;

    private final Set<String> observationTypes;

    private CkanSosObservationReference reference;

    private SensorBuilder sensorBuilder;

    DataInsertion(SensorBuilder sensorBuilder) {
        this.sensorBuilder = sensorBuilder;
        this.observations = new ArrayList<>();
        this.observationTypes = new HashSet<>();
    }

    public void setSensorBuilder(SensorBuilder sensorBuilder) {
        this.sensorBuilder = sensorBuilder;
    }

    public SensorBuilder getSensorBuilder() {
        return sensorBuilder;
    }

    public InsertSensorRequest buildInsertSensorRequest() {
        return sensorBuilder.build();
    }

    public AbstractFeature getFeature() {
        return sensorBuilder.getFeature();
    }

//    public Set<String> getFeaturesCharacteristics() {
//        return isMovingPlatform() // XXX
//                ? Collections.singleton(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_CURVE)
//                : Collections.singleton(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_FEATURE);
//    }

    void setReference(CkanSosObservationReference reference) {
        this.reference = reference;
    }

    boolean hasObservationsReference() {
        return reference != null;
    }

    CkanSosObservationReference getObservationsReference() {
        if (hasObservationsReference()) {
            for (OmObservation observation : observations) {
                reference.addObservationReference(observation);
            }
        }
        return reference;
    }

    List<String> getOfferingIds() {
        List<String> ids = new ArrayList<>();
        InsertSensorRequest request = buildInsertSensorRequest();
        for (SosOffering offering : request.getAssignedOfferings()) {
            ids.add(offering.getIdentifier());
        }
        return ids;
    }

    void addObservation(SosObservation sosObservation) {
        if (sosObservation == null) {
            return;
        }

        OmObservation observation = sosObservation.getObservation();
        observationTypes.add(sosObservation.getObservationType());
        observations.add(observation);
    }

    boolean hasObservations() {
        return observations != null && !observations.isEmpty();
    }

    Set<String> getObservationTypes() {
        return Collections.unmodifiableSet(observationTypes);
    }

    InsertObservationRequest createInsertObservationRequest() throws OwsExceptionReport {
        InsertSensorRequest insertSensorRequest = buildInsertSensorRequest();
        for (OmObservation observation : observations) {
            /*
             * for observations belonging to a track (mobile platforms) the
             * feature and offering ids are only available after iterating
             * over the observation set. When request is created, we assume
             * that the csv dataset(s) has been parsed all information is
             * valid from that point in time.
             */
            OmObservationConstellation constellation = observation.getObservationConstellation();
            constellation.setProcedure(insertSensorRequest.getProcedureDescription());
            constellation.setFeatureOfInterest(getFeature());
            constellation.setOfferings(getOfferingIds());
        }
        InsertObservationRequest insertObservationRequest = new InsertObservationRequest();
        insertObservationRequest.setOfferings(getOfferingIds());
        insertObservationRequest.setObservation(observations);
        return insertObservationRequest;
    }

    @Override
    public String toString() {
        String featureIdentifier = "Feature: '" + getFeature().getIdentifier() + "'";
        String observationCount = "Observations: #" + observations.size();
        return getClass().getSimpleName() + " [ " + featureIdentifier + ", " + observationCount + "]";
    }

}

