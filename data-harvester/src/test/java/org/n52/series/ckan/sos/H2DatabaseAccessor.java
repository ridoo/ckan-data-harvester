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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.n52.sos.ds.hibernate.DescribeSensorDAO;
import org.n52.sos.ds.hibernate.GetDataAvailabilityDAO;
import org.n52.sos.ds.hibernate.GetFeatureOfInterestDAO;
import org.n52.sos.ds.hibernate.GetObservationDAO;
import org.n52.sos.ds.hibernate.H2Configuration;
import org.n52.sos.gda.GetDataAvailabilityRequest;
import org.n52.sos.gda.GetDataAvailabilityResponse;
import org.n52.sos.gda.GetDataAvailabilityResponse.DataAvailability;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.ows.OwsExceptionReport;
import org.n52.sos.ogc.sos.Sos2Constants;
import org.n52.sos.ogc.sos.SosConstants;
import org.n52.sos.request.DescribeSensorRequest;
import org.n52.sos.request.GetFeatureOfInterestRequest;
import org.n52.sos.request.GetObservationRequest;
import org.n52.sos.response.DescribeSensorResponse;
import org.n52.sos.response.GetFeatureOfInterestResponse;
import org.n52.sos.response.GetObservationResponse;
import org.n52.sos.service.AbstractServiceCommunicationObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DatabaseAccessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2DatabaseAccessor.class);

    public H2DatabaseAccessor() throws IOException, URISyntaxException {
        H2Configuration.assertInitialized();
    }


    public static Matcher<H2DatabaseAccessor> hasObservationsAvailable() {

        // TODO move to separate matcher

        return new TypeSafeMatcher<H2DatabaseAccessor>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("emptyStore should return ").appendValue(Boolean.TRUE);
            }
            @Override
            protected void describeMismatchSafely(H2DatabaseAccessor item, Description mismatchDescription) {
                mismatchDescription.appendText("was").appendValue(Boolean.FALSE);
            }
            @Override
            protected boolean matchesSafely(H2DatabaseAccessor database) {
                return !database.getObservations().isEmpty();
            }
        };
    }


    // TODO test via data loaders for each file set (reduce each to a minimum to keep tests fast)
    // TODO think of refactoring how strategy works to run tests fast
    // TODO think of making this an integration test

    List<OmObservation> getObservations() {
        try {
            GetObservationDAO getObsDAO = new GetObservationDAO();
            GetObservationRequest request = applyCommonParameters(new GetObservationRequest());
            GetObservationResponse getObsResponse = getObsDAO.getObservation(request);
            return getObsResponse.getObservationCollection();
        } catch (OwsExceptionReport e) {
            LOGGER.error("Could not query H2 database!", e);
            Assert.fail("Could not query H2 database!");
            return null;
        }
    }


    DescribeSensorResponse describeSensor(String procedureId) {
        try {
            DescribeSensorDAO descSensor = new DescribeSensorDAO();
            DescribeSensorRequest request = applyCommonParameters(new DescribeSensorRequest());
            request.setProcedureDescriptionFormat("http://www.opengis.net/sensorML/1.0.1");
            request.setProcedure(procedureId);
            return descSensor.getSensorDescription(request);
        } catch (OwsExceptionReport e) {
            LOGGER.error("Could not query H2 database!", e);
            Assert.fail("Could not query H2 database!");
            return null;
        }
    }
    
    GetFeatureOfInterestResponse getFeatures(String... featureIds) {
        try {
            GetFeatureOfInterestDAO getFeature = new GetFeatureOfInterestDAO();
            GetFeatureOfInterestRequest request = applyCommonParameters(new GetFeatureOfInterestRequest());
            request.setFeatureIdentifiers(featureIds == null
                    ? Collections.<String>emptyList()
                    :Arrays.asList(featureIds));
            return getFeature.getFeatureOfInterest(request);
        } catch (OwsExceptionReport e) {
            LOGGER.error("Could not query H2 database!", e);
            Assert.fail("Could not query H2 database!");
            return null;
        }
    }

    List<DataAvailability> getDataAvailability() {
        return getDataAvailability(applyCommonParameters(new GetDataAvailabilityRequest()));
    }

    List<DataAvailability> getDataAvailabilityForFeatures(String... features) {
         GetDataAvailabilityRequest request = applyCommonParameters(new GetDataAvailabilityRequest());
        request = features != null
                ? request.setFeatureOfInterest(Arrays.asList(features))
                : request;
        return getDataAvailability(request);
    }

    List<DataAvailability> getDataAvailability(GetDataAvailabilityRequest request) {
        try {
            GetDataAvailabilityDAO gdaDAO = new GetDataAvailabilityDAO();
            GetDataAvailabilityResponse gdaResponse = gdaDAO.getDataAvailability(request);
            return gdaResponse.getDataAvailabilities();
        } catch (OwsExceptionReport e) {
            LOGGER.error("Could not query H2 database!", e);
            Assert.fail("Could not query H2 database!");
            return null;
        }
    }

    private <T extends AbstractServiceCommunicationObject> T applyCommonParameters(T request) {
        request.setVersion(Sos2Constants.SERVICEVERSION);
        request.setService(SosConstants.SOS);
        return request;
    }

}
