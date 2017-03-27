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
package org.n52.series.ckan.sos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.n52.series.ckan.beans.DataCollection;
import org.n52.series.ckan.beans.DataFile;
import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.ResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.model.CkanDataset;

class StationaryInsertStrategy extends AbstractInsertStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(StationaryInsertStrategy.class);

    StationaryInsertStrategy() {
        super(null);
    }

    StationaryInsertStrategy(CkanSosReferenceCache ckanSosReferenceCache) {
        super(ckanSosReferenceCache);
    }

    @Override
    public Map<String, DataInsertion> createDataInsertions(DataTable dataTable, DataCollection dataCollection) {
        final List<Phenomenon> phenomena = parsePhenomena(dataTable);

        LOGGER.debug("Create stationary insertions ...");
        Map<String, DataInsertion> dataInsertions = new HashMap<>();
        for (Entry<ResourceKey, Map<ResourceField, String>> rowEntry : dataTable.getTable().rowMap().entrySet()) {

            // TODO how and what to create in which order depends on the actual strategy chosen

            CkanDataset dataset = dataCollection.getDataset();
            ResourceMember member = rowEntry.getKey().getMember();
            SimpleFeatureBuilder foiBuilder = new SimpleFeatureBuilder(dataset);
            foiBuilder.visit(rowEntry.getValue());

            SensorBuilder sensorBuilder = SensorBuilder.create()
                    .withFeature(foiBuilder.getResult())
                    .withDataset(dataset)
                    .setMobile(false);

            for (Phenomenon phenomenon : phenomena) {
                sensorBuilder.addPhenomenon(phenomenon);
                String procedureId = sensorBuilder.getProcedureId();
                if ( !dataInsertions.containsKey(procedureId)) {
                    LOGGER.debug("Building sensor with: procedure '{}', phenomenon '{}' (unit '{}')",
                                 procedureId,
                                 phenomenon.getLabel(),
                                 phenomenon.getUom());
                    DataFile dataFile = dataCollection.getDataFile(member);
                    DataInsertion dataInsertion = createDataInsertion(sensorBuilder, dataFile);
                    dataInsertions.put(procedureId, dataInsertion);
                }

                DataInsertion dataInsertion = dataInsertions.get(procedureId);
                final SosObservation observation = ObservationBuilder
                        .create(phenomenon, rowEntry.getKey())
                        .withUomParser(getUomParser())
                        .withSensorBuilder(sensorBuilder)
                        .visit(rowEntry.getValue())
                        .getResult();
                if (observation != null) {
                    dataInsertion.addObservation(observation);
                }
            }
        }
        return dataInsertions;
    }

}
