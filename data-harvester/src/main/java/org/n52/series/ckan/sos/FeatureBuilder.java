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

import com.vividsolutions.jts.geom.Geometry;
import eu.trentorise.opendata.jackan.model.CkanDataset;

import java.util.Map;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.util.GeometryBuilder;
import org.n52.sos.exception.ows.concrete.InvalidSridException;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureBuilder {

    private final static Logger LOGGER = LoggerFactory.getLogger(FeatureBuilder.class);

    private final String orgaName;

    public FeatureBuilder(CkanDataset dataset) {
        this.orgaName = dataset.getOrganization().getName();
    }

    SamplingFeature createFeature(Map<ResourceField, String> rowEntry) {
        final SamplingFeature feature = new SamplingFeature(null);
        final GeometryBuilder geometryBuilder = GeometryBuilder.create();
        for (Map.Entry<ResourceField, String> fieldEntry : rowEntry.entrySet()) {
            ResourceField field = fieldEntry.getKey();
            if (field.isField(CkanConstants.KnownFieldIdValue.PLATFORM_ID)) {
                feature.setIdentifier(orgaName + "-" + fieldEntry.getValue());
            }
            if (field.isField(CkanConstants.KnownFieldIdValue.STATION_NAME)) {
                feature.addName(fieldEntry.getValue());
            }
            if ( !field.isOfResourceType(CkanConstants.ResourceType.OBSERVATIONS_WITH_GEOMETRIES)) {
                parseGeometryField(geometryBuilder, fieldEntry);
            }
        }
        if (geometryBuilder.canBuildGeometry()) {
            setFeatureGeometry(feature, geometryBuilder.getGeometry());
            feature.setFeatureType(geometryBuilder.getFeatureType());
        }
        return feature;
    }

    private void parseGeometryField(final GeometryBuilder geometryBuilder, Map.Entry<ResourceField, String> fieldEntry) {
        ResourceField field = fieldEntry.getKey();
        if (field.isField(CkanConstants.KnownFieldIdValue.CRS)) {
            geometryBuilder.withCrs(fieldEntry.getValue());
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.LATITUDE)) {
            geometryBuilder.setLatitude(fieldEntry.getValue());
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.LONGITUDE)) {
            geometryBuilder.setLongitude(fieldEntry.getValue());
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.ALTITUDE)) {
            geometryBuilder.setAltitude(fieldEntry.getValue());
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.LOCATION)) {
            if (field.isOfType("JsonObject")) {
                geometryBuilder.withGeoJson(fieldEntry.getValue());
            } else {
                geometryBuilder.withWKT(fieldEntry.getValue());
            }
        }
    }

    private void setFeatureGeometry(SamplingFeature feature, Geometry geometry) {
        try {
            feature.setGeometry(geometry);
        }
        catch (InvalidSridException e) {
            LOGGER.error("could not set feature's geometry.", e);
        }
    }

}
