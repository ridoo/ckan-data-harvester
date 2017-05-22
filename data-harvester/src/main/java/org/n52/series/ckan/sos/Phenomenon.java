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

import java.util.Objects;

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.n52.sos.ogc.om.OmConstants;
import org.n52.sos.ogc.om.OmObservableProperty;

public class Phenomenon {

    private final String id;

    private final ResourceField valueField;

    private final String observationType;
    
    private final String uom;

    private boolean softTyped;
    
    private String label;

    public Phenomenon(String id, String label, ResourceField valueField, String uom) {
        this.valueField = valueField;
        this.id = id;
        this.label = label;
        this.observationType = parseObservationType(valueField);
        // TODO String phenomenonDescription = parseDescription(field);
        this.uom = uom;
    }

    private String parseObservationType(ResourceField valueField) {
        if (valueField.isOneOfType(CkanConstants.DataType.QUANTITY)) {
            return OmConstants.OBS_TYPE_MEASUREMENT;
        }
        // fallback
        return OmConstants.OBS_TYPE_TEXT_OBSERVATION;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUom() {
        return uom;
    }
    
    public ResourceField getValueField() {
        return valueField;
    }

    public int getValueFieldIdx() {
        return valueField.getIndex();
    }

    public OmObservableProperty toObservableProperty() {
        OmObservableProperty observableProperty = new OmObservableProperty(getId());
        observableProperty.setHumanReadableIdentifier(getLabel());
        observableProperty.setUnit(getUom());
        return observableProperty;
    }

    public String getObservationType() {
        return observationType;
    }

    public boolean isSoftTyped() {
        return softTyped;
    }

    public void setSoftTyped(boolean softTyped) {
        this.softTyped = softTyped;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.id);
        hash = 37 * hash + Objects.hashCode(valueField);
        hash = 37 * hash + (softTyped ? 1231 : 1237);
        hash = 37 * hash + Objects.hashCode(this.label);
        hash = 37 * hash + Objects.hashCode(this.uom);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Phenomenon other = (Phenomenon) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(valueField, other.valueField)) {
            return false;
        }
        if (!Objects.equals(this.label, other.label)) {
            return false;
        }
        if (softTyped != other.softTyped) {
            return false;
        }
        if (!Objects.equals(this.uom, other.uom)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[id=").append(id)
                .append(", label=").append(label)
                .append(", uom=").append(uom)
                .append(", fieldIdx=").append(valueField.getIndex())
                .append(", softTyped=").append(softTyped)
                .append("]")
                .toString();
    }



}
