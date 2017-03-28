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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.n52.series.ckan.da.CkanMapping;

import eu.trentorise.opendata.jackan.model.CkanDataset;

public class DataCollection implements Iterable<Map.Entry<ResourceMember, DataFile>>{

    private final CkanDataset dataset;

    private final Map<ResourceMember, DataFile> dataCollection;

    private final DescriptionFile schemaDescriptor;

    public DataCollection() {
        this(null, null, null);
    }

    public DataCollection(CkanDataset dataset, DescriptionFile description, Map<String, DataFile> csvContents) {
        this.dataset = dataset == null
                ? new CkanDataset()
                : dataset;
        this.schemaDescriptor = description == null
                ? new DescriptionFile()
                : description;
        SchemaDescriptor descriptor = description.getSchemaDescription();
        this.dataCollection = descriptor.relateWithDataFiles(csvContents);
    }

    public CkanDataset getDataset() {
        return dataset;
    }

    public DataFile getDataFile(ResourceMember resourceMember) {
        return dataCollection.get(resourceMember);
    }

    public String getDescription() {
        return schemaDescriptor != null
                ? schemaDescriptor.getSchemaDescription().getDescription()
                : null;
    }

    public DescriptionFile getSchemaDescriptor() {
        return schemaDescriptor;
    }

    public Entry<ResourceMember, DataFile> getDataEntry(ResourceMember resourceMember) {
        Map<ResourceMember, DataFile> collection = getDataCollection();
        for (Entry<ResourceMember, DataFile> entry : collection.entrySet()) {
            if (entry.getKey().equals(resourceMember)) {
                return entry;
            }
        }
        return null;
    }

    public Map<ResourceMember, DataFile> getDataCollection() {
        return dataCollection != null
                ? Collections.unmodifiableMap(dataCollection)
                : Collections.<ResourceMember, DataFile>emptyMap();
    }


  public CkanMapping getCkanMapping() {
      SchemaDescriptor descriptor = schemaDescriptor.getSchemaDescription();
      return descriptor.getCkanMapping();
  }

    public DescriptorVersion getDescriptorVersion() {
        SchemaDescriptor schemaDescription = schemaDescriptor.getSchemaDescription();
        return new DescriptorVersion(schemaDescription.getVersion());
    }

    public Map<String, List<ResourceMember>> getResourceMembersByType() {
        return getResourceMembersByType(null);
    }

    public Map<String, List<ResourceMember>> getResourceMembersByType(Set<String> filter) {
        Map<String, List<ResourceMember>> resourceMembersByType = new HashMap<>();
        for (ResourceMember member : dataCollection.keySet()) {
            String resourceType = member.getResourceType();
            if (filter == null || filter.isEmpty() || filter.contains(resourceType)) {
                if ( !resourceMembersByType.containsKey(resourceType)) {
                    resourceMembersByType.put(resourceType, new ArrayList<ResourceMember>());
                }
                resourceMembersByType.get(resourceType).add(member);
            }
        }
        return resourceMembersByType;
    }

    public Set<ResourceField> getJoinFieldIds(Set<ResourceMember> members) {
        List<ResourceField> allFields = new ArrayList<>();
        FieldCounter counter = new FieldCounter();
        for (ResourceMember member : members) {
            final List<ResourceField> fields = member.getResourceFields();
            counter.updateWith(fields);
            allFields.addAll(fields);
        }

        // XXX buggy as it contains fields of the same resource type
        // this might lead (for example) to join columns of similar
        // structured resources (two observation tables containing
        // both the field MESS_DATUM

        Set<ResourceField> joinColumns = new LinkedHashSet<>();
        for (ResourceField field : allFields) {
            if (counter.isJoinColumn(field)) {
                joinColumns.add(field);
            }
        }
        return joinColumns;
    }
    
    @Override
    public Iterator<Entry<ResourceMember, DataFile>> iterator() {
        return dataCollection != null
                ? dataCollection.entrySet().iterator()
                : Collections.<ResourceMember, DataFile>emptyMap().entrySet().iterator();
    }

    private class FieldCounter {
        private final Map<ResourceField, FieldCount> counts = new HashMap<>();
        void updateWith(List<ResourceField> fields) {
            for (ResourceField field : fields) {
                if (counts.containsKey(field)) {
                    counts.get(field).count++;
                } else {
                    counts.put(field, new FieldCount());
                }
            }
        }
        boolean isJoinColumn(ResourceField field) {
            return counts.get(field).count > 1;
        }
    }

    private class FieldCount {
        private int count;
        public FieldCount() {
            count++;
        }
    }

}
