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
package org.n52.series.ckan.sos.matcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.n52.sos.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.om.features.FeatureCollection;
import org.n52.sos.ogc.om.features.SfConstants;
import org.n52.sos.ogc.om.features.samplingFeatures.AbstractSamplingFeature;
import org.n52.sos.response.GetFeatureOfInterestResponse;

public class FeatureOfInterestMatcher {

    public static Matcher<GetFeatureOfInterestResponse> isSamplingPoint(String featureId) {
        return new SampleFeatureMatcher() {
            @Override
            protected boolean matchesSafely(GetFeatureOfInterestResponse response) {
                return isSamplingPoint(featureId, response);
            }
        };
    }
    public static Matcher<GetFeatureOfInterestResponse> isSamplingCurve(String featureId) {
        return new SampleFeatureMatcher() {
            @Override
            protected boolean matchesSafely(GetFeatureOfInterestResponse response) {
                return isSamplingCurve(featureId, response);
            }
        };
    }

    public static Matcher<GetFeatureOfInterestResponse> contains(final String featureId) {
        return new GetFoiResponseMatcher() {
            @Override
            public void describeTo(Description description) {
                description.appendText("getFoi should have contained feature with ID").appendValue(featureId);
            }
            @Override
            protected void describeMismatchSafely(GetFeatureOfInterestResponse item, Description mismatchDescription) {
                mismatchDescription.appendText("was").appendValue(Boolean.FALSE);
            }
            @Override
            protected boolean matchesSafely(GetFeatureOfInterestResponse response) {
                return getFeature(featureId, response) != null;
            }
        };
    }

    private static abstract class SampleFeatureMatcher extends GetFoiResponseMatcher {
        private AbstractSamplingFeature feature;
        @Override
        public void describeTo(Description description) {
            if ( !isSamplingFeature(feature)) {
                description.appendText("was").appendValue("not a sampling feature!");
            } else {
                AbstractSamplingFeature sf = (AbstractSamplingFeature) feature;
                description.appendText("was").appendValue(sf.getFeatureType());
            }
        }
        protected boolean isSamplingPoint(String featureId, GetFeatureOfInterestResponse response) {
            AbstractSamplingFeature feature = getSamplingFeature(featureId, response);
            return isOfSamplingType(feature, SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_POINT);
        }
        protected boolean isSamplingCurve(String featureId, GetFeatureOfInterestResponse response) {
            AbstractSamplingFeature feature = getSamplingFeature(featureId, response);
            return isOfSamplingType(feature, SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_CURVE);
        }

        private boolean isOfSamplingType(AbstractSamplingFeature feature, String type) {
            return feature != null && feature.getFeatureType().equals(type);
        }

        protected boolean isSamplingFeature(AbstractFeature feature) {
            return feature != null && AbstractSamplingFeature.class.isAssignableFrom(feature.getClass());
        }

        protected AbstractSamplingFeature getSamplingFeature(String featureId, GetFeatureOfInterestResponse response) {
            AbstractFeature abstractFeature = super.getFeature(featureId, response);
            feature = isSamplingFeature(abstractFeature)
                    ? (AbstractSamplingFeature) abstractFeature
                    : null;
            return feature;
        }
    }

    private static abstract class GetFoiResponseMatcher extends TypeSafeMatcher<GetFeatureOfInterestResponse> {

        protected AbstractFeature getFeature(String featureId, GetFeatureOfInterestResponse response) {
            AbstractFeature feature = response.getAbstractFeature();
            if (feature == null) {
                return feature;
            }
            if ( !FeatureCollection.class.isAssignableFrom(feature.getClass())) {
                String id = feature.getIdentifier();
                return id != null && id.equals(featureId)
                        ? feature
                        : null;
            }
            FeatureCollection features = (FeatureCollection) feature;
            for (AbstractFeature item : features) {
                String id = item.getIdentifier();
                if (id != null && id.equals(featureId)) {
                    return item;
                }
            }
            return null;
        }
    }
}
