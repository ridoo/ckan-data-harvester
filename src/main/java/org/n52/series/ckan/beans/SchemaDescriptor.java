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
package org.n52.series.ckan.beans;

import static org.n52.series.ckan.util.JsonUtil.parseMissingToNegativeInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.da.CkanMapping;
import org.n52.series.ckan.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class SchemaDescriptor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaDescriptor.class);

    private final JsonNode node;

    private final CkanDataset dataset;

    private CkanMapping ckanMapping;

    private final List<ResourceMember> members;

    public SchemaDescriptor(CkanDataset dataset, JsonNode node) {
        this(dataset, node, CkanMapping.loadCkanMapping());
    }

    public SchemaDescriptor(CkanDataset dataset, JsonNode node, CkanMapping ckanMapping) {
        this.node = node;
        this.dataset = dataset;
        this.ckanMapping = ckanMapping == null
                ? CkanMapping.loadCkanMapping()
                : ckanMapping;
        this.members = parseMemberDescriptions();
    }

    private String getStringValueOf(JsonNode jsonNode, String field) {
        return JsonUtil.parseMissingToEmptyString(jsonNode, ckanMapping.getMappings(field));
    }

    public String getVersion() {
        return getStringValueOf(node, CkanConstants.SchemaDescriptor.VERSION);
    }

    public String getDescription() {
        return getStringValueOf(node, CkanConstants.SchemaDescriptor.DESCRIPTION);
    }

    public String getSchemaDescriptionType() {
        return getStringValueOf(node, CkanConstants.SchemaDescriptor.RESOURCE_TYPE);
    }

    public JsonNode getNode() {
        return node;
    }

    public CkanDataset getDataset() {
        return dataset;
    }

    public List<ResourceMember> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public boolean hasDescription() {
        return !node.isMissingNode();
    }

    public Map<ResourceMember, DataFile> relateWithDataFiles(Map<String, DataFile> csvContents) {
        Map<ResourceMember, DataFile> memberRelations = new HashMap<>();
        for (ResourceMember member : members) {
            DataFile dataFile = csvContents.get(member.getId());
            if (dataFile == null) {
                LOGGER.info("Ignoring member '{}' has missing datafile (was null)", member);
            } else {
                memberRelations.put(member, dataFile);
            }
        }
        return memberRelations;
    }

    private List<ResourceMember> parseMemberDescriptions() {
        List<ResourceMember> resourceMembers = new ArrayList<>();
        final JsonNode membersNode = node.findValue("members");
        final Iterator<JsonNode> iter = membersNode.elements();
        while (iter.hasNext()) {
            JsonNode memberNode = iter.next();
            for (String id : JsonUtil.parseMissingToEmptyArray(memberNode, ckanMapping.getMappings(CkanConstants.MemberProperty.RESOURCE_NAME))) {
                ResourceMember member = new ResourceMember();
                member.setId(id); // TODO missing ids will cause conflicts/inconsistencies
                member.setResourceType(getStringValueOf(memberNode, CkanConstants.MemberProperty.RESOURCE_TYPE));
                final int headerRows = parseMissingToNegativeInt(memberNode, ckanMapping.getMappings(CkanConstants.MemberProperty.HEADER_ROWS));
                member.setHeaderRows(headerRows < 0 ? 1 : headerRows); // assume 1 header row by default
                member.setResourceFields(parseResourceFields(member, memberNode));
                resourceMembers.add(member);
            }
        }
        return resourceMembers;
    }

    private List<ResourceField> parseResourceFields(ResourceMember qualifier, JsonNode member) {
        List<ResourceField> fields = new ArrayList<>();
        JsonNode fieldsNode = member.findValue("fields");
        Iterator<JsonNode> iter = fieldsNode.elements();
        int index = 0;
        while (iter.hasNext()) {
            JsonNode fieldNode = iter.next();
            fields.add(new ResourceField(fieldNode, index, ckanMapping)
                       .withQualifier(qualifier));
            index++;
        }
        return fields;
    }

    public CkanMapping getCkanMapping() {
        return ckanMapping;
    }


}
