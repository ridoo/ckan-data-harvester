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

package org.n52.series.ckan.beans;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.table.ResourceTestHelper;

public class ResourceMemberTest {

    private static final String DWD_TEMPERATUR_DATASET_ID = "eab53bfe-fce7-4fd8-8325-a0fe5cdb23c8";

    private static final String DWDKREISE_DATASET_ID = "2518529a-fbf1-4940-8270-a1d4d0fa8c4d";

    private static final String PLATFORM_DATA_ID_1 = "8f0637bc-c15e-4f74-b7d8-bfc4ed2ac2f9";

    private static final String OBSERVATION_DATA_ID_1 = "2cbb2409-5591-40a9-a60b-544ebb809fb8";

    private static final String OBSERVATION_DATA_ID_2 = "515aa961-a0a2-4e4b-9dcc-a57998e19b39";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private ResourceTestHelper resourceHelper;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        resourceHelper = new ResourceTestHelper(testFolder);
    }

    @Test
    public void when_trivialCreation_then_noExceptions() {
        new ResourceMember();
    }

    @Test
    public void when_trivialCreation_then_falseOnContainsField() {
        assertThat(new ResourceMember().containsField("field_id"), is(false));
    }

    @Test
    public void when_trivialCreation_then_emptyColumnHeaders() {
        assertThat(new ResourceMember().getColumnHeaders(), is(empty()));
    }

    @Test
    public void when_havingTwoTrivials_then_notJoinable() {
        assertThat(new ResourceMember().isJoinable(new ResourceMember()), is(false));
    }

    @Test
    public void when_trivialCreation_then_emptyJoinableFields() {
        ResourceMember member = new ResourceMember();
        assertThat(new ResourceMember().getJoinableFields(member), is(empty()));
    }

    @Test
    public void when_gettingSchemaDescriptor_resourceTypeIsCsvObservationCollection() {
        String expectedType = CkanConstants.ResourceType.CSV_OBSERVATIONS_COLLECTION;
        SchemaDescriptor schemaDescriptor = resourceHelper.getSchemaDescriptor(DWD_TEMPERATUR_DATASET_ID);
        assertThat(schemaDescriptor.getSchemaDescriptionType(), Matchers.is(expectedType));
    }

    @Test
    public void when_sameResourceTypeAndColumns_then_isExtensible() {
        ResourceMember obs1 = getObservationResource(OBSERVATION_DATA_ID_1);
        ResourceMember obs2 = getObservationResource(OBSERVATION_DATA_ID_2);
        assertThat(obs1.isExtensible(obs2), is(true));
    }

    @Test
    public void when_differentResourceType_then_isNotExtensible() {
        ResourceMember obs1 = getPlatformResource(PLATFORM_DATA_ID_1);
        ResourceMember obs2 = getObservationResource(OBSERVATION_DATA_ID_2);
        assertThat(obs1.isExtensible(obs2), is(false));
    }

    @Test
    public void findJoinableFields() {
        SchemaDescriptor schemaDescriptor = resourceHelper.getSchemaDescriptor(DWD_TEMPERATUR_DATASET_ID);
        List<ResourceMember> members = schemaDescriptor.getMembers();

        ResourceMember platformDescription = members.get(0);
        ResourceMember observationDescription = members.get(1);
        Set<ResourceField> joinableFields = platformDescription.getJoinableFields(observationDescription);
        assertThat(joinableFields.size(), is(1));
        assertThat(joinableFields.iterator()
                                 .next()
                                 .getFieldId(),
                   is("STATIONS_ID"));
    }

    @Test
    public void findJoinableFieldsHavingMappedIds() {
        SchemaDescriptor schemaDescriptor = resourceHelper.getSchemaDescriptor(DWDKREISE_DATASET_ID);
        List<ResourceMember> members = schemaDescriptor.getMembers();

        ResourceMember firstMember = members.get(0);
        ResourceMember secondMember = members.get(1);
        Set<ResourceField> joinableFields = firstMember.getJoinableFields(secondMember);
        assertThat(joinableFields.size(), is(1));
        FieldBuilder builder = FieldBuilder.aField();
        ResourceField expectedField = builder.createSimple("warncellid");
        ResourceField expectedAlternateField = builder.createSimple("gc_warncellid");
        Iterator<ResourceField> iterator = joinableFields.iterator();
        ResourceField firstField = iterator.next();
        assertThat(firstField, is(expectedField));
        assertThat(firstField, is(expectedAlternateField));
    }

    private ResourceMember getObservationResource(String resourceId) {
        ResourceMember resourceMember = new ResourceMember(resourceId, "observations");
        return resourceHelper.getResourceMember(DWD_TEMPERATUR_DATASET_ID, resourceMember);
    }

    private ResourceMember getPlatformResource(String resourceId) {
        ResourceMember resourceMember = new ResourceMember(resourceId, "platforms");
        return resourceHelper.getResourceMember(DWD_TEMPERATUR_DATASET_ID, resourceMember);
    }

    @Test
    public void when_columnHasNoShortName_then_fieldIdIsColumnHeader() {
        ResourceMember resourceMember = new ResourceMember();
        ResourceField field = FieldBuilder.aField()
                                          .createSimple("foo");
        resourceMember.setResourceFields(Collections.singletonList(field));
        List<String> columnHeaders = resourceMember.getColumnHeaders();
        assertThat(columnHeaders.size(), is(1));
        assertThat(columnHeaders.get(0), is("foo"));
    }

    @Test
    public void when_fieldHasMappedIdValues_then_containsEvaluatesTrueInCaseOfAlternateValues() {
        ResourceMember resourceMember = new ResourceMember();
        ResourceField first = FieldBuilder.aField()
                                          .withFieldMappings("foo", "bar")
                                          .createSimple("foo");
        resourceMember.setResourceFields(Collections.singletonList(first));
        assertThat(resourceMember.containsField("bar"), is(true));
    }

    @Test
    public void when_joinFieldsWithMappedValues_then_detectIfJoinable() {
        ResourceMember resourceMemberA = new ResourceMember("id_A", "type_A");
        ResourceField[] fieldsA = {
            FieldBuilder.aField()
                        .withFieldMappings("foo", "bar")
                        .createSimple("foo"),
            FieldBuilder.aField()
                        .createSimple("blah")
        };
        resourceMemberA.setResourceFields(Arrays.asList(fieldsA));

        ResourceMember resourceMemberB = new ResourceMember("id_B", "type_B");
        ResourceField[] fieldsB = {
            FieldBuilder.aField()
                        .withFieldMappings("foo", "bar")
                        .createSimple("bar"),
        };
        resourceMemberB.setResourceFields(Arrays.asList(fieldsB));
        assertThat("resource members with mapped join columns must be joinable",
                   resourceMemberA.isJoinable(resourceMemberB),
                   is(true));
    }
}
