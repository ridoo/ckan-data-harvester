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
package org.n52.series.ckan.da;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

public interface CkanConstants {

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    public static final String DEFAULT_DATE_TIME_FORMAT_STRING = "YYYY-MM-dd'T'HH:mm:ssZ";

    public static final SimpleDateFormat DEFAULT_DATE_TIME_FORMAT = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT_STRING);

    public interface SchemaDescriptor {
        public static final String SCHEMA_DESCRIPTOR = "schema_descriptor";
        public static final String VERSION = "schema_descriptor_version";
        public static final String ID = "schema_descriptor_id";
        public static final String DESCRIPTION = "schema_descriptor_description";
        public static final String RESOURCE_TYPE = "resource_type";
    }

    public interface ResourceType {
        public static final String CSV_OBSERVATIONS_COLLECTION = "csv-observations-collection";
        public static final String PLATFORMS = "platforms";
        public static final String OBSERVATIONS = "observations";
    }

    public interface FieldPropertyName {
        public static final String RESOURCE_NAME = "resource_name";
        public static final String RESOURCE_TYPE = "resource_type";
        public static final String HEADER_ROWS = "headerrows";
        public static final String FIELD_ID = "field_id";
        public static final String SHORT_NAME = "short_name";
        public static final String LONG_NAME = "long_name";
        public static final String FIELD_DESCRIPTION = "description";
        public static final String FIELD_TYPE = "field_type";
        public static final String NO_DATA = "no_data";
        public static final String FIELD_ROLE = "field_role";
        public static final String PHENOMENON = "phenomenon";
        public static final String UOM = "uom";
        public static final String CRS = "crs";
        public static final String DATE_FORMAT = "date_format";
    }

    // TODO refactor to only contain ids mapping to ckan-mapping-ids

    public interface KnownFieldIdValue {
        public static final String STATION_ID = "station_id";
        public static final String CRS = "crs";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String ALTITUDE = "altitude";
        public static final String STATION_NAME = "station_name";
        public static final String FIRST_DATE = "first_date";
        public static final String LAST_DATE = "last_date";
        public static final String RESULT_TIME = "result_time";
        public static final String LOCATION = "location";
        public static final String VALID_TIME_START = "valid_time_start";
        public static final String VALID_TIME_END = "valid_time_end";
        public static final String VALUE = "value";
        public static final String QUALITY = "quality";
    }

    public interface KnownFieldRoleValue {
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String HEIGHT = "height";
        public static final String TIMESTAMP = "timestamp";
        public static final String VALID_TIME_START = "valid_time_start";
        public static final String VALID_TIME_END = "valid_time_end";
        public static final String LOCATION = "location";
    }

    public interface DataType {
        public static final String INTEGER = "Integer";
        public static final String BOOLEAN = "Boolean";
        public static final String GEOMETRY = "Geometry";
        public static final String DOUBLE = "Double";
        public static final String STRING = "String";
        public static final String DATE = "Date";
    }

}
