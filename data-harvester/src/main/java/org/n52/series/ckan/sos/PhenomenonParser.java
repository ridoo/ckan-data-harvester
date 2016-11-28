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

package org.n52.series.ckan.sos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;

public class PhenomenonParser {

    // TODO evaluate separating observableProperty parsing to schemaDescription

    private final UomParser uomParser;

    public PhenomenonParser() {
        this(new UcumParser());
    }

    public PhenomenonParser(UomParser uomParser) {
        this.uomParser = uomParser;
    }

    public List<Phenomenon> parse(Collection<ResourceField> resourceFields) {
        Set<Phenomenon> phenomena = new HashSet<>();
        for (ResourceField field : resourceFields) {
            if (isValueField(field)) {
                phenomena.add(parsePhenomenon(field));
            }
        }

        return new ArrayList<>(phenomena);
    }

    private boolean isValueField(ResourceField field) {
        return field.hasProperty(CkanConstants.FieldPropertyName.PHENOMENON)
                || field.hasProperty(CkanConstants.FieldPropertyName.UOM)
                && field.isField(CkanConstants.KnownFieldIdValue.VALUE);
    }

    private Phenomenon parsePhenomenon(ResourceField field) {
        int index = field.getIndex();
        String uom = uomParser.parse(field);
        String phenomenonId = parsePhenomenonId(field);
        String phenomenonName = parsePhenomenonName(field);
//        TODO String phenomenonDescription = parseDescription(field);
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
        }else if (field.hasProperty(CkanConstants.FieldPropertyName.PHENOMENON)) {
            return field.getOther(CkanConstants.FieldPropertyName.PHENOMENON);
        } else if (field.hasProperty(CkanConstants.FieldPropertyName.SHORT_NAME)) {
            return field.getOther(CkanConstants.FieldPropertyName.SHORT_NAME);
        } else {
            return field.getFieldId();
        }
    }

}
