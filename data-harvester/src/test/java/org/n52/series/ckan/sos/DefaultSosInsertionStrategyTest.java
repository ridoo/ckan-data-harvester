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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.n52.series.ckan.util.FileBasedCkanHarvestingService;
import org.n52.sos.config.SettingsManager;
import org.n52.sos.ogc.ows.OwsExceptionReport;

@Ignore("currently toooooo slooooooooooow for unit testing")
public class DefaultSosInsertionStrategyTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void when_insertingMeasurementDatasets_then_getObservationNotEmpty() throws OwsExceptionReport, IOException, URISyntaxException {
        FileBasedCkanHarvestingService service = new FileBasedCkanHarvestingService(testFolder.getRoot());
        SosH2Store sosStore = new SosH2Store(service.getCkanDataCache());
        sosStore.insertDatasetViaStrategy("12c3c8f1-bf44-47e1-b294-b6fc889873fc", new DefaultSosInsertionStrategy());
        sosStore.assertObservationsAvailable();
        SettingsManager.getInstance().cleanup();
    }

    @Test
    public void when_insertingGeometryObservations_then_getObservationNotEmpty() throws OwsExceptionReport, IOException, URISyntaxException {
      FileBasedCkanHarvestingService service = new FileBasedCkanHarvestingService(testFolder.getRoot());
      SosH2Store sosStore = new SosH2Store(service.getCkanDataCache());
      sosStore.insertDatasetViaStrategy("fb0f0f57-7a01-4385-9fe1-9fb366a63c4e", new DefaultSosInsertionStrategy());
      sosStore.assertObservationsAvailable();
      SettingsManager.getInstance().cleanup();
  }

}
