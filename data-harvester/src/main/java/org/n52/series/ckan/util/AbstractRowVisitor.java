package org.n52.series.ckan.util;

import java.util.Map;

import org.n52.series.ckan.beans.ResourceField;

public abstract class AbstractRowVisitor<T> implements FieldVisitor<T> {

    /**
     * Initialize state of visitor. 
     */
    public void init() {};
    
    /**
     * Visits a complete row, mapping <code>ResourceField</code>s to <code>String</code> values. 
     * 
     * @param rowEntry the row.
     */
    public FieldVisitor<T> visit(Map<ResourceField, String> rowEntry) {
        this.init();
        rowEntry.entrySet().stream()
            .forEach(row -> row.getKey().accept(this, row.getValue()));
        return this;
    }
    
}
