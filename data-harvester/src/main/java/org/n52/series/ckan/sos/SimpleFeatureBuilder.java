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

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.da.CkanMapping;
import org.n52.series.ckan.util.AbstractRowVisitor;
import org.n52.series.ckan.util.GeometryBuilder;
import org.n52.sos.exception.ows.concrete.InvalidSridException;
import org.n52.sos.ogc.om.features.samplingFeatures.SamplingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class SimpleFeatureBuilder extends AbstractRowVisitor<SamplingFeature> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SimpleFeatureBuilder.class);

    private static final String UNINITIALIZED_FEATURE_ID = "UNINITIALIZED FEATURE";

    private final GeometryBuilder geometryBuilder;

    private final SamplingFeature feature;

    private final CkanDataset dataset;

    public SimpleFeatureBuilder(CkanDataset dataset) {
        this(dataset, CkanMapping.loadCkanMapping());
    }

    public SimpleFeatureBuilder(CkanDataset dataset, CkanMapping ckanMapping) {
        this.geometryBuilder = GeometryBuilder.create();
        this.feature = createEmptyFeature();
        this.dataset = dataset;
    }

    static SamplingFeature createEmptyFeature() {
        SamplingFeature emptyFeature = new SamplingFeature(null);
        emptyFeature.setIdentifier(UNINITIALIZED_FEATURE_ID);
        return emptyFeature;
    }

    @Override
    public void visit(ResourceField field, String value) {
        if (field.isField(CkanConstants.KnownFieldIdValue.PLATFORM_ID)) {
            String orgaName = dataset.getOrganization().getName();
            feature.setIdentifier(orgaName + "-" + value);
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.PLATFORM_NAME)) {
            feature.addName(value);
        }
        if ( !field.isOfResourceType(CkanConstants.ResourceType.OBSERVATIONS_WITH_GEOMETRIES)) {
            geometryBuilder.visit(field, value);
        }
    }


    @Override
    public boolean hasResult() {
        return !feature.getIdentifier()
                .equals(UNINITIALIZED_FEATURE_ID);
    }

    @Override
    public SamplingFeature getResult() {
        if (geometryBuilder.canBuildGeometry()) {
            setFeatureGeometry(feature, geometryBuilder.getGeometry());
            feature.setFeatureType(geometryBuilder.getFeatureType());
        }
        return feature;
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
