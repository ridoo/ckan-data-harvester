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
package org.n52.series.ckan.beans;

import static org.n52.series.ckan.util.JsonUtil.parseMissingToNegativeInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.n52.series.ckan.da.CkanConstants;
import org.n52.series.ckan.da.CkanMapping;
import org.n52.series.ckan.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class SchemaDescriptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaDescriptor.class);

    private final JsonNode node;

    private final CkanDataset dataset;

    private CkanMapping ckanMapping;

    private final List<ResourceMember> members;

    public SchemaDescriptor() {
        this(null, null);
    }

    public SchemaDescriptor(CkanDataset dataset, JsonNode node) {
        this(dataset, node, CkanMapping.loadCkanMapping());
    }

    public SchemaDescriptor(CkanDataset dataset, JsonNode node, CkanMapping ckanMapping) {
        this.node = node == null
                ? MissingNode.getInstance()
                : node;
        this.dataset = dataset == null
                ? new CkanDataset()
                : dataset;
        this.ckanMapping = ckanMapping == null
                ? CkanMapping.loadCkanMapping()
                : ckanMapping;
        this.members = parseMemberDescriptions();
    }

    public String getVersion() {
        return getStringValueOf(node, CkanConstants.SchemaDescriptor.VERSION);
    }

    public String getDescription() {
        return getStringValueOf(node, CkanConstants.SchemaDescriptor.DESCRIPTION);
    }

    public String getSchemaDescriptionType() {
        return getSchemaDescriptionType(node);
    }
    
    public String getSchemaDescriptionType(JsonNode jsonNode) {
        return getStringValueOf(jsonNode, CkanConstants.SchemaDescriptor.RESOURCE_TYPE);
    }

    private String getStringValueOf(JsonNode jsonNode, String field) {
        return JsonUtil.parse(jsonNode, ckanMapping.getSchemaDescriptionMappings(field));
    }

    public JsonNode getNode() {
        return node;
    }

    public JsonNode getMemberNodes() {
        return node.at("/members");
    }

    public CkanDataset getDataset() {
        return dataset;
    }

    public List<ResourceMember> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public Map<ResourceMember, DataFile> relateWithDataFiles(Map<String, DataFile> csvContents) {
        Map<ResourceMember, DataFile> memberRelations = new HashMap<>();
        if (csvContents == null) {
            return memberRelations;
        }

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
        final Iterator<JsonNode> iter = getMemberNodes().elements();
        while (iter.hasNext()) {
            JsonNode memberNode = iter.next();
            Set<String> resourceNames = ckanMapping.getFieldMappings(CkanConstants.FieldPropertyName.RESOURCE_NAME);
            List<String> resourceIds = JsonUtil.parseToList(memberNode, resourceNames);
            for (String resourceId : resourceIds) {
                String resourceType = getSchemaDescriptionType(memberNode);
                ResourceMember member = new ResourceMember(resourceId, resourceType, ckanMapping);
                member.setDatasetName(dataset.getName());
                Set<String> headerRowNames = ckanMapping.getFieldMappings(CkanConstants.FieldPropertyName.HEADER_ROWS);
                final int headerRows = parseMissingToNegativeInt(memberNode, headerRowNames);
                member.setHeaderRows(headerRows < 0 ? 1 : headerRows); // assume 1 header row by default
                member.setResourceFields(parseResourceFields(member, memberNode));
                resourceMembers.add(member);
            }
        }
        return resourceMembers;
    }

    private List<ResourceField> parseResourceFields(ResourceMember qualifier, JsonNode member) {
        List<ResourceField> fields = new ArrayList<>();
        JsonNode resourceType = member.findValue(CkanConstants.SchemaDescriptor.RESOURCE_TYPE);
        JsonNode fieldsNode = member.findValue(CkanConstants.SchemaDescriptor.FIELDS);
        Iterator<JsonNode> iter = fieldsNode.elements();
        int index = 0;
        while (iter.hasNext()) {
            JsonNode fieldNode = iter.next();
            fields.add(new ResourceField(fieldNode, index, ckanMapping)
                    .withResourceType(resourceType.asText())
                    .withQualifier(qualifier));
            index++;
        }
        return fields;
    }

    public CkanMapping getCkanMapping() {
        return ckanMapping;
    }


}
