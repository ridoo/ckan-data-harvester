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

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import org.n52.series.ckan.util.JsonUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class SerializableCkanDataset implements Serializable {

    private static final long serialVersionUID = 830823271754623889L;

    private final String ckanDatasetAsJson;

    private final String id;

    public SerializableCkanDataset(CkanDataset dataset) throws JsonProcessingException {
        this.ckanDatasetAsJson = JsonUtil.getCkanObjectWriter()
                                         .writeValueAsString(dataset);
        this.id = dataset.getId();
    }

    public CkanDataset getCkanDataset() throws IOException {
        return JsonUtil.getCkanObjectMapper()
                       .readValue(ckanDatasetAsJson, CkanDataset.class);
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SerializableCkanDataset)) {
            return false;
        }
        SerializableCkanDataset other = (SerializableCkanDataset) obj;
        return Objects.equals(id, other.id);
    }

}
