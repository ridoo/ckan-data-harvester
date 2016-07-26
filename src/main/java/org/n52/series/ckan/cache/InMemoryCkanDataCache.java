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

import java.util.HashMap;
import java.util.Map;

import org.n52.series.ckan.beans.CsvObservationsCollection;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class InMemoryCkanDataCache implements CkanDataSink {

    private final Map<String, Entry<CkanDataset, CsvObservationsCollection>> datasets = new HashMap<>();

    @Override
    public void insertOrUpdate(CkanDataset dataset, CsvObservationsCollection csvObservationsCollection) {
        if (dataset == null) {
            return;
        }

        if (datasets.containsKey(dataset.getId())) {
            // TODO update
        } else {
            datasets.put(dataset.getId(), new Entry<>(dataset, csvObservationsCollection));
        }
    }

    public Iterable<Entry<CkanDataset, CsvObservationsCollection>> getCollections() {
        return datasets.values();
    }

    public class Entry<M,D> {
        private M dataset;
        private D data;

        public Entry(M dataset, D data) {
            this.dataset = dataset;
            this.data = data;
        }

        public M getDataset() {
            return dataset;
        }

        public void setDataset(M dataset) {
            this.dataset = dataset;
        }

        public D getData() {
            return data;
        }

        public void setData(D data) {
            this.data = data;
        }

    }

}
