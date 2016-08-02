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
import java.util.List;

import org.n52.series.ckan.beans.CsvObservationsCollection;
import org.n52.series.ckan.cache.InMemoryCkanDataCache;
import org.n52.series.ckan.cache.InMemoryCkanDataCache.Entry;
import org.n52.sos.ds.hibernate.GetObservationDAO;
import org.n52.sos.ds.hibernate.H2Configuration;
import org.n52.sos.ds.hibernate.HibernateTestCase;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.Sos2Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.request.GetObservationRequest;
import org.n52.sos.response.GetObservationResponse;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class SosH2Store extends HibernateTestCase {

    private InMemoryCkanDataCache ckanDataCache;

    public SosH2Store(InMemoryCkanDataCache ckanDataCache) throws IOException, URISyntaxException {
        this.ckanDataCache = ckanDataCache;
        H2Configuration.assertInitialized();
    }

    // TODO test via data loaders for each file set (reduce each to a minimum to keep tests fast)
    // TODO think of refactoring how strategy works to run tests fast
    // TODO think of making this an integration test

    void insertDatasetViaStrategy(String datasetId, SosInsertionStrategy insertionStrategy)
            throws OwsExceptionReport {
        Entry<CkanDataset, CsvObservationsCollection> collection = ckanDataCache.getCollection(datasetId);
        insertionStrategy.insertOrUpdate(collection.getDataset(), collection.getData());
    }

    void assertObservationsAvailable() throws OwsExceptionReport {
        GetObservationDAO getObsDAO = new GetObservationDAO();
        GetObservationRequest getObsReq = new GetObservationRequest();
        getObsReq.setService(SosConstants.SOS);
        getObsReq.setVersion(Sos2Constants.SERVICEVERSION);
        GetObservationResponse getObsResponse = getObsDAO.getObservation(getObsReq);
        List<OmObservation> observationCollection = getObsResponse.getObservationCollection();
        assertThat(observationCollection, is(notNullValue()));
        assertThat(observationCollection, is(not(empty())));
    }

}
