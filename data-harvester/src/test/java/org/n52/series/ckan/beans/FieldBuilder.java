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


import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;

import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.da.CkanMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FieldBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldBuilder.class);

    private static final JsonNodeFactory JSON_FACTORY = JsonNodeFactory.instance;

    private final Map<String, JsonNode> valuesByField;

    private CkanMapping ckanMapping;

    private String resourceType;

    private int index;

    public FieldBuilder() {
        this(0);
    }

    public FieldBuilder(int index) {
        this(null, index);
    }

    public FieldBuilder(String id, int index) {
        this.index = index;
        this.valuesByField = new HashMap<>();
        this.ckanMapping = CkanMapping.loadCkanMapping();
        if (id != null) {
            this.valuesByField.put("field_id", JSON_FACTORY.textNode(id));
        }
    }

    public static FieldBuilder aField() {
        return FieldBuilder.aFieldAt(0);
    }

    public static FieldBuilder aFieldAt(int index) {
        return new FieldBuilder(index);
    }

    public FieldBuilder withIndex(int index) {
        this.index = index;
        return this;
    }

    public FieldBuilder withResourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public FieldBuilder withCkanMapping(CkanMapping ckanMapping) {
        this.ckanMapping = ckanMapping;
        return this;
    }

    public FieldBuilder withFieldId(String fieldId) {
        return withProperty(CkanConstants.FieldPropertyName.FIELD_ID, fieldId);
    }

    public FieldBuilder withFieldType(String fieldType) {
        return withProperty(CkanConstants.FieldPropertyName.FIELD_TYPE, fieldType);
    }

    public FieldBuilder withDateFormat(String dataFormat) {
        return withProperty(CkanConstants.FieldPropertyName.DATE_FORMAT, dataFormat);
    }

    public FieldBuilder withDescription(String description) {
        return withProperty(CkanConstants.FieldPropertyName.FIELD_DESCRIPTION, description);
    }

    public FieldBuilder withRole(String role) {
        return withProperty(CkanConstants.FieldPropertyName.FIELD_ROLE, role);
    }

    public FieldBuilder withCrs(String crs) {
        return withProperty(CkanConstants.FieldPropertyName.CRS, crs);
    }

    public FieldBuilder withHeaderRows(int headerRows) {
        return withProperty(CkanConstants.FieldPropertyName.HEADER_ROWS, headerRows);
    }

    public FieldBuilder withPhenomenon(String phenomenon) {
        return withProperty(CkanConstants.FieldPropertyName.PHENOMENON, phenomenon);
    }

    public FieldBuilder withNoData(String noData) {
        return withProperty(CkanConstants.FieldPropertyName.NO_DATA, noData);
    }

    public FieldBuilder withLongName(String longName) {
        return withProperty(CkanConstants.FieldPropertyName.LONG_NAME, longName);
    }

    public FieldBuilder withShortName(String shortName) {
        return withProperty(CkanConstants.FieldPropertyName.SHORT_NAME, shortName);
    }

    public FieldBuilder withResourceName(String resourceName) {
        return withProperty(CkanConstants.FieldPropertyName.RESOURCE_NAME, resourceName);
    }

    public FieldBuilder withUom(String uom) {
        return withProperty(CkanConstants.FieldPropertyName.UOM, uom);
    }


    public FieldBuilder withProperty(String key, JsonNode value) {
        this.valuesByField.put(key, value);
        return this;
    }

    public FieldBuilder withProperty(String key, String value) {
        this.valuesByField.put(key, JSON_FACTORY.textNode(value));
        return this;
    }

    public FieldBuilder withProperty(String key, int value) {
        this.valuesByField.put(key, JSON_FACTORY.numberNode(value));
        return this;
    }

    public FieldBuilder withProperty(String key, double value) {
        this.valuesByField.put(key, JSON_FACTORY.numberNode(value));
        return this;
    }

    public FieldBuilder withProperty(String key, float value) {
        this.valuesByField.put(key, JSON_FACTORY.numberNode(value));
        return this;
    }

    public FieldBuilder withProperty(String key, boolean value) {
        this.valuesByField.put(key, JSON_FACTORY.booleanNode(value));
        return this;
    }

    public FieldBuilder withProperty(String key, Object value) {
        this.valuesByField.put(key, JSON_FACTORY.pojoNode(value));
        return this;
    }

    public ResourceField create() {
        ObjectNode field = JSON_FACTORY.objectNode();
        for (Map.Entry<String, JsonNode> property : valuesByField.entrySet()) {
            field.put(property.getKey(), property.getValue());
        }
        return new ResourceField(field, index, ckanMapping)
                .withResourceType(resourceType);
    }
    public ResourceField createSimple(String id) {
        return createViaTemplate("{ \"field_id\": \"%s\", \"field_type\": \"string\" }", id);
    }

    public ResourceField createViaTemplate(String jsonTemplate, Object... args) {
        try {
            jsonTemplate = String.format(jsonTemplate, args);
            JsonNode node = new ObjectMapper().readTree(jsonTemplate);
            return new ResourceField(node, index, ckanMapping);
        } catch (IllegalFormatException | IOException e) {
            LOGGER.error("Failed to create ResourceField", e);
            fail("Could not create field!");
            return null;
        }
    }

}
