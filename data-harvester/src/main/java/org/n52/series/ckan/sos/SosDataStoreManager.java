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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.da.DataStoreManager;
import org.n52.series.ckan.table.DataTable;
import org.n52.sos.ds.hibernate.InsertObservationDAO;
import org.n52.sos.ds.hibernate.InsertSensorDAO;
import org.n52.sos.ext.deleteobservation.DeleteObservationConstants;
import org.n52.sos.ext.deleteobservation.DeleteObservationDAO;
import org.n52.sos.ext.deleteobservation.DeleteObservationRequest;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.SosInsertionMetadata;
import org.n52.sos.request.InsertSensorRequest;
import org.n52.sos.service.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.model.CkanResource;

public abstract class SosDataStoreManager implements DataStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SosDataStoreManager.class);

    private final InsertSensorDAO insertSensorDao;

    private final InsertObservationDAO insertObservationDao;

    private final DeleteObservationDAO deleteObservationDao;

    private final CkanSosReferenceCache ckanSosReferenceCache;

    SosDataStoreManager() {
        this(null);
    }

    SosDataStoreManager(CkanSosReferenceCache ckanSosReferenceCache) {
        this(new InsertSensorDAO(), new InsertObservationDAO(), new DeleteObservationDAO(), ckanSosReferenceCache);
    }

    public SosDataStoreManager(InsertSensorDAO insertSensorDao,
            InsertObservationDAO insertObservationDao,
            DeleteObservationDAO deleteObservationDao,
            CkanSosReferenceCache ckanSosReferenceCache) {
        this.insertSensorDao = insertSensorDao;
        this.insertObservationDao = insertObservationDao;
        this.deleteObservationDao = deleteObservationDao;
        this.ckanSosReferenceCache = ckanSosReferenceCache;
    }

    @Override
    public void insertOrUpdate(DataCollection dataCollection) {
        try {
            Map<String, DataInsertion> datainsertions = getDataInsertions(dataCollection);
            if (storeDataInsertions(datainsertions)) {
                // Trigger SOS Capabilities cache reloading after insertion
                Configurator.getInstance().getCacheController().update();
            }
        }
        catch (OwsExceptionReport e) {
            LOGGER.warn("Error while reloading SOS Capabilities cache", e);
        }
    }

    private Map<String, DataInsertion> getDataInsertions(DataCollection dataCollection) {
        Map<String, DataInsertion> dataInsertions = new HashMap<>();

        // add stationary observation data
        SosInsertStrategy stationaryStrategy = hasReferenceCache()
                ? new StationaryInsertStrategy()
                : new StationaryInsertStrategy(getCkanSosReferenceCache());
        DataTable stationaryInserts = loadData(dataCollection, getStationaryObservationTypes());
        dataInsertions.putAll(stationaryStrategy.createDataInsertions(stationaryInserts, dataCollection));

        // add mobile observation data
        SosInsertStrategy mobileStrategy = hasReferenceCache()
                ? new MobileInsertStrategy()
                : new MobileInsertStrategy(getCkanSosReferenceCache());
        DataTable mobileInserts = loadData(dataCollection, getMobileObservationTypes());
        dataInsertions.putAll(mobileStrategy.createDataInsertions(mobileInserts, dataCollection));

        return dataInsertions;
    }

    protected abstract DataTable loadData(DataCollection dataCollection, Set<String> resourceTypesToInsert);

    protected boolean isUpdateNeeded(CkanResource resource, DataFile dataFile) {
        if (hasReferenceCache()) {
            return true;
        }

        try {
            if ( !ckanSosReferenceCache.exists(resource)) {
                CkanSosObservationReference reference = new CkanSosObservationReference(resource);
                ckanSosReferenceCache.addOrUpdate(reference);
                return true;
            }
            CkanSosObservationReference reference = ckanSosReferenceCache.getReference(resource);
            final CkanResource ckanResource = reference.getResource().getCkanResource();

            if ( !dataFile.isNewerThan(ckanResource)) {
                LOGGER.debug("Resource with id '{}' has no data update since {}.",
                             ckanResource.getId(),
                             ckanResource.getLastModified());
                return false;
            }

            long count = 0;

            // TODO a real update would be better (instead of deleting and inserting)

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

    protected boolean hasReferenceCache() {
        return ckanSosReferenceCache == null;
    }

    private boolean storeDataInsertions(Map<String, DataInsertion> dataInsertionByProcedure) {
        boolean dataInserted = false;
        LOGGER.debug("#{} data insertions: {}", dataInsertionByProcedure.size(), dataInsertionByProcedure);
        for (Entry<String, DataInsertion> entry : dataInsertionByProcedure.entrySet()) {
            try {
                DataInsertion dataInsertion = entry.getValue();
                LOGGER.debug("procedure {} => store {}", entry.getKey(), dataInsertion);
                long start = System.currentTimeMillis();
                if (dataInsertion.hasObservations()) {
                    InsertSensorRequest insertSensorRequest = dataInsertion.buildInsertSensorRequest();

                    SosInsertionMetadata metadata = createSosInsertionMetadata(dataInsertion);
                    insertSensorRequest.setMetadata(metadata);

                    insertSensorDao.insertSensor(insertSensorRequest);
                    insertObservationDao.insertObservation(dataInsertion.createInsertObservationRequest());
                }
                LOGGER.debug("Insertion completed in {}s.", (System.currentTimeMillis() - start) / 1000d);
                dataInserted = true;

                if (ckanSosReferenceCache != null && dataInsertion.hasObservationsReference()) {
                    ckanSosReferenceCache.addOrUpdate(dataInsertion.getObservationsReference());
                }
            }
            catch (Exception e) {
                LOGGER.error("Could not insert: {}", entry.getValue(), e);
            }
        }
        return dataInserted;
    }

    private SosInsertionMetadata createSosInsertionMetadata(DataInsertion dataInsertion) {
        SosInsertionMetadata metadata = new SosInsertionMetadata();
        metadata.setFeatureOfInterestTypes(Collections.<String>emptyList());
        metadata.setObservationTypes(dataInsertion.getObservationTypes());
        return metadata;
    }

    public InsertSensorDAO getInsertSensorDao() {
        return insertSensorDao;
    }

    public InsertObservationDAO getInsertObservationDao() {
        return insertObservationDao;
    }

    public DeleteObservationDAO getDeleteObservationDao() {
        return deleteObservationDao;
    }

    public CkanSosReferenceCache getCkanSosReferenceCache() {
        return ckanSosReferenceCache;
    }

}
