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
package org.n52.series.ckan.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.MissingNode;

import eu.trentorise.opendata.jackan.CkanClient;

public class JsonUtil {

    private static final ObjectMapper om = new ObjectMapper();

    static {
        CkanClient.configureObjectMapper(om);
    }

    public static ObjectMapper getCkanObjectMapper() {
        return om;
    }

    public static ObjectWriter getCkanObjectWriter() {
        return getWriter(om);
    }

    public static ObjectWriter getJsonWriter() {
        ObjectMapper mapper = new ObjectMapper();
        return getWriter(mapper);
    }

    private static ObjectWriter getWriter(ObjectMapper mapper) {
        mapper.reader();
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
        pp.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        return mapper.writer(pp);
    }

    public static String parse(JsonNode node, Set<String> alternateFieldNames) {
        JsonNode field = findField(node, alternateFieldNames);
        return !field.isMissingNode()
                ? field.asText()
                : "";
    }

    public static int parseMissingToNegativeInt(JsonNode node, Set<String> alternateFieldNames) {
        JsonNode field = findField(node, alternateFieldNames);
        return !field.isMissingNode()
                ? field.asInt()
                : -1;
    }
    public static List<String> parseToList(JsonNode node, Set<String> alternateFieldNames) {
        List<String> values = new ArrayList<>();
        JsonNode field = findField(node, alternateFieldNames);
        if ( !field.isMissingNode() && field.isArray()) {
            final Iterator<JsonNode> iter = field.iterator();
            while (iter.hasNext()) {
                values.add(iter.next().asText());
            }
        }
        return values;
    }

    private static JsonNode findField(JsonNode node, Set<String> alternateFieldNames) {
        JsonNode field = MissingNode.getInstance();
        if (alternateFieldNames != null) {
            for (String alternateFieldName : alternateFieldNames) {
                field = getNodeWithName(alternateFieldName, node);
                field = tryLowerCasedIfMissing(field, alternateFieldName, node);
                if ( !field.isMissingNode()) {
                    break;
                }
            }
        }
        return field;
    }

    private static JsonNode getNodeWithName(String fieldName, JsonNode node) {
        return node.at("/" + fieldName);
    }

    private static JsonNode tryLowerCasedIfMissing(JsonNode field, String fieldName, JsonNode node) {
        return field.isMissingNode()
                ? getNodeWithLowerCasedName(fieldName, node)
                : field;
    }

    private static JsonNode getNodeWithLowerCasedName(String fieldName, JsonNode node) {
        return node.at("/" + fieldName.toLowerCase(Locale.ROOT));
    }




}
