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

import static org.n52.series.ckan.sos.H2DatabaseAccessor.hasDatasetCount;
import static org.n52.series.ckan.sos.H2DatabaseAccessor.hasObservationsAvailable;

import java.io.IOException;
import java.net.URISyntaxException;

import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.n52.series.ckan.cache.InMemoryDataStoreManager;
import org.n52.series.ckan.util.FileBasedCkanHarvestingService;
import org.n52.sos.config.SettingsManager;
import org.n52.sos.ds.ConnectionProviderException;
import org.n52.sos.ds.hibernate.H2Configuration;
import org.n52.sos.ds.hibernate.HibernateTestCase;
import org.n52.sos.exception.ConfigurationException;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore("currently toooooo slooooooooooow for unit testing")
public class SosInsertionTest extends HibernateTestCase {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SosInsertionTest.class);

    private FileBasedCkanHarvestingService service;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private InMemoryDataStoreManager ckanDataMgr;

    private SosDataStoreManager sosDataMgr;

    private H2DatabaseAccessor database;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        service = new FileBasedCkanHarvestingService(testFolder.getRoot());
        ckanDataMgr = service.getCkanDataStoreManager();
        sosDataMgr = new SosDataStoreManager();
        database = new H2DatabaseAccessor();
        // empty database before each test
        H2Configuration.recreate();
    }

    @After
    public void cleanUp() throws ConnectionProviderException, ConfigurationException {
        SettingsManager.getInstance().cleanup();
    }

    @Test
    public void when_inserting_DWDWind_dataset_then_getObservationNotEmpty() {
        insertDataset("12c3c8f1-bf44-47e1-b294-b6fc889873fc");
        MatcherAssert.assertThat(database, hasDatasetCount(2));
        assertThat(database, hasObservationsAvailable());
    }

    @Test
    public void when_inserting_DWDTemperature_dataset_then_getObservationNotEmpty() {
        insertDataset("eab53bfe-fce7-4fd8-8325-a0fe5cdb23c8");
        MatcherAssert.assertThat(database, hasDatasetCount(8));
        assertThat(database, hasObservationsAvailable());
    }

    @Test
    public void when_inserting_DWDSun_dataset_then_getObservationNotEmpty() {
        insertDataset("582ca1ba-bdc0-48de-a685-3184339d29f0");
        MatcherAssert.assertThat(database, hasDatasetCount(2));
        assertThat(database, hasObservationsAvailable());
    }

    @Test
    public void when_inserting_DWDPrecipitation_dataset_then_getObservationNotEmpty() {
        insertDataset("26128007-1fa7-475f-a4f1-5e798185eab9");
        // TODO schema descriptor actually describes two more (NIEDERSCHLAG_GEFALLEN_IND 
        // and NIEDESCHLAGSFORM) phenomena but both are missing the `phenomenon` field value
        //MatcherAssert.assertThat(database, hasDatasetCount(12));
        MatcherAssert.assertThat(database, hasDatasetCount(4));
        assertThat(database, hasObservationsAvailable());

    }

    @Test
    public void when_inserting_FraunhoferIgdTemp_dataset_then_getObservationNotEmpty() {
        insertDataset("f5c3eb65-c695-49a4-8992-ed54f973b950");
        MatcherAssert.assertThat(database, hasDatasetCount(1));
        assertThat(database, hasObservationsAvailable());
    }

    @Test
    public void when_inserting_FraunhoferIgdCoord_dataset_then_getObservationNotEmpty() {
        insertDataset("3aa04d90-4003-408a-8f3f-463dd6cb7486");
        MatcherAssert.assertThat(database, hasDatasetCount(1));
        assertThat(database, hasObservationsAvailable());
    }

    @Test
    public void when_inserting_OpenWeatherMapCoord_dataset_then_getObservationNotEmpty() {
        insertDataset("fb0f0f57-7a01-4385-9fe1-9fb366a63c4e");
        MatcherAssert.assertThat(database, hasDatasetCount(3));
        assertThat(database, hasObservationsAvailable());
    }

    @Test
    public void when_inserting_OpenWeatherMapTemp_dataset_then_getObservationNotEmpty() {
        insertDataset("a5442a6a-0a84-4326-a5b5-e6288e8fa457");
        MatcherAssert.assertThat(database, hasDatasetCount(3));
        assertThat(database, hasObservationsAvailable());
    }

    @Test
    public void when_inserting_heavyMetalSamples_dataset_then_getObservationNotEmpty() {
        insertDataset("3eb54ee2-6ec5-4ad9-af96-264159008aa7");
        MatcherAssert.assertThat(database, hasDatasetCount(6));
        assertThat(database, hasObservationsAvailable());
        
        // check if each track is available as own dataset
        MatcherAssert.assertThat(database, H2DatabaseAccessor.hasDatasetsWithFeatureId("2012-07-20 - Bannewitz"));
        MatcherAssert.assertThat(database, H2DatabaseAccessor.hasDatasetsWithFeatureId("2012-07-21 - Bannewitz"));
        MatcherAssert.assertThat(database, H2DatabaseAccessor.hasDatasetsWithFeatureId("2012-07-22 - Bannewitz"));
        MatcherAssert.assertThat(database, H2DatabaseAccessor.hasDatasetsWithFeatureId("2012-07-23 - Bannewitz"));
        MatcherAssert.assertThat(database, H2DatabaseAccessor.hasDatasetsWithFeatureId("2012-07-25 - Bannewitz"));
        MatcherAssert.assertThat(database, H2DatabaseAccessor.hasDatasetsWithFeatureId("2012-07-27 - Bannewitz"));
    }
    
    @Test
    @Ignore("this dataset needs some discussion first")
    public void when_inserting_emmissionSimulationResults_dataset_then_getObservationNotEmpty() {
        // TODO simulation results need discussion
        // 1) a mobile platform (car) which is declared as observation --> observation_with_geometry
        // 2) the track must be identifiable ... can be artificially differentiated by combining columns
        //    --> e.g. timestamp (using raw, or an aggregating pattern)
        insertDataset("30bdf3a2-74ba-43e0-9b31-50e5f5414402");
        assertThat(database, hasObservationsAvailable());
    }

    private void insertDataset(String datasetId) {
        try {
            sosDataMgr.insertOrUpdate(ckanDataMgr.getCollection(datasetId));
        } catch (Exception e) {
            LOGGER.error("could not insert test data!", e);
            Assert.fail("Could not insert test data!");
        }
    }

}
