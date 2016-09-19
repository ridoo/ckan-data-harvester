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
package org.n52.series.ckan.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

public class DataTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataTable.class);

    protected final Table<ResourceKey,ResourceField,String> table;

    protected final ResourceMember resourceMember;

    private final List<ResourceMember> joinedMembers;

    protected DataTable(ResourceMember resourceMember) {
        this(HashBasedTable.<ResourceKey,ResourceField,String>create(), resourceMember);
    }

    private DataTable(Table<ResourceKey,ResourceField,String> table, ResourceMember resourceMember) {
        this.table = table;
        this.resourceMember = resourceMember;
        this.joinedMembers = new ArrayList<>();
    }

    public Table<ResourceKey,ResourceField,String> getTable() {
        return table == null
                ? HashBasedTable.<ResourceKey,ResourceField,String>create()
                : ImmutableTable.<ResourceKey,ResourceField,String>copyOf(table);
    }

    public ResourceMember getResourceMember() {
        return resourceMember;
    }

    public DataTable extendWith(DataTable other) {
        if (resourceMember == null || resourceMember.getId() == null) {
            return other; // ignore trivial instance
        }

        if ( !resourceMember.isExtensible(other.resourceMember)) {
            return this;
        }

        DataTable outputTable = new DataTable(resourceMember);
        extendTable(other, outputTable);
        return outputTable;
    }

    private void extendTable(DataTable other, DataTable outputTable) {
        LOGGER.debug("joining table {} (#{} rows, #{} cols) with table {} (#{} rows, #{} cols)",
                     resourceMember.getId(), rowSize(), columnSize(),
                     other.resourceMember.getId(), other.rowSize(), columnSize());
        long start = System.currentTimeMillis();
        outputTable.table.putAll(table);
        outputTable.table.putAll(other.table);
        LOGGER.debug("extended table has #{} rows and #{} columns, took {}s",
                     outputTable.rowSize(),
                     outputTable.columnSize(),
                     (System.currentTimeMillis() - start) / 1000d);
    }

    public DataTable innerJoin(DataTable other, ResourceField... fields) {
        if (resourceMember == null || resourceMember.getId() == null) {
            return other; // ignore trivial instance
        }

        if ( !resourceMember.isJoinable(other.resourceMember)) {
            return this;
        }

        DataTable outputTable = new DataTable(resourceMember);
        outputTable.joinedMembers.add(other.resourceMember);
        Collection<ResourceField> joinFields = fields == null || fields.length == 0
                ? resourceMember.getJoinableFields(other.resourceMember)
                : Arrays.asList(fields);

        joinTable(other, outputTable, joinFields);
        return outputTable;
    }

    private void joinTable(DataTable other, DataTable outputTable, Collection<ResourceField> joinFields) {
        LOGGER.debug("joining table {} (#{} rows, #{} cols) with table {} (#{} rows, #{} cols)",
                resourceMember.getId(), rowSize(), columnSize(),
                other.resourceMember.getId(), other.rowSize(), columnSize());
        long start = System.currentTimeMillis();
        for (ResourceField field : joinFields) {
            final Map<ResourceKey, String> joinOnIndex = table.column(field);
            int i = 0;
            for (Map.Entry<ResourceKey, String> joinOnIndexEntry : joinOnIndex.entrySet()) {
                Map<ResourceKey, String> toJoinIndex = other.table.column(field);
                for (Map.Entry<ResourceKey, String> toJoinIndexEntry : toJoinIndex.entrySet()) {
                    if ( !field.equalsValues(joinOnIndexEntry.getValue(), toJoinIndexEntry.getValue())) {
                        continue;
                    }
                    final ResourceKey otherKey = toJoinIndexEntry.getKey();
                    final String newId = toJoinIndexEntry.getKey().getKeyId() + "_" + i++;
                    ResourceKey newKey = new ResourceKey(newId, outputTable.resourceMember);

                    // add other's values
                    final Map<ResourceField, String> toJoinRow = other.table.row(otherKey);
                    for (Map.Entry<ResourceField, String> otherValue : toJoinRow.entrySet()) {
                        final ResourceField rightField = otherValue.getKey();
                        ResourceField joinedField = ResourceField.copy(rightField)
                                .withQualifier(otherKey.getMember());
                        outputTable.table.put(newKey, joinedField, otherValue.getValue());
                    }

                    // add this instance's values
                    Map<ResourceField, String> joinOnRow = table.row(joinOnIndexEntry.getKey());
                    for (Map.Entry<ResourceField, String> value : joinOnRow.entrySet()) {
                        outputTable.table.put(newKey, value.getKey(), value.getValue());
                    }
                }
            }
        }
        LOGGER.debug("joined table has #{} rows and #{} columns, took {}s",
                outputTable.rowSize(),
                outputTable.columnSize(),
                (System.currentTimeMillis() - start) / 1000d);
    }

    public void addRow(ResourceKey id, ResourceField field, String value) {
        table.put(id, field, value);
    }

    public int rowSize() {
        return table.rowKeySet().size();
    }

    public int columnSize() {
        return table.columnKeySet().size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        return sb.append("DataTable(")
                .append("#rows=")
                .append(rowSize())
                .append(", #columns=")
                .append(columnSize())
                .append(", resource=")
                .append(resourceMember)
                .append(". Joined resources: [ ")
                .append(Arrays.toString(joinedMembers.toArray()))
                .append(" ])")
                .toString();
    }

}
