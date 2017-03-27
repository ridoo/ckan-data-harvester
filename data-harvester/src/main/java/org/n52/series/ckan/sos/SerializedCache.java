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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;

public class SerializedCache implements CkanSosReferenceCache, Serializable {

    private static final long serialVersionUID = 1742116877657459353L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializedCache.class);

    private final Map<SerializableCkanResource, CkanSosObservationReference> observationReferenceCache;

    private final Map<String, SerializableCkanDataset> datasets;

    public SerializedCache() {
        observationReferenceCache = new HashMap<>();
        datasets = new HashMap<>();
    }

    @Override
    public void addOrUpdate(CkanSosObservationReference reference) {
        observationReferenceCache.put(reference.getResource(), reference);
    }

    @Override
    public void delete(CkanSosObservationReference reference) {
        observationReferenceCache.remove(reference.getResource());
    }

    @Override
    public void delete(CkanResource resource) {
        try {
            observationReferenceCache.remove(serialize(resource));
        } catch (JsonProcessingException e) {
            LOGGER.error("Invalid CkanResource.", e);
        }
    }

    @Override
    public boolean exists(CkanResource reference) {
        try {
            SerializableCkanResource resource = serialize(reference);
            return observationReferenceCache.containsKey(resource);
        } catch (JsonProcessingException e) {
            LOGGER.debug("Invalid resource.", e);
            return false;
        }
    }

    @Override
    public CkanSosObservationReference getReference(CkanResource resource) {
        try {
            return observationReferenceCache.get(serialize(resource));
        } catch (JsonProcessingException e) {
            LOGGER.debug("Invalid resource.", e);
            return null;
        }
    }

    public Map<CkanResource, CkanSosObservationReference> getObservationReferenceCache() {
        return deserializeObservationReferences();
    }

    public Map<String, CkanDataset> getDatasets() {
        return deserializeDatasets();
    }

    public void setDatasets(Iterable<CkanDataset> datasets) {
        this.datasets.clear();
        for (CkanDataset dataset : datasets) {
            addDataset(dataset);
        }
    }

    private void addDataset(CkanDataset dataset) {
        try {
            this.datasets.put(dataset.getId(), serialize(dataset));
        } catch(JsonProcessingException e) {
            LOGGER.debug("Could not serialize dataset.", e);
        }
    }

    private SerializableCkanResource serialize(CkanResource resource) throws JsonProcessingException {
        return new SerializableCkanResource(resource);
    }

    private SerializableCkanDataset serialize(CkanDataset dataset) throws JsonProcessingException {
        return new SerializableCkanDataset(dataset);
    }

    private Map<String, CkanDataset> deserializeDatasets() {
        Map<String, CkanDataset> ckanDatasets = new HashMap<>();
        for (Map.Entry<String, SerializableCkanDataset> entry : datasets.entrySet()) {
            final CkanDataset ckanDataset = deserialize(entry.getValue());
            if (ckanDataset != null) {
                ckanDatasets.put(entry.getKey(), ckanDataset);
            }
        }
        return ckanDatasets;
    }

    private CkanDataset deserialize(SerializableCkanDataset dataset) {
        try {
            return dataset.getCkanDataset();
        } catch (IOException e) {
            LOGGER.error("Unable to deserialize broken dataset.", e);
            return null;
        }
    }

    private Map<CkanResource, CkanSosObservationReference> deserializeObservationReferences() {
        Map<CkanResource, CkanSosObservationReference> observationReferences = new HashMap<>();
        for (Map.Entry<SerializableCkanResource, CkanSosObservationReference> entry : observationReferenceCache.entrySet()) {
            final CkanResource ckanResource = deserialize(entry.getKey());
            if (ckanResource != null) {
                observationReferences.put(ckanResource, entry.getValue());
            }
        }
        return observationReferences;
    }

    private CkanResource deserialize(SerializableCkanResource resource) {
        try {
            return resource.getCkanResource();
        } catch (IOException e) {
            LOGGER.error("Unable to deserialize broken resource.", e);
            return null;
        }
    }
}
