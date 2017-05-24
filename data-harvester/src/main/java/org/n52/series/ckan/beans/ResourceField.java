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

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.da.CkanMapping;
import org.n52.series.ckan.util.FieldVisitor;
import org.n52.series.ckan.util.JsonUtil;
import org.n52.series.ckan.util.VisitableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;

public class ResourceField implements VisitableField {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceField.class);

    public static ResourceField copy(ResourceField field) {
        return new ResourceField(field.node, field.index, field.ckanMapping);
    }

    private final String fieldId;

    private final JsonNode node;

    private final int index;

    private ResourceMember qualifier;

    private String resourceType;

    private CkanMapping ckanMapping;

    public ResourceField() {
        this(MissingNode.getInstance(), -1);
    }

    public ResourceField(JsonNode node, int index) {
        this(node, index, CkanMapping.loadCkanMapping());
    }

    public ResourceField(JsonNode node, int index, CkanMapping ckanMapping) {
        this.index = index;
        this.node = node == null
                ? MissingNode.getInstance()
                : node;
        this.ckanMapping = ckanMapping == null
                ? CkanMapping.loadCkanMapping()
                : ckanMapping;
        Set<String> alternates = this.ckanMapping.getFieldMappings(CkanConstants.FieldPropertyName.FIELD_ID);
        this.fieldId = JsonUtil.parse(node, alternates);
    }

    public ResourceField withResourceType(String type) {
        this.resourceType = type;
        return this;
    }

    public ResourceField withQualifier(ResourceMember qualifier) {
        this.qualifier = qualifier;
        return this;
    }

    public boolean isObservationField() {
        return isOfResourceType(CkanConstants.ResourceType.OBSERVATIONS)
                || isOfResourceType(CkanConstants.ResourceType.OBSERVATIONS_WITH_GEOMETRIES);
    }

    public boolean isOfResourceType(String type) {
        Set<String> knownMappings = ckanMapping.getResourceTypeMappings(type);
        if (resourceType != null && !resourceType.isEmpty()) {
            return knownMappings.contains(resourceType);
        }
        return qualifier != null && knownMappings.contains(qualifier.getResourceType());
    }

    public String getFieldId() {
        return fieldId;
    }

    public String getLowerCasedFieldId() {
        return fieldId.toLowerCase(Locale.ROOT);
    }

    public boolean matchesIndex(int i) {
        return index == i;
    }

    public int getIndex() {
        return index;
    }

    public ResourceMember getQualifier() {
        return qualifier;
    }

    public void setQualifier(ResourceMember qualifier) {
        this.qualifier = qualifier;
    }

    public String getShortName() {
        return getValueOfField(CkanConstants.FieldPropertyName.SHORT_NAME);
    }

    public String getLongName() {
        return getValueOfField(CkanConstants.FieldPropertyName.LONG_NAME);
    }

    public String getDescription() {
        return getValueOfField(CkanConstants.FieldPropertyName.FIELD_DESCRIPTION);
    }

    public String getFieldType() {
        return getValueOfField(CkanConstants.FieldPropertyName.FIELD_TYPE);
    }

    public String getFieldRole() {
        return getValueOfField(CkanConstants.FieldPropertyName.FIELD_ROLE);
    }

    private String getValueOfField(String property) {
        return JsonUtil.parse(node, ckanMapping.getPropertyMappings(property));
    }

    public boolean hasFieldRole() {
        return !getFieldRole().isEmpty();
    }

    public boolean isField(String field) {
        if (node.isMissingNode()) {
            return false;
        }
        final String value = getFieldId();
        return ckanMapping.hasFieldMappings(field, value);
    }

    public boolean hasProperty(String property) {
        // TODO move to JsonUtil and involve ckanMapping
        return !node.at("/" + property).isMissingNode();
    }

    public String getOther(String name) {
        return node.at("/" + name).asText();
    }

    public boolean equalsValues(String thisValue, String otherValue) {
        if (otherValue != null) {
            try {
                if (this.isOfType(Integer.class)) {
                    return new Integer(thisValue).equals(new Integer(otherValue));
                }
                if (this.isOfType(String.class)) {
                    return thisValue.equals(otherValue);
                }
            } catch (NumberFormatException e) {
                LOGGER.error("could not compare field value '{}' with '{}'", thisValue, otherValue, e);
            }
        }
        return false;
    }

    public boolean isOfType(Class<?> clazz) {
        return isOfType(clazz.getSimpleName());
    }


    public boolean isOneOfType(String[] types) {
        for (String type : types) {
            if (isOfType(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOfType(String ofType) {
        if (node.isMissingNode()) {
            return false;
        }
        final String fieldType = getFieldType();
        return ckanMapping.hasDataTypeMappings(ofType, fieldType);
    }

    @Override
    public <T> void accept(FieldVisitor<T> visitor, String value) {
        visitor.visit(this, normalizeValue(value));
    }

    private String normalizeValue(String value) {
        if (value != null) {
            try {
                if (this.isOfType(Integer.class)) {
                    value = new Integer(value).toString();
                } else if (this.isOfType(Double.class)) {
                    value = new Double(value).toString();
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Could normalize field value '{}' (type {}) ", value, getFieldType(), e);
            }
        }
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getLowerCasedFieldId(), 7);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResourceField other = (ResourceField) obj;
        if (!Objects.equals(this.getLowerCasedFieldId(), other.getLowerCasedFieldId())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ResourceField{fieldId=" + getFieldId() + ", qualifier=" + getQualifier() + ", index=" + getIndex() + '}';
    }

}
