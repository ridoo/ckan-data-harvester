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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.IllegalFormatException;

import org.n52.series.ckan.da.CkanMapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResourceFieldCreator {

    private CkanMapping ckanMapping;

    public ResourceFieldCreator() {
        this.ckanMapping = CkanMapping.loadCkanMapping();
    }

    public ResourceFieldCreator withCkanMapping(CkanMapping ckanMapping) {
        this.ckanMapping = ckanMapping;
        return this;
    }

    public ResourceField createSimple(String id) {
        return createFull("{ \"field_id\": \"%s\", \"field_type\": \"string\" }", id);
    }

    public ResourceField createFull(String json, Object... args) {
        try {
            json = String.format(json, args);
            JsonNode node = new ObjectMapper().readTree(json);
            return new ResourceField(node, 0)
                    .withCkanMapping(ckanMapping);
        } catch (IllegalFormatException | IOException e) {
            e.printStackTrace();
            fail("Could not create field!");
            return null;
        }

    }

}
