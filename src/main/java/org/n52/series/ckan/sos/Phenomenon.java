/**
 * Copyright (C) 2013-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.series.ckan.sos;

import java.util.Objects;

public class Phenomenon {

    private final String id;

    private final int fieldIdx;

    private final String label;

    private final String uom;

    public Phenomenon(String id, String label, int fieldIdx) {
        this(id, label, fieldIdx, null);
    }

    public Phenomenon(String id, String label, int fieldIdx, String uom) {
        this.id = id;
        this.uom = uom;
        this.label = label;
        this.fieldIdx = fieldIdx;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getUom() {
        return uom;
    }

    public int getFieldIdx() {
        return fieldIdx;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.id);
        hash = 37 * hash + this.fieldIdx;
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
        if (this.fieldIdx != other.fieldIdx) {
            return false;
        }
        if (!Objects.equals(this.label, other.label)) {
            return false;
        }
        if (!Objects.equals(this.uom, other.uom)) {
            return false;
        }
        return true;
    }



}
