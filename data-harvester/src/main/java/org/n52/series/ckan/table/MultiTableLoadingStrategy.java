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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.da.CkanConstants;
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
        final List<DataTable> joinedDataTables = new ArrayList<>();
        DataTable platformTable = new ResourceTable();

        // TODO do not know if mobile here

        // TODO write test for it
        // TODO if dataset is newer than in cache -> set flag to re-insert whole datacollection

        Set<String> platformTypes =
                new HashSet<>(Arrays.asList(CkanConstants.ResourceType.PLATFORMS,
                                            CkanConstants.ResourceType.OBSERVED_GEOMETRIES,
                                            CkanConstants.ResourceType.OBSERVATIONS_WITH_GEOMETRIES));
        Map<String, List<ResourceMember>> platformData = dataCollection.getResourceMembersByType(platformTypes);
        for (List<ResourceMember> members : platformData.values()) {
            platformTable = readDataToTable(platformTable, dataCollection, members);
        }

        final DataTable platforms = platformTable;
        Set<String> observationTypes = new HashSet<>(Arrays.asList(CkanConstants.ResourceType.OBSERVATIONS));
        Map<String, List<ResourceMember>> observationData = dataCollection.getResourceMembersByType(observationTypes);
        for (Entry<String, List<ResourceMember>> membersByType : observationData.entrySet()) {
            List<ResourceMember> members = membersByType.getValue();
            members.stream()
                   .filter(m -> {
                       // only perform join on resources to be updated
                       DataFile dataFile = dataCollection.getDataFile(m);
                       CkanResource resource = dataFile.getResource();
                       return dataStoreManager.isUpdateNeeded(resource, dataFile);
                   })
                   .forEach(m -> {
                       DataTable dataTable = readDataToTable(dataCollection, Collections.singleton(m));
                       joinedDataTables.add(dataTable.innerJoin(platforms, dataStoreManager.isInterrupted()));
                   });
        }
        return joinedDataTables.isEmpty()
                ? Collections.singleton(platformTable)
                : joinedDataTables;
    }

    protected DataTable readDataToTable(DataCollection dataCollection, Collection<ResourceMember> members) {
        return readDataToTable(null, dataCollection, members);
    }

    protected DataTable readDataToTable(DataTable dataTable,
                                        DataCollection dataCollection,
                                        Collection<ResourceMember> members) {
        for (ResourceMember member : members) {
            Entry<ResourceMember, DataFile> memberData = dataCollection.getDataEntry(member);
            ResourceTable singleDatatable = new ResourceTable(memberData);
            singleDatatable.readIntoMemory();

            LOGGER.debug("Extend table with: '{}'", singleDatatable);
            dataTable = dataTable != null
                    ? dataTable.extendWith(singleDatatable)
                    : singleDatatable;
        }
        return dataTable;
    }

}
