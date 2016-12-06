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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.ResourceKey;
import org.n52.sos.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.om.AbstractPhenomenon;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.request.InsertSensorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;

class StationaryInsertStrategy implements SosInsertStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(StationaryInsertStrategy.class);

    private final CkanSosReferenceCache ckanSosReferenceCache;

    private UomParser uomParser = new UcumParser();

    StationaryInsertStrategy() {
        this(null);
    }

    StationaryInsertStrategy(CkanSosReferenceCache ckanSosReferencingCache) {
        this.ckanSosReferenceCache = ckanSosReferencingCache;
    }

    @Override
    public Map<String, DataInsertion> createDataInsertions(DataTable dataTable, DataCollection dataCollection) {
        PhenomenonParser phenomenonParser = new PhenomenonParser(uomParser);
        List<ResourceField> resourceFields = dataTable.getResourceMember().getResourceFields();
        final List<Phenomenon> phenomena = phenomenonParser.parse(resourceFields);
        LOGGER.debug("Phenomena: {}", phenomena);

        LOGGER.debug("Create stationary insertions ...");
        Map<String, DataInsertion> dataInsertions = new HashMap<>();
        for (Entry<ResourceKey, Map<ResourceField, String>> rowEntry : dataTable.getTable().rowMap().entrySet()) {

            // TODO how and what to create in which order depends on the actual strategy chosen

            CkanDataset dataset = dataCollection.getDataset();
            ResourceMember member = rowEntry.getKey().getMember();
            FeatureBuilder foiBuilder = new FeatureBuilder(dataset);
            AbstractFeature feature = foiBuilder.createFeature(rowEntry.getValue());

            ObservationBuilder observationBuilder = new ObservationBuilder(rowEntry, uomParser);

            for (Phenomenon phenomenon : phenomena) {
                InsertSensorRequestBuilder insertSensorRequestBuilder = InsertSensorRequestBuilder.create(feature, phenomenon)
                        .withDataset(dataset)
                        .setMobile(false);
                String procedureId = insertSensorRequestBuilder.getProcedureId();
                if ( !dataInsertions.containsKey(procedureId)) {
                    LOGGER.debug("Building InsertSensorRequest with: procedure '{}', phenomenon '{}' (unit '{}')",
                                 procedureId,
                                 phenomenon.getLabel(),
                                 phenomenon.getUom());
                    DataInsertion dataInsertion = new DataInsertion(insertSensorRequestBuilder);
                    dataInsertions.put(procedureId, dataInsertion);

                    if (ckanSosReferenceCache != null) {
                        DataFile dataFile = dataCollection.getDataFile(member);
                        CkanResource resource = dataFile.getResource();
                        dataInsertion.setReference(CkanSosObservationReference.create(resource));
                    }
                }

                DataInsertion dataInsertion = dataInsertions.get(procedureId);
                InsertSensorRequest insertSensorRequest = dataInsertion.getRequest();
                List<String> offerings = dataInsertion.getOfferingIds();
                OmObservationConstellation constellation = new OmObservationConstellation();
                constellation.setObservableProperty(createPhenomenon(phenomenon));
                constellation.setFeatureOfInterest(dataInsertion.getFeature());
                constellation.setOfferings(offerings);
                constellation.setObservationType(OmConstants.OBS_TYPE_MEASUREMENT);
                constellation.setProcedure(insertSensorRequest.getProcedureDescription());
                observationBuilder.setInsertSensorRequestBuilder(insertSensorRequestBuilder);
                final SosObservation observation = observationBuilder.createObservation(constellation, phenomenon);
                if (observation != null) {
                    dataInsertion.addObservation(observation);
                }
            }
        }
        return dataInsertions;
    }

    private AbstractPhenomenon createPhenomenon(Phenomenon phenomenon) {
        return new OmObservableProperty(phenomenon.getId());
    }

}
