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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.n52.sos.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.SosOffering;
import org.n52.sos.request.InsertObservationRequest;
import org.n52.sos.request.InsertSensorRequest;

class DataInsertion {

    private final Map<ObservationDiscriminator, SosObservation> observationsByTime;

    private final Set<String> observationTypes;

    private CkanSosObservationReference reference;

    private SensorBuilder sensorBuilder;

    DataInsertion(SensorBuilder sensorBuilder) {
        this.sensorBuilder = sensorBuilder;
        this.observationsByTime = new HashMap<>();
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

    void setReference(CkanSosObservationReference reference) {
        this.reference = reference;
    }

    boolean hasObservationsReference() {
        return reference != null;
    }

    CkanSosObservationReference getObservationsReference() {
        if (hasObservationsReference()) {
            for (SosObservation observation : observationsByTime.values()) {
                OmObservation omObservation = observation.getObservation();
                reference.addObservationReference(omObservation);
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

        observationTypes.add(sosObservation.getObservationType());
        OmObservation observation = sosObservation.getObservation();
        OmObservationConstellation constellation = observation.getObservationConstellation();
        observationsByTime.put(new ObservationDiscriminator(sosObservation), sosObservation);
    }

    boolean hasObservations() {
        return observationsByTime != null && !observationsByTime.isEmpty();
    }

    Set<String> getObservationTypes() {
        return Collections.unmodifiableSet(observationTypes);
    }

    InsertObservationRequest createInsertObservationRequest() throws OwsExceptionReport {
        InsertSensorRequest insertSensorRequest = buildInsertSensorRequest();
        for (SosObservation observation : observationsByTime.values()) {
            /*
             * for observations belonging to a track (mobile platforms) the feature and offering ids are only
             * available after iterating over the observation set. When request is created, we assume that the
             * csv dataset(s) has been parsed all information is valid from that point in time.
             */
            OmObservation omObservation = observation.getObservation();
            OmObservationConstellation constellation = omObservation.getObservationConstellation();
            constellation.setProcedure(insertSensorRequest.getProcedureDescription());
            constellation.setFeatureOfInterest(getFeature());
            constellation.setOfferings(getOfferingIds());
        }
        Collection<SosObservation> observations = observationsByTime.values();
        List<OmObservation> omObservations = observations.stream()
                                                         .map(e -> e.getObservation())
                                                         .collect(Collectors.toList());
        InsertObservationRequest insertObservationRequest = new InsertObservationRequest();
        insertObservationRequest.setOfferings(getOfferingIds());
        insertObservationRequest.setObservation(omObservations);
        return insertObservationRequest;
    }

    @Override
    public String toString() {
        String featureIdentifier = "Feature: '" + getFeature().getIdentifier() + "'";
        String observationCount = "Observations: #" + observationsByTime.size();
        return getClass().getSimpleName() + " [ " + featureIdentifier + ", " + observationCount + "]";
    }

    private static class ObservationDiscriminator {

        private OmObservationConstellation constellation;

        private TimeInstant timestamp;

        ObservationDiscriminator(SosObservation observation) {
            OmObservation omObservation = observation.getObservation();
            this.constellation = omObservation.getObservationConstellation();
            this.timestamp = observation.getPhenomenonTime();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((constellation == null)
                    ? 0
                    : constellation.hashCode());
            result = prime * result + ((timestamp == null)
                    ? 0
                    : timestamp.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ObservationDiscriminator other = (ObservationDiscriminator) obj;
            if (constellation == null) {
                if (other.constellation != null) {
                    return false;
                }
            } else if (!constellation.equals(other.constellation)) {
                return false;
            }
            if (timestamp == null) {
                {
                    if (other.timestamp != null) {
                        return false;
                    }
                }
            } else if (!timestamp.equals(other.timestamp)) {
                return false;
            }
            return true;
        }
    }

}
