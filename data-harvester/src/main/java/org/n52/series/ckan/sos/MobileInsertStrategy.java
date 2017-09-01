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
import org.n52.series.ckan.da.CkanMapping;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.ResourceKey;
import org.n52.sos.ogc.gml.AbstractFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class MobileInsertStrategy extends AbstractInsertStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileInsertStrategy.class);

    public MobileInsertStrategy() {
        super();
    }

    MobileInsertStrategy(CkanSosReferenceCache ckanSosReferencingCache) {
        super(ckanSosReferencingCache);
    }

    @Override
    public Map<String, DataInsertion> createDataInsertions(DataTable dataTable, DataCollection dataCollection) {
        List<Phenomenon> phenomena = parsePhenomena(dataTable);

        ResourceMember member = dataTable.getResourceMember();
        CkanDataset dataset = dataCollection.getDataset();
        CkanMapping ckanMapping = member.getCkanMapping();
        Procedure procedure = new Procedure(dataset.getId(), dataset.getName());

        SensorBuilder sensorBuilderTemplate = SensorBuilder.create()
                .withProcedure(procedure)
                .withDataset(dataset)
                .setMobile(true);

        LOGGER.debug("Create mobile insertions ...");
        Map<String, DataInsertion> dataInsertions = new HashMap<>();
        TrackPointCollector trackCollector = new TrackPointCollector(ckanMapping);
        TrackPointBuilder trackBuilder = new TrackPointBuilder(trackCollector);
        for (Entry<ResourceKey, Map<ResourceField, String>> rowEntry : dataTable.getTable().rowMap().entrySet()) {

            /*
             * Track points have to be collected first to create the
             * feature afterwards (need to determine first observation
             * value of a track for the feature id).
             */
            trackBuilder.visit(rowEntry.getValue());
            String trackId = trackBuilder.getResult();

            for (Phenomenon phenomenon : phenomena) {
                sensorBuilderTemplate.addPhenomenon(phenomenon);

                if ( !dataInsertions.containsKey(trackId)) {
                    LOGGER.debug("Building sensor with: procedure '{}'", trackId);
                    DataFile dataFile = dataCollection.getDataFile(member);
                    DataInsertion dataInsertion = createDataInsertion(sensorBuilderTemplate, dataFile);
                    dataInsertions.put(trackId, dataInsertion);
                }

                DataInsertion dataInsertion = dataInsertions.get(trackId);
                final SosObservation observation = ObservationBuilder
                        .create(phenomenon, rowEntry.getKey())
                        .withUomParser(getUomParser())
                        .withSensorBuilder(sensorBuilderTemplate)
                        .visit(rowEntry.getValue())
                        .getResult();

                if (observation != null) {
                    dataInsertion.addObservation(observation);
                }
            }
        }
        for (Entry<String, DataInsertion> entry : dataInsertions.entrySet()) {
            String trackId = entry.getKey();
            DataInsertion dataInsertion = entry.getValue();
            SensorBuilder template = dataInsertion.getSensorBuilder();
            AbstractFeature feature = trackCollector.getFeatureFor(trackId);
            dataInsertion.setSensorBuilder(template.copy().withFeature(feature));
        }
        return dataInsertions;
    }



}
