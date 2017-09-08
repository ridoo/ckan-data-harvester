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

package org.n52.series.ckan.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.n52.series.ckan.beans.ResourceField;
import org.n52.series.ckan.beans.ResourceMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

public class DataTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataTable.class);

    private static final ForkJoinPool FORK_JOIN_POOL = ForkJoinPool.commonPool();

    protected final Table<ResourceKey, ResourceField, String> table;

    protected final ResourceMember resourceMember;

    private final List<ResourceMember> joinedMembers;

    protected DataTable(ResourceMember resourceMember) {
        this(HashBasedTable.<ResourceKey, ResourceField, String> create(), resourceMember);
    }

    private DataTable(Table<ResourceKey, ResourceField, String> table, ResourceMember resourceMember) {
        this.table = table;
        this.resourceMember = resourceMember;
        this.joinedMembers = new ArrayList<>();
    }

    public Table<ResourceKey, ResourceField, String> getTable() {
        return table == null
                ? HashBasedTable.<ResourceKey, ResourceField, String> create()
                : ImmutableTable.<ResourceKey, ResourceField, String> copyOf(table);
    }

    public ResourceMember getResourceMember() {
        return resourceMember;
    }

    public Collection<ResourceField> getResourceFields() {
        Set<ResourceField> fields = new HashSet<>(resourceMember.getResourceFields());
        for (ResourceMember joinedMember : joinedMembers) {
            fields.addAll(joinedMember.getResourceFields());
        }
        return Collections.unmodifiableSet(fields);
    }

    public DataTable extendWith(DataTable other) {
        return extendWith(other, () -> Boolean.FALSE);
    }

    public DataTable extendWith(DataTable other, Supplier<Boolean> interruptedSupplier) {
        if (resourceMember == null || resourceMember.getId() == null) {
            // ignore trivial instance
            return other;
        }

        // if (!resourceMember.isExtensible(other.resourceMember)) {
        // LOGGER.warn("Extension not applied as columns do not match: '{}'", other);
        // return this;
        // }

        DataTable outputTable = new DataTable(resourceMember);
        extendTable(other, outputTable);
        return outputTable;
    }

    private void extendTable(DataTable other, DataTable outputTable) {
        LOGGER.debug("extending table '{}' (#{} rows, #{} cols) with table '{}' (#{} rows, #{} cols)",
                     resourceMember.getId(),
                     rowSize(),
                     columnSize(),
                     other.resourceMember.getId(),
                     other.rowSize(),
                     columnSize());
        long start = System.currentTimeMillis();
        outputTable.table.putAll(table);
        outputTable.table.putAll(other.table);
        LOGGER.debug("extended table has #{} rows and #{} columns, took {}s",
                     outputTable.rowSize(),
                     outputTable.columnSize(),
                     (System.currentTimeMillis() - start) / 1000d);
    }

    public DataTable innerJoin(DataTable other, ResourceField... fields) {
        return innerJoin(other, () -> Boolean.FALSE, fields);
    }

    public DataTable innerJoin(DataTable other, Supplier<Boolean> interruptedSupplier, ResourceField... fields) {
        if (resourceMember == null || resourceMember.getId() == null) {
            // ignore trivial instance
            return other;
        }
        if (!resourceMember.isJoinable(other.resourceMember)) {
            LOGGER.debug("Tables are not joinable.");
            return this;
        }

        DataTable outputTable = new DataTable(resourceMember);
        outputTable.joinedMembers.add(other.resourceMember);
        Collection<ResourceField> joinFields = fields == null || fields.length == 0
                ? resourceMember.getJoinableFields(other.resourceMember)
                : Arrays.asList(fields);

        joinTable(other, outputTable, joinFields, interruptedSupplier);
        return outputTable;
    }

    private void joinTable(final DataTable other,
                           final DataTable outputTable,
                           Collection<ResourceField> joinFields,
                           Supplier<Boolean> interruptedSupplier) {
        LOGGER.debug("joining (on fields {}) {} with {}.", joinFields, this, other);
        final long start = System.currentTimeMillis();

        final int interval = 10;
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        ScheduledFuture< ? > processLogger = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                int rows = outputTable.rowSize();
                long rowsPer = rows * interval / getSeconds(start);
                LOGGER.trace("join performs #{} output rows per {}s (#{} total)", rowsPer, interval, rows);
            }

            private long getSeconds(long startTime) {
                long now = System.currentTimeMillis();
                return (now - start) / 1000;
            }
        }, interval, interval, TimeUnit.SECONDS);

        try {
            Stream<ResourceField> joinStream = joinFields.stream();
            ForkJoinTask.invokeAll(joinStream.map(f -> createJoinTasks(f, other, outputTable, interruptedSupplier))
                                             .collect(ArrayList<ForkJoinTask< ? >>::new, List::addAll, List::addAll));
        } catch (Throwable e) {
            LOGGER.info("Unable to complete join task", e);
        } finally {
            ForkJoinTask.helpQuiesce();
            processLogger.cancel(true);
        }

        LOGGER.debug("joined table has #{} rows and #{} columns, took {}s",
                     outputTable.rowSize(),
                     outputTable.columnSize(),
                     (System.currentTimeMillis() - start) / 1000d);
    }

    private List<ForkJoinTask< ? >> createJoinTasks(ResourceField field,
                                                    final DataTable other,
                                                    final DataTable outputTable,
                                                    Supplier<Boolean> interruptedSupplier) {
        // XXX does not consider AND in joinFields, but rather joins multiple times!
        final Map<ResourceKey, String> joinOnIndex = table.column(field);
        final Map<ResourceKey, String> toJoinIndex = other.table.column(field);
        Set<Entry<ResourceKey, String>> joinOn = joinOnIndex.entrySet();
        Set<Entry<ResourceKey, String>> toJoin = toJoinIndex.entrySet();
        return joinOn.stream()
                     .map(joinOnCell -> {
                         return FORK_JOIN_POOL.submit(() -> {
                             toJoin.stream()
                                   .filter(toJoinCell -> getJoinFilter(field, joinOnCell, toJoinCell))
                                   .forEach(cell -> {
                                       if (interruptedSupplier.get()) {
                                           // cancel
                                           return;
                                       }
                                       final ResourceKey joinOnKey = cell.getKey();
                                       // use key from cell iterated over once!
                                       final ResourceKey toJoinKey = joinOnCell.getKey();
                                       ResourceKey newKey = createJoinedRowId(outputTable, joinOnKey, toJoinKey);

                                       // add other's values
                                       final Map<ResourceField, String> toJoinRow = other.table.row(toJoinKey);
                                       Set<Entry<ResourceField, String>> toJoinValues = toJoinRow.entrySet();
                                       for (Map.Entry<ResourceField, String> toJoinValue : toJoinValues) {
                                           ResourceMember member = other.getResourceMember();
                                           ResourceField joinedField = cloneValueField(member, toJoinValue);
                                           outputTable.table.put(newKey, joinedField, toJoinValue.getValue());
                                       }

                                       // add this instance's values
                                       Map<ResourceField, String> joinOnRow = table.row(joinOnCell.getKey());
                                       for (Map.Entry<ResourceField, String> value : joinOnRow.entrySet()) {
                                           outputTable.table.put(newKey, value.getKey(), value.getValue());
                                       }
                                   });
                         });
                     })
                     .collect(Collectors.toList());
    }

    private ResourceField cloneValueField(ResourceMember member, Map.Entry<ResourceField, String> value) {
        return ResourceField.copy(value.getKey())
                            .setQualifier(member);
    }

    private ResourceKey createJoinedRowId(DataTable outputTable, ResourceKey joinOnKey, ResourceKey toJoinKey) {
        // String newId = otherKey.getKeyId()
        // + "_"
        // + outputTable.rowSize();
        String newId = joinOnKey.getKeyId()
                + "_"
                + toJoinKey.getKeyId();
        return new ResourceKey(newId, outputTable.resourceMember);
    }

    private boolean getJoinFilter(ResourceField field,
                                  Entry<ResourceKey, String> cellA,
                                  Entry<ResourceKey, String> cellB) {
        // TODO multiple join cells
        return field.equalsValues(cellA.getValue(), cellB.getValue());
    }

    public void setCellValue(ResourceKey id, ResourceField field, String value) {
        table.put(id, field, value);
    }

    public int rowSize() {
        // avoid ConcurrentModificationException
        return Collections.unmodifiableCollection(table.rowKeySet())
                          .size();
    }

    public int columnSize() {
        // avoid ConcurrentModificationException
        return new HashSet<>(table.columnKeySet())
                                                  .size();
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    public List<ResourceMember> getJoinedMembers() {
        return hasJoinedMembers()
                ? Collections.unmodifiableList(joinedMembers)
                : Collections.emptyList();
    }

    public boolean hasJoinedMembers() {
        return joinedMembers != null && !joinedMembers.isEmpty();
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

    private static class ShutdownInterruptException extends RuntimeException {

    }

}
