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

import java.util.Map;

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.da.CkanMapping;
import org.n52.series.ckan.sos.TrackPointCollector.TrackPoint;
import org.n52.series.ckan.util.GeometryBuilder;
import org.n52.sos.exception.ows.concrete.InvalidSridException;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class FeatureBuilder {

    private final static Logger LOGGER = LoggerFactory.getLogger(FeatureBuilder.class);

    private final CkanDataset dataset;

    private TrackPointCollector trackPointCollector;

    public FeatureBuilder(CkanDataset dataset) {
        this(dataset, CkanMapping.loadCkanMapping());
    }

    public FeatureBuilder(CkanDataset dataset, CkanMapping ckanMapping) {
        this.trackPointCollector = new TrackPointCollector(this, ckanMapping);
        this.dataset = dataset;
    }

    String addTrackPoint(Map<ResourceField, String> rowEntry) {
        final TrackPoint trackPoint = trackPointCollector.newTrackPoint();
        final GeometryBuilder geometryBuilder = GeometryBuilder.create();
        for (Map.Entry<ResourceField, String> fieldEntry : rowEntry.entrySet()) {
            ResourceField field = fieldEntry.getKey();
            String value = field.normalizeValue(fieldEntry.getValue());
            if (field.isField(CkanConstants.KnownFieldIdValue.OBSERVATION_TIME)) {
                trackPoint.withProperty(field, value);
            }
            if (field.isField(CkanConstants.KnownFieldIdValue.TRACK_ID)) {
                trackPoint.withProperty(field, value);
            }
            if (field.isField(CkanConstants.KnownFieldIdValue.TRACK_POINT)) {
                trackPoint.withProperty(field, value);
            } else {
                trackPoint.withProperty(field, value);
            }
            parseGeometryField(geometryBuilder, fieldEntry);
        }
        if (geometryBuilder.canBuildGeometry()) {
            trackPoint.withGeometry(geometryBuilder.getGeometry());
        }

        return trackPointCollector.addToTrack(trackPoint);
//        return trackPointCollector.getFeature(trackPoint);
    }

    SamplingFeature getFeatureFor(String trackId) {
        TrackPoint startPoint = trackPointCollector.getStart(trackId);
        return trackPointCollector.createFeature(startPoint);
    }

    SamplingFeature createFeature(Map<ResourceField, String> rowEntry) {
        final SamplingFeature feature = createEmptyFeature();
        final GeometryBuilder geometryBuilder = GeometryBuilder.create();
        for (Map.Entry<ResourceField, String> fieldEntry : rowEntry.entrySet()) {
            ResourceField field = fieldEntry.getKey();
            String value = field.normalizeValue(fieldEntry.getValue());
            if (field.isField(CkanConstants.KnownFieldIdValue.PLATFORM_ID)) {
                String orgaName = dataset.getOrganization().getName();
                feature.setIdentifier(orgaName + "-" + value);
            }
            if (field.isField(CkanConstants.KnownFieldIdValue.PLATFORM_NAME)) {
                feature.addName(value);
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

    SamplingFeature createEmptyFeature() {
        SamplingFeature emptyFeature = new SamplingFeature(null);
        emptyFeature.setIdentifier("UNINITIALIZED FEATURE");
        return emptyFeature;
    }

    private void parseGeometryField(final GeometryBuilder geometryBuilder, Map.Entry<ResourceField, String> fieldEntry) {
        ResourceField field = fieldEntry.getKey();
        String value = field.normalizeValue(fieldEntry.getValue());
        if (field.isField(CkanConstants.KnownFieldIdValue.CRS)) {
            geometryBuilder.withCrs(value);
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.LATITUDE)) {
            geometryBuilder.setLatitude(value);
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.LONGITUDE)) {
            geometryBuilder.setLongitude(value);
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.ALTITUDE)) {
            geometryBuilder.setAltitude(value);
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
