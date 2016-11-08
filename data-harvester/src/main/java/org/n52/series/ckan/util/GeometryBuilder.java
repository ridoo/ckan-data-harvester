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

import java.io.IOException;

import org.n52.io.crs.CRSUtils;
import org.n52.io.geojson.GeoJSONDecoder;
import org.n52.io.geojson.GeoJSONException;
import org.n52.sos.ogc.gml.ReferenceType;
import org.n52.sos.ogc.om.NamedValue;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.features.SfConstants;
import org.n52.sos.ogc.om.values.GeometryValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class GeometryBuilder {

    // TODO add line string builder

    private static final Logger LOGGER = LoggerFactory.getLogger(GeometryBuilder.class);

    private final CRSUtils utils = CRSUtils.createEpsgStrictAxisOrder();

    private final GeoJSONDecoder geoJsonDecoder = new GeoJSONDecoder();

    private ObjectMapper om = new ObjectMapper();

    private String crs = "EPSG:4326";

    private Double longitude;

    private Double latitude;

    private double altitude;

    private Geometry geometry;

    public static GeometryBuilder create() {
        return new GeometryBuilder();
    }

    public GeometryBuilder withObjectMapper(ObjectMapper om) {
        this.om = om == null
                ? this.om
                : om;
        return this;
    }

    public GeometryValue createGeometryValue() {
        return new GeometryValue(getGeometry());
    }

    public NamedValue<Geometry> createNamedValue() {
        final NamedValue<Geometry> namedValue = new NamedValue<>();
        namedValue.setName(new ReferenceType(OmConstants.PARAM_NAME_SAMPLING_GEOMETRY));
        namedValue.setValue(new GeometryValue(getGeometry()));
        return namedValue;
    }

    public boolean canBuildGeometry() {
        return geometry != null || hasCoordinates();
    }

    public Geometry getGeometry() {
        if ( !canBuildGeometry()) {
            return null;
        }
        if (geometry != null) {
            return geometry;
        }
        final Point lonLatPoint = utils.createPoint(longitude, latitude, altitude, crs);
        try {
            return utils.transformInnerToOuter(lonLatPoint, crs);
        } catch (TransformException | FactoryException e) {
            LOGGER.error("could not switch axes to conform strictly to {}", crs, e);
            return lonLatPoint;
        }
    }

    public GeometryBuilder withGeoJson(String json) {
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(json.replace("'", "\""));
            this.geometry = geoJsonDecoder.decodeGeometry(jsonNode);
        } catch (IOException | GeoJSONException e) {
            LOGGER.error("Location value is not a JSON object. Value: {}", json, e);
        }
        return this;
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

    public String getFeatureType() {
        if (getGeometry() != null) {
            if (getGeometry().getGeometryType().equalsIgnoreCase("POINT")) {
                return SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_POINT;
            } else if (getGeometry().getGeometryType().equalsIgnoreCase("LINESTRING")) {
                return SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_CURVE;
            } else if (getGeometry().getGeometryType().equalsIgnoreCase("POLYGON")) {
                return SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_SURFACE;
            }
        }

        // TODO further types?

        return "http://www.opengis.net/def/nil/OGC/0/unknown";
    }

}
