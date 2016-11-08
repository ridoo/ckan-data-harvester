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
package org.n52.series.ckan.table;

import static java.lang.System.currentTimeMillis;

import java.util.Map.Entry;

import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceTable extends DataTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceTable.class);

    private DataFile dataFile;

    public ResourceTable() {
        this(new ResourceMember(), new DataFile());
    }

    public ResourceTable(Entry<ResourceMember, DataFile> dataEntry) {
        this(dataEntry.getKey(), dataEntry.getValue());
    }

    public ResourceTable(ResourceMember resourceMember, DataFile dataFile) {
        super(resourceMember);
        this.dataFile = dataFile;
    }

    public void readIntoMemory() {
        long start = currentTimeMillis();
        String ckanResourceName = dataFile.getResource().getName();
        LOGGER.debug("Load data file '{}': {}", ckanResourceName, dataFile.toString());

        try {
            table.clear();
            createTableLoader(dataFile).loadData();
        } catch (TableLoadException e) {
            LOGGER.info("Could not load table data for resource '{}'", ckanResourceName, e);
        }

        LOGGER.debug("Resource data '{}' loaded into memory (#{} rows and #{} columns), took {}s",
                     resourceMember.getId(), rowSize(), columnSize(),
                     (currentTimeMillis() - start)/1000d);
    }

    private TableLoader createTableLoader(DataFile file) {
        if (file.getFormat().equalsIgnoreCase("csv")) {
            return new CsvTableLoader(this, file);
        } else if (file.getFormat().equalsIgnoreCase("json")) {
            return new JsonTableLoader(this, file);
        } else  {
            return new EmptyTableLoader(this, file);
        }
    }


}
