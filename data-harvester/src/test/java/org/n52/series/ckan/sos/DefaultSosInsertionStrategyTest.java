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
import java.net.URISyntaxException;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.n52.series.ckan.util.FileBasedCkanHarvestingService;
import org.n52.sos.config.SettingsManager;
import org.n52.sos.exception.ConfigurationException;
import org.n52.sos.ogc.ows.OwsExceptionReport;

@Ignore("currently toooooo slooooooooooow for unit testing")
public class DefaultSosInsertionStrategyTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void when_inserting_DWDWind_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("12c3c8f1-bf44-47e1-b294-b6fc889873fc");
    }

    @Test
    public void when_inserting_DWDTemperature_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("eab53bfe-fce7-4fd8-8325-a0fe5cdb23c8");
    }

    @Test
    public void when_inserting_DWDSun_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("582ca1ba-bdc0-48de-a685-3184339d29f0");
    }

    @Test
    public void when_inserting_DWDPrecipitation_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("26128007-1fa7-475f-a4f1-5e798185eab9");
    }

    @Test
    @Ignore("need for json parser to make this insertion work")
    public void when_inserting_TempFraunhoferIgd_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("a54b09b1-bbc0-4e8c-ac69-a9616603827e");
    }

    @Test
    public void when_inserting_FraunhoferIgdTemp_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("f5c3eb65-c695-49a4-8992-ed54f973b950");
    }

    @Test
    public void when_inserting_FraunhoferIgdCoord_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("3aa04d90-4003-408a-8f3f-463dd6cb7486");
    }

    @Test
    @Ignore("need for json parser to make this insertion work")
    public void when_inserting_CoordFraunhoferIgd_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("3464c356-cf6d-4a73-bcc3-febbee006669");
    }

    @Test
    @Ignore("need for json parser to make this insertion work")
    public void when_inserting_CoordAtFraunhoferIgd_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("8bcc9868-9851-410e-93bd-86bde049d8ce");
    }

    @Test
    @Ignore("need for json parser to make this insertion work")
    public void when_inserting_TempAtFraunhoferIgd_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("4f09614f-0ff4-4604-9a4a-06057540aab4");
    }

    @Test
    public void when_inserting_OpenWeatherMapCoord_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("fb0f0f57-7a01-4385-9fe1-9fb366a63c4e");
    }

    @Test
    public void when_inserting_OpenWeatherMapTemp_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("a5442a6a-0a84-4326-a5b5-e6288e8fa457");
    }

    @Test
    @Ignore("fix parsing of mobile sensing data")
    public void when_inserting_heavyMetalSamples_dataset_then_getObservationNotEmpty()
            throws OwsExceptionReport, IOException, URISyntaxException {
        assertDataInsertion("3eb54ee2-6ec5-4ad9-af96-264159008aa7");
    }

    private void assertDataInsertion(String datasetId)
            throws URISyntaxException, IOException, OwsExceptionReport, ConfigurationException {
        FileBasedCkanHarvestingService service = new FileBasedCkanHarvestingService(testFolder.getRoot());
        SosH2Store sosStore = new SosH2Store(service.getCkanDataCache());
        sosStore.insertDatasetViaStrategy(datasetId, new DefaultSosInsertionStrategy());
        sosStore.assertObservationsAvailable();
        SettingsManager.getInstance().cleanup();
    }

}
