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

package org.n52.series.ckan.table;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.da.DataStoreManager;
import org.n52.series.ckan.sos.TableLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;

public class MultiTableLoadingStrategy extends TableLoadingStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTableLoadingStrategy.class);

    public MultiTableLoadingStrategy(DataStoreManager dataStoreManager) {
        super(dataStoreManager);
    }

    @Override
    protected Collection<DataTable> loadData(DataCollection dataCollection) {
        CkanDataset dataset = dataCollection.getDataset();
        LOGGER.debug("load data for dataset '{}'", dataset.getName());
        DataTable fullTable = new ResourceTable();


        // TODO refined strategy to load platforms/observation_with_geometry(?) first
        // and perform a join + insert for each observation resource available

        // TODO do not know if mobile here

        // TODO write test for it
        // TODO if dataset is newer than in cache -> set flag to re-insert whole datacollection

        Set<String> typesToInsert = dataCollection.getResourceTypes();

        Map<String, List<ResourceMember>> membersByType = dataCollection.getResourceMembersByType(typesToInsert);
        for (List<ResourceMember> membersWithCommonResourceTypes : membersByType.values()) {
            DataTable dataTable = new ResourceTable();
            for (ResourceMember member : membersWithCommonResourceTypes) {

                // TODO write test for it

                DataFile dataFile = dataCollection.getDataFile(member);
                CkanResource resource = dataFile.getResource();
                if (dataStoreManager.isUpdateNeeded(resource, dataFile)) {
                    ResourceTable singleDatatable = new ResourceTable(dataCollection.getDataEntry(member));
                    singleDatatable.readIntoMemory();
                    LOGGER.debug("Extend table with: '{}'", singleDatatable);
                    dataTable = dataTable.extendWith(singleDatatable);
                }
            }
            String resourceType = membersWithCommonResourceTypes.get(0)
                                                                .getResourceType();
            LOGGER.debug("Fully extended table for resource '{}': '{}'", resourceType, dataTable);
            fullTable = fullTable.rowSize() > dataTable.rowSize()
                    ? dataTable.innerJoin(fullTable, dataStoreManager.isInterrupted())
                    : fullTable.innerJoin(dataTable, dataStoreManager.isInterrupted());
        }
        LOGGER.debug("Fully joined table: '{}'", fullTable);
        return Collections.singleton(fullTable);
    }

}
