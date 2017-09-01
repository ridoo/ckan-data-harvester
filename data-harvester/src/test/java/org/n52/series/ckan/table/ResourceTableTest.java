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
package org.n52.series.ckan.table;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.FieldBuilder;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.sos.Phenomenon;
import org.n52.series.ckan.sos.PhenomenonParser;

import com.google.common.collect.Table;

public class ResourceTableTest {

    private static final String DWD_TEMPERATUR_DATASET_ID = "eab53bfe-fce7-4fd8-8325-a0fe5cdb23c8";

    private static final String DWD_PLATFORM_DATA_ID_1 = "8f0637bc-c15e-4f74-b7d8-bfc4ed2ac2f9";

    private static final String OBSERVATION_DATA_ID_1 = "2cbb2409-5591-40a9-a60b-544ebb809fb8";

    private static final String OBSERVATION_DATA_ID_2 = "515aa961-a0a2-4e4b-9dcc-a57998e19b39";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private ResourceTestHelper tableHelper;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        tableHelper = new ResourceTestHelper(testFolder);
    }

    @Test
    public void when_trivialCreation_then_nonNullTable() {
        assertThat(new ResourceTable().getTable(), is(notNullValue(Table.class)));
    }

    @Test
    public void when_twoTrivialTables_then_emptyOutput() {
        ResourceTable table = new ResourceTable();
        ResourceTable other = new ResourceTable();
        DataTable joinedTable = table.innerJoin(other);
        assertThat(joinedTable.getTable(), is(notNullValue(Table.class)));
        assertThat(joinedTable.getTable().size(), is(0));
    }

    @Test
    public void when_joinNonTrivialWithTrivial_outputHasRowsOfNonTrivial() {
        ResourceTable nonTrivial = tableHelper.readObservationTable(DWD_TEMPERATUR_DATASET_ID, OBSERVATION_DATA_ID_1);

        List<String> expectedColumns = nonTrivial.getResourceMember().getColumnHeaders();
        DataTable joined = nonTrivial.innerJoin(new ResourceTable());
        assertThat(nonTrivial.rowSize(), is(joined.rowSize()));
        assertThat(expectedColumns, is(joined.getResourceMember().getColumnHeaders()));
        assertThat(joined.getResourceMember().getResourceType(), is("observations"));
    }

    @Test
    public void when_joiningTrivialWithNonTrivial_outputHasRowsOfNonTrivial() {
        ResourceTable nonTrivial = tableHelper.readObservationTable(DWD_TEMPERATUR_DATASET_ID, OBSERVATION_DATA_ID_1);

        List<String> expectedHeaders = nonTrivial.getResourceMember().getColumnHeaders();
        DataTable joined = new ResourceTable().innerJoin(nonTrivial);
        assertThat(nonTrivial.rowSize(), is(joined.rowSize()));
        assertThat(expectedHeaders, is(joined.getResourceMember().getColumnHeaders()));
        assertThat(joined.getResourceMember().getResourceType(), is("observations"));
    }

    @Test
    public void when_joiningDataOfSameCollectionAndType_rowsAreAddedToOutput() {
        ResourceTable nonTrivial1 = tableHelper.readObservationTable(DWD_TEMPERATUR_DATASET_ID, OBSERVATION_DATA_ID_1);
        ResourceTable nonTrivial2 = tableHelper.readObservationTable(DWD_TEMPERATUR_DATASET_ID, OBSERVATION_DATA_ID_2);
        DataTable output = nonTrivial1.extendWith(nonTrivial2);
        assertThat(output.getResourceMember().getResourceType(), is("observations"));

        ResourceMember member1 = nonTrivial1.getResourceMember();
        ResourceMember member2 = nonTrivial2.getResourceMember();
        assertThat(member1.getColumnHeaders(), is(member2.getColumnHeaders()));

        int expectedRowSize = nonTrivial1.rowSize() + nonTrivial2.rowSize();
        assertThat(output.rowSize(), is(expectedRowSize));

        List<String> actualColumnHeaders = output.getResourceMember().getColumnHeaders();
        List<String> expectedColumnHeaders = member1.getColumnHeaders();
        assertThat(actualColumnHeaders, is(expectedColumnHeaders));
    }

    @Test
    public void when_joiningDataOfSameCollectionButDifferentType_joinColumnsAppearJustOnce() {
        ResourceTable nonTrivial1 = tableHelper.readObservationTable(DWD_TEMPERATUR_DATASET_ID, OBSERVATION_DATA_ID_1);
        ResourceTable nonTrivial2 = tableHelper.readPlatformTable(DWD_TEMPERATUR_DATASET_ID, DWD_PLATFORM_DATA_ID_1);
        DataTable output = nonTrivial1.innerJoin(nonTrivial2);

        ResourceMember member1 = nonTrivial1.getResourceMember();
        ResourceMember member2 = nonTrivial2.getResourceMember();
        int joinColumnSize = member1.getJoinableFields(member2).size();

        int allColumnSize = nonTrivial1.columnSize() + nonTrivial2.columnSize();
        assertThat(output.columnSize(), is(allColumnSize - joinColumnSize));
    }

    @Test
    public void when_joiningDataOfSameCollectionButDifferentType_phenomenaOfBothTablesAreAvailable() {
        ResourceTable nonTrivial1 = tableHelper.readObservationTable(DWD_TEMPERATUR_DATASET_ID, OBSERVATION_DATA_ID_1);
        ResourceTable nonTrivial2 = tableHelper.readPlatformTable(DWD_TEMPERATUR_DATASET_ID, DWD_PLATFORM_DATA_ID_1);
        DataTable output = nonTrivial1.innerJoin(nonTrivial2);

        PhenomenonParser parser = new PhenomenonParser();
        List<Phenomenon> phenomenaObservations = parser.parse(nonTrivial1.getResourceFields());
        List<Phenomenon> phenomenaPlatforms = parser.parse(nonTrivial2.getResourceFields());
        List<Phenomenon> allPhenomena = parser.parse(output.getResourceFields());
        assertThat(allPhenomena.size(), is(phenomenaObservations.size() + phenomenaPlatforms.size()));
    }

    @Test
    public void when_twoJoinColumnsWithSwitchedValues_then_noRowsGetsJoined() {
        ResourceMember first = new ResourceMember("TABLE_A", "aType");
        ResourceField firstA = FieldBuilder.aFieldAt(0).createSimple("A");
        ResourceField firstB = FieldBuilder.aFieldAt(1).createSimple("B");
        ResourceTable table = prepareTable(firstA, firstB, first);


        ResourceMember second = new ResourceMember("TABLE_B", "bType");
        ResourceField secondA = FieldBuilder.aFieldAt(0).createSimple("A");
        ResourceField secondB = FieldBuilder.aFieldAt(1).createSimple("B");
        ResourceTable other = prepareTable(secondB, secondA, second);

        DataTable joinedTable = table.innerJoin(other);
        assertThat(joinedTable.rowSize(), is(0));
    }

    private ResourceTable prepareTable(ResourceField first, ResourceField second, ResourceMember member) {
        member.setResourceFields(Arrays.asList(first, second));
        ResourceTable table = new ResourceTable(member, new DataFile());
        table.setCellValue(member.createResourceKey(first.getIndex()), first , "A");
        table.setCellValue(member.createResourceKey(second.getIndex()), second , "B");
        return table;
    }

}
