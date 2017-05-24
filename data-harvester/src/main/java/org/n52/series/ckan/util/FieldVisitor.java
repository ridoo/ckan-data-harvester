package org.n52.series.ckan.util;

import org.n52.series.ckan.beans.ResourceField;

public interface FieldVisitor<T> {

    public void visit(ResourceField field, String value);

    public boolean hasResult();

    public T getResult();

}
