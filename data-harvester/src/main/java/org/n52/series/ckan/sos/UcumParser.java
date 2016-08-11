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

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.da.CkanConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcumParser implements UomParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(UcumParser.class);

    @Override
    public String parse(ResourceField field) {
        String value = field.getOther(CkanConstants.FieldPropertyName.UOM);
        final String unit = parseFromAlias(value);
        return unit != null
            ? unit
            : "";
    }

    protected String parseFromAlias(String uom) {
        try {
            // TODO non valid units from a mapping
            // return StandardUnitDB.instance().get(uom);
            return uom;
        }
        catch (Exception e) {
            LOGGER.error("Could not parse UOM '{}' to known UCUM symbol.", uom, e);
        }
        return null;
    }

}
