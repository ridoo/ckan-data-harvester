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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.n52.sos.gda.GetDataAvailabilityResponse.DataAvailability;
import org.n52.sos.ogc.gml.ReferenceType;

public class DataAvailabilityMatcher {

    public static Matcher<DataAvailability> hasPhenomenon(final String phenomenon) {
        return new TypeSafeMatcher<DataAvailability>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("dataset should contain observed property ").appendValue(phenomenon);
            }
            @Override
            protected void describeMismatchSafely(DataAvailability item, Description mismatchDescription) {
                ReferenceType observedProperty = item.getObservedProperty();
                mismatchDescription.appendText("was").appendValue(observedProperty);
            }
            @Override
            protected boolean matchesSafely(DataAvailability dataset) {
                return hasPhenomenon(dataset, phenomenon);
            }
        };
    }

    public static Matcher<List<DataAvailability>> containsDatasetWithPhenomenon(final String phenomenon) {
        return new TypeSafeMatcher<List<DataAvailability>>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("no dataset with observed property ").appendValue(phenomenon);
            }
            @Override
            protected void describeMismatchSafely(List<DataAvailability> items, Description mismatchDescription) {
                Set<String> phenomena = new HashSet<>();
                for (DataAvailability dataset : items) {
                    phenomena.add(dataset.getObservedProperty().getHref());
                }
                mismatchDescription.appendText("was").appendValue(DataAvailabilityMatcher.toString(phenomena));
            }
            @Override
            protected boolean matchesSafely(List<DataAvailability> datasets) {
                for (DataAvailability dataset : datasets) {
                    if (hasPhenomenon(dataset, phenomenon)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static Matcher<List<DataAvailability>> containsDatasetWithFeature(final String feature) {
        return new TypeSafeMatcher<List<DataAvailability>>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("no dataset with feature").appendValue(feature);
            }
            @Override
            protected void describeMismatchSafely(List<DataAvailability> items, Description mismatchDescription) {
                Set<String> features = new HashSet<>();
                for (DataAvailability dataset : items) {
                    features.add(dataset.getFeatureOfInterest().getHref());
                }
                mismatchDescription.appendText("was").appendValue(DataAvailabilityMatcher.toString(features));
            }
            @Override
            protected boolean matchesSafely(List<DataAvailability> datasets) {
            for (DataAvailability dataset : datasets) {
                if (hasFeature(dataset, feature)) {
                    return true;
                }
            }
            return false;
            }
        };
    }

    private static boolean hasFeature(DataAvailability availability, final String feature) {
        ReferenceType foi = availability.getFeatureOfInterest();
        return foi.getHref().equals(feature);
    }

    private static boolean hasPhenomenon(DataAvailability availability, final String phenomenon) {
        ReferenceType observedProperty = availability.getObservedProperty();
        return observedProperty.getHref().equals(phenomenon);
    }

    private static String toString(Set<String> phenomena) {
        String[] asArray = phenomena.toArray(new String[0]);
        return Arrays.toString(asArray);
    }

}
