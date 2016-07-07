/**
 * Copyright (C) 2013-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.series.ckan.sos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.n52.sos.ogc.om.OmObservation;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.trentorise.opendata.jackan.model.CkanResource;


public class CkanSosObservationReference implements Serializable {

    private static final long serialVersionUID = 3444072630244881068L;

    private final SerializableCkanResource resource;

    private final List<String> observationIdentifiers;

    public CkanSosObservationReference(CkanResource resource) throws JsonProcessingException {
        this.resource = new SerializableCkanResource(resource);
        this.observationIdentifiers = new ArrayList<>();
    }

    public void addObservationReference(OmObservation observation) {
        if (observation != null) {
            observationIdentifiers.add(observation.getIdentifier());
        }
    }

    public Iterable<String> getObservationIdentifiers() {
        return observationIdentifiers;
    }

    public boolean hasReference(OmObservation observation) {
        return observation != null
                ? hasReference(observation.getIdentifier())
                : false;
    }

    public boolean hasReference(String identifier) {
        return identifier != null
                ? hasReference(identifier)
                : false;
    }

    public SerializableCkanResource getResource() {
        return resource;
    }

}
