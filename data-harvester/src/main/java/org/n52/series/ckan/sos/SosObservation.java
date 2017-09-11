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

import org.n52.sos.ogc.gml.time.TimeInstant;
import org.n52.sos.ogc.om.OmObservation;

public class SosObservation {

    private final OmObservation observation;

    private final String observationType;

    private TimeInstant phenomenonTime;

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

    public TimeInstant getPhenomenonTime() {
        return phenomenonTime;
    }

    public void setPhenomenonTime(TimeInstant time) {
        this.phenomenonTime = time;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((observation == null || observation.getIdentifier() == null)
                ? 0
                : observation.getIdentifier()
                             .hashCode());
        result = prime * result + ((phenomenonTime == null)
                ? 0
                : phenomenonTime.hashCode());
        result = prime * result + ((observationType == null)
                ? 0
                : observationType.hashCode());
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
        SosObservation other = (SosObservation) obj;
        if (observation == null) {
            if (other.observation != null) {
                return false;
            }
        } else if (observation.equals(other.observation)) {
            return false;
        }
        if (phenomenonTime == null) {
            if (other.phenomenonTime != null) {
                return false;
            }
        } else if (!phenomenonTime.equals(other.phenomenonTime)) {
            return false;
        }
        if (observationType == null) {
            if (other.observationType != null) {
                return false;
            }
        } else if (!observationType.equals(other.observationType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String simpleName = getClass().getSimpleName();
        StringBuilder sb = new StringBuilder(simpleName).append("[observationType=")
                                                        .append(observationType);
        if (observation != null) {
            sb.append(", observationValue=")
              .append(observation.getValue())
              .append(", observationID=")
              .append(observation.getObservationID())
              .append(", seriesType=")
              .append(observation.getSeriesType())
              .append(", phenomenonTime=")
              .append(observation.getPhenomenonTime())
              .append(", ... ]");
        }
        return sb.toString();
    }

}
