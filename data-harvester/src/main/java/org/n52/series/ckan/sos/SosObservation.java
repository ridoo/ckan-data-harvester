package org.n52.series.ckan.sos;

import org.n52.sos.ogc.om.OmObservation;

public class SosObservation {

    private final String observationType;

    private final OmObservation observation;

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

}
