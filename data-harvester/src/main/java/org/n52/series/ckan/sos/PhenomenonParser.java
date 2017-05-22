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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.table.DataTable;
import org.n52.series.ckan.table.ResourceKey;

import com.google.common.collect.Table;

public class PhenomenonParser {

    // TODO evaluate separating observableProperty parsing to schemaDescription

    private final UomParser uomParser;

    public PhenomenonParser() {
        this(new UcumParser());
    }

    public PhenomenonParser(UomParser uomParser) {
        this.uomParser = uomParser;
    }

    public List<Phenomenon> parse(DataTable dataTable) {
        Collection<ResourceField> resourceFields = dataTable.getResourceFields();
        List<Phenomenon> phenomena = parse(resourceFields);
        Predicate<ResourceField> filter = new Predicate<ResourceField>() {
            @Override
            public boolean test(ResourceField field) {
                return isValueFieldWithPhenomenonReference(field);
            }
        };
        for (ResourceField valueField : filterFields(resourceFields, filter)) {
            String ref = valueField.getOther(CkanConstants.FieldPropertyName.PHENOMENON_REF);
            ResourceField phenomenonField = getReferencedField(ref, resourceFields);
            Table<ResourceKey, ResourceField, String> table = dataTable.getTable();
            Map<ResourceKey, String> column = table.column(phenomenonField);
            HashSet<String> phenomenonValues = new HashSet<String>(column.values());
            phenomena.addAll(parseSoftTypedPhenomenon(phenomenonField, valueField, phenomenonValues));
        }
        return phenomena;
    }

    private Collection< ? extends Phenomenon> parseSoftTypedPhenomenon(ResourceField phenomenonField,
                                                                       ResourceField valueField,
                                                                       HashSet<String> values) {
        Function< ? super String, ? extends Phenomenon> parseReferencedPhenomenon = e -> {
            String id = e;
            String label = phenomenonField.getFieldId() + "_" + e;
            String uom = uomParser.parse(phenomenonField);
            int index = valueField.getIndex();
            Phenomenon phenomenon = new Phenomenon(id, label, index, uom);
            phenomenon.setSoftTyped(true);
            return phenomenon;
        };
        return values.stream()
                     .map(parseReferencedPhenomenon)
                     .collect(Collectors.toList());
    }

    public List<Phenomenon> parse(Collection<ResourceField> resourceFields) {
        Predicate<ResourceField> filter = new Predicate<ResourceField>() {
            @Override
            public boolean test(ResourceField field) {
                return isValueField(field);
            }
        };
        return resourceFields.stream()
                             .filter(filter)
                             .map(e -> parsePhenomenon(e))
                             .collect(Collectors.toList());
    }

    private List<ResourceField> filterFields(Collection<ResourceField> fields, Predicate<ResourceField> filter) {
        return fields.stream()
                     .filter(filter)
                     .collect(Collectors.toList());
    }

    private ResourceField getReferencedField(String ref, Collection<ResourceField> resourceFields) {
        for (ResourceField resourceField : resourceFields) {
            if (resourceField.isField(ref)) {
                return resourceField;
            }
        }
        return null;
    }

    private boolean isValueField(ResourceField field) {
        return field.hasProperty(CkanConstants.FieldPropertyName.PHENOMENON)
                || field.hasProperty(CkanConstants.FieldPropertyName.UOM)
                        && field.isField(CkanConstants.KnownFieldIdValue.VALUE);
    }

    private boolean isValueFieldWithPhenomenonReference(ResourceField field) {
        return field.hasProperty(CkanConstants.FieldPropertyName.PHENOMENON_REF);
    }

    private Phenomenon parsePhenomenon(ResourceField field) {
        int index = field.getIndex();
        String uom = uomParser.parse(field);
        String phenomenonId = parsePhenomenonId(field);
        String phenomenonName = parsePhenomenonName(field);
        // TODO String phenomenonDescription = parseDescription(field);
        return new Phenomenon(phenomenonId, phenomenonName, index, uom);
    }

    private String parsePhenomenonId(ResourceField field) {
        // ignore "fieldId" : "value"
        return field.isField(CkanConstants.KnownFieldIdValue.VALUE)
                ? field.getOther(CkanConstants.FieldPropertyName.LONG_NAME)
                : field.getFieldId();
    }

    private String parsePhenomenonName(ResourceField field) {
        if (field.hasProperty(CkanConstants.FieldPropertyName.LONG_NAME)) {
            return field.getOther(CkanConstants.FieldPropertyName.LONG_NAME);
        } else if (field.hasProperty(CkanConstants.FieldPropertyName.PHENOMENON)) {
            return field.getOther(CkanConstants.FieldPropertyName.PHENOMENON);
        } else if (field.hasProperty(CkanConstants.FieldPropertyName.SHORT_NAME)) {
            return field.getOther(CkanConstants.FieldPropertyName.SHORT_NAME);
        } else {
            return field.getFieldId();
        }
    }

}
