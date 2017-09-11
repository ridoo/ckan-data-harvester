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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.csv.CSVParser;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.FieldBuilder;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;

public class CsvTableLoaderTest {

    @Test
    public void when_csvContainsQuotedValueWithCommas_then_valueGetParsedCorrectly() throws TableLoadException {
        ResourceMember member = new ResourceMember();
        ResourceField[] fields = {
            FieldBuilder.aFieldAt(0).createSimple("A"),
            FieldBuilder.aFieldAt(1).createSimple("B"),
        };
        member.setResourceFields(Arrays.asList(fields));

        DataTable table = new DataTable(member);
        String rawCsv = "\"quoted,value\",another value";
        CsvTableLoader tableLoader = new IntputStreamCsvTableLoader(rawCsv, table);
        tableLoader.loadData();
        MatcherAssert.assertThat(table.rowSize(), CoreMatchers.is(1));
        MatcherAssert.assertThat(table.columnSize(), CoreMatchers.is(2));
    }

    private static class IntputStreamCsvTableLoader extends CsvTableLoader {

        private String rawCsv;

        public IntputStreamCsvTableLoader(String rawCsv, DataTable table) {
            super(table, new DataFile());
            this.rawCsv = rawCsv;
        }

        @Override
        protected CSVParser createCsvParser(DataFile dataFile) throws FileNotFoundException, IOException {
            ByteArrayInputStream rawCsvStream = new ByteArrayInputStream(rawCsv.getBytes());
            InputStreamReader streamReader = new InputStreamReader(rawCsvStream, "UTF-8");
            return super.createCsvParser(0, streamReader, Charset.forName("UTF-8"));
        }

    }
}
