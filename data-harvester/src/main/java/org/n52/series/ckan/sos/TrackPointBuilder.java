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
import org.n52.series.ckan.sos.TrackPointCollector.TrackPoint;
import org.n52.series.ckan.util.AbstractRowVisitor;
import org.n52.series.ckan.util.FieldVisitor;
import org.n52.series.ckan.util.GeometryBuilder;

public class TrackPointBuilder extends AbstractRowVisitor<String> {

    private final TrackPointCollector trackPointCollector;

    private GeometryBuilder geometryBuilder;

    private TrackPoint trackPoint;

    public TrackPointBuilder(TrackPointCollector trackPointCollector) {
        this.trackPointCollector = trackPointCollector;
    }

    @Override
    public void init() {
        this.trackPoint = trackPointCollector.newTrackPoint();
        this.geometryBuilder = GeometryBuilder.create();
    }

    @Override
    public FieldVisitor<String> visit(ResourceField field, String value) {
        if (field.isField(CkanConstants.KnownFieldIdValue.OBSERVATION_TIME)) {
            trackPoint.withProperty(field, value);
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.TRACK_ID)) {
            trackPoint.withProperty(field, value);
        }
        if (field.isField(CkanConstants.KnownFieldIdValue.TRACK_POINT)) {
            trackPoint.withProperty(field, value);
        }
        geometryBuilder.visit(field, value);
        return this;
    }

    @Override
    public boolean hasResult() {
        return trackPoint.isValid();
    }

    @Override
    public String getResult() {
        if (geometryBuilder.canBuildGeometry()) {
            trackPoint.setGeometry(geometryBuilder.getGeometry());
        }
        return trackPointCollector.addToTrack(trackPoint);
    }
}
