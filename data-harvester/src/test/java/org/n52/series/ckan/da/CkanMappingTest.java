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
package org.n52.series.ckan.da;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class CkanMappingTest {

    @Test
    public void when_parsingIdMappings_then_idMappingsNotEmpty() {
        CkanMapping mappings = CkanMapping.loadCkanMapping("config-ckan-mapping.json");
        assertThat(mappings.getFieldMappings(CkanConstants.KnownFieldIdValue.OBSERVATION_TIME), containsInAnyOrder("resulttime", "result_time", "datetime", "mess_datum", "timestamp", "observation_time"));
        assertThat(mappings.getFieldMappings("latitude"), containsInAnyOrder("geobreite", "latitude", "lat"));
        assertThat(mappings.getFieldMappings("longitude"), containsInAnyOrder("geolaenge", "longitude", "lon"));
    }

    @Test
    public void when_arbitraryConfigFileName_then_readConfig() {
         CkanMapping ckanMappin = CkanMapping.loadCkanMapping("some-ckan-config.json");
         assertTrue(ckanMappin.hasFieldMappings("crs", "9999"));
    }

    @Test
    public void when_retrieveMappings_then_nameIsIncluded() {
         CkanMapping ckanMappin = CkanMapping.loadCkanMapping("some-ckan-config.json");
         assertTrue(ckanMappin.hasFieldMappings("crs", "crs"));
    }

    @Test
    public void when_noMappings_then_onlyNameIsIncluded() {
         CkanMapping ckanMappin = CkanMapping.loadCkanMapping("some-ckan-config.json");
         assertThat(ckanMappin.getMappings("some", "does-not-exist"), hasSize(1));
         assertTrue(ckanMappin.hasMappings("some", "does-not-exist", "does-not-exist"));
    }

    @Test
    public void when_parsingDefault_then_idMappingsNotEmpty() {
        CkanMapping mappings = CkanMapping.loadCkanMapping();
        assertTrue(mappings.hasMappings("", "default", "default"));
    }

}
