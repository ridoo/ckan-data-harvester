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

package org.n52.series.ckan.da;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface CkanConstants {

    Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    String DEFAULT_DATE_TIME_FORMAT_STRING = "YYYY-MM-dd'T'HH:mm:ssZ";

    String VALID_TIME_START = "valid_time_start";
    String VALID_TIME_END = "valid_time_end";
    String RESOURCE_TYPE = "resource_type";
    String DESCRIPTION = "description";
    String LONGITUDE = "longitude";
    String LATITUDE = "latitude";
    String LOCATION = "location";
    String CRS = "crs";

    interface SchemaDescriptor {
        String SCHEMA_DESCRIPTOR = "schema_descriptor";
        String RESOURCE_TYPE = CkanConstants.RESOURCE_TYPE;
        String DESCRIPTION = CkanConstants.DESCRIPTION;
        String VERSION = "version";
        String FIELDS = "fields";
        String ID = "id";
    }

    interface ResourceType {
        String CSV_OBSERVATIONS_COLLECTION = "csv-observations-collection";
        /**
         * since schema descriptior version 0.3
         */
        String OBSERVATIONS_WITH_GEOMETRIES = "observations_with_geometry";
        /**
         * since schema descriptior version 0.3
         */
        String OBSERVED_GEOMETRIES = "observed_geometries";
        String PLATFORMS = "platforms";
        String OBSERVATIONS = "observations";
    }

    /**
     * Internal property names which indicate a property being parsed. As actual descriptors may use different
     * terms, a {@link CkanMapping} is used to map alternate property names from either global mapping file or
     * for each dataset individually. A schema descriptor which uses <tt>my_field_id</tt> instead of
     * {@value #FIELD_ID} like here
     *
     * <pre>
     *  {
     *    "my_field_id" : "name",
     *    "field_type" : "String"
     *  }
     * </pre>
     *
     * can be recognized as {@link #FIELD_ID} when having a mapping as the following:
     *
     * <pre>
     *   property {
     *     "field_id": [
     *       "my_field_id"
     *     ]
     *   }
     * </pre>
     *
     * However, configuring alternate values have to be done like described in
     * {@link CkanConstants.KnownFieldIdValue}.
     */
    interface FieldPropertyName {
        String CRS = CkanConstants.CRS;
        String RESOURCE_TYPE = CkanConstants.RESOURCE_TYPE;
        String FIELD_DESCRIPTION = CkanConstants.DESCRIPTION;
        String RESOURCE_NAME = "resource_name";
        String HEADER_ROWS = "headerrows";
        String FIELD_ID = "field_id";
        String SHORT_NAME = "short_name";
        String LONG_NAME = "long_name";
        String FIELD_TYPE = "field_type";
        String NO_DATA = "no_data";
        String FIELD_ROLE = "field_role";
        String PHENOMENON = "phenomenon";
        String PHENOMENON_REF = "phenomenon_ref";
        String UOM = "uom";
        String DATE_FORMAT = "date_format";
    }

    /**
     * Internal <code>fieldId</code> valuess which indicate a specific field. As actual descriptors may use
     * different terms, a {@link CkanMapping} is used to map alternate field id values from either global
     * mapping file or for each dataset individually. For example, internal <tt>platform_id</tt> might be
     * mapped in the mapping file to alternate names via
     *
     * <pre>
     *   field {
     *     "platform_id": [
     *       "my_custom_platform_id",
     *       "another_id"
     *     ]
     *   }
     * </pre>
     */
    interface KnownFieldIdValue {
        String LATITUDE = CkanConstants.LATITUDE;
        String LONGITUDE = CkanConstants.LONGITUDE;
        String LOCATION = CkanConstants.LOCATION;
        String VALID_TIME_START = CkanConstants.VALID_TIME_START;
        String VALID_TIME_END = CkanConstants.VALID_TIME_END;
        String CRS = CkanConstants.CRS;
        String PLATFORM_ID = "platform_id";
        String ALTITUDE = "altitude";
        String PLATFORM_NAME = "platform_name";
        String FIRST_DATE = "first_date";
        String LAST_DATE = "last_date";
        String OBSERVATION_TIME = "observation_time";
        String VALUE = "value";
        String QUALITY = "quality";
        String TRACK_ID = "track_id";
        String TRACK_POINT = "track_point";
    }

    /**
     * Internal specific <code>role</code> values being parsed. As actual descriptors may use different terms,
     * a {@link CkanMapping} is used to map alternate role values from either global mapping file or for each
     * dataset individually. For example, internal role value <tt>location</tt> might be mapped in the mapping
     * file to alternate names via
     *
     * <pre>
     *   role {
     *     "location": [
     *       "place",
     *       "station"
     *     ]
     *   }
     * </pre>
     */
    interface KnownFieldRoleValue {
        String LOCATION = CkanConstants.LOCATION;
        String LATITUDE = CkanConstants.LATITUDE;
        String LONGITUDE = CkanConstants.LONGITUDE;
        String VALID_TIME_START = CkanConstants.VALID_TIME_START;
        String VALID_TIME_END = CkanConstants.VALID_TIME_END;
        String HEIGHT = "height";
        String TIMESTAMP = "timestamp";
    }

    /**
     * Internal specific <code>datatype</code> being parsed. As actual descriptors may use different terms, a
     * {@link CkanMapping} is used to map alternate data types from either global mapping file or for each
     * dataset individually. For example, internal datatype value <tt>double</tt> might be mapped in the
     * mapping file to alternate names via
     *
     * <pre>
     *   datatype {
     *     "double": [
     *       "float",
     *       "decimal"
     *     ]
     *   }
     * </pre>
     */
    interface DataType {
        String INTEGER = "integer";
        String BOOLEAN = "boolean";
        String GEOMETRY = "geometry";
        String DOUBLE = "double";
        String STRING = "string";
        String DATE = "date";
        String JSON = "json";
        List<String> QUANTITY = Collections.unmodifiableList(Arrays.<String> asList(
                                                                                    INTEGER,
                                                                                    DOUBLE));
    }

    interface Config {
        String CONFIG_PATH_JOIN_COLUMNS = "/join_fields";
    }
}
