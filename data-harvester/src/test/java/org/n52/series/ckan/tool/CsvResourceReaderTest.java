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

package org.n52.series.ckan.tool;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.ResourceKey;
import org.n52.series.ckan.table.ResourceTable;
import org.n52.series.ckan.table.ResourceTestHelper;
import org.n52.series.ckan.table.TableLoadException;
import org.n52.series.ckan.table.TableLoader;
import org.n52.series.ckan.util.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore("sandbox to try out alternate csv csv->table loading strategies")
public class CsvResourceReaderTest {

    private static final String DWD_LARGE_RESOURCE_MEMBER = "b5b7e5cb-25c7-46e8-b6e5-22521cfc9a97";

    private static final String DWD_KREISE_DATASET_ID = "2518529a-fbf1-4940-8270-a1d4d0fa8c4d";

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvResourceReaderTest.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private DataCollection dataCollection;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        // String dataFolder = "/files/" + TestConstants.TEST_TRIMMED_DATA_FOLDER;
        String dataFolder = "/files/" + TestConstants.TEST_TRIMMED_DATA_FOLDER;
        ResourceTestHelper testHelper = new ResourceTestHelper(testFolder, dataFolder);
        dataCollection = testHelper.getDataCollection(DWD_KREISE_DATASET_ID);
    }

    @Test
    @Ignore
    public void readViaDefaultCsvReader() {
        long start = System.currentTimeMillis();
        Entry<ResourceMember, DataFile> largeMember = dataCollection.getDataEntry(DWD_LARGE_RESOURCE_MEMBER);
//        readResourceTable(BufferedReaderForLoopLoader::new, largeMember);
         readResourceTable(BufferedReaderStreamLoader::new, largeMember);
//         readResourceTable(CsvTableLoader::new, largeMember);
        LOGGER.info("Reading to memory took {}s", (System.currentTimeMillis() - start) / 1000d);
    }

    @Test
    public void readAllDataReader() {
        long start = System.currentTimeMillis();
//         readWholeDataCollection(CsvTableLoader::new); // 1.8s
        readWholeDataCollection(BufferedReaderStreamLoader::new); // 1.8s
//        readWholeDataCollection(BufferedReaderForLoopLoader::new); // 2.0s
        LOGGER.info("Reading to memory took {}s", (System.currentTimeMillis() - start) / 1000d);
    }

    private void readWholeDataCollection(BiFunction<ResourceTable, DataFile, TableLoader> tableLoader) {
        for (Entry<ResourceMember, DataFile> entry : dataCollection) {
            readResourceTable(tableLoader, entry);
        }
    }

    private void readResourceTable(BiFunction<ResourceTable, DataFile, TableLoader> tableLoader,
                                   Entry<ResourceMember, DataFile> entry) {
        new ResourceTable(entry) {
            @Override
            protected TableLoader createTableLoader(DataFile file) {
                return tableLoader.apply(this, entry.getValue());
            }
        }.readIntoMemory();
    }

    private static class BufferedReaderForLoopLoader extends TableLoader {

        private int lineNbr;

        public BufferedReaderForLoopLoader(DataTable table, DataFile datafile) {
            super(table, datafile);
        }

        @Override
        public void loadData() throws TableLoadException {
            DataFile dataFile = getDataFile();
            File file = dataFile.getFile();
            ResourceMember resourceMember = getResourceMember();

            lineNbr = 0;
            Pattern csvValuesPatter = Pattern.compile(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            List<String> columnHeaders = resourceMember.getColumnHeaders();
            int headerRows = resourceMember.getHeaderRows();
            try {
                List<String> allLines = Files.readAllLines(file.toPath());
                for (int i = headerRows; i < allLines.size(); i++) {
                    ResourceKey id = resourceMember.createResourceKey(lineNbr++);
                    String[] values = csvValuesPatter.split(allLines.get(i));
                    for (int j = 0; j < columnHeaders.size(); j++) {
                        final ResourceField field = resourceMember.getField(j);
                        setCellValue(id, field, values[j]);
                    }
                }
            } catch (IOException e) {
                throw new TableLoadException("Unable to read file: " + file, e);
            }

        }

    }

    private static class BufferedReaderStreamLoader extends TableLoader {

        private int lineNbr;

        public BufferedReaderStreamLoader(DataTable table, DataFile datafile) {
            super(table, datafile);
        }

        @Override
        public void loadData() throws TableLoadException {
            DataFile dataFile = getDataFile();
            File file = dataFile.getFile();
            ResourceMember resourceMember = getResourceMember();

            lineNbr = 0;
            Pattern csvValuesPatter = Pattern.compile(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            List<String> columnHeaders = resourceMember.getColumnHeaders();
            int headerRows = resourceMember.getHeaderRows();
            try {
                Files.newBufferedReader(file.toPath(), dataFile.getEncoding())
                     .lines()
                     .skip(headerRows)
                     .forEach(l -> {
                         ResourceKey id = resourceMember.createResourceKey(lineNbr++);
                         // String[] values = l.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                         String[] values = csvValuesPatter.split(l);
                         for (int j = 0; j < columnHeaders.size(); j++) {
                             final ResourceField field = resourceMember.getField(j);
                             setCellValue(id, field, values[j]);
                         }

                     });
            } catch (IOException e) {
                throw new TableLoadException("Unable to read file: " + file, e);
            }

        }

    }
}
