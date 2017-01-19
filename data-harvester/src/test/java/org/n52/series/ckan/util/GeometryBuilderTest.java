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
package org.n52.series.ckan.util;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Geometry;

public class GeometryBuilderTest {

    private GeometryBuilder builder;

    @Before
    public void setUp() {
        this.builder = new GeometryBuilder();
    }

    @Test
    public void when_nullValues_then_returnEmptyGeometry() {
        Geometry geometry = builder.getGeometry();
        Assert.assertThat(geometry, IsNull.nullValue());
    }

    @Test
    public void when_geoJsonPoint_then_returnGeometry() {
        Geometry geometry = builder.withGeoJson("{'coordinates':[52.52,13.41],'type':'Point'}")
                          .getGeometry();
        Assert.assertThat(geometry,
                          instanceOf(Geometry.class));
    }

    @Test
    public void when_geoJsonLPolygon_then_returnGeometry() {
        Assert.assertThat(builder.withGeoJson(""
                + "{" +
                "    \"type\": \"Polygon\"," +
                "    \"coordinates\": [" +
                "        [" +
                "            [100.0, 0.0]," +
                "            [101.0, 0.0]," +
                "            [101.0, 1.0]," +
                "            [100.0, 1.0]," +
                "            [100.0, 0.0]" +
                "        ]" +
                "    ]" +
                "}").getGeometry(), instanceOf(Geometry.class));
    }

    @Test
    public void when_wkt2DPoint_then_returnGeometry() {
        Geometry geometry = builder.withWKT("POINT(52.52 13.41)")
                          .getGeometry();
        Assert.assertThat(geometry, instanceOf(Geometry.class));
    }

    @Test
    public void when_wkt3DPoint_then_returnGeometry() {
        Geometry geometry = builder.withWKT("POINT(52.52 13.41 30)")
                          .getGeometry();
        Assert.assertThat(geometry, instanceOf(Geometry.class));
    }

    @Test
    public void when_wktPointWithSrid_then_geometryHasSrid() {
        Geometry geometry = builder.withWKT("POINT(52.52 13.41 30)")
                .withCrs("999")
                .getGeometry();
        Assert.assertThat(geometry.getSRID(), is(999));
    }
}
