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
package org.n52.series.ckan.cache;

import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanPair;
import java.util.Collections;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.n52.series.ckan.beans.SchemaDescriptor;
import org.n52.series.ckan.da.CkanConstants;

public class InMemoryCkanMetadataStoreTest {

    private InMemoryMetadataStore metadataStore;

    private final String simpleDescriptor = "{"
            + "  \"resource_type\":\"csv-observations-collection\","
            + "  \"schema_descriptor_version\":\"0.1\","
            + "  \"members\":["
            + "  ]"
            + "}";

    @Before
    public void setUp() {
        metadataStore = new InMemoryMetadataStore();
    }

    @Test
    public void shouldInstantiateEmpty() {
        final Iterable<String> ids = metadataStore.getDatasetIds();
        MatcherAssert.assertThat(ids.iterator().hasNext(), CoreMatchers.is(false));
    }

    @Test
    public void shouldReturnResourceDescription() {
        CkanDataset dataset = new CkanDataset("test-dataset");
        CkanPair extras = new CkanPair(CkanConstants.SchemaDescriptor.SCHEMA_DESCRIPTOR, simpleDescriptor);
        dataset.setExtras(Collections.singletonList(extras));

        metadataStore.insertOrUpdate(dataset);
        final SchemaDescriptor actual = metadataStore.getSchemaDescription(dataset.getId());
        String actualVersion = actual.getVersion();
        MatcherAssert.assertThat(actualVersion, CoreMatchers.is("0.1"));
    }

    @Test
    public void when_havingCustomMappingFile_then_customMappingsAreUsed() {
        CkanDataset dataset = new CkanDataset("test-dataset");
        dataset.setId("test"); // will use config-ckan-mapping-test.json
        CkanPair extras = new CkanPair("custom_descriptor_key", simpleDescriptor);
        dataset.setExtras(Collections.singletonList(extras));

        metadataStore.insertOrUpdate(dataset);
        final SchemaDescriptor actual = metadataStore.getSchemaDescription(dataset.getId());
        String actualId = actual.getVersion();
        MatcherAssert.assertThat(actualId, CoreMatchers.is("0.1"));
    }
}
