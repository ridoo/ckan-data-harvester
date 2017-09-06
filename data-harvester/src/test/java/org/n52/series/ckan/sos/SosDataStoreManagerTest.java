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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.n52.series.ckan.da.CkanMapping;
import org.n52.series.ckan.table.MultiTableLoadingStrategy;
import org.n52.series.ckan.table.SingleTableLoadingStrategy;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class SosDataStoreManagerTest {

    private SosDataStoreManager sosDataStore;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        sosDataStore = new SosDataStoreManager(null, null, null, null);
    }

    @Test
    public void when_multiTableLoadingConfig_then_createMultiTableStrategy() {
        String tableLoaderConfig = "{"
                + "  \"strategy\": {"
                + "    \"table_loader\": {"
                + "      \"class\": \"org.n52.series.ckan.table.MultiTableLoadingStrategy\""
                + "    }"
                + "  }"
                + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(tableLoaderConfig.getBytes());
        CkanMapping ckanMapping = CkanMapping.loadCkanMapping(inputStream);
        TableLoadingStrategy tableLoader = sosDataStore.createTableLoader(ckanMapping, new CkanDataset());
        Assert.assertTrue("Unexpected type: " + tableLoaderConfig.getClass(),
                          tableLoader instanceof MultiTableLoadingStrategy);
    }

    @Test
    public void when_noTableLoadingConfig_then_useSingleTableStrategy() {
        String tableLoaderConfig = "{"
                + "  \"strategy\": {"
                + "    \"table_loader\": {"
                + "      \"class\": \"\""
                + "    }"
                + "  }"
                + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(tableLoaderConfig.getBytes());
        CkanMapping ckanMapping = CkanMapping.loadCkanMapping(inputStream);
        TableLoadingStrategy tableLoader = sosDataStore.createTableLoader(ckanMapping, new CkanDataset());
        Class<?> actualType = tableLoader != null
                ? tableLoader.getClass()
                : null;
        Assert.assertTrue("Unexpected type: " + actualType, tableLoader instanceof SingleTableLoadingStrategy);
    }

    @Test
    public void when_noMobileConfig_then_createStationaryInsertStrategy() {
        String tableLoaderConfig = "{"
                + "  \"strategy\": {"
                + "  }"
                + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(tableLoaderConfig.getBytes());
        CkanMapping ckanMapping = CkanMapping.loadCkanMapping(inputStream);
        SosInsertStrategy insertStrategy = sosDataStore.createInsertStrategy(ckanMapping, new CkanDataset());
        Class<?> actualType = insertStrategy != null
                ? insertStrategy.getClass()
                : null;
        Assert.assertTrue("Unexpected type: " + actualType, insertStrategy instanceof StationaryInsertStrategy);
    }

    @Test
    public void when_mobileTrueConfig_then_createMobileInsertStrategy() {
        String tableLoaderConfig = "{"
                + "  \"strategy\": {"
                + "    \"mobile\": true"
                + "  }"
                + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(tableLoaderConfig.getBytes());
        CkanMapping ckanMapping = CkanMapping.loadCkanMapping(inputStream);
        SosInsertStrategy insertStrategy = sosDataStore.createInsertStrategy(ckanMapping, new CkanDataset());
        Class<?> actualType = insertStrategy != null
                ? insertStrategy.getClass()
                : null;
        Assert.assertTrue("Unexpected type: " + actualType, insertStrategy instanceof MobileInsertStrategy);
    }

    @Test
    public void when_mobileFalseConfig_then_createMobileInsertStrategy() {
        String tableLoaderConfig = "{"
                + "  \"strategy\": {"
                + "    \"mobile\": false"
                + "  }"
                + "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(tableLoaderConfig.getBytes());
        CkanMapping ckanMapping = CkanMapping.loadCkanMapping(inputStream);
        SosInsertStrategy insertStrategy = sosDataStore.createInsertStrategy(ckanMapping, new CkanDataset());
        Class<?> actualType = insertStrategy != null
                ? insertStrategy.getClass()
                : null;
        Assert.assertTrue("Unexpected type: " + actualType, insertStrategy instanceof StationaryInsertStrategy);
    }

}
