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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.ResourceKey;
import org.n52.series.ckan.table.ResourceTable;
import org.n52.sos.ds.hibernate.InsertObservationDAO;
import org.n52.sos.ds.hibernate.InsertSensorDAO;
import org.n52.sos.ext.deleteobservation.DeleteObservationConstants;
import org.n52.sos.ext.deleteobservation.DeleteObservationDAO;
import org.n52.sos.ext.deleteobservation.DeleteObservationRequest;
import org.n52.sos.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.om.AbstractPhenomenon;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.SosInsertionMetadata;
import org.n52.sos.request.InsertSensorRequest;
import org.n52.sos.service.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;
import org.n52.series.ckan.da.CkanConstants;

class DefaultSosInsertionStrategy implements SosInsertionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSosInsertionStrategy.class);

    private final InsertSensorDAO insertSensorDao;

    private final InsertObservationDAO insertObservationDao;

    private final DeleteObservationDAO deleteObservationDao;

    private final CkanSosReferenceCache ckanSosReferencingCache;

    private UomParser uomParser = new UcumParser();

    DefaultSosInsertionStrategy() {
        this(null);
    }

    DefaultSosInsertionStrategy(CkanSosReferenceCache ckanSosReferenceCache) {
        this(new InsertSensorDAO(), new InsertObservationDAO(), new DeleteObservationDAO(), ckanSosReferenceCache);
    }

    DefaultSosInsertionStrategy(InsertSensorDAO insertSensorDao,
                                InsertObservationDAO insertObservationDao,
                                DeleteObservationDAO deleteObservationDao,
                                CkanSosReferenceCache ckanSosReferencingCache) {
        this.insertSensorDao = insertSensorDao;
        this.insertObservationDao = insertObservationDao;
        this.deleteObservationDao = deleteObservationDao;
        this.ckanSosReferencingCache = ckanSosReferencingCache;
    }

    @Override
    public void insertOrUpdate(DataCollection dataCollection) {
        try {
            DataTable fullTable = loadData(dataCollection);
            if (insertOrUpdateData(fullTable, dataCollection)) {
                // Trigger SOS Capabilities cache reloading after insertion
                Configurator.getInstance().getCacheController().update();
            }
        }
        catch (OwsExceptionReport e) {
            LOGGER.warn("Error while reloading SOS Capabilities cache", e);
        }
    }

    private DataTable loadData(DataCollection dataCollection) {
        CkanDataset dataset = dataCollection.getDataset();
        LOGGER.debug("load data for dataset '{}'", dataset.getName());
        DataTable fullTable = new ResourceTable();

        // TODO write test for it
        // TODO if dataset is newer than in cache -> set flag to re-insert whole datacollection

        Map<String, List<ResourceMember>> resourceMembersByType = dataCollection.getResourceMembersByType();
        for (List<ResourceMember> membersWithCommonResourceTypes : resourceMembersByType.values()) {
            DataTable dataTable = new ResourceTable();
            for (ResourceMember member : membersWithCommonResourceTypes) {

                // TODO write test for it

                DataFile dataFile = dataCollection.getDataFile(member);
                CkanResource resource = dataFile.getResource();
                if (isUpdateNeeded(resource, dataFile)) {
                    ResourceTable singleDatatable = new ResourceTable(dataCollection.getDataEntry(member));
                    singleDatatable.readIntoMemory();
                    LOGGER.debug("Extend table with: '{}'", singleDatatable);
                    dataTable = dataTable.extendWith(singleDatatable);
                }
            }
            String resourceType = membersWithCommonResourceTypes.get(0).getResourceType();
            LOGGER.debug("Fully extended table for resource '{}': '{}'", resourceType, dataTable);
            fullTable = fullTable.innerJoin(dataTable);
        }
        LOGGER.debug("Fully joined table: '{}'", fullTable);
        return fullTable;
    }


    boolean insertOrUpdateData(DataTable dataTable, DataCollection dataCollection) {
        boolean dataInserted = false;
        PhenomenonParser phenomenonParser = new PhenomenonParser(uomParser);
        List<ResourceField> resourceFields = dataTable.getResourceMember().getResourceFields();
        final List<Phenomenon> phenomena = phenomenonParser.parse(resourceFields);
        LOGGER.debug("Phenomena: {}", phenomena);

        LOGGER.debug("Start insertion ...");
        Map<String, DataInsertion> dataInsertions = new HashMap<>();
        for (Entry<ResourceKey, Map<ResourceField, String>> rowEntry : dataTable.getTable().rowMap().entrySet()) {

            // TODO how and what to create in which order depends on the actual strategy chosen

            CkanDataset dataset = dataCollection.getDataset();
            ResourceMember member = rowEntry.getKey().getMember();
            FeatureBuilder foiBuilder = new FeatureBuilder(dataset, member.getResourceType());
            AbstractFeature feature = foiBuilder.createFeature(rowEntry.getValue());

            ObservationBuilder observationBuilder = new ObservationBuilder(rowEntry, uomParser);

            for (Phenomenon phenomenon : phenomena) {
                InsertSensorRequestBuilder insertSensorRequestBuilder = InsertSensorRequestBuilder.create(feature, phenomenon)
                        .setMobile(member.isOfType(CkanConstants.ResourceType.OBSERVATIONS_WITH_GEOMETRIES))
                        .withDataset(dataset);
                String procedureId = insertSensorRequestBuilder.getProcedureId();
                if ( !dataInsertions.containsKey(procedureId)) {
                    LOGGER.debug("Building InsertSensorRequest with: procedure '{}', phenomenon '{}' (unit '{}')",
                                 procedureId,
                                 phenomenon.getLabel(),
                                 phenomenon.getUom());
                    DataInsertion dataInsertion = new DataInsertion(insertSensorRequestBuilder);
                    dataInsertions.put(procedureId, dataInsertion);

                    if (ckanSosReferencingCache != null) {
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

        LOGGER.debug("#{} data insertions: {}", dataInsertions.size(), dataInsertions);
        for (Entry<String, DataInsertion> entry : dataInsertions.entrySet()) {
            try {
                DataInsertion dataInsertion = entry.getValue();
                LOGGER.debug("procedure {} => store {}", entry.getKey(), dataInsertion);
                long start = System.currentTimeMillis();
                if (dataInsertion.hasObservations()) {
                    InsertSensorRequest insertSensorRequest = dataInsertion.getRequest();

                    SosInsertionMetadata metadata = createSosInsertionMetadata(dataInsertion);
                    insertSensorRequest.setMetadata(metadata);

                    insertSensorDao.insertSensor(insertSensorRequest);
                    insertObservationDao.insertObservation(dataInsertion.createInsertObservationRequest());
                }
                LOGGER.debug("Insertion completed in {}s.", (System.currentTimeMillis() - start) / 1000d);
                dataInserted = true;

                if (ckanSosReferencingCache != null && dataInsertion.hasObservationsReference()) {
                    ckanSosReferencingCache.addOrUpdate(dataInsertion.getObservationsReference());
                }
            }
            catch (Exception e) {
                LOGGER.error("Could not insert: {}", entry.getValue(), e);
            }
        }

        return dataInserted;
    }

    private boolean isUpdateNeeded(CkanResource resource, DataFile dataFile) {
        if (ckanSosReferencingCache == null) {
            return true;
        }

        try {
            if ( !ckanSosReferencingCache.exists(resource)) {
                CkanSosObservationReference reference = new CkanSosObservationReference(resource);
                ckanSosReferencingCache.addOrUpdate(reference);
                return true;
            }
            CkanSosObservationReference reference = ckanSosReferencingCache.getReference(resource);
            final CkanResource ckanResource = reference.getResource().getCkanResource();

            if ( !dataFile.isNewerThan(ckanResource)) {
                LOGGER.debug("Resource with id '{}' has no data update since {}.",
                             ckanResource.getId(),
                             ckanResource.getLastModified());
                return false;
            }

            long count = 0;
            LOGGER.debug("start deleting existing observation data before updating data.");
            for (String observationIdentifier : reference.getObservationIdentifiers()) {
                try {
                    String namespace = DeleteObservationConstants.NS_SOSDO_1_0;
                    DeleteObservationRequest doRequest = new DeleteObservationRequest(namespace);
                    doRequest.addObservationIdentifier(observationIdentifier);
                    deleteObservationDao.deleteObservation(doRequest);
                    count++;
                }
                catch (OwsExceptionReport e) {
                    LOGGER.error("could not delete observation with id '{}'", observationIdentifier, e);
                }
            }
            LOGGER.debug("deleted #{} observations.", count);
        }
        catch (IOException e) {
            LOGGER.error("Serialization error:  resource with id '{}'", resource.getId(), e);
        }
        return true;
    }

    private SosInsertionMetadata createSosInsertionMetadata(DataInsertion dataInsertion) {
        SosInsertionMetadata metadata = new SosInsertionMetadata();
//        metadata.setFeatureOfInterestTypes(dataInsertion.getFeaturesCharacteristics());
        metadata.setObservationTypes(dataInsertion.getObservationTypes());
        return metadata;
    }

    private AbstractPhenomenon createPhenomenon(Phenomenon phenomenon) {
        return new OmObservableProperty(phenomenon.getId());
    }

}
