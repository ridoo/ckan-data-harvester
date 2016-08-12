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
package org.n52.series.ckan.table;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvTableLoader extends TableLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvTableLoader.class);

    public CsvTableLoader(DataTable table, DataFile datafile) {
        super(table, datafile);
    }

    @Override
    public void loadData() throws TableLoadException {
        ResourceMember resourceMember = getResourceMember();
        try (CSVParser csvParser = createCsvParser(getDataFile())) {
            Iterator<CSVRecord> iterator = csvParser.iterator();
            List<String> columnHeaders = resourceMember.getColumnHeaders();

            for (int i = 0 ; i < resourceMember.getHeaderRows() ; i++) {
                iterator.next(); // skip
            }
            int lineNbr = 0;
            int ignoredCount = 0;
            while (iterator.hasNext()) {
                CSVRecord line = iterator.next();
                LOGGER.trace("parsing line '{}'", line.toString());
//                if (line.size() != columnHeaders.size()) {
                if ( !line.isConsistent()) {

                    // TODO choose csv parsing strategy

                    LOGGER.trace("headers: {}", Arrays.toString(columnHeaders.toArray()));
                    LOGGER.trace("ignore line: #columnheaders != #csvValues");
                    LOGGER.trace("line: {}", line);
                    ignoredCount++;
                    continue;
                }
                ResourceKey id = new ResourceKey("" + lineNbr++, resourceMember);
                for (int j = 0 ; j < columnHeaders.size() ; j++) {
                    final ResourceField field = resourceMember.getField(j);
                    final String value = line.get(j);
                    addRow(id, field, value);
                }
            }

            if (ignoredCount > 0) {
                LOGGER.debug("#{} ignored rows as not matching rowheader. "
                        + "Set LOG level to TRACE to log each.", ignoredCount);
            }

        } catch (Throwable e) { // RuntimeExceptions in csvParser#getNextRecord()
            throw new TableLoadException("could not parse csv data", e);
        }
    }

    private CSVParser createCsvParser(final DataFile dataFile) throws FileNotFoundException, IOException {
        Charset encoding = dataFile.getEncoding();
        final Path filePath = dataFile.getFile().toPath();
        int headerRows = getResourceMember().getHeaderRows();
        FileInputStream fis = new FileInputStream(filePath.toFile());
        InputStreamReader fileReader = new InputStreamReader(fis, encoding);
        return new CSVParser(fileReader, CSVFormat.DEFAULT, headerRows, 0);
    }
}
