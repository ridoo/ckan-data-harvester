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

import org.n52.sos.ogc.om.OmObservation;

public class SosObservation {

    private final OmObservation observation;

    private final String observationType;

    public SosObservation() {
        this(new OmObservation(), null);
    }

    public SosObservation(OmObservation observation, String observationType) {
        this.observation = observation;
        this.observationType = observationType;
    }

    public String getObservationType() {
        return observationType;
    }

    public OmObservation getObservation() {
        return observation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName())
                .append("[observationType=").append(observationType);
        if (observation != null) {
            sb.append(", observationValue=").append(observation.getValue())
                .append(", observationID=").append(observation.getObservationID())
                .append(", seriesType=").append(observation.getSeriesType())
                .append(", phenomenonTime=").append(observation.getPhenomenonTime())
                .append(", ... ]");
        }
        return sb.toString();
    }
}
