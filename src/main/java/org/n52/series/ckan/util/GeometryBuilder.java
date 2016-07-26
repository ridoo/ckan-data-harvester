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
package org.n52.series.ckan.util;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.n52.io.crs.CRSUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeometryBuilder {

    // TODO add line string builder

    private static final Logger LOGGER = LoggerFactory.getLogger(GeometryBuilder.class);

    private final CRSUtils utils = CRSUtils.createEpsgStrictAxisOrder();

    private String crs = "EPSG:4326";

    private Double longitude;

    private Double latitude;

    private double altitude;

    public static GeometryBuilder create() {
        return new GeometryBuilder();
    }

    public GeometryBuilder withCrs(String crs) {
        this.crs = crs;
        return this;
    }

    public GeometryBuilder setLongitude(String lon) {
        this.longitude = parseToDouble(lon);
        return this;
    }

    public GeometryBuilder setLatitude(String lat) {
        this.latitude = parseToDouble(lat);
        return this;
    }

    public GeometryBuilder setAltitude(String alt) {
        this.altitude = parseToDouble(alt);
        return this;
    }

    private double parseToDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOGGER.error("invalid coordinate value: {}", value, e);
            return 0d;
        }
    }

    public boolean hasCoordinates() {
        return longitude != null &&  latitude !=  null;
    }

    public Geometry getPoint() {
        if (!hasCoordinates()) {
            return null;
        }
        final Point lonLatPoint = utils.createPoint(longitude, latitude, altitude, crs);
        try {
            return utils.transformInnerToOuter(lonLatPoint, crs);
        } catch (TransformException | FactoryException e) {
            LOGGER.error("could not switch axes to conform strictly to {}", crs, e);
            return lonLatPoint;
        }
    }

}
