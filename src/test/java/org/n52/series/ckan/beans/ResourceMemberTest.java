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
package org.n52.series.ckan.beans;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.n52.series.ckan.da.CkanConstants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class ResourceMemberTest {

    private static final String SCHEMA_DESCRIPTOR = "/files/dwd/temperature-dwd/schema_descriptor.json";

    private SchemaDescriptor descriptor;

    @Before
    public void setUp() throws IOException {
        ObjectMapper om = new ObjectMapper();
        final JsonNode node = om.readTree(getClass().getResource(SCHEMA_DESCRIPTOR));
        descriptor = new SchemaDescriptor(new CkanDataset(), node);
        assertThat(descriptor.getSchemaDescriptionType(), Matchers.is(CkanConstants.ResourceType.CSV_OBSERVATIONS_COLLECTION));
    }

    @Test
    public void findJoinableFields() {
        List<ResourceMember> members = descriptor.getMembers();
        ResourceMember platformDescription = members.get(0);
        ResourceMember observationDescription = members.get(1);
        Set<ResourceField> joinableFields = platformDescription.getJoinableFields(observationDescription);
        assertThat(joinableFields.size(), is(6));
    }
}
