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

import java.util.ArrayList;
import java.util.List;

import org.n52.sos.ogc.OGCConstants;
import org.n52.sos.ogc.sensorML.elements.SmlIdentifier;

public class Procedure {

    private String id;

    private String longName;

    Procedure(String id, String longName) {
        this.id = id;
        this.longName = longName;
    }

    public String getId() {
        return id;
    }

    public String getLongName() {
        return longName;
    }

    List<SmlIdentifier> createIdentifierList() {
        List<SmlIdentifier> idents = new ArrayList<>();
        idents.add(createSmlId(OGCConstants.UNIQUE_ID, OGCConstants.URN_UNIQUE_IDENTIFIER, id));

        if (longName != null) {
            String urn = "urn:ogc:def:identifier:OGC:1.0:longName";
            idents.add(new SmlIdentifier("longName", urn, longName));
        }
        return idents;
    }

    private SmlIdentifier createSmlId(String name, String urn, String idValue) {
        return new SmlIdentifier(name, urn, idValue);
    }

}
